package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.tower.ExchangeStationBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.ExchangeStations;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** One shared station entity registration dispatches both independent instrument sets. */
public final class ExchangeStationRenderer implements BlockEntityRenderer<ExchangeStationBlockEntity, ExchangeStationRenderer.State> {
	private final EnergyExchangeRenderer energy;
	private final CargoExchangeRenderer cargo;
	public static final class State extends BlockEntityRenderState {
		int kind;
		final EnergyExchangeRenderer.State energy = new EnergyExchangeRenderer.State();
		final CargoExchangeRenderer.State cargo = new CargoExchangeRenderer.State();
	}
	public ExchangeStationRenderer(BlockEntityRendererProvider.Context context) { this(context.font()); }
	ExchangeStationRenderer(Font font) { energy = new EnergyExchangeRenderer(font); cargo = new CargoExchangeRenderer(font); }
	@Override public State createRenderState() { return new State(); }
	@Override public int getViewDistance() { return 48; }
	@Override public boolean shouldRenderOffScreen() { return true; }
	@Override public void extractRenderState(ExchangeStationBlockEntity station, State state, float partialTick,
			Vec3 cameraPos, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(station, state, crumbling);
		state.kind = station.isRemoved() ? 0 : station.getBlockState().is(ExchangeStations.ENERGY) ? 1
				: station.getBlockState().is(ExchangeStations.CARGO) ? 2 : 0;
		if (state.kind == 1) energy.extractRenderState(station, state.energy, partialTick, cameraPos, crumbling);
		else if (state.kind == 2) cargo.extractRenderState(station, state.cargo, partialTick, cameraPos, crumbling);
	}
	@Override public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.kind == 1) energy.submit(state.energy, poses, collector, camera);
		else if (state.kind == 2) cargo.submit(state.cargo, poses, collector, camera);
	}
}
