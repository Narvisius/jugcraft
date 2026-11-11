package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.nuclear.BoardPlant;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentPlacement;
import io.github.jimbozoomer.jugcraft.nuclear.JugcraftNuclear;
import io.github.jimbozoomer.jugcraft.nuclear.ReactorCore;
import io.github.jimbozoomer.jugcraft.nuclear.TurbineHall;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * Turbine hall part 1 (Justin: "Local trip + turning gear"): the main turbine-generator's manual trip on its front
 * standard trips a latched turbine and opens its generator breaker; its turning gear goes in only at rest, rolls the
 * shaft at 3 rpm and drops out when steam rolls it; a hot rotor standing without it bows (the control room's
 * eccentricity alarm). Placed, the turbine takes its body's cells and finds its trip handle and lever where they are drawn.
 */
public class TurbineHallGameTests {
	private static ReactorCore fuelled() {
		ReactorCore core = new ReactorCore();
		for (int i = 0; i < ReactorCore.CORE_SIZE; i++) core.fuel.add(0.0);
		core.temperature = 290;
		return core;
	}

	/** A board's snapshot of the turbine as the core stands (the parts the eccentricity alarm reads). */
	private static BoardPlant board(ReactorCore c) {
		BoardPlant p = new BoardPlant();
		p.present = true;
		p.steamToPower = true;
		p.turbineTripped = c.turbineTripped;
		p.scrammed = c.scrammed;
		p.turbineSpeed = c.turbineSpeed;
		p.turningGear = c.turningGear;
		p.temperature = c.temperature;
		return p;
	}

	@GameTest(maxTicks = 20)
	public void turningGearTripAndEccentricity(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		ReactorCore.Plant plant = new ReactorCore.Plant(1, true, true);
		// Below P-9, so the reactor runs on hot when the turbine trips.
		ReactorCore c = fuelled();
		c.rodTarget = 70;
		for (int t = 0; t < 20 * 300; t++) c.tick(plant, need -> need);
		helper.assertTrue(c.electric > 0 && c.turbineSpeed == 1, "at power and at speed");
		helper.assertTrue(!c.turningGear, "the turning gear dropped out as the shaft ran up");
		helper.assertTrue(TurbineHall.lever(c) == TurbineHall.Lever.ROLLING && !c.turningGear, "the gear won't go in at speed");
		// The manual trip: the turbine trips and its breaker opens; again, it's already tripped.
		helper.assertTrue(TurbineHall.trip(c, true) && c.turbineTripped && c.genOpen && !c.syncArmed, "tripped from the front standard");
		helper.assertTrue(!TurbineHall.trip(c, true), "already tripped");
		helper.assertTrue(!TurbineHall.trip(fuelled(), false), "no steam, nothing to trip");
		// Coasting down with the gear out it comes to rest; hot, standing still, its rotor bows.
		int t = 0;
		while (c.turbineSpeed >= ReactorCore.ZERO_SPEED && t++ < 20 * 600) c.tick(plant, need -> need);
		helper.assertTrue(c.turbineSpeed < ReactorCore.ZERO_SPEED && !c.scrammed && c.temperature > 150, "at rest, the reactor hot: "
				+ c.turbineSpeed + " " + c.temperature + " after " + t);
		helper.assertTrue(t > 20 * 60, "it coasted for over a minute: " + t);
		BoardPlant bowed = board(c);
		helper.assertTrue(bowed.rotorBowing() && bowed.alarm("eccen_hi", 0) && bowed.value("tsi.eccen", 0) > 5, "eccentricity high off its gear");
		// The gear goes in at rest; its motor brings the shaft up to 3 rpm and holds it there; the alarm clears.
		helper.assertTrue(TurbineHall.lever(c) == TurbineHall.Lever.ENGAGED && c.turningGear, "engaged at rest");
		for (int i = 0; i < 200; i++) c.tick(plant, need -> need);
		helper.assertTrue(Math.abs(c.turbineSpeed - ReactorCore.TURNING_GEAR) < 1e-9 && c.turningGear, "rolling at 3 rpm: " + c.turbineSpeed * 1800);
		BoardPlant rolling = board(c);
		helper.assertTrue(!rolling.rotorBowing() && !rolling.alarm("eccen_hi", 0), "no eccentricity on the gear");
		// Latched again, steam rolls it off the gear, which drops out.
		c.turbineTripped = false;
		for (int i = 0; i < 40; i++) c.tick(plant, need -> need);
		helper.assertTrue(c.turbineSpeed > ReactorCore.TURNING_GEAR * 3 && !c.turningGear, "rolled off its gear: " + c.turbineSpeed * 1800);
		// Out again at rest, it can be taken out any time.
		c.turningGear = true;
		helper.assertTrue(TurbineHall.lever(c) == TurbineHall.Lever.DISENGAGED && !c.turningGear, "taken out");
		helper.succeed();
	}

