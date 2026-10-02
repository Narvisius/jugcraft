package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.drone.BuildJobs;
import io.github.jimbozoomer.jugcraft.drone.CargoPackagerBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.ControlScreenBlock;
import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DepotView;
import io.github.jimbozoomer.jugcraft.drone.DockLayout;
import io.github.jimbozoomer.jugcraft.drone.FlightPath;
import io.github.jimbozoomer.jugcraft.drone.SupplyPickupBlock;
import io.github.jimbozoomer.jugcraft.drone.DroneFleet;
import io.github.jimbozoomer.jugcraft.drone.DroneSize;
import io.github.jimbozoomer.jugcraft.drone.DroneTier;
import io.github.jimbozoomer.jugcraft.drone.FlightScheduler;
import io.github.jimbozoomer.jugcraft.drone.JugcraftDrones;
import io.github.jimbozoomer.jugcraft.drone.LandingPadBlock;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.PlatformLayout;
import io.github.jimbozoomer.jugcraft.drone.SimpleBuildJobs;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import io.github.jimbozoomer.jugcraft.party.PartyManager;
import io.github.jimbozoomer.jugcraft.party.UseMode;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * Drone Depot rules (docs/features/drone-depot.md): platform shape and capacity up to 100 drones,
 * pooled power, and the flight scheduler (synthetic platforms), plus two in-world tests that build a
 * real depot (a 9x9 base with a 5x5 of pad plates on top, which form one pad with a charger port) and
 * check that its drones fill blocks from the cargo packager, that pads unform and re-form, and that Party mode
 * serves party members' jobs but not outsiders'.
 */
public class DroneGameTests {
	/** A w x d platform with padsX x padsZ pads starting at (padX0, padZ0). */
	private static PlatformLayout.CellLookup grid(int w, int d, int padX0, int padZ0, int padsX, int padsZ) {
		return (x, z) -> {
			if (x < 0 || z < 0 || x >= w || z >= d) {
				return PlatformLayout.Cell.NONE;
			}
			boolean pad = x >= padX0 && z >= padZ0 && x < padX0 + padsX * 5 && z < padZ0 + padsZ * 5;
			return pad ? PlatformLayout.Cell.PAD : PlatformLayout.Cell.PLATFORM;
		};
	}

	/** The base platform is 9x9 with a centred 5x5 pad; smaller or pad-less platforms are refused. */
	@GameTest
	public void basePlatformIsNineByNine(GameTestHelper helper) {
		PlatformLayout base = PlatformLayout.scan(4, 4, grid(9, 9, 2, 2, 1, 1));
		helper.assertTrue(base.isValid() && base.padCount() == 1 && base.slots() == 4, "9x9 base: " + base.status() + ", " + base.padCount() + " pads");
		helper.assertTrue(PlatformLayout.scan(3, 3, grid(8, 9, 1, 2, 1, 1)).status() == PlatformLayout.Status.TOO_SMALL, "8x9 is too small");
		helper.assertTrue(PlatformLayout.scan(4, 4, grid(9, 9, 0, 0, 0, 0)).status() == PlatformLayout.Status.NO_PAD, "a platform needs a pad");
		helper.succeed();
	}

	/** A w x d base with a padsX x padsZ grid of pads, one block apart, starting at (2, 2). */
	private static PlatformLayout.CellLookup spacedGrid(int w, int d, int padsX, int padsZ) {
		return (x, z) -> {
			if (x < 0 || z < 0 || x >= w || z >= d) {
				return PlatformLayout.Cell.NONE;
			}
			int px = x - 2, pz = z - 2;
			boolean pad = px >= 0 && pz >= 0 && px < padsX * 6 && pz < padsZ * 6 && px % 6 < 5 && pz % 6 < 5;
			return pad ? PlatformLayout.Cell.PAD : PlatformLayout.Cell.PLATFORM;
		};
	}

