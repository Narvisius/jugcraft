package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A Bowling Pumpkin rolling along the ground. It rolls on from its push, slowing by {@value #FRICTION} a tick and
 * falling where the ground falls away; it knocks down every Skeleton Pin it rolls into (on the server), slowing a little
 * each time, and a pin it knocks may knock down the pins behind it, one time in {@value #DOMINO_CHANCE}. When it stops,
 * hits a wall or has rolled {@value #MAX_TICKS} ticks, it scores the roll on its Bowling Scoreboard (if it was rolled
 * near one) and comes to rest as the item again.
 */
public class BowlingPumpkin extends ThrowableItemProjectile {
	public static final double SPEED = 0.55;
	public static final double FRICTION = 0.985;
	public static final int DOMINO_CHANCE = 2;
	public static final int MAX_TICKS = 300;
	private static final double STOP_SPEED = 0.04;
	private static final double GRAVITY = 0.06;
	private static final double PIN_SLOWING = 0.9;

	/** The pumpkin's radius, in blocks. */
	public static final float RADIUS = 0.25F;

	private @Nullable BlockPos scoreboard;
	/** On the client, for drawing: how far it has turned over as it rolls (radians), the tick before and now, and the way it heads. */
	public float spinO;
	public float spin;
	public float heading;

	public BowlingPumpkin(EntityType<? extends BowlingPumpkin> type, Level level) {
		super(type, level);
	}

	public BowlingPumpkin(ServerLevel level, Vec3 start, Vec3 direction, ItemStack pumpkin, @Nullable BlockPos scoreboard) {
		super(JugcraftAgriculture.BOWLING_PUMPKIN, start.x, start.y, start.z, level, pumpkin.copyWithCount(1));
		this.scoreboard = scoreboard;
		setDeltaMovement(direction.scale(SPEED));
	}

	@Override
	protected Item getDefaultItem() {
		return JugcraftAgriculture.item("bowling_pumpkin");
	}

	/** Rolls instead of flying: friction along the ground, gravity off it, no hitting things in the air. */
	@Override
	public void tick() {
		baseTick();
		Vec3 motion = getDeltaMovement();
		motion = new Vec3(motion.x * FRICTION, onGround() ? -0.01 : motion.y - GRAVITY, motion.z * FRICTION);
		setDeltaMovement(motion);
		move(MoverType.SELF, motion);
		if (level() instanceof ServerLevel level) {
			roll(level);
		} else {
			double dx = getX() - xo;
			double dz = getZ() - zo;
			double moved = Math.sqrt(dx * dx + dz * dz);
			spinO = spin;
			spin += (float) (moved / RADIUS);
			if (moved > 1.0E-3) {
				heading = (float) Math.atan2(dx, dz);
			}
		}
	}

	private void roll(ServerLevel level) {
		Vec3 motion = getDeltaMovement();
		double speed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
		if (speed > 1.0E-6) {
			Direction toward = toward(motion.x, motion.z);
			Vec3 ahead = position().add(motion.x / speed * 0.4, 0.2, motion.z / speed * 0.4);
			for (BlockPos pos : new BlockPos[] {BlockPos.containing(getX(), getY() + 0.2, getZ()), BlockPos.containing(ahead)}) {
				if (SkeletonPinBlock.knock(level, pos, toward)) {
					setDeltaMovement(motion.multiply(PIN_SLOWING, 1.0, PIN_SLOWING));
					domino(level, pos, toward, 2);
				}
			}
		}
		if (horizontalCollision || speed < STOP_SPEED || tickCount > MAX_TICKS || getY() < level.getMinY()) {
			finish(level);
		}
	}

	/** A falling pin may knock down the pins behind it and to either side behind. */
	private void domino(ServerLevel level, BlockPos pos, Direction toward, int depth) {
		if (depth <= 0) {
			return;
		}
		BlockPos behind = pos.relative(toward);
		for (BlockPos next : new BlockPos[] {behind, behind.relative(toward.getClockWise()), behind.relative(toward.getCounterClockWise())}) {
			if (level.getRandom().nextInt(DOMINO_CHANCE) == 0 && SkeletonPinBlock.knock(level, next, toward)) {
				domino(level, next, toward, depth - 1);
			}
		}
	}

	/** The way a pin falls when pushed along (dx, dz). */
	public static Direction toward(double dx, double dz) {
		if (Math.abs(dx) > Math.abs(dz)) {
			return dx > 0 ? Direction.EAST : Direction.WEST;
		}
		return dz > 0 ? Direction.SOUTH : Direction.NORTH;
	}

	/** Comes to rest: scores the roll and leaves the pumpkin to be picked up. */
	private void finish(ServerLevel level) {
		if (scoreboard != null && level.getBlockEntity(scoreboard) instanceof BowlingScoreboardBlockEntity board) {
			board.rolled(level);
		}
		spawnAtLocation(level, getItem().copyWithCount(1));
		discard();
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (scoreboard != null) {
			output.store("scoreboard", BlockPos.CODEC, scoreboard);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		scoreboard = input.read("scoreboard", BlockPos.CODEC).orElse(null);
	}
}
