package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.SilkSpoolStackBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SilkSpoolStackBlockEntity;
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
 * Draws the silk wound on a Silk Spool Stack's three spools (decor19_quads.json: {@code silk_spool_silk_0} to {@code _2},
 * left to right as seen from the front of a stack facing north), each tinted its dye colour over a pale, sheened
 * winding.
 */
public class SilkSpoolRenderer implements BlockEntityRenderer<SilkSpoolStackBlockEntity, SilkSpoolRenderer.State> {
	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		final int[] colours = new int[SilkSpoolStackBlock.SPOOLS];
	}

	public SilkSpoolRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(SilkSpoolStackBlockEntity spools, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(spools, state, crumbling);
		BlockState block = spools.getBlockState();
		if (!(block.getBlock() instanceof SilkSpoolStackBlock)) {
			return;
		}
		state.facing = block.getValue(SilkSpoolStackBlock.FACING);
		for (int i = 0; i < SilkSpoolStackBlock.SPOOLS; i++) {
			state.colours[i] = 0xFF000000 | spools.colour(i).getTextureDiffuseColor();
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		for (int i = 0; i < SilkSpoolStackBlock.SPOOLS; i++) {
			QuadModel silk = DecorQuads.get("silk_spool_silk_" + i);
			if (silk != null) {
				silk.submit(pose, collector, state.lightCoords, state.colours[i]);
			}
		}
		pose.popPose();
	}
}