	/** Expanded to 33x33 with 25 pads one block apart, a depot holds 100 small drones; the 101st is refused. */
	@GameTest
	public void expandedPlatformHoldsOneHundredDrones(GameTestHelper helper) {
		PlatformLayout big = PlatformLayout.scan(0, 0, spacedGrid(33, 33, 5, 5));
		helper.assertTrue(big.padCount() == 25 && big.slots() == 100, "33x33 platform: " + big.padCount() + " pads");
		DroneFleet fleet = new DroneFleet();
		for (int i = 0; i < PlatformLayout.MAX_DRONES; i++) {
			helper.assertTrue(fleet.link(DroneTier.COURIER_QUAD, big.slots()) == DroneFleet.LinkResult.OK, "drone " + i + " links");
		}
		helper.assertTrue(fleet.link(DroneTier.COURIER_QUAD, 1000) == DroneFleet.LinkResult.FLEET_FULL, "101st drone refused");
		helper.assertTrue(PlatformLayout.padsNeeded(100, DroneSize.LARGE) == 100, "100 large drones need 100 pads");
		helper.succeed();
	}

	/**
	 * Pads must be separated by at least one bare base block: two 5x5s touching form nothing, a 5x5 with
	 * one extra plate touching it forms nothing, and a separate 3x3 of pickup plates is a supply pickup.
	 */
	@GameTest
	public void padsNeedAGapAndPickupsAreFound(GameTestHelper helper) {
		PlatformLayout touching = PlatformLayout.scan(0, 0, grid(16, 9, 2, 2, 2, 1));
		helper.assertTrue(touching.padCount() == 0 && touching.status() == PlatformLayout.Status.NO_PAD,
				"touching pads: " + touching.padCount() + " formed");
		PlatformLayout spaced = PlatformLayout.scan(0, 0, spacedGrid(17, 9, 2, 1));
		helper.assertTrue(spaced.padCount() == 2, "pads one block apart: " + spaced.padCount());
		PlatformLayout.CellLookup extraPlate = (x, z) -> x == 7 && z == 4 ? PlatformLayout.Cell.PAD : grid(9, 9, 2, 2, 1, 1).at(x, z);
		helper.assertTrue(PlatformLayout.scan(0, 0, extraPlate).padCount() == 0, "a 5x5 with an extra plate touching is not a pad");
		PlatformLayout.CellLookup withPickup = (x, z) -> {
			if (x < 0 || z < 0 || x >= 9 || z >= 13) {
				return PlatformLayout.Cell.NONE;
			}
			if (x >= 2 && x <= 6 && z >= 2 && z <= 6) {
				return PlatformLayout.Cell.PAD;
			}
			return x >= 3 && x <= 5 && z >= 8 && z <= 10 ? PlatformLayout.Cell.PICKUP : PlatformLayout.Cell.PLATFORM;
		};
		PlatformLayout depot = PlatformLayout.scan(0, 0, withPickup);
		helper.assertTrue(depot.isValid() && depot.padCount() == 1 && depot.pickups().size() == 1
				&& depot.pickups().getFirst()[0] == 3 && depot.pickups().getFirst()[1] == 8, "9x13 depot with pad and pickup: " + depot.status());
		helper.succeed();
	}

	/** Docks: four small drones share a pad's quarters; larger drones are placed first and never overlap. */
	@GameTest
	public void docksFillPadsWithoutOverlap(GameTestHelper helper) {
		List<DroneTier> fleet = List.of(DroneTier.COURIER_QUAD, DroneTier.DUCTED_FAN_RUNNER, DroneTier.COURIER_QUAD, DroneTier.COURIER_QUAD);
		List<DockLayout.Dock> docks = DockLayout.assign(fleet, 2);
		java.util.Set<String> used = new java.util.HashSet<>();
		for (DockLayout.Dock dock : docks) {
			helper.assertTrue(used.add(dock.pad() + ":" + dock.dx() + ":" + dock.dz()), "two drones on one spot: " + docks);
		}
		helper.assertTrue(docks.get(1).pad() == 0 && docks.get(1).dx() == 0, "the medium drone takes a half of pad 0: " + docks.get(1));
		helper.succeed();
	}

