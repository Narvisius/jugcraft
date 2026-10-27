package io.github.jimbozoomer.jugcraft.creatures.herobrine;

import io.github.jimbozoomer.jugcraft.finance.LandRegistry;
import io.github.jimbozoomer.jugcraft.town.Town;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jspecify.annotations.Nullable;

/**
 * What Herobrine's marks would change, and the checks that keep them away from players' things
 * (docs/features/herobrine.md). Before any block is read, every chunk a plan could read (the site, its floor and roof,
 * and a margin) must already be loaded, so planning never loads a chunk. A plan is then only made where no chunk of the
 * site and margin is claimed, no column is in the town, and every block in them looks natural (ground, rock, ore,
 * plants, water) with no block entity at all, so a mark never lands beside a chest, a machine or anything plainly built.
 * That is a heuristic, not proof: a build of plain stone or logs, or a tree a player grew, looks natural. So the marks
 * that take blocks away, the grove and the tunnel, are only planned inside land an operator has approved for them
 * ({@link HerobrineMarks#approved}), and fail closed everywhere else. The marks that only add (shrines, pyramids, signs)
 * build only into air or plain ground cover. {@link HerobrineMarks} applies a plan a few blocks a tick, checking each
 * block again just before it changes.
 */
public final class HerobrineMarkPlans {
	private static final TagKey<Block> ORES = TagKey.create(Registries.BLOCK, Identifier.parse("c:ores"));
	/** How far round a mark the land must be wild, in blocks. */
	public static final int MARGIN = 6;
	/** How far round where he stood a grove loses its leaves, in blocks, and the fewest and most leaves it takes. */
	public static final int GROVE_RADIUS = 8;
	public static final int GROVE_MIN_LEAVES = 30;
	public static final int GROVE_MAX_LEAVES = 600;
	/** The shortest and longest 2x2 tunnel he digs, in blocks, and how often a redstone torch lights it. */
	public static final int TUNNEL_MIN = 6;
	public static final int TUNNEL_MAX = 20;
	public static final int TORCH_EVERY = 5;

	/** Cryptic sign texts, four lines each. "You brought someone" is kept for sightings with more than one witness. */
	public static final List<List<String>> MESSAGES = List.of(
			List.of("YOU TOOK", "THE LONG", "WAY", ""),
			List.of("I WAS", "HERE", "FIRST", ""),
			List.of("COUNT THE", "WINDOWS", "", ""),
			List.of("STILL", "HERE", "", ""),
			List.of("THE WATER", "REMEMBERS", "", ""),
			List.of("NOT YET", "", "", ""));
	public static final List<String> BROUGHT_SOMEONE = List.of("YOU", "BROUGHT", "SOMEONE", "");

	/** The common ores tag, which covers modded ores too: a mark never removes one. */
	/** Plain ground cover a built mark may stand in (never flowers or tall plants, which a player may have set). */
	private static final Set<Block> GROUND_COVER = Set.of(Blocks.SHORT_GRASS, Blocks.FERN, Blocks.DEAD_BUSH, Blocks.SNOW);
	// In 26.3 the dirt tag excludes grassy/forest soils. They remain natural terrain, never excavation permission.
	private static final Set<Block> NATURAL = Set.of(Blocks.GRASS_BLOCK, Blocks.PODZOL, Blocks.MYCELIUM, Blocks.MOSS_BLOCK,
			Blocks.PALE_MOSS_BLOCK, Blocks.GRAVEL, Blocks.CLAY, Blocks.SNOW_BLOCK, Blocks.ICE, Blocks.PACKED_ICE,
			Blocks.SANDSTONE, Blocks.RED_SANDSTONE, Blocks.TERRACOTTA, Blocks.CALCITE, Blocks.DRIPSTONE_BLOCK, Blocks.POINTED_DRIPSTONE,
			Blocks.SWEET_BERRY_BUSH, Blocks.SUGAR_CANE, Blocks.CACTUS, Blocks.PUMPKIN, Blocks.MELON, Blocks.BROWN_MUSHROOM,
			Blocks.RED_MUSHROOM, Blocks.BAMBOO, Blocks.LILY_PAD, Blocks.KELP, Blocks.KELP_PLANT, Blocks.MOSS_CARPET);

