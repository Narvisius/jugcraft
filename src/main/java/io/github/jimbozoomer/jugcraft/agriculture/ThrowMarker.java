package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A little flag where a trebuchet's pumpkin came down, its distance shown above it, for {@link #LIFETIME} ticks.
 * It is never saved, takes no part in collisions and cannot be hurt.
 */
public class ThrowMarker extends Entity {
	public static final int LIFETIME = 600;

	public ThrowMarker(EntityType<? extends ThrowMarker> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	@Override
	public void tick() {
		super.tick();
		if (!level().isClientSide() && tickCount > LIFETIME) {
			discard();
		}
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}
}
