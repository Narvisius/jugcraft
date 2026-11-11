package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.nuclear.ConsoleActionPayload;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentPlacement;
import io.github.jimbozoomer.jugcraft.nuclear.JugcraftNuclear;
import io.github.jimbozoomer.jugcraft.nuclear.PipeKind;
import io.github.jimbozoomer.jugcraft.nuclear.ReactorCore;
import io.github.jimbozoomer.jugcraft.nuclear.ReactorUnit;
import net.minecraft.core.Direction;
import io.github.jimbozoomer.jugcraft.nuclear.Radiation;
import io.github.jimbozoomer.jugcraft.nuclear.RadiationKind;
import io.github.jimbozoomer.jugcraft.nuclear.RadiationSources;
import io.github.jimbozoomer.jugcraft.nuclear.RadiationField;
import net.minecraft.server.level.ServerLevel;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Nuclear stage 1 (docs/features/nuclear.md): radiation falls off with distance, shielding and the hazmat suit stop it, dose makes you sick. */
public class NuclearGameTests {
	private static final double STRENGTH = 1_000;

	/** A survival mock player standing at a relative position, with a source somewhere relative too. */
	private static ServerPlayer player(GameTestHelper helper, BlockPos at) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		Vec3 pos = Vec3.atBottomCenterOf(helper.absolutePos(at));
		player.setPos(pos.x, pos.y, pos.z);
		return player;
	}

	private static BlockPos source(GameTestHelper helper, BlockPos at) {
		BlockPos abs = helper.absolutePos(at);
		RadiationSources.set(helper.getLevel(), abs, STRENGTH, RadiationKind.NUCLEAR);
		return abs;
	}

	private static void done(GameTestHelper helper, BlockPos source) {
		RadiationSources.clear(helper.getLevel(), source);
		helper.succeed();
	}

	/** The rate follows strength / (1 + d^2): twice as far is much weaker. */
	@GameTest(maxTicks = 20)
	public void rateFallsOffWithDistance(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		BlockPos src = source(helper, new BlockPos(1, 2, 1));
		ServerPlayer near = player(helper, new BlockPos(1, 1, 3));
		double rNear = Radiation.rateAt(near);
		near.setPos(near.getX(), near.getY(), near.getZ() + 3);
		double rFar = Radiation.rateAt(near);
		double d1 = Vec3.atCenterOf(src).distanceToSqr(near.getEyePosition().add(0, 0, -3));
		double d2 = Vec3.atCenterOf(src).distanceToSqr(near.getEyePosition());
		// Inverse square, and a little lost to the air on the way (RadiationField.air).
		double want1 = STRENGTH / (1 + d1) * RadiationField.air(Math.sqrt(d1)), want2 = STRENGTH / (1 + d2) * RadiationField.air(Math.sqrt(d2));
		helper.assertTrue(Math.abs(rNear - want1) < want1 * 1e-6, "near rate " + rNear + " = strength / (1 + d^2) through air: " + want1);
		helper.assertTrue(Math.abs(rFar - want2) < want2 * 1e-6, "far rate " + rFar + " = strength / (1 + d^2) through air: " + want2);
		helper.assertTrue(rFar < rNear / 2, "farther is weaker: " + rFar + " vs " + rNear);
		done(helper, src);
	}

	/** A wall of shielding concrete lets 0.1% through; stone 10% (round 14, "realistic-ish"); glass 30%. */
	@GameTest(maxTicks = 20)
	public void shieldingConcreteStopsMostRadiation(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		BlockPos src = source(helper, new BlockPos(1, 2, 0));
		ServerPlayer player = player(helper, new BlockPos(1, 1, 4));
		double open = Radiation.rateAt(player);
		for (int x = -1; x <= 3; x++) {
			for (int y = 1; y <= 4; y++) {
				helper.setBlock(new BlockPos(x, y, 2), JugcraftNuclear.SHIELDING_CONCRETE.defaultBlockState());
			}
		}
		double shielded = Radiation.rateAt(player);
		helper.assertTrue(Math.abs(shielded / open - 0.001) < 0.0001, "concrete lets 0.1% through: " + shielded + " of " + open);
		for (int x = -1; x <= 3; x++) {
			for (int y = 1; y <= 4; y++) {
				helper.setBlock(new BlockPos(x, y, 2), Blocks.STONE.defaultBlockState());
			}
		}
		double stone = Radiation.rateAt(player);
		helper.assertTrue(Math.abs(stone / open - 0.1) < 0.005, "stone lets 10% through: " + stone + " of " + open);
		for (int x = -1; x <= 3; x++) {
			for (int y = 1; y <= 4; y++) {
				helper.setBlock(new BlockPos(x, y, 2), Blocks.GLASS.defaultBlockState());
			}
		}
		double glass = Radiation.rateAt(player);
		for (int x = -1; x <= 3; x++) {
			for (int y = 1; y <= 4; y++) {
				helper.setBlock(new BlockPos(x, y, 2), Blocks.AIR.defaultBlockState());
			}
		}
		helper.assertTrue(Math.abs(glass / open - 0.3) < 0.01, "glass lets 30% through: " + glass + " of " + open);
		done(helper, src);
	}

	/** Each suit piece stops 22.5%; the full suit 90%. */
	@GameTest(maxTicks = 20)
	public void hazmatSuitStopsNinetyPercent(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		BlockPos src = source(helper, new BlockPos(1, 2, 1));
		ServerPlayer player = player(helper, new BlockPos(1, 1, 3));
		double bare = Radiation.rateAt(player);
		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(JugcraftNuclear.HAZMAT_ITEMS[0]));
		double hood = Radiation.rateAt(player);
		helper.assertTrue(Math.abs(hood - bare * 0.775) < bare * 1e-6, "the hood alone stops 22.5%: " + hood + " of " + bare);
		EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
		for (int i = 0; i < 4; i++) {
			player.setItemSlot(slots[i], new ItemStack(JugcraftNuclear.HAZMAT_ITEMS[i]));
		}
		double suit = Radiation.rateAt(player);
		helper.assertTrue(Math.abs(suit - bare * 0.1) < bare * 1e-6, "the full suit stops 90%: " + suit + " of " + bare);
		done(helper, src);
	}

	/** Dose builds up while exposed; a high dose brings weakness and nausea, and hurts once spawn protection wears off. */
	@GameTest(maxTicks = 200)
	public void doseBuildsAndMakesYouSick(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		BlockPos src = source(helper, new BlockPos(1, 2, 1));
		ServerPlayer player = player(helper, new BlockPos(1, 1, 2));
		// (A test's mock player always says it is in creative, so it is updated as a survival player explicitly.)
		Radiation.update(player, false);
		double first = Radiation.state(player).dose();
		RadiationSources.clear(helper.getLevel(), src);
		helper.assertTrue(first > 0, "exposure gives a dose: " + first);
		helper.assertTrue(Radiation.state(player).rate() > 0, "the rate is synced to the player");
		Radiation.setDose(player, Radiation.SEVERE + 100);
		float health = player.getHealth();
		Radiation.update(player, false);
		helper.assertTrue(player.hasEffect(MobEffects.WEAKNESS), "weakness at a high dose");
		helper.assertTrue(player.hasEffect(MobEffects.NAUSEA), "nausea at a high dose");
		// A newly joined player cannot be hurt for a few seconds; after that the dose does damage.
		helper.onEachTick(() -> Radiation.update(player, false));
		helper.succeedWhen(() -> helper.assertTrue(player.getHealth() < health, "a severe dose hurts: " + player.getHealth() + " < " + health));
	}

	/** A radioactive item carried in the inventory counts, at arm's length. */
	@GameTest(maxTicks = 20)
	public void carriedAndDroppedSourcesCount(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		ServerPlayer player = player(helper, new BlockPos(1, 1, 1));
		helper.assertTrue(Radiation.rateAt(player) == 0, "nothing nearby, no rate");
		player.getInventory().add(new ItemStack(JugcraftNuclear.CHECK_SOURCE));
		double carried = Radiation.rateAt(player);
		player.getInventory().clearContent();     // every test sets up and clears its sources in one tick, so none sees another's
		helper.assertTrue(Math.abs(carried - 2 / 1.25) < 1e-6, "a carried check source: " + carried);
		// Shut in a shulker box it is just as radioactive (the box doesn't shield it).
		ItemStack box = new ItemStack(net.minecraft.world.item.Items.SHULKER_BOX);
		box.set(net.minecraft.core.component.DataComponents.CONTAINER,
				net.minecraft.world.item.component.ItemContainerContents.fromItems(java.util.List.of(new ItemStack(JugcraftNuclear.CHECK_SOURCE))));
		helper.assertTrue(Math.abs(Radiation.strength(box) - 2) < 1e-6, "a check source in a shulker box still counts: " + Radiation.strength(box));
		helper.succeed();
	}

	/** In creative a Geiger counter still reads the rate (Justin's round 13: it showed nothing), but no dose is taken. */
	@GameTest(maxTicks = 20)
	public void creativePlayersReadTheRateButTakeNoDose(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		BlockPos src = source(helper, new BlockPos(1, 2, 1));
		ServerPlayer player = player(helper, new BlockPos(1, 1, 2));
		player.setGameMode(GameType.CREATIVE);
		Radiation.update(player);
		double rate = Radiation.state(player).rate(), dose = Radiation.state(player).dose();
		RadiationSources.clear(helper.getLevel(), src);
		helper.assertTrue(rate > 0, "a creative player's counter reads the rate: " + rate);
		helper.assertTrue(dose == 0, "but takes no dose: " + dose);
		helper.succeed();
	}

	/** Uranium blocks are radioactive where they stand; so is a source shut in a chest (a little less). */
	@GameTest(maxTicks = 20)
	public void uraniumBlocksAndStoredSourcesCount(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		ServerPlayer player = player(helper, new BlockPos(1, 1, 1));
		helper.assertTrue(Radiation.rateAt(player) == 0, "nothing nearby, no rate");
		helper.setBlock(new BlockPos(1, 2, 3), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(io.github.jimbozoomer.jugcraft.Jugcraft.id("uranium_block")));
		double block = Radiation.rateAt(player);
		helper.setBlock(new BlockPos(1, 2, 3), Blocks.AIR);
		helper.assertTrue(block > 0.05, "a uranium block two blocks away reads: " + block);
		helper.setBlock(new BlockPos(1, 2, 3), Blocks.CHEST);
		if (!(helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(1, 2, 3))) instanceof net.minecraft.world.Container chest)) {
			throw new AssertionError("no chest");
		}
		chest.setItem(0, new ItemStack(JugcraftNuclear.CHECK_SOURCE));
		double stored = Radiation.rateAt(player);
		chest.clearContent();
		helper.setBlock(new BlockPos(1, 2, 3), Blocks.AIR);
		helper.assertTrue(stored > 0.05, "a check source in a chest reads: " + stored);
		helper.succeed();
	}

	/**
	 * Round 14: "the more radiation in one spot, the further it spreads". A thousand uranium blocks (a 10-block cube)
	 * read far further off than one: the pile shields its own inside, so it counts about as its near face, about a
	 * hundred times one block. Built well away from the other tests, and taken down again.
	 */
	@GameTest(maxTicks = 20)
	public void bigPileReachesFurther(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(3_000, 40, -3_000);
		net.minecraft.world.level.block.state.BlockState uranium = net.minecraft.core.registries.BuiltInRegistries.BLOCK
				.getValue(io.github.jimbozoomer.jugcraft.Jugcraft.id("uranium_block")).defaultBlockState();
		Vec3 centre = Vec3.atLowerCornerOf(base).add(5, 5, 5);
		Vec3 eye = centre.add(60, 0, 0);
		Vec3 far = centre.add(150, 0, 0);
		for (int x = 0; x < 10; x++) for (int y = 0; y < 10; y++) for (int z = 0; z < 10; z++) {
			level.setBlockAndUpdate(base.offset(x, y, z), uranium);
		}
		RadiationField.refreshAround(level, centre, 16);
		double pile = Radiation.rateAt(level, eye), pileFar = Radiation.rateAt(level, far);
		for (int x = 0; x < 10; x++) for (int y = 0; y < 10; y++) for (int z = 0; z < 10; z++) {
			level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
		}
		BlockPos one = base.offset(9, 5, 5);
		level.setBlockAndUpdate(one, uranium);
		RadiationField.refreshAround(level, centre, 16);
		double single = Radiation.rateAt(level, eye), singleFar = Radiation.rateAt(level, far);
		level.setBlockAndUpdate(one, Blocks.AIR.defaultBlockState());
		RadiationField.refreshAround(level, centre, 16);
		helper.assertTrue(single > 0, "one block still reads 60 blocks off: " + single);
		helper.assertTrue(pile > 40 * single && pile < 400 * single, "a thousand blocks read about a hundred times one (it shields its own middle): "
				+ pile + " vs " + single);
		helper.assertTrue(pileFar > 1e-3, "the pile reads over 1 uSv/h 150 blocks off: " + pileFar);
		helper.assertTrue(singleFar < pileFar / 40, "one block is lost in the background there: " + singleFar);
		helper.assertTrue(Radiation.rateAt(level, eye) < 1e-9, "gone when taken down: " + Radiation.rateAt(level, eye));
		helper.succeed();
	}

	// ------------------------------------------------------------------ the reactor core model (no world needed)

	private static ReactorCore fuelled() {
		ReactorCore core = new ReactorCore();
		for (int i = 0; i < ReactorCore.CORE_SIZE; i++) core.fuel.add(0.0);
		core.temperature = 290;
		return core;
	}

	/** Standard fuel with full cooling can't overheat, even with every rod out; it makes the rated power. */
	@GameTest(maxTicks = 20)
	public void standardFuelCannotOverheatWithFullCooling(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		ReactorCore core = fuelled();
		core.rodTarget = 0;
		ReactorCore.Plant plant = new ReactorCore.Plant(1, true, true);
		double hottest = 0;
		long electric = 0;
		for (int t = 0; t < 20 * 600; t++) {
			electric = core.tick(plant, need -> need);
			hottest = Math.max(hottest, core.temperature);
		}
		helper.assertTrue(core.rods < 0.01, "rods all out: " + core.rods);
		helper.assertTrue(hottest < ReactorCore.ALARM_TEMP, "never reaches the alarm: hottest " + hottest);
		helper.assertTrue(!core.scrammed, "no trip");
		helper.assertTrue(Math.abs(electric - ReactorCore.RATED_JE) < ReactorCore.RATED_JE / 50, "rated output: " + electric);
		helper.succeed();
	}

	/** Losing the cooling water heats the core until the protection system trips it at 370 °C; decay heat goes on. */
	@GameTest(maxTicks = 20)
	public void lossOfCoolingTripsTheReactor(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		ReactorCore core = fuelled();
		core.rodTarget = 0;
		ReactorCore.Plant plant = new ReactorCore.Plant(1, true, true);
		for (int t = 0; t < 20 * 300; t++) core.tick(plant, need -> need);
		int ticks = 0;
		while (!core.scrammed && ticks < 20 * 120) {
			core.tick(plant, need -> 0);
			ticks++;
		}
		helper.assertTrue(core.scrammed, "tripped after losing water");
		helper.assertTrue(core.temperature >= ReactorCore.SCRAM_TEMP && core.temperature < ReactorCore.SCRAM_TEMP + 5,
				"tripped at the SCRAM temperature: " + core.temperature);
		for (int t = 0; t < 60; t++) core.tick(plant, need -> 0);
		helper.assertTrue(core.rods > 99.9, "all rods in within three seconds: " + core.rods);
		helper.assertTrue(core.fission < 0.05 && core.decay > 0.03, "fission stops, decay heat remains: " + core.fission + " / " + core.decay);
		helper.assertTrue(!core.fuelDamaged, "no fuel damage with protection on");
		helper.succeed();
	}

	/**
	 * The control room's boards (round 27): a coolant pump stopped at full power coasts its loop's flow down and the
	 * reactor trips on low flow in one loop (above P-8), the first out recorded; the turbine trips with it.
	 */
	@GameTest(maxTicks = 20)
	public void stoppingACoolantPumpAtPowerTripsTheReactor(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		ReactorCore core = fuelled();
		core.rodTarget = 0;
		ReactorCore.Plant plant = new ReactorCore.Plant(1, true, true);
		for (int t = 0; t < 20 * 300; t++) core.tick(plant, need -> need);
		helper.assertTrue(core.fission > ReactorCore.P8 && core.electric > 0, "at power: " + core.fission);
		core.rcpStopped = 1 << 2;
		int ticks = 0;
		while (!core.scrammed && ticks < 20 * 10) {
			core.tick(plant, need -> need);
			ticks++;
		}
		helper.assertTrue(core.scrammed && core.tripCause == ReactorCore.TRIP_FLOW1, "tripped on low flow in one loop: " + core.tripCause);
		helper.assertTrue(ticks > 5 && ticks < 60, "as the flow coasts below 90%, within seconds: " + ticks);
		helper.assertTrue(core.loopFlow[2] < ReactorCore.LOW_FLOW && core.loopFlow[0] == 1, "loop 3's flow is down, loop 1's full");
		for (int t = 0; t < 20 * 30; t++) core.tick(plant, need -> need);
		helper.assertTrue(core.loopFlow[2] < 0.05, "coasted down: " + core.loopFlow[2]);
		helper.assertTrue(Math.abs(core.cooling - 0.75) < 0.02, "three loops carry the heat: " + core.cooling);
		helper.assertTrue(core.electric == 0 && core.turbineSpeed < 1, "the turbine tripped with the reactor and coasts down: " + core.turbineSpeed);
		core.rcpStopped = 0;
		for (int t = 0; t < 20 * 60; t++) core.tick(plant, need -> need);
		helper.assertTrue(core.loopFlow[2] == 1, "its pump started again, the flow is back: " + core.loopFlow[2]);
		helper.succeed();
	}

	/** Below P-8 one loop's pump can stop without a trip; the other three loops carry the heat. */
	@GameTest(maxTicks = 20)
	public void stoppingACoolantPumpAtLowPowerDoesNotTrip(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		ReactorCore core = fuelled();
		core.rodTarget = 70;
		ReactorCore.Plant plant = new ReactorCore.Plant(1, true, true);
		for (int t = 0; t < 20 * 300; t++) core.tick(plant, need -> need);
		helper.assertTrue(core.fission > ReactorCore.P7 && core.fission < ReactorCore.P8, "between P-7 and P-8: " + core.fission);
		core.rcpStopped = 1;
		for (int t = 0; t < 20 * 60; t++) core.tick(plant, need -> need);
		helper.assertTrue(!core.scrammed, "no trip below P-8 with one loop out");
		helper.assertTrue(core.electric > 0, "still making power");
		core.rcpStopped = 1 | 2;
		int ticks = 0;
		while (!core.scrammed && ticks < 20 * 10) {
			core.tick(plant, need -> need);
			ticks++;
		}
		helper.assertTrue(core.scrammed && core.tripCause == ReactorCore.TRIP_FLOW2, "two loops out above P-7 trips: " + core.tripCause);
		helper.succeed();
	}

	/**
	 * A turbine trip from its board above P-9 trips the reactor; below it the reactor runs on, its steam dumped. Latched
	 * again the turbine runs up to speed, and the generator breaker closes through the synchroscope.
	 */
	@GameTest(maxTicks = 20)
	public void turbineTripAndGeneratorBreaker(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		ReactorCore core = fuelled();
		core.rodTarget = 0;
		ReactorCore.Plant plant = new ReactorCore.Plant(1, true, true);
		for (int t = 0; t < 20 * 300; t++) core.tick(plant, need -> need);
		core.turbineTripped = true;
		core.genOpen = true;
		core.tick(plant, need -> need);
		helper.assertTrue(core.scrammed && core.tripCause == ReactorCore.TRIP_TURBINE, "above P-9 the reactor trips with the turbine: " + core.tripCause);
		helper.assertTrue(core.electric == 0, "no power");
		// Below P-9: the reactor runs on.
		ReactorCore low = fuelled();
		low.rodTarget = 70;
		for (int t = 0; t < 20 * 300; t++) low.tick(plant, need -> need);
		long before = low.electric;
		low.turbineTripped = true;
		low.genOpen = true;
		for (int t = 0; t < 20 * 60; t++) low.tick(plant, need -> need);
		helper.assertTrue(!low.scrammed && before > 0 && low.electric == 0, "below P-9 no reactor trip, no power: " + low.electric);
		helper.assertTrue(low.turbineSpeed < 0.5, "the turbine coasting down: " + low.turbineSpeed);
		low.turbineTripped = false;
		for (int t = 0; t < 20 * 40; t++) low.tick(plant, need -> need);
		helper.assertTrue(low.turbineSpeed >= 0.99 && low.electric == 0, "latched, at speed, its breaker still open: " + low.turbineSpeed);
		low.syncArmed = true;
		long t = 1;
		while (low.genOpen && t < 200) {
			io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.synchronize(low, true, t++);
			low.tick(plant, need -> need);
		}
		helper.assertTrue(!low.genOpen && !low.syncArmed && t <= 81, "closed when the synchroscope came round: " + t);
		low.tick(plant, need -> need);
		helper.assertTrue(low.electric > 0, "making power again");
		helper.succeed();
	}

	/**
	 * An annunciator window's sequence (ISA "R"): an alarm flashes until acknowledged, then stays lit; cleared, it
	 * flashes slowly until reset. One that clears before it is acknowledged goes to the slow flash when it is.
	 */
	@GameTest(maxTicks = 20)
	public void annunciatorSequence(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		byte s = io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.CLEAR;
		s = io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.next(s, true);
		helper.assertTrue(s == io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.ALERT, "comes in flashing");
		var layout = new io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.Layout(
				java.util.List.of(new io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.Window("rps_bypass", false)), java.util.List.of(), java.util.List.of());
		var state = new io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.State();
		state.windows = new byte[] {s};
		var plant = new io.github.jimbozoomer.jugcraft.nuclear.BoardPlant();
		plant.present = true;
		plant.protection = false;
		helper.assertTrue(state.unacked(), "the horn sounds");
		io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.acknowledge(state, layout, plant, 0);
		helper.assertTrue(state.windows[0] == io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.ACKED && !state.unacked(), "acknowledged: steady, quiet");
		plant.protection = true;
		state.windows[0] = io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.next(state.windows[0], plant.alarm("rps_bypass", 0));
		helper.assertTrue(state.windows[0] == io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.RINGBACK && state.cleared(), "cleared: slow flash");
		io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.reset(state);
		helper.assertTrue(state.windows[0] == io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.CLEAR, "reset: dark");
		state.windows[0] = io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.ALERT;
		state.windows[0] = io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.next(state.windows[0], false);
		helper.assertTrue(state.windows[0] == io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.ALERT, "an alarm that clears unacknowledged keeps flashing");
		io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.acknowledge(state, layout, plant, 0);
		helper.assertTrue(state.windows[0] == io.github.jimbozoomer.jugcraft.nuclear.ControlBoard.RINGBACK, "and goes to the slow flash when acknowledged");
		helper.succeed();
	}

	/**
	 * Round 28: a plant run from its console alone is as it was (main feed holds every steam generator at its level
	 * through power, a trip and a cooldown); manual safety injection trips the reactor, isolates feedwater and the
	 * containment and starts auxiliary feed, which holds the levels; it can be reset after a minute, and the reactor
	 * not before it is.
	 */
	@GameTest(maxTicks = 20)
	public void safetyInjectionFullActuation(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		ReactorCore.Plant plant = new ReactorCore.Plant(1, true, true);
		ReactorCore quiet = fuelled();
		quiet.rodTarget = 0;
		long atPower = 0;
		for (int t = 0; t < 20 * 900; t++) {
			if (t == 20 * 300) {
				atPower = quiet.electric;
				quiet.trip(ReactorCore.TRIP_MANUAL);
			}
			quiet.tick(plant, need -> need);
			for (int k = 0; k < 4; k++) {
				helper.assertTrue(Math.abs(quiet.nr(k) - 0.5) < 0.01, "main feed holds the level: " + quiet.nr(k));
			}
		}
		helper.assertTrue(atPower == ReactorCore.RATED_JE && quiet.tripCause == ReactorCore.TRIP_MANUAL && quiet.afwRunning == 0,
				"console only: rated power, the manual trip, no aux feed: " + atPower + " " + quiet.tripCause);
		ReactorCore core = fuelled();
		core.rodTarget = 0;
		for (int t = 0; t < 20 * 300; t++) core.tick(plant, need -> need);
		core.safetyInjection(1000);
		core.tick(plant, need -> need);
		helper.assertTrue(core.scrammed && core.tripCause == ReactorCore.TRIP_SI && core.feedIsolated && core.cntIsolated && core.afwRunning == 7,
				"SI: tripped on SI, feed and containment isolated, aux feed running");
		for (int t = 0; t < 20 * 300; t++) core.tick(plant, need -> need);
		for (int k = 0; k < 4; k++) {
			helper.assertTrue(core.sgFeed[k] == 0 && core.sgAux[k] > 0 && core.nr(k) > 0.4, "aux feed holds the level: " + core.nr(k));
		}
		helper.assertTrue(!core.reset(), "no reactor reset while SI is in");
		helper.assertTrue(!core.resetSafetyInjection(1000 + 600), "no SI reset before a minute");
		helper.assertTrue(core.resetSafetyInjection(1000 + ReactorCore.SI_RESET) && !core.feedIsolated && !core.cntIsolated && core.afwRunning == 7,
				"SI reset: isolations cleared, aux feed still running");
		helper.assertTrue(core.reset(), "then the reactor resets");
		helper.succeed();
	}

	/**
	 * Round 29: a shut aux feed flow control valve gives its generator no aux feed (its level falls while the others
	 * hold); each ARV opened by hand cools the plant about 30 degrees and boils its generator off a little faster.
	 */
	@GameTest(maxTicks = 20)
	public void auxFeedValvesAndArvs(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		ReactorCore.Plant plant = new ReactorCore.Plant(1, true, true);
		ReactorCore core = fuelled();
		core.rodTarget = 0;
		for (int t = 0; t < 20 * 300; t++) core.tick(plant, need -> need);
		core.safetyInjection(0);
		core.afwValveShut = 1;
		for (int t = 0; t < 20 * 120; t++) core.tick(plant, need -> need);
		helper.assertTrue(core.sgAux[0] == 0 && core.sgAux[1] > 0 && core.nr(0) < core.nr(1) - 0.02,
				"FCV 1 shut: no aux feed to SG 1, its level falls: " + core.nr(0) + " vs " + core.nr(1));
		ReactorCore hot = fuelled();
		hot.rodTarget = 0;
		for (int t = 0; t < 20 * 300; t++) hot.tick(plant, need -> need);
		double before = hot.temperature;
		hot.arvManual = 3;
		for (int t = 0; t < 20 * 60; t++) hot.tick(plant, need -> need);
		helper.assertTrue(before - hot.temperature > 50 && before - hot.temperature < 70,
				"two ARVs open by hand: about 60 degrees cooler: " + before + " -> " + hot.temperature);
		helper.assertTrue(hot.sgSteam[0] > hot.sgSteam[2], "and those generators steam more");
		hot.arvManual = 0;
		for (int t = 0; t < 20 * 60; t++) hot.tick(plant, need -> need);
		helper.assertTrue(Math.abs(hot.temperature - before) < 2, "closed again, the plant warms back: " + hot.temperature);
		helper.succeed();
	}

	/**
	 * Round 28: an MSIV shut cuts its generator's steam off the turbine (a quarter of the power); all four shut trip
	 * the turbine and, above P-9, the reactor. Feed lost at power: a generator at low-low level trips the reactor and
	 * starts the aux feed pumps.
	 */
	@GameTest(maxTicks = 20)
	public void msivsAndAuxFeed(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		ReactorCore.Plant plant = new ReactorCore.Plant(1, true, true);
		ReactorCore core = fuelled();
		core.rodTarget = 0;
		for (int t = 0; t < 20 * 300; t++) core.tick(plant, need -> need);
		long full = core.electric;
		core.msivClosed = 1;
		core.tick(plant, need -> need);
		helper.assertTrue(Math.abs(core.electric - full * 3 / 4) < full / 50 && !core.scrammed, "one MSIV shut: three quarters: " + core.electric + " of " + full);
		core.msivClosed = 15;
		core.tick(plant, need -> need);
		core.tick(plant, need -> need);
		helper.assertTrue(core.turbineTripped && core.genOpen && core.scrammed && core.tripCause == ReactorCore.TRIP_TURBINE && core.electric == 0,
				"all four shut: turbine trip, the reactor with it: " + core.tripCause);
		ReactorCore dry = fuelled();
		dry.rodTarget = 0;
		for (int t = 0; t < 20 * 300; t++) dry.tick(plant, need -> need);
		dry.feedIsolated = true;
		int t = 0;
		while (!dry.scrammed && t++ < 20 * 120) dry.tick(plant, need -> need);
		helper.assertTrue(dry.scrammed && dry.tripCause == ReactorCore.TRIP_SG_LO && dry.afwRunning == 7 && t < 20 * 30,
				"feed lost: low-low trip within half a minute, aux feed started: " + dry.tripCause + " after " + t);
		helper.succeed();
	}

	/**
	 * Round 30: the reactor control console's rod control and nuclear instruments. The shutdown banks come out by
	 * themselves in four seconds after a reset; the lever held OUT drives the control banks in their overlap (A first,
	 * B once A is at 128 steps) and stops when let go; the instruments climb through the ranges past P-6 and P-10; after a
	 * trip they drop at once to about 6% and then fall a third of a decade a minute, and the intermediate and power range
	 * blocks clear below P-10 (the source range's not until P-6); AUTO holds the power as assemblies are used up and
	 * drops to MAN on a trip; rods set from the console's screen block the instruments' trips by themselves.
	 */
	@GameTest(maxTicks = 20)
	public void rodControlAndInstruments(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		ReactorCore.Plant plant = new ReactorCore.Plant(1, true, true);
		ReactorCore core = fuelled();
		core.trip(ReactorCore.TRIP_MANUAL);
		for (int t = 0; t < 100; t++) core.tick(plant, need -> need);
		helper.assertTrue(core.sdBanks == 0 && core.bankSteps(4) == 0 && core.bankSteps(7) == 0, "tripped: every bank on the bottom");
		helper.assertTrue(!core.drive(1), "the lever does nothing while tripped");
		helper.assertTrue(core.reset(), "reset");
		for (int t = 0; t < 70; t++) core.tick(plant, need -> need);
		helper.assertTrue(core.sdBanks > 150 && core.sdBanks < ReactorCore.STEPS, "the shutdown banks coming out: " + core.sdBanks);
		for (int t = 0; t < 20; t++) core.tick(plant, need -> need);
		helper.assertTrue(core.sdBanks > ReactorCore.STEPS - 1e-6 && core.rods == 100 && core.fission == 0, "out within five seconds, the control banks still in");
		double maxSur = 0;
		for (int t = 0; t < 20 * 10; t++) {
			if (t % 4 == 0) core.drive(1);
			core.tick(plant, need -> need);
			maxSur = Math.max(maxSur, core.sur);
		}
		helper.assertTrue(core.bankSteps(4) > 100 && core.bankSteps(4) < ReactorCore.STEPS && core.bankSteps(5) == 0,
				"held OUT ten seconds: bank A coming out first: " + core.bankSteps(4));
		helper.assertTrue(core.nis >= ReactorCore.P6 && maxSur > 1, "the instruments past P-6, the startup rate high: " + core.nis + " " + maxSur);
		for (int t = 0; t < 20 * 6; t++) {
			if (t % 4 == 0) core.drive(1);
			core.tick(plant, need -> need);
		}
		helper.assertTrue(core.bankSteps(5) > 0 && core.bankSteps(6) == 0, "bank B follows in the overlap: " + core.bankSteps(4) + " " + core.bankSteps(5));
		for (int t = 0; t < 20 * 6; t++) {
			if (t % 4 == 0) core.drive(1);
			core.tick(plant, need -> need);
		}
		helper.assertTrue(core.nis >= ReactorCore.P10 && Math.abs(core.nis - core.fission) < 0.01, "past P-10, the instruments caught up: " + core.nis);
		for (int t = 0; t < 10; t++) core.tick(plant, need -> need);
		double rods = core.rods;
		core.tick(plant, need -> need);
		helper.assertTrue(core.rods == rods && core.rodsMoved == 0, "let go, the rods stop");
		// The trip: the prompt drop, then a third of a decade a minute; the blocks clear below their permissives.
		core.srBlocked = core.irBlocked = core.prLoBlocked = true;
		double before = core.nis;
		core.trip(ReactorCore.TRIP_MANUAL);
		for (int t = 0; t < 60; t++) core.tick(plant, need -> need);
		double dropped = core.nis;
		helper.assertTrue(dropped > before * 0.04 && dropped < before * 0.08, "the prompt drop to about 6%: " + before + " -> " + dropped);
		for (int t = 0; t < 1200; t++) core.tick(plant, need -> need);
		double ratio = core.nis / dropped;
		helper.assertTrue(ratio > 0.4 && ratio < 0.53 && Math.abs(core.sur + 1 / 3.0) < 0.05,
				"then a third of a decade a minute: " + ratio + ", SUR " + core.sur);
		helper.assertTrue(core.srBlocked && !core.irBlocked && !core.prLoBlocked, "below P-10 the IR and PR blocks clear, the SR block stays above P-6");
		// AUTO holds the power as a quarter of the core is used up.
		ReactorCore auto = fuelled();
		auto.rodTarget = 40;
		for (int t = 0; t < 20 * 30; t++) auto.tick(plant, need -> need);
		auto.rodControl(true);
		double sp = auto.autoSetpoint;
		for (int i = 0; i < 48; i++) auto.fuel.set(i, 1.0);
		for (int t = 0; t < 20 * 30; t++) auto.tick(plant, need -> need);
		helper.assertTrue(Math.abs(sp - 0.6) < 0.01 && Math.abs(auto.fission - sp) < 0.01 && auto.rods < 25,
				"AUTO steps the rods out to hold " + sp + ": " + auto.fission + " with the rods at " + auto.rods);
		auto.trip(ReactorCore.TRIP_MANUAL);
		helper.assertTrue(!auto.rodAuto, "a trip drops rod control to MAN");
		// Rods set from the screen: the instruments' trips block themselves as the power passes P-6 and P-10.
		ReactorCore screen = fuelled();
		screen.screenRods = true;
		screen.rodTarget = 0;
		for (int t = 0; t < 20 * 60; t++) screen.tick(plant, need -> need);
		helper.assertTrue(screen.srBlocked && screen.irBlocked && screen.prLoBlocked && screen.nis > 0.95, "a startup from the screen blocks them itself");
		helper.succeed();
	}

	/** With the protection system off and no cooling, the core reaches 1,200 °C and its fuel is damaged. */
	@GameTest(maxTicks = 20)
	public void protectionOffDamagesFuel(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		ReactorCore core = fuelled();
		core.rodTarget = 0;
		core.protection = false;
		ReactorCore.Plant none = new ReactorCore.Plant(0, false, false);
		int ticks = 0;
		while (!core.fuelDamaged && ticks < 20 * 600) {
			core.tick(none, need -> 0);
			ticks++;
		}
		helper.assertTrue(core.fuelDamaged, "fuel damaged after " + ticks + " ticks at " + core.temperature);
		helper.assertTrue(core.activeFuel() == 0, "damaged fuel makes no more power");
		helper.succeed();
	}

	// ------------------------------------------------------------------ equipment in the world

	/**
	 * The plant computer room (older rooms round 2: ServerRoom; it only looks live, 25 October 2026): its UPS (the hub)
	 * is always fed and its output on; a fire put down has its smoke detectors look, and two seeing it start the
	 * pre-discharge countdown (the hub ticking only now); the abort station held holds it; at its end the agent puts the
	 * fire out and the cylinders are empty until RESET re-arms the system; with nothing happening its hub has no tick.
	 */
	@GameTest(structure = "jugcraft-test:drone_tower", maxTicks = 1100)
	public void computerRoomCleanAgent(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos ups = helper.absolutePos(new BlockPos(2, 1, 2));
		net.minecraft.world.level.block.Block block = io.github.jimbozoomer.jugcraft.nuclear.PlantInterior.BLOCKS.get(io.github.jimbozoomer.jugcraft.nuclear.ServerRoom.UPS);
		io.github.jimbozoomer.jugcraft.nuclear.MultiFurnitureBlock multi = (io.github.jimbozoomer.jugcraft.nuclear.MultiFurnitureBlock) block;
		net.minecraft.world.level.block.state.BlockState s = block.defaultBlockState().setValue(io.github.jimbozoomer.jugcraft.nuclear.MultiFurnitureBlock.FACING, Direction.NORTH);
		level.setBlock(ups, s.setValue(multi.partProperty(), 0), 3);
		level.setBlock(ups.above(), s.setValue(multi.partProperty(), 1), 3);
		for (int x : new int[] {4, 7}) {
			level.setBlock(helper.absolutePos(new BlockPos(x, 5, 4)), io.github.jimbozoomer.jugcraft.nuclear.PlantInterior.BLOCKS.get("smoke_detector").defaultBlockState(), 3);
		}
		BlockPos fire = helper.absolutePos(new BlockPos(5, 2, 4));
		level.setBlock(fire.below(), Blocks.NETHERRACK.defaultBlockState(), 3);
		level.setBlock(fire, Blocks.FIRE.defaultBlockState(), 3);
		java.util.function.Supplier<io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity> hub = () ->
				(io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity) level.getBlockEntity(ups);
		helper.runAfterDelay(50, () -> {
			helper.assertTrue(hub.get() != null, "the UPS module has its block entity");
			helper.assertTrue((hub.get().srvBits & io.github.jimbozoomer.jugcraft.nuclear.ServerRoom.OUT_ON) != 0, "the UPS is always fed");
			helper.assertTrue(hub.get().srvPhase == io.github.jimbozoomer.jugcraft.nuclear.ServerRoom.PRE_DISCHARGE,
					"two detectors seeing the fire start the countdown (phase " + hub.get().srvPhase + ")");
			io.github.jimbozoomer.jugcraft.nuclear.ServerRoom.abort(level, ups, null);
		});
		helper.runAfterDelay(50 + 25 * 20, () -> helper.assertTrue(hub.get().srvPhase == io.github.jimbozoomer.jugcraft.nuclear.ServerRoom.PRE_DISCHARGE,
				"the abort held the countdown: no discharge yet"));
		helper.runAfterDelay(50 + 45 * 20, () -> {
			helper.assertTrue(hub.get().srvPhase == io.github.jimbozoomer.jugcraft.nuclear.ServerRoom.RELEASED, "the agent has discharged");
			helper.assertTrue(!level.getBlockState(fire).is(net.minecraft.tags.BlockTags.FIRE), "the fire is out");
			helper.assertTrue(hub.get().srvPsi == 0, "the cylinders are empty");
			io.github.jimbozoomer.jugcraft.nuclear.ServerRoom.key(hub.get(), level, 2, null);
			helper.assertTrue(hub.get().srvPhase == io.github.jimbozoomer.jugcraft.nuclear.ServerRoom.NORMAL
					&& hub.get().srvPsi == io.github.jimbozoomer.jugcraft.nuclear.ServerRoom.PSI, "RESET re-arms it with new cylinders");
		});
		helper.runAfterDelay(50 + 45 * 20 + 30, () -> {
			helper.assertTrue(!level.getBlockTicks().hasScheduledTick(ups, level.getBlockState(ups).getBlock()), "back to normal, the hub stops ticking");
			helper.succeed();
		});
	}

	/**
	 * A load centre works as real 480 V switchgear (older rooms round 1: LoadCentre): its control switch trips and
	 * closes a breaker (a closed breaker cannot be racked: the interlock; its spring must charge again after a close); a
	 * breaker racks CONNECT, TEST, DISCONNECT and back, closed in TEST carrying nothing; held over its pickup its trip
	 * unit's long delay trips it, and it will not close again until its trip unit is reset; the transformer's fan switch
	 * turns MANUAL and back; the log desk's laptop finds it and lists it (SwitchgearRoom).
	 */
	@GameTest(structure = "jugcraft-test:drone_tower", maxTicks = 40)
	public void loadCentreBreakers(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		net.minecraft.server.level.ServerLevel level = helper.getLevel();
		BlockPos anchor = helper.absolutePos(new BlockPos(6, 1, 12));
		helper.assertTrue(EquipmentPlacement.place(level, anchor, EquipmentKind.SWITCHGEAR_LINEUP, Direction.NORTH, null), "load centre placed");
		helper.assertTrue(level.getBlockEntity(anchor) instanceof EquipmentBlockEntity, "its block entity");
		EquipmentBlockEntity e = (EquipmentBlockEntity) level.getBlockEntity(anchor);
		helper.assertTrue(e.lc != null && e.bus != null, "it has a load centre's state and a bus");
		io.github.jimbozoomer.jugcraft.nuclear.LoadCentre.State s = e.lc;
		java.util.Map<String, net.minecraft.world.phys.Vec3> at = new java.util.HashMap<>();
		for (io.github.jimbozoomer.jugcraft.nuclear.LoadCentre.Control c : io.github.jimbozoomer.jugcraft.nuclear.LoadCentre.controls()) {
			// The control's middle on the lineup's front, in the world (facing north: x east, z out toward the viewer).
			at.put(c.id(), new net.minecraft.world.phys.Vec3(anchor.getX() + (c.x0() + c.x1()) / 2, anchor.getY() + (c.y0() + c.y1()) / 2,
					anchor.getZ() + 1 - 0.1));
		}
		for (String id : new String[] {"cs0", "cs3", "rack3", "reset3", "fans"}) {
			helper.assertTrue(at.containsKey(id), "the lineup's " + id + " control: " + at.keySet());
		}
		net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		java.util.function.Consumer<String> click = id -> io.github.jimbozoomer.jugcraft.nuclear.LoadCentre.use(level, e, player, at.get(id));
		int b = 3;
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.Switchgear.closed(e, b), "MCC 1A3's breaker starts closed");
		click.accept("rack3");
		helper.assertTrue(s.rack[b] == io.github.jimbozoomer.jugcraft.nuclear.LoadCentre.CONNECT, "a closed breaker cannot be racked");
		click.accept("cs3");
		helper.assertTrue(!io.github.jimbozoomer.jugcraft.nuclear.Switchgear.closed(e, b), "its control switch trips it");
		click.accept("rack3");
		helper.assertTrue(s.rack[b] == io.github.jimbozoomer.jugcraft.nuclear.LoadCentre.TEST, "open, it racks out to TEST");
		s.closedAt[b] -= 1000;
		click.accept("cs3");
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.Switchgear.closed(e, b), "it closes in TEST");
		helper.assertTrue(!io.github.jimbozoomer.jugcraft.nuclear.Switchgear.carrying(e, b), "closed in TEST it carries nothing");
		click.accept("cs3");
		click.accept("rack3");
		helper.assertTrue(s.rack[b] == io.github.jimbozoomer.jugcraft.nuclear.LoadCentre.DISCONNECT, "on out to DISCONNECT");
		helper.assertTrue(!s.controlPower(b), "with no control power there");
		click.accept("rack3");
		click.accept("rack3");
		helper.assertTrue(s.rack[b] == io.github.jimbozoomer.jugcraft.nuclear.LoadCentre.CONNECT, "and back in to CONNECT");
		click.accept("cs3");
		helper.assertTrue(!io.github.jimbozoomer.jugcraft.nuclear.Switchgear.closed(e, b), "the spring still charging, it does not close");
		s.closedAt[b] -= 1000;
		click.accept("cs3");
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.Switchgear.carrying(e, b), "spring charged, it closes and carries its load");
		// Three times its pickup: the long delay trips it in under five seconds.
		e.bus.setAmount(io.github.jimbozoomer.jugcraft.nuclear.Switchgear.CAPACITY / 2);
		e.flows[b] = (int) Math.round(3 * io.github.jimbozoomer.jugcraft.nuclear.LoadCentre.PICKUP / io.github.jimbozoomer.jugcraft.nuclear.LoadCentre.AMPS_PER_JE);
		double temp = s.temp;
		for (int k = 0; k < 100 && io.github.jimbozoomer.jugcraft.nuclear.Switchgear.closed(e, b); k++) {
			io.github.jimbozoomer.jugcraft.nuclear.LoadCentre.tick(level, e);
		}
		helper.assertTrue(!io.github.jimbozoomer.jugcraft.nuclear.Switchgear.closed(e, b) && s.target(b), "overloaded, its trip unit tripped it");
		helper.assertTrue(s.temp > temp, "the transformer warmed under the load: " + temp + " to " + s.temp);
		java.util.Map<String, String> screen = new java.util.HashMap<>();
		io.github.jimbozoomer.jugcraft.nuclear.SwitchgearRoom.screen(io.github.jimbozoomer.jugcraft.nuclear.SwitchgearRoom.nearest(level, anchor.offset(2, 0, 4)), screen);
		helper.assertTrue("LC 1A  TRIP UNITS".equals(screen.get("sgr_t")) && "04 MCC 1A3".equals(screen.get("sgr_n3"))
				&& "TRIPPED".equals(screen.get("sgr_y3")), "the laptop shows it tripped: " + screen);
		e.flows[b] = 0;
		s.closedAt[b] -= 1000;
		click.accept("cs3");
		helper.assertTrue(!io.github.jimbozoomer.jugcraft.nuclear.Switchgear.closed(e, b), "it will not close with its target up");
		click.accept("reset3");
		helper.assertTrue(!s.target(b), "RESET clears the target");
		click.accept("cs3");
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.Switchgear.closed(e, b), "and then it closes");
		click.accept("fans");
		helper.assertTrue(s.fanManual && s.fans, "the fan switch to MANUAL runs the fans");
		click.accept("fans");
		helper.assertTrue(!s.fanManual, "and back to AUTO");
		helper.succeed();
	}

	/** Equipment placed from its item fills its body with frame blocks, and breaking one part takes it all down. */
	@GameTest(structure = "jugcraft-test:drone_tower", maxTicks = 40)
	public void equipmentPlacesAndBreaksWhole(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		BlockPos anchor = helper.absolutePos(new BlockPos(20, 1, 20));
		helper.assertTrue(EquipmentPlacement.place(helper.getLevel(), anchor, EquipmentKind.COOLANT_PUMP, Direction.NORTH, null), "pump placed");
		int frames = 0;
		for (BlockPos offset : EquipmentKind.COOLANT_PUMP.frames(Direction.NORTH)) {
			if (helper.getLevel().getBlockState(anchor.offset(offset)).is(JugcraftNuclear.FRAME)) frames++;
		}
		helper.assertTrue(frames == EquipmentKind.COOLANT_PUMP.frames(Direction.NORTH).size(), "every frame placed: " + frames);
		// Every part of it is the pump by name (Jade names what you look at by its picked item), not "Nuclear Equipment".
		for (BlockPos offset : EquipmentKind.COOLANT_PUMP.frames(Direction.NORTH)) {
			net.minecraft.world.item.ItemStack picked = helper.getLevel().getBlockState(anchor.offset(offset))
					.getCloneItemStack(helper.getLevel(), anchor.offset(offset), false);
			helper.assertTrue(picked.is(JugcraftNuclear.EQUIPMENT_ITEMS.get(EquipmentKind.COOLANT_PUMP)),
					"a frame block is picked as the pump: " + picked);
		}
		helper.assertTrue(!EquipmentPlacement.place(helper.getLevel(), anchor.offset(1, 0, 0), EquipmentKind.FEEDWATER_PUMP, Direction.NORTH, null),
				"no room on top of the pump");
		BlockPos frame = anchor.offset(EquipmentKind.COOLANT_PUMP.frames(Direction.NORTH).get(5));
		helper.getLevel().destroyBlock(frame, false);
		helper.assertTrue(helper.getLevel().getBlockState(anchor).isAir(), "the anchor went with it");
		helper.assertTrue(helper.getLevel().getBlockState(anchor.offset(EquipmentKind.COOLANT_PUMP.frames(Direction.NORTH).get(0))).isAir(),
				"and the rest of the frame");
		helper.succeed();
	}

	/** A pressurizer touching the vessel and a steam generator on a coolant pipe join the unit; the rest is listed missing. */
	@GameTest(structure = "jugcraft-test:drone_tower", maxTicks = 40)
	public void unitJoinsThroughCoolantPipe(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		BlockPos vessel = helper.absolutePos(new BlockPos(10, 1, 20));
		helper.assertTrue(EquipmentPlacement.place(level, vessel, EquipmentKind.REACTOR_VESSEL, Direction.NORTH, null), "vessel placed");
		// North-facing: x to the east, z to the north. The steam generator stands 4 blocks east of the vessel.
		BlockPos sg = vessel.offset(11, 0, 0);
		helper.assertTrue(EquipmentPlacement.place(level, sg, EquipmentKind.STEAM_GENERATOR, Direction.NORTH, null), "steam generator placed");
		// A coolant pipe from the vessel's east side (local x 7, z 3) to the steam generator's lower shell (local x 1, z 3).
		for (int x = 7; x <= 11; x++) {
			BlockPos p = vessel.offset(x, 1, -3);
			level.setBlock(p, JugcraftNuclear.PIPES.get(PipeKind.COOLANT).defaultBlockState(), 3);
		}
		BlockPos pressurizer = vessel.offset(5, 0, 1);
		helper.assertTrue(EquipmentPlacement.place(level, pressurizer, EquipmentKind.PRESSURIZER, Direction.SOUTH, null), "pressurizer placed");
		ReactorUnit unit = ReactorUnit.find(level, vessel);
		helper.assertTrue(unit.steamGenerators.contains(sg), "the steam generator joins through the pipe: " + unit.steamGenerators);
		helper.assertTrue(unit.pressurizers.contains(pressurizer), "the pressurizer touching the vessel joins: " + unit.pressurizers);
		helper.assertTrue(unit.loops() == 0 && unit.missing().contains("coolant_pump") && unit.missing().contains("turbine"),
				"no pump yet, no turbine: " + unit.missing());
		helper.assertTrue(!unit.formed(), "not formed");
		helper.succeed();
	}

	/** The console's buttons: only the owner (or their party in Party mode) may use them; refuelling needs a shut-down core. */
	@GameTest(structure = "jugcraft-test:drone_tower", maxTicks = 40)
	public void consoleChecksPermissionAndShutdown(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		ServerPlayer owner = player(helper, new BlockPos(20, 1, 20));
		BlockPos consolePos = helper.absolutePos(new BlockPos(20, 1, 22));
		BlockPos vesselPos = helper.absolutePos(new BlockPos(20, 1, 25));
		EquipmentPlacement.place(level, consolePos, EquipmentKind.CONTROL_CONSOLE, Direction.NORTH, owner);
		EquipmentPlacement.place(level, vesselPos, EquipmentKind.REACTOR_VESSEL, Direction.SOUTH, owner);
		EquipmentBlockEntity console = (EquipmentBlockEntity) level.getBlockEntity(consolePos);
		EquipmentBlockEntity vessel = (EquipmentBlockEntity) level.getBlockEntity(vesselPos);
		console.linked = vesselPos;
		console.setOwner(java.util.UUID.randomUUID());
		helper.assertTrue(!ConsoleActionPayload.apply(console, owner, ConsoleActionPayload.SET_RODS, 30), "someone else's console won't move the rods");
		console.setOwner(owner.getUUID());
		helper.assertTrue(ConsoleActionPayload.apply(console, owner, ConsoleActionPayload.SET_RODS, 30) && vessel.core.rodTarget == 30,
				"the owner can");
		helper.assertTrue(!ConsoleActionPayload.apply(console, owner, ConsoleActionPayload.REFUEL, 0) && !vessel.core.refuelOpen,
				"no refuelling with the rods set out");
		ConsoleActionPayload.apply(console, owner, ConsoleActionPayload.SET_RODS, 100);
		helper.assertTrue(ConsoleActionPayload.apply(console, owner, ConsoleActionPayload.REFUEL, 0) && vessel.core.refuelOpen,
				"a cold, shut-down core opens for refuelling");
		helper.assertTrue(vessel.loadAssemblies(500) == ReactorCore.CORE_SIZE, "a core holds " + ReactorCore.CORE_SIZE + " assemblies");
		ConsoleActionPayload.apply(console, owner, ConsoleActionPayload.SCRAM, 0);
		helper.assertTrue(vessel.core.scrammed, "SCRAM");
		vessel.core.refuelOpen = false;
		vessel.core.fuel.clear();
		helper.succeed();
	}

	/**
	 * The remote shutdown panel (round 24) finds the nearest reactor itself; its transfer switches take the controls
	 * from the console (which can then only trip the reactor) until it hands them back; its trip drives the rods in; it
	 * only works a reactor its user may run; and it shows the reactor's state.
	 */
	@GameTest(structure = "jugcraft-test:drone_tower", maxTicks = 40)
	public void remoteShutdownPanelTripsAndTakesControl(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		ServerPlayer owner = player(helper, new BlockPos(12, 1, 20));
		BlockPos consolePos = helper.absolutePos(new BlockPos(20, 1, 22));
		BlockPos vesselPos = helper.absolutePos(new BlockPos(20, 1, 25));
		BlockPos panelPos = helper.absolutePos(new BlockPos(8, 1, 22));
		EquipmentPlacement.place(level, consolePos, EquipmentKind.CONTROL_CONSOLE, Direction.NORTH, owner);
		EquipmentPlacement.place(level, vesselPos, EquipmentKind.REACTOR_VESSEL, Direction.SOUTH, owner);
		helper.assertTrue(EquipmentPlacement.place(level, panelPos, EquipmentKind.REMOTE_SHUTDOWN_PANEL, Direction.NORTH, owner), "panel placed");
		EquipmentBlockEntity console = (EquipmentBlockEntity) level.getBlockEntity(consolePos);
		EquipmentBlockEntity vessel = (EquipmentBlockEntity) level.getBlockEntity(vesselPos);
		EquipmentBlockEntity panel = (EquipmentBlockEntity) level.getBlockEntity(panelPos);
		console.linked = vesselPos;
		console.setOwner(owner.getUUID());
		vessel.setOwner(owner.getUUID());
		panel.setOwner(owner.getUUID());
		panel.linkShutdownPanel(level);
		helper.assertTrue(vesselPos.equals(panel.linked), "the panel finds the reactor by itself: " + panel.linked);
		helper.assertTrue(panel.panelOnline, "and reads it");
		helper.assertTrue(ConsoleActionPayload.apply(panel, owner, ConsoleActionPayload.TRANSFER, 1) && vessel.core.remote,
				"its transfer switches take control");
		helper.assertTrue(panel.core.remote, "it shows control is here");
		helper.assertTrue(!ConsoleActionPayload.apply(console, owner, ConsoleActionPayload.SET_RODS, 30) && vessel.core.rodTarget == 100,
				"the console can't move the rods while the panel has control");
		helper.assertTrue(ConsoleActionPayload.apply(panel, owner, ConsoleActionPayload.SCRAM, 0) && vessel.core.scrammed,
				"the panel trips the reactor");
		vessel.core.scrammed = false;
		helper.assertTrue(ConsoleActionPayload.apply(console, owner, ConsoleActionPayload.SCRAM, 0) && vessel.core.scrammed,
				"the console can still trip it");
		helper.assertTrue(ConsoleActionPayload.apply(panel, owner, ConsoleActionPayload.TRANSFER, 0) && !vessel.core.remote,
				"control handed back");
		vessel.core.scrammed = false;
		helper.assertTrue(ConsoleActionPayload.apply(console, owner, ConsoleActionPayload.SET_RODS, 30) && vessel.core.rodTarget == 30,
				"the console has the rods again");
		vessel.setOwner(java.util.UUID.randomUUID());
		helper.assertTrue(!ConsoleActionPayload.apply(panel, owner, ConsoleActionPayload.SCRAM, 0) && !vessel.core.scrammed,
				"it won't trip a reactor that belongs to someone else");
		helper.succeed();
	}

	// ------------------------------------------------------------------ diesels make real power (27 October 2026)

	/** A core at full power for five minutes. */
	private static ReactorCore atPower(ReactorCore.Plant plant) {
		ReactorCore core = fuelled();
		core.rodTarget = 0;
		for (int t = 0; t < 20 * 300; t++) core.tick(plant, need -> need);
		return core;
	}

	/**
	 * Decay heat falls as a real core's does (quickly, then for hours): 6.2% a minute after a trip from full power, 4.6%
	 * after ten, 2.8% after three quarters of an hour. Left to itself the plant cools to the RHR's 175 C in about 45
	 * minutes and the RHR pumps take it below 120 (refuelling); with four ARVs opened it gets there within five.
	 */
	@GameTest(maxTicks = 20)
	public void decayHeatFallsAsARealCoresDoes(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		ReactorCore.Plant plant = new ReactorCore.Plant(1, true, true);
		ReactorCore core = atPower(plant);
		core.trip(ReactorCore.TRIP_MANUAL);
		double[] at = new double[3];
		long cold = -1;
		for (int t = 1; t <= 20 * 60 * 50; t++) {
			core.tick(plant, need -> need);
			if (t == 20 * 60) at[0] = core.decay;
			if (t == 20 * 600) at[1] = core.decay;
			if (t == 20 * 2700) at[2] = core.decay;
			if (cold < 0 && core.temperature < 120) cold = t;
		}
		helper.assertTrue(Math.abs(at[0] - 0.062) < 0.003 && Math.abs(at[1] - 0.046) < 0.003 && Math.abs(at[2] - 0.028) < 0.003,
				"decay heat 1, 10 and 45 minutes on: " + at[0] + ", " + at[1] + ", " + at[2]);
		helper.assertTrue(cold > 20 * 60 * 40 && cold < 20 * 60 * 50, "left to itself, under 120 C in about 45 minutes: " + cold / 1200.0 + " min");
		ReactorCore vented = atPower(plant);
		vented.trip(ReactorCore.TRIP_MANUAL);
		vented.arvManual = 15;
		long ventedCold = -1;
		for (int t = 1; t <= 20 * 60 * 10 && ventedCold < 0; t++) {
			vented.tick(plant, need -> need);
			if (vented.temperature < 120) ventedCold = t;
		}
		helper.assertTrue(ventedCold > 0 && ventedCold < 20 * 60 * 5, "four ARVs open: under 120 C within five minutes: " + ventedCold / 1200.0 + " min");
		helper.succeed();
	}

	/**
	 * Losing offsite power at full power with both diesels picking up their buses: the turbine trips (the reactor with
	 * it), the coolant pumps coast down and natural circulation (about 5% flow) carries the decay heat; with no condenser
	 * the steam goes out the relief valves (no condenser water is drawn) and the aux feed pumps (the motor-driven ones
	 * once the sequencer starts them) hold the generators' levels; the core never reaches its trip temperature.
	 */
	@GameTest(maxTicks = 20)
	public void lossOfOffsitePowerCoolsByNaturalCirculation(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		ReactorCore.Plant plant = new ReactorCore.Plant(1, true, true);
		ReactorCore core = atPower(plant);
		core.power = ReactorCore.PW_1A | ReactorCore.PW_1B;
		core.rcpStopped = 15;
		core.turbineTripped = true;
		core.genOpen = true;
		core.afwRunning |= 7;
		core.afwPower = 0;
		int[] drawn = {0};
		double hottest = 0, lowest = 1;
		for (int t = 0; t < 20 * 60 * 10; t++) {
			if (t == 20 * 25) core.afwPower = 3;
			core.tick(plant, need -> {
				drawn[0] += need;
				return need;
			});
			hottest = Math.max(hottest, core.temperature);
			for (int k = 0; k < 4; k++) lowest = Math.min(lowest, core.sgWater[k]);
		}
		helper.assertTrue(core.scrammed && core.tripCause == ReactorCore.TRIP_TURBINE, "the turbine's trip trips the reactor: " + core.tripCause);
		helper.assertTrue(Math.abs(core.loopFlow[0] - ReactorCore.NAT_CIRC) < 0.005, "natural circulation round the loops: " + core.loopFlow[0]);
		helper.assertTrue(hottest < ReactorCore.SCRAM_TEMP - 30, "it carries the decay heat: hottest " + hottest);
		helper.assertTrue(drawn[0] == 0, "no condenser: no cooling water drawn (" + drawn[0] + " mB)");
		helper.assertTrue(lowest > 0.9 && core.sgAux[0] > 0, "aux feed holds the generators: lowest " + lowest);
		helper.succeed();
	}

	/**
	 * A station blackout (no diesels): the turbine-driven aux feed pump, on steam and its DC control power, keeps the
	 * generators fed for the hour; with it lost too, they boil dry in about three quarters of an hour and the core heats
	 * on its decay heat until its fuel is damaged (in about an hour and a half).
	 */
	@GameTest(maxTicks = 20)
	public void stationBlackoutDamagesTheFuelWithNoFeed(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		ReactorCore.Plant plant = new ReactorCore.Plant(1, true, true);
		ReactorCore[] cores = {atPower(plant), atPower(plant)};
		long[] dry = {-1, -1}, damaged = {-1, -1};
		for (int c = 0; c < 2; c++) {
			ReactorCore core = cores[c];
			core.power = 0;
			core.rcpStopped = 15;
			core.turbineTripped = true;
			core.genOpen = true;
			core.afwRunning |= 7;
			core.afwPower = 0;
			core.rhrPower = 0;
			core.tdafwDc = c == 0;
			for (int t = 0; t < 20 * 60 * 120 && damaged[c] < 0; t++) {
				core.tick(plant, need -> need);
				if (dry[c] < 0 && core.sgWater[0] < ReactorCore.DRY) dry[c] = t;
				if (core.fuelDamaged) damaged[c] = t;
				if (c == 0 && t == 20 * 60 * 60) break;
			}
		}
		helper.assertTrue(dry[0] < 0 && damaged[0] < 0 && cores[0].sgWater[0] > 0.5, "the turbine-driven pump keeps them fed for the hour: SG 1 at "
				+ cores[0].sgWater[0]);
		helper.assertTrue(dry[1] > 20 * 60 * 35 && dry[1] < 20 * 60 * 55, "with no feed they boil dry in about 45 minutes: " + dry[1] / 1200.0);
		helper.assertTrue(damaged[1] > 20 * 60 * 75 && damaged[1] < 20 * 60 * 115, "then the fuel is damaged, about an hour and a half in: "
				+ damaged[1] / 1200.0);
		helper.succeed();
	}
}
