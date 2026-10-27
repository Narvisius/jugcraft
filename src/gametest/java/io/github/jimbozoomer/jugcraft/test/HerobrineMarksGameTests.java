package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.creatures.herobrine.HerobrineMarkPlans;
import io.github.jimbozoomer.jugcraft.creatures.herobrine.HerobrineMarks;
import io.github.jimbozoomer.jugcraft.creatures.herobrine.HerobrinePyramids;
import io.github.jimbozoomer.jugcraft.finance.FinanceBook;
import io.github.jimbozoomer.jugcraft.finance.LandRegistry;
import io.github.jimbozoomer.jugcraft.finance.Ledger;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

/**
 * Herobrine's marks (docs/features/herobrine.md): a shrine is built in wild land, writes its sign, and is undone cleanly;
 * a player's build nearby, player-placed leaves, or a claim keep a mark away; groves and tunnels fail closed without an
 * operator's approval, even beside a plain stone and log build or a grown tree that looks natural; planning never loads
 * a chunk, even at the loaded world's edge; a grove takes only leaves the world grew and leaves the logs; a tunnel digs
 * natural stone and stops short of ore; a block set mid-build stops a mark on the real build queue; undo leaves any block a player changed (even
 * only turned) as they left it; a shoreline pyramid is built with its windows and journal and undone; and a block a
 * player sets in its way stops the building rather than being overwritten; a sign edited in any way (back, style, dye,
 * glow, wax) is kept by undo; a trail moves on only when its mark is finished, never points to a pyramid still rising or
 * stopped part-built, and waits for a finished one. Everything stands on a raised stone
 * platform so the margin a mark checks stays in the test arena. Marks share the server's build slots, so each test
 * waits for a free one and for its mark to finish; the pyramids are built at once (buildNow), through the same checks.
 */
public class HerobrineMarksGameTests {
	private static final String ARENA = "jugcraft-test:tatterlace_arena";
	private static final int GROUND = 3;
	private static final String[] FIRST = {"COUNT THE", "WINDOWS", "", ""};

	private static void platform(GameTestHelper helper) {
		for (int x = 0; x < 48; x++) {
			for (int z = 0; z < 48; z++) {
				helper.setBlock(new BlockPos(x, GROUND, z), Blocks.STONE);
			}
		}
	}

	private static BlockPos feet(GameTestHelper helper, int x, int z) {
		return helper.absolutePos(new BlockPos(x, GROUND + 1, z));
	}

	private static List<String> message() {
		return List.of(FIRST);
	}

	/** Starts {@code plan} as soon as a build slot is free, then hands its id to {@code then} once it is built. */
	private static void whenBuilt(GameTestHelper helper, HerobrineMarkPlans.Plan plan, int patience, Consumer<String> then) {
		String id = HerobrineMarks.start(helper.getLevel(), plan);
		if (id == null) {
			helper.assertTrue(patience > 0, "No build slot came free");
			helper.runAfterDelay(1, () -> whenBuilt(helper, plan, patience - 1, then));
		} else {
			untilBuilt(helper, id, patience, then);
		}
	}

	private static void untilBuilt(GameTestHelper helper, String id, int patience, Consumer<String> then) {
		if (HerobrineMarks.building(id)) {
			helper.assertTrue(patience > 0, "Mark " + id + " never finished building");
			helper.runAfterDelay(1, () -> untilBuilt(helper, id, patience - 1, then));
		} else {
			then.accept(id);
		}
	}

	@GameTest(structure = ARENA, padding = 48, skyAccess = true, maxTicks = 400)
	public void aShrineIsBuiltInTheWildAndUndoneCleanly(GameTestHelper helper) {
		platform(helper);
		ServerLevel level = helper.getLevel();
		BlockPos feet = feet(helper, 24, 24);
		HerobrineMarkPlans.Plan plan = HerobrineMarkPlans.shrine(level, feet, feet(helper, 24, 8), message());
		helper.assertTrue(plan != null, "Open, untouched ground should take a shrine");
		whenBuilt(helper, plan, 300, id -> {
			helper.assertTrue(level.getBlockState(feet).is(Blocks.MOSSY_COBBLESTONE), "The shrine's base should be built");
			helper.assertTrue(level.getBlockState(feet.above()).is(Blocks.NETHERRACK), "The shrine's heart should be netherrack");
			helper.assertTrue(level.getBlockState(feet.above().north()).is(Blocks.REDSTONE_TORCH), "Redstone torches should stand round it");
			BlockPos sign = plan.signs().keySet().iterator().next();
			helper.assertTrue(level.getBlockEntity(sign) instanceof SignBlockEntity entity
					&& entity.getText(SignTextSlot.FRONT).getMessages(false).getFirst().getString().equals(FIRST[0]), "Its sign should carry the message");
			HerobrineMarks.Undo undo = HerobrineMarks.undo(level, id);
			helper.assertTrue(undo != null && undo.kept() == 0 && undo.waiting() == 0, "The whole shrine should come back: " + undo);
			helper.assertTrue(level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir() && level.getBlockState(sign).isAir(),
					"Undo should leave the ground as it was");
			helper.assertTrue(!HerobrineMarks.get(level.getServer()).marks().containsKey(id), "An undone mark leaves no record");
			helper.succeed();
		});
	}

