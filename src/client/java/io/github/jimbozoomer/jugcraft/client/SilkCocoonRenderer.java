package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.SilkCocoonBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SilkCocoonBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a Silk Cocoon (decor19_quads.json: {@code silk_cocoon}, the bundle on its thread hung from the top of its block)
 * swaying slowly about where the thread meets the ceiling, each cocoon out of step with the next; for
 * {@value SilkCocoonBlock#WRIGGLE_TICKS} ticks after it is opened or twitches, it wriggles hard and settles.
 */
public class SilkCocoonRenderer implements BlockEntityRenderer<SilkCocoonBlockEntity, SilkCocoonRenderer.State> {
	public static final class State extends BlockEntityRenderState {
		float swayX;
		float swayZ;
		float twist;
	}

	public SilkCocoonRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(SilkCocoonBlockEntity cocoon, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(cocoon, state, crumbling);
		if (cocoon.getLevel() == null) {
			return;
		}
		long now = cocoon.getLevel().getGameTime();
		float t = Math.floorMod(now, 24000L) + partialTick;
		float phase = (cocoon.getBlockPos().hashCode() & 255) / 40.0F;
		state.swayX = 3.0F * Mth.sin(t * 0.045F + phase);
		state.swayZ = 2.0F * Mth.sin(t * 0.031F + phase * 1.7F);
		state.twist = 0.0F;
		float since = now - cocoon.wriggled() + partialTick;
		if (since >= 0 && since < SilkCocoonBlock.WRIGGLE_TICKS) {
			float fade = 1.0F - since / SilkCocoonBlock.WRIGGLE_TICKS;
			state.swayX += 9.0F * fade * Mth.sin(since * 1.9F);
			state.twist = 20.0F * fade * Mth.sin(since * 1.3F);
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel cocoon = DecorQuads.get("silk_cocoon");
		if (cocoon == null) {
			return;
		}
		pose.pushPose();
		pose.translate(0.5F, 1.0F, 0.5F);
		pose.rotateDegrees(Axis.XP, state.swayX);
		pose.rotateDegrees(Axis.ZP, state.swayZ);
		pose.rotateDegrees(Axis.YP, state.twist);
		pose.translate(-0.5F, -1.0F, -0.5F);
		cocoon.submit(pose, collector, state.lightCoords);
		pose.popPose();
	}
}
