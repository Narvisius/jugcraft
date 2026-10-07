package io.github.jimbozoomer.jugcraft.prospecting;

import java.util.ArrayList;
import io.github.jimbozoomer.jugcraft.chemistry.OilReservoirs;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
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
			family("uranium", "jugcraft:uranium_ore"), family("titanium", "jugcraft:titanium_ore"), family("thallite", "jugcraft:thallite_ore"),
			family("salt", "jugcraft:salt_ore"), family("phosphate", "jugcraft:phosphate_ore"),
			family("lepidolite", "jugcraft:lepidolite_ore"), family("monazite", "jugcraft:monazite_ore"));
	/** Surveyed chunks in each direction from the player's chunk: 1 means 3x3. */
	public static final int RADIUS = 1;
	/** Only every STRIDE-th column in x and z is sampled. */
	public static final int STRIDE = 2;
	/** How far above the player the survey reaches. */
	public static final int ABOVE = 16;

	public record Family(String name, TagKey<Block> tag, Identifier icon) {
	}

	/**
	 * One reading: the ore family's icon, a signal of 1 (traces) to 5 (rich), a depth band (0 shallow, 1 middle, 2 deep),
	 * and a translation key for its name, or "" to name it after the icon.
	 */
	public record Reading(Identifier icon, int signal, int depth, String label) {
		public Reading(Identifier icon, int signal, int depth) {
			this(icon, signal, depth, "");
		}
	}

	/** Oil readings use the crude oil bucket as their icon. */
	public static final Identifier OIL_ICON = Identifier.fromNamespaceAndPath("jugcraft", "crude_oil_bucket");

	private OreSurvey() {
	}

	private static Family family(String name, String icon) {
		return new Family(name, TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores/" + name)), Identifier.parse(icon));
	}

	public static List<Reading> survey(ServerLevel level, BlockPos center, RandomSource random) {
		return survey(level, center, random, RADIUS, STRIDE);
	}

	/**
	 * A survey of {@code radius} chunks in each direction, sampling every {@code stride}-th column (batch 38: the survey
	 * rocket's wider, coarser look). Chunks that are not loaded are skipped, never loaded or generated.
	 */
	public static List<Reading> survey(ServerLevel level, BlockPos center, RandomSource random, int radius, int stride) {
		int[] counts = new int[FAMILIES.size()];
		long[] heights = new long[FAMILIES.size()];
		int middleX = SectionPos.blockToSectionCoord(center.getX());
		int middleZ = SectionPos.blockToSectionCoord(center.getZ());
		int bottom = level.getMinY();
		int top = Math.min(level.getMaxY(), center.getY() + ABOVE);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int cx = middleX - radius; cx <= middleX + radius; cx++) {
			for (int cz = middleZ - radius; cz <= middleZ + radius; cz++) {
				if (!level.hasChunk(cx, cz)) {
					continue;
				}
				for (int dx = 0; dx < 16; dx += stride) {
					for (int dz = 0; dz < 16; dz += stride) {
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
		oil(level, middleX, middleZ, radius, random, readings);
		readings.sort((a, b) -> Integer.compare(b.signal(), a.signal()));
		return readings;
	}

	/**
	 * Oil reservoirs under the surveyed chunks (see {@link OilReservoirs}): one reading for pumpable oil (middle depth)
	 * and one for shale oil (deep), from how much is left, as vaguely as the ores.
	 */
	private static void oil(ServerLevel level, int middleX, int middleZ, int radius, RandomSource random, List<Reading> readings) {
		long conventional = 0;
		long shale = 0;
		for (int cx = middleX - radius; cx <= middleX + radius; cx++) {
			for (int cz = middleZ - radius; cz <= middleZ + radius; cz++) {
				if (!level.hasChunk(cx, cz)) {
					continue;
				}
				OilReservoirs.Reservoir reservoir = OilReservoirs.get(level, new ChunkPos(cx, cz));
				switch (reservoir.kind()) {
					case CONVENTIONAL -> conventional += reservoir.remaining();
					case SHALE -> shale += reservoir.remaining();
					default -> {
					}
				}
			}
		}
		if (conventional > 0) {
			readings.add(new Reading(OIL_ICON, oilSignal(conventional, random), 1, "prospector.jugcraft.oil"));
		}
		if (shale > 0) {
			readings.add(new Reading(OIL_ICON, oilSignal(shale, random), 2, "prospector.jugcraft.shale_oil"));
		}
	}

	/** Millibuckets left to a vague 1-5 signal, nudged like the ores' signals. */
	static int oilSignal(long mb, RandomSource random) {
		int signal = mb < 50_000 ? 1 : mb < 150_000 ? 2 : mb < 400_000 ? 3 : mb < 1_000_000 ? 4 : 5;
		if (random.nextInt(4) == 0) {
			signal += random.nextBoolean() ? 1 : -1;
		}
		return Math.max(1, Math.min(5, signal));
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
