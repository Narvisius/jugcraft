package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.drone.DepotView;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.JugcraftDrones;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.energy.EnergyNetworks;
import io.github.jimbozoomer.jugcraft.energy.CableBlock;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.tower.ExchangeStationBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.ExchangeStationBlock;
import io.github.jimbozoomer.jugcraft.tower.ExchangeStations;
import io.github.jimbozoomer.jugcraft.tower.JugcraftTower;
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/** Instrument synchronization is presentation-only; original transfer ownership remains authoritative. */
public class EnergyExchangeGameTests {
	@GameTest
	public void lowRoofHoodsHaveTheirRealRotatedCollision(GameTestHelper h) {
		if (TestFocus.skip("tower")) { h.succeed(); return; }
		BlockPos at = h.absolutePos(new BlockPos(1, 1, 1));
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			int hoods = 0;
			for (int part = 0; part < ExchangeStations.ENERGY.footprint().size(); part++) {
				if (ExchangeStations.ENERGY.footprint().offsets().get(part).getY() != 3) continue;
				hoods++;
				var state = ExchangeStations.ENERGY.defaultBlockState().setValue(ExchangeStationBlock.LARGE, false).setValue(ExchangeStationBlock.FACING, facing).setValue(ExchangeStationBlock.PART, part);
				var collision = state.getCollisionShape(h.getLevel(), at).bounds();
				double x0 = switch (facing) { case EAST -> 2.5; case WEST -> 1.5; default -> 3.6; };
				double z0 = switch (facing) { case SOUTH -> 2.5; case NORTH -> 1.5; default -> 3.6; };
				double width = facing.getAxis() == Direction.Axis.Z ? 8.8 : 12;
				double depth = facing.getAxis() == Direction.Axis.Z ? 12 : 8.8;
				h.assertTrue(Math.abs(collision.maxY - 3.6 / 16) < .000001 && collision.minY == 0
						&& Math.abs(collision.minX - x0 / 16) < .000001 && Math.abs(collision.minZ - z0 / 16) < .000001
						&& Math.abs(collision.getXsize() - width / 16) < .000001 && Math.abs(collision.getZsize() - depth / 16) < .000001,
						"roof hood collision follows its shallow 3.6px native body, including asymmetric rotation in " + facing);
				h.assertTrue(state.getShape(h.getLevel(), at).bounds().equals(collision), "selection and collision identify the same real hood");
			}
			h.assertTrue(hoods == 3, "all three upper parts receive the native hood collision");
		}
		h.succeed();
	}

	@GameTest(structure = "jugcraft-test:drone_tower", skyAccess = true, maxTicks = 100)
	public void normalCableVariantsConnectAndConservePowerAcrossBothSizesAndAllFacings(GameTestHelper h) {
		if (TestFocus.skip("tower")) { h.succeed(); return; }
		var level = h.getLevel(); BlockPos corePos = h.absolutePos(new BlockPos(48, 1, 48));
		level.setBlock(corePos, JugcraftTower.CORE.defaultBlockState(), Block.UPDATE_ALL);
		var core = (TowerCoreBlockEntity) level.getBlockEntity(corePos); core.setOwner(UUID.randomUUID()); core.buildInstantly(level, 1);
		var terminal = core.terminal(); terminal.rescanNow();
		var cables = BuiltInRegistries.BLOCK.stream().filter(block -> block instanceof CableBlock).toList();
		h.assertTrue(cables.size() >= 3, "registered normal copper, aluminum and silver carriers exist");
		BlockPos master = corePos.offset(-12, 5, -10);
		for (boolean large : new boolean[] {false, true}) for (Block cable : cables) for (Direction facing : Direction.Plane.HORIZONTAL) {
			var state = ExchangeStations.ENERGY.defaultBlockState().setValue(ExchangeStationBlock.FACING, facing).setValue(ExchangeStationBlock.LARGE, large);
			BlockPos port = ExchangeStations.ENERGY.footprint(state).partPos(master, facing, ExchangeStations.ENERGY.portPart(state));
			BlockPos wire = port.relative(facing);
			level.setBlock(wire, cable.defaultBlockState(), Block.UPDATE_ALL);
			level.setBlock(master, state, Block.UPDATE_ALL); ExchangeStations.ENERGY.setPlacedBy(level, master, state, null, new net.minecraft.world.item.ItemStack(ExchangeStations.ENERGY));
			var station = (ExchangeStationBlockEntity) level.getBlockEntity(master);
			h.assertTrue(station.formed() && station.bind(core), "real rotated power station is formed and bound");
			var connected = level.getBlockState(wire);
			h.assertTrue(connected.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(facing.getOpposite())), "normal station neighbor update creates the actual inward cable arm");
			var bounds = connected.getShape(level, wire).bounds();
			double face = facing == Direction.NORTH ? bounds.maxZ : facing == Direction.SOUTH ? bounds.minZ
					: facing == Direction.WEST ? bounds.maxX : bounds.minX;
			h.assertTrue(Math.abs(face - (facing == Direction.NORTH || facing == Direction.WEST ? 1 : 0)) < 1E-6,
					"cable shape reaches the power socket's shared cell face");
			terminal.energy().setAmount(0);
			var source = new SimpleEnergyStorage(10000, 10000, 10000, () -> { }); source.setAmount(10000);
			long expected = Math.min(4096, ((CableBlock) cable).transferRate());
			long moved = EnergyNetworks.pushToNeighbors(level, wire.relative(facing), source, 4096, java.util.List.of(facing.getOpposite()));
			h.assertTrue(moved == expected && terminal.energy().getAmount() == expected && source.getAmount() == 10000 - expected,
					"normal cable rate and original terminal buffer conserve power for " + facing + ", large=" + large);
			var cached = EnergyStorage.SIDED.find(level, port, facing);
			h.assertTrue(cached != null && EnergyStorage.SIDED.find(level, port, facing.getOpposite()) == null, "only the real outward face exposes power");
			level.removeBlock(master, false);
			h.assertFalse(level.getBlockState(wire).getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(facing.getOpposite())), "cable arm disconnects after full station teardown");
			try (var tx = Transaction.openOuter()) { h.assertTrue(cached.insert(1, tx) == 0, "cached power handle cannot write after teardown"); tx.commit(); }
			level.removeBlock(wire, false);
		}
		h.succeed();
	}
	@GameTest(structure = "jugcraft-test:drone_tower", skyAccess = true, maxTicks = 100)
	public void exactDisplayLinkSurvivesPacketsAndReplacementStillRejectsPower(GameTestHelper h) {
		if (TestFocus.skip("tower")) { h.succeed(); return; }
		var level = h.getLevel(); BlockPos corePos = h.absolutePos(new BlockPos(48, 1, 48));
		level.setBlock(corePos, JugcraftTower.CORE.defaultBlockState(), Block.UPDATE_ALL);
		var core = (TowerCoreBlockEntity) level.getBlockEntity(corePos); core.setOwner(UUID.randomUUID()); core.buildInstantly(level, 1);
		DroneTerminalBlockEntity terminal = core.terminal(); terminal.rescanNow(); terminal.energy().setAmount(12345);
		BlockPos anchor = corePos.offset(-22, 1, 4), port = corePos.offset(-22, 2, 0);
		var station = (ExchangeStationBlockEntity) level.getBlockEntity(anchor);
		h.assertTrue(station.terminal() == terminal, "normal server station is bound before display synchronization");
		var tag = station.getUpdateTag(level.registryAccess());
		var input = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag);
		h.assertTrue(input.child("display_link").isPresent() && input.getLong("energy").isEmpty()
				&& input.read("owner", net.minecraft.core.UUIDUtil.CODEC).isEmpty(), "display packet contains link only, no copied buffer or owner authority");
		var replica = new ExchangeStationBlockEntity(anchor, station.getBlockState()); replica.setLevel(level);
		replica.loadWithComponents(input);
		h.assertTrue(terminal.getBlockPos().equals(replica.displayTerminalPos()) && corePos.equals(replica.displayCorePos())
				&& terminal.depotId().equals(replica.displayDepotId()), "chunk/update packet restores exact terminal, core and depot identity");
		h.assertTrue(replica.terminal() == null, "presentation-only packet cannot manufacture server ownership");
		var terminalInput = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), terminal.getUpdateTag(level.registryAccess()));
		DepotView view = DepotView.read(terminalInput.child("view").orElseThrow());
		h.assertTrue(terminal.depotId().equals(view.depotId) && view.energy == 12345 && view.capacity == terminal.energy().getCapacity(),
				"existing terminal snapshot supplies exact identity and original energy, without another store");
		EnergyStorage handle = EnergyStorage.SIDED.find(level, port, Direction.WEST);
		try (var tx = Transaction.openOuter()) { h.assertTrue(handle.insert(100, tx) == 100, "unchanged marked socket accepts real power"); tx.commit(); }
		h.assertTrue(terminal.energy().getAmount() == 12445, "display synchronization leaves real energy conservation intact");
		BlockPos terminalPos = terminal.getBlockPos(); level.removeBlock(terminalPos, false);
		level.setBlock(terminalPos, JugcraftDrones.TERMINAL.defaultBlockState(), Block.UPDATE_ALL);
		var replacement = (DroneTerminalBlockEntity) level.getBlockEntity(terminalPos);
		replacement.setOwner(core.owner()); replacement.linkTower(corePos);
		h.assertTrue(!replacement.depotId().equals(station.displayDepotId()) && station.terminal() == null,
				"same-position replacement cannot inherit the old display/power identity");
		try (var tx = Transaction.openOuter()) { h.assertTrue(handle.insert(100, tx) == 0, "cached API still rejects replacement without explicit bind"); tx.commit(); }
		h.assertTrue(station.bind(core) && station.displayDepotId().equals(replacement.depotId()), "explicit existing bind refreshes the display identity");
		h.succeed();
	}

	@GameTest
	public void depotIdentityIsOptionalInOlderSnapshotsAndPreservedInCurrentSnapshots(GameTestHelper h) {
		if (TestFocus.skip("tower")) { h.succeed(); return; }
		var registries = h.getLevel().registryAccess(); var old = new net.minecraft.nbt.CompoundTag();
		DepotView legacy = DepotView.read(TagValueInput.create(ProblemReporter.DISCARDING, registries, old));
		h.assertTrue(legacy.depotId == null, "older snapshots load safely without inventing an instrument link");
		DepotView current = new DepotView(); current.depotId = UUID.randomUUID(); current.energy = 89012; current.capacity = 200000;
		var out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries); current.write(out);
		DepotView read = DepotView.read(TagValueInput.create(ProblemReporter.DISCARDING, registries, out.buildResult()));
		h.assertTrue(current.depotId.equals(read.depotId) && read.energy == current.energy && read.capacity == current.capacity,
				"snapshot identity round-trips with its original energy values");
		h.succeed();
	}
}
