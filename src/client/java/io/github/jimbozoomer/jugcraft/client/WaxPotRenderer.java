package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.WaxPotBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The wax in a Wax Melting Pot: a surface at the height of how much there is, in its colour (its dyes, or the wax's
 * own), glossy and bright while molten, dull once set.
 */
public class WaxPotRenderer implements BlockEntityRenderer<WaxPotBlockEntity, WaxPotRenderer.State> {
	private static final RenderType SURFACE = RenderTypes.entityCutout(Jugcraft.id("textures/entity/wax_surface.png"));

	public static final class State extends BlockEntityRenderState {
		int total;
		boolean molten;
		int color;
	}

	public WaxPotRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(WaxPotBlockEntity pot, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(pot, state, crumbling);
		state.total = pot.total();
		state.molten = pot.molten() > 0;
		state.color = pot.color();
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.total <= 0) {
			return;
		}
		float y = 1.0F + 8.0F * state.total / WaxPotBlockEntity.CAPACITY;
		int color = state.molten ? state.color : darker(state.color);
		int light = state.lightCoords;
		collector.submitCustomGeometry(pose, SURFACE, (matrix, buffer) -> TintedBoxes.top(buffer, matrix, 3.0F, 3.0F, 13.0F, 13.0F, y,
				0xFF000000 | color, light));
	}

	/** Set wax is a shade duller. */
	static int darker(int rgb) {
		int r = ((rgb >> 16) & 0xFF) * 4 / 5;
		int g = ((rgb >> 8) & 0xFF) * 4 / 5;
		int b = (rgb & 0xFF) * 4 / 5;
		return (r << 16) | (g << 8) | b;
	}
}
