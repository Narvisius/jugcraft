package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.client.DroneDepotRenderer.Geometry;
import io.github.jimbozoomer.jugcraft.nuclear.LockerBlockEntity;
import io.github.jimbozoomer.jugcraft.nuclear.StackingFurnitureBlock;
import io.github.jimbozoomer.jugcraft.nuclear.UniformItem;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Draws the coat hanging in a locker (the top block of a stack, where the rail is), the same coat you wear. */
public class LockerRenderer implements BlockEntityRenderer<LockerBlockEntity, LockerRenderer.State> {
	/** An empty coat on a hanger is drawn at three quarters of a wearer's size, so it fits the locker. */
	private static final float SCALE = 0.75F / 16F;

	public static final class State extends BlockEntityRenderState {
		UniformItem.@Nullable Kind kind;
		Direction facing = Direction.NORTH;
	}

	public LockerRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(LockerBlockEntity locker, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(locker, state, crumbling);
		BlockState block = locker.getBlockState();
		state.facing = block.getValue(HorizontalDirectionalBlock.FACING);
		boolean top = !block.getValue(StackingFurnitureBlock.UP);
		state.kind = top && locker.coat().getItem() instanceof UniformItem uniform ? uniform.kind : null;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		UniformItem.Kind kind = state.kind;
		if (kind == null) {
			return;
		}
		pose.pushPose();
		// The locker's model faces north with its open front to the south; turn as the block model is turned.
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -yRotation(state.facing));
		pose.translate(0.0F, 11.0F / 16F, 9.0F / 16F - 0.5F);    // the hook on the rail
		pose.scale(SCALE, SCALE, SCALE);
		// A locker's inside is dim; the coat is lit at least a little, so it reads (its own cloth is white).
		int light = LightCoordsUtil.pack(Math.max(LightCoordsUtil.block(state.lightCoords), 7), LightCoordsUtil.sky(state.lightCoords));
		collector.submitCustomGeometry(pose, UniformLayer.RENDER_TYPE, (p, consumer) -> {
			Geometry g = new Geometry(p, consumer);
			g.light = light;
			UniformLayer.hanging(g, kind);
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

	@Override
	public boolean shouldRenderOffScreen() {
		return true; // The coat hangs down into the block below.
	}
}
