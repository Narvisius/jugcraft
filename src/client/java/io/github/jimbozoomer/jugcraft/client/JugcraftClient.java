package io.github.jimbozoomer.jugcraft.client;

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
import io.github.jimbozoomer.jugcraft.tools.JugcraftTools;
import io.github.jimbozoomer.jugcraft.weapons.JugcraftWeapons;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

/**
 * Client entrypoint: machine menus to their screens, the handbook to its book, ore surveys to the prospector
 * screen, and the drone depot's renderers (drones, pickup lift, control room screen) and terminal screen.
 */
public final class JugcraftClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		for (MachineKind kind : MachineKind.values()) {
			MenuScreens.register(JugcraftMachines.menuType(kind), MachineScreen::new);
		}
		BlockEntityRendererRegistry.register(JugcraftMachines.MACHINE_ENTITY, WindTurbineRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftKinetics.BELT_PULLEY_ENTITY, BeltRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftKinetics.SHAFT_ENTITY, KineticRotorRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftLogistics.CONVEYOR_ENTITY, ConveyorRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftTools.CHARGING_STATION_ENTITY, ChargingStationRenderer::new);
		RocketPackClient.register();
		EntityRendererRegistry.register(JugcraftWeapons.GRENADE, ThrownItemRenderer::new);
		PetroFluidsClient.register();
		RocketPackLayer.register();
		BlockEntityRendererRegistry.register(JugcraftKinetics.HAND_CRANK_ENTITY, KineticRotorRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftKinetics.ELECTRIC_MOTOR_ENTITY, KineticRotorRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftKinetics.STEAM_ENGINE_ENTITY, KineticRotorRenderer::new);
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			StoredFluid stored = stack.get(JugcraftFluids.STORED_FLUID);
			if (stored != null) {
				lines.add(stored.describe());
			}
		});
		EngineersHandbookItem.openScreen = () -> Minecraft.getInstance().gui.setScreen(new HandbookScreen());
		ClientPlayNetworking.registerGlobalReceiver(SurveyPayload.TYPE,
				(payload, context) -> Minecraft.getInstance().gui.setScreen(new ProspectorScreen(payload.readings())));
		BlockEntityRenderers.register(JugcraftDrones.TERMINAL_ENTITY, context -> new DroneDepotRenderer());
		DroneSounds.register();
		BlockEntityRenderers.register(JugcraftDrones.SCREEN_ENTITY, ControlScreenRenderer::new);
		BlockEntityRenderers.register(JugcraftDrones.HOLO_ENTITY, context -> new HoloMapRenderer());
		BlockEntityRenderers.register(io.github.jimbozoomer.jugcraft.blueprint.JugcraftBlueprints.STAKE_ENTITY, context -> new SurveyStakeRenderer());
		io.github.jimbozoomer.jugcraft.client.blueprint.ClientBlueprints.register();
		net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.tower.JugcraftTower.SEAT, net.minecraft.client.renderer.entity.NoopRenderer::new);
		DroneTerminalBlock.openScreen = pos -> Minecraft.getInstance().gui.setScreen(new DroneTerminalScreen(pos));
		io.github.jimbozoomer.jugcraft.tower.TowerCoreBlock.openScreen = pos -> Minecraft.getInstance().gui.setScreen(new TowerScreen(pos));
	}
}
