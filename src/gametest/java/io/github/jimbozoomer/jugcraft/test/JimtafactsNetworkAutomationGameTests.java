package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.jimtafacts.network.*;
import io.github.jimbozoomer.jugcraft.fluid.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

/** Scheduled for the final combined validation batch. */
public final class JimtafactsNetworkAutomationGameTests {
    @GameTest public void denseTrunkCarriesMoreThanEightRealDeviceChannels(GameTestHelper h) {
        var p = player(h); var controller = node(h, p, "controller", new BlockPos(1, 2, 2)); controller.energy.setAmount(1000);
        h.setBlock(new BlockPos(2, 2, 2), DigitalStorage.BLOCKS.get("fluix_smart_cable"));
        for (int y = 1; y <= 3; y++) for (int z = 1; z <= 3; z++) node(h, p, "drive", new BlockPos(3, y, z));
        h.assertTrue(DigitalNetwork.find(controller, p).nodes().size() == 9, "Ordinary trunk admits controller plus eight channel devices");
        h.assertTrue(h.getBlockState(new BlockPos(2, 2, 2)).getValue(DigitalCableBlock.CHANNELS) == 8, "Smart cable shows full ordinary trunk");
        h.setBlock(new BlockPos(2, 2, 2), DigitalStorage.BLOCKS.get("fluix_smart_dense_cable"));
        h.assertTrue(DigitalNetwork.find(controller, p).nodes().size() == 10, "Dense trunk admits ninth actual device");
        h.assertTrue(h.getBlockState(new BlockPos(2, 2, 2)).getValue(DigitalCableBlock.CHANNELS) == 3, "Dense display groups channels in fours"); h.succeed();
    }
    @GameTest public void processingWaitsForRealOutputsAndCancellationNeverRefundsDeliveredItems(GameTestHelper h) {
        var p = player(h); var chest = node(h, p, "me_chest", new BlockPos(1, 2, 2)); chest.energy.setAmount(10000);
        chest.setItem(0, new ItemStack(DigitalStorage.ITEMS.get("item_storage_cell_1k")));
        var provider = node(h, p, "pattern_provider", new BlockPos(2, 2, 2));
        h.setBlock(new BlockPos(2, 2, 1), Blocks.BARREL);
        var barrel = (Container) h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(2, 2, 1)));
        var pattern = new DigitalProcessing.Pattern(java.util.List.of(new ItemStack(Items.IRON_INGOT, 2)), java.util.List.of(new ItemStack(Items.GOLD_INGOT)));
        var card = new ItemStack(DigitalStorage.ITEMS.get("processing_pattern")); card.set(DigitalProcessing.PATTERN, pattern); provider.setItem(0, card);
        var network = DigitalNetwork.find(provider, p);
        try (var tx = Transaction.openOuter()) { network.insert(new ItemStack(Items.IRON_INGOT), 4, tx); tx.commit(); }
        h.assertTrue(DigitalProcessing.request(new DigitalProcessing.Binding(provider, pattern), 2), "Machine order accepted");
        DigitalProcessing.tick(provider, network, p);
        h.assertTrue(barrel.getItem(0).is(Items.IRON_INGOT) && barrel.getItem(0).getCount() == 2, "Only actual inputs delivered");
        for (int i = 0; i < 3; i++) DigitalProcessing.tick(provider, network, p);
        h.assertTrue(network.contents().stream().noneMatch(e -> e.sample().is(Items.GOLD_INGOT)), "Declared outputs are never fabricated");
        barrel.setItem(0, new ItemStack(Items.GOLD_INGOT)); DigitalProcessing.tick(provider, network, p);
        h.assertTrue(barrel.isEmpty() && network.contents().stream().anyMatch(e -> e.sample().is(Items.GOLD_INGOT) && e.count() == 1), "Actual result returned once");
        DigitalProcessing.tick(provider, network, p); DigitalProcessing.cancel(provider);
        h.assertTrue(barrel.getItem(0).getCount() == 2 && network.contents().stream().noneMatch(e -> e.sample().is(Items.IRON_INGOT)), "Cancellation leaves delivered batch in machine");
        h.succeed();
    }
    @GameTest public void portableCellsRollbackChargeAndCloseWhenNoLongerHeld(GameTestHelper h) {
        var p = player(h); var cell = new ItemStack(DigitalStorage.ITEMS.get("portable_item_cell_1k"));
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, cell);
        io.github.jimbozoomer.jugcraft.tools.Chargeable.charge(cell, 1000);
        var held = new PortableCells.Held(p, cell);
        try (var tx = Transaction.openOuter()) { h.assertTrue(held.item(new ItemStack(Items.DIAMOND), 16, tx, true) == 16, "Portable insert accepted"); }
        h.assertTrue(held.contents().isEmpty() && io.github.jimbozoomer.jugcraft.tools.Chargeable.energy(cell) == 1000, "Aborted transfer restores held cell and JE");
        try (var tx = Transaction.openOuter()) { held.item(new ItemStack(Items.DIAMOND), 16, tx, true); tx.commit(); }
        var menu = DigitalTerminal.Menu.portable(7, p.getInventory(), cell);
        h.assertTrue(menu.stillValid(p), "Held cell menu valid");
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        h.assertTrue(!menu.stillValid(p) && !held.valid(), "Moving cell invalidates old menu");
        try (var tx = Transaction.openOuter()) { h.assertTrue(held.item(new ItemStack(Items.DIAMOND), 1, tx, false) == 0, "Stale menu cannot extract"); }
        h.assertTrue(cell.get(DigitalStorage.CONTENTS).amount() == 16, "Moved cell retains its contents"); h.succeed();
    }
    @GameTest public void differentlyColoredCablesSeparateNetworksAndTransparentCableBridgesThem(GameTestHelper h) {
        var p = player(h); var chest = node(h, p, "me_chest", new BlockPos(1, 2, 1)); chest.energy.setAmount(1000);
        var terminal = node(h, p, "terminal", new BlockPos(4, 2, 1));
        h.setBlock(new BlockPos(2, 2, 1), DigitalStorage.BLOCKS.get("red_glass_cable"));
        h.setBlock(new BlockPos(3, 2, 1), DigitalStorage.BLOCKS.get("blue_covered_cable"));
        h.assertTrue(!DigitalNetwork.find(terminal, p).online(), "Different dyes do not connect directly");
        h.setBlock(new BlockPos(3, 2, 1), DigitalStorage.BLOCKS.get("fluix_covered_cable"));
        h.assertTrue(DigitalNetwork.find(terminal, p).online(), "Transparent cable bridges palette groups"); h.succeed();
    }
    private ServerPlayer player(GameTestHelper h) {
        var p = h.makeMockServerPlayerInLevel(); p.setGameMode(GameType.SURVIVAL); p.getInventory().clearContent();
        p.snapTo(Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 2, 2)))); return p;
    }
    private DigitalNode node(GameTestHelper h, ServerPlayer p, String kind, BlockPos at) {
        h.setBlock(at, DigitalStorage.BLOCKS.get(kind).defaultBlockState().setValue(DigitalNodeBlock.FACING, Direction.NORTH));
        var node = (DigitalNode) h.getLevel().getBlockEntity(h.absolutePos(at)); node.owner(p.getUUID()); return node;
    }
    @GameTest public void externalContainerRemainsTheOnlyInventoryAndRollsBack(GameTestHelper h) {
        var p = player(h); var controller = node(h, p, "controller", new BlockPos(1, 2, 2)); controller.energy.setAmount(1000);
        var bus = node(h, p, "storage_bus", new BlockPos(2, 2, 2));
        h.setBlock(new BlockPos(2, 2, 1), Blocks.BARREL);
        var barrel = (Container) h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(2, 2, 1)));
        barrel.setItem(0, new ItemStack(Items.GOLD_INGOT, 20)); var network = DigitalNetwork.find(bus, p);
        h.assertTrue(network.contents().stream().mapToInt(CellContents.Entry::count).sum() == 20, "External stock is visible without cell copies");
        try (var tx = Transaction.openOuter()) { h.assertTrue(network.extract(new ItemStack(Items.GOLD_INGOT), 7, tx) == 7, "Real container withdrawal"); }
        h.assertTrue(barrel.getItem(0).getCount() == 20 && controller.energy.getAmount() == 1000, "Rollback restores external inventory and JE");
        try (var tx = Transaction.openOuter()) { network.extract(new ItemStack(Items.GOLD_INGOT), 7, tx); tx.commit(); }
        h.assertTrue(barrel.getItem(0).getCount() == 13 && controller.energy.getAmount() == 993, "Commit only removes original stock");
        bus.filter(new ItemStack(Items.IRON_INGOT)); h.assertTrue(network.contents().isEmpty(), "External filter hides disallowed stock"); h.succeed();
    }
    @GameTest public void fluidStorageBusPreservesRealTankAndRollback(GameTestHelper h) {
        var p = player(h); var controller = node(h, p, "controller", new BlockPos(1, 2, 2)); controller.energy.setAmount(1000);
        var bus = node(h, p, "fluid_storage_bus", new BlockPos(2, 2, 2));
        h.setBlock(new BlockPos(2, 2, 1), JugcraftFluids.FLUID_TANK);
        var tank = (FluidTankBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(2, 2, 1)));
        var water = FluidVariant.of(Fluids.WATER);
        try (var tx = Transaction.openOuter()) { tank.storage.insert(water, 3 * FluidConstants.BUCKET, tx); tx.commit(); }
        var port = new FluidCells.Port(bus, p);
        h.assertTrue(port.amounts().get(water) == 3 * FluidConstants.BUCKET, "Tank visible through network");
        try (var tx = Transaction.openOuter()) { h.assertTrue(port.extract(water, FluidConstants.BUCKET, tx) == FluidConstants.BUCKET, "External fluid withdrawal"); }
        h.assertTrue(tank.storage.getAmount() == 3 * FluidConstants.BUCKET && controller.energy.getAmount() == 1000, "Aborted fluid transaction preserves tank and JE");
        try (var tx = Transaction.openOuter()) { port.extract(water, FluidConstants.BUCKET, tx); tx.commit(); }
        h.assertTrue(tank.storage.getAmount() == 2 * FluidConstants.BUCKET && controller.energy.getAmount() == 990, "Committed fluid costs JE once");
        bus.filter(new ItemStack(Items.LAVA_BUCKET)); h.assertTrue(port.amounts().isEmpty(), "Bucket filter applies to external tanks"); h.succeed();
    }
    @GameTest public void levelEmitterTracksActualStockAndDisconnect(GameTestHelper h) {
        var p = player(h); var chest = node(h, p, "me_chest", new BlockPos(1, 2, 2)); chest.energy.setAmount(1000);
        chest.setItem(0, new ItemStack(DigitalStorage.ITEMS.get("item_storage_cell_1k")));
        var emitter = node(h, p, "level_emitter", new BlockPos(2, 2, 2)); emitter.filter(new ItemStack(Items.COBBLESTONE));
        var network = DigitalNetwork.find(emitter, p);
        try (var tx = Transaction.openOuter()) { network.insert(new ItemStack(Items.COBBLESTONE), 64, tx); tx.commit(); }
        DigitalControls.update(emitter, network); h.assertTrue(emitter.getBlockState().getValue(DigitalControls.SIGNAL), "Threshold reached");
        try (var tx = Transaction.openOuter()) { network.extract(new ItemStack(Items.COBBLESTONE), 1, tx); tx.commit(); }
        DigitalControls.update(emitter, network); h.assertTrue(!emitter.getBlockState().getValue(DigitalControls.SIGNAL), "Falling stock clears signal");
        var menu = new DigitalControls.Menu(9, p.getInventory(), emitter); menu.clickMenuButton(p, 6);
        h.assertTrue(emitter.getBlockState().getValue(DigitalControls.SIGNAL), "Below mode emits for low stock");
        chest.energy.setAmount(0); DigitalControls.update(emitter, network);
        h.assertTrue(!emitter.getBlockState().getValue(DigitalControls.SIGNAL), "Offline networks never claim a live threshold"); h.succeed();
    }
}
