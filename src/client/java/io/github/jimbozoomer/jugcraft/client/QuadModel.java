package io.github.jimbozoomer.jugcraft.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Textured quads exported from the generators' model boxes (tools/kinetic_rotors.py): a JSON list of
 * {texture, normal, vertices [x, y, z (pixels), u, v]}. Drawn by block entity renderers and render
 * layers that show parts of a block or item model moving (spinning rotors, a rocket pack on a back).
 */
public final class QuadModel {
	private record Quad(float[] normal, float[][] vertices) {
	}

	private final Map<RenderType, List<Quad>> quads = new LinkedHashMap<>();

	private QuadModel() {
	}

	/** Reads the quads, with positions converted from pixels to blocks. */
	public static QuadModel parse(JsonArray list) {
		QuadModel model = new QuadModel();
		for (JsonElement element : list) {
			JsonObject quad = element.getAsJsonObject();
			String texture = quad.get("texture").getAsString();
			// Plain names are block textures; "item/..." and other paths are taken from textures/ directly.
			String path = texture.contains("/") ? "textures/" + texture + ".png" : "textures/block/" + texture + ".png";
			RenderType type = RenderTypes.entitySolid(Jugcraft.id(path));
			JsonArray n = quad.getAsJsonArray("normal");
			JsonArray corners = quad.getAsJsonArray("vertices");
			float[][] vertices = new float[4][];
			for (int i = 0; i < 4; i++) {
				JsonArray v = corners.get(i).getAsJsonArray();
				vertices[i] = new float[] {v.get(0).getAsFloat() / 16, v.get(1).getAsFloat() / 16, v.get(2).getAsFloat() / 16,
						v.get(3).getAsFloat(), v.get(4).getAsFloat()};
			}
			model.quads.computeIfAbsent(type, t -> new ArrayList<>())
					.add(new Quad(new float[] {n.get(0).getAsFloat(), n.get(1).getAsFloat(), n.get(2).getAsFloat()}, vertices));
		}
		return model;
	}

	/** Draws the quads in the current pose, one geometry submission per texture. */
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light) {
		for (Map.Entry<RenderType, List<Quad>> entry : quads.entrySet()) {
			List<Quad> list = entry.getValue();
			collector.submitCustomGeometry(pose, entry.getKey(), (matrix, buffer) -> {
				for (Quad quad : list) {
					for (float[] v : quad.vertices()) {
						buffer.addVertex(matrix, v[0], v[1], v[2]).setColor(0xFFFFFFFF).setUv(v[3], v[4])
								.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
								.setNormal(matrix, quad.normal()[0], quad.normal()[1], quad.normal()[2]);
					}
				}
			});
		}
	}
}