	/** Flight paths: legs climb over the cruise height, and the timing matches the scheduler's. */
	@GameTest
	public void flightPathMatchesScheduler(GameTestHelper helper) {
		double[][] points = {{0, 0, 0}, {10, 0, 0}, {10, 2, 20}, {0, 0, 0}};
		FlightPath path = new FlightPath(points, new double[] {5, 8, 8}, 4);
		helper.assertTrue(Math.abs(path.legLength(0) - (10 + 5 + 5)) < 1e-9, "leg 0 length " + path.legLength(0));
		FlightScheduler<Integer> scheduler = new FlightScheduler<>();
		scheduler.startTick();
		FlightScheduler.Flight<Integer> flight = scheduler.launch(0, 4, path.legLengths(), List.of(new FlightScheduler.Stop<>(1)));
		helper.assertTrue(flight != null && flight.total() == path.totalTicks() * FlightScheduler.FULL_RATE,
				"scheduler total " + (flight == null ? -1 : flight.total()) + " vs path " + path.totalTicks());
		FlightPath.Pose top = path.poseAt(path.pickupArrive() - FlightScheduler.legTicks(path.legLength(0), 4) / 2.0);
		helper.assertTrue(Math.abs(top.y() - 5) < 1e-6, "cruising at 5 mid-leg, was " + top.y());
		helper.assertTrue(path.poseAt(path.pickupLeave() + 1).cable() == FlightPath.CARRY_CABLE, "carrying a crate after the pickup");
		helper.assertTrue(path.poseAt(path.totalTicks()).cable() < 0, "no crate after the last drop");
		DepotView.FlightView view = new DepotView.FlightView(0, 1, DepotView.decode(DepotView.encode(path), 4), 0, flight.total(), 1000);
		helper.assertTrue(view.path().totalTicks() == path.totalTicks(), "path survives the trip to the client");
		helper.succeed();
	}

	/** Pooled power is two cached numbers, updated when drones are linked. */
	@GameTest
	public void pooledPowerIsCached(GameTestHelper helper) {
		DroneFleet fleet = new DroneFleet();
		fleet.link(DroneTier.COURIER_QUAD, 8);
		fleet.link(DroneTier.DUCTED_FAN_RUNNER, 8);
		helper.assertTrue(fleet.workingDraw() == 8 + 32, "working draw " + fleet.workingDraw());
		helper.assertTrue(fleet.standbyDraw() == DroneFleet.STANDBY_BASE + 2, "standby draw " + fleet.standbyDraw());
		helper.assertTrue(fleet.link(DroneTier.COURIER_QUAD, 3) == DroneFleet.LinkResult.NO_PAD_ROOM, "pad room is checked");
		helper.succeed();
	}

	/** 100 flights at once: launches are spread out, each stop is delivered once, then the drone lands. */
	@GameTest
	public void schedulerFliesOneHundredDrones(GameTestHelper helper) {
		FlightScheduler<Integer> scheduler = new FlightScheduler<>();
		int[] counts = new int[2];
		FlightScheduler.Listener<Integer> listener = new FlightScheduler.Listener<>() {
			@Override
			public void onDeliver(FlightScheduler.Flight<Integer> flight, int index) {
				counts[0]++;
			}

			@Override
			public void onLanded(FlightScheduler.Flight<Integer> flight) {
				counts[1]++;
			}
		};
		int launched = 0;
		int ticks = 0;
		while (launched < 100 && ticks < 1000) {
			scheduler.startTick();
			for (int drone = launched; drone < 100 && scheduler.canLaunch(); drone++) {
				if (scheduler.launch(drone, 4, new double[] {10, 20, 25}, List.of(new FlightScheduler.Stop<>(drone))) != null) {
					launched++;
				}
			}
			scheduler.tick(FlightScheduler.FULL_RATE, listener);
			ticks++;
		}
		helper.assertTrue(ticks == 100 / FlightScheduler.MAX_LAUNCHES_PER_TICK, "launches spread over " + ticks + " ticks");
		helper.assertTrue(scheduler.activeFlights() == 100, "100 flying at once");
		while (scheduler.activeFlights() > 0 && ticks < 10_000) {
			scheduler.tick(FlightScheduler.FULL_RATE, listener);
			ticks++;
		}
		helper.assertTrue(counts[0] == 100 && counts[1] == 100, "delivered " + counts[0] + ", landed " + counts[1]);
		helper.succeed();
	}

