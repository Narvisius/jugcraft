package io.github.jimbozoomer.jugcraft.jimtafacts.network;

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

/** Authored for the final combined batch; no imported executable is used. */
public final class DigitalNetworkSystemsGameTests {
    @GameTest public void compactCasingRecipePreservesStoredEnergy(GameTestHelper h) {
        var source=new ItemStack(DigitalStorage.BLOCKS.get("energy_acceptor"));source.set(DigitalWorkshops.ENERGY,12345L);
        var recipe=new DigitalPartConversionRecipe("energy_acceptor","cable_energy_acceptor");
        var input=net.minecraft.world.item.crafting.CraftingInput.of(1,1,java.util.List.of(source));
        var output=recipe.assemble(input);
        h.assertTrue(output.is(DigitalStorage.BLOCKS.get("cable_energy_acceptor").asItem())&&output.getOrDefault(DigitalWorkshops.ENERGY,0L)==12345L,"Changing casing keeps exact charge");
        h.assertTrue(source.getOrDefault(DigitalWorkshops.ENERGY,0L)==12345L,"Recipe preview does not consume or mutate input");h.succeed();
    }
    @GameTest public void condenserFractionalFluidRollbackAndBlockedOutputPreserveCredit(GameTestHelper h) {
        var p = player(h); var n = node(h, p, "condenser", 1, 2, 1); n.setItem(0, item("cell_component_1k"));
        var water = FluidVariant.of(Fluids.WATER);
        var port=FluidStorage.SIDED.find(h.getLevel(),n.getBlockPos(),Direction.UP);
        h.assertTrue(port!=null,"Condenser exposes its fluid automation port");
        h.assertTrue(net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(h.getLevel(),n.getBlockPos(),Direction.UP)!=null,"Condenser exposes its item automation port");
        try (var tx = Transaction.openOuter()) { h.assertTrue(port.insert(water, 255 * FluidConstants.BUCKET + 1, tx) > 0, "Fractional fluid accepted"); }
        h.assertTrue(n.condensed == 0, "Rollback restores exact fluid credit");
        try (var tx = Transaction.openOuter()) { port.insert(water, 255 * FluidConstants.BUCKET + 1, tx); tx.commit(); }
        DigitalCondenser.tick(n); h.assertTrue(n.getItem(2).isEmpty(), "Fraction does not round up into an output");
        n.setItem(1, new ItemStack(Items.COBBLESTONE)); n.setItem(2, new ItemStack(Items.DIRT)); DigitalCondenser.tick(n);
        h.assertTrue(n.condensed == 256 * FluidConstants.BUCKET + 1, "Blocked output keeps all paid credit");
        n.setItem(2, ItemStack.EMPTY); DigitalCondenser.tick(n);
        h.assertTrue(n.getItem(2).is(DigitalStorage.ITEMS.get("matter_ball")) && n.condensed == 1, "One output costs exactly 256 units"); h.succeed();
    }
    @GameTest public void inscriberKeepsDieAndDoesNotConsumeWhenOutputBlocked(GameTestHelper h) {
        var p = player(h); var n = node(h, p, "inscriber", 1, 2, 1); n.energy.setAmount(1000);
        n.setItem(0, item("logic_processor_press")); n.setItem(1, new ItemStack(Items.GOLD_INGOT)); n.setItem(4, new ItemStack(Items.DIRT));
        var network = DigitalNetwork.find(n, p); DigitalProduction.print(n, network);
        h.assertTrue(n.getItem(1).is(Items.GOLD_INGOT) && n.energy.getAmount() == 1000, "Blocked printing costs no material or JE");
        n.setItem(4, ItemStack.EMPTY); DigitalProduction.print(n, network);
        h.assertTrue(n.getItem(4).is(DigitalStorage.ITEMS.get("printed_logic_processor")) && n.getItem(0).is(DigitalStorage.ITEMS.get("logic_processor_press")) && n.getItem(1).isEmpty() && n.energy.getAmount() == 900, "Actual printed result consumes material and power, keeping die"); h.succeed();
    }
    @GameTest public void pairedItemTunnelMovesActualInventoryAndReplacementInvalidatesLink(GameTestHelper h) {
        var p = player(h); var controller = node(h, p, "controller", 1, 2, 1); controller.energy.setAmount(1000);
        var input = node(h, p, "item_p2p_tunnel", 2, 2, 1); var output = node(h, p, "item_p2p_tunnel", 4, 2, 1);
        input.peer = DigitalLinks.identity(output); output.peer = DigitalLinks.identity(input); input.linkInput = true;
        h.setBlock(new BlockPos(2, 2, 0), Blocks.BARREL); h.setBlock(new BlockPos(4, 2, 0), Blocks.BARREL);
        var from = (Container) h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(2, 2, 0))); var to = (Container) h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(4, 2, 0)));
        from.setItem(0, new ItemStack(Items.DIAMOND, 12));
        DigitalLinks.tick(input, DigitalNetwork.find(input, p), p);
        h.assertTrue(from.isEmpty() && to.getItem(0).getCount() == 12 && controller.energy.getAmount() == 990, "Tunnel moves twelve real items and spends JE");
        h.setBlock(new BlockPos(4, 2, 1), Blocks.AIR); node(h, p, "item_p2p_tunnel", 4, 2, 1); from.setItem(0, new ItemStack(Items.DIAMOND));
        DigitalLinks.tick(input, DigitalNetwork.find(input, p), p);
        h.assertTrue(from.getItem(0).getCount() == 1 && to.getItem(0).getCount() == 12, "Replacement at same coordinates cannot inherit pairing"); h.succeed();
    }
    @GameTest public void networkTunnelDisconnectsWithoutPhantomStorage(GameTestHelper h) {
        var p = player(h); var chest = node(h, p, "me_chest", 1, 2, 1); chest.energy.setAmount(1000); chest.setItem(0, item("item_storage_cell_1k"));
        var a = node(h, p, "me_p2p_tunnel", 2, 2, 1); var b = node(h, p, "me_p2p_tunnel", 4, 2, 1); var terminal = node(h, p, "terminal", 5, 2, 1);
        a.peer = DigitalLinks.identity(b); b.peer = DigitalLinks.identity(a); a.linkInput = true;
        try (var tx = Transaction.openOuter()) { chest.insert(new ItemStack(Items.DIAMOND), 3, tx); tx.commit(); }
        h.assertTrue(DigitalNetwork.find(terminal, p).contents().stream().anyMatch(e -> e.sample().is(Items.DIAMOND) && e.count() == 3), "Paired network sees real remote cell");
        a.peer = null; h.assertTrue(DigitalNetwork.find(terminal, p).contents().isEmpty(), "Unpaired terminal loses remote access");
        h.assertTrue(chest.contents().getFirst().count() == 3, "Disconnect leaves stored contents intact"); h.succeed();
    }
    @GameTest public void removableCardsAlterFiltersWithoutConsumingSamples(GameTestHelper h) {
        var p = player(h); var n = node(h, p, "import_bus", 1, 2, 1);
        n.setItem(0, new ItemStack(Items.DIAMOND)); n.setItem(1, new ItemStack(Items.GOLD_INGOT));
        h.assertTrue(!DigitalUpgrades.accepts(n, new ItemStack(Items.GOLD_INGOT)), "Extra sample inactive without capacity card");
        n.setItem(9, item("capacity_card")); h.assertTrue(DigitalUpgrades.accepts(n, new ItemStack(Items.GOLD_INGOT)), "Capacity enables additional actual sample");
        n.setItem(10, item("inverter_card")); h.assertTrue(!DigitalUpgrades.accepts(n, new ItemStack(Items.GOLD_INGOT)) && DigitalUpgrades.accepts(n, new ItemStack(Items.IRON_INGOT)), "Inverter switches allowlist to denylist");
        n.removeItem(9, 1); h.assertTrue(n.getItem(1).is(Items.GOLD_INGOT), "Removing capacity retains now-disabled sample"); h.succeed();
    }
    private static ItemStack item(String id) { return new ItemStack(DigitalStorage.ITEMS.get(id)); }
    @GameTest public void skyTankRollbackAndChestCapacity(GameTestHelper h) {
        h.setBlock(new BlockPos(1, 2, 1), DigitalStorage.BLOCKS.get("sky_stone_tank"));
        var tank = (DigitalUtilityStorage.Tank) h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(1, 2, 1)));
        try (var tx = Transaction.openOuter()) { tank.storage.insert(FluidVariant.of(Fluids.WATER), 20 * FluidConstants.BUCKET, tx); }
        h.assertTrue(tank.storage.amount == 0, "Uncommitted fill leaves tank empty");
        try (var tx = Transaction.openOuter()) { h.assertTrue(tank.storage.insert(FluidVariant.of(Fluids.WATER), 20 * FluidConstants.BUCKET, tx) == 16 * FluidConstants.BUCKET, "Tank holds exactly16buckets"); tx.commit(); }
        h.setBlock(new BlockPos(3, 2, 1), DigitalStorage.BLOCKS.get("sky_stone_chest"));
        var chest = (DigitalUtilityStorage.Chest) h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(3, 2, 1)));
        chest.setItem(35, new ItemStack(Items.DIAMOND)); h.assertTrue(chest.getContainerSize() == 36 && chest.getItem(35).is(Items.DIAMOND), "Original36slots are real accessible storage"); h.succeed();
    }
    @GameTest public void stonecuttingPatternUsesActualRecipeAndTransactionalInputs(GameTestHelper h) {
        var p = player(h); var chest = node(h, p, "me_chest", 1, 2, 1); chest.energy.setAmount(1000); chest.setItem(0, item("item_storage_cell_1k"));
        var grid = new java.util.ArrayList<>(java.util.Collections.nCopies(9, ItemStack.EMPTY)); grid.set(0, new ItemStack(Items.STONE));
        var pattern = DigitalSpecialPatterns.encode(1, grid, new ItemStack(Items.STONE_SLAB), h.getLevel());
        h.assertTrue(pattern != null && pattern.output(h.getLevel()).is(Items.STONE_SLAB), "Pattern resolves a current stonecutting recipe");
        var network = DigitalNetwork.find(chest, p);
        h.assertTrue(!DigitalPatterns.craft(pattern, network, h.getLevel()), "No input means no crafted slabs");
        try (var tx = Transaction.openOuter()) { chest.insert(new ItemStack(Items.STONE), 1, tx); tx.commit(); }
        h.assertTrue(DigitalPatterns.craft(pattern, network, h.getLevel()), "Real stone can be cut");
        h.assertTrue(chest.contents().stream().noneMatch(e -> e.sample().is(Items.STONE)) && chest.contents().stream().anyMatch(e -> e.sample().is(Items.STONE_SLAB) && e.count() == 2), "One stone becomes exactly two slabs"); h.succeed();
    }
    @GameTest public void quantumFrameBreakRevokesRemoteStorageAndRollbackPreservesBothSides(GameTestHelper h) {
        var p = player(h); var a = node(h, p, "quantum_link", 2, 3, 2); var b = node(h, p, "quantum_link", 6, 3, 6);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) if (dx != 0 || dz != 0) {
            h.setBlock(new BlockPos(2 + dx, 3, 2 + dz), DigitalStorage.BLOCKS.get("quantum_ring")); h.setBlock(new BlockPos(6 + dx, 3, 6 + dz), DigitalStorage.BLOCKS.get("quantum_ring"));
        }
        var local = node(h, p, "me_chest", 2, 4, 2); var remote = node(h, p, "me_chest", 6, 4, 6);
        local.energy.setAmount(1000); remote.energy.setAmount(1000); local.setItem(0, item("item_storage_cell_1k")); remote.setItem(0, item("item_storage_cell_1k"));
        a.setItem(0, item("quantum_entangled_singularity")); b.setItem(0, item("quantum_entangled_singularity")); a.peer = DigitalLinks.identity(b); b.peer = DigitalLinks.identity(a);
        try (var tx = Transaction.openOuter()) { remote.insert(new ItemStack(Items.DIAMOND), 12, tx); tx.commit(); }
        var network = DigitalNetwork.find(local, p); h.assertTrue(network.remote().size() == 1 && network.contents().stream().anyMatch(e -> e.sample().is(Items.DIAMOND) && e.count() == 12), "Formed bridge exposes actual remote cell");
        try (var tx = Transaction.openOuter()) { h.assertTrue(network.extract(new ItemStack(Items.DIAMOND), 10, tx) == 10, "Remote transfer participates in outer transaction"); }
        h.assertTrue(remote.contents().getFirst().count() == 12 && local.energy.getAmount() == 1000 && remote.energy.getAmount() == 1000, "Abort restores both stores and JE debits");
        h.setBlock(new BlockPos(5, 3, 5), Blocks.AIR);
        h.assertTrue(DigitalNetwork.find(local, p).contents().stream().noneMatch(e -> e.sample().is(Items.DIAMOND)), "Broken frame revokes access immediately"); h.succeed();
    }
    private static ServerPlayer player(GameTestHelper h) { var p = h.makeMockServerPlayerInLevel(); p.setGameMode(GameType.SURVIVAL); p.snapTo(Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 2, 2)))); return p; }
    private static DigitalNode node(GameTestHelper h, ServerPlayer p, String id, int x, int y, int z) { var at = new BlockPos(x, y, z); h.setBlock(at, DigitalStorage.BLOCKS.get(id)); var n = (DigitalNode) h.getLevel().getBlockEntity(h.absolutePos(at)); n.owner(p.getUUID()); return n; }
}
