package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.drone.DepotView;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DroneTier;
import io.github.jimbozoomer.jugcraft.drone.FlightPath;
import io.github.jimbozoomer.jugcraft.drone.FlightScheduler;
import io.github.jimbozoomer.jugcraft.tower.JugcraftTower;
import io.github.jimbozoomer.jugcraft.tower.ExchangeStations;
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.TowerData;
import io.github.jimbozoomer.jugcraft.tower.TowerPanelBlock;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;

/** Save compatibility and owner-requested tier/power readouts for the industrial design. */
public class IndustrialTowerGameTests {
	@GameTest(structure = "jugcraft-test:drone_tower", skyAccess = true, maxTicks = 100)
	public void commandRoomHasTwoFormedScreensPinnedToItsOwnTerminal(GameTestHelper helper) {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		var level = helper.getLevel(); BlockPos origin = helper.absolutePos(new BlockPos(48, 1, 48));
		level.setBlock(origin, JugcraftTower.CORE.defaultBlockState(), 3);
		var core = (TowerCoreBlockEntity) level.getBlockEntity(origin);
		core.setOwner(helper.makeMockPlayer(GameType.CREATIVE).getUUID()); core.buildInstantly(level, 1);
		int screens = 0, anchors = 0;
		java.util.Map<Integer, Integer> partCounts = new java.util.HashMap<>();
		for (int[] b : core.layout().tier(1).place) {
			BlockPos pos = origin.offset(b[0], b[1], b[2]); var state = level.getBlockState(pos);
			if (!(state.getBlock() instanceof io.github.jimbozoomer.jugcraft.drone.ControlScreenBlock)) continue;
			screens++;
			int part = state.getValue(io.github.jimbozoomer.jugcraft.drone.ControlScreenBlock.PART);
			helper.assertTrue(part >= 1 && part <= 6, "every actual control-room screen panel is formed, not standby");
			partCounts.merge(part, 1, Integer::sum);
			if (part == 1) {
				anchors++;
				helper.assertTrue(level.getBlockEntity(pos) instanceof io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity display
						&& core.terminalPos().equals(display.terminal()), "each screen anchor is pinned to this exact tower terminal");
			}
		}
		helper.assertTrue(screens == 12 && anchors == 2, "two independent formed 3x2 displays provide twelve panels and two anchors");
		for (int part = 1; part <= 6; part++) helper.assertTrue(partCounts.getOrDefault(part, 0) == 2, "each display contains exactly one part " + part);
		helper.succeed();
	}

