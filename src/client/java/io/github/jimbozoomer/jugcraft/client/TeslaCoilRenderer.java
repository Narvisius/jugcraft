package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.TeslaCoilBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a Tesla Coil's arcs, on its lower half: for {@link TeslaCoilBlockEntity#ARC_TICKS} ticks after the server
 * says it struck, a jagged violet bolt from the top of its toroid to the top of the coil it struck (or a point in
 * the air), at full brightness, re-jagged every tick so it crackles.
 */
public class TeslaCoilRenderer implements BlockEntityRenderer<TeslaCoilBlockEntity, TeslaCoilRenderer.State> {
	private static final RenderType ARC = RenderTypes.entityCutout(Jugcraft.id("textures/entity/tesla_coil_arc.png"));
	private static final int FULL_BRIGHT = 0xF000F0;
	/** The top of the toroid, from the lower half's floor, in blocks. */
	private static final float TOP = 1.0F + 14.5F / 16;
	private static final int SEGMENTS = 8;
	private static final float WIDTH = 0.035F;

	public static final class State extends BlockEntityRenderState {
		boolean striking;
		float[][] points = new float[SEGMENTS + 1][3];
	}

	public TeslaCoilRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(TeslaCoilBlockEntity coil, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(coil, state, crumbling);
		state.striking = false;
		if (coil.getLevel() == null) {
			return;
		}
		long time = coil.getLevel().getGameTime();
		long age = time - coil.arcTime();
		if (age < 0 || age >= TeslaCoilBlockEntity.ARC_TICKS) {
			return;
		}
		int[] offset = TeslaCoilBlockEntity.unpack(coil.arc());
		boolean intoAir = (coil.arc() & TeslaCoilBlockEntity.INTO_AIR) != 0;
		float tx = offset[0] + 0.5F;
		float ty = offset[1] + (intoAir ? 0.5F : TOP);
		float tz = offset[2] + 0.5F;
		RandomSource random = RandomSource.create(coil.arcTime() * 31 + time + coil.getBlockPos().asLong());
		float length = (float) Math.sqrt((tx - 0.5F) * (tx - 0.5F) + (ty - TOP) * (ty - TOP) + (tz - 0.5F) * (tz - 0.5F));
		float jag = Math.min(0.35F, 0.08F + length * 0.04F);
		for (int i = 0; i <= SEGMENTS; i++) {
			float t = (float) i / SEGMENTS;
			float edge = i == 0 || i == SEGMENTS ? 0.0F : 1.0F;
			state.points[i][0] = 0.5F + (tx - 0.5F) * t + (random.nextFloat() - 0.5F) * 2 * jag * edge;
			state.points[i][1] = TOP + (ty - TOP) * t + (random.nextFloat() - 0.5F) * 2 * jag * edge;
			state.points[i][2] = 0.5F + (tz - 0.5F) * t + (random.nextFloat() - 0.5F) * 2 * jag * edge;
		}
		state.striking = true;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.striking) {
			return;
		}
		float[][] points = new float[SEGMENTS + 1][];
		for (int i = 0; i <= SEGMENTS; i++) {
			points[i] = state.points[i].clone();
		}
		collector.submitCustomGeometry(pose, ARC, (matrix, buffer) -> {
			for (int i = 0; i < SEGMENTS; i++) {
				segment(buffer, matrix, points[i], points[i + 1]);
			}
		});
	}

	/** One segment of the bolt as two crossed thin cards, so it shows from every side. */
	private static void segment(VertexConsumer buffer, PoseStack.Pose matrix, float[] a, float[] b) {
		DecorDraw.quad(buffer, matrix, new float[][] {
				{a[0] - WIDTH, a[1], a[2], 0, 0}, {b[0] - WIDTH, b[1], b[2], 0, 1}, {b[0] + WIDTH, b[1], b[2], 1, 1}, {a[0] + WIDTH, a[1], a[2], 1, 0}},
				0, 0, 1, 0xFFFFFFFF, FULL_BRIGHT);
		DecorDraw.quad(buffer, matrix, new float[][] {
				{a[0], a[1], a[2] - WIDTH, 0, 0}, {b[0], b[1], b[2] - WIDTH, 0, 1}, {b[0], b[1], b[2] + WIDTH, 1, 1}, {a[0], a[1], a[2] + WIDTH, 1, 0}},
				1, 0, 0, 0xFFFFFFFF, FULL_BRIGHT);
		DecorDraw.quad(buffer, matrix, new float[][] {
				{a[0], a[1] - WIDTH, a[2], 0, 0}, {b[0], b[1] - WIDTH, b[2], 0, 1}, {b[0], b[1] + WIDTH, b[2], 1, 1}, {a[0], a[1] + WIDTH, a[2], 1, 0}},
				0, 1, 0, 0xFFFFFFFF, FULL_BRIGHT);
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true; // Its arcs reach other coils.
	}
}