	@GameTest(structure = ARENA, padding = 48, skyAccess = true, maxTicks = 400)
	public void undoLeavesWhatAPlayerChangedAlone(GameTestHelper helper) {
		platform(helper);
		ServerLevel level = helper.getLevel();
		BlockPos feet = feet(helper, 24, 24);
		HerobrineMarkPlans.Plan plan = HerobrineMarkPlans.pyramid(level, feet, false, feet(helper, 24, 8), message());
		helper.assertTrue(plan != null, "Open ground should take a mossy pyramid");
		whenBuilt(helper, plan, 300, id -> {
			helper.assertTrue(level.getBlockState(feet.above(2)).is(Blocks.MOSSY_COBBLESTONE), "The pyramid should be built");
			BlockPos changed = feet.east(2).south(2);
			level.setBlockAndUpdate(changed, Blocks.OAK_PLANKS.defaultBlockState());
			BlockPos sign = plan.signs().keySet().iterator().next();
			// Only turned, not replaced: still the mark's sign, but a player's change all the same.
			int rotation = level.getBlockState(sign).getValue(BlockStateProperties.ROTATION_16);
			level.setBlockAndUpdate(sign, level.getBlockState(sign).setValue(BlockStateProperties.ROTATION_16, (rotation + 4) & 15));
			HerobrineMarks.Undo undo = HerobrineMarks.undo(level, id);
			helper.assertTrue(undo != null && undo.kept() == 2, "The replaced block and the turned sign should both be kept: " + undo);
			helper.assertTrue(level.getBlockState(changed).is(Blocks.OAK_PLANKS), "The player's block should stay");
			helper.assertTrue(level.getBlockState(sign).is(Blocks.OAK_SIGN), "The turned sign should stay");
			helper.assertTrue(level.getBlockState(feet).isAir() && level.getBlockState(feet.above(2)).isAir(), "The rest should be undone");
			helper.succeed();
		});
	}

	@GameTest(structure = ARENA, padding = 48, skyAccess = true, maxTicks = 20)
	public void aPlayersBuildKeepsHimAway(GameTestHelper helper) {
		platform(helper);
		ServerLevel level = helper.getLevel();
		BlockPos feet = feet(helper, 24, 24);
		level.setBlockAndUpdate(feet.east(5), Blocks.OAK_PLANKS.defaultBlockState());
		helper.assertTrue(HerobrineMarkPlans.shrine(level, feet, feet(helper, 24, 8), message()) == null,
				"Planks within the margin should keep a shrine away");
		helper.assertTrue(HerobrineMarkPlans.sign(level, feet, feet(helper, 24, 8), message()) == null,
				"Even a sign should stay away from a build");
		helper.succeed();
	}

	@GameTest(structure = ARENA, padding = 48, skyAccess = true, maxTicks = 20)
	public void aClaimKeepsHimAway(GameTestHelper helper) {
		platform(helper);
		ServerLevel level = helper.getLevel();
		BlockPos feet = feet(helper, 24, 24);
		ServerPlayer admin = helper.makeMockServerPlayerInLevel();
		LandRegistry land = LandRegistry.get(level.getServer());
		FinanceBook book = Ledger.get(level.getServer()).book();
		long key = LandRegistry.keyOf(feet);
		helper.assertTrue(land.assign(book, key, book.account(UUID.randomUUID(), "HerobrineTestOwner"), 0).ok(), "The claim fixture");
		try {
			helper.assertTrue(HerobrineMarkPlans.shrine(level, feet, feet(helper, 24, 8), message()) == null,
					"A claimed chunk should take no mark");
		} finally {
			land.abandon(admin, book, key, true, 0);
		}
		helper.assertTrue(HerobrineMarkPlans.shrine(level, feet, feet(helper, 24, 8), message()) != null,
				"Once unclaimed, the same wild ground takes a shrine");
		helper.succeed();
	}

