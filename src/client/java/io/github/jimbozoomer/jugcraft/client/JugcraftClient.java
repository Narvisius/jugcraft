package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.guide.EngineersHandbookItem;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.prospecting.SurveyPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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
		EngineersHandbookItem.openScreen = () -> Minecraft.getInstance().gui.setScreen(new HandbookScreen());
		ClientPlayNetworking.registerGlobalReceiver(SurveyPayload.TYPE,
				(payload, context) -> Minecraft.getInstance().gui.setScreen(new ProspectorScreen(payload.readings())));
	}
}
