package io.github.jimbozoomer.jugcraft.client;

import eu.pb4.trinkets.api.client.renderer.element.TrinketRenderElements;
import io.github.jimbozoomer.jugcraft.client.trinket.UnlessCoveredTrinketElement;
import io.github.jimbozoomer.jugcraft.concordance.trinket.WornDisplayPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Wayfaring on the client: the render element its worn belt and boots are drawn through
 * ({@link UnlessCoveredTrinketElement}: not under covering armour, and not on a wearer who has hidden them), given to
 * Trinkets before resources load, so the render definitions in assets/jugcraft/trinkets can name it; and this player's
 * "Show my worn trinkets" setting, sent to the server on joining a world and each time the setting is saved
 * ({@link #sendChoice}), so everyone who sees this player sees their choice.
 */
final class WayfaringClient {
	private WayfaringClient() {
	}

	static void register() {
		TrinketRenderElements.ID_MAPPER.put(UnlessCoveredTrinketElement.TYPE, UnlessCoveredTrinketElement.CODEC);
		ClientPlayConnectionEvents.JOIN.register((listener, sender, client) -> sendChoice());
	}

	/** Tells the server whether this player's worn trinkets are shown (nothing, when not in a world whose server has Jugcraft). */
	static void sendChoice() {
		if (ClientPlayNetworking.canSend(WornDisplayPayload.TYPE)) {
			ClientPlayNetworking.send(new WornDisplayPayload(ConcordanceClientOptions.wornTrinkets()));
		}
	}
}
