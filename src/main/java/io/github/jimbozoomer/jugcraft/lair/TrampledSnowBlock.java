package io.github.jimbozoomer.jugcraft.lair;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Trampled Snow, the Glacier Hall's firm footing (docs/features/glacier-hall.md): round the ice columns, at the snow
 * ramp's foot, before the dais and on the dens' paths, the snow the Yeti King never bares; and the snow ramp itself, whose
 * half steps are its half blocks ({@link #HEIGHT} 1, in half blocks), so the ramp falls half a block a block and players
 * walk down it (and up it) without jumping.
 */
public class TrampledSnowBlock extends Block {
	public static final IntegerProperty HEIGHT = IntegerProperty.create("height", 1, 2);
	private static final VoxelShape HALF = Block.box(0, 0, 0, 16, 8, 16);

	public TrampledSnowBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(HEIGHT, 2));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(HEIGHT) == 1 ? HALF : Shapes.block();
	}

	@Override
	protected boolean useShapeForLightOcclusion(BlockState state) {
		return state.getValue(HEIGHT) == 1;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(HEIGHT);
	}
}
