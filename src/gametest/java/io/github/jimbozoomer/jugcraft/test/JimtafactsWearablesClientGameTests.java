package io.github.jimbozoomer.jugcraft.test;

import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.trinkets.api.client.TrinketRendererRegistry;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.jimtafacts.JimtafactsWearables;
import io.github.jimbozoomer.jugcraft.concordance.trinket.WornDisplay;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

/** Run only with the combined integration validation; screenshots are evidence for human visual review. */
public final class JimtafactsWearablesClientGameTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        // Creative replaces InventoryScreen with its catalog, hiding the equipped player.
        try (var world = context.worldBuilder().adjustSettings(s -> s.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("gamerule minecraft:spawn_mobs false");
            context.runOnClient(mc -> {
                for (String name : JimtafactsWearables.names()) {
                    var item = BuiltInRegistries.ITEM.getValue(Jugcraft.id("jimta_" + name));
                    if (!TrinketRendererRegistry.hasRenderer(item)) throw new AssertionError("No worn renderer: " + name);
                    for (boolean slim : new boolean[]{false, true}) for (HumanoidArm arm : HumanoidArm.values()) {
                        var part = JimtafactsWearables.geometry(name, slim, arm, true).bakeRoot();
                        if (part.getAllParts().stream().allMatch(p -> p.isEmpty())) throw new AssertionError("Empty original mesh: " + name);
                    }
                }
            });
            for (String name : JimtafactsWearables.names()) {
                world.getServer().runOnServer(server -> {
                    var player = server.getPlayerList().getPlayers().getFirst();
                    WornDisplay.choose(player, true);
                    var trinkets = TrinketsApi.getAttachment(player);
                    trinkets.forEach((access, stack) -> access.set(ItemStack.EMPTY));
                    ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id("jimta_" + name)));
                    boolean equipped = false;
                    for (var inventory : trinkets.getInventories().values()) {
                        var access = inventory.getOrCreateSlotAccess(0);
                        if (access.isValid() && access.slotType().validatorCheck(stack, access, player)) {
                            access.set(stack); equipped = true; break;
                        }
                    }
                    if (!equipped) throw new AssertionError("No equip route: " + name);
                });
                context.waitTicks(5);
                context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
                context.waitTicks(3);
                context.runOnClient(mc -> {
                    if (!(mc.gui.screen() instanceof InventoryScreen)) throw new AssertionError("Wearable preview must show the player inventory");
                });
                context.takeScreenshot("jimtafacts_worn_" + name);
                context.setScreen(() -> null);
            }
        }
    }
}
