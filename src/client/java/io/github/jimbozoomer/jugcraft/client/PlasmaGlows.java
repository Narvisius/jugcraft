package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;

/**
 * Plasma's glows, as the rifle's bolt and the baton both draw them (PlasmaBoltRenderer, BatonPlasma): soft glows laid
 * over the scene on the eyes layer, which lays them on by their opacity ({@link Glows}), each quad turned to face
 * whoever looks. Two textures: a round soft glow (stretched along an axis for a core, a sheath, a corona) and a
 * filament (a thin bright line, for arcs).
 */
public final class PlasmaGlows {
	static final Identifier GLOW = Jugcraft.id("textures/entity/plasma_glow.png");
	static final Identifier FILAMENT = Jugcraft.id("textures/entity/plasma_filament.png");
	static final RenderType GLOW_TYPE = RenderTypes.eyes(GLOW), FILAMENT_TYPE = RenderTypes.eyes(FILAMENT);

	private PlasmaGlows() {
	}

	/**
	 * A soft glow stretched along {@code axis} ({@code length} by {@code width}, centred on c), turned round it to face
	 * the view ({@code view}: the way from the eye to it).
	 */
	static void axial(PoseStack.Pose p, VertexConsumer out, Vector3f c, Vector3f axis, Vector3f view, float length, float width, float[] rgb, float k,
			int facing) {
		Vector3f side = new Vector3f(axis).cross(view);
		if (side.lengthSquared() < 1e-6F) {
			side = new Vector3f(axis).cross(Math.abs(axis.y) < 0.9F ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0));
		}
		side.normalize().mul(width / 2);
		Vector3f a = new Vector3f(axis).normalize().mul(length / 2);
		quad(p, out, new Vector3f(c).sub(a).sub(side), new Vector3f(c).add(a).sub(side), new Vector3f(c).add(a).add(side), new Vector3f(c).sub(a).add(side),
				view, rgb, k, facing);
	}

	/** A round soft glow facing the view, {@code size} across. */
	static void sprite(PoseStack.Pose p, VertexConsumer out, Vector3f c, Vector3f view, float size, float[] rgb, float k, int facing) {
		Vector3f right = new Vector3f(view).cross(0, 1, 0);
		if (right.lengthSquared() < 1e-6F) {
			right.set(1, 0, 0);
		}
		right.normalize().mul(size / 2);
		Vector3f up = new Vector3f(right).cross(view).normalize().mul(size / 2);
		quad(p, out, new Vector3f(c).sub(right).sub(up), new Vector3f(c).add(right).sub(up), new Vector3f(c).add(right).add(up), new Vector3f(c).sub(right).add(up),
				view, rgb, k, facing);
	}

	/**
	 * One quad, its texture across it, light of colour rgb at strength k ({@link Glows#added}). {@code facing}: 1, wound
	 * to face the eye (one face only: drawn once whether the layer culls or not); -1, the other way round (a mirrored
	 * pose turns the winding over); 0, both (where which way is the eye's isn't known: a menu's slot).
	 */
	static void quad(PoseStack.Pose p, VertexConsumer out, Vector3f a, Vector3f b, Vector3f c, Vector3f d, Vector3f view, float[] rgb, float k, int facing) {
		int colour = Glows.added(rgb[0], rgb[1], rgb[2], k);
		Vector3f n = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a));
		boolean turned = n.dot(view) > 0;
		if (facing == 0 || turned == (facing > 0)) {
			vertex(p, out, d, 0, 1, colour);
			vertex(p, out, c, 1, 1, colour);
			vertex(p, out, b, 1, 0, colour);
			vertex(p, out, a, 0, 0, colour);
		}
		if (facing == 0 || turned != (facing > 0)) {
			vertex(p, out, a, 0, 0, colour);
			vertex(p, out, b, 1, 0, colour);
			vertex(p, out, c, 1, 1, colour);
			vertex(p, out, d, 0, 1, colour);
		}
	}

	private static void vertex(PoseStack.Pose p, VertexConsumer out, Vector3f v, float u, float w, int colour) {
		out.addVertex(p, v.x, v.y, v.z).setColor(colour).setUv(u, w).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(p, 0, 1, 0);
	}
}
