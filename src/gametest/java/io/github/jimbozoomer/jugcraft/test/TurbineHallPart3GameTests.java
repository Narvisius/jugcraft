package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.nuclear.ControlRoomConsoles;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentPlacement;
import io.github.jimbozoomer.jugcraft.nuclear.HallCrane;
import io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity;
import io.github.jimbozoomer.jugcraft.nuclear.PlantInterior;
import io.github.jimbozoomer.jugcraft.nuclear.TurbineControlRoom;
import io.github.jimbozoomer.jugcraft.nuclear.TurbineHall;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/**
 * Turbine hall part 3 (Justin: "Works from a pendant"): the overhead crane runs from its pendant as long as a button is
 * held (each press a few ticks more) and stops when let go; its bridge runs to its runway's ends, its trolley along the
 * bridge, its hooks down to the floor under them; STOP takes its power off until START. The turbine control room's
 * cabinets and console read their turbine as the boards do, and the lube oil room's plant reads with the oil plant.
 */
public class TurbineHallPart3GameTests {
	private static final double EPS = 1e-6;

	@GameTest(structure = "jugcraft-test:kiln_bowl", maxTicks = 20)
	public void craneRunsFromItsPendant(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		// The crane facing east over a runway along x: its trucks over two beams 41 blocks apart (z 10 and 51 here), the
		// beams from x 3 to 30 (offsets -2 to 20 for its 6 deep bridge), a floor at y 1 under its hooks.
		Direction facing = Direction.EAST;
		BlockPos anchor = helper.absolutePos(new BlockPos(5, 6, 10));
		for (int x = 3; x <= 30; x++) {
			for (int z : new int[] {10, 51}) {
				helper.getLevel().setBlockAndUpdate(helper.absolutePos(new BlockPos(x, 5, z)), Blocks.STONE.defaultBlockState());
			}
		}
		for (int x = 5; x <= 12; x++) {
			for (int z = 11; z <= 50; z++) {
				helper.getLevel().setBlockAndUpdate(helper.absolutePos(new BlockPos(x, 1, z)), Blocks.STONE.defaultBlockState());
			}
		}
		helper.assertTrue(EquipmentPlacement.place(helper.getLevel(), anchor, EquipmentKind.OVERHEAD_CRANE, facing, null), "the crane fits");
		EquipmentBlockEntity crane = (EquipmentBlockEntity) helper.getLevel().getBlockEntity(anchor);
		helper.assertTrue(crane != null && crane.kind() == EquipmentKind.OVERHEAD_CRANE, "its anchor");
		helper.assertTrue(HallCrane.near(helper.getLevel(), anchor.offset(4, -3, 2)) == crane, "a pendant nearby finds it");
		long now = helper.getLevel().getGameTime();
		// EAST runs the bridge along its runway; held, it runs on; let go, it stops a few ticks later.
		HallCrane.press(helper.getLevel(), crane, "east", null);
		helper.assertTrue(crane.craneDir[HallCrane.BRIDGE] == 1 && Math.abs(crane.craneLo + 2) < EPS && Math.abs(crane.craneHi - 20) < EPS,
				"the bridge runs east, the runway's ends found: " + crane.craneLo + " " + crane.craneHi);
		helper.assertTrue(Math.abs(HallCrane.at(crane, HallCrane.BRIDGE, now + 4) - 0.4) < EPS, "2 blocks a second");
		helper.assertTrue(Math.abs(HallCrane.at(crane, HallCrane.BRIDGE, now + 100) - 0.1 * HallCrane.HOLD) < EPS
				&& !HallCrane.moving(crane, HallCrane.BRIDGE, now + 100), "it stops when the button is let go");
		// SOUTH runs the trolley across (the crane faces east: south is its right).
		HallCrane.press(helper.getLevel(), crane, "south", null);
		helper.assertTrue(crane.craneDir[HallCrane.TROLLEY] == 1 && Math.abs(HallCrane.at(crane, HallCrane.TROLLEY, now + 2) - (HallCrane.TROLLEY_HOME + 0.1)) < EPS,
				"the trolley runs south");
		HallCrane.press(helper.getLevel(), crane, "north", null);
		helper.assertTrue(crane.craneDir[HallCrane.TROLLEY] == -1, "and back north");
		// The main hook goes down to the floor under it (its tip on the floor's top at y 2: 4 blocks under the anchor).
		HallCrane.press(helper.getLevel(), crane, "main_down", null);
		double floor = 6 + HallCrane.MAIN_TIP - 2;
		helper.assertTrue(crane.craneDir[HallCrane.MAIN] == 1 && Math.abs(crane.craneStop[HallCrane.MAIN] - floor) < 1e-3,
				"the hook's stop is the floor: " + crane.craneStop[HallCrane.MAIN] + " for " + floor);
		helper.assertTrue(Math.abs(HallCrane.at(crane, HallCrane.MAIN, now + 6) - 0.15) < EPS, "half a block a second");
		// Up at its rest, it goes no higher.
		crane.craneDir[HallCrane.AUX] = 0;
		crane.craneFrom[HallCrane.AUX] = 0;
		HallCrane.press(helper.getLevel(), crane, "aux_up", null);
		helper.assertTrue(crane.craneDir[HallCrane.AUX] == 0, "the aux hook is up at its limit");
		// At the runway's end the bridge goes no further.
		crane.craneFrom[HallCrane.BRIDGE] = 20;
		crane.craneDir[HallCrane.BRIDGE] = 0;
		HallCrane.press(helper.getLevel(), crane, "east", null);
		helper.assertTrue(crane.craneDir[HallCrane.BRIDGE] == 0 && Math.abs(HallCrane.at(crane, HallCrane.BRIDGE, now + 10) - 20) < EPS, "at the runway's end");
		HallCrane.press(helper.getLevel(), crane, "west", null);
		helper.assertTrue(crane.craneDir[HallCrane.BRIDGE] == -1, "it runs back west");
		// STOP: everything stops where it is and the power is off; nothing runs until START.
		HallCrane.press(helper.getLevel(), crane, "stop", null);
		helper.assertTrue(!crane.cranePower && crane.craneDir[HallCrane.BRIDGE] == 0 && crane.craneDir[HallCrane.MAIN] == 0, "stopped, power off");
		HallCrane.press(helper.getLevel(), crane, "west", null);
		helper.assertTrue(crane.craneDir[HallCrane.BRIDGE] == 0, "no power, no motion");
		HallCrane.press(helper.getLevel(), crane, "start", null);
		HallCrane.press(helper.getLevel(), crane, "west", null);
		helper.assertTrue(crane.cranePower && crane.craneDir[HallCrane.BRIDGE] == -1, "START: it runs again");
		helper.succeed();
	}

