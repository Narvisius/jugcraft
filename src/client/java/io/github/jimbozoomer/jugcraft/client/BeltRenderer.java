package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.kinetic.BeltPulleyBlockEntity;
import io.github.jimbozoomer.jugcraft.kinetic.ShaftBlock;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * Draws a drive belt between two linked belt pulleys: the upper and lower runs, from groove to groove.
 * Only one pulley of each pair draws it (the one with the smaller position), so it is not drawn twice.
 */
public class BeltRenderer implements BlockEntityRenderer<BeltPulleyBlockEntity, BeltRenderer.State> {
	private static final RenderType BELT = RenderTypes.entitySolid(Jugcraft.id("textures/block/belt.png"));
	/** Distance of the belt from the pulley's axis (the groove radius), and the belt's width along the axis. */
	private static final float RADIUS = 6.1F / 16;
	private static final float WIDTH = 3.5F / 16;

	public static final class State extends BlockEntityRenderState {
		@Nullable BlockPos offset;
		Direction.Axis axis = Direction.Axis.Y;
	}

	public BeltRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(BeltPulleyBlockEntity pulley, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(pulley, state, crumbling);
		BlockPos link = pulley.link();
		BlockPos pos = pulley.getBlockPos();
		state.offset = link != null && pos.asLong() < link.asLong() ? link.subtract(pos) : null;
		state.axis = pulley.getBlockState().getValue(ShaftBlock.AXIS);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.offset == null) {
			return;
		}
		Vector3f start = new Vector3f(0.5F, 0.5F, 0.5F);
		Vector3f length = new Vector3f(state.offset.getX(), state.offset.getY(), state.offset.getZ());
		Vector3f axis = new Vector3f(state.axis == Direction.Axis.X ? 1 : 0, state.axis == Direction.Axis.Y ? 1 : 0,
				state.axis == Direction.Axis.Z ? 1 : 0);
		// The belt runs on both sides of the line between the pulleys, in the plane square to the axis.
		Vector3f side = new Vector3f(axis).cross(length).normalize();
		Vector3f across = new Vector3f(axis).mul(WIDTH / 2);
		int light = state.lightCoords;
		collector.submitCustomGeometry(pose, BELT, (matrix, buffer) -> {
			for (int run = -1; run <= 1; run += 2) {
				Vector3f a = new Vector3f(side).mul(RADIUS * run).add(start);
				Vector3f b = new Vector3f(a).add(length);
				Vector3f normal = new Vector3f(side).mul(run);
				strip(buffer, matrix, light, a, b, across, normal);
				strip(buffer, matrix, light, b, a, across, normal.negate());
			}
		});
	}

	/** One face of a belt run from {@code a} to {@code b}, {@code across} wide on each side of the centre line. */
	private static void strip(VertexConsumer buffer, PoseStack.Pose matrix, int light, Vector3f a, Vector3f b, Vector3f across,
			Vector3f normal) {
		vertex(buffer, matrix, light, normal, new Vector3f(a).sub(across), 0, 0);
		vertex(buffer, matrix, light, normal, new Vector3f(a).add(across), 1, 0);
		vertex(buffer, matrix, light, normal, new Vector3f(b).add(across), 1, 1);
		vertex(buffer, matrix, light, normal, new Vector3f(b).sub(across), 0, 1);
	}

	private static void vertex(VertexConsumer buffer, PoseStack.Pose matrix, int light, Vector3f normal, Vector3f at, float u, float v) {
		buffer.addVertex(matrix, at.x(), at.y(), at.z()).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light).setNormal(matrix, normal.x(), normal.y(), normal.z());
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 96;
	}
}
