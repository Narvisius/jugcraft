package io.github.jimbozoomer.jugcraft.raiders;

import io.github.jimbozoomer.jugcraft.chemistry.PetroItems;
import io.github.jimbozoomer.jugcraft.weapons.Blast;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A raider's grenade (thrown by a grenadier or launched by a walker) or, heavy, a blimp's bomb. It bursts on whatever
 * it hits as a damage-only {@link Blast} (never breaking a block) that spares raiders. It passes through raiders
 * rather than bursting on them.
 */
public class RaiderBomb extends ThrowableItemProjectile {
	private boolean heavy;

	public RaiderBomb(EntityType<? extends RaiderBomb> type, Level level) {
		super(type, level);
	}

	public RaiderBomb(Level level, LivingEntity owner, boolean heavy) {
		super(JugcraftRaiders.BOMB, owner, level, new ItemStack(PetroItems.GRENADE));
		this.heavy = heavy;
	}

	public boolean heavy() {
		return heavy;
	}

	@Override
	protected Item getDefaultItem() {
		return PetroItems.GRENADE;
	}

	@Override
	protected boolean canHitEntity(net.minecraft.world.entity.Entity entity) {
		return !(entity instanceof Raider) && super.canHitEntity(entity);
	}

	@Override
	protected void onHit(HitResult hit) {
		super.onHit(hit);
		if (!(level() instanceof ServerLevel server) || isRemoved()) {
			return;
		}
		Vec3 center = hit.getLocation();
		if (hit instanceof BlockHitResult block) {
			// Just off the face it hit, so the block itself does not shield the blast.
			Direction face = block.getDirection();
			center = center.add(face.getStepX() * 0.3, face.getStepY() * 0.3, face.getStepZ() * 0.3);
		} else if (hit instanceof EntityHitResult entity) {
			center = entity.getEntity().position().add(0, entity.getEntity().getBbHeight() / 2, 0);
		}
		Blast.detonate(server, center, this, getOwner(), heavy ? JugcraftRaiders.BOMB_RADIUS : JugcraftRaiders.GRENADE_RADIUS,
				heavy ? JugcraftRaiders.BOMB_DAMAGE : JugcraftRaiders.GRENADE_DAMAGE, target -> target instanceof Raider);
		discard();
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("heavy", heavy);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		heavy = input.getBooleanOr("heavy", false);
	}
}
