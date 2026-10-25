package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.jimtafacts.network.*;
import io.github.jimbozoomer.jugcraft.tools.Chargeable;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Authored now; execute with the final combined implementation suite. */
public final class JimtafactsEquipmentGameTests {
    @GameTest public void namePressConsumesOneIngotAndKnifeDurability(GameTestHelper h) {
        var p = h.makeMockServerPlayerInLevel(); p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); var knife = new ItemStack(DigitalStorage.ITEMS.get("certus_quartz_cutting_knife")); p.setItemInHand(InteractionHand.MAIN_HAND, knife);
        var menu = new DigitalPressMenu.Menu(1, p.getInventory(), InteractionHand.MAIN_HAND, knife);
        menu.slots.get(0).set(new ItemStack(Items.IRON_INGOT, 2)); menu.name("Workshop pick");
        var output = menu.slots.get(1).remove(1); menu.slots.get(1).onTake(p, output);
        h.assertTrue(output.is(DigitalStorage.ITEMS.get("name_press")) && output.getOrDefault(DigitalEquipment.PRESS_NAME, "").equals("Workshop pick"), "Output carries the engraved name");
        h.assertTrue(menu.slots.get(0).getItem().getCount() == 1 && knife.getDamageValue() == 1, "One ingot and one durability paid exactly once");
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); h.assertTrue(!menu.stillValid(p), "Moving the knife invalidates the menu"); menu.removed(p); h.succeed();
    }
    @GameTest public void memorySettingsStripComponentsAndNeverCloneCells(GameTestHelper h) {
        var cell = new ItemStack(DigitalStorage.ITEMS.get("item_storage_cell_1k"));
        cell.set(DigitalStorage.CONTENTS, new CellContents(java.util.List.of(new CellContents.Entry(new ItemStack(Items.DIAMOND), 900))));
        var settings = new DigitalEquipment.Settings("export_bus", cell, 120, true);
        h.assertTrue(settings.filter().getCount() == 1 && !settings.filter().has(DigitalStorage.CONTENTS), "Copied filter holds only item identity");
        var borrowed = settings.filter(); borrowed.shrink(1); h.assertTrue(!settings.filter().isEmpty(), "Saved sample is immutable to callers"); h.succeed();
    }
    @GameTest public void entropyCannotAlterAnotherPlayersClaim(GameTestHelper h) {
        var p = h.makeMockServerPlayerInLevel(); var pos = new BlockPos(2, 2, 2); h.setBlock(pos, Blocks.COBBLESTONE);
        var tool = new ItemStack(DigitalStorage.ITEMS.get("entropy_manipulator")); Chargeable.setEnergy(tool, 10_000); p.setItemInHand(InteractionHand.MAIN_HAND, tool);
        var claim = TestClaims.open().block(h.absolutePos(pos), UUID.randomUUID());
        try { tool.getItem().useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(h.absolutePos(pos)), Direction.UP, h.absolutePos(pos), false)));
            h.assertTrue(h.getBlockState(pos).is(Blocks.COBBLESTONE) && Chargeable.energy(tool) == 10_000, "No mutation or debit on protected block");
        } finally { claim.close(); }
        h.succeed();
    }
    @GameTest public void acceleratedGrowthRespectsClaimAtBudDestination(GameTestHelper h) {
        var p = h.makeMockServerPlayerInLevel(); var source = new BlockPos(2, 2, 2); h.setBlock(source, Blocks.BUDDING_AMETHYST);
        var claim = TestClaims.open(); for (Direction d : Direction.values()) { h.setBlock(source.relative(d), Blocks.AIR); claim.block(h.absolutePos(source.relative(d)), UUID.randomUUID()); }
        try { for (int i = 0; i < 200; i++) DigitalFixtures.GrowthNode.grow(h.getLevel(), p, h.absolutePos(source), net.minecraft.util.RandomSource.create(i));
            for (Direction d : Direction.values()) h.assertTrue(h.getBlockState(source.relative(d)).isAir(), "Protected candidate never receives a bud");
        } finally { claim.close(); }
        boolean grew = false; var random = net.minecraft.util.RandomSource.create(220);
        for (int i = 0; i < 100 && !grew; i++) grew = DigitalFixtures.GrowthNode.grow(h.getLevel(), p, h.absolutePos(source), random);
        h.assertTrue(grew, "The same powered growth operation works once permission permits it"); h.succeed();
    }
}
