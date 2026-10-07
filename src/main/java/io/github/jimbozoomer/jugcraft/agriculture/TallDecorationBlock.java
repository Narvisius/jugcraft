package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A two-block-tall decoration that stands on any solid top face and faces the player who placed it: the
 * Scarecrow and the Corn Shock. It behaves like a vanilla tall flower (breaking either half breaks both
 * and drops once, from the lower half; nothing drops in creative), but keeps its facing, and any other
 * property, on both halves.
 */
public class TallDecorationBlock extends DoublePlantBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	private final VoxelShape lower;
	private final VoxelShape upper;

	public TallDecorationBlock(Properties properties, VoxelShape lower, VoxelShape upper) {
		super(properties);
		this.lower = lower;
		this.upper = upper;
		registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER).setValue(FACING, Direction.NORTH));
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		return state == null ? null : state.setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	/** Places the upper half as a copy of the lower one, so the two always agree. */
	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
	}

	/** Stands on anything with a solid top, not only on soil like a flower. */
	@Override
	protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
		return state.isFaceSturdy(level, pos, Direction.UP);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? lower : upper;
	}

	/** Sets {@code changed} on this half and the other half too. */
	protected static void setBoth(Level level, BlockPos pos, BlockState changed) {
		level.setBlock(pos, changed, Block.UPDATE_ALL);
		DoubleBlockHalf half = changed.getValue(HALF);
		BlockPos other = pos.relative(half.getDirectionToOther());
		BlockState otherState = level.getBlockState(other);
		if (otherState.is(changed.getBlock())) {
			level.setBlock(other, changed.setValue(HALF, half.getOtherHalf()), Block.UPDATE_ALL);
		}
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
		super.createBlockStateDefinition(builder);
		builder.add(FACING);
	}
}
