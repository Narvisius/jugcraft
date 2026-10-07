package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.BatJarBlock;
import io.github.jimbozoomer.jugcraft.agriculture.BeatingHeartJarBlock;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.HandJarBlock;
import io.github.jimbozoomer.jugcraft.agriculture.OddityJarBlock;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws what is in an Oddity Jar (models from decor17_quads.json, tools/decor17_data.py):
 * <ul>
 * <li>nine eyeballs, each rolling to follow the nearest player within {@value OddityJarBlock#WATCH_RANGE} blocks, a
 * little behind the others;</li>
 * <li>a heart on its brass stand that swells with each beat the block gives (and slowly turns);</li>
 * <li>a bat hanging asleep from the lid, or, awake, fluttering in the middle of the jar with its wings beating, always
 * inside the glass;</li>
 * <li>a coiled two-headed snake whose heads sway, their tongues flicking for a moment after the jar is used;</li>
 * <li>a floating hand drumming its fingers one after another, or, powered, pointing at the nearest player.</li>
 * </ul>
 */
public class OddityJarRenderer implements BlockEntityRenderer<DecorationBlockEntity, OddityJarRenderer.State> {
	private static final float[][] EYEBALLS = {{6.2F, 2.4F, 6.4F}, {9.6F, 2.5F, 6.6F}, {7.8F, 2.3F, 9.6F}, {6.6F, 4.8F, 8.6F}, {9.8F, 4.6F, 9.2F},
			{8.0F, 5.0F, 6.2F}, {6.4F, 7.2F, 6.8F}, {9.4F, 7.4F, 7.6F}, {7.8F, 7.6F, 9.8F}};
	private static final float[][] KNUCKLES = {{-1.5F, 1.6F, 0.0F}, {-0.5F, 1.6F, 0.0F}, {0.5F, 1.6F, 0.0F}, {1.5F, 1.6F, 0.0F}};
	private static final float EYE_TURN = 6.0F;
	/**
	 * The bat, in its jar's glass (2.2 to 13.8 pixels across): drawn at {@value #BAT_SCALE}; awake it circles
	 * {@value #BAT_ORBIT} pixels round the middle at {@value #BAT_FLY_Y} pixels up, bobbing {@value #BAT_BOB}; asleep it hangs
	 * at {@value #BAT_HANG_Y}. tools/decor17.py JARS bat_in_a_jar holds the same numbers, and the audit checks that its
	 * wings never leave the glass.
	 */
	private static final float BAT_SCALE = 0.9F;
	private static final float BAT_ORBIT = 0.2F;
	private static final float BAT_FLY_Y = 7.2F;
	private static final float BAT_BOB = 1.0F;
	private static final float BAT_HANG_Y = 11.2F;
	private static final float MAX_PITCH = 50.0F;
	/**
	 * The heart: its middle in the jar (pixels), how much more it swells at the peak of a beat and the ticks a beat takes
	 * to settle. tools/decor17.py JARS beating_heart_jar holds the middle and swell ("heart", "swell"), and the audit
	 * checks that the heart, at its fullest and turned any way, stays inside the glass. The Giant's Beating Heart draws it
	 * the same.
	 */
	static final float[] HEART = {8.0F, 6.4F, 8.0F};
	static final float HEART_SWELL = 0.14F;
	static final float HEART_DECAY_TICKS = 7.0F;
	/** Each jar's eyes as this client last left them: {yaw, pitch} per eye, then the game time. */
	private final Map<DecorationBlockEntity, double[]> eyes = new WeakHashMap<>();
	/** When each heart's last beat began, as this client saw it: {beat was on, game time}. */
	private final Map<DecorationBlockEntity, double[]> beats = new WeakHashMap<>();

	public static final class State extends BlockEntityRenderState {
		OddityJarBlock.Kind kind = OddityJarBlock.Kind.EYEBALLS;
		float time;
		float[] eyeYaw = new float[9];
		float[] eyePitch = new float[9];
		float pulse;
		boolean awake;
		boolean flicking;
		boolean pointing;
		float pointYaw;
		int seed;
	}

	public OddityJarRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DecorationBlockEntity jar, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(jar, state, crumbling);
		BlockState block = jar.getBlockState();
		Level level = jar.getLevel();
		if (!(block.getBlock() instanceof OddityJarBlock oddity) || level == null) {
			return;
		}
		state.kind = oddity.kind();
		double now = level.getGameTime() + (double) partialTick;
		state.time = (float) (now % 24000.0);
		state.seed = jar.getBlockPos().hashCode();
		BlockPos pos = jar.getBlockPos();
		switch (state.kind) {
			case EYEBALLS -> watch(jar, level, pos, now, partialTick, state);
			case HEART -> {
				boolean beat = block.hasProperty(BeatingHeartJarBlock.BEAT) && block.getValue(BeatingHeartJarBlock.BEAT);
				double[] seen = beats.computeIfAbsent(jar, j -> new double[] {0.0, -100.0});
				if (beat && seen[0] == 0.0) {
					seen[1] = now;
				}
				seen[0] = beat ? 1.0 : 0.0;
				state.pulse = pulse(now - seen[1]);
			}
			case BAT -> state.awake = block.hasProperty(BatJarBlock.AWAKE) && block.getValue(BatJarBlock.AWAKE);
			case SNAKE -> state.flicking = now - jar.marked() < OddityJarBlock.FLICK_TICKS;
			case HAND -> {
				state.pointing = block.hasProperty(HandJarBlock.POWERED) && block.getValue(HandJarBlock.POWERED);
				Vec3 nearest = nearest(level, Vec3.atCenterOf(pos), partialTick);
				if (state.pointing && nearest != null) {
					Vec3 to = nearest.subtract(Vec3.atCenterOf(pos));
					state.pointYaw = (float) Math.toDegrees(Math.atan2(-to.x, -to.z));
				}
			}
		}
	}

	private static @Nullable Vec3 nearest(Level level, Vec3 from, float partialTick) {
		Vec3 best = null;
		double range = OddityJarBlock.WATCH_RANGE * OddityJarBlock.WATCH_RANGE;
		for (Player player : level.players()) {
			Vec3 eyes = player.getEyePosition(partialTick);
			double d = eyes.distanceToSqr(from);
			if (!player.isSpectator() && d <= range) {
				range = d;
				best = eyes;
			}
		}
		return best;
	}

	/** Each eye turns toward the nearest player (or wanders), at most {@value #EYE_TURN} degrees a tick, each its own way. */
	private void watch(DecorationBlockEntity jar, Level level, BlockPos pos, double now, float partialTick, State state) {
		Vec3 target = nearest(level, Vec3.atCenterOf(pos), partialTick);
		double[] look = eyes.computeIfAbsent(jar, j -> new double[EYEBALLS.length * 2 + 1]);
		double elapsed = look[EYEBALLS.length * 2] == 0 ? 0 : Math.max(0.0, Math.min(20.0, now - look[EYEBALLS.length * 2]));
		for (int i = 0; i < EYEBALLS.length; i++) {
			float[] e = EYEBALLS[i];
			Vec3 eye = new Vec3(pos.getX() + e[0] / 16, pos.getY() + e[1] / 16, pos.getZ() + e[2] / 16);
			float yaw;
			float pitch;
			if (target != null) {
				Vec3 to = target.subtract(eye);
				yaw = (float) Math.toDegrees(Math.atan2(-to.x, -to.z));
				pitch = Mth.clamp((float) Math.toDegrees(Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z))), -MAX_PITCH, MAX_PITCH);
			} else {
				yaw = 70.0F * Mth.sin((float) now * 0.013F + i * 1.9F);
				pitch = 20.0F * Mth.sin((float) now * 0.021F + i * 0.7F);
			}
			// The later eyes in the jar follow more slowly, so they don't all turn as one.
			float step = (EYE_TURN - i * 0.4F) * (float) elapsed;
			double oldYaw = look[i * 2];
			double oldPitch = look[i * 2 + 1];
			look[i * 2] = oldYaw + Mth.clamp(Mth.wrapDegrees(yaw - (float) oldYaw), -step, step);
			look[i * 2 + 1] = oldPitch + Mth.clamp(pitch - (float) oldPitch, -step, step);
			state.eyeYaw[i] = (float) look[i * 2];
			state.eyePitch[i] = (float) look[i * 2 + 1];
		}
		look[EYEBALLS.length * 2] = now;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		switch (state.kind) {
			case EYEBALLS -> eyeballs(state, pose, collector);
			case HEART -> heart(state, pose, collector);
			case BAT -> bat(state, pose, collector);
			case SNAKE -> snake(state, pose, collector);
			case HAND -> hand(state, pose, collector);
		}
	}

	private static void eyeballs(State state, PoseStack pose, SubmitNodeCollector collector) {
		QuadModel eyeball = DecorQuads.get("oddity_eyeball");
		if (eyeball == null) {
			return;
		}
		for (int i = 0; i < EYEBALLS.length; i++) {
			float[] e = EYEBALLS[i];
			pose.pushPose();
			pose.translate(e[0] / 16, e[1] / 16, e[2] / 16);
			pose.rotateDegrees(Axis.YP, state.eyeYaw[i]);
			pose.rotateDegrees(Axis.XP, state.eyePitch[i]);
			eyeball.submit(pose, collector, state.lightCoords);
			pose.popPose();
		}
	}

	private static void heart(State state, PoseStack pose, SubmitNodeCollector collector) {
		heart(pose, collector, state.time, state.pulse, state.lightCoords);
	}

	/** How far through its swell the heart is, {@code ticks} after its beat began: 1 at the beat, settling to 0. */
	static float pulse(double ticks) {
		return (float) Math.max(0.0, 1.0 - ticks / HEART_DECAY_TICKS);
	}

	/**
	 * The heart on its brass stand, in a jar's block ({@code pose} at its corner): turning slowly with {@code time} (ticks)
	 * and swelling with {@code pulse} ({@link #pulse}). The Giant's Beating Heart draws this in a pose scaled up with it.
	 */
	static void heart(PoseStack pose, SubmitNodeCollector collector, float time, float pulse, int light) {
		QuadModel stand = DecorQuads.get("oddity_heart_stand");
		QuadModel heart = DecorQuads.get("oddity_heart");
		if (stand != null) {
			stand.submit(pose, collector, light);
		}
		if (heart != null) {
			float swell = 1.0F + HEART_SWELL * pulse * pulse;
			pose.pushPose();
			pose.translate(HEART[0] / 16, HEART[1] / 16, HEART[2] / 16);
			pose.rotateDegrees(Axis.YP, time * 0.6F % 360.0F);
			pose.scale(swell, swell * 0.96F + 0.04F, swell);
			heart.submit(pose, collector, light);
			pose.popPose();
		}
	}

	private static void bat(State state, PoseStack pose, SubmitNodeCollector collector) {
		QuadModel body = DecorQuads.get("oddity_bat_body");
		QuadModel left = DecorQuads.get("oddity_bat_wing_left");
		QuadModel right = DecorQuads.get("oddity_bat_wing_right");
		pose.pushPose();
		float wing;
		if (state.awake) {
			// Round and round the middle of the jar, bobbing, head up, wings beating fast.
			float a = state.time * 0.35F;
			pose.translate(0.5F + BAT_ORBIT / 16 * Mth.cos(a), (BAT_FLY_Y + BAT_BOB * Mth.sin(state.time * 0.5F)) / 16,
					0.5F + BAT_ORBIT / 16 * Mth.sin(a));
			pose.rotateDegrees(Axis.YP, (float) Math.toDegrees(-a) + 180.0F);
			pose.rotateDegrees(Axis.ZP, 180.0F);
			wing = 55.0F * Mth.sin(state.time * 1.6F);
		} else {
			// Hanging by its feet from under the lid, head down, wings wrapped round it.
			pose.translate(0.5F, BAT_HANG_Y / 16, 0.5F);
			pose.rotateDegrees(Axis.YP, 20.0F * Mth.sin(state.time * 0.02F + state.seed));
			wing = 80.0F;
		}
		pose.scale(BAT_SCALE, BAT_SCALE, BAT_SCALE);
		if (body != null) {
			body.submit(pose, collector, state.lightCoords);
		}
		if (left != null) {
			pose.pushPose();
			pose.translate(-1.2F / 16, 0.0F, 0.0F);
			pose.rotateDegrees(Axis.YP, -wing);
			left.submit(pose, collector, state.lightCoords);
			pose.popPose();
		}
		if (right != null) {
			pose.pushPose();
			pose.translate(1.2F / 16, 0.0F, 0.0F);
			pose.rotateDegrees(Axis.YP, wing);
			right.submit(pose, collector, state.lightCoords);
			pose.popPose();
		}
		pose.popPose();
	}

	private static void snake(State state, PoseStack pose, SubmitNodeCollector collector) {
		QuadModel coil = DecorQuads.get("oddity_snake_coil");
		QuadModel head = DecorQuads.get("oddity_snake_head");
		QuadModel tongue = DecorQuads.get("oddity_snake_tongue");
		if (coil != null) {
			coil.submit(pose, collector, state.lightCoords);
		}
		boolean showTongue = state.flicking && ((int) state.time / 2) % 2 == 0;
		for (int h = 0; h < 2; h++) {
			pose.pushPose();
			pose.translate((7.0F + h * 2.0F) / 16, 7.9F / 16, 8.0F / 16);
			pose.rotateDegrees(Axis.YP, 35.0F * Mth.sin(state.time * 0.03F + h * 1.7F) + (h == 0 ? 20.0F : -20.0F));
			pose.rotateDegrees(Axis.XP, 10.0F * Mth.sin(state.time * 0.05F + h));
			if (head != null) {
				head.submit(pose, collector, state.lightCoords);
			}
			if (showTongue && tongue != null) {
				tongue.submit(pose, collector, state.lightCoords);
			}
			pose.popPose();
		}
	}

	private static void hand(State state, PoseStack pose, SubmitNodeCollector collector) {
		QuadModel palm = DecorQuads.get("oddity_hand_palm");
		QuadModel finger = DecorQuads.get("oddity_hand_finger");
		pose.pushPose();
		pose.translate(0.5F, (5.6F + 0.3F * Mth.sin(state.time * 0.06F)) / 16, 0.5F);
		if (state.pointing) {
			pose.rotateDegrees(Axis.YP, state.pointYaw);
			pose.rotateDegrees(Axis.XP, -60.0F);
		} else {
			pose.rotateDegrees(Axis.YP, 25.0F * Mth.sin(state.time * 0.015F + state.seed));
		}
		if (palm != null) {
			palm.submit(pose, collector, state.lightCoords);
		}
		if (finger != null) {
			for (int f = 0; f < KNUCKLES.length; f++) {
				float bend;
				if (state.pointing) {
					// The index finger (the first) straight out, the others curled into the palm.
					bend = f == 0 ? 0.0F : 80.0F;
				} else {
					// Drumming: each finger taps in turn.
					bend = 35.0F * Math.max(0.0F, Mth.sin(state.time * 0.45F - f * 0.9F));
				}
				pose.pushPose();
				pose.translate(KNUCKLES[f][0] / 16, KNUCKLES[f][1] / 16, KNUCKLES[f][2] / 16);
				pose.rotateDegrees(Axis.XP, -bend);
				finger.submit(pose, collector, state.lightCoords);
				pose.popPose();
			}
		}
		pose.popPose();
	}
}
