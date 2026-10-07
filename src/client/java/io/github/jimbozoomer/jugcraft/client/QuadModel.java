package io.github.jimbozoomer.jugcraft.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Textured quads exported from the generators' model boxes (tools/kinetic_rotors.py): a JSON list of
 * {texture, normal, vertices [x, y, z (pixels), u, v]}, and "cutout": true for a quad drawn cut out. Drawn by block entity renderers and render
 * layers that show parts of a block or item model moving (spinning rotors, a rocket pack on a back), and worn 3D armor
 * ({@link WornModelLayer}).
 *
 * <p>A quad may also have "nocull": true, to be drawn from both sides (a balloon's envelope, seen from inside its
 * basket), and "normals", one for each corner, for a curved surface lit smoothly across its quads.
 */
public final class QuadModel {
	private record Quad(float[][] normals, float[][] vertices) {
	}

	private final Map<RenderType, List<Quad>> quads = new LinkedHashMap<>();
	/** The texture of each render type's quads, for {@link #submitAs}. */
	private final Map<RenderType, Identifier> textures = new HashMap<>();

	private QuadModel() {
	}

	/** Reads the quads, with positions converted from pixels to blocks. */
	public static QuadModel parse(JsonArray list) {
		QuadModel model = new QuadModel();
		for (JsonElement element : list) {
			JsonObject quad = element.getAsJsonObject();
			String name = quad.get("texture").getAsString();
			// Plain names are block textures; "item/..." and other paths are taken from textures/ directly.
			Identifier texture = Jugcraft.id(name.contains("/") ? "textures/" + name + ".png" : "textures/block/" + name + ".png");
			// A quad marked "cutout" (a silhouette) leaves out its texture's see-through pixels.
			boolean cutout = quad.has("cutout") && quad.get("cutout").getAsBoolean();
			// A quad marked "nocull" is drawn from both sides: 26.3's translucent entity type doesn't cull (its culling one is
			// entityTranslucentCull). An opaque texture draws through it as solid.
			boolean nocull = quad.has("nocull") && quad.get("nocull").getAsBoolean();
			RenderType type = nocull ? RenderTypes.entityTranslucent(texture) : cutout ? RenderTypes.entityCutout(texture) : RenderTypes.entitySolid(texture);
			JsonArray n = quad.getAsJsonArray("normal");
			float[] flat = {n.get(0).getAsFloat(), n.get(1).getAsFloat(), n.get(2).getAsFloat()};
			float[][] normals = {flat, flat, flat, flat};
			if (quad.has("normals")) {
				JsonArray each = quad.getAsJsonArray("normals");
				for (int i = 0; i < 4; i++) {
					JsonArray c = each.get(i).getAsJsonArray();
					normals[i] = new float[] {c.get(0).getAsFloat(), c.get(1).getAsFloat(), c.get(2).getAsFloat()};
				}
			}
			JsonArray corners = quad.getAsJsonArray("vertices");
			float[][] vertices = new float[4][];
			for (int i = 0; i < 4; i++) {
				JsonArray v = corners.get(i).getAsJsonArray();
				vertices[i] = new float[] {v.get(0).getAsFloat() / 16, v.get(1).getAsFloat() / 16, v.get(2).getAsFloat() / 16,
						v.get(3).getAsFloat(), v.get(4).getAsFloat()};
			}
			model.quads.computeIfAbsent(type, t -> new ArrayList<>()).add(new Quad(normals, vertices));
			model.textures.put(type, texture);
		}
		return model;
	}

	/** Draws the quads in the current pose, one geometry submission per texture. */
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light) {
		submit(pose, collector, light, 0xFFFFFFFF);
	}

	/** Draws the quads in the current pose tinted {@code color} (ARGB), one geometry submission per texture. */
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, int color) {
		for (Map.Entry<RenderType, List<Quad>> entry : quads.entrySet()) {
			List<Quad> list = entry.getValue();
			collector.submitCustomGeometry(pose, entry.getKey(), (matrix, buffer) -> emit(matrix, buffer, list, light, color));
		}
	}

	/**
	 * Draws the quads in the current pose with the render type {@code type} makes of their texture, in submission order
	 * {@code order} (a later order is drawn after an earlier one). Worn armor is drawn this way: RenderTypes::armorCutoutNoCull,
	 * then RenderTypes::armorCutoutNoCullGlint at a later order over an enchanted piece. The quads' own "cutout" and
	 * "nocull" are not used.
	 */
	public void submitAs(PoseStack pose, SubmitNodeCollector collector, int order, int light, Function<Identifier, RenderType> type) {
		for (Map.Entry<RenderType, List<Quad>> entry : quads.entrySet()) {
			List<Quad> list = entry.getValue();
			collector.order(order).submitCustomGeometry(pose, type.apply(textures.get(entry.getKey())),
					(matrix, buffer) -> emit(matrix, buffer, list, light, 0xFFFFFFFF));
		}
	}

	private static void emit(PoseStack.Pose matrix, VertexConsumer buffer, List<Quad> list, int light, int color) {
		for (Quad quad : list) {
			for (int i = 0; i < 4; i++) {
				float[] v = quad.vertices()[i];
				float[] n = quad.normals()[i];
				buffer.addVertex(matrix, v[0], v[1], v[2]).setColor(color).setUv(v[3], v[4])
						.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(matrix, n[0], n[1], n[2]);
			}
		}
	}
}
