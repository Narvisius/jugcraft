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
}
