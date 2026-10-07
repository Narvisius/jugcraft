package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.BoneWindChimesBlock;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the Bone Wind Chimes' bones and the little skull striker in the middle, each swinging on its string from the
 * wooden disc, every one a little out of step with the others, by {@link BoneWindChimesBlock#swing} for the weather.
 */
public class BoneWindChimesRenderer implements BlockEntityRenderer<DecorationBlockEntity, BoneWindChimesRenderer.State> {
	private static final float RADIUS = 3.0F;
	private static final float PIVOT_Y = 12.5F;

	public static final class State extends BlockEntityRenderState {
		float time;
		float swing;
		float phase;
	}

	public BoneWindChimesRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DecorationBlockEntity chimes, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(chimes, state, crumbling);
		Level level = chimes.getLevel();
		if (level == null) {
			return;
		}
		state.time = (float) ((level.getGameTime() + (double) partialTick) % 24000.0);
		state.swing = BoneWindChimesBlock.swing(level.getRainLevel(partialTick), level.getThunderLevel(partialTick));
		state.phase = (chimes.getBlockPos().hashCode() & 0xFF) / 256.0F * Mth.TWO_PI;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel bone = DecorQuads.get("wind_chime_bone");
		QuadModel skull = DecorQuads.get("wind_chime_skull");
		float speed = 0.08F + state.swing * 0.006F;
		for (int i = 0; i <= BoneWindChimesBlock.BONES; i++) {
			boolean striker = i == BoneWindChimesBlock.BONES;
			QuadModel model = striker ? skull : bone;
			if (model == null) {
				continue;
			}
			float angle = Mth.TWO_PI * i / BoneWindChimesBlock.BONES;
			float x = striker ? 8.0F : 8.0F + RADIUS * Mth.cos(angle);
			float z = striker ? 8.0F : 8.0F + RADIUS * Mth.sin(angle);
			float t = state.time * speed + state.phase + i * 1.7F;
			pose.pushPose();
			pose.translate(x / 16, PIVOT_Y / 16, z / 16);
			pose.rotateDegrees(Axis.XP, state.swing * Mth.sin(t));
			pose.rotateDegrees(Axis.ZP, state.swing * 0.7F * Mth.cos(t * 1.3F));
			pose.translate(0.0F, -PIVOT_Y / 16, 0.0F);
			model.submit(pose, collector, state.lightCoords);
			pose.popPose();
		}
	}
}
