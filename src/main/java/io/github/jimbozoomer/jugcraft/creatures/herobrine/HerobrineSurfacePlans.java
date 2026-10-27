package io.github.jimbozoomer.jugcraft.creatures.herobrine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

/**
 * Herobrine's surface ruins (docs/features/herobrine-sky.md): small buildings he leaves on wild ground. A watching hut
 * of dark timber with one window turned towards whoever was near; a house like a plains village house, left unfinished;
 * and a ritual circle of standing stones and candles in a clearing. They use the same checks as his other marks
 * ({@link HerobrineMarkPlans#loaded} before any read, {@link HerobrineMarkPlans#wild} round them, every block placed
 * into air or plain ground cover), stand on level ground (a dip of one block takes a cobblestone foundation), only add
 * blocks, keep {@value #SPACING} blocks from one another, and are built, recorded and undone by {@link HerobrineMarks}.
 */
public final class HerobrineSurfacePlans {
	/** Kinds of surface ruin, as operators name them. */
	public static final String WATCHING_HUT = "watching_hut";
	public static final String UNFINISHED_HOUSE = "unfinished_house";
	public static final String RITUAL_CIRCLE = "ritual_circle";
	public static final List<String> VARIANTS = List.of(WATCHING_HUT, UNFINISHED_HOUSE, RITUAL_CIRCLE);

	/** No two surface ruins nearer than this on either axis, in blocks. */
	public static final int SPACING = 128;
	/** How many places {@link #find} weighs, within this many blocks of where it is asked. */
	public static final int SITE_TRIES = 6;
	public static final int SITE_RADIUS = 12;
	/** How far away the person a watching hut turns towards may be, in blocks. */
	public static final double WATCHED_RANGE = 96.0;
	/** How far round a ritual circle's middle the ground must be open and level, in blocks. */
	public static final int CLEARING = 7;

	private static final int[][] STONES = {{5, 0}, {4, 4}, {0, 5}, {-4, 4}, {-5, 0}, {-4, -4}, {0, -5}, {4, -4}};

	private HerobrineSurfacePlans() {
	}

	/** {@link #plan(ServerLevel, BlockPos, String, BlockPos, boolean)} turned by its own seed, keeping its distance. */
	public static HerobrineMarkPlans.@Nullable Plan plan(ServerLevel level, BlockPos feet, String variant) {
		return plan(level, feet, variant, null, true);
	}

