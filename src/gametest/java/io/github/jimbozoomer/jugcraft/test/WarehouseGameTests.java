package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.warehouse.CaseLoad;
import io.github.jimbozoomer.jugcraft.warehouse.DockBlock;
import io.github.jimbozoomer.jugcraft.warehouse.DockBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.DroneKind;
import io.github.jimbozoomer.jugcraft.warehouse.Drums;
import io.github.jimbozoomer.jugcraft.warehouse.EmptierBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.FillerBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.IntakeBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.JugcraftWarehouse;
import io.github.jimbozoomer.jugcraft.warehouse.LaneBlock;
import io.github.jimbozoomer.jugcraft.warehouse.Order;
import io.github.jimbozoomer.jugcraft.warehouse.PalletLoad;
import io.github.jimbozoomer.jugcraft.warehouse.PieceBlock;
import io.github.jimbozoomer.jugcraft.warehouse.RackBlock;
import io.github.jimbozoomer.jugcraft.warehouse.RackBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.ReadyPool;
import io.github.jimbozoomer.jugcraft.warehouse.ReadyStorageBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.StationBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.TerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.Warehouses;
import io.github.jimbozoomer.jugcraft.warehouse.WarehousePiece;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/**
 * The automated warehouse (claude/jugcraft-warehouse-plan.md): a small warehouse built in the arena (a lane field, an
 * Intake Case Packer and a Palletizing Station facing it, a rack of pallet places, a terminal, two docks with a stacker
 * drone and a pallet drone). Part W1: what goes into the intake is packed into cases, stacked on a pallet, wrapped and
 * put away in a rack place, and the terminal's books say so. Part W2: an order's pallet is fetched to Ready Storage's
 * hatch, what it wants comes off into Ready Storage, and the rest goes back to its place.
 */
public class WarehouseGameTests {
	private static final String ARENA = "jugcraft-test:drone_tower";

	private static BlockPos abs(GameTestHelper helper, int x, int y, int z) {
		return helper.absolutePos(new BlockPos(x, y, z));
	}

