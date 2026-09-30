package io.github.jimbozoomer.jugcraft.prospecting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The prospector's survey: samples the 3x3 chunks around the player (every second column, from the
 * bottom of the world to a little above the player) and turns what it finds into deliberately vague
 * readings: a signal strength of 1-5 per ore family, with some jitter, and a rough depth band. It never
 * says which chunk or block an ore is in; players still have to dig.
 */
public final class OreSurvey {
	/** Ore families surveyed, by c:ores/&lt;name&gt; block tag, with the item shown for each (its ore block). */
	public static final List<Family> FAMILIES = List.of(
			family("coal", "minecraft:coal_ore"), family("iron", "minecraft:iron_ore"), family("copper", "minecraft:copper_ore"),
			family("gold", "minecraft:gold_ore"), family("redstone", "minecraft:redstone_ore"), family("lapis", "minecraft:lapis_ore"),
			family("diamond", "minecraft:diamond_ore"), family("emerald", "minecraft:emerald_ore"),
			family("tin", "jugcraft:tin_ore"), family("zinc", "jugcraft:zinc_ore"), family("lead", "jugcraft:lead_ore"),
			family("silver", "jugcraft:silver_ore"), family("nickel", "jugcraft:nickel_ore"), family("tungsten", "jugcraft:tungsten_ore"),
			family("uranium", "jugcraft:uranium_ore"), family("salt", "jugcraft:salt_ore"), family("phosphate", "jugcraft:phosphate_ore"),
			family("lepidolite", "jugcraft:lepidolite_ore"), family("monazite", "jugcraft:monazite_ore"));
	/** Surveyed chunks in each direction from the player's chunk: 1 means 3x3. */
	public static final int RADIUS = 1;
	/** Only every STRIDE-th column in x and z is sampled. */
	public static final int STRIDE = 2;
	/** How far above the player the survey reaches. */
	public static final int ABOVE = 16;

	public record Family(String name, TagKey<Block> tag, Identifier icon) {
	}

	/** One reading: the ore family's icon, a signal of 1 (traces) to 5 (rich), and a depth band (0 shallow, 1 middle, 2 deep). */
	public record Reading(Identifier icon, int signal, int depth) {
	}

	private OreSurvey() {
	}

	private static Family family(String name, String icon) {
		return new Family(name, TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores/" + name)), Identifier.parse(icon));
	}

	public static List<Reading> survey(ServerLevel level, BlockPos center, RandomSource random) {
		int[] counts = new int[FAMILIES.size()];
		long[] heights = new long[FAMILIES.size()];
		int middleX = SectionPos.blockToSectionCoord(center.getX());
		int middleZ = SectionPos.blockToSectionCoord(center.getZ());
		int bottom = level.getMinY();
		int top = Math.min(level.getMaxY(), center.getY() + ABOVE);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int cx = middleX - RADIUS; cx <= middleX + RADIUS; cx++) {
			for (int cz = middleZ - RADIUS; cz <= middleZ + RADIUS; cz++) {
				for (int dx = 0; dx < 16; dx += STRIDE) {
					for (int dz = 0; dz < 16; dz += STRIDE) {
						for (int y = bottom; y <= top; y++) {
							BlockState state = level.getBlockState(pos.set((cx << 4) + dx, y, (cz << 4) + dz));
							if (state.isAir()) {
								continue;
							}
							for (int i = 0; i < FAMILIES.size(); i++) {
								if (state.is(FAMILIES.get(i).tag())) {
									counts[i]++;
									heights[i] += y;
									break;
								}
							}
						}
					}
				}
			}
		}
		List<Reading> readings = new ArrayList<>();
		for (int i = 0; i < FAMILIES.size(); i++) {
			if (counts[i] > 0) {
				int average = (int) (heights[i] / counts[i]);
				readings.add(new Reading(FAMILIES.get(i).icon(), signal(counts[i], random), average >= 40 ? 0 : average >= 0 ? 1 : 2));
			}
		}
		readings.sort((a, b) -> Integer.compare(b.signal(), a.signal()));
		return readings;
	}

	/** Sample count to a vague 1-5 signal, nudged one step either way a quarter of the time. */
	static int signal(int count, RandomSource random) {
		int signal = count < 4 ? 1 : count < 16 ? 2 : count < 48 ? 3 : count < 128 ? 4 : 5;
		if (random.nextInt(4) == 0) {
			signal += random.nextBoolean() ? 1 : -1;
		}
		return Math.max(1, Math.min(5, signal));
	}
}
