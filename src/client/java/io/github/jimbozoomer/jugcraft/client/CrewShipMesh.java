package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import org.jspecify.annotations.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

/**
 * The crew drop ship's mesh (tools/crew_ship.py writes {@code assets/jugcraft/crew_ship/ship.bin.gz} from the approved
 * mockup's geometry, with {@code textures/entity/crew_ship/<material>.png}, a 16-texel tile repeated across the faces).
 * Parts, each in its own frame about its pivot: the hull (with the bay's shell and the flight deck's lining), the
 * interior's fittings (seats, consoles, the bay's kit: drawn only near), the gear, the ramp (level, along +z from its
 * hinge), the side doors (shut), the door guns' arms (stowed), the two thrusters (flat), each of those again as a
 * coarser far model (its name with {@code ~far}), and the guns as models of their own (base, the part that turns, the
 * part that elevates about its trunnion). Since format 2 (the rework, 8 November 2026) every corner carries its own
 * normal, so the hull's skin and its fairings shade smooth; the glass and the head-up displays are see-through.
 */
final class CrewShipMesh {
	/**
	 * One material's quads in a part: corners (x y z, 4 a quad), their texture coordinates and normals (one a corner),
	 * and whether they glow; each quad's facing and middle ({@code face}: nx ny nz x y z; NaN for one that mustn't be
	 * skipped as facing away), whether it's see-through, and the light worked out for its corners last (the renderer's:
	 * kept while the ship and the world's light round it stay as they were). "Optimize what you can", 9 November 2026.
	 */
	static final class Batch {
		final int material;
		final boolean glow, translucent;
		final float[] pos, uv, normal, face;
		int[] lit;
		@Nullable Object litFor;
		final org.joml.Matrix4f litM = new org.joml.Matrix4f();
		double litX, litY, litZ;

		Batch(int material, boolean glow, boolean translucent, float[] pos, float[] uv, float[] normal) {
			this.material = material;
			this.glow = glow;
			this.translucent = translucent;
			this.pos = pos;
			this.uv = uv;
			this.normal = normal;
			this.face = faces(pos, normal);
		}

		int material() {
			return material;
		}

		boolean glow() {
			return glow;
		}

		float[] pos() {
			return pos;
		}

		float[] uv() {
			return uv;
		}

		float[] normal() {
			return normal;
		}

		int quads() {
			return pos.length / 12;
		}

		/** Each quad's facing (from its corners' winding: the way the game's culling takes it) and middle; NaN where the
		 * winding and its corners' normals disagree (never skipped then). */
		private static float[] faces(float[] pos, float[] normal) {
			int n = pos.length / 12;
			float[] f = new float[n * 6];
			for (int q = 0; q < n; q++) {
				int o = q * 12;
				float ax = pos[o + 6] - pos[o], ay = pos[o + 7] - pos[o + 1], az = pos[o + 8] - pos[o + 2];      // corner 2 - corner 0
				float bx = pos[o + 9] - pos[o + 3], by = pos[o + 10] - pos[o + 4], bz = pos[o + 11] - pos[o + 5];  // corner 3 - corner 1
				float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
				float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
				float vx = 0, vy = 0, vz = 0;
				for (int c = 0; c < 4; c++) {
					vx += normal[o + c * 3];
					vy += normal[o + c * 3 + 1];
					vz += normal[o + c * 3 + 2];
				}
				boolean ok = len > 1e-9F && (nx * vx + ny * vy + nz * vz) > 0;
				f[q * 6] = ok ? nx / len : Float.NaN;
				f[q * 6 + 1] = ok ? ny / len : Float.NaN;
				f[q * 6 + 2] = ok ? nz / len : Float.NaN;
				f[q * 6 + 3] = (pos[o] + pos[o + 3] + pos[o + 6] + pos[o + 9]) / 4;
				f[q * 6 + 4] = (pos[o + 1] + pos[o + 4] + pos[o + 7] + pos[o + 10]) / 4;
				f[q * 6 + 5] = (pos[o + 2] + pos[o + 5] + pos[o + 8] + pos[o + 11]) / 4;
			}
			return f;
		}
	}

	/** The materials drawn see-through (the canopy's and windows' glass, any model's glass, the head-up displays). */
	static boolean translucent(String material) {
		return material.equals("glass") || material.endsWith("glass") || material.equals("lens_hud");
	}

	record Part(String name, float px, float py, float pz, List<Batch> batches) {
	}

