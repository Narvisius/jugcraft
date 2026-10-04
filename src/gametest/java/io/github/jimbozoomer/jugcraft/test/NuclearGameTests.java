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
		helper.assertTrue(Math.abs(rNear - STRENGTH / (1 + d1)) < 1e-6, "near rate " + rNear + " = strength / (1 + d^2)");
		helper.assertTrue(Math.abs(rFar - STRENGTH / (1 + d2)) < 1e-6, "far rate " + rFar + " = strength / (1 + d^2)");
		helper.assertTrue(rFar < rNear / 2, "farther is weaker: " + rFar + " vs " + rNear);
		done(helper, src);
	}

	/** A wall of shielding concrete stops 95%; stone only 40%. */
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
		helper.assertTrue(Math.abs(shielded - open * 0.05) < open * 1e-6, "concrete lets 5% through: " + shielded + " of " + open);
		for (int x = -1; x <= 3; x++) {
			for (int y = 1; y <= 4; y++) {
				helper.setBlock(new BlockPos(x, y, 2), Blocks.STONE.defaultBlockState());
			}
		}
		double stone = Radiation.rateAt(player);
		helper.assertTrue(Math.abs(stone - open * 0.6) < open * 1e-6, "stone lets 60% through: " + stone + " of " + open);
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
		Radiation.update(player);
		double first = Radiation.state(player).dose();
		RadiationSources.clear(helper.getLevel(), src);
		helper.assertTrue(first > 0, "exposure gives a dose: " + first);
		helper.assertTrue(Radiation.state(player).rate() > 0, "the rate is synced to the player");
		Radiation.setDose(player, Radiation.SEVERE + 100);
		float health = player.getHealth();
		Radiation.update(player);
		helper.assertTrue(player.hasEffect(MobEffects.WEAKNESS), "weakness at a high dose");
		helper.assertTrue(player.hasEffect(MobEffects.NAUSEA), "nausea at a high dose");
		// A newly joined player cannot be hurt for a few seconds; after that the dose does damage.
		helper.onEachTick(() -> Radiation.update(player));
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
		helper.assertTrue(Math.abs(carried - 400 / 1.25) < 1e-6, "a carried check source: " + carried);
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
}
