package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.FloatingCandleBlock;
import io.github.jimbozoomer.jugcraft.agriculture.FloatingCandleBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws Floating Candles: each candle a little column of wax at its place ({@link FloatingCandleBlock#candle}),
 * bobbing out of step with the others ({@link FloatingCandleBlock#bob}), with a flame at full brightness while lit.
 * The texture holds the wax's side (u 0-8), the flame (u 8-12) and the wax's top with its wick (u 12-14).
 */
public class FloatingCandleRenderer implements BlockEntityRenderer<FloatingCandleBlockEntity, FloatingCandleRenderer.State> {
	private static final RenderType CANDLE = RenderTypes.entityCutout(Jugcraft.id("textures/entity/floating_candle.png"));
	private static final float WIDTH = 2.0F / 16;
	private static final float FLAME_WIDTH = 2.0F / 16;
	private static final float FLAME_HEIGHT = 3.0F / 16;
	private static final int FULL_BRIGHT = 0xF000F0;

	public static final class State extends BlockEntityRenderState {
		int count;
		boolean lit;
		final float[][] at = new float[FloatingCandleBlock.MAX][3];
	}

	public FloatingCandleRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(FloatingCandleBlockEntity entity, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(entity, state, crumbling);
		BlockState block = entity.getBlockState();
		state.count = 0;
		if (!(block.getBlock() instanceof FloatingCandleBlock) || entity.getLevel() == null) {
			return;
		}
		state.count = block.getValue(FloatingCandleBlock.CANDLES);
		state.lit = block.getValue(FloatingCandleBlock.LIT);
		long time = entity.getLevel().getGameTime();
		for (int i = 0; i < state.count; i++) {
			float[] at = FloatingCandleBlock.candle(state.count, i);
			state.at[i][0] = at[0] / 16;
			state.at[i][1] = at[2] / 16 + FloatingCandleBlock.bob(entity.getBlockPos(), i, time, partialTick);
			state.at[i][2] = at[1] / 16;
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		int count = state.count;
		if (count == 0) {
			return;
		}
		// The geometry is built later in the frame; take the values now.
		float[][] at = new float[count][];
		for (int i = 0; i < count; i++) {
			at[i] = state.at[i].clone();
		}
		boolean lit = state.lit;
		int light = state.lightCoords;
		collector.submitCustomGeometry(pose, CANDLE, (matrix, buffer) -> {
			for (int i = 0; i < count; i++) {
				float height = FloatingCandleBlock.HEIGHT[i] / 16;
				wax(buffer, matrix, light, at[i][0], at[i][1], at[i][2], height);
				if (lit) {
					flame(buffer, matrix, at[i][0], at[i][1] + height, at[i][2]);
				}
			}
		});
	}

	/** A column of wax {@link #WIDTH} square, {@code height} tall, its bottom centred on (x, y, z). */
	private static void wax(VertexConsumer buffer, PoseStack.Pose matrix, int light, float x, float y, float z, float height) {
		float h = WIDTH / 2;
		float x0 = x - h;
		float x1 = x + h;
		float z0 = z - h;
		float z1 = z + h;
		float y1 = y + height;
		float v = height;
		// Sides: the wax texture's drips, as tall as the candle.
		quad(buffer, matrix, light, 0, 0, -1, x1, y, z0, x0, y, z0, x0, y1, z0, x1, y1, z0, 0.0F, 0.125F, v);
		quad(buffer, matrix, light, 0, 0, 1, x0, y, z1, x1, y, z1, x1, y1, z1, x0, y1, z1, 0.125F, 0.25F, v);
		quad(buffer, matrix, light, -1, 0, 0, x0, y, z0, x0, y, z1, x0, y1, z1, x0, y1, z0, 0.25F, 0.375F, v);
		quad(buffer, matrix, light, 1, 0, 0, x1, y, z1, x1, y, z0, x1, y1, z0, x1, y1, z1, 0.375F, 0.5F, v);
		// Top with the wick, and the bottom.
		quad(buffer, matrix, light, 0, 1, 0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0.75F, 0.875F, 0.125F);
		quad(buffer, matrix, light, 0, -1, 0, x0, y, z0, x1, y, z0, x1, y, z1, x0, y, z1, 0.75F, 0.875F, 0.125F);
	}

	/**
	 * Two crossed flames standing on the wick at (x, y, z), each seen from both sides: two sides lifted off the flame's
	 * middle ({@link DecorDraw#twoSidedReadable}), never one plane drawn twice, the back reading the same way round as the
	 * front.
	 */
	private static void flame(VertexConsumer buffer, PoseStack.Pose matrix, float x, float y, float z) {
		float h = FLAME_WIDTH / 2;
		float y1 = y + FLAME_HEIGHT;
		float v = 0.375F;
		DecorDraw.twoSidedReadable(buffer, matrix, new float[][] {{x - h, y, z, 0.5F, v}, {x + h, y, z, 0.75F, v}, {x + h, y1, z, 0.75F, 0.0F},
				{x - h, y1, z, 0.5F, 0.0F}}, 0, 0, 1, 0xFFFFFFFF, FULL_BRIGHT, DecorDraw.TWO_SIDED_LIFT);
		DecorDraw.twoSidedReadable(buffer, matrix, new float[][] {{x, y, z + h, 0.5F, v}, {x, y, z - h, 0.75F, v}, {x, y1, z - h, 0.75F, 0.0F},
				{x, y1, z + h, 0.5F, 0.0F}}, 1, 0, 0, 0xFFFFFFFF, FULL_BRIGHT, DecorDraw.TWO_SIDED_LIFT);
	}

	/**
	 * One quad with corners bottom-left, bottom-right, top-right, top-left (seen from its front); its texture runs
	 * from u0 to u1 across and from v (bottom) up to 0 (top).
	 */
	private static void quad(VertexConsumer buffer, PoseStack.Pose matrix, int light, float nx, float ny, float nz, float ax, float ay, float az,
			float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz, float u0, float u1, float v) {
		vertex(buffer, matrix, light, nx, ny, nz, ax, ay, az, u0, v);
		vertex(buffer, matrix, light, nx, ny, nz, bx, by, bz, u1, v);
		vertex(buffer, matrix, light, nx, ny, nz, cx, cy, cz, u1, 0.0F);
		vertex(buffer, matrix, light, nx, ny, nz, dx, dy, dz, u0, 0.0F);
	}

	private static void vertex(VertexConsumer buffer, PoseStack.Pose matrix, int light, float nx, float ny, float nz, float x, float y, float z,
			float u, float v) {
		buffer.addVertex(matrix, x, y, z).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
				.setNormal(matrix, nx, ny, nz);
	}
}
