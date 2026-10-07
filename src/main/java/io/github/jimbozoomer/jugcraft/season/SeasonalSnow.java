package io.github.jimbozoomer.jugcraft.season;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Opt-in winter snow ({@code seasons.snow=on}). While it snows (winter, and raining), a few random surface spots
 * round each player get a layer of seasonal snow, up to {@code seasons.snow_depth} layers, in biomes tagged
 * {@code #jugcraft:has_winter_snow} where vanilla would otherwise rain. Work is bounded: {@link #TRIES} spots per
 * player per tick, only in loaded chunks. In spring the snow melts layer by layer (SeasonalSnowBlock).
 *
 * <p>It never freezes water, never covers farmland, paths or other part blocks (snow needs a full top face), never
 * touches vanilla snow and never places snow where vanilla's own snow already falls.
 */
public final class SeasonalSnow {
	public static final String ID = "seasonal_snow";
	/** Snow attempts per player per tick, within {@link #RADIUS} blocks. */
	public static final int TRIES = 2;
	public static final int RADIUS = 48;

	public static Block BLOCK;

	private SeasonalSnow() {
	}

	static void register() {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Jugcraft.id(ID));
		BLOCK = Registry.register(BuiltInRegistries.BLOCK, key,
				new SeasonalSnowBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SNOW).setId(key)));
	}

	/** Lays winter snow round every player in levels where it is raining (bounded per tick). */
	static void tick(MinecraftServer server, int depth) {
		if (!SeasonState.serverSnowing()) {
			return;
		}
		for (ServerLevel level : server.getAllLevels()) {
			if (!level.isRaining() || !level.dimensionType().hasSkyLight()) {
				continue;
			}
			for (ServerPlayer player : level.players()) {
				for (int i = 0; i < TRIES; i++) {
					int x = player.getBlockX() + level.getRandom().nextInt(2 * RADIUS + 1) - RADIUS;
					int z = player.getBlockZ() + level.getRandom().nextInt(2 * RADIUS + 1) - RADIUS;
					BlockPos column = new BlockPos(x, level.getMinY(), z);
					if (level.hasChunkAt(column)) {
						snowAt(level, level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, column), depth);
					}
				}
			}
		}
	}

	/**
	 * One step of winter snow at {@code pos} (the open block above the ground): a first layer, or one more layer up
	 * to {@code depth}. False when nothing changed: not snowing, not a winter-snow biome, a place where vanilla snows
	 * anyway, too much block light, no full face to lie on, or something else in the way.
	 */
	public static boolean snowAt(ServerLevel level, BlockPos pos, int depth) {
		if (!SeasonState.serverSnowing()) {
			return false;
		}
		Holder<Biome> biome = level.getBiome(pos);
		if (!SeasonalBiome.of(biome.value()).jugcraft$hasWinterSnow() || !biome.value().warmEnoughToRain(pos, level.getSeaLevel())
				|| level.getBrightness(LightLayer.BLOCK, pos) >= 10) {
			return false;
		}
		BlockState state = level.getBlockState(pos);
		if (state.is(BLOCK)) {
			int layers = state.getValue(SnowLayerBlock.LAYERS);
			if (layers >= depth) {
				return false;
			}
			level.setBlockAndUpdate(pos, state.setValue(SnowLayerBlock.LAYERS, layers + 1));
			return true;
		}
		BlockState snow = BLOCK.defaultBlockState();
		if (!state.isAir() || !snow.canSurvive(level, pos)) {
			return false;
		}
		level.setBlockAndUpdate(pos, snow);
		return true;
	}
}
