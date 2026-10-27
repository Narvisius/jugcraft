package io.github.jimbozoomer.jugcraft.creatures.herobrine;

import io.github.jimbozoomer.jugcraft.finance.LandRegistry;
import io.github.jimbozoomer.jugcraft.town.Town;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

/**
 * Herobrine's sky islands (docs/features/herobrine-sky.md): rare floating landmarks, each a rock of weathered stone
 * carrying a haunted building with rooms and a way through it. There are six: a ruined castle, a manor in a dead forest,
 * a hanging monastery with two islands joined by bridges, a fortress on an upside-down mountain, a castle shattered into
 * five drifting pieces, and a watching keep. They only add blocks, into empty sky, and are built by
 * {@link HerobrineMarks} like any mark (recorded, rechecked block by block, undoable by an operator, never removed
 * otherwise).
 *
 * <p>A plan is made only where the island's whole volume, with {@value #MARGIN} blocks round it, is loaded (checked
 * before any block is read), unclaimed, outside the town and empty air; at least {@value #CLEARANCE} blocks above
 * anything standing below it; at least {@value #HEADROOM} blocks under the top of the world; and, through
 * {@link #find}, no nearer than {@value #SPACING} blocks to another sky island. Every design is drawn from a seed fixed by
 * its column and kind, so the same place always gives the same island.
 *
 * <p>Blocks whose shape depends on their neighbours are kept so that undo can tell them from a player's change: stairs
 * never meet at corners, and fences and iron bars are placed after everything beside them and never side by side
 * (glass panes, which the record allows to join, are the only exception).
 */
public final class HerobrineSkyIslands {
	/** Kinds of sky island, as operators name them. */
	public static final String RUINED_CASTLE = "ruined_castle";
	public static final String DEAD_FOREST_MANOR = "dead_forest_manor";
	public static final String HANGING_MONASTERY = "hanging_monastery";
	public static final String INVERTED_MOUNTAIN_FORTRESS = "inverted_mountain_fortress";
	public static final String SHATTERED_CASTLE = "shattered_castle";
	public static final String WATCHING_KEEP = "watching_keep";
	public static final List<String> VARIANTS = List.of(RUINED_CASTLE, DEAD_FOREST_MANOR, HANGING_MONASTERY, INVERTED_MOUNTAIN_FORTRESS,
			SHATTERED_CASTLE, WATCHING_KEEP);

	/** How far round an island the sky must be empty, in blocks. */
	public static final int MARGIN = HerobrineMarkPlans.MARGIN;
	/** The least open air between an island's lowest block and anything below it or its margin, in blocks. */
	public static final int CLEARANCE = 24;
	/** The least room between an island's margin and the top of the world, in blocks. */
	public static final int HEADROOM = 32;
	/** No two sky islands nearer than this on either axis, in blocks. */
	public static final int SPACING = 1024;
	/** The most blocks one island may place. */
	public static final int MAX_STEPS = 15000;
	/** How many sites {@link #find} weighs, and how far from where it is asked (blocks). */
	public static final int SITE_TRIES = 4;
	public static final int NEAR_MIN = 48;
	public static final int NEAR_MAX = 112;
	/** How much higher than it must, at most, {@link #find} raises an island, in blocks. */
	public static final int LIFT = 16;

	private static final Palette SOIL = Palette.of(Blocks.MOSS_BLOCK, Blocks.MOSS_BLOCK, Blocks.COARSE_DIRT, Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT);
	private static final Palette DEAD_SOIL = Palette.of(Blocks.COARSE_DIRT, Blocks.COARSE_DIRT, Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT);
	private static final Palette BARE = Palette.of(Blocks.STONE, Blocks.ANDESITE, Blocks.TUFF, Blocks.COARSE_DIRT, Blocks.MOSS_BLOCK);
	private static final Palette ROCK = Palette.of(Blocks.STONE, Blocks.STONE, Blocks.STONE, Blocks.ANDESITE, Blocks.ANDESITE, Blocks.TUFF,
			Blocks.COBBLESTONE);
	private static final Palette DEEP = Palette.of(Blocks.COBBLED_DEEPSLATE, Blocks.COBBLED_DEEPSLATE, Blocks.TUFF, Blocks.STONE);
	private static final Palette DARK_ROCK = Palette.of(Blocks.COBBLED_DEEPSLATE, Blocks.TUFF, Blocks.BLACKSTONE, Blocks.STONE);
	private static final Palette DARKEST = Palette.of(Blocks.BLACKSTONE, Blocks.COBBLED_DEEPSLATE, Blocks.COBBLED_DEEPSLATE);
	private static final Palette BRICK = Palette.of(Blocks.STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS,
			Blocks.MOSSY_STONE_BRICKS);
	private static final Palette DARK_BRICK = Palette.of(Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_BRICKS, Blocks.CRACKED_DEEPSLATE_BRICKS,
			Blocks.POLISHED_BLACKSTONE_BRICKS);
	static final Palette RUBBLE = Palette.of(Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.MOSSY_COBBLESTONE);
	static final Palette TIMBER = Palette.of(Blocks.DARK_OAK_PLANKS);
	private static final Direction[] SIDES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

	private HerobrineSkyIslands() {
	}

	/** {@link #plan(ServerLevel, BlockPos, String, boolean)}, keeping its distance from other sky islands. */
	public static HerobrineMarkPlans.@Nullable Plan plan(ServerLevel level, BlockPos centre, String variant) {
		return plan(level, centre, variant, true);
	}

