package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Chimera Finial (Halloween decorations batch 18): a winged chimera crouched on a ball finial, for a roof ridge. It is
 * a weather sensor ({@link #WEATHER}): with rain or snow falling on it a redstone signal of {@value #RAIN} to every side,
 * {@value #STORM} in a thunderstorm, and in a storm it spreads its wings. It looks at the sky once every
 * {@value #CHECK_TICKS} ticks.
 */
public class ChimeraFinialBlock extends HorizontalDirectionalBlock {
	public static final int RAIN = 7;
	public static final int STORM = 15;
	public static final int CHECK_TICKS = 20;
	public static final EnumProperty<Weather> WEATHER = EnumProperty.create("weather", Weather.class);
	private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 15.0, 13.0);

	/** What is falling on the finial. */
	public enum Weather implements StringRepresentable {
		CLEAR, RAIN, STORM;

		public int signal() {
			return switch (this) {
				case RAIN -> ChimeraFinialBlock.RAIN;
				case STORM -> ChimeraFinialBlock.STORM;
				default -> 0;
			};
		}

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public ChimeraFinialBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WEATHER, Weather.CLEAR));
	}

	/** The weather falling on the block at {@code pos}: rain or snow on it from an open sky, and thunder. */
	public static Weather weather(Level level, BlockPos pos) {
		return weather(level.isRaining(), level.isThundering(), level.canSeeSky(pos.above()),
				level.getBiome(pos).value().getPrecipitationAt(pos, level.getSeaLevel()));
	}

	/**
	 * The weather on a finial: {@code raining} and {@code thundering} in the world, {@code open} to the sky, and what its
	 * biome lets fall there. Snow counts as rain; a desert's dry sky is clear, as is anywhere under a roof.
	 */
	public static Weather weather(boolean raining, boolean thundering, boolean open, Biome.Precipitation precipitation) {
		if (!raining || !open || precipitation == Biome.Precipitation.NONE) {
			return Weather.CLEAR;
		}
		return thundering ? Weather.STORM : Weather.RAIN;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
		super.onPlace(state, level, pos, old, moved);
		if (!old.is(this)) {
			level.scheduleTick(pos, this, 1);
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		Weather now = weather(level, pos);
		if (now != state.getValue(WEATHER)) {
			level.setBlock(pos, state.setValue(WEATHER, now), Block.UPDATE_ALL);
		}
		level.scheduleTick(pos, this, CHECK_TICKS);
	}

	@Override
	protected boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return state.getValue(WEATHER).signal();
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, WEATHER);
	}
}
