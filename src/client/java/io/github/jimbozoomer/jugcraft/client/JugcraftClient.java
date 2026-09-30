package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.guide.EngineersHandbookItem;
import io.github.jimbozoomer.jugcraft.kinetic.JugcraftKinetics;
import io.github.jimbozoomer.jugcraft.logistics.JugcraftLogistics;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.prospecting.SurveyPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;

/** Client entrypoint: machine menus to their screens, the handbook to its book, and ore surveys to the prospector screen. */
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
		BlockEntityRendererRegistry.register(JugcraftKinetics.HAND_CRANK_ENTITY, KineticRotorRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftKinetics.ELECTRIC_MOTOR_ENTITY, KineticRotorRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftKinetics.STEAM_ENGINE_ENTITY, KineticRotorRenderer::new);
		EngineersHandbookItem.openScreen = () -> Minecraft.getInstance().gui.setScreen(new HandbookScreen());
		ClientPlayNetworking.registerGlobalReceiver(SurveyPayload.TYPE,
				(payload, context) -> Minecraft.getInstance().gui.setScreen(new ProspectorScreen(payload.readings())));
	}
}
