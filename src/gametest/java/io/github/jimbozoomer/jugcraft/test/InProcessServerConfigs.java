package io.github.jimbozoomer.jugcraft.test;

import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;

/** Fixture repair for Fabric's client and dedicated test server sharing a JVM, never shipped in Jugcraft. */
public final class InProcessServerConfigs {
	private InProcessServerConfigs() {}

	public static void beforeReconnect(TestDedicatedServerContext server) {
		if (!FabricLoader.getInstance().isModLoaded("forgeconfigapiport")) return;
		reload(server);
	}

	public static void beforeAdditionalClient(TestDedicatedServerContext server) {
		// The first in-process client also replaces the singleton with its received
		// remote config (which has no disk path). Restore the real test-server files
		// before another, separate client requests them. Keep normal config sync.
		beforeReconnect(server);
	}

	private static void reload(TestDedicatedServerContext server) {
		// FCAP's remote-client disconnect unloads SERVER configs. In this harness that also clears
		// the still-running server's singleton registry. A real separate server process retains it.
		// Reload the same global/world files before the next handshake; do not bypass config sync.
		server.runOnServer(minecraft -> {
			ConfigTracker.INSTANCE.loadConfigs(ModConfig.Type.SERVER, FabricLoader.getInstance().getConfigDir(),
					minecraft.getWorldPath(LevelResource.ROOT).resolve("serverconfig"));
			for (var config : ModConfigs.getConfigSet(ModConfig.Type.SERVER)) {
				if (config.getFullPath() == null || config.getLoadedConfig() == null)
					throw new AssertionError("Test server config was not restored: " + config.getFileName());
			}
		});
	}
}
