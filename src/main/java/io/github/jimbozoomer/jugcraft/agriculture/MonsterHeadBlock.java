package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Monster's Head (the haunted house's props): a stitched monster's great green head with a bolt each side of its
 * neck, facing whoever placed it. Give it a redstone signal and it wakes with a groan: its jaw drops, its eyes glow
 * (light {@value #LIGHT}) and sparks crackle at its bolts (one tick in {@value #SPARK_CHANCE}). It sleeps again when
 * the signal goes.
 */
public class MonsterHeadBlock extends HorizontalDirectionalBlock {
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	public static final int LIGHT = 6;
	public static final int SPARK_CHANCE = 3;
	/** The bolts' heads: how far out to each side of the middle, how far forward, and how high (pixels). */
	private static final double BOLT_OUT = 5.1;
	private static final double BOLT_FORWARD = 0.2;
	private static final double BOLT_Y = 2.6;
	private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

	public MonsterHeadBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
	}

	/** Its light: its eyes glow while it is awake. */
	public static int light(BlockState state) {
		return state.getValue(POWERED) ? LIGHT : 0;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
				.setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation, boolean moved) {
		super.neighborChanged(state, level, pos, neighbor, orientation, moved);
		if (level instanceof ServerLevel server) {
			boolean powered = level.hasNeighborSignal(pos);
			if (powered != state.getValue(POWERED)) {
				level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
				if (powered) {
					server.playSound(null, pos, SoundEvents.ZOMBIE_AMBIENT, SoundSource.BLOCKS, 0.9F, 0.55F);
				}
			}
		}
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(POWERED) || random.nextInt(SPARK_CHANCE) != 0) {
			return;
		}
		Direction facing = state.getValue(FACING);
		Direction side = facing.getClockWise();
		double out = (random.nextBoolean() ? BOLT_OUT : -BOLT_OUT) / 16;
		double x = pos.getX() + 0.5 + side.getStepX() * out + facing.getStepX() * BOLT_FORWARD / 16;
		double z = pos.getZ() + 0.5 + side.getStepZ() * out + facing.getStepZ() * BOLT_FORWARD / 16;
		level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, pos.getY() + BOLT_Y / 16, z, side.getStepX() * out * 2, 0.05, side.getStepZ() * out * 2);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, POWERED);
	}
}
