package io.github.jimbozoomer.jugcraft.client.tv;

import io.github.jimbozoomer.jugcraft.tv.JugcraftTv;
import io.github.jimbozoomer.jugcraft.tv.TvPayloads;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.Minecraft;

/** The flat screen TVs on the client: their renderer and the speakers', the SCREEN window, the players let go when not drawn. */
public final class TvClient {
	private TvClient() {
	}

	public static void register() {
		BlockEntityRendererRegistry.register(JugcraftTv.TV_ENTITY, TvRenderer::new);
		BlockEntityRendererRegistry.register(JugcraftTv.SPEAKER_ENTITY, io.github.jimbozoomer.jugcraft.client.TvSpeakerRenderer::new);
		ClientPlayNetworking.registerGlobalReceiver(TvPayloads.Open.TYPE, (payload, context) -> Minecraft.getInstance().gui.setScreen(new TvScreen(payload)));
		ClientTickEvents.END_CLIENT_TICK.register(client -> TvPlayers.tick());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(TvPlayers::closeAll));
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> TvPlayers.closeAll());
	}
}