	/** Where a point on the turbine, in its own frame, is in the world. */
	private static Vec3 world(BlockPos anchor, Direction facing, double x, double y, double z) {
		Direction right = facing.getClockWise();
		return new Vec3(anchor.getX() + 0.5 + right.getStepX() * (x - 0.5) + facing.getStepX() * (z - 0.5), anchor.getY() + y,
				anchor.getZ() + 0.5 + right.getStepZ() * (x - 0.5) + facing.getStepZ() * (z - 0.5));
	}

	@GameTest(structure = "jugcraft-test:kiln_bowl", maxTicks = 20)
	public void turbinePlacesAndFindsItsControls(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		Direction facing = Direction.SOUTH;
		BlockPos anchor = helper.absolutePos(new BlockPos(40, 1, 10));
		EquipmentKind kind = EquipmentKind.TURBINE_GENERATOR;
		helper.assertTrue(EquipmentPlacement.place(helper.getLevel(), anchor, kind, facing, null), "the turbine fits");
		for (BlockPos at : kind.frames(facing)) {
			helper.assertTrue(helper.getLevel().getBlockState(anchor.offset(at)).is(JugcraftNuclear.FRAME), "a frame in its body at " + at);
		}
		// Clear: beside the HP above its pedestal, above the LP hoods' sides, beside the generator's top.
		for (int[] c : new int[][] {{0, 3, 20}, {10, 3, 20}, {1, 6, 20}, {2, 4, 40}, {8, 4, 40}, {0, 1, 1}}) {
			helper.assertTrue(helper.getLevel().getBlockState(anchor.offset(EquipmentKind.toWorld(new BlockPos(c[0], c[1], c[2]), facing))).isAir(),
					"clear at " + c[0] + " " + c[1] + " " + c[2]);
		}
		EquipmentBlockEntity turbine = (EquipmentBlockEntity) helper.getLevel().getBlockEntity(anchor);
		// The trip handle on the front standard, the turning gear lever by the last LP, elsewhere the casing.
		helper.assertTrue(TurbineHall.spot(turbine, world(anchor, facing, 5.5, 0.55, 0.0)) == TurbineHall.Spot.TRIP, "the trip handle");
		helper.assertTrue(TurbineHall.spot(turbine, world(anchor, facing, 3.0, 1.15, 36.7)) == TurbineHall.Spot.LEVER, "the turning gear lever");
		helper.assertTrue(TurbineHall.spot(turbine, world(anchor, facing, 5.5, 2.0, 40.0)) == TurbineHall.Spot.ELSEWHERE, "the generator");
		// With no reactor about, using it only says so.
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		EquipmentPlacement.use(helper.getLevel(), anchor, player, ItemStack.EMPTY, InteractionHand.MAIN_HAND, world(anchor, facing, 5.5, 0.55, 0.0));
		EquipmentPlacement.use(helper.getLevel(), anchor, player, ItemStack.EMPTY, InteractionHand.MAIN_HAND, world(anchor, facing, 3.0, 1.15, 36.7));
		helper.assertTrue(turbine.turbineFlags == 0 && turbine.tripGuardUntil == 0, "no reactor, nothing to trip");
		helper.succeed();
	}

	// ------------------------------------------------------------------ turbine hall part 2: the feed train

	/** A core at full power with every rod out, the hall's pumps there (they say so each tick here). */
	private static ReactorCore atPower(ReactorCore.Plant plant, boolean pieces) {
		ReactorCore c = fuelled();
		c.rodTarget = 0;
		for (int t = 0; t < 20 * 300; t++) {
			if (pieces) here(c);
			c.tick(plant, need -> need);
		}
		return c;
	}