	/** A piece placed as its item would place it: every part, facing the way its user looks. */
	private static BlockPos piece(GameTestHelper helper, Block block, WarehousePiece piece, int x, int z, Direction facing) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = abs(helper, x, 1, z);
		BlockState state = block.defaultBlockState().setValue(PieceBlock.FACING, facing);
		PieceBlock pb = (PieceBlock) block;
		for (int p = 0; p < piece.parts(); p++) {
			level.setBlockAndUpdate(origin.offset(piece.offset(facing, p)), state.setValue(pb.partProperty(), p));
		}
		return origin;
	}

	/**
	 * The test warehouse: the stone floor, lanes over x 1..12, z 4..8; the intake (x 3..5, z 2..3) and the station
	 * (x 7, z 2..3) facing north with their fronts to the lanes; the terminal (x 10..12, z 3); docks at (1, 6) and
	 * (1, 7) facing west; a rack of places x 6..9, z 9, three high, facing south (into it from the aisle at z 8).
	 */
	private static BlockPos build(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 12; z++) {
				level.setBlockAndUpdate(abs(helper, x, 0, z), Blocks.STONE.defaultBlockState());
			}
		}
		for (int x = 1; x <= 12; x++) {
			for (int z = 4; z <= 8; z++) {
				level.setBlockAndUpdate(abs(helper, x, 0, z), JugcraftWarehouse.LANE.defaultBlockState());
			}
		}
		piece(helper, JugcraftWarehouse.INTAKE, WarehousePiece.INTAKE, 4, 3, Direction.NORTH);
		piece(helper, JugcraftWarehouse.STATION, WarehousePiece.STATION, 7, 3, Direction.NORTH);
		BlockPos terminal = piece(helper, JugcraftWarehouse.TERMINAL, WarehousePiece.TERMINAL, 11, 3, Direction.NORTH);
		unlimited(helper, terminal);
		for (int z : new int[] {6, 7}) {
			level.setBlockAndUpdate(abs(helper, 1, 1, z), JugcraftWarehouse.DOCK.defaultBlockState().setValue(DockBlock.FACING, Direction.WEST));
		}
		for (int x = 6; x <= 9; x++) {
			for (int y = 1; y <= 3; y++) {
				level.setBlockAndUpdate(abs(helper, x, y, 9), JugcraftWarehouse.RACK.defaultBlockState().setValue(RackBlock.FACING, Direction.SOUTH));
			}
		}
		return terminal;
	}

	/** Part W5: the tests' warehouses run without a switchboard (its own test powers one by cable). */
	private static void unlimited(GameTestHelper helper, BlockPos terminal) {
		if (helper.getLevel().getBlockEntity(terminal) instanceof TerminalBlockEntity t) {
			t.setUnlimitedPower(true);
		}
	}

	/** Part W2: lanes on to x 15, Ready Storage facing north at x 13..15, z 2..3 (its bed at (14, 4), worked from (14, 5)). */
	private static BlockPos readyStorage(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (int x = 13; x <= 15; x++) {
			for (int z = 4; z <= 8; z++) {
				level.setBlockAndUpdate(abs(helper, x, 0, z), JugcraftWarehouse.LANE.defaultBlockState());
			}
		}
		return piece(helper, JugcraftWarehouse.READY_STORAGE, WarehousePiece.READY_STORAGE, 14, 3, Direction.NORTH);
	}

	private static void park(GameTestHelper helper, int z, DroneKind kind) {
		DockBlockEntity dock = (DockBlockEntity) helper.getLevel().getBlockEntity(abs(helper, 1, 1, z));
		helper.assertTrue(dock != null, "a dock at (1, " + z + ")");
		ItemStack drone = new ItemStack(kind == DroneKind.STACKER ? JugcraftWarehouse.STACKER_DRONE : JugcraftWarehouse.PALLET_DRONE);
		dock.park(helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL), drone, kind);
	}

	/** The lanes join up: a tile with lanes all round shows no yellow line, one at the field's edge does. */
	@GameTest(structure = ARENA, maxTicks = 20, skyAccess = true)
	public void lanesShowTheirEdges(GameTestHelper helper) {
		if (TestFocus.skip("warehouse")) {
			helper.succeed();
			return;
		}
		build(helper);
		helper.runAfterDelay(2, () -> {
			int inner = helper.getLevel().getBlockState(abs(helper, 6, 0, 6)).getValue(LaneBlock.MASK);
			int edge = helper.getLevel().getBlockState(abs(helper, 1, 0, 4)).getValue(LaneBlock.MASK);
			helper.assertTrue(inner == 0, "an inner tile has no edge lines (mask " + inner + ")");
			helper.assertTrue(edge == (1 | 8), "the north-west corner tile has lines north and west (mask " + edge + ")");
			RackBlock.RackLevel low = helper.getLevel().getBlockState(abs(helper, 6, 1, 9)).getValue(RackBlock.LEVEL);
			RackBlock.RackLevel mid = helper.getLevel().getBlockState(abs(helper, 6, 2, 9)).getValue(RackBlock.LEVEL);
			helper.assertTrue(low == RackBlock.RackLevel.GROUND && mid == RackBlock.RackLevel.FIRST, "the rack's levels join up");
			helper.succeed();
		});
	}

	/**
	 * Eight full cases of cobblestone put into the intake end up as one wrapped pallet in a rack place, put there by the
	 * drones, and the terminal knows it (2,048 cobblestone, one place used, its code on the rack).
	 */
	@GameTest(structure = ARENA, maxTicks = 3200, skyAccess = true)
	public void whatGoesInIsPalletizedAndPutAway(GameTestHelper helper) {
		if (TestFocus.skip("warehouse")) {
			helper.succeed();
			return;
		}
		BlockPos terminalPos = build(helper);
		park(helper, 6, DroneKind.STACKER);
		park(helper, 7, DroneKind.PALLET);
		StationBlockEntity station = (StationBlockEntity) helper.getLevel().getBlockEntity(abs(helper, 7, 1, 3));
		helper.assertTrue(station != null, "the station's block entity");
		station.addPallets(4);
		IntakeBlockEntity intake = (IntakeBlockEntity) helper.getLevel().getBlockEntity(abs(helper, 4, 1, 3));
		helper.assertTrue(intake != null, "the intake's block entity");
		try (Transaction t = Transaction.openOuter()) {
			long in = intake.input().insert(ItemVariant.of(Items.COBBLESTONE), 2048, t);
			t.commit();
			helper.assertTrue(in == 2048, "the intake took all 2,048 (took " + in + ")");
		}
		helper.succeedWhen(() -> {
			int full = 0;
			for (int x = 6; x <= 9; x++) {
				for (int y = 1; y <= 3; y++) {
					RackBlockEntity rack = (RackBlockEntity) helper.getLevel().getBlockEntity(abs(helper, x, y, 9));
					if (rack != null && rack.load() != null) {
						full++;
						helper.assertTrue(rack.load().size() == 8, "the pallet has 8 cases (" + rack.load().size() + ")");
						helper.assertTrue(!rack.label().isEmpty(), "the place has its code");
					}
				}
			}
			helper.assertTrue(full == 1, "one pallet put away (" + full + ")");
			TerminalBlockEntity terminal = (TerminalBlockEntity) helper.getLevel().getBlockEntity(terminalPos);
			helper.assertTrue(terminal != null && terminal.placesUsed() == 1, "the terminal counts one place used");
			long cobble = terminal.stock().values().stream().filter(s -> s.item.is(Items.COBBLESTONE)).mapToLong(s -> s.count).sum();
			helper.assertTrue(cobble == 2048, "the terminal's stock has 2,048 cobblestone (" + cobble + ")");
		});
	}

	/**
	 * Part W2: an order for 100 coal from a stored pallet of 2,048: the pallet drone fetches the pallet to Ready Storage's
	 * hatch, its pick cell splits a case, Ready Storage holds the 100 for the order (READY), and the pallet goes back to
	 * its place with 1,948 on it.
	 */
	@GameTest(structure = ARENA, maxTicks = 2400, skyAccess = true)
	public void anOrderComesOutIntoReadyStorage(GameTestHelper helper) {
		if (TestFocus.skip("warehouse")) {
			helper.succeed();
			return;
		}
		BlockPos terminalPos = build(helper);
		BlockPos rsPos = readyStorage(helper);
		park(helper, 7, DroneKind.PALLET);
		RackBlockEntity place = (RackBlockEntity) helper.getLevel().getBlockEntity(abs(helper, 6, 1, 9));
		helper.assertTrue(place != null, "a rack place at (6, 1, 9)");
		java.util.List<CaseLoad> cases = new java.util.ArrayList<>();
		for (int k = 0; k < 8; k++) {
			cases.add(new CaseLoad(new ItemStack(Items.COAL), 256));
		}
		place.setLoad(new PalletLoad(cases));
		int[] id = {0};
		helper.runAfterDelay(30, () -> {
			TerminalBlockEntity terminal = (TerminalBlockEntity) helper.getLevel().getBlockEntity(terminalPos);
			helper.assertTrue(terminal != null && terminal.pool().capacity() == ReadyPool.MODULE, "Ready Storage's 108 slots are counted");
			Order o = terminal.placeOrder(null, "test", new ItemStack(Items.COAL), 100, Order.WINDOW, "TEST", helper.getLevel().getGameTime());
			helper.assertTrue(o != null, "the order is placed");
			id[0] = o.id;
		});
		helper.succeedWhen(() -> {
			TerminalBlockEntity terminal = (TerminalBlockEntity) helper.getLevel().getBlockEntity(terminalPos);
			helper.assertTrue(terminal != null && id[0] > 0, "the order was placed");
			Order o = terminal.order(id[0]);
			helper.assertTrue(o != null && o.state == Order.READY, "the order is READY (" + (o == null ? "gone" : Order.STATES[o.state]) + ")");
			long coal = 0;
			for (int i = 0; i < terminal.pool().size(); i++) {
				if (terminal.pool().get(i).is(Items.COAL) && terminal.pool().order(i) == id[0]) {
					coal += terminal.pool().get(i).getCount();
				}
			}
			helper.assertTrue(coal == 100, "Ready Storage holds the 100 coal (" + coal + ")");
			RackBlockEntity back = (RackBlockEntity) helper.getLevel().getBlockEntity(abs(helper, 6, 1, 9));
			helper.assertTrue(back != null && back.load() != null, "the pallet is back in its place");
			long left = back.load().cases().stream().mapToLong(CaseLoad::count).sum();
			helper.assertTrue(left == 2048 - 100, "1,948 coal went back (" + left + ")");
			ReadyStorageBlockEntity rs = (ReadyStorageBlockEntity) helper.getLevel().getBlockEntity(rsPos);
			helper.assertTrue(rs != null && rs.free(), "the hatch is free again");
		});
	}

	/**
	 * Part W3: the Small Warehouse staked with three floors is its parts stacked (base, a middle floor, the top); its
	 * terminal finds it, three floors; ADD A FLOOR takes the ladder up to the roof hatch and the lift's drive off the
	 * old top floor (given back), turns the roof's band of wall plain and stakes the fourth floor over the roof.
	 */
	@GameTest(structure = "jugcraft-test:drone_tower_full", maxTicks = 60, skyAccess = true)
	public void aPreMadeWarehouseGrowsAFloor(GameTestHelper helper) {
		if (TestFocus.skip("warehouse")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		io.github.jimbozoomer.jugcraft.blueprint.Blueprint b = io.github.jimbozoomer.jugcraft.blueprint.Blueprint.get("small_warehouse@3", false);
		var s = io.github.jimbozoomer.jugcraft.blueprint.BlueprintStacks.of("small_warehouse@3");
		helper.assertTrue(b != null && s != null && b.size() > 6000, "the Small Warehouse stacks three floors");
		BlockPos stake = abs(helper, s.anchor().getX() + 2, 2, s.anchor().getZ() + 2);
		var site = new io.github.jimbozoomer.jugcraft.blueprint.BuildSite(b, net.minecraft.world.level.block.Rotation.NONE, stake);
		for (int i = 0; i < site.size(); i++) {
			level.setBlock(site.blockPos(i), site.state(i), Block.UPDATE_ALL);
		}
		// A file position of the stack (the base's frame) in the world.
		java.util.function.IntFunction<java.util.function.IntFunction<java.util.function.IntFunction<BlockPos>>> at = x -> y -> z ->
				stake.offset(x - s.anchor().getX(), y - s.anchor().getY(), z - s.anchor().getZ());
		BlockPos terminalPos = at.apply(s.terminal().getX()).apply(s.terminal().getY()).apply(s.terminal().getZ());
		helper.runAfterDelay(5, () -> {
			helper.assertTrue(level.getBlockEntity(terminalPos) instanceof TerminalBlockEntity, "the terminal is in its office");
			TerminalBlockEntity t = (TerminalBlockEntity) level.getBlockEntity(terminalPos);
			var found = io.github.jimbozoomer.jugcraft.warehouse.WarehouseBuilding.detect(level, terminalPos);
			helper.assertTrue(found != null && found.stack().kind().equals("small_warehouse") && found.floors() == 3 && !found.incomplete(),
					"the terminal finds a Small Warehouse of three floors (" + (found == null ? "none" : found.floors() + " " + found.incomplete()) + ")");
			BlockPos drive = at.apply(28).apply(20).apply(2);
			helper.assertTrue(level.getBlockState(drive).is(JugcraftWarehouse.LIFT_DRIVE), "the lift's drive is over the top landing");
			var player = helper.makeMockServerPlayerInLevel();
			helper.assertTrue(io.github.jimbozoomer.jugcraft.warehouse.WarehouseBuilding.addFloor(level, t, player, false), "ADD A FLOOR stakes floor 4");
			helper.assertTrue(level.getBlockEntity(found.addStake()) instanceof io.github.jimbozoomer.jugcraft.blueprint.SurveyStakeBlockEntity st
					&& st.blueprintId().equals("small_warehouse@add"), "the fourth floor's stake stands over the roof");
			helper.assertTrue(level.getBlockState(drive).isAir(), "the lift's drive came off");
			helper.assertTrue(player.getInventory().countItem(JugcraftWarehouse.LIFT_DRIVE.asItem()) == 1, "and was given back");
			BlockState band = level.getBlockState(at.apply(2).apply(24).apply(0));
			helper.assertTrue(band.is(io.github.jimbozoomer.jugcraft.exterior.JugcraftExterior.WALL_PANEL)
					&& band.getValue(io.github.jimbozoomer.jugcraft.exterior.WallPanelBlock.TONE) == io.github.jimbozoomer.jugcraft.exterior.WallPanelBlock.Tone.SILVER,
					"the old roof's band is plain wall now (" + band + ")");
			helper.assertTrue(t.pendingFloor() != null && t.pendingFrom() == 3, "the terminal waits for floor 4");
			helper.succeed();
		});
	}

	// ------------------------------------------------------------------ part W4: liquids

	/**
	 * Part W4's test warehouse: the stone floor, lanes over x 1..15, z 4..8; a Drum Filling Station (x 3..5) and a Drum
	 * Emptying Station (x 7..9) at z 2..3 facing north with their fronts to the lanes; the terminal (x 11..13, z 3);
	 * docks at (1, 6) and (1, 7) facing west; a rack of places x 6..9, z 9, three high, facing south.
	 */
	private static BlockPos buildLiquids(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 12; z++) {
				level.setBlockAndUpdate(abs(helper, x, 0, z), Blocks.STONE.defaultBlockState());
			}
		}
		for (int x = 1; x <= 15; x++) {
			for (int z = 4; z <= 8; z++) {
				level.setBlockAndUpdate(abs(helper, x, 0, z), JugcraftWarehouse.LANE.defaultBlockState());
			}
		}
		piece(helper, JugcraftWarehouse.DRUM_FILLER, WarehousePiece.DRUM_FILLER, 4, 3, Direction.NORTH);
		piece(helper, JugcraftWarehouse.DRUM_EMPTIER, WarehousePiece.DRUM_EMPTIER, 8, 3, Direction.NORTH);
		BlockPos terminal = piece(helper, JugcraftWarehouse.TERMINAL, WarehousePiece.TERMINAL, 12, 3, Direction.NORTH);
		unlimited(helper, terminal);
		for (int z : new int[] {6, 7}) {
			level.setBlockAndUpdate(abs(helper, 1, 1, z), JugcraftWarehouse.DOCK.defaultBlockState().setValue(DockBlock.FACING, Direction.WEST));
		}
		for (int x = 6; x <= 9; x++) {
			for (int y = 1; y <= 3; y++) {
				level.setBlockAndUpdate(abs(helper, x, y, 9), JugcraftWarehouse.RACK.defaultBlockState().setValue(RackBlock.FACING, Direction.SOUTH));
			}
		}
		return terminal;
	}

	/**
	 * 60 buckets of water into a Drum Filling Station loaded with empty drums and pallets: its robot fills three steel
	 * drums (20 B each), the pallet is strapped once nothing more comes, and a pallet drone puts it away; the terminal
	 * counts 60 B of water in three drums.
	 */
	@GameTest(structure = ARENA, maxTicks = 3000, skyAccess = true)
	public void aLiquidIsDrummedAndPutAway(GameTestHelper helper) {
		if (TestFocus.skip("warehouse")) {
			helper.succeed();
			return;
		}
		BlockPos terminalPos = buildLiquids(helper);
		park(helper, 7, DroneKind.PALLET);
		FillerBlockEntity filler = (FillerBlockEntity) helper.getLevel().getBlockEntity(abs(helper, 4, 1, 3));
		helper.assertTrue(filler != null, "the filling station's block entity");
		var player = helper.makeMockServerPlayerInLevel();
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Drums.item(Drums.Kind.STEEL), 4));
		player.setShiftKeyDown(true);
		filler.use(helper.getLevel(), filler.getBlockPos(), player, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND);
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(JugcraftWarehouse.PALLET, 2));
		filler.use(helper.getLevel(), filler.getBlockPos(), player, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND);
		helper.assertTrue(filler.drums(Drums.Kind.STEEL) == 4 && filler.pallets() == 2, "four drums and two pallets loaded (" + filler.drums(Drums.Kind.STEEL)
				+ ", " + filler.pallets() + ")");
		try (Transaction t = Transaction.openOuter()) {
			long in = filler.buffer.insert(FluidVariant.of(Fluids.WATER), Drums.droplets(60_000), t);
			t.commit();
			helper.assertTrue(in == Drums.droplets(60_000), "the buffer tank took 60 B");
		}
		try (Transaction t = Transaction.openOuter()) {
			helper.assertTrue(filler.buffer.insert(FluidVariant.of(Fluids.LAVA), Drums.droplets(1000), t) == 0, "lava is turned away");
		}
		helper.succeedWhen(() -> {
			PalletLoad stored = null;
			for (int x = 6; x <= 9; x++) {
				for (int y = 1; y <= 3; y++) {
					RackBlockEntity rack = (RackBlockEntity) helper.getLevel().getBlockEntity(abs(helper, x, y, 9));
					if (rack != null && rack.load() != null) {
						stored = rack.load();
					}
				}
			}
			helper.assertTrue(stored != null, "a pallet of drums is in a rack place");
			helper.assertTrue(stored.size() == 3 && stored.liquid() == 60_000, "three drums, 60 B (" + stored.describe() + ")");
			helper.assertTrue(stored.look() == Drums.LOAD0 + 2, "the rack shows three steel drums (look " + stored.look() + ")");
			TerminalBlockEntity terminal = (TerminalBlockEntity) helper.getLevel().getBlockEntity(terminalPos);
			long water = terminal == null ? 0 : terminal.stock().values().stream().filter(st -> Drums.isLiquid(st.item)).mapToLong(st -> st.count).sum();
			helper.assertTrue(water == 60_000, "the terminal counts 60 B of water (" + water + ")");
			helper.assertTrue(filler.drums(Drums.Kind.STEEL) == 1 && filler.tank() == 0, "one drum left, the tank empty");
		});
	}

	/**
	 * Part W4: an order for 30 buckets of water from a stored pallet of four full drums: the pallet drone fetches it to
	 * the Drum Emptying Station, its pump empties the first drum and half the second into the holding tank, the order is
	 * READY there (for the fill point), and the pallet goes back to its place with 50 B on it.
	 */
	@GameTest(structure = ARENA, maxTicks = 2400, skyAccess = true)
	public void aLiquidOrderIsPumpedOut(GameTestHelper helper) {
		if (TestFocus.skip("warehouse")) {
			helper.succeed();
			return;
		}
		BlockPos terminalPos = buildLiquids(helper);
		park(helper, 7, DroneKind.PALLET);
		RackBlockEntity place = (RackBlockEntity) helper.getLevel().getBlockEntity(abs(helper, 6, 1, 9));
		helper.assertTrue(place != null, "a rack place at (6, 1, 9)");
		FluidVariant water = FluidVariant.of(Fluids.WATER);
		java.util.List<CaseLoad> drums = new java.util.ArrayList<>();
		for (int k = 0; k < 4; k++) {
			drums.add(new CaseLoad(Drums.book(water), (int) Drums.CAPACITY));
		}
		place.setLoad(new PalletLoad(drums));
		int[] id = {0};
		helper.runAfterDelay(30, () -> {
			TerminalBlockEntity terminal = (TerminalBlockEntity) helper.getLevel().getBlockEntity(terminalPos);
			helper.assertTrue(terminal != null && terminal.available(Drums.book(water)) == 80_000, "the terminal has 80 B of water to give");
			Order o = terminal.placeOrder(null, "test", Drums.book(water), 30_000, Order.WINDOW, "TEST", helper.getLevel().getGameTime());
			helper.assertTrue(o != null, "the order is placed");
			id[0] = o.id;
		});
		helper.succeedWhen(() -> {
			TerminalBlockEntity terminal = (TerminalBlockEntity) helper.getLevel().getBlockEntity(terminalPos);
			helper.assertTrue(terminal != null && id[0] > 0, "the order was placed");
			Order o = terminal.order(id[0]);
			helper.assertTrue(o != null && o.state == Order.READY, "the order is READY (" + (o == null ? "gone" : Order.STATES[o.state]) + ")");
			EmptierBlockEntity e = (EmptierBlockEntity) helper.getLevel().getBlockEntity(abs(helper, 8, 1, 3));
			helper.assertTrue(e != null && e.level() == 30_000 && e.shareOf(id[0]) == 30_000, "the holding tank has the 30 B for it ("
					+ (e == null ? "-" : e.level()) + ")");
			RackBlockEntity back = (RackBlockEntity) helper.getLevel().getBlockEntity(abs(helper, 6, 1, 9));
			helper.assertTrue(back != null && back.load() != null && back.load().liquid() == 50_000, "the pallet is back with 50 B ("
					+ (back == null || back.load() == null ? "-" : back.load().describe()) + ")");
			helper.assertTrue(e.free(), "the platform is free again");
		});
	}

	// ------------------------------------------------------------------ part W5: fleet and control

	private static java.util.List<CaseLoad> cases(net.minecraft.world.item.Item item, int n) {
		java.util.List<CaseLoad> out = new java.util.ArrayList<>();
		for (int k = 0; k < n; k++) {
			out.add(new CaseLoad(new ItemStack(item), 256));
		}
		return out;
	}

	/**
	 * Part W5: two part pallets of cobblestone (4 and 3 cases) with the fleet idle: a pallet drone brings the bigger one
	 * to a station, another brings the smaller and holds it while the stacker moves its cases across, the pallet of seven
	 * is wrapped and put away, the emptied one goes back into a station's magazine: one place used, 1,792 cobblestone.
	 */
	@GameTest(structure = ARENA, maxTicks = 6000, skyAccess = true)
	public void twoPartPalletsAreConsolidated(GameTestHelper helper) {
		if (TestFocus.skip("warehouse")) {
			helper.succeed();
			return;
		}
		BlockPos terminalPos = build(helper);
		// A second station (the emptied pallet goes back to it).
		piece(helper, JugcraftWarehouse.STATION, WarehousePiece.STATION, 9, 3, Direction.NORTH);
		park(helper, 6, DroneKind.STACKER);
		park(helper, 7, DroneKind.PALLET);
		for (int x : new int[] {7, 9}) {
			StationBlockEntity station = (StationBlockEntity) helper.getLevel().getBlockEntity(abs(helper, x, 1, 3));
			helper.assertTrue(station != null, "a station at x " + x);
			station.addPallets(2);
		}
		RackBlockEntity a = (RackBlockEntity) helper.getLevel().getBlockEntity(abs(helper, 6, 1, 9));
		RackBlockEntity b = (RackBlockEntity) helper.getLevel().getBlockEntity(abs(helper, 9, 2, 9));
		helper.assertTrue(a != null && b != null, "two rack places");
		a.setLoad(new PalletLoad(cases(Items.COBBLESTONE, 3)));
		b.setLoad(new PalletLoad(cases(Items.COBBLESTONE, 4)));
		helper.succeedWhen(() -> {
			int used = 0, size = 0;
			for (int x = 6; x <= 9; x++) {
				for (int y = 1; y <= 3; y++) {
					RackBlockEntity rack = (RackBlockEntity) helper.getLevel().getBlockEntity(abs(helper, x, y, 9));
					if (rack != null && rack.load() != null) {
						used++;
						size = rack.load().size();
					}
				}
			}
			helper.assertTrue(used == 1 && size == 7, "one pallet of seven cases is left (" + used + " places, " + size + " cases)");
			TerminalBlockEntity terminal = (TerminalBlockEntity) helper.getLevel().getBlockEntity(terminalPos);
			long cobble = terminal == null ? 0 : terminal.stock().values().stream().filter(st -> st.item.is(Items.COBBLESTONE)).mapToLong(st -> st.count).sum();
			helper.assertTrue(cobble == 1792, "the terminal counts 1,792 cobblestone (" + cobble + ")");
			helper.assertTrue(terminal.drones().stream().allMatch(d -> d.carried == null && d.held == null), "no drone holds anything");
		});
	}

	/**
	 * Part W5: a warehouse without unlimited power runs on its switchboard's store: none, nothing works; a store, it is
	 * powered; a circuit switched off, that circuit isn't; the main switch off, nothing is.
	 */
	@GameTest(structure = ARENA, maxTicks = 200, skyAccess = true)
	public void aSwitchboardPowersTheWarehouse(GameTestHelper helper) {
		if (TestFocus.skip("warehouse")) {
			helper.succeed();
			return;
		}
		BlockPos terminalPos = build(helper);
		TerminalBlockEntity terminal = (TerminalBlockEntity) helper.getLevel().getBlockEntity(terminalPos);
		helper.assertTrue(terminal != null, "the terminal");
		terminal.setUnlimitedPower(false);
		BlockPos sbPos = piece(helper, JugcraftWarehouse.SWITCHBOARD, WarehousePiece.SWITCHBOARD, 14, 11, Direction.NORTH);
		io.github.jimbozoomer.jugcraft.warehouse.SwitchboardBlockEntity sb = (io.github.jimbozoomer.jugcraft.warehouse.SwitchboardBlockEntity)
				helper.getLevel().getBlockEntity(sbPos);
		helper.assertTrue(sb != null, "the switchboard");
		var stations = io.github.jimbozoomer.jugcraft.warehouse.WarehousePower.Circuit.STATIONS;
		helper.runAfterDelay(45, () -> {
			helper.assertTrue(!terminal.powered(stations) && terminal.lowPower(), "no power stored: nothing runs");
			sb.energy.setAmount(50_000);
		});
		helper.runAfterDelay(90, () -> {
			helper.assertTrue(terminal.powered(stations) && !terminal.lowPower(), "50,000 JE stored: the warehouse is powered");
			terminal.toggleCircuit(stations.ordinal());
		});
		helper.runAfterDelay(115, () -> {
			helper.assertTrue(!terminal.powered(stations) && terminal.powered(io.github.jimbozoomer.jugcraft.warehouse.WarehousePower.Circuit.LIFTS),
					"STATIONS switched off, LIFTS still on");
			terminal.toggleCircuit(stations.ordinal());
			sb.setMain(false);
		});
		helper.runAfterDelay(160, () -> {
			helper.assertTrue(!terminal.powered(stations), "the main switch off: nothing runs");
			helper.succeed();
		});
	}

	/**
	 * Part W5: a Signal Box set to STOCK BELOW 512 cobblestone with 256 stored gives a signal (a redstone lamp by it
	 * lights); with 2,048 stored it doesn't.
	 */
	@GameTest(structure = ARENA, maxTicks = 200, skyAccess = true)
	public void aSignalBoxSignalsLowStock(GameTestHelper helper) {
		if (TestFocus.skip("warehouse")) {
			helper.succeed();
			return;
		}
		build(helper);
		RackBlockEntity place = (RackBlockEntity) helper.getLevel().getBlockEntity(abs(helper, 6, 1, 9));
		helper.assertTrue(place != null, "a rack place");
		place.setLoad(new PalletLoad(cases(Items.COBBLESTONE, 1)));
		BlockPos boxPos = piece(helper, JugcraftWarehouse.SIGNAL_BOX, WarehousePiece.SIGNAL_BOX, 13, 2, Direction.NORTH);
		BlockPos lamp = abs(helper, 14, 1, 2);
		helper.getLevel().setBlockAndUpdate(lamp, Blocks.REDSTONE_LAMP.defaultBlockState());
		io.github.jimbozoomer.jugcraft.warehouse.SignalBoxBlockEntity box = (io.github.jimbozoomer.jugcraft.warehouse.SignalBoxBlockEntity)
				helper.getLevel().getBlockEntity(boxPos);
		helper.assertTrue(box != null, "the signal box");
		box.setMode(io.github.jimbozoomer.jugcraft.warehouse.SignalBoxBlockEntity.OUTPUT);
		box.setRule(io.github.jimbozoomer.jugcraft.warehouse.SignalBoxBlockEntity.STOCK_BELOW);
		box.setItem(new ItemStack(Items.COBBLESTONE));
		box.setThreshold(512);
		helper.runAfterDelay(60, () -> {
			helper.assertTrue(box.linked() && box.output() == 15, "256 stored, under 512: the box signals (" + box.output() + ")");
			helper.assertTrue(helper.getLevel().getBlockState(lamp).getValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT), "the lamp by it is lit");
			place.setLoad(new PalletLoad(cases(Items.COBBLESTONE, 8)));
		});
		helper.runAfterDelay(140, () -> {
			helper.assertTrue(box.output() == 0, "2,048 stored: no signal (" + box.output() + ")");
			helper.succeed();
		});
	}

	/** Part W5: with the fleet idle, a pallet drone counts the rack places (daily): every place is counted. */
	@GameTest(structure = ARENA, maxTicks = 3000, skyAccess = true)
	public void anIdleFleetCountsThePlaces(GameTestHelper helper) {
		if (TestFocus.skip("warehouse")) {
			helper.succeed();
			return;
		}
		BlockPos terminalPos = build(helper);
		park(helper, 7, DroneKind.PALLET);
		RackBlockEntity place = (RackBlockEntity) helper.getLevel().getBlockEntity(abs(helper, 7, 2, 9));
		helper.assertTrue(place != null, "a rack place");
		place.setLoad(new PalletLoad(cases(Items.COAL, 8)));
		helper.succeedWhen(() -> {
			TerminalBlockEntity terminal = (TerminalBlockEntity) helper.getLevel().getBlockEntity(terminalPos);
			helper.assertTrue(terminal != null, "the terminal");
			for (int x = 6; x <= 9; x++) {
				for (int y = 1; y <= 3; y++) {
					helper.assertTrue(terminal.counted(abs(helper, x, y, 9)) > 0, "place (" + x + ", " + y + ") counted");
				}
			}
		});
	}

	/**
	 * A warehouse hooked up to a Drone Tower gives it what's on its racks at once (Justin, 10 November 2026: "it should be
	 * able to detect everything in the warehouse and drag it from there too"; his pick: "Take instantly"): pallets of iron
	 * on three places, one of them booked for a drone; the tower's warehouse supply counts the two free pallets and takes
	 * from them, part cases first left as they are, the books and stock index following.
	 */
	@GameTest(structure = ARENA, maxTicks = 40, skyAccess = true)
	public void aTowerTakesFromTheRacksAtOnce(GameTestHelper helper) {
		if (TestFocus.skip("warehouse")) {
			helper.succeed();
			return;
		}
		BlockPos terminalPos = build(helper);
		ServerLevel level = helper.getLevel();
		helper.runAfterDelay(5, () -> {
			TerminalBlockEntity terminal = (TerminalBlockEntity) level.getBlockEntity(terminalPos);
			helper.assertTrue(terminal != null, "the terminal");
			ItemStack iron = new ItemStack(Items.IRON_INGOT);
			int cap = CaseLoad.capacity(iron);
			for (int x = 6; x <= 8; x++) {
				RackBlockEntity rack = (RackBlockEntity) level.getBlockEntity(abs(helper, x, 1, 9));
				helper.assertTrue(rack != null, "a rack place at x " + x);
				java.util.List<CaseLoad> cases = new java.util.ArrayList<>();
				for (int c = 0; c < 2; c++) {
					cases.add(new CaseLoad(iron, cap));
				}
				rack.setLoad(new PalletLoad(cases));
			}
			Warehouses.changed(level, terminalPos);
		});
		helper.runAfterDelay(15, () -> {
			TerminalBlockEntity terminal = (TerminalBlockEntity) level.getBlockEntity(terminalPos);
			var variant = net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(Items.IRON_INGOT);
			int cap = CaseLoad.capacity(new ItemStack(Items.IRON_INGOT));
			long all = terminal.free(level, variant);
			helper.assertTrue(all == 6L * cap, "three pallets of two cases on the racks: " + all + " (case " + cap + ")");
			terminal.reserve(abs(helper, 8, 1, 9), 99);
			helper.assertTrue(terminal.free(level, variant) == 4L * cap, "a pallet booked for a drone isn't free");
			var source = io.github.jimbozoomer.jugcraft.tower.TowerSupply.warehouse(level, terminal);
			helper.assertTrue(source.count(Items.IRON_INGOT) == 4L * cap, "the tower's warehouse supply counts what's free");
			long took = source.take(Items.IRON_INGOT, cap + 10);
			helper.assertTrue(took == cap + 10 && terminal.free(level, variant) == 3L * cap - 10, "taken at once: " + took);
			RackBlockEntity booked = (RackBlockEntity) level.getBlockEntity(abs(helper, 8, 1, 9));
			helper.assertTrue(booked.load() != null && booked.load().cases().stream().mapToLong(CaseLoad::count).sum() == 2L * cap,
					"the booked pallet left alone");
			var stock = terminal.stock().get(variant);
			helper.assertTrue(stock != null && stock.count == 5L * cap - 10, "the stock index follows: " + (stock == null ? null : stock.count));
			helper.assertTrue(source.take(Items.IRON_INGOT, 100_000) == 3L * cap - 10 && terminal.free(level, variant) == 0, "all that's free, no more");
			helper.succeed();
		});
	}
}
