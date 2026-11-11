package io.github.jimbozoomer.jugcraft.exterior;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A flat roof's block (tools/exterior_kit.py roof_deck): the single-ply membrane on top; underneath, the trapezoidal
 * metal deck (its ribs along AXIS) and, with GIRDER, the steel girder carrying it across them (on a column line). The
 * soffit leaves the bottom of the block open, so it hides nothing beside it, but it lets no light through.
 */
public class RoofDeckBlock extends Block {
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
	public static final BooleanProperty GIRDER = BooleanProperty.create("girder");

	public RoofDeckBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X).setValue(GIRDER, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS, GIRDER);
	}

	/** The ribs across the way the placer looks. */
	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getClockWise().getAxis());
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return turnAxis(state, AXIS, rotation);
	}

	public static BlockState turnAxis(BlockState state, EnumProperty<Direction.Axis> axis, Rotation rotation) {
		if (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90) {
			return state.setValue(axis, state.getValue(axis) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
		}
		return state;
	}

	@Override
	protected VoxelShape getOcclusionShape(BlockState state) {
		return Shapes.empty();
	}

	@Override
	protected int getLightDampening(BlockState state) {
		return 15;
	}
}