	private static void tree(GameTestHelper helper, int x, int z, boolean placedByHand) {
		for (int y = GROUND + 1; y <= GROUND + 5; y++) {
			helper.setBlock(new BlockPos(x, y, z), Blocks.OAK_LOG);
		}
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				for (int y = GROUND + 3; y <= GROUND + 6; y++) {
					if ((dx != 0 || dz != 0 || y > GROUND + 5)) {
						helper.setBlock(new BlockPos(x + dx, y, z + dz), Blocks.OAK_LEAVES.defaultBlockState()
								.setValue(LeavesBlock.PERSISTENT, placedByHand).setValue(LeavesBlock.DISTANCE, 1));
					}
				}
			}
		}
	}

	@GameTest(structure = ARENA, padding = 48, skyAccess = true, maxTicks = 500)
	public void aGroveTakesOnlyTheLeavesAndLeavesTheLogs(GameTestHelper helper) {
		platform(helper);
		ServerLevel level = helper.getLevel();
		tree(helper, 24, 24, false);
		BlockPos feet = feet(helper, 22, 22);
		HerobrineMarks.Wild wild = HerobrineMarks.approve(level, feet, 24, "test");
		HerobrineMarkPlans.Plan plan = HerobrineMarkPlans.grove(level, feet, feet(helper, 22, 8), message(), level.getRandom());
		helper.assertTrue(plan != null, "A wild tree on approved land should make a grove");
		whenBuilt(helper, plan, 400, id -> {
			HerobrineMarks.revoke(level, wild);
			int leaves = 0;
			for (BlockPos pos : BlockPos.betweenClosed(helper.absolutePos(new BlockPos(22, GROUND + 3, 22)),
					helper.absolutePos(new BlockPos(26, GROUND + 6, 26)))) {
				leaves += level.getBlockState(pos).is(Blocks.OAK_LEAVES) ? 1 : 0;
			}
			helper.assertTrue(leaves == 0, leaves + " leaves are left on the bare tree");
			helper.assertTrue(level.getBlockState(helper.absolutePos(new BlockPos(24, GROUND + 4, 24))).is(Blocks.OAK_LOG), "The trunk should stand");
			HerobrineMarks.undo(level, id);
			helper.succeed();
		});
	}

	@GameTest(structure = ARENA, padding = 48, skyAccess = true, maxTicks = 20)
	public void leavesAPlayerPlacedKeepAGroveAway(GameTestHelper helper) {
		platform(helper);
		ServerLevel level = helper.getLevel();
		tree(helper, 24, 24, true);
		BlockPos feet = feet(helper, 22, 22);
		HerobrineMarks.Wild wild = HerobrineMarks.approve(level, feet, 24, "test");
		HerobrineMarkPlans.Plan plan = HerobrineMarkPlans.grove(level, feet, feet(helper, 22, 8), message(), level.getRandom());
		HerobrineMarks.revoke(level, wild);
		helper.assertTrue(plan == null, "Even on approved land, a tree a player dressed in leaves is a build, not a grove");
		helper.succeed();
	}

	@GameTest(structure = ARENA, padding = 48, skyAccess = true, maxTicks = 400)
	public void aTunnelDigsNaturalStoneAndStopsShortOfOre(GameTestHelper helper) {
		platform(helper);
		ServerLevel level = helper.getLevel();
		for (int x = 14; x <= 34; x++) {
			for (int y = GROUND + 1; y <= GROUND + 5; y++) {
				for (int z = 24; z <= 44; z++) {
					helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
				}
			}
		}
		helper.setBlock(new BlockPos(24, GROUND + 2, 33), Blocks.DIAMOND_ORE);
		BlockPos feet = feet(helper, 24, 22);
		HerobrineMarks.Wild wild = HerobrineMarks.approve(level, feet.relative(Direction.SOUTH, 12), 28, "test");
		HerobrineMarkPlans.Plan plan = HerobrineMarkPlans.tunnel(level, feet, Direction.SOUTH, feet(helper, 24, 8), message());
		helper.assertTrue(plan != null, "A stone hillside on approved land should take a tunnel");
		whenBuilt(helper, plan, 300, id -> {
			HerobrineMarks.revoke(level, wild);
			helper.assertTrue(level.getBlockState(helper.absolutePos(new BlockPos(24, GROUND + 1, 26))).isAir()
					&& level.getBlockState(helper.absolutePos(new BlockPos(23, GROUND + 2, 26))).isAir(), "The tunnel should be dug two wide and two high");
			helper.assertTrue(level.getBlockState(helper.absolutePos(new BlockPos(24, GROUND + 2, 33))).is(Blocks.DIAMOND_ORE), "The ore should be left");
			helper.assertTrue(!level.getBlockState(helper.absolutePos(new BlockPos(24, GROUND + 1, 33))).isAir(), "The tunnel should stop before the ore");
			helper.assertTrue(level.getBlockState(helper.absolutePos(new BlockPos(24, GROUND + 3, 26))).is(Blocks.STONE), "The roof should stay");
			HerobrineMarks.Undo undo = HerobrineMarks.undo(level, id);
			helper.assertTrue(undo != null && undo.kept() == 0, "The tunnel should fill back in: " + undo);
			helper.assertTrue(level.getBlockState(helper.absolutePos(new BlockPos(24, GROUND + 1, 26))).is(Blocks.STONE), "Undo should restore the stone");
			helper.succeed();
		});
	}

	/** The shore for a pyramid: a pool just outside its base, inside the arena. */
	private static void pool(GameTestHelper helper) {
		for (int x = 42; x <= 45; x++) {
			for (int z = 20; z <= 28; z++) {
				helper.setBlock(new BlockPos(x, GROUND, z), Blocks.WATER);
			}
		}
	}

	@GameTest(structure = ARENA, padding = 48, skyAccess = true, maxTicks = 40)
	public void aShorelinePyramidIsBuiltWithItsJournalAndUndone(GameTestHelper helper) {
		platform(helper);
		pool(helper);
		ServerLevel level = helper.getLevel();
		BlockPos centre = feet(helper, 24, 24);
		HerobrineMarkPlans.Plan plan = HerobrinePyramids.plan(level, centre.getX(), centre.getZ(), feet(helper, 24, 0), false);
		helper.assertTrue(plan != null, "Level, wild land by the water should take a pyramid");
		String id = HerobrineMarks.buildNow(level, plan);
		{
			helper.assertTrue(level.getBlockState(centre.above(HerobrinePyramids.LAYERS - 1)).is(Blocks.BRICKS), "Its single top brick should be laid");
			helper.assertTrue(level.getBlockState(centre).is(Blocks.BRICKS), "Its floor should be laid");
			BlockPos lectern = plan.lecterns().getFirst();
			helper.assertTrue(level.getBlockState(lectern).is(Blocks.LECTERN) && level.getBlockState(lectern).getValue(BlockStateProperties.HAS_BOOK),
					"The lectern should hold the journal");
			helper.assertTrue(level.getBlockEntity(lectern) instanceof net.minecraft.world.level.block.entity.LecternBlockEntity entity
					&& ItemStack.isSameItemSameComponents(entity.getBook(), HerobrinePyramids.journal()), "The book should be the journal");
			int panes = 0;
			for (HerobrineMarkPlans.Step step : plan.steps()) {
				panes += step.state().is(Blocks.GLASS_PANE) ? 1 : 0;
			}
			helper.assertTrue(panes == 24, "Twelve windows, each two panes deep, not " + panes + " panes");
			HerobrineMarks.Undo undo = HerobrineMarks.undo(level, id);
			helper.assertTrue(undo != null && undo.kept() == 0 && undo.waiting() == 0, "The whole pyramid should come down: " + undo);
			helper.assertTrue(level.getBlockState(centre).isAir() && level.getBlockState(lectern).isAir(), "The land should be as it was");
			helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(lectern).inflate(2.0), item -> item.isAlive()
					&& ItemStack.isSameItemSameComponents(item.getItem(), HerobrinePyramids.journal())).isEmpty(),
					"The journal should go with the pyramid, not drop loose");
			helper.succeed();
		}
	}

	@GameTest(structure = ARENA, padding = 48, skyAccess = true, maxTicks = 40)
	public void aBlockSetInAPyramidsWayStopsItRatherThanBeingOverwritten(GameTestHelper helper) {
		platform(helper);
		pool(helper);
		ServerLevel level = helper.getLevel();
		BlockPos centre = feet(helper, 24, 24);
		HerobrineMarkPlans.Plan plan = HerobrinePyramids.plan(level, centre.getX(), centre.getZ(), feet(helper, 24, 0), false);
		helper.assertTrue(plan != null, "Level, wild land by the water should take a pyramid");
		// A player sets a block where the third layer will go, after the plan was checked.
		BlockPos inTheWay = centre.offset(HerobrinePyramids.HALF - 2, 2, 0);
		level.setBlockAndUpdate(inTheWay, Blocks.OAK_PLANKS.defaultBlockState());
		String id = HerobrineMarks.buildNow(level, plan);
		{
			helper.assertTrue(level.getBlockState(inTheWay).is(Blocks.OAK_PLANKS), "The player's block should never be overwritten");
			helper.assertTrue(!level.getBlockState(centre.above(HerobrinePyramids.LAYERS - 1)).is(Blocks.BRICKS),
					"Building should have stopped there, never reaching the top");
			HerobrineMarks.Undo undo = HerobrineMarks.undo(level, id);
			helper.assertTrue(undo != null && undo.waiting() == 0, "What was built should come down: " + undo);
			helper.assertTrue(level.getBlockState(inTheWay).is(Blocks.OAK_PLANKS) && level.getBlockState(centre).isAir(),
					"Undo should take only the pyramid's blocks");
			helper.succeed();
		}
	}

	/**
	 * Unclaimed is not untouched: a plain stone and log build, and a tree like one a player grew, look natural. So a
	 * grove and a tunnel are refused there without an operator's approval, and only the approval lets them through.
	 */
	@GameTest(structure = ARENA, padding = 48, skyAccess = true, maxTicks = 20)
	public void groovesAndTunnelsFailClosedWithoutApproval(GameTestHelper helper) {
		platform(helper);
		ServerLevel level = helper.getLevel();
		// A little unclaimed hut of plain stone walls on log corners, and a tree beside it.
		for (int x = 20; x <= 28; x++) {
			for (int y = GROUND + 1; y <= GROUND + 4; y++) {
				for (int z = 26; z <= 34; z++) {
					boolean corner = (x == 20 || x == 28) && (z == 26 || z == 34);
					boolean wall = x == 20 || x == 28 || z == 26 || z == 34 || y == GROUND + 4;
					if (wall) {
						helper.setBlock(new BlockPos(x, y, z), corner ? Blocks.OAK_LOG : Blocks.STONE);
					}
				}
			}
		}
		tree(helper, 18, 16, false);
		// Keep the grove and its wilderness margin inside the arena; the outer floor is not wilderness.
		BlockPos groveAt = feet(helper, 18, 19);
		BlockPos tunnelAt = feet(helper, 24, 24);
		helper.assertTrue(HerobrineMarkPlans.grove(level, groveAt, feet(helper, 18, 3), message(), level.getRandom()) == null,
				"Without approval a grown tree is left alone");
		helper.assertTrue(HerobrineMarkPlans.tunnel(level, tunnelAt, Direction.SOUTH, feet(helper, 24, 8), message()) == null,
				"Without approval a stone wall is not dug, however natural it looks");
		HerobrineMarks.Wild wild = HerobrineMarks.approve(level, feet(helper, 18, 22), 40, "test");
		try {
			helper.assertTrue(HerobrineMarkPlans.grove(level, groveAt, feet(helper, 18, 3), message(), level.getRandom()) != null,
					"An operator's approval is what lets a grove through");
		} finally {
			HerobrineMarks.revoke(level, wild);
		}
		helper.assertTrue(HerobrineMarkPlans.grove(level, groveAt, feet(helper, 18, 3), message(), level.getRandom()) == null,
				"Withdrawing the approval closes it again");
		helper.succeed();
	}

	/** Planning reads nothing until every chunk it could read is loaded: far away, and at the loaded world's very edge. */
	@GameTest(structure = ARENA, padding = 48, skyAccess = true, maxTicks = 20)
	public void planningNeverLoadsAChunk(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos far = feet(helper, 24, 24).offset(40000, 0, 40000);
		helper.assertTrue(!level.hasChunkAt(far), "The far place should start unloaded");
		helper.assertTrue(HerobrineMarkPlans.shrine(level, far, far.north(16), message()) == null
				&& HerobrineMarkPlans.sign(level, far, far.north(16), message()) == null
				&& HerobrineMarkPlans.pyramid(level, far, true, far.north(16), message()) == null
				&& HerobrinePyramids.plan(level, far.getX(), far.getZ(), far.north(40), false) == null, "Nothing can be planned there");
		HerobrineMarks.Wild farWild = HerobrineMarks.approve(level, far, 64, "test");
		try {
			helper.assertTrue(HerobrineMarkPlans.tunnel(level, far, Direction.EAST, far.north(16), message()) == null
					&& HerobrineMarkPlans.grove(level, far, far.north(16), message(), level.getRandom()) == null, "Nor dug, even approved");
		} finally {
			HerobrineMarks.revoke(level, farWild);
		}
		helper.assertTrue(!level.hasChunkAt(far), "Planning should not have loaded the far chunk");
		// The loaded world's edge: walk east to the first unloaded chunk, and plan just inside it.
		BlockPos edge = feet(helper, 24, 24);
		int walked = 0;
		while (level.hasChunkAt(edge.east(16)) && walked++ < 256) {
			edge = edge.east(16);
		}
		BlockPos outside = new BlockPos(((edge.getX() >> 4) + 1) << 4, edge.getY(), edge.getZ());
		helper.assertTrue(level.hasChunkAt(edge) && !level.hasChunkAt(outside), "There should be an edge to the loaded world");
		BlockPos inside = new BlockPos(outside.getX() - 3, edge.getY(), edge.getZ());
		HerobrineMarks.Wild edgeWild = HerobrineMarks.approve(level, inside, 64, "test");
		try {
			helper.assertTrue(HerobrineMarkPlans.shrine(level, inside, inside.west(16), message()) == null
					&& HerobrineMarkPlans.tunnel(level, inside, Direction.EAST, inside.west(16), message()) == null,
					"A plan whose reach crosses into unloaded land is refused");
		} finally {
			HerobrineMarks.revoke(level, edgeWild);
		}
		helper.assertTrue(!level.hasChunkAt(outside), "Planning at the edge should not have loaded the chunk beyond it");
		helper.succeed();
	}

	/** On the real build queue, a block a player sets after building began stops the mark there; it is never overwritten. */
	@GameTest(structure = ARENA, padding = 48, skyAccess = true, maxTicks = 400)
	public void aBlockSetMidBuildStopsAQueuedMark(GameTestHelper helper) {
		platform(helper);
		ServerLevel level = helper.getLevel();
		BlockPos feet = feet(helper, 24, 24);
		HerobrineMarkPlans.Plan plan = HerobrineMarkPlans.pyramid(level, feet, false, feet(helper, 24, 8), message());
		helper.assertTrue(plan != null, "Open ground should take a mossy pyramid");
		BlockPos cap = feet.above(2);
		startThen(helper, plan, 300, id -> helper.runAfterDelay(1, () -> {
			helper.assertTrue(HerobrineMarks.building(id) && level.getBlockState(cap).isAir(), "The cap should not be laid yet");
			level.setBlockAndUpdate(cap, Blocks.OAK_PLANKS.defaultBlockState());
			untilBuilt(helper, id, 300, done -> {
				helper.assertTrue(level.getBlockState(cap).is(Blocks.OAK_PLANKS), "The player's block should never be overwritten");
				BlockPos sign = plan.signs().keySet().iterator().next();
				helper.assertTrue(level.getBlockState(sign).isAir(), "Building should have stopped at the player's block, before the sign");
				HerobrineMarks.Undo undo = HerobrineMarks.undo(level, id);
				helper.assertTrue(undo != null && undo.waiting() == 0 && level.getBlockState(feet).isAir()
						&& level.getBlockState(cap).is(Blocks.OAK_PLANKS), "Undo should take only the mark's blocks: " + undo);
				helper.succeed();
			});
		}));
	}

	/** Starts {@code plan} as soon as a build slot is free, then hands over its id at once. */
	private static void startThen(GameTestHelper helper, HerobrineMarkPlans.Plan plan, int patience, Consumer<String> then) {
		String id = HerobrineMarks.start(helper.getLevel(), plan);
		if (id == null) {
			helper.assertTrue(patience > 0, "No build slot came free");
			helper.runAfterDelay(1, () -> startThen(helper, plan, patience - 1, then));
		} else {
			then.accept(id);
		}
	}

	/** Calls {@code leave} until it leaves a mark (other tests may hold the build slots), then hands over its id at once. */
	private static void whenLeft(GameTestHelper helper, Supplier<String> leave, int patience, Consumer<String> then) {
		String id = leave.get();
		if (id == null) {
			helper.assertTrue(patience > 0, "No mark could be left");
			helper.runAfterDelay(1, () -> whenLeft(helper, leave, patience - 1, then));
		} else {
			then.accept(id);
		}
	}

	private static ServerPlayer witness(GameTestHelper helper, int x, int z) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos at = feet(helper, x, z);
		player.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
		return player;
	}

	/** A trail moves on when its mark is finished, not when it is queued; a mark stopped part-built never moves it. */
	@GameTest(structure = ARENA, padding = 48, skyAccess = true, maxTicks = 600)
	public void aTrailMovesOnOnlyWhenItsMarkIsFinished(GameTestHelper helper) {
		platform(helper);
		ServerLevel level = helper.getLevel();
		HerobrineMarks marks = HerobrineMarks.get(level.getServer());
		BlockPos where = feet(helper, 24, 24);
		ServerPlayer finisher = witness(helper, 24, 8);
		// No land is approved, so the trail begins with a shrine rather than a grove.
		whenLeft(helper, () -> HerobrineMarks.beginTrail(level, where, finisher, level.getRandom()), 300, first -> {
			helper.assertTrue(marks.trail(finisher.getUUID()) == 0, "Queued is not finished: the trail should not move yet");
			untilBuilt(helper, first, 300, built -> {
				helper.assertTrue(marks.marks().get(built).finished(), "The shrine should be recorded finished");
				helper.assertTrue(marks.trail(finisher.getUUID()) == 1, "Finished, it should move the trail on");
				HerobrineMarks.undo(level, built);
				ServerPlayer stopped = witness(helper, 24, 8);
				whenLeft(helper, () -> HerobrineMarks.beginTrail(level, where, stopped, level.getRandom()), 300, second -> {
					// The land is claimed before a block is laid: the mark stops, unfinished.
					ServerPlayer admin = helper.makeMockServerPlayerInLevel();
					LandRegistry land = LandRegistry.get(level.getServer());
					FinanceBook book = Ledger.get(level.getServer()).book();
					long key = LandRegistry.keyOf(where);
					helper.assertTrue(land.assign(book, key, book.account(UUID.randomUUID(), "HerobrineTrailOwner"), 0).ok(), "The claim fixture");
					untilBuilt(helper, second, 300, over -> {
						land.abandon(admin, book, key, true, 0);
						HerobrineMarks.Mark mark = marks.marks().get(over);
						helper.assertTrue(mark == null || !mark.finished(), "A stopped mark is never finished");
						helper.assertTrue(marks.trail(stopped.getUUID()) == 0, "A stopped mark should never move the trail");
						if (mark != null) {
							HerobrineMarks.undo(level, over);
						}
						helper.succeed();
					});
				});
			});
		});
	}

	/** The four lines of the sign a mark left, front side, or null. */
	private static String[] signLines(ServerLevel level, HerobrineMarks.Mark mark) {
		for (HerobrineMarks.Change change : mark.changes()) {
			if (level.getBlockEntity(change.pos()) instanceof SignBlockEntity sign) {
				List<Component> messages = sign.getText(SignTextSlot.FRONT).getMessages(false);
				return messages.stream().map(Component::getString).toArray(String[]::new);
			}
		}
		return null;
	}

	/**
	 * A pyramid stopped part-built by a player's block is never pointed to: while it rises and after it stops, a trail
	 * sighting leaves no signpost and the trail stays put. Once a pyramid on that shore is finished, the next sighting
	 * leaves the signpost, pointing to it, and the trail moves on when the signpost is finished.
	 */
	@GameTest(structure = ARENA, padding = 48, skyAccess = true, maxTicks = 1200)
	public void anAbortedPyramidIsNeverPointedToAndTheTrailWaitsForAFinishedOne(GameTestHelper helper) {
		platform(helper);
		pool(helper);
		ServerLevel level = helper.getLevel();
		HerobrineMarks marks = HerobrineMarks.get(level.getServer());
		BlockPos centre = feet(helper, 24, 24);
		// The signpost's place: on a natural stone pillar beside the pyramid, six high, so its margin (6 blocks round,
		// 2 below) clears both the arena's walls and the pyramid's lowest steps, the only ones that near the edge.
		for (int y = GROUND + 1; y <= GROUND + 6; y++) {
			helper.setBlock(new BlockPos(6, y, 24), Blocks.STONE);
		}
		BlockPos where = feet(helper, 6, 24).above(6);
		ServerPlayer player = witness(helper, 6, 30);
		whenLeft(helper, () -> HerobrineMarks.beginTrail(level, centre, player, level.getRandom()), 300, first -> untilBuilt(helper, first, 300, built -> {
			helper.assertTrue(marks.trail(player.getUUID()) == 1, "The trail's first mark should be finished");
			HerobrineMarks.undo(level, built);
			HerobrineMarkPlans.Plan plan = HerobrinePyramids.plan(level, centre.getX(), centre.getZ(), feet(helper, 24, 0), false);
			helper.assertTrue(plan != null, "Level, wild land by the water should take a pyramid");
			startThen(helper, plan, 300, id -> {
				// A player's block in its floor, a few rows in: the pyramid will stop there, part-built.
				BlockPos inTheWay = centre.offset(-HerobrinePyramids.HALF + 2, 0, 0);
				level.setBlockAndUpdate(inTheWay, Blocks.OAK_PLANKS.defaultBlockState());
				helper.assertTrue(HerobrineMarks.continueTrail(level, where, player, level.getRandom()) == null,
						"While the pyramid rises a sighting should leave no signpost");
				helper.assertTrue(HerobrinePyramids.nearest(level, centre, 64) == null, "A rising pyramid is never pointed to");
				helper.assertTrue(marks.trail(player.getUUID()) == 1, "The trail should wait");
				untilBuilt(helper, id, 900, stopped -> {
					HerobrineMarks.Mark mark = marks.marks().get(stopped);
					helper.assertTrue(mark != null && !mark.finished() && !mark.changes().isEmpty(),
							"It should stop part-built: recorded, undoable, unfinished");
					helper.assertTrue(level.getBlockState(inTheWay).is(Blocks.OAK_PLANKS), "The player's block should stand");
					helper.assertTrue(HerobrinePyramids.nearest(level, centre, 64) == null, "A pyramid stopped part-built is never pointed to");
					helper.assertTrue(marks.trail(player.getUUID()) == 1, "The trail should still wait");
					// Later the ruin is undone and a pyramid on the same shore is finished.
					HerobrineMarks.undo(level, stopped);
					level.setBlockAndUpdate(inTheWay, Blocks.AIR.defaultBlockState());
					HerobrineMarkPlans.Plan again = HerobrinePyramids.plan(level, centre.getX(), centre.getZ(), feet(helper, 24, 0), false);
					helper.assertTrue(again != null, "The cleared shore should take a pyramid again");
					String pyramid = HerobrineMarks.buildNow(level, again);
					helper.assertTrue(marks.marks().get(pyramid).finished() && centre.equals(HerobrinePyramids.nearest(level, centre, 64)),
							"A finished pyramid is pointed to");
					whenLeft(helper, () -> HerobrineMarks.continueTrail(level, where, player, level.getRandom()), 300, post -> {
						helper.assertTrue(marks.trail(player.getUUID()) == 1, "Queued is not finished: the trail should not move yet");
						untilBuilt(helper, post, 300, signpost -> {
							helper.assertTrue(marks.trail(player.getUUID()) == 2, "The finished signpost should move the trail on");
							String[] lines = signLines(level, marks.marks().get(signpost));
							helper.assertTrue(lines != null && lines[0].equals("THE WATER") && lines[1].equals("REMEMBERS")
									&& lines[2].equals("EAST") && lines[3].equals("18 BLOCKS"),
									"The signpost should point east, 18 blocks, to the pyramid: " + java.util.Arrays.toString(lines));
							HerobrineMarks.undo(level, signpost);
							HerobrineMarks.undo(level, pyramid);
							helper.succeed();
						});
					});
				});
			});
		}));
	}

	/** Undo keeps a mark's sign edited in any way: its back, a line's style, its dye, glow ink or wax. */
	@GameTest(structure = ARENA, padding = 48, skyAccess = true, maxTicks = 40)
	public void undoKeepsASignEditedInAnyWay(GameTestHelper helper) {
		platform(helper);
		ServerLevel level = helper.getLevel();
		String[] edits = {"back", "style", "dye", "glow", "wax", "none"};
		int[][] sites = {{12, 12}, {24, 12}, {36, 12}, {12, 36}, {24, 36}, {36, 36}};
		for (int i = 0; i < edits.length; i++) {
			BlockPos feet = feet(helper, sites[i][0], sites[i][1]);
			HerobrineMarkPlans.Plan plan = HerobrineMarkPlans.shrine(level, feet, feet.north(16), message());
			helper.assertTrue(plan != null, "Open ground should take a shrine at " + feet);
			String id = HerobrineMarks.buildNow(level, plan);
			BlockPos pos = plan.signs().keySet().iterator().next();
			helper.assertTrue(level.getBlockEntity(pos) instanceof SignBlockEntity, "The shrine's sign should stand");
			SignBlockEntity sign = (SignBlockEntity) level.getBlockEntity(pos);
			switch (edits[i]) {
				case "back" -> sign.setText(sign.getText(SignTextSlot.BACK).asMutable().setLine(0, Component.literal("EDITED")).asImmutable(), SignTextSlot.BACK);
				case "style" -> sign.setText(sign.getText(SignTextSlot.FRONT).asMutable().setLine(0, Component.literal(FIRST[0]).withStyle(ChatFormatting.BOLD)).asImmutable(),
						SignTextSlot.FRONT);
				case "dye" -> sign.setText(sign.getText(SignTextSlot.FRONT).withColor(DyeColor.RED), SignTextSlot.FRONT);
				case "glow" -> sign.setText(sign.getText(SignTextSlot.FRONT).withGlowingText(true), SignTextSlot.FRONT);
				case "wax" -> sign.setWaxed(false);
				default -> {
				}
			}
			HerobrineMarks.Undo undo = HerobrineMarks.undo(level, id);
			boolean edited = !edits[i].equals("none");
			helper.assertTrue(undo != null && undo.kept() == (edited ? 1 : 0) && level.getBlockState(pos).is(Blocks.OAK_SIGN) == edited,
					edited ? "A sign edited (" + edits[i] + ") should be kept: " + undo : "An untouched sign should be taken back: " + undo);
		}
		helper.succeed();
	}
}
