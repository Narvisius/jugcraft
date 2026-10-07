package io.github.jimbozoomer.jugcraft.diagonal;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.mixin.WallBlockInvoker;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Walls that join diagonally (docs/features/diagonal-connections.md). Vanilla's walls keep their own states; each has a
 * {@link DiagonalWallBlock}, {@code jugcraft:diagonal_<wall>}, that it becomes while it joins a wall diagonally and turns
 * back from when its last diagonal goes ({@link #settle}, called wherever vanilla works out a wall's state:
 * {@code mixin/DiagonalWallMixin}).
 */
public final class DiagonalWalls {
	private static final List<Direction> HORIZONTAL = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
	// Masks of a straight run along a diagonal (bits in Diagonal order).
	private static final int RUN_NORTH_EAST = 0b0101;
	private static final int RUN_NORTH_WEST = 0b1010;
	private static final Map<Block, DiagonalWallBlock> BY_WALL = new IdentityHashMap<>();

	private DiagonalWalls() {
	}

	/**
	 * Registers a diagonal wall for each of vanilla's walls ({@code jugcraft:diagonal_cobblestone_wall} and so on) and
	 * for each of Jugcraft's own (the bastion concrete wall, batch 55), which must be registered before this runs.
	 */
	public static void register() {
		List<WallBlock> walls = new ArrayList<>();
		for (Block block : BuiltInRegistries.BLOCK) {
			String namespace = BuiltInRegistries.BLOCK.getKey(block).getNamespace();
			if (block instanceof WallBlock wall && (namespace.equals("minecraft") || namespace.equals(Jugcraft.MOD_ID))) {
				walls.add(wall);
			}
		}
		for (WallBlock wall : walls) {
			ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Jugcraft.id("diagonal_" + BuiltInRegistries.BLOCK.getKey(wall).getPath()));
			BlockBehaviour.Properties properties = BlockBehaviour.Properties.ofFullCopy(wall).overrideLootTable(wall.getLootTable())
					.overrideDescription(wall.getDescriptionId()).setId(key);
			BY_WALL.put(wall, Registry.register(BuiltInRegistries.BLOCK, key, new DiagonalWallBlock(wall, properties)));
		}
	}

	/** Every diagonal wall. */
	public static Collection<DiagonalWallBlock> all() {
		return Collections.unmodifiableCollection(BY_WALL.values());
	}

	/** The diagonal wall a vanilla wall becomes, or null. */
	public static DiagonalWallBlock of(Block wall) {
		return BY_WALL.get(wall);
	}

	/** Whether the block is a wall, vanilla's or a diagonal one. */
	public static boolean isWall(BlockState state) {
		return state.getBlock() instanceof WallBlock || state.getBlock() instanceof DiagonalWallBlock;
	}

	/**
	 * A vanilla wall's state, as vanilla worked it out, or the diagonal wall it becomes if it joins a wall diagonally: its
	 * sides joined where vanilla's are, its diagonals, tall if the block above covers them all ({@link #tall}), and its
	 * post by vanilla's rule ({@link #post}).
	 */
	public static BlockState settle(BlockState wallState, BlockGetter level, BlockPos pos) {
		DiagonalWallBlock diagonal = BY_WALL.get(wallState.getBlock());
		if (diagonal == null || !wallState.is(DiagonalConnections.TAG) || !JugcraftConfig.isFeatureEnabled(DiagonalConnections.FEATURE)) {
			return wallState;
		}
		int mask = DiagonalConnections.diagonals(wallState, level, pos);
		if (mask == 0) {
			return wallState;
		}
		BlockState state = diagonal.defaultBlockState().setValue(DiagonalWallBlock.WATERLOGGED, wallState.getValue(WallBlock.WATERLOGGED));
		for (Direction side : HORIZONTAL) {
			state = state.setValue(DiagonalWallBlock.SIDES.get(side), wallState.getValue(WallBlock.PROPERTY_BY_DIRECTION.get(side)) != WallSide.NONE);
		}
		for (DiagonalConnections.Diagonal each : DiagonalConnections.Diagonal.ALL) {
			state = state.setValue(each.property, (mask & 1 << each.ordinal()) != 0);
		}
		BlockPos abovePos = pos.above();
		BlockState above = level.getBlockState(abovePos);
		VoxelShape aboveFace = above.getCollisionShape(level, abovePos).getFaceShape(Direction.DOWN);
		boolean tall = tall(wallState, mask, aboveFace);
		return state.setValue(DiagonalWallBlock.TALL, tall)
				.setValue(DiagonalWallBlock.UP, post((WallBlock) wallState.getBlock(), wallState, mask, tall, above, aboveFace));
	}

	/**
	 * Whether every joined side and diagonal arm is covered by the block above, as vanilla raises a wall's side to the
	 * tall height: vanilla has already found each straight side tall or low, and each diagonal arm is tested the same
	 * way, with a thin line from the middle to the corner that the face of the block above must cover. The diagonal wall
	 * has one height for all its arms, so one uncovered arm keeps them all low rather than raise an arm into the air.
	 */
	private static boolean tall(BlockState wallState, int mask, VoxelShape aboveFace) {
		for (Direction side : HORIZONTAL) {
			if (wallState.getValue(WallBlock.PROPERTY_BY_DIRECTION.get(side)) == WallSide.LOW) {
				return false;
			}
		}
		for (DiagonalConnections.Diagonal each : DiagonalConnections.Diagonal.ALL) {
			if ((mask & 1 << each.ordinal()) != 0
					&& Shapes.joinIsNotEmpty(DiagonalConnections.Arms.of(1 / 16.0, 0, 1, 1 << each.ordinal()), aboveFace, BooleanOp.ONLY_FIRST)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * The vanilla wall a diagonal wall stands for: its joined sides tall if it is tall, else low. Vanilla works out each
	 * side's height from the block above again whenever it updates the wall's sides.
	 */
	public static BlockState toWall(BlockState state) {
		DiagonalWallBlock diagonal = (DiagonalWallBlock) state.getBlock();
		BlockState wall = diagonal.wall().defaultBlockState().setValue(WallBlock.UP, state.getValue(DiagonalWallBlock.UP))
				.setValue(WallBlock.WATERLOGGED, state.getValue(DiagonalWallBlock.WATERLOGGED));
		WallSide joined = state.getValue(DiagonalWallBlock.TALL) ? WallSide.TALL : WallSide.LOW;
		for (Direction side : HORIZONTAL) {
			wall = wall.setValue(WallBlock.PROPERTY_BY_DIRECTION.get(side), state.getValue(DiagonalWallBlock.SIDES.get(side)) ? joined : WallSide.NONE);
		}
		return wall;
	}

	/**
	 * A diagonal wall's post, raised by vanilla's own rule with the diagonals counted. A wall with no straight sides and
	 * one pair of opposite diagonals runs straight on, so the rule sees it as a straight wall, low or tall as the diagonal
	 * wall is: a low run has no post unless something above calls for one (a torch, a wall's post, a block over the
	 * middle), and a tall run has none, as vanilla's tall straight walls have none. Any other wall with diagonals is an
	 * end, a corner or a junction, and the rule gives it its post.
	 */
	private static boolean post(WallBlock wall, BlockState wallState, int mask, boolean tall, BlockState above, VoxelShape aboveFace) {
		BlockState seen = wallState;
		boolean noSides = HORIZONTAL.stream().allMatch(side -> wallState.getValue(WallBlock.PROPERTY_BY_DIRECTION.get(side)) == WallSide.NONE);
		if ((mask == RUN_NORTH_EAST || mask == RUN_NORTH_WEST) && noSides) {
			WallSide height = tall ? WallSide.TALL : WallSide.LOW;
			seen = wallState.setValue(WallBlock.NORTH, height).setValue(WallBlock.SOUTH, height);
		}
		return ((WallBlockInvoker) wall).jugcraft$shouldRaisePost(seen, above, aboveFace);
	}
}
