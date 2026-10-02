package io.github.jimbozoomer.jugcraft.client.blueprint;

import io.github.jimbozoomer.jugcraft.blueprint.BlueprintNetwork;
import io.github.jimbozoomer.jugcraft.blueprint.BlueprintTableBlock;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/** Client side of blueprints: the library from the server, the table and stake screens, and the placement preview. */
public final class ClientBlueprints {
	private ClientBlueprints() {
	}

	public static void register() {
		BlueprintStages.register();
		ClientPlayNetworking.registerGlobalReceiver(BlueprintNetwork.SyncPayload.TYPE, (payload, context) -> BlueprintNetwork.receive(payload));
		ClientPlayNetworking.registerGlobalReceiver(BlueprintNetwork.ImportResultPayload.TYPE, (payload, context) -> {
			if (BlueprintTableScreen.open != null) {
				BlueprintTableScreen.open.importResult(payload);
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(BlueprintNetwork.StakeInfoPayload.TYPE, (payload, context) -> {
			if (StakeScreen.open != null && StakeScreen.open == Minecraft.getInstance().gui.screen()) {
				StakeScreen.open.update(payload);
			} else if (payload.open()) {
				Minecraft.getInstance().gui.setScreen(new StakeScreen(payload));
			}
		});
		BlueprintTableBlock.openScreen = pos -> Minecraft.getInstance().gui.setScreen(new BlueprintTableScreen(pos));
		BlueprintPlacementPreview.register();
	}
}
