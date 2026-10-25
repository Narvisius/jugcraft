package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.jimtafacts.network.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Written for the final combined batch; covers real cells and transaction/protection boundaries. */
public final class JimtafactsNetworkGameTests {
    private ServerPlayer player(GameTestHelper h) {
        var p = h.makeMockServerPlayerInLevel(); p.setGameMode(GameType.SURVIVAL); p.getInventory().clearContent();
        p.snapTo(Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 2, 2)))); return p;
    }
    private DigitalNode node(GameTestHelper h, ServerPlayer p, String kind, BlockPos at) {
        h.setBlock(at, DigitalStorage.BLOCKS.get(kind));
        var node = (DigitalNode) h.getLevel().getBlockEntity(h.absolutePos(at)); node.owner(p.getUUID()); return node;
    }
    @GameTest public void abortedTransferRestoresCellsAndPowerTogether(GameTestHelper h) {
        var p = player(h); var controller = node(h, p, "controller", new BlockPos(1, 2, 1));
        var drive = node(h, p, "drive", new BlockPos(2, 2, 1));
        drive.setItem(0, new ItemStack(DigitalStorage.ITEMS.get("item_storage_cell_1k"))); controller.energy.setAmount(1000);
        var network = DigitalNetwork.find(controller, p);
        try (Transaction tx = Transaction.openOuter()) { h.assertTrue(network.insert(new ItemStack(Items.DIAMOND), 64, tx) == 64, "Capacity accepts transfer"); }
        h.assertTrue(drive.contents().isEmpty() && controller.energy.getAmount() == 1000, "Aborted cells and power restored");
        try (Transaction tx = Transaction.openOuter()) { network.insert(new ItemStack(Items.DIAMOND), 64, tx); tx.commit(); }
        h.assertTrue(drive.contents().getFirst().count() == 64 && controller.energy.getAmount() == 936, "Committed once");
        try (Transaction tx = Transaction.openOuter()) { network.extract(new ItemStack(Items.DIAMOND), 23, tx); }
        h.assertTrue(drive.contents().getFirst().count() == 64 && controller.energy.getAmount() == 936, "Aborted withdrawal restored"); h.succeed();
    }
    @GameTest public void cellsStayWithTheirContentsAndCannotContainAnotherCell(GameTestHelper h) {
        var p = player(h); var chest = node(h, p, "me_chest", new BlockPos(2, 2, 1)); chest.energy.setAmount(1000);
        var cell = new ItemStack(DigitalStorage.ITEMS.get("item_storage_cell_1k")); chest.setItem(0, cell);
        var network = DigitalNetwork.find(chest, p);
        try (Transaction tx = Transaction.openOuter()) { h.assertTrue(network.insert(cell, 1, tx) == 0, "No recursive cells"); network.insert(new ItemStack(Items.COBBLESTONE), 100, tx); tx.commit(); }
        var removed = chest.removeItem(0, 1);
        h.assertTrue(removed.get(DigitalStorage.CONTENTS).count(new ItemStack(Items.COBBLESTONE)) == 100, "Cell retains real items");
        h.assertTrue(network.contents().isEmpty(), "Removed cell no longer visible through old network");
        chest.setItem(0, removed); h.assertTrue(network.contents().getFirst().count() == 100, "Returning cell doesn't duplicate contents"); h.succeed();
    }
    @GameTest public void disconnectedAndForeignNetworksCannotBeRead(GameTestHelper h) {
        var owner = player(h); var visitor = player(h);
        var chest = node(h, owner, "me_chest", new BlockPos(2, 2, 1)); chest.energy.setAmount(1000);
        chest.setItem(0, new ItemStack(DigitalStorage.ITEMS.get("item_storage_cell_1k")));
        try (Transaction tx = Transaction.openOuter()) { DigitalNetwork.find(chest, owner).insert(new ItemStack(Items.GOLD_INGOT), 8, tx); tx.commit(); }
        h.assertTrue(DigitalNetwork.find(chest, visitor).contents().isEmpty(), "Foreign owner cannot inspect cells");
        var terminal = node(h, owner, "terminal", new BlockPos(4, 2, 1)); h.setBlock(new BlockPos(3, 2, 1), DigitalStorage.BLOCKS.get("fluix_glass_cable"));
        h.assertTrue(!DigitalNetwork.find(terminal, owner).contents().isEmpty(), "Connected cable finds storage");
        h.setBlock(new BlockPos(3, 2, 1), Blocks.AIR); h.assertTrue(DigitalNetwork.find(terminal, owner).contents().isEmpty(), "Cut cable ends access immediately"); h.succeed();
    }
    @GameTest public void automaticCraftReservesAllInputsOutputsAndRemainders(GameTestHelper h) {
        var p = player(h); var chest = node(h, p, "me_chest", new BlockPos(2, 2, 1)); chest.energy.setAmount(10000);
        chest.setItem(0, new ItemStack(DigitalStorage.ITEMS.get("item_storage_cell_1k")));
        var network = DigitalNetwork.find(chest, p); var grid = new ArrayList<ItemStack>(Collections.nCopies(9, ItemStack.EMPTY)); grid.set(0, new ItemStack(Items.OAK_LOG));
        var pattern = new DigitalPatterns.Pattern(grid);
        h.assertTrue(!DigitalPatterns.craft(pattern, network, h.getLevel()), "Missing ingredients cannot fabricate results");
        try (Transaction tx = Transaction.openOuter()) { network.insert(new ItemStack(Items.OAK_LOG), 1, tx); tx.commit(); }
        h.assertTrue(DigitalPatterns.craft(pattern, network, h.getLevel()), "Real recipe crafts");
        var contents = chest.getItem(0).get(DigitalStorage.CONTENTS);
        h.assertTrue(contents.count(new ItemStack(Items.OAK_LOG)) == 0 && contents.count(new ItemStack(Items.OAK_PLANKS)) == 4, "Inputs spent, output produced exactly once");
        h.assertTrue(!DigitalPatterns.craft(pattern, network, h.getLevel()), "Cannot repeat without another log"); h.succeed();
    }
    @GameTest public void fluidCellsRollbackAndRemainPortable(GameTestHelper h) {
        var p = player(h); var chest = node(h, p, "me_chest", new BlockPos(2, 2, 1)); chest.energy.setAmount(1000);
        chest.setItem(0, new ItemStack(DigitalStorage.ITEMS.get("fluid_storage_cell_1k")));
        var port = new FluidCells.Port(chest, p);
        var water = net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.minecraft.world.level.material.Fluids.WATER);
        long bucket = net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET;
        try (var tx = Transaction.openOuter()) { h.assertTrue(port.insert(water, bucket, tx) == bucket, "Water accepted"); }
        h.assertTrue(port.amounts().isEmpty() && chest.energy.getAmount() == 1000, "Fluid and JE rollback together");
        try (var tx = Transaction.openOuter()) { port.insert(water, bucket, tx); tx.commit(); }
        h.assertTrue(port.amounts().get(water) == bucket && chest.energy.getAmount() == 990, "Exactly one bucket and charge committed");
        var cell = chest.removeItem(0, 1); h.assertTrue(port.amounts().isEmpty(), "Removed cell no longer exposed");
        h.assertTrue(cell.get(FluidCells.CONTENTS).amount(water) == bucket, "Portable fluid retained"); h.succeed();
    }
    @GameTest public void craftingCanBuildIntermediateIngredientsAndRejectCycles(GameTestHelper h) {
        var p = player(h); var chest = node(h, p, "me_chest", new BlockPos(2, 2, 1)); chest.energy.setAmount(10000);
        chest.setItem(0, new ItemStack(DigitalStorage.ITEMS.get("item_storage_cell_1k")));
        var provider = node(h, p, "pattern_provider", new BlockPos(3, 2, 1));
        var planks = new ArrayList<ItemStack>(Collections.nCopies(9, ItemStack.EMPTY)); planks.set(0, new ItemStack(Items.OAK_LOG));
        var sticks = new ArrayList<ItemStack>(Collections.nCopies(9, ItemStack.EMPTY)); sticks.set(0, new ItemStack(Items.OAK_PLANKS)); sticks.set(3, new ItemStack(Items.OAK_PLANKS));
        var encoded = new ItemStack(DigitalStorage.ITEMS.get("crafting_pattern")); encoded.set(DigitalPatterns.PATTERN, new DigitalPatterns.Pattern(planks)); provider.setItem(0, encoded);
        var network = DigitalNetwork.find(chest, p);
        try (var tx = Transaction.openOuter()) { network.insert(new ItemStack(Items.OAK_LOG), 1, tx); tx.commit(); }
        h.assertTrue(DigitalPatterns.craft(new DigitalPatterns.Pattern(sticks), network, h.getLevel(), true), "Intermediate recipe resolves from installed patterns");
        var contents = chest.getItem(0).get(DigitalStorage.CONTENTS);
        h.assertTrue(contents.count(new ItemStack(Items.OAK_PLANKS)) == 2 && contents.count(new ItemStack(Items.STICK)) == 4, "All intermediate surplus remains in cell");
        var ironBlock = new ArrayList<ItemStack>(Collections.nCopies(9, new ItemStack(Items.IRON_INGOT)));
        var ironIngots = new ArrayList<ItemStack>(Collections.nCopies(9, ItemStack.EMPTY)); ironIngots.set(0, new ItemStack(Items.IRON_BLOCK));
        var blockPattern = new DigitalPatterns.Pattern(ironBlock); var ingotPattern = new DigitalPatterns.Pattern(ironIngots);
        var b = encoded.copy(); b.set(DigitalPatterns.PATTERN, blockPattern); provider.setItem(1, b);
        var i = encoded.copy(); i.set(DigitalPatterns.PATTERN, ingotPattern); provider.setItem(2, i);
        long before = chest.energy.getAmount();
        h.assertTrue(!DigitalPatterns.craft(blockPattern, network, h.getLevel(), true), "No-input circular compression cannot fabricate iron");
        h.assertTrue(chest.energy.getAmount() == before, "Rejected dependency tree leaves JE unchanged"); h.succeed();
    }
    @GameTest public void partitionedCellsRejectUnlistedInputsWithoutChangingExistingContents(GameTestHelper h) {
        var p = player(h); var chest = node(h, p, "me_chest", new BlockPos(2, 2, 1)); chest.energy.setAmount(1000);
        var cell = new ItemStack(DigitalStorage.ITEMS.get("item_storage_cell_1k"));
        cell.set(DigitalWorkshops.FILTERS, List.of(new ItemStack(Items.COBBLESTONE)));
        cell.set(DigitalStorage.CONTENTS, CellContents.EMPTY.add(new ItemStack(Items.DIAMOND), 3)); chest.setItem(0, cell);
        var network = DigitalNetwork.find(chest, p);
        try (var tx = Transaction.openOuter()) {
            h.assertTrue(network.insert(new ItemStack(Items.GOLD_INGOT), 1, tx) == 0, "Filter rejects new unlisted items");
            h.assertTrue(network.insert(new ItemStack(Items.COBBLESTONE), 64, tx) == 64, "Filter accepts matching items");
            h.assertTrue(network.extract(new ItemStack(Items.DIAMOND), 3, tx) == 3, "Old contents remain recoverable even after filters change"); tx.commit();
        }
        h.assertTrue(chest.getItem(0).get(DigitalStorage.CONTENTS).amount() == 64, "No loss or extra output"); h.succeed();
    }
}
