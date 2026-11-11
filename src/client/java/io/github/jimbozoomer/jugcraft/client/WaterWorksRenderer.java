package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.nuclear.WaterWorksBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

/**
 * A pump house's live parts (towers and pump houses build), drawn from its controller under the grass: the pumps'
 * running parts and lamps, the valves, the screens' panels creeping up, the doors, the roll-up door, the lights, the
 * annunciator's windows and the figures on its panel's screens (SiteLive).
 */
public class WaterWorksRenderer implements BlockEntityRenderer<WaterWorksBlockEntity, WaterWorksRenderer.State> {
	public static class State extends BlockEntityRenderState {
		final SiteLive.State site = new SiteLive.State();
	}

	public WaterWorksRenderer(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context context) {
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
	public void extractRenderState(WaterWorksBlockEntity works, State state, float partialTick, Vec3 cameraPos,
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