	/** Without power flights wait in place; they never drop their cargo. */
	@GameTest
	public void noPowerMeansWaitNotDrop(GameTestHelper helper) {
		FlightScheduler<Integer> scheduler = new FlightScheduler<>();
		scheduler.startTick();
		scheduler.launch(0, 4, new double[] {4, 4, 4}, List.of(new FlightScheduler.Stop<>(1)));
		FlightScheduler.Listener<Integer> fail = new FlightScheduler.Listener<>() {
			@Override
			public void onDeliver(FlightScheduler.Flight<Integer> flight, int index) {
				throw helper.assertionException("delivered without power");
			}

			@Override
			public void onLanded(FlightScheduler.Flight<Integer> flight) {
				throw helper.assertionException("landed without power");
			}
		};
		for (int i = 0; i < 200; i++) {
			scheduler.tick(0, fail);
		}
		helper.assertTrue(scheduler.activeFlights() == 1, "flight still waiting");
		helper.succeed();
	}

	/** Every drone tier has an item (5-9 are creative-only prototypes), and the depot blocks exist. */
	@GameTest
	public void dronesAreRegistered(GameTestHelper helper) {
		for (DroneTier tier : DroneTier.values()) {
			helper.assertTrue(JugcraftDrones.DRONES.containsKey(tier), "item registered for " + tier);
		}
		helper.assertTrue(JugcraftDrones.TERMINAL != null && JugcraftDrones.CARGO_PACKAGER != null, "depot blocks registered");
		helper.succeed();
	}

	// ------------------------------------------------------------------ in-world depot

	/** A 16x16 open-sky test area (the default is an 8x8 barrier box, too small for a depot). */
	private static final String ARENA = "jugcraft-test:drone_depot";
	/** The platform's north-west corner in the arena. */
	private static final int O = 3;
	// Depot: base x 0-8, z 0-12 (arena 3-11, 3-15).

	private static BlockPos at(int x, int y, int z) {
		return new BlockPos(O + x, y, O + z);
	}

	/** The cargo packager stands on the base right beside the supply pickup. */
	private static final BlockPos PACKAGER = new BlockPos(O + 6, 2, O + 9);

	/**
	 * Builds a real depot the way a player does: a 9x13 base layer of landing platform at y = 1, a 5x5 of
	 * landing pad plates on top (z 2-6), a 3x3 of supply pickup plates one block further on (z 8-10), a
	 * cargo packager beside the pickup holding {@code stone} stone, and a powered terminal beside the
	 * platform, owned by {@code owner}. Checks that the terminal formed the pad (charger port in the
	 * middle) and the pickup (lift hatch in the middle).
	 */
	private static DroneTerminalBlockEntity buildDepot(GameTestHelper helper, UUID owner, int stone, DroneTier tier, int drones) {
		for (int x = 0; x < 9; x++) {
			for (int z = 0; z < 13; z++) {
				helper.setBlock(at(x, 1, z), JugcraftDrones.LANDING_PLATFORM);
				if (x >= 2 && x <= 6 && z >= 2 && z <= 6) {
					helper.setBlock(at(x, 2, z), JugcraftDrones.LANDING_PAD);
				}
				if (x >= 3 && x <= 5 && z >= 8 && z <= 10) {
					helper.setBlock(at(x, 2, z), JugcraftDrones.SUPPLY_PICKUP);
				}
			}
		}
		BlockPos packagerPos = PACKAGER;
		helper.setBlock(packagerPos, JugcraftDrones.CARGO_PACKAGER);
		if (stone > 0) {
			helper.getBlockEntity(packagerPos, CargoPackagerBlockEntity.class).setItem(0, new ItemStack(Items.STONE, stone));
		}
		BlockPos terminalPos = at(-2, 2, 4);
		helper.setBlock(terminalPos, JugcraftDrones.TERMINAL);
		DroneTerminalBlockEntity terminal = helper.getBlockEntity(terminalPos, DroneTerminalBlockEntity.class);
		terminal.setOwner(owner);
		terminal.energy().setAmount(DroneTerminalBlockEntity.ENERGY_CAPACITY);
		terminal.rescanNow();
		helper.assertTrue(terminal.layout().isValid() && terminal.layout().padCount() == 1, "depot platform: " + terminal.layout().status());
		assertPart(helper, at(2, 2, 2), 1);
		assertPart(helper, at(4, 2, 4), LandingPadBlock.CHARGER_PART);
		assertPart(helper, at(6, 2, 6), 25);
		helper.assertBlockProperty(at(4, 2, 9), SupplyPickupBlock.PART, SupplyPickupBlock.HATCH_PART);
		helper.assertTrue(terminal.pickupCorner() != null, "supply pickup found");
		for (int i = 0; i < drones; i++) {
			helper.assertTrue(terminal.fleet().link(tier, terminal.layout().slots()) == DroneFleet.LinkResult.OK, "drone " + i + " links");
		}
		return terminal;
	}

