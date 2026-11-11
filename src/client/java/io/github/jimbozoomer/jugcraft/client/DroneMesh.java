package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

/**
 * The drones' detailed meshes ({@code assets/jugcraft/drone_mesh/t1.bin} to {@code t9.bin}), exported from the
 * approved mockup models (the drone redesign, October 2026) by the drone mesh exporter. Every part is coloured
 * per vertex; labels and hazard stripes sample {@code textures/entity/drone_fleet.png}, everything else its
 * white corner, so no texture is ever stretched across a face. Lights are flagged to draw full bright.
 *
 * <p>Each tier has four variants: parked (gear down), hover (gear up), cruise (gear up, tilting parts forward)
 * and empty (hover without its cargo, used while a crate hangs on the winch). A variant is a list of parts:
 * part 0 is the fixed body, the rest are rotors and fans that turn about their own axis through their pivot.
 */
final class DroneMesh {
	static final Identifier TEXTURE = Jugcraft.id("textures/entity/drone_fleet.png");
	static final RenderType RENDER_TYPE = RenderTypes.entityCutout(TEXTURE);
	static final int PARKED = 0, HOVER = 1, CRUISE = 2, EMPTY = 3;
	/** The atlas's white corner, for parts drawn in plain vertex colour. */
	static final float WHITE_U = 8 / 512f, WHITE_V = 8 / 512f;

	/** One part: {@code spin} 0 for the body, otherwise its turning sense (+1 or -1). */
	record Part(int spin, float px, float py, float pz, float ax, float ay, float az, float[] pos, byte[] normal, int[] color,
			byte[] glow, float[] uv) {
		int vertices() {
			return color.length;
		}
	}

	private static final Part[][][] MESHES = new Part[10][][];
	private static final boolean[] TRIED = new boolean[10];

	private DroneMesh() {
	}

	/** The parts of {@code variant} for {@code tier} (1 to 9), or null if the mesh could not be read. */
	static Part[] get(int tier, int variant) {
		if (tier < 1 || tier > 9) {
			return null;
		}
		if (!TRIED[tier]) {
			TRIED[tier] = true;
			MESHES[tier] = load(tier);
		}
		Part[][] variants = MESHES[tier];
		return variants == null ? null : variants[Math.max(0, Math.min(variants.length - 1, variant))];
	}

	private static final float[][] FOOTPRINTS = new float[10][];

	/** Width (x) and length (z) in blocks of {@code tier}'s parked model, or null if its mesh could not be read. */
	static float[] footprint(int tier) {
		if (tier < 1 || tier > 9) {
			return null;
		}
		if (FOOTPRINTS[tier] == null) {
			Part[] parts = get(tier, PARKED);
			if (parts == null) {
				return null;
			}
			float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
			for (Part part : parts) {
				for (int i = 0; i + 2 < part.pos().length; i += 3) {
					minX = Math.min(minX, part.pos()[i]);
					maxX = Math.max(maxX, part.pos()[i]);
					minZ = Math.min(minZ, part.pos()[i + 2]);
					maxZ = Math.max(maxZ, part.pos()[i + 2]);
				}
			}
			FOOTPRINTS[tier] = new float[] {maxX - minX, maxZ - minZ};
		}
		return FOOTPRINTS[tier];
	}

	private static final float[][] MIDDLES = new float[10][];

	/**
	 * The middle of {@code tier}'s parked model's plan (x, z) in its own frame (a model's origin is not always its
	 * middle: the T4's long nose puts its middle 0.31 forward of it), or (0, 0) if its mesh could not be read.
	 */
	static float[] middle(int tier) {
		if (tier < 1 || tier > 9) {
			return new float[2];
		}
		if (MIDDLES[tier] == null) {
			Part[] parts = get(tier, PARKED);
			if (parts == null) {
				return new float[2];
			}
			float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
			for (Part part : parts) {
				for (int i = 0; i + 2 < part.pos().length; i += 3) {
					minX = Math.min(minX, part.pos()[i]);
					maxX = Math.max(maxX, part.pos()[i]);
					minZ = Math.min(minZ, part.pos()[i + 2]);
					maxZ = Math.max(maxZ, part.pos()[i + 2]);
				}
			}
			MIDDLES[tier] = new float[] {(minX + maxX) / 2, (minZ + maxZ) / 2};
		}
		return MIDDLES[tier];
	}

	private static final float[][] REACHES = new float[10][];

	/**
	 * How far {@code tier}'s parked model reaches from its middle, across (x and z together: the circle it sweeps turning
	 * on the spot), and how high it stands; null if its mesh could not be read.
	 */
	static float[] reach(int tier) {
		if (tier < 1 || tier > 9) {
			return null;
		}
		if (REACHES[tier] == null) {
			Part[] parts = get(tier, PARKED);
			if (parts == null) {
				return null;
			}
			float r = 0, top = 0, bottom = 0;
			for (Part part : parts) {
				float[] pos = part.pos();
				for (int i = 0; i + 2 < pos.length; i += 3) {
					r = Math.max(r, (float) Math.hypot(pos[i], pos[i + 2]));
					top = Math.max(top, pos[i + 1]);
					bottom = Math.min(bottom, pos[i + 1]);
				}
			}
			REACHES[tier] = new float[] {r, top - bottom};
		}
		return REACHES[tier];
	}

	private static Part[][] load(int tier) {
		Identifier id = Jugcraft.id("drone_mesh/t" + tier + ".bin");
		try {
			Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(id);
			if (resource.isEmpty()) {
				Jugcraft.LOGGER.error("Missing drone mesh {}", id);
				return null;
			}
			try (InputStream raw = resource.get().open(); DataInputStream in = new DataInputStream(new GZIPInputStream(raw))) {
				if (in.readInt() != 0x4A444D32) {
					throw new IllegalStateException("not a drone mesh");
				}
				int variants = in.readByte();
				Part[][] out = new Part[variants][];
				for (int v = 0; v < variants; v++) {
					int parts = in.readByte();
					out[v] = new Part[parts];
					for (int p = 0; p < parts; p++) {
						int spin = in.readByte();
						float px = in.readFloat(), py = in.readFloat(), pz = in.readFloat();
						float ax = in.readFloat(), ay = in.readFloat(), az = in.readFloat();
						int n = in.readInt();
						float[] pos = new float[n * 3];
						byte[] normal = new byte[n * 3];
						int[] color = new int[n];
						byte[] glow = new byte[n];
						float[] uv = new float[n * 2];
						for (int i = 0; i < n; i++) {
							pos[i * 3] = in.readFloat();
							pos[i * 3 + 1] = in.readFloat();
							pos[i * 3 + 2] = in.readFloat();
							normal[i * 3] = in.readByte();
							normal[i * 3 + 1] = in.readByte();
							normal[i * 3 + 2] = in.readByte();
							glow[i] = in.readByte();
							color[i] = in.readInt();
							uv[i * 2] = in.readFloat();
							uv[i * 2 + 1] = in.readFloat();
						}
						out[v][p] = new Part(spin, px, py, pz, ax, ay, az, pos, normal, color, glow, uv);
					}
				}
				return out;
			}
		} catch (Exception e) {
			Jugcraft.LOGGER.error("Could not read drone mesh {}", id, e);
			return null;
		}
	}
}
