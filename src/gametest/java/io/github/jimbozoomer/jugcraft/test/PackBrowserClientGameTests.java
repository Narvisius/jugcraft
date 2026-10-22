package io.github.jimbozoomer.jugcraft.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;

/** Real optional-library screens in a disposable client; never installs a pack. */
public class PackBrowserClientGameTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        if (FabricLoader.getInstance().isModLoaded("diorama")) {
            context.runOnClient(client -> {
                try {
                    var settings = Class.forName("dev.diorama.Settings");
                    var current = settings.getMethod("reload").invoke(null);
                    if (settings.getField("minimap").getBoolean(current))
                        throw new AssertionError("Fresh pack must start with minimap off");
                    if (!settings.getField("waypointsOnMap").getBoolean(current)
                            || settings.getField("minimapSize").get(current) == null)
                        throw new AssertionError("Partial pack config must retain upstream defaults");
                    settings.getField("minimap").setBoolean(current, true);
                    settings.getMethod("save").invoke(current);
                    current = settings.getMethod("reload").invoke(null);
                    if (!settings.getField("minimap").getBoolean(current))
                        throw new AssertionError("Player can re-enable and save minimap");
                    settings.getField("minimap").setBoolean(current, false);
                    settings.getMethod("save").invoke(current);
                } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
            });
        }
        if (!FabricLoader.getInstance().isModLoaded("resourcify")) return;
        context.runOnClient(client -> {
            try {
                var field = Class.forName("dev.dediamondpro.resourcify.util.MultiThreading").getDeclaredField("pool");
                field.setAccessible(true);
                var pool = (java.util.concurrent.ExecutorService) field.get(null);
                if (!pool.submit(() -> Thread.currentThread().isDaemon()).get(5, java.util.concurrent.TimeUnit.SECONDS))
                    throw new AssertionError("Browser workers must not keep Minecraft alive after quit");
            } catch (Exception e) { throw new AssertionError(e); }
        });
        for (String type : new String[]{"RESOURCE_PACK", "IRIS_SHADER"}) {
            context.runOnClient(client -> {
                try {
                    var projectType = Class.forName("dev.dediamondpro.resourcify.services.ProjectType");
                    var screen = Class.forName("dev.dediamondpro.resourcify.gui.browsepage.BrowseScreen");
                    var constructor = screen.getConstructor(projectType, java.io.File.class,
                            Class.forName("dev.dediamondpro.resourcify.services.IService"), int.class,
                            Class.forName("kotlin.jvm.internal.DefaultConstructorMarker"));
                    var folder = new java.io.File(client.gameDirectory,
                            type.equals("RESOURCE_PACK") ? "resourcepacks" : "shaderpacks");
                    client.gui.setScreen((Screen) constructor.newInstance(projectType.getField(type).get(null),
                            folder, null, 4, null));
                } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
            });
            context.waitTicks(80);
            context.runOnClient(client -> {
                if (!client.gui.screen().getClass().getName().endsWith(".BrowseScreen"))
                    throw new AssertionError("Browser did not stay open");
            });
            context.takeScreenshot("jugcraft_browser_" + type.toLowerCase(java.util.Locale.ROOT));
        }
        context.runOnClient(client -> client.gui.setScreen(new TitleScreen()));
    }
}