	@GameTest(structure = "jugcraft-test:drone_tower", skyAccess = true, maxTicks = 100)
	public void unownedTerminalIsClaimedByItsFirstDeskOperator(GameTestHelper helper) {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		var level = helper.getLevel();
		BlockPos target = helper.absolutePos(new BlockPos(12, 2, 12)), desk = target.east(5);
		level.setBlock(target, io.github.jimbozoomer.jugcraft.drone.JugcraftDrones.TERMINAL.defaultBlockState(), 3);
		level.setBlock(desk, JugcraftTower.BLOCKS.get("console_desk").defaultBlockState(), 3);
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

	@GameTest(structure = "jugcraft-test:drone_tower", skyAccess = true, maxTicks = 100)
	public void deskModeControlsUseTheClickedDeskAndRejectStaleLinks(GameTestHelper helper) {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		var level = helper.getLevel(); var player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.CREATIVE);
		BlockPos origin = helper.absolutePos(new BlockPos(48, 1, 48));
		level.setBlock(origin, JugcraftTower.CORE.defaultBlockState(), 3);
		var core = (TowerCoreBlockEntity) level.getBlockEntity(origin);
		core.setOwner(player.getUUID()); core.buildInstantly(level, 1);
		var terminal = core.terminal(); BlockPos target = core.terminalPos(), desk = origin.offset(6, 1, -2);
		player.setPos(origin.getX() + 5.5, origin.getY() + 1, origin.getZ() - 1.5);
		helper.assertTrue(player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(target)) > 64,
				"rightmost desk operator is outside direct terminal reach");
		var before = terminal.mode();
		io.github.jimbozoomer.jugcraft.drone.TerminalModePayload.handle(player,
				new io.github.jimbozoomer.jugcraft.drone.TerminalModePayload(target));
		helper.assertTrue(terminal.mode() == before, "direct context cannot borrow the nearby desk's reach");
		io.github.jimbozoomer.jugcraft.drone.TerminalModePayload.handle(player,
				new io.github.jimbozoomer.jugcraft.drone.TerminalModePayload(target, desk));
		helper.assertTrue(terminal.mode() != before, "mode works from the actual operator side of the rightmost desk");
		before = terminal.mode();
		player.setPos(origin.getX() + 15.5, origin.getY() + 1, origin.getZ() - 1.5);
		io.github.jimbozoomer.jugcraft.drone.TerminalModePayload.handle(player,
				new io.github.jimbozoomer.jugcraft.drone.TerminalModePayload(target, desk));
		helper.assertTrue(terminal.mode() == before, "moving beyond the desk's eight-metre reach rejects the old UI");
		player.setPos(origin.getX() + 5.5, origin.getY() + 1, origin.getZ() - 1.5);
		level.setBlock(desk, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
		helper.assertFalse(io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock.mayAccessTerminal(player, target, desk),
				"replaced desk cannot authorize an old screen");
		level.setBlock(desk, JugcraftTower.BLOCKS.get("console_desk").defaultBlockState(), 3);
		((io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity) level.getBlockEntity(desk)).pin(target.east());
		helper.assertFalse(io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock.mayAccessTerminal(player, target, desk),
				"a differently pinned desk cannot control this terminal");
		helper.succeed();
	}

