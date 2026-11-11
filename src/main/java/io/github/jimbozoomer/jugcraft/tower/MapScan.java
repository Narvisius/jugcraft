package io.github.jimbozoomer.jugcraft.tower;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import org.jspecify.annotations.Nullable;

/**
 * What the Big Map Table shows (work list item 16, client/BigMapTableRenderer): the world round its tower in miniature,
 * read from the world itself. Justin asked for "actual minecraft terrain from the world its in", the trees, the
 * topography, progress as things change, everything at its true height ("if there's a floating island ... it'll show it
 * floating"), the map zooming out as the drones go out, and, against lag, "only refresh the world like in the area that
 * it maps out ... only refresh like every 15 seconds".
 *
 * <p>So a scan covers only the round area in view, sampled about {@link #PER_VIEW} times from the middle to the edge, and starts
 * at most every {@link #REFRESH} ticks, only while someone is near the table, spread over the ticks
 * ({@link #BUDGET_NANOS} a tick at most). Each sample is a column of the world at the sample's tallest point:
 * <ul>
 * <li>where its chunk is loaded, read block by block from the top down: every run of blocks over open air is a
 *     <em>span</em> (a floating island, the tower's decks and cradles, a canopy over the ground beside its trunk), and the
 *     first run {@link #GROUND_DEPTH} thick is the ground; nothing under the ground is read (no ores, no caves). Air shut
 *     in under a run (no sky light: a cave, a room) is counted solid unless it is taller than {@link #BIG_GAP}, so caves and
 *     the tower's halls never show as holes. Each run takes the map colour of its top block, its sides that of the block
 *     under it; see-through solid blocks (glass, which has no map colour) are {@link #GLASS}.</li>
 * <li>where it is not loaded (no chunk is ever loaded or generated for the map), from the world generator's surface on a
 *     coarser lattice, interpolated, its colour from the biome there; those never change, so they are kept.</li>
 * </ul>
 * The view is the farthest drone's (the site, the land, the build radius: {@link #VIEWS}), widened until the tallest
 * thing in view stands under {@link #HEADROOM} blocks over the map (one scale across and up). A finished scan that
 * differs from the last goes to the players near the table ({@link Payload}); a player who comes near gets the last one.
 */
public final class MapScan {
	/** The views by how far the drones are out: the tower's site, the land round it, the build radius and a little more. */
	public static final double[] VIEWS = {72, 420, TowerCoreBlockEntity.BUILD_RADIUS_CHUNKS * 16 * 1.06};
	/** The map's radius on the table and the height over its floor the tallest thing may reach (client/BigMapTableRenderer). */
	public static final double MAP_R = 5.7, HEADROOM = 5.0;
	/** Samples from the middle to the edge: about this many, never more than {@link #MAX_HALF}. */
	public static final int PER_VIEW = 72, MAX_HALF = 80;
	/** A scan starts at most this often (ticks): 15 seconds. */
	public static final int REFRESH = 300;
	/** At most this long a tick on the scan. */
	public static final long BUDGET_NANOS = 500_000;
	/** Players within this distance of the table get the map (and a scan runs only while one is). */
	public static final int SEND_RANGE = 64;
	/** A run of blocks this thick (counting air shut in under it) is the ground. */
	public static final int GROUND_DEPTH = 16;
	/** Dark air (no sky light) under a run up to this tall is a cave or a room, counted solid; taller, it is open air. */
	public static final int BIG_GAP = 12;
	/** Spans a column keeps; any further down merge into the lowest. */
	public static final int MAX_SPANS = 12;
	/** Spans a map keeps (its payload stays well under a packet's limit); past that a column's further spans merge too. */
	public static final int MAX_TOTAL_SPANS = 60_000;
	/** The colour of a see-through solid block (no map colour of its own: glass). MapColor ids are 1 to 63. */
	public static final int GLASS = 0;
	/** Sub-columns along each side of a sample's square looked at for its tallest point. */
	static final int SUB = 6;
	/** The generator's lattice: every this many samples. */
	static final int LATTICE = 4;
	/** Kept generator points at most (one long and an int each). */
	static final int CACHE = 40_000;

