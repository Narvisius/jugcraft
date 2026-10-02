package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.fluid.FluidTankBlockEntity;
import io.github.jimbozoomer.jugcraft.fluid.JugcraftFluids;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the fluid inside a glass tank (batch 20): a column filling the tank from the bottom to its level, tinted the
 * fluid's colour (the gauge colour of Jugcraft's fluids; water and lava their own; anything else a neutral grey),
 * seen through the glass. Tinplate tanks hide theirs.
 */
public class GlassTankRenderer implements BlockEntityRenderer<FluidTankBlockEntity, GlassTankRenderer.State> {
	private static final RenderType FLUID = RenderTypes.entityTranslucentCull(Jugcraft.id("textures/block/glass_tank_fluid.png"));
	/** The glass and frame are a pixel thick: the fluid fills the inside. */
	private static final float INSET = 1.05F / 16;

	public static final class State extends BlockEntityRenderState {
		float fill;
		int color;
	}

	public GlassTankRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(FluidTankBlockEntity tank, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(tank, state, crumbling);
		boolean glass = tank.getBlockState().is(JugcraftFluids.GLASS_TANK);
		state.fill = glass && !tank.storage.isResourceBlank()
				? (float) tank.storage.amount / FluidTankBlockEntity.CAPACITY : 0;
		state.color = glass ? color(tank.storage.variant.getFluid()) : 0;
	}

	/** ARGB, a little see-through. */
	static int color(Fluid fluid) {
		int rgb;
		if (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER) {
			rgb = 0x3F76E4;
		} else if (fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA) {
			rgb = 0xE8642A;
		} else {
			int gauge = PetroFluids.gaugeColor(fluid);
			rgb = gauge != 0 ? gauge & 0xFFFFFF : 0x9AA4B0;
		}
		return 0xC8000000 | rgb;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.fill <= 0) {
			return;
		}
		float lo = INSET;
		float hi = 1 - INSET;
		float top = lo + (hi - lo) * Math.min(1, state.fill);
		int color = state.color;
		int light = state.lightCoords;
		collector.submitCustomGeometry(pose, FLUID, (matrix, buffer) -> {
			// Top, bottom and the four sides, each facing outwards.
			quad(buffer, matrix, light, color, 0, 1, 0, lo, top, lo, lo, top, hi, hi, top, hi, hi, top, lo);
			quad(buffer, matrix, light, color, 0, -1, 0, lo, lo, lo, hi, lo, lo, hi, lo, hi, lo, lo, hi);
			quad(buffer, matrix, light, color, 0, 0, -1, lo, lo, lo, lo, top, lo, hi, top, lo, hi, lo, lo);
			quad(buffer, matrix, light, color, 0, 0, 1, hi, lo, hi, hi, top, hi, lo, top, hi, lo, lo, hi);
			quad(buffer, matrix, light, color, -1, 0, 0, lo, lo, hi, lo, top, hi, lo, top, lo, lo, lo, lo);
			quad(buffer, matrix, light, color, 1, 0, 0, hi, lo, lo, hi, top, lo, hi, top, hi, hi, lo, hi);
		});
	}

	private static void quad(VertexConsumer buffer, PoseStack.Pose matrix, int light, int color, float nx, float ny, float nz,
			float... corners) {
		float[][] uv = {{0, 1}, {0, 0}, {1, 0}, {1, 1}};
		for (int i = 0; i < 4; i++) {
			buffer.addVertex(matrix, corners[i * 3], corners[i * 3 + 1], corners[i * 3 + 2]).setColor(color)
					.setUv(uv[i][0], uv[i][1]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
					.setNormal(matrix, nx, ny, nz);
		}
	}
}
