package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.RestlessSpirit;
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
 * A restless spirit: a pale, hooded shape of see-through wisps, its arms reaching out and its tail trailing away to
 * nothing, with dark hollows for eyes and a moaning mouth. It glows faintly (drawn at full brightness), bobs and sways,
 * and is drawn only while revealed ({@link RestlessSpirit#visibility}), fading in and out; hidden, nothing of it shows.
 * Drawn facing along -z, then turned to the spirit's heading.
 */
public class RestlessSpiritRenderer extends EntityRenderer<RestlessSpirit, RestlessSpiritRenderer.State> {
	private static final RenderType WISPS = RenderTypes.entityTranslucent(Jugcraft.id("textures/entity/restless_spirit.png"));
	private static final int PALE = 0xE6F2FF;
	private static final int HOLLOW = 0x0E1620;

	public static final class State extends EntityRenderState {
		float visibility;
		float yaw;
		float time;
	}

	public RestlessSpiritRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(RestlessSpirit spirit, State state, float partialTick) {
		super.extractRenderState(spirit, state, partialTick);
		state.visibility = spirit.visibility(spirit.level().getGameTime(), partialTick);
		state.yaw = Mth.rotLerp(partialTick, spirit.yRotO, spirit.getYRot());
		state.time = spirit.tickCount + partialTick + spirit.getId() * 7;
	}

	private static int argb(int rgb, float alpha) {
		return (int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24 | rgb;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.visibility <= 0.01F) {
			return;
		}
		float t = state.time;
		int body = argb(PALE, 0.55F * state.visibility);
		int faint = argb(PALE, 0.32F * state.visibility);
		int hollow = argb(HOLLOW, 0.85F * state.visibility);
		int light = LightCoordsUtil.FULL_BRIGHT;
		float bob = Mth.sin(t * 0.09F) * 1.2F;
		float sway = Mth.sin(t * 0.13F) * 1.0F;
		float reach = Mth.sin(t * 0.07F) * 0.8F;
		pose.pushPose();
		pose.rotateDegrees(Axis.YP, 180.0F - state.yaw);
		pose.translate(-0.5F, bob / 16.0F, -0.5F);
		collector.submitCustomGeometry(pose, WISPS, (matrix, buffer) -> {
			// The tail, trailing away below, swaying.
			TintedBoxes.box(buffer, matrix, 6.5F + sway, 0.5F, 7.0F, 9.5F + sway, 3.0F, 9.5F, faint, light);
			TintedBoxes.box(buffer, matrix, 5.5F + sway * 0.5F, 3.0F, 6.0F, 10.5F + sway * 0.5F, 6.0F, 10.5F, faint, light);
			// The robe, widening up to the shoulders, its hem ragged.
			TintedBoxes.box(buffer, matrix, 4.5F, 6.0F, 5.0F, 11.5F, 11.0F, 11.0F, body, light);
			TintedBoxes.box(buffer, matrix, 4.0F, 11.0F, 4.5F, 12.0F, 16.0F, 11.5F, body, light);
			TintedBoxes.box(buffer, matrix, 4.0F, 5.0F, 5.0F, 5.5F, 6.5F, 6.5F, faint, light);
			TintedBoxes.box(buffer, matrix, 10.5F, 4.5F, 9.5F, 12.0F, 6.5F, 11.0F, faint, light);
			// The arms, reaching out in front.
			TintedBoxes.box(buffer, matrix, 2.5F, 11.5F - reach, 1.0F, 4.5F, 13.5F - reach, 7.0F, body, light);
			TintedBoxes.box(buffer, matrix, 11.5F, 11.5F + reach, 1.0F, 13.5F, 13.5F + reach, 7.0F, body, light);
			// The hooded head and its peak.
			TintedBoxes.box(buffer, matrix, 4.5F, 16.0F, 4.5F, 11.5F, 22.0F, 11.5F, body, light);
			TintedBoxes.box(buffer, matrix, 6.0F, 22.0F, 6.0F, 10.0F, 23.5F, 10.0F, faint, light);
			// Hollow eyes and a moaning mouth on its face.
			TintedBoxes.box(buffer, matrix, 5.5F, 18.5F, 4.2F, 7.2F, 20.3F, 4.6F, hollow, light);
			TintedBoxes.box(buffer, matrix, 8.8F, 18.5F, 4.2F, 10.5F, 20.3F, 4.6F, hollow, light);
			TintedBoxes.box(buffer, matrix, 7.2F, 16.6F, 4.2F, 8.8F, 17.9F, 4.6F, hollow, light);
		});
		pose.popPose();
		super.submit(state, pose, collector, camera);
	}
}
