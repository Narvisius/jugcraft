package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.MourningAngelBlock;
import io.github.jimbozoomer.jugcraft.agriculture.PorchWitchBlock;
import io.github.jimbozoomer.jugcraft.agriculture.PorchWitchBlockEntity;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the Porch Witch's stirring arm and her head. The arm swings about her shoulder so the spoon goes round the
 * pot ({@link PorchWitchBlock#stir}), fast while she cackles. Her head turns to the nearest player within
 * {@link PorchWitchBlock#WATCH_RANGE} blocks; cackling, she throws it back and it shakes, and her eyes glow green (at
 * night too). Coordinates are in pixels of the lower half, for a witch facing north.
 */
public class PorchWitchRenderer implements BlockEntityRenderer<PorchWitchBlockEntity, PorchWitchRenderer.State> {
	private static final RenderType EYES = RenderTypes.entityCutout(Jugcraft.id("textures/entity/porch_witch_eyes.png"));
	private static final int FULL_BRIGHT = 0xF000F0;
	private static final float SHOULDER_X = 12.5F;
	private static final float SHOULDER_Y = 20.5F;
	private static final float SHOULDER_Z = 11.5F;
	private static final float NECK_Y = 21.0F;
	private static final float NECK_Z = 12.0F;
	private static final float TURN_SPEED = 5.0F;
	/** Her eyes: {left x, right x, bottom y, front z}. */
	private static final float[] EYES_AT = {6.2F, 8.8F, 24.6F, 8.95F};
	/** Each witch's head as this client last left it: {yaw, game time}. */
	private final Map<PorchWitchBlockEntity, double[]> heads = new WeakHashMap<>();

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		boolean glow;
		float headYaw;
		float headPitch;
		float stirYaw;
		float stirPitch;
	}

	public PorchWitchRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(PorchWitchBlockEntity witch, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(witch, state, crumbling);
		BlockState block = witch.getBlockState();
		Level level = witch.getLevel();
		if (!(block.getBlock() instanceof PorchWitchBlock) || level == null) {
			return;
		}
		state.facing = block.getValue(PorchWitchBlock.FACING);
		boolean cackling = block.getValue(PorchWitchBlock.CACKLING);
		double now = level.getGameTime() + (double) partialTick;
		float time = (float) (now % 24000.0);
		float[] stir = PorchWitchBlock.stir(cackling, time);
		state.stirYaw = stir[0];
		state.stirPitch = stir[1];
		state.glow = cackling || MourningAngelBlock.night(level);
		state.headPitch = cackling ? 18.0F + 4.0F * Mth.sin(time * 1.3F) : 0.0F; // thrown back
		Vec3 head = Vec3.atBottomCenterOf(witch.getBlockPos()).add(0.0, (NECK_Y + 3.0F) / 16, 0.0);
		Vec3 nearest = null;
		double best = PorchWitchBlock.WATCH_RANGE * PorchWitchBlock.WATCH_RANGE;
		for (Player player : level.players()) {
			Vec3 eyes = player.getEyePosition(partialTick);
			double distance = eyes.distanceToSqr(head);
			if (!player.isSpectator() && distance <= best) {
				best = distance;
				nearest = eyes;
			}
		}
		float target = 0.0F;
		if (nearest != null) {
			float angle = (float) Math.toDegrees(Math.atan2(nearest.x - head.x, -(nearest.z - head.z)));
			target = Mth.clamp(Mth.wrapDegrees(angle - state.facing.toYRot() + 180.0F), -PorchWitchBlock.MAX_TURN, PorchWitchBlock.MAX_TURN);
		}
		float start = target;
		double[] look = heads.computeIfAbsent(witch, w -> new double[] {start, now});
		float elapsed = (float) Math.max(0.0, Math.min(20.0, now - look[1]));
		look[0] = (float) look[0] + Mth.clamp(target - (float) look[0], -TURN_SPEED * elapsed, TURN_SPEED * elapsed);
		look[1] = now;
		state.headYaw = (float) look[0];
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel arm = DecorQuads.get("porch_witch_arm");
		QuadModel head = DecorQuads.get("porch_witch_head");
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		if (arm != null) {
			pose.pushPose();
			pose.translate(SHOULDER_X / 16, SHOULDER_Y / 16, SHOULDER_Z / 16);
			pose.rotateDegrees(Axis.YP, state.stirYaw);
			pose.rotateDegrees(Axis.XP, state.stirPitch);
			pose.translate(-SHOULDER_X / 16, -SHOULDER_Y / 16, -SHOULDER_Z / 16);
			arm.submit(pose, collector, state.lightCoords);
			pose.popPose();
		}
		pose.translate(0.5F, NECK_Y / 16, NECK_Z / 16);
		pose.rotateDegrees(Axis.YP, -state.headYaw);
		pose.rotateDegrees(Axis.XP, state.headPitch);
		pose.translate(-0.5F, -NECK_Y / 16, -NECK_Z / 16);
		if (head != null) {
			head.submit(pose, collector, state.lightCoords);
		}
		if (state.glow) {
			collector.submitCustomGeometry(pose, EYES, (matrix, buffer) -> {
				float z = EYES_AT[3] / 16;
				float y0 = EYES_AT[2] / 16;
				float y1 = (EYES_AT[2] + 1.0F) / 16;
				for (float x : new float[] {EYES_AT[0], EYES_AT[1]}) {
					float x0 = x / 16;
					float x1 = (x + 1.0F) / 16;
					DecorDraw.quad(buffer, matrix, new float[][] {{x0, y0, z, 1, 1}, {x0, y1, z, 1, 0}, {x1, y1, z, 0, 0}, {x1, y0, z, 0, 1}},
							0, 0, -1, 0xFFFFFFFF, FULL_BRIGHT);
				}
			});
		}
		pose.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true; // She stands two blocks tall, her hat higher still.
	}
}
