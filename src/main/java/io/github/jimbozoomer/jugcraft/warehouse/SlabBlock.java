package io.github.jimbozoomer.jugcraft.warehouse;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Warehouse Floor Slab (part W3): the floor under the racks on an upper floor, its epoxy topping over the SOFFIT
 * (the metal deck, a girder on a column line) of the floor below.
 */
public class SlabBlock extends Block implements WarehouseBlock {
	public static final EnumProperty<Soffit> SOFFIT = Soffit.PROPERTY;

	public SlabBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(SOFFIT, Soffit.NONE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(SOFFIT);
	}

	@Override
	protected VoxelShape getOcclusionShape(BlockState state) {
		return state.getValue(SOFFIT) == Soffit.NONE ? Shapes.block() : Shapes.empty();
	}

	@Override
	protected int getLightDampening(BlockState state) {
		return 15;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(SOFFIT, state.getValue(SOFFIT).rotate(rotation));
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (!oldState.is(this)) {
			Warehouses.changed(level, pos);
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		Warehouses.changed(level, pos);
	}
}
