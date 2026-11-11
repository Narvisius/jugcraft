package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.nuclear.DieselPanels;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentFrameBlock;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentPlacement;
import io.github.jimbozoomer.jugcraft.nuclear.JugcraftNuclear;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The emergency diesel generator: its walkways stand on frames you walk on, the steps up to each on half-block frames
 * (so they walk like stairs), and using it starts and stops it; its starting air (round 31): each start takes air from
 * its receivers, too little and it fails to start, its compressors refill them.
 */
public class DieselGeneratorGameTests {
	@GameTest(structure = "jugcraft-test:drone_tower", maxTicks = 20)
	public void stepsWalkAndItStartsAndStops(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		BlockPos anchor = helper.absolutePos(new BlockPos(5, 1, 20));
		Direction facing = Direction.NORTH;
		helper.assertTrue(EquipmentPlacement.place(helper.getLevel(), anchor, EquipmentKind.EMERGENCY_DIESEL_GENERATOR, facing, null),
				"the generator fits");
		EquipmentKind kind = EquipmentKind.EMERGENCY_DIESEL_GENERATOR;
		// The steps up to each walkway (round 31: the left three blocks, x 0 to 2, and the right, 7 to 9), from the floor:
		// half (z 14), whole (z 13), whole and a half (z 12), the deck at 2 (z 11).
		int[][] expect = {{14, 0, 1}, {13, 0, 2}, {12, 0, 2}, {12, 1, 1}, {11, 1, 2}, {11, 0, 2}};
		for (int x : new int[] {0, 2, 7, 9}) {
			for (int[] e : expect) {
				BlockPos at = anchor.offset(EquipmentKind.toWorld(new BlockPos(x, e[1], e[0]), facing));
				BlockState state = helper.getLevel().getBlockState(at);
				helper.assertTrue(state.is(JugcraftNuclear.FRAME), "a frame at step x " + x + " z " + e[0] + " y " + e[1]);
				helper.assertTrue(state.getValue(EquipmentFrameBlock.SHAPE) == (e[2] == 1 ? EquipmentFrameBlock.Shape.HALF : EquipmentFrameBlock.Shape.FULL),
						"step x " + x + " z " + e[0] + " y " + e[1] + " is " + state.getValue(EquipmentFrameBlock.SHAPE));
			}
			// Above each walkway and step is clear to stand in; the floor at the foot of each flight is clear.
			for (int[] c : new int[][] {{2, 5}, {3, 8}, {2, 12}, {1, 13}, {1, 14}, {0, 15}}) {
				helper.assertTrue(helper.getLevel().getBlockState(anchor.offset(EquipmentKind.toWorld(new BlockPos(x, c[0], c[1]), facing))).isAir(),
						"clear above x " + x + " y " + c[0] + " z " + c[1]);
			}
		}
		// Used, the set only says how it stands (round 31 part 2: it is started from its engine panel or the control
		// room). Started from the control room with no diesel near: it won't start. With a day tank of diesel it starts,
		// and stops.
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		EquipmentBlockEntity generator = (EquipmentBlockEntity) helper.getLevel().getBlockEntity(anchor);
		EquipmentPlacement.use(helper.getLevel(), anchor, player, ItemStack.EMPTY, InteractionHand.MAIN_HAND);
		helper.assertTrue(!generator.running && !generator.fuelOut, "a click on the set doesn't start it");
		EquipmentPlacement.dieselSwitch(helper.getLevel(), generator, player, true);
		helper.assertTrue(!generator.running && generator.fuelOut, "no fuel: it doesn't start");
		// (East of the generator, which stands east and north of its anchor: inside the test's space.)
		var tank = dayTank(helper.getLevel(), anchor.offset(11, -1, -6));
		fill(tank, 50 * net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET);
		EquipmentPlacement.dieselSwitch(helper.getLevel(), generator, player, true);
		helper.assertTrue(generator.running && !generator.fuelOut, "started");
		EquipmentPlacement.dieselSwitch(helper.getLevel(), generator, player, false);
		helper.assertTrue(!generator.running, "stopped");
		helper.succeed();
	}

