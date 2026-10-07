package io.github.jimbozoomer.jugcraft.artillery;

import io.github.jimbozoomer.jugcraft.SmoothFlight;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LinearInterpolationHandler;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * The Observation Balloon (batch 51): a kite balloon tethered where it was placed. While someone rides in its basket
 * it winches up {@value JugcraftArtillery#BALLOON_CLIMB} blocks a tick to {@value JugcraftArtillery#BALLOON_HEIGHT}
 * blocks above its anchor (less if something is in the way); empty, it winches back down. It never drifts sideways, so
 * a spotter with a Range Finder has a steady view for marking targets far away.
 */
public class ObservationBalloon extends Entity {
	private static final net.minecraft.network.syncher.EntityDataAccessor<Float> ANCHOR = net.minecraft.network.syncher.SynchedEntityData
			.defineId(ObservationBalloon.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);
	private double anchorY = Double.NaN;
	private float damage;

	public ObservationBalloon(EntityType<? extends ObservationBalloon> type, Level level) {
		super(type, level);
	}

	public double anchorY() {
		return Double.isNaN(anchorY) ? getY() : anchorY;
	}

	/** Client side: the anchor's height, for drawing the winch cable. */
	public float syncedAnchorY() {
		float anchor = entityData.get(ANCHOR);
		return Float.isNaN(anchor) ? (float) getY() : anchor;
	}

	public InteractionResult use(Player player, InteractionHand hand) {
		if (player.isPassenger() || isVehicle()) {
			return InteractionResult.PASS;
		}
		if (level().isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		return player.startRiding(this) ? InteractionResult.SUCCESS : InteractionResult.PASS;
	}

	/** The client eases to each height the server sends instead of jumping to it (see {@link SmoothFlight}). */
	@Override
	public LinearInterpolationHandler createInterpolationHandler() {
		return SmoothFlight.handler(this);
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel)) {
			SmoothFlight.step(this);
			return;
		}
		if (Double.isNaN(anchorY)) {
			anchorY = getY();
		}
		if (entityData.get(ANCHOR) != (float) anchorY) {
			entityData.set(ANCHOR, (float) anchorY);
		}
		double wanted = isVehicle() ? anchorY + JugcraftArtillery.BALLOON_HEIGHT : anchorY;
		double vy = Mth.clamp(wanted - getY(), -JugcraftArtillery.BALLOON_CLIMB, JugcraftArtillery.BALLOON_CLIMB);
		setDeltaMovement(0, vy, 0);
		move(MoverType.SELF, getDeltaMovement());
		resetFallDistance();
		for (Entity passenger : getPassengers()) {
			passenger.resetFallDistance();
		}
		if (damage > 0 && tickCount % 10 == 0) {
			damage = Math.max(0, damage - 1);
		}
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return getPassengers().isEmpty();
	}

	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
		return new Vec3(0, 0.2, 0);
	}

	@Override
	public boolean isPickable() {
		return !isRemoved();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (isRemoved() || !(source.getEntity() instanceof Player player) || player.getRootVehicle() == this) {
			return false;
		}
		if (player.getAbilities().instabuild) {
			discard();
			return true;
		}
		damage += amount;
		if (damage >= JugcraftArtillery.BALLOON_HEALTH) {
			spawnAtLocation(level, new ItemStack(JugcraftArtillery.BALLOON_ITEM));
			discard();
		}
		return true;
	}

	@Override
	protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
		builder.define(ANCHOR, Float.NaN);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		float saved = input.getFloatOr("anchor_y", Float.NaN);
		anchorY = Float.isNaN(saved) ? Double.NaN : saved;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putFloat("anchor_y", (float) anchorY());
	}
}
