package io.github.jimbozoomer.jugcraft.creatures.herobrine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.Filterable;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

/**
 * Part 3 (docs/features/herobrine.md): the massive brick pyramids near shorelines. A stepped, hollow pyramid of bricks
 * {@value #WIDTH} blocks across and {@value #LAYERS} high, on near-level land within {@value #SHORE} blocks of water,
 * with a doorway on the landward side, twelve glass windows, and a chamber: a netherrack altar ringed with redstone
 * torches and topped with a skull, cobwebs, a sign or two, and a lectern holding one written journal (the only thing
 * to take, once). There is no chest, nothing valuable and nothing that refills. A site is planned only where every chunk
 * it reads is already loaded, the whole footprint, its volume and a margin look like wild land
 * ({@link HerobrineMarkPlans#wild}), every block it would place is air or plain ground cover (never water), and no other
 * pyramid stands within {@value #SPACING} blocks. It only adds blocks, never takes any; it is then built
 * by {@link HerobrineMarks}, bottom up, a few blocks a tick, each block checked again just before it is set, and it is
 * saved and undoable like any mark.
 */
public final class HerobrinePyramids {
	/** Half the base's width: the base is {@value #WIDTH} blocks across. */
	public static final int HALF = 15;
	public static final int WIDTH = 2 * HALF + 1;
	/** How many layers it rises: one block narrower on every side each layer, to a single brick. */
	public static final int LAYERS = HALF + 1;
	/** Water must lie within this many blocks of the base's edge. */
	public static final int SHORE = 12;
	/** The most the ground may rise and fall across the base, in blocks (the gap is filled with brick). */
	public static final int MAX_SLOPE = 3;
	/** No two pyramids stand nearer than this, in blocks. */
	public static final int SPACING = 768;
	/** How many sites are weighed for one pyramid. */
	public static final int SITE_TRIES = 4;

	/** The journal's pages: a surveyor's notes, original to Jugcraft. */
	public static final List<String> JOURNAL = List.of(
			"Day 3.\n\nThe bricks were here before the town. I am almost sure of it.",
			"Day 5.\n\nFrom the beach I counted twelve windows. From inside I count eleven.",
			"Day 6.\n\nSomeone stands on the ridge at dusk. He does not wave back.",
			"Day 9.\n\nI left the torches out. They were lit when I came back. I did not light them.",
			"Last page.\n\nIf you are reading this, he let you in. Do not count the windows again.");

	private HerobrinePyramids() {
	}

