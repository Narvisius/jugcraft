package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.SpecimenJarBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SpecimenVesselBlock;
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
 * Draws what floats in a Tall Specimen Jar or a Specimen Tank, from its first block: the Specimen Jar's own specimen and
 * bubbles ({@link SpecimenJarRenderer#draw}), bobbing and turning as in the jar, {@link SpecimenVesselBlock#scale()}
 * times bigger like the jar round them.
 */
public class SpecimenVesselRenderer implements BlockEntityRenderer<DecorationBlockEntity, SpecimenVesselRenderer.State> {
	public static final class State extends BlockEntityRenderState {
		SpecimenJarBlock.Specimen specimen = SpecimenJarBlock.Specimen.EYE;
		Direction facing = Direction.NORTH;
		int scale = 1;
		int across = 1;
		int deep = 1;
		boolean whole;
		float bob;
		float turn;
		float time;
	}

	public SpecimenVesselRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DecorationBlockEntity vessel, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(vessel, state, crumbling);
		BlockState block = vessel.getBlockState();
		state.whole = block.getBlock() instanceof SpecimenVesselBlock && vessel.getLevel() != null;
		if (!(block.getBlock() instanceof SpecimenVesselBlock big) || vessel.getLevel() == null) {
			return;
		}
		state.specimen = block.getValue(SpecimenVesselBlock.SPECIMEN);
		state.facing = block.getValue(SpecimenVesselBlock.FACING);
		state.scale = big.scale();
		state.across = big.across();
		state.deep = big.deep();
		state.time = vessel.getLevel().getGameTime() % 24000 + partialTick;
		state.bob = SpecimenJarBlock.bob(vessel.getBlockPos(), state.time);
		state.turn = SpecimenJarRenderer.turn(vessel.getBlockPos(), state.time);
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
		// The jar's frame runs across from its far block, to the left of the first (as the placer saw it). The Specimen
		// Jar's block, made `scale` times bigger about its bottom middle, stands at the middle of the frame's floor.
		pose.translate(-(state.across - 1), 0.0F, 0.0F);
		pose.translate(state.across / 2.0F, 0.0F, state.deep / 2.0F);
		pose.scale(state.scale, state.scale, state.scale);
		pose.translate(-0.5F, 0.0F, -0.5F);
		SpecimenJarRenderer.draw(pose, collector, state.specimen, state.bob, state.turn, state.time);
		pose.popPose();
	}

	/** The fluid fills a jar two or three blocks tall, past the first block's box. */
	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}
}
