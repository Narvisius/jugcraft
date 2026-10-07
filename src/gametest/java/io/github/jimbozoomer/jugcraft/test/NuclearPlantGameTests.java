package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.blueprint.Blueprint;
import io.github.jimbozoomer.jugcraft.blueprint.BuildSite;
import io.github.jimbozoomer.jugcraft.nuclear.CoolingTowerBlockEntity;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind;
import io.github.jimbozoomer.jugcraft.nuclear.JugcraftNuclear;
import io.github.jimbozoomer.jugcraft.nuclear.PlantBuilder;
import io.github.jimbozoomer.jugcraft.nuclear.ReactorCore;
import io.github.jimbozoomer.jugcraft.nuclear.ReactorUnit;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Rotation;

/**
 * The pre-made nuclear plants (docs/features/nuclear.md, "Pre-made plants"): the plant and part blueprints load from
 * the jar at their full size, the new core designs and cooling towers behave, and a whole small modular plant built by
 * /nuclear build forms six working units with both cooling tower banks.
 */
public class NuclearPlantGameTests {
	/** Block counts the blueprints must have (the same as tools/nuclear_plants.py writes; checked again there). */
	private static final String[][] PLANTS = {{"nuclear_small_modular", "78685"}, {"nuclear_medium_pwr", "331638"}, {"nuclear_large_epr", "382021"}};

	@GameTest(maxTicks = 20)
	public void plantBlueprintsLoadWhole(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		for (String[] plant : PLANTS) {
			Blueprint blueprint = Blueprint.get(plant[0], false);
			helper.assertTrue(blueprint != null, plant[0] + " loads");
			helper.assertTrue(blueprint.size() == Integer.parseInt(plant[1]), plant[0] + " has " + plant[1] + " blocks: " + blueprint.size());
			helper.assertTrue(blueprint.kind == Blueprint.Kind.COMPLETE && blueprint.category == Blueprint.Category.SET, plant[0] + " is a blue set");
			helper.assertTrue(blueprint.large() && blueprint.local() && blueprint.json.isEmpty(), plant[0] + " is a large built-in, never sent");
			helper.assertTrue(!blueprint.parts.isEmpty(), plant[0] + " lists its parts");
		}
		for (String part : new String[] {"ct_natural", "ct_natural_large", "ct_mechanical", "pwr_containment", "epr_turbine_hall"}) {
			Blueprint blueprint = Blueprint.get(part, false);
			helper.assertTrue(blueprint != null && blueprint.kind == Blueprint.Kind.PART, part + " is a green part");
		}
		// Bottom first, and a site finds its cells again.
		Blueprint tower = Blueprint.get("ct_natural", false);
		BuildSite site = new BuildSite(tower, Rotation.CLOCKWISE_90, BlockPos.ZERO);
		for (int i = 1; i < site.size(); i++) {
			helper.assertTrue(site.layerOf(i) >= site.layerOf(i - 1), "cells go bottom layer first");
		}
		helper.assertTrue(site.find(site.blockPos(site.size() / 2)) == site.size() / 2, "find gives the cell back");
		helper.succeed();
	}

	/** The EPR core makes 1.6 times the standard output with two turbines, one turbine's worth with one; a module an eighth. */
	@GameTest(maxTicks = 20)
	public void coreDesignsAndTowers(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		long two = run(ReactorCore.Design.EPR, 2 * ReactorCore.RATED_JE, 0);
		long one = run(ReactorCore.Design.EPR, ReactorCore.RATED_JE, 0);
		long module = run(ReactorCore.Design.SMALL_MODULAR, ReactorCore.RATED_JE, 0);
		helper.assertTrue(Math.abs(two - ReactorCore.RATED_JE * 8 / 5) < ReactorCore.RATED_JE / 30, "EPR with two turbines: " + two);
		helper.assertTrue(one == ReactorCore.RATED_JE, "EPR with one turbine is capped: " + one);
		helper.assertTrue(Math.abs(module - ReactorCore.RATED_JE / 8) < ReactorCore.RATED_JE / 200, "module: " + module);
		// With a cooling tower taking all the heat, the condenser only asks for make-up water.
		ReactorCore core = fuelled(ReactorCore.Design.STANDARD);
		ReactorCore.Plant towered = new ReactorCore.Plant(1, true, true, ReactorCore.RATED_JE, 1);
		for (int t = 0; t < 20 * 600; t++) {
			core.tick(towered, need -> need);
		}
		helper.assertTrue(core.need <= ReactorCore.FULL_WATER * ReactorCore.MAKEUP * 1.05 && core.need > 0, "make-up only: " + core.need);
		helper.succeed();
	}

	private static ReactorCore fuelled(ReactorCore.Design design) {
		ReactorCore core = new ReactorCore();
		core.design = design;
		for (int i = 0; i < design.size(); i++) core.fuel.add(0.0);
		core.temperature = 290;
		core.rodTarget = 0;
		return core;
	}

	private static long run(ReactorCore.Design design, long limit, double tower) {
		ReactorCore core = fuelled(design);
		ReactorCore.Plant plant = new ReactorCore.Plant(1, true, true, limit, tower);
		long electric = 0;
		for (int t = 0; t < 20 * 600; t++) {
			electric = core.tick(plant, need -> need);
		}
		return electric;
	}

	/** /nuclear build of the small modular plant, far from the other tests: six modules, each a working unit, and both tower banks formed. */
	@GameTest(maxTicks = 200)
	public void smallPlantBuildsAndForms(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		BlockPos stake = helper.absolutePos(BlockPos.ZERO).offset(4_000, 0, 4_000);
		PlantBuilder.Result built = PlantBuilder.build(level, Blueprint.get("nuclear_small_modular", false), stake, Rotation.NONE, null, true);
		helper.assertTrue(built.reactors() == 6, "six modules: " + built.reactors());
		for (BlockPos vessel : built.vessels()) {
			ReactorUnit unit = ReactorUnit.find(level, vessel);
			helper.assertTrue(unit.module && unit.formed() && unit.smallTurbines.size() == 1, "module " + vessel.toShortString() + " forms: " + unit.missing());
			var v = (EquipmentBlockEntity) level.getBlockEntity(vessel);
			helper.assertTrue(v.core.fuel.size() == 37, "fuelled: " + v.core.fuel.size());
		}
		int towers = 0;
		Blueprint blueprint = Blueprint.get("nuclear_small_modular", false);
		for (int i = 0; i < blueprint.size(); i++) {
			if (blueprint.state(i, Rotation.NONE).is(JugcraftNuclear.COOLING_TOWER)) {
				BlockPos at = stake.offset(blueprint.offset(i, Rotation.NONE));
				helper.assertTrue(level.getBlockEntity(at) instanceof CoolingTowerBlockEntity tower && tower.checkNow(level), "tower bank at " + at.toShortString() + " formed");
				towers++;
			}
		}
		helper.assertTrue(towers == 2, "two tower banks: " + towers);
		helper.assertTrue(EquipmentKind.SMALL_MODULAR_REACTOR.design() == ReactorCore.Design.SMALL_MODULAR, "module design");
		helper.succeed();
	}
}