	final String[] materials;
	final RenderType[] types;
	final Identifier white;
	final Map<String, Part> parts = new HashMap<>();

	private static @Nullable CrewShipMesh mesh;
	private static boolean tried;
	/** Other models in the same format (a drop pod's: tools/drop_pod_export.py), by file, loaded once (null: unreadable). */
	private static final Map<String, Optional<CrewShipMesh>> OTHERS = new HashMap<>();

	private CrewShipMesh(String[] materials, List<Part> list, String textures) {
		this.materials = materials;
		types = new RenderType[materials.length];
		for (int i = 0; i < materials.length; i++) {
			Identifier tex = Jugcraft.id("textures/entity/" + textures + "/" + materials[i] + ".png");
			types[i] = translucent(materials[i]) ? RenderTypes.entityTranslucentCull(tex) : RenderTypes.entityCutoutCull(tex);
		}
		white = Jugcraft.id("textures/entity/crew_ship/white.png");
		for (Part p : list) {
			parts.put(p.name(), p);
		}
	}

	static @Nullable CrewShipMesh get() {
		if (!tried) {
			tried = true;
			mesh = load("crew_ship/ship.bin.gz", "crew_ship");
		}
		return mesh;
	}

	/** Another model in this format: {@code file} under assets/jugcraft, its materials' textures in textures/entity/{@code textures}. */
	static @Nullable CrewShipMesh of(String file, String textures) {
		return OTHERS.computeIfAbsent(file, f -> Optional.ofNullable(load(f, textures))).orElse(null);
	}

	private static @Nullable CrewShipMesh load(String file, String textures) {
		Identifier id = Jugcraft.id(file);
		try {
			Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(id);
			if (resource.isEmpty()) {
				Jugcraft.LOGGER.error("Missing crew ship mesh {}", id);
				return null;
			}
			try (InputStream raw = resource.get().open(); DataInputStream in = new DataInputStream(new GZIPInputStream(raw))) {
				if (in.readInt() != 0x4A435331 || in.readInt() != 2) {
					throw new IllegalStateException("not a crew ship mesh (format 2)");
				}
				String[] mats = new String[in.readInt()];
				for (int i = 0; i < mats.length; i++) {
					mats[i] = in.readUTF();
				}
				int n = in.readInt();
				List<Part> parts = new ArrayList<>();
				for (int g = 0; g < n; g++) {
					String name = in.readUTF();
					float px = in.readFloat(), py = in.readFloat(), pz = in.readFloat();
					int quads = in.readInt();
					Map<Integer, List<float[]>> byKey = new HashMap<>();
					for (int q = 0; q < quads; q++) {
						int mat = in.readUnsignedByte();
						int glow = in.readUnsignedByte();
						float[] quad = new float[32];                  // a corner: x y z u v, then its normal
						for (int c = 0; c < 4; c++) {
							for (int k = 0; k < 5; k++) {
								quad[c * 8 + k] = in.readFloat();
							}
							for (int k = 0; k < 3; k++) {
								quad[c * 8 + 5 + k] = in.readByte() / 127f;
							}
						}
						byKey.computeIfAbsent(mat * 2 + glow, k -> new ArrayList<>()).add(quad);
					}
					List<Batch> batches = new ArrayList<>();
					for (Map.Entry<Integer, List<float[]>> e : byKey.entrySet()) {
						List<float[]> list = e.getValue();
						float[] pos = new float[list.size() * 12], uv = new float[list.size() * 8], normal = new float[list.size() * 12];
						for (int q = 0; q < list.size(); q++) {
							float[] quad = list.get(q);
							for (int c = 0; c < 4; c++) {
								for (int k = 0; k < 3; k++) {
									pos[q * 12 + c * 3 + k] = quad[c * 8 + k];
									normal[q * 12 + c * 3 + k] = quad[c * 8 + 5 + k];
								}
								uv[q * 8 + c * 2] = quad[c * 8 + 3];
								uv[q * 8 + c * 2 + 1] = quad[c * 8 + 4];
							}
						}
						batches.add(new Batch(e.getKey() / 2, (e.getKey() & 1) == 1, translucent(mats[e.getKey() / 2]), pos, uv, normal));
					}
					parts.add(new Part(name, px, py, pz, batches));
				}
				return new CrewShipMesh(mats, parts, textures);
			}
		} catch (Exception e) {
			Jugcraft.LOGGER.error("Could not read crew ship mesh {}", id, e);
			return null;
		}
	}
}
