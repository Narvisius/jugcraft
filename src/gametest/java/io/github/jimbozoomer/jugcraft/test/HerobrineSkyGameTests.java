package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.creatures.herobrine.HerobrineMarkPlans;
import io.github.jimbozoomer.jugcraft.creatures.herobrine.HerobrineMarks;
import io.github.jimbozoomer.jugcraft.creatures.herobrine.HerobrineSkyIslands;
import io.github.jimbozoomer.jugcraft.creatures.herobrine.HerobrineSurfacePlans;
import io.github.jimbozoomer.jugcraft.finance.FinanceBook;
import io.github.jimbozoomer.jugcraft.finance.LandRegistry;
import io.github.jimbozoomer.jugcraft.finance.Ledger;
import io.github.jimbozoomer.jugcraft.town.Town;
import io.github.jimbozoomer.jugcraft.town.TownData;
import io.github.jimbozoomer.jugcraft.town.TownState;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Herobrine's sky islands and surface ruins (docs/features/herobrine-sky.md). Each of the six sky islands is planned over
 * the arena: high above the ground, under the world's roof, every block in its place exactly as planned once built (so
 * nothing shifts as later blocks arrive and undo can tell his blocks from a player's), with its sign, and taken back
 * completely by undo. A stray block in its margin, too little clearance, the top of the world, a claim or unloaded land
 * refuses one, and so does the town's ground below it however high it is; planning far away loads nothing; sky islands and surface ruins keep their distance from their own
 * kind. The three surface ruins build whole and come down the same way, and keep off a player's build, uneven ground
 * and (the ritual circle) ground under leaves. Everything is built at once ({@code HerobrineMarks.buildNow}), through
 * the same checks as the build queue, and taken back in the same tick.
 */
public class HerobrineSkyGameTests {
	private static final String ARENA = "jugcraft-test:tatterlace_arena";
	private static final int GROUND = 3;

	/** The top centre for {@code variant} over the arena's middle: as low as its clearance allows, and two higher. */
	private static BlockPos skyCentre(GameTestHelper helper, String variant) {
		BlockPos middle = helper.absolutePos(new BlockPos(24, 0, 24));
		Integer top = HerobrineSkyIslands.lowestTop(helper.getLevel(), middle.getX(), middle.getZ(), variant);
		helper.assertTrue(top != null, "The arena's sky should be loaded");
		return new BlockPos(middle.getX(), top + 2, middle.getZ());
	}

	private static boolean joins(BlockState state) {
		return state.getBlock() instanceof FenceBlock || state.is(Blocks.IRON_BARS);
	}

	/**
	 * Builds {@code plan} at once and checks it whole: finished, one recorded change a block, every block exactly as
	 * planned, every sign written; then undoes it, nothing kept, everything back as it was.
	 */
	private static void buildCheckAndUndo(GameTestHelper helper, HerobrineMarkPlans.Plan plan, String what) {
		ServerLevel level = helper.getLevel();
		Map<BlockPos, BlockState> before = new HashMap<>();
		Map<BlockPos, BlockState> planned = new HashMap<>();
		for (HerobrineMarkPlans.Step step : plan.steps()) {
			before.put(step.pos(), level.getBlockState(step.pos()));
			planned.put(step.pos(), step.state());
		}
		// Fences and iron bars never side by side: undo could not tell their joins from a player's change.
		for (Map.Entry<BlockPos, BlockState> entry : planned.entrySet()) {
			if (joins(entry.getValue())) {
				for (Direction side : Direction.Plane.HORIZONTAL) {
					BlockState next = planned.get(entry.getKey().relative(side));
					helper.assertTrue(next == null || !joins(next), what + ": joining blocks side by side at " + entry.getKey());
				}
			}
		}
		helper.assertTrue(!plan.signs().isEmpty() && planned.keySet().containsAll(plan.signs().keySet()), what + " should carry a sign");
		String id = HerobrineMarks.buildNow(level, plan);
		HerobrineMarks.Mark mark = HerobrineMarks.get(level.getServer()).marks().get(id);
		helper.assertTrue(mark != null && mark.finished() && mark.changes().size() == plan.steps().size(),
				what + " should be built whole: " + (mark == null ? "no record" : mark.changes().size() + " of " + plan.steps().size()));
		for (HerobrineMarkPlans.Step step : plan.steps()) {
			BlockState now = level.getBlockState(step.pos());
			helper.assertTrue(now.equals(step.state()), what + ": " + step.pos() + " should be " + step.state() + ", not " + now);
		}
		for (Map.Entry<BlockPos, List<String>> sign : plan.signs().entrySet()) {
			helper.assertTrue(level.getBlockEntity(sign.getKey()) instanceof SignBlockEntity entity
					&& entity.getText(SignTextSlot.FRONT).getMessages(false).getFirst().getString().equals(sign.getValue().getFirst()),
					what + ": its sign should read " + sign.getValue());
		}
		HerobrineMarks.Undo undo = HerobrineMarks.undo(level, id);
		helper.assertTrue(undo != null && undo.kept() == 0 && undo.waiting() == 0, what + " should come down completely: " + undo);
		for (Map.Entry<BlockPos, BlockState> entry : before.entrySet()) {
			helper.assertTrue(level.getBlockState(entry.getKey()).equals(entry.getValue()), what + ": " + entry.getKey() + " should be as it was");
		}
	}

