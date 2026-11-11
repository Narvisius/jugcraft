package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.nuclear.ReactorBuildingBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

/**
 * The reactor building's live parts (22 October 2026), drawn from its controller under the loop floor: the head
 * package's fans and the coolant pumps' couplings turning, the gauges' needles, the area radiation monitor's and the
 * frisker's figures (SiteLive). Within 96 blocks: the building's dome hides them from farther off.
 */
public class ReactorBuildingRenderer implements BlockEntityRenderer<ReactorBuildingBlockEntity, ReactorBuildingRenderer.State> {
	public static class State extends BlockEntityRenderState {
		final SiteLive.State site = new SiteLive.State();
	}

	public ReactorBuildingRenderer(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context context) {
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
		return 96;
	}

	@Override
	public void extractRenderState(ReactorBuildingBlockEntity works, State state, float partialTick, Vec3 cameraPos,
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
