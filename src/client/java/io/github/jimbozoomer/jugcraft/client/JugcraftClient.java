package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.agriculture.HarvestMoon;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.OpenCarvingPayload;
import io.github.jimbozoomer.jugcraft.agriculture.OpenEpitaphPayload;
import io.github.jimbozoomer.jugcraft.agriculture.SpookyBurstPayload;
import io.github.jimbozoomer.jugcraft.client.arms.ArmsMotion;
import io.github.jimbozoomer.jugcraft.client.arms.FlailHeads;
import io.github.jimbozoomer.jugcraft.client.arms.TwoHandedInput;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlock;
import io.github.jimbozoomer.jugcraft.drone.JugcraftDrones;
import io.github.jimbozoomer.jugcraft.fluid.JugcraftFluids;
import io.github.jimbozoomer.jugcraft.fluid.StoredFluid;
import io.github.jimbozoomer.jugcraft.guide.EngineersHandbookItem;
import io.github.jimbozoomer.jugcraft.kinetic.JugcraftKinetics;
import io.github.jimbozoomer.jugcraft.logistics.JugcraftLogistics;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.prospecting.SurveyPayload;
import io.github.jimbozoomer.jugcraft.solar.JugcraftSolar;
import io.github.jimbozoomer.jugcraft.tools.JugcraftTools;
import io.github.jimbozoomer.jugcraft.weapons.JugcraftWeapons;
import io.github.jimbozoomer.jugcraft.weapons.WeaponArtPayload;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

/**
 * Client entrypoint: machine and Cooking Pot menus to their screens, the wind turbine and belt renderers,
 * the handbook to its book, ore surveys to the prospector screen, carved and giant pumpkins to their renderers and carving screen,
 * pumpkin boats to theirs, gravestones to the renderer of their engravings, and Halloween's night creatures,
 * thrown pumpkins and landing markers to theirs; the Harvest Moon's state to the pumpkins' sparks; the drone
 * depot's renderers (drones, pickup lift, control room screen) and terminal screen, and the server's season to
 * grass and foliage colours, and the Party key to the Party screen; and the mesh item models the Runebound arms are
 * drawn with (MeshItemModels).
 */
