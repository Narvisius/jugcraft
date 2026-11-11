package io.github.jimbozoomer.jugcraft.client.warehouse;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.warehouse.HiddenBlock;
import io.github.jimbozoomer.jugcraft.warehouse.JugcraftWarehouse;
import io.github.jimbozoomer.jugcraft.warehouse.WarehouseLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3fc;

/**
 * The warehouse's moving things' models, as the game baked them (the hidden warehouse_part and warehouse_load blocks'
 * states: tools/warehouse_data.py PARTS, the pallet loads), drawn at any pose in the model's own pixels, as the plant
 * doors' leaves are (client/PlantDoorRenderer.java). A part's status light can be tinted (the drones' strips).
 * <p>The solid faces are drawn with the solid things and the see-through ones (a pallet's stretch film, glass) after
 * them: drawn together, a film face drawn before the cartons behind it hid them, and you saw through the cartons
 * (Justin, 27 October 2026).
 */
public final class WhQuads {
	public static final RenderType RENDER_TYPE = RenderTypes.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS);
	/** The solid (and cut-out) faces: drawn before anything see-through. */
	public static final RenderType SOLID_TYPE = RenderTypes.entityCutoutCull(TextureAtlas.LOCATION_BLOCKS);
	private static final Identifier LIGHT = Identifier.fromNamespaceAndPath("jugcraft", "block/pi_wh_light");
	private static final Identifier BEACON = Identifier.fromNamespaceAndPath("jugcraft", "block/pi_wh_beacon");
	/** Part W5: lit white, tinted like the status strips (a pilot lamp's colour, a map cell's). */
	private static final Identifier WHITE = Identifier.fromNamespaceAndPath("jugcraft", "block/pi_wh5_white");
	private static final Map<BlockState, Cached> CACHE = new HashMap<>();
	/** Each model's faces split into the solid ones and the see-through ones (by the list, as cached). */
	private static final Map<List<BakedQuad>, List<BakedQuad>[]> SPLIT = new java.util.IdentityHashMap<>();

	private record Cached(BlockStateModel model, List<BakedQuad> quads) {
	}

	/** How a part's lit bits are drawn: its status strip's colour (ARGB) and whether its beacon is on. */
	public record Tint(int light, boolean beacon) {
		public static final Tint NONE = new Tint(0xFFFFFFFF, true);
	}

	private WhQuads() {
	}

	public static int part(String name) {
		return WarehouseLayout.get().part(name);
	}

	public static List<BakedQuad> partQuads(int part) {
		return quads(((HiddenBlock) JugcraftWarehouse.PART).with(part));
	}

	public static List<BakedQuad> loadQuads(int look) {
		return quads(((HiddenBlock) JugcraftWarehouse.LOAD).with(Math.max(0, look)));
	}

	public static List<BakedQuad> quads(BlockState state) {
		BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
		Cached cached = CACHE.get(state);
		if (cached != null && cached.model() == model) {
			return cached.quads();
		}
		List<BlockStateModelPart> parts = new ArrayList<>();
		model.collectParts(RandomSource.create(42L), parts);
		List<BakedQuad> quads = new ArrayList<>();
		for (BlockStateModelPart p : parts) {
			for (Direction side : Direction.values()) {
				quads.addAll(p.getQuads(side));
			}
			quads.addAll(p.getQuads(null));
		}
		Cached old = CACHE.put(state, new Cached(model, quads));
		if (old != null) {
			SPLIT.remove(old.quads());
		}
		return quads;
	}

	/** The faces' solid and see-through halves (the layer the game baked each face for, from its texture). */
	@SuppressWarnings("unchecked")
	private static List<BakedQuad>[] split(List<BakedQuad> quads) {
		List<BakedQuad>[] got = SPLIT.get(quads);
		if (got != null) {
			return got;
		}
		List<BakedQuad> solid = new ArrayList<>(), clear = new ArrayList<>();
		for (BakedQuad q : quads) {
			(q.materialInfo().layer() == net.minecraft.client.renderer.chunk.ChunkSectionLayer.TRANSLUCENT ? clear : solid).add(q);
		}
		got = new List[] {solid, clear};
		if (SPLIT.size() > 4096) {
			SPLIT.clear();
		}
		SPLIT.put(quads, got);
		return got;
	}

	/**
	 * Submits a part by name at the pose: a part written moved (tools/warehouse_data.py PART_SHIFT: its elements must
	 * stay within -16..32) is moved back.
	 */
	public static void submitPart(SubmitNodeCollector collector, PoseStack pose, String name, int light, Tint tint) {
		com.google.gson.JsonObject shifts = WarehouseLayout.get().section("part_shift");
		if (shifts != null && shifts.has(name)) {
			com.google.gson.JsonArray d = shifts.getAsJsonArray(name);
			pose.pushPose();
			pose.translate(-d.get(0).getAsFloat(), -d.get(1).getAsFloat(), -d.get(2).getAsFloat());
			submit(collector, pose, partQuads(part(name)), light, tint);
			pose.popPose();
			return;
		}
		submit(collector, pose, partQuads(part(name)), light, tint);
	}

	/** Submits quads at the pose (in the model's pixels): the solid ones with the solid things, the see-through ones after. */
	public static void submit(SubmitNodeCollector collector, PoseStack pose, List<BakedQuad> quads, int light, Tint tint) {
		List<BakedQuad>[] halves = split(quads);
		if (!halves[0].isEmpty()) {
			collector.submitCustomGeometry(pose, SOLID_TYPE, (p, consumer) -> draw(p, consumer, halves[0], light, tint));
		}
		if (!halves[1].isEmpty()) {
			collector.submitCustomGeometry(pose, RENDER_TYPE, (p, consumer) -> draw(p, consumer, halves[1], light, tint));
		}
	}

	/** Part W5: a lit lamp's lens (the w5_led part) of a colour: its middle at (x, y), its face at z, radius r (a lens there). */
	public static void lamp(SubmitNodeCollector collector, PoseStack pose, float x, float y, float z, float r, int colour) {
		pose.pushPose();
		pose.translate(x, y, z - 0.05F);
		pose.scale(r * 1.04F, r * 1.04F, 0.2F);
		submitPart(collector, pose, "w5_led", LightCoordsUtil.FULL_BRIGHT, new Tint(colour, true));
		pose.popPose();
	}

	/** The middle of the lit white sprite (u, v), from the w5_fill part's quad. */
	private static float[] whiteUv() {
		List<BakedQuad> q = partQuads(part("w5_fill"));
		if (q.isEmpty()) {
			return new float[] {0, 0};
		}
		float u = 0, v = 0;
		for (int i = 0; i < 4; i++) {
			long uv = q.get(0).packedUV(i);
			u += UVPair.unpackU(uv) / 4;
			v += UVPair.unpackV(uv) / 4;
		}
		return new float[] {u, v};
	}

	/**
	 * Part W5: lit rectangles facing +z at z (the status board's map): rects holds x0, y0, x1, y1 for each, colours its
	 * ARGB (pixels of the current frame).
	 */
	public static void rects(SubmitNodeCollector collector, PoseStack pose, float[] rects, int[] colours, int n, float z) {
		if (n <= 0) {
			return;
		}
		float[] uv = whiteUv();
		float[] r = java.util.Arrays.copyOf(rects, n * 4);
		int[] c = java.util.Arrays.copyOf(colours, n);
		collector.submitCustomGeometry(pose, RENDER_TYPE, (p, consumer) -> {
			for (int k = 0; k < n; k++) {
				float x0 = r[k * 4], y0 = r[k * 4 + 1], x1 = r[k * 4 + 2], y1 = r[k * 4 + 3];
				vertex(p, consumer, x0, y0, z, uv, c[k], 0, 0, 1);
				vertex(p, consumer, x1, y0, z, uv, c[k], 0, 0, 1);
				vertex(p, consumer, x1, y1, z, uv, c[k], 0, 0, 1);
				vertex(p, consumer, x0, y1, z, uv, c[k], 0, 0, 1);
			}
		});
	}

	/** Part W5: a lit line from a to b (pixels of the current frame), width w across, seen from every side (a scanner's beam). */
	public static void line(SubmitNodeCollector collector, PoseStack pose, float[] a, float[] b, float w, int colour) {
		float[] uv = whiteUv();
		org.joml.Vector3f d = new org.joml.Vector3f(b[0] - a[0], b[1] - a[1], b[2] - a[2]);
		if (d.lengthSquared() < 1e-6F) {
			return;
		}
		org.joml.Vector3f u = new org.joml.Vector3f(d).cross(Math.abs(d.y) > 0.9F * d.length() ? 1 : 0, Math.abs(d.y) > 0.9F * d.length() ? 0 : 1, 0)
				.normalize(w / 2);
		org.joml.Vector3f v = new org.joml.Vector3f(d).cross(u).normalize(w / 2);
		collector.submitCustomGeometry(pose, RENDER_TYPE, (p, consumer) -> {
			for (org.joml.Vector3f s : new org.joml.Vector3f[] {u, v}) {
				float[][] q = {{a[0] - s.x, a[1] - s.y, a[2] - s.z}, {a[0] + s.x, a[1] + s.y, a[2] + s.z}, {b[0] + s.x, b[1] + s.y, b[2] + s.z},
						{b[0] - s.x, b[1] - s.y, b[2] - s.z}};
				for (int side = 0; side < 2; side++) {
					for (int i = 0; i < 4; i++) {
						float[] pt = q[side == 0 ? i : 3 - i];
						vertex(p, consumer, pt[0], pt[1], pt[2], uv, colour, 0, 1, 0);
					}
				}
			}
		});
	}

	private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, float x, float y, float z, float[] uv, int colour, float nx, float ny, float nz) {
		consumer.addVertex(pose, x, y, z).setColor(colour).setUv(uv[0], uv[1]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT)
				.setNormal(pose, nx, ny, nz);
	}

	public static void draw(PoseStack.Pose pose, VertexConsumer consumer, List<BakedQuad> quads, int light, Tint tint) {
		for (BakedQuad quad : quads) {
			int emission = quad.materialInfo().lightEmission();
			int colour = 0xFFFFFFFF;
			Identifier sprite = quad.materialInfo().sprite().contents().name();
			if (LIGHT.equals(sprite) || WHITE.equals(sprite)) {
				colour = tint.light();
			} else if (BEACON.equals(sprite) && !tint.beacon()) {
				colour = 0xFF6A5A40;
				emission = 0;
			}
			int lit = emission > 0 ? LightCoordsUtil.lightCoordsWithEmission(light, emission) : light;
			Direction d = quad.direction();
			for (int i = 0; i < 4; i++) {
				Vector3fc v = quad.position(i);
				long uv = quad.packedUV(i);
				consumer.addVertex(pose, v.x() * 16, v.y() * 16, v.z() * 16).setColor(colour).setUv(UVPair.unpackU(uv), UVPair.unpackV(uv))
						.setOverlay(OverlayTexture.NO_OVERLAY).setLight(lit).setNormal(pose, d.getStepX(), d.getStepY(), d.getStepZ());
			}
		}
	}
}
