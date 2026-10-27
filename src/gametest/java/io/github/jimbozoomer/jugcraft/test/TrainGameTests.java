package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.train.CargoWagon;
import io.github.jimbozoomer.jugcraft.train.JugcraftTrains;
import io.github.jimbozoomer.jugcraft.train.Locomotive;
import io.github.jimbozoomer.jugcraft.train.PassengerCar;
import io.github.jimbozoomer.jugcraft.train.TrackPoint;
import io.github.jimbozoomer.jugcraft.train.TrainCar;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * The train framework (docs/features/trains.md), on a line of rails along z at x 3 in an empty 48-block test arena: a place on the
 * line follows a curve and ends where the line does; a locomotive pulls its coupled wagon the same distance; a train
 * stops at the end of the line and short of another train; a car couples only just behind the train; breaking the rail under a car knocks it off
 * with its goods; the driver's keys work the throttle and brake; and a train's places and goods survive a save.
 */
public class TrainGameTests {
	private static final Vec3 SOUTH = new Vec3(0, 0, 1);

	/** A straight line of rails on stone along z, from {@code from} to {@code to} inclusive. */
	private static void line(GameTestHelper helper, int from, int to) {
		for (int z = from; z <= to; z++) {
			helper.setBlock(new BlockPos(3, 0, z), Blocks.STONE);
			helper.setBlock(new BlockPos(3, 1, z), Blocks.RAIL);
		}
	}

