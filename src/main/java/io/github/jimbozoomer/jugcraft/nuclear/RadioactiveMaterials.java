package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;

/**
 * What is radioactive without being told (no {@link JugcraftNuclear#RADIOACTIVITY} component of its own): uranium
 * and thorium (monazite) from the ore up, depleted uranium, the spent fuel in its racks and dry storage casks. Each
 * as the dose rate one block away (mSv/h), for one block or one item of it; a stack counts each item. Placed blocks
 * are found round a player by {@link #blocksNear}; the same materials carried, dropped or stored count as items.
 * <p>The numbers are game numbers, kept in proportion: natural uranium ore a faint rise over background that a
 * counter picks up through a few blocks of stone (prospecting), metal and concentrate clearly more, spent fuel
 * dangerous.
 */
public final class RadioactiveMaterials {
	private static final Map<String, Double> BLOCKS = Map.ofEntries(
			Map.entry("uranium_ore", 0.1), Map.entry("deepslate_uranium_ore", 0.1),
			Map.entry("raw_uranium_block", 0.9), Map.entry("uranium_block", 1.2),
			Map.entry("monazite_ore", 0.04), Map.entry("deepslate_monazite_ore", 0.04), Map.entry("monazite_block", 0.35),
			Map.entry("depleted_uranium_armor", 0.06), Map.entry("depleted_uranium_armor_stairs", 0.045),
			Map.entry("depleted_uranium_armor_slab", 0.03),
			// Spent fuel: the casks are heavily shielded (a little gets out); the racks' fuel is meant to be under water.
			Map.entry("dry_storage_cask", 0.4), Map.entry("spent_fuel_rack", 300.0));
	private static final Map<String, Double> ITEMS = Map.ofEntries(
			Map.entry("raw_uranium", 0.1), Map.entry("washed_uranium_ore", 0.11), Map.entry("uranium_dust", 0.12),
			Map.entry("uranium_ingot", 0.13), Map.entry("uranium_nugget", 0.015),
			Map.entry("monazite", 0.04));
	private static Map<Block, Double> blocks;
	private static Map<Item, Double> items;

	private RadioactiveMaterials() {
	}

	private static synchronized void build() {
		if (blocks != null) {
			return;
		}
		Map<Block, Double> b = new HashMap<>();
		BLOCKS.forEach((id, s) -> BuiltInRegistries.BLOCK.getOptional(Jugcraft.id(id)).ifPresent(block -> b.put(block, s)));
		Map<Item, Double> i = new HashMap<>();
		ITEMS.forEach((id, s) -> BuiltInRegistries.ITEM.getOptional(Jugcraft.id(id)).ifPresent(item -> i.put(item, s)));
		// A radioactive block carried as an item is as radioactive as the block.
		b.forEach((block, s) -> {
			Item item = block.asItem();
			if (item instanceof BlockItem) {
				i.putIfAbsent(item, s);
			}
		});
		blocks = b;
		items = i;
	}

	/** One item's radioactivity (mSv/h a block away), or 0. */
	public static double item(Item item) {
		build();
		return items.getOrDefault(item, 0.0);
	}

	/** A placed block's radioactivity (mSv/h a block away), or 0. */
	public static double block(BlockState state) {
		build();
		return blocks.getOrDefault(state.getBlock(), 0.0);
	}

	/** A radioactive block near a point: where it is and how strong. */
	public record Found(BlockPos pos, double strength) {
	}

	/**
	 * The radioactive blocks within {@code range} of a point (a cube), nearest first, at most {@code max}. Only the
	 * chunk sections that hold one are looked through, so plain ground costs next to nothing.
	 */
	public static List<Found> blocksNear(ServerLevel level, Vec3 at, int range, int max) {
		BlockPos c = BlockPos.containing(at);
		return blocksIn(level, at, c.getX() - range, c.getY() - range, c.getZ() - range, c.getX() + range, c.getY() + range, c.getZ() + range, max);
	}

	/** The radioactive blocks in a box (inclusive block bounds), nearest to {@code at} first, at most {@code max}. */
	public static List<Found> blocksIn(ServerLevel level, Vec3 at, int x0, int y0, int z0, int x1, int y1, int z1, int max) {
		build();
		List<Found> out = new ArrayList<>();
		if (blocks.isEmpty()) {
			return out;
		}
		for (int cx = x0 >> 4; cx <= x1 >> 4; cx++) {
			for (int cz = z0 >> 4; cz <= z1 >> 4; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) {
					continue;
				}
				LevelChunkSection[] sections = chunk.getSections();
				for (int sy = Math.max(y0 >> 4, level.getMinSectionY()); sy <= Math.min(y1 >> 4, level.getMaxSectionY()); sy++) {
					LevelChunkSection section = sections[level.getSectionIndexFromSectionY(sy)];
					if (section == null || section.hasOnlyAir() || !section.maybeHas(s -> blocks.containsKey(s.getBlock()))) {
						continue;
					}
					for (int y = Math.max(y0, sy << 4); y <= Math.min(y1, (sy << 4) + 15); y++) {
						for (int z = Math.max(z0, cz << 4); z <= Math.min(z1, (cz << 4) + 15); z++) {
							for (int x = Math.max(x0, cx << 4); x <= Math.min(x1, (cx << 4) + 15); x++) {
								Double s = blocks.get(section.getBlockState(x & 15, y & 15, z & 15).getBlock());
								if (s != null) {
									out.add(new Found(new BlockPos(x, y, z), s));
								}
							}
						}
					}
				}
			}
		}
		if (out.size() > max) {
			out.sort(java.util.Comparator.comparingDouble(f -> Vec3.atCenterOf(f.pos()).distanceToSqr(at)));
			return new ArrayList<>(out.subList(0, max));
		}
		return out;
	}
}
