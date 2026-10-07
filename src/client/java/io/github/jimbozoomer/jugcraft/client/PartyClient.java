package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.party.PartyStatePayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;

/** The Party key (P by default) and the party state the server sends for {@link PartyScreen}. */
final class PartyClient {
	/** Jugcraft's own section in Controls. */
	static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Jugcraft.id(Jugcraft.MOD_ID));
	private static KeyMapping openParty;

	private PartyClient() {
	}

	static void register() {
		openParty = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.jugcraft.party", InputConstants.KEY_P, CATEGORY));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openParty.consumeClick()) {
				if (client.gui.screen() == null && client.player != null) {
					client.gui.setScreen(new PartyScreen());
				}
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(PartyStatePayload.TYPE, (payload, context) -> PartyScreen.receive(payload));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(PartyScreen::clear));
	}
}
