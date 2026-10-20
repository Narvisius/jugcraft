package io.github.jimbozoomer.jugcraft;

import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.biome.JugcraftDimensions;
import io.github.jimbozoomer.jugcraft.biome.JugcraftRegions;
import io.github.jimbozoomer.jugcraft.chemistry.PetroBlocks;
import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.chemistry.PetroItems;
import io.github.jimbozoomer.jugcraft.config.FeatureEnabledCondition;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.deposit.JugcraftDeposits;
import io.github.jimbozoomer.jugcraft.electronics.JugcraftElectronics;
import io.github.jimbozoomer.jugcraft.drone.JugcraftDrones;
import io.github.jimbozoomer.jugcraft.farming.JugcraftFarming;
import io.github.jimbozoomer.jugcraft.fluid.JugcraftFluids;
import io.github.jimbozoomer.jugcraft.gear.JugcraftExosuit;
import io.github.jimbozoomer.jugcraft.gear.JugcraftGear;
import io.github.jimbozoomer.jugcraft.gear.JugcraftGrapple;
import io.github.jimbozoomer.jugcraft.guide.JugcraftGuide;
import io.github.jimbozoomer.jugcraft.kinetic.JugcraftKinetics;
import io.github.jimbozoomer.jugcraft.logistics.JugcraftLogistics;
import io.github.jimbozoomer.jugcraft.materials.JugcraftComponents;
import io.github.jimbozoomer.jugcraft.materials.JugcraftMaterials;
import io.github.jimbozoomer.jugcraft.materials.JugcraftWorldgen;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import io.github.jimbozoomer.jugcraft.prospecting.JugcraftProspecting;
import io.github.jimbozoomer.jugcraft.season.JugcraftSeasons;
import io.github.jimbozoomer.jugcraft.storage.JugcraftStorage;
import io.github.jimbozoomer.jugcraft.tools.JugcraftTools;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;

