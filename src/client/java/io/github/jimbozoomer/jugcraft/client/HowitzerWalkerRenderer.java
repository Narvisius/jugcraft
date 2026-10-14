package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.walker.HowitzerWalker;
import io.github.jimbozoomer.jugcraft.walker.WalkerLeap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/**
 * Draws the Howitzer Walker from the parts tools/howitzer_walker.py exports (assets/jugcraft/howitzer_walker_quads.json),
 * posed by the same curves its Blockbench clips are sampled from:
 * <ul>
 * <li><b>walk</b>, from the stride: each leg swings at its hip and tucks at its knee; the hull bounces twice a stride,
 * stretching as it rises and squashing as it lands, sways and nods, and the gun wobbles on its trunnion a beat behind;</li>
 * <li><b>shoot</b>, from the ticks since it fired: the gun slams back along its barrel and runs out with a wobble, the
 * hull rocks back and squats, a cartoon star burst pops and spins at the muzzle and smoke puffs roll away;</li>
 * <li><b>jump</b>, from {@link WalkerLeap}: a crouch, a stretch off the ground, the legs tucked in the air and a squashy,
 * bouncy landing. The walker itself rises and falls, so the clip's own rise is left out.</li>
 * </ul>
 * The gun rests at its synced elevation (25 degrees up, or where laying has put it) rather than the clip's fixed one.
 */
public class HowitzerWalkerRenderer extends EntityRenderer<HowitzerWalker, HowitzerWalkerRenderer.State> {
	/** Joints in pixels: keep in sync with HIPS, TRUNNION, KNEE and BARREL_END in tools/howitzer_walker.py. */
	private static final float[][] HIPS = {{11, 42, 0}, {-11, 42, 0}};
	private static final float[] TRUNNION = {-7, 70, 4};
	private static final float[] KNEE = {0, -20.33F, -8.42F};
	private static final float[] BARREL_END = {0, 1.5F, 36};
	/** The curves' numbers: keep in sync with tools/howitzer_walker.py. */
	private static final float STRIDE_RATE = 2.0F;
	private static final float LEG_SWING = 24.0F;
	private static final float KNEE_BEND = 20.0F;
	private static final float BOB = 2.5F;
	private static final float SQUASH = 0.06F;
	private static final float ROLL = 3.0F;
	private static final float PITCH = 2.0F;
	private static final float GUN_WOBBLE = 6.0F;
	private static final float RECOIL_STROKE = 6.0F;
	private static final float RECOIL_TICKS = 14.0F;
	private static final float KICK = 6.0F;
	private static final float SQUAT = 2.0F;
	private static final float FLASH_TICKS = 8.0F;
	private static final float PUFF_TICKS = 24.0F;
	private static final int FULL_BRIGHT = 0xF000F0;

	public static final class State extends EntityRenderState {
		float yRot;
		float stride;
		float strideWeight;
		float elevation;
		float sinceShot;
		float jump;
	}

	public HowitzerWalkerRenderer(EntityRendererProvider.Context context) {
		super(context);
		shadowRadius = 1.5F;
	}

