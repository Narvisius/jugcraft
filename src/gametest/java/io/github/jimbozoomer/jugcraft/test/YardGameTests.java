package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.nuclear.JugcraftNuclear;
import io.github.jimbozoomer.jugcraft.nuclear.PipeKind;
import io.github.jimbozoomer.jugcraft.nuclear.ReactorCore;
import io.github.jimbozoomer.jugcraft.nuclear.SiteData;
import io.github.jimbozoomer.jugcraft.nuclear.SiteRegistry;
import io.github.jimbozoomer.jugcraft.nuclear.Sites;
import io.github.jimbozoomer.jugcraft.nuclear.YardBlock;
import io.github.jimbozoomer.jugcraft.nuclear.YardBlockEntity;
import io.github.jimbozoomer.jugcraft.nuclear.Yards;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

/**
 * The medium plant's transformer yard and switchyard (transformer yard and switchyard build, 20 October 2026): the
 * transformers warm with their load and their fans come on in stages; the switchyard's breaker follows the generator
 * and the power flows out through it; the disconnect arcs if opened under load, and its earth switch is interlocked
 * with it; the deluge zones trip and reset; a reactor's generator finds the yard on its bus.
 */
public class YardGameTests {
	private static boolean skip() {
		return TestFocus.skip("yards") && TestFocus.skip("nuclear");
	}

	private static YardBlockEntity yard(GameTestHelper helper, BlockPos world, YardBlock.Works works) {
		helper.getLevel().setBlockAndUpdate(world, JugcraftNuclear.YARD.defaultBlockState().setValue(YardBlock.FACING, Direction.SOUTH)
				.setValue(YardBlock.WORKS, works));
		SiteRegistry.add(helper.getLevel(), world, Direction.SOUTH, works.site);
		return (YardBlockEntity) helper.getLevel().getBlockEntity(world);
	}

	/** A second of the generator running at a share of its rating (the reactor handing its power to the yard). */
	private static void second(ServerLevel level, YardBlockEntity y, YardBlockEntity sw, double share) {
		for (int t = 0; t < 20; t++) {
			if (share > 0) y.take(Math.round(ReactorCore.RATED_JE * share));
			y.serverTransfer(level);
			if (sw != null) {
				sw.serverTransfer(level);
				// (line 1 takes what the switchyard passes on)
				sw.output.setAmount(0);
			}
		}
		y.serverSecond(level);
		if (sw != null) sw.serverSecond(level);
	}

	private static int var(SiteData d, String name) {
		for (int i = 0; i < d.variables.size(); i++) {
			if (d.variables.get(i).name().equals(name)) return i;
		}
		throw new IllegalArgumentException(name);
	}

