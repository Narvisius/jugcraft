package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.GiantBeatingHeartBlock;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the Giant's Beating Heart's heart from its first block: the Beating Heart Jar's own heart on its brass stand
 * ({@link OddityJarRenderer#heart}), {@value GiantBeatingHeartBlock#SIZE} times bigger like the jar round it, turning
 * slowly and swelling with each beat the block gives just as the jar's does, lit by the light in the middle of the jar.
 */
public class GiantBeatingHeartRenderer implements BlockEntityRenderer<DecorationBlockEntity, GiantBeatingHeartRenderer.State> {
	/** When each heart's last beat began, as this client saw it: {beat was on, game time}. */
	private final Map<DecorationBlockEntity, double[]> beats = new WeakHashMap<>();

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		float time;
		float pulse;
		int light;
		boolean whole;
	}

	public GiantBeatingHeartRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DecorationBlockEntity heart, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(heart, state, crumbling);
		BlockState block = heart.getBlockState();
		Level level = heart.getLevel();
		state.whole = block.getBlock() instanceof GiantBeatingHeartBlock && level != null;
		if (!(block.getBlock() instanceof GiantBeatingHeartBlock giant) || level == null) {
			return;
		}
		state.facing = block.getValue(GiantBeatingHeartBlock.FACING);
		double now = level.getGameTime() + (double) partialTick;
		state.time = (float) (now % 24000.0);
		boolean beat = block.getValue(GiantBeatingHeartBlock.BEAT);
		double[] seen = beats.computeIfAbsent(heart, h -> new double[] {0.0, -100.0});
		if (beat && seen[0] == 0.0) {
			seen[1] = now;
		}
		seen[0] = beat ? 1.0 : 0.0;
		state.pulse = OddityJarRenderer.pulse(now - seen[1]);
		state.light = LightCoordsUtil.getLightCoords(level, giant.partPos(heart.getBlockPos(), state.facing, GiantBeatingHeartBlock.MIDDLE));
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.whole) {
			return;
		}
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		// The jar's frame runs across from its far block, to the left of the first (as the placer saw it), and is the
		// Beating Heart Jar's block made SIZE times bigger.
		pose.translate(-(GiantBeatingHeartBlock.SIZE - 1), 0.0F, 0.0F);
		pose.scale(GiantBeatingHeartBlock.SIZE, GiantBeatingHeartBlock.SIZE, GiantBeatingHeartBlock.SIZE);
		OddityJarRenderer.heart(pose, collector, state.time, state.pulse, state.light);
		pose.popPose();
	}

	/** The heart fills a jar three blocks across, past its first block's box. */
	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}
}
