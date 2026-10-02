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
 * A landing pad plate, placed on top of the landing platform's base layer. Loose plates show their
 * own texture ({@code part = 0}). When a drone terminal finds a complete 5x5 of plates on the base, it
 * forms them into one pad: each plate gets its tile number (1 to 25, row by row) and together they
 * show one black pad with the charger port in the middle, where docked drones recharge.
 * Breaking a plate of a formed pad unforms the rest until the terminal sees a complete 5x5 again.
 */
public class LandingPadBlock extends LandingPlatformBlock {
	/** 0 = loose plate; 1..25 = tile of a formed 5x5 pad, row by row from the north-west corner. */
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, PlatformLayout.PAD_SIZE * PlatformLayout.PAD_SIZE);
	/** The middle tile, with the charger port. */
	public static final int CHARGER_PART = 1 + (PlatformLayout.PAD_SIZE / 2) * PlatformLayout.PAD_SIZE + PlatformLayout.PAD_SIZE / 2;
	/** Plate thickness in pixels. */
	public static final int HEIGHT = 3;
	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, HEIGHT, 16);

	public LandingPadBlock(Properties properties) {
		super(properties, PlatformLayout.Cell.PAD);
		registerDefaultState(stateDefinition.any().setValue(PART, 0));
	}

	/** The tile number for the plate at offset (dx, dz) from a formed pad's north-west corner. */
	public static int part(int dx, int dz) {
		return 1 + dz * PlatformLayout.PAD_SIZE + dx;
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

	/** Sets every plate of the formed pad that {@code pos} (tile {@code part}) belonged to back to loose. */
	static void unform(Level level, BlockPos pos, int part) {
		int index = part - 1;
		BlockPos corner = pos.offset(-(index % PlatformLayout.PAD_SIZE), 0, -(index / PlatformLayout.PAD_SIZE));
		for (int dx = 0; dx < PlatformLayout.PAD_SIZE; dx++) {
			for (int dz = 0; dz < PlatformLayout.PAD_SIZE; dz++) {
				BlockPos at = corner.offset(dx, 0, dz);
				BlockState plate = level.getBlockState(at);
				if (plate.getBlock() instanceof LandingPadBlock && plate.getValue(PART) == part(dx, dz)) {
					level.setBlock(at, plate.setValue(PART, 0), Block.UPDATE_CLIENTS);
				}
			}
		}
	}
}