	/** The kinds of mark. */
	public enum Type {
		SHRINE, MOSSY_PYRAMID, SAND_PYRAMID, SIGN, GROVE, TUNNEL, PYRAMID,
		UNDERGROUND, UNDERWATER, SKY_ISLAND, SURFACE_RUIN
	}

	/** One block a mark changes: what it must still be when the time comes ({@code expect}), and what it becomes. */
	public record Step(BlockPos pos, Block expect, BlockState state) {
	}

	/** A mark ready to build: its steps in order, the text of any sign it places, and any lectern to lay the journal on. */
	public record Plan(Type type, BlockPos at, List<Step> steps, Map<BlockPos, List<String>> signs, List<BlockPos> lecterns) {
	}

	private HerobrineMarkPlans() {
	}

	/** Whether a block is part of the wild world: ground, rock, ore, plants the world grew, or water. */
	public static boolean natural(BlockState state) {
		if (state.isAir() || state.getBlock() instanceof LiquidBlock) {
			return true;
		}
		if (state.is(BlockTags.LEAVES)) {
			return !grownByHand(state);
		}
		return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(BlockTags.LOGS)
				|| state.is(BlockTags.FLOWERS) || state.is(BlockTags.REPLACEABLE) || ore(state) || NATURAL.contains(state.getBlock());
	}

	private static boolean grownByHand(BlockState leaves) {
		return leaves.hasProperty(LeavesBlock.PERSISTENT) && leaves.getValue(LeavesBlock.PERSISTENT);
	}

	private static boolean ore(BlockState state) {
		return state.is(ORES);
	}

	/** Whether a built mark may set a block here: air, or plain ground cover, with no fluid. */
	public static boolean open(BlockState state) {
		return (state.isAir() || GROUND_COVER.contains(state.getBlock())) && state.getFluidState().isEmpty();
	}

