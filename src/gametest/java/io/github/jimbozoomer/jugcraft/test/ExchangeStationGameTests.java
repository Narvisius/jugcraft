package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.JugcraftDrones;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.tower.ExchangePortBlock;
import io.github.jimbozoomer.jugcraft.tower.ExchangeStationBlock;
import io.github.jimbozoomer.jugcraft.tower.ExchangeStationBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.ExchangeStations;
import io.github.jimbozoomer.jugcraft.tower.JugcraftTower;
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Focused station behavior: rotations, drop integrity, transactions, identity links, legacy ports and persistence. */
public class ExchangeStationGameTests {
	@GameTest(structure = "jugcraft-test:drone_tower", skyAccess = true, maxTicks = 100)
	public void realEnergyCablesAndItemPipesUseOnlyMarkedSockets(GameTestHelper helper) {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		var level = helper.getLevel();
		BlockPos corePos = helper.absolutePos(new BlockPos(48, 1, 48));
		level.setBlock(corePos, JugcraftTower.CORE.defaultBlockState(), Block.UPDATE_ALL);
		var core = (TowerCoreBlockEntity) level.getBlockEntity(corePos);
		core.setOwner(helper.makeMockPlayer(GameType.CREATIVE).getUUID());
		core.buildInstantly(level, 1);
		var terminal = core.terminal(); terminal.rescanNow();
		BlockPos powerPort = corePos.offset(-22, 2, 0), cargoPort = corePos.offset(20, 2, 0);
		for (BlockPos master : new BlockPos[] {corePos.offset(-22, 1, 4), corePos.offset(20, 1, -4)}) {
			var state = level.getBlockState(master); var block = (ExchangeStationBlock) state.getBlock();
			helper.assertTrue(state.getValue(ExchangeStationBlock.LARGE) && block.footprint(state).size() == 270
					&& ((ExchangeStationBlockEntity) level.getBlockEntity(master)).formed(), "new tower builds each complete nine-by-six-by-five input machine");
		}
		helper.assertTrue(terminal.layout().pads().size() == 8 && terminal.activePickups().size() == 1
				&& terminal.packagerEntity().getBlockPos().equals(corePos.offset(13, 1, 11))
				&& terminal.getBlockPos().equals(corePos.offset(-3, 1, 0)), "larger housings preserve ground fleet, pickup, original store and terminal anchors");
		for (int i = 1; i <= 2; i++) {
			level.setBlock(powerPort.west(i), io.github.jimbozoomer.jugcraft.machine.JugcraftMachines.COPPER_CABLE.defaultBlockState(), Block.UPDATE_ALL);
			level.setBlock(cargoPort.east(i), io.github.jimbozoomer.jugcraft.logistics.JugcraftLogistics.BRASS_ITEM_PIPE.defaultBlockState(), Block.UPDATE_ALL);
		}
		helper.assertTrue(io.github.jimbozoomer.jugcraft.energy.CableBlock.connectsTo(level, powerPort.west(), Direction.EAST, level.getBlockState(powerPort)), "cable joins external energy socket");
		helper.assertFalse(io.github.jimbozoomer.jugcraft.energy.CableBlock.connectsTo(level, powerPort.east(), Direction.WEST, level.getBlockState(powerPort)), "cable does not pretend to join closed back face");
		helper.assertFalse(io.github.jimbozoomer.jugcraft.energy.CableBlock.connectsTo(level, cargoPort.east(), Direction.WEST, level.getBlockState(cargoPort)), "energy cable does not join cargo socket");
		helper.assertTrue(io.github.jimbozoomer.jugcraft.logistics.ItemPipeBlock.connectsTo(level, cargoPort.east(), Direction.WEST, level.getBlockState(cargoPort)), "item pipe joins marked cargo socket");
		helper.assertFalse(io.github.jimbozoomer.jugcraft.logistics.ItemPipeBlock.connectsTo(level, powerPort.west(), Direction.EAST, level.getBlockState(powerPort)), "item pipe does not join power station");
		for (Block cable : new Block[] {io.github.jimbozoomer.jugcraft.machine.JugcraftMachines.COPPER_CABLE,
				io.github.jimbozoomer.jugcraft.machine.JugcraftMachines.ALUMINUM_CABLE,
				io.github.jimbozoomer.jugcraft.machine.JugcraftMachines.SILVER_CABLE}) {
			for (int i = 1; i <= 2; i++) level.setBlock(powerPort.west(i), cable.defaultBlockState(), Block.UPDATE_ALL);
			terminal.energy().setAmount(0);
			var source = new io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage(10000, 10000, 10000, () -> { }); source.setAmount(10000);
			long rate = ((io.github.jimbozoomer.jugcraft.energy.CableBlock) cable).transferRate();
			long moved = io.github.jimbozoomer.jugcraft.energy.EnergyNetworks.pushToNeighbors(level, powerPort.west(3), source, 4096, java.util.List.of(Direction.EAST));
			helper.assertTrue(moved == rate && terminal.energy().getAmount() == rate && source.getAmount() == 10000 - rate,
					"each actual copper/aluminum/silver network powers the same socket at its own rate");
		}
		BlockPos chest = cargoPort.east(3); level.setBlock(chest, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
		var inventory = (net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(chest);
		inventory.setItem(0, new ItemStack(Items.STONE_BRICKS, 16));
		long sent = io.github.jimbozoomer.jugcraft.logistics.ItemNetworks.push(level, chest, Direction.WEST, ItemStorage.SIDED.find(level, chest, Direction.WEST), 16, java.util.Set.of(chest));
		helper.assertTrue(sent == 16 && inventory.getItem(0).isEmpty() && terminal.packagerEntity().count(stack -> stack.is(Items.STONE_BRICKS)) == 16,
				"real item network delivers to the tower's original packager inventory");
		helper.succeed();
	}
	private static final String ARENA = "jugcraft-test:drone_tower";

	@GameTest(structure = ARENA, skyAccess = true, maxTicks = 100)
	public void stationsRotateAndBreakAsOneItem(GameTestHelper helper) {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		var level = helper.getLevel();
		BlockPos master = helper.absolutePos(new BlockPos(15, 3, 15));
		for (ExchangeStationBlock block : new ExchangeStationBlock[] {ExchangeStations.ENERGY, ExchangeStations.CARGO}) for (boolean large : new boolean[] {false, true}) {
			for (Direction facing : Direction.Plane.HORIZONTAL) {
				var state = block.defaultBlockState().setValue(ExchangeStationBlock.FACING, facing).setValue(ExchangeStationBlock.LARGE, large);
				var footprint = block.footprint(state);
				helper.assertTrue(footprint.size() == (large ? 270 : block.energy ? 64 : 60), "saved size chooses its exact bounded part count");
				helper.assertTrue(block.portPart(state) == (large ? 58 : block.energy ? 26 : 22), "socket numbering remains size-specific");
				level.setBlock(master, state, Block.UPDATE_ALL);
				block.setPlacedBy(level, master, state, null, new ItemStack(block));
				var station = (ExchangeStationBlockEntity) level.getBlockEntity(master);
				helper.assertTrue(station != null && station.formed(), "every rotated MBS is fully assembled");
				BlockPos port = footprint.partPos(master, facing, block.portPart(state));
				helper.assertTrue(block.isPort(level.getBlockState(port), facing), "marked outward port rotates with station");
				helper.assertFalse(block.isPort(level.getBlockState(port), facing.getOpposite()), "rear of port is not external IO");
				helper.assertTrue(level.getBlockEntity(port) == null, "dummy port has no ticking entity");
				int before = drops(level, master, block);
				level.destroyBlock(port, true);
				helper.assertTrue(drops(level, master, block) == before + 1, "breaking a dummy part drops exactly one complete item");
				for (int part = 0; part < footprint.size(); part++) {
					helper.assertFalse(level.getBlockState(footprint.partPos(master, facing, part)).is(block),
							"all sibling parts are removed");
				}
			}
		}
		helper.succeed();
	}

	@GameTest(structure = ARENA, skyAccess = true, maxTicks = 100)
	public void savedStatesWithoutSizeStayLegacyWhileNormalPlacementBuildsNineBySixByFive(GameTestHelper helper) {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		var level = helper.getLevel(); var player = helper.makeMockPlayer(GameType.CREATIVE);
		BlockPos master = helper.absolutePos(new BlockPos(15, 12, 15));
		for (ExchangeStationBlock block : new ExchangeStationBlock[] {ExchangeStations.ENERGY, ExchangeStations.CARGO}) {
			var old = block.defaultBlockState().setValue(ExchangeStationBlock.FACING, Direction.WEST).setValue(ExchangeStationBlock.PART, block.portPart());
			var saved = NbtUtils.writeBlockState(old); saved.getCompound("properties").orElseThrow().remove("large");
			var decoded = NbtUtils.readBlockState(level.registryAccess().lookupOrThrow(Registries.BLOCK), saved);
			helper.assertTrue(decoded.is(block) && !decoded.getValue(ExchangeStationBlock.LARGE)
					&& decoded.getValue(ExchangeStationBlock.PART) == (block.energy ? 26 : 22)
					&& block.footprint(decoded).size() == (block.energy ? 64 : 60) && block.isPort(decoded, Direction.WEST),
					"actual saved-state reader preserves missing-size old footprint and socket numbering");
			var current = old.setValue(ExchangeStationBlock.LARGE, true).setValue(ExchangeStationBlock.PART, 58);
			helper.assertTrue(NbtUtils.readBlockState(level.registryAccess().lookupOrThrow(Registries.BLOCK), NbtUtils.writeBlockState(current)).equals(current),
					"explicit current size and part58 round-trip without reinterpreting old part numbers");
			for (Direction facing : Direction.Plane.HORIZONTAL) {
				player.setYRot(facing.getOpposite().toYRot());
				var context = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, new ItemStack(block),
						new BlockHitResult(Vec3.atCenterOf(master), Direction.UP, master, false));
				var placed = block.getStateForPlacement(context);
				helper.assertTrue(placed != null && placed.getValue(ExchangeStationBlock.LARGE) && placed.getValue(ExchangeStationBlock.FACING) == facing,
						"normal item placement selects the new large size in " + facing);
				var offsets = block.footprint(placed).offsets();
				var ghost = block.ghost(context);
				helper.assertTrue(ghost.fits() && ghost.cells().size() == 270
						&& ghost.cells().stream().allMatch(cell -> cell.state().getValue(ExchangeStationBlock.LARGE)),
						"placement preview shares all270 current-size cells with actual placement");
				helper.assertTrue(offsets.size() == 270 && offsets.stream().mapToInt(net.minecraft.core.Vec3i::getX).max().orElseThrow() == 8
						&& offsets.stream().mapToInt(net.minecraft.core.Vec3i::getZ).max().orElseThrow() == 5
						&& offsets.stream().mapToInt(net.minecraft.core.Vec3i::getY).max().orElseThrow() == 4,
						"large placement is exactly nine wide, six deep and five high");
				BlockPos far = block.footprint(placed).partPos(master, facing, 269);
				level.setBlock(far, Blocks.GLASS.defaultBlockState(), Block.UPDATE_ALL);
				helper.assertTrue(block.getStateForPlacement(context) == null && block.ghost(context).blocked().contains(far),
						"solid far-corner obstruction rejects placement and marks the same preview cell");
				level.removeBlock(far, false);
				var top = new BlockPos(master.getX(), level.getMaxY() - 3, master.getZ());
				var ceilingContext = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, new ItemStack(block),
						new BlockHitResult(Vec3.atCenterOf(top), Direction.UP, top, false));
				helper.assertTrue(block.getStateForPlacement(ceilingContext) == null, "five-high placement cannot cross the build-height boundary");
			}
		}
		helper.succeed();
	}
	@GameTest(structure = ARENA, skyAccess = true, maxTicks = 100)
	public void frozenIndustrialTowersKeepLegacyStationsBoundToTheirOriginalBuffers(GameTestHelper helper) {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		var level = helper.getLevel(); BlockPos corePos = helper.absolutePos(new BlockPos(48, 1, 48));
		level.setBlock(corePos, JugcraftTower.CORE.defaultBlockState(), Block.UPDATE_ALL);
		var core = (TowerCoreBlockEntity) level.getBlockEntity(corePos);
		var initial = core.saveWithoutMetadata(level.registryAccess()); var owner = java.util.UUID.randomUUID();
		for (int revision : new int[] {2, 3, 4}) {
			initial.putInt("layout_version", revision);
			core.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), initial));
			core.setOwner(owner); core.buildInstantly(level, 1); var terminal = core.terminal(); terminal.rescanNow();
			for (ExchangeStationBlock block : new ExchangeStationBlock[] {ExchangeStations.ENERGY, ExchangeStations.CARGO}) {
				BlockPos master = corePos.offset(ExchangeStations.masterOffset(block, false));
				var state = level.getBlockState(master); var station = (ExchangeStationBlockEntity) level.getBlockEntity(master);
				helper.assertTrue(state.is(block) && !state.getValue(ExchangeStationBlock.LARGE)
						&& block.footprint(state).size() == (block.energy ? 64 : 60) && station.formed() && station.terminal() == terminal,
						"frozen tower " + revision + " retains its original machine size and explicit endpoint");
				BlockPos port = block.footprint(state).partPos(master, state.getValue(ExchangeStationBlock.FACING), block.portPart(state));
				if (block.energy) {
					long before = terminal.energy().getAmount();
					try (var tx = Transaction.openOuter()) { helper.assertTrue(EnergyStorage.SIDED.find(level, port, Direction.WEST).insert(7, tx) == 7, "old energy socket accepts power"); tx.commit(); }
					helper.assertTrue(terminal.energy().getAmount() == before + 7, "old socket feeds the original buffer");
				} else {
					int before = terminal.packagerEntity().count(stack -> stack.is(Items.DIAMOND));
					try (var tx = Transaction.openOuter()) { helper.assertTrue(ItemStorage.SIDED.find(level, port, Direction.EAST).insert(ItemVariant.of(Items.DIAMOND), 3, tx) == 3, "old cargo socket accepts items"); tx.commit(); }
					helper.assertTrue(terminal.packagerEntity().count(stack -> stack.is(Items.DIAMOND)) == before + 3, "old socket feeds the original27-slot store");
				}
			}
			helper.assertTrue(core.layoutVersion() == revision && core.layout() == io.github.jimbozoomer.jugcraft.tower.TowerData.get(revision), "using old stations never upgrades the saved tower");
		}
		helper.succeed();
	}
	@GameTest(structure = ARENA, skyAccess = true, maxTicks = 100)
	public void mixedSizeSiblingDisconnectsBothOldAndLargeApiUntilExactStateIsRestored(GameTestHelper helper) {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		var level = helper.getLevel(); BlockPos corePos = helper.absolutePos(new BlockPos(48, 1, 48));
		level.setBlock(corePos, JugcraftTower.CORE.defaultBlockState(), Block.UPDATE_ALL);
		var core = (TowerCoreBlockEntity) level.getBlockEntity(corePos); core.setOwner(java.util.UUID.randomUUID()); core.buildInstantly(level, 1);
		var terminal = core.terminal(); terminal.rescanNow(); terminal.energy().setAmount(1000);
		var store = terminal.packagerEntity(); store.setItem(0, new ItemStack(Items.STONE_BRICKS, 16));
		BlockPos master = corePos.offset(12, 6, -10);
		for (ExchangeStationBlock block : new ExchangeStationBlock[] {ExchangeStations.ENERGY, ExchangeStations.CARGO}) for (boolean large : new boolean[] {false, true}) {
			var state = block.defaultBlockState().setValue(ExchangeStationBlock.LARGE, large);
			level.setBlock(master, state, Block.UPDATE_ALL); block.setPlacedBy(level, master, state, null, new ItemStack(block));
			var station = (ExchangeStationBlockEntity) level.getBlockEntity(master);
			helper.assertTrue(station.formed() && station.bind(core), "complete saved size links before corruption");
			var energy = station.energyStorage(); var cargo = station.cargoStorage();
			BlockPos sibling = block.footprint(state).partPos(master, Direction.NORTH, 1);
			var original = level.getBlockState(sibling);
			level.setBlock(sibling, original.setValue(ExchangeStationBlock.LARGE, !large), Block.UPDATE_ALL);
			helper.assertFalse(station.formed(), "same ID/facing/part with a foreign size is never a formed machine");
			helper.assertTrue(station.terminal() == null, "mixed size cannot inherit the saved endpoint authority");
			try (var tx = Transaction.openOuter()) {
				helper.assertTrue(energy.insert(1, tx) == 0 && cargo.extract(ItemVariant.of(Items.STONE_BRICKS), 1, tx) == 0,
						"cached energy/item handles refuse a mixed-size sibling"); tx.commit();
			}
			helper.assertTrue(terminal.energy().getAmount() == 1000 && store.count(stack -> true) == 16, "mixed-state rejection preserves original buffers");
			level.setBlock(sibling, original, Block.UPDATE_ALL);
			helper.assertTrue(station.formed() && station.terminal() == terminal && original.getValue(ExchangeStationBlock.LARGE) == large,
					"exact-state repair restores the existing size without upgrading a legacy machine");
			level.removeBlock(master, false);
		}
		helper.succeed();
	}
	@GameTest(structure = ARENA, skyAccess = true, maxTicks = 100)
	public void forgedLargeSocketOutsideLegacyFootprintCannotExposeItsEnergyBuffer(GameTestHelper helper) {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		var level = helper.getLevel(); BlockPos corePos = helper.absolutePos(new BlockPos(48, 1, 48));
		level.setBlock(corePos, JugcraftTower.CORE.defaultBlockState(), Block.UPDATE_ALL);
		var core = (TowerCoreBlockEntity) level.getBlockEntity(corePos); core.setOwner(java.util.UUID.randomUUID()); core.buildInstantly(level, 1);
		var terminal = core.terminal(); terminal.rescanNow(); terminal.energy().setAmount(1000);
		var block = ExchangeStations.ENERGY;
		BlockPos master = corePos.offset(-12, 6, -10);
		var legacy = block.defaultBlockState().setValue(ExchangeStationBlock.LARGE, false).setValue(ExchangeStationBlock.FACING, Direction.NORTH);
		level.setBlock(master, legacy, Block.UPDATE_ALL); block.setPlacedBy(level, master, legacy, null, new ItemStack(block));
		var station = (ExchangeStationBlockEntity) level.getBlockEntity(master);
		helper.assertTrue(station.formed() && station.bind(core), "complete legacy power station is bound before malformed parts are added");
		BlockPos actualPort = block.footprint(legacy).partPos(master, Direction.NORTH, block.portPart(legacy));
		for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST}) {
			var forged = block.defaultBlockState().setValue(ExchangeStationBlock.LARGE, true)
					.setValue(ExchangeStationBlock.FACING, facing).setValue(ExchangeStationBlock.PART, 58);
			BlockPos outside = block.footprint(forged).partPos(master, facing, block.portPart(forged));
			helper.assertFalse(block.footprint(legacy).offsets().stream().anyMatch(offset -> master.offset(offset).equals(outside)),
					"forged large socket is outside every real legacy cell");
			level.setBlock(outside, forged, Block.UPDATE_ALL);
			helper.assertTrue(block.masterPos(outside, forged).equals(master) && station.formed(),
					"regression preserves the old complete station while forged part computes its master position");
			helper.assertTrue(block.controller(level, outside, forged) == null && EnergyStorage.SIDED.find(level, outside, facing) == null,
					"foreign size or facing cannot expose the complete legacy master's buffer through a second socket");
			var genuine = EnergyStorage.SIDED.find(level, actualPort, Direction.NORTH);
			long before = terminal.energy().getAmount();
			try (var tx = Transaction.openOuter()) { helper.assertTrue(genuine != null && genuine.insert(7, tx) == 7, "original legacy socket still accepts power"); tx.commit(); }
			helper.assertTrue(terminal.energy().getAmount() == before + 7, "malformed neighboring part does not disable or copy the real buffer");
			level.removeBlock(outside, false);
			helper.assertTrue(station.formed(), "removing a malformed foreign-size part preserves every legacy sibling");
		}
		helper.succeed();
	}
	@GameTest(structure = ARENA, skyAccess = true, maxTicks = 100)
	public void stationLinksArePersistentAndCannotSwitchToForeignTerminals(GameTestHelper helper) {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		var level = helper.getLevel();
		var owner = helper.makeMockPlayer(GameType.CREATIVE);
		BlockPos corePos = helper.absolutePos(new BlockPos(48, 1, 48));
		level.setBlock(corePos, JugcraftTower.CORE.defaultBlockState(), Block.UPDATE_ALL);
		var core = (TowerCoreBlockEntity) level.getBlockEntity(corePos);
		core.setOwner(owner.getUUID());
		core.buildInstantly(level, 1);
		var terminal = core.terminal();
		helper.assertTrue(terminal != null, "tower terminal exists");
		terminal.rescanNow();
		BlockPos powerMaster = corePos.offset(-22, 1, 4);
		BlockPos powerPort = corePos.offset(-22, 2, 0);
		BlockPos cargoMaster = corePos.offset(20, 1, -4);
		BlockPos cargoPort = corePos.offset(20, 2, 0);
		var power = (ExchangeStationBlockEntity) level.getBlockEntity(powerMaster);
		var cargo = (ExchangeStationBlockEntity) level.getBlockEntity(cargoMaster);
		helper.assertTrue(power != null && cargo != null && power.terminal() == terminal && cargo.terminal() == terminal,
				"automatic stations are explicitly pinned to their own terminal");
		var energy = EnergyStorage.SIDED.find(level, powerPort, Direction.WEST);
		helper.assertTrue(energy != null && EnergyStorage.SIDED.find(level, powerPort, Direction.EAST) == null,
				"only external face is a power port");
		terminal.energy().setAmount(500);
		try (var tx = Transaction.openOuter()) { energy.insert(1000, tx); }
		helper.assertTrue(terminal.energy().getAmount() == 500, "aborted power transaction rolls back");
		try (var tx = Transaction.openOuter()) { energy.insert(1000, tx); tx.commit(); }
		helper.assertTrue(terminal.energy().getAmount() == 1500, "committed power reaches original buffer");
		var items = ItemStorage.SIDED.find(level, cargoPort, Direction.EAST);
		helper.assertTrue(items != null, "formed cargo MBS exposes original packager");
		try (var tx = Transaction.openOuter()) {
			helper.assertTrue(items.insert(ItemVariant.of(Items.STONE_BRICKS), 16, tx) == 16, "cargo transfer accepts materials");
			tx.commit();
		}
		var packager = terminal.packagerEntity();
		var cachedView = items.nonEmptyViews().iterator().next();
		helper.assertTrue(packager != null && packager.count(stack -> stack.is(Items.STONE_BRICKS)) == 16,
				"station has no duplicate inventory");
		// A physically nearer foreign depot must not hijack the pinned station.
		BlockPos foreignPos = powerPort.offset(-2, 0, 0);
		level.setBlock(foreignPos, JugcraftDrones.TERMINAL.defaultBlockState(), Block.UPDATE_ALL);
		var foreign = (DroneTerminalBlockEntity) level.getBlockEntity(foreignPos);
		foreign.setOwner(java.util.UUID.randomUUID());
		helper.assertTrue(power.terminal() == terminal, "nearby foreign terminal never changes explicit link");
		var saved = power.saveWithoutMetadata(level.registryAccess());
		var reloaded = new ExchangeStationBlockEntity(powerMaster, power.getBlockState());
		reloaded.setLevel(level);
		reloaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
		helper.assertTrue(reloaded.terminal() == terminal, "station identity survives save/load");
		level.destroyBlock(cargoPort, true);
		helper.assertTrue(ItemStorage.SIDED.find(level, cargoPort, Direction.EAST) == null, "broken station disconnects immediately");
		try (var tx = Transaction.openOuter()) {
			helper.assertTrue(items.insert(ItemVariant.of(Items.STONE_BRICKS), 1, tx) == 0
					&& cachedView.extract(ItemVariant.of(Items.STONE_BRICKS), 1, tx) == 0,
					"cached cargo storage and views cannot bypass broken MBS");
			tx.commit();
		}
		helper.assertTrue(packager.count(stack -> stack.is(Items.STONE_BRICKS)) == 16 && terminal.energy().getAmount() == 1500,
				"breaking cargo MBS preserves items and energy");
		// Legacy one-block ports continue to work; they are not replaced or enlarged in old saves.
		BlockPos legacy = corePos.offset(-10, 2, -9);
		level.setBlock(legacy, ExchangePortBlock.ENERGY.defaultBlockState(), Block.UPDATE_ALL);
		helper.assertTrue(EnergyStorage.SIDED.find(level, legacy, Direction.UP) != null, "legacy energy port still works");
		level.setBlock(legacy, ExchangePortBlock.CARGO.defaultBlockState(), Block.UPDATE_ALL);
		helper.assertTrue(ItemStorage.SIDED.find(level, legacy, Direction.UP) != null, "legacy cargo port still works");
		// Replacing the original terminal (even with the same owner) does not reuse its depot identity.
		BlockPos terminalPos = terminal.getBlockPos();
		level.removeBlock(terminalPos, false);
		level.setBlock(terminalPos, JugcraftDrones.TERMINAL.defaultBlockState(), Block.UPDATE_ALL);
		var replacement = (DroneTerminalBlockEntity) level.getBlockEntity(terminalPos);
		replacement.setOwner(owner.getUUID());
		replacement.linkTower(corePos);
		helper.assertTrue(power.terminal() == null, "a replaced terminal requires explicit owner relinking");
		try (var tx = Transaction.openOuter()) {
			helper.assertTrue(energy.insert(1000, tx) == 0, "cached power handle refuses replacement terminal");
			tx.commit();
		}
		helper.succeed();
	}

	@GameTest(structure = ARENA, skyAccess = true, maxTicks = 100)
	public void cargoScreenUsesStationDistanceAndClosesWhenItsLinkChanges(GameTestHelper helper) {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		var level = helper.getLevel();
		var owner = helper.makeMockPlayer(GameType.CREATIVE);
		BlockPos corePos = helper.absolutePos(new BlockPos(48, 1, 48));
		level.setBlock(corePos, JugcraftTower.CORE.defaultBlockState(), Block.UPDATE_ALL);
		var core = (TowerCoreBlockEntity) level.getBlockEntity(corePos);
		core.setOwner(owner.getUUID());
		core.buildInstantly(level, 1);
		var terminal = core.terminal();
		helper.assertTrue(terminal != null, "tower terminal exists");
		terminal.rescanNow();
		BlockPos cargoPort = corePos.offset(20, 2, 0);
		var cargo = (ExchangeStationBlockEntity) level.getBlockEntity(corePos.offset(20, 1, -4));
		var packager = terminal.packagerEntity();
		helper.assertTrue(cargo != null && packager != null, "station and original packager exist");
		owner.setPos(cargoPort.getX() + 1.5, cargoPort.getY() + .5, cargoPort.getZ() + .5);
		helper.assertFalse(packager.stillValid(owner), "regression setup: marked station port is outside original packager menu range");
		var menu = cargo.cargoMenu(1, owner.getInventory(), owner, cargoPort);
		helper.assertTrue(menu != null && menu.stillValid(owner), "cargo screen remains open beside station port");
		helper.assertTrue(menu.getContainer() == packager, "screen uses original inventory, no proxy store");
		menu.getSlot(0).set(new ItemStack(Items.STONE_BRICKS, 16));
		helper.assertTrue(packager.getItem(0).getCount() == 16, "screen slot writes original cargo packager");
		var stranger = helper.makeMockPlayer(GameType.CREATIVE);
		stranger.setPos(owner.position());
		helper.assertFalse(menu.stillValid(stranger), "foreign player cannot keep station screen open");
		helper.assertTrue(cargo.cargoMenu(2, stranger.getInventory(), stranger, cargoPort) == null, "foreign player cannot open cargo screen");
		owner.setPos(cargoPort.getX() + 9.5, cargoPort.getY() + .5, cargoPort.getZ() + .5);
		helper.assertFalse(menu.stillValid(owner), "walking more than eight blocks from station closes screen");
		owner.setPos(cargoPort.getX() + 1.5, cargoPort.getY() + .5, cargoPort.getZ() + .5);
		BlockPos packagerPos = packager.getBlockPos();
		level.removeBlock(packagerPos, false);
		level.setBlock(packagerPos, JugcraftDrones.CARGO_PACKAGER.defaultBlockState(), Block.UPDATE_ALL);
		helper.assertFalse(menu.stillValid(owner), "replaced packager invalidates previously opened cargo screen");
		var replacementPackager = terminal.packagerEntity();
		var replacementMenu = cargo.cargoMenu(3, owner.getInventory(), owner, cargoPort);
		helper.assertTrue(replacementPackager != null && replacementMenu != null && replacementMenu.stillValid(owner),
				"owner may open fresh screen for explicitly linked depot's current packager");
		replacementMenu.getSlot(0).set(new ItemStack(Items.STONE_BRICKS, 16));
		BlockPos terminalPos = terminal.getBlockPos();
		level.removeBlock(terminalPos, false);
		level.setBlock(terminalPos, JugcraftDrones.TERMINAL.defaultBlockState(), Block.UPDATE_ALL);
		var replacementTerminal = (DroneTerminalBlockEntity) level.getBlockEntity(terminalPos);
		replacementTerminal.setOwner(owner.getUUID());
		replacementTerminal.linkTower(corePos);
		helper.assertFalse(replacementMenu.stillValid(owner), "replacement terminal identity closes cargo screen");
		helper.assertTrue(cargo.bind(core), "owner's station can be explicitly rebound");
		var reboundMenu = cargo.cargoMenu(4, owner.getInventory(), owner, cargoPort);
		helper.assertTrue(reboundMenu != null && reboundMenu.stillValid(owner), "new explicitly bound screen is valid");
		level.destroyBlock(cargoPort, true);
		helper.assertFalse(reboundMenu.stillValid(owner), "breaking station closes cargo screen immediately");
		helper.assertTrue(replacementPackager.getItem(0).getCount() == 16, "station teardown preserves original inventory");
		helper.succeed();
	}

	@GameTest(structure = ARENA, skyAccess = true, maxTicks = 100)
	public void explosionCannotDuplicateAStationItem(GameTestHelper helper) {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		var level = helper.getLevel();
		BlockPos master = helper.absolutePos(new BlockPos(15, 3, 15));
		ExchangeStationBlock block = ExchangeStations.CARGO;
		var state = block.defaultBlockState().setValue(ExchangeStationBlock.LARGE, true);
		level.setBlock(master, state, Block.UPDATE_ALL);
		block.setPlacedBy(level, master, state, null, new ItemStack(block));
		level.explode(null, master.getX() + 2.5, master.getY() + 1.5, master.getZ() + 1.5, 5F, Level.ExplosionInteraction.TNT);
		helper.assertTrue(drops(level, master, block) <= 1, "one exploding large MBS yields at most one item, never270 dummy drops");
		helper.succeed();
	}

	private static int drops(net.minecraft.server.level.ServerLevel level, BlockPos master, Block block) {
		return level.getEntitiesOfClass(ItemEntity.class, new AABB(master).inflate(10),
				item -> item.getItem().is(block.asItem())).stream().mapToInt(item -> item.getItem().getCount()).sum();
	}
}