	@GameTest(structure = "jugcraft-test:kiln_bowl", maxTicks = 20)
	public void turbineControlRoomReadsItsTurbine(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		Direction facing = Direction.SOUTH;
		BlockPos anchor = helper.absolutePos(new BlockPos(40, 1, 10));
		helper.assertTrue(EquipmentPlacement.place(helper.getLevel(), anchor, EquipmentKind.TURBINE_GENERATOR, facing, null), "the turbine fits");
		EquipmentBlockEntity turbine = (EquipmentBlockEntity) helper.getLevel().getBlockEntity(anchor);
		// A cabinet in the room joins it.
		BlockPos at = helper.absolutePos(new BlockPos(10, 1, 60));
		helper.getLevel().setBlockAndUpdate(at, PlantInterior.BLOCKS.get(TurbineControlRoom.EXCITATION).defaultBlockState()
				.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
		LivePanelBlockEntity cabinet = (LivePanelBlockEntity) helper.getLevel().getBlockEntity(at);
		helper.assertTrue(cabinet != null, "the excitation cabinet's live part");
		cabinet.serverTick(helper.getLevel());
		helper.assertTrue(anchor.equals(cabinet.panel), "it joined the turbine: " + cabinet.panel);
		// On load: the field breaker closed, the generator's volts and field as the boards have them, the 86G reset.
		turbine.turbineFlags = TurbineHall.PRESENT | TurbineHall.LATCHED | TurbineHall.ONLINE | TurbineHall.HOT;
		turbine.turbineRpm = 1800;
		turbine.turbineLoad = 1;
		TurbineControlRoom.Turbine t = TurbineControlRoom.Turbine.of(turbine);
		helper.assertTrue(t.excited() && !t.lockout() && !t.bowing(), "excited, reset, straight");
		Map<String, Double> numbers = new HashMap<>();
		Map<String, Integer> lit = new HashMap<>();
		Map<String, String> text = new HashMap<>();
		TurbineControlRoom.fill(numbers, lit, text, 4, 0, turbine, 0);
		helper.assertTrue(lit.get("exc_41_flag") == 1 && Math.abs(numbers.get("exc_fa") - 3.9) < 1e-6 && Math.abs(numbers.get("exc_fv") - 410) < 1e-6
				&& "22.0 KV".equals(text.get("exc_r0")), "the excitation: " + numbers + " " + text);
		TurbineControlRoom.fill(numbers, lit, text, 3, 0, turbine, 0);
		helper.assertTrue(numbers.get("tsi_b6") > 0.3 && numbers.get("tsi_b6") < 0.5 && Math.abs(numbers.get("tsi_thrust") - 0.5) < 1e-6
				&& "1,800 RPM".equals(text.get("tsi_rpm")), "the TSI at speed: " + numbers.get("tsi_b6") + " " + numbers.get("tsi_thrust"));
		ControlRoomConsoles.Line[] gen = TurbineControlRoom.lines("TURBINE GEN", turbine, 0);
		helper.assertTrue("LATCHED".equals(gen[0].value()) && "1,150 MWE".equals(gen[2].value()) && "60.00 HZ".equals(gen[3].value()),
				"the console's page: " + gen[0].value() + " " + gen[2].value() + " " + gen[3].value());
		// Tripped hot: the 86G locked out (TRIP and 32 lit); coasted down without its gear, the rotor bows.
		turbine.turbineFlags = TurbineHall.PRESENT | TurbineHall.HOT;
		turbine.turbineRpm = 0;
		turbine.turbineLoad = 0;
		TurbineControlRoom.fill(numbers, lit, text, 5, 0, turbine, 0);
		helper.assertTrue(lit.get("gp_lor_target") == 1 && numbers.get("gp_lor") == 1 && lit.get("gp_t00") == 1 && lit.get("gp_t03") == 1
				&& lit.get("gp_t01") == 0 && lit.get("gp_trip_ckt") == 0, "the 86G tripped");
		helper.assertTrue(TurbineControlRoom.Turbine.of(turbine).bowing() && TurbineControlRoom.lines("TSI", turbine, 0)[6].value().equals("5.8 MILS"),
				"the rotor bowing on the TSI page");
		TurbineControlRoom.fill(numbers, lit, text, 4, 0, turbine, 0);
		helper.assertTrue(lit.get("exc_41_flag") == 2 && numbers.get("exc_fa") == 0, "the field breaker open");
		// The lube oil room: the conditioner running with its heater on while the turbine is slow, its filter's DP.
		int f = TurbineHall.PRESENT;
		helper.assertTrue(TurbineHall.lamp("cond_run", f, 3) && TurbineHall.lamp("cond_heat", f, 3) && !TurbineHall.lamp("cond_heat", f, 1800)
				&& !TurbineHall.lamp("lox_a", f, 1800), "the conditioner's lamps");
		helper.assertTrue(TurbineHall.value("COND FILTER DP", f, 1800, 1) == 6 && TurbineHall.value("COND FILTER DP", 0, 0, 0) == 0
				&& TurbineHall.dial("LO XFER DISCH")[1] == 100, "its gauges");
		helper.succeed();
	}
}
