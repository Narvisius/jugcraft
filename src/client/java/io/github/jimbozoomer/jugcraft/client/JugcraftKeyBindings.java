package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;

/** Pack defaults and a one-time upgrade of the previous gun controls. No optional mod linkage. */
public final class JugcraftKeyBindings {
    private JugcraftKeyBindings() {}

    public static int defaultKey(String name, InputConstants.Type type, int value) {
        if (type != InputConstants.Type.KEYBOARD) return value;
        if (name.startsWith("key.jugcraft.") && value < 0) return InputConstants.UNKNOWN.getValue();
        // B opens the encyclopedia, N opens the optional backpack, comma creates a waypoint.
        // Saved custom keys remain untouched.
        if (name.equals("key.diorama.new_waypoint") && value == InputConstants.KEY_B) return InputConstants.KEY_COMMA;
        if (name.equals("key.travelersbackpack.inventory") && value == InputConstants.KEY_B) return InputConstants.KEY_N;
        if (value != InputConstants.KEY_R) return value;
        return switch (name) {
            case "iris.keybind.reload" -> InputConstants.KEY_F8;
            case "key.jei.showRecipe" -> InputConstants.KEY_Y;
            default -> value;
        };
    }

    /** Old Nouveau builds saved keyboard -1. In 26.3 unbound is UNKNOWN (0), not a negative SDL index. */
    public static InputConstants.Key safeSavedKey(String name, InputConstants.Key key) {
        return name.startsWith("key.jugcraft.") && key.getType() == InputConstants.Type.KEYBOARD && key.getValue() < 0
                ? InputConstants.UNKNOWN : key;
    }

    public static void register() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            Path marker = FabricLoader.getInstance().getConfigDir().resolve("jugcraft-gun-controls-v2.txt");
            if (Files.exists(marker)) return;
            if (migratePreviousDefaults(client.options.keyMappings)) {
                KeyMapping.resetMapping();
                client.options.save();
            }
            try {
                Files.createDirectories(marker.getParent());
                Files.writeString(marker, "Gun controls upgraded: R reload, F8 shaders, Y recipes. Later custom bindings are preserved.\n");
            } catch (IOException e) {
                Jugcraft.LOGGER.warn("Could not record gun controls upgrade", e);
            }
        });
    }

    /** Only exact old defaults change; custom bindings and mouse buttons remain intact. */
    public static boolean migratePreviousDefaults(KeyMapping[] mappings) {
        boolean changed = false;
        for (KeyMapping mapping : mappings) {
            InputConstants.Key key = KeyMappingHelper.getBoundKeyOf(mapping);
            if (key.getType() != InputConstants.Type.KEYBOARD) continue;
            int replacement = switch (mapping.getName()) {
                case "key.jugcraft.reload" -> key.getValue() == InputConstants.KEY_G ? InputConstants.KEY_R : key.getValue();
                case "iris.keybind.reload" -> key.getValue() == InputConstants.KEY_R ? InputConstants.KEY_F8 : key.getValue();
                case "key.jei.showRecipe" -> key.getValue() == InputConstants.KEY_R ? InputConstants.KEY_Y : key.getValue();
                default -> key.getValue();
            };
            if (replacement != key.getValue()) {
                mapping.setKey(InputConstants.Type.KEYBOARD.getOrCreate(replacement));
                changed = true;
            }
        }
        return changed;
    }
}