	/**
	 * The thermal model (IEEE C57.91's, at game speed): a main transformer at full load settles with both fan groups
	 * running at about 71 C top oil and 84 C winding; a unit auxiliary transformer carrying the house load at about 58 / 77
	 * C with its fans; SAT-1 lightly loaded at about 46 C with its fans off, with the house load on group 1. Too hot, a
	 * transformer trips, and resets once it has cooled.
	 */
	@GameTest(maxTicks = 20)
	public void transformersWarmAndCoolInStages(GameTestHelper helper) {
		if (skip()) {
			helper.succeed();
			return;
		}
		Yards.Unit gsu = new Yards.Unit(Yards.Kind.GSU), uat = new Yards.Unit(Yards.Kind.UAT), sat = new Yards.Unit(Yards.Kind.SAT),
				house = new Yards.Unit(Yards.Kind.SAT);
		boolean firstGroupFirst = false;
		for (int s = 0; s < 3600; s++) {
			gsu.second(25, 1, true);
			uat.second(25, YardBlockEntity.HOUSE_LOAD, true);
			sat.second(25, YardBlockEntity.SAT_LIGHT, true);
			house.second(25, YardBlockEntity.SAT_HOUSE, true);
			if (gsu.stage == 1 && !firstGroupFirst) firstGroupFirst = gsu.oil < 75;
		}
		helper.assertTrue(firstGroupFirst, "group 1 starts before group 2");
		helper.assertTrue(gsu.stage == 2 && Math.abs(gsu.oil - 71) < 2 && Math.abs(gsu.wdg - 84) < 2.5,
				"MT at full load: both groups, 71/84 C: " + gsu.oil + " / " + gsu.wdg + " stage " + gsu.stage);
		helper.assertTrue(uat.stage == 1 && Math.abs(uat.oil - 58) < 2.5 && Math.abs(uat.wdg - 77) < 3,
				"UAT on the house load: fans, 58/77 C: " + uat.oil + " / " + uat.wdg);
		helper.assertTrue(sat.stage == 0 && Math.abs(sat.oil - 46) < 2.5, "SAT-1 lightly loaded: fans off, 46 C: " + sat.oil);
		helper.assertTrue(house.stage == 1 && Math.abs(house.oil - 63) < 3, "SAT-1 on the house load: group 1, 63 C: " + house.oil);
		helper.assertTrue(!gsu.hot() && !gsu.tripped, "no alarm at full load");
		helper.assertTrue(gsu.oilMax >= gsu.oil && gsu.wdgMax >= gsu.wdg, "the drag hands stand at the highest");
		gsu.oil = 106;
		gsu.second(25, 1, true);
		helper.assertTrue(gsu.tripped && gsu.hot(), "over 105 C top oil: HI TEMP and the trip");
		for (int s = 0; s < 3600; s++) {
			gsu.second(25, 0, false);
		}
		helper.assertTrue(!gsu.tripped && gsu.oil < 30 && gsu.stage == 0, "de-energized it cools, its fans stop and the lockout resets: " + gsu.oil);
		helper.succeed();
	}

	/**
	 * The breaker follows the plant (Justin, 18 October: cosmetic, it closes and opens on its own): the generator comes
	 * on and BKR 1-1 closes a couple of seconds later; the power goes out through it to line 1; the generator stops and it
	 * opens. Its counter counts both. The main transformers carry the export, the UATs the house load.
	 */
	@GameTest(maxTicks = 20)
	public void breakerFollowsTheGenerator(GameTestHelper helper) {
		if (skip()) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(16_000, 1, 16_000);
		YardBlockEntity y = yard(helper, base, YardBlock.Works.YARD);
		YardBlockEntity sw = yard(helper, base.offset(74, 0, 4), YardBlock.Works.SWITCHYARD);
		helper.assertTrue(y != null && sw != null && y.siteKey().equals("transformer_yard") && sw.siteKey().equals("switchyard"), "both anchor their sites");
		helper.assertTrue(Yards.nearest(level, base, YardBlock.Works.SWITCHYARD) == sw, "the yard finds its switchyard");
		int ops = sw.breakerOps();
		helper.assertTrue(!sw.breakerClosed(), "built with the generator off: the breaker open");
		second(level, y, sw, 1.0);
		helper.assertTrue(y.generatorRunning() && !sw.breakerClosed(), "the generator on: the breaker not closed yet");
		second(level, y, sw, 1.0);
		helper.assertTrue(sw.breakerClosed() && sw.breakerOps() == ops + 1, "two seconds on: BKR 1-1 closes, its counter counts it");
		second(level, y, sw, 1.0);
		helper.assertTrue(y.exportLoad() > 0.9F, "the power goes out through it: " + y.exportLoad());
		helper.assertTrue(y.unit(0).energized && y.unit(3).energized, "the main and unit auxiliary transformers energized");
		second(level, y, sw, 0);
		second(level, y, sw, 0);
		helper.assertTrue(!y.generatorRunning() && !sw.breakerClosed() && sw.breakerOps() == ops + 2, "the generator stops: it opens");
		helper.assertTrue(!y.unit(0).energized && sw.unit(0).load == YardBlockEntity.SAT_HOUSE, "SAT-1 carries the house load");
		SiteRegistry.remove(level, base);
		SiteRegistry.remove(level, base.offset(74, 0, 4));
		helper.succeed();
	}