	/** A scan: its middle, view, sample step and size, and per sample the ground and its spans. */
	public static final class Grid {
		public final BlockPos centre;
		public final double view;
		public final int step, n;
		/** Per sample (row by row from the north-west): the ground's top block y, or {@link Short#MIN_VALUE} outside the view. */
		public final short[] ground;
		public final byte[] top, side;
		/** Per sample, how many spans; the spans in sample order, highest first. */
		public final byte[] count;
		public short[] y0, y1;
		public byte[] spanTop, spanSide;
		public int spans;

		Grid(BlockPos centre, double view, int step, int n) {
			this.centre = centre.immutable();
			this.view = view;
			this.step = step;
			this.n = n;
			ground = new short[n * n];
			Arrays.fill(ground, Short.MIN_VALUE);
			top = new byte[n * n];
			side = new byte[n * n];
			count = new byte[n * n];
			y0 = new short[256];
			y1 = new short[256];
			spanTop = new byte[256];
			spanSide = new byte[256];
		}

		Grid(BlockPos centre, double view, int step, int n, short[] ground, byte[] top, byte[] side, byte[] count, short[] y0, short[] y1,
				byte[] spanTop, byte[] spanSide) {
			this.centre = centre.immutable();
			this.view = view;
			this.step = step;
			this.n = n;
			this.ground = ground;
			this.top = top;
			this.side = side;
			this.count = count;
			this.y0 = y0;
			this.y1 = y1;
			this.spanTop = spanTop;
			this.spanSide = spanSide;
			this.spans = y0.length;
		}

		void span(int sample, int bottom, int topY, int topColour, int sideColour) {
			if (count[sample] >= MAX_SPANS || spans >= MAX_TOTAL_SPANS) {
				if (count[sample] > 0) {
					y0[spans - 1] = (short) bottom; // merged into the column's lowest kept
				}
				return;
			}
			if (spans == y0.length) {
				int size = y0.length * 2;
				y0 = Arrays.copyOf(y0, size);
				y1 = Arrays.copyOf(y1, size);
				spanTop = Arrays.copyOf(spanTop, size);
				spanSide = Arrays.copyOf(spanSide, size);
			}
			y0[spans] = (short) bottom;
			y1[spans] = (short) (topY + 1);
			spanTop[spans] = (byte) topColour;
			spanSide[spans] = (byte) sideColour;
			spans++;
			count[sample]++;
		}

		void trim() {
			y0 = Arrays.copyOf(y0, spans);
			y1 = Arrays.copyOf(y1, spans);
			spanTop = Arrays.copyOf(spanTop, spans);
			spanSide = Arrays.copyOf(spanSide, spans);
		}

		/**
		 * This grid at twice the step, for a map seen from further off (client/BigMap): each sample the tallest of the three
		 * by three round it, so the tower's tops and a floating island's are kept.
		 */
		public Grid coarser() {
			int half = n / 2, h2 = half / 2, n2 = 2 * h2 + 1;
			Grid c = new Grid(centre, view, step * 2, n2);
			int[] first = new int[n * n + 1];
			for (int k = 0; k < n * n; k++) {
				first[k + 1] = first[k] + count[k];
			}
			for (int j2 = 0; j2 < n2; j2++) {
				for (int i2 = 0; i2 < n2; i2++) {
					int ci = half + 2 * (i2 - h2), cj = half + 2 * (j2 - h2);
					int best = -1, bestTop = Integer.MIN_VALUE;
					for (int dj = -1; dj <= 1; dj++) {
						for (int di = -1; di <= 1; di++) {
							int i = ci + di, j = cj + dj;
							if (i < 0 || j < 0 || i >= n || j >= n) continue;
							int k = j * n + i;
							if (ground[k] == Short.MIN_VALUE) continue;
							int h = count[k] > 0 ? Math.max(ground[k] + 1, y1[first[k]]) : ground[k] + 1;
							if (h > bestTop) {
								bestTop = h;
								best = k;
							}
						}
					}
					int k2 = j2 * n2 + i2;
					int centreK = cj * n + ci;
					if (best < 0 || ground[centreK] == Short.MIN_VALUE) continue;
					c.ground[k2] = ground[best];
					c.top[k2] = top[best];
					c.side[k2] = side[best];
					for (int s = first[best]; s < first[best + 1]; s++) {
						c.span(k2, y0[s], y1[s] - 1, spanTop[s], spanSide[s]);
					}
				}
			}
			c.trim();
			return c;
		}