	private static void here(ReactorCore c) {
		for (int k = 0; k < 3; k++) c.cpSeen[k] = TurbineHall.SEEN;
		for (int k = 0; k < 2; k++) c.mfpSeen[k] = TurbineHall.SEEN;
	}

	private static double lowest(ReactorCore c) {
		double w = 9;
		for (int k = 0; k < c.loops; k++) w = Math.min(w, c.nr(k));
		return w;
	}

	@GameTest(maxTicks = 20)
	public void feedTrainPumpsAndPlant(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		ReactorCore.Plant plant = new ReactorCore.Plant(1, true, true);
		// No pumps at the hall: main feed has no limit, as before.
		ReactorCore free = atPower(plant, false);
		helper.assertTrue(free.feedCap == ReactorCore.FEED_FREE && free.fission > 0.95 && lowest(free) > 0.45, "a console-only plant feeds as before: "
				+ free.fission + " " + lowest(free));
		// With them: two condensate pumps and both MFPs carry full power; the third condensate pump stands by.
		ReactorCore c = atPower(plant, true);
		helper.assertTrue(c.fission > 0.95 && !c.scrammed && Math.abs(c.feedCap - 1.0) < 1e-9 && c.cpOn() == 2 && c.mfpOn() == 2,
				"at power on the hall's pumps: cap " + c.feedCap + " cps " + c.cpOn());
		// A running condensate pump stopped at full power: the standby starts by itself (AUTO), the stopped one stands by.
		helper.assertTrue(TurbineHall.condensatePump(c, 0, false) == TurbineHall.Pressed.STOPPED_AUTO, "1A stopped");
		for (int t = 0; t < 40; t++) {
			here(c);
			c.tick(plant, need -> need);
		}
		helper.assertTrue((c.cpRunning & 4) != 0 && (c.cpAutoStarted & 4) != 0 && (c.cpAuto & 1) != 0 && c.cpOn() == 2,
				"1C started by AUTO, 1A standing by: " + c.cpRunning + " " + c.cpAuto);
		BoardPlant board = feedBoard(c);
		helper.assertTrue(board.alarm("cond_auto", 0) && !board.alarm("cond_p_lo", 0), "COND PUMP AUTO START in, the header's pressure held");
		// STOP again takes it out of AUTO; START runs it.
		helper.assertTrue(TurbineHall.condensatePump(c, 0, false) == TurbineHall.Pressed.AUTO_OFF, "1A out of AUTO");
		helper.assertTrue(TurbineHall.condensatePump(c, 0, true) == TurbineHall.Pressed.STARTED && c.cpOn() == 3, "1A started");
		helper.succeed();
	}

	/** The board's snapshot of a core's feed train (as BoardPlant.of takes it). */
	private static BoardPlant feedBoard(ReactorCore c) {
		BoardPlant p = new BoardPlant();
		p.present = true;
		p.steamToPower = true;
		p.temperature = c.temperature;
		p.feedIso = c.feedIsolated;
		for (int k = 0; k < 3; k++) if (c.cpPresent(k)) p.cpFound |= 1 << k;
		for (int k = 0; k < 2; k++) if (c.mfpPresent(k)) p.mfpFound |= 1 << k;
		p.cpRunning = c.cpRunning;
		p.cpAuto = c.cpAuto;
		p.cpAutoStarted = c.cpAutoStarted;
		p.mfpRunning = c.mfpRunning;
		p.mfpTripped = c.mfpTripped;
		p.cpShort = c.cpShort;
		p.feedCap = c.feedCap;
		return p;
	}