	/**
	 * Diesels make real power (27 October 2026): a strike trips the switchyard's lines; the grid is lost (BKR 1-1 opens,
	 * and won't close onto it while the generator runs) until they reclose by themselves, when the breaker follows the
	 * generator again. With the generator off and the grid there, SAT-1 feeds the plant's load centres.
	 */
	@GameTest(maxTicks = 200)
	public void linesTripAndReclose(GameTestHelper helper) {
		if (skip()) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(20_000, 1, 20_000);
		YardBlockEntity y = yard(helper, base, YardBlock.Works.YARD);
		YardBlockEntity sw = yard(helper, base.offset(74, 0, 4), YardBlock.Works.SWITCHYARD);
		for (int k = 0; k < 3; k++) second(level, y, sw, 1.0);
		helper.assertTrue(sw.breakerClosed() && sw.gridLive(level.getGameTime()), "the generator on the grid: BKR 1-1 closed");
		helper.assertTrue(y.switchyard() == sw, "the yard knows its switchyard");
		sw.tripLines(level, 60);
		helper.assertTrue(!sw.gridLive(level.getGameTime()) && !sw.breakerClosed(), "a strike trips the lines: the grid lost, BKR 1-1 open");
		second(level, y, sw, 1.0);
		second(level, y, sw, 1.0);
		second(level, y, sw, 1.0);
		helper.assertTrue(!sw.breakerClosed(), "it won't close onto the dead grid");
		helper.runAfterDelay(70, () -> {
			second(level, y, sw, 1.0);
			helper.assertTrue(sw.gridLive(level.getGameTime()) && sw.gridBack() == 0, "the lines reclose by themselves");
			second(level, y, sw, 1.0);
			second(level, y, sw, 1.0);
			helper.assertTrue(sw.breakerClosed(), "and BKR 1-1 follows the generator again");
			SiteRegistry.remove(level, base);
			SiteRegistry.remove(level, base.offset(74, 0, 4));
			helper.succeed();
		});
	}

	/**
	 * 89-1 (decided 18 October: disconnects can't break load): opened while the breaker carries the generator it arcs
	 * and the breaker trips; it stays open until 89-1 is closed again. The earth switch goes on only with 89-1 open, and
	 * 89-1 won't close with it on. The blades' and flags' states follow.
	 */
	@GameTest(maxTicks = 60)
	public void disconnectArcsUnderLoad(GameTestHelper helper) {
		if (skip()) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(17_000, 1, 17_000);
		YardBlockEntity y = yard(helper, base, YardBlock.Works.YARD);
		YardBlockEntity sw = yard(helper, base.offset(74, 0, 4), YardBlock.Works.SWITCHYARD);
		Player p = helper.makeMockPlayer(GameType.SURVIVAL);
		for (int s = 0; s < 3; s++) second(level, y, sw, 1.0);
		helper.assertTrue(sw.breakerClosed(), "running: the breaker closed");
		sw.onZone(level, p, "es_toggle");
		helper.assertTrue(sw.esPos() == 0, "the earth switch is interlocked while 89-1 is closed");
		SiteData d = SiteData.get("switchyard");
		int dsflag = var(d, "dsflag"), esflag = var(d, "esflag");
		helper.assertTrue(sw.value(dsflag) == d.variables.get(dsflag).indexOf("True"), "89-1's flag: CLOSED");
		int ops = sw.breakerOps();
		sw.onZone(level, p, "ds_open");
		helper.runAfterDelay(YardBlockEntity.ARC_AFTER + 2, () -> {
			sw.serverTransfer(level);
			helper.assertTrue(!sw.breakerClosed() && sw.breakerOps() == ops + 1, "the arc: the breaker trips");
			sw.finishTravel();
			helper.assertTrue(sw.dsPos() == 1 && sw.value(dsflag) == d.variables.get(dsflag).indexOf("False"), "89-1 open: its flag OPEN");
			second(level, y, sw, 1.0);
			second(level, y, sw, 1.0);
			helper.assertTrue(!sw.breakerClosed(), "the breaker stays open with 89-1 open");
			sw.onZone(level, p, "es_toggle");
			sw.finishTravel();
			helper.assertTrue(sw.esPos() == 1 && sw.value(esflag) == d.variables.get(esflag).indexOf("True"), "earthed: its plate EARTHED");
			sw.onZone(level, p, "ds_close");
			sw.finishTravel();
			helper.assertTrue(sw.dsPos() == 1, "89-1 won't close while earthed");
			sw.onZone(level, p, "es_toggle");
			sw.finishTravel();
			sw.onZone(level, p, "ds_close");
			sw.finishTravel();
			helper.assertTrue(sw.dsPos() == 0 && sw.esPos() == 0, "earth off, 89-1 closed again");
			second(level, y, sw, 1.0);
			second(level, y, sw, 1.0);
			helper.assertTrue(sw.breakerClosed(), "the generator still on: the breaker closes again");
			SiteRegistry.remove(level, base);
			SiteRegistry.remove(level, base.offset(74, 0, 4));
			helper.succeed();
		});
	}

