package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.SkyLantern;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;

/**
 * A sky lantern: a paper body in its colour, a little wider at the top than at its open bottom, on a thin bamboo ring,
 * with the flame glowing in the opening. Lit, the paper glows (drawn at full brightness, lighter where the flame shines
 * through) and flickers a little; burning out, it dims to its plain colour in the dark. It turns slowly as it rises.
 */
public class SkyLanternRenderer extends EntityRenderer<SkyLantern, SkyLanternRenderer.State> {
	private static final RenderType PAPER = RenderTypes.entityCutout(Jugcraft.id("textures/entity/sky_lantern.png"));

	public static final class State extends EntityRenderState {
		int colour = SkyLantern.DEFAULT_COLOUR;
		float brightness = 1.0F;
		float time;
	}

	public SkyLanternRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(SkyLantern lantern, State state, float partialTick) {
		super.extractRenderState(lantern, state, partialTick);
		state.colour = lantern.colour();
		state.brightness = lantern.brightness(lantern.level().getGameTime());
		state.time = lantern.tickCount + partialTick + lantern.getId() * 13;
	}

	/** {@code colour} lit by the flame: lighter and warmer by {@code glow} (0 to 1). */
	static int lit(int colour, float glow) {
		int r = colour >> 16 & 0xFF;
		int g = colour >> 8 & 0xFF;
		int b = colour & 0xFF;
		r = (int) Mth.lerp(glow * 0.45F, r, 255);
		g = (int) Mth.lerp(glow * 0.35F, g, 230);
		b = (int) Mth.lerp(glow * 0.15F, b, 150);
		return 0xFF000000 | r << 16 | g << 8 | b;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		float flicker = 0.9F + 0.1F * Mth.sin(state.time * 0.7F) * Mth.sin(state.time * 0.23F);
		float glow = state.brightness * flicker;
		int light = glow > 0.05F ? LightCoordsUtil.FULL_BRIGHT : state.lightCoords;
		int paper = lit(state.colour, glow);
		int dark = lit(state.colour, glow * 0.6F) & 0xFFE0E0E0;
		int flame = glow > 0.05F ? 0xFFFFE89A : 0xFF3A3020;
		pose.pushPose();
		pose.rotateDegrees(Axis.YP, state.time * 0.6F);
		pose.translate(-0.5F, 0.0F, -0.5F);
		collector.submitCustomGeometry(pose, PAPER, (matrix, buffer) -> {
			// The open bottom's bamboo ring, the paper in three widening tiers, and a gathered top.
			TintedBoxes.box(buffer, matrix, 4.5F, 1.0F, 4.5F, 11.5F, 1.6F, 11.5F, 0xFF8A6A3A, light);
			TintedBoxes.box(buffer, matrix, 4.5F, 1.6F, 4.5F, 11.5F, 5.0F, 11.5F, dark, light);
			TintedBoxes.box(buffer, matrix, 4.0F, 5.0F, 4.0F, 12.0F, 8.5F, 12.0F, paper, light);
			TintedBoxes.box(buffer, matrix, 3.5F, 8.5F, 3.5F, 12.5F, 11.0F, 12.5F, paper, light);
			TintedBoxes.box(buffer, matrix, 5.0F, 11.0F, 5.0F, 11.0F, 11.6F, 11.0F, dark, light);
			// The flame, in the opening.
			TintedBoxes.box(buffer, matrix, 7.2F, 0.2F, 7.2F, 8.8F, 1.8F, 8.8F, flame, light);
		});
		pose.popPose();
		super.submit(state, pose, collector, camera);
	}
}
