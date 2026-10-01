package io.github.jimbozoomer.jugcraft.drone;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A supply pickup plate, placed on top of the landing platform's base layer. Nine of them in a 3x3
 * (not touching any other plates) form one supply pickup: a yellow and black loading square with a
 * lift hatch in the middle. The cargo packager stands next to it; when a drone comes for a crate the
 * hatch opens, the crate rises on the lift and the drone winches it up (drawn by the client).
 * Breaking a plate unforms the pickup until the terminal sees a complete 3x3 again.
 */
public class SupplyPickupBlock extends LandingPlatformBlock {
	public static final int SIZE = PlatformLayout.PICKUP_SIZE;
	/** 0 = loose plate; 1..9 = tile of a formed 3x3 pickup, row by row from the north-west corner. */
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, SIZE * SIZE);
	/** The middle tile, with the lift hatch. */
	public static final int HATCH_PART = 1 + (SIZE / 2) * SIZE + SIZE / 2;
	/** Plate thickness in pixels (same as landing pads). */
	public static final int HEIGHT = LandingPadBlock.HEIGHT;
	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, HEIGHT, 16);

	public SupplyPickupBlock(Properties properties) {
		super(properties, PlatformLayout.Cell.PICKUP);
		registerDefaultState(stateDefinition.any().setValue(PART, 0));
	}

	/** The tile number for the plate at offset (dx, dz) from a formed pickup's north-west corner. */
	public static int part(int dx, int dz) {
		return 1 + dz * SIZE + dx;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(PART);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		int part = state.getValue(PART);
		if (part > 0 && !level.getBlockState(pos).is(this)) {
			unform(level, pos, part);
		}
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
	}

	/** Sets every plate of the formed pickup that {@code pos} (tile {@code part}) belonged to back to loose. */
	static void unform(Level level, BlockPos pos, int part) {
		int index = part - 1;
		BlockPos corner = pos.offset(-(index % SIZE), 0, -(index / SIZE));
		for (int dx = 0; dx < SIZE; dx++) {
			for (int dz = 0; dz < SIZE; dz++) {
				BlockPos at = corner.offset(dx, 0, dz);
				BlockState plate = level.getBlockState(at);
				if (plate.getBlock() instanceof SupplyPickupBlock && plate.getValue(PART) == part(dx, dz)) {
					level.setBlock(at, plate.setValue(PART, 0), Block.UPDATE_CLIENTS);
				}
			}
		}
	}
}
