package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.guide.EngineersHandbookItem;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;

/** Client entrypoint: connects each machine menu to its screen and the handbook item to its book screen. */
public final class JugcraftClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		for (MachineKind kind : MachineKind.values()) {
			MenuScreens.register(JugcraftMachines.menuType(kind), MachineScreen::new);
		}
		EngineersHandbookItem.openScreen = () -> Minecraft.getInstance().gui.setScreen(new HandbookScreen());
	}
}
