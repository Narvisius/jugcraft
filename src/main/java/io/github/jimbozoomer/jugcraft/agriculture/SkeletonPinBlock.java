package io.github.jimbozoomer.jugcraft.agriculture;

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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Skeleton Pin for Pumpkin Bowling: a little skeleton standing to attention, or knocked flat ({@link #DOWN}), lying
 * the way the pumpkin sent it ({@link #FACING}). A rolling Bowling Pumpkin knocks it down; the Bowling Scoreboard
 * stands it up again, and so does using it. Nothing walks into it, so the pumpkin rolls through the pins.
 */
public class SkeletonPinBlock extends Block {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty DOWN = BooleanProperty.create("down");
	private static final VoxelShape STANDING = Block.box(5.0, 0.0, 5.0, 11.0, 14.0, 11.0);
	private static final VoxelShape FALLEN = Block.box(1.0, 0.0, 1.0, 15.0, 4.0, 15.0);

	public SkeletonPinBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(DOWN, false));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(DOWN) ? FALLEN : STANDING;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	/** Knocks the pin at {@code pos} down, lying toward {@code toward}; returns whether it was standing. */
	public static boolean knock(Level level, BlockPos pos, Direction toward) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof SkeletonPinBlock) || state.getValue(DOWN)) {
			return false;
		}
		level.setBlock(pos, state.setValue(DOWN, true).setValue(FACING, toward), Block.UPDATE_ALL);
		level.playSound(null, pos, SoundEvents.SKELETON_HURT, SoundSource.BLOCKS, 0.5F, 1.4F + level.getRandom().nextFloat() * 0.3F);
		return true;
	}

	/** Stands a fallen pin up again. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!state.getValue(DOWN)) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			level.setBlock(pos, state.setValue(DOWN, false), Block.UPDATE_ALL);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, DOWN);
	}
}
