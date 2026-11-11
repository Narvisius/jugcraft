package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.building.RainCover;
import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.nuclear.RestroomBlocks;
import io.github.jimbozoomer.jugcraft.tower.RoomKit;
import io.github.jimbozoomer.jugcraft.tower.RoomKitBlock;
import io.github.jimbozoomer.jugcraft.tower.RoomKitBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.RoomKitDisplayBlock;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The upper floor's room kits as blocks (work list item 22 part B, tower/RoomKit): one item places a whole piece facing
 * whoever places it, and any cell broken takes the piece with it, dropping the one item; rows join and the tier risers
 * show their nosing where they step down; a wall screen keeps its block entity at its anchor and changes what it shows
 * on a sneak-click; a toilet takes a stall partition along its side and gives it back when broken; every cell with a
 * live face (B2) holds a block entity, and the bottle filler fills bottles and counts them; the mod's roofs keep the rain
 * out (building/RainCover).
 */
public class RoomKitGameTests {
	private static RoomKitBlock kit(String id) {
		return RoomKit.BLOCKS.get(id);
	}

	private static void clearAround(ServerLevel level, BlockPos from, BlockPos to) {
		for (BlockPos p : BlockPos.betweenClosed(from, to)) {
			level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
		}
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(Vec3.atLowerCornerOf(from), Vec3.atLowerCornerOf(to).add(1, 1, 1)))) {
			item.discard();
		}
	}

	/** Every cell of the piece anchored at {@code anchor} is there, of this piece and this anchor. */
	static void assertWhole(GameTestHelper helper, ServerLevel level, BlockPos anchor, String id) {
		BlockState state = level.getBlockState(anchor);
		RoomKitBlock kit = kit(id);
		helper.assertTrue(kit != null && state.is(kit) && kit.isAnchor(state), "the " + id + "'s anchor at " + anchor + ": " + state);
		List<BlockPos> cells = kit.cells(anchor, state);
		helper.assertTrue(cells.size() == kit.spec.cells().size(), "the " + id + " has " + kit.spec.cells().size() + " cells, got " + cells.size());
		for (BlockPos c : cells) {
			BlockState there = level.getBlockState(c);
			helper.assertTrue(there.is(kit) && kit.anchor(c, there).equals(anchor), "the " + id + "'s cell at " + c + ": " + there);
		}
	}

	/**
	 * Each multi-block piece put in by its anchor alone (as the tower's build or a command does) comes whole; breaking a
	 * cell that is not its anchor takes all of it and drops the one item.
	 */
	@GameTest(structure = "jugcraft-test:drone_depot")
	public void piecesPlaceWholeAndBreakAsOne(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos anchor = helper.absolutePos(new BlockPos(5, 2, 3));   // (each piece fits the 16 x 8 x 16 test area from here)
		for (String id : new String[] {"pilot_simulator", "boardroom_table", "ops_wall_screen_large", "kitchen_run", "exam_couch", "crew_bunk",
				"vending_machine", "lounge_rug", "briefing_sign", "office_bench"}) {
			RoomKitBlock kit = kit(id);
			clearAround(level, helper.absolutePos(BlockPos.ZERO), helper.absolutePos(new BlockPos(15, 7, 15)));
			level.setBlock(anchor, kit.defaultBlockState(), Block.UPDATE_ALL);
			assertWhole(helper, level, anchor, id);
			List<BlockPos> cells = kit.cells(anchor, level.getBlockState(anchor));
			BlockPos last = cells.get(cells.size() - 1);
			level.destroyBlock(last, true);
			for (BlockPos c : cells) {
				helper.assertTrue(level.getBlockState(c).isAir(), "breaking one cell of the " + id + " took its cell at " + c + ": " + level.getBlockState(c));
			}
			int dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(last).inflate(8)).stream()
					.filter(e -> e.getItem().is(kit.asItem())).mapToInt(e -> e.getItem().getCount()).sum();
			helper.assertTrue(dropped == 1, "the " + id + " dropped one item, got " + dropped);
		}
		helper.succeed();
	}

	/** Placed from its item, a piece faces the player who placed it and comes whole; it will not go where it does not fit. */
	@GameTest(structure = "jugcraft-test:drone_depot")
	public void placedPieceFacesThePlayer(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		Player player = helper.makeMockPlayer(GameType.CREATIVE);
		player.setYRot(180);                                   // looking north: the piece faces south, to the player
		BlockPos floor = helper.absolutePos(new BlockPos(8, 1, 8));
		level.setBlock(floor, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
		RoomKitBlock sim = kit("pilot_simulator");
		var context = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, new ItemStack(sim),
				new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false));
		BlockState state = sim.getStateForPlacement(context);
		helper.assertTrue(state != null && state.getValue(RoomKitBlock.FACING_PROP) == Direction.SOUTH, "the simulator faces the player: " + state);
		BlockPos at = floor.above();
		level.setBlock(at, state, Block.UPDATE_ALL);
		sim.setPlacedBy(level, at, state, player, new ItemStack(sim));
		assertWhole(helper, level, at, "pilot_simulator");
		// a block in the way of a cell: no room for another one there
		BlockPos next = at.west(3);
		level.setBlock(next.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(next.above(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
		var blocked = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, new ItemStack(sim),
				new BlockHitResult(Vec3.atCenterOf(next.below()).add(0, 0.5, 0), Direction.UP, next.below(), false));
		helper.assertTrue(sim.getStateForPlacement(blocked) == null, "a simulator does not go where a block is in its way");
		helper.succeed();
	}

	/** Leather sofas side by side facing the same way join up; a tier riser shows its nosing only where it steps down. */
	@GameTest(structure = "jugcraft-test:drone_depot")
	public void rowsJoinAndRisersStepDown(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		RoomKitBlock sofa = kit("leather_sofa");
		BlockPos a = helper.absolutePos(new BlockPos(4, 1, 4));
		BlockPos b = a.east();
		level.setBlock(a, sofa.defaultBlockState().setValue(RoomKitBlock.FACING_PROP, Direction.NORTH), Block.UPDATE_ALL);
		level.setBlock(b, sofa.defaultBlockState().setValue(RoomKitBlock.FACING_PROP, Direction.NORTH), Block.UPDATE_ALL);
		BlockState west = level.getBlockState(a), east = level.getBlockState(b);
		// facing north, the sitter's left is the west: the west sofa joins on its right, the east one on its left
		helper.assertTrue(west.toString().contains("right=true") && west.toString().contains("left=false"), "the west sofa joins the east one: " + west);
		helper.assertTrue(east.toString().contains("left=true") && east.toString().contains("right=false"), "the east sofa joins the west one: " + east);
		RoomKitBlock riser = kit("tier_riser");
		BlockPos r0 = helper.absolutePos(new BlockPos(4, 1, 9));
		for (int i = 0; i < 3; i++) {
			level.setBlock(r0.east(i), riser.defaultBlockState(), Block.UPDATE_ALL);
		}
		BlockState first = level.getBlockState(r0), middle = level.getBlockState(r0.east());
		helper.assertTrue(first.toString().contains("edge_west=true") && first.toString().contains("edge_east=false"),
				"the row's west end steps down on its west side: " + first);
		helper.assertTrue(middle.toString().contains("edge_west=false") && middle.toString().contains("edge_east=false")
				&& middle.toString().contains("edge_north=true"), "the middle riser steps down only to the north and south: " + middle);
		helper.succeed();
	}

	/** A wall screen keeps its block entity at its anchor; a sneak-click on any cell changes what all of it shows. */
	@GameTest(structure = "jugcraft-test:drone_depot")
	public void wallScreenChangesRole(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		var screen = (RoomKitDisplayBlock) kit("ops_wall_screen");
		BlockPos anchor = helper.absolutePos(new BlockPos(8, 4, 8));
		level.setBlock(anchor, screen.defaultBlockState(), Block.UPDATE_ALL);
		assertWhole(helper, level, anchor, "ops_wall_screen");
		helper.assertTrue(level.getBlockEntity(anchor) instanceof DepotDisplayBlockEntity, "the screen's display at its anchor");
		List<BlockPos> cells = screen.cells(anchor, level.getBlockState(anchor));
		helper.assertTrue(cells.stream().skip(1).noneMatch(c -> level.getBlockEntity(c) != null), "only the anchor has a block entity");
		Player player = helper.makeMockPlayer(GameType.CREATIVE);
		player.setShiftKeyDown(true);
		BlockPos other = cells.get(cells.size() - 1);
		level.getBlockState(other).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(other), Direction.NORTH, other, false));
		for (BlockPos c : cells) {
			helper.assertTrue(level.getBlockState(c).getValue(screen.role()) == 1, "every cell shows role 1 now: " + level.getBlockState(c));
		}
		helper.assertTrue(RoomKitDisplayBlock.LESSON == 15 && screen.role().getPossibleValues().size() == RoomKitDisplayBlock.LESSON + 1,
				"the desks' fifteen roles and the lesson");
		helper.succeed();
	}

	/** A toilet partition used on a toilet stands along its side (and collides); broken, the toilet gives both back. */
	@GameTest(structure = "jugcraft-test:drone_depot")
	public void toiletTakesAPartition(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		Block toilet = BuiltInRegistries.BLOCK.getValue(Jugcraft.id("restroom_toilet"));
		BlockPos at = helper.absolutePos(new BlockPos(8, 1, 8));
		level.setBlock(at, toilet.defaultBlockState(), Block.UPDATE_ALL);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack panel = new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id("restroom_stall_partition")), 2);
		player.setItemInHand(InteractionHand.MAIN_HAND, panel);
		level.getBlockState(at).useItemOn(panel, level, player, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false));
		BlockState fitted = level.getBlockState(at);
		helper.assertTrue(fitted.getValue(RestroomBlocks.PARTITION), "the partition stands along the toilet: " + fitted);
		helper.assertTrue(panel.getCount() == 1, "one partition used, got " + panel.getCount());
		var shape = fitted.getCollisionShape(level, at);
		helper.assertTrue(shape.max(Direction.Axis.Y) > 1.4 && shape.min(Direction.Axis.X) < 0.06, "its panel collides on the toilet's west side: " + shape);
		level.destroyBlock(at, true);
		var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(3));
		helper.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(toilet.asItem()))
				&& drops.stream().anyMatch(e -> e.getItem().is(panel.getItem())), "the toilet and its partition dropped: " + drops);
		helper.succeed();
	}

	/**
	 * Every cell of a piece with a live face (a clock's figures, a laptop's page, a simulator's HUD, the filler's
	 * counter) holds the block entity the renderer draws it from, and no other cell does; a glass bottle held to the
	 * bottle filler comes away full of water and its counter counts it.
	 */
	@GameTest(structure = "jugcraft-test:drone_depot")
	public void liveFacesHoldBlockEntitiesAndTheFillerCounts(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos anchor = helper.absolutePos(new BlockPos(5, 2, 3));
		for (String id : new String[] {"digital_wall_clock", "training_desk", "pilot_simulator", "zone_clocks", "electronics_bench", "bottle_filler"}) {
			var kit = (RoomKitDisplayBlock) kit(id);
			clearAround(level, helper.absolutePos(BlockPos.ZERO), helper.absolutePos(new BlockPos(15, 7, 15)));
			level.setBlock(anchor, kit.defaultBlockState(), Block.UPDATE_ALL);
			assertWhole(helper, level, anchor, id);
			int live = 0;
			for (BlockPos c : kit.cells(anchor, level.getBlockState(anchor))) {
				BlockState there = level.getBlockState(c);
				boolean faces = !kit.live(there).isEmpty();
				live += faces ? 1 : 0;
				helper.assertTrue(faces == level.getBlockEntity(c) instanceof RoomKitBlockEntity, "the " + id + "'s cell " + there + " has "
						+ (faces ? "live faces and needs" : "none and wants no") + " block entity: " + level.getBlockEntity(c));
			}
			helper.assertTrue(live > 0, "the " + id + " has a live face");
		}
		var filler = (RoomKitDisplayBlock) kit("bottle_filler");
		BlockPos counterCell = filler.cells(anchor, level.getBlockState(anchor)).stream()
				.filter(c -> filler.live(level.getBlockState(c)).stream().anyMatch(f -> f.kind().equals("counter"))).findFirst().orElseThrow();
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack bottles = new ItemStack(Items.GLASS_BOTTLE, 3);
		player.setItemInHand(InteractionHand.MAIN_HAND, bottles);
		for (int k = 0; k < 2; k++) {
			level.getBlockState(anchor).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(anchor), Direction.NORTH, anchor, false));
		}
		helper.assertTrue(player.getMainHandItem().is(Items.GLASS_BOTTLE) && player.getMainHandItem().getCount() == 1, "two bottles filled: "
				+ player.getMainHandItem());
		int water = 0;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack st = player.getInventory().getItem(i);
			if (st.is(Items.POTION) && st.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.WATER)) {
				water += st.getCount();
			}
		}
		helper.assertTrue(water == 2, "two water bottles, got " + water);
		helper.assertTrue(level.getBlockEntity(counterCell) instanceof RoomKitBlockEntity counter && counter.count() == 2,
				"the filler's counter counted two: " + level.getBlockEntity(counterCell));
		helper.succeed();
	}

	/**
	 * Jugcraft's roofs keep the rain out (Justin, 27 Oct 2026: "despite being inside, it rains"; RainCover): a roof of the
	 * tower's reinforced concrete stands in the motion-blocking heightmaps (where rain stops, and where it is raining at
	 * all), as vanilla stone does; a ceiling light panel (thin, not solid) does not; taking the roof away lets the rain in.
	 */
	@GameTest(structure = "jugcraft-test:drone_depot")
	public void roofsKeepTheRainOut(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		Block concrete = BuiltInRegistries.BLOCK.getValue(Jugcraft.id("reinforced_concrete"));
		Block vinyl = BuiltInRegistries.BLOCK.getValue(Jugcraft.id("vinyl_floor"));
		Block panel = BuiltInRegistries.BLOCK.getValue(Jugcraft.id("ceiling_light_panel"));
		helper.assertTrue(RainCover.covers(concrete.defaultBlockState()) && RainCover.covers(vinyl.defaultBlockState()),
				"the tower's concrete and the plant's vinyl floor keep the rain out");
		helper.assertFalse(RainCover.covers(panel.defaultBlockState()), "a thin ceiling light panel does not");
		helper.assertFalse(RainCover.covers(Blocks.STONE.defaultBlockState()), "vanilla's blocks are left to the game's own tag");
		// one column, cleared well up over the test
		BlockPos floor = helper.absolutePos(new BlockPos(5, 1, 5));
		for (int dy = 1; dy <= 40; dy++) {
			level.setBlock(floor.above(dy), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
		}
		level.setBlock(floor, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
		BlockPos roof = floor.above(6);
		level.setBlock(roof, concrete.defaultBlockState(), Block.UPDATE_ALL);
		for (Heightmap.Types type : RainCover.TYPES) {
			int top = level.getHeight(type, floor.getX(), floor.getZ());
			helper.assertTrue(top == roof.getY() + 1, type + ": the rain stops on the concrete roof, top " + top + " roof " + roof.getY());
		}
		level.setBlock(roof, panel.defaultBlockState(), Block.UPDATE_ALL);
		int open = level.getHeight(Heightmap.Types.MOTION_BLOCKING, floor.getX(), floor.getZ());
		helper.assertTrue(open == floor.getY() + 1, "under a light panel alone it rains down to the floor: top " + open);
		level.setBlock(roof, vinyl.defaultBlockState(), Block.UPDATE_ALL);
		int covered = level.getHeight(Heightmap.Types.MOTION_BLOCKING, floor.getX(), floor.getZ());
		helper.assertTrue(covered == roof.getY() + 1, "a vinyl floor over it keeps the rain out again: top " + covered);
		level.setBlock(roof, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(floor, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		helper.succeed();
	}
}
