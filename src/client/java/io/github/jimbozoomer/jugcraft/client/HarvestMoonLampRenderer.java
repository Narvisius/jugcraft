package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.HarvestMoonLampBlock;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the Harvest Moon Lamp's face from its first block (decor19_quads.json: {@code harvest_moon_face_0} to {@code _7},
 * the disc two blocks across in tonight's phase, for a lamp facing north whose other column lies to the west), at full
 * brightness while it is lit.
 */
public class HarvestMoonLampRenderer implements BlockEntityRenderer<DecorationBlockEntity, HarvestMoonLampRenderer.State> {
	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		int phase;
		boolean lit;
	}

	public HarvestMoonLampRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DecorationBlockEntity lamp, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(lamp, state, crumbling);
		BlockState block = lamp.getBlockState();
		if (!(block.getBlock() instanceof HarvestMoonLampBlock) || lamp.getLevel() == null) {
			return;
		}
		state.facing = block.getValue(HarvestMoonLampBlock.FACING);
		state.phase = HarvestMoonLampBlock.phase(lamp.getLevel());
		state.lit = block.getValue(HarvestMoonLampBlock.LIT);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel face = DecorQuads.get("harvest_moon_face_" + state.phase);
		if (face == null) {
			return;
		}
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		face.submit(pose, collector, state.lit ? 0xF000F0 : state.lightCoords);
		pose.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true; // The face spans all four blocks.
	}
}
