package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The plasma baton's plasma (Justin, 10 November 2026: he picked A, "Patrol", from the rebuild sheets), drawn the way the
 * rifle's bolt is ({@link PlasmaGlows}): the air round the rod glowing, an uneven sheath along it, a hot core, a bloom
 * where the head is wider and at the emitter's mouth, fine streamers standing off the rod all along it and a few arcs
 * that crackle anew every tick. Soft glows over the scene, each turned to face whoever looks, the ones that wrap the rod
 * drawn a little towards the eye so they lie over it.
 *
 * <p>Drawn in the item's own model space (the 0..1 cube its model is in: tools/plasma_baton.py), so the same code draws
 * it for the item wherever that is drawn (in a hand, a frame, on the ground, in a menu: {@link BatonPlasmaItem}) and for
 * a guard's (TowerCrewRenderer, growing out of the emitter as it lights). The baton's own frame: x across, y up, z along
 * it from the middle of the grip, in pixels (a sixteenth of the cube).
 */
public final class BatonPlasma {
	/** From tools/plasma_baton.py: the grip's middle along the model, the insulator's face (where the plasma starts) and
	 * the end cap's. */
	public static final float GX = 4.2F, ROD_FROM = 4.96F, TIP = 11.95F;
	private static final float PX = 1F / 16F;
	/** The colours (added light): white-hot, the sheath's blue-violet, the glowing air, the arcs. */
	private static final float[] WHITE = {1.0F, 0.97F, 1.0F}, SHEATH = {0.56F, 0.42F, 1.0F}, CORONA = {0.30F, 0.17F, 0.66F}, ARC = {0.80F, 0.74F, 1.0F};
	/** How far towards the eye what wraps the rod is drawn (so the rod, its rings and its head don't cut it in two). */
	private static final float LIFT = 0.56F * PX;
	private static final int STREAMERS = 26, ARCS = 4;

	private BatonPlasma() {
	}

