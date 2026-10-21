package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.finance.ExchangeBlocks;
import io.github.jimbozoomer.jugcraft.finance.FinanceActionPayload;
import io.github.jimbozoomer.jugcraft.finance.MarketChestPayload;
import io.github.jimbozoomer.jugcraft.finance.FinanceSnapshotPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;

/**
 * The Exchange hotkey (semicolon by default; B belongs to the Encyclopedia; rebindable under Controls > Jugcraft, {@link ExchangeKeys#CATEGORY}) and the snapshot receiver. Pressing the key
 * asks the server for the player's numbers; the screen opens when they arrive, so it never shows stale or invented data.
 */
public final class FinanceClient {
	public static KeyMapping openExchange;

	private FinanceClient() {
	}

	public static void register() {
        // Diorama normally draws over container screens; keep Market Chest prices readable.
        // Wrap its registered HUD element only after every client initializer has run.
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("diorama")) {
                net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.replaceElement(
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("diorama", "minimap"), original -> (graphics, delta) -> {
                        var screen=client.gui.screen();
                        if (!(screen instanceof MarketChestScreen || screen instanceof ExchangeScreen
                            || screen instanceof ExchangeGuideScreen || screen instanceof CasinoScreen
                            || screen instanceof PokerScreen || screen instanceof ScratchScreen)) {
                            original.extractRenderState(graphics, delta);
                        }
                    });
            }
        });
		// The Derby Table's horses, raced in the world for everyone nearby.
		net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(
				io.github.jimbozoomer.jugcraft.finance.ExchangeBlocks.CASINO_ENTITY, DerbyRenderer::new);
		io.github.jimbozoomer.jugcraft.finance.ExchangeGuide.openScreen =
				() -> Minecraft.getInstance().gui.setScreen(new ExchangeGuideScreen(null));
		openExchange = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.jugcraft.exchange", InputConstants.KEY_SEMICOLON,
				ExchangeKeys.CATEGORY));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openExchange.consumeClick()) {
				if (client.player != null && client.getConnection() != null && client.gui.screen() == null) {
					ClientPlayNetworking.send(FinanceActionPayload.simple(FinanceActionPayload.Action.OPEN));
				}
			}
		});
		MenuScreens.register(ExchangeBlocks.MARKET_CHEST_MENU, MarketChestScreen::new);
		ClientPlayNetworking.registerGlobalReceiver(io.github.jimbozoomer.jugcraft.finance.PokerTablePayload.TYPE,
				(payload, context) -> PokerScreen.receive(payload));
		ClientPlayNetworking.registerGlobalReceiver(io.github.jimbozoomer.jugcraft.finance.CasinoPayload.TYPE,
				(payload, context) -> CasinoScreen.receive(payload));
		ClientPlayNetworking.registerGlobalReceiver(io.github.jimbozoomer.jugcraft.finance.ScratchPayload.TYPE,
				(payload, context) -> ScratchScreen.receive(payload));
		ClientPlayNetworking.registerGlobalReceiver(MarketChestPayload.TYPE, (payload, context) -> {
			if (Minecraft.getInstance().gui.screen() instanceof MarketChestScreen screen) {
				screen.update(payload);
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(FinanceSnapshotPayload.TYPE, (payload, context) -> {
			Minecraft client = Minecraft.getInstance();
			if (client.gui.screen() instanceof ExchangeScreen screen) {
				screen.update(payload);
			} else if (payload.open()) {
				client.gui.setScreen(new ExchangeScreen(payload));
			}
		});
	}
}
