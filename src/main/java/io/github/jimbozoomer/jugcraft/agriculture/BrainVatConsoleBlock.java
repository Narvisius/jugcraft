package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Brain-Vat Console (Halloween decorations batch 19, the Reanimation Rig): a brass console with a brain in a glass
 * vat, two dials and a toggle, facing whoever placed it. It is an analogue memory cell: it remembers ({@link #MEMORY})
 * the strongest redstone signal fed into its back and gives it out of its front, and to a comparator, until a signal
 * arriving on either side clears it (to whatever its back holds then). The brain glows as brightly as what it
 * remembers and sparks while it remembers anything.
 */
public class BrainVatConsoleBlock extends HorizontalDirectionalBlock {
	public static final IntegerProperty MEMORY = BlockStateProperties.POWER;
	/** Whether either side has a signal (it clears as the signal comes, not while it lasts). */
	public static final BooleanProperty CLEARING = BooleanProperty.create("clearing");
	private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 15.0, 15.0);

	public BrainVatConsoleBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(MEMORY, 0).setValue(CLEARING, false));
	}

	public static int light(BlockState state) {
		return state.getValue(MEMORY) / 2;
	}

	/**
	 * What it remembers after its back reads {@code back}, with {@code clear} true when a signal has just reached a side:
	 * the stronger of what it held and its back, or after clearing only its back.
	 */
	public static int remember(int held, int back, boolean clear) {
		return clear ? back : Math.max(held, back);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	/** The signal fed into its back. */
	private static int back(Level level, BlockPos pos, Direction facing) {
		Direction back = facing.getOpposite();
		return level.getSignal(pos.relative(back), back);
	}

	private static boolean sides(Level level, BlockPos pos, Direction facing) {
		Direction left = facing.getClockWise();
		Direction right = facing.getCounterClockWise();
		return level.getSignal(pos.relative(left), left) > 0 || level.getSignal(pos.relative(right), right) > 0;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation, boolean moved) {
		super.neighborChanged(state, level, pos, neighbor, orientation, moved);
		if (level.isClientSide()) {
			return;
		}
		Direction facing = state.getValue(FACING);
		boolean side = sides(level, pos, facing);
		boolean clear = side && !state.getValue(CLEARING);
		int memory = remember(state.getValue(MEMORY), back(level, pos, facing), clear);
		if (memory == state.getValue(MEMORY) && side == state.getValue(CLEARING)) {
			return;
		}
		level.setBlock(pos, state.setValue(MEMORY, memory).setValue(CLEARING, side), Block.UPDATE_ALL);
		if (memory != state.getValue(MEMORY)) {
			level.playSound(null, pos, memory > state.getValue(MEMORY) ? SoundEvents.BEACON_POWER_SELECT : SoundEvents.BEACON_DEACTIVATE,
					SoundSource.BLOCKS, 0.4F, 1.4F + memory * 0.04F);
		}
	}

	/** Used, it says what it remembers. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.brain_vat", state.getValue(MEMORY)));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected boolean isSignalSource(BlockState state) {
		return true;
	}

	/** It gives what it remembers out of its front only. */
	@Override
	protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return direction == state.getValue(FACING).getOpposite() ? state.getValue(MEMORY) : 0;
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return state.getValue(MEMORY);
	}

	/** The brain sparks while it remembers. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		int memory = state.getValue(MEMORY);
		if (memory > 0 && random.nextInt(16) < memory) {
			level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.4, pos.getY() + 0.85,
					pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.4, 0.0, 0.02, 0.0);
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, MEMORY, CLEARING);
	}
}
