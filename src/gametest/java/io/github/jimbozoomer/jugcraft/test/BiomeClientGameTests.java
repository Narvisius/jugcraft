package io.github.jimbozoomer.jugcraft.test;

import com.mojang.datafixers.util.Pair;
import io.github.jimbozoomer.jugcraft.agriculture.SeasonalLeavesBlock;
import io.github.jimbozoomer.jugcraft.biome.JugcraftDimensions;
import io.github.jimbozoomer.jugcraft.biome.JugcraftRegions;
import io.github.jimbozoomer.jugcraft.season.JugcraftSeasons;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The biomes branch in a real, normally generated world (seed "jugcraft"): how far from the start each Jugcraft biome is
 * (logged), that their trees' seasonal leaves are generated in today's look, and screenshots of those found,
 * seen from above the treetops where the biome is on the surface.
 */
public class BiomeClientGameTests implements FabricClientGameTest {
	private static final Logger LOGGER = LoggerFactory.getLogger("jugcraft-test");
	private static final String SEED = "jugcraft";
	/** How far to look for each biome, in blocks. */
	private static final int SEARCH = 6400;
	/**
	 * At least this share of the Jugcraft biomes must be within SEARCH blocks of the start (many, such as the wetlands,
	 * grow only in one of four region layouts and in small climates).
	 */
	private static final double FOUND_SHARE = 0.5;
	/** Biomes to photograph, if found. */
	private static final String[] SHOTS = {"maple_woods", "seasonal_forest", "aspen_glade", "coniferous_forest", "dead_forest", "tundra", "muskeg",
			"field", "flower_meadow", "grassland", "heathland", "lavender_field", "lush_grassland", "prairie", "shrubland", "steppe",
			"bog", "dead_swamp", "lush_swamp", "swamp_woods", "bayou", "floodplain", "ghost_forest", "sludge_mire", "lush_river", "fen",
			"lake_district", "quagmire", "marsh", "wetland",
			"dryland", "xeric_shrubland", "jacaranda_glade", "lush_desert", "bone_flats", "dry_river", "cold_desert", "scrubland",
			"lush_savanna", "outback", "oasis", "wasteland", "burnt_forest", "mediterranean_forest", "orchard",
			"rainforest", "eucalyptus_forest", "tropics", "subtropics", "dense_forest", "redwood_forest", "temperate_rainforest", "woodland",
			"volcano", "canyon", "highland", "basin", "shield", "karst_pinnacles", "hot_springs", "ice_sheet", "ocean_trench", "gravel_beach",
			"dune_beach", "overgrown_beach", "flower_isle",
			"cinder_barrens", "elder_vale", "frostlight_garden", "gilded_shrubland", "glimmer_grove", "gloomweald", "hallowed_bog",
			"highsun_meadow", "mycelial_jungle", "shrine_springs", "snowpetal_grove", "starlit_wood", "toadstool_field", "webwood",
			"wild_greens"};
	/** Seasonal leaves are counted within this many blocks (east-west and north-south) of each biome found. */
	private static final int LEAF_REACH = 24;
	/** How far below the top block of a column to look for leaves. */
	private static final int LEAF_DEPTH = 40;
	/**
	 * The search finds a biome at any height (its nearest sample may be underground, under another biome); screenshots
	 * and leaf counts use the nearest column within this many blocks whose surface has the biome.
	 */
	private static final int SURFACE_REACH = 64;
	private static final int SURFACE_STEP = 8;
	/** A spot is preferred where the surface 16 blocks to each side has the biome too. */
	private static final int SURFACE_MARGIN = 16;
	/**
	 * And where the camera looks: the screenshot faces north-west (yaw 135) and down, so the middle of the picture is
	 * about this many blocks west and north of the spot.
	 */
	private static final int SURFACE_VIEW = 20;
	/** Nether and End shots: how far from the biome's nearest place to look for a standing spot, and how finely. */
	private static final int STAND_REACH = 384;
	private static final int STAND_STEP = 8;
	/** At most this many columns are generated in looking for one. */
	private static final int STAND_TRIES = 32;
	/** Blocks of air the camera must have ahead of it, diagonally to the north-west at eye level. */
	private static final int STAND_VIEW = 4;
	/** Nether and End shares: every Jugcraft biome must turn up in this square round the origin, sampled this finely. */
	private static final int SAMPLE_REACH = 4096;
	private static final int SAMPLE_STEP = 64;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().adjustSettings(creator -> {
			creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
			creator.setWorldType(new WorldCreationUiState.WorldTypeEntry(creator.getSettings().worldgenLoadContext()
					.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL)));
			creator.setSeed(SEED);
		}).create()) {
			singleplayer.getConnection().waitForChunksRender();
			TestServerContext server = singleplayer.getServer();
			server.runCommand("gamerule sendCommandFeedback false");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			BlockPos start = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
			List<String> found = new ArrayList<>();
			List<BlockPos> places = new ArrayList<>();
			List<ResourceKey<Biome>> keys = new ArrayList<>();
			for (ResourceKey<Biome> biome : JugcraftRegions.biomes()) {
				BlockPos place = server.computeOnServer(minecraft -> {
					Pair<BlockPos, Holder<Biome>> nearest = minecraft.overworld().findClosestBiome3d(holder -> holder.is(biome), start,
							SEARCH, 64, 64);
					return nearest == null ? null : nearest.getFirst();
				});
				String name = biome.identifier().getPath();
				LOGGER.info("Biomes, seed {}: {} {}", SEED, name, place == null ? "not within " + SEARCH + " blocks"
						: String.format(Locale.ROOT, "at %d %d (%d blocks from the start)", place.getX(), place.getZ(),
								(int) Math.sqrt(place.distSqr(start.atY(place.getY())))));
				if (place != null) {
					found.add(name);
					places.add(place);
					keys.add(biome);
				}
			}
			// Vanilla regions keep the biomes the Jugcraft layout replaces.
			List<String> vanillaMissing = new ArrayList<>();
			for (ResourceKey<Biome> biome : List.of(Biomes.TAIGA, Biomes.FOREST, Biomes.BIRCH_FOREST)) {
				BlockPos place = server.computeOnServer(minecraft -> {
					Pair<BlockPos, Holder<Biome>> nearest = minecraft.overworld().findClosestBiome3d(holder -> holder.is(biome), start,
							SEARCH, 64, 64);
					return nearest == null ? null : nearest.getFirst();
				});
				LOGGER.info("Biomes, seed {}: vanilla {} {}", SEED, biome.identifier().getPath(), place == null ? "not within " + SEARCH
						+ " blocks" : "at " + place.getX() + " " + place.getZ());
				if (place == null) {
					vanillaMissing.add(biome.identifier().getPath());
				}
			}
			// Logged only: how near the climates are that the rarer Jugcraft biomes replace (hot lands, mangroves, jungles,
			// old-growth taigas), to tell a biome missing near the start from one the regions never place.
			for (ResourceKey<Biome> biome : List.of(Biomes.DESERT, Biomes.BADLANDS, Biomes.SAVANNA_PLATEAU, Biomes.MANGROVE_SWAMP,
					Biomes.JUNGLE, Biomes.OLD_GROWTH_PINE_TAIGA)) {
				BlockPos place = server.computeOnServer(minecraft -> {
					Pair<BlockPos, Holder<Biome>> nearest = minecraft.overworld().findClosestBiome3d(holder -> holder.is(biome), start,
							SEARCH, 64, 64);
					return nearest == null ? null : nearest.getFirst();
				});
				LOGGER.info("Biomes, seed {}: climate check, vanilla {} {}", SEED, biome.identifier().getPath(), place == null
						? "not within " + SEARCH + " blocks" : String.format(Locale.ROOT, "at %d %d (%d blocks from the start)", place.getX(),
								place.getZ(), (int) Math.sqrt(place.distSqr(start.atY(place.getY())))));
			}
			List<BlockPos> spots = new ArrayList<>();
			for (int i = 0; i < found.size(); i++) {
				BlockPos place = places.get(i);
				ResourceKey<Biome> biome = keys.get(i);
				BlockPos spot = server.computeOnServer(minecraft -> surfaceSpot(minecraft.overworld(), place, biome));
				String view = spot == null ? "" : server.computeOnServer(minecraft -> viewBiome(minecraft.overworld(), spot));
				LOGGER.info("Biomes, seed {}: {} on the surface {}", SEED, found.get(i), spot == null ? "not within " + SURFACE_REACH
						+ " blocks of where it was found; using that place" : "at " + spot.getX() + " " + spot.getY() + " " + spot.getZ()
						+ " (the camera looks at " + view + ")");
				spots.add(spot == null ? place : spot);
			}
			// The chunks around each biome found are generated now, far from the player, so no random tick has turned
			// their leaves: the seasonal_leaves tree decorator must have generated them in today's look.
			int leaves = 0;
			int stale = 0;
			for (int i = 0; i < found.size(); i++) {
				BlockPos spot = spots.get(i);
				int[] counts = server.computeOnServer(minecraft -> leafLooks(minecraft.overworld(), spot));
				LOGGER.info("Biomes, seed {}: {} generated seasonal leaves green {}, gold {}, bare {}; {} not in today's look (day {})",
						SEED, found.get(i), counts[0], counts[1], counts[2], counts[3], JugcraftSeasons.today());
				leaves += counts[0] + counts[1] + counts[2];
				stale += counts[3];
			}
			for (String shot : SHOTS) {
				int index = found.indexOf(shot);
				if (index < 0) {
					continue;
				}
				BlockPos place = spots.get(index);
				int ground = server.computeOnServer(minecraft -> {
					ServerLevel level = minecraft.overworld();
					level.getChunk(place.getX() >> 4, place.getZ() >> 4);
					return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, place.getX(), place.getZ());
				});
				int y = ground + 14;
				server.runCommand("setblock %d %d %d minecraft:barrier".formatted(place.getX(), y - 1, place.getZ()));
				server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 %d %d.5 135 30", place.getX(), y, place.getZ()));
				context.waitTicks(60);
				singleplayer.getConnection().waitForChunksRender();
				context.takeScreenshot("jugcraft_biome_" + shot);
			}
			// The Nether and End biomes (batches 8 and 9): how often each turns up over a square round the origin (every
			// Jugcraft one must), and each where it is nearest the origin, from a standing spot on its floor, looking out.
			List<String> dimensionMissing = new ArrayList<>();
			for (ResourceKey<Level> dimension : List.of(Level.NETHER, Level.END)) {
				List<ResourceKey<Biome>> biomes = dimension == Level.NETHER ? JugcraftDimensions.nether() : JugcraftDimensions.end();
				Map<String, Integer> sampled = server.computeOnServer(minecraft -> sampleBiomes(minecraft.getLevel(dimension)));
				LOGGER.info("Biomes, seed {}: {} biomes every {} blocks over {} blocks round the origin: {}", SEED,
						dimension.identifier().getPath(), SAMPLE_STEP, SAMPLE_REACH, sampled);
				for (ResourceKey<Biome> biome : biomes) {
					if (!sampled.containsKey(biome.identifier().toString())) {
						dimensionMissing.add(biome.identifier().toString());
					}
				}
				for (ResourceKey<Biome> biome : biomes) {
					BlockPos spot = server.computeOnServer(minecraft -> standingSpot(minecraft.getLevel(dimension), biome));
					String name = biome.identifier().getPath();
					if (spot == null) {
						continue;
					}
					LOGGER.info("Biomes, seed {}: {} in {} standing at {} {} {}", SEED, name, dimension.identifier().getPath(),
							spot.getX(), spot.getY(), spot.getZ());
					server.runCommand(String.format(Locale.ROOT, "execute in %s run tp @p %d.5 %d %d.5 135 15", dimension.identifier(),
							spot.getX(), spot.getY(), spot.getZ()));
					context.waitTicks(80);
					singleplayer.getConnection().waitForChunksRender();
					context.takeScreenshot("jugcraft_biome_" + name);
				}
			}
			if (!dimensionMissing.isEmpty()) {
				throw new AssertionError("Not sampled within " + SAMPLE_REACH + " blocks of the origin: " + dimensionMissing);
			}
			if (!vanillaMissing.isEmpty()) {
				throw new AssertionError("Vanilla " + vanillaMissing + " not within " + SEARCH + " blocks of the start (seed " + SEED + ")");
			}
			if (leaves == 0 || stale > 0) {
				throw new AssertionError(stale + " of " + leaves + " generated seasonal leaves near the biomes found are not in today's look");
			}
			if (found.size() < JugcraftRegions.biomes().size() * FOUND_SHARE) {
				throw new AssertionError("Only " + found.size() + " of " + JugcraftRegions.biomes().size() + " Jugcraft biomes within "
						+ SEARCH + " blocks of the start (seed " + SEED + "): " + found);
			}
		}
	}

	/**
	 * The column nearest {@code place} (within {@link #SURFACE_REACH} blocks, every {@link #SURFACE_STEP}) whose surface
	 * has {@code biome}. Preferred, in order: one inside the biome (the biome {@link #SURFACE_MARGIN} blocks to each side
	 * and where the camera looks) on dry ground; one inside the biome; any. Null if none.
	 */
	private static BlockPos surfaceSpot(ServerLevel level, BlockPos place, ResourceKey<Biome> biome) {
		List<int[]> offsets = new ArrayList<>();
		for (int dx = -SURFACE_REACH; dx <= SURFACE_REACH; dx += SURFACE_STEP) {
			for (int dz = -SURFACE_REACH; dz <= SURFACE_REACH; dz += SURFACE_STEP) {
				offsets.add(new int[] {dx, dz});
			}
		}
		offsets.sort(Comparator.comparingInt(offset -> offset[0] * offset[0] + offset[1] * offset[1]));
		BlockPos inside = null;
		BlockPos edge = null;
		for (int[] offset : offsets) {
			int x = place.getX() + offset[0];
			int z = place.getZ() + offset[1];
			if (!surfaceIs(level, x, z, biome)) {
				continue;
			}
			BlockPos spot = surface(level, x, z);
			if (surfaceIs(level, x + SURFACE_MARGIN, z, biome) && surfaceIs(level, x - SURFACE_MARGIN, z, biome)
					&& surfaceIs(level, x, z + SURFACE_MARGIN, biome) && surfaceIs(level, x, z - SURFACE_MARGIN, biome)
					&& surfaceIs(level, x - SURFACE_VIEW, z - SURFACE_VIEW, biome)) {
				if (level.getFluidState(spot.below()).isEmpty()) {
					return spot;
				}
				if (inside == null) {
					inside = spot;
				}
			}
			if (edge == null) {
				edge = spot;
			}
		}
		return inside != null ? inside : edge;
	}

	/**
	 * In a Nether or End level, a spot to photograph the biome from. From the biome's nearest place to the origin, the
	 * columns within {@link #STAND_REACH} blocks (every {@link #STAND_STEP}) that have the biome, nearest first, found by
	 * the biome lookup alone, which generates no chunk: the first {@link #STAND_TRIES} are generated and searched from
	 * below the Nether's roof down for a solid floor in the biome with three blocks of air to stand in, no fluid beside
	 * them, and air ahead at eye level where the camera looks (north-west). Null if none; why is logged.
	 */
	private static BlockPos standingSpot(ServerLevel level, ResourceKey<Biome> biome) {
		String where = biome.identifier().getPath() + " in " + level.dimension().identifier().getPath();
		Pair<BlockPos, Holder<Biome>> nearest = level.findClosestBiome3d(holder -> holder.is(biome), BlockPos.ZERO.atY(64), SEARCH, 32, 32);
		if (nearest == null) {
			LOGGER.info("Biomes, seed {}: {} not within {} blocks of the origin", SEED, where, SEARCH);
			return null;
		}
		BlockPos place = nearest.getFirst();
		List<int[]> offsets = new ArrayList<>();
		for (int dx = -STAND_REACH; dx <= STAND_REACH; dx += STAND_STEP) {
			for (int dz = -STAND_REACH; dz <= STAND_REACH; dz += STAND_STEP) {
				offsets.add(new int[] {dx, dz});
			}
		}
		offsets.sort(Comparator.comparingInt(offset -> offset[0] * offset[0] + offset[1] * offset[1]));
		int top = Math.min(level.getMaxY() - 1, level.getMinY() + 120);
		int tries = 0;
		for (int[] offset : offsets) {
			int x = place.getX() + offset[0];
			int z = place.getZ() + offset[1];
			if (!level.getBiome(new BlockPos(x, place.getY(), z)).is(biome)) {
				continue;
			}
			if (++tries > STAND_TRIES) {
				break;
			}
			level.getChunk(x >> 4, z >> 4);
			for (int y = top; y > level.getMinY() + 4; y--) {
				BlockPos feet = new BlockPos(x, y, z);
				if (level.getBlockState(feet.below()).isSolid() && roomToStand(level, feet) && level.getBiome(feet).is(biome)) {
					return feet;
				}
			}
		}
		LOGGER.info("Biomes, seed {}: {} nearest the origin at {} {} {}; no standing room with a view in the {} columns of it searched",
				SEED, where, place.getX(), place.getY(), place.getZ(), Math.min(tries, STAND_TRIES));
		return null;
	}

	/** Three blocks of air at {@code feet}, no fluid beside them to flow in, and air ahead of the eyes to the north-west. */
	private static boolean roomToStand(ServerLevel level, BlockPos feet) {
		for (int up = 0; up < 3; up++) {
			BlockPos at = feet.above(up);
			if (!level.getBlockState(at).isAir()) {
				return false;
			}
			for (Direction side : Direction.Plane.HORIZONTAL) {
				if (!level.getFluidState(at.relative(side)).isEmpty()) {
					return false;
				}
			}
		}
		for (int ahead = 1; ahead <= STAND_VIEW; ahead++) {
			if (!level.getBlockState(feet.above().offset(-ahead, 0, -ahead)).isAir()) {
				return false;
			}
		}
		return true;
	}

	/** How often each biome is found at y 64 every {@link #SAMPLE_STEP} blocks over a square round the origin (biome lookups only). */
	private static Map<String, Integer> sampleBiomes(ServerLevel level) {
		Map<String, Integer> counts = new TreeMap<>();
		for (int x = -SAMPLE_REACH; x <= SAMPLE_REACH; x += SAMPLE_STEP) {
			for (int z = -SAMPLE_REACH; z <= SAMPLE_REACH; z += SAMPLE_STEP) {
				level.getBiome(new BlockPos(x, 64, z)).unwrapKey().ifPresent(key -> counts.merge(key.identifier().toString(), 1, Integer::sum));
			}
		}
		return counts;
	}

	/** The biome on the surface where the camera looks from {@code spot}. */
	private static String viewBiome(ServerLevel level, BlockPos spot) {
		return level.getBiome(surface(level, spot.getX() - SURFACE_VIEW, spot.getZ() - SURFACE_VIEW)).unwrapKey()
				.map(key -> key.identifier().toString()).orElse("?");
	}

	private static BlockPos surface(ServerLevel level, int x, int z) {
		level.getChunk(x >> 4, z >> 4);
		return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
	}

	private static boolean surfaceIs(ServerLevel level, int x, int z, ResourceKey<Biome> biome) {
		return level.getBiome(surface(level, x, z)).is(biome);
	}

	/**
	 * Seasonal leaves within {@link #LEAF_REACH} blocks of {@code place} (generating the chunks): how many are green,
	 * gold and bare, and how many are not in today's look.
	 */
	private static int[] leafLooks(ServerLevel level, BlockPos place) {
		int day = JugcraftSeasons.today();
		int[] counts = new int[4];
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = place.getX() - LEAF_REACH; x <= place.getX() + LEAF_REACH; x++) {
			for (int z = place.getZ() - LEAF_REACH; z <= place.getZ() + LEAF_REACH; z++) {
				level.getChunk(x >> 4, z >> 4);
				int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
				for (int y = top; y > top - LEAF_DEPTH; y--) {
					BlockState state = level.getBlockState(pos.set(x, y, z));
					if (state.getBlock() instanceof SeasonalLeavesBlock seasonal) {
						SeasonalLeavesBlock.Foliage look = state.getValue(SeasonalLeavesBlock.SEASON);
						counts[look.ordinal()]++;
						if (look != seasonal.schedule().on(day, pos)) {
							counts[3]++;
						}
					}
				}
			}
		}
		return counts;
	}
}
