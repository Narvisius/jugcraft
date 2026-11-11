package io.github.jimbozoomer.jugcraft.tower;

import org.jspecify.annotations.Nullable;

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
	/** An Operator Chair's seat top (tower/crew/TowerCrew sits there as a player would). */
	public static final double OPERATOR_SEAT_HEIGHT = SEAT_HEIGHT;

	/** A drop ship's seat (tower/crew/CrewShip.sit): the ship it's in (its id; -1 not), and where its sitter stands up
	 * (in front of it, on the bay's floor). Not saved: a seat loaded with the world goes on its first tick. */
	private int ship = -1;
	private @Nullable Vec3 standUp;

	public SeatEntity(EntityType<? extends SeatEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
	}

	/** Seats {@code player} in a drop ship's seat at {@code at} facing {@code yaw}, standing up at {@code standUp} (the
	 * ship {@code ship}: its seats are only for while it's down, CrewShip#seatsOpen). False if it couldn't. */
	public static boolean sitInShip(Level level, Player player, Vec3 at, float yaw, Vec3 standUp, int ship) {
		if (player.isPassenger() || !level.getEntitiesOfClass(SeatEntity.class, net.minecraft.world.phys.AABB.ofSize(at, 0.6, 0.6, 0.6)).isEmpty()) {
			return false;
		}
		SeatEntity seat = new SeatEntity(JugcraftTower.SEAT, level);
		seat.ship = ship;
		seat.standUp = standUp;
		seat.snapTo(at.x, at.y, at.z, yaw, 0);
		level.addFreshEntity(seat);
		player.setYRot(yaw);
		player.setYHeadRot(yaw);
		if (!player.startRiding(seat, true, true)) {
			seat.discard();
			return false;
		}
		return true;
	}

	/** Seats {@code player} on the chair at {@code pos}, facing the way the chair faces. False if it is taken. */
	public static boolean sit(Level level, BlockPos pos, Player player, Direction facing) {
		return sit(level, pos, player, facing, SEAT_HEIGHT);
	}

	/** As {@link #sit(Level, BlockPos, Player, Direction)}, for a seat whose top is {@code height} up the block. */
	public static boolean sit(Level level, BlockPos pos, Player player, Direction facing, double height) {
		return sit(level, pos, player, new Vec3(pos.getX() + 0.5, pos.getY() + height, pos.getZ() + 0.5), facing.toYRot());
	}

	/** Preserve authored 16-way furniture headings instead of snapping the rider to cardinal directions. */
	public static boolean sit(Level level, BlockPos pos, Player player, float yaw, double height) {
		return sit(level, pos, player, new Vec3(pos.getX() + 0.5, pos.getY() + height, pos.getZ() + 0.5), yaw);
	}

	/** Seats {@code player} on the chair at {@code pos} with the seat at {@code at} (inside that block), facing {@code yaw}. */
	public static boolean sit(Level level, BlockPos pos, Player player, Vec3 at, float yaw) {
		if (local.peepo.CompanionSeats.isReserved(level, pos)) {
			return false;
		}
		if (player.isPassenger() || player.isShiftKeyDown()) {
			return false;
		}
		if (!level.getEntitiesOfClass(SeatEntity.class, new net.minecraft.world.phys.AABB(pos)).isEmpty()) {
			return false;
		}
		SeatEntity seat = new SeatEntity(JugcraftTower.SEAT, level);
		seat.snapTo(at.x, at.y, at.z, yaw, 0);
		level.addFreshEntity(seat);
		player.setYRot(yaw);
		player.setYHeadRot(yaw);
		boolean mounted = player.startRiding(seat, true, true);
		if (!mounted) {
			seat.discard();
		}
		return mounted;
	}

	/**
	 * Seats a crew member (tower/crew/TowerCrew) on the chair at {@code pos} as {@link #sit(Level, BlockPos, Player, Vec3, float)}
	 * seats a player: the seat at {@code at}, facing {@code yaw}. False if the chair is taken or kept for a companion.
	 */
	public static boolean seat(Level level, BlockPos pos, net.minecraft.world.entity.Entity rider, Vec3 at, float yaw) {
		if (local.peepo.CompanionSeats.isReserved(level, pos) || rider.isPassenger()) {
			return false;
		}
		// taken: a seat on the chair's cell, or at the point (a bunk's sleeper lies across its two cells)
		if (!level.getEntitiesOfClass(SeatEntity.class, new net.minecraft.world.phys.AABB(pos)).isEmpty()
				|| !level.getEntitiesOfClass(SeatEntity.class, net.minecraft.world.phys.AABB.ofSize(at, 0.6, 0.6, 0.6)).isEmpty()) {
			return false;
		}
		SeatEntity seat = new SeatEntity(JugcraftTower.SEAT, level);
		seat.snapTo(at.x, at.y, at.z, yaw, 0);
		level.addFreshEntity(seat);
		rider.setYRot(yaw);
		rider.setYHeadRot(yaw);
		if (!rider.startRiding(seat, true, true)) {
			seat.discard();
			return false;
		}
		return true;
	}

	@Override
	public void tick() {
		super.tick();
		// (a crew member's seat on a room kit chair or a Pilot Simulator stays while they sit: tower/crew/TowerCrew minds it)
		if (level() instanceof ServerLevel && ship >= 0) {      // a drop ship's: while it's down (everyone up before it lifts)
			if (getPassengers().isEmpty() || !(level().getEntity(ship) instanceof io.github.jimbozoomer.jugcraft.tower.crew.CrewShip s)
					|| s.isRemoved() || !s.seatsOpen()) {
				ejectPassengers();
				discard();
			}
			return;
		}
		if (level() instanceof ServerLevel && (getPassengers().isEmpty() || !isChair(level().getBlockState(blockPosition()))
				&& !(getFirstPassenger() instanceof io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew))) {
			ejectPassengers();
			discard();
		}
	}

	private static boolean isChair(BlockState state) {
		return state.is(JugcraftTower.BLOCKS.get("operator_chair")) || state.getBlock() instanceof Sittable;
	}

	@Override
	protected void removePassenger(Entity passenger) {
		super.removePassenger(passenger);
		if (standUp != null) {                           // a drop ship's: up on the bay's floor in front of the seat
			passenger.setPos(standUp.x, standUp.y, standUp.z);
			return;
		}
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
