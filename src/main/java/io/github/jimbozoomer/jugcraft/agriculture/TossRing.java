package io.github.jimbozoomer.jugcraft.agriculture;

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
 * A thrown Toss Ring (fall addition 26). Landing on the top of a Ring Toss crate it may be a ringer
 * ({@link RingTossBlock#land}), which uses it up; anywhere else it drops back to the ground as an item, to be thrown
 * again. It harms nothing. It remembers where it was thrown from, which a ringer must be far enough from.
 */
public class TossRing extends ThrowableItemProjectile {
	private Vec3 from;

	public TossRing(EntityType<? extends TossRing> type, Level level) {
		super(type, level);
		from = position();
	}

	public TossRing(Level level, LivingEntity thrower, ItemStack ring) {
		super(JugcraftAgriculture.TOSS_RING, thrower, level, ring.copyWithCount(1));
		from = thrower.position();
	}

	/** Where it was thrown from. */
	public Vec3 from() {
		return from;
	}

	@Override
	protected Item getDefaultItem() {
		return JugcraftAgriculture.item(TossRingItem.ID);
	}

	@Override
	protected void onHit(HitResult hit) {
		super.onHit(hit);
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		boolean ringer = hit instanceof BlockHitResult block && RingTossBlock.land(level, block.getBlockPos(), hit.getLocation(), from, getOwner());
		if (!ringer) {
			spawnAtLocation(level, getItem().copyWithCount(1));
			level.sendParticles(ParticleTypes.POOF, getX(), getY(), getZ(), 3, 0.1, 0.1, 0.1, 0.01);
		}
		discard();
	}
}