	/**
	 * A surface ruin of kind {@code variant} centred on {@code feet} (the first open block above the ground), its front
	 * (a hut's window, a door, a sign) towards {@code toward} if given. Null unless every rule in the class comment
	 * holds; {@code spacing} false skips only the distance to other surface ruins (tests).
	 */
	public static HerobrineMarkPlans.@Nullable Plan plan(ServerLevel level, BlockPos feet, String variant, @Nullable BlockPos toward,
			boolean spacing) {
		HerobrineSkyIslands.Shape shape = design(variant, feet, toward);
		if (shape == null) {
			return null;
		}
		List<BlockPos> footprint = footprint(shape, variant);
		BlockPos min = shape.min().below();
		BlockPos max = shape.max();
		if (!HerobrineMarkPlans.loaded(level, min, max) || spacing && tooNear(level, feet)) {
			return null;
		}
		// Level ground: no column higher than its floor, none more than one lower (that one takes a foundation).
		for (BlockPos column : footprint) {
			int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
			if (ground == feet.getY() - 1) {
				shape.setWorld(new BlockPos(column.getX(), feet.getY() - 1, column.getZ()), Blocks.COBBLESTONE.defaultBlockState());
			} else if (ground != feet.getY()) {
				return null;
			}
		}
		if (variant.equals(RITUAL_CIRCLE) && !clearing(level, feet)) {
			return null;
		}
		LinkedHashMap<BlockPos, BlockState> blocks = shape.finish(level);
		for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
			BlockPos pos = entry.getKey();
			if (!HerobrineMarkPlans.open(level.getBlockState(pos))) {
				return null;
			}
			// Whatever is at ground level rests on sturdy ground or on the ruin itself.
			BlockPos below = pos.below();
			if (pos.getY() < feet.getY() + 1 && !blocks.containsKey(below)
					&& !level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
				return null;
			}
		}
		if (!HerobrineMarkPlans.wild(level, shape.min(), max)) {
			return null;
		}
		List<HerobrineMarkPlans.Step> steps = new ArrayList<>(blocks.size());
		for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
			steps.add(new HerobrineMarkPlans.Step(entry.getKey(), level.getBlockState(entry.getKey()).getBlock(), entry.getValue()));
		}
		return new HerobrineMarkPlans.Plan(HerobrineMarkPlans.Type.SURFACE_RUIN, feet, steps, shape.signs(), List.of());
	}

	/**
	 * A surface ruin near {@code near}: kinds in random order, at {@code near} itself and then at random places within
	 * {@value #SITE_RADIUS} blocks, {@value #SITE_TRIES} places in all; the first {@link #plan} accepts. A hut turns its
	 * window towards the nearest player within {@value #WATCHED_RANGE} blocks. Null if none.
	 */
	public static HerobrineMarkPlans.@Nullable Plan find(ServerLevel level, BlockPos near) {
		RandomSource random = level.getRandom();
		Player watched = level.getNearestPlayer(near.getX(), near.getY(), near.getZ(), WATCHED_RANGE,
				entity -> entity.isAlive() && !entity.isSpectator());
		BlockPos toward = watched == null ? null : watched.blockPosition();
		List<String> kinds = new ArrayList<>(VARIANTS);
		Collections.shuffle(kinds, new java.util.Random(random.nextLong()));
		for (int attempt = 0; attempt < SITE_TRIES; attempt++) {
			int x = near.getX() + (attempt == 0 ? 0 : random.nextInt(2 * SITE_RADIUS + 1) - SITE_RADIUS);
			int z = near.getZ() + (attempt == 0 ? 0 : random.nextInt(2 * SITE_RADIUS + 1) - SITE_RADIUS);
			if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
				continue;
			}
			BlockPos feet = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
			HerobrineMarkPlans.Plan plan = plan(level, feet, kinds.get(attempt % kinds.size()), toward, true);
			if (plan != null) {
				return plan;
			}
		}
		return null;
	}

	/** Whether a surface ruin stands (finished) or is rising within {@value #SPACING} blocks of {@code feet}. */
	private static boolean tooNear(ServerLevel level, BlockPos feet) {
		String type = HerobrineMarkPlans.Type.SURFACE_RUIN.name().toLowerCase(Locale.ROOT);
		return HerobrineMarks.get(level.getServer()).marks().entrySet().stream()
				.anyMatch(entry -> entry.getValue().type().equals(type) && (entry.getValue().finished() || HerobrineMarks.building(entry.getKey()))
						&& Math.abs(entry.getValue().at().getX() - feet.getX()) < SPACING
						&& Math.abs(entry.getValue().at().getZ() - feet.getZ()) < SPACING);
	}

	/** Whether the ground within {@value #CLEARING} blocks is open and level: no leaves overhead, nothing standing on it. */
	private static boolean clearing(ServerLevel level, BlockPos feet) {
		for (int dx = -CLEARING; dx <= CLEARING; dx++) {
			for (int dz = -CLEARING; dz <= CLEARING; dz++) {
				if (dx * dx + dz * dz > CLEARING * CLEARING) {
					continue;
				}
				int x = feet.getX() + dx;
				int z = feet.getZ() + dz;
				int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
				if (ground > feet.getY() || ground < feet.getY() - 1 || level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) != ground) {
					return false;
				}
			}
		}
		return true;
	}

	/** The world columns a ruin stands on, which must be level ground. */
	private static List<BlockPos> footprint(HerobrineSkyIslands.Shape shape, String variant) {
		List<BlockPos> columns = new ArrayList<>();
		if (variant.equals(RITUAL_CIRCLE)) {
			for (int[] stone : STONES) {
				columns.add(shape.at(stone[0], 0, stone[1]));
			}
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					columns.add(shape.at(dx, 0, dz));
				}
			}
			columns.add(shape.at(0, 0, 3));
			return columns;
		}
		int half = variant.equals(WATCHING_HUT) ? 2 : 3;
		for (int dx = -half; dx <= half; dx++) {
			for (int dz = -half; dz <= half; dz++) {
				columns.add(shape.at(dx, 0, dz));
			}
		}
		return columns;
	}

	/** The drawing of a {@code variant} ruin at {@code feet}, its front towards {@code toward} (or its seed's way). */
	static HerobrineSkyIslands.@Nullable Shape design(String variant, BlockPos feet, @Nullable BlockPos toward) {
		long seed = feet.getX() * 341873128712L ^ feet.getZ() * 132897987541L ^ variant.hashCode() * 0x9E3779B97F4A7C15L;
		Rotation rotation = toward == null ? Rotation.values()[(int) Math.floorMod(seed >>> 7, 4L)]
				: switch (HerobrineMarkPlans.toward(feet, toward)) {
					case WEST -> Rotation.CLOCKWISE_90;
					case NORTH -> Rotation.CLOCKWISE_180;
					case EAST -> Rotation.COUNTERCLOCKWISE_90;
					default -> Rotation.NONE;
				};
		HerobrineSkyIslands.Shape shape = new HerobrineSkyIslands.Shape(feet, rotation, seed);
		switch (variant) {
			case WATCHING_HUT -> watchingHut(shape);
			case UNFINISHED_HOUSE -> unfinishedHouse(shape);
			case RITUAL_CIRCLE -> ritualCircle(shape);
			default -> {
				return null;
			}
		}
		return shape;
	}

	// Each is drawn round (0, 0, 0), the first open block above the ground, its front towards +z.

	/** A hut of dark timber, 5 by 5: one window towards the watched, a door at the side, a stool facing the window. */
	private static void watchingHut(HerobrineSkyIslands.Shape s) {
		s.walls(-2, 0, -2, 2, 2, 2, HerobrineSkyIslands.TIMBER);
		for (int x : new int[] {-2, 2}) {
			for (int z : new int[] {-2, 2}) {
				for (int y = 0; y <= 2; y++) {
					s.set(x, y, z, HerobrineSkyIslands.log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
				}
			}
		}
		s.fill(-2, 3, -2, 2, 3, 2, HerobrineSkyIslands.Palette.of(
				Blocks.DARK_OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM)));
		s.clear(0, 1, 2, 0, 1, 2);
		s.clear(2, 0, 0, 2, 1, 0);
		s.set(0, 0, 0, HerobrineSkyIslands.stairs(Blocks.DARK_OAK_STAIRS, Direction.NORTH));
		s.fixture(-1, 0, 1, HerobrineSkyIslands.candles(1, false), Direction.DOWN);
		s.set(-1, 2, -1, Blocks.COBWEB.defaultBlockState());
		s.sign(1, 0, -1, Direction.SOUTH, List.of("I WATCHED", "YOU FROM", "HERE", ""));
	}

	/**
	 * A house like a plains village house, 7 by 7 (an original layout: cobblestone footing, oak log corners, plank walls,
	 * glass windows, an oak stair roof), left unfinished: the back wall and one side stop short, only half the roof is on,
	 * and its materials lie inside, with a ladder up to the unfinished top.
	 */
	private static void unfinishedHouse(HerobrineSkyIslands.Shape s) {
		s.walls(-3, 0, -3, 3, 0, 3, HerobrineSkyIslands.Palette.of(Blocks.COBBLESTONE));
		s.fill(-2, 0, -2, 2, 0, 2, HerobrineSkyIslands.Palette.of(Blocks.OAK_PLANKS));
		s.walls(-3, 1, -3, 3, 3, 3, HerobrineSkyIslands.Palette.of(Blocks.OAK_PLANKS));
		for (int x : new int[] {-3, 3}) {
			for (int z : new int[] {-3, 3}) {
				for (int y = 1; y <= 4; y++) {
					s.set(x, y, z, HerobrineSkyIslands.log(Blocks.OAK_LOG, Direction.Axis.Y));
				}
			}
		}
		s.clear(0, 1, 3, 0, 2, 3);
		// Unfinished: the back wall stops short, the right side lacks its top row.
		s.clear(-2, 3, -3, 0, 3, -3);
		s.clear(-2, 2, -3, -1, 2, -3);
		s.clear(3, 3, -2, 3, 3, 2);
		for (int x : new int[] {-2, 2}) {
			HerobrineSkyIslands.windowPane(s, x, 2, 3);
		}
		HerobrineSkyIslands.windowPane(s, -3, 2, 0);
		HerobrineSkyIslands.windowPane(s, 3, 2, 0);
		// A top plate over the finished half, and half a roof on it.
		for (int x = -3; x <= 0; x++) {
			s.set(x, 4, -3, HerobrineSkyIslands.log(Blocks.OAK_LOG, Direction.Axis.X));
			s.set(x, 4, 3, HerobrineSkyIslands.log(Blocks.OAK_LOG, Direction.Axis.X));
		}
		HerobrineSkyIslands.gableRoof(s, -4, -1, 3, 4, Blocks.OAK_STAIRS, Blocks.OAK_PLANKS, Blocks.OAK_SLAB, -3, -3);
		// What the builder left: planks, a log, a crafting table, a ladder up the back.
		s.set(1, 1, 1, Blocks.OAK_PLANKS.defaultBlockState());
		s.set(2, 1, 1, Blocks.OAK_PLANKS.defaultBlockState());
		s.set(1, 1, 0, HerobrineSkyIslands.log(Blocks.OAK_LOG, Direction.Axis.X));
		s.set(-2, 1, -2, Blocks.CRAFTING_TABLE.defaultBlockState());
		for (int y = 1; y <= 3; y++) {
			s.fixture(1, y, -2, HerobrineSkyIslands.ladder(Direction.SOUTH), Direction.NORTH);
		}
		s.sign(0, 1, 1, Direction.SOUTH, List.of("IT WAS", "NEVER", "FINISHED", ""));
	}

	/** Eight standing stones of mossy cobblestone round a cobblestone altar with a skull, candles on the stones. */
	private static void ritualCircle(HerobrineSkyIslands.Shape s) {
		for (int i = 0; i < STONES.length; i++) {
			int x = STONES[i][0];
			int z = STONES[i][1];
			int height = s.random.nextInt(3) == 0 ? 1 : 2;
			for (int y = 0; y < height; y++) {
				s.set(x, y, z, HerobrineSkyIslands.RUBBLE);
			}
			s.fixture(x, height, z, HerobrineSkyIslands.candles(1 + s.random.nextInt(2), i % 3 == 0), Direction.DOWN);
		}
		s.set(0, 0, 0, Blocks.COBBLESTONE.defaultBlockState());
		s.fixture(0, 1, 0, HerobrineSkyIslands.skull(Direction.SOUTH), null);
		for (int[] corner : new int[][] {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
			s.fixture(corner[0], 0, corner[1], HerobrineSkyIslands.candles(1, false), Direction.DOWN);
		}
		s.sign(0, 0, 3, Direction.SOUTH, List.of("HE DANCED", "HERE", "ALONE", ""));
	}
}
