package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Shielded storage for radioactive material (round 14; tools/nuclear_storage.py draws them), each a store that lets
 * only a small share of its contents' radiation out ({@link ShieldedContainer}; an ordinary chest lets 80% out):
 * <ul>
 *   <li>the lead pig: 9 slots, a squat lead flask; picked up with what is in it (like a shulker box), and still
 *   shielding it in your inventory; lets 0.1% out;</li>
 *   <li>the lead-lined locker: 27 slots, doors that swing open while it is in use; 0.1%;</li>
 *   <li>the shielded waste drum: 18 slots, concrete-lined; a full block high, so they stack; 0.2%.</li>
 * </ul>
 * FACING is the way the user looks (the front faces the other way), like the plant furniture.
 */
public class ShieldedStorageBlock extends BaseEntityBlock implements ShieldedContainer.Block {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

	public enum Kind {
		PIG(9, 0.001, 2.6, 0, 2.6, 13.4, 12.2, 13.4),
		LOCKER(27, 0.001, 0.8, 0, 1.8, 15.2, 15.6, 15.6),
		DRUM(18, 0.002, 2.0, 0, 2.0, 14.0, 16, 14.0);

		public final int slots;
		public final double leakage;
		final double[] box;

		Kind(int slots, double leakage, double... box) {
			this.slots = slots;
			this.leakage = leakage;
			this.box = box;
		}

		public String id() {
			return switch (this) {
				case PIG -> "lead_pig";
				case LOCKER -> "lead_locker";
				case DRUM -> "waste_drum";
			};
		}
	}

	public final Kind kind;
	private final VoxelShape[] shapes = new VoxelShape[4];

	public ShieldedStorageBlock(Kind kind, Properties properties) {
		super(properties);
		this.kind = kind;
		BlockState state = stateDefinition.any().setValue(FACING, Direction.NORTH);
		if (kind == Kind.LOCKER) {
			state = state.setValue(OPEN, false);
		}
		registerDefaultState(state);
		double[] b = kind.box;
		for (Direction d : Direction.Plane.HORIZONTAL) {
			shapes[d.get2DDataValue()] = turned(b, d);
		}
	}

	/** The north-facing box turned the way the blockstate turns the model (y 90 = east). */
	static VoxelShape turned(double[] b, Direction facing) {
		double x0 = b[0], y0 = b[1], z0 = b[2], x1 = b[3], y1 = b[4], z1 = b[5];
		return switch (facing) {
			case EAST -> Block.box(16 - z1, y0, x0, 16 - z0, y1, x1);
			case SOUTH -> Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0);
			case WEST -> Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0);
			default -> Block.box(x0, y0, z0, x1, y1, z1);
		};
	}

	@Override
	public double leakage() {
		return kind.leakage;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
		// (Only the locker shows itself open; the property is on all three so they share one definition.)
		builder.add(OPEN);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapes[state.getValue(FACING).get2DDataValue()];
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
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
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ShieldedStorageBlockEntity store) {
			player.openMenu(store);
		}
		return InteractionResult.SUCCESS;
	}

	/** A lead pig broken in creative still drops, with what is in it (survival breaking goes by the loot table). */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (kind == Kind.PIG && !level.isClientSide() && player.preventsBlockDrops()
				&& level.getBlockEntity(pos) instanceof ShieldedStorageBlockEntity store && !store.isEmpty()) {
			ItemStack stack = new ItemStack(this);
			stack.applyComponents(store.collectComponents());
			ItemEntity item = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
			item.setDefaultPickUpDelay();
			level.addFreshEntity(item);
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	/** The openers counter's recheck (someone walked away with it open): like a barrel's. */
	@Override
	protected void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
		if (level.getBlockEntity(pos) instanceof ShieldedStorageBlockEntity store) {
			store.recheckOpen();
		}
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ShieldedStorageBlockEntity(pos, state);
	}
}