	/**
	 * Round 31 part 2 (Justin: controls "Engine and breaker", a LOCAL start's breaker "Closes itself", the 86G from
	 * "Handle and relay tests", the emergency stop "Latches in"): MODE decides who can start it (AUTO the control room,
	 * LOCAL its engine panel, OFF nobody, and it stops); its breaker closes itself five seconds into a start, trips open
	 * (the engine running on light) and closes again; a relay test drops the relay's target and trips the 86G, which
	 * opens the breaker, stops the engine and keeps it from starting until reset; the emergency stop stops it and keeps
	 * it from starting until pulled out.
	 */
	@GameTest(maxTicks = 20)
	public void panelControlsModeBreakerAndLockout(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		BlockPos anchor = helper.absolutePos(BlockPos.ZERO).offset(9_000, 1, 9_000);
		helper.assertTrue(EquipmentPlacement.place(level, anchor, EquipmentKind.EMERGENCY_DIESEL_GENERATOR, Direction.NORTH, null), "the generator fits");
		fill(dayTank(level, anchor.offset(-9, -1, -6)), 50 * net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET);
		EquipmentBlockEntity edg = (EquipmentBlockEntity) level.getBlockEntity(anchor);
		helper.assertTrue(edg.edgMode == DieselPanels.AUTO && DieselPanels.remoteReady(edg), "it stands in AUTO");
		helper.assertTrue(!EquipmentPlacement.dieselSwitch(level, edg, null, true, DieselPanels.PANEL) && !edg.running, "in AUTO its panel's START doesn't start it");
		DieselPanels.mode(level, anchor, null, edg, true);
		helper.assertTrue(edg.edgMode == DieselPanels.LOCAL && !DieselPanels.remoteReady(edg), "MODE turned back to LOCAL");
		helper.assertTrue(!EquipmentPlacement.dieselSwitch(level, edg, null, true) && !edg.running, "in LOCAL the control room can't start it");
		helper.assertTrue(EquipmentPlacement.dieselSwitch(level, edg, null, true, DieselPanels.PANEL) && edg.running, "in LOCAL its START does");
		helper.assertTrue(edg.breakerAt == edg.runningSince + 100 && !edg.breakerClosed(edg.runningSince + 99) && edg.breakerClosed(edg.runningSince + 100),
				"its breaker closes itself five seconds in: " + edg.breakerAt);
		// Up to speed (as if started ten seconds ago): tripped, the loads drop and it runs on; closed, they come back.
		long now = level.getGameTime();
		edg.runningSince = now - 200;
		edg.breakerAt = edg.runningSince + 100;
		DieselPanels.breakerSwitch(level, anchor, null, edg, false);
		helper.assertTrue(edg.running && edg.breakerAt == -1 && edg.breakerSw == 1, "TRIP opens the breaker, the engine runs on");
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.DieselFuel.kw(edg.breakerSeconds(now + 200)) == 0, "no load with the breaker open");
		DieselPanels.breakerSwitch(level, anchor, null, edg, true);
		helper.assertTrue(edg.breakerClosed(now) && edg.breakerAt == now && edg.breakerSw == 2, "CLOSE closes it again");
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.DieselFuel.kw(edg.breakerSeconds(now + 400)) > 1000, "the sequencer loads it again");
		// A relay test: its target drops, the 86G trips, the breaker opens, the engine stops and won't start.
		DieselPanels.relayTest(level, anchor, null, edg, 0);
		helper.assertTrue(edg.lockout && (edg.relayTargets & 1) != 0 && !edg.running && edg.breakerAt == -1, "the 87G test trips the 86G");
		helper.assertTrue(!EquipmentPlacement.dieselSwitch(level, edg, null, true, DieselPanels.PANEL) && !edg.running, "locked out it won't start");
		DieselPanels.relayReset(level, anchor, null, edg, 0);
		helper.assertTrue(edg.relayTargets == 0 && edg.lockout, "the target resets; the 86G stays tripped");
		DieselPanels.resetLockout(level, anchor, null, edg);
		helper.assertTrue(!edg.lockout && EquipmentPlacement.dieselSwitch(level, edg, null, true, DieselPanels.PANEL) && edg.running, "reset, it starts");
		// The emergency stop: from LOCAL or AUTO alike it stops it, and it stays in.
		DieselPanels.estop(level, anchor, null, edg);
		helper.assertTrue(edg.estop && !edg.running, "EMERG STOP stops it and stays in");
		helper.assertTrue(!EquipmentPlacement.dieselSwitch(level, edg, null, true, DieselPanels.PANEL) && !edg.running, "and it won't start");
		DieselPanels.estop(level, anchor, null, edg);
		helper.assertTrue(!edg.estop && EquipmentPlacement.dieselSwitch(level, edg, null, true, DieselPanels.PANEL) && edg.running, "pulled out, it starts");
		// MODE round to AUTO, then OFF: it stops, and nothing starts it.
		DieselPanels.mode(level, anchor, null, edg, false);
		helper.assertTrue(edg.edgMode == DieselPanels.AUTO && edg.running, "to AUTO it runs on");
		DieselPanels.mode(level, anchor, null, edg, false);
		helper.assertTrue(edg.edgMode == DieselPanels.OFF && !edg.running, "OFF stops it");
		helper.assertTrue(!EquipmentPlacement.dieselSwitch(level, edg, null, true) && !EquipmentPlacement.dieselSwitch(level, edg, null, true, DieselPanels.PANEL),
				"at OFF nothing starts it");
		helper.succeed();
	}

	/**
	 * A small flammable-rated storage tank (3 x 3 x 3 inside: 108 buckets) with its outlet on the north side, its floor
	 * at base + 1; formed. The generator's day tank in these tests.
	 */
	static io.github.jimbozoomer.jugcraft.nuclear.StorageTankPortBlockEntity dayTank(net.minecraft.server.level.ServerLevel level, BlockPos base) {
		var plate = io.github.jimbozoomer.jugcraft.nuclear.StorageTanks.PLATE.defaultBlockState();
		for (int x = 0; x <= 6; x++) {
			for (int z = 0; z <= 6; z++) {
				level.setBlock(base.offset(x, 0, z), net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState(), 3);
				for (int y = 1; y <= 5; y++) {
					level.setBlock(base.offset(x, y, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
		for (int x = 1; x <= 5; x++) {
			for (int z = 1; z <= 5; z++) {
				level.setBlock(base.offset(x, 4, z), plate, 3);
				for (int y = 1; y <= 3; y++) {
					if (x == 1 || x == 5 || z == 1 || z == 5) {
						level.setBlock(base.offset(x, y, z), plate, 3);
					}
				}
			}
		}
		BlockPos outlet = base.offset(3, 1, 0);
		// Rated for flammable liquids, as a diesel day tank must be (a general tank refuses diesel).
		level.setBlock(outlet, io.github.jimbozoomer.jugcraft.nuclear.StorageTanks.OUTLET.defaultBlockState()
				.setValue(io.github.jimbozoomer.jugcraft.nuclear.StorageTankPortBlock.FACING, Direction.NORTH)
				.setValue(io.github.jimbozoomer.jugcraft.nuclear.StorageTankOutletBlock.RATING, io.github.jimbozoomer.jugcraft.nuclear.TankRating.FLAMMABLE), 3);
		var tank = (io.github.jimbozoomer.jugcraft.nuclear.StorageTankPortBlockEntity) level.getBlockEntity(outlet);
		tank.check(level);
		if (!tank.formed()) {
			throw new AssertionError("the test's day tank doesn't form");
		}
		return tank;
	}

	static void fill(io.github.jimbozoomer.jugcraft.nuclear.StorageTankPortBlockEntity tank, long droplets) {
		try (var t = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
			tank.fluid.insert(io.github.jimbozoomer.jugcraft.nuclear.DieselFuel.diesel(), droplets, t);
			t.commit();
		}
	}

	/**
	 * Round 14 (Justin: diesel at a realistic rate, in millibuckets): running, the generator burns its day tank's diesel
	 * as a real 7 MW diesel does, about 1,690 litres (mB) a Minecraft hour (1,000 ticks) at full load, through the
	 * load sequence first; run dry, it stops and FAIL TO START shows.
	 */
	@GameTest(maxTicks = 20)
	public void burnsDieselFromItsDayTank(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		BlockPos anchor = helper.absolutePos(BlockPos.ZERO).offset(3_000, 1, 3_000);
		helper.assertTrue(EquipmentPlacement.place(level, anchor, EquipmentKind.EMERGENCY_DIESEL_GENERATOR, Direction.NORTH, null), "the generator fits");
		var tank = dayTank(level, anchor.offset(-9, -1, -6));
		long start = 100 * net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET;
		fill(tank, start);
		EquipmentBlockEntity edg = (EquipmentBlockEntity) level.getBlockEntity(anchor);
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.DieselFuel.hasFuel(level, edg), "the generator finds its day tank");
		double fullLoad = io.github.jimbozoomer.jugcraft.nuclear.DieselFuel.IDLE_LPH + io.github.jimbozoomer.jugcraft.nuclear.DieselFuel.LPH_PER_KW * 6800;
		helper.assertTrue(fullLoad > 1600 && fullLoad < 1800, "about 1,690 L an hour at full load: " + fullLoad);
		// Started, then 1,800 ticks (1.8 Minecraft hours): the sequence (40 s), then full load: about 2,350 mB.
		long t0 = level.getGameTime();
		edg.setDieselRunning(t0, true);
		double expected = 0;
		for (long now = t0; now < t0 + 1800; now++) {
			io.github.jimbozoomer.jugcraft.nuclear.DieselFuel.tick(level, edg, now);
			expected += io.github.jimbozoomer.jugcraft.nuclear.DieselFuel.litresPerHour((now - t0) / 20.0) / io.github.jimbozoomer.jugcraft.nuclear.DieselFuel.TICKS_PER_HOUR;
		}
		double burned = (start - tank.fluid.amount) / (double) io.github.jimbozoomer.jugcraft.fluid.FluidNetworks.DROPLETS_PER_MB;
		helper.assertTrue(Math.abs(burned - expected) < 40 && burned > 2100 && burned < 2600,
				String.format(java.util.Locale.ROOT, "burned %.0f mB in 1,800 ticks, expected %.0f", burned, expected));
		helper.assertTrue(edg.running, "still running with fuel left");
		// Nearly dry: within a second it runs out and stops.
		try (var t = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
			tank.fluid.extract(io.github.jimbozoomer.jugcraft.nuclear.DieselFuel.diesel(), tank.fluid.amount - 10 * io.github.jimbozoomer.jugcraft.fluid.FluidNetworks.DROPLETS_PER_MB, t);
			t.commit();
		}
		for (long now = t0 + 1800; now < t0 + 1840; now++) {
			io.github.jimbozoomer.jugcraft.nuclear.DieselFuel.tick(level, edg, now);
		}
		helper.assertTrue(!edg.running && edg.fuelOut, "run dry, it stops (fuel out)");
		helper.assertTrue(edg.dayLevel >= 0 && edg.dayLevel < io.github.jimbozoomer.jugcraft.nuclear.DieselFuel.LOW, "the low level shows: " + edg.dayLevel);
		helper.succeed();
	}

	/**
	 * Round 14 (Justin): the diesel room panels link to the nearest generator, and a generator takes at most one of each
	 * panel: two engine panels by generator A link one to A and the other to B; a generator panel by A links to A; when
	 * a generator goes, its panels find another free one or none.
	 */
	@GameTest(maxTicks = 20)
	public void panelsLinkOnePerGenerator(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		// Out of the way of the other tests.
		BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(-3_000, 1, -3_000);
		BlockPos a = base, b = base.offset(16, 0, 0);
		helper.assertTrue(EquipmentPlacement.place(level, a, EquipmentKind.EMERGENCY_DIESEL_GENERATOR, Direction.NORTH, null), "generator A fits");
		helper.assertTrue(EquipmentPlacement.place(level, b, EquipmentKind.EMERGENCY_DIESEL_GENERATOR, Direction.NORTH, null), "generator B fits");
		var engine = io.github.jimbozoomer.jugcraft.nuclear.PlantInterior.BLOCKS.get("edg_engine_panel");
		var gen = io.github.jimbozoomer.jugcraft.nuclear.PlantInterior.BLOCKS.get("edg_generator_panel");
		// Just behind A (facing north it stands east and north of its anchor): two engine panels and a generator panel.
		BlockPos p1 = a.offset(0, 0, 3), p2 = a.offset(2, 0, 4), p3 = a.offset(4, 0, 3);
		level.setBlock(p1, engine.defaultBlockState(), 3);
		level.setBlock(p2, engine.defaultBlockState(), 3);
		level.setBlock(p3, gen.defaultBlockState(), 3);
		var one = (io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity) level.getBlockEntity(p1);
		var two = (io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity) level.getBlockEntity(p2);
		var three = (io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity) level.getBlockEntity(p3);
		helper.assertTrue(one != null && two != null && three != null, "the panels have their block entities");
		one.serverTick(level);
		two.serverTick(level);
		three.serverTick(level);
		helper.assertTrue(a.equals(one.generator), "the first engine panel links to the nearest generator: " + one.generator);
		helper.assertTrue(b.equals(two.generator), "the second engine panel takes the other generator: " + two.generator);
		helper.assertTrue(a.equals(three.generator), "a generator panel by A links to A: " + three.generator);
		// The engine panel's hour meter counts from its link: zero now, half an hour after 500 ticks of running.
		EquipmentBlockEntity genA = (EquipmentBlockEntity) level.getBlockEntity(a);
		helper.assertTrue(one.hours(genA, level.getGameTime()) == 0, "the hour meter reads zero when linked: " + one.hours(genA, level.getGameTime()));
		genA.runTicks += 500;
		helper.assertTrue(Math.abs(one.hours(genA, level.getGameTime()) - 0.5) < 1e-9, "500 ticks run: half an hour: " + one.hours(genA, level.getGameTime()));
		// Generator B goes: the second engine panel has none left to link to (A has an engine panel).
		level.setBlock(b, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
		for (int i = 0; i <= 40; i++) {
			two.serverTick(level);
		}
		helper.assertTrue(two.generator == null, "with B gone, the second engine panel is unlinked: " + two.generator);
		helper.succeed();
	}

	/**
	 * Round 31 (Justin: "Starting air system ... Yes, it works"): a generator with two receivers, two compressors and a
	 * dryer near it takes them as its own; each start takes 20 psig from 250, five starts and the sixth fails (FAIL TO
	 * START, it doesn't run); its compressors then refill the receivers (both below 200 psig, at 2 psig a second each)
	 * and stop at 250; between 200 and 225 only the lead (A) runs; with its air back it starts again.
	 */
	@GameTest(maxTicks = 20)
	public void startingAirStartsAndRefills(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		BlockPos anchor = helper.absolutePos(BlockPos.ZERO).offset(6_000, 1, 6_000);
		helper.assertTrue(EquipmentPlacement.place(level, anchor, EquipmentKind.EMERGENCY_DIESEL_GENERATOR, Direction.NORTH, null), "the generator fits");
		fill(dayTank(level, anchor.offset(-9, -1, -6)), 50 * net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET);
		// East of it: its receivers, compressors (A and B by their labels) and dryer, facing west as along a wall.
		BlockPos[] at = {anchor.offset(12, 0, -1), anchor.offset(12, 0, -4), anchor.offset(12, 0, -8), anchor.offset(12, 0, -12), anchor.offset(12, 0, -15)};
		EquipmentKind[] kinds = {EquipmentKind.EDG_AIR_RECEIVER, EquipmentKind.EDG_AIR_RECEIVER, EquipmentKind.EDG_AIR_COMPRESSOR, EquipmentKind.EDG_AIR_COMPRESSOR,
				EquipmentKind.EDG_AIR_DRYER};
		for (int k = 0; k < at.length; k++) {
			helper.assertTrue(EquipmentPlacement.place(level, at[k], kinds[k], Direction.WEST, null), kinds[k].id + " fits");
		}
		level.setBlock(at[3], level.getBlockState(at[3]).setValue(io.github.jimbozoomer.jugcraft.nuclear.EquipmentAnchorBlock.VARIANT, 1), 3);
		EquipmentBlockEntity edg = (EquipmentBlockEntity) level.getBlockEntity(anchor);
		EquipmentBlockEntity lead = (EquipmentBlockEntity) level.getBlockEntity(at[2]), lag = (EquipmentBlockEntity) level.getBlockEntity(at[3]);
		EquipmentBlockEntity receiver = (EquipmentBlockEntity) level.getBlockEntity(at[0]), dryer = (EquipmentBlockEntity) level.getBlockEntity(at[4]);
		long t = level.getGameTime();
		t = run(level, edg, t, 40);
		helper.assertTrue(edg.airReceivers == 2 && edg.airPieces.size() == 5, "it finds its two receivers and the rest: " + edg.airPieces.size());
		helper.assertTrue(anchor.equals(receiver.linked) && anchor.equals(dryer.linked) && anchor.equals(lead.linked), "they join it");
		helper.assertTrue(edg.air == io.github.jimbozoomer.jugcraft.nuclear.StartingAir.FULL && !lead.running && !lag.running, "full, the compressors stopped");
		for (int k = 0; k < 5; k++) {
			EquipmentPlacement.dieselSwitch(level, edg, null, true);
			helper.assertTrue(edg.running && !edg.airFail, "start " + (k + 1) + " runs");
			EquipmentPlacement.dieselSwitch(level, edg, null, false);
		}
		helper.assertTrue(Math.abs(edg.air - 150) < 1e-6, "five starts take it to 150 psig: " + edg.air);
		EquipmentPlacement.dieselSwitch(level, edg, null, true);
		helper.assertTrue(!edg.running && edg.airFail && edg.air < 150, "the sixth fails to start: " + edg.air);
		// Refilling: both compressors run (below 200), 4 psig a second; full in under half a minute, then they stop.
		t = run(level, edg, t, 100);
		helper.assertTrue(lead.running && lag.running && edg.air > 150 && edg.air < 200, "both run: " + edg.air);
		helper.assertTrue(Math.abs(receiver.air - edg.air) < 1e-6, "the receiver's gauge shows it");
		t = run(level, edg, t, 600);
		helper.assertTrue(edg.air == io.github.jimbozoomer.jugcraft.nuclear.StartingAir.FULL && !lead.running && !lag.running, "full again, both stopped: " + edg.air);
		// Between 200 and 225 the lead alone.
		edg.air = 215;
		t = run(level, edg, t, 20);
		helper.assertTrue(lead.running && !lag.running, "the lead alone below 225");
		t = run(level, edg, t, 400);
		helper.assertTrue(!lead.running && edg.air == io.github.jimbozoomer.jugcraft.nuclear.StartingAir.FULL, "and it stops at 250");
		EquipmentPlacement.dieselSwitch(level, edg, null, true);
		helper.assertTrue(edg.running && !edg.airFail, "with its air back it starts");
		helper.succeed();
	}

	/**
	 * Round 31 part 3: the room's own plant joins its generator; the CO2 system (Justin: "Discharge that hurts") counts
	 * down from the station's pull (ABORT stops it), then discharges: the engine is shut down and won't start, anything
	 * breathing in the room chokes; RESET purges it and all is normal again. The monorail's hoist steps a block a click
	 * (Justin: "One step per click"), moving there at a block a second.
	 */
	@GameTest(maxTicks = 20)
	public void roomPlantCo2AndHoist(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		BlockPos anchor = helper.absolutePos(BlockPos.ZERO).offset(7_000, 1, 6_000);
		helper.assertTrue(EquipmentPlacement.place(level, anchor, EquipmentKind.EMERGENCY_DIESEL_GENERATOR, Direction.NORTH, null), "the generator fits");
		fill(dayTank(level, anchor.offset(-9, -1, -6)), 50 * net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET);
		BlockPos bankAt = anchor.offset(12, 0, -2), skidAt = anchor.offset(12, 0, -8), railAt = anchor.offset(-12, 10, -4);
		helper.assertTrue(EquipmentPlacement.place(level, bankAt, EquipmentKind.EDG_CO2_BANK, Direction.WEST, null), "the CO2 bank fits");
		helper.assertTrue(EquipmentPlacement.place(level, skidAt, EquipmentKind.EDG_JW_KEEPWARM, Direction.WEST, null), "the keep-warm skid fits");
		helper.assertTrue(EquipmentPlacement.place(level, railAt, EquipmentKind.EDG_MONORAIL, Direction.NORTH, null), "the monorail fits");
		EquipmentBlockEntity edg = (EquipmentBlockEntity) level.getBlockEntity(anchor);
		EquipmentBlockEntity bank = (EquipmentBlockEntity) level.getBlockEntity(bankAt), skid = (EquipmentBlockEntity) level.getBlockEntity(skidAt);
		EquipmentBlockEntity rail = (EquipmentBlockEntity) level.getBlockEntity(railAt);
		long t = level.getGameTime();
		for (int k = 0; k < 40; k++) {
			io.github.jimbozoomer.jugcraft.nuclear.DieselRoom.tick(level, edg, t + k);
		}
		helper.assertTrue(edg.roomPieces.size() == 3 && anchor.equals(bank.linked) && anchor.equals(skid.linked) && anchor.equals(rail.linked),
				"its room's pieces join it: " + edg.roomPieces.size());
		// The CO2: pulled with the engine running, aborted; pulled again and left to run out.
		EquipmentPlacement.dieselSwitch(level, edg, null, true);
		helper.assertTrue(edg.running, "the engine runs");
		io.github.jimbozoomer.jugcraft.nuclear.Co2System.pull(level, edg, bankAt, null);
		helper.assertTrue(edg.co2Phase == io.github.jimbozoomer.jugcraft.nuclear.Co2System.COUNTING, "pulled: the countdown");
		io.github.jimbozoomer.jugcraft.nuclear.Co2System.abort(level, edg, null);
		helper.assertTrue(edg.co2Phase == io.github.jimbozoomer.jugcraft.nuclear.Co2System.NORMAL && edg.running, "ABORT stops it");
		io.github.jimbozoomer.jugcraft.nuclear.Co2System.pull(level, edg, bankAt, null);
		long t0 = edg.co2At;
		for (int k = 1; k < io.github.jimbozoomer.jugcraft.nuclear.Co2System.COUNTDOWN * 20; k++) {
			io.github.jimbozoomer.jugcraft.nuclear.Co2System.tick(level, edg, t0 + k);
		}
		helper.assertTrue(edg.co2Phase == io.github.jimbozoomer.jugcraft.nuclear.Co2System.COUNTING && edg.running, "still counting a tick before");
		io.github.jimbozoomer.jugcraft.nuclear.Co2System.tick(level, edg, t0 + io.github.jimbozoomer.jugcraft.nuclear.Co2System.COUNTDOWN * 20L);
		helper.assertTrue(edg.co2Phase == io.github.jimbozoomer.jugcraft.nuclear.Co2System.DISCHARGED && !edg.running && edg.co2Room != null
				&& !edg.co2Room.isEmpty(), "discharged: the engine shut down, the room found");
		var pig = helper.spawn(net.minecraft.world.entity.EntityTypes.PIG, helper.relativePos(io.github.jimbozoomer.jugcraft.nuclear.Co2System.start(bank)));
		float before = pig.getHealth();
		io.github.jimbozoomer.jugcraft.nuclear.Co2System.tick(level, edg, edg.co2At + 20);
		helper.assertTrue(pig.getHealth() < before, "a pig in the room chokes: " + before + " -> " + pig.getHealth());
		EquipmentPlacement.dieselSwitch(level, edg, null, true);
		helper.assertTrue(!edg.running, "it won't start until the CO2 is reset");
		io.github.jimbozoomer.jugcraft.nuclear.Co2System.reset(level, edg, null);
		helper.assertTrue(edg.co2Phase == io.github.jimbozoomer.jugcraft.nuclear.Co2System.PURGING, "RESET: the purge");
		io.github.jimbozoomer.jugcraft.nuclear.Co2System.tick(level, edg, edg.co2At + io.github.jimbozoomer.jugcraft.nuclear.Co2System.PURGE * 20L);
		helper.assertTrue(edg.co2Phase == io.github.jimbozoomer.jugcraft.nuclear.Co2System.NORMAL, "and then normal");
		pig.discard();
		// The hoist: a block east a click, at a block a second; the hook a block down.
		long now = level.getGameTime();
		io.github.jimbozoomer.jugcraft.nuclear.Monorail.press(level, rail, io.github.jimbozoomer.jugcraft.nuclear.Monorail.EAST, null);
		helper.assertTrue(rail.hoistX == 3 && Math.abs(io.github.jimbozoomer.jugcraft.nuclear.Monorail.trolley(rail, now) - 2) < 1e-6
				&& Math.abs(io.github.jimbozoomer.jugcraft.nuclear.Monorail.trolley(rail, now + 20) - 3) < 1e-6, "the trolley steps east: " + rail.hoistX);
		io.github.jimbozoomer.jugcraft.nuclear.Monorail.press(level, rail, io.github.jimbozoomer.jugcraft.nuclear.Monorail.DOWN, null);
		helper.assertTrue(rail.hoistDrop == 1, "the hook steps down: " + rail.hoistDrop);
		helper.succeed();
	}

	/** Ticks a generator's starting air {@code ticks} game ticks on from {@code t}; returns where it got to. */
	private static long run(net.minecraft.server.level.ServerLevel level, EquipmentBlockEntity edg, long t, int ticks) {
		for (int k = 0; k < ticks; k++, t++) {
			io.github.jimbozoomer.jugcraft.nuclear.StartingAir.tick(level, edg, t);
		}
		return t;
	}
}
