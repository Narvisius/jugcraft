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
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * A thrown Toilet Paper Roll. Where it lands it unrolls: up to {@value #STREAMERS} streamers go up within
 * {@value #REACH} blocks ({@link ToiletPaperStreamerBlock#drape}), and the roll is used up. It does no harm to whatever
 * it hits.
 */
public class ToiletPaperRoll extends ThrowableItemProjectile {
	public static final int STREAMERS = 4;
	public static final int REACH = 2;

	public ToiletPaperRoll(EntityType<? extends ToiletPaperRoll> type, Level level) {
		super(type, level);
	}

	public ToiletPaperRoll(Level level, LivingEntity thrower, ItemStack roll) {
		super(JugcraftAgriculture.TOILET_PAPER_ROLL, thrower, level, roll.copyWithCount(1));
	}

	@Override
	protected Item getDefaultItem() {
		return JugcraftAgriculture.item("toilet_paper_roll");
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (level() instanceof ServerLevel level) {
			ToiletPaperStreamerBlock.drape(level, hit.getBlockPos().relative(hit.getDirection()), REACH, STREAMERS, random);
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		if (level() instanceof ServerLevel level) {
			ToiletPaperStreamerBlock.drape(level, hit.getEntity().blockPosition(), REACH, STREAMERS, random);
		}
	}

	@Override
	protected void onHit(HitResult hit) {
		super.onHit(hit);
		if (level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.POOF, getX(), getY(), getZ(), 6, 0.2, 0.2, 0.2, 0.02);
			discard();
		}
	}
}
