package io.github.jimbozoomer.jugcraft;

import io.github.jimbozoomer.jugcraft.config.FeatureEnabledCondition;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.fluid.JugcraftFluids;
import io.github.jimbozoomer.jugcraft.guide.JugcraftGuide;
import io.github.jimbozoomer.jugcraft.logistics.JugcraftLogistics;
import io.github.jimbozoomer.jugcraft.materials.JugcraftComponents;
import io.github.jimbozoomer.jugcraft.materials.JugcraftMaterials;
import io.github.jimbozoomer.jugcraft.materials.JugcraftWorldgen;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import io.github.jimbozoomer.jugcraft.prospecting.JugcraftProspecting;
import io.github.jimbozoomer.jugcraft.storage.JugcraftStorage;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
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
		JugcraftFluids.register();
		JugcraftLogistics.register();
		JugcraftStorage.register();
		JugcraftProspecting.register();
		JugcraftGuide.register();
		FeatureEnabledCondition.register();
		JugcraftWorldgen.register();
		registerMachineStylePack();
		LOGGER.info("Jugcraft loaded");
	}

	/**
	 * Machines look steampunk by default. The other look (classic) ships as a built-in resource pack,
	 * off by default, so anyone can switch in Options > Resource Packs. See tools/model_writer.py.
	 */
	private static void registerMachineStylePack() {
		boolean registered = FabricLoader.getInstance().getModContainer(MOD_ID)
				.map(container -> ResourceLoader.registerBuiltinPack(id("alternate_machines"), container,
						Component.translatable("pack.jugcraft.alternate_machines"), PackActivationType.NORMAL))
				.orElse(false);
		if (!registered) {
			LOGGER.warn("Could not register the built-in alternate machine style pack");
		}
	}
}