		/** Sample i's world x, z. */
		public int x(int i) {
			return centre.getX() + (i % n - n / 2) * step;
		}

		public int z(int i) {
			return centre.getZ() + (i / n - n / 2) * step;
		}

		/** The lowest ground and the highest top in view (blocks), {lo, hi}; {0, 0} for an empty grid. */
		public int[] range() {
			int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE, s = 0;
			for (int i = 0; i < ground.length; i++) {
				if (ground[i] != Short.MIN_VALUE) {
					lo = Math.min(lo, ground[i]);
					hi = Math.max(hi, ground[i] + 1);
				}
				for (int k = 0; k < count[i]; k++, s++) {
					hi = Math.max(hi, y1[s]);
				}
			}
			return lo > hi ? new int[] {0, 0} : new int[] {lo, hi};
		}

		boolean same(@Nullable Grid o) {
			return o != null && o.centre.equals(centre) && o.view == view && o.step == step && o.n == n && Arrays.equals(o.ground, ground)
					&& Arrays.equals(o.top, top) && Arrays.equals(o.side, side) && Arrays.equals(o.count, count) && Arrays.equals(o.y0, y0)
					&& Arrays.equals(o.y1, y1) && Arrays.equals(o.spanTop, spanTop) && Arrays.equals(o.spanSide, spanSide);
		}
	}

	private @Nullable Grid work, done;
	private int cursor;
	private long started = Long.MIN_VALUE / 2;
	private final Set<UUID> sent = new HashSet<>();
	/** Generator points (world x, z packed) -> top y << 8 | colour id. */
	private final Map<Long, Integer> generated = new HashMap<>();
	private @Nullable BiomeResolver biomes;
	private final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();

	/** The last finished scan, or null. */
	public @Nullable Grid done() {
		return done;
	}

	/** Blocks between samples for a view: about {@link #PER_VIEW} samples to the edge, never more than {@link #MAX_HALF}. */
	public static int stepFor(double view) {
		return Math.max(1, Math.max((int) Math.round(view / PER_VIEW), (int) Math.ceil(view / MAX_HALF)));
	}

	/** The view a scan round {@code centre} takes with the drones out as far as {@code far} blocks. */
	public double view(double far) {
		double target = VIEWS[VIEWS.length - 1];
		for (double v : VIEWS) {
			if (far * 1.15 <= v) {
				target = v;
				break;
			}
		}
		// wide enough for the tallest thing in the last scan to stand under the headroom, one scale across and up
		double view = target;
		if (done != null) {
			int[] r = done.range();
			view = Math.max(view, Math.min(VIEWS[VIEWS.length - 1], (r[1] - r[0] + 1) * MAP_R / HEADROOM));
			if (Math.abs(view - done.view) < done.view * 0.03 && done.view >= target) {
				view = done.view; // no creeping by a block or two each scan
			}
		}
		return view;
	}

