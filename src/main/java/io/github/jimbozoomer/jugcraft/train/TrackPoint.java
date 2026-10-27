package io.github.jimbozoomer.jugcraft.train;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A place on a rail line (docs/features/trains.md): a rail block, the way along it (from one of its ends to the other)
 * and how far, {@code t} from 0 to 1. Any rail counts (rails, powered, detector and activator rails, and other mods'
 * rails), with its shape as it is now: curves, slopes and junctions are followed as Minecraft lays them. A rail's path
 * runs straight from the middle of the edge at one end to the middle of the edge at the other, as a minecart's does,
 * at the rail's height; a slope's high end is a block up.
 */
public record TrackPoint(BlockPos rail, Direction from, Direction to, double t) {
	public static final Codec<TrackPoint> CODEC = RecordCodecBuilder.create(i -> i.group(
			BlockPos.CODEC.fieldOf("rail").forGetter(TrackPoint::rail),
			Direction.CODEC.fieldOf("from").forGetter(TrackPoint::from),
			Direction.CODEC.fieldOf("to").forGetter(TrackPoint::to),
			Codec.DOUBLE.fieldOf("t").forGetter(TrackPoint::t)).apply(i, TrackPoint::new));
	/** How far above its block a rail's top is: where the wheels run. */
	public static final double RAIL_TOP = 1.0 / 16.0;

	/** A rail's two ends: which way each leads, and how high it is above the rail's block (0, or 1 for a slope's top). */
	public record Ends(Direction a, Direction b, int riseA, int riseB) {
		public boolean has(Direction side) {
			return side == a || side == b;
		}

		public Direction other(Direction side) {
			return side == a ? b : a;
		}

		public int rise(Direction side) {
			return side == a ? riseA : riseB;
		}
	}

	/** The result of a move: where it got to, how far it went, and whether the line ended (or broke) before the end. */
	public record Move(TrackPoint point, double moved, boolean blocked) {
	}

	/** The rail's shape, from its shape property (whatever the rail block), or null if the block is not a rail. */
	@SuppressWarnings("unchecked")
	public static @Nullable RailShape shape(BlockState state) {
		for (Property<?> property : state.getProperties()) {
			if (property.getValueClass() == RailShape.class) {
				return state.getValue((Property<RailShape>) property);
			}
		}
		return null;
	}

	/** The ends of the rail in {@code state}, or null if it is not a rail. */
	public static @Nullable Ends ends(BlockState state) {
		RailShape shape = shape(state);
		if (shape == null) {
			return null;
		}
		return switch (shape) {
			case NORTH_SOUTH -> new Ends(Direction.NORTH, Direction.SOUTH, 0, 0);
			case EAST_WEST -> new Ends(Direction.EAST, Direction.WEST, 0, 0);
			case ASCENDING_EAST -> new Ends(Direction.WEST, Direction.EAST, 0, 1);
			case ASCENDING_WEST -> new Ends(Direction.EAST, Direction.WEST, 0, 1);
			case ASCENDING_NORTH -> new Ends(Direction.SOUTH, Direction.NORTH, 0, 1);
			case ASCENDING_SOUTH -> new Ends(Direction.NORTH, Direction.SOUTH, 0, 1);
			case SOUTH_EAST -> new Ends(Direction.SOUTH, Direction.EAST, 0, 0);
			case SOUTH_WEST -> new Ends(Direction.SOUTH, Direction.WEST, 0, 0);
			case NORTH_WEST -> new Ends(Direction.NORTH, Direction.WEST, 0, 0);
			case NORTH_EAST -> new Ends(Direction.NORTH, Direction.EAST, 0, 0);
		};
	}

	/** The middle of the rail's edge on {@code side}, at the rail's height there. */
	public static Vec3 end(BlockPos rail, Ends ends, Direction side) {
		return new Vec3(rail.getX() + 0.5 + side.getStepX() * 0.5, rail.getY() + RAIL_TOP + ends.rise(side), rail.getZ() + 0.5 + side.getStepZ() * 0.5);
	}