	/**
	 * The island of kind {@code variant} whose top stands at {@code centre}, or null unless every rule holds (see the
	 * class comment). {@code spacing} false skips only the distance to other sky islands (tests).
	 */
	public static HerobrineMarkPlans.@Nullable Plan plan(ServerLevel level, BlockPos centre, String variant, boolean spacing) {
		Shape shape = design(variant, centre);
		if (shape == null || shape.size() > MAX_STEPS) {
			return null;
		}
		BlockPos min = shape.min();
		BlockPos max = shape.max();
		// Every chunk it could read is loaded before any block is read.
		if (!HerobrineMarkPlans.loaded(level, min, max) || spacing && tooNear(level, centre) || !clearSky(level, min, max)) {
			return null;
		}
		LinkedHashMap<BlockPos, BlockState> blocks = shape.finish(level);
		List<HerobrineMarkPlans.Step> steps = new ArrayList<>(blocks.size());
		for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
			steps.add(new HerobrineMarkPlans.Step(entry.getKey(), level.getBlockState(entry.getKey()).getBlock(), entry.getValue()));
		}
		return new HerobrineMarkPlans.Plan(HerobrineMarkPlans.Type.SKY_ISLAND, centre, steps, shape.signs(), List.of());
	}

	/**
	 * A rare sky island near {@code near}: up to {@value #SITE_TRIES} sites, {@value #NEAR_MIN}–{@value #NEAR_MAX}
	 * blocks away in random directions and of random kinds, each as high as its clearance needs and up to {@value #LIFT}
	 * higher; the first that {@link #plan(ServerLevel, BlockPos, String)} accepts. Null if none.
	 */
	public static HerobrineMarkPlans.@Nullable Plan find(ServerLevel level, BlockPos near) {
		RandomSource random = level.getRandom();
		for (int attempt = 0; attempt < SITE_TRIES; attempt++) {
			String variant = VARIANTS.get(random.nextInt(VARIANTS.size()));
			double angle = random.nextDouble() * Math.PI * 2.0;
			int distance = NEAR_MIN + random.nextInt(NEAR_MAX - NEAR_MIN + 1);
			int x = near.getX() + (int) Math.round(Math.cos(angle) * distance);
			int z = near.getZ() + (int) Math.round(Math.sin(angle) * distance);
			Integer top = lowestTop(level, x, z, variant);
			if (top == null) {
				continue;
			}
			HerobrineMarkPlans.Plan plan = plan(level, new BlockPos(x, top + random.nextInt(LIFT + 1), z), variant);
			if (plan != null) {
				return plan;
			}
		}
		return null;
	}

	/**
	 * The lowest height the top of a {@code variant} island over column {@code x}, {@code z} may stand at: its lowest
	 * block {@value #CLEARANCE} blocks above everything under it and its margin. Null for an unknown kind, or where that
	 * ground is not all loaded (nothing is read then).
	 */
	public static @Nullable Integer lowestTop(ServerLevel level, int x, int z, String variant) {
		Shape shape = design(variant, new BlockPos(x, 0, z));
		if (shape == null) {
			return null;
		}
		BlockPos min = shape.min();
		BlockPos max = shape.max();
		if (!HerobrineMarkPlans.loaded(level, min, max)) {
			return null;
		}
		int ground = level.getMinY();
		for (int cx = min.getX() - MARGIN; cx <= max.getX() + MARGIN; cx++) {
			for (int cz = min.getZ() - MARGIN; cz <= max.getZ() + MARGIN; cz++) {
				ground = Math.max(ground, level.getHeight(Heightmap.Types.MOTION_BLOCKING, cx, cz));
			}
		}
		return ground + CLEARANCE - min.getY();
	}

	/**
	 * Whether the sky from {@code min} to {@code max}, with {@value #MARGIN} blocks round it on every side, may take an
	 * island: inside the world's height with {@value #HEADROOM} to spare, every chunk unclaimed, no column the town's at
	 * any height ({@link #protectedTownColumn}), the ground and anything built on it {@value #CLEARANCE} blocks below,
	 * and every block empty air. The chunks must already be known to be loaded.
	 */
	static boolean clearSky(ServerLevel level, BlockPos min, BlockPos max) {
		int x0 = min.getX() - MARGIN;
		int x1 = max.getX() + MARGIN;
		int y0 = min.getY() - MARGIN;
		int y1 = max.getY() + MARGIN;
		int z0 = min.getZ() - MARGIN;
		int z1 = max.getZ() + MARGIN;
		if (y1 > level.getMaxY() - HEADROOM || y0 <= level.getMinY()) {
			return false;
		}
		for (int cx = x0 >> 4; cx <= x1 >> 4; cx++) {
			for (int cz = z0 >> 4; cz <= z1 >> 4; cz++) {
				if (LandRegistry.isClaimed(level, new BlockPos(cx << 4, y0, cz << 4))) {
					return false;
				}
			}
		}
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				if (protectedTownColumn(level, new BlockPos(x, y0, z))
						|| level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) > min.getY() - CLEARANCE) {
					return false;
				}
			}
		}
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int y = y0; y <= y1; y++) {
			for (int x = x0; x <= x1; x++) {
				for (int z = z0; z <= z1; z++) {
					if (!level.getBlockState(pos.set(x, y, z)).isAir()) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Whether the column of {@code pos} (its x and z; its height is ignored) is the town's protected ground, at any
	 * height. The town's own protection reaches only from {@value Town#PROTECT_BELOW} below to {@value Town#PROTECT_ABOVE}
	 * above its ground, so the sky over it is asked at the town's height: no sky island stands over the town, however
	 * high. False when there is no town.
	 */
	public static boolean protectedTownColumn(ServerLevel level, BlockPos pos) {
		BlockPos origin = Town.origin(level);
		return origin != null && Town.isProtected(level, new BlockPos(pos.getX(), origin.getY(), pos.getZ()));
	}

	/** Whether a sky island stands (finished) or is rising within {@value #SPACING} blocks of {@code centre}. */
	private static boolean tooNear(ServerLevel level, BlockPos centre) {
		String type = HerobrineMarkPlans.Type.SKY_ISLAND.name().toLowerCase(Locale.ROOT);
		return HerobrineMarks.get(level.getServer()).marks().entrySet().stream()
				.anyMatch(entry -> entry.getValue().type().equals(type) && (entry.getValue().finished() || HerobrineMarks.building(entry.getKey()))
						&& Math.abs(entry.getValue().at().getX() - centre.getX()) < SPACING
						&& Math.abs(entry.getValue().at().getZ() - centre.getZ()) < SPACING);
	}

	/** The drawing of a {@code variant} island whose top centre is {@code centre}; null for an unknown kind. */
	static @Nullable Shape design(String variant, BlockPos centre) {
		long seed = centre.getX() * 341873128712L ^ centre.getZ() * 132897987541L ^ variant.hashCode() * 0x9E3779B97F4A7C15L;
		Shape shape = new Shape(centre, Rotation.values()[(int) Math.floorMod(seed >>> 7, 4L)], seed);
		switch (variant) {
			case RUINED_CASTLE -> ruinedCastle(shape);
			case DEAD_FOREST_MANOR -> deadForestManor(shape);
			case HANGING_MONASTERY -> hangingMonastery(shape);
			case INVERTED_MOUNTAIN_FORTRESS -> invertedMountainFortress(shape);
			case SHATTERED_CASTLE -> shatteredCastle(shape);
			case WATCHING_KEEP -> watchingKeep(shape);
			default -> {
				return null;
			}
		}
		return shape;
	}

	// The six islands. Each is drawn round its top centre, (0, 0, 0), with its front (gate or door) towards +z.

	/** A castle of weathered stone brick: crumbling curtain walls with a gate, four corner towers, and a two-storey keep. */
	private static void ruinedCastle(Shape s) {
		rock(s, 0, 0, 0, 15, 10, false, SOIL, ROCK, DEEP);
		int w = 9;
		s.walls(-w, 1, -w, w, 5, w, BRICK);
		crenels(s, -w, -w, w, w, 6, BRICK);
		s.clear(-1, 1, w, 1, 3, w);
		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sz = -1; sz <= 1; sz += 2) {
				// One tower has fallen to half its height.
				int height = sx < 0 && sz < 0 ? 6 : 9;
				tower(s, sx * w, 0, sz * w, height, BRICK, -sx * 2, -sz, 0, sz, sz > 0 ? Direction.NORTH : Direction.SOUTH);
			}
		}
		keep(s, -5, -8, 5, -2, BRICK);
		// Weather: the curtain walls' tops and the battlements crumble (never what holds a ladder, stair or floor);
		// rubble lies in the courtyard.
		weatherCurtain(s, w, 5, 6, 0.3);
		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sz = -1; sz <= 1; sz += 2) {
				int top = sx < 0 && sz < 0 ? 6 : 9;
				s.crumble(sx * w - 2, top + 1, sz * w - 2, sx * w + 2, top + 1, sz * w + 2, 0.35);
			}
		}
		s.crumble(-5, 11, -8, 5, 11, -2, 0.3);
		for (int i = 0; i < 16; i++) {
			int x = s.random.nextInt(2 * w - 3) - (w - 2);
			int z = s.random.nextInt(w + 1) - 1;
			if (!s.has(x, 1, z) && s.has(x, 0, z) && Math.abs(x) > 1) {
				s.set(x, 1, z, RUBBLE);
			}
		}
		s.sign(0, 1, 5, Direction.SOUTH, List.of("HE KEPT", "THE KEYS", "", ""));
	}

	/** A dark-timber manor of two storeys and an attic among dead trees, on a bare, dead island. */
	private static void deadForestManor(Shape s) {
		rock(s, 0, 0, 0, 16, 9, false, DEAD_SOIL, ROCK, DEEP);
		// A cobble course, plank walls framed in dark oak, a beam at the upper floor.
		s.walls(-6, 1, -4, 6, 1, 4, RUBBLE);
		s.walls(-6, 2, -4, 6, 4, 4, TIMBER);
		s.walls(-6, 6, -4, 6, 8, 4, TIMBER);
		for (int x = -6; x <= 6; x++) {
			s.set(x, 5, -4, log(Blocks.DARK_OAK_LOG, Direction.Axis.X));
			s.set(x, 5, 4, log(Blocks.DARK_OAK_LOG, Direction.Axis.X));
		}
		for (int z = -3; z <= 3; z++) {
			s.set(-6, 5, z, log(Blocks.DARK_OAK_LOG, Direction.Axis.Z));
			s.set(6, 5, z, log(Blocks.DARK_OAK_LOG, Direction.Axis.Z));
		}
		for (int x : new int[] {-6, -2, 2, 6}) {
			for (int z : new int[] {-4, 4}) {
				for (int y = 1; y <= 8; y++) {
					s.set(x, y, z, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
				}
			}
		}
		for (int z : new int[] {-4, 4}) {
			for (int y = 1; y <= 8; y++) {
				s.set(-6, y, z, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
				s.set(6, y, z, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
			}
		}
		s.fill(-5, 5, -3, 5, 5, 3, TIMBER);
		s.fill(-5, 9, -3, 5, 9, 3, TIMBER);
		for (int x = -6; x <= 6; x++) {
			s.set(x, 9, -4, log(Blocks.DARK_OAK_LOG, Direction.Axis.X));
			s.set(x, 9, 4, log(Blocks.DARK_OAK_LOG, Direction.Axis.X));
		}
		gableRoof(s, -7, 7, 4, 9, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_SLAB, -6, 6);
		// The door, and windows of old glass, some broken.
		s.clear(0, 1, 4, 0, 2, 4);
		for (int x : new int[] {-4, 4}) {
			for (int y : new int[] {2, 3, 6, 7}) {
				windowPane(s, x, y, 4);
				windowPane(s, x, y, -4);
			}
		}
		for (int y : new int[] {2, 3, 6, 7}) {
			windowPane(s, -6, y, 0);
			windowPane(s, 6, y, 0);
		}
		// A chimney up the back.
		for (int y = 1; y <= 15; y++) {
			s.set(4, y, -5, RUBBLE);
		}
		// Stairs up along the back wall to the upper floor, and a parting wall below.
		for (int k = 0; k < 4; k++) {
			s.set(-5 + k, 1 + k, -3, stairs(Blocks.DARK_OAK_STAIRS, Direction.EAST));
			s.clear(-5 + k, 5, -3, -5 + k, 5, -3);
		}
		s.fill(1, 1, -3, 1, 4, 3, TIMBER);
		s.clear(1, 1, 0, 1, 2, 0);
		// The rooms: a table with candles, shelves, a rug, a skull, cobwebs, two lanterns.
		s.late(3, 1, 1, Blocks.DARK_OAK_FENCE.defaultBlockState());
		s.fixture(3, 2, 1, candles(2, true), Direction.DOWN);
		s.fill(5, 1, -3, 5, 2, -2, Palette.of(Blocks.BOOKSHELF));
		s.fill(5, 6, -3, 5, 7, -1, Palette.of(Blocks.BOOKSHELF));
		for (int x = -3; x <= -1; x++) {
			for (int z = 1; z <= 2; z++) {
				s.fixture(x, 6, z, Blocks.CARPET.gray().defaultBlockState(), Direction.DOWN);
			}
		}
		s.fixture(3, 6, 2, skull(Direction.SOUTH), null);
		hang(s, -1, 5, 0, 0);
		hang(s, 2, 9, 0, 0);
		s.set(5, 4, 3, Blocks.COBWEB.defaultBlockState());
		s.set(-5, 8, 3, Blocks.COBWEB.defaultBlockState());
		s.set(5, 8, -3, Blocks.COBWEB.defaultBlockState());
		// Dead trees round it, bare of leaves, and dead bushes.
		for (int i = 0; i < 9; i++) {
			int x = s.random.nextInt(27) - 13;
			int z = s.random.nextInt(27) - 13;
			if (Math.abs(x) <= 8 && Math.abs(z) <= 6 || x * x + z * z > 13 * 13 || !s.has(x, 0, z)) {
				continue;
			}
			deadTree(s, x, z);
		}
		for (int i = 0; i < 14; i++) {
			int x = s.random.nextInt(29) - 14;
			int z = s.random.nextInt(29) - 14;
			if ((Math.abs(x) > 7 || Math.abs(z) > 5) && s.has(x, 0, z) && !s.has(x, 1, z)) {
				s.fixture(x, 1, z, Blocks.DEAD_BUSH.defaultBlockState(), Direction.DOWN);
			}
		}
		s.sign(1, 1, 6, Direction.SOUTH, List.of("WHO LEFT", "THE DOOR", "OPEN", ""));
	}

	/**
	 * A monastery on a deep rock, a cell on each of two smaller islands joined to it by bridges hung with chains, and,
	 * down a ladder through the rock, a shrine hanging beneath it.
	 */
	private static void hangingMonastery(Shape s) {
		rock(s, 0, 0, 0, 10, 12, false, SOIL, ROCK, DEEP);
		rock(s, 18, -3, 0, 5, 6, false, SOIL, ROCK, DEEP);
		rock(s, -18, 3, 0, 5, 6, false, SOIL, ROCK, DEEP);
		// The hall: stone below, dark timber above, a steep roof.
		s.walls(-5, 1, -4, 5, 3, 4, BRICK);
		s.walls(-5, 4, -4, 5, 7, 4, TIMBER);
		for (int x : new int[] {-5, 0, 5}) {
			for (int z : new int[] {-4, 4}) {
				for (int y = 4; y <= 7; y++) {
					s.set(x, y, z, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
				}
			}
		}
		for (int x = -5; x <= 5; x++) {
			s.set(x, 8, -4, log(Blocks.DARK_OAK_LOG, Direction.Axis.X));
			s.set(x, 8, 4, log(Blocks.DARK_OAK_LOG, Direction.Axis.X));
		}
		gableRoof(s, -6, 6, 4, 8, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_SLAB, -5, 5);
		s.clear(0, 1, 4, 0, 2, 4);
		s.clear(5, 1, 0, 5, 2, 0);
		s.clear(-5, 1, 0, -5, 2, 0);
		for (int x : new int[] {-3, 3}) {
			windowPane(s, x, 5, 4);
			windowPane(s, x, 6, 4);
			windowPane(s, x, 5, -4);
			windowPane(s, x, 6, -4);
		}
		// Pews facing the altar; the altar with candles; a skull; lanterns from the ridge.
		for (int z : new int[] {1, 3}) {
			for (int x : new int[] {-3, -2, 2, 3}) {
				s.set(x, 1, z, stairs(Blocks.DARK_OAK_STAIRS, Direction.SOUTH));
			}
		}
		s.set(0, 1, -2, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
		s.fixture(0, 2, -2, candles(3, true), Direction.DOWN);
		s.fixture(-1, 1, -3, skull(Direction.SOUTH), null);
		hang(s, 0, 12, 0, 3);
		s.set(4, 3, -3, Blocks.COBWEB.defaultBlockState());
		s.set(-4, 3, -3, Blocks.COBWEB.defaultBlockState());
		// Bridges to the cells, railed with posts and chains, hung beneath with chains.
		bridge(s, 6, 0, 13, -3);
		bridge(s, -6, 0, -13, 3);
		cell(s, 18, -3, -1, List.of("WE PRAYED", "FOR HIM", "TO LEAVE", ""));
		cell(s, -18, 3, 1, List.of("HE SAT", "WITH US", "AT VESPERS", ""));
		// A ladder down through the rock, and on under it beside a pillar, to the hanging shrine.
		int bottom = s.bottom(2, -2);
		int platform = bottom - 6;
		for (int y = 0; y >= platform + 1; y--) {
			s.clear(2, y, -2, 2, y, -2);
			if (!s.has(2, y, -3)) {
				s.set(2, y, -3, BRICK);
			}
			s.fixture(2, y, -2, ladder(Direction.SOUTH), Direction.NORTH);
		}
		s.fill(0, platform, -4, 4, platform, 0, Palette.of(Blocks.POLISHED_BLACKSTONE_BRICKS));
		s.set(2, platform, -3, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
		for (int x : new int[] {0, 4}) {
			for (int z : new int[] {-4, 0}) {
				for (int y = s.bottom(x, z) - 1; y > platform; y--) {
					s.set(x, y, z, Blocks.IRON_CHAIN.defaultBlockState());
				}
			}
		}
		s.fixture(1, platform + 1, -4, candles(2, true), Direction.DOWN);
		s.fixture(3, platform + 1, -4, skull(Direction.SOUTH), null);
		s.sign(3, platform + 1, -1, Direction.SOUTH, List.of("HE LISTENS", "FROM", "BELOW", ""));
	}

	/** A fortress of dark brick on the flat top of an upside-down mountain: walls, corner towers, and a tall inner tower. */
	private static void invertedMountainFortress(Shape s) {
		rock(s, 0, 0, 0, 13, 24, true, BARE, DARK_ROCK, DARKEST);
		for (int i = 0; i < 6; i++) {
			int x = s.random.nextInt(9) - 4;
			int z = s.random.nextInt(9) - 4;
			int bottom = s.bottom(x, z);
			for (int k = 1, length = 2 + s.random.nextInt(5); k <= length; k++) {
				s.set(x, bottom - k, z, DARKEST);
			}
		}
		int w = 8;
		s.walls(-w, 1, -w, w, 6, w, DARK_BRICK);
		crenels(s, -w, -w, w, w, 7, DARK_BRICK);
		s.clear(-1, 1, w, 1, 3, w);
		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sz = -1; sz <= 1; sz += 2) {
				tower(s, sx * w, 0, sz * w, 9, DARK_BRICK, -sx * 2, -sz, 0, sz, sz > 0 ? Direction.NORTH : Direction.SOUTH);
			}
		}
		// The inner tower: three floors and a roof, a ladder up the west wall, arrow slits.
		s.walls(-3, 1, -3, 3, 18, 3, DARK_BRICK);
		for (int y : new int[] {6, 12, 18}) {
			s.fill(-2, y, -2, 2, y, 2, y == 18 ? DARK_BRICK : TIMBER);
			s.clear(-2, y, 0, -2, y, 0);
		}
		crenels(s, -3, -3, 3, 3, 19, DARK_BRICK);
		s.clear(0, 1, 3, 0, 2, 3);
		for (int y = 1; y <= 18; y++) {
			s.fixture(-2, y, 0, ladder(Direction.EAST), Direction.WEST);
		}
		for (int y : new int[] {4, 9, 15}) {
			slit(s, 0, y, -3);
			slit(s, 3, y, 0);
			if (y > 4) {
				slit(s, 0, y, 3);
			}
		}
		for (int y : new int[] {6, 12, 18}) {
			hang(s, 1, y, 1, 0);
		}
		// Skulls on stakes in the courtyard.
		for (int x : new int[] {-5, 5}) {
			s.late(x, 1, 5, Blocks.DARK_OAK_FENCE.defaultBlockState());
			s.fixture(x, 2, 5, skull(Direction.SOUTH), null);
		}
		weatherCurtain(s, w, 6, 7, 0.2);
		s.sign(0, 1, 6, Direction.SOUTH, List.of("THE MOUNTAIN", "FELL", "UPWARD", ""));
	}

	/** A castle broken into five drifting pieces: a hall's corner, a tower's stump, a gate, a stair to nowhere, a bridge. */
	private static void shatteredCastle(Shape s) {
		// The great hall's corner, on the largest piece.
		rock(s, 0, 0, 0, 7, 7, false, SOIL, ROCK, DEEP);
		s.fill(-4, 0, -3, 3, 0, 3, BRICK);
		s.fill(-5, 1, -4, 3, 8, -4, BRICK);
		s.fill(-5, 1, -3, -5, 8, 3, BRICK);
		s.clear(-2, 3, -4, -1, 5, -4);
		s.clear(-5, 3, 0, -5, 5, 1);
		for (int x = -4; x <= 0; x++) {
			s.set(x, 8, -3, log(Blocks.DARK_OAK_LOG, Direction.Axis.X));
		}
		s.crumble(-5, 5, -4, 3, 8, 3, 0.35);
		for (int x = -4; x <= 0; x++) {
			s.set(x, 8, -3, log(Blocks.DARK_OAK_LOG, Direction.Axis.X));
		}
		hang(s, -1, 8, -3, 1);
		s.fixture(1, 1, 0, skull(Direction.SOUTH), null);
		s.sign(0, 1, 2, Direction.SOUTH, List.of("IT WAS", "WHOLE", "ONCE", ""));
		// A tower's stump.
		rock(s, 14, 4, -6, 5, 5, false, SOIL, ROCK, DEEP);
		tower(s, 14, 4, -6, 7, BRICK, -2, 0, 0, 1, Direction.NORTH);
		s.crumble(12, 9, -8, 16, 13, -4, 0.4);
		s.set(15, 7, -5, Blocks.COBWEB.defaultBlockState());
		// A gate standing alone, its portcullis half down.
		rock(s, -13, -3, 5, 5, 5, false, SOIL, ROCK, DEEP);
		s.fill(-16, -2, 5, -15, 3, 5, BRICK);
		s.fill(-11, -2, 5, -10, 3, 5, BRICK);
		s.fill(-16, 4, 5, -10, 4, 5, BRICK);
		s.late(-14, 3, 5, Blocks.IRON_BARS.defaultBlockState());
		s.late(-12, 3, 5, Blocks.IRON_BARS.defaultBlockState());
		s.set(-13, 3, 5, Blocks.IRON_CHAIN.defaultBlockState());
		// A stair climbing to nothing.
		rock(s, 5, 7, 13, 4, 4, false, SOIL, ROCK, DEEP);
		for (int k = 0; k < 5; k++) {
			for (int y = 8; y < 8 + k; y++) {
				s.set(5, y, 15 - k, BRICK);
			}
			s.set(5, 8 + k, 15 - k, stairs(Blocks.STONE_BRICK_STAIRS, Direction.NORTH));
		}
		// The stub of a bridge, its end gone.
		rock(s, -6, -6, -13, 4, 4, false, SOIL, ROCK, DEEP);
		for (int z = -12; z <= -6; z++) {
			if (z >= -7 && s.random.nextBoolean()) {
				continue;
			}
			s.fill(-7, -6, z, -5, -6, z, TIMBER);
		}
		for (int z = -12; z <= -8; z += 2) {
			s.late(-7, -5, z, Blocks.DARK_OAK_FENCE.defaultBlockState());
			s.late(-5, -5, z, Blocks.DARK_OAK_FENCE.defaultBlockState());
		}
		for (int y = -7; y >= -10; y--) {
			s.set(-6, y, -8, Blocks.IRON_CHAIN.defaultBlockState());
		}
	}

	/** A tall keep on a small island: four floors joined by a ladder, and a top room open on every side, watching. */
	private static void watchingKeep(Shape s) {
		rock(s, 0, 0, 0, 8, 10, false, SOIL, ROCK, DEEP);
		s.walls(-3, 1, -3, 3, 19, 3, Palette.of(Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS, Blocks.DEEPSLATE_BRICKS,
				Blocks.MOSSY_STONE_BRICKS));
		for (int y : new int[] {5, 10, 15}) {
			s.fill(-2, y, -2, 2, y, 2, TIMBER);
		}
		s.fill(-3, 20, -3, 3, 20, 3, BRICK);
		crenels(s, -3, -3, 3, 3, 21, BRICK);
		for (int y : new int[] {5, 10, 15, 20}) {
			s.clear(-2, y, 0, -2, y, 0);
		}
		s.clear(0, 1, 3, 0, 2, 3);
		for (int y = 1; y <= 20; y++) {
			s.fixture(-2, y, 0, ladder(Direction.EAST), Direction.WEST);
		}
		for (int y : new int[] {3, 8, 13}) {
			slit(s, 3, y, 0);
			slit(s, 0, y, -3);
			if (y > 3) {
				slit(s, 0, y, 3);
			}
		}
		// The watch room: wide openings on three sides, two narrow ones by the ladder.
		s.clear(-1, 17, 3, 1, 18, 3);
		s.clear(-1, 17, -3, 1, 18, -3);
		s.clear(3, 17, -1, 3, 18, 1);
		s.clear(-3, 17, 2, -3, 18, 2);
		s.clear(-3, 17, -2, -3, 18, -2);
		s.fixture(0, 16, 1, skull(Direction.SOUTH), null);
		s.fixture(2, 16, -2, candles(1, false), Direction.DOWN);
		for (int y : new int[] {5, 10, 15, 20}) {
			hang(s, 1, y, 1, 0);
		}
		s.set(2, 4, 2, Blocks.COBWEB.defaultBlockState());
		s.set(2, 9, -2, Blocks.COBWEB.defaultBlockState());
		s.set(-1, 14, 2, Blocks.COBWEB.defaultBlockState());
		s.sign(1, 1, 5, Direction.SOUTH, List.of("I SEE YOU", "FROM HERE", "", ""));
	}

	// Parts.

	/**
	 * A floating rock round (cx, cy, cz): a top of {@code top}, then {@code stone}, the lowest third {@code deep}. It
	 * tapers from {@code depth} under the middle to nothing at the {@code radius}; {@code inverted} makes it a narrow
	 * cone, a mountain upside down.
	 */
	private static void rock(Shape s, int cx, int cy, int cz, int radius, int depth, boolean inverted, Palette top, Palette stone,
			Palette deep) {
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz) + s.random.nextDouble() * 1.2;
				if (d > radius) {
					continue;
				}
				double t = d / radius;
				int column = (int) Math.round(inverted ? depth * Math.pow(1.0 - t, 1.4) : depth * Math.pow(1.0 - t * t, 0.9));
				column = Math.max(1, column + s.random.nextInt(2));
				s.set(cx + dx, cy, cz + dz, top);
				for (int k = 1; k < column; k++) {
					s.set(cx + dx, cy - k, cz + dz, k > column * 2 / 3 ? deep : stone);
				}
			}
		}
	}

	/** Weathers the top rows ({@code y0}–{@code y1}) of a square curtain wall {@code w} out from the middle, wall by wall. */
	private static void weatherCurtain(Shape s, int w, int y0, int y1, double chance) {
		s.crumble(-w, y0, -w, w, y1, -w, chance);
		s.crumble(-w, y0, w, w, y1, w, chance);
		s.crumble(-w, y0, -w + 1, -w, y1, w - 1, chance);
		s.crumble(w, y0, -w + 1, w, y1, w - 1, chance);
	}

	/** Battlements: every other block round the box's edge, at height {@code y}. */
	private static void crenels(Shape s, int x0, int z0, int x1, int z1, int y, Palette palette) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
				if (edge && Math.floorMod(x + z, 2) == 0) {
					s.set(x, y, z, palette);
				}
			}
		}
	}

	/**
	 * A hollow 5x5 tower of {@code height} round (cx, cz), standing on {@code base}: a door at the wall cell
	 * (cx + doorX, cz + doorZ), a ladder at the inner cell (cx + ladderX, cz + ladderZ) facing {@code ladderFacing} up to
	 * its top floor, and battlements.
	 */
	private static void tower(Shape s, int cx, int base, int cz, int height, Palette wall, int doorX, int doorZ, int ladderX, int ladderZ,
			Direction ladderFacing) {
		int top = base + height;
		s.walls(cx - 2, base + 1, cz - 2, cx + 2, top, cz + 2, wall);
		s.clear(cx - 1, base + 1, cz - 1, cx + 1, top - 1, cz + 1);
		s.fill(cx - 1, top, cz - 1, cx + 1, top, cz + 1, wall);
		crenels(s, cx - 2, cz - 2, cx + 2, cz + 2, top + 1, wall);
		s.clear(cx + doorX, base + 1, cz + doorZ, cx + doorX, base + 2, cz + doorZ);
		s.clear(cx + ladderX, top, cz + ladderZ, cx + ladderX, top, cz + ladderZ);
		for (int y = base + 1; y <= top; y++) {
			s.fixture(cx + ladderX, y, cz + ladderZ, ladder(ladderFacing), ladderFacing.getOpposite());
		}
	}

	/** A two-storey keep filling (x0, z0)–(x1, z1): a door at the front, stairs up the west side, a hall above. */
	private static void keep(Shape s, int x0, int z0, int x1, int z1, Palette wall) {
		s.walls(x0, 1, z0, x1, 9, z1, wall);
		s.fill(x0 + 1, 5, z0 + 1, x1 - 1, 5, z1 - 1, TIMBER);
		s.fill(x0, 10, z0, x1, 10, z1, wall);
		crenels(s, x0, z0, x1, z1, 11, wall);
		s.clear(0, 1, z1, 0, 2, z1);
		// Stairs from the door's side up to the floor, the floor cut away above them.
		for (int k = 0; k < 4; k++) {
			s.set(x0 + 1, 1 + k, z1 - 1 - k, stairs(Blocks.STONE_BRICK_STAIRS, Direction.NORTH));
			s.clear(x0 + 1, 5, z1 - 1 - k, x0 + 1, 5, z1 - 1 - k);
		}
		for (int y : new int[] {3, 7}) {
			slit(s, x0, y, (z0 + z1) / 2);
			slit(s, x1, y, (z0 + z1) / 2);
		}
		slit(s, x1 - 2, 7, z1);
		// Below, a table with candles; above, shelves, a skull at the window, cobwebs; a lantern on each floor.
		s.late(2, 1, (z0 + z1) / 2, Blocks.DARK_OAK_FENCE.defaultBlockState());
		s.fixture(2, 2, (z0 + z1) / 2, candles(3, true), Direction.DOWN);
		s.fill(-3, 6, z0 + 1, 3, 7, z0 + 1, Palette.of(Blocks.BOOKSHELF));
		s.fixture(x1 - 2, 6, z1 - 1, skull(Direction.SOUTH), null);
		s.set(x1 - 1, 8, z0 + 1, Blocks.COBWEB.defaultBlockState());
		s.set(x0 + 2, 4, z0 + 1, Blocks.COBWEB.defaultBlockState());
		hang(s, 0, 5, (z0 + z1) / 2, 0);
		hang(s, 0, 10, (z0 + z1) / 2, 1);
		s.sign(-1, 6, z1 - 2, Direction.SOUTH, List.of("THE KING", "NEVER", "CAME BACK", ""));
	}

	/**
	 * A gable roof over x0..x1 whose eaves overhang z = ±(halfDepth + 1) at height {@code y}, rising one block a row
	 * to a ridge along x, with the gable ends (x = endA, endB) closed in {@code fill}.
	 */
	static void gableRoof(Shape s, int x0, int x1, int halfDepth, int y, Block stairBlock, Block fill, Block cap, int endA, int endB) {
		int rows = halfDepth + 1;
		for (int k = 0; k < rows; k++) {
			for (int x = x0; x <= x1; x++) {
				s.set(x, y + k, rows - k, stairs(stairBlock, Direction.NORTH));
				s.set(x, y + k, -(rows - k), stairs(stairBlock, Direction.SOUTH));
			}
			for (int z = -(rows - k) + 1; z <= rows - k - 1; z++) {
				s.set(endA, y + k, z, fill.defaultBlockState());
				s.set(endB, y + k, z, fill.defaultBlockState());
			}
		}
		for (int x = x0; x <= x1; x++) {
			s.set(x, y + rows - 1, 0, fill.defaultBlockState());
			s.set(x, y + rows, 0, cap.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM));
		}
	}

	/** A window of old glass in a wall, or, one time in four, just the hole where it was. */
	static void windowPane(Shape s, int x, int y, int z) {
		if (s.random.nextInt(4) == 0) {
			s.clear(x, y, z, x, y, z);
		} else {
			s.late(x, y, z, Blocks.GLASS_PANE.defaultBlockState());
		}
	}

	/** An arrow slit: one iron bar in a wall. */
	private static void slit(Shape s, int x, int y, int z) {
		s.late(x, y, z, Blocks.IRON_BARS.defaultBlockState());
	}

	/** A lantern hung under the block at (x, ceiling, z), on {@code chain} links of chain. */
	private static void hang(Shape s, int x, int ceiling, int z, int chain) {
		for (int k = 1; k <= chain; k++) {
			s.set(x, ceiling - k, z, Blocks.IRON_CHAIN.defaultBlockState());
		}
		s.fixture(x, ceiling - chain - 1, z, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), Direction.UP);
	}

	/** A bare tree of dark oak: a trunk and a few branches, no leaves. */
	private static void deadTree(Shape s, int x, int z) {
		int height = 4 + s.random.nextInt(4);
		for (int y = 1; y <= height; y++) {
			s.set(x, y, z, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
		}
		for (int b = 0, branches = 1 + s.random.nextInt(3); b < branches; b++) {
			Direction side = SIDES[s.random.nextInt(SIDES.length)];
			int y = height - 1 - s.random.nextInt(2);
			for (int k = 1, length = 1 + s.random.nextInt(2); k <= length; k++) {
				s.set(x + side.getStepX() * k, y + k - 1, z + side.getStepZ() * k, log(Blocks.DARK_OAK_LOG, side.getAxis()));
			}
		}
	}

	/**
	 * A plank bridge from (x0, y0) to (x1, y1) along x at z = 0, three wide, stepping evenly between the heights: posts and
	 * chain rails on both edges, and chains hanging beneath.
	 */
	private static void bridge(Shape s, int x0, int y0, int x1, int y1) {
		int step = x1 > x0 ? 1 : -1;
		int span = Math.abs(x1 - x0);
		for (int i = 0; i <= span; i++) {
			int x = x0 + i * step;
			int y = y0 + (int) Math.round((y1 - y0) * (double) i / span);
			s.fill(x, y, -1, x, y, 1, TIMBER);
			for (int z : new int[] {-1, 1}) {
				if (i % 2 == 0) {
					s.late(x, y + 1, z, Blocks.DARK_OAK_FENCE.defaultBlockState());
				} else {
					s.set(x, y + 1, z, Blocks.IRON_CHAIN.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
				}
			}
			if (i % 3 == 1) {
				for (int k = 1; k <= 3; k++) {
					s.set(x, y - k, 0, Blocks.IRON_CHAIN.defaultBlockState());
				}
			}
		}
	}

	/** A monk's cell on the island at (cx, cy, 0), its door towards the middle ({@code inward}: +1 or -1 along x). */
	private static void cell(Shape s, int cx, int cy, int inward, List<String> words) {
		s.walls(cx - 2, cy + 1, -2, cx + 2, cy + 3, 2, TIMBER);
		for (int x : new int[] {cx - 2, cx + 2}) {
			for (int z : new int[] {-2, 2}) {
				for (int y = cy + 1; y <= cy + 3; y++) {
					s.set(x, y, z, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
				}
			}
		}
		s.fill(cx - 2, cy + 4, -2, cx + 2, cy + 4, 2, Palette.of(Blocks.DARK_OAK_SLAB.defaultBlockState()));
		s.clear(cx + 2 * inward, cy + 1, 0, cx + 2 * inward, cy + 2, 0);
		s.fixture(cx, cy + 1, 0, Blocks.CARPET.black().defaultBlockState(), Direction.DOWN);
		s.fixture(cx - inward, cy + 1, 1, candles(1, false), Direction.DOWN);
		s.set(cx - inward, cy + 3, -1, Blocks.COBWEB.defaultBlockState());
		s.sign(cx - inward, cy + 1, -1, inward > 0 ? Direction.EAST : Direction.WEST, words);
	}

	// Block states.

	static BlockState log(Block block, Direction.Axis axis) {
		return block.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
	}

	static BlockState stairs(Block block, Direction facing) {
		return block.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, Half.BOTTOM);
	}

	static BlockState ladder(Direction facing) {
		return Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, facing);
	}

	static BlockState candles(int count, boolean lit) {
		return Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, count).setValue(CandleBlock.LIT, lit);
	}

	static BlockState skull(Direction faces) {
		return Blocks.SKELETON_SKULL.defaultBlockState().setValue(BlockStateProperties.ROTATION_16, segment(faces));
	}

	/** The sixteenth-turn a standing sign or skull takes to face {@code direction}. */
	static int segment(Direction direction) {
		return switch (direction) {
			case WEST -> 4;
			case NORTH -> 8;
			case EAST -> 12;
			default -> 0;
		};
	}

	/** A choice of block states, by weight (a block named twice is twice as likely). */
	@FunctionalInterface
	interface Palette {
		BlockState pick(Random random);

		static Palette of(Block... blocks) {
			return random -> blocks[random.nextInt(blocks.length)].defaultBlockState();
		}

		static Palette of(BlockState state) {
			return random -> state;
		}
	}

	/**
	 * A structure drawn in local coordinates round an origin (x across, y up, z towards its front), turned into the world
	 * by one of four rotations. It holds solid blocks; "late" blocks (fences and iron bars, placed after everything
	 * beside them); and fixtures, which stand on, hang from or cling to a holder and are placed last, only where that
	 * holder is drawn (or, on the ground, already sturdy). Positions, block states and the direction of each holder all
	 * turn together.
	 */
	static final class Shape {
		final Random random;
		private final BlockPos origin;
		private final Rotation rotation;
		private final Map<BlockPos, BlockState> solid = new HashMap<>();
		private final Map<BlockPos, BlockState> late = new HashMap<>();
		private final Map<BlockPos, BlockState> fixtures = new LinkedHashMap<>();
		private final Map<BlockPos, @Nullable Direction> holders = new HashMap<>();
		private final Map<BlockPos, List<String>> signs = new LinkedHashMap<>();

		Shape(BlockPos origin, Rotation rotation, long seed) {
			this.origin = origin.immutable();
			this.rotation = rotation;
			this.random = new Random(seed);
		}

		/** The world position of local (x, y, z). */
		BlockPos at(int x, int y, int z) {
			return switch (rotation) {
				case CLOCKWISE_90 -> origin.offset(-z, y, x);
				case CLOCKWISE_180 -> origin.offset(-x, y, -z);
				case COUNTERCLOCKWISE_90 -> origin.offset(z, y, -x);
				default -> origin.offset(x, y, z);
			};
		}

		void set(int x, int y, int z, BlockState state) {
			BlockPos pos = at(x, y, z);
			forget(pos);
			solid.put(pos, state.rotate(rotation));
		}

		void set(int x, int y, int z, Palette palette) {
			set(x, y, z, palette.pick(random));
		}

		/** A fence or iron bar: placed after every solid block, and never beside another one. */
		void late(int x, int y, int z, BlockState state) {
			BlockPos pos = at(x, y, z);
			forget(pos);
			late.put(pos, state.rotate(rotation));
		}

		/** Something held by the drawn block in local direction {@code holder} (DOWN stands, UP hangs); none if null. */
		void fixture(int x, int y, int z, BlockState state, @Nullable Direction holder) {
			BlockPos pos = at(x, y, z);
			forget(pos);
			fixtures.put(pos, state.rotate(rotation));
			holders.put(pos, holder == null ? null : rotation.rotate(holder));
		}

		/** A standing sign of dark oak facing local {@code faces}, reading {@code lines}. */
		/** A block at a world position, as it is (no turning): a foundation under ground that dips. */
		void setWorld(BlockPos pos, BlockState state) {
			forget(pos);
			solid.put(pos.immutable(), state);
		}

		void sign(int x, int y, int z, Direction faces, List<String> lines) {
			fixture(x, y, z, Blocks.DARK_OAK_SIGN.defaultBlockState().setValue(BlockStateProperties.ROTATION_16, segment(faces)), Direction.DOWN);
			signs.put(at(x, y, z), lines);
		}

		boolean has(int x, int y, int z) {
			return solid.containsKey(at(x, y, z));
		}

		/** The lowest solid block in local column (x, z), or 0 if there is none. */
		int bottom(int x, int z) {
			int lowest = 1;
			for (int y = 0; y > -64; y--) {
				if (has(x, y, z)) {
					lowest = y;
				}
			}
			return lowest > 0 ? 0 : lowest;
		}

		void fill(int x0, int y0, int z0, int x1, int y1, int z1, Palette palette) {
			for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
				for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
					for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
						set(x, y, z, palette);
					}
				}
			}
		}

		/** The four walls of a box (its sides, not its floor or top). */
		void walls(int x0, int y0, int z0, int x1, int y1, int z1, Palette palette) {
			for (int y = y0; y <= y1; y++) {
				for (int x = x0; x <= x1; x++) {
					for (int z = z0; z <= z1; z++) {
						if (x == x0 || x == x1 || z == z0 || z == z1) {
							set(x, y, z, palette);
						}
					}
				}
			}
		}

		void clear(int x0, int y0, int z0, int x1, int y1, int z1) {
			for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
				for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
					for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
						forget(at(x, y, z));
					}
				}
			}
		}

		/** Takes away about {@code chance} of the solid blocks in a local box, more of them higher up, as time would. */
		void crumble(int x0, int y0, int z0, int x1, int y1, int z1, double chance) {
			for (int y = y0; y <= y1; y++) {
				double here = chance * (0.5 + (y - y0 + 1.0) / (y1 - y0 + 1.0));
				for (int x = x0; x <= x1; x++) {
					for (int z = z0; z <= z1; z++) {
						if (has(x, y, z) && random.nextDouble() < here) {
							forget(at(x, y, z));
						}
					}
				}
			}
		}

		private void forget(BlockPos pos) {
			solid.remove(pos);
			late.remove(pos);
			fixtures.remove(pos);
			holders.remove(pos);
			signs.remove(pos);
		}

		/** How many blocks it may place. */
		int size() {
			return solid.size() + late.size() + fixtures.size();
		}

		BlockPos min() {
			return corner(true);
		}

		BlockPos max() {
			return corner(false);
		}

		private BlockPos corner(boolean least) {
			int x = least ? Integer.MAX_VALUE : Integer.MIN_VALUE;
			int y = x;
			int z = x;
			for (Map<BlockPos, BlockState> part : List.of(solid, late, fixtures)) {
				for (BlockPos pos : part.keySet()) {
					x = least ? Math.min(x, pos.getX()) : Math.max(x, pos.getX());
					y = least ? Math.min(y, pos.getY()) : Math.max(y, pos.getY());
					z = least ? Math.min(z, pos.getZ()) : Math.max(z, pos.getZ());
				}
			}
			return x == Integer.MAX_VALUE || x == Integer.MIN_VALUE ? origin : new BlockPos(x, y, z);
		}

		/** The signs among what {@link #finish} keeps. */
		Map<BlockPos, List<String>> signs() {
			return signs;
		}

		/**
		 * The blocks in building order, world positions: solid blocks bottom up, then fences and iron bars, then each
		 * fixture whose holder is drawn (or is sturdy in {@code world}). Fences, bars and panes are joined to what will be
		 * round them, and stairs take the shape their neighbours give them, so nothing changes as later blocks arrive.
		 * {@code world} is read only next to the drawing, which must already be known to be loaded.
		 */
		LinkedHashMap<BlockPos, BlockState> finish(BlockGetter world) {
			Comparator<BlockPos> upward = Comparator.<BlockPos>comparingInt(BlockPos::getY).thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ);
			LinkedHashMap<BlockPos, BlockState> out = new LinkedHashMap<>();
			List<BlockPos> order = new ArrayList<>(solid.keySet());
			order.sort(upward);
			for (BlockPos pos : order) {
				out.put(pos, solid.get(pos));
			}
			order = new ArrayList<>(late.keySet());
			order.sort(upward);
			for (BlockPos pos : order) {
				out.put(pos, late.get(pos));
			}
			order = new ArrayList<>(fixtures.keySet());
			order.sort(upward);
			for (BlockPos pos : order) {
				Direction holder = holders.get(pos);
				if (holder == null || held(world, out, pos.relative(holder), holder.getOpposite())) {
					out.put(pos, fixtures.get(pos));
				}
			}
			for (Map.Entry<BlockPos, BlockState> entry : out.entrySet()) {
				entry.setValue(settled(world, out, entry.getKey(), entry.getValue()));
			}
			signs.keySet().retainAll(out.keySet());
			return out;
		}

		/** Whether the block at {@code pos} will hold something on its {@code face}: drawn solid, or sturdy there already. */
		private static boolean held(BlockGetter world, Map<BlockPos, BlockState> out, BlockPos pos, Direction face) {
			BlockState drawn = out.get(pos);
			if (drawn != null) {
				return true;
			}
			BlockState there = world.getBlockState(pos);
			return there.isFaceSturdy(world, pos, face);
		}

		/** A block's state once everything round it is in place: joined fences, bars and panes; shaped stairs. */
		private static BlockState settled(BlockGetter world, Map<BlockPos, BlockState> out, BlockPos pos, BlockState state) {
			Block block = state.getBlock();
			if (block instanceof FenceBlock || block instanceof IronBarsBlock) {
				for (Direction side : Direction.Plane.HORIZONTAL) {
					BlockPos next = pos.relative(side);
					BlockState neighbour = out.containsKey(next) ? out.get(next) : world.getBlockState(next);
					boolean sturdy = !neighbour.is(BlockTags.LEAVES) && neighbour.isFaceSturdy(world, next, side.getOpposite());
					boolean joins = block instanceof FenceBlock
							? neighbour.is(BlockTags.FENCES) && neighbour.is(BlockTags.WOODEN_FENCES) == state.is(BlockTags.WOODEN_FENCES) || sturdy
							: neighbour.getBlock() instanceof IronBarsBlock || neighbour.is(BlockTags.WALLS) || sturdy;
					state = state.setValue(switch (side) {
						case NORTH -> CrossCollisionBlock.NORTH;
						case EAST -> CrossCollisionBlock.EAST;
						case SOUTH -> CrossCollisionBlock.SOUTH;
						default -> CrossCollisionBlock.WEST;
					}, joins);
				}
				return state;
			}
			if (block instanceof StairBlock) {
				return state.setValue(StairBlock.SHAPE, stairShape(out, pos, state));
			}
			return state;
		}

		/** The shape the game gives a stair from the stairs in front of and behind it. */
		private static StairsShape stairShape(Map<BlockPos, BlockState> out, BlockPos pos, BlockState state) {
			Direction facing = state.getValue(StairBlock.FACING);
			Half half = state.getValue(StairBlock.HALF);
			BlockState front = out.get(pos.relative(facing));
			if (front != null && front.getBlock() instanceof StairBlock && front.getValue(StairBlock.HALF) == half) {
				Direction other = front.getValue(StairBlock.FACING);
				if (other.getAxis() != facing.getAxis() && takes(out, pos, state, other.getOpposite())) {
					return other == facing.getCounterClockWise() ? StairsShape.OUTER_LEFT : StairsShape.OUTER_RIGHT;
				}
			}
			BlockState back = out.get(pos.relative(facing.getOpposite()));
			if (back != null && back.getBlock() instanceof StairBlock && back.getValue(StairBlock.HALF) == half) {
				Direction other = back.getValue(StairBlock.FACING);
				if (other.getAxis() != facing.getAxis() && takes(out, pos, state, other)) {
					return other == facing.getCounterClockWise() ? StairsShape.INNER_LEFT : StairsShape.INNER_RIGHT;
				}
			}
			return StairsShape.STRAIGHT;
		}

		private static boolean takes(Map<BlockPos, BlockState> out, BlockPos pos, BlockState state, Direction side) {
			BlockState neighbour = out.get(pos.relative(side));
			return neighbour == null || !(neighbour.getBlock() instanceof StairBlock)
					|| neighbour.getValue(StairBlock.FACING) != state.getValue(StairBlock.FACING)
					|| neighbour.getValue(StairBlock.HALF) != state.getValue(StairBlock.HALF);
		}
	}
}
