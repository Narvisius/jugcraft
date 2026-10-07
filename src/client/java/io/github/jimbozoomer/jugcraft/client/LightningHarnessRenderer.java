package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.LightningHarnessBlock;
import io.github.jimbozoomer.jugcraft.agriculture.LightningHarnessBlockEntity;
import java.util.ArrayList;
import java.util.List;
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
 * Draws a Lightning Harness's arcs for {@value LightningHarnessBlock#ARC_TICKS} ticks after it fires: a crackling bolt
 * between its two electrode tips, and two more from them down to the Lab Table below (when there is one), re-jagged
 * every tick, at full brightness, in the Tesla Coil's violet.
 */
public class LightningHarnessRenderer implements BlockEntityRenderer<LightningHarnessBlockEntity, LightningHarnessRenderer.State> {
	private static final RenderType ARC = RenderTypes.entityCutout(Jugcraft.id("textures/entity/tesla_coil_arc.png"));
	private static final int FULL_BRIGHT = 0xF000F0;
	/** The electrode tips, in blocks from the harness's corner (tools/decor19_data.py draws them there). */
	private static final float[] LEFT = {3.5F / 16, 3.2F / 16, 0.5F};
	private static final float[] RIGHT = {12.5F / 16, 3.2F / 16, 0.5F};
	/** The table top's height above the floor of its block. */
	private static final float TABLE_TOP = 15.0F / 16;
	private static final int SEGMENTS = 7;
	private static final float WIDTH = 0.03F;

	public static final class State extends BlockEntityRenderState {
		final List<float[][]> bolts = new ArrayList<>();
	}

	public LightningHarnessRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(LightningHarnessBlockEntity harness, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(harness, state, crumbling);
		state.bolts.clear();
		if (harness.getLevel() == null) {
			return;
		}
		long time = harness.getLevel().getGameTime();
		long age = time - harness.firedAt();
		if (age < 0 || age >= LightningHarnessBlock.ARC_TICKS) {
			return;
		}
		RandomSource random = RandomSource.create(harness.firedAt() * 31 + time + harness.getBlockPos().asLong());
		state.bolts.add(bolt(random, LEFT, RIGHT));
		if (harness.arcDown() > 0) {
			float[] table = {0.5F, -harness.arcDown() + TABLE_TOP, 0.5F};
			state.bolts.add(bolt(random, LEFT, table));
			state.bolts.add(bolt(random, RIGHT, table));
		}
	}

	/** A jagged line of {@value #SEGMENTS} segments from {@code a} to {@code b}, its ends fixed. */
	static float[][] bolt(RandomSource random, float[] a, float[] b) {
		float length = (float) Math.sqrt((b[0] - a[0]) * (b[0] - a[0]) + (b[1] - a[1]) * (b[1] - a[1]) + (b[2] - a[2]) * (b[2] - a[2]));
		float jag = Math.min(0.3F, 0.05F + length * 0.05F);
		float[][] points = new float[SEGMENTS + 1][3];
		for (int i = 0; i <= SEGMENTS; i++) {
			float t = (float) i / SEGMENTS;
			float edge = i == 0 || i == SEGMENTS ? 0.0F : 1.0F;
			for (int k = 0; k < 3; k++) {
				points[i][k] = a[k] + (b[k] - a[k]) * t + (random.nextFloat() - 0.5F) * 2 * jag * edge;
			}
		}
		return points;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.bolts.isEmpty()) {
			return;
		}
		List<float[][]> bolts = new ArrayList<>(state.bolts);
		collector.submitCustomGeometry(pose, ARC, (matrix, buffer) -> {
			for (float[][] points : bolts) {
				for (int i = 0; i < points.length - 1; i++) {
					segment(buffer, matrix, points[i], points[i + 1]);
				}
			}
		});
	}

	/** One segment as three crossed thin cards, so it shows from every side. */
	static void segment(VertexConsumer buffer, PoseStack.Pose matrix, float[] a, float[] b) {
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
		return true; // Its arcs reach down to the table.
	}
}
