package io.github.jimbozoomer.jugcraft;

import io.github.jimbozoomer.jugcraft.config.FeatureEnabledCondition;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.materials.JugcraftComponents;
import io.github.jimbozoomer.jugcraft.materials.JugcraftMaterials;
import io.github.jimbozoomer.jugcraft.materials.JugcraftWorldgen;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Jugcraft implements ModInitializer {
	public static final String MOD_ID = "jugcraft";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		JugcraftConfig.load();
		// Registration always happens, even when a feature is disabled, so saved
		// blocks and items are never lost. The config only controls acquisition.
		JugcraftMaterials.register();
		JugcraftComponents.register();
		JugcraftMachines.register();
		FeatureEnabledCondition.register();
		JugcraftWorldgen.register();
		LOGGER.info("Jugcraft loaded");
	}
}
