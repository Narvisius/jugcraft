package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.WeathervaneBlock;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a Weathervane's vane turned to point into the wind ({@link WeathervaneBlock#wind}, swung about by
 * {@link WeathervaneBlock#gust} in rain and storms), turning at most {@link WeathervaneBlock#TURN_SPEED} degrees a
 * tick. A vane that has just come into view already points into the wind.
 */
public class WeathervaneRenderer implements BlockEntityRenderer<DecorationBlockEntity, WeathervaneRenderer.State> {
	/** Each vane as this client last left it: {yaw, game time}. */
	private final Map<DecorationBlockEntity, double[]> vanes = new WeakHashMap<>();

	public static final class State extends BlockEntityRenderState {
		String design = "bat";
		float yaw;
	}

	public WeathervaneRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DecorationBlockEntity vane, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(vane, state, crumbling);
		BlockState block = vane.getBlockState();
		Level level = vane.getLevel();
		if (!(block.getBlock() instanceof WeathervaneBlock weathervane) || level == null) {
			return;
		}
		state.design = weathervane.design();
		double now = level.getGameTime() + (double) partialTick;
		float target = WeathervaneBlock.wind(now) + WeathervaneBlock.gust(now, level.getRainLevel(partialTick), level.getThunderLevel(partialTick));
		double[] at = vanes.computeIfAbsent(vane, v -> new double[] {target, now});
		float elapsed = (float) Math.max(0.0, Math.min(40.0, now - at[1]));
		float step = WeathervaneBlock.TURN_SPEED * elapsed;
		at[0] = at[0] + Mth.clamp(Mth.wrapDegrees(target - (float) at[0]), -step, step);
		at[1] = now;
		state.yaw = (float) at[0];
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel model = DecorQuads.get("weathervane_" + state.design);
		if (model == null) {
			return;
		}
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -state.yaw);
		pose.translate(-0.5F, 0.0F, -0.5F);
		model.submit(pose, collector, state.lightCoords);
		pose.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true; // The vane turns above the block.
	}
}