import io.github.jimbozoomer.jugcraft.solar.JugcraftSolar;
import io.github.jimbozoomer.jugcraft.weapons.JugcraftWeapons;
import io.github.jimbozoomer.jugcraft.world.AlpineSpawn;
import io.github.jimbozoomer.jugcraft.world.PixelHollows;
import io.github.jimbozoomer.jugcraft.world.RetroTrader;
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
		io.github.jimbozoomer.jugcraft.world.design.WorldDesigner.register();
		// Registration always happens, even when a feature is disabled, so saved
		// blocks and items are never lost. The config only controls acquisition.
		JugcraftMaterials.register();
		JugcraftComponents.register();
		io.github.jimbozoomer.jugcraft.machine.Electroplating.register();
		JugcraftDeposits.register();
		JugcraftMachines.register();
		io.github.jimbozoomer.jugcraft.machine.form.JugcraftForms.register();
		io.github.jimbozoomer.jugcraft.machine.form.IndustrialForms.register();
		JugcraftFluids.register();
		PetroFluids.register();
		PetroItems.register();
		JugcraftWeapons.register();
		JugcraftSolar.register();
		PetroBlocks.register();
		JugcraftLogistics.register();
		io.github.jimbozoomer.jugcraft.wallrun.WallRuns.register();
		JugcraftStorage.register();
		JugcraftElectronics.register();
		io.github.jimbozoomer.jugcraft.control.JugcraftControl.register();
		io.github.jimbozoomer.jugcraft.rocketry.JugcraftRocketry.register();
		io.github.jimbozoomer.jugcraft.building.Dieselworks.register();
		io.github.jimbozoomer.jugcraft.building.Kaiserworks.register();
		io.github.jimbozoomer.jugcraft.building.Trenchworks.register();
		io.github.jimbozoomer.jugcraft.airship.JugcraftAirships.register();
		io.github.jimbozoomer.jugcraft.walker.JugcraftWalkers.register();
		io.github.jimbozoomer.jugcraft.landship.JugcraftLandships.register();
		io.github.jimbozoomer.jugcraft.artillery.JugcraftArtillery.register();
		io.github.jimbozoomer.jugcraft.artillery.JugcraftTowerGuns.register();
		io.github.jimbozoomer.jugcraft.building.Fortifications.register();
		io.github.jimbozoomer.jugcraft.building.Bunkerworks.register();
		io.github.jimbozoomer.jugcraft.building.AgedWood.register();
		io.github.jimbozoomer.jugcraft.building.FireControl.register();
		io.github.jimbozoomer.jugcraft.raiders.JugcraftRaiders.register();
		JugcraftFarming.register();
		JugcraftProspecting.register();
		JugcraftKinetics.register();
		JugcraftTools.register();
		JugcraftGear.register();
		JugcraftExosuit.register();
		JugcraftGrapple.register();
		io.github.jimbozoomer.jugcraft.weapons.JugcraftArms.register();
		io.github.jimbozoomer.jugcraft.guns.JugcraftGuns.register();
		io.github.jimbozoomer.jugcraft.weapons.FieldChemistry.register();
		io.github.jimbozoomer.jugcraft.chemistry.ConstructionChemistry.register();
		JugcraftGuide.register();
		JugcraftAgriculture.register();
		io.github.jimbozoomer.jugcraft.lair.JugcraftLairs.register();
		io.github.jimbozoomer.jugcraft.lair.vesperine.JugcraftVesperine.register();
		io.github.jimbozoomer.jugcraft.lair.tatterlace.JugcraftTatterlace.register();
		io.github.jimbozoomer.jugcraft.lair.yeti.JugcraftYeti.register();
		io.github.jimbozoomer.jugcraft.lair.tyrant.JugcraftTyrant.register();
		io.github.jimbozoomer.jugcraft.lair.hag.JugcraftHag.register();
		io.github.jimbozoomer.jugcraft.lair.lich.JugcraftLich.register();
		io.github.jimbozoomer.jugcraft.lair.dreadnought.JugcraftDreadnought.register();
		io.github.jimbozoomer.jugcraft.lair.alpha.JugcraftAlpha.register();
		io.github.jimbozoomer.jugcraft.styx.JugcraftStyx.register();
		JugcraftDrones.register();
		io.github.jimbozoomer.jugcraft.tower.JugcraftTower.register();
		io.github.jimbozoomer.jugcraft.blueprint.JugcraftBlueprints.register();
		io.github.jimbozoomer.jugcraft.placement.StructureReach.register();
		// A tank a kit placed comes down whole when any block of it is broken, giving the kit back.
		io.github.jimbozoomer.jugcraft.nuclear.TankKitBreaking.register();
		io.github.jimbozoomer.jugcraft.powerline.JugcraftPowerLines.register();
		io.github.jimbozoomer.jugcraft.nuclear.JugcraftNuclear.register();
		io.github.jimbozoomer.jugcraft.energy.CreativeEnergyCellBlock.register();
		io.github.jimbozoomer.jugcraft.drone.CreativeSupplyCrateBlock.register();
		PixelHollows.register();
		RetroTrader.register();
		AlpineSpawn.register();
		JugcraftRegions.register();
		JugcraftDimensions.register();
		JugcraftSeasons.register();
		new io.github.jimbozoomer.jugcraft.creatures.scary.ScaryMod().onInitialize();
		io.github.jimbozoomer.jugcraft.town.JugcraftTown.register();
		io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance.register();
		io.github.jimbozoomer.jugcraft.nouveau.JugNouveau.register();
		io.github.jimbozoomer.jugcraft.menagerie.Menagerie.register();
		io.github.jimbozoomer.jugcraft.diagonal.DiagonalWalls.register();
		io.github.jimbozoomer.jugcraft.vehiclebay.VehicleBays.register();
		FeatureEnabledCondition.register();
		JugcraftWorldgen.register();
		JugcraftParties.register();
		registerMachineStylePack();
		// Higher-priority data overrides only exist when the optional backpack mod is installed.
		if (FabricLoader.getInstance().isModLoaded("travelersbackpack")) {
			FabricLoader.getInstance().getModContainer(MOD_ID).ifPresent(container ->
					ResourceLoader.registerBuiltinPack(id("travelers_backpack"), container,
							Component.literal("Jugcraft: Backpack Progression"), PackActivationType.ALWAYS_ENABLED));
		}
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
