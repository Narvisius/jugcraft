package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The anchor of a piece of nuclear equipment: the front left bottom corner of its footprint, as placed. It holds the
 * block entity (the reactor core, the condenser's water, the transformer's output, the console's link); invisible
 * {@link EquipmentFrameBlock}s give the rest of the body. The client draws the whole thing from the kind.
 */
public class EquipmentAnchorBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	/**
	 * Which labelled version of a piece this is (round 26, 8c: the DC rooms' motor control centres, MCC 1A or 1B; the load
	 * centres LC 1A to 1E, the pump house's the fifth): the client draws its name by it. 0 for everything else.
	 */
	public static final net.minecraft.world.level.block.state.properties.IntegerProperty VARIANT =
			net.minecraft.world.level.block.state.properties.IntegerProperty.create("variant", 0, 4);
	/** The variant a hideable kind (EquipmentKind.hideable) stands hidden in: its anchor only, nothing drawn. */
	public static final int HIDDEN = 4;
	public final EquipmentKind kind;

	/** Whether a block is a piece of equipment standing hidden (its anchor only, under a building whose site draws it). */
	public static boolean hidden(BlockState state) {
		return state.getBlock() instanceof EquipmentAnchorBlock a && a.kind.hideable() && state.getValue(VARIANT) == HIDDEN;
	}

	public EquipmentAnchorBlock(EquipmentKind kind, Properties properties) {
		super(properties);
		this.kind = kind;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(VARIANT, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, VARIANT);
	}

	/** Turns with a blueprint (or a structure) like any facing block. */
	@Override
	protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new EquipmentBlockEntity(pos, state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return kind.solid(0, 0, 0) || hidden(state) ? Shapes.block() : Shapes.empty();
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel)) {
			return null;
		}
		return createTickerHelper(type, JugcraftNuclear.EQUIPMENT_ENTITY, (l, pos, s, equipment) -> equipment.serverTick((ServerLevel) l));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		return EquipmentPlacement.use(level, pos, player, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit.getLocation());
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		return EquipmentPlacement.use(level, pos, player, stack, hand, hit.getLocation());
	}
}
