package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Dead Hollow Tree: a dead, gnarled trunk {@value #HEIGHT} blocks tall, a dark hollow at its foot and a face in its
 * bark whose eyes glow, its bare branches reaching out over the blocks around it, with an iron lantern hanging from two
 * of them (light {@value #LIGHT}). Only the trunk is solid. Placed upward from the block aimed at; see
 * {@link MultiDecorationBlock}.
 */
public class DeadHollowTreeBlock extends MultiDecorationBlock {
	public static final int HEIGHT = 4;
	public static final int LIGHT = 13;
	/** The part the lanterns hang at. */
	public static final int LANTERNS = 2;
	private static final int[][] CELLS = {{0, 0}, {0, 1}, {0, 2}, {0, 3}};
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, CELLS.length - 1);
	private static final VoxelShape FOOT = Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);
	private static final VoxelShape TRUNK = Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);

	public DeadHollowTreeBlock(Properties properties) {
		super(properties);
	}

	public static int light(BlockState state) {
		return state.getValue(LIT) && state.getValue(PART) == LANTERNS ? LIGHT : 0;
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
		return state.getValue(PART) == 0 ? FOOT : TRUNK;
	}
}
