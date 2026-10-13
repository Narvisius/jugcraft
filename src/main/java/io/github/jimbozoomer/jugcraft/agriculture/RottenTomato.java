package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A thrown Rotten Tomato ({@link RottenTomatoItem}). Whoever it hits it bumps as a snowball does, with no damage; it bursts
 * where it lands in a red splat and is gone. It touches no block.
 */
public class RottenTomato extends ThrowableItemProjectile {
	/** The splat's red, the owner's rotten tomato's. */
	public static final int SPLAT = 0x8E2B1E;

	public RottenTomato(EntityType<? extends RottenTomato> type, Level level) {
		super(type, level);
	}

	public RottenTomato(Level level, LivingEntity thrower, ItemStack tomato) {
		super(JugcraftAgriculture.ROTTEN_TOMATO, thrower, level, tomato.copyWithCount(1));
	}

	@Override
	protected Item getDefaultItem() {
		return JugcraftAgriculture.item(RottenTomatoItem.ID);
	}

	/** No damage, as a snowball's: the hit still bumps them back. */
	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		if (level() instanceof ServerLevel level) {
			hit.getEntity().hurtServer(level, damageSources().thrown(this, getOwner()), 0.0F);
		}
	}

	@Override
	protected void onHit(HitResult hit) {
		super.onHit(hit);
		if (level() instanceof ServerLevel level && !isRemoved()) {
			Vec3 at = hit.getLocation();
			level.sendParticles(new DustParticleOptions(SPLAT, 1.2F), at.x, at.y + 0.1, at.z, 14, 0.25, 0.15, 0.25, 0.0);
			level.playSound(null, at.x, at.y, at.z, SoundEvents.HONEY_BLOCK_BREAK, SoundSource.NEUTRAL, 0.8F, 1.3F);
			discard();
		}
	}
}