	private static void assertPart(GameTestHelper helper, BlockPos pos, int part) {
		helper.assertBlockProperty(pos, LandingPadBlock.PART, part);
	}

	private static SimpleBuildJobs job(GameTestHelper helper, UUID owner, UseMode mode, List<BlockPos> relative) {
		SimpleBuildJobs jobs = new SimpleBuildJobs(owner, mode);
		for (BlockPos pos : relative) {
			jobs.want(helper.absolutePos(pos), Blocks.STONE.defaultBlockState());
		}
		BuildJobs.register(jobs);
		return jobs;
	}

	/** Four tier-1 drones fly six stone blocks from the packager onto the platform rim, then all land. */
	@GameTest(structure = ARENA, maxTicks = 2000, skyAccess = true)
	public void depotBuildsBlocksInWorld(GameTestHelper helper) {
		UUID owner = UUID.randomUUID();
		DroneTerminalBlockEntity terminal = buildDepot(helper, owner, 16, DroneTier.COURIER_QUAD, 4);
		List<BlockPos> wall = List.of(at(8, 2, 9), at(8, 2, 10), at(8, 2, 11), at(7, 2, 11), at(8, 3, 10), at(8, 3, 11));
		SimpleBuildJobs jobs = job(helper, owner, UseMode.PERSONAL, wall);
		CargoPackagerBlockEntity packager = helper.getBlockEntity(PACKAGER, CargoPackagerBlockEntity.class);
		int[] maxFlying = new int[1];
		helper.onEachTick(() -> maxFlying[0] = Math.max(maxFlying[0], terminal.scheduler().activeFlights()));
		helper.succeedWhen(() -> {
			for (BlockPos pos : wall) {
				helper.assertBlockPresent(Blocks.STONE, pos);
			}
			helper.assertTrue(terminal.scheduler().activeFlights() == 0, "drones still flying or charging");
			helper.assertTrue(packager.count(stack -> stack.is(Items.STONE)) == 10, "packager should have 10 stone left, has "
					+ packager.count(stack -> stack.is(Items.STONE)));
			helper.assertTrue(maxFlying[0] >= 2, "several drones should fly at once, max was " + maxFlying[0]);
			BuildJobs.unregister(jobs);
		});
	}

	/**
	 * Breaking one plate of a formed pad turns the other 24 back into loose plates and the depot loses its
	 * pad; putting the plate back re-forms the pad with its charger port on the next platform scan.
	 */
	@GameTest(structure = ARENA, maxTicks = 200, skyAccess = true)
	public void padUnformsAndReforms(GameTestHelper helper) {
		DroneTerminalBlockEntity terminal = buildDepot(helper, UUID.randomUUID(), 0, DroneTier.COURIER_QUAD, 0);
		helper.destroyBlock(at(5, 2, 5));
		assertPart(helper, at(4, 2, 4), 0);
		assertPart(helper, at(2, 2, 2), 0);
		terminal.rescanNow();
		helper.assertTrue(terminal.layout().status() == PlatformLayout.Status.NO_PAD, "no pad while a plate is missing: " + terminal.layout().status());
		helper.setBlock(at(5, 2, 5), JugcraftDrones.LANDING_PAD);
		helper.succeedWhen(() -> {
			assertPart(helper, at(4, 2, 4), LandingPadBlock.CHARGER_PART);
			assertPart(helper, at(5, 2, 5), LandingPadBlock.part(3, 3));
			helper.assertTrue(terminal.layout().padCount() == 1, "pad formed again");
		});
	}