	/** The time now (ticks, smooth), for a baton nothing else times: an item's. */
	static float now() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) {
			return 0;
		}
		return (float) (minecraft.level.getGameTime() % 24000L) + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
	}

	/**
	 * Draws it into the item's model space ({@code pose}: the cube's corner at the origin). {@code ext}: how far it is
	 * out along the rod (0 to 1: it grows from the emitter as the baton lights); {@code flat}: seen square on from far
	 * off, as a menu's slot shows an item (the eye isn't at the pose's origin there); {@code seed}: whose it is (two
	 * batons don't flicker as one); {@code time}: ticks, smooth.
	 */
	public static void submit(PoseStack pose, SubmitNodeCollector collector, float ext, boolean flat, int seed, float time) {
		if (ext <= 0.01F) {
			return;
		}
		collector.submitCustomGeometry(pose, PlasmaGlows.GLOW_TYPE, (p, out) -> glows(new Looker(p, flat), out, ext, seed, time));
		collector.submitCustomGeometry(pose, PlasmaGlows.FILAMENT_TYPE, (p, out) -> filaments(new Looker(p, flat), out, ext, seed, time));
	}

	/**
	 * Who looks, in the space the glows are drawn in. The world, a hand and anything else drawn in perspective is posed
	 * about the eye (the pose's origin is the camera), so the eye is the origin brought back through the pose. A menu
	 * draws square on (no eye to bring back; so does the figure in the inventory, which the item can't know of itself:
	 * the projection in force says so): the look is straight in, and both faces of a glow are drawn, as a menu's pose
	 * can be turned either way. A mirrored pose (the left hand's) turns every quad's winding over.
	 */
	static final class Looker {
		final PoseStack.Pose pose;
		final boolean flat, ok;
		final int facing;
		final Vector3f eye = new Vector3f(), straight = new Vector3f(0, 0, -1);

		Looker(PoseStack.Pose pose, boolean flat) {
			this.pose = pose;
			this.flat = flat || RenderSystem.isOnRenderThread() && RenderSystem.getProjectionType() == ProjectionType.ORTHOGRAPHIC;
			Matrix4f back = new Matrix4f(pose.pose()).invert();
			if (this.flat) {
				back.transformDirection(straight);
				ok = straight.isFinite() && straight.lengthSquared() > 1e-12F;
				if (ok) {
					straight.normalize();
				}
				facing = 0;
			} else {
				back.transformPosition(eye);
				ok = eye.isFinite();
				facing = pose.pose().determinant3x3() < 0 ? -1 : 1;
			}
		}

		/** The way from the eye to c. */
		Vector3f view(Vector3f c) {
			if (flat) {
				return new Vector3f(straight);
			}
			Vector3f v = new Vector3f(c).sub(eye);
			return v.lengthSquared() < 1e-12F ? new Vector3f(0, 0, -1) : v.normalize();
		}
	}

	/** A point of the baton's frame (px) in the model's cube. */
	private static Vector3f at(float x, float y, float z) {
		return new Vector3f((z + GX) * PX, (y + 8) * PX, (8 - x) * PX);
	}

	/** The baton's length in the cube: +x. */
	private static final Vector3f ALONG = new Vector3f(1, 0, 0);

	/** A soft glow stretched along {@code axis} at c, drawn {@code lift} towards the eye. */
	private static void glow(Looker l, VertexConsumer out, Vector3f c, Vector3f axis, float length, float width, float[] rgb, float k, float lift) {
		Vector3f view = l.view(c);
		PlasmaGlows.axial(l.pose, out, new Vector3f(c).fma(-lift, view), axis, view, length, width, rgb, k, l.facing);
	}

	/** A round soft glow at c, drawn {@code lift} towards the eye. */
	private static void round(Looker l, VertexConsumer out, Vector3f c, float size, float[] rgb, float k, float lift) {
		Vector3f view = l.view(c);
		PlasmaGlows.sprite(l.pose, out, new Vector3f(c).fma(-lift, view), view, size, rgb, k, l.facing);
	}

	/** The plasma's two ends along the rod (px) with it {@code ext} out. */
	private static float end(float ext) {
		float from = ROD_FROM - 0.1F;
		return from + (TIP + 0.45F - from) * Mth.clamp(ext, 0, 1);
	}

	/** The glowing air, the sheath, the core, the head's bloom and the emitter's mouth. */
	static void glows(Looker l, VertexConsumer out, float ext, int seed, float time) {
		if (!l.ok) {
			return;
		}
		float from = ROD_FROM - 0.1F, to = end(ext), length = to - from, mid = (from + to) / 2;
		float k = Mth.clamp(ext * 4, 0, 1) * (0.9F + 0.1F * drift(seed, 0, time / 1.5F));
		// the air round it glowing: one long glow and a few swells that wander along it
		glow(l, out, at(0, 0, mid), ALONG, (length + 2.4F) * PX, 4.6F * PX, CORONA, 0.50F * k, 0);
		for (int i = 0; i < 4; i++) {
			float z = from + (i + 0.2F + 0.6F * drift(seed, 10 + i, time / 7F)) / 4F * length;
			glow(l, out, at(0, 0, z), ALONG, (2.4F + 1.2F * drift(seed, 20 + i, time / 9F)) * PX, (3.6F + 1.8F * drift(seed, 30 + i, time / 6F)) * PX,
					CORONA, 0.34F * k, 0);
		}
		// the sheath: uneven, lengths of it overlapping, each swelling and thinning on its own
		int lobes = 7;
		for (int i = 0; i < lobes; i++) {
			float z = from + (i + 0.5F) / lobes * length;
			float wide = 2.0F + 0.9F * drift(seed, 40 + i, time / 3F);
			float dx = 0.35F * (drift(seed, 50 + i, time / 4F) - 0.5F), dy = 0.35F * (drift(seed, 60 + i, time / 4F) - 0.5F);
			glow(l, out, at(dx, dy, z), ALONG, length / lobes * 2.6F * PX, wide * PX, SHEATH, 0.64F * k, LIFT);
		}
		// the hot core round the rod
		glow(l, out, at(0, 0, mid - 0.15F * Math.min(1, ext * 4)), ALONG, (length + 0.3F) * PX, 1.5F * PX, WHITE, 0.66F * k, LIFT + 0.02F * PX);
		// the head, wider than the rod: the plasma blooms round it (the head standing out against it), a veil of it in front
		if (ext > 0.85F) {
			float bloom = k * (ext - 0.85F) / 0.15F;
			Vector3f head = at(0, 0, TIP - 0.6F);
			round(l, out, head, 3.0F * PX, SHEATH, 0.74F * bloom, 0.45F * PX);
			round(l, out, head, 1.5F * PX, WHITE, 0.45F * bloom, 0.47F * PX);
			round(l, out, head, 2.4F * PX, SHEATH, 0.26F * bloom, 1.1F * PX);
		}
		// the emitter's mouth
		round(l, out, at(0, 0, ROD_FROM + 0.15F), 2.0F * PX, SHEATH, 0.6F * k, 0.75F * PX);
	}

	/** The streamers (fine hairs of it standing off the rod, new every other tick) and the arcs (new every tick). */
	static void filaments(Looker l, VertexConsumer out, float ext, int seed, float time) {
		float from = ROD_FROM - 0.1F, length = end(ext) - from;
		if (!l.ok || length < 0.8F) {
			return;
		}
		float k = Mth.clamp(ext * 4, 0, 1);
		Random r = new Random(seed * 7919L + (long) Math.floor(time / 2F) * 104729L);
		for (int f = 0; f < STREAMERS; f++) {
			float z = from + 0.2F + r.nextFloat() * (length - 0.5F);
			float turn = r.nextFloat() * Mth.TWO_PI, reach = 0.35F + 0.65F * r.nextFloat();
			float cos = Mth.cos(turn), sin = Mth.sin(turn);
			Vector3f here = new Vector3f(0.30F * cos, 0.30F * sin, z);
			Vector3f d = new Vector3f(cos, sin, (float) r.nextGaussian() * 0.45F);
			int segs = 3;
			for (int s = 0; s < segs; s++) {
				d.add((float) r.nextGaussian() * 0.45F, (float) r.nextGaussian() * 0.45F, (float) r.nextGaussian() * 0.45F).normalize();
				Vector3f next = new Vector3f(here).fma(reach / segs, d);
				line(l, out, here, next, 1.3F, (0.13F - 0.025F * s) * PX, 0.5F * (float) Math.pow(1 - (float) s / segs, 1.3) * k, LIFT + 0.05F * PX);
				here = next;
			}
		}
		Random a = new Random(seed * 31337L + (long) Math.floor(time) * 15485863L);
		for (int f = 0; f < ARCS; f++) {
			float z = from + a.nextFloat() * length * 0.9F;
			float turn = a.nextFloat() * Mth.TWO_PI, reach = 1.0F + 1.3F * a.nextFloat();
			float cos = Mth.cos(turn), sin = Mth.sin(turn);
			Vector3f here = new Vector3f(0.32F * cos, 0.32F * sin, z);
			Vector3f d = new Vector3f(cos * 0.7F, sin * 0.7F, (float) a.nextGaussian() * 0.8F).normalize();
			int segs = 5;
			for (int s = 0; s < segs; s++) {
				d.add((float) a.nextGaussian() * 0.55F, (float) a.nextGaussian() * 0.55F, (float) a.nextGaussian() * 0.55F);
				d.z *= 0.8F;                             // kept off the rod's line: they stand out from it
				d.normalize();
				Vector3f next = new Vector3f(here).fma(reach / segs, d);
				line(l, out, here, next, 1.2F, 0.24F * PX, 0.85F * (1 - (float) s / segs) * k, 0.78F * PX);
				here = next;
			}
		}
	}

	/** A length of filament from a to b (the baton's frame, px), drawn {@code stretch} times as long so lengths overlap. */
	private static void line(Looker l, VertexConsumer out, Vector3f a, Vector3f b, float stretch, float width, float k, float lift) {
		Vector3f u = at(a.x, a.y, a.z), v = at(b.x, b.y, b.z);
		Vector3f axis = new Vector3f(v).sub(u);
		float length = axis.length();
		if (length < 1e-6F) {
			return;
		}
		glow(l, out, new Vector3f(u).add(v).mul(0.5F), axis, length * stretch, width, ARC, k, lift);
	}

	/** A value from 0 to 1 that wanders smoothly as t goes on (a new one to head for every whole t), its own for each n. */
	private static float drift(int seed, int n, float t) {
		int whole = Mth.floor(t);
		float part = t - whole;
		part = part * part * (3 - 2 * part);
		return Mth.lerp(part, hash(seed, n, whole), hash(seed, n, whole + 1));
	}

	private static float hash(int a, int b, int c) {
		int h = a * 0x9E3779B1 ^ b * 0x85EBCA77 ^ c * 0xC2B2AE3D;
		h ^= h >>> 15;
		h *= 0x2C1B3C6D;
		h ^= h >>> 12;
		h *= 0x297A2D39;
		h ^= h >>> 15;
		return (h & 0xFFFFFF) / (float) 0x1000000;
	}
}
