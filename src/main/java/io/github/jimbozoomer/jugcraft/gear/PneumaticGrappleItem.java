package io.github.jimbozoomer.jugcraft.gear;

import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * The pneumatic grapple (batch 30). Use it to fire a hook on a line, {@link JugcraftGrapple#SHOT_COST} mB of nitrogen a
 * shot: a hook in a block reels its owner in (no fall damage, a hop over the edge at the end); a hook in a mob pulls the
 * mob in. Use it again to let go. Use it on anything holding nitrogen (the air separation unit, a gas holder) to fill it,
 * like the scuba tank with oxygen.
 */
public class PneumaticGrappleItem extends Item {
	/** Fabric counts fluids in droplets: 81,000 to the bucket, 81 to the millibucket. */
	private static final long DROPLETS_PER_MB = 81;
	/** Nitrogen's gauge colour. */
	private static final int BAR_COLOR = 0xFF96A8D6;

	public PneumaticGrappleItem(Properties properties) {
		super(properties);
	}

	public static int nitrogen(ItemStack stack) {
		return stack.getOrDefault(JugcraftGrapple.NITROGEN, 0);
	}

	public static void setNitrogen(ItemStack stack, int amount) {
		stack.set(JugcraftGrapple.NITROGEN, Math.max(0, Math.min(JugcraftGrapple.CAPACITY, amount)));
	}

	/** Fills from the nitrogen in whatever was clicked, in whole millibuckets; anything else falls through to a shot. */
	@Override
	public InteractionResult useOn(UseOnContext context) {
		ItemStack stack = context.getItemInHand();
		int room = JugcraftGrapple.CAPACITY - nitrogen(stack);
		if (room <= 0 || !(context.getLevel() instanceof ServerLevel level)) {
			return InteractionResult.PASS;
		}
		Storage<FluidVariant> storage = FluidStorage.SIDED.find(level, context.getClickedPos(), context.getClickedFace());
		if (storage == null) {
			return InteractionResult.PASS;
		}
		FluidVariant nitrogen = FluidVariant.of(PetroFluids.NITROGEN.fluid());
		long millibuckets;
		try (Transaction simulation = Transaction.openOuter()) {
			millibuckets = storage.extract(nitrogen, room * DROPLETS_PER_MB, simulation) / DROPLETS_PER_MB;
		}
		if (millibuckets <= 0) {
			return InteractionResult.PASS;
		}
		try (Transaction transaction = Transaction.openOuter()) {
			if (storage.extract(nitrogen, millibuckets * DROPLETS_PER_MB, transaction) != millibuckets * DROPLETS_PER_MB) {
				return InteractionResult.PASS;
			}
			transaction.commit();
		}
		setNitrogen(stack, nitrogen(stack) + (int) millibuckets);
		level.playSound(null, context.getClickedPos(), SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 0.6F, 1.6F);
		return InteractionResult.SUCCESS;
	}

	/** Fires the hook, or lets go of the one already out. */
	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		GrappleHook out = GrappleHook.active(player);
		if (out != null) {
			out.discard();
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 0.5F, 1.4F);
			return InteractionResult.SUCCESS;
		}
		boolean free = player.hasInfiniteMaterials();
		if (!free && nitrogen(stack) < JugcraftGrapple.SHOT_COST) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.pneumatic_grapple.empty"));
			return InteractionResult.FAIL;
		}
		GrappleHook hook = Projectile.spawnProjectileFromRotation(GrappleHook::new, server, stack.copyWithCount(1), player, 0.0F,
				JugcraftGrapple.LAUNCH_SPEED, 0.0F);
		GrappleHook.track(player, hook);
		if (!free) {
			setNitrogen(stack, nitrogen(stack) - JugcraftGrapple.SHOT_COST);
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 0.8F, 1.6F);
		player.getCooldowns().addCooldown(stack, JugcraftGrapple.COOLDOWN);
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.round(13.0F * nitrogen(stack) / JugcraftGrapple.CAPACITY);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return BAR_COLOR;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.jugcraft.nitrogen", String.format("%,d", nitrogen(stack)),
				String.format("%,d", JugcraftGrapple.CAPACITY)).withStyle(ChatFormatting.BLUE));
		tooltip.accept(Component.translatable("tooltip.jugcraft.pneumatic_grapple").withStyle(ChatFormatting.GRAY));
	}
}