public final class JugcraftClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MeshItemModels.register();
		StructureItemRenderers.register();
		new io.github.jimbozoomer.jugcraft.client.scary.ScaryClient().onInitializeClient();
		for (MachineKind kind : MachineKind.values()) {
			MenuScreens.register(JugcraftMachines.menuType(kind), MachineScreen::new);
		}
		BlockEntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.nuclear.PlantInterior.SIGN_ENTITY, PlantSignRenderer::new);
		BlockEntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.nuclear.PlantInterior.STATUS_ENTITY, StatusScreenRenderer::new);
		BlockEntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.nuclear.PlantInterior.LIVE_PANEL_ENTITY, LivePanelRenderer::new);
		BlockEntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.nuclear.PlantInterior.LOCKER_ENTITY, LockerRenderer::new);
		BlockEntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.nuclear.PlantInterior.RACK_ENTITY, StorageRackRenderer::new);
		BlockEntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.nuclear.PlantInterior.ROLLER_ENTITY, RollerDoorRenderer::new);
		SpeedGateRenderer.register();
		BlockEntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.nuclear.StorageTanks.PORT_ENTITY, StorageTankRenderer::new);
		BlockEntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.nuclear.StorageTanks.GATE_ENTITY, RailingGateRenderer::new);
		BlockEntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.nuclear.StorageTanks.HATCH_ENTITY, StorageTankHatchRenderer::new);
		MenuScreens.register(JugcraftAgriculture.COOKING_POT_MENU, CookingPotScreen::new);
		MenuScreens.register(io.github.jimbozoomer.jugcraft.rocketry.JugcraftRocketry.ROCKET_PAD_MENU, RocketPadScreen::new);
		net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.rocketry.JugcraftRocketry.ZIPLINE_RIDER,
				net.minecraft.client.renderer.entity.NoopRenderer::new);
		BlockEntityRenderers.register(io.github.jimbozoomer.jugcraft.rocketry.JugcraftRocketry.ZIPLINE_ANCHOR_ENTITY, ZiplineRenderer::new);
		EntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.rocketry.JugcraftRocketry.COMBAT_ROCKET, ThrownItemRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftMachines.MACHINE_ENTITY, WindTurbineRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftKinetics.BELT_PULLEY_ENTITY, BeltRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftKinetics.SHAFT_ENTITY, KineticRotorRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftLogistics.CONVEYOR_ENTITY, ConveyorRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftTools.CHARGING_STATION_ENTITY, ChargingStationRenderer::new);
		BlockEntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.powerline.JugcraftPowerLines.ANCHOR_ENTITY, context -> new LineRenderer());
		// Every MBS item shows a see-through copy of what it would place (the power lines' preview among them).
		MbsPreview.register();
		WallRunModel.register();
		io.github.jimbozoomer.jugcraft.nuclear.StorageTanks.openScreen = pos -> net.minecraft.client.Minecraft.getInstance().gui.setScreen(new StorageTankScreen(pos));
		WallRunOverlay.register();
		RocketPackClient.register();
		BroomstickClient.register();
		BalloonClient.register();
		EntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.airship.JugcraftAirships.ZEPPELIN, ZeppelinRenderer::new);
		// The raider faction (batch 57): infantry in uniform, the walker and blimp in raider paint, and their bombs.
		EntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.raiders.JugcraftRaiders.GRUNT, RaiderRenderer::new);
		EntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.raiders.JugcraftRaiders.GRENADIER, RaiderRenderer::new);
		EntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.raiders.JugcraftRaiders.OFFICER, RaiderRenderer::new);
		EntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.raiders.JugcraftRaiders.WALKER, RaiderWalkerRenderer::new);
		EntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.raiders.JugcraftRaiders.BLIMP, RaiderBlimpRenderer::new);
		EntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.raiders.JugcraftRaiders.BOMB, ThrownItemRenderer::new);
		net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry.registerModelLayer(RaiderModel.LAYER, TownsfolkModel::createLayer);
		ZeppelinClient.register();
		EntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.walker.JugcraftWalkers.DIESEL_WALKER, DieselWalkerRenderer::new);
		EntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.walker.JugcraftWalkers.ARMOURED_WALKER, ArmouredWalkerRenderer::new);
		WalkerClient.register();
		EntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.landship.JugcraftLandships.LANDSHIP, LandshipRenderer::new);
		EntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.landship.JugcraftLandships.SHELL, ThrownItemRenderer::new);
		LandshipClient.register();
		EntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.artillery.JugcraftArtillery.SIEGE_MORTAR, ArtilleryRenderers.Mortar::new);
		EntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.artillery.JugcraftArtillery.HOWITZER, ArtilleryRenderers.Howitzer::new);
		EntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.artillery.JugcraftArtillery.FLAK_GUN, ArtilleryRenderers.Flak::new);
		EntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.artillery.JugcraftArtillery.BALLOON, ArtilleryRenderers.Balloon::new);
		EntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.artillery.JugcraftArtillery.HEAVY_SHELL, ThrownItemRenderer::new);
		EntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.artillery.JugcraftArtillery.FLAK_SHELL, ThrownItemRenderer::new);
		for (var type : io.github.jimbozoomer.jugcraft.artillery.JugcraftTowerGuns.TYPES.values()) {
			EntityRendererRegistry.register(type, TowerGunRenderer::new);
		}
		EntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.artillery.JugcraftTowerGuns.GREAT_SHELL, ThrownItemRenderer::new);
		ArtilleryClient.register();
		BlockEntityRenderers.register(io.github.jimbozoomer.jugcraft.building.Trenchworks.SEARCHLIGHT_ENTITY, SearchlightRenderer::new);
		ArmsMotion.load();
		FlailHeads.load();
		FlailHeads.register();
		TwoHandedInput.register();
		ClientPlayNetworking.registerGlobalReceiver(WeaponArtPayload.TYPE, (payload, context) -> ArmsMotion.receive(payload));
		EntityRendererRegistry.register(JugcraftWeapons.GRENADE, ThrownItemRenderer::new);
		EntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.weapons.JugcraftArms.THROWN_ARM,
				io.github.jimbozoomer.jugcraft.client.arms.ThrownArmRenderer::new);
		PetroFluidsClient.register();
		RocketPackLayer.register();
		HazmatLayer.register();
		UniformLayer.register();
		RadiationHud.register();
		RadiationNeedle.register();
		GhostSheetLayer.register();
		CostumeLayer.register();
		FacePaintLayer.register();
		WornModelLayer.register();
		BlockEntityRendererRegistry.register(JugcraftKinetics.HAND_CRANK_ENTITY, KineticRotorRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftKinetics.ELECTRIC_MOTOR_ENTITY, KineticRotorRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftKinetics.FLYWHEEL_ENTITY, KineticRotorRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftFluids.TANK_ENTITY, GlassTankRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftSolar.SOLAR_TRACKER_ENTITY, KineticRotorRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftSolar.HELIOSTAT_ENTITY, KineticRotorRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftKinetics.STEAM_ENGINE_ENTITY, KineticRotorRenderer::new);
		// An MBS's longer placing reach while in the main hand (placement/StructureReach.java), set at once on the
		// client too so the crosshair reaches as soon as the item is in the hand.
		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player != null) {
				io.github.jimbozoomer.jugcraft.placement.StructureReach.update(client.player);
			}
		});
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			int side = io.github.jimbozoomer.jugcraft.placement.StructureReach.longestSide(stack, true);
			if (side > 0) {
				lines.add(net.minecraft.network.chat.Component.translatable("tooltip.jugcraft.structure_reach", 2 * side)
						.withStyle(net.minecraft.ChatFormatting.GRAY));
			}
			StoredFluid stored = stack.get(JugcraftFluids.STORED_FLUID);
			if (stored != null) {
				lines.add(stored.describe());
			}
			String plating = stack.get(io.github.jimbozoomer.jugcraft.machine.Electroplating.PLATING);
			if (plating != null) {
				lines.add(net.minecraft.network.chat.Component.translatable("tooltip.jugcraft.plating." + plating)
						.withStyle(net.minecraft.ChatFormatting.GOLD));
				lines.add(net.minecraft.network.chat.Component.translatable("tooltip.jugcraft.plating.repair")
						.withStyle(net.minecraft.ChatFormatting.GRAY));
			}
		});
		EngineersHandbookItem.openScreen = () -> Minecraft.getInstance().gui.setScreen(new HandbookScreen());
		// Gear traits' descriptions show while Shift is held (gear/TraitTooltips.java, docs/features/trait-details.md).
		io.github.jimbozoomer.jugcraft.gear.TraitTooltips.details = () -> Minecraft.getInstance().hasShiftDown();
		SeasonColors.register();
		PartyClient.register();
		ClientPlayNetworking.registerGlobalReceiver(SurveyPayload.TYPE,
				(payload, context) -> Minecraft.getInstance().gui.setScreen(new ProspectorScreen(payload.readings())));
		BlockEntityRendererRegistry.register(JugcraftAgriculture.CARVED_PUMPKIN_ENTITY, CarvedPumpkinRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.GIANT_PUMPKIN_ENTITY, GiantPumpkinRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.SCARECROW_ENTITY, ScarecrowRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.GRAVESTONE_ENTITY, GravestoneRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.HEADSTONE_ENTITY, HeadstoneRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.GRAVEYARD_BUILDING_ENTITY, GraveyardBuildingRenderer::new);
		ClientPlayNetworking.registerGlobalReceiver(OpenEpitaphPayload.TYPE,
				(payload, context) -> Minecraft.getInstance().gui.setScreen(new EpitaphScreen(payload)));
		BlockEntityRendererRegistry.register(JugcraftAgriculture.STRING_LIGHT_HOOK_ENTITY, StringLightsRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.HAUNTED_PORTRAIT_ENTITY, HauntedPortraitRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.FLOATING_CANDLE_ENTITY, FloatingCandleRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.PUMPKIN_CRATE_ENTITY, PumpkinCrateRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.ROCKING_CHAIR_ENTITY, RockingChairRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.LURKING_EYES_ENTITY, LurkingEyesRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.SILHOUETTE_WINDOW_ENTITY, SilhouetteWindowRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.GIANT_FAKE_SPIDER_ENTITY, GiantFakeSpiderRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.HAUNTED_CHANDELIER_ENTITY, HauntedChandelierRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.PIPE_ORGAN_ENTITY, PipeOrganRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.SUIT_OF_ARMOR_ENTITY, SuitOfArmorRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.DUST_SHEET_ENTITY, DustSheetRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.SPIRIT_MIRROR_ENTITY, SpiritMirrorRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.TATTERED_CURTAINS_ENTITY, TatteredCurtainsRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.CREEPY_DOLL_ENTITY, CreepyDollRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.TESLA_COIL_ENTITY, TeslaCoilRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.LAB_TABLE_ENTITY, LabTableRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.SPECIMEN_JAR_ENTITY, SpecimenJarRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.SARCOPHAGUS_ENTITY, MummySarcophagusRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.RAVEN_PERCH_ENTITY, RavenRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.BLACK_CAT_ENTITY, BlackCatRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.INFLATABLE_ENTITY, InflatableRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.PORCH_WITCH_ENTITY, PorchWitchRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.WIND_CHIMES_ENTITY, BoneWindChimesRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.WEATHERVANE_ENTITY, WeathervaneRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.SPOOKY_SIGN_ENTITY, SpookySignRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.GLOW_PAINT_ENTITY, GlowPaintRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.BRAZIER_ENTITY, WitchFireBrazierRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.SHADOW_LAMP_ENTITY, ShadowPuppetLampRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.FLOATING_HAT_ENTITY, FloatingWitchHatRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.JUMP_SCARE_ENTITY, JumpScareTrapRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.BOWLING_SCOREBOARD_ENTITY, BowlingScoreboardRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.DANCE_FLOOR_ENTITY, DanceFloorRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.GHOST_BELL_ENTITY, GhostBellRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.FLYING_EYEBALL_ENTITY, FlyingEyeballRenderer::new);
		// Halloween decorations batch 17, the Witch's Workshop.
		BlockEntityRendererRegistry.register(JugcraftAgriculture.HORNED_SKULL_CAULDRON_ENTITY, HornedSkullCauldronRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.CANDELABRUM_ENTITY, CandelabrumRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.ENCHANTED_BROOM_ENTITY, EnchantedBroomRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.DUSTPAN_ENTITY, DustpanRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.SHOWCASE_ENTITY, ShowcaseRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.MOTH_CASE_ENTITY, MothCaseRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.ODDITY_JAR_ENTITY, OddityJarRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.GIANT_HEART_ENTITY, GiantBeatingHeartRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.SPECIMEN_VESSEL_ENTITY, SpecimenVesselRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.COFFIN_WARDROBE_ENTITY, CoffinWardrobeRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.SARCOPHAGUS_TOMB_ENTITY, SarcophagusRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.COLOSSAL_SKULL_ENTITY, ColossalSkullRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.GARGOYLE_SENTINEL_ENTITY, GargoyleSentinelRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.GARGOYLE_RAINSPOUT_ENTITY, GargoyleRainspoutRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.LIGHTNING_HARNESS_ENTITY, LightningHarnessRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.CRAWLING_HAND_ENTITY, CrawlingHandRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.SILK_COCOON_ENTITY, SilkCocoonRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.EGG_SAC_ENTITY, EggSacRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.SILK_SPOOL_ENTITY, SilkSpoolRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.DINING_CHAIR_ENTITY, DiningChairRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.TABLE_SETTING_ENTITY, TableSettingRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.GRANDFATHER_CLOCK_ENTITY, GrandfatherClockRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.WITCHLIGHT_ENTITY, WitchlightRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.SILHOUETTE_ENTITY, YardSilhouetteRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.MOON_LAMP_ENTITY, HarvestMoonLampRenderer::new);
		// Halloween decorations batch 20, Pumpkin Night.
		BlockEntityRendererRegistry.register(JugcraftAgriculture.SINGING_PUMPKIN_ENTITY, SingingPumpkinRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.HARVEST_EFFIGY_ENTITY, HarvestEffigyRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.FARM_STAND_ENTITY, FarmStandRenderer::new);
		MenuScreens.register(JugcraftAgriculture.FARM_STAND_MENU, FarmStandScreen::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.FORTUNE_TABLE_ENTITY, FortuneTellerTableRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.BONFIRE_ENTITY, HalloweenBonfireRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.WAX_POT_ENTITY, WaxPotRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.AURA_CANDLE_ENTITY, AuraCandleRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.CIDER_PRESS_ENTITY, CiderPressRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.CANNING_KETTLE_ENTITY, CanningKettleRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.PANTRY_SHELF_ENTITY, PantryShelfRenderer::new);
		ParticleProviderRegistry.getInstance().register(JugcraftAgriculture.FOG, FogParticle::provider);
		ParticleProviderRegistry.getInstance().register(JugcraftAgriculture.SPOOKY_SPARK, SpookySparkParticle::provider);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.SHOW_LAUNCHER_ENTITY, ShowLauncherRenderer::new);
		EntityRendererRegistry.register(JugcraftAgriculture.SPOOKY_ROCKET, context -> new ThrownItemRenderer<>(context, 1.0F, true));
		EntityRendererRegistry.register(JugcraftAgriculture.SKY_LANTERN, SkyLanternRenderer::new);
		EntityRendererRegistry.register(JugcraftAgriculture.FLYING_BROOMSTICK, BroomstickRenderer::new);
		EntityRendererRegistry.register(JugcraftAgriculture.WEREWOLF, WerewolfRenderer::new);
		EntityRendererRegistry.register(JugcraftAgriculture.SQUIRREL, SquirrelRenderer::new);
		EntityRendererRegistry.register(JugcraftAgriculture.PUMPKLING, PumpklingRenderer::new);
		EntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.gear.JugcraftGrapple.GRAPPLE_HOOK, GrappleHookRenderer::new);
		EntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.weapons.FieldChemistry.CHEMICAL_CLOUD,
				net.minecraft.client.renderer.entity.NoopRenderer::new);
		EntityRendererRegistry.register(JugcraftAgriculture.RESTLESS_SPIRIT, RestlessSpiritRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.FEAST_TABLE_ENTITY, FeastTableRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.CANDY_KETTLE_ENTITY, CandyKettleRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.SPINNING_WHEEL_ENTITY, SpinningWheelRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.HEARTH_OVEN_ENTITY, HearthOvenRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.SPIRIT_BOARD_ENTITY, SpiritBoardRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftAgriculture.OFRENDA_ENTITY, OfrendaRenderer::new);
		ClientPlayNetworking.registerGlobalReceiver(SpookyBurstPayload.TYPE, (payload, context) -> SpookyBursts.receive(payload));
		ModelLayerRegistry.registerModelLayer(WispModel.LAYER, WispModel::createLayer);
		ModelLayerRegistry.registerModelLayer(CrowModel.LAYER, CrowModel::createLayer);
		ModelLayerRegistry.registerModelLayer(HayGolemModel.LAYER, HayGolemModel::createLayer);
		ModelLayerRegistry.registerModelLayer(TurkeyModel.LAYER, TurkeyModel::createLayer);
		ModelLayerRegistry.registerModelLayer(HorsemanModel.LAYER, HorsemanModel::createLayer);
		ModelLayerRegistry.registerModelLayer(WerewolfModel.LAYER, WerewolfModel::createLayer);
		ModelLayerRegistry.registerModelLayer(SquirrelModel.LAYER, SquirrelModel::createLayer);
		ModelLayerRegistry.registerModelLayer(PumpklingModel.LAYER, PumpklingModel::createLayer);
		EntityRendererRegistry.register(JugcraftAgriculture.WILL_O_WISP, WispRenderer::new);
		EntityRendererRegistry.register(JugcraftAgriculture.CROW, CrowRenderer::new);
		EntityRendererRegistry.register(JugcraftAgriculture.HAY_GOLEM, HayGolemRenderer::new);
		EntityRendererRegistry.register(JugcraftAgriculture.TURKEY, TurkeyRenderer::new);
		EntityRendererRegistry.register(JugcraftAgriculture.HEADLESS_HORSEMAN, HorsemanRenderer::new);
		EntityRendererRegistry.register(JugcraftAgriculture.FLYING_PUMPKIN, context -> new ThrownItemRenderer<>(context, 1.5F, false));
		EntityRendererRegistry.register(JugcraftAgriculture.FLAMING_PUMPKIN, context -> new ThrownItemRenderer<>(context, 1.5F, true));
		EntityRendererRegistry.register(JugcraftAgriculture.BOWLING_PUMPKIN, BowlingPumpkinRenderer::new);
		EntityRendererRegistry.register(JugcraftAgriculture.TOILET_PAPER_ROLL, ThrownItemRenderer::new);
		EntityRendererRegistry.register(JugcraftAgriculture.TOSS_RING, ThrownItemRenderer::new);
		EntityRendererRegistry.register(JugcraftAgriculture.FERRIS_WHEEL, FerrisWheelRenderer::new);
		EntityRendererRegistry.register(JugcraftAgriculture.PINATA, PinataRenderer::new);
		EntityRendererRegistry.register(JugcraftAgriculture.HOT_AIR_BALLOON, HotAirBalloonRenderer::new);
		EntityRendererRegistry.register(JugcraftAgriculture.PIBAL, PibalRenderer::new);
		EntityRendererRegistry.register(JugcraftAgriculture.HAUNTED_HAYRIDE, HauntedHayrideRenderer::new);
		EntityRendererRegistry.register(JugcraftAgriculture.THROW_MARKER, ThrowMarkerRenderer::new);
		EntityRendererRegistry.register(JugcraftAgriculture.SEAT, SeatRenderer::new);
		ClientPlayNetworking.registerGlobalReceiver(HarvestMoon.Payload.TYPE, (payload, context) -> HarvestMoon.clientActive = payload.active());
		EntityRendererRegistry.register(JugcraftAgriculture.PUMPKIN_BARGE, PumpkinBoatRenderer::new);
		EntityRendererRegistry.register(JugcraftAgriculture.PUMPKIN_RACER, PumpkinBoatRenderer::new);
		ClientPlayNetworking.registerGlobalReceiver(OpenCarvingPayload.TYPE,
				(payload, context) -> Minecraft.getInstance().gui.setScreen(new CarvingScreen(payload)));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			HarvestMoon.clientActive = false;
			client.execute(CarvingTextures::clear);
		});
		BlockEntityRenderers.register(JugcraftDrones.TERMINAL_ENTITY, context -> new DroneDepotRenderer());
		DroneSounds.register();
		DieselSounds.register();
		SoundOcclusion.register();
		BlockEntityRenderers.register(JugcraftDrones.SCREEN_ENTITY, ControlScreenRenderer::new);
		BlockEntityRenderers.register(JugcraftDrones.HOLO_ENTITY, context -> new HoloMapRenderer());
		BlockEntityRenderers.register(io.github.jimbozoomer.jugcraft.blueprint.JugcraftBlueprints.STAKE_ENTITY, context -> new SurveyStakeRenderer());
		io.github.jimbozoomer.jugcraft.client.blueprint.ClientBlueprints.register();
		net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.tower.JugcraftTower.SEAT, net.minecraft.client.renderer.entity.NoopRenderer::new);
		// The walled town: townsfolk, the shop and ATM screens.
		net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry.registerModelLayer(TownsfolkModel.LAYER, TownsfolkModel::createLayer);
		EntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.town.JugcraftTown.TOWNSFOLK, TownsfolkRenderer::new);
		net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry.registerModelLayer(StyxModel.LAYER, StyxModel::createLayer);
		EntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.styx.JugcraftStyx.WIZARD, StyxRenderer::new);
		MenuScreens.register(io.github.jimbozoomer.jugcraft.town.JugcraftTown.SHOP_MENU, ShopScreen::new);
		MenuScreens.register(io.github.jimbozoomer.jugcraft.town.JugcraftTown.ATM_MENU, AtmScreen::new);
		MenuScreens.register(io.github.jimbozoomer.jugcraft.control.JugcraftControl.CONTROLLER_MENU, LogicControllerScreen::new);
		BlockEntityRenderers.register(io.github.jimbozoomer.jugcraft.control.JugcraftControl.MONITOR_ENTITY, ControlMonitorRenderer::new);
		io.github.jimbozoomer.jugcraft.drone.GuideBooks.openScreen = book -> Minecraft.getInstance().gui.setScreen(new GuideBookScreen(book));
		DroneTerminalBlock.openScreen = pos -> Minecraft.getInstance().gui.setScreen(new DroneTerminalScreen(pos));
		io.github.jimbozoomer.jugcraft.tower.TowerCoreBlock.openScreen = pos -> Minecraft.getInstance().gui.setScreen(new TowerScreen(pos));
		BlockEntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.nuclear.JugcraftNuclear.EQUIPMENT_ENTITY, NuclearRenderer::new);
		BlockEntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.nuclear.JugcraftNuclear.COOLING_TOWER_ENTITY, CoolingTowerRenderer::new);
		io.github.jimbozoomer.jugcraft.nuclear.EquipmentPlacement.openConsole = pos -> Minecraft.getInstance().gui.setScreen(new ReactorConsoleScreen(pos));
		io.github.jimbozoomer.jugcraft.nuclear.EquipmentPlacement.openShutdownPanel = pos -> Minecraft.getInstance().gui.setScreen(new RemoteShutdownScreen(pos));
		net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry.getInstance().register(
				io.github.jimbozoomer.jugcraft.nuclear.JugcraftNuclear.COOLING_PLUME, CoolingPlumeParticle.Provider::new);
	}
}
