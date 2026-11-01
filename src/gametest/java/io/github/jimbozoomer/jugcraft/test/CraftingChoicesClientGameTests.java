package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.client.CraftingChoicesClient;
import io.github.jimbozoomer.jugcraft.crafting.CraftingChoices;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;

public final class CraftingChoicesClientGameTests implements FabricClientGameTest {
    private static void press(Button button) { button.onPress(new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0)); }
    @Override public void runTest(ClientGameTestContext c) {
        try(var world=c.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();world.getServer().runCommand("gamemode survival @a");
            world.getServer().runOnServer(server->{
                var p=server.getPlayerList().getPlayers().getFirst();var pos=p.blockPosition().below();
                p.level().setBlockAndUpdate(pos,Blocks.CRAFTING_TABLE.defaultBlockState());
                p.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inv,who)->new CraftingMenu(id,inv,ContainerLevelAccess.create(p.level(),pos)),net.minecraft.network.chat.Component.literal("Crafting")));
                var grid=CraftingChoices.grid(p.containerMenu);for(int i=0;i<9;i++)grid.inputs().setItem(i,new ItemStack(Items.CARROT,2));
            });
            c.waitForScreen(CraftingScreen.class);
            c.waitFor(mc->CraftingChoicesClient.button(mc.gui.screen())!=null&&CraftingChoicesClient.button(mc.gui.screen()).visible);
            c.takeScreenshot("crafting_choices_table_button");
            c.runOnClient(mc->press(CraftingChoicesClient.button(mc.gui.screen())));
            c.waitForScreen(CraftingChoicesClient.Picker.class);c.waitTicks(3);c.takeScreenshot("crafting_choices_results");
            c.runOnClient(mc->press((Button)Screens.getWidgets(mc.gui.screen()).stream().filter(b->b.getMessage().getString().contains("Giant Carrot")).findFirst().orElseThrow()));
            c.waitForScreen(CraftingScreen.class);
            c.waitFor(mc->net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(mc.player.containerMenu.getSlot(0).getItem().getItem()).getPath().equals("giant_carrot"));
            c.takeScreenshot("crafting_choices_selected_result");
            world.getServer().runOnServer(server->{
                var p=server.getPlayerList().getPlayers().getFirst();var grid=CraftingChoices.grid(p.containerMenu);
                for(int i=0;i<9;i++)if(grid.inputs().getItem(i).getCount()!=2)throw new AssertionError("Choosing a result must not consume ingredients");
                p.closeContainer();
                p.inventoryMenu.getSlot(1).set(new ItemStack(Items.MILK_BUCKET));
            });
            c.waitFor(mc->mc.gui.screen()==null);c.getInput().pressKey(o->o.keyInventory);
            c.waitForScreen(InventoryScreen.class);
            c.waitFor(mc->CraftingChoicesClient.button(mc.gui.screen())!=null&&CraftingChoicesClient.button(mc.gui.screen()).visible);
            c.takeScreenshot("crafting_choices_inventory_button");
            c.runOnClient(mc->press(CraftingChoicesClient.button(mc.gui.screen())));
            c.waitForScreen(CraftingChoicesClient.Picker.class);
            c.runOnClient(mc->press((Button)Screens.getWidgets(mc.gui.screen()).stream().filter(b->b.getMessage().getString().contains("Carrot")).findFirst().orElseThrow()));
            c.waitForScreen(InventoryScreen.class);c.waitFor(mc->mc.player.inventoryMenu.getSlot(0).getItem().is(Items.CARROT));
            c.takeScreenshot("crafting_choices_inventory_selected");
            c.getInput().pressKey(InputConstants.KEY_ESCAPE);
        }
    }
}
