package io.github.jimbozoomer.jugcraft.weapons;

import io.github.jimbozoomer.jugcraft.chemistry.PetroItems;
import net.minecraft.core.Direction;
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

/** A thrown or launched grenade: it goes off when it hits anything, as its item's {@link Warhead} says. */
public class GrenadeEntity extends ThrowableItemProjectile {
	public GrenadeEntity(EntityType<? extends GrenadeEntity> type, Level level) {
		super(type, level);
	}

	public GrenadeEntity(Level level, LivingEntity owner, ItemStack stack) {
		super(JugcraftWeapons.GRENADE, owner, level, stack);
	}

	@Override
	protected Item getDefaultItem() {
		return PetroItems.GRENADE;
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
			Warhead.of(getItem()).detonate(server, center, this, getOwner());
			discard();
		}
	}
}
