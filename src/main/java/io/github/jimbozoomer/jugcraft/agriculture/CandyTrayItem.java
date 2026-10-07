package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/**
 * A Candy Tray: a buttered tin tray that a Candy Kettle's candy is poured onto ({@link CandyBatch}, kept on the tray).
 * Used, a tray of candy that has set breaks into its pieces and is empty again:
 * <ul>
 * <li>most candy sets in {@value #SET_TICKS} ticks;</li>
 * <li>rock candy grows its crystals for {@value #CRYSTAL_TICKS} ticks (a day);</li>
 * <li>taffy must be pulled while it is warm (for {@value #WARM_TICKS} ticks after pouring): hold use for
 * {@value #PULL_TICKS} ticks, {@value #PULLS} times. Left to go cold unpulled, it sets hard, as hard candy;</li>
 * <li>hard candy, broken with sticks in the other hand, makes a lollipop for each stick;</li>
 * <li>candy corn can take another layer of candy corn poured on, up to {@value #MAX_LAYERS}, each its own colour.</li>
 * </ul>
 */
public class CandyTrayItem extends Item {
	public static final int SET_TICKS = 100;
	public static final int WARM_TICKS = 600;
	public static final int PULL_TICKS = 20;
	public static final int PULLS = 4;
	public static final int CRYSTAL_TICKS = 24000;
	public static final int MAX_LAYERS = 3;

	public CandyTrayItem(Properties properties) {
		super(properties);
	}

	public static @Nullable CandyBatch batch(ItemStack tray) {
		return tray.get(JugcraftAgriculture.CANDY_BATCH);
	}

	/** Puts {@code batch} on {@code tray}: a tray of candy does not stack, and shows its candy's colour and name. */
	public static void fill(ItemStack tray, CandyBatch batch) {
		tray.set(JugcraftAgriculture.CANDY_BATCH, batch);
		tray.set(DataComponents.MAX_STACK_SIZE, 1);
		tray.set(DataComponents.DYED_COLOR, new DyedItemColor(Candies.shown(batch.kind(), batch.colors())));
		tray.set(DataComponents.ITEM_NAME, Component.translatable("item.jugcraft.candy_tray.filled",
				Candies.name(batch.kind(), batch.kind().flavoured() ? batch.flavours() : List.of())));
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack tray = player.getItemInHand(hand);
		CandyBatch batch = batch(tray);
		if (batch == null) {
			return InteractionResult.PASS;
		}
		long now = level.getGameTime();
		if (batch.pullable(now)) {
			player.startUsingItem(hand);
			return InteractionResult.CONSUME;
		}
		if (level.isClientSide() || !(player instanceof ServerPlayer server)) {
			return InteractionResult.SUCCESS;
		}
		if (!batch.ready(now)) {
			long left = batch.left(now);
			server.sendOverlayMessage(batch.kind() == CandyKind.ROCK_CANDY
					? Component.translatable("message.jugcraft.candy_tray.crystallising", (left + 1199) / 1200)
					: Component.translatable("message.jugcraft.candy_tray.setting", (left + 19) / 20));
			return InteractionResult.SUCCESS;
		}
		List<ItemStack> pieces = breakUp(batch, now, player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND
				: InteractionHand.MAIN_HAND), player);
		player.setItemInHand(hand, player.hasInfiniteMaterials() ? tray : new ItemStack(this));
		for (ItemStack candy : pieces) {
			if (!player.getInventory().add(candy)) {
				Block.popResource(level, player.blockPosition(), candy);
			}
		}
		CandyKind kind = batch.sets(now);
		boolean crisp = kind == CandyKind.HARD_CANDY || kind == CandyKind.TOFFEE || kind == CandyKind.ROCK_CANDY || kind == CandyKind.BURNT_SUGAR;
		level.playSound(null, player.getX(), player.getY(), player.getZ(), crisp ? SoundEvents.AMETHYST_BLOCK_BREAK : SoundEvents.HONEY_BLOCK_BREAK,
				SoundSource.PLAYERS, 0.6F, crisp ? 1.6F : 1.2F);
		return InteractionResult.SUCCESS;
	}

	/**
	 * The pieces a set tray of {@code batch} breaks into at {@code now}: hard candy makes a lollipop for each stick in
	 * {@code other} (the hand not holding the tray), using them up.
	 */
	public static List<ItemStack> breakUp(CandyBatch batch, long now, ItemStack other, Player player) {
		CandyKind kind = batch.sets(now);
		int pieces = batch.pieces();
		List<ItemStack> out = new ArrayList<>();
		if (kind == CandyKind.HARD_CANDY && other.is(Items.STICK)) {
			int sticks = Math.min(other.getCount(), pieces);
			out.add(Candies.make(CandyKind.LOLLIPOP, batch.flavours(), batch.colors(), sticks));
			other.consume(sticks, player);
			pieces -= sticks;
		}
		if (pieces > 0) {
			out.add(Candies.make(kind, batch.flavours(), batch.colors(), pieces));
		}
		return out;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.BRUSH;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		CandyBatch batch = batch(stack);
		return batch != null && batch.kind() == CandyKind.SALT_WATER_TAFFY ? PULL_TICKS : 0;
	}

	/** Held to the end of a pull, warm taffy is pulled once more. */
	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		CandyBatch batch = batch(stack);
		if (!level.isClientSide() && batch != null && batch.pullable(level.getGameTime())) {
			CandyBatch pulled = batch.pull();
			fill(stack, pulled);
			level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.PLAYERS, 0.8F, 1.3F);
			if (entity instanceof ServerPlayer player) {
				player.sendOverlayMessage(pulled.pulled() ? Component.translatable("message.jugcraft.candy_tray.pulled")
						: Component.translatable("message.jugcraft.candy_tray.pull", pulled.pulls(), PULLS));
				if (pulled.pulled()) {
					TrickOrTreat.award(player, "taffy_puller");
				}
			}
		}
		return stack;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		CandyBatch batch = batch(stack);
		if (batch == null) {
			tooltip.accept(Component.translatable("tooltip.jugcraft.candy_tray.empty").withStyle(ChatFormatting.GRAY));
			return;
		}
		tooltip.accept(Component.translatable("tooltip.jugcraft.candy_tray.pieces", batch.pieces()).withStyle(ChatFormatting.GOLD));
		if (batch.kind() == CandyKind.CANDY_CORN) {
			tooltip.accept(Component.translatable("tooltip.jugcraft.candy_tray.layers", batch.colors().size(), MAX_LAYERS)
					.withStyle(ChatFormatting.GRAY));
		}
		if (batch.kind() == CandyKind.SALT_WATER_TAFFY) {
			tooltip.accept(Component.translatable("tooltip.jugcraft.candy_tray.pulls", batch.pulls(), PULLS).withStyle(ChatFormatting.GRAY));
		}
		tooltip.accept(Component.translatable("tooltip.jugcraft.candy_tray." + batch.kind().getSerializedName()).withStyle(ChatFormatting.GRAY));
	}
}
