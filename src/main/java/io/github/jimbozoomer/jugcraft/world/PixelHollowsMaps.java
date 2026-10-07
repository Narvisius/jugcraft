package io.github.jimbozoomer.jugcraft.world;

import com.mojang.datafixers.util.Pair;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/**
 * Finding the Pixel Hollows for the map. Server-only and bounded: the game's own biome search (the one behind
 * {@code /locate biome}) samples the biome noise only, loading or generating no chunks, in columns {@link #STEP}
 * blocks apart out to {@link #RADIUS} blocks, every {@link #VERTICAL_STEP} blocks of height, starting at the
 * biome's depth. At most (2 * 32 + 1)^2 = 4,225 columns of 12 samples. It runs only when a player uses a map, and
 * then off the server thread (PixelHollowsMapItem).
 */
public final class PixelHollowsMaps {
	/** Keep in sync with MAP_SEARCH in tools/pixel_hollows.py. */
	public static final int RADIUS = 2048;
	public static final int STEP = 64;
	public static final int VERTICAL_STEP = 32;
	public static final int START_Y = -16;

	private PixelHollowsMaps() {
	}

	/** The nearest Pixel Hollows in reach, or empty when there is none (or the biome cannot generate here at all). */
	public static Optional<BlockPos> find(ServerLevel level, BlockPos origin) {
		if (!JugcraftConfig.isFeatureEnabled(PixelHollows.FEATURE)) {
			return Optional.empty();
		}
		BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
		if (source.possibleBiomes().stream().noneMatch(biome -> biome.is(PixelHollows.BIOME))) {
			return Optional.empty();
		}
		Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(biome -> biome.is(PixelHollows.BIOME),
				new BlockPos(origin.getX(), START_Y, origin.getZ()), RADIUS, STEP, VERTICAL_STEP);
		return found == null ? Optional.empty() : Optional.of(found.getFirst());
	}

	/** A filled explorer map centred on the target, marked with the Pixel Hollows marker and named with its depth. */
	public static ItemStack map(ServerLevel level, BlockPos target) {
		ItemStack map = MapItem.create(level, target.getX(), target.getZ(), (byte) 2, true, true);
		MapItem.renderBiomePreviewMap(level, map);
		MapItemSavedData.addTargetDecoration(map, target, "+", RetroTrader.MAP_MARKER);
		map.set(DataComponents.ITEM_NAME, Component.translatable("filled_map.jugcraft.pixel_hollows", target.getY()));
		return map;
	}
}
