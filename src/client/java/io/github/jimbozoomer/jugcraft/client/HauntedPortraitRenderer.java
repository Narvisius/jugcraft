package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.HauntedPortraitBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HauntedPortraitBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a Haunted Portrait's pupils so that they follow whoever is looking at it. Each pupil is a pixel square inside
 * its painted eye white ({@link HauntedPortraitBlock.Portrait#eyes}), shifted toward the camera as far as the eye
 * allows. At night the pupils glow red. Only this client's camera is used: nothing is sent to the server.
 */
public class HauntedPortraitRenderer implements BlockEntityRenderer<HauntedPortraitBlockEntity, HauntedPortraitRenderer.State> {
	private static final RenderType PUPIL = RenderTypes.entityCutout(Jugcraft.id("textures/entity/portrait_pupil.png"));
	/** The painted canvas lies this far behind the block's centre, toward the wall; pupils sit just in front of it. */
	private static final float CANVAS = -7.0F / 16 + 0.004F;
	/** How far (in pixels) the pupils lean per unit of the camera's direction, before the eye's edge stops them. */
	private static final float LEAN = 1.6F;
	private static final int FULL_BRIGHT = 0xF000F0;

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		HauntedPortraitBlock.@Nullable Portrait portrait;
		float lookX;
		float lookY;
		boolean night;
	}

	public HauntedPortraitRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(HauntedPortraitBlockEntity entity, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(entity, state, crumbling);
		BlockState block = entity.getBlockState();
		state.portrait = null;
		if (!(block.getBlock() instanceof HauntedPortraitBlock)) {
			return;
		}
		Direction facing = block.getValue(HauntedPortraitBlock.FACING);
		state.facing = facing;
		state.portrait = block.getValue(HauntedPortraitBlock.PORTRAIT);
		Vec3 face = Vec3.atCenterOf(entity.getBlockPos()).add(facing.getStepX() * CANVAS, 0.0, facing.getStepZ() * CANVAS);
		Vec3 look = camera.subtract(face);
		double distance = Math.max(1.0E-3, look.length());
		Direction right = facing.getCounterClockWise();
		state.lookX = (float) ((look.x * right.getStepX() + look.z * right.getStepZ()) / distance);
		state.lookY = (float) (look.y / distance);
		Level level = entity.getLevel();
		long hour = level == null ? 6000 : Math.floorMod(level.getOverworldClockTime(), 24000L);
		state.night = hour >= 13000 && hour < 23000;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		HauntedPortraitBlock.Portrait portrait = state.portrait;
		if (portrait == null) {
			return;
		}
		pose.pushPose();
		pose.translate(0.5F, 0.5F, 0.5F);
		// Turn so that +x is the viewer's right and +z points out of the painting.
		pose.rotateDegrees(Axis.YP, -state.facing.toYRot());
		int light = state.night ? FULL_BRIGHT : state.lightCoords;
		int color = state.night ? 0xFFFF2A18 : 0xFF140E0A;
		collector.submitCustomGeometry(pose, PUPIL, (matrix, buffer) -> {
			for (int[] eye : portrait.eyes) {
				float maxX = (eye[2] - 1) / 2.0F;
				float maxY = (eye[3] - 1) / 2.0F;
				float u = eye[0] + eye[2] / 2.0F + Math.max(-maxX, Math.min(maxX, state.lookX * LEAN));
				float v = eye[1] + eye[3] / 2.0F + Math.max(-maxY, Math.min(maxY, -state.lookY * LEAN));
				pupil(buffer, matrix, light, color, (u - 8.0F) / 16, (8.0F - v) / 16);
			}
		});
		pose.popPose();
	}

	/** A pupil one pixel square centred on (x, y) in the painting's plane. */
	private static void pupil(VertexConsumer buffer, PoseStack.Pose matrix, int light, int color, float x, float y) {
		float h = 0.5F / 16;
		vertex(buffer, matrix, light, color, x - h, y - h, 0.0F, 1.0F);
		vertex(buffer, matrix, light, color, x + h, y - h, 1.0F, 1.0F);
		vertex(buffer, matrix, light, color, x + h, y + h, 1.0F, 0.0F);
		vertex(buffer, matrix, light, color, x - h, y + h, 0.0F, 0.0F);
	}

	private static void vertex(VertexConsumer buffer, PoseStack.Pose matrix, int light, int color, float x, float y, float u, float v) {
		buffer.addVertex(matrix, x, y, CANVAS).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
				.setNormal(matrix, 0.0F, 0.0F, 1.0F);
	}
}
