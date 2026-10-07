package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.SilhouetteWindowBlock;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Lights a Silhouette Window: on each side whose far side is lit ({@link SilhouetteWindowBlock#glows}), it lays the
 * glowing paper (the same cut-out, bright) over the pane at full brightness, so the cut-out stands black against it.
 */
public class SilhouetteWindowRenderer implements BlockEntityRenderer<DecorationBlockEntity, SilhouetteWindowRenderer.State> {
	private static final int FULL_BRIGHT = 0xF000F0;
	private static final Map<SilhouetteWindowBlock.Design, RenderType> LIT = new EnumMap<>(SilhouetteWindowBlock.Design.class);

	static {
		for (SilhouetteWindowBlock.Design design : SilhouetteWindowBlock.Design.values()) {
			LIT.put(design, RenderTypes.entitySolid(Jugcraft.id("textures/entity/silhouette_window_" + design.getSerializedName() + "_lit.png")));
		}
	}

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		SilhouetteWindowBlock.Design design = SilhouetteWindowBlock.Design.BAT;
		boolean front;
		boolean back;
	}

	public SilhouetteWindowRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DecorationBlockEntity window, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(window, state, crumbling);
		BlockState block = window.getBlockState();
		state.front = false;
		state.back = false;
		if (!(block.getBlock() instanceof SilhouetteWindowBlock) || window.getLevel() == null) {
			return;
		}
		state.facing = block.getValue(SilhouetteWindowBlock.FACING);
		state.design = block.getValue(SilhouetteWindowBlock.DESIGN);
		state.front = SilhouetteWindowBlock.glows(window.getLevel(), window.getBlockPos(), state.facing);
		state.back = SilhouetteWindowBlock.glows(window.getLevel(), window.getBlockPos(), state.facing.getOpposite());
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.front && !state.back) {
			return;
		}
		boolean front = state.front;
		boolean back = state.back;
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		collector.submitCustomGeometry(pose, LIT.get(state.design), (matrix, buffer) -> {
			float lo = 1.0F / 16;
			float hi = 15.0F / 16;
			if (front) {
				// The north side, as the block model maps a north face: u runs from east to west.
				float z = 7.25F / 16; // a quarter pixel proud of the paper, so it never sinks into it
				// Corners in the order the generated quads use (tools/decor6_data.py FACE_CORNERS), which these render types
				// show from the front.
				float[][] corners = {{lo, lo}, {lo, hi}, {hi, hi}, {hi, lo}};
				for (float[] c : corners) {
					buffer.addVertex(matrix, c[0], c[1], z).setColor(0xFFFFFFFF).setUv(1.0F - c[0], 1.0F - c[1])
							.setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(matrix, 0, 0, -1);
				}
			}
			if (back) {
				// The south side: u runs from west to east.
				float z = 8.75F / 16;
				float[][] corners = {{hi, lo}, {hi, hi}, {lo, hi}, {lo, lo}};
				for (float[] c : corners) {
					buffer.addVertex(matrix, c[0], c[1], z).setColor(0xFFFFFFFF).setUv(c[0], 1.0F - c[1])
							.setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(matrix, 0, 0, 1);
				}
			}
		});
		pose.popPose();
	}
}
