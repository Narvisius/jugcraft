package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.MourningAngelBlock;
import io.github.jimbozoomer.jugcraft.agriculture.YardSilhouetteBlock;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a Yard Silhouette (decor19_quads.json: {@code silhouette_<figure>} on its stake, facing north) turned to its
 * sixteenth of a circle, and at night its eyes ({@code silhouette_<figure>_eyes}) glowing yellow at full brightness.
 */
public class YardSilhouetteRenderer implements BlockEntityRenderer<DecorationBlockEntity, YardSilhouetteRenderer.State> {
	public static final class State extends BlockEntityRenderState {
		String figure = "arched_cat";
		float yaw;
		boolean night;
	}

	public YardSilhouetteRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DecorationBlockEntity cutout, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(cutout, state, crumbling);
		BlockState block = cutout.getBlockState();
		if (!(block.getBlock() instanceof YardSilhouetteBlock) || cutout.getLevel() == null) {
			return;
		}
		state.figure = block.getValue(YardSilhouetteBlock.FIGURE).getSerializedName();
		state.yaw = RotationSegment.convertToDegrees(block.getValue(YardSilhouetteBlock.ROTATION));
		state.night = MourningAngelBlock.night(cutout.getLevel());
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel figure = DecorQuads.get("silhouette_" + state.figure);
		if (figure == null) {
			return;
		}
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		// As a sign is: rotation 0 faces south; the figure is drawn facing north, so it is turned half round first.
		pose.rotateDegrees(Axis.YP, 180.0F - state.yaw);
		pose.translate(-0.5F, 0.0F, -0.5F);
		figure.submit(pose, collector, state.lightCoords);
		QuadModel eyes = state.night ? DecorQuads.get("silhouette_" + state.figure + "_eyes") : null;
		if (eyes != null) {
			eyes.submit(pose, collector, 0xF000F0);
		}
		pose.popPose();
	}
}
