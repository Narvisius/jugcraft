package io.github.jimbozoomer.jugcraft.client.guns;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.guns.GunItem;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Aiming through a scope (slice 7), in the player's own first-person view, near the end of the aim ({@link #VIEW_AT}):
 * <ul>
 * <li>a magnifying scope fills the screen with the view through it, as a spyglass does: the owner's reticle and lens rim
 * on a square as tall as the screen, black beside it. The gun is put away meanwhile (GunRenderer), and the view narrows
 * and the mouse slows by the scope's zoom ({@link GunView});</li>
 * <li>a reflex sight leaves the gun in view, its window on the middle of the screen, and puts its red dot there.</li>
 * </ul>
 * Either way the crosshair is left out: the reticle or the dot is the aim.
 */
public final class GunScope {
	/** How far into the aim (0 to 1, {@link GunView#aim}) the view through the scope comes up. */
	static final float VIEW_AT = 0.9F;
	/** How big the reflex sight's dot texture is drawn, in GUI pixels: its 2 x 2 dot comes to 2. */
	private static final int DOT_SIZE = 16;
	private static final Map<String, Identifier> TEXTURES = new HashMap<>();
	/** Frames the view through a scope and the reflex dot were drawn, so far (for the client game tests). */
	private static long views;
	private static long dots;

	private GunScope() {
	}

	public static void register() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, Jugcraft.id("gun_scope"), GunScope::overlay);
		HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, crosshair -> (graphics, delta) -> {
			if (aimedOptic(delta.getGameTimeDeltaPartialTick(false)) == null) {
				crosshair.extractRenderState(graphics, delta);
			}
		});
	}

	/** The scope on the gun in the player's main hand while they aim through it in first person; otherwise null. */
	static GunLooks.@Nullable Optic aimedOptic(float partialTick) {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null || !client.options.getCameraType().isFirstPerson() || !(player.getMainHandItem().getItem() instanceof GunItem)
				|| GunView.aim(partialTick) < VIEW_AT) {
			return null;
		}
		return GunLooks.optic(player.getMainHandItem());
	}

	/** Whether the view through a scope fills the screen now, so the gun is put away. */
	static boolean viewing(float partialTick) {
		GunLooks.Optic optic = aimedOptic(partialTick);
		return optic != null && optic.fillsView();
	}

	private static void overlay(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		GunLooks.Optic optic = aimedOptic(delta.getGameTimeDeltaPartialTick(false));
		if (optic == null) {
			return;
		}
		int width = graphics.guiWidth();
		int height = graphics.guiHeight();
		if (optic.fillsView()) {
			int side = Math.min(width, height);
			int left = (width - side) / 2;
			int top = (height - side) / 2;
			graphics.blit(RenderPipelines.GUI_TEXTURED, texture(optic.reticle()), left, top, 0.0F, 0.0F, side, side, side, side);
			if (optic.vignette() != null) {
				graphics.blit(RenderPipelines.GUI_TEXTURED, texture(optic.vignette()), left, top, 0.0F, 0.0F, side, side, side, side);
			}
			graphics.fill(0, 0, width, top, 0xFF000000);
			graphics.fill(0, top + side, width, height, 0xFF000000);
			graphics.fill(0, top, left, top + side, 0xFF000000);
			graphics.fill(left + side, top, width, top + side, 0xFF000000);
			views++;
		} else if (optic.dot() != null) {
			graphics.blit(RenderPipelines.GUI_TEXTURED, texture(optic.dot()), (width - DOT_SIZE) / 2, (height - DOT_SIZE) / 2, 0.0F, 0.0F,
					DOT_SIZE, DOT_SIZE, DOT_SIZE, DOT_SIZE);
			dots++;
		}
	}

	private static Identifier texture(String name) {
		return TEXTURES.computeIfAbsent(name, key -> Jugcraft.id("textures/item/guns/optics/" + key + ".png"));
	}

	/** Frames the view through a scope was drawn (for the client game tests). */
	public static long views() {
		return views;
	}

	/** Frames a reflex sight's dot was drawn (for the client game tests). */
	public static long dots() {
		return dots;
	}
}
