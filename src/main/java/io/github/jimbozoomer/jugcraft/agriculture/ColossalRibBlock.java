package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jspecify.annotations.Nullable;

/**
 * A Colossal Rib (Halloween decorations batch 18, the Buried Colossus): a giant's rib two blocks tall rising from the
 * ground and curving forward over the block in front of it. Set two facing each other with one block between them
 * ({@value #SPAN} apart) and they meet over it as an arch you can walk under ({@link #JOINED}, on both, drawn as the
 * tips joining); breaking either parts them again.
 */
public class ColossalRibBlock extends TallDecorationBlock {
	public static final int SPAN = 2;
	public static final BooleanProperty JOINED = BooleanProperty.create("joined");

	public ColossalRibBlock(Properties properties) {
		super(properties, Block.box(3.0, 0.0, 9.0, 13.0, 16.0, 16.0), Block.box(3.0, 0.0, 6.0, 13.0, 12.0, 15.0));
		registerDefaultState(defaultBlockState().setValue(JOINED, false));
	}

	/** The lower half of the rib that would meet the one with its lower half at {@code lower}, facing {@code facing}. */
	public static BlockPos partner(BlockPos lower, Direction facing) {
		return lower.relative(facing, SPAN);
	}

	/** Whether {@code other} is the lower half of a rib facing back toward one facing {@code facing}. */
	private boolean faces(BlockState other, Direction facing) {
		return other.is(this) && other.getValue(HALF) == DoubleBlockHalf.LOWER && other.getValue(FACING) == facing.getOpposite();
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level.isClientSide()) {
			return;
		}
		Direction facing = state.getValue(FACING);
		BlockPos partner = partner(pos, facing);
		BlockState other = level.getBlockState(partner);
		if (faces(other, facing) && !other.getValue(JOINED)) {
			setBoth(level, pos, level.getBlockState(pos).setValue(JOINED, true));
			setBoth(level, partner, other.setValue(JOINED, true));
		}
	}

	/** Breaking a joined rib parts its partner. */
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (level.getBlockState(pos).is(this) || state.getValue(HALF) != DoubleBlockHalf.LOWER || !state.getValue(JOINED)) {
			return;
		}
		BlockPos partner = partner(pos, state.getValue(FACING));
		BlockState other = level.getBlockState(partner);
		if (faces(other, state.getValue(FACING)) && other.getValue(JOINED)) {
			setBoth(level, partner, other.setValue(JOINED, false));
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(JOINED);
	}
}
