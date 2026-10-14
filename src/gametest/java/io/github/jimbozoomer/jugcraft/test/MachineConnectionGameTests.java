package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.fluid.JugcraftFluids;
import io.github.jimbozoomer.jugcraft.machine.*;
import io.github.jimbozoomer.jugcraft.machine.form.FormMachineBlock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;

/** Rendered connection arms must agree with real sided ports, in either placement order. */
public class MachineConnectionGameTests {
    private record Case(MachineBlock block, Direction facing, boolean compact, boolean fluid) {}
    private record Contact(BlockPos cable, Direction towardMachine, BlockPos part) {}

    @GameTest(structure = "jugcraft-test:arms_arena", maxTicks = 3000)
    public void everyMachineFaceConnectsAfterCommandPlacement(GameTestHelper h) {
        List<Case> cases = new ArrayList<>();
        for (Block candidate : BuiltInRegistries.BLOCK) {
            if (!(candidate instanceof MachineBlock block)
                    || !BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("jugcraft")) continue;
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                cases.add(new Case(block, facing, false, false));
                if (block instanceof EnlargedMachineBlock) cases.add(new Case(block, facing, true, false));
                if (block instanceof FormMachineBlock || block.kind().keepsContents() || block.kind().isBoiler()
                        || block.kind() == MachineKind.STEEL_FOUNDRY) cases.add(new Case(block, facing, false, true));
            }
        }
        checkNext(h, cases, 0);
    }

    private void checkNext(GameTestHelper h, List<Case> cases, int index) {
        if (index == cases.size()) { h.succeed(); return; }
        Case test = cases.get(index);
        BlockPos origin = h.absolutePos(new BlockPos(10, 1, 10));
        var state = test.block.defaultBlockState().setValue(MachineBlock.FACING, test.facing);
        if (test.block instanceof LargeMachineBlock large && !test.compact) state = large.formed(state);
        h.getLevel().setBlock(origin, state, Block.UPDATE_ALL);
        if (!test.compact) test.block.setPlacedBy(h.getLevel(), origin, state, null, ItemStack.EMPTY);
        var footprint = test.block.footprint(state);
        var occupied = new HashSet<BlockPos>();
        for (int i = 0; i < footprint.size(); i++) occupied.add(footprint.partPos(origin, test.facing, i));
        List<Contact> contacts = new ArrayList<>();
        Block cable = test.fluid ? JugcraftFluids.BRONZE_FLUID_PIPE : JugcraftMachines.COPPER_CABLE;
        for (BlockPos part : occupied) for (Direction side : Direction.values()) {
            BlockPos at = part.relative(side);
            if (occupied.contains(at)) continue;
            h.getLevel().setBlock(at, cable.defaultBlockState(), Block.UPDATE_ALL);
            contacts.add(new Contact(at, side.getOpposite(), part));
        }
        // Direct placement bypasses getStateForPlacement, as structures and the local yard did.
        h.runAfterDelay(3, () -> {
            for (Contact contact : contacts) {
                Direction face = contact.towardMachine.getOpposite();
                boolean expected = test.fluid
                        ? FluidStorage.SIDED.find(h.getLevel(), contact.part, face) != null
                        : EnergyStorage.SIDED.find(h.getLevel(), contact.part, face) != null;
                boolean actual = h.getLevel().getBlockState(contact.cable)
                        .getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(contact.towardMachine));
                h.assertTrue(actual == expected, BuiltInRegistries.BLOCK.getKey(test.block) + " "
                        + test.facing + " " + contact.part + "/" + face + " fluid=" + test.fluid);
            }
            for (Contact contact : contacts) h.getLevel().setBlock(contact.cable, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            // Clear this isolated fixture without tearing down neighbours outside the test machine.
            for (BlockPos part : occupied) if (!part.equals(origin)) h.getLevel().setBlock(part, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            h.getLevel().setBlock(origin, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            checkNext(h, cases, index + 1);
        });
    }

    @GameTest(maxTicks = 40)
    public void removingAnEndpointRetractsTheCableAndPlacingItAgainReconnects(GameTestHelper h) {
        BlockPos battery = new BlockPos(2, 1, 2), cable = battery.north();
        h.setBlock(battery, JugcraftMachines.MACHINES.get(MachineKind.BATTERY_BOX));
        h.setBlock(cable, JugcraftMachines.COPPER_CABLE);
        h.runAfterDelay(3, () -> {
            h.assertTrue(h.getBlockState(cable).getValue(PipeBlock.SOUTH), "Command-placed cable extends to battery");
            h.setBlock(battery, Blocks.AIR);
            h.runAfterDelay(3, () -> {
                h.assertTrue(!h.getBlockState(cable).getValue(PipeBlock.SOUTH), "Removed battery retracts cable");
                h.setBlock(battery, JugcraftMachines.MACHINES.get(MachineKind.BATTERY_BOX));
                h.runAfterDelay(3, () -> {
                    h.assertTrue(h.getBlockState(cable).getValue(PipeBlock.SOUTH), "Battery placed after cable connects");
                    h.succeed();
                });
            });
        });
    }
}