	/**
	 * A tick of the table at {@code table}: starts a scan round {@code centre} every {@link #REFRESH} ticks while a player
	 * is near (the drones as far as {@code far}), goes on with the one in hand, and sends finished maps.
	 */
	public void tick(ServerLevel level, BlockPos table, BlockPos centre, double far) {
		long now = level.getGameTime();
		boolean anyone = false;
		for (ServerPlayer player : level.players()) {
			if (player.blockPosition().closerThan(table, SEND_RANGE)) {
				anyone = true;
				break;
			}
		}
		if (work == null && anyone && now - started >= REFRESH) {
			double view = view(far);
			int step = stepFor(view);
			int half = (int) Math.ceil(view / step);
			work = new Grid(centre, view, step, 2 * half + 1);
			cursor = 0;
			started = now;
		}
		if (work != null) {
			scan(level);
		}
		if (now % 40 == 0 && done != null) {
			Set<UUID> near = new HashSet<>();
			for (ServerPlayer player : level.players()) {
				if (player.blockPosition().closerThan(table, SEND_RANGE)) {
					near.add(player.getUUID());
				}
			}
			sent.retainAll(near);
			for (ServerPlayer player : level.players()) {
				if (near.contains(player.getUUID()) && sent.add(player.getUUID())) {
					ServerPlayNetworking.send(player, Payload.of(table, done));
				}
			}
		}
	}

	/**
	 * Scans round {@code centre} out to {@code view} blocks at once, whoever is near and whatever it takes (the tests;
	 * tower/MapScan's own scans go a budget a tick), and keeps it as the map.
	 */
	public Grid scanNow(ServerLevel level, BlockPos centre, double view) {
		int step = stepFor(view);
		int half = (int) Math.ceil(view / step);
		work = new Grid(centre, view, step, 2 * half + 1);
		cursor = 0;
		budget = Long.MAX_VALUE;
		try {
			scan(level);
		} finally {
			budget = BUDGET_NANOS;
		}
		return done;
	}

	/** This tick's time for the scan (nanoseconds). */
	private long budget = BUDGET_NANOS;

	/** Goes on with the scan in hand for this tick's budget; finished, it is the map. */
	private void scan(ServerLevel level) {
		Grid g = work;
		long start = System.nanoTime();
		double reach = g.view + g.step * 0.5;
		while (cursor < g.n * g.n) {
			if (((cursor & 7) == 0) && System.nanoTime() - start > budget) {
				return;
			}
			int i = cursor;
			int dx = (i % g.n - g.n / 2) * g.step, dz = (i / g.n - g.n / 2) * g.step;
			if ((double) dx * dx + (double) dz * dz > reach * reach) {
				cursor++;
				continue;
			}
			if (!sample(level, g, i, start)) {
				return; // a generator point could not be worked out in this tick's budget
			}
			cursor++;
		}
		g.trim();
		work = null;
		if (!g.same(done)) {
			done = g;
			sent.clear(); // everyone near gets the new map at the next send
		}
		if (generated.size() > CACHE) {
			generated.clear();
		}
	}

