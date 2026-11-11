package io.github.jimbozoomer.jugcraft.warehouse;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A drone crossing over a walkway (part W3; WalkwayBlock tells the story): the zebra bars along AXIS, the way the drones
 * cross. Drones drive on it as on a lane tile (Network); its beacons flash while one is booked through it.
 */
public class CrossingBlock extends Block implements WarehouseBlock {
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
	public static final EnumProperty<Soffit> SOFFIT = Soffit.PROPERTY;

	public CrossingBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Z).setValue(SOFFIT, Soffit.NONE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS, SOFFIT);
	}

	/** The bars along the way the placer looks (the drones' way). */
	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getAxis());
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
		BlockState turned = io.github.jimbozoomer.jugcraft.exterior.RoofDeckBlock.turnAxis(state, AXIS, rotation);
		return turned.setValue(SOFFIT, state.getValue(SOFFIT).rotate(rotation));
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
