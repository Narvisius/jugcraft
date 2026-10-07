package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.client.DroneDepotRenderer.Geometry;
import io.github.jimbozoomer.jugcraft.nuclear.StorageTankHatchBlock;
import io.github.jimbozoomer.jugcraft.nuclear.StorageTankHatchBlockEntity;
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
 * A tank roof hatch's lid, on its two hinges at the back of the coaming: it swings up and over (a little past upright,
 * resting back on its stops) as it opens, and drops shut. A white plate with a turned-down lip, its gasket underneath,
 * the hinge straps across it, a yellow lifting handle and the quick-release dog at the front.
 */
public class StorageTankHatchRenderer implements BlockEntityRenderer<StorageTankHatchBlockEntity, StorageTankHatchRenderer.State> {
	private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(StorageTankRenderer.TEXTURE);
	/** How far the lid opens (degrees). */
	private static final float OPEN_ANGLE = 108;

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		float swing;
	}

	public StorageTankHatchRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(StorageTankHatchBlockEntity hatch, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(hatch, state, crumbling);
		BlockState block = hatch.getBlockState();
		state.facing = block.getValue(StorageTankHatchBlock.FACING);
		long now = hatch.getLevel() == null ? 0 : hatch.getLevel().getGameTime();
		boolean open = block.getValue(StorageTankHatchBlock.OPEN);
		if (hatch.animStart == Long.MIN_VALUE) {
			hatch.animStart = now - 100;
			hatch.animOpen = open;
		}
		if (open != hatch.animOpen) {
			hatch.animOpen = open;
			hatch.animStart = now;
		}
		// Lifted steadily open; it falls shut, quicker at the end.
		float t = Math.min(1, Math.max(0, (now + partialTick - hatch.animStart) / (open ? 14F : 9F)));
		state.swing = open ? t * t * (3 - 2 * t) : 1 - t * t;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		int light = state.lightCoords;
		float angle = state.swing * OPEN_ANGLE;
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		pose.scale(1 / 16F, 1 / 16F, 1 / 16F);
		collector.submitCustomGeometry(pose, RENDER_TYPE, (p, consumer) -> {
			Geometry g = new Geometry(p, consumer);
			g.light = light;
			// About the hinge pins at the back (z = 2, just over the coaming's top), the front coming up and over.
			g.push();
			g.translate(0, 19.4, 2.0);
			g.pitch(Math.toRadians(-angle));
			// The lid plate, its lip turned down round the edge, the gasket on its underside.
			LineRenderer.box(g, 2.0, -0.4, 0, 14.0, 0.4, 12.0, StorageTankRenderer.WHITE);
			LineRenderer.box(g, 2.0, -1.3, 0, 2.5, -0.4, 12.0, StorageTankRenderer.WHITE);
			LineRenderer.box(g, 13.5, -1.3, 0, 14.0, -0.4, 12.0, StorageTankRenderer.WHITE);
			LineRenderer.box(g, 2.5, -1.3, 11.5, 13.5, -0.4, 12.0, StorageTankRenderer.WHITE);
			LineRenderer.box(g, 3.0, -0.7, 1.0, 13.0, -0.4, 11.0, StorageTankRenderer.GASKET);
			// The hinge straps and their knuckles round the pins.
			for (double x : new double[] {4.0, 10.5}) {
				LineRenderer.box(g, x, 0.4, -0.2, x + 1.5, 0.75, 5.0, StorageTankRenderer.GUNMETAL);
				LineRenderer.box(g, x, -0.6, -0.9, x + 1.5, 0.6, 0.3, StorageTankRenderer.GUNMETAL);
			}
			// The lifting handle (legs and grip) and the quick-release dog at the front.
			LineRenderer.box(g, 6.0, 0.4, 9.6, 6.6, 1.8, 10.2, StorageTankRenderer.YELLOW);
			LineRenderer.box(g, 9.4, 0.4, 9.6, 10.0, 1.8, 10.2, StorageTankRenderer.YELLOW);
			LineRenderer.box(g, 6.0, 1.8, 9.6, 10.0, 2.4, 10.2, StorageTankRenderer.YELLOW);
			LineRenderer.box(g, 7.3, -2.0, 11.9, 8.7, 0.8, 12.7, StorageTankRenderer.GUNMETAL);
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