	/** The deluge valve house: a manual pull release trips its zone (its lamps and gauges follow) until RESET. */
	@GameTest(maxTicks = 20)
	public void delugeZonesTripAndReset(GameTestHelper helper) {
		if (skip()) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(18_000, 1, 18_000);
		YardBlockEntity y = yard(helper, base, YardBlock.Works.YARD);
		Player p = helper.makeMockPlayer(GameType.SURVIVAL);
		SiteData d = SiteData.get("transformer_yard");
		int dv3 = var(d, "dv3");
		helper.assertTrue(y.deluge() == 0 && y.value(dv3) == d.variables.get(dv3).indexOf("False"), "built armed");
		y.onZone(level, p, "dv_pull:2");
		helper.assertTrue(y.deluge() == 4 && y.value(dv3) == d.variables.get(dv3).indexOf("True"), "DV-3's pull: MT-1C's zone trips");
		y.onZone(level, p, "dv_reset");
		helper.assertTrue(y.deluge() == 0, "RESET re-arms it");
		helper.assertTrue(d.live.getAsJsonObject("sprays").getAsJsonArray("DV-3").size() > 20, "MT-1C's rings have their nozzles");
		SiteRegistry.remove(level, base);
		helper.succeed();
	}

	/** A reactor's generator finds the transformer yard where its isolated phase bus meets the yard's (the bus port). */
	@GameTest(maxTicks = 20)
	public void generatorFindsTheYardOnItsBus(GameTestHelper helper) {
		if (skip()) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(19_000, 1, 19_000);
		yard(helper, base, YardBlock.Works.YARD);
		SiteData d = SiteData.get("transformer_yard");
		var port = d.live.getAsJsonArray("bus_port");
		SiteRegistry.Placement pl = SiteRegistry.placements(level, "transformer_yard").stream().filter(x -> x.anchor().equals(base)).findFirst().orElseThrow();
		BlockPos portCell = pl.world(port.get(0).getAsInt(), port.get(1).getAsInt(), port.get(2).getAsInt());
		helper.assertTrue(SiteRegistry.find(level, portCell) != null, "the bus port is one of the yard's blocks");
		// The generator's bus ends in the turbine hall's wall, beside it.
		BlockPos pipe = portCell.west();
		level.setBlockAndUpdate(pipe, JugcraftNuclear.PIPES.get(PipeKind.BUS).defaultBlockState());
		helper.assertTrue(Yards.yardsFrom(level, List.of(pipe)).contains(base), "following the bus pipes finds the yard");
		SiteRegistry.remove(level, base);
		helper.succeed();
	}