	/** The journal on the lectern: one written book. */
	public static ItemStack journal() {
		ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
		List<Filterable<Component>> pages = JOURNAL.stream().map(page -> Filterable.passThrough((Component) Component.literal(page))).toList();
		book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough("Surveyor's Journal"), "Unknown", 0, pages, true));
		return book;
	}

	/**
	 * Looks for a pyramid site {@value #SITE_TRIES} times, 24 to 56 blocks from {@code near} in random directions, and
	 * plans the first that will take one, its door towards {@code toward}. Null if none will.
	 */
	public static HerobrineMarkPlans.@Nullable Plan near(ServerLevel level, BlockPos near, BlockPos toward, RandomSource random) {
		for (int attempt = 0; attempt < SITE_TRIES; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double distance = 24 + random.nextInt(33);
			int cx = near.getX() + (int) Math.round(Math.cos(angle) * distance);
			int cz = near.getZ() + (int) Math.round(Math.sin(angle) * distance);
			HerobrineMarkPlans.Plan plan = plan(level, cx, cz, toward);
			if (plan != null) {
				return plan;
			}
		}
		return null;
	}

	/**
	 * A pyramid centred on column {@code cx}, {@code cz}, its doorway on the side facing {@code toward}; null unless every
	 * rule holds: loaded land, no other pyramid within {@value #SPACING} blocks, dry ground across the base rising and
	 * falling no more than {@value #MAX_SLOPE}, water within {@value #SHORE} blocks, room for every block, and wild land.
	 */
	public static HerobrineMarkPlans.@Nullable Plan plan(ServerLevel level, int cx, int cz, BlockPos toward) {
		return plan(level, cx, cz, toward, true);
	}

	/** {@link #plan(ServerLevel, int, int, BlockPos)}, where {@code spacing} false skips the distance to other pyramids (tests). */
	public static HerobrineMarkPlans.@Nullable Plan plan(ServerLevel level, int cx, int cz, BlockPos toward, boolean spacing) {
		// Before any block is read: the base, its margin and a block more, all already loaded (shore columns are checked one by one).
		if (!HerobrineMarkPlans.loaded(level, new BlockPos(cx - HALF, 0, cz - HALF), new BlockPos(cx + HALF, 0, cz + HALF))) {
			return null;
		}
		if (spacing && tooNear(level, cx, cz)) {
			return null;
		}
		int[][] ground = new int[WIDTH][WIDTH];
		int low = Integer.MAX_VALUE;
		int high = Integer.MIN_VALUE;
		for (int dx = -HALF; dx <= HALF; dx++) {
			for (int dz = -HALF; dz <= HALF; dz++) {
				int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx + dx, cz + dz);
				BlockPos top = new BlockPos(cx + dx, y - 1, cz + dz);
				if (!level.getBlockState(top).getFluidState().isEmpty()) {
					return null;
				}
				ground[dx + HALF][dz + HALF] = y;
				low = Math.min(low, y);
				high = Math.max(high, y);
			}
		}
		if (high - low > MAX_SLOPE || !shore(level, cx, cz)) {
			return null;
		}
		int base = high;
		BlockPos centre = new BlockPos(cx, base, cz);
		Direction front = HerobrineMarkPlans.toward(centre, toward);
		Direction side = front.getClockWise();
		List<HerobrineMarkPlans.Step> steps = new ArrayList<>();
		Map<BlockPos, List<String>> signs = new LinkedHashMap<>();
		BlockState brick = Blocks.BRICKS.defaultBlockState();
		// The foundation, then each layer: a solid floor, then a ring two bricks thick, to the single brick on top.
		for (int dx = -HALF; dx <= HALF; dx++) {
			for (int dz = -HALF; dz <= HALF; dz++) {
				for (int y = ground[dx + HALF][dz + HALF]; y < base; y++) {
					steps.add(place(level, new BlockPos(cx + dx, y, cz + dz), brick));
				}
			}
		}
		for (int layer = 0; layer < LAYERS; layer++) {
			int h = HALF - layer;
			for (int dx = -h; dx <= h; dx++) {
				for (int dz = -h; dz <= h; dz++) {
					int ring = Math.max(Math.abs(dx), Math.abs(dz));
					if (layer > 0 && ring < h - 1) {
						continue;
					}
					BlockPos pos = new BlockPos(cx + dx, base + layer, cz + dz);
					int along = dx * front.getStepX() + dz * front.getStepZ();
					int across = dx * side.getStepX() + dz * side.getStepZ();
					if (doorway(layer, along, across)) {
						continue;
					}
					steps.add(place(level, pos, window(layer, h, ring, along, across, dx, dz) ? pane(h, dx, dz) : brick));
				}
			}
		}
		// The chamber: an altar, torches round it and in the corners, a skull, cobwebs, two signs and the journal.
		BlockPos floor = centre.above();
		steps.add(place(level, floor, Blocks.NETHERRACK.defaultBlockState()));
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			steps.add(place(level, floor.relative(direction), Blocks.REDSTONE_TORCH.defaultBlockState()));
		}
		steps.add(place(level, floor.above(), Blocks.SKELETON_SKULL.defaultBlockState()
				.setValue(BlockStateProperties.ROTATION_16, HerobrineMarkPlans.facing(floor, floor.relative(front, 8)))));
		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sz = -1; sz <= 1; sz += 2) {
				steps.add(place(level, floor.offset(11 * sx, 0, 11 * sz), Blocks.REDSTONE_TORCH.defaultBlockState()));
				steps.add(place(level, centre.offset(9 * sx, 4, 9 * sz), Blocks.COBWEB.defaultBlockState()));
			}
		}
		BlockPos lectern = floor.relative(front.getOpposite(), 9);
		steps.add(place(level, lectern, Blocks.LECTERN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, front)));
		BlockPos inside = floor.relative(front, 10).relative(side, 2);
		steps.add(place(level, inside, Blocks.OAK_SIGN.defaultBlockState()
				.setValue(BlockStateProperties.ROTATION_16, HerobrineMarkPlans.facing(inside, floor))));
		signs.put(inside, List.of("COUNT THE", "WINDOWS", "", ""));
		BlockPos altarSign = floor.relative(front, 2);
		steps.add(place(level, altarSign, Blocks.OAK_SIGN.defaultBlockState()
				.setValue(BlockStateProperties.ROTATION_16, HerobrineMarkPlans.facing(altarSign, floor.relative(front, 8)))));
		signs.put(altarSign, List.of("I WAS", "HERE", "FIRST", ""));
		BlockPos min = new BlockPos(cx - HALF, low, cz - HALF);
		BlockPos max = new BlockPos(cx + HALF, base + LAYERS, cz + HALF);
		for (HerobrineMarkPlans.Step step : steps) {
			if (!HerobrineMarkPlans.open(level.getBlockState(step.pos()))) {
				return null;
			}
		}
		if (!HerobrineMarkPlans.wild(level, min, max)) {
			return null;
		}
		return new HerobrineMarkPlans.Plan(HerobrineMarkPlans.Type.PYRAMID, centre, steps, signs, List.of(lectern));
	}

	/** The doorway, one block wide, cut through the ring on the front at the floor's height and the step above. */
	private static boolean doorway(int layer, int along, int across) {
		return across == 0 && (layer == 1 && (along == HALF - 1 || along == HALF - 2) || layer == 2 && (along == HALF - 2 || along == HALF - 3));
	}

	/** Twelve windows: eight on the fifth layer, two each side, and four on the ninth, one each side. */
	private static boolean window(int layer, int h, int ring, int along, int across, int dx, int dz) {
		boolean onSide = Math.abs(dx) == ring || Math.abs(dz) == ring;
		int offset = Math.abs(dx) == ring ? dz : dx;
		return onSide && ring >= h - 1 && (layer == 4 && Math.abs(offset) == 4 && Math.abs(dx) != Math.abs(dz)
				|| layer == 8 && offset == 0);
	}

	/**
	 * A window pane already joined to what will surround it: the wall it sits in (bricks, and the pane two deep beside
	 * it) on layers above the doorway, where the wall is rings {@code h - 1} and {@code h}. A pane does not join blocks
	 * set before it by itself, so it is placed joined.
	 */
	private static BlockState pane(int h, int dx, int dz) {
		BlockState pane = Blocks.GLASS_PANE.defaultBlockState();
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			int ring = Math.max(Math.abs(dx + direction.getStepX()), Math.abs(dz + direction.getStepZ()));
			boolean wall = ring >= h - 1 && ring <= h;
			pane = pane.setValue(switch (direction) {
				case NORTH -> BlockStateProperties.NORTH;
				case EAST -> BlockStateProperties.EAST;
				case SOUTH -> BlockStateProperties.SOUTH;
				default -> BlockStateProperties.WEST;
			}, wall);
		}
		return pane;
	}

	/** Whether any loaded column within {@value #SHORE} blocks outside the base has water at its surface. */
	private static boolean shore(ServerLevel level, int cx, int cz) {
		int reach = HALF + SHORE;
		for (int dx = -reach; dx <= reach; dx += 2) {
			for (int dz = -reach; dz <= reach; dz += 2) {
				if (Math.abs(dx) <= HALF && Math.abs(dz) <= HALF || !level.hasChunkAt(new BlockPos(cx + dx, 0, cz + dz))) {
					continue;
				}
				int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx + dx, cz + dz);
				if (!level.getBlockState(new BlockPos(cx + dx, y - 1, cz + dz)).getFluidState().isEmpty()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Whether a pyramid already stands (finished) or is rising within {@value #SPACING} blocks. One stopped part-built does
	 * not count: it stays recorded for undo, and another may be raised instead.
	 */
	private static boolean tooNear(ServerLevel level, int cx, int cz) {
		return HerobrineMarks.get(level.getServer()).marks().entrySet().stream()
				.anyMatch(entry -> entry.getValue().type().equals("pyramid") && (entry.getValue().finished() || HerobrineMarks.building(entry.getKey()))
						&& Math.abs(entry.getValue().at().getX() - cx) < SPACING && Math.abs(entry.getValue().at().getZ() - cz) < SPACING);
	}

	/**
	 * The nearest finished pyramid to {@code from}, within {@code range} blocks on each axis, or null. One still being
	 * built, stopped part-built or partly undone is never pointed to.
	 */
	public static @Nullable BlockPos nearest(ServerLevel level, BlockPos from, int range) {
		return HerobrineMarks.get(level.getServer()).marks().values().stream()
				.filter(mark -> mark.type().equals("pyramid") && mark.finished()).map(HerobrineMarks.Mark::at)
				.filter(at -> Math.abs(at.getX() - from.getX()) <= range && Math.abs(at.getZ() - from.getZ()) <= range)
				.min(java.util.Comparator.comparingDouble(at -> at.distSqr(from))).orElse(null);
	}

	/** Whether a pyramid is being built within {@code range} blocks of {@code from} on each axis. */
	public static boolean rising(ServerLevel level, BlockPos from, int range) {
		return HerobrineMarks.get(level.getServer()).marks().entrySet().stream()
				.anyMatch(entry -> entry.getValue().type().equals("pyramid") && HerobrineMarks.building(entry.getKey())
						&& Math.abs(entry.getValue().at().getX() - from.getX()) <= range
						&& Math.abs(entry.getValue().at().getZ() - from.getZ()) <= range);
	}

	private static HerobrineMarkPlans.Step place(ServerLevel level, BlockPos pos, BlockState state) {
		return new HerobrineMarkPlans.Step(pos.immutable(), level.getBlockState(pos).getBlock(), state);
	}
}
