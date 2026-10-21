package io.github.jimbozoomer.jugcraft.world;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jspecify.annotations.Nullable;

/**
 * Builds Adam-ondi-Ahman's knolls (docs/features/adam-ondi-ahman.md) into the world, as the town is built: once every
 * chunk under a knoll and its blend ring has been newly generated (so no chunk that stood before is changed) and all of
 * them are loaded, the ground is levelled to the knoll's height over its pad and blended back to the land's own height
 * over a ring {@link #BLEND} blocks wide, and the knoll's template (tools/adam_ondi_ahman.py) is placed, turned as its
 * site's number says. A knoll is built once ({@link AdamOndiAhmanState} remembers which, and which of its chunks are new).
 *
 * <p>The knoll's height is the middle of the land's heights at a 3 x 3 grid over its pad, read from the generator's
 * noise, so it is the same whenever it is built. Nothing is loaded to build a knoll: it waits until its chunks are.
 */
public final class AltarKnolls {
	public static final Identifier TEMPLATE = Jugcraft.id("adam_ondi_ahman/altar_knoll");
	/** Keep in sync with tools/adam_ondi_ahman.py: SIZE (x and z), CENTRE and GROUND_Y (the template's ground layer). */
	public static final int SIZE = 48;
	public static final int CENTRE = 23;
	public static final int GROUND_Y = 1;
	/** Half the pad the knoll's ground is levelled over, however the template is turned (it reaches CENTRE + 1 either way). */
	public static final int HALF = 25;
	/** The ring round the pad over which the ground blends back to the land's own height. */
	public static final int BLEND = 8;
	/** How deep under the pad a hollow is filled at most, and how far above it the land is cleared at most. */
	public static final int FILL_DEPTH = 24;
	public static final int CLEAR_HEIGHT = 32;
	private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
	private static final Deque<AdamOndiAhman.Site> QUEUE = new ArrayDeque<>();
	private static final Set<Long> QUEUED = new HashSet<>();

	private AltarKnolls() {
	}

