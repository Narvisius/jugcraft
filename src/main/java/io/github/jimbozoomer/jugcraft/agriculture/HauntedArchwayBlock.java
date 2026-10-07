package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Haunted Archway: a gateway {@value #WIDTH} blocks wide and {@value #HEIGHT} tall for the front of a yard or a
 * graveyard. Two mossy stone pillars, each with an iron lantern hung on its front (light {@value #LIGHT}), carry a
 * wrought-iron arch with a skull and crossbones at its crown and cobwebs in its corners; you walk through the middle.
 * Placed from the block aimed at (its left pillar, as the player sees it) to the right; see {@link MultiDecorationBlock}.
 */
public class HauntedArchwayBlock extends MultiDecorationBlock {
	public static final int WIDTH = 3;
	public static final int HEIGHT = 3;
	public static final int LIGHT = 14;
	/** The left pillar from the bottom up, the crown, then the right pillar from the top down. */
	private static final int[][] CELLS = {{0, 0}, {0, 1}, {0, 2}, {1, 2}, {2, 2}, {2, 1}, {2, 0}};
	public static final int CROWN = 3;
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, CELLS.length - 1);
	private static final VoxelShape PILLAR_NS = Block.box(1.0, 0.0, 3.0, 15.0, 16.0, 13.0);
	private static final VoxelShape PILLAR_EW = Block.box(3.0, 0.0, 1.0, 13.0, 16.0, 15.0);
	private static final VoxelShape ARCH_NS = Block.box(0.0, 2.0, 6.0, 16.0, 12.0, 10.0);
	private static final VoxelShape ARCH_EW = Block.box(6.0, 2.0, 0.0, 10.0, 12.0, 16.0);

	public HauntedArchwayBlock(Properties properties) {
		super(properties);
	}

	/** Whether part {@code part} carries a lantern: the middle of each pillar. */
	public static boolean lantern(int part) {
		return part == 1 || part == 5;
	}

	public static int light(BlockState state) {
		return state.getValue(LIT) && lantern(state.getValue(PART)) ? LIGHT : 0;
	}

	@Override
	public int[][] cells() {
		return CELLS;
	}

	@Override
	public IntegerProperty partProperty() {
		return PART;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		boolean alongX = state.getValue(FACING).getAxis() == Direction.Axis.Z;
		if (state.getValue(PART) == CROWN) {
			return alongX ? ARCH_NS : ARCH_EW;
		}
		return alongX ? PILLAR_NS : PILLAR_EW;
	}
}
