package io.github.jimbozoomer.jugcraft.client;

import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.enums.PlayState;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.crimson.Exhaustion;
import io.github.jimbozoomer.jugcraft.concordance.crimson.OfferingState;
import io.github.jimbozoomer.jugcraft.concordance.crimson.Offerings;
import io.github.jimbozoomer.jugcraft.concordance.vigil.CrimsonChaliceItem;
import io.github.jimbozoomer.jugcraft.concordance.vigil.Vigil;
import io.github.jimbozoomer.jugcraft.concordance.vigil.VigilGesturePayload;
import io.github.jimbozoomer.jugcraft.weapons.ArmItem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The Crimson Vigil on the client (roadmap step 16). Nothing here decides anything:
 * <ul>
 * <li>the offering gesture plays, on a Player Animation Library layer of its own, when the server says a player has
 * made an offering. <b>ArmsMotion coordination:</b> ArmsMotion poses the arms of a player holding a Jugcraft arm
 * ({@link ArmItem}); the gesture is skipped for such a player, so the two never pose the same arms at once (the
 * offering itself still happened on the server);</li>
 * <li>while a Crimson Chalice or Thornheart Blade is held, a line at the bottom right says the player's health, the
 * chalice's Vitae and their offering exhaustion with its efficiency and recovery time, each apart, never as one bar.</li>
 * </ul>
 */
public final class VigilClient {
	public static final Identifier LAYER = Jugcraft.id("vigil");
	public static final Identifier GESTURE = Jugcraft.id("vigil_offering");
	/** Above the circle's layer, below Spell Engine's casting gestures. */
	private static final int LAYER_PRIORITY = 950;
	private static final int HEALTH_COLOR = 0xFFE05555;
	private static final int VITAE_COLOR = 0xFFB0202A;
	private static final int TIRED_COLOR = 0xFFB8A070;

	private VigilClient() {
	}

	static void register() {
		PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, LAYER_PRIORITY,
				avatar -> new PlayerAnimationController(avatar, (controller, state, setter) -> PlayState.STOP));
		ClientPlayNetworking.registerGlobalReceiver(VigilGesturePayload.TYPE, (payload, context) -> gesture(payload.entity()));
		HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, Jugcraft.id("vigil_status"), VigilClient::statusLine);
	}

	private static void gesture(int entity) {
		Minecraft client = Minecraft.getInstance();
		Entity found = client.level == null ? null : client.level.getEntity(entity);
		if (!(found instanceof Player player) || player.getMainHandItem().getItem() instanceof ArmItem
				|| player.getOffhandItem().getItem() instanceof ArmItem) {
			return;
		}
		if (PlayerAnimationAccess.getPlayerAnimationLayer(player, LAYER) instanceof PlayerAnimationController controller) {
			controller.triggerAnimation(GESTURE);
		}
	}

	/** Health, Vitae and exhaustion, each in its own words and colour, while a Vigil item is held. */
	private static void statusLine(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		Minecraft client = Minecraft.getInstance();
		Player player = client.player;
		if (player == null || player.isSpectator() || client.level == null) {
			return;
		}
		ItemStack chalice = player.getMainHandItem().is(Vigil.CRIMSON_CHALICE) ? player.getMainHandItem() : player.getOffhandItem();
		boolean holding = chalice.is(Vigil.CRIMSON_CHALICE) || player.getMainHandItem().is(Vigil.THORNHEART_BLADE)
				|| player.getOffhandItem().is(Vigil.THORNHEART_BLADE);
		if (!holding) {
			return;
		}
		long now = client.level.getGameTime();
		OfferingState state = player.getAttachedOrElse(Vigil.OFFERING, OfferingState.NONE);
		Exhaustion exhaustion = state.exhaustion();
		int tired = exhaustion.current(now);
		Font font = client.font;
		int y = graphics.guiHeight() - 36;
		int right = graphics.guiWidth() - 4;
		String health = Component.translatable("screen.jugcraft.concordance.vigil.health", Math.round(player.getHealth()),
				Math.round(player.getMaxHealth())).getString();
		graphics.text(font, health, right - font.width(health), y, HEALTH_COLOR);
		if (chalice.is(Vigil.CRIMSON_CHALICE)) {
			String vitae = Component.translatable("screen.jugcraft.concordance.vigil.vitae", CrimsonChaliceItem.vitae(chalice),
					Vigil.CHALICE_CAPACITY).getString();
			graphics.text(font, vitae, right - font.width(vitae), y + 12, VITAE_COLOR);
		}
		long seconds = (exhaustion.ticksToClear(now) + 19) / 20;
		String rest = Component.translatable("screen.jugcraft.concordance.vigil.exhaustion", tired, Exhaustion.MAX,
				Offerings.efficiency(tired), seconds / 60, String.format(java.util.Locale.ROOT, "%02d", seconds % 60)).getString();
		graphics.text(font, rest, right - font.width(rest), y - 12, TIRED_COLOR);
	}
}
