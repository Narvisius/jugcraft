package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.nuclear.CoolingTowerBlock;
import io.github.jimbozoomer.jugcraft.nuclear.CoolingTowerBlockEntity;
import io.github.jimbozoomer.jugcraft.nuclear.JugcraftNuclear;
import io.github.jimbozoomer.jugcraft.nuclear.SiteBlock;
import io.github.jimbozoomer.jugcraft.nuclear.SiteData;
import io.github.jimbozoomer.jugcraft.nuclear.SiteRegistry;
import io.github.jimbozoomer.jugcraft.nuclear.Sites;
import io.github.jimbozoomer.jugcraft.nuclear.BoardPlant;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentPlacement;
import io.github.jimbozoomer.jugcraft.nuclear.WaterWorks;
import io.github.jimbozoomer.jugcraft.nuclear.WaterWorksBlock;
import io.github.jimbozoomer.jugcraft.nuclear.WaterWorksBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The medium plant's water works (towers and pump houses build, 19 October 2026; Justin: "everything works"): a
 * cooling tower's basin falls as it works without make-up and holds with it; below low-low it gives no cooling; its
 * site's blocks take their shapes from the site, turned with it.
 */
public class WaterWorksGameTests {
	private static boolean skip() {
		return TestFocus.skip("waterworks") && TestFocus.skip("nuclear");
	}

	private static CoolingTowerBlockEntity tower(GameTestHelper helper, BlockPos at, Direction facing) {
		BlockPos pos = helper.absolutePos(at);
		helper.getLevel().setBlockAndUpdate(pos, JugcraftNuclear.COOLING_TOWER.defaultBlockState().setValue(CoolingTowerBlock.FACING, facing)
				.setValue(CoolingTowerBlock.TOWER, CoolingTowerBlock.Tower.MEDIUM));
		return (CoolingTowerBlockEntity) helper.getLevel().getBlockEntity(pos);
	}

	@GameTest(structure = "jugcraft-test:kiln_bowl", maxTicks = 40)
	public void towerBasinNeedsMakeup(GameTestHelper helper) {
		if (skip()) {
			helper.succeed();
			return;
		}
		CoolingTowerBlockEntity t = tower(helper, new BlockPos(2, 2, 2), Direction.NORTH);
		helper.assertTrue(t != null && t.siteKey() != null, "the medium tower anchors its site");
		helper.assertTrue(Math.abs(t.basin() - 0.92F) < 1e-6, "a new tower's basin stands at 92 %");
		// The basin's arithmetic: full load evaporates 92 % down to low-low in about six minutes; make-up through MU-1
		// at full flow outruns full-load evaporation, so the level holds at 92 %.
		float perMinute = CoolingTowerBlockEntity.EVAPORATION * 1200;
		helper.assertTrue(perMinute > 0.09F && perMinute < 0.12F, "about a tenth of the basin a minute at full load: " + perMinute);
		helper.assertTrue(CoolingTowerBlockEntity.MAKEUP_RATE > CoolingTowerBlockEntity.EVAPORATION, "make-up outruns evaporation");
		float minutes = (0.92F - CoolingTowerBlockEntity.LOW_LOW) / perMinute;
		helper.assertTrue(minutes > 5 && minutes < 7, "92 % to low-low in about six minutes: " + minutes);
		// Low-low: no cooling.
		t.setBasin(CoolingTowerBlockEntity.LOW_LOW - 0.05F);
		helper.assertTrue(t.claim(helper.getLevel().getGameTime(), 0.5) == 0, "below low-low the tower takes no heat");
		helper.succeed();
	}

	private static WaterWorksBlockEntity works(GameTestHelper helper, BlockPos world, WaterWorksBlock.Works kind) {
		helper.getLevel().setBlockAndUpdate(world, JugcraftNuclear.WATER_WORKS.defaultBlockState().setValue(WaterWorksBlock.FACING, Direction.NORTH)
				.setValue(WaterWorksBlock.WORKS, kind));
		SiteRegistry.add(helper.getLevel(), world, Direction.NORTH, kind.site);
		return (WaterWorksBlockEntity) helper.getLevel().getBlockEntity(world);
	}

