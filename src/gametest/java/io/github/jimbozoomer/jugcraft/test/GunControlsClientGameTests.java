package io.github.jimbozoomer.jugcraft.test;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.client.GunsClient;
import io.github.jimbozoomer.jugcraft.client.JugcraftKeyBindings;
import io.github.jimbozoomer.jugcraft.guns.EnergyCellItem;
import io.github.jimbozoomer.jugcraft.guns.GunItem;
import io.github.jimbozoomer.jugcraft.guns.GunShots;
import io.github.jimbozoomer.jugcraft.guns.JugcraftGuns;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** Regression for S0wc2rx: saved invalid keys, focus recovery, R reload and unobstructed gun icons. */
public class GunControlsClientGameTests implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var reload = GunsClient.reloadKey();
            require(reload.getDefaultKey().getValue() == InputConstants.KEY_R, "Reload default must be R");
            for (var mapping : client.options.keyMappings) {
                int expected = switch (mapping.getName()) {
                    case "iris.keybind.reload" -> InputConstants.KEY_F8;
                    case "key.jei.showRecipe" -> InputConstants.KEY_Y;
                    default -> -1;
                };
                if (expected >= 0) {
                    require(mapping.getDefaultKey().getValue() == expected, "Reset default: " + mapping.getName());
                    mapping.setKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_R));
                }
                if (mapping.getName().startsWith("key.jugcraft.nouveau_")) {
                    require(mapping.getDefaultKey().equals(InputConstants.UNKNOWN), "Nouveau default must be unbound");
                    // Options.load uses setKey with exactly this persisted key.keyboard.-1 value.
                    mapping.setKey(InputConstants.Type.KEYBOARD.getOrCreate(-1));
                    require(mapping.isUnbound(), "Old saved Nouveau key must be repaired before focus recovery");
                }
            }
            reload.setKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_G));
            require(JugcraftKeyBindings.migratePreviousDefaults(client.options.keyMappings), "Upgrade previous bindings");
            require(KeyMappingHelper.getBoundKeyOf(reload).getValue() == InputConstants.KEY_R, "Old G reload must become R");
            require(!JugcraftKeyBindings.migratePreviousDefaults(client.options.keyMappings), "Migration must be idempotent");
            reload.setKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_F9));
            JugcraftKeyBindings.migratePreviousDefaults(client.options.keyMappings);
            require(KeyMappingHelper.getBoundKeyOf(reload).getValue() == InputConstants.KEY_F9, "Preserve custom binding");
            reload.setKey(InputConstants.Type.MOUSE.getOrCreate(4));
            JugcraftKeyBindings.migratePreviousDefaults(client.options.keyMappings);
            require(KeyMappingHelper.getBoundKeyOf(reload).getType() == InputConstants.Type.MOUSE, "Preserve mouse binding");
            reload.setKey(reload.getDefaultKey());
            KeyMapping.resetMapping();
            for (var mapping : client.options.keyMappings) {
                if (mapping != reload && mapping.getCategory() != KeyMapping.Category.DEBUG) {
                    require(!mapping.same(reload), "R conflict: " + mapping.getName());
                }
            }
            KeyMapping.setAll();
        });
        try (var world = context.worldBuilder().adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
            world.getConnection().waitForChunksRender();
            context.runOnClient(client -> {
            for (var gun : JugcraftGuns.GUNS.values()) {
                var stack = new ItemStack(gun);
                for (int rounds : new int[] {0, gun.spec().capacity() / 2, gun.spec().capacity()}) {
                    GunItem.setLoaded(stack, rounds);
                    require(!stack.isBarVisible(), "Gun inventory bar: " + gun.name());
                }
            }
            });
            var server = world.getServer();
            server.runCommand("gamerule minecraft:send_command_feedback false");
            for (String name : List.of("rust_midge", "thunderpipe", "beam_pistol")) {
                var gun = JugcraftGuns.GUNS.get(name);
                var spec = gun.spec();
                var ammo = JugcraftGuns.ammo(spec);
                int charge = JugcraftGuns.charge(gun);
                server.runCommand("clear @p");
                server.runCommand("item replace entity @p weapon.mainhand with jugcraft:%s[jugcraft:loaded_rounds=%d]".formatted(name, spec.capacity() - 1));
                String ammoId = BuiltInRegistries.ITEM.getKey(ammo).toString();
                server.runCommand(charge > 0 ? "give @p %s[jugcraft:energy=%dL]".formatted(ammoId, EnergyCellItem.CAPACITY)
                        : "give @p %s 32".formatted(ammoId));
                context.waitTicks(30);
                context.runOnClient(client -> client.gui.setScreen(new InventoryScreen(client.player)));
                context.waitForScreen(InventoryScreen.class);
                context.getInput().pressKey(InputConstants.KEY_ESCAPE);
                context.waitForScreen(null);
                context.runOnClient(client -> KeyMapping.setAll());
                context.getInput().pressKey(InputConstants.KEY_R);
                context.waitTicks(spec.reloadTicks(1) + 20);
                server.runOnServer(s -> {
                    var p = s.getPlayerList().getPlayers().getFirst();
                    require(GunItem.loaded(p.getMainHandItem()) == spec.capacity(), "Physical R reload: " + name);
                    if (charge > 0) require(GunShots.charged(p.getInventory(), ammo) == EnergyCellItem.CAPACITY - charge, "Energy conservation");
                    else require(GunShots.count(p.getInventory(), ammo) == 31, "Ammunition conservation: " + name);
                });
                context.takeScreenshot("gun_controls_" + name + "_reloaded");
            }
            server.runOnServer(s -> {
                var p = s.getPlayerList().getPlayers().getFirst();
                p.getInventory().clearContent();
                int slot = 9;
                for (String name : List.of("rust_midge", "thunderpipe", "beam_pistol")) {
                    var gun = JugcraftGuns.GUNS.get(name);
                    for (int rounds : new int[] {0, gun.spec().capacity() / 2, gun.spec().capacity()}) {
                        var stack = new ItemStack(gun);
                        GunItem.setLoaded(stack, rounds);
                        p.getInventory().setItem(slot++, stack);
                    }
                }
            });
            context.waitTicks(10);
            context.runOnClient(client -> { client.gui.toastManager().clear(); client.gui.setScreen(new InventoryScreen(client.player)); });
            context.waitForScreen(InventoryScreen.class);
            context.takeScreenshot("guns_inventory_no_ammunition_bars");
            context.getInput().pressKey(InputConstants.KEY_ESCAPE);
            context.waitForScreen(null);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
