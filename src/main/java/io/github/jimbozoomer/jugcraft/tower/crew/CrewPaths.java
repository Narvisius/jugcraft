package io.github.jimbozoomer.jugcraft.tower.crew;

import io.github.jimbozoomer.jugcraft.tower.HallFixtureBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * How walkers find their way round the mod's pieces (Justin, 6 November 2026: "I see the people like teleporting around a
 * lot ... There's this dude who's just like bouncing up and down in front of a window").
 *
 * <p>The game's path finder takes any block that isn't a whole cube as somewhere to walk unless the block says otherwise,
 * and almost none of the mod's pieces said so: a Framed Glass window (a thin pane at the back of its cell), a desk, a
 * shelf, a console, a part of a big piece. A walker's path went into them, it bumped and hopped against them (the game
 * makes a mob jump when it is inside a cell with a block in it), never got where it was going, and the crew's "stuck"
 * timer put it back at its post. Now a piece of the mod taller than a step ({@link #STEP}) is in the way for walking
 * ({@link #obstacle}, mixin/PiecePathMixin), as a whole block is; low ones (a raised floor tile, a floor emitter) are
 * still walked over, and fittings up at the top of their cells (a ceiling light) walked under.
 *
 * <p>The service bay's lifted cradle (a drone stand in its service style) reaches out over the cells round its stand:
 * the crew walk round those too ({@link Navigation}), and players and mobs bump into its corners from every side
 * (mixin/LargeShapeEdgeMixin; the game only looked at big shapes straight across).
 */
public final class CrewPaths {
	/** The tallest a piece can be and still be walked over (the game's step up for walkers); and how high in its cell a
	 * piece must start to be walked under (a ceiling light, an awning: a walker's head reaches 0.8 into the cell above). */
	public static final double STEP = 0.6, HEAD = 0.8;
	/** A walker's feet box in its cell (the crew are 0.5625 wide, 1.69 tall), to test the cradle's reach against. */
	private static final AABB FEET = new AABB(0.21, 0.05, 0.21, 0.79, 1.8, 0.79);

	private CrewPaths() {
	}

	/**
	 * True for a piece of the mod that is in a walker's way: it stands taller than a step from low in its cell (not a
	 * floor tile to step on, not a fitting up at the top of its cell to walk under). Never stairs: they're walked up.
	 */
	public static boolean obstacle(BlockState state) {
		// stairs are a way up, as the game's own are (C1, Justin, 8 November 2026: the crew "rarely go up to the second
		// floor": the hall's stairs, the mod's concrete stairs, were in everyone's way, so nobody could walk upstairs)
		if (state.getBlock() instanceof net.minecraft.world.level.block.StairBlock) {
			return false;
		}
		try {
			if (!"jugcraft".equals(state.getBlock().builtInRegistryHolder().key().identifier().getNamespace())) {
				return false;
			}
		} catch (IllegalStateException unregistered) {
			return false;
		}
		VoxelShape shape = state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
		return !shape.isEmpty() && shape.max(Direction.Axis.Y) > STEP && shape.min(Direction.Axis.Y) < HEAD;
	}

	/** True when a service cradle beside (x, y, z) reaches over a walker standing in it. */
	public static boolean underCradle(PathfindingContext context, int x, int y, int z) {
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (dx == 0 && dz == 0) {
					continue;
				}
				BlockState s = context.getBlockState(p.set(x + dx, y, z + dz));
				if (s.getBlock() instanceof HallFixtureBlock.Stand && s.hasLargeCollisionShape()) {
					VoxelShape shape = s.getCollisionShape(context.level(), p);
					if (!shape.isEmpty() && shape.bounds().move(dx, 0, dz).intersects(FEET)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * True when the block under (x, y, z) is a piece of the mod that isn't a floor (a desk, a console, a gate's doors, a
	 * window's sill): the crew don't hop up onto the furniture as a short cut. A floor is a whole block, or a piece whose
	 * top lies across the whole cell.
	 */
	static boolean onAPiece(PathfindingContext context, int x, int y, int z) {
		BlockPos below = new BlockPos(x, y - 1, z);
		BlockState s = context.getBlockState(below);
		if (s.isAir() || !obstacle(s) || s.isCollisionShapeFullBlock(context.level(), below)) {
			return false;
		}
		VoxelShape shape = s.getCollisionShape(context.level(), below);
		if (shape.isEmpty()) {
			return false;   // nothing there to stand on (a lamp, a light): its bounds would throw
		}
		double top = shape.max(Direction.Axis.Y) - 0.01;
		for (double[] at : new double[][] {{0.5, 0.5}, {0.15, 0.15}, {0.85, 0.15}, {0.15, 0.85}, {0.85, 0.85}}) {
			if (!shape.bounds().contains(at[0], top, at[1]) || shape.toAabbs().stream().noneMatch(b -> b.contains(at[0], top, at[1]))) {
				return true;
			}
		}
		return false;
	}

	/** True for a speed gate's lane (the doors open for the crew, either way: nuclear/SpeedGateBlockEntity). */
	static boolean gateLane(BlockState s) {
		return s.getBlock() instanceof io.github.jimbozoomer.jugcraft.nuclear.SpeedGateBlock
				&& s.getValue(io.github.jimbozoomer.jugcraft.nuclear.SpeedGateBlock.PART) == io.github.jimbozoomer.jugcraft.nuclear.SpeedGateBlock.Part.LANE;
	}

	/**
	 * The crew's way finding: the game's walking, keeping out from under the service cradles and off the furniture, round
	 * a landed drop ship (its hull isn't blocks, so the game's path finder didn't see it), and through the speed gates.
	 */
	public static final class Navigation extends GroundPathNavigation {
		public Navigation(Mob mob, Level level) {
			super(mob, level);
		}

		@Override
		protected PathFinder createPathFinder(int maxVisitedNodes) {
			nodeEvaluator = new Evaluator();
			return new PathFinder(nodeEvaluator, maxVisitedNodes);
		}
	}

	static final class Evaluator extends WalkNodeEvaluator {
		private final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();

		/** The path finder's node for a cell (while prepared): the walk map asks each cell's steps on (CrewRoutes). */
		net.minecraft.world.level.pathfinder.Node cell(int x, int y, int z) {
			return getNode(x, y, z);
		}

		@Override
		public PathType getPathType(PathfindingContext context, int x, int y, int z) {
			if (gateLane(context.getBlockState(at.set(x, y, z)))) {
				BlockPos below = at.below();
				return context.getBlockState(below).isFaceSturdy(context.level(), below, Direction.UP) ? PathType.WALKABLE : PathType.BLOCKED;
			}
			// the cell over a gate's lane is head room, not the top of the gate to stand on: with the gate shut the lane was
			// a way through at the feet and blocked at the head, so no walk was ever planned through shut gates, and the
			// way in from the entrance went nowhere (10 November 2026)
			if (gateLane(context.getBlockState(at.set(x, y - 1, z)))) {
				return PathType.OPEN;
			}
			PathType type = super.getPathType(context, x, y, z);
			if (type == PathType.WALKABLE || type == PathType.OPEN) {
				if (underCradle(context, x, y, z) || type == PathType.WALKABLE && onAPiece(context, x, y, z)
						|| mob != null && CrewShipHull.blocks(mob.level(), new AABB(x + 0.21, y + 0.05, z + 0.21, x + 0.79, y + 1.8, z + 0.79))) {
					return PathType.BLOCKED;
				}
			}
			return type;
		}
	}
}
