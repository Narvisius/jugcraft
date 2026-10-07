package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.fluid.FluidConnectable;
import io.github.jimbozoomer.jugcraft.fluid.FluidNetworks;
import java.util.Map;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A storage tank's inlet or outlet nozzle: a flanged nozzle low on the shell with a gate valve, standing on the tank's
 * foundation against the shell and facing out. Pipes join at its outer flange. Right-click opens or shuts the valve
 * (its handwheel turns and its stem rises or sinks); a redstone signal holds it shut. Its lamp is green while open,
 * amber while shut and dark while the tank is not formed. Buckets fill the tank at the inlet and draw from it at the
 * outlet. The outlet is the tank's controller ({@link StorageTankPortBlockEntity}): it forms the tank and holds its
 * fluid.
 */
public class StorageTankPortBlock extends Block implements EntityBlock, FluidConnectable {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	public static final BooleanProperty FORMED = StorageTankPlateBlock.FORMED;

	private final boolean outlet;
	private final Map<Direction, VoxelShape> shapes;

	public StorageTankPortBlock(Properties properties, boolean outlet) {
		super(properties);
		this.outlet = outlet;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, true).setValue(POWERED, false)
				.setValue(FORMED, false));
		// Facing north (out): the valve and its pipe support, the yoke, and the nozzle into the shell (outlet) or the
		// elbow up into the fill line (inlet).
		VoxelShape north = outlet
				? Shapes.or(Block.box(4.3, 0, 0, 11.7, 12.2, 16), Block.box(5.6, 12.2, 6.4, 10.4, 16, 10.9))
				: Shapes.or(Block.box(4.3, 0, 0, 11.7, 12.2, 9.0), Block.box(5.6, 0, 9.0, 10.4, 16, 16), Block.box(5.6, 12.2, 3.4, 10.4, 16, 8.0));
		shapes = Shapes.rotateHorizontal(north.optimize());
	}

	public boolean outlet() {
		return outlet;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, OPEN, POWERED, FORMED);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		// Against the shell, facing out: the face clicked on the shell faces out; otherwise opposite the player's look.
		Direction face = context.getClickedFace();
		Direction out = face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite();
		return defaultBlockState().setValue(FACING, out).setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapes.get(state.getValue(FACING));
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new StorageTankPortBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!outlet || level.isClientSide() || type != StorageTanks.PORT_ENTITY) {
			return null;
		}
		return (l, pos, s, entity) -> ((StorageTankPortBlockEntity) entity).serverTick((ServerLevel) l);
	}

	/** Whether fluid can pass the valve: opened by hand and not held shut by redstone. */
	public static boolean valveOpen(BlockState state) {
		return state.getValue(OPEN) && !state.getValue(POWERED);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		InteractionResult upgraded = TankUpgrades.use(stack, level, pos, player);
		if (upgraded != null) {
			return upgraded;
		}
		net.fabricmc.fabric.api.transfer.v1.storage.Storage<net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant> held =
				FluidStorage.ITEM.find(stack, ContainerItemContext.forPlayerInteraction(player, hand));
		if (!(level.getBlockEntity(pos) instanceof StorageTankPortBlockEntity port) || held == null) {
			return super.useItemOn(stack, state, level, pos, player, hand, hit);
		}
		if (FluidStorageUtil.interactWithFluidStorage(port.portStorage(), player, hand)) {
			return InteractionResult.SUCCESS;
		}
		if (!level.isClientSide()) {
			// A liquid the tank isn't rated for: say which tank it needs.
			net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant pouring = net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil.findStoredResource(held);
			net.minecraft.network.chat.Component refused = !outlet && pouring != null && port.tank() != null
					? TankUpgrades.refusal(port.rating(), pouring) : null;
			player.sendOverlayMessage(refused != null ? refused : port.status(state, null));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof StorageTankPortBlockEntity port) {
			boolean open = !state.getValue(OPEN);
			BlockState now = state.setValue(OPEN, open);
			level.setBlock(pos, now, Block.UPDATE_ALL);
			level.playSound(null, pos, open ? SoundEvents.IRON_TRAPDOOR_OPEN : SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.7F,
					open ? 0.7F : 0.6F);
			FluidNetworks.invalidate(level);
			player.sendOverlayMessage(port.status(now, open));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation,
			boolean movedByPiston) {
		boolean powered = level.hasNeighborSignal(pos);
		if (powered != state.getValue(POWERED)) {
			level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
			FluidNetworks.invalidate(level);
		}
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		FluidNetworks.invalidate(level);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		FluidNetworks.invalidate(level);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return level.getBlockEntity(pos) instanceof StorageTankPortBlockEntity port ? port.redstone() : 0;
	}
}