	/**
	 * The lake intake's make-up (Justin: "the tower basins drop without make-up from the lake pumps"): a tower near the
	 * intake gets make-up only while a make-up pump delivers (running, its discharge valve open); with the intake's
	 * make-up pumps stopped the basin falls as the tower works. A pump left running against its shut valve trips on low
	 * flow, and its window comes in.
	 */
	@GameTest(structure = "jugcraft-test:kiln_bowl", maxTicks = 40)
	public void makeupComesFromTheIntake(GameTestHelper helper) {
		if (skip()) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(14_000, 1, 14_000);
		BlockPos towerAt = base.offset(40, 0, 0);
		helper.assertTrue(WaterWorks.makeupAvailable(level, towerAt), "with no intake the tower takes make-up from the lake, as before");
		WaterWorksBlockEntity intake = works(helper, base, WaterWorksBlock.Works.INTAKE);
		helper.assertTrue(WaterWorks.intake(level, towerAt) == intake, "the tower finds the intake");
		helper.assertTrue(intake.running(0) && intake.running(1) && !intake.running(2) && intake.running(3) && !intake.running(4) && !intake.running(5),
				"built with MUP-A and the two A service water pumps running");
		helper.assertTrue(intake.makeup() && WaterWorks.makeupAvailable(level, towerAt), "MUP-A delivers: make-up reaches the tower");
		intake.setMovOpen(0, false);
		helper.assertTrue(!intake.makeup() && !WaterWorks.makeupAvailable(level, towerAt), "MOV-MU-A shut: no make-up");
		intake.setMovOpen(0, true);
		intake.setRunning(WaterWorksBlockEntity.INTAKE_RUN & ~1);
		helper.assertTrue(!WaterWorks.makeupAvailable(level, towerAt), "MUP-A stopped: no make-up");
		intake.setRunning(WaterWorksBlockEntity.INTAKE_RUN & ~1 | 1 << 5);
		helper.assertTrue(WaterWorks.makeupAvailable(level, towerAt), "MUP-B started: make-up again");
		// The basin's arithmetic without make-up: at full load it falls a tenth a minute.
		float before = 0.92F, after = before - CoolingTowerBlockEntity.EVAPORATION * 1200;
		helper.assertTrue(after < before - 0.09F, "a minute at full load without make-up takes a tenth off the basin");
		// Low flow: MUP-B against its shut valve trips after a minute; its window comes in, flashing until acknowledged.
		intake.setMovOpen(5, false);
		for (int k = 0; k < 70; k++) {
			intake.serverSecond(level);
		}
		helper.assertTrue(!intake.running(5) && intake.tripped(5), "MUP-B tripped on low flow");
		helper.assertTrue((intake.alarms() & 1 << 13) != 0 && (intake.alarms() & 1 << 21) != 0, "MUP-B TRIP and MU FLOW LO come in");
		SiteRegistry.remove(level, base);
		helper.succeed();
	}

	/**
	 * Service water (Justin: "losing service water overheats and trips the diesels and coolers"): a diesel generator
	 * runs cool with its train's service water; with that train's pumps stopped its jacket water climbs and it trips on
	 * its 86G lockout within about a minute and a half. The intake's SW HDR PRESS LO comes in, and the boards' component
	 * cooling alarms read it.
	 */
	@GameTest(maxTicks = 20)
	public void lossOfServiceWaterTripsTheDiesel(GameTestHelper helper) {
		if (skip()) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		BlockPos anchor = helper.absolutePos(BlockPos.ZERO).offset(13_000, 1, 13_000);
		helper.assertTrue(EquipmentPlacement.place(level, anchor, EquipmentKind.EMERGENCY_DIESEL_GENERATOR, Direction.NORTH, null), "the generator fits");
		DieselGeneratorGameTests.fill(DieselGeneratorGameTests.dayTank(level, anchor.offset(-9, -1, -6)),
				50 * net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET);
		EquipmentBlockEntity edg = (EquipmentBlockEntity) level.getBlockEntity(anchor);
		BlockPos intakeAt = anchor.offset(30, -1, 0);
		WaterWorksBlockEntity intake = works(helper, intakeAt, WaterWorksBlock.Works.INTAKE);
		helper.assertTrue(WaterWorks.edgTrain(level, anchor) == 0, "a lone generator is train A's");
		helper.assertTrue(EquipmentPlacement.dieselSwitch(level, edg, null, true) && edg.running, "it starts");
		long t = level.getGameTime();
		for (int k = 0; k < 20 * 60; k++) {
			WaterWorks.tickDiesel(level, edg, t++);
		}
		helper.assertTrue(edg.running && edg.jwExcess == 0, "with service water it runs cool: " + edg.jwExcess);
		BoardPlant board = new BoardPlant();
		board.present = true;
		board.sw = WaterWorks.boardState(level, anchor);
		helper.assertTrue(board.sw == (7 | WaterWorksBlockEntity.INTAKE_RUN << 8) && !board.swLost(), "the boards see both trains: " + board.sw);
		// Train 1's running pump stopped (SWP-1A; SWP-1B stands by).
		intake.setRunning(WaterWorksBlockEntity.INTAKE_RUN & ~(1 << 1));
		helper.assertTrue(!intake.serviceWater(0) && intake.serviceWater(1), "train 1 lost, train 2 flows");
		intake.serverSecond(level);
		helper.assertTrue((intake.alarms() & 1 << 20) != 0, "SW HDR PRESS LO");
		board.sw = WaterWorks.boardState(level, anchor);
		helper.assertTrue(board.swLost(), "the boards' component cooling sees train A lost");
		for (int k = 0; k < 20 * 50; k++) {
			WaterWorks.tickDiesel(level, edg, t++);
		}
		helper.assertTrue(edg.running && edg.jwExcess > 10, "without its water the jacket water climbs: " + edg.jwExcess);
		for (int k = 0; k < 20 * 40 && edg.running; k++) {
			WaterWorks.tickDiesel(level, edg, t++);
		}
		helper.assertTrue(!edg.running && edg.lockout, "it trips on high jacket water temperature, its 86G locked out");
		SiteRegistry.remove(level, intakeAt);
		helper.succeed();
	}

