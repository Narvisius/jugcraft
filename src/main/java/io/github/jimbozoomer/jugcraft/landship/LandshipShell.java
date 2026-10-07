package io.github.jimbozoomer.jugcraft.landship;

import io.github.jimbozoomer.jugcraft.weapons.Blast;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A shell from the Landship's cannon (batch 49, docs/features/landship.md). It flies in a shallow arc and bursts when
 * it hits anything or after {@link #LIFETIME} ticks. The burst is a {@link Blast}: it hurts living things only and
 * never breaks, moves or burns a block.
 */
public class LandshipShell extends ThrowableItemProjectile {
	public static final int LIFETIME = 120;
	private static final double GRAVITY = 0.02;

	public LandshipShell(EntityType<? extends LandshipShell> type, Level level) {
		super(type, level);
	}

	public LandshipShell(Level level, LivingEntity owner) {
		super(JugcraftLandships.SHELL, owner, level, new ItemStack(JugcraftLandships.CANNON_SHELL));
	}

	@Override
	protected Item getDefaultItem() {
		return JugcraftLandships.CANNON_SHELL;
	}

	@Override
	protected double getDefaultGravity() {
		return GRAVITY;
	}

	@Override
	public void tick() {
		super.tick();
		if (isRemoved()) {
			return;
		}
		if (level().isClientSide()) {
			Vec3 back = position().subtract(getDeltaMovement().scale(0.5));
			level().addParticle(ParticleTypes.SMOKE, back.x, back.y, back.z, 0, 0.01, 0);
		} else if (tickCount >= LIFETIME) {
			burst((ServerLevel) level(), position());
		}
	}

	@Override
	protected void onHit(HitResult hit) {
		super.onHit(hit);
		if (level() instanceof ServerLevel server && !isRemoved()) {
			Vec3 center = hit.getLocation();
			if (hit instanceof BlockHitResult block) {
				// Just off the face it hit, so the block itself does not shield the blast.
				Direction face = block.getDirection();
				center = center.add(face.getStepX() * 0.3, face.getStepY() * 0.3, face.getStepZ() * 0.3);
			}
			burst(server, center);
		}
	}

	private void burst(ServerLevel level, Vec3 center) {
		Blast.detonate(level, center, this, getOwner(), Landship.SHELL_RADIUS, Landship.SHELL_DAMAGE);
		discard();
	}
}