	/** Where this point is, or the rail's middle if the rail is gone. */
	public Vec3 position(BlockGetter level) {
		Ends ends = ends(level.getBlockState(rail));
		if (ends == null || !ends.has(from) || !ends.has(to)) {
			return Vec3.atBottomCenterOf(rail).add(0, RAIL_TOP, 0);
		}
		return end(rail, ends, from).lerp(end(rail, ends, to), t);
	}

	/** Whether the rail is still there, still running between this point's ends. */
	public boolean valid(BlockGetter level) {
		Ends ends = ends(level.getBlockState(rail));
		return ends != null && ends.has(from) && ends.has(to) && from != to;
	}

	/** The same place, facing the other way. */
	public TrackPoint reversed() {
		return new TrackPoint(rail, to, from, 1.0 - t);
	}

	/** The way the line runs here, a unit vector (forwards). */
	public Vec3 heading(BlockGetter level) {
		Ends ends = ends(level.getBlockState(rail));
		if (ends == null || !ends.has(from) || !ends.has(to)) {
			return new Vec3(to.getStepX(), 0, to.getStepZ());
		}
		return end(rail, ends, to).subtract(end(rail, ends, from)).normalize();
	}

	/**
	 * Moves {@code distance} blocks along the line: forwards if positive, backwards if negative. It stops at the end of
	 * the line (no next rail, or one that does not join), and says so.
	 */
	public Move move(BlockGetter level, double distance) {
		if (distance < 0) {
			Move back = reversed().move(level, -distance);
			return new Move(back.point().reversed(), -back.moved(), back.blocked());
		}
		double left = distance;
		TrackPoint at = this;
		for (int guard = 0; guard < 256; guard++) {
			Ends ends = ends(level.getBlockState(at.rail));
			if (ends == null || !ends.has(at.from) || !ends.has(at.to)) {
				return new Move(at, distance - left, true);
			}
			double length = end(at.rail, ends, at.from).distanceTo(end(at.rail, ends, at.to));
			double room = (1.0 - at.t) * length;
			if (left <= room) {
				return new Move(new TrackPoint(at.rail, at.from, at.to, at.t + left / length), distance, false);
			}
			left -= room;
			TrackPoint next = next(level, at.rail, at.to, ends.rise(at.to));
			if (next == null) {
				return new Move(new TrackPoint(at.rail, at.from, at.to, 1.0), distance - left, true);
			}
			at = next;
		}
		return new Move(at, distance - left, true);
	}

	/**
	 * The rail the line runs on to past {@code rail}'s {@code exit} end ({@code rise} high), entered from that side: level
	 * with the exit (a flat join, or a slope starting up), or a block below it (a slope running down from there).
	 */
	static @Nullable TrackPoint next(BlockGetter level, BlockPos rail, Direction exit, int rise) {
		BlockPos ahead = rail.relative(exit).above(rise);
		Direction back = exit.getOpposite();
		for (BlockPos candidate : List.of(ahead, ahead.below())) {
			Ends ends = ends(level.getBlockState(candidate));
			if (ends != null && ends.has(back) && ends.rise(back) == ahead.getY() - candidate.getY()) {
				return new TrackPoint(candidate, back, ends.other(back), 0.0);
			}
		}
		return null;
	}

	/**
	 * A point on the rail at {@code rail}, halfway along, facing whichever of its ends lies nearest {@code look} (a
	 * horizontal direction); null if there is no rail there.
	 */
	public static @Nullable TrackPoint onRail(BlockGetter level, BlockPos rail, Vec3 look) {
		Ends ends = ends(level.getBlockState(rail));
		if (ends == null) {
			return null;
		}
		double a = look.x * ends.a().getStepX() + look.z * ends.a().getStepZ();
		double b = look.x * ends.b().getStepX() + look.z * ends.b().getStepZ();
		return a >= b ? new TrackPoint(rail, ends.b(), ends.a(), 0.5) : new TrackPoint(rail, ends.a(), ends.b(), 0.5);
	}
}
