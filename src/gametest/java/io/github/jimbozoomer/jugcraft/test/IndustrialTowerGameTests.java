package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.drone.ControlScreenBlock;
import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DepotView;
import io.github.jimbozoomer.jugcraft.drone.DroneSize;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DroneTier;
import io.github.jimbozoomer.jugcraft.drone.FlightPath;
import io.github.jimbozoomer.jugcraft.drone.FlightScheduler;
import io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock;
import io.github.jimbozoomer.jugcraft.tower.ExchangeStationBlock;
import io.github.jimbozoomer.jugcraft.tower.ExchangeStations;
import io.github.jimbozoomer.jugcraft.tower.HallFixtureBlock;
import io.github.jimbozoomer.jugcraft.tower.JugcraftTower;
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.TowerData;
import io.github.jimbozoomer.jugcraft.tower.TowerPanelBlock;
import io.github.jimbozoomer.jugcraft.tower.TowerUpgradePayload;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

/**
 * The Tri-Pillar's control building (its wall screens, desks and hall fixtures pinned to the tower's own terminal out in
 * the yard), save compatibility, night lighting, dock allocation and the control readouts. The class keeps its name from
 * the industrial design these tests were first written for.
 */
public class IndustrialTowerGameTests {
	/**
	 * A free spot in the open ground east of the building, 15 blocks from the terminal (inside a desk's 24-block link), and
	 * one in the hall's west 51 from it (the old hall spot is the upper floor's glass since the rooms).
	 */
	private static final BlockPos YARD_SPOT = new BlockPos(51, 1, 4), HALL_SPOT = new BlockPos(-20, 1, -5);
	/** A free spot inside the arena, outside the foundation, for a second core. */
	private static final BlockPos OTHER_CORE = new BlockPos(66, 1, 40);
	/** The radius of the hall floor round the central table that must be well lit (the rotunda is round since layout 20). */
	private static final int HALL = 8;

	/**
	 * The control building's screens are pinned to its own tower's terminal, beyond their own link range: the hall's three
	 * Curved Video Walls (one of each board; they took the six 3x2 wall screens' place with the rotunda, layout 20) and the
	 * security office's 3x2 control screen, formed.
	 */
	@GameTest(structure = TriPillarArena.ARENA, skyAccess = true, maxTicks = 100)
	public void controlBuildingScreensArePinnedToItsOwnTerminal(GameTestHelper helper) {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		var level = helper.getLevel();
		var core = TriPillarArena.build(helper, helper.makeMockPlayer(GameType.CREATIVE), 1);
		BlockPos terminal = core.terminalPos();
		int screens = 0, anchors = 0;
		java.util.Map<Integer, Integer> partCounts = new java.util.HashMap<>();
		java.util.Set<io.github.jimbozoomer.jugcraft.tower.CurvedVideoWallBlock.Board> boards = java.util.EnumSet.noneOf(
				io.github.jimbozoomer.jugcraft.tower.CurvedVideoWallBlock.Board.class);
		for (int[] b : core.layout().foundation.place) {
			BlockPos pos = core.getBlockPos().offset(b[0], b[1], b[2]); var state = level.getBlockState(pos);
			if (state.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.CurvedVideoWallBlock) {
				boards.add(state.getValue(io.github.jimbozoomer.jugcraft.tower.CurvedVideoWallBlock.BOARD));
				helper.assertTrue(level.getBlockEntity(pos) instanceof DepotDisplayBlockEntity display && terminal.equals(display.terminal()),
						"the video wall at " + pos + " is pinned to this exact tower terminal");
				continue;
			}
			if (!(state.getBlock() instanceof ControlScreenBlock)) continue;
			screens++;
			int part = state.getValue(ControlScreenBlock.PART);
			helper.assertTrue(part >= 1 && part <= 6, "every screen panel is formed, not standby: " + pos);
			partCounts.merge(part, 1, Integer::sum);
			if (part == 1) {
				anchors++;
				helper.assertTrue(level.getBlockEntity(pos) instanceof DepotDisplayBlockEntity display && terminal.equals(display.terminal())
						&& display.reaches(terminal, ControlScreenBlock.LINK_RANGE), "the screen's anchor is pinned to this exact tower terminal");
				helper.assertFalse(pos.closerThan(terminal, ControlScreenBlock.LINK_RANGE),
						"the screen is beyond a screen's own link range: only the core's pin reaches the terminal");
			}
		}
		helper.assertTrue(boards.size() == 3, "three video walls, one of each board: " + boards);
		helper.assertTrue(screens == 6 && anchors == 1, "one formed 3x2 control screen: " + screens + " panels, " + anchors + " anchors");
		for (int part = 1; part <= 6; part++) helper.assertTrue(partCounts.getOrDefault(part, 0) == 1, "the screen has its part " + part);
		helper.succeed();
	}

