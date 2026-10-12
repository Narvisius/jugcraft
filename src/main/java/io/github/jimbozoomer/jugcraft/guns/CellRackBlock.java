package io.github.jimbozoomer.jugcraft.guns;

import io.github.jimbozoomer.jugcraft.energy.EnergyConnectable;
import io.github.jimbozoomer.jugcraft.energy.EnergyNetworks;
import io.github.jimbozoomer.jugcraft.tools.Chargeable;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Cell Rack (slice 10E): a rack of six cradles, two shelves of three, that charges every Energy Cell standing in it
 * at once from cables. Use it with an Energy Cell to stand the cell in the cradle nearest where you point (or the
 * nearest free one); use it with an empty hand to take the cell from the cradle nearest where you point. A hopper
 * above or beside it puts cells in; a hopper below takes out only full ones ({@link CellRackBlockEntity}).
 */
public class CellRackBlock extends BaseEntityBlock implements EnergyConnectable {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	/** The cradles' columns across the front, left to right seen from the front, in pixels of the north-facing model. */
	static final double[] COLUMNS = {12.5, 8.0, 3.5};
	/** Where the upper shelf begins, in pixels: a cradle above it is in the upper row. */
	static final double UPPER_ROW = 9.0;
	/** Its plinth and body, the body a pixel back from the front; turned to each facing. */
	private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(
			Shapes.or(Block.box(0, 0, 0, 16, 1.5, 16), Block.box(0, 1.5, 1, 16, 16, 16)));

	public CellRackBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, LIT);
	}

	/** Faces the player. */
	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CellRackBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel)) {
			return null;
		}
		return createTickerHelper(type, JugcraftGuns.CELL_RACK_ENTITY,
				(tickLevel, pos, tickState, rack) -> rack.serverTick((ServerLevel) tickLevel, pos, tickState));
	}

	/**
	 * The cradle nearest a point on the rack (block coordinates, 0 to 1): its column across the rack's front and its
	 * row by height. Slots 0 to 2 are the lower shelf, 3 to 5 the upper, each left to right seen from the front.
	 */
	public static int cradleAt(Direction facing, double x, double y, double z) {
		// The point in the north-facing model's frame (the block model is turned to face the rack's way).
		double across = switch (facing) {
			case SOUTH -> 1.0 - x;
			case EAST -> z;
			case WEST -> 1.0 - z;
			default -> x;
		};
		int column = 0;
		for (int i = 1; i < COLUMNS.length; i++) {
			if (Math.abs(across * 16.0 - COLUMNS[i]) < Math.abs(across * 16.0 - COLUMNS[column])) {
				column = i;
			}
		}
		return (y * 16.0 >= UPPER_ROW ? COLUMNS.length : 0) + column;
	}

	private static int cradleAt(BlockState state, BlockPos pos, BlockHitResult hit) {
		Vec3 at = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
		return cradleAt(state.getValue(FACING), at.x, at.y, at.z);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (!stack.is(JugcraftGuns.ENERGY_CELL) || !(level.getBlockEntity(pos) instanceof CellRackBlockEntity rack)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		int slot = rack.nearest(cradleAt(state, pos, hit), true);
		if (slot < 0) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			rack.setItem(slot, stack.split(1));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof CellRackBlockEntity rack)) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			int slot = rack.nearest(cradleAt(state, pos, hit), false);
			if (slot >= 0) {
				ItemStack cell = rack.removeItemNoUpdate(slot);
				rack.changed();
				player.sendOverlayMessage(Component.translatable("message.jugcraft.cell_rack.cell", cell.getHoverName(),
						String.format("%,d", Chargeable.energy(cell)), String.format("%,d", Chargeable.capacity(cell))));
				if (!player.getInventory().add(cell)) {
					Block.popResource(level, pos, cell);
				}
			} else {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.cell_rack",
						String.format("%,d", rack.energy().getAmount()), String.format("%,d", rack.energy().getCapacity())));
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (!oldState.is(this)) {
			EnergyNetworks.invalidate(level);
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		EnergyNetworks.invalidate(level);
	}
}