	@GameTest(structure = "jugcraft-test:drone_tower", skyAccess = true, maxTicks = 100)
	public void remoteDeskUpgradeRequiresItsExactTowerAndOwner(GameTestHelper helper) {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		var level = helper.getLevel(); var player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.CREATIVE);
		BlockPos origin = helper.absolutePos(new BlockPos(48, 1, 48));
		level.setBlock(origin, JugcraftTower.CORE.defaultBlockState(), 3);
		var core = (TowerCoreBlockEntity) level.getBlockEntity(origin);
		core.setOwner(player.getUUID()); core.buildInstantly(level, 1);
		for (String module : JugcraftTower.MODULES) core.deposit(new ItemStack(JugcraftTower.MODULE_ITEMS.get(module), TowerCoreBlockEntity.MODULE_CAPACITY));
		BlockPos desk = origin.offset(15, 1, 9);
		level.setBlock(desk, JugcraftTower.BLOCKS.get("console_desk").defaultBlockState(), 3);
		var display = (io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity) level.getBlockEntity(desk);
		display.pin(core.terminalPos());
		player.setPos(desk.getX() + .5, desk.getY(), desk.getZ() + 1.5);
		helper.assertTrue(player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(origin)) > TowerCoreBlockEntity.REACH_SQR,
				"remote crafted desk is beyond direct core reach");
		helper.assertTrue(io.github.jimbozoomer.jugcraft.tower.TowerUpgradePayload.mayAccessTower(player, core, desk),
				"nearby desk pinned to this tower's terminal authorizes the access context");
		BlockPos otherPos = origin.offset(14, 0, -14);
		level.setBlock(otherPos, JugcraftTower.CORE.defaultBlockState(), 3);
		var other = (TowerCoreBlockEntity) level.getBlockEntity(otherPos); other.setOwner(player.getUUID());
		io.github.jimbozoomer.jugcraft.tower.TowerUpgradePayload.handle(player,
				new io.github.jimbozoomer.jugcraft.tower.TowerUpgradePayload(otherPos, desk));
		helper.assertTrue(other.building() == 0, "desk cannot authorize another core even for the same owner");
		var stranger = helper.makeMockServerPlayerInLevel(); stranger.setPos(player.position());
		io.github.jimbozoomer.jugcraft.tower.TowerUpgradePayload.handle(stranger,
				new io.github.jimbozoomer.jugcraft.tower.TowerUpgradePayload(origin, desk));
		helper.assertTrue(core.building() == 0, "valid desk access does not bypass tower ownership");
		display.pin(otherPos);
		helper.assertFalse(io.github.jimbozoomer.jugcraft.tower.TowerUpgradePayload.mayAccessTower(player, core, desk),
				"mislinked desk rejects the stale tower UI");
		display.pin(core.terminalPos());
		player.setPos(desk.getX() + 10.5, desk.getY(), desk.getZ() + 1.5);
		helper.assertFalse(io.github.jimbozoomer.jugcraft.tower.TowerUpgradePayload.mayAccessTower(player, core, desk),
				"far operator cannot reuse a valid desk link");
		player.setPos(desk.getX() + .5, desk.getY(), desk.getZ() + 1.5);
		level.setBlock(desk, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
		helper.assertFalse(io.github.jimbozoomer.jugcraft.tower.TowerUpgradePayload.mayAccessTower(player, core, desk),
				"removed desk cannot authorize a remote upgrade");
		level.setBlock(desk, JugcraftTower.BLOCKS.get("console_desk").defaultBlockState(), 3);
		((io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity) level.getBlockEntity(desk)).pin(core.terminalPos());
		io.github.jimbozoomer.jugcraft.tower.TowerUpgradePayload.handle(player,
				new io.github.jimbozoomer.jugcraft.tower.TowerUpgradePayload(origin, desk));
		helper.assertTrue(core.building() == 2, "owner starts the real upgrade through the validated remote desk");
		helper.succeed();
	}

	@GameTest(structure = "jugcraft-test:drone_tower_full", skyAccess = true, maxTicks = 400)
	public void everyExposedTowerFloorHasBlockLighting(GameTestHelper helper) {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		var level = helper.getLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(48, 1, 48));
		level.setBlock(origin, JugcraftTower.CORE.defaultBlockState(), 3);
		var core = (TowerCoreBlockEntity) level.getBlockEntity(origin);
		core.setOwner(helper.makeMockPlayer(GameType.CREATIVE).getUUID());
		helper.assertTrue(level.dimensionType().monsterSpawnBlockLightLimit() == 0,
				"native Overworld ordinary-monster spawning requires zero block light");
		checkLightingStage(helper, core, new int[] {1, 2, 9}, 0);
	}

	private static void checkLightingStage(GameTestHelper helper, TowerCoreBlockEntity core, int[] stages, int index) {
		var level = helper.getLevel(); var origin = core.getBlockPos(); int stage = stages[index];
		core.buildInstantly(level, stage);
		helper.runAfterDelay(80, () -> {
			helper.setTime(18000);
			java.util.Set<BlockPos> candidates = new java.util.HashSet<>();
			for (var tier : core.layout().tiers) if (tier.number <= stage)
				for (int[] b : tier.place) candidates.add(origin.offset(b[0], b[1], b[2]));
			int ground = 0, upper = 0, room = 0; java.util.List<String> dark = new java.util.ArrayList<>();
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
				boolean insideRoom = Math.abs(relative.getX()) <= 7 && relative.getZ() >= -6 && relative.getZ() <= 8;
				// Every native spawnable surface needs positive block light at a mob's feet.
				// Keep the occupied control-room floor brighter; upper fixture positions are frozen separately.
				int minimum = relative.getY() == 0 && insideRoom ? 5 : 1;
				int light = level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, floor.above());
				if (relative.getY() == 0) { ground++; if (insideRoom) room++; } else upper++;
				if (light < minimum && dark.size() < 20) dark.add(relative.toShortString() + "=" + light + " (needs " + minimum + ")");
				// This native predicate is checked against genuine block light, never inferred from skylight.
				helper.assertFalse(net.minecraft.world.entity.monster.Monster.isDarkEnoughToSpawn(level, floor.above(),
						net.minecraft.util.RandomSource.create(floor.asLong())), "native monster light rule rejects the lit floor " + relative);
			}
			helper.assertTrue(ground > 2000 && room > 30, "tier " + stage + " checks the actual broad apron and occupied control-room floors: " + ground + "/" + room);
			if (stage >= 2) helper.assertTrue(upper > 300, "tier " + stage + " checks actual higher roof, walkway and hangar floors: " + upper);
			helper.assertTrue(dark.isEmpty(), "tier " + stage + " block-light coverage is insufficient at night: " + dark);
			if (index + 1 < stages.length) checkLightingStage(helper, core, stages, index + 1); else helper.succeed();
		});
	}

	@GameTest(structure = "jugcraft-test:drone_tower", skyAccess = true, maxTicks = 100)
	public void commandDesksArePassiveAndPinnedToTheirOwnTower(GameTestHelper helper) {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		var level = helper.getLevel(); BlockPos origin = helper.absolutePos(new BlockPos(48, 1, 48));
		level.setBlock(origin, JugcraftTower.CORE.defaultBlockState(), 3);
		var core = (TowerCoreBlockEntity) level.getBlockEntity(origin);
		core.setOwner(helper.makeMockPlayer(GameType.CREATIVE).getUUID()); core.buildInstantly(level, 1);
		int desks = 0;
		for (int[] b : core.layout().tier(1).place) {
			BlockPos at = origin.offset(b[0], b[1], b[2]); var state = level.getBlockState(at);
			if (!(state.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock desk)) continue;
			desks++;
			var display = (io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity) level.getBlockEntity(at);
			helper.assertTrue(core.terminalPos().equals(display.terminal()), "desk uses the tower's exact terminal");
			helper.assertTrue(desk.getTicker(level, state, JugcraftTower.CONSOLE_ENTITY) == null, "desk adds no server ticking");
			var tag = display.saveWithoutMetadata(level.registryAccess());
			var loaded = new io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity(JugcraftTower.CONSOLE_ENTITY, at, state);
			loaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
			helper.assertTrue(core.terminalPos().equals(loaded.terminal()), "pinned desk survives save/load");
		}
		helper.assertTrue(desks == 2, "command room has two working wall stations beside its separate master console"); helper.succeed();
	}
	@GameTest(structure = "jugcraft-test:drone_tower", maxTicks = 160, skyAccess = true)
	public void industrialBuildCommandRefusesLegacyWithoutChangingIt(GameTestHelper helper)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		var level = helper.getLevel();
		BlockPos pos = helper.absolutePos(new BlockPos(48, 1, 48));
		level.setBlock(pos, JugcraftTower.CORE.defaultBlockState(), 3);
		var core = (TowerCoreBlockEntity) level.getBlockEntity(pos);
		CompoundTag old = core.saveWithoutMetadata(level.registryAccess());
		old.remove("layout_version");
		core.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), old));
		var player = helper.makeMockServerPlayerInLevel();
		player.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
		core.setOwner(player.getUUID());
		BlockPos marker = pos.offset(-25, 1, 0);
		level.setBlock(marker, net.minecraft.world.level.block.Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
		var source = level.getServer().createCommandSourceStack().withEntity(player);
		var commands = level.getServer().getCommands().getDispatcher();
		helper.assertTrue(commands.execute("dronetest build industrial 1", source) == 0, "explicit industrial command refuses the saved legacy core");
		helper.assertTrue(core.layoutVersion() == TowerData.LEGACY_LAYOUT && core.tier() == 0 && core.building() == 0,
				"refusal preserves the saved core and its progress");
		helper.assertTrue(level.getBlockEntity(pos) == core && level.getBlockState(marker).is(net.minecraft.world.level.block.Blocks.DIAMOND_BLOCK),
				"refusal does not replace the core or alter nearby blocks");
		helper.assertTrue(commands.execute("dronetest build 1", source) == 1, "original build command still builds the nearest saved tower");
		helper.assertTrue(core.layoutVersion() == TowerData.LEGACY_LAYOUT && core.tier() == 1, "original command keeps legacy geometry");
		helper.succeed();
	}

	@GameTest(structure = "jugcraft-test:drone_tower", maxTicks = 160, skyAccess = true)
	public void industrialBuildCommandBuildsTheIndustrialDesign(GameTestHelper helper)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		var level = helper.getLevel();
		BlockPos pos = helper.absolutePos(new BlockPos(48, 1, 48));
		level.setBlock(pos, JugcraftTower.CORE.defaultBlockState(), 3);
		var core = (TowerCoreBlockEntity) level.getBlockEntity(pos);
		var player = helper.makeMockServerPlayerInLevel();
		player.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
		core.setOwner(player.getUUID());
		var source = level.getServer().createCommandSourceStack().withEntity(player);
		int result = level.getServer().getCommands().getDispatcher().execute("dronetest build industrial 1", source);
		helper.assertTrue(result == 1 && core.tier() == 1 && core.layoutVersion() == TowerData.INDUSTRIAL_LAYOUT,
				"explicit command builds the industrial Command Post");
		helper.assertTrue(level.getBlockState(pos.offset(-22, 2, 0)).is(ExchangeStations.ENERGY), "industrial power station is present");
		helper.assertTrue(level.getBlockState(pos.offset(20, 2, 0)).is(ExchangeStations.CARGO), "industrial cargo station is present");
		helper.succeed();
	}

	@GameTest(structure = "jugcraft-test:drone_tower", maxTicks = 100, skyAccess = true)
	public void revisedBuildCommandPreservesPreviousIndustrialConstruction(GameTestHelper helper)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		var level = helper.getLevel(); BlockPos pos = helper.absolutePos(new BlockPos(48, 1, 48));
		level.setBlock(pos, JugcraftTower.CORE.defaultBlockState(), 3);
		var core = (TowerCoreBlockEntity) level.getBlockEntity(pos);
		var tag = core.saveWithoutMetadata(level.registryAccess());
		var player = helper.makeMockServerPlayerInLevel(); player.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
		var source = level.getServer().createCommandSourceStack().withEntity(player);
		for (int revision : new int[] {TowerData.FIRST_INDUSTRIAL_LAYOUT, TowerData.PREVIOUS_INDUSTRIAL_LAYOUT, TowerData.SERVICE_LAYOUT, TowerData.LARGE_INPUT_LAYOUT, TowerData.APRON_LAYOUT}) {
			tag.putInt("layout_version", revision); tag.putInt("tier", 1); tag.putInt("building", 2); tag.putIntArray("filled", new int[] {0, 4});
			core.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
			core.setOwner(player.getUUID());
			var frozen = core.layout(); var tile = frozen.tier(2).tiles.get(4);
			helper.assertTrue(level.getServer().getCommands().getDispatcher().execute("dronetest build industrial 9", source) == 0,
					"revised command refuses an existing industrial tower " + revision);
			helper.assertTrue(core.layoutVersion() == revision && core.layout() == frozen && core.tier() == 1
					&& core.building() == 2 && core.filledTiles().cardinality() == 2 && core.filledTiles().get(4)
					&& core.layout().tier(2).tiles.get(4) == tile,
					"frozen industrial geometry and unfinished tile indices remain intact " + revision);
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 200, skyAccess = true)
	public void industrialBuildCommandSpawnsFreshTower(GameTestHelper helper)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		var level = helper.getLevel();
		// The command searches neighbouring chunks, so isolate its fresh-site fixture from
		// other game-test arenas and load only the 5x5 chunks occupied by its ground floor.
		BlockPos pos = helper.absolutePos(new BlockPos(0, 1, 0)).offset(1_000_000, 0, 1_000_000);
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) level.getChunk((pos.getX() >> 4) + dx, (pos.getZ() >> 4) + dz);
		}
		var player = helper.makeMockServerPlayerInLevel();
		player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
		helper.assertFalse(level.getBlockEntity(pos) instanceof TowerCoreBlockEntity, "fresh command site has no core");
		var source = level.getServer().createCommandSourceStack().withEntity(player);
		int result = level.getServer().getCommands().getDispatcher().execute("dronetest build industrial 1", source);
		helper.assertTrue(result == 1 && level.getBlockEntity(pos) instanceof TowerCoreBlockEntity, "command creates the core at the fresh site");
		var core = (TowerCoreBlockEntity) level.getBlockEntity(pos);
		helper.assertTrue(core.layoutVersion() == TowerData.INDUSTRIAL_LAYOUT && core.tier() == 1 && player.getUUID().equals(core.owner()),
				"fresh command tower uses the industrial design and belongs to the caller");
		helper.assertTrue(level.getBlockState(pos.offset(-22, 2, 0)).is(ExchangeStations.ENERGY)
				&& level.getBlockState(pos.offset(20, 2, 0)).is(ExchangeStations.CARGO), "fresh command tower includes both industrial stations");
		helper.succeed();
	}

	@GameTest
	public void continuousMaterialsFollowWorldCoordinates(GameTestHelper helper) {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		var concrete = JugcraftTower.BLOCKS.get("reinforced_concrete").defaultBlockState();
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
	@GameTest
	public void legacySaveKeepsLayoutAndUnfinishedTiles(GameTestHelper helper) {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		var level = helper.getLevel();
		var state = JugcraftTower.CORE.defaultBlockState();
		var original = new TowerCoreBlockEntity(BlockPos.ZERO, state);
		helper.assertTrue(original.layoutVersion() == TowerData.INDUSTRIAL_LAYOUT, "new cores use industrial layout");
		original.deposit(new ItemStack(JugcraftTower.MODULE_ITEMS.get("structural_module"), 17));
		CompoundTag old = original.saveWithoutMetadata(level.registryAccess());
		old.remove("layout_version");
		old.putInt("tier", 2);
		old.putInt("building", 3);
		old.putIntArray("filled", new int[] {0, 2, 5});
		var loaded = new TowerCoreBlockEntity(BlockPos.ZERO, state);
		loaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), old));
		helper.assertTrue(loaded.layoutVersion() == TowerData.LEGACY_LAYOUT && loaded.layout() == TowerData.get(TowerData.LEGACY_LAYOUT), "old saves retain original geometry and tile ordering");
		helper.assertTrue(loaded.tier() == 2 && loaded.building() == 3 && loaded.filledTiles().cardinality() == 3 && loaded.filledTiles().get(5), "unfinished tile progress survives");
		helper.assertTrue(loaded.modules(0) == 17, "stored modules survive");
		var roundTrip = new TowerCoreBlockEntity(BlockPos.ZERO, state);
		roundTrip.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), loaded.saveWithoutMetadata(level.registryAccess())));
		helper.assertTrue(roundTrip.layoutVersion() == TowerData.LEGACY_LAYOUT && roundTrip.filledTiles().equals(loaded.filledTiles()), "version is explicit after saving again");
		var modern = new TowerCoreBlockEntity(BlockPos.ZERO, state);
		modern.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), original.saveWithoutMetadata(level.registryAccess())));
		helper.assertTrue(modern.layoutVersion() == TowerData.INDUSTRIAL_LAYOUT && modern.modules(0) == 17, "industrial save round-trip");
		for (int revision : new int[] {TowerData.FIRST_INDUSTRIAL_LAYOUT, TowerData.PREVIOUS_INDUSTRIAL_LAYOUT, TowerData.SERVICE_LAYOUT, TowerData.LARGE_INPUT_LAYOUT, TowerData.APRON_LAYOUT}) {
			old.putInt("layout_version", revision);
			var previousIndustrial = new TowerCoreBlockEntity(BlockPos.ZERO, state);
			previousIndustrial.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), old));
			var frozen = TowerData.get(revision); var savedTile = frozen.tier(3).tiles.get(5);
			helper.assertTrue(previousIndustrial.layoutVersion() == revision && previousIndustrial.layout() == frozen
					&& frozen != TowerData.get() && previousIndustrial.filledTiles().get(5),
					"earlier industrial saves retain their frozen tile order while new towers use layout 7: " + revision);
			var savedAgain = new TowerCoreBlockEntity(BlockPos.ZERO, state);
			savedAgain.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(),
					previousIndustrial.saveWithoutMetadata(level.registryAccess())));
			helper.assertTrue(savedAgain.layoutVersion() == revision && savedAgain.layout() == frozen
					&& savedAgain.building() == 3 && savedAgain.filledTiles().equals(previousIndustrial.filledTiles())
					&& savedAgain.layout().tier(3).tiles.get(5) == savedTile, "unfinished frozen tile addresses survive another save: " + revision);
			helper.assertTrue(frozen.tier(9).capacity == TowerData.get().tier(9).capacity, "revision preserves fleet capacity");
			String expectedFolder = "tower_industrial_v" + revision;
			helper.assertTrue(TowerData.previewFolder(revision).equals(expectedFolder), "saved tower status uses its frozen images");
			for (int tier = 0; tier <= 9; tier++) helper.assertTrue(TowerData.class.getResource(
					"/assets/jugcraft/textures/gui/" + expectedFolder + "/tier_" + tier + ".png") != null,
					"frozen status image exists for revision " + revision + " tier " + tier);
		}
		helper.assertTrue(TowerData.previewFolder(TowerData.INDUSTRIAL_LAYOUT).equals("tower_industrial")
				&& TowerData.previewFolder(TowerData.LEGACY_LAYOUT).equals("drone_tower"), "new and legacy status pictures retain their own paths");
		helper.succeed();
	}

	@GameTest(structure = "jugcraft-test:drone_tower", maxTicks = 160, skyAccess = true)
	public void tiersReservePadsAndCreativeDronesUseSpareBays(GameTestHelper helper) {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		var level = helper.getLevel();
		BlockPos pos = helper.absolutePos(new BlockPos(48, 1, 48));
		level.setBlock(pos, JugcraftTower.CORE.defaultBlockState(), 3);
		var core = (TowerCoreBlockEntity) level.getBlockEntity(pos);
		var player = helper.makeMockPlayer(GameType.CREATIVE);
		core.setOwner(player.getUUID());
		core.buildInstantly(level, 2);
		for (int[] placed : core.layout().tier(1).place) {
			BlockPos at = pos.offset(placed[0], placed[1], placed[2]);
			var built = level.getBlockState(at);
			if (built.getBlock() instanceof TowerPanelBlock) {
				helper.assertTrue(built == TowerPanelBlock.at(built, at), "authored material sheets use world alignment");
			}
		}
		DroneTerminalBlockEntity terminal = core.terminal();
		for (int i = 0; i < 32; i++) helper.assertTrue(terminal.linkDrone(player, DroneTier.COURIER_QUAD), "ground slot " + i);
		helper.assertFalse(terminal.linkDrone(player, DroneTier.COURIER_QUAD), "T1 cannot overflow into T2");
		helper.assertTrue(terminal.linkDrone(player, DroneTier.CREATIVE_DRONE), "creative drone can use a T2 bay");
		for (int i = 0; i < 23; i++) helper.assertTrue(terminal.linkDrone(player, DroneTier.SURVEY_HEXACOPTER), "T2 bay " + i);
		helper.assertFalse(terminal.linkDrone(player, DroneTier.SURVEY_HEXACOPTER), "filled tier refuses overflow");
		helper.assertTrue(terminal.dockOf(32).y > pos.getY() + 10, "creative drone occupies the remaining upper bay after regular drones");
		for (int i = 0; i < 32; i++) helper.assertTrue(terminal.dockOf(i).y < pos.getY() + 2, "T1 stays at ground level");
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
}
