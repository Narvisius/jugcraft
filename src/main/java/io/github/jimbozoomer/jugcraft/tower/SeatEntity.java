package io.github.jimbozoomer.jugcraft.tower;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * An invisible seat on an Operator Chair: right-click the chair to sit, sneak to stand up. The seat goes away
 * as soon as nobody sits on it or the chair is broken (a seat saved with the world goes on its first tick; it
 * must be saveable, or the game will not let anyone ride it).
 */
public class SeatEntity extends Entity {
	/** The seat cushion's top, above the chair block's bottom. */
	private static final double SEAT_HEIGHT = 0.56;

	public SeatEntity(EntityType<? extends SeatEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
	}

	/** Seats {@code player} on the chair at {@code pos}, facing the way the chair faces. False if it is taken. */
	public static boolean sit(Level level, BlockPos pos, Player player, Direction facing) {
		if (player.isPassenger() || player.isShiftKeyDown()) {
			return false;
		}
		Vec3 at = new Vec3(pos.getX() + 0.5, pos.getY() + SEAT_HEIGHT, pos.getZ() + 0.5);
		if (!level.getEntitiesOfClass(SeatEntity.class, new net.minecraft.world.phys.AABB(pos)).isEmpty()) {
			return false;
		}
		SeatEntity seat = new SeatEntity(JugcraftTower.SEAT, level);
		float yaw = facing.toYRot();
		seat.snapTo(at.x, at.y, at.z, yaw, 0);
		level.addFreshEntity(seat);
		player.setYRot(yaw);
		player.setYHeadRot(yaw);
		return player.startRiding(seat, true, true);
	}

	@Override
	public void tick() {
		super.tick();
		if (level() instanceof ServerLevel && (getPassengers().isEmpty() || !isChair(level().getBlockState(blockPosition())))) {
			ejectPassengers();
			discard();
		}
	}

	private static boolean isChair(BlockState state) {
		return state.is(JugcraftTower.BLOCKS.get("operator_chair"));
	}

	@Override
	protected void removePassenger(Entity passenger) {
		super.removePassenger(passenger);
		// Stand up on top of the chair rather than inside it.
		passenger.setPos(passenger.getX(), Math.floor(getY()) + 1.0, passenger.getZ());
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}
}
