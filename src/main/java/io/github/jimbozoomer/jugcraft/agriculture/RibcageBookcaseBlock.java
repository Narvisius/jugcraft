package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Ribcage Bookcase (Halloween decorations batch 18): a giant's ribcage on a plank plinth holding six books between
 * its ribs. It is a chiseled bookshelf in all but looks: use a book, written book or enchanted book on a place on its
 * front to shelve it there and use it again to take it back; hoppers fill and empty it; a comparator reads the last
 * place used; and it counts as a bookshelf for an enchanting table (block tag
 * {@code minecraft:enchantment_power_provider}). Vanilla's chiseled bookshelf block entity holds its books; this block is
 * added to that block entity type's blocks when it is registered.
 */
public class RibcageBookcaseBlock extends ChiseledBookShelfBlock {
	private static final VoxelShape[] SHAPES = {
			LongDecorationBlock.turned(new double[] {0.5, 0.0, 1.0, 15.5, 16.0, 15.0}, Direction.NORTH),
			LongDecorationBlock.turned(new double[] {0.5, 0.0, 1.0, 15.5, 16.0, 15.0}, Direction.EAST)};

	public RibcageBookcaseBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES[state.getValue(HorizontalDirectionalBlock.FACING).getAxis() == Direction.Axis.Z ? 0 : 1];
	}
}
