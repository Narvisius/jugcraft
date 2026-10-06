package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A roller shutter door: build it as a wall of these blocks in an opening and it works as one door. Using any part
 * rolls the whole door up into its drum (the blocks stay, open and walk-through) or brings it down again.
 */
public class RollerDoorBlock extends HorizontalDirectionalBlock {
	public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
	/** The biggest door that opens as one. */
	public static final int MAX_BLOCKS = 400;
	private static final VoxelShape NS = Block.box(0, 0, 6.5, 16, 16, 9.5);
	private static final VoxelShape EW = Block.box(6.5, 0, 0, 9.5, 16, 16);

	public RollerDoorBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, OPEN);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(FACING).getAxis() == Direction.Axis.Z ? NS : EW;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(OPEN) ? Shapes.empty() : getShape(state, level, pos, context);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			boolean open = !state.getValue(OPEN);
			Direction facing = state.getValue(FACING);
			// The whole door: every connected roller door block in the same plane.
			Set<BlockPos> door = new HashSet<>();
			ArrayDeque<BlockPos> queue = new ArrayDeque<>();
			queue.add(pos);
			door.add(pos);
			Direction along = facing.getClockWise();
			while (!queue.isEmpty() && door.size() < MAX_BLOCKS) {
				BlockPos at = queue.poll();
				for (BlockPos next : new BlockPos[] {at.above(), at.below(), at.relative(along), at.relative(along.getOpposite())}) {
					if (!door.contains(next) && level.getBlockState(next).is(this) && level.getBlockState(next).getValue(FACING).getAxis() == facing.getAxis()) {
						door.add(next);
						queue.add(next);
					}
				}
			}
			for (BlockPos at : door) {
				level.setBlock(at, level.getBlockState(at).setValue(OPEN, open), Block.UPDATE_CLIENTS);
			}
			level.playSound(null, pos, open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.6F);
		}
		return InteractionResult.SUCCESS;
	}
}
