package io.github.jimbozoomer.jugcraft.concordance;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * A lantern that holds Radiance ({@link LanternCharge}). Using it lights it or puts it out. Lit, it burns one measure
 * every {@value #BURN_TICKS} ticks whether carried or not (worked out from the game time, so a lantern left lit in a
 * chest is out when it is taken back), and while it is in either hand the server keeps a {@link LumenMoteBlock#TRAIL}
 * light in the open air beside its carrier, through {@link Illumination#trail}. It is recharged at a Lampwright's Bench.
 * <p>
 * Anyone may carry and light one, so a Lampwright can give or trade light to people who never study Radiance; only
 * someone who understands it can recharge one. Keep the numbers equal to LANTERN_CAPACITY and LANTERN_BURN_TICKS in
 * tools/concordance.py.
 */
public class KindledLanternItem extends Item {
	public static final int CAPACITY = 64;
	public static final int BURN_TICKS = 400;
	/** How far a trail light looks for the lantern that keeps it lit. */
	private static final double TRAIL_REACH = 2.5;

	public KindledLanternItem(Properties properties) {
		super(properties);
	}

	public static boolean lit(ItemStack stack) {
		return stack.has(JugcraftConcordance.LANTERN_LIT);
	}

	public static LanternCharge charge(ItemStack stack) {
		return stack.getOrDefault(JugcraftConcordance.RADIANCE, LanternCharge.EMPTY);
	}

	/** The Radiance left at {@code now}. */
	public static int remaining(ItemStack stack, long now) {
		return charge(stack).remaining(now, lit(stack));
	}

	/** Sets the charge (settled to {@code now}) and the lit state together, so the two can never disagree. */
	public static void set(ItemStack stack, int stored, long now, boolean lit) {
		stack.set(JugcraftConcordance.RADIANCE, new LanternCharge(stored, now));
		if (lit && stored > 0) {
			stack.set(JugcraftConcordance.LANTERN_LIT, Unit.INSTANCE);
		} else {
			stack.remove(JugcraftConcordance.LANTERN_LIT);
		}
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		ItemStack stack = player.getItemInHand(hand);
		long now = ConcordanceProgress.now(server);
		boolean wasLit = lit(stack);
		int left = remaining(stack, now);
		if (wasLit) {
			// The measure now burning is spent (roadmap step 29): relighting never gives light for nothing.
			int kept = charge(stack).kept(now, true);
			set(stack, kept, now, false);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.lantern.out", kept));
			server.playSound(null, player.blockPosition(), JugcraftConcordance.LANTERN_SNUFF_SOUND, SoundSource.PLAYERS, 0.7F, 1.0F);
			return InteractionResult.SUCCESS;
		}
		if (left <= 0) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.lantern.empty"));
			return InteractionResult.FAIL;
		}
		set(stack, left, now, true);
		player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.lantern.lit", left));
		server.playSound(null, player.blockPosition(), JugcraftConcordance.LANTERN_IGNITE_SOUND, SoundSource.PLAYERS, 0.7F, 1.0F);
		return InteractionResult.SUCCESS;
	}

	/** Sneak-used on a Lumen Sconce, the lantern draws Radiance back out of it (its owner only). */
	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player != null && player.isShiftKeyDown()
				&& context.getLevel().getBlockState(context.getClickedPos()).is(JugcraftConcordance.LUMEN_SCONCE)) {
			return LumenSconceBlock.exchange(context.getLevel(), context.getClickedPos(), player, context.getItemInHand(), true);
		}
		return InteractionResult.PASS;
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
		if (!lit(stack)) {
			return;
		}
		long now = ConcordanceProgress.now(level);
		LanternCharge charge = charge(stack);
		int left = charge.remaining(now, true);
		if (left <= 0) {
			set(stack, 0, now, false);
			level.playSound(null, entity.blockPosition(), JugcraftConcordance.LANTERN_SNUFF_SOUND, SoundSource.PLAYERS, 0.5F, 0.8F);
			return;
		}
		if (left < charge.stored()) {
			// Settle one burnt measure at a time, so the bar and the item stay true without writing every tick.
			stack.set(JugcraftConcordance.RADIANCE, charge.settle(now, true));
		}
		if (entity instanceof Player player && now % LumenMoteBlock.TRAIL_CHECK_TICKS == 0
				&& (player.getMainHandItem() == stack || player.getOffhandItem() == stack) && !player.isSpectator()) {
			Illumination.trail(level, player, player.blockPosition());
		}
	}

	/** Whether a player within reach of {@code pos} holds a lit Kindled Lantern (what keeps a trail light). */
	public static boolean litLanternNear(ServerLevel level, BlockPos pos) {
		double reach = TRAIL_REACH * TRAIL_REACH;
		for (Player player : level.players()) {
			if (!player.isSpectator() && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= reach
					&& (isLitLantern(player.getMainHandItem()) || isLitLantern(player.getOffhandItem()))) {
				return true;
			}
		}
		return false;
	}

	private static boolean isLitLantern(ItemStack stack) {
		return stack.is(JugcraftConcordance.KINDLED_LANTERN) && lit(stack);
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.round(13.0F * charge(stack).stored() / CAPACITY);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return lit(stack) ? 0xF4D27A : 0x8A7BB8;
	}

	/** Lighting, burning and recharging change only the lantern's components: no re-equip bob in the hand. */
	public boolean allowComponentsUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
		return false;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.jugcraft.kindled_lantern").withStyle(ChatFormatting.GRAY));
	}
}
