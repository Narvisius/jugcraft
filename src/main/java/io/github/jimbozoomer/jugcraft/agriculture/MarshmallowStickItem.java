package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A Marshmallow on a Stick. Hold it out (keep using it) over a lit Halloween Bonfire within {@value #BONFIRE_REACH}
 * blocks, or a lit campfire within {@value #CAMPFIRE_REACH}, and let go: after {@value #TOAST_TICKS} ticks it is a
 * golden Toasted Marshmallow, after {@value #BURN_TICKS} a Burnt Marshmallow (it starts to smoke a second before);
 * let go sooner and it is still raw. Walk away from the fire and it stops toasting.
 */
public class MarshmallowStickItem extends Item {
	public static final int TOAST_TICKS = 60;
	public static final int BURN_TICKS = 140;
	public static final double BONFIRE_REACH = 3.5;
	public static final double CAMPFIRE_REACH = 2.0;
	private static final int USE_TICKS = 72000;

	public MarshmallowStickItem(Properties properties) {
		super(properties);
	}

	/** Whether a lit bonfire or campfire is near enough {@code entity} to toast over. Looks at a 9 by 5 by 9 box. */
	public static boolean fireNear(Level level, LivingEntity entity) {
		Vec3 eyes = entity.getEyePosition();
		BlockPos feet = entity.blockPosition();
		for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-4, -2, -4), feet.offset(4, 2, 4))) {
			BlockState state = level.getBlockState(pos);
			double distance = Math.sqrt(Vec3.atCenterOf(pos).distanceToSqr(eyes));
			if (state.getBlock() instanceof HalloweenBonfireBlock && state.getValue(HalloweenBonfireBlock.LIT) && distance <= BONFIRE_REACH
					|| state.is(BlockTags.CAMPFIRES) && state.getValue(CampfireBlock.LIT) && distance <= CAMPFIRE_REACH) {
				return true;
			}
		}
		return false;
	}

	/** What a marshmallow held over the fire for {@code ticks} becomes (the same stick, still raw, before it toasts). */
	public static Item toasted(int ticks) {
		if (ticks >= BURN_TICKS) {
			return JugcraftAgriculture.item("burnt_marshmallow");
		}
		return JugcraftAgriculture.item(ticks >= TOAST_TICKS ? "toasted_marshmallow" : "marshmallow_on_a_stick");
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!fireNear(level, player)) {
			if (!level.isClientSide()) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.marshmallow_on_a_stick.no_fire"));
			}
			return InteractionResult.FAIL;
		}
		player.startUsingItem(hand);
		return InteractionResult.CONSUME;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.BOW;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return USE_TICKS;
	}

	@Override
	public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
		int held = USE_TICKS - remaining;
		if (!(level instanceof ServerLevel server) || held % 10 != 0) {
			return;
		}
		if (!fireNear(level, entity)) {
			entity.stopUsingItem();
			return;
		}
		Vec3 tip = entity.getEyePosition().add(entity.getLookAngle().scale(1.2));
		if (held >= BURN_TICKS - 20) {
			server.sendParticles(ParticleTypes.SMOKE, tip.x, tip.y, tip.z, 3, 0.05, 0.05, 0.05, 0.01);
		} else if (held >= TOAST_TICKS) {
			server.sendParticles(ParticleTypes.SMALL_FLAME, tip.x, tip.y, tip.z, 1, 0.02, 0.02, 0.02, 0.0);
		}
	}

	@Override
	public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
		int held = USE_TICKS - remaining;
		Item result = toasted(held);
		if (held < TOAST_TICKS || !(entity instanceof Player player)) {
			return false;
		}
		if (!level.isClientSide()) {
			stack.consume(1, player);
			ItemStack done = new ItemStack(result);
			if (!player.getInventory().add(done)) {
				Block.popResource(level, player.blockPosition(), done);
			}
			level.playSound(null, player.blockPosition(), held >= BURN_TICKS ? SoundEvents.FIRE_EXTINGUISH : SoundEvents.FIRECHARGE_USE,
					SoundSource.PLAYERS, 0.5F, 1.6F);
		}
		return true;
	}
}
