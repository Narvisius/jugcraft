package io.github.jimbozoomer.jugcraft.building;

import io.github.jimbozoomer.jugcraft.agriculture.LongDecorationBlock;
import io.github.jimbozoomer.jugcraft.agriculture.TallDecorationBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Gas Curtain (batch 59): a heavy wet blanket hung from a timber batten across a doorway, two blocks tall. Anyone
 * walks through it (it has no collision), but while it hangs down it keeps chlorine and smoke out: a cloud does not reach
 * a target when an unrolled curtain lies on the straight line from the cloud's middle to the target's eyes
 * ({@link #shields}, used by weapons/ChemicalCloud). Thermite, which burns on the floor, is not stopped. Using either half
 * rolls it up under its batten or lets it down again ({@link #ROLLED}).
 */
public class GasCurtainBlock extends TallDecorationBlock {
	/** Rolled up under its batten: open, and no use against gas. */
	public static final BooleanProperty ROLLED = BooleanProperty.create("rolled");
	/** How far apart (blocks) {@link #shields} looks along its line, at most. */
	public static final double STEP = 0.25;
	private static final double[][] HANGING = {{0, 0, 7, 16, 16, 9}};
	private static final double[][] BATTEN = {{0, 13, 6, 16, 16, 10}};
	private static final double[][] ROLL = {{0, 6, 5, 16, 16, 11}};

	public GasCurtainBlock(Properties properties) {
		super(properties, Block.box(0, 0, 7, 16, 16, 9), Block.box(0, 0, 7, 16, 16, 9));
		registerDefaultState(defaultBlockState().setValue(ROLLED, false));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		boolean lower = state.getValue(HALF) == DoubleBlockHalf.LOWER;
		if (state.getValue(ROLLED)) {
			return lower ? Shapes.empty() : LongDecorationBlock.shape(ROLL, state.getValue(FACING));
		}
		return LongDecorationBlock.shape(lower ? HANGING : new double[][] {HANGING[0], BATTEN[0]}, state.getValue(FACING));
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	/** Rolls it up, or lets it down: both halves together. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			boolean rolled = !state.getValue(ROLLED);
			setBoth(level, pos, state.setValue(ROLLED, rolled));
			level.playSound(null, pos, rolled ? SoundEvents.WOOL_PLACE : SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * Whether an unrolled gas curtain stands between {@code from} and {@code to}: the blocks on the straight line between
	 * them, looked at every {@value #STEP} blocks or closer, each block once.
	 */
	public static boolean shields(Level level, Vec3 from, Vec3 to) {
		Vec3 line = to.subtract(from);
		int steps = Math.max(1, (int) Math.ceil(line.length() / STEP));
		BlockPos last = null;
		for (int i = 0; i <= steps; i++) {
			BlockPos at = BlockPos.containing(from.add(line.scale((double) i / steps)));
			if (at.equals(last)) {
				continue;
			}
			last = at;
			BlockState state = level.getBlockState(at);
			if (state.getBlock() instanceof GasCurtainBlock && !state.getValue(ROLLED)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(ROLLED);
	}
}
