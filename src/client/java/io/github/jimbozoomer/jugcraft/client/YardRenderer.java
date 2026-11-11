package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.nuclear.YardBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

/**
 * A yard's live parts (transformer yard and switchyard build), drawn from its controller under the grass: the
 * transformers' needles and drag hands, lamps, fans turning and flow gauges, the deluge valve sets' gauges and the
 * releasing panel's lamps; the breaker's flag and lamps, the disconnect's and earth switch's blades swinging, the tap
 * changer's needle, the operations counters' figures (SiteLive).
 */
public class YardRenderer implements BlockEntityRenderer<YardBlockEntity, YardRenderer.State> {
	public static class State extends BlockEntityRenderState {
		final SiteLive.State site = new SiteLive.State();
	}

	public YardRenderer(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 256;
	}

	@Override
	public void extractRenderState(YardBlockEntity works, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(works, state, crumbling);
		state.site.data = null;
		if (works.getLevel() != null) {
			SiteLive.extract(works, works.getLevel(), works.getBlockPos(), state.site);
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.site.data != null) {
			SiteLive.submit(state.site, pose, collector);
		}
	}
}
