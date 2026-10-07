package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Colossal Vertebra (Halloween decorations batch 18, the Buried Colossus): a giant's vertebra, its round body with the
 * spine and wings standing off it, that lines up along whichever axis it is placed on like a log, so a row of them is a
 * spine.
 */
public class ColossalVertebraBlock extends RotatedPillarBlock {
	private static final VoxelShape X = Block.box(0.0, 2.0, 2.0, 16.0, 14.0, 14.0);
	private static final VoxelShape Y = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);
	private static final VoxelShape Z = Block.box(2.0, 2.0, 0.0, 14.0, 14.0, 16.0);

	public ColossalVertebraBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(AXIS)) {
			case X -> X;
			case Z -> Z;
			default -> Y;
		};
	}
}
