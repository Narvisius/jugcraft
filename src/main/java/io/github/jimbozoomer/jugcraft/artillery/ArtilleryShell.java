package io.github.jimbozoomer.jugcraft.artillery;

import io.github.jimbozoomer.jugcraft.weapons.Blast;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A shell from the big guns (batch 51, docs/features/big-guns.md). A Heavy Shell flies a long arc and bursts on what it
 * hits; a Flak Shell bursts beside anything flying that it passes within {@value JugcraftArtillery#FLAK_PROXIMITY}
 * blocks of, or when its fuse runs out. A Great Shell (batch 54, the Grand Mortar's) flies like a Heavy Shell and bursts
 * wider and harder. Every burst is a {@link Blast}: it hurts living things only and never breaks, moves or burns a block.
 */
public class ArtilleryShell extends ThrowableItemProjectile {
	private static final int HEAVY_LIFETIME = 600;

	public ArtilleryShell(EntityType<? extends ArtilleryShell> type, Level level) {
		super(type, level);
	}

	public ArtilleryShell(EntityType<? extends ArtilleryShell> type, Level level, LivingEntity owner) {
		super(type, owner, level, new ItemStack(itemFor(type)));
	}

	private static Item itemFor(EntityType<?> type) {
		return type == JugcraftArtillery.FLAK_SHELL ? JugcraftArtillery.FLAK_SHELL_ITEM
				: type == JugcraftTowerGuns.GREAT_SHELL ? JugcraftTowerGuns.GREAT_SHELL_ITEM : JugcraftArtillery.HEAVY_SHELL_ITEM;
	}

	private boolean flak() {
		return getType() == JugcraftArtillery.FLAK_SHELL;
	}

	private boolean great() {
		return getType() == JugcraftTowerGuns.GREAT_SHELL;
	}

	@Override
	protected Item getDefaultItem() {
		return itemFor(getType());
	}

	@Override
	protected double getDefaultGravity() {
		return flak() ? JugcraftArtillery.FLAK_GRAVITY : great() ? JugcraftTowerGuns.GREAT_GRAVITY : JugcraftArtillery.HEAVY_GRAVITY;
	}

	@Override
	public void tick() {
		super.tick();
		if (isRemoved()) {
			return;
		}
		if (level().isClientSide()) {
			Vec3 back = position().subtract(getDeltaMovement().scale(0.5));
			level().addParticle(flak() ? ParticleTypes.SMOKE : ParticleTypes.LARGE_SMOKE, back.x, back.y, back.z, 0, 0.01, 0);
			return;
		}
		ServerLevel level = (ServerLevel) level();
		if (flak()) {
			if (tickCount >= JugcraftArtillery.FLAK_FUSE || nearFlyer(level)) {
				burst(level, position());
			}
		} else if (tickCount >= HEAVY_LIFETIME) {
			burst(level, position());
		}
	}

	/** Whether something airborne (not standing on the ground, not riding) is within the proximity fuse's reach. */
	private boolean nearFlyer(ServerLevel level) {
		Entity owner = getOwner();
		AABB reach = getBoundingBox().inflate(JugcraftArtillery.FLAK_PROXIMITY);
		return !level.getEntitiesOfClass(LivingEntity.class, reach, e -> e.isAlive() && !e.onGround() && !e.isPassenger()
				&& e != owner && (owner == null || e.getRootVehicle() != owner.getRootVehicle())).isEmpty();
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
		if (flak()) {
			level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y, center.z, 10, 0.4, 0.4, 0.4, 0.02);
			Blast.detonate(level, center, this, getOwner(), JugcraftArtillery.FLAK_RADIUS, JugcraftArtillery.FLAK_DAMAGE);
		} else if (great()) {
			level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 3, 1.5, 0.5, 1.5, 0);
			Blast.detonate(level, center, this, getOwner(), JugcraftTowerGuns.GREAT_RADIUS, JugcraftTowerGuns.GREAT_DAMAGE);
		} else {
			level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, 0, 0, 0, 0);
			Blast.detonate(level, center, this, getOwner(), JugcraftArtillery.HEAVY_RADIUS, JugcraftArtillery.HEAVY_DAMAGE);
		}
		discard();
	}
}