	public static void register() {
		ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> {
			if (level == level.getServer().overworld()) {
				chunkLoaded(level, chunk.getPos(), generated);
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(AltarKnolls::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			QUEUE.clear();
			QUEUED.clear();
		});
	}

	/** The chunks a knoll at this site and its blend ring cover. */
	public static List<ChunkPos> footprint(AdamOndiAhman.Site site) {
		int reach = HALF + BLEND;
		List<ChunkPos> out = new ArrayList<>();
		for (int cx = (site.x() - reach) >> 4; cx <= (site.x() + reach) >> 4; cx++) {
			for (int cz = (site.z() - reach) >> 4; cz <= (site.z() + reach) >> 4; cz++) {
				out.add(new ChunkPos(cx, cz));
			}
		}
		return out;
	}

	/** A site's key, for the saved state. */
	public static long key(AdamOndiAhman.Site site) {
		return (long) site.x() << 32 ^ (site.z() & 0xFFFFFFFFL);
	}

	/** A chunk loaded: a new one under a knoll counts towards it; any one under a knoll owed may let it be built. */
	static void chunkLoaded(ServerLevel level, ChunkPos pos, boolean generated) {
		AdamOndiAhman.Site site = AdamOndiAhman.knollAt(pos.x(), pos.z());
		if (site == null) {
			return;
		}
		AdamOndiAhmanState state = AdamOndiAhmanState.get(level);
		long key = key(site);
		if (state.built(key)) {
			return;
		}
		if (generated) {
			state.markNew(key, pos.pack());
		}
		if (QUEUED.add(key)) {
			QUEUE.add(site);
		}
	}

	private static void tick(MinecraftServer server) {
		if (QUEUE.isEmpty()) {
			return;
		}
		ServerLevel level = server.overworld();
		AdamOndiAhman.Site site = QUEUE.poll();
		long key = key(site);
		QUEUED.remove(key);
		AdamOndiAhmanState state = AdamOndiAhmanState.get(level);
		if (state.built(key) || !AdamOndiAhman.enabled()) {
			return;
		}
		List<ChunkPos> chunks = footprint(site);
		for (ChunkPos pos : chunks) {
			// Every chunk under it must be new (never one a world had before) and loaded; until then it waits.
			if (!state.isNew(key, pos.pack()) || level.getChunkSource().getChunkNow(pos.x(), pos.z()) == null) {
				return;
			}
		}
		if (build(level, site)) {
			state.markBuilt(key);
			Jugcraft.LOGGER.info("Adam-ondi-Ahman: built the knoll at {} {}", site.x(), site.z());
		}
	}

	/** The knoll's ground height (its template's ground layer) at a site, from the generator's noise. */
	public static int groundHeight(ServerLevel level, AdamOndiAhman.Site site) {
		var generator = level.getChunkSource().getGenerator();
		var random = level.getChunkSource().randomState();
		int[] heights = new int[9];
		int n = 0;
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				heights[n++] = generator.getBaseHeight(site.x() + i * (HALF - 4), site.z() + j * (HALF - 4), Heightmap.Types.OCEAN_FLOOR_WG, level,
						random);
			}
		}
		Arrays.sort(heights);
		// The first free height over the land, less one: the land's top block, where the template's ground layer goes.
		return heights[4] - 1;
	}

	/** Levels the ground under a knoll and places it; false if its template cannot be read. */
	public static boolean build(ServerLevel level, AdamOndiAhman.Site site) {
		StructureTemplate template = template(level);
		if (template == null) {
			return false;
		}
		int pad = groundHeight(level, site);
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		int reach = HALF + BLEND;
		for (int x = site.x() - reach; x <= site.x() + reach; x++) {
			for (int z = site.z() - reach; z <= site.z() + reach; z++) {
				int out = Math.max(0, Math.max(Math.abs(x - site.x()), Math.abs(z - site.z())) - HALF);
				int land = ground(level, at, x, z);
				int target = out == 0 ? pad : (int) Math.round(pad + (land - pad) * (out / (double) (BLEND + 1)));
				shape(level, at, x, z, land, target);
			}
		}
		Rotation rotation = Rotation.values()[(int) ((site.hash() >>> 60) & 3)];
		BlockPos origin = origin(site, rotation, pad - GROUND_Y);
		StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation).setKnownShape(true);
		template.placeInWorld(level, origin, origin, settings, level.getRandom(), Block.UPDATE_CLIENTS);
		return true;
	}

	/**
	 * Where the template's corner goes so that its middle (CENTRE, CENTRE) stands on the site, turned about its corner as
	 * a template is: a quarter turn clockwise takes (x, z) to (-z, x).
	 */
	public static BlockPos origin(AdamOndiAhman.Site site, Rotation rotation, int y) {
		return switch (rotation) {
			case NONE -> new BlockPos(site.x() - CENTRE, y, site.z() - CENTRE);
			case CLOCKWISE_90 -> new BlockPos(site.x() + CENTRE, y, site.z() - CENTRE);
			case CLOCKWISE_180 -> new BlockPos(site.x() + CENTRE, y, site.z() + CENTRE);
			case COUNTERCLOCKWISE_90 -> new BlockPos(site.x() - CENTRE, y, site.z() + CENTRE);
		};
	}

	/** The land's top solid block in a column (not trees or plants). */
	private static int ground(ServerLevel level, BlockPos.MutableBlockPos at, int x, int z) {
		int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
		for (int y = top; y > level.getMinY(); y--) {
			BlockState state = level.getBlockState(at.set(x, y, z));
			if (!state.getCollisionShape(level, at).isEmpty() && !state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES)) {
				return y;
			}
		}
		return level.getMinY();
	}

	/** Brings a column's ground from {@code land} to {@code target}: fills with earth under grass, or clears down to it. */
	private static void shape(ServerLevel level, BlockPos.MutableBlockPos at, int x, int z, int land, int target) {
		// Clear trees, plants and land above the target, up to the column's top.
		int top = Math.min(level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z), target + CLEAR_HEIGHT);
		for (int y = top; y > target; y--) {
			if (!level.getBlockState(at.set(x, y, z)).isAir()) {
				level.setBlock(at, Blocks.AIR.defaultBlockState(), FLAGS);
			}
		}
		// Fill a hollow under it with earth.
		for (int y = Math.max(land + 1, target - FILL_DEPTH); y < target; y++) {
			level.setBlock(at.set(x, y, z), Blocks.DIRT.defaultBlockState(), FLAGS);
		}
		if (target != land) {
			level.setBlock(at.set(x, target, z), Blocks.GRASS_BLOCK.defaultBlockState(), FLAGS);
		}
	}

	/** The knoll's template, read fresh from the data packs; null (logged) if missing or unreadable. */
	static @Nullable StructureTemplate template(ServerLevel level) {
		Identifier path = Identifier.fromNamespaceAndPath(TEMPLATE.getNamespace(), "structure/" + TEMPLATE.getPath() + ".nbt");
		try (InputStream in = level.getServer().getResourceManager().getResourceOrThrow(path).open()) {
			StructureTemplate template = new StructureTemplate();
			template.load(level.registryAccess().lookupOrThrow(Registries.BLOCK), NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap()));
			return template;
		} catch (IOException e) {
			Jugcraft.LOGGER.error("Adam-ondi-Ahman: could not read the knoll's template {}: {}", path, e.toString());
			return null;
		}
	}
}