	/** The barrel, the burst and the rolling smoke reach well past the hitbox. */
	@Override
	protected AABB getBoundingBoxForCulling(HowitzerWalker walker, float partialTick) {
		return walker.getBoundingBox().inflate(4.0, 2.0, 4.0);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(HowitzerWalker walker, State state, float partialTick) {
		super.extractRenderState(walker, state, partialTick);
		state.yRot = walker.getYRot(partialTick);
		state.stride = walker.stride(partialTick);
		state.strideWeight = walker.strideWeight(partialTick);
		state.elevation = walker.elevation(partialTick);
		state.sinceShot = walker.sinceShot(partialTick);
		state.jump = walker.jumpTime(partialTick);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		int light = state.lightCoords;
		float t = state.sinceShot;
		// The jump, when it plays: {drop px, stretch, hull pitch, gun pitch, thigh, shin}. The walk gives way to it.
		float[] jump = state.jump >= 0 ? jumpPose(state.jump) : new float[6];
		float gait = state.strideWeight * (1.0F - WalkerLeap.weight(state.jump));
		float phase = state.stride * STRIDE_RATE;
		float bob = BOB * gait * (0.5F - 0.5F * Mth.cos(2 * phase));
		float stretch = SQUASH * gait * Mth.sin(2 * phase);
		float roll = ROLL * gait * Mth.sin(phase);
		float nod = PITCH * gait * Mth.sin(2 * phase + 0.4F);
		float wobble = GUN_WOBBLE * gait * Mth.sin(2 * phase + 1.0F);
		float drop = jump[0];

		pose.pushPose();
		pose.rotateDegrees(Axis.YP, -state.yRot);
		// The hull and everything on it, about the ground under its middle.
		pose.pushPose();
		pose.translate(0, (bob - squat(t) - drop) / 16.0F, 0);
		pose.rotateDegrees(Axis.XP, nod + kick(t) + jump[2]);
		pose.rotateDegrees(Axis.ZP, roll);
		float across = (1 - stretch / 2) * (1 - jump[1] / 2);
		pose.scale(across, (1 + stretch) * (1 + jump[1]), across);
		draw("howitzer_walker_hull", pose, collector, light);
		draw("howitzer_walker_lamps", pose, collector, FULL_BRIGHT);
		// The gun, from its trunnion: at its elevation, wobbling with the walk, recoiling along the barrel.
		pose.pushPose();
		at(pose, TRUNNION);
		pose.translate(0, -bob * 0.3F / 16.0F, 0);
		pose.rotateDegrees(Axis.XP, -state.elevation + wobble + jump[3]);
		pose.translate(0, 0, -recoil(t) * RECOIL_STROKE / 16.0F);
		draw("howitzer_walker_gun", pose, collector, light);
		float burst = flashScale(t);
		if (burst > 0) {
			pose.pushPose();
			at(pose, BARREL_END);
			pose.rotateDegrees(Axis.ZP, 60.0F * t / FLASH_TICKS);
			pose.scale(burst, burst, burst);
			draw("howitzer_walker_flash", pose, collector, FULL_BRIGHT);
			pose.popPose();
		}
		float puff = puffScale(t);
		if (puff > 0) {
			pose.pushPose();
			at(pose, BARREL_END);
			pose.translate(0, 0.5F * t / 16.0F, (6 + 1.4F * t) / 16.0F);
			pose.scale(puff, puff, puff);
			draw("howitzer_walker_puff", pose, collector, FULL_BRIGHT);
			pose.popPose();
		}
		pose.popPose();
		pose.popPose();
		// The legs, each from its hip: the thigh swings, the shin tucks at the knee.
		for (int i = 0; i < HIPS.length; i++) {
			float p = phase + (i == 0 ? 0 : Mth.PI);
			pose.pushPose();
			at(pose, HIPS[i]);
			pose.translate(0, -drop / 16.0F, 0);
			pose.rotateDegrees(Axis.XP, LEG_SWING * Mth.sin(p) * gait + jump[4]);
			draw("howitzer_walker_thigh", pose, collector, light);
			at(pose, KNEE);
			pose.rotateDegrees(Axis.XP, KNEE_BEND * gait * Math.max(0.0F, Mth.sin(p + 0.7F)) + jump[5]);
			draw("howitzer_walker_shin", pose, collector, light);
			pose.popPose();
		}
		pose.popPose();
		super.submit(state, pose, collector, camera);
	}

	/** How far back the gun is, as a share of its stroke: a slam back in two ticks, then a run out past rest and a wobble. */
	static float recoil(float t) {
		if (t < 0 || t > 40) {
			return 0.0F;
		}
		if (t < 2) {
			return t / 2;
		}
		float u = (t - 2) / (RECOIL_TICKS - 2);
		return (float) (Math.exp(-u * 3) * Math.cos(u * 4.5));
	}

	/** The hull's pitch after firing, degrees: it rocks back, then bounces to rest. */
	static float kick(float t) {
		return t >= 0 && t < 40 ? (float) (KICK * Math.exp(-t / 8) * Math.cos(t * 0.45)) : 0.0F;
	}

	static float squat(float t) {
		return t >= 0 && t < 12 ? SQUAT * Mth.sin(Mth.PI * t / 12) : 0.0F;
	}

	/** The star burst pops up in two ticks and shrinks away by FLASH_TICKS. */
	static float flashScale(float t) {
		if (t < 0 || t >= FLASH_TICKS) {
			return 0.0F;
		}
		return 1.3F * (t < 2 ? t / 2 : 1 - (t - 2) / (FLASH_TICKS - 2));
	}

	/** The puffs grow quickly, drift, and shrink away in their last quarter. */
	static float puffScale(float t) {
		if (t < 0 || t >= PUFF_TICKS) {
			return 0.0F;
		}
		float u = t / PUFF_TICKS;
		float grow = 0.3F + 1.3F * (1 - (1 - u) * (1 - u));
		return grow * (1 - Math.max(0.0F, (u - 0.75F) / 0.25F));
	}

	/**
	 * The jump at clip tick t: {drop px, stretch, hull pitch, gun pitch, thigh, shin}, jump_pose in tools/howitzer_walker.py
	 * without its rise (the walker really leaves the ground).
	 */
	static float[] jumpPose(float t) {
		if (t < 6) {
			float c = smooth(t / 6);
			return new float[] {6 * c, -0.10F * c, 3 * c, -4 * c, 15 * c, 25 * c};
		}
		if (t < 9) {
			float u = (t - 6) / 3;
			return new float[] {6 * (1 - u), 0.12F * u, -8 * u, 8 * u, 15 - 21 * u, 25 - 33 * u};
		}
		if (t < 20) {
			float u = (t - 9) / 11;
			float tuck = smooth(u * 2.5F) * (1 - smooth((u - 0.7F) / 0.3F));
			return new float[] {0, 0.04F * (1 - Math.abs(2 * u - 1)), -8 + 14 * u, 8 - 14 * u, -6 + 14 * tuck, -8 + 24 * tuck};
		}
		if (t < 24) {
			float u = Mth.sin(Mth.PI * (t - 20) / 4);
			return new float[] {8 * u, -0.15F * u, -4 * u, -6 * u, 20 * u, 32 * u};
		}
		float u = (t - 24) / (WalkerLeap.END - 24);
		float w = (float) (Math.exp(-u * 3) * Math.cos(u * 6) * (1 - u));
		return new float[] {2 * w, 0.05F * w, 2 * w, -3 * w, 5 * w, 8 * w};
	}

	private static float smooth(float u) {
		u = Mth.clamp(u, 0.0F, 1.0F);
		return u * u * (3 - 2 * u);
	}

	private static void at(PoseStack pose, float[] pixels) {
		pose.translate(pixels[0] / 16.0F, pixels[1] / 16.0F, pixels[2] / 16.0F);
	}

	private static void draw(String part, PoseStack pose, SubmitNodeCollector collector, int light) {
		QuadModel model = DecorQuads.get(part);
		if (model != null) {
			model.submit(pose, collector, light);
		}
	}
}