	/**
	 * The medium plant's reactor building (22 October 2026: the approved machines as compiled sites): its reactor
	 * equipment stands hidden under the loop floor and still forms a working four-loop unit by its buried pipes; the
	 * building's controller puts its site in at power and swaps to the refuelling one when the reactor is opened for
	 * refuelling (and back); the operating floor is solid to stand on.
	 */
	@GameTest(maxTicks = 40)
	public void reactorBuildingFormsHiddenAndShows(GameTestHelper helper) {
		if (TestFocus.skip("reactorbuilding") && TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos stake = helper.absolutePos(BlockPos.ZERO).offset(-5_000, 0, 6_000);
		io.github.jimbozoomer.jugcraft.blueprint.Blueprint print = io.github.jimbozoomer.jugcraft.blueprint.Blueprint.get("nuclear_medium_pwr", false);
		var built = io.github.jimbozoomer.jugcraft.nuclear.PlantBuilder.build(level, print, stake, net.minecraft.world.level.block.Rotation.NONE, null, true);
		helper.assertTrue(built.vessels().size() == 1, "one reactor: " + built.vessels().size());
		BlockPos vessel = built.vessels().get(0);
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.EquipmentAnchorBlock.hidden(level.getBlockState(vessel)), "the vessel stands hidden");
		var unit = io.github.jimbozoomer.jugcraft.nuclear.ReactorUnit.find(level, vessel);
		helper.assertTrue(unit.loops() == 4 && unit.pressurizers.size() == 1 && unit.formed(),
				"four loops and a pressurizer, formed: loops " + unit.loops() + ", missing " + unit.missing());
		BlockPos controller = null;
		for (int i = 0; i < print.size(); i++) {
			if (print.state(i, net.minecraft.world.level.block.Rotation.NONE).getBlock() instanceof io.github.jimbozoomer.jugcraft.nuclear.ReactorBuildingBlock) {
				controller = stake.offset(print.offset(i, net.minecraft.world.level.block.Rotation.NONE));
			}
		}
		helper.assertTrue(controller != null && level.getBlockEntity(controller) instanceof io.github.jimbozoomer.jugcraft.nuclear.ReactorBuildingBlockEntity,
				"the reactor building's controller is built");
		var rb = (io.github.jimbozoomer.jugcraft.nuclear.ReactorBuildingBlockEntity) level.getBlockEntity(controller);
		rb.serverTick(level);
		rb.update(level);
		var at = SiteRegistry.at(level, controller);
		helper.assertTrue(at != null && at.data().key.equals("reactor_building"), "at power its site is in: " + (at == null ? "none" : at.data().key));
		// the operating floor west of the internals' bay (its slab: site cell y 14)
		BlockPos floor = at.world(-14, 14, -9);
		var shape = io.github.jimbozoomer.jugcraft.nuclear.SiteBlock.shape(level, floor);
		helper.assertTrue(level.getBlockState(floor).getBlock() instanceof io.github.jimbozoomer.jugcraft.nuclear.SiteBlock && !shape.isEmpty()
				&& shape.max(Direction.Axis.Y) > 0.99, "the operating floor is solid: " + level.getBlockState(floor) + " " + shape);
		var v = (io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity) level.getBlockEntity(vessel);
		v.core.refuelOpen = true;
		rb.update(level);
		at = SiteRegistry.at(level, controller);
		helper.assertTrue(rb.refuelling() && at != null && at.data().key.equals("reactor_building_refuel"), "refuelling, its refuelling site is in");
		v.core.refuelOpen = false;
		rb.update(level);
		at = SiteRegistry.at(level, controller);
		helper.assertTrue(at != null && at.data().key.equals("reactor_building"), "closed again, back at power");
		Sites.unload(rb, level);
		helper.succeed();
	}
}
