package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Knitting Needles: with a ball of yarn in the other hand, hold use to knit a row ({@value #ROW_TICKS} ticks), using the
 * ball. When the project has all its rows ({@link Knitwear#rows}) the garment is done, in the blend of the rows' colours,
 * and the needles take a point of wear. Sneak and use to change the project (a beanie, socks, or one of the sweaters);
 * with rows on the needles, sneaking unpicks them instead, giving the yarn back. The work is the needles' own
 * ({@link KnittingWork}), so it keeps when they are put away.
 */
public class KnittingNeedlesItem extends Item {
	public static final int ROW_TICKS = 40;

	public KnittingNeedlesItem(Properties properties) {
		super(properties);
	}

	public static KnittingWork work(ItemStack needles) {
		return needles.getOrDefault(JugcraftAgriculture.KNITTING, KnittingWork.NONE);
	}

	private static ItemStack other(Player player, InteractionHand hand) {
		return player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack needles = player.getItemInHand(hand);
		KnittingWork work = work(needles);
		if (player.isShiftKeyDown()) {
			if (!level.isClientSide() && player instanceof ServerPlayer server) {
				if (work.rows() > 0) {
					// Unpick the rows: their yarn back, in their blend.
					give(server, Knitting.yarn(work.color(), work.rows()));
					needles.set(JugcraftAgriculture.KNITTING, work.restart(false));
					server.sendOverlayMessage(Component.translatable("message.jugcraft.knitting.unpicked", work.rows()));
				} else {
					KnittingWork next = work.restart(true);
					needles.set(JugcraftAgriculture.KNITTING, next);
					server.sendOverlayMessage(Component.translatable("message.jugcraft.knitting.project", garmentName(next.knitwear()),
							next.knitwear().rows));
				}
				level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WOOL_HIT, SoundSource.PLAYERS, 0.6F, 1.4F);
			}
			return InteractionResult.SUCCESS;
		}
		if (!Knitting.isYarn(other(player, hand))) {
			if (player instanceof ServerPlayer server) {
				server.sendOverlayMessage(Component.translatable("message.jugcraft.knitting.no_yarn"));
			}
			return InteractionResult.FAIL;
		}
		player.startUsingItem(hand);
		return InteractionResult.CONSUME;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.BRUSH;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return ROW_TICKS;
	}

	/** Held to the end of a row: a ball of yarn knitted in, and the garment finished on the last row. */
	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (!level.isClientSide() && entity instanceof Player player) {
			knitRow(player, stack, other(player, player.getUsedItemHand()));
		}
		return stack;
	}

	/**
	 * Knits one row on {@code needles} with a ball of {@code yarn} (used up but by a creative player); finishes the
	 * garment on its last row (given to {@code player}). Returns the garment finished, or empty.
	 */
	public static ItemStack knitRow(Player player, ItemStack needles, ItemStack yarn) {
		if (!Knitting.isYarn(yarn)) {
			return ItemStack.EMPTY;
		}
		KnittingWork work = work(needles).row(Knitting.color(yarn));
		yarn.consume(1, player);
		Level level = player.level();
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 0.5F, 1.6F);
		if (!work.done()) {
			needles.set(JugcraftAgriculture.KNITTING, work);
			if (player instanceof ServerPlayer server) {
				server.sendOverlayMessage(Component.translatable("message.jugcraft.knitting.row", work.rows(), work.knitwear().rows,
						garmentName(work.knitwear())));
			}
			return ItemStack.EMPTY;
		}
		ItemStack garment = Knitting.garment(work.knitwear(), work.color());
		needles.set(JugcraftAgriculture.KNITTING, work.restart(false));
		give(player, garment.copy());
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 0.8F, 1.0F);
		if (player instanceof ServerPlayer server) {
			server.sendOverlayMessage(Component.translatable("message.jugcraft.knitting.done", garmentName(work.knitwear())));
			TrickOrTreat.award(server, "knit_one_purl_two");
		}
		needles.hurtAndBreak(1, player, player.getUsedItemHand());
		return garment;
	}

	private static void give(Player player, ItemStack stack) {
		if (!player.getInventory().add(stack)) {
			Block.popResource(player.level(), player.blockPosition(), stack);
		}
	}

	private static Component garmentName(Knitwear knit) {
		return Component.translatable("item.jugcraft." + knit.item);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		KnittingWork work = work(stack);
		tooltip.accept(Component.translatable("tooltip.jugcraft.knitting_needles.project", garmentName(work.knitwear()), work.rows(),
				work.knitwear().rows).withStyle(ChatFormatting.GRAY));
	}
}