	/** The middle of the rail at z, facing south. */
	private static TrackPoint at(GameTestHelper helper, int z) {
		TrackPoint point = TrackPoint.onRail(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, z)), SOUTH);
		if (point == null) {
			throw helper.assertionException("No rail at z " + z);
		}
		return point;
	}

	private static <T extends TrainCar> T put(GameTestHelper helper, T car, int z) {
		car.place(at(helper, z));
		helper.getLevel().addFreshEntity(car);
		return car;
	}

	private static double z(GameTestHelper helper, double relative) {
		return helper.absolutePos(BlockPos.ZERO).getZ() + relative;
	}

	@GameTest(structure = "jugcraft-test:tatterlace_arena")
	public void aPlaceOnTheLineFollowsCurvesAndEndsWithTheLine(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		line(helper, 1, 11);
		for (int x = 4; x <= 7; x++) {
			helper.setBlock(new BlockPos(x, 0, 11), Blocks.STONE);
			helper.setBlock(new BlockPos(x, 1, 11), Blocks.RAIL);
		}
		RailShape corner = TrackPoint.shape(level.getBlockState(helper.absolutePos(new BlockPos(3, 1, 11))));
		helper.assertTrue(corner == RailShape.NORTH_EAST, "The corner rail should curve north to east, but is " + corner);
		BlockPos origin = helper.absolutePos(BlockPos.ZERO);
		TrackPoint start = at(helper, 5);

		TrackPoint.Move four = start.move(level, 4);
		helper.assertTrue(!four.blocked() && Math.abs(four.moved() - 4) < 1.0E-6, "Four blocks along a straight line: " + four);
		helper.assertTrue(Math.abs(four.point().position(level).z - z(helper, 9.5)) < 1.0E-6, "It should be four blocks south");

		// 5.5 blocks to the corner, its diagonal, then east.
		TrackPoint.Move round = start.move(level, 7.5);
		Vec3 there = round.point().position(level);
		double east = 7.5 - 5.5 - Math.sqrt(0.5);
		helper.assertTrue(!round.blocked() && Math.abs(there.z - z(helper, 11.5)) < 1.0E-6
				&& Math.abs(there.x - (origin.getX() + 4.0 + east)) < 1.0E-6, "Round the corner it should be heading east, at " + there);

		TrackPoint.Move far = start.move(level, 100);
		helper.assertTrue(far.blocked() && Math.abs(far.point().position(level).x - (origin.getX() + 8.0)) < 1.0E-6,
				"It should stop at the east end of the line, at " + far.point().position(level));
		helper.assertTrue(Math.abs(far.moved() - (5.5 + Math.sqrt(0.5) + 4.0)) < 1.0E-6, "It went " + far.moved());

		TrackPoint.Move back = start.move(level, -10);
		helper.assertTrue(back.blocked() && Math.abs(back.moved() + 4.5) < 1.0E-6
				&& Math.abs(back.point().position(level).z - z(helper, 1.0)) < 1.0E-6, "Backwards it should stop at the north end: " + back);
		helper.succeed();
	}

	@GameTest(structure = "jugcraft-test:tatterlace_arena", maxTicks = 200)
	public void aLocomotivePullsItsCoupledWagon(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		line(helper, 1, 46);
		Locomotive engine = put(helper, new Locomotive(JugcraftTrains.LOCOMOTIVE, level), 9);
		CargoWagon wagon = put(helper, new CargoWagon(JugcraftTrains.CARGO_WAGON, level), 5);
		helper.assertTrue(engine.couple(level, wagon), "A wagon just behind the locomotive should couple");
		double gap = Locomotive.LENGTH / 2 + TrainCar.COUPLING_GAP + CargoWagon.LENGTH / 2;
		engine.setFuel(1000);
		engine.setNotch(2);
		double start = engine.getZ();
		helper.runAfterDelay(60, () -> {
			helper.assertTrue(engine.getZ() - start > 3, "At notch 2 it should have run south, but ran " + (engine.getZ() - start));
			helper.assertTrue(Math.abs(engine.getZ() - wagon.getZ() - gap) < 0.01,
					"The wagon should follow " + gap + " blocks behind, not " + (engine.getZ() - wagon.getZ()));
			helper.assertTrue(engine.fuel() < 1000, "Working should burn fuel");
			helper.assertTrue(engine.cars().equals(List.of(wagon.getUUID())) && engine.getUUID().equals(wagon.coupledTo()),
					"The wagon should stay coupled");
			helper.succeed();
		});
	}

	@GameTest(structure = "jugcraft-test:tatterlace_arena", maxTicks = 200)
	public void aTrainStopsAtTheEndOfTheLine(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		line(helper, 1, 14);
		Locomotive engine = put(helper, new Locomotive(JugcraftTrains.LOCOMOTIVE, level), 6);
		engine.setFuel(1000);
		engine.setNotch(Locomotive.NOTCHES);
		helper.succeedWhen(() -> {
			double front = engine.ahead(Locomotive.LENGTH / 2).point().position(level).z;
			helper.assertTrue(Math.abs(front - z(helper, 15.0)) < 1.0E-3, "Its front should stop at the end of the line, not " + front);
			helper.assertTrue(engine.speed() == 0 && engine.notch() == 0, "It should stand with the throttle at idle");
		});
	}

	@GameTest(structure = "jugcraft-test:tatterlace_arena", maxTicks = 300)
	public void aTrainStopsShortOfAnotherTrain(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		line(helper, 1, 46);
		Locomotive engine = put(helper, new Locomotive(JugcraftTrains.LOCOMOTIVE, level), 6);
		CargoWagon standing = put(helper, new CargoWagon(JugcraftTrains.CARGO_WAGON, level), 30);
		engine.setFuel(1000);
		engine.setNotch(Locomotive.NOTCHES);
		double end = standing.getZ() - CargoWagon.LENGTH / 2;
		helper.succeedWhen(() -> {
			double front = engine.ahead(Locomotive.LENGTH / 2).point().position(level).z;
			helper.assertTrue(engine.speed() == 0 && front > end - 0.5, "It should run up to the standing wagon; its front is at " + front);
			helper.assertTrue(front <= end + 1.0E-6, "It should not run into the wagon: its front is at " + front + ", the wagon's end at " + end);
			helper.assertTrue(Math.abs(standing.getZ() - z(helper, 30.5)) < 1.0E-6, "The standing wagon should not move");
		});
	}

	@GameTest(structure = "jugcraft-test:tatterlace_arena")
	public void aCarCouplesOnlyJustBehindTheTrain(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		line(helper, 1, 40);
		Locomotive engine = put(helper, new Locomotive(JugcraftTrains.LOCOMOTIVE, level), 30);
		PassengerCar far = put(helper, new PassengerCar(JugcraftTrains.PASSENGER_CAR, level), 10);
		helper.assertFalse(engine.couple(level, far), "A coach far down the line should not couple");
		PassengerCar near = put(helper, new PassengerCar(JugcraftTrains.PASSENGER_CAR, level), 26);
		helper.assertTrue(engine.couple(level, near), "A coach just behind should couple");
		helper.assertTrue(engine.getUUID().equals(near.coupledTo()), "The coach should know its locomotive");
		double gap = Locomotive.LENGTH / 2 + TrainCar.COUPLING_GAP + PassengerCar.LENGTH / 2;
		helper.assertTrue(Math.abs(engine.getZ() - near.getZ() - gap) < 1.0E-6, "It should be set at the coupling distance");
		helper.assertFalse(engine.couple(level, far), "The far coach is still not just behind the train");
		helper.assertFalse(engine.couple(level, near), "A coupled coach should not couple twice");
		engine.uncouple(near);
		helper.assertTrue(engine.cars().isEmpty() && near.coupledTo() == null, "Uncoupling should leave the coach on its own");
		helper.succeed();
	}

	@GameTest(structure = "jugcraft-test:tatterlace_arena", maxTicks = 100)
	public void breakingTheRailKnocksTheCarOffWithItsGoods(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		line(helper, 1, 10);
		CargoWagon wagon = put(helper, new CargoWagon(JugcraftTrains.CARGO_WAGON, level), 5);
		wagon.cargo().setItem(0, new ItemStack(Items.DIAMOND, 3));
		helper.runAfterDelay(5, () -> helper.setBlock(new BlockPos(3, 1, 5), Blocks.AIR));
		helper.succeedWhen(() -> {
			helper.assertTrue(wagon.isRemoved(), "The wagon should come off the line");
			helper.assertItemEntityPresent(JugcraftTrains.CARGO_WAGON_ITEM, new BlockPos(3, 1, 5), 2.0);
			helper.assertItemEntityPresent(Items.DIAMOND, new BlockPos(3, 1, 5), 2.0);
		});
	}

	@GameTest(structure = "jugcraft-test:tatterlace_arena", maxTicks = 200)
	public void theDriverWorksTheThrottleAndBrake(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		line(helper, 1, 46);
		Locomotive engine = put(helper, new Locomotive(JugcraftTrains.LOCOMOTIVE, level), 6);
		engine.setFuel(2000);
		ServerPlayer driver = helper.makeMockServerPlayerInLevel();
		driver.setPos(engine.getX(), engine.getY(), engine.getZ());
		helper.assertTrue(driver.startRiding(engine, true, true), "The driver could not climb into the cab");
		double start = engine.getZ();
		int[] ticks = {0};
		helper.onEachTick(() -> {
			ticks[0]++;
			engine.steer(driver, ticks[0] <= 40 ? 1 : 0, ticks[0] > 60);
		});
		helper.runAfterDelay(45, () -> helper.assertTrue(engine.notch() == Locomotive.NOTCHES,
				"Holding forwards should notch the throttle up to " + Locomotive.NOTCHES + ", not " + engine.notch()));
		helper.runAfterDelay(110, () -> {
			helper.assertTrue(engine.getZ() - start > 2, "It should have run south, but ran " + (engine.getZ() - start));
			helper.assertTrue(engine.speed() == 0 && engine.notch() == 0, "The brake should bring it to a stand at idle");
			helper.succeed();
		});
	}

	@GameTest(structure = "jugcraft-test:tatterlace_arena")
	public void aTrainKeepsItsPlacesAndGoodsThroughASave(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		line(helper, 1, 20);
		Locomotive engine = put(helper, new Locomotive(JugcraftTrains.LOCOMOTIVE, level), 12);
		CargoWagon wagon = put(helper, new CargoWagon(JugcraftTrains.CARGO_WAGON, level), 8);
		helper.assertTrue(engine.couple(level, wagon), "The wagon should couple");
		engine.setFuel(3000);
		wagon.cargo().setItem(4, new ItemStack(Items.IRON_INGOT, 7));

		TagValueOutput engineSave = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		engine.saveWithoutId(engineSave);
		Locomotive engineCopy = new Locomotive(JugcraftTrains.LOCOMOTIVE, level);
		engineCopy.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), engineSave.buildResult()));
		helper.assertTrue(engine.track().equals(engineCopy.track()), "The locomotive's place on the line should be saved");
		helper.assertTrue(engineCopy.cars().equals(engine.cars()) && engineCopy.fuel() == 3000, "Its cars and fuel should be saved");

		TagValueOutput wagonSave = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		wagon.saveWithoutId(wagonSave);
		CargoWagon wagonCopy = new CargoWagon(JugcraftTrains.CARGO_WAGON, level);
		wagonCopy.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), wagonSave.buildResult()));
		helper.assertTrue(wagon.track().equals(wagonCopy.track()) && engine.getUUID().equals(wagonCopy.coupledTo()),
				"The wagon's place and coupling should be saved");
		ItemStack goods = wagonCopy.cargo().getItem(4);
		helper.assertTrue(goods.is(Items.IRON_INGOT) && goods.getCount() == 7, "Its goods should be saved, not " + goods);
		helper.succeed();
	}
}
