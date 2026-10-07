package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
 * A Weathervane: a black iron vane on a pole over the four compass points (which always point north, east, south and
 * west), a bat or a witch on a broomstick on its tail (one block each). It stands on a roof, a post or a fence. The
 * vane turns to point into the wind ({@link #wind}): the same wind everywhere in the world, slowly coming round over
 * the days, so every vane in a town points the same way; in a storm it swings about. Turning is drawn by the client
 * (client/WeathervaneRenderer.java), at most {@value #TURN_SPEED} degrees a tick.
 */
public class WeathervaneBlock extends BaseEntityBlock {
	public static final float TURN_SPEED = 3.0F;
	/** How far a storm (and rain) swings the vane about the wind, in degrees. */
	public static final float STORM_SWING = 35.0F;
	public static final float RAIN_SWING = 10.0F;
	private static final VoxelShape SHAPE = Block.box(6.0, 0.0, 6.0, 10.0, 16.0, 10.0);

	private final String design;

	public WeathervaneBlock(Properties properties, String design) {
		super(properties);
		this.design = design;
	}

	/** bat or witch. */
	public String design() {
		return design;
	}

	/**
	 * Where the wind comes from at {@code time} (ticks, with the partial tick): a yaw in degrees from 0 to 360, 0 from
	 * the north and 90 from the east. It comes round slowly over the days, with a gentler swing over hours.
	 */
	public static float wind(double time) {
		double t = time / 24000.0;
		double yaw = 180.0 + 150.0 * Math.sin(t * Math.PI * 2 / 3.0) + 60.0 * Math.sin(t * Math.PI * 2 * 2.3 + 1.7)
				+ 12.0 * Math.sin(time / 610.0 + 0.4);
		return (float) Mth.positiveModulo(yaw, 360.0);
	}

	/** How far from the wind the vane swings in this weather at {@code time} (degrees). */
	public static float gust(double time, float rain, float thunder) {
		return (float) (RAIN_SWING * rain * Math.sin(time / 9.0) + STORM_SWING * thunder * Math.sin(time / 4.3 + 1.1) * Math.cos(time / 13.0));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DecorationBlockEntity(JugcraftAgriculture.WEATHERVANE_ENTITY, pos, state);
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

	/** It stands on anything that holds a torch up: a roof, a post, a fence or a wall. */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return Block.canSupportCenter(level, pos.below(), Direction.UP);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighbor, RandomSource random) {
		return direction == Direction.DOWN && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
	}
}
