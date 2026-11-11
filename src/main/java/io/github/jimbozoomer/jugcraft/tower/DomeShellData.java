package io.github.jimbozoomer.jugcraft.tower;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The atrium dome's shell cells (tools/atrium_dome.py, {@value #PATH}): for each Dome Shell block's index, its place in
 * the Tri-Pillar (from the core), its solid at a quarter block (its collision and outline) and its look: quads in the
 * cell (block units), each a material (0 concrete, 1 light line, 2 formwork joint), a cull face (a Direction's index, or
 * -1) and four corners of x, y, z, u, v (0..1 over the texture), nx, ny, nz. Both sides read it: the block its shapes,
 * the client its look ({@code client/DomeShellModel}).
 */
public final class DomeShellData {
	public static final String PATH = "/data/jugcraft/drone_tower/atrium_dome.bin.gz";
	public static final int SUB = 4, FLOATS = 8;
	private static final DomeShellData INSTANCE = load();

	public final int count;
	private final int[] place;          // x, y, z per cell
	private final long[] solid;
	private final int[] quadStart;      // per cell, into the quads (count + 1 entries)
	private final byte[] material, cull;
	private final float[] corners;      // per quad: 4 corners of FLOATS
	private final VoxelShape[] shapes;

	private DomeShellData(int count, int[] place, long[] solid, int[] quadStart, byte[] material, byte[] cull, float[] corners) {
		this.count = count;
		this.place = place;
		this.solid = solid;
		this.quadStart = quadStart;
		this.material = material;
		this.cull = cull;
		this.corners = corners;
		this.shapes = new VoxelShape[count];
	}

	public static DomeShellData get() {
		return INSTANCE;
	}

	private static DomeShellData load() {
		try (InputStream raw = DomeShellData.class.getResourceAsStream(PATH)) {
			if (raw == null) {
				Jugcraft.LOGGER.error("Missing the atrium dome's data {}", PATH);
				return empty();
			}
			DataInputStream in = new DataInputStream(new java.io.BufferedInputStream(new GZIPInputStream(raw)));
			byte[] magic = new byte[4];
			in.readFully(magic);
			if (magic[0] != 'D' || magic[1] != 'O' || magic[2] != 'M' || magic[3] != 'E' || in.readInt() != 1) {
				throw new IOException("not the atrium dome's data");
			}
			int n = in.readInt();
			int[] place = new int[n * 3];
			long[] solid = new long[n];
			int[] start = new int[n + 1];
			java.io.ByteArrayOutputStream m = new java.io.ByteArrayOutputStream(), c = new java.io.ByteArrayOutputStream();
			float[] v = new float[1 << 16];
			int nv = 0, q = 0;
			for (int i = 0; i < n; i++) {
				place[i * 3] = in.readInt();
				place[i * 3 + 1] = in.readInt();
				place[i * 3 + 2] = in.readInt();
				solid[i] = in.readLong();
				int k = in.readInt();
				start[i] = q;
				for (int j = 0; j < k; j++) {
					m.write(in.readByte());
					c.write(in.readByte());
					if (nv + 4 * FLOATS > v.length) {
						v = java.util.Arrays.copyOf(v, v.length * 2);
					}
					for (int f = 0; f < 4 * FLOATS; f++) {
						v[nv++] = in.readFloat();
					}
					q++;
				}
			}
			start[n] = q;
			return new DomeShellData(n, place, solid, start, m.toByteArray(), c.toByteArray(), java.util.Arrays.copyOf(v, nv));
		} catch (IOException e) {
			Jugcraft.LOGGER.error("Could not read the atrium dome's data {}", PATH, e);
			return empty();
		}
	}

	private static DomeShellData empty() {
		return new DomeShellData(0, new int[0], new long[0], new int[] {0}, new byte[0], new byte[0], new float[0]);
	}

	/** Where cell i is from the tower's core. */
	public int x(int i) {
		return place[i * 3];
	}

	public int y(int i) {
		return place[i * 3 + 1];
	}

	public int z(int i) {
		return place[i * 3 + 2];
	}

	public int quadStart(int i) {
		return i >= 0 && i < count ? quadStart[i] : 0;
	}

	public int quadEnd(int i) {
		return i >= 0 && i < count ? quadStart[i + 1] : 0;
	}

	public int material(int quad) {
		return material[quad];
	}

	public int cull(int quad) {
		return cull[quad];
	}

	/** Corner c of a quad: x, y, z, u, v, nx, ny, nz (field f). */
	public float corner(int quad, int c, int f) {
		return corners[(quad * 4 + c) * FLOATS + f];
	}

	/** Cell i's concrete at a quarter block (a full block for an index it doesn't know). */
	public VoxelShape shape(int i) {
		if (i < 0 || i >= count) {
			return Shapes.block();
		}
		VoxelShape s = shapes[i];
		if (s == null) {
			boolean[][][] v = new boolean[SUB][SUB][SUB];
			for (int a = 0; a < SUB; a++) {
				for (int b = 0; b < SUB; b++) {
					for (int c = 0; c < SUB; c++) {
						v[a][b][c] = (solid[i] >>> (a * SUB * SUB + b * SUB + c) & 1L) != 0;
					}
				}
			}
			s = merge(v);
			shapes[i] = s;
		}
		return s;
	}

	/** Boxes merged along x, then z, then y. */
	static VoxelShape merge(boolean[][][] v) {
		int n = v.length;
		boolean[][][] left = new boolean[n][n][n];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				left[i][j] = v[i][j].clone();
			}
		}
		VoxelShape shape = Shapes.empty();
		for (int y = 0; y < n; y++) {
			for (int x = 0; x < n; x++) {
				for (int z = 0; z < n; z++) {
					if (!left[x][y][z]) {
						continue;
					}
					int x1 = x;
					while (x1 + 1 < n && left[x1 + 1][y][z]) {
						x1++;
					}
					int z1 = z;
					outer:
					while (z1 + 1 < n) {
						for (int a = x; a <= x1; a++) {
							if (!left[a][y][z1 + 1]) {
								break outer;
							}
						}
						z1++;
					}
					int y1 = y;
					outer2:
					while (y1 + 1 < n) {
						for (int a = x; a <= x1; a++) {
							for (int c = z; c <= z1; c++) {
								if (!left[a][y1 + 1][c]) {
									break outer2;
								}
							}
						}
						y1++;
					}
					for (int a = x; a <= x1; a++) {
						for (int b = y; b <= y1; b++) {
							for (int c = z; c <= z1; c++) {
								left[a][b][c] = false;
							}
						}
					}
					shape = Shapes.joinUnoptimized(shape, Shapes.box(x / (double) n, y / (double) n, z / (double) n, (x1 + 1) / (double) n,
							(y1 + 1) / (double) n, (z1 + 1) / (double) n), BooleanOp.OR);
				}
			}
		}
		return shape.isEmpty() ? Shapes.box(0, 0, 0, 1, 1 / (double) n, 1) : shape.optimize();
	}
}
