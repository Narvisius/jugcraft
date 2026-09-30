package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Draws the spinning part of a turning shaft, hand crank, electric motor or running steam engine (see {@link KineticRotors}). */
public class KineticRotorRenderer<T extends BlockEntity> implements BlockEntityRenderer<T, KineticRotorRenderer.State> {
	public static final class State extends BlockEntityRenderState {
		KineticRotors.@Nullable Spin spin;
	}

	public KineticRotorRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(T entity, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(entity, state, crumbling);
		state.spin = KineticRotors.extract(entity, partialTick);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		KineticRotors.submit(state.spin, pose, collector, state.lightCoords);
	}

	@Override
	public int getViewDistance() {
		return 96;
	}
}