	/**
	 * Whether every chunk a plan from {@code min} to {@code max} could read is already loaded: the box, its
	 * {@value #MARGIN}-block margin and one block more (neighbours, floors, roofs). Called before any block is read, so
	 * planning never loads a chunk, even at the edge of the loaded world.
	 */
	public static boolean loaded(ServerLevel level, BlockPos min, BlockPos max) {
		int pad = MARGIN + 1;
		for (int cx = (Math.min(min.getX(), max.getX()) - pad) >> 4; cx <= (Math.max(min.getX(), max.getX()) + pad) >> 4; cx++) {
			for (int cz = (Math.min(min.getZ(), max.getZ()) - pad) >> 4; cz <= (Math.max(min.getZ(), max.getZ()) + pad) >> 4; cz++) {
				if (!level.hasChunkAt(new BlockPos(cx << 4, min.getY(), cz << 4))) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Whether the box from {@code min} to {@code max}, with {@value #MARGIN} blocks round it (two below), looks like wild
	 * land: every chunk loaded and unclaimed, no column in the town, every block natural-looking, no block entity. Not
	 * proof that no player built there (see the class comment).
	 */
	public static boolean wild(ServerLevel level, BlockPos min, BlockPos max) {
		BlockPos lo = new BlockPos(Math.min(min.getX(), max.getX()) - MARGIN, Math.min(min.getY(), max.getY()) - 2,
				Math.min(min.getZ(), max.getZ()) - MARGIN);
		BlockPos hi = new BlockPos(Math.max(min.getX(), max.getX()) + MARGIN, Math.max(min.getY(), max.getY()) + MARGIN,
				Math.max(min.getZ(), max.getZ()) + MARGIN);
		for (int cx = lo.getX() >> 4; cx <= hi.getX() >> 4; cx++) {
			for (int cz = lo.getZ() >> 4; cz <= hi.getZ() >> 4; cz++) {
				BlockPos chunk = new BlockPos(cx << 4, lo.getY(), cz << 4);
				if (!level.hasChunkAt(chunk) || LandRegistry.isClaimed(level, chunk)) {
					return false;
				}
			}
		}
		for (int x = lo.getX(); x <= hi.getX(); x++) {
			for (int z = lo.getZ(); z <= hi.getZ(); z++) {
				if (Town.isProtected(level, new BlockPos(x, min.getY(), z))) {
					return false;
				}
			}
		}
		for (BlockPos pos : BlockPos.betweenClosed(lo, hi)) {
			BlockState state = level.getBlockState(pos);
			if (state.hasBlockEntity() || !natural(state)) {
				return false;
			}
		}
		return true;
	}

	/** A redstone-torch shrine on a 3x3 mossy base, a netherrack heart, and a sign facing {@code toward}. */
	public static @Nullable Plan shrine(ServerLevel level, BlockPos feet, BlockPos toward, List<String> message) {
		if (!loaded(level, feet.offset(-3, -2, -3), feet.offset(3, 3, 3))) {
			return null;
		}
		List<Step> steps = new ArrayList<>();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				steps.add(place(level, feet.offset(dx, 0, dz), Blocks.MOSSY_COBBLESTONE.defaultBlockState()));
			}
		}
		steps.add(place(level, feet.above(), Blocks.NETHERRACK.defaultBlockState()));
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			steps.add(place(level, feet.above().relative(direction), Blocks.REDSTONE_TORCH.defaultBlockState()));
		}
		return built(level, Type.SHRINE, feet, steps, toward, message, 2);
	}

	/** A little stepped pyramid, 5x5, 3x3 and 1, of mossy cobblestone (or sandstone with a chiseled cap). */
	public static @Nullable Plan pyramid(ServerLevel level, BlockPos feet, boolean sand, BlockPos toward, List<String> message) {
		if (!loaded(level, feet.offset(-4, -2, -4), feet.offset(4, 4, 4))) {
			return null;
		}
		BlockState body = (sand ? Blocks.SANDSTONE : Blocks.MOSSY_COBBLESTONE).defaultBlockState();
		BlockState cap = (sand ? Blocks.CHISELED_SANDSTONE : Blocks.MOSSY_COBBLESTONE).defaultBlockState();
		List<Step> steps = new ArrayList<>();
		for (int layer = 0; layer < 3; layer++) {
			int half = 2 - layer;
			for (int dx = -half; dx <= half; dx++) {
				for (int dz = -half; dz <= half; dz++) {
					steps.add(place(level, feet.offset(dx, layer, dz), layer == 2 ? cap : body));
				}
			}
		}
		return built(level, sand ? Type.SAND_PYRAMID : Type.MOSSY_PYRAMID, feet, steps, toward, message, 3);
	}

	/** A lone sign where he stood, facing {@code toward}. */
	public static @Nullable Plan sign(ServerLevel level, BlockPos feet, BlockPos toward, List<String> message) {
		if (!loaded(level, feet.offset(-1, -2, -1), feet.offset(1, 2, 1))) {
			return null;
		}
		return built(level, Type.SIGN, feet, new ArrayList<>(), toward, message, 0);
	}

	/**
	 * Adds the sign ({@code signAt} blocks from {@code feet} towards {@code toward}, or at {@code feet}) and checks a
	 * built mark: every step goes into air or a plant (no fluid), rests on something solid or on an earlier step, and the
	 * site is wild.
	 */
	private static @Nullable Plan built(ServerLevel level, Type type, BlockPos feet, List<Step> steps, BlockPos toward,
			List<String> message, int signAt) {
		Direction front = toward(feet, toward);
		BlockPos signPos = feet.relative(front, signAt);
		steps.add(place(level, signPos, Blocks.OAK_SIGN.defaultBlockState().setValue(BlockStateProperties.ROTATION_16, facing(signPos, toward))));
		BlockPos min = feet;
		BlockPos max = feet;
		List<BlockPos> planned = new ArrayList<>();
		for (Step step : steps) {
			if (!open(level.getBlockState(step.pos()))) {
				return null;
			}
			BlockPos below = step.pos().below();
			if (!planned.contains(below) && !level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
				return null;
			}
			planned.add(step.pos());
			min = lower(min, step.pos());
			max = upper(max, step.pos());
		}
		if (!wild(level, min, max)) {
			return null;
		}
		Map<BlockPos, List<String>> signs = new LinkedHashMap<>();
		signs.put(signPos, message);
		return new Plan(type, feet, steps, signs, List.of());
	}

	/**
	 * A leafless grove: every leaf the world grew within {@value #GROVE_RADIUS} blocks round where he stood (two below to
	 * sixteen above) is taken, in a random order, so the trees stand bare; at least {@value #GROVE_MIN_LEAVES}, at most
	 * {@value #GROVE_MAX_LEAVES}. Logs, and leaves a player placed, stay. A sign stands where he stood if there is room.
	 */
	public static @Nullable Plan grove(ServerLevel level, BlockPos feet, BlockPos toward, List<String> message, RandomSource random) {
		BlockPos min = feet.offset(-GROVE_RADIUS, -2, -GROVE_RADIUS);
		BlockPos max = feet.offset(GROVE_RADIUS, 16, GROVE_RADIUS);
		// It takes blocks away, and a tree a player grew looks like any other: only where an operator approved it.
		if (!HerobrineMarks.approved(level, min, max) || !loaded(level, min, max) || !wild(level, min, max)) {
			return null;
		}
		List<Step> steps = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
			int dx = pos.getX() - feet.getX();
			int dz = pos.getZ() - feet.getZ();
			BlockState state = level.getBlockState(pos);
			if (dx * dx + dz * dz <= GROVE_RADIUS * GROVE_RADIUS && state.is(BlockTags.LEAVES) && !grownByHand(state)) {
				steps.add(new Step(pos.immutable(), state.getBlock(), Blocks.AIR.defaultBlockState()));
			}
		}
		if (steps.size() < GROVE_MIN_LEAVES) {
			return null;
		}
		for (int i = steps.size() - 1; i > 0; i--) {
			int j = random.nextInt(i + 1);
			Step swap = steps.get(i);
			steps.set(i, steps.get(j));
			steps.set(j, swap);
		}
		if (steps.size() > GROVE_MAX_LEAVES) {
			steps = new ArrayList<>(steps.subList(0, GROVE_MAX_LEAVES));
		}
		Map<BlockPos, List<String>> signs = new LinkedHashMap<>();
		BlockState here = level.getBlockState(feet);
		if (open(here) && level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)) {
			steps.add(new Step(feet, here.getBlock(), Blocks.OAK_SIGN.defaultBlockState()
					.setValue(BlockStateProperties.ROTATION_16, facing(feet, toward))));
			signs.put(feet, message);
		}
		return new Plan(Type.GROVE, feet, steps, signs, List.of());
	}

	/**
	 * A straight 2x2 tunnel dug from beside where he stood, heading {@code direction}: it starts at the first solid face
	 * within three blocks and runs while every block to dig is natural stone or soil (no ore), nothing beside it holds a
	 * fluid, its roof is not loose sand or gravel, and its floor is solid; {@value #TUNNEL_MIN} to {@value #TUNNEL_MAX}
	 * blocks. A redstone torch stands every {@value #TORCH_EVERY} blocks, and a sign at the end.
	 */
	public static @Nullable Plan tunnel(ServerLevel level, BlockPos feet, Direction direction, BlockPos toward, List<String> message) {
		// Everything it could read: from beside him to past the longest tunnel, a block either side, floor to above the roof.
		BlockPos reachMin = lower(feet.offset(-2, -2, -2), feet.relative(direction, TUNNEL_MAX + 4).offset(-2, -2, -2));
		BlockPos reachMax = upper(feet.offset(2, 4, 2), feet.relative(direction, TUNNEL_MAX + 4).offset(2, 4, 2));
		// It digs, and a plain stone wall looks like a hillside: only where an operator approved it.
		if (!HerobrineMarks.approved(level, reachMin, reachMax) || !loaded(level, reachMin, reachMax)) {
			return null;
		}
		Direction side = direction.getClockWise();
		int start = -1;
		for (int k = 0; k <= 3 && start < 0; k++) {
			BlockPos face = feet.relative(direction, k);
			if (diggable(level, face) && diggable(level, face.above())) {
				start = k;
			}
		}
		if (start < 0) {
			return null;
		}
		List<Step> digs = new ArrayList<>();
		List<Step> lights = new ArrayList<>();
		int length = 0;
		BlockPos end = null;
		for (int i = 0; i < TUNNEL_MAX; i++) {
			BlockPos base = feet.relative(direction, start + i);
			BlockPos[] cells = {base, base.above(), base.relative(side), base.relative(side).above()};
			boolean ok = level.getBlockState(base.below()).isFaceSturdy(level, base.below(), Direction.UP)
					&& level.getBlockState(base.relative(side).below()).isFaceSturdy(level, base.relative(side).below(), Direction.UP)
					&& !(level.getBlockState(base.above(2)).getBlock() instanceof FallingBlock)
					&& !(level.getBlockState(base.relative(side).above(2)).getBlock() instanceof FallingBlock);
			for (BlockPos cell : cells) {
				ok &= diggable(level, cell) && !touchesFluid(level, cell);
			}
			if (!ok) {
				break;
			}
			for (BlockPos cell : cells) {
				digs.add(new Step(cell, level.getBlockState(cell).getBlock(), Blocks.AIR.defaultBlockState()));
			}
			if (i % TORCH_EVERY == TORCH_EVERY - 1) {
				lights.add(new Step(base, Blocks.AIR, Blocks.REDSTONE_TORCH.defaultBlockState()));
			}
			length = i + 1;
			end = base;
		}
		if (length < TUNNEL_MIN || !wild(level, lower(feet, end), upper(feet, end.relative(side).above()))) {
			return null;
		}
		List<Step> steps = new ArrayList<>(digs);
		steps.addAll(lights);
		BlockPos signPos = end.relative(side);
		steps.add(new Step(signPos, Blocks.AIR, Blocks.OAK_SIGN.defaultBlockState()
				.setValue(BlockStateProperties.ROTATION_16, facing(signPos, toward))));
		Map<BlockPos, List<String>> signs = new LinkedHashMap<>();
		signs.put(signPos, message);
		return new Plan(Type.TUNNEL, feet, steps, signs, List.of());
	}

	/** Natural stone or soil, with no block entity: what a tunnel may dig. Never ore, sand, gravel or anything placed. */
	public static boolean diggable(ServerLevel level, BlockPos pos) {
		if (!level.hasChunkAt(pos)) {
			return false;
		}
		BlockState state = level.getBlockState(pos);
		return !state.hasBlockEntity() && (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.DIRT)) && !ore(state);
	}

	private static boolean touchesFluid(ServerLevel level, BlockPos pos) {
		for (Direction direction : Direction.values()) {
			BlockPos next = pos.relative(direction);
			if (!level.hasChunkAt(next) || !level.getBlockState(next).getFluidState().isEmpty()) {
				return true;
			}
		}
		return false;
	}

	private static Step place(ServerLevel level, BlockPos pos, BlockState state) {
		return new Step(pos.immutable(), level.getBlockState(pos).getBlock(), state);
	}

	/** The horizontal direction nearest to the way from {@code from} to {@code to}. */
	static Direction toward(BlockPos from, BlockPos to) {
		int dx = to.getX() - from.getX();
		int dz = to.getZ() - from.getZ();
		return Math.abs(dx) >= Math.abs(dz) ? (dx >= 0 ? Direction.EAST : Direction.WEST) : (dz >= 0 ? Direction.SOUTH : Direction.NORTH);
	}

	private static BlockPos lower(BlockPos a, BlockPos b) {
		return new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
	}

	private static BlockPos upper(BlockPos a, BlockPos b) {
		return new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
	}

	/** The standing-sign rotation (0 to 15) whose face looks from {@code at} towards {@code toward}. */
	static int facing(BlockPos at, BlockPos toward) {
		double dx = toward.getX() - at.getX();
		double dz = toward.getZ() - at.getZ();
		float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
		return Mth.floor(yaw * 16.0F / 360.0F + 0.5F) & 15;
	}
}