	/**
	 * A Party-mode depot works for its party: a member's Party job is built, while a Personal job of the
	 * same member and a job of someone outside the party are left alone. The depot owner switches to
	 * Party mode as a player would (sneak-use on the terminal).
	 */
	@GameTest(structure = ARENA, maxTicks = 2000, skyAccess = true)
	public void partyDepotServesOnlyItsParty(GameTestHelper helper) {
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		UUID member = UUID.randomUUID(), outsider = UUID.randomUUID();
		PartyManager parties = JugcraftParties.manager();
		long now = System.currentTimeMillis();
		helper.assertTrue(parties.invite(member, owner.getUUID(), now) == PartyManager.Result.OK, "member invites the depot owner");
		helper.assertTrue(parties.accept(owner.getUUID(), now) == PartyManager.Result.OK, "depot owner joins");
		DroneTerminalBlockEntity terminal = buildDepot(helper, owner.getUUID(), 16, DroneTier.SURVEY_HEXACOPTER, 4);
		terminal.toggleMode(owner);
		helper.assertTrue(terminal.mode() == UseMode.PARTY, "terminal switched to Party");

		List<BlockPos> party = List.of(at(8, 2, 11), at(7, 2, 11));
		List<BlockPos> personal = List.of(at(8, 2, 9));
		List<BlockPos> foreign = List.of(at(7, 2, 12));
		SimpleBuildJobs partyJob = job(helper, member, UseMode.PARTY, party);
		SimpleBuildJobs personalJob = job(helper, member, UseMode.PERSONAL, personal);
		SimpleBuildJobs foreignJob = job(helper, outsider, UseMode.PARTY, foreign);
		helper.succeedWhen(() -> {
			for (BlockPos pos : party) {
				helper.assertBlockPresent(Blocks.STONE, pos);
			}
			helper.assertTrue(terminal.scheduler().activeFlights() == 0, "drones still flying or charging");
			helper.assertBlockPresent(Blocks.AIR, personal.get(0));
			helper.assertBlockPresent(Blocks.AIR, foreign.get(0));
			BuildJobs.unregister(partyJob);
			BuildJobs.unregister(personalJob);
			BuildJobs.unregister(foreignJob);
			parties.leave(owner.getUUID());
			parties.leave(member);
		});
	}

	/**
	 * Six control screen panels in a 3x2 wall form one screen (tile 1 top left as seen from the front),
	 * whose anchor links to the terminal nearby; a seventh panel touching them unforms all of them.
	 */
	@GameTest(structure = ARENA, maxTicks = 100, skyAccess = true)
	public void controlScreenFormsAndLinks(GameTestHelper helper) {
		BlockPos terminalPos = new BlockPos(8, 1, 8);
		helper.setBlock(terminalPos, JugcraftDrones.TERMINAL);
		// Screens on the wall at z = 4, facing south (towards +z), 3 wide (x 6-8) and 2 tall (y 1-2).
		for (int x = 6; x <= 8; x++) {
			for (int y = 1; y <= 2; y++) {
				helper.setBlock(new BlockPos(x, y, 4), JugcraftDrones.CONTROL_SCREEN.defaultBlockState()
						.setValue(ControlScreenBlock.FACING, net.minecraft.core.Direction.SOUTH));
			}
		}
		// Facing south, the viewer stands south looking north: their left is west (x 6), so tile 1 is (6, 2).
		helper.assertBlockProperty(new BlockPos(6, 2, 4), ControlScreenBlock.PART, 1);
		helper.assertBlockProperty(new BlockPos(8, 1, 4), ControlScreenBlock.PART, 6);
		DepotDisplayBlockEntity anchor = helper.getBlockEntity(new BlockPos(6, 2, 4), DepotDisplayBlockEntity.class);
		helper.succeedWhen(() -> {
			helper.assertTrue(helper.absolutePos(terminalPos).equals(anchor.terminal()), "anchor linked to " + anchor.terminal());
			helper.setBlock(new BlockPos(9, 1, 4), JugcraftDrones.CONTROL_SCREEN.defaultBlockState()
					.setValue(ControlScreenBlock.FACING, net.minecraft.core.Direction.SOUTH));
			helper.assertBlockProperty(new BlockPos(6, 2, 4), ControlScreenBlock.PART, 0);
		});
	}

}
