package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Grasping Hands: two rotting hands clawing up out of a little mound of dirt. Step on them without sneaking and they
 * snatch at your ankles: a harmless Slowness {@value #SLOWNESS_LEVEL} for {@value #SLOW_TICKS} ticks, and a groan. The
 * hands stay up, clenched, for {@value #GRAB_TICKS} ticks ({@link Phase#GRAB}), then sink back for
 * {@value #REST_TICKS} ({@link Phase#RECOVER}) before they can grab again. Only the mound has a collision shape, so
 * you walk over them; anything living that steps on them is grabbed, players and mobs alike.
 */
public class GraspingHandsBlock extends Block {
	public static final int SLOW_TICKS = 30;
	public static final int SLOWNESS_LEVEL = 2;
	public static final int GRAB_TICKS = 15;
	public static final int REST_TICKS = 40;
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final EnumProperty<Phase> PHASE = EnumProperty.create("phase", Phase.class);
	private static final VoxelShape OUTLINE = Block.box(1.0, 0.0, 1.0, 15.0, 12.0, 15.0);
	private static final VoxelShape MOUND = Block.box(1.0, 0.0, 1.0, 15.0, 2.0, 15.0);

	/** Resting (clawing at the air), grabbing (up and clenched), or sinking back. */
	public enum Phase implements StringRepresentable {
		REST, GRAB, RECOVER;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public GraspingHandsBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PHASE, Phase.REST));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return OUTLINE;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return MOUND;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
		return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighbor, RandomSource random) {
		return direction == Direction.DOWN && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
	}

	/** Something steps on them: if they are resting and it isn't sneaking past, they grab it. */
	@Override
	public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
		super.stepOn(level, pos, state, entity);
		if (!level.isClientSide() && entity instanceof LivingEntity living && !living.isSteppingCarefully()) {
			grab(level, pos, state, living);
		}
	}

	/** Grabs {@code living} by the ankle if the hands are resting; returns whether they did. */
	public static boolean grab(Level level, BlockPos pos, BlockState state, LivingEntity living) {
		if (!(state.getBlock() instanceof GraspingHandsBlock) || state.getValue(PHASE) != Phase.REST) {
			return false;
		}
		living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SLOW_TICKS, SLOWNESS_LEVEL - 1));
		level.setBlock(pos, state.setValue(PHASE, Phase.GRAB), Block.UPDATE_ALL);
		level.scheduleTick(pos, state.getBlock(), GRAB_TICKS);
		level.playSound(null, pos, SoundEvents.ZOMBIE_AMBIENT, SoundSource.BLOCKS, 0.6F, 1.4F);
		return true;
	}

	/** Up, they sink back; sunk, they rest and can grab again. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		Phase phase = state.getValue(PHASE);
		if (phase == Phase.GRAB) {
			level.setBlock(pos, state.setValue(PHASE, Phase.RECOVER), Block.UPDATE_ALL);
			level.scheduleTick(pos, this, REST_TICKS);
		} else if (phase == Phase.RECOVER) {
			level.setBlock(pos, state.setValue(PHASE, Phase.REST), Block.UPDATE_ALL);
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
		builder.add(FACING, PHASE);
	}
}