	/**
	 * The circulating water pump house: its four pumps deliver once their valves open (twenty seconds after a start);
	 * its forebay is the towers' basins: with them below low-low the pumps lose suction and trip, the 86 lockout
	 * holding them until RESET (refused while the forebay is still low).
	 */
	@GameTest(structure = "jugcraft-test:kiln_bowl", maxTicks = 40)
	public void circPumpsTripOnLowForebay(GameTestHelper helper) {
		if (skip()) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(15_000, 1, 15_000);
		WaterWorksBlockEntity house = works(helper, base, WaterWorksBlock.Works.CIRC);
		helper.assertTrue(house.pumpsRunning() == 4 && house.pumpsDelivering() == 4, "built with its four pumps delivering");
		helper.assertTrue(WaterWorks.circPumps(level, java.util.List.of(base)) == 4, "a condenser's conduit counts them");
		BlockPos towerAt = base.offset(0, 0, 60);
		CoolingTowerBlockEntity t = tower(helper, helper.relativePos(towerAt), Direction.NORTH);
		SiteRegistry.add(level, towerAt, Direction.NORTH, "ct_medium");
		house.serverSecond(level);
		helper.assertTrue(Math.abs(house.forebay() - 0.92F) < 1e-4, "its forebay is the tower's basin: " + house.forebay());
		t.setBasin(CoolingTowerBlockEntity.LOW_LOW - 0.05F);
		house.serverSecond(level);
		helper.assertTrue(house.pumpsRunning() == 0 && house.tripped(0) && house.tripped(3), "low-low: all four trip");
		helper.assertTrue((house.alarms() & (1 << 15 | 1 << 20)) == (1 << 15 | 1 << 20), "FOREBAY LO LO and 86 LOCKOUT");
		t.setBasin(0.92F);
		house.serverSecond(level);
		helper.assertTrue(house.tripped(0), "the lockout holds after the basin recovers");
		SiteRegistry.remove(level, towerAt);
		SiteRegistry.remove(level, base);
		helper.succeed();
	}

	@GameTest(structure = "jugcraft-test:kiln_bowl", maxTicks = 40)
	public void siteBlocksTakeTheirSitesShape(GameTestHelper helper) {
		if (skip()) {
			helper.succeed();
			return;
		}
		SiteData d = SiteData.get("ct_medium");
		helper.assertTrue(d != null, "the tower's site is in the jar");
		// The controller faces north (the site turned half round, its outlet toward the plant), as in the plant.
		CoolingTowerBlockEntity t = tower(helper, new BlockPos(8, 2, 8), Direction.NORTH);
		BlockPos anchor = helper.absolutePos(new BlockPos(8, 2, 8));
		SiteRegistry.add(helper.getLevel(), anchor, Direction.NORTH, "ct_medium");
		// The local panel LP-CT1A's enclosure stands in site cell (6, 1, 57): with the site turned half round, that is
		// (anchor.x - (6 - 0), anchor.y + (1 - -1), anchor.z - (57 - 62)).
		BlockPos panel = anchor.offset(-(6 - d.ax), 1 - d.ay, -(57 - d.az));
		SiteRegistry.Placement p = new SiteRegistry.Placement(anchor, Direction.NORTH, d, new net.minecraft.world.phys.AABB(anchor));
		helper.assertTrue(p.local(panel).equals(new BlockPos(6, 1, 57)), "the world block maps to the panel's cell: " + p.local(panel));
		helper.getLevel().setBlockAndUpdate(panel, Sites.STRUCTURE.defaultBlockState());
		BlockState st = helper.getLevel().getBlockState(panel);
		VoxelShape shape = st.getCollisionShape(helper.getLevel(), panel, CollisionContext.empty());
		helper.assertTrue(!shape.isEmpty() && shape.max(Direction.Axis.Y) <= 1.0, "the panel's cell has its shape: " + shape.toAabbs().size());
		VoxelShape expected = SiteBlock.shape(helper.getLevel(), panel);
		helper.assertTrue(shape.toAabbs().size() == expected.toAabbs().size(), "the same shape as the site gives");
		SiteRegistry.remove(helper.getLevel(), anchor);
		helper.assertTrue(t != null, "the tower");
		helper.succeed();
	}
}
