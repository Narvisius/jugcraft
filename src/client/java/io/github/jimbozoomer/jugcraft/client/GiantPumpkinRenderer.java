package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.agriculture.CarvingFace;
import io.github.jimbozoomer.jugcraft.agriculture.GiantPumpkinBlock;
import io.github.jimbozoomer.jugcraft.agriculture.GiantPumpkinBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a full-grown giant pumpkin's carvings over its 3x3x3 block model: one 48x48 quad per carved side, a
 * hair outside the cube, textured by {@link CarvingTextures}. It is drawn from the master block (the lowest
 * north-west corner), so it is drawn even when that corner is off screen. Each side takes the light in
 * front of its middle; with a torch inside, the carving glows at full brightness.
 */
public class GiantPumpkinRenderer implements BlockEntityRenderer<GiantPumpkinBlockEntity, GiantPumpkinRenderer.State> {
	private static final float OUT = 0.002F;
	private static final int SIZE = GiantPumpkinBlock.MAX_SIZE;

	public static final class State extends BlockEntityRenderState {
		final int[][] faces = new int[4][];
		final boolean[] carved = new boolean[4];
		final int[] light = new int[4];
		@Nullable RenderType type;
	}

	public GiantPumpkinRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(GiantPumpkinBlockEntity pumpkin, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(pumpkin, state, crumbling);
		state.type = null;
		if (!pumpkin.fullGrown() || !pumpkin.carved()) {
			return;
		}
		boolean lit = pumpkin.lit();
		Level level = pumpkin.getLevel();
		BlockPos middle = pumpkin.getBlockPos().offset(SIZE / 2, SIZE / 2, SIZE / 2);
		for (Direction side : Direction.Plane.HORIZONTAL) {
			int index = side.get2DDataValue();
			state.faces[index] = pumpkin.face(side);
			state.carved[index] = !CarvingFace.isBlank(state.faces[index]);
			state.light[index] = lit ? LightCoordsUtil.FULL_BRIGHT
					: level != null ? LightCoordsUtil.getLightCoords(level, middle.relative(side, SIZE / 2 + 1)) : state.lightCoords;
		}
		state.type = CarvingTextures.getGiant(state.faces, lit, pumpkin.soul());
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		RenderType type = state.type;
		if (type == null) {
			return;
		}
		boolean[] carved = state.carved.clone();
		int[] light = state.light.clone();
		collector.submitCustomGeometry(pose, type, (matrix, buffer) -> {
			for (Direction side : Direction.Plane.HORIZONTAL) {
				int index = side.get2DDataValue();
				if (carved[index]) {
					side(buffer, matrix, side, index, light[index]);
				}
			}
		});
	}

	/** One face of the texture on one side of the cube, seen the right way round from outside. */
	private static void side(VertexConsumer buffer, PoseStack.Pose matrix, Direction side, int face, int light) {
		float u0 = face / 4.0F;
		float u1 = (face + 1) / 4.0F;
		float s = SIZE;
		float[][] corners = switch (side) {
			case NORTH -> new float[][] {{s, s, -OUT}, {s, 0, -OUT}, {0, 0, -OUT}, {0, s, -OUT}};
			case SOUTH -> new float[][] {{0, s, s + OUT}, {0, 0, s + OUT}, {s, 0, s + OUT}, {s, s, s + OUT}};
			case WEST -> new float[][] {{-OUT, s, 0}, {-OUT, 0, 0}, {-OUT, 0, s}, {-OUT, s, s}};
			default -> new float[][] {{s + OUT, s, s}, {s + OUT, 0, s}, {s + OUT, 0, 0}, {s + OUT, s, 0}};
		};
		float[][] uv = {{u0, 0}, {u0, 1}, {u1, 1}, {u1, 0}};
		for (int i = 0; i < 4; i++) {
			buffer.addVertex(matrix, corners[i][0], corners[i][1], corners[i][2]).setColor(0xFFFFFFFF).setUv(uv[i][0], uv[i][1])
					.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(matrix, side.getStepX(), side.getStepY(), side.getStepZ());
		}
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true; // The cube reaches two blocks past its master block, which may be off screen.
	}
}