	@GameTest(maxTicks = 20)
	public void mfpTripRunsBackInAuto(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		ReactorCore.Plant plant = new ReactorCore.Plant(1, true, true);
		ReactorCore c = atPower(plant, true);
		c.rodControl(true);
		// MFP 1A tripped at its station above 60%: the rods in AUTO run the plant back to 55%; one MFP carries it.
		helper.assertTrue(TurbineHall.mainFeedPump(c, 0, true) == TurbineHall.Pressed.TRIPPED, "1A tripped");
		double low = 1;
		for (int t = 0; t < 20 * 90; t++) {
			here(c);
			c.tick(plant, need -> need);
			low = Math.min(low, lowest(c));
		}
		helper.assertTrue(!c.scrammed && Math.abs(c.fission - ReactorCore.RUNBACK) < 0.03 && low > ReactorCore.LO_LO,
				"run back to 55% without a trip: " + c.fission + " lowest level " + low);
		helper.assertTrue(feedBoard(c).alarm("mfp_a_trip", 0) && !feedBoard(c).mfpRun(0) && feedBoard(c).mfpRun(1), "MFP A TRIP in");
		// Reset at its station, started from the board (as ControlBoard does), it runs again.
		helper.assertTrue(TurbineHall.mainFeedPump(c, 0, false) == TurbineHall.Pressed.RESET && c.mfpOn() == 1, "reset, not yet started");
		c.mfpRunning |= 1;
		helper.assertTrue(c.mfpOn() == 2, "started again");
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void mfpTripInManualTripsOnLowLevel(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		ReactorCore.Plant plant = new ReactorCore.Plant(1, true, true);
		ReactorCore c = atPower(plant, true);
		// The rods in manual: one MFP can't carry full power; the generators' levels fall to low-low; the reactor trips
		// and the auxiliary feed pumps start.
		TurbineHall.mainFeedPump(c, 0, true);
		int t = 0;
		while (!c.scrammed && t++ < 20 * 120) {
			here(c);
			c.tick(plant, need -> need);
		}
		helper.assertTrue(c.scrammed && c.tripCause == ReactorCore.TRIP_SG_LO && (c.afwRunning & 3) == 3, "tripped on low-low level after " + t
				+ " ticks: " + c.tripCause + " afw " + c.afwRunning);
		helper.assertTrue(t > 20 * 20, "it took a while: " + t);
		// Both MFPs lost at power start the motor-driven auxiliary feed pumps at once.
		ReactorCore d = atPower(plant, true);
		TurbineHall.mainFeedPump(d, 0, true);
		TurbineHall.mainFeedPump(d, 1, true);
		here(d);
		d.tick(plant, need -> need);
		helper.assertTrue((d.afwRunning & 3) == 3 && d.feedCap == 0, "both lost: aux feed started, no main feed");
		helper.succeed();
	}

	@GameTest(structure = "jugcraft-test:kiln_bowl", maxTicks = 20)
	public void feedPumpsFindTheirButtons(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		Direction facing = Direction.SOUTH;
		BlockPos cp = helper.absolutePos(new BlockPos(5, 1, 5));
		helper.assertTrue(EquipmentPlacement.place(helper.getLevel(), cp, EquipmentKind.CONDENSATE_PUMP, facing, null), "the condensate pump fits");
		EquipmentBlockEntity pump = (EquipmentBlockEntity) helper.getLevel().getBlockEntity(cp);
		helper.assertTrue(TurbineHall.button(pump, world(cp, facing, 0.17, 1.12, 0.0)) == TurbineHall.Button.START, "START");
		helper.assertTrue(TurbineHall.button(pump, world(cp, facing, 0.43, 1.12, 0.0)) == TurbineHall.Button.STOP, "STOP");
		helper.assertTrue(TurbineHall.button(pump, world(cp, facing, 1.5, 3.0, 0.0)) == TurbineHall.Button.NONE, "the motor");
		BlockPos mfp = helper.absolutePos(new BlockPos(20, 1, 5));
		helper.assertTrue(EquipmentPlacement.place(helper.getLevel(), mfp, EquipmentKind.MAIN_FEED_PUMP, facing, null), "the main feed pump fits");
		EquipmentBlockEntity feed = (EquipmentBlockEntity) helper.getLevel().getBlockEntity(mfp);
		helper.assertTrue(TurbineHall.button(feed, world(mfp, facing, 3.1, 1.35, 0.0)) == TurbineHall.Button.TRIP, "TRIP");
		helper.assertTrue(TurbineHall.button(feed, world(mfp, facing, 3.44, 1.35, 0.0)) == TurbineHall.Button.RESET, "RESET");
		// No turbine about: using it only says so; sneaking with an empty hand relabels it.
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		EquipmentPlacement.use(helper.getLevel(), cp, player, ItemStack.EMPTY, InteractionHand.MAIN_HAND, world(cp, facing, 0.17, 1.12, 0.0));
		helper.assertTrue(pump.turbineFlags == 0, "no turbine, nothing running");
		TurbineHall.usePiece(helper.getLevel(), pump, player, null, true);
		helper.assertTrue(TurbineHall.label(helper.getLevel(), pump) == 1, "relabelled 1B");
		helper.succeed();
	}
}
