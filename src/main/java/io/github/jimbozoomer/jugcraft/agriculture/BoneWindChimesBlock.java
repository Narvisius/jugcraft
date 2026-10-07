package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Bone Wind Chimes: {@value #BONES} hollow bones and a little skull hung on string from a wooden disc, under a block
 * (a porch roof, a beam, a branch). They swing in the wind, {@value #CALM_SWING} degrees on a still day, more in the
 * rain and up to {@value #STORM_SWING} in a thunderstorm, and clack together now and then: more often and louder as
 * the weather worsens. Both the swinging and the clacking are each client's own (client/BoneWindChimesRenderer.java and
 * {@link #animateTick}); the server only keeps the block.
 */
public class BoneWindChimesBlock extends BaseEntityBlock {
	public static final int BONES = 5;
	public static final float CALM_SWING = 4.0F;
	public static final float RAIN_SWING = 12.0F;
	public static final float STORM_SWING = 28.0F;
	/** One time in this many that the client looks at the chimes, they clack: still, in rain, in a storm. */
	public static final int CALM_CHANCE = 12;
	public static final int RAIN_CHANCE = 4;
	public static final int STORM_CHANCE = 1;
	/** The notes the bones clack on (note-block pitches, a pentatonic scale). */
	private static final float[] NOTES = {0.749154F, 0.840896F, 1.0F, 1.122462F, 1.259921F};
	private static final VoxelShape SHAPE = Block.box(4.0, 2.0, 4.0, 12.0, 16.0, 12.0);

	public BoneWindChimesBlock(Properties properties) {
		super(properties);
	}

	/** How far (degrees) the bones swing, for how hard it rains and thunders (each 0 to 1). */
	public static float swing(float rain, float thunder) {
		return CALM_SWING + (RAIN_SWING - CALM_SWING) * rain + (STORM_SWING - RAIN_SWING) * thunder;
	}

	/** One time in how many the chimes clack, in this weather. */
	public static int chance(boolean raining, boolean thundering) {
		return thundering ? STORM_CHANCE : raining ? RAIN_CHANCE : CALM_CHANCE;
	}

	/** How loud they clack, in this weather. */
	public static float volume(boolean raining, boolean thundering) {
		return thundering ? 1.0F : raining ? 0.6F : 0.3F;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DecorationBlockEntity(JugcraftAgriculture.WIND_CHIMES_ENTITY, pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState();
		return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
	}

	/** They hang from the block above. */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return Block.canSupportCenter(level, pos.above(), Direction.DOWN);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighbor, RandomSource random) {
		return direction == Direction.UP && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
	}

	/** A clack of bone on bone, now and then: more often and louder in rain and storms. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		boolean raining = level.isRaining();
		boolean thundering = level.isThundering();
		if (random.nextInt(chance(raining, thundering)) == 0) {
			level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, SoundEvents.NOTE_BLOCK_XYLOPHONE.value(), SoundSource.BLOCKS,
					volume(raining, thundering), NOTES[random.nextInt(NOTES.length)], false);
		}
	}
}