	/** Sample i of the grid: false if it needs a generator point this tick has no time left for. */
	private boolean sample(ServerLevel level, Grid g, int i, long start) {
		int x = g.x(i), z = g.z(i);
		LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
		if (chunk != null) {
			// the tallest point of the sample's square stands for it (a tree, a spire)
			int bx = x, bz = z, best = Integer.MIN_VALUE;
			if (g.step == 1) {
				best = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
			} else {
				int sub = Math.min(SUB, g.step);
				for (int a = 0; a < sub; a++) {
					for (int b = 0; b < sub; b++) {
						int sx = x - g.step / 2 + (a * g.step + g.step / 2) / sub, sz = z - g.step / 2 + (b * g.step + g.step / 2) / sub;
						LevelChunk c = (sx >> 4) == (x >> 4) && (sz >> 4) == (z >> 4) ? chunk : level.getChunkSource().getChunkNow(sx >> 4, sz >> 4);
						if (c == null) continue;
						int h = c.getHeight(Heightmap.Types.WORLD_SURFACE, sx & 15, sz & 15);
						if (h > best) {
							best = h;
							bx = sx;
							bz = sz;
						}
					}
				}
			}
			LevelChunk c = (bx >> 4) == (x >> 4) && (bz >> 4) == (z >> 4) ? chunk : level.getChunkSource().getChunkNow(bx >> 4, bz >> 4);
			LevelChunk in = c == null ? chunk : c;
			read(column(level, in, c == null ? x : bx, c == null ? z : bz), best, in.getMinY(), g, i);
			return true;
		}
		// not loaded: the generator's surface on the lattice round it, interpolated
		int cell = g.step * LATTICE;
		int x0 = Math.floorDiv(x, cell) * cell, z0 = Math.floorDiv(z, cell) * cell;
		int[] p = new int[4];
		for (int k = 0; k < 4; k++) {
			Integer v = point(level, x0 + (k & 1) * cell, z0 + (k >> 1) * cell, start);
			if (v == null) {
				return false;
			}
			p[k] = v;
		}
		double fx = (x - x0) / (double) cell, fz = (z - z0) / (double) cell;
		double h = (p[0] >> 8) * (1 - fx) * (1 - fz) + (p[1] >> 8) * fx * (1 - fz) + (p[2] >> 8) * (1 - fx) * fz + (p[3] >> 8) * fx * fz;
		int nearest = p[(fx < 0.5 ? 0 : 1) + (fz < 0.5 ? 0 : 2)];
		g.ground[i] = (short) Math.round(h);
		int colour = nearest & 0xFF;
		g.top[i] = (byte) colour;
		g.side[i] = (byte) sideOf(colour);
		return true;
	}

