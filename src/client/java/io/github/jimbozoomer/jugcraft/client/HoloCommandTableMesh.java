package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

/**
 * The Holo Command Table's mesh ({@code assets/jugcraft/tower_mesh/holo_command_table.bin.gz}, written by
 * tools/holo_command_table.py) in the master cell's frame: its quads by kind (lit by the world, full bright, glowing
 * see-through), each corner {x, y, z, u, v, nx, ny, nz}; the five touch screens' corners and lamps; the field's radius
 * and height and the top's height.
 */
final class HoloCommandTableMesh {
	static final Identifier TEXTURE = Jugcraft.id("textures/entity/holo_command_table.png");
	static final int SOLID = 0, LIT = 1, GLOW = 2;
	/** Floats a corner: position, uv, normal. */
	static final int CORNER = 8;

	/** Quads of each kind, 4 corners each. */
	final float[][] quads = new float[3][];
	/** Per screen: top-left, top-right, bottom-right, bottom-left (as its user sees it), then the status lamp. */
	final float[][][] screens;
	final float fieldR, fieldY, top;

	private HoloCommandTableMesh(float[][] quads, float[][][] screens, float fieldR, float fieldY, float top) {
		System.arraycopy(quads, 0, this.quads, 0, 3);
		this.screens = screens;
		this.fieldR = fieldR;
		this.fieldY = fieldY;
		this.top = top;
	}

	private static HoloCommandTableMesh mesh;
	private static boolean tried;

	/** The mesh, or null if it could not be read (the table then draws nothing but its live parts). */
	static HoloCommandTableMesh get() {
		if (!tried) {
			tried = true;
			mesh = load();
		}
		return mesh;
	}

	private static HoloCommandTableMesh load() {
		Identifier id = Jugcraft.id("tower_mesh/holo_command_table.bin.gz");
		try {
			Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(id);
			if (resource.isEmpty()) {
				Jugcraft.LOGGER.error("Missing holo command table mesh {}", id);
				return null;
			}
			try (InputStream raw = resource.get().open(); DataInputStream in = new DataInputStream(new GZIPInputStream(raw))) {
				if (in.readInt() != 0x4A484354 || in.readInt() != 1) {
					throw new IllegalStateException("not a holo command table mesh");
				}
				int n = in.readInt();
				float[][] byKind = new float[3][n * 4 * CORNER];
				int[] used = new int[3];
				for (int q = 0; q < n; q++) {
					int kind = Math.max(0, Math.min(2, in.readByte()));
					float[] out = byKind[kind];
					for (int k = 0; k < 4; k++) {
						int o = used[kind];
						out[o] = in.readFloat();
						out[o + 1] = in.readFloat();
						out[o + 2] = in.readFloat();
						out[o + 5] = in.readByte() / 127f;
						out[o + 6] = in.readByte() / 127f;
						out[o + 7] = in.readByte() / 127f;
						out[o + 3] = in.readFloat();
						out[o + 4] = in.readFloat();
						used[kind] += CORNER;
					}
				}
				for (int k = 0; k < 3; k++) {
					byKind[k] = Arrays.copyOf(byKind[k], used[k]);
				}
				int screens = in.readInt();
				float[][][] s = new float[screens][5][3];
				for (int i = 0; i < screens; i++) {
					for (int c = 0; c < 5; c++) {
						s[i][c][0] = in.readFloat();
						s[i][c][1] = in.readFloat();
						s[i][c][2] = in.readFloat();
					}
				}
				float fieldR = in.readFloat(), fieldY = in.readFloat(), top = in.readFloat();
				return new HoloCommandTableMesh(byKind, s, fieldR, fieldY, top);
			}
		} catch (Exception e) {
			Jugcraft.LOGGER.error("Could not read the holo command table mesh {}", id, e);
			return null;
		}
	}
}
