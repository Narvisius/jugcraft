package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;

/**
 * Draws the Armoured Walker's parts (batch 58, the owner's model; tools/armoured_walker.py) for both its renderers:
 * the player's walker in blue-grey plate and the raiders' in their paint, which differ only in the parts' prefix.
 * The legs swing with its stride, the tool arm sways with them, the piston rams out and back, and the lamps glow.
 */
final class ArmouredWalkerParts {
	/** Joints in pixels: keep in sync with HIPS, SHOULDER, PISTON and PISTON_STROKE in tools/armoured_walker.py. */
	static final float[][] HIPS = {{10, 27, 0}, {-10, 27, 0}};
	static final float[] SHOULDER = {-24, 56, 1};
	static final float[] PISTON = {24, 49, 6};
	static final float PISTON_STROKE = 10;
	private static final float STRIDE_RATE = 2.0F;
	private static final float LEG_SWING = 24.0F;
	/** The ram: how many ticks the piston takes to strike and come back. */
	static final float RAM_TICKS = 10.0F;
	private static final int FULL_BRIGHT = 0xF000F0;

	private ArmouredWalkerParts() {
	}

	/** The ram's reach this frame, 0 to 1, from the ticks since it rammed. */
	static float ram(float sinceRam) {
		return sinceRam < RAM_TICKS ? Mth.sin(sinceRam / RAM_TICKS * Mth.PI) : 0.0F;
	}

	/** Draws the walker facing +z in the current pose, with {@code prefix} naming its parts' paint. */
	static void draw(String prefix, PoseStack pose, SubmitNodeCollector collector, int light, float stride, float ram) {
		draw(prefix + "_hull", pose, collector, light);
		draw(prefix + "_lamps", pose, collector, FULL_BRIGHT);
		float swing = Mth.sin(stride * STRIDE_RATE) * LEG_SWING;
		for (int i = 0; i < HIPS.length; i++) {
			pose.pushPose();
			at(pose, HIPS[i]);
			pose.rotateDegrees(Axis.XP, i == 0 ? swing : -swing);
			draw(prefix + "_leg", pose, collector, light);
			pose.popPose();
		}
		pose.pushPose();
		at(pose, SHOULDER);
		pose.rotateDegrees(Axis.XP, -swing * 0.25F);
		draw(prefix + "_tool_arm", pose, collector, light);
		pose.popPose();
		draw(prefix + "_piston_base", pose, collector, light);
		pose.pushPose();
		at(pose, PISTON);
		pose.translate(0, 0, ram * PISTON_STROKE / 16.0F);
		draw(prefix + "_piston_head", pose, collector, light);
		pose.popPose();
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
