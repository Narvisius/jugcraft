package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A building's name sign (or a room's door plaque): a panel with the name on it, hung on the wall behind it and drawn
 * as wide as the name needs (client/PlantSignRenderer.java). Which name is a property, so a blueprint carries it.
 */
public class PlantSignBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<Label> LABEL = EnumProperty.create("label", Label.class);

	/** The names (lang: sign.jugcraft.<name>). */
	public enum Label implements StringRepresentable {
		UNIT_1, REACTOR_BUILDING, TURBINE_BUILDING, CONTROL_BUILDING, AUXILIARY_BUILDING, FUEL_BUILDING, DIESEL_BUILDING,
		PUMP_HOUSE, ADMINISTRATION, SECURITY, WAREHOUSE, TRANSFORMER_YARD, SWITCHYARD,
		MAIN_CONTROL_ROOM, SHIFT_SUPERVISOR, RELAY_ROOM, CABLE_SPREADING_ROOM, SERVER_ROOM, BATTERY_ROOM, HVAC_ROOM,
		SWITCHGEAR_ROOM, LUBE_OIL_ROOM, WORKSHOP, TURBINE_CONTROL_ROOM, DIESEL_A, DIESEL_B, OFFICE, MEETING_ROOM,
		CAFETERIA, LOBBY, STAIRS, GUARD_ROOM, STORES, PUMP_ROOM, ELECTRICAL_ROOM, HEALTH_PHYSICS, CHARGING_PUMPS,
		RHR_PUMPS, HEAT_EXCHANGERS, TANK_ROOM, VENTILATION, FUEL_POOL, TRUCK_BAY, LOCKER_ROOM,
		BREAK_ROOM, CHEMISTRY_LAB, DOCUMENT_CONTROL, TRAINING_ROOM, ENGINEERING, PLANT_MANAGER, IT_SUPPORT, FIRST_AID, OPEN_OFFICE,
		BOARD_ROOM, SCREENING, STAFF_ONLY, OPERATIONS, MAINTENANCE_PLANNING, HUMAN_RESOURCES, VISITORS, WASHROOM, INTERN_AREA;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	private static final VoxelShape[] SHAPES = {
			Block.box(2, 4, 15, 14, 12, 16), Block.box(0, 4, 2, 1, 12, 14), Block.box(2, 4, 0, 14, 12, 1), Block.box(15, 4, 2, 16, 12, 14)};

	public PlantSignBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LABEL, Label.UNIT_1));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, LABEL);
	}

	/** Faces away from the wall it was put on. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction face = context.getClickedFace();
		return defaultBlockState().setValue(FACING, face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		// The mounting plate against the wall behind (the facing's opposite side).
		return switch (state.getValue(FACING)) {
			case NORTH -> SHAPES[0];
			case EAST -> SHAPES[1];
			case SOUTH -> SHAPES[2];
			default -> SHAPES[3];
		};
	}

	@Override
	protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new PlantSignBlockEntity(pos, state);
	}

	public boolean big() {
		return this == PlantInterior.BUILDING_SIGN;
	}
}
