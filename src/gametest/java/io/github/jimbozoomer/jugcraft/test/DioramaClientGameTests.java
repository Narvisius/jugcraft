package io.github.jimbozoomer.jugcraft.test;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.Arrays;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;

/** Exercises the installed map without linking optional Diorama classes into Jugcraft. */
public class DioramaClientGameTests implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        if (!FabricLoader.getInstance().isModLoaded("diorama")) {
            context.runOnClient(client -> require(Arrays.stream(client.options.keyMappings)
                    .noneMatch(k -> k.getName().startsWith("key.diorama.")), "Absent map must not register controls"));
            Jugcraft.LOGGER.info("[diorama-check] Optional-absent startup passed; map tests skipped.");
            return;
        }
        context.runOnClient(client -> {
            var waypoint = Arrays.stream(client.options.keyMappings)
                    .filter(k -> k.getName().equals("key.diorama.new_waypoint")).findFirst().orElseThrow();
            require(waypoint.getDefaultKey().getValue() == InputConstants.KEY_COMMA, "Waypoint reset default is comma");
            require(Arrays.stream(client.options.keyMappings).noneMatch(k -> k != waypoint
                    && k.getCategory() != KeyMapping.Category.DEBUG
                    && k.getDefaultKey().equals(waypoint.getDefaultKey())), "Waypoint default must not conflict");
            require(Arrays.stream(client.options.keyMappings).noneMatch(k -> k != waypoint
                    && k.getCategory() != KeyMapping.Category.DEBUG && k.same(waypoint)),
                    "Waypoint active binding must not conflict with installed pack controls");
        });
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            context.waitTicks(100);
            context.takeScreenshot("diorama_minimap");
            open(context, InputConstants.KEY_M, "dev.diorama.MapScreen", "diorama_world_map");
            open(context, InputConstants.KEY_COMMA, "dev.diorama.waypoint.WaypointEditScreen", "diorama_new_waypoint");
            open(context, InputConstants.KEY_U, "dev.diorama.waypoint.WaypointListScreen", "diorama_waypoints");
            context.runOnClient(client -> KeyMapping.setAll());
            context.waitTicks(20);
        }
        Jugcraft.LOGGER.info("[diorama-check] World load, minimap render, M map, comma waypoint and U list passed.");
    }

    private static void open(ClientGameTestContext context, int key, String screen, String screenshot) {
        context.getInput().pressKey(key);
        context.waitFor(client -> client.gui.screen() != null
                && client.gui.screen().getClass().getName().equals(screen), 200);
        context.waitTicks(20);
        context.takeScreenshot(screenshot);
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitForScreen(null);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