	/** One sky island over the arena: high and clear, bounded, built whole, and taken back. */
	private static void skyIslandBuildsWholeAndComesDown(GameTestHelper helper, String variant, int fewest) {
		ServerLevel level = helper.getLevel();
		BlockPos centre = skyCentre(helper, variant);
		HerobrineMarkPlans.Plan plan = HerobrineSkyIslands.plan(level, centre, variant, false);
		helper.assertTrue(plan != null, "Open sky over the arena should take a " + variant);
		helper.assertTrue(plan.type() == HerobrineMarkPlans.Type.SKY_ISLAND && plan.at().equals(centre), "It is a sky island at its centre");
		int count = plan.steps().size();
		helper.assertTrue(count >= fewest && count <= HerobrineSkyIslands.MAX_STEPS, variant + " places " + count + " blocks");
		// Measured before building: the ground (and anything standing on it) under each block's own column.
		for (HerobrineMarkPlans.Step step : plan.steps()) {
			int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, step.pos().getX(), step.pos().getZ());
			helper.assertTrue(step.pos().getY() >= ground + HerobrineSkyIslands.CLEARANCE,
					variant + " should float " + HerobrineSkyIslands.CLEARANCE + " clear of the ground under " + step.pos() + " (ground " + ground + ")");
			helper.assertTrue(step.pos().getY() <= level.getMaxY() - HerobrineSkyIslands.HEADROOM, variant + " should stay under the roof");
			helper.assertTrue(step.expect() == Blocks.AIR, variant + " only fills empty sky");
		}
		buildCheckAndUndo(helper, plan, variant);
		helper.succeed();
	}

	@GameTest(structure = ARENA, padding = 48, skyAccess = true, maxTicks = 20)
	public void aRuinedCastleBuildsWholeAndComesDown(GameTestHelper helper) {
		skyIslandBuildsWholeAndComesDown(helper, HerobrineSkyIslands.RUINED_CASTLE, 2500);
	}

	@GameTest(structure = ARENA, padding = 48, skyAccess = true, maxTicks = 20)
	public void aDeadForestManorBuildsWholeAndComesDown(GameTestHelper helper) {
		skyIslandBuildsWholeAndComesDown(helper, HerobrineSkyIslands.DEAD_FOREST_MANOR, 2500);
	}

	@GameTest(structure = ARENA, padding = 48, skyAccess = true, maxTicks = 20)
	public void aHangingMonasteryBuildsWholeAndComesDown(GameTestHelper helper) {
		skyIslandBuildsWholeAndComesDown(helper, HerobrineSkyIslands.HANGING_MONASTERY, 1500);
	}

	@GameTest(structure = ARENA, padding = 48, skyAccess = true, maxTicks = 20)
	public void anInvertedMountainFortressBuildsWholeAndComesDown(GameTestHelper helper) {
		skyIslandBuildsWholeAndComesDown(helper, HerobrineSkyIslands.INVERTED_MOUNTAIN_FORTRESS, 2500);
	}

	@GameTest(structure = ARENA, padding = 48, skyAccess = true, maxTicks = 20)
	public void aShatteredCastleBuildsWholeAndComesDown(GameTestHelper helper) {
		skyIslandBuildsWholeAndComesDown(helper, HerobrineSkyIslands.SHATTERED_CASTLE, 1000);
	}

	@GameTest(structure = ARENA, padding = 48, skyAccess = true, maxTicks = 20)
	public void aWatchingKeepBuildsWholeAndComesDown(GameTestHelper helper) {
		skyIslandBuildsWholeAndComesDown(helper, HerobrineSkyIslands.WATCHING_KEEP, 1000);
	}

	/** A stray block in the margin, too little clearance, the world's roof, a claim or unloaded land each refuse one. */
	@GameTest(structure = ARENA, padding = 48, skyAccess = true, maxTicks = 20)
	public void aSkyIslandIsRefusedUnlessItsWholeSkyIsClear(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		String variant = HerobrineSkyIslands.WATCHING_KEEP;
		BlockPos centre = skyCentre(helper, variant);
		helper.assertTrue(HerobrineSkyIslands.plan(level, centre, variant, false) != null, "Open sky should take a keep");
		// A player's block hanging in the island's margin, beyond its rock.
		BlockPos stray = centre.offset(11, 4, 0);
		level.setBlockAndUpdate(stray, Blocks.OAK_PLANKS.defaultBlockState());
		helper.assertTrue(HerobrineSkyIslands.plan(level, centre, variant, false) == null, "A block in its margin should refuse it");
		level.setBlockAndUpdate(stray, Blocks.AIR.defaultBlockState());
		helper.assertTrue(HerobrineSkyIslands.plan(level, centre, variant, false) != null, "Cleared again, the sky should take it");
		helper.assertTrue(HerobrineSkyIslands.plan(level, centre.below(4), variant, false) == null, "Too near the ground should refuse it");
		BlockPos roof = new BlockPos(centre.getX(), level.getMaxY() - 12, centre.getZ());
		helper.assertTrue(HerobrineSkyIslands.plan(level, roof, variant, false) == null, "Too near the top of the world should refuse it");
		// A claim on the chunk below its middle.
		ServerPlayer admin = helper.makeMockServerPlayerInLevel();
		LandRegistry land = LandRegistry.get(level.getServer());
		FinanceBook book = Ledger.get(level.getServer()).book();
		long key = LandRegistry.keyOf(centre);
		helper.assertTrue(land.assign(book, key, book.account(UUID.randomUUID(), "HerobrineSkyOwner"), 0).ok(), "The claim fixture");
		try {
			helper.assertTrue(HerobrineSkyIslands.plan(level, centre, variant, false) == null, "A claim under it should refuse it");
		} finally {
			land.abandon(admin, book, key, true, 0);
		}
		// Far away, nothing is loaded: refused, and still nothing loaded.
		BlockPos far = centre.offset(40000, 0, 40000);
		helper.assertTrue(!level.hasChunkAt(far), "The far place should start unloaded");
		helper.assertTrue(HerobrineSkyIslands.lowestTop(level, far.getX(), far.getZ(), variant) == null
				&& HerobrineSkyIslands.plan(level, far, variant, false) == null && HerobrineSkyIslands.find(level, far) == null
				&& HerobrineSurfacePlans.find(level, far) == null, "Nothing can be planned in unloaded land");
		helper.assertTrue(!level.hasChunkAt(far), "Planning should not have loaded the far chunk");
		helper.succeed();
	}

	/** The town column nearest the middle of the town's data that lies inside its wall. */
	private static int[] insideColumn(TownData data) {
		int[] best = null;
		long bestDistance = Long.MAX_VALUE;
		for (int x = 0; x < data.size; x++) {
			for (int z = 0; z < data.size; z++) {
				long dx = x - data.size / 2;
				long dz = z - data.size / 2;
				if (data.mask(x, z) == TownData.INSIDE && dx * dx + dz * dz < bestDistance) {
					best = new int[] {x, z};
					bestDistance = dx * dx + dz * dz;
				}
			}
		}
		return best;
	}

	/**
	 * No sky island over the town, however high: the town's own protection reaches only {@value Town#PROTECT_ABOVE}
	 * blocks above its ground, so the sky is checked by column. With no town in the world, one is placed for a moment
	 * with its inside under the arena and its ground far below the island, and forgotten again before the test returns;
	 * a town already placed (by another test) is only checked by column.
	 */
	@GameTest(structure = ARENA, padding = 48, skyAccess = true, maxTicks = 20)
	public void aSkyIslandNeverStandsOverTheTown(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		TownState state = TownState.get(level);
		int[] inside = insideColumn(TownData.get());
		helper.assertTrue(inside != null, "The town's data should have an inside");
		BlockPos existing = state.origin();
		if (existing != null) {
			BlockPos high = existing.offset(inside[0], Town.PROTECT_ABOVE + 40, inside[1]);
			helper.assertTrue(!Town.isProtected(level, high) && HerobrineSkyIslands.protectedTownColumn(level, high),
					"High over the town is beyond its own protection, but still its column");
			helper.succeed();
			return;
		}
		String variant = HerobrineSkyIslands.WATCHING_KEEP;
		BlockPos centre = skyCentre(helper, variant);
		helper.assertTrue(HerobrineSkyIslands.plan(level, centre, variant, false) != null, "With no town, open sky should take a keep");
		BlockPos middle = helper.absolutePos(new BlockPos(24, 0, 24));
		state.place(new BlockPos(middle.getX() - inside[0], centre.getY() - Town.PROTECT_ABOVE - 30, middle.getZ() - inside[1]));
		try {
			helper.assertTrue(!Town.isProtected(level, centre) && HerobrineSkyIslands.protectedTownColumn(level, centre),
					"The island's height is beyond the town's own protection, but its column is the town's");
			helper.assertTrue(HerobrineSkyIslands.plan(level, centre, variant, false) == null, "No sky island over the town, however high");
		} finally {
			state.forget();
		}
		helper.assertTrue(HerobrineSkyIslands.plan(level, centre, variant, false) != null, "With the town gone, the sky should take it again");
		helper.succeed();
	}

	/** While a sky island stands, no other is planned within its spacing; undone, the place is free again. */
	@GameTest(structure = ARENA, padding = 48, skyAccess = true, maxTicks = 20)
	public void skyIslandsKeepTheirDistance(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		String variant = HerobrineSkyIslands.WATCHING_KEEP;
		BlockPos centre = skyCentre(helper, variant);
		HerobrineMarkPlans.Plan first = HerobrineSkyIslands.plan(level, centre, variant, false);
		helper.assertTrue(first != null, "Open sky should take a keep");
		String id = HerobrineMarks.buildNow(level, first);
		// A second keep high above the first: its sky is clear, but it is too near another island.
		Integer top = HerobrineSkyIslands.lowestTop(level, centre.getX(), centre.getZ(), variant);
		helper.assertTrue(top != null, "The sky should still be loaded");
		BlockPos above = new BlockPos(centre.getX(), top + 2, centre.getZ());
		helper.assertTrue(HerobrineSkyIslands.plan(level, above, variant, false) != null, "Its sky alone should take a keep");
		helper.assertTrue(HerobrineSkyIslands.plan(level, above, variant) == null, "A standing sky island should keep another away");
		HerobrineMarks.undo(level, id);
		helper.assertTrue(HerobrineSkyIslands.plan(level, centre, variant) != null, "Undone, it no longer keeps one away");
		helper.succeed();
	}

	// Surface ruins.

	/** Survival ground for the surface ruins: a level field of grass blocks. */
	private static void platform(GameTestHelper helper) {
		for (int x = 0; x < 48; x++) {
			for (int z = 0; z < 48; z++) {
				helper.setBlock(new BlockPos(x, GROUND, z), Blocks.GRASS_BLOCK);
			}
		}
	}

	private static BlockPos feet(GameTestHelper helper, int x, int z) {
		return helper.absolutePos(new BlockPos(x, GROUND + 1, z));
	}

	/** Each surface ruin, on open grassland, built whole facing who it was turned to, and taken back. */
	@GameTest(structure = ARENA, padding = 48, skyAccess = true, maxTicks = 20)
	public void surfaceRuinsBuildWholeAndComeDown(GameTestHelper helper) {
		platform(helper);
		ServerLevel level = helper.getLevel();
		int[][] sites = {{12, 12}, {36, 12}, {24, 34}};
		for (int i = 0; i < HerobrineSurfacePlans.VARIANTS.size(); i++) {
			String variant = HerobrineSurfacePlans.VARIANTS.get(i);
			BlockPos feet = feet(helper, sites[i][0], sites[i][1]);
			HerobrineMarkPlans.Plan plan = HerobrineSurfacePlans.plan(level, feet, variant, feet.south(10), false);
			helper.assertTrue(plan != null, "Open, level grassland should take a " + variant);
			helper.assertTrue(plan.type() == HerobrineMarkPlans.Type.SURFACE_RUIN, "It is a surface ruin");
			if (variant.equals(HerobrineSurfacePlans.WATCHING_HUT)) {
				// Turned to the south, its window (one block up in the south wall) looks that way.
				boolean window = plan.steps().stream().noneMatch(step -> step.pos().equals(feet.offset(0, 1, 2)));
				boolean wall = plan.steps().stream().anyMatch(step -> step.pos().equals(feet.offset(1, 1, 2)));
				helper.assertTrue(window && wall, "The hut's window should face who it watches");
			}
			buildCheckAndUndo(helper, plan, variant);
		}
		helper.succeed();
	}

	/** A player's build nearby, a bump in the ground, leaves over a circle, or another ruin near refuse a surface ruin. */
	@GameTest(structure = ARENA, padding = 48, skyAccess = true, maxTicks = 20)
	public void surfaceRuinsKeepToLevelWildGround(GameTestHelper helper) {
		platform(helper);
		ServerLevel level = helper.getLevel();
		BlockPos feet = feet(helper, 24, 24);
		String hut = HerobrineSurfacePlans.WATCHING_HUT;
		helper.assertTrue(HerobrineSurfacePlans.plan(level, feet, hut, null, false) != null, "Open grassland should take a hut");
		level.setBlockAndUpdate(feet.east(6), Blocks.OAK_PLANKS.defaultBlockState());
		helper.assertTrue(HerobrineSurfacePlans.plan(level, feet, hut, null, false) == null, "Planks within its margin should refuse it");
		level.setBlockAndUpdate(feet.east(6), Blocks.AIR.defaultBlockState());
		level.setBlockAndUpdate(feet.offset(1, 0, 1), Blocks.STONE.defaultBlockState());
		helper.assertTrue(HerobrineSurfacePlans.plan(level, feet, hut, null, false) == null, "A bump in its floor should refuse it");
		level.setBlockAndUpdate(feet.offset(1, 0, 1), Blocks.AIR.defaultBlockState());
		// Leaves over the middle: a circle needs a clearing.
		String circle = HerobrineSurfacePlans.RITUAL_CIRCLE;
		helper.assertTrue(HerobrineSurfacePlans.plan(level, feet, circle, null, false) != null, "Open grassland should take a circle");
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				level.setBlockAndUpdate(feet.offset(dx, 5, dz), Blocks.OAK_LEAVES.defaultBlockState());
			}
		}
		helper.assertTrue(HerobrineSurfacePlans.plan(level, feet, circle, null, false) == null, "Leaves overhead should refuse a circle");
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				level.setBlockAndUpdate(feet.offset(dx, 5, dz), Blocks.AIR.defaultBlockState());
			}
		}
		// A standing ruin keeps another of its kind away.
		String id = HerobrineMarks.buildNow(level, HerobrineSurfacePlans.plan(level, feet, hut, null, false));
		BlockPos other = feet(helper, 24, 8);
		helper.assertTrue(HerobrineSurfacePlans.plan(level, other, hut, null, false) != null, "Its ground alone should take a hut");
		helper.assertTrue(HerobrineSurfacePlans.plan(level, other, hut, null, true) == null, "A standing ruin should keep another away");
		HerobrineMarks.undo(level, id);
		helper.succeed();
	}
}