	@GameTest(structure = "jugcraft-test:drone_tower", skyAccess = true, maxTicks = 100)
	public void unownedTerminalIsClaimedByItsFirstDeskOperator(GameTestHelper helper) {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		var level = helper.getLevel();
		BlockPos target = helper.absolutePos(new BlockPos(12, 2, 12)), desk = target.east(5);
		level.setBlock(target, io.github.jimbozoomer.jugcraft.drone.JugcraftDrones.TERMINAL.defaultBlockState(), 3);
		level.setBlock(desk, io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("console_desk").defaultBlockState(), 3);
		((io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity) level.getBlockEntity(desk)).pin(target);
		var terminal = (DroneTerminalBlockEntity) level.getBlockEntity(target);
		helper.assertTrue(terminal.owner() == null, "imported terminal starts unowned");
		var first = helper.makeMockServerPlayerInLevel(); first.setPos(desk.getX() + .5, desk.getY(), desk.getZ() + 1.5);
		var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(desk), net.minecraft.core.Direction.SOUTH, desk, false);
		level.getBlockState(desk).useWithoutItem(level, first, hit);
		helper.assertTrue(first.getUUID().equals(terminal.owner()), "normal desk use claims the unowned terminal for its first operator");
		var mode = terminal.mode();
		io.github.jimbozoomer.jugcraft.drone.TerminalModePayload.handle(first,
				new io.github.jimbozoomer.jugcraft.drone.TerminalModePayload(target, desk));
		helper.assertTrue(terminal.mode() != mode, "first desk operator controls the claimed terminal");
		mode = terminal.mode();
		var second = helper.makeMockServerPlayerInLevel(); second.setPos(first.position());
		level.getBlockState(desk).useWithoutItem(level, second, hit);
		io.github.jimbozoomer.jugcraft.drone.TerminalModePayload.handle(second,
				new io.github.jimbozoomer.jugcraft.drone.TerminalModePayload(target, desk));
		helper.assertTrue(first.getUUID().equals(terminal.owner()) && terminal.mode() == mode,
				"second desk user cannot reclaim or change the owner's mode");
		helper.succeed();
	}

	/** A hall desk, 30-odd blocks from the yard's terminal, controls it through the core's pin; a stale or unpinned desk does not. */
	@GameTest(structure = TriPillarArena.ARENA, skyAccess = true, maxTicks = 100)
	public void deskModeControlsUseTheClickedDeskAndRejectStaleLinks(GameTestHelper helper) {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		var level = helper.getLevel(); var player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.CREATIVE);
		var core = TriPillarArena.build(helper, player, 1);
		var terminal = core.terminal(); BlockPos target = core.terminalPos(), desk = firstDesk(core);
		Direction facing = level.getBlockState(desk).getValue(HorizontalDirectionalBlock.FACING);
		// The operator sits on the side the desk faces away from (they look the way it faces).
		BlockPos seat = desk.relative(facing.getOpposite());
		player.setPos(seat.getX() + .5, seat.getY(), seat.getZ() + .5);
		helper.assertTrue(player.distanceToSqr(Vec3.atCenterOf(target)) > 64, "the hall desk's operator is far outside direct terminal reach");
		var before = terminal.mode();
		io.github.jimbozoomer.jugcraft.drone.TerminalModePayload.handle(player,
				new io.github.jimbozoomer.jugcraft.drone.TerminalModePayload(target));
		helper.assertTrue(terminal.mode() == before, "direct context cannot borrow the nearby desk's reach");
		io.github.jimbozoomer.jugcraft.drone.TerminalModePayload.handle(player,
				new io.github.jimbozoomer.jugcraft.drone.TerminalModePayload(target, desk));
		helper.assertTrue(terminal.mode() != before, "mode works from the operator side of a hall desk the core pinned to the yard's terminal");
		before = terminal.mode();
		player.setPos(seat.getX() + 10.5, seat.getY(), seat.getZ() + .5);
		io.github.jimbozoomer.jugcraft.drone.TerminalModePayload.handle(player,
				new io.github.jimbozoomer.jugcraft.drone.TerminalModePayload(target, desk));
		helper.assertTrue(terminal.mode() == before, "moving beyond the desk's eight-metre reach rejects the old UI");
		player.setPos(seat.getX() + .5, seat.getY(), seat.getZ() + .5);
		level.setBlock(desk, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
		helper.assertFalse(ConsoleDeskBlock.mayAccessTerminal(player, target, desk), "replaced desk cannot authorize an old screen");
		level.setBlock(desk, io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("console_desk").defaultBlockState(), 3);
		helper.assertFalse(ConsoleDeskBlock.mayAccessTerminal(player, target, desk),
				"a new desk the core never pinned has no link to a terminal this far away");
		var display = (DepotDisplayBlockEntity) level.getBlockEntity(desk);
		display.pin(target.east());
		helper.assertFalse(ConsoleDeskBlock.mayAccessTerminal(player, target, desk), "a differently pinned desk cannot control this terminal");
		display.pin(target);
		helper.assertTrue(ConsoleDeskBlock.mayAccessTerminal(player, target, desk), "pinned to the terminal again, the desk reaches it across the site");
		helper.succeed();
	}

	/**
	 * A desk the owner sets out in the yard pins itself to the terminal 15 blocks away and starts the next tier only for its
	 * exact tower and owner; one set down in the hall, 51 blocks from the terminal, links to nothing.
	 */
	@GameTest(structure = TriPillarArena.ARENA, skyAccess = true, maxTicks = 100)
	public void remoteDeskUpgradeRequiresItsExactTowerAndOwner(GameTestHelper helper) {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		var level = helper.getLevel(); var player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.CREATIVE);
		var core = TriPillarArena.build(helper, player, 1);
		TriPillarArena.stock(core);
		BlockPos origin = core.getBlockPos(), desk = TriPillarArena.at(core, YARD_SPOT);
		helper.assertTrue(level.getBlockState(desk).isAir(), "the yard has room for a desk");
		var display = placeDesk(helper, player, desk);
		helper.assertTrue(core.terminalPos().equals(display.terminal()), "a desk placed within 24 blocks of the terminal pins itself to it");
		player.setPos(desk.getX() + .5, desk.getY(), desk.getZ() + 1.5);
		helper.assertTrue(player.distanceToSqr(Vec3.atCenterOf(origin)) > TowerCoreBlockEntity.REACH_SQR,
				"the yard desk is beyond direct core reach");
		helper.assertTrue(TowerUpgradePayload.mayAccessTower(player, core, desk),
				"nearby desk pinned to this tower's terminal authorizes the access context");
		BlockPos otherPos = TriPillarArena.at(core, OTHER_CORE);
		level.setBlock(otherPos, JugcraftTower.CORE.defaultBlockState(), 3);
		var other = (TowerCoreBlockEntity) level.getBlockEntity(otherPos); other.setOwner(player.getUUID());
		TowerUpgradePayload.handle(player, new TowerUpgradePayload(otherPos, desk));
		helper.assertTrue(other.building() == 0 && other.foundationState() == 0, "desk cannot authorize another core even for the same owner");
		var stranger = helper.makeMockServerPlayerInLevel(); stranger.setPos(player.position());
		TowerUpgradePayload.handle(stranger, new TowerUpgradePayload(origin, desk));
		helper.assertTrue(core.building() == 0, "valid desk access does not bypass tower ownership");
		display.pin(otherPos);
		helper.assertFalse(TowerUpgradePayload.mayAccessTower(player, core, desk), "mislinked desk rejects the stale tower UI");
		display.pin(core.terminalPos());
		player.setPos(desk.getX() + 10.5, desk.getY(), desk.getZ() + 1.5);
		helper.assertFalse(TowerUpgradePayload.mayAccessTower(player, core, desk), "far operator cannot reuse a valid desk link");
		player.setPos(desk.getX() + .5, desk.getY(), desk.getZ() + 1.5);
		level.setBlock(desk, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
		helper.assertFalse(TowerUpgradePayload.mayAccessTower(player, core, desk), "removed desk cannot authorize a remote upgrade");
		BlockPos hall = TriPillarArena.at(core, HALL_SPOT);
		helper.assertTrue(level.getBlockState(hall).isAir(), "the hall has room for a desk");
		helper.assertTrue(placeDesk(helper, player, hall).terminal() == null, "a desk placed 51 blocks from the terminal links to nothing");
		player.setPos(hall.getX() + .5, hall.getY(), hall.getZ() + 1.5);
		helper.assertTrue(player.distanceToSqr(Vec3.atCenterOf(origin)) > TowerCoreBlockEntity.REACH_SQR
				&& !TowerUpgradePayload.mayAccessTower(player, core, hall), "an unlinked hall desk cannot upgrade the tower");
		placeDesk(helper, player, desk);
		player.setPos(desk.getX() + .5, desk.getY(), desk.getZ() + 1.5);
		TowerUpgradePayload.handle(player, new TowerUpgradePayload(origin, desk));
		helper.assertTrue(core.building() == 2, "owner starts the real upgrade through the validated yard desk");
		helper.succeed();
	}

	/**
	 * Night: nothing can spawn on any floor of the foundation and the tiers a mob could stand on, and the hall round the
	 * table keeps plenty of light. Since the night lighting (5 November 2026) the rooms nobody is in at night go dark on
	 * purpose; what keeps mobs off them is the tower's own spawn guard (TowerRegistry.inTower, mixin/TowerSpawnMixin),
	 * which covers the whole tower. A floor outside it must have block light.
	 */
	@GameTest(structure = TriPillarArena.FULL, skyAccess = true, maxTicks = 600)
	public void everyExposedTowerFloorHasBlockLighting(GameTestHelper helper) {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		var level = helper.getLevel();
		var core = TriPillarArena.placeCore(helper);
		core.setOwner(helper.makeMockPlayer(GameType.CREATIVE).getUUID());
		helper.assertTrue(level.dimensionType().monsterSpawnBlockLightLimit() == 0,
				"native Overworld ordinary-monster spawning requires zero block light");
		checkLightingStage(helper, core, new int[] {1, 2, 9}, 0);
	}

	private static void checkLightingStage(GameTestHelper helper, TowerCoreBlockEntity core, int[] stages, int index) {
		var level = helper.getLevel(); var origin = core.getBlockPos(); int stage = stages[index];
		core.buildInstantly(level, stage);
		helper.runAfterDelay(100, () -> {
			helper.setTime(18000);
			java.util.Set<BlockPos> candidates = new java.util.HashSet<>();
			for (int[] b : core.layout().foundation.place) candidates.add(origin.offset(b[0], b[1], b[2]));
			for (var tier : core.layout().tiers) if (tier.number <= stage)
				for (int[] b : tier.place) candidates.add(origin.offset(b[0], b[1], b[2]));
			int ground = 0, upper = 0, room = 0; java.util.List<String> dark = new java.util.ArrayList<>();
			var registry = io.github.jimbozoomer.jugcraft.tower.TowerRegistry.get(level);
			helper.assertTrue(registry.allCores().contains(origin), "the core lists itself for the spawn guard");
			for (BlockPos floor : candidates) {
				var state = level.getBlockState(floor);
				if (!state.isValidSpawn(level, floor, net.minecraft.world.entity.EntityTypes.ZOMBIE)) continue;
				boolean clear = true;
				for (int above = 1; above <= 2; above++) {
					BlockPos at = floor.above(above); var headroom = level.getBlockState(at);
					clear &= net.minecraft.world.level.NaturalSpawner.isValidEmptySpawnBlock(level, at, headroom,
							headroom.getFluidState(), net.minecraft.world.entity.EntityTypes.ZOMBIE);
				}
				if (!clear) continue;
				// Non-full door rails pass the block-level predicates, but may still intersect the mob's body.
				var spawnBox = net.minecraft.world.entity.EntityTypes.ZOMBIE.getDimensions()
						.makeBoundingBox(floor.getX() + .5, floor.getY() + 1, floor.getZ() + .5);
				if (!level.noCollision(spawnBox)) continue;
				BlockPos relative = floor.subtract(origin);
				// (a circle round the table, as the rotunda is: the old square's corners lie between the lit glass floor strips that
				// run out to the feet, at light 3 or 4)
				boolean hall = relative.getY() == 0 && relative.getX() * relative.getX() + relative.getZ() * relative.getZ() <= HALL * HALL;
				// Every native spawnable surface needs positive block light at a mob's feet; the hall round the table, under
				// its ceiling panels, much more.
				boolean guarded = registry.inTower(level, floor.above());
				int minimum = hall ? 5 : guarded ? 0 : 1;
				int light = level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, floor.above());
				if (relative.getY() == 0) { ground++; if (hall) room++; } else upper++;
				if (light < minimum && dark.size() < 20) dark.add(relative.toShortString() + "=" + light + " (needs " + minimum + ")");
				// This native predicate is checked against genuine block light, never inferred from skylight.
				helper.assertFalse(!guarded && net.minecraft.world.entity.monster.Monster.isDarkEnoughToSpawn(level, floor.above(),
						net.minecraft.util.RandomSource.create(floor.asLong())), "a floor outside the tower's spawn guard is dark enough for monsters: " + relative);
			}
			// (the round hall's floor a mob could stand on: about 70 cells, round the table, the console and its chairs)
			helper.assertTrue(ground > 3000 && room > 50, "tier " + stage + " checks the yard, the building's ground floor and the hall round the table: "
					+ ground + "/" + room);
			helper.assertTrue(upper > (stage >= 9 ? 20000 : 5000), "tier " + stage + " checks the building's upper floors, the roof and the tiers: " + upper);
			helper.assertTrue(dark.isEmpty(), "tier " + stage + " block-light coverage is insufficient at night: " + dark);
			if (index + 1 < stages.length) checkLightingStage(helper, core, stages, index + 1); else helper.succeed();
		});
	}

	/** The hall's 45 desks and its 16 fixtures are pinned to their own tower's terminal (and stay so after saving); desks never tick. */
	@GameTest(structure = TriPillarArena.ARENA, skyAccess = true, maxTicks = 100)
	public void commandDesksArePassiveAndPinnedToTheirOwnTower(GameTestHelper helper) {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		var level = helper.getLevel();
		var core = TriPillarArena.build(helper, helper.makeMockPlayer(GameType.CREATIVE), 1);
		BlockPos terminal = core.terminalPos();
		int desks = 0, fixtures = 0, touch = 0;
		for (int[] b : core.layout().foundation.place) {
			BlockPos at = core.getBlockPos().offset(b[0], b[1], b[2]); var state = level.getBlockState(at);
			if (state.getBlock() instanceof HallFixtureBlock fixture) {
				fixtures++;
				if (fixture.interactive()) touch++;
				helper.assertTrue(level.getBlockEntity(at) instanceof DepotDisplayBlockEntity display && terminal.equals(display.terminal()),
						"hall fixture " + at + " shows the tower's own depot");
				continue;
			}
			if (!(state.getBlock() instanceof ConsoleDeskBlock desk)) continue;
			desks++;
			var display = (DepotDisplayBlockEntity) level.getBlockEntity(at);
			helper.assertTrue(terminal.equals(display.terminal()) && display.reaches(terminal, desk.linkRange()), "desk uses the tower's exact terminal");
			helper.assertTrue(desk.getTicker(level, state, JugcraftTower.CONSOLE_ENTITY) == null, "desk adds no server ticking");
			var tag = display.saveWithoutMetadata(level.registryAccess());
			var loaded = new DepotDisplayBlockEntity(JugcraftTower.CONSOLE_ENTITY, at, state);
			loaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
			helper.assertTrue(terminal.equals(loaded.terminal()) && loaded.reaches(terminal, desk.linkRange()),
					"pinned desk (and its reach across the site) survives save/load");
		}
		// six rows of three: the five of TriPillarArena.DESK_ROWS and the security office's (its single desks became a row, layout 45)
		helper.assertTrue(desks == 18, "the control building has its 18 working desks beside its separate master console (the operator "
				+ "rows went for the flight-ops trees, layout 45): " + desks);
		// the five touch consoles went for the Tower Control Console (layout 10), the six holo emitters for the Holo Projector
		// (layout 19): the four drone display stands, the info totem and the service bay's arm are left
		helper.assertTrue(fixtures == 6 && touch == 0, "the hall has 6 live fixtures, none of them touch consoles: " + fixtures + "/" + touch);
		helper.succeed();
	}

	@GameTest
	public void continuousMaterialsFollowWorldCoordinates(GameTestHelper helper) {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		var concrete = io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("reinforced_concrete").defaultBlockState();
		helper.assertTrue(concrete.getValue(TowerPanelBlock.PHASE) == 0, "old saved blocks have the default phase");
		for (int phase = 0; phase < 8; phase++) {
			BlockPos positive = new BlockPos(10 + (phase & 1), 20 + ((phase >> 1) & 1), 30 + ((phase >> 2) & 1));
			BlockPos negative = positive.offset(-32, -32, -32);
			helper.assertTrue(TowerPanelBlock.at(concrete, positive).getValue(TowerPanelBlock.PHASE) == phase,
					"material phase follows absolute coordinates " + phase);
			helper.assertTrue(TowerPanelBlock.at(concrete, negative).getValue(TowerPanelBlock.PHASE) == phase,
					"negative coordinates keep the same sheet alignment " + phase);
		}
		var ordinary = JugcraftTower.CORE.defaultBlockState();
		helper.assertTrue(TowerPanelBlock.at(ordinary, BlockPos.ZERO) == ordinary, "other block states remain unchanged");
		helper.succeed();
	}

	/**
	 * New cores are the Tri-Pillar. A save of an earlier design loads with that design and its unfinished tiles (it is
	 * converted on its first server tick, which a detached block entity never has), and Tri-Pillar saves keep their foundation.
	 */
	@GameTest
	public void legacySaveKeepsLayoutAndUnfinishedTiles(GameTestHelper helper) {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		var level = helper.getLevel();
		var state = JugcraftTower.CORE.defaultBlockState();
		var original = new TowerCoreBlockEntity(BlockPos.ZERO, state);
		helper.assertTrue(original.layoutVersion() == TowerData.CURRENT_LAYOUT && original.layout() == TowerData.get(), "new cores use the Tri-Pillar");
		helper.assertTrue(original.foundationState() == 0 && original.needsFoundation() && original.nextStep() == original.layout().foundation,
				"a new core's first step is the foundation");
		var concrete = io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("reinforced_concrete").asItem();
		helper.assertTrue(original.deposit(new ItemStack(concrete, 17)) == 17, "a new core takes the foundation's concrete");
		CompoundTag old = original.saveWithoutMetadata(level.registryAccess());
		old.remove("layout_version");
		old.putInt("tier", 2);
		old.putInt("building", 3);
		old.putIntArray("filled", new int[] {0, 2, 5});
		var loaded = load(level, old);
		helper.assertTrue(loaded.layoutVersion() == TowerData.LEGACY_LAYOUT && loaded.layout() == TowerData.get(TowerData.LEGACY_LAYOUT),
				"old saves retain original geometry and tile ordering");
		helper.assertTrue(!loaded.converting() && loaded.foundationState() == 0 && !loaded.needsFoundation(),
				"loading alone converts nothing, and the original design has no foundation");
		helper.assertTrue(loaded.tier() == 2 && loaded.building() == 3 && loaded.filledTiles().cardinality() == 3 && loaded.filledTiles().get(5), "unfinished tile progress survives");
		helper.assertTrue(loaded.stored(concrete) == 17, "stored blocks survive");
		var roundTrip = load(level, loaded.saveWithoutMetadata(level.registryAccess()));
		helper.assertTrue(roundTrip.layoutVersion() == TowerData.LEGACY_LAYOUT && roundTrip.filledTiles().equals(loaded.filledTiles()), "version is explicit after saving again");
		var modern = load(level, original.saveWithoutMetadata(level.registryAccess()));
		helper.assertTrue(modern.layoutVersion() == TowerData.CURRENT_LAYOUT && modern.stored(concrete) == 17 && modern.needsFoundation(), "Tri-Pillar save round-trip");
		CompoundTag tri = original.saveWithoutMetadata(level.registryAccess());
		tri.putInt("foundation", 2);
		var founded = load(level, tri);
		helper.assertTrue(founded.foundationState() == 2 && !founded.needsFoundation() && founded.tier() == 0
				&& founded.nextStep() == founded.layout().tier(1), "a saved built foundation stays built; the next step is the roof pads");
		tri.remove("foundation");
		tri.putInt("tier", 0);
		tri.putInt("building", 1);
		tri.putInt("progress", 900);
		var firstBuild = load(level, tri);
		helper.assertTrue(firstBuild.buildingFoundation() && firstBuild.building() == 0 && firstBuild.tier() == 0,
				"a Tri-Pillar saved while building the hall and pads as one tier 1 builds its foundation again");
		tri.putInt("tier", 3);
		tri.putInt("building", 0);
		var builtBefore = load(level, tri);
		helper.assertTrue(builtBefore.foundationState() == 2 && builtBefore.tier() == 3, "a Tri-Pillar saved at tier 3 before the foundation step had it built");
		helper.assertTrue(TowerData.get().capacity(TowerData.TIERS) == 128, "the Tri-Pillar holds 128 drones at tier 9");
		for (int revision : new int[] {TowerData.FIRST_INDUSTRIAL_LAYOUT, TowerData.PREVIOUS_INDUSTRIAL_LAYOUT, TowerData.SERVICE_LAYOUT,
				TowerData.LARGE_INPUT_LAYOUT, TowerData.APRON_LAYOUT, TowerData.INDUSTRIAL_LAYOUT}) {
			old.putInt("layout_version", revision);
			var previousIndustrial = load(level, old);
			var frozen = TowerData.get(revision); var savedTile = frozen.tier(3).tiles.get(5);
			helper.assertTrue(previousIndustrial.layoutVersion() == revision && previousIndustrial.layout() == frozen
					&& frozen != TowerData.get() && previousIndustrial.filledTiles().get(5) && !previousIndustrial.converting(),
					"earlier industrial saves load with their frozen tile order (they convert when they tick): " + revision);
			var savedAgain = load(level, previousIndustrial.saveWithoutMetadata(level.registryAccess()));
			helper.assertTrue(savedAgain.layoutVersion() == revision && savedAgain.layout() == frozen
					&& savedAgain.building() == 3 && savedAgain.filledTiles().equals(previousIndustrial.filledTiles())
					&& savedAgain.layout().tier(3).tiles.get(5) == savedTile, "unfinished frozen tile addresses survive another save: " + revision);
			helper.assertTrue(frozen.foundation == null && frozen.tier(9).capacity == TowerData.get(TowerData.LEGACY_LAYOUT).tier(9).capacity,
					"the revision keeps its own fleet sizes and has no foundation step: " + revision);
			String expectedFolder = revision == TowerData.INDUSTRIAL_LAYOUT ? "tower_industrial" : "tower_industrial_v" + revision;
			helper.assertTrue(TowerData.previewFolder(revision).equals(expectedFolder), "saved tower status uses its frozen images");
			for (int tier = 0; tier <= 9; tier++) helper.assertTrue(TowerData.class.getResource(
					"/assets/jugcraft/textures/gui/" + expectedFolder + "/tier_" + tier + ".png") != null,
					"frozen status image exists for revision " + revision + " tier " + tier);
		}
		helper.assertTrue(TowerData.previewFolder(TowerData.TRIPILLAR_LAYOUT).equals("tower_tripillar")
				&& TowerData.previewFolder(TowerData.LEGACY_LAYOUT).equals("drone_tower"), "new and legacy status pictures retain their own paths");
		for (String picture : new String[] {"foundation", "tier_0", "tier_1", "tier_2", "tier_3", "tier_4", "tier_5", "tier_6", "tier_7", "tier_8", "tier_9"}) {
			helper.assertTrue(TowerData.class.getResource("/assets/jugcraft/textures/gui/tower_tripillar/" + picture + ".png") != null,
					"Tri-Pillar status image exists: " + picture);
		}
		helper.succeed();
	}

	/**
	 * Tier 2: the 24 roof docks came with tier 1, the 20 pillar-foot bays with tier 2. Since 7 November 2026 (Justin: "I
	 * want people to be able to use any drone for any pad", up to the tower's tier) any drone takes the lowest free dock
	 * that fits: the pillar-foot bays first, then the roof. (The yard's landing pad, four more tier 1 slots, went with the
	 * yard's platform in layout 40: a tower's platform is its pickup, with no pad.)
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 160, skyAccess = true)
	public void anyDroneTakesTheLowestFreeDockThatFits(GameTestHelper helper) {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		var level = helper.getLevel();
		var player = helper.makeMockPlayer(GameType.CREATIVE);
		var core = TriPillarArena.build(helper, player, 2);
		BlockPos pos = core.getBlockPos();
		TowerData data = core.layout();
		for (TowerData.Tier step : List.of(data.foundation, data.tier(1), data.tier(2))) {
			for (int[] placed : step.place) {
				BlockPos at = pos.offset(placed[0], placed[1], placed[2]);
				var built = level.getBlockState(at);
				if (built.getBlock() instanceof TowerPanelBlock) {
					helper.assertTrue(built == TowerPanelBlock.at(built, at), "authored material sheets use world alignment");
				}
			}
		}
		Set<Vec3> roof = docks(pos, data.tier(1)), bays = docks(pos, data.tier(2));
		helper.assertTrue(roof.size() == 24 && data.tier(1).docks.stream().allMatch(d -> d.size() == DroneSize.SMALL && d.y() == TriPillarArena.ROOF_Y + 1),
				"tier 1 is 24 small docks on the roof pads");
		helper.assertTrue(bays.size() == 20 && data.tier(2).docks.stream().allMatch(d -> d.size() == DroneSize.SMALL), "tier 2 adds 20 small bays");
		DroneTerminalBlockEntity terminal = core.terminal();
		helper.assertTrue(terminal.maxDrones() == 44, "a tier 2 tower holds 44 drones: " + terminal.maxDrones());
		for (int i = 0; i < 24; i++) helper.assertTrue(terminal.linkDrone(player, DroneTier.COURIER_QUAD), "T1 drone " + i);
		// the pillar feet's lower bays are on the roof's level (21) and nearer the core than the roof pads; their upper bays at 26
		for (int i = 0; i < 10; i++) helper.assertTrue(bays.contains(terminal.dockOf(i)) && terminal.dockOf(i).y == pos.getY() + TriPillarArena.ROOF_Y + 1,
				"T1 drone " + i + " takes a lower pillar-foot bay first (the nearest on the lowest floor): " + terminal.dockOf(i));
		for (int i = 10; i < 24; i++) helper.assertTrue(roof.contains(terminal.dockOf(i)), "then T1 drone " + i + " a roof dock on the same floor");
		helper.assertTrue(terminal.linkDrone(player, DroneTier.CREATIVE_DRONE) && roof.contains(terminal.dockOf(24)), "a creative drone takes a roof dock");
		for (int i = 0; i < 19; i++) helper.assertTrue(terminal.linkDrone(player, DroneTier.SURVEY_HEXACOPTER), "T2 drone " + i + " (any tier on any pad)");
		for (int i = 25; i < 34; i++) helper.assertTrue(roof.contains(terminal.dockOf(i)), "T2 drone " + (i - 25) + " fills the roof");
		for (int i = 34; i < 44; i++) helper.assertTrue(bays.contains(terminal.dockOf(i)) && terminal.dockOf(i).y > pos.getY() + TriPillarArena.ROOF_Y + 1,
				"then T2 drone " + (i - 25) + " an upper pillar-foot bay: " + terminal.dockOf(i));
		for (int i = 1; i < 44; i++) {
			Vec3 a = terminal.dockOf(i - 1), b = terminal.dockOf(i);
			double da = (a.x - pos.getX()) * (a.x - pos.getX()) + (a.z - pos.getZ()) * (a.z - pos.getZ());
			double db = (b.x - pos.getX()) * (b.x - pos.getX()) + (b.z - pos.getZ()) * (b.z - pos.getZ());
			helper.assertTrue(b.y > a.y || b.y == a.y && db >= da - 1e-6, "the docks fill lowest floor first, nearest the core first: " + (i - 1) + " " + a + ", " + i + " " + b);
		}
		helper.assertFalse(terminal.linkDrone(player, DroneTier.SURVEY_HEXACOPTER), "the full fleet of 44 refuses more");
		helper.assertFalse(terminal.linkDrone(player, DroneTier.LIFTER_OCTOCOPTER), "and a T3 drone is past a tier 2 tower");
		Set<Vec3> taken = new HashSet<>();
		for (int i = 0; i < 44; i++) {
			helper.assertTrue((bays.contains(terminal.dockOf(i)) || roof.contains(terminal.dockOf(i))) && taken.add(terminal.dockOf(i)),
					"drone " + i + " has its own dock");
		}
		for (int i = 0; i < 64 && terminal.fleet().size() > 0; i++) terminal.unlinkDrone(player);
		helper.assertTrue(terminal.fleet().size() == 0, "the fleet is unlinked again");
		for (int i = 0; i < 21; i++) helper.assertTrue(terminal.linkDrone(player, DroneTier.SURVEY_HEXACOPTER), "T2 drone " + i + " of an empty fleet");
		helper.assertTrue(roof.contains(terminal.dockOf(20)), "a 21st T2 drone takes a roof dock now");
		helper.assertTrue(terminal.linkDrone(player, DroneTier.CREATIVE_DRONE) && roof.contains(terminal.dockOf(21)),
				"a creative drone takes a spare roof dock");
		helper.succeed();
	}

	@GameTest
	public void controlReadoutsSeparateBatteryLoadAndAirborne(GameTestHelper helper) {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		DepotView view = new DepotView();
		view.energy = 100_000; view.capacity = 200_000;
		view.standbyDraw = 16; view.workingDraw = 2048;
		FlightPath path = new FlightPath(new double[][] {{0, 1, 0}, {8, 1, 0}, {12, 1, 0}, {0, 1, 0}}, new double[] {4, 4, 4}, 4);
		view.flights.add(new DepotView.FlightView(0, 1, path, 0, path.totalTicks() * FlightScheduler.FULL_RATE, FlightScheduler.FULL_RATE));
		helper.assertTrue(view.active() == 1 && view.airborne(1) == 1 && view.loadPercent() == 50, "active job, airborne flight and rated input load");
		helper.assertTrue(view.airborne(path.totalTicks() - FlightScheduler.RECHARGE_TICKS) == 0 && view.active() == 1, "charging drone is active but grounded");
		view.flights.clear();
		helper.assertTrue(view.draw() == 16 && view.loadPercent() == 0, "idle uses standby draw, independent of 50% battery charge");
		helper.succeed();
	}

	/**
	 * The Tri-Pillar's data: every state parses, its fleet sizes, its foundation (fits the arenas, holds the terminal and both
	 * exchange masters where binding looks), docks and cradles; a core set by code or placed by a player is the Tri-Pillar.
	 */
	@GameTest(structure = TriPillarArena.ARENA, skyAccess = true, maxTicks = 20)
	public void tripillarDesignParses(GameTestHelper helper) {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		TowerData data = TowerData.get(TowerData.CURRENT_LAYOUT);
		helper.assertTrue(TowerData.CURRENT_LAYOUT >= TowerData.TRIPILLAR_LAYOUT && TowerData.get() == data, "the Tri-Pillar is the current design");
		for (int i = 0; i < data.palette.size(); i++) {
			helper.assertTrue(!data.palette.get(i).isAir(), "every Tri-Pillar block state parses (palette entry " + i + ")");
		}
		int[] capacities = {24, 44, 62, 78, 92, 104, 114, 122, 128};
		for (int t = 1; t <= TowerData.TIERS; t++) helper.assertTrue(data.capacity(t) == capacities[t - 1], "tier " + t + " holds " + capacities[t - 1] + " drones");
		TowerData.Tier foundation = data.foundation;
		helper.assertTrue(foundation != null && foundation.number == 0 && foundation.docks.isEmpty() && foundation.pickups.isEmpty()
				&& foundation.capacity == 0 && !data.cost(foundation).isEmpty(), "the foundation is its own paid step with no docks");
		for (int[] b : foundation.place) {
			// (up to y 26 since layout 40: the goods lift's house on the roof, the supply pickup on its top; the arena is 48 high)
			helper.assertTrue(b[0] >= -52 && b[0] <= 62 && b[1] >= -3 && b[1] <= 26 && b[2] >= -53 && b[2] <= 45,
					"the foundation stays within the arenas' footprint: " + b[0] + ", " + b[1] + ", " + b[2]);
		}
		java.util.List<Integer> cradles = new java.util.ArrayList<>();
		for (int t = 1; t <= TowerData.TIERS; t++) {
			for (TowerData.Pickup pickup : data.tier(t).pickups) cradles.add(pickup.tier());
			for (TowerData.Dock dock : data.tier(t).docks) {
				helper.assertTrue(dock.tier() == t, "each tier's docks belong to its own drones");
				helper.assertTrue(t == 1 ? dock.door() == null && dock.y() == TriPillarArena.ROOF_Y + 1 : dock.door() != null && dock.door().length >= 8,
						"roof pads are open; every bay has a door");
			}
		}
		helper.assertTrue(cradles.equals(List.of(2, 4, 6, 8)), "supply cradles at tiers 2, 4, 6 and 8: " + cradles);
		java.util.Map<Long, Integer> ground = new java.util.HashMap<>();
		for (int[] b : foundation.place) ground.put(BlockPos.asLong(b[0], b[1], b[2]), b[3]);
		for (var station : new ExchangeStationBlock[] {ExchangeStations.ENERGY, ExchangeStations.CARGO}) {
			var master = ExchangeStations.masterOffset(station, TowerData.CURRENT_LAYOUT);
			Integer index = ground.get(BlockPos.asLong(master.getX(), master.getY(), master.getZ()));
			Direction facing = ExchangeStations.facing(station, TowerData.CURRENT_LAYOUT);
			helper.assertTrue(index != null && data.state(index).is(station) && data.state(index).getValue(ExchangeStationBlock.FACING) == facing,
					"the foundation puts the exchange's master cell where binding looks, facing " + facing);
		}
		helper.assertTrue(new BlockPos(data.terminal[0], data.terminal[1], data.terminal[2]).equals(TriPillarArena.TERMINAL), "the terminal is in dispatch");
		Integer terminal = ground.get(BlockPos.asLong(data.terminal[0], data.terminal[1], data.terminal[2]));
		helper.assertTrue(terminal != null && data.state(terminal).is(io.github.jimbozoomer.jugcraft.drone.JugcraftDrones.TERMINAL),
				"the foundation places the depot terminal");
		var core = TriPillarArena.placeCore(helper);
		helper.assertTrue(core.layoutVersion() == TowerData.CURRENT_LAYOUT && core.foundationState() == 0 && core.nextStep() == foundation,
				"a core set by code is a Tri-Pillar waiting for its foundation");
		var placer = helper.makeMockPlayer(GameType.CREATIVE);
		JugcraftTower.CORE.setPlacedBy(helper.getLevel(), core.getBlockPos(), helper.getLevel().getBlockState(core.getBlockPos()), placer, ItemStack.EMPTY);
		helper.assertTrue(core.layoutVersion() == TowerData.CURRENT_LAYOUT && placer.getUUID().equals(core.owner()),
				"a core a player places is theirs, and stays the Tri-Pillar");
		helper.succeed();
	}

	// ------------------------------------------------------------------ helpers

	/** The first console desk the foundation places. */
	private static BlockPos firstDesk(TowerCoreBlockEntity core) {
		TowerData data = core.layout();
		for (int[] b : data.foundation.place) {
			if (data.state(b[3]).getBlock() instanceof ConsoleDeskBlock) return core.getBlockPos().offset(b[0], b[1], b[2]);
		}
		throw new IllegalStateException("the foundation has no console desk");
	}

	/** A console desk placed at {@code at} as {@code player} would (it pins itself to the nearest terminal within 24). */
	private static DepotDisplayBlockEntity placeDesk(GameTestHelper helper, net.minecraft.world.entity.player.Player player, BlockPos at) {
		var level = helper.getLevel(); var block = io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("console_desk");
		level.setBlock(at, block.defaultBlockState(), 3);
		block.setPlacedBy(level, at, level.getBlockState(at), player, new ItemStack(block));
		return (DepotDisplayBlockEntity) level.getBlockEntity(at);
	}

	/** Where a tier's docks are, in the world. */
	private static Set<Vec3> docks(BlockPos core, TowerData.Tier tier) {
		Set<Vec3> out = new HashSet<>();
		for (TowerData.Dock d : tier.docks) out.add(new Vec3(core.getX() + d.x(), core.getY() + d.y(), core.getZ() + d.z()));
		return out;
	}

	/** A detached core loaded from {@code tag}. */
	private static TowerCoreBlockEntity load(net.minecraft.server.level.ServerLevel level, CompoundTag tag) {
		var core = new TowerCoreBlockEntity(BlockPos.ZERO, JugcraftTower.CORE.defaultBlockState());
		core.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
		return core;
	}
}
