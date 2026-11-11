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
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
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
	/** A Tri-Pillar built to tier 1, its depot rescanned (the packager in dispatch is the store). */
	private static TowerCoreBlockEntity tower(GameTestHelper h) {
		return ExchangeStationGameTests.tower(h);
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
	@GameTest(structure = TriPillarArena.ARENA, skyAccess = true, maxTicks = 100)
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
	@GameTest(structure = TriPillarArena.ARENA, skyAccess = true, maxTicks = 100)
	public void scheduledExtractorWaitsForReal27SlotStoreSpaceWithoutLosingItems(GameTestHelper h) {
		if (TestFocus.skip("tower")) { h.succeed(); return; }
		var core = tower(h); var terminal = core.terminal(); var store = terminal.packagerEntity(); var level = h.getLevel();
		for (int i = 0; i < 27; i++) store.setItem(i, new ItemStack(Items.STONE_BRICKS, 64));
		// The Tri-Pillar's own Cargo Exchange socket, facing east out over the apron: pipes, extractor and chest run east of it.
		BlockPos port = TriPillarArena.port(level, TriPillarArena.master(core, false)), chestPos = port.east(4);
		h.assertTrue(level.getBlockState(port).getValue(ExchangeStationBlock.FACING) == Direction.EAST, "the cargo socket faces east");
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
	@GameTest(structure = TriPillarArena.ARENA, skyAccess = true, maxTicks = 100)
	public void normalItemPipeVariantsReachEveryRotatedSocketAndDisconnectSafely(GameTestHelper h) {
		if (TestFocus.skip("tower")) { h.succeed(); return; }
		var core = tower(h); var terminal = core.terminal(); var store = terminal.packagerEntity(); var level = h.getLevel();
		var pipes = BuiltInRegistries.BLOCK.stream().filter(block -> block instanceof ItemPipeBlock).toList();
		h.assertTrue(pipes.contains(JugcraftLogistics.BRASS_ITEM_PIPE) && pipes.size() >= 2,
				"registered item carriers include ordinary brass and its wall run");
		// Open air east of the building (the old yard) within the terminal's reach: every rotation of both sizes, its carrier and its chest fit there.
		BlockPos master = TriPillarArena.at(core, ExchangeStationGameTests.YARD_AIR);
		for (boolean large : new boolean[] {false, true}) for (Block pipe : pipes) for (Direction facing : Direction.Plane.HORIZONTAL) {
			store.clearContent();
			var state = ExchangeStations.CARGO.defaultBlockState().setValue(ExchangeStationBlock.FACING, facing).setValue(ExchangeStationBlock.LARGE, large);
			BlockPos port = ExchangeStations.CARGO.footprint(state).partPos(master, facing, ExchangeStations.CARGO.portPart(state));
			BlockPos tube = port.relative(facing), chestPos = port.relative(facing, 2);
			// Build the carrier first: normal station placement must update its actual connection state. (A directional
			// carrier, Jimtafacts' iron pipe, has its outlet turned to the socket, as a player would turn it.)
			var carrier = pipe.defaultBlockState();
			if (carrier.hasProperty(net.minecraft.world.level.block.DirectionalBlock.FACING))
				carrier = carrier.setValue(net.minecraft.world.level.block.DirectionalBlock.FACING, facing.getOpposite());
			level.setBlock(tube, carrier, Block.UPDATE_ALL);
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
			// a carrier that only hands on to other pipes (Jimtafacts' sandstone, as BuildCraft's), or one that turns uncoloured
			// items away (the daizuli and emzuli pipes, until set up), never fills a store from a plain chest
			var route = (ItemPipeBlock) pipe;
			if (!route.routesTo(level.getBlockState(tube), facing.getOpposite(), level.getBlockState(port))
					|| route.routeColor(level, tube, level.getBlockState(tube), ItemVariant.of(Items.STONE_BRICKS), -1) == -2) {
				level.removeBlock(master, false); level.removeBlock(tube, false); level.removeBlock(chestPos, false);
				continue;
			}
			// a slow carrier (the cobblestone pipes) moves less a push: push until all sixteen have gone (or nothing moves)
			long delivered = 0;
			for (int push = 0; push < 16 && delivered < 16; push++) {
				long moved = ItemNetworks.push(level, chestPos, facing.getOpposite(),
						ItemStorage.SIDED.find(level, chestPos, facing.getOpposite()), 16 - delivered, java.util.Set.of(chestPos));
				if (moved <= 0) break;
				delivered += moved;
			}
			h.assertTrue(delivered == 16 && source.isEmpty() && store.count(stack -> stack.is(Items.STONE_BRICKS)) == 16,
					"real item network transfers through each carrier and rotation without duplicating stock: " + BuiltInRegistries.BLOCK.getKey(pipe)
							+ " " + facing + (large ? " large" : " small") + ": delivered " + delivered + ", left " + source.getItem(0).getCount() + ", stored "
							+ store.count(stack -> stack.is(Items.STONE_BRICKS)));
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
	@GameTest(structure = TriPillarArena.ARENA, skyAccess = true, maxTicks = 100)
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

	/**
	 * The drop pods are bought, and the tiers paid for, from what's hooked up to the tower's Cargo Exchange (Justin, 10
	 * November 2026: the drop pod screen said he hadn't the resources "even though I have a chest filled with everything
	 * that's needed ... attached via pipes"; his pick: "Read through pipes"): a station in the yard bound to the tower, a
	 * run of brass pipe from its port to a chest at the far end and past a double chest (both its halves on the pipes).
	 * The tower counts all of it, each chest once, with nothing pushed into it, and a squad bought takes its price from
	 * those chests.
	 */
	@GameTest(structure = TriPillarArena.ARENA, skyAccess = true, maxTicks = 100)
	public void dropPodsArePaidFromChestsPipedToTheCargoExchange(GameTestHelper h) {
		if (TestFocus.skip("tower")) { h.succeed(); return; }
		var core = tower(h); var terminal = core.terminal(); var level = h.getLevel();
		terminal.packagerEntity().clearContent();
		BlockPos master = TriPillarArena.at(core, ExchangeStationGameTests.YARD_AIR);
		Direction facing = Direction.EAST;
		var state = ExchangeStations.CARGO.defaultBlockState().setValue(ExchangeStationBlock.FACING, facing).setValue(ExchangeStationBlock.LARGE, false);
		BlockPos port = ExchangeStations.CARGO.footprint(state).partPos(master, facing, ExchangeStations.CARGO.portPart(state));
		for (int i = 1; i <= 3; i++) level.setBlock(port.relative(facing, i), JugcraftLogistics.BRASS_ITEM_PIPE.defaultBlockState(), Block.UPDATE_ALL);
		BlockPos far = port.relative(facing, 4);
		level.setBlock(far, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
		// a double chest along the pipe's north side, both halves touching it (west half RIGHT, east half LEFT, facing south)
		BlockPos west = port.relative(facing, 2).north(), east = port.relative(facing, 3).north();
		level.setBlock(west, Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.FACING, Direction.SOUTH)
				.setValue(net.minecraft.world.level.block.ChestBlock.TYPE, net.minecraft.world.level.block.state.properties.ChestType.RIGHT), Block.UPDATE_ALL);
		level.setBlock(east, Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.FACING, Direction.SOUTH)
				.setValue(net.minecraft.world.level.block.ChestBlock.TYPE, net.minecraft.world.level.block.state.properties.ChestType.LEFT), Block.UPDATE_ALL);
		level.setBlock(master, state, Block.UPDATE_ALL);
		ExchangeStations.CARGO.setPlacedBy(level, master, state, null, new ItemStack(ExchangeStations.CARGO));
		var station = (ExchangeStationBlockEntity) level.getBlockEntity(master);
		h.assertTrue(station.formed() && station.bind(core), "the yard's station is the tower's");
		var chest = (ChestBlockEntity) level.getBlockEntity(far);
		chest.setItem(0, new ItemStack(Items.GOLD_INGOT, 64));
		chest.setItem(1, new ItemStack(Items.DIAMOND, 64));
		chest.setItem(2, new ItemStack(Items.GOLD_INGOT, 64));
		var left = (ChestBlockEntity) level.getBlockEntity(west);
		var right = (ChestBlockEntity) level.getBlockEntity(east);
		for (int i = 0; i < 5; i++) left.setItem(i, new ItemStack(Items.IRON_INGOT, 64));
		right.setItem(0, new ItemStack(Items.COPPER_INGOT, 64));
		right.setItem(1, new ItemStack(Items.COPPER_INGOT, 64));
		right.setItem(4, new ItemStack(Items.COPPER_INGOT, 64));
		right.setItem(5, new ItemStack(Items.COPPER_INGOT, 64));
		right.setItem(2, new ItemStack(Items.DIAMOND, 1).copyWithCount(1));
		// (an enchanted one doesn't count: only plain items, as the tower storage keeps)
		var named = new ItemStack(Items.DIAMOND, 5);
		named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Shiny"));
		right.setItem(3, named);
		var hooked = io.github.jimbozoomer.jugcraft.tower.TowerSupply.hooked(level, core, terminal, terminal.packagerEntity());
		h.assertTrue(hooked.storages().size() >= 3 && hooked.warehouses().isEmpty(),
				"the far chest and both halves of the double chest are found through the pipes: " + hooked.storages().size());
		int[] have = io.github.jimbozoomer.jugcraft.tower.squad.Requisitions.have(core);
		h.assertTrue(have[0] == 320 && have[1] == 256 && have[2] == 128 && have[3] == 65,
				"the tower counts the piped chests, each once, plain items only: " + java.util.Arrays.toString(have));
		h.assertTrue(io.github.jimbozoomer.jugcraft.tower.TowerSupply.of(core).count(Items.DIAMOND) == 65, "the tiers see the same supply");
		h.assertTrue(io.github.jimbozoomer.jugcraft.tower.squad.Requisitions.pay(core, 0), "a squad of three is bought from them");
		int[] cost = io.github.jimbozoomer.jugcraft.tower.squad.JugcraftSquads.COST[0];
		int[] after = io.github.jimbozoomer.jugcraft.tower.squad.Requisitions.have(core);
		h.assertTrue(after[0] == 320 - cost[0] && after[1] == 256 - cost[1] && after[2] == 128 - cost[2] && after[3] == 65 - cost[3],
				"its price came out of the chests: " + java.util.Arrays.toString(after));
		int ingots = 0;
		for (int i = 0; i < left.getContainerSize(); i++) ingots += left.getItem(i).is(Items.IRON_INGOT) ? left.getItem(i).getCount() : 0;
		for (int i = 0; i < right.getContainerSize(); i++) ingots += right.getItem(i).is(Items.IRON_INGOT) ? right.getItem(i).getCount() : 0;
		h.assertTrue(ingots == 320 - cost[0] && right.getItem(3).getCount() == 5, "really taken from the double chest, the named diamonds left: " + ingots);
		for (BlockPos p : new BlockPos[] {master, far, west, east, port.relative(facing), port.relative(facing, 2), port.relative(facing, 3)}) {
			level.removeBlock(p, false);
		}
		h.succeed();
	}

	/**
	 * The tower storage at the Cargo Exchange takes anything by hand (Justin, 10 November 2026: "you can't physically put
	 * things into the cargo storage ... I'd like there to be a way for you to physically put things in"; his pick: a big
	 * scrolling chest screen): diamonds dropped on its grid go in, a click takes them back out, a shift-click moves a stack
	 * in from the inventory, a named item stays out; what's in counts for drop pods.
	 */
	@GameTest(structure = TriPillarArena.ARENA, skyAccess = true, maxTicks = 100)
	public void towerStorageTakesAnythingByHand(GameTestHelper h) {
		if (TestFocus.skip("tower")) { h.succeed(); return; }
		var core = tower(h); var level = h.getLevel();
		core.terminal().packagerEntity().clearContent();
		var player = h.makeMockServerPlayerInLevel();
		player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		core.setOwner(player.getUUID());
		var menu = new io.github.jimbozoomer.jugcraft.tower.TowerStorageMenu(1, player.getInventory(),
				new io.github.jimbozoomer.jugcraft.tower.TowerStorageMenu.Opening(core.getBlockPos(), core.getBlockPos()));
		int before = core.stored(Items.DIAMOND);
		menu.setCarried(new ItemStack(Items.DIAMOND, 40));
		menu.clicked(5, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
		h.assertTrue(menu.getCarried().isEmpty() && core.stored(Items.DIAMOND) == before + 40, "dropped on the grid, all forty go in");
		menu.broadcastChanges();
		int at = -1;
		for (int i = 0; i < io.github.jimbozoomer.jugcraft.tower.TowerStorageMenu.GRID; i++) {
			if (menu.getSlot(i).getItem().is(Items.DIAMOND)) at = i;
		}
		h.assertTrue(at >= 0 && menu.getSlot(at).getItem().getCount() == before + 40, "the grid shows them");
		menu.clicked(at, 1, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
		h.assertTrue(menu.getCarried().getCount() == (before + 41) / 2 && core.stored(Items.DIAMOND) == before + 40 - (before + 41) / 2,
				"a right-click takes half back out");
		menu.setCarried(ItemStack.EMPTY);
		player.getInventory().setItem(9, new ItemStack(Items.GOLD_INGOT, 64));
		int goldSlot = io.github.jimbozoomer.jugcraft.tower.TowerStorageMenu.GRID;   // the inventory's first row's first slot
		h.assertTrue(menu.getSlot(goldSlot).getItem().is(Items.GOLD_INGOT), "the inventory under the grid");
		menu.clicked(goldSlot, 0, net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, player);
		h.assertTrue(player.getInventory().getItem(9).isEmpty() && core.stored(Items.GOLD_INGOT) >= 64, "a shift-click moves the gold in");
		var named = new ItemStack(Items.DIAMOND, 3);
		named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Shiny"));
		menu.setCarried(named);
		int diamonds = core.stored(Items.DIAMOND);
		menu.clicked(0, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
		h.assertTrue(menu.getCarried().getCount() == 3 && core.stored(Items.DIAMOND) == diamonds, "a named item stays out");
		menu.setCarried(ItemStack.EMPTY);
		h.assertTrue(io.github.jimbozoomer.jugcraft.tower.squad.Requisitions.have(core)[2] >= 64, "what's in counts for drop pods");
		h.succeed();
	}
}
