package io.github.jimbozoomer.jugcraft.drone;

import io.github.jimbozoomer.jugcraft.energy.EnergyConnectable;
import io.github.jimbozoomer.jugcraft.energy.EnergyNetworks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Drone Depot Terminal block: a command console (a desk with one big display) that faces whoever placed
 * it. Cables power it from any side. Right-click shows the depot readout; the owner sneak-right-clicks to
 * switch between Personal and Party use.
 */
public class DroneTerminalBlock extends BaseEntityBlock implements EnergyConnectable {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	private static final java.util.Map<Direction, VoxelShape> SHAPES = new java.util.EnumMap<>(Direction.class);

	static {
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			// Desk, and the display standing at its far edge.
			SHAPES.put(facing, Shapes.or(Block.box(0, 0, 0, 16, 12, 16), turned(new double[] {0.5, 12, 2, 15.5, 16, 4.5}, facing)));
		}
	}

	public DroneTerminalBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	private static VoxelShape turned(double[] b, Direction facing) {
		double x0 = b[0], z0 = b[2], x1 = b[3], z1 = b[5];
		int turns = switch (facing) {
			case EAST -> 1;
			case SOUTH -> 2;
			case WEST -> 3;
			default -> 0;
		};
		for (int i = 0; i < turns; i++) {
			double nx0 = 16 - z1, nx1 = 16 - z0, nz0 = x0, nz1 = x1;
			x0 = nx0;
			x1 = nx1;
			z0 = nz0;
			z1 = nz1;
		}
		return Block.box(x0, b[1], z0, x1, b[4], z1);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DroneTerminalBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel)) {
			return null;
		}
		return createTickerHelper(type, JugcraftDrones.TERMINAL_ENTITY,
				(tickLevel, pos, tickState, terminal) -> terminal.serverTick((ServerLevel) tickLevel, pos));
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (!level.isClientSide() && placer instanceof Player player && level.getBlockEntity(pos) instanceof DroneTerminalBlockEntity terminal) {
			terminal.setOwner(player.getUUID());
		}
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
		// Tower modules handed to the terminal go into its Drone Tower's core (it sits under the plotting table).
		// A module in hand never opens the terminal screen, on either side, whatever happens to it.
		if (io.github.jimbozoomer.jugcraft.tower.JugcraftTower.MODULE_ITEMS.containsValue(stack.getItem())
				&& level.getBlockEntity(pos) instanceof DroneTerminalBlockEntity terminal) {
			if (!level.isClientSide()) {
				if (terminal.towerPos() == null
						|| !(level.getBlockEntity(terminal.towerPos()) instanceof io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity core)) {
					player.sendOverlayMessage(Component.translatable("message.jugcraft.tower.no_tower"));
				} else if (!core.mayUse(player)) {
					player.sendOverlayMessage(Component.translatable("message.jugcraft.tower.not_owner"));
				} else if (core.deposit(stack)) {
					player.sendOverlayMessage(Component.translatable("message.jugcraft.tower.deposited"));
				}
			}
			return InteractionResult.SUCCESS;
		}
		return super.useItemOn(stack, state, level, pos, player, hand, hit);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.getBlockEntity(pos) instanceof DroneTerminalBlockEntity terminal) {
			if (!level.isClientSide() && terminal.owner() == null) {
				// An unowned terminal (for example from the control room template) belongs to its first user.
				terminal.setOwner(player.getUUID());
				player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.jugcraft.drone.claimed"));
			}
			if (player.isShiftKeyDown()) {
				if (!level.isClientSide()) {
					terminal.toggleMode(player);
				}
			} else if (level.isClientSide()) {
				openScreen.accept(pos);
			} else {
				terminal.rescanNow();
				terminal.refreshClients();
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** Opens the terminal screen on the client; set by the client entry point (no-op on a server). */
	public static java.util.function.Consumer<BlockPos> openScreen = pos -> {
	};

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		EnergyNetworks.invalidate(level);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		EnergyNetworks.invalidate(level);
	}
}
