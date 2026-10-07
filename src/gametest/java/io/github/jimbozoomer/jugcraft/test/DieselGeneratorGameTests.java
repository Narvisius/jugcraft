package io.github.jimbozoomer.jugcraft.test;

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
 * (so they walk like stairs), and using it starts and stops it.
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
		// The steps up to each walkway (left x 0, right x 6), from the floor: half (z 11), whole (z 10), whole and a half
		// (z 9), the deck.
		int[][] expect = {{11, 0, 1}, {10, 0, 2}, {9, 0, 2}, {9, 1, 1}, {8, 1, 2}, {8, 0, 2}};
		for (int x : new int[] {0, 6}) {
			for (int[] e : expect) {
				BlockPos at = anchor.offset(EquipmentKind.toWorld(new BlockPos(x, e[1], e[0]), facing));
				BlockState state = helper.getLevel().getBlockState(at);
				helper.assertTrue(state.is(JugcraftNuclear.FRAME), "a frame at step x " + x + " z " + e[0] + " y " + e[1]);
				helper.assertTrue(state.getValue(EquipmentFrameBlock.SHAPE) == (e[2] == 1 ? EquipmentFrameBlock.Shape.HALF : EquipmentFrameBlock.Shape.FULL),
						"step x " + x + " z " + e[0] + " y " + e[1] + " is " + state.getValue(EquipmentFrameBlock.SHAPE));
			}
			// Above each walkway and step is clear to stand in; the floor at the foot of each flight is clear.
			for (int[] c : new int[][] {{2, 5}, {2, 9}, {1, 10}, {1, 11}, {0, 12}}) {
				helper.assertTrue(helper.getLevel().getBlockState(anchor.offset(EquipmentKind.toWorld(new BlockPos(x, c[0], c[1]), facing))).isAir(),
						"clear above x " + x + " y " + c[0] + " z " + c[1]);
			}
		}
		// Used with no diesel near: it won't start. With a day tank of diesel: used, it starts; used again, it stops.
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		EquipmentBlockEntity generator = (EquipmentBlockEntity) helper.getLevel().getBlockEntity(anchor);
		EquipmentPlacement.use(helper.getLevel(), anchor, player, ItemStack.EMPTY, InteractionHand.MAIN_HAND);
		helper.assertTrue(!generator.running && generator.fuelOut, "no fuel: it doesn't start");
		// (East of the generator, which stands east and north of its anchor: inside the test's space.)
		var tank = dayTank(helper.getLevel(), anchor.offset(9, -1, -6));
		fill(tank, 50 * net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET);
		EquipmentPlacement.use(helper.getLevel(), anchor, player, ItemStack.EMPTY, InteractionHand.MAIN_HAND);
		helper.assertTrue(generator.running && !generator.fuelOut, "started");
		EquipmentPlacement.use(helper.getLevel(), anchor, player, ItemStack.EMPTY, InteractionHand.MAIN_HAND);
		helper.assertTrue(!generator.running, "stopped");
		helper.succeed();
	}

	/**
	 * A small storage tank (3 x 3 x 3 inside: 108 buckets) with its outlet on the north side, its floor at base + 1;
	 * formed. The generator's day tank in these tests.
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
		level.setBlock(outlet, io.github.jimbozoomer.jugcraft.nuclear.StorageTanks.OUTLET.defaultBlockState()
				.setValue(io.github.jimbozoomer.jugcraft.nuclear.StorageTankPortBlock.FACING, Direction.NORTH), 3);
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
}
