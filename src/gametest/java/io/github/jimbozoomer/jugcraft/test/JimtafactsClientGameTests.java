package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.jimtafacts.Jimtafacts;
import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaSpellbooks;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public final class JimtafactsClientGameTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().adjustSettings(s -> s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                var inventory = new SimpleContainer(54); int slot = 0;
                for (var item : Jimtafacts.ITEMS.values()) inventory.setItem(slot++, new ItemStack(item));
                // Exercise the lazily initialized Gecko item renderer, not only flat icons.
                inventory.setItem(53, new ItemStack(JimtaSpellbooks.ITEMS.get("tyros_statue")));
                player.openMenu(new SimpleMenuProvider((id, inv, who) -> new ChestMenu(MenuType.GENERIC_9x6, id, inv, inventory, 6), Component.literal("Jimtafacts — supplied item art")));
            });
            context.waitTicks(15);
            context.runOnClient(mc -> {
                if (!(mc.player.containerMenu instanceof ChestMenu menu)) throw new AssertionError("Accessory gallery not synchronized");
                for (int i = 0; i < 49; i++) if (menu.getSlot(i).getItem().isEmpty()) throw new AssertionError("Missing accessory " + i);
                if (!menu.getSlot(53).getItem().is(JimtaSpellbooks.ITEMS.get("tyros_statue"))) throw new AssertionError("Missing statue render fixture");
            });
            context.takeScreenshot("jimtafacts_supplied_accessory_icons");
            context.runOnClient(mc -> mc.player.closeContainer());
        }
    }
}
