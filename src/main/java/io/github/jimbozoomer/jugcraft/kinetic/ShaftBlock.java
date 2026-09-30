package io.github.jimbozoomer.jugcraft.kinetic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** An iron shaft: carries rotation along its axis (see {@link KineticNetworks}). Placed like a log. */
public class ShaftBlock extends RotatedPillarBlock {
	/** Shown turning while a source drives it. */
	public static final BooleanProperty TURNING = BooleanProperty.create("turning");
	private static final VoxelShape X = Block.box(0, 6, 6, 16, 10, 10);
	private static final VoxelShape Y = Block.box(6, 0, 6, 10, 16, 10);
	private static final VoxelShape Z = Block.box(6, 6, 0, 10, 10, 16);

	public ShaftBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.Y).setValue(TURNING, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(TURNING);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(AXIS)) {
			case X -> X;
			case Y -> Y;
			case Z -> Z;
		};
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (!oldState.is(this)) {
			KineticNetworks.invalidate(level);
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		KineticNetworks.invalidate(level);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
			Orientation orientation, boolean movedByPiston) {
		KineticNetworks.invalidate(level);
	}

	/** Stops the turning look once no source has pushed through for a while. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		spinDown(this, state, level, pos);
	}

	static void spinDown(Block block, BlockState state, ServerLevel level, BlockPos pos) {
		if (!state.getValue(TURNING)) {
			return;
		}
		if (KineticNetworks.recentlyTurned(level, pos)) {
			level.scheduleTick(pos, block, KineticNetworks.SPIN_DOWN * 2);
		} else {
			level.setBlock(pos, state.setValue(TURNING, false), Block.UPDATE_CLIENTS);
		}
	}
}
