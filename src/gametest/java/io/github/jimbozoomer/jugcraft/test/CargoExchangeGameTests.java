package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.drone.CargoPackagerBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DepotView;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.logistics.JugcraftLogistics;
import io.github.jimbozoomer.jugcraft.logistics.ItemNetworks;
import io.github.jimbozoomer.jugcraft.logistics.ItemPipeBlock;
import io.github.jimbozoomer.jugcraft.logistics.PneumaticExtractorBlock;
import io.github.jimbozoomer.jugcraft.energy.CableBlock;
import io.github.jimbozoomer.jugcraft.tower.ExchangeStationBlock;
import io.github.jimbozoomer.jugcraft.tower.ExchangeStationBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.ExchangeStations;
import io.github.jimbozoomer.jugcraft.tower.JugcraftTower;
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/** Aggregate telemetry from the original inventory, including real scheduled routing and backpressure. */
public class CargoExchangeGameTests {
	private static TowerCoreBlockEntity tower(GameTestHelper h) {
		var level = h.getLevel(); BlockPos at = h.absolutePos(new BlockPos(48, 1, 48));
		level.setBlock(at, JugcraftTower.CORE.defaultBlockState(), Block.UPDATE_ALL);
		var core = (TowerCoreBlockEntity) level.getBlockEntity(at); core.setOwner(UUID.randomUUID()); core.buildInstantly(level, 1);
		core.terminal().rescanNow(); return core;
	}
	private static DepotView snapshot(GameTestHelper h, DroneTerminalBlockEntity terminal) {
		return DepotView.read(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(),
				terminal.getUpdateTag(h.getLevel().registryAccess())).child("view").orElseThrow());
	}
	private static void stock(GameTestHelper h, DroneTerminalBlockEntity terminal, int items, int occupied) {
		var cargo = snapshot(h, terminal).cargo;
		h.assertTrue(cargo != null && cargo.items() == items && cargo.occupiedSlots() == occupied && cargo.totalSlots() == 27
				&& cargo.packagerPos().equals(terminal.packagerEntity().getBlockPos()), "snapshot is the exact original store: " + items + " items in " + occupied + "/27 slots");
	}
	@GameTest(structure = "jugcraft-test:drone_tower", skyAccess = true, maxTicks = 100)
	public void mixedStackSizesAndDroneTakeReturnProduceExactSnapshotCounts(GameTestHelper h) {
		if (TestFocus.skip("tower")) { h.succeed(); return; }
		var core = tower(h); var terminal = core.terminal(); var store = terminal.packagerEntity();
		store.setItem(0, new ItemStack(Items.STONE_BRICKS, 64)); store.setItem(8, new ItemStack(Items.DIAMOND, 17));
		store.setItem(26, new ItemStack(Items.IRON_SWORD)); stock(h, terminal, 82, 3);
		var one = store.takeOne(stack -> stack.is(Items.DIAMOND));
		h.assertTrue(one.getCount() == 1, "real drone extraction removes one matching item"); stock(h, terminal, 81, 3);
		h.assertTrue(store.giveBack(new ItemStack(Items.DIAMOND, 5)).isEmpty(), "real cargo return accepts its matching stack");
		stock(h, terminal, 86, 4); // giveBack first uses the earlier empty slot, rather than compacting the inventory.
		store.setUnlimited(true); var original = snapshot(h, terminal);
		h.assertTrue(original.cargo.unlimited(), "development unlimited supply is explicit, not fake stocked items");
		var out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, h.getLevel().registryAccess()); original.write(out);
		var reloaded = DepotView.read(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), out.buildResult()));
		h.assertTrue(original.cargo.equals(reloaded.cargo) && original.depotId.equals(reloaded.depotId)
				&& original.energy == reloaded.energy && original.capacity == reloaded.capacity,
				"optional cargo child round-trips with exact depot and unchanged power snapshot");
		h.assertTrue(reloaded.cargo.totalSlots() == CargoPackagerBlockEntity.SIZE && store.getItem(26).getMaxStackSize() == 1,
				"slots represent actual occupancy, not a fictitious all-64-item capacity");
		h.succeed();
	}
	@GameTest(structure = "jugcraft-test:drone_tower", skyAccess = true, maxTicks = 100)
	public void scheduledExtractorWaitsForReal27SlotStoreSpaceWithoutLosingItems(GameTestHelper h) {
		if (TestFocus.skip("tower")) { h.succeed(); return; }
		var core = tower(h); var terminal = core.terminal(); var store = terminal.packagerEntity(); var level = h.getLevel();
		for (int i = 0; i < 27; i++) store.setItem(i, new ItemStack(Items.STONE_BRICKS, 64));
		BlockPos port = core.getBlockPos().offset(20, 2, 0), chestPos = port.east(4);
		level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
		var chest = (ChestBlockEntity) level.getBlockEntity(chestPos); chest.setItem(0, new ItemStack(Items.STONE_BRICKS, 16));
		for (int distance = 1; distance <= 2; distance++) level.setBlock(port.east(distance), JugcraftLogistics.BRASS_ITEM_PIPE.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(port.east(3), JugcraftLogistics.PNEUMATIC_EXTRACTOR.defaultBlockState().setValue(PneumaticExtractorBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
		int[] held = {0};
		h.runAtTickTime(20, () -> {
			stock(h, terminal, 1728, 27);
			h.assertTrue(chest.getItem(0).getCount() == 16, "scheduled extractor leaves source untouched while the full store applies backpressure");
			held[0] += store.removeItem(0, 4).getCount();
		});
		h.runAtTickTime(40, () -> {
			stock(h, terminal, 1728, 27);
			h.assertTrue(chest.getItem(0).getCount() == 12 && held[0] == 4,
					"scheduled real extractor/pipe accepts only the four available stack spaces");
			held[0] += store.removeItem(26, 64).getCount();
		});
		h.runAtTickTime(64, () -> {
			stock(h, terminal, 1676, 27);
			h.assertTrue(chest.isEmpty() && held[0] == 68 && store.count(stack -> true) + held[0] == 1728 + 16,
					"scheduled transfer resumes into an empty slot and conserves every source, stored and deliberately removed item");
			h.succeed();
		});
	}
	@GameTest(structure = "jugcraft-test:drone_tower", skyAccess = true, maxTicks = 100)
	public void normalItemPipeVariantsReachEveryRotatedSocketAndDisconnectSafely(GameTestHelper h) {
		if (TestFocus.skip("tower")) { h.succeed(); return; }
		var core = tower(h); var terminal = core.terminal(); var store = terminal.packagerEntity(); var level = h.getLevel();
		var pipes = BuiltInRegistries.BLOCK.stream().filter(block -> block instanceof ItemPipeBlock).toList();
		h.assertTrue(pipes.contains(JugcraftLogistics.BRASS_ITEM_PIPE) && pipes.size() >= 2,
				"registered item carriers include ordinary brass and its wall run");
		BlockPos master = core.getBlockPos().offset(12, 5, -10);
		for (boolean large : new boolean[] {false, true}) for (Block pipe : pipes) for (Direction facing : Direction.Plane.HORIZONTAL) {
			store.clearContent();
			var state = ExchangeStations.CARGO.defaultBlockState().setValue(ExchangeStationBlock.FACING, facing).setValue(ExchangeStationBlock.LARGE, large);
			BlockPos port = ExchangeStations.CARGO.footprint(state).partPos(master, facing, ExchangeStations.CARGO.portPart(state));
			BlockPos tube = port.relative(facing), chestPos = port.relative(facing, 2);
			// Build the carrier first: normal station placement must update its actual connection state.
			level.setBlock(tube, pipe.defaultBlockState(), Block.UPDATE_ALL);
			level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
			var source = (ChestBlockEntity) level.getBlockEntity(chestPos); source.setItem(0, new ItemStack(Items.STONE_BRICKS, 16));
			level.setBlock(master, state, Block.UPDATE_ALL);
			ExchangeStations.CARGO.setPlacedBy(level, master, state, null, new ItemStack(ExchangeStations.CARGO));
			var station = (ExchangeStationBlockEntity) level.getBlockEntity(master);
			h.assertTrue(station.formed() && station.bind(core), "rotated socket belongs to its real tower: " + facing);
			var connected = level.getBlockState(tube);
			h.assertTrue(connected.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(facing.getOpposite())),
					"ordinary neighbor update points " + BuiltInRegistries.BLOCK.getKey(pipe) + " into " + facing + " socket");
			var tubeBox = connected.getShape(level, tube).bounds().move(tube);
			var socketBox = level.getBlockState(port).getShape(level, port).bounds().move(port);
			double tubeFace = facing == Direction.NORTH ? tubeBox.maxZ : facing == Direction.SOUTH ? tubeBox.minZ
					: facing == Direction.WEST ? tubeBox.maxX : tubeBox.minX;
			double socketFace = facing == Direction.NORTH ? socketBox.minZ : facing == Direction.SOUTH ? socketBox.maxZ
					: facing == Direction.WEST ? socketBox.minX : socketBox.maxX;
			h.assertTrue(Math.abs(tubeFace - socketFace) < 1E-6, "real carrier and integrated socket meet at the shared block face");
			h.assertFalse(CableBlock.connectsTo(level, tube, facing.getOpposite(), level.getBlockState(port)),
					"copper energy cable does not pretend to be an item connection");
			var inventory = ItemStorage.SIDED.find(level, port, facing);
			h.assertTrue(inventory != null && ItemStorage.SIDED.find(level, port, facing.getOpposite()) == null,
					"only the outward cargo socket exposes the original store");
			long delivered = ItemNetworks.push(level, chestPos, facing.getOpposite(),
					ItemStorage.SIDED.find(level, chestPos, facing.getOpposite()), 16, java.util.Set.of(chestPos));
			h.assertTrue(delivered == 16 && source.isEmpty() && store.count(stack -> stack.is(Items.STONE_BRICKS)) == 16,
					"real item network transfers through each carrier and rotation without duplicating stock");
			try (var tx = Transaction.openOuter()) {
				h.assertTrue(inventory.extract(ItemVariant.of(Items.STONE_BRICKS), 5, tx) == 5, "outward extraction is supported");
			}
			h.assertTrue(store.count(stack -> true) == 16, "aborted cargo extraction rolls back");
			try (var tx = Transaction.openOuter()) {
				h.assertTrue(inventory.extract(ItemVariant.of(Items.STONE_BRICKS), 5, tx) == 5, "committed extraction takes exactly five"); tx.commit();
			}
			h.assertTrue(store.count(stack -> true) == 11, "original store plus five extracted items conserves all sixteen");
			level.removeBlock(master, false);
			h.assertFalse(level.getBlockState(tube).getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(facing.getOpposite())),
					"real carrier disconnects when the formed housing is removed");
			try (var tx = Transaction.openOuter()) {
				h.assertTrue(inventory.extract(ItemVariant.of(Items.STONE_BRICKS), 1, tx) == 0, "cached socket cannot extract after teardown"); tx.commit();
			}
			level.removeBlock(tube, false); level.removeBlock(chestPos, false);
		}
		h.succeed();
	}
	@GameTest(structure = "jugcraft-test:drone_tower", skyAccess = true, maxTicks = 100)
	public void olderSnapshotAndMissingStoreRemainUnknownRatherThanFakeEmpty(GameTestHelper h) {
		if (TestFocus.skip("tower")) { h.succeed(); return; }
		var registries = h.getLevel().registryAccess(); var old = new net.minecraft.nbt.CompoundTag(); old.putBoolean("packager", true);
		h.assertTrue(DepotView.read(TagValueInput.create(ProblemReporter.DISCARDING, registries, old)).cargo == null,
				"older snapshot is explicitly unknown, not an invented zero-item store");
		old.put("cargo", new net.minecraft.nbt.CompoundTag());
		var incomplete = DepotView.read(TagValueInput.create(ProblemReporter.DISCARDING, registries, old)).cargo;
		h.assertTrue(incomplete.items() == -1 && incomplete.occupiedSlots() == -1 && incomplete.totalSlots() == -1,
				"incomplete optional fields preserve unknown counts");
		var core = tower(h); var terminal = core.terminal(); stock(h, terminal, 0, 0);
		BlockPos store = terminal.packagerEntity().getBlockPos(); h.getLevel().removeBlock(store, false);
		var missing = snapshot(h, terminal).cargo;
		h.assertTrue(missing.packagerPos() == null && missing.items() == -1 && missing.occupiedSlots() == -1 && missing.totalSlots() == -1,
				"removed cached store reports unavailable with no stale stock counts");
		h.succeed();
	}
}