	/** A lattice point: its top y << 8 | colour id; null if it is not worked out yet and this tick has no time left. */
	private @Nullable Integer point(ServerLevel level, int x, int z, long start) {
		LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
		if (chunk != null) {
			int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
			int colour = colour(level, chunk.getBlockState(at.set(x, y, z)), at);
			return y << 8 | (colour < 0 ? MapColor.GRASS.id : colour);
		}
		long key = BlockPos.asLong(x, 0, z);
		Integer known = generated.get(key);
		if (known != null) {
			return known;
		}
		if (System.nanoTime() - start > budget) {
			return null;
		}
		var source = level.getChunkSource();
		var generator = source.getGenerator();
		int y = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, source.randomState()) - 1;
		int sea = generator.getSeaLevel();
		int colour;
		if (y < sea) {
			colour = MapColor.WATER.id;
		} else {
			if (biomes == null) {
				biomes = generator.getBiomeSource().createCachingResolver(source.randomState());
			}
			colour = biomeColour(biomes.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z)), y, sea);
		}
		int v = y << 8 | colour;
		generated.put(key, v);
		return v;
	}

	/** The colour a biome's ground shows on a map, as near as the generator's surface lets us say. */
	static int biomeColour(Holder<Biome> biome, int y, int sea) {
		if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER) || biome.is(BiomeTags.IS_BEACH)) {
			return y <= sea ? MapColor.WATER.id : MapColor.SAND.id;
		}
		if (biome.is(BiomeTags.IS_BADLANDS)) {
			return MapColor.COLOR_ORANGE.id;
		}
		String name = biome.unwrapKey().map(k -> k.identifier().getPath()).orElse("");
		if (name.contains("desert")) {
			return MapColor.SAND.id;
		}
		if (biome.value().getBaseTemperature() < 0.15F || name.contains("snow") || name.contains("frozen") || name.contains("ice")) {
			return MapColor.SNOW.id;
		}
		if (y > sea + 90) {
			return MapColor.STONE.id;
		}
		if (biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_JUNGLE) || biome.is(BiomeTags.IS_TAIGA) || name.contains("swamp")) {
			return MapColor.PLANT.id;
		}
		return MapColor.GRASS.id;
	}

	/** The colour of a generated column's sides under its top. */
	static int sideOf(int top) {
		if (top == MapColor.GRASS.id || top == MapColor.PLANT.id) {
			return MapColor.DIRT.id;
		}
		if (top == MapColor.SNOW.id) {
			return MapColor.STONE.id;
		}
		return top;
	}

	/** A block's map colour id, {@link #GLASS} for a see-through solid block, -1 for air and what a map looks through. */
	static int colour(ServerLevel level, BlockState state, BlockPos pos) {
		if (state.isAir()) {
			return -1;
		}
		MapColor colour = state.getMapColor(level, pos);
		if (colour == MapColor.NONE) {
			return state.isSolid() ? GLASS : -1;
		}
		return colour.id;
	}

	/** A column of the world as a scan reads it, from the top down. */
	interface Column {
		/** Whether the 16 blocks of the section holding y are all air. */
		boolean emptySection(int y);

		/** The colour at y ({@link #colour}): -1 for air and what a map looks through. */
		int colour(int y);

		/** Whether the air at y has no sky light (shut in: a cave, a room). */
		boolean dark(int y);
	}

	/** A loaded chunk's column at (x, z). */
	private Column column(ServerLevel level, LevelChunk chunk, int x, int z) {
		LevelChunkSection[] sections = chunk.getSections();
		return new Column() {
			private @Nullable LevelChunkSection section(int y) {
				int index = chunk.getSectionIndex(y);
				return index >= 0 && index < sections.length ? sections[index] : null;
			}

			@Override
			public boolean emptySection(int y) {
				LevelChunkSection section = section(y);
				return section == null || section.hasOnlyAir();
			}

			@Override
			public int colour(int y) {
				LevelChunkSection section = section(y);
				return section == null ? -1 : MapScan.colour(level, section.getBlockState(x & 15, y & 15, z & 15), at.set(x, y, z));
			}

			@Override
			public boolean dark(int y) {
				return level.getBrightness(LightLayer.SKY, at.set(x, y, z)) == 0;
			}
		};
	}

	/**
	 * Reads a column from its top block {@code topY} down to {@code minY} into sample i of the grid: its spans, then the
	 * ground (see the class's comment).
	 */
	static void read(Column c, int topY, int minY, Grid g, int i) {
		int runTop = Integer.MIN_VALUE, runTopColour = 0, runSideColour = 0, runLength = 0;
		int gapTop = Integer.MIN_VALUE, gapLength = 0;
		boolean gapDark = false;
		int y = topY;
		while (y >= minY) {
			boolean empty = c.emptySection(y);
			int colour = empty ? -1 : c.colour(y);
			if (colour >= 0) {
				if (runTop == Integer.MIN_VALUE) {
					runTop = y;
					runTopColour = runSideColour = colour;
					runLength = 0;
				} else if (gapTop != Integer.MIN_VALUE) {
					// a dark gap closing within BIG_GAP (a taller one was open air and ended the run) is shut in: solid
					runLength += gapLength;
					gapTop = Integer.MIN_VALUE;
				}
				if (runLength == 1) {
					runSideColour = colour;
				}
				runLength++;
				if (runLength >= GROUND_DEPTH) {
					break;
				}
				y--;
				continue;
			}
			// air: a whole section of it at once where it is all air
			int bottom = empty ? Math.max(minY, (y >> 4) << 4) : y;
			if (runTop != Integer.MIN_VALUE) {
				if (gapTop == Integer.MIN_VALUE) {
					gapTop = y;
					gapLength = 0;
					gapDark = c.dark(y);
				}
				gapLength += y - bottom + 1;
				if (!gapDark || gapLength > BIG_GAP) {
					g.span(i, gapTop + 1, runTop, runTopColour, runSideColour);
					runTop = gapTop = Integer.MIN_VALUE;
				}
			}
			y = bottom - 1;
		}
		if (runTop == Integer.MIN_VALUE) {
			// nothing under the last span (the void): the ground is the world's floor
			g.ground[i] = (short) (minY - 1);
			g.top[i] = g.side[i] = (byte) MapColor.COLOR_BLACK.id;
			return;
		}
		g.ground[i] = (short) runTop;
		g.top[i] = (byte) runTopColour;
		g.side[i] = (byte) runSideColour;
	}

	/** Server to client: a Big Map Table's map (the table's position, then the grid). */
	public record Payload(BlockPos table, Grid grid) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(Jugcraft.id("big_map_table"));
		/** Samples across at most. */
		static final int MAX_N = 2 * MAX_HALF + 1;
		public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = StreamCodec.of(Payload::write, Payload::read);

		static Payload of(BlockPos table, Grid grid) {
			return new Payload(table.immutable(), grid);
		}

		private static void write(RegistryFriendlyByteBuf buf, Payload p) {
			Grid g = p.grid();
			buf.writeBlockPos(p.table());
			buf.writeBlockPos(g.centre);
			buf.writeDouble(g.view);
			buf.writeVarInt(g.step);
			buf.writeVarInt(g.n);
			// heights as offsets from the lowest, so each is a byte or two
			int base = Integer.MAX_VALUE;
			for (short h : g.ground) {
				if (h != Short.MIN_VALUE) base = Math.min(base, h);
			}
			for (int s = 0; s < g.spans; s++) {
				base = Math.min(base, g.y0[s]);
			}
			if (base == Integer.MAX_VALUE) base = 0;
			buf.writeVarInt(base + 4096);
			for (short h : g.ground) {
				buf.writeVarInt(h == Short.MIN_VALUE ? 0 : h - base + 1);
			}
			buf.writeBytes(g.top);
			buf.writeBytes(g.side);
			buf.writeBytes(g.count);
			buf.writeVarInt(g.spans);
			for (int s = 0; s < g.spans; s++) {
				buf.writeVarInt(g.y0[s] - base);
				buf.writeVarInt(g.y1[s] - g.y0[s]);
				buf.writeByte(g.spanTop[s]);
				buf.writeByte(g.spanSide[s]);
			}
		}

		private static Payload read(RegistryFriendlyByteBuf buf) {
			BlockPos table = buf.readBlockPos();
			BlockPos centre = buf.readBlockPos();
			double view = buf.readDouble();
			int step = Math.max(1, buf.readVarInt());
			int n = buf.readVarInt();
			if (n < 1 || n > MAX_N) {
				throw new IllegalArgumentException("a big map of " + n + " samples across");
			}
			int base = buf.readVarInt() - 4096;
			short[] ground = new short[n * n];
			for (int i = 0; i < ground.length; i++) {
				int v = buf.readVarInt();
				ground[i] = v == 0 ? Short.MIN_VALUE : (short) (v - 1 + base);
			}
			byte[] top = new byte[n * n], side = new byte[n * n], count = new byte[n * n];
			buf.readBytes(top);
			buf.readBytes(side);
			buf.readBytes(count);
			int spans = buf.readVarInt();
			int most = n * n * MAX_SPANS;
			if (spans < 0 || spans > most) {
				throw new IllegalArgumentException(spans + " spans on a big map");
			}
			short[] y0 = new short[spans], y1 = new short[spans];
			byte[] spanTop = new byte[spans], spanSide = new byte[spans];
			for (int s = 0; s < spans; s++) {
				y0[s] = (short) (buf.readVarInt() + base);
				y1[s] = (short) (y0[s] + buf.readVarInt());
				spanTop[s] = buf.readByte();
				spanSide[s] = buf.readByte();
			}
			return new Payload(table, new Grid(centre, view, step, n, ground, top, side, count, y0, y1, spanTop, spanSide));
		}

		@Override
		public Type<Payload> type() {
			return TYPE;
		}
	}

	static void register() {
		PayloadTypeRegistry.clientboundPlay().register(Payload.TYPE, Payload.CODEC);
	}
}
