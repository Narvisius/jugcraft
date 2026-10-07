package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A one-block wrought-iron candelabrum (Halloween decorations batch 17): the Table Candelabrum (stands on a block, faces
 * its placer), the Wall Girandole (hangs on a wall, facing out) or the Branching Chandelier (hangs under a block or a
 * chain, its arms reaching a block out on every side and down into the block below). Its candles and their flames are
 * drawn by the client ({@link Candelabra} for what they take and how they light).
 */
public class CandelabrumBlock extends Block implements EntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

	/** How a fitting is held up. */
	public enum Mount {
		TABLE, WALL, HANGING
	}

	private static final VoxelShape TABLE_SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 13.0, 13.0);
	private static final VoxelShape HANGING_SHAPE = Block.box(6.0, 0.0, 6.0, 10.0, 16.0, 10.0);
	private static final VoxelShape[] WALL_SHAPES = {Block.box(3.0, 1.5, 8.5, 13.0, 14.5, 16.0), Block.box(0.0, 1.5, 3.0, 7.5, 14.5, 13.0),
			Block.box(3.0, 1.5, 0.0, 13.0, 14.5, 7.5), Block.box(8.5, 1.5, 3.0, 16.0, 14.5, 13.0)};
	private final String kind;
	private final Mount mount;

	public CandelabrumBlock(Properties properties, String kind, Mount mount) {
		super(properties);
		this.kind = kind;
		this.mount = mount;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Candelabra.WAX, Candelabra.Wax.IVORY)
				.setValue(Candelabra.FLAME, Candelabra.Flame.ORDINARY).setValue(Candelabra.LIT, false).setValue(Candelabra.POWERED, false)
				.setValue(Candelabra.DRIPS, 0));
	}

	/** The fitting's id, for its candles' layout ({@link Candelabra#candles}). */
	public String kind() {
		return kind;
	}

	public Mount mount() {
		return mount;
	}

	/** Which way its candles are turned: the chandelier's are the same all round. */
	public static Direction facing(BlockState state) {
		return state.getBlock() instanceof CandelabrumBlock c && c.mount == Mount.HANGING ? Direction.NORTH : state.getValue(FACING);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DecorationBlockEntity(JugcraftAgriculture.CANDELABRUM_ENTITY, pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (mount) {
			case TABLE -> TABLE_SHAPE;
			case HANGING -> HANGING_SHAPE;
			case WALL -> WALL_SHAPES[switch (state.getValue(FACING)) {
				case EAST -> 1;
				case SOUTH -> 2;
				case WEST -> 3;
				default -> 0;
			}];
		};
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState base = defaultBlockState();
		if (mount == Mount.WALL) {
			for (Direction direction : context.getNearestLookingDirections()) {
				if (direction.getAxis().isHorizontal()) {
					BlockState state = base.setValue(FACING, direction.getOpposite());
					if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
						return state;
					}
				}
			}
			return null;
		}
		BlockState state = base.setValue(FACING, context.getHorizontalDirection().getOpposite());
		return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
	}

	/** A table candelabrum stands on a block, a girandole needs its wall, a chandelier something to hang from. */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return switch (mount) {
			case TABLE -> Block.canSupportCenter(level, pos.below(), Direction.UP);
			case HANGING -> Block.canSupportCenter(level, pos.above(), Direction.DOWN);
			case WALL -> {
				Direction facing = state.getValue(FACING);
				BlockPos behind = pos.relative(facing.getOpposite());
				yield level.getBlockState(behind).isFaceSturdy(level, behind, facing);
			}
		};
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighbor, RandomSource random) {
		return !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		return Candelabra.use(stack, state, level, pos, player, hand, changed -> level.setBlock(pos, changed, Block.UPDATE_ALL));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		return Candelabra.snuff(state, level, pos, player, changed -> level.setBlock(pos, changed, Block.UPDATE_ALL));
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation, boolean moved) {
		super.neighborChanged(state, level, pos, neighbor, orientation, moved);
		if (level instanceof ServerLevel server) {
			BlockState changed = Candelabra.powered(state, level.hasNeighborSignal(pos));
			if (changed != null) {
				level.setBlock(pos, changed, Block.UPDATE_ALL);
				server.playSound(null, pos, changed.getValue(Candelabra.LIT) ? SoundEvents.FIRECHARGE_USE : SoundEvents.CANDLE_EXTINGUISH,
						SoundSource.BLOCKS, 0.3F, 1.4F);
			}
		}
	}

	/** A burning arrow lights the candles. */
	@Override
	protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
		if (!level.isClientSide() && projectile.isOnFire() && !state.getValue(Candelabra.LIT)) {
			level.setBlock(hit.getBlockPos(), state.setValue(Candelabra.LIT, true), Block.UPDATE_ALL);
		}
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return Candelabra.drips(state);
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		BlockState changed = Candelabra.drip(state, random);
		if (changed != null) {
			level.setBlock(pos, changed, Block.UPDATE_CLIENTS);
		}
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		Candelabra.smoke(kind, state, level, pos, facing(state), random);
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
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, Candelabra.WAX, Candelabra.FLAME, Candelabra.LIT, Candelabra.POWERED, Candelabra.DRIPS);
	}
}
