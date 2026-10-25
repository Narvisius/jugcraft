package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.*;
import io.github.jimbozoomer.jugcraft.machine.*;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;

public final class JimtaUpgradeGameTests {
    private static MachineBlockEntity machine(GameTestHelper h) {
        var at = new BlockPos(2,2,2); h.setBlock(at, JimtaIndustry.MACHINES.get("jimta_macerator"));
        return h.getBlockEntity(at, MachineBlockEntity.class);
    }
    private static ItemStack card(IndustrialUpgrades.Kind kind) { return new ItemStack(IndustrialUpgrades.ITEMS.get(kind)); }
    @GameTest public void upgradesChangeRealEnergyAndPersistStrandedChargeWithoutCreatingIt(GameTestHelper h) {
        var machine = machine(h); var energy = (SimpleEnergyStorage) machine.energyFor(null); long base = energy.getCapacity();
        machine.setItem(machine.kind().slots, card(IndustrialUpgrades.Kind.ENERGY_STORAGE));
        h.assertTrue(energy.getCapacity() == base + 10000 && energy.getAmount() == 0, "Capacity card grants no free JE");
        energy.setAmount(base + 9000); machine.setItem(machine.kind().slots, ItemStack.EMPTY);
        try (var tx = Transaction.openOuter()) { h.assertTrue(energy.insert(1000, tx) == 0, "Removed card cannot keep accepting extra charge"); }
        var saved = machine.saveWithFullMetadata(h.getLevel().registryAccess());
        energy.setAmount(0); machine.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), saved));
        h.assertTrue(energy.getAmount() == base + 9000, "Stranded charge survives save/reload");
        energy.setAmount(0); long ordinary;
        try (var tx = Transaction.openOuter()) { ordinary = energy.insert(base, tx); }
        machine.setItem(machine.kind().slots, card(IndustrialUpgrades.Kind.TRANSFORMER));
        try (var tx = Transaction.openOuter()) { h.assertTrue(energy.insert(base, tx) == Math.min(base, ordinary * 4), "Input tier multiplies real transfer ceiling"); }
        h.assertTrue(energy.getAmount() == 0, "Aborted probes do not charge machine"); h.succeed();
    }
    @GameTest public void overclockingSpendsMorePowerAndUnsupportedFormCardsAreRejected(GameTestHelper h) {
        var effect = MachineUpgrades.effect(List.of(card(IndustrialUpgrades.Kind.OVERCLOCKER)));
        h.assertTrue(effect.ticks(100) == 70 && effect.use(10) == 16, "Recovered source time and JE multipliers");
        h.assertTrue(effect.ticks(100) * effect.use(10) > 1000, "Speed has increased per-item JE cost");
        h.assertTrue(!MachineUpgrades.isFormUpgrade(card(IndustrialUpgrades.Kind.ENERGY_STORAGE)), "Form controllers cannot accept unsupported storage cards");
        h.assertTrue(MachineUpgrades.isFormUpgrade(card(IndustrialUpgrades.Kind.OVERCLOCKER)), "Forms share actual process speed support"); h.succeed();
    }
    @GameTest(structure="jugcraft-test:claim_combat_arena") public void pullingUsesSidedTransactionsAndCannotCrossAnotherPlayersClaim(GameTestHelper h) {
        // Place actual adjacent endpoints on opposite sides of a chunk boundary.
        var base=h.absolutePos(new BlockPos(16,2,16));
        var machineAbs=new BlockPos(Math.floorDiv(base.getX(),16)*16+15,base.getY(),base.getZ());
        var chestPos=machineAbs.east();
        // Use absolute world placement: helper-relative round trips rotate the
        // points again in rotated test arenas and can move both into one chunk.
        h.getLevel().setBlockAndUpdate(machineAbs,JimtaIndustry.MACHINES.get("jimta_macerator").defaultBlockState());
        var machine=(MachineBlockEntity)h.getLevel().getBlockEntity(machineAbs);
        h.getLevel().setBlockAndUpdate(chestPos,Blocks.CHEST.defaultBlockState());
        var chest=(ChestBlockEntity)h.getLevel().getBlockEntity(chestPos);chest.setItem(0,new ItemStack(Items.COAL,10));
        machine.setItem(machine.kind().slots, card(IndustrialUpgrades.Kind.PULLING));
        try (var claim = new NativeTestClaim(h,chestPos)) {
            h.assertTrue(!io.github.jimbozoomer.jugcraft.finance.ClaimTransfers.allowed(h.getLevel(),machine.getBlockPos(),chest.getBlockPos()),"Native endpoints cross ownership boundary: "+machine.getBlockPos()+" to "+chest.getBlockPos());
            h.assertTrue(machine.getItem(0).isEmpty(),"Machine input starts empty: "+machine.getItem(0));
            IndustrialUpgrades.transfer(machine, h.getLevel());
            h.assertTrue(chest.getItem(0).getCount() == 10 && machine.getItem(0).isEmpty(), "Protected source untouched; chest="+chest.getItem(0)+", machine="+machine.getItem(0));
        }
        IndustrialUpgrades.transfer(machine, h.getLevel());
        h.assertTrue(chest.getItem(0).getCount() == 6 && machine.getItem(0).is(Items.COAL) && machine.getItem(0).getCount() == 4, "One bounded real transfer");
        machine.setItem(0, new ItemStack(Items.COAL, 64)); IndustrialUpgrades.transfer(machine, h.getLevel());
        h.assertTrue(chest.getItem(0).getCount() == 6, "Full input never deletes source items"); h.succeed();
    }
}
