package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.client.DroneDepotRenderer.Geometry;
import io.github.jimbozoomer.jugcraft.nuclear.RailingGateBlock;
import io.github.jimbozoomer.jugcraft.nuclear.RailingGateBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The railing gate itself, swinging on its hinge post: a quarter turn towards the side it faces as it opens (quickly),
 * back again as its spring shuts it (a little slower), with a yellow-and-black tag on it.
 */
public class RailingGateRenderer implements BlockEntityRenderer<RailingGateBlockEntity, RailingGateRenderer.State> {
	private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(StorageTankRenderer.TEXTURE);

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		float swing;
	}

	public RailingGateRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(RailingGateBlockEntity gate, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(gate, state, crumbling);
		BlockState block = gate.getBlockState();
		state.facing = block.getValue(RailingGateBlock.FACING);
		long now = gate.getLevel() == null ? 0 : gate.getLevel().getGameTime();
		boolean open = block.getValue(RailingGateBlock.OPEN);
		if (gate.animStart == Long.MIN_VALUE) {
			gate.animStart = now - 100;
			gate.animOpen = open;
		}
		if (open != gate.animOpen) {
			gate.animOpen = open;
			gate.animStart = now;
		}
		float t = Math.min(1, Math.max(0, (now + partialTick - gate.animStart) / (open ? 7F : 11F)));
		t = open ? 1 - (1 - t) * (1 - t) : t * t;
		state.swing = open ? t : 1 - t;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		int light = state.lightCoords;
		float swing = state.swing;
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		pose.scale(1 / 16F, 1 / 16F, 1 / 16F);
		collector.submitCustomGeometry(pose, RENDER_TYPE, (p, consumer) -> {
			Geometry g = new Geometry(p, consumer);
			g.light = light;
			// About the hinge at x = 1.3, swinging the free end towards -z (the side it faces).
			g.push();
			g.translate(1.3, 0, 8);
			g.yaw(Math.toRadians(90 * swing));
			LineRenderer.box(g, 1.1, 14.4, -0.5, 12.5, 15.6, 0.5, StorageTankRenderer.YELLOW);
			LineRenderer.box(g, 1.1, 7.6, -0.4, 12.5, 8.6, 0.4, StorageTankRenderer.YELLOW);
			LineRenderer.box(g, 1.1, 1.8, -0.4, 12.5, 2.8, 0.4, StorageTankRenderer.YELLOW);
			LineRenderer.box(g, 1.1, 1.8, -0.5, 2.1, 15.6, 0.5, StorageTankRenderer.YELLOW);
			LineRenderer.box(g, 11.5, 1.8, -0.5, 12.5, 15.6, 0.5, StorageTankRenderer.YELLOW);
			// The hinge knuckles, and the tag hung on the middle rail.
			LineRenderer.box(g, -0.5, 3.0, -0.6, 1.1, 5.0, 0.6, StorageTankRenderer.GUNMETAL);
			LineRenderer.box(g, -0.5, 11.0, -0.6, 1.1, 13.0, 0.6, StorageTankRenderer.GUNMETAL);
			LineRenderer.panel(g, 5.0, 9.0, -0.75, 8.6, 12.6, -0.45, LineRenderer.NORTH, StorageTankRenderer.HAZARD_TAG, StorageTankRenderer.YELLOW);
			g.pop();
		});
		pose.popPose();
	}

	private static float yRotation(Direction facing) {
		return switch (facing) {
			case EAST -> 90;
			case SOUTH -> 180;
			case WEST -> 270;
			default -> 0;
		};
	}
}
