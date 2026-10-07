package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A Leaf Pile: fallen leaves heaped on the ground, red, orange or yellow (one block for each). Place more on it to heap
 * it higher, {@value #MAX_LAYERS} layers of {@value #LAYER_PIXELS} pixels at most, like snow. Each layer softens a fall
 * into it by {@value #SOFTENING_PER_LAYER} (a full pile by four fifths, like hay), and walking over it kicks leaves up.
 * It needs solid ground and breaks into one pile a layer.
 */
public class LeafPileBlock extends Block {
	public static final int MAX_LAYERS = 4;
	public static final int LAYER_PIXELS = 4;
	public static final float SOFTENING_PER_LAYER = 0.2F;
	/** One in this many steps over a pile kicks leaves up. */
	public static final int SCATTER_CHANCE = 4;
	public static final IntegerProperty LAYERS = IntegerProperty.create("layers", 1, MAX_LAYERS);
	private static final VoxelShape[] SHAPES = new VoxelShape[MAX_LAYERS + 1];

	static {
		for (int layers = 1; layers <= MAX_LAYERS; layers++) {
			SHAPES[layers] = Block.box(0.0, 0.0, 0.0, 16.0, layers * LAYER_PIXELS, 16.0);
		}
	}

	public LeafPileBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(LAYERS, 1));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES[state.getValue(LAYERS)];
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		BlockPos below = pos.below();
		return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighbor, RandomSource random) {
		return direction == Direction.DOWN && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
	}

	/** Another pile of the same colour heaps it higher; a single layer gives way to anything else placed there. */
	@Override
	protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
		int layers = state.getValue(LAYERS);
		if (!context.getItemInHand().is(asItem()) || layers >= MAX_LAYERS) {
			return layers == 1;
		}
		return !context.replacingClickedOnBlock() || context.getClickedFace() == Direction.UP;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState there = context.getLevel().getBlockState(context.getClickedPos());
		if (there.is(this)) {
			return there.setValue(LAYERS, Math.min(MAX_LAYERS, there.getValue(LAYERS) + 1));
		}
		BlockState state = defaultBlockState();
		return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
	}

	/** How much of a fall into a pile of {@code layers} still hurts. */
	public static float fallDamageFactor(int layers) {
		return 1.0F - SOFTENING_PER_LAYER * layers;
	}

	@Override
	public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
		entity.causeFallDamage(fallDistance, fallDamageFactor(state.getValue(LAYERS)), level.damageSources().fall());
	}

	/** Walking over the pile kicks a few leaves up, for everyone near to see. */
	@Override
	public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
		if (level instanceof ServerLevel server && server.getRandom().nextInt(SCATTER_CHANCE) == 0
				&& Math.abs(entity.getX() - entity.xOld) + Math.abs(entity.getZ() - entity.zOld) > 0.01) {
			double top = pos.getY() + state.getValue(LAYERS) * LAYER_PIXELS / 16.0;
			server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), entity.getX(), top, entity.getZ(), 4, 0.2, 0.05, 0.2, 0.05);
		}
		super.stepOn(level, pos, state, entity);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LAYERS);
	}
}
