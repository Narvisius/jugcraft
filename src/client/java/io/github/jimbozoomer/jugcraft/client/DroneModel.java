package io.github.jimbozoomer.jugcraft.client;

/**
 * The drones as drawn in the world and as items: the detailed meshes of the October 2026 redesign
 * ({@link DroneMesh}), one per tier, with their rotors and fans turning and their gear, tilting nacelles and
 * cargo following the flight. The old 16x16 regions of {@code drone_depot.png} stay here for the depot's
 * supply hatches and lifts, which still use that sheet.
 *
 * <ul>
 * <li>1 Courier Quad: carbon arms on folding hinges, battery sled, GNSS mast, winch and parcel tote.
 * <li>2 Survey Hexacopter: moulded shell with a lit band, faired arms, LED motor rings, telescoping legs.
 * <li>3 Lifter Octocopter: coaxial X8 with prop guards, lit arm spines and a four-jaw crate grapple.
 * <li>4 Fan-Wing Runner: an arrowhead lifting body with four fans in the wing and a belly cargo pod.
 * <li>5 Tiltrotor Carrier: six tilting nacelles on a wing and V-tail, a hold with a rear ramp.
 * <li>6 Disc Freighter: an armoured saucer round one big fan, the 16-block module under the hub.
 * <li>7 Spine Carrier: two armoured lift hulls (hydrogen cells inside) with four modules on a spine.
 * <li>8 Halo Ion Carrier: a sleek hull through an ion-wind ring, belly lift fans, folding gear.
 * <li>9 Armoured Dropship: a chined armoured hull with six recessed superconducting fans and a ramp.
 * </ul>
 */
final class DroneModel {
	/** One texel of the 256x256 sheet. */
	private static final float PX = 1 / 256f;

	/** A 16x16 region of the old part of the sheet, by column and row. */
	private static float[] region(int col, int row) {
		return new float[] {col * 16 * PX, row * 16 * PX, (col + 1) * 16 * PX, (row + 1) * 16 * PX};
	}

	static final float[] ROTOR = region(1, 1);
	static final float[] CRATE = region(2, 1);
	static final float[] HATCH = region(3, 1);
	static final float[] LIFT = region(4, 1);
	static final float[] ARM = region(5, 1);
	static final float[] SHAFT = region(7, 1);
	static final float[] WHITE = region(0, 2);

	/** Crate edge per tier (bigger drones carry bigger crates). */
	private static final float[] CRATE_SIZES = {0.34f, 0.4f, 0.5f, 0.5f, 0.7f, 0.9f, 1.2f, 1.2f, 1.5f};
	static final float CRATE_SIZE = CRATE_SIZES[0];

	private DroneModel() {
	}

	/**
	 * How much smaller a drone of {@code tier} is drawn at a ground depot (no tower) so that, parked facing south, it stays
	 * on its own part of a 5x5 landing pad (DockLayout: a small drone a 2.5x2.5 quarter, a medium one a 5x2.5 half, a large
	 * one the whole pad). The newer, life-size models of the bigger tiers are larger than those pads; in the tower's bays
	 * they are drawn full size.
	 */
	static float groundFit(int tier) {
		var size = io.github.jimbozoomer.jugcraft.drone.DroneTier.byNumber(tier).map(io.github.jimbozoomer.jugcraft.drone.DroneTier::size)
				.orElse(io.github.jimbozoomer.jugcraft.drone.DroneSize.SMALL);
		float[] f = DroneMesh.footprint(tier == 10 ? 1 : tier);
		if (f == null) {
			return 1;
		}
		float margin = 0.2f;
		double fit = switch (size) {
			case SMALL -> Math.min((2.5 - margin) / f[0], (2.5 - margin) / f[1]);
			case MEDIUM -> Math.min((5 - margin) / f[0], (2.5 - margin) / f[1]);
			case LARGE -> Math.min((5 - margin) / f[0], (5 - margin) / f[1]);
		};
		return (float) Math.min(1, fit);
	}

	/** Heading of a docked drone: every drone faces south. */
	static float dockYaw(int tier) {
		return 0;
	}

	/** Plain colour from the fleet atlas's white corner. */
	private static final float[] FLEET_WHITE = {4 / 512f, 4 / 512f, 12 / 512f, 12 / 512f};

	/**
	 * One drone, in {@link DroneMesh#RENDER_TYPE}. {@code (x, y, z)} is the middle of its underside, {@code yaw}
	 * its heading, {@code cable} the length of the winch cable with a crate on it (negative: no crate),
	 * {@code spin} the rotor angle (0 when resting), {@code cruising} 0 to 1 how far into forward flight it is.
	 */
	static void draw(DroneDepotRenderer.Geometry g, float x, float y, float z, float yaw, int tier, float cable, float spin, int light,
			float cruising) {
		draw(g, x, y, z, yaw, tier, cable, spin, light, cruising, 1);
	}

	/** As {@link #draw}, the whole drone drawn {@code scale} times its size (a ground depot's drones fit its pads). */
	static void draw(DroneDepotRenderer.Geometry g, float x, float y, float z, float yaw, int tier, float cable, float spin, int light,
			float cruising, float scale) {
		g.reset();
		g.light = light;
		g.color = 0xFFFFFFFF;
		g.translate(x, y, z).yaw(yaw);
		if (scale != 1) {
			g.scale(scale);
		}
		int variant = cable > 0 ? DroneMesh.EMPTY : spin == 0 ? DroneMesh.PARKED : cruising > 0.5f ? DroneMesh.CRUISE : DroneMesh.HOVER;
		DroneMesh.Part[] parts = DroneMesh.get(tier == 10 ? 1 : tier, variant);
		float away = g.cameraDistance() / Math.max(0.05f, scale);
		int level = away > LOD_FAR ? 2 : away > LOD_NEAR ? 1 : 0;
		// at rest (in its bay, on a stand, in a case): the whole drone kept on the card as it stands (Retained), one draw
		if (parts != null && spin == 0 && cable <= 0) {
			boolean gold = tier == 10;
			int lod = level;
			if (g.retained(new Kept(parts, lod, gold), light, (pose, out) -> {
				DroneDepotRenderer.Geometry b = new DroneDepotRenderer.Geometry(pose, out);
				for (DroneMesh.Part part : parts) {
					drawPart(b, part, light, gold, lod);
				}
			}, 3 * scale)) {
				g.color = 0xFFFFFFFF;
				g.reset();
				return;
			}
		}
		if (parts == null) {
			g.color = 0xFF30353A;
			g.box(-0.4, 0.1, -0.4, 0.4, 0.5, 0.4, FLEET_WHITE);
		} else {
			for (DroneMesh.Part part : parts) {
				if (part.spin() != 0) {
					g.push();
					g.translate(part.px(), part.py(), part.pz()).rotate(spin * part.spin(), part.ax(), part.ay(), part.az())
							.translate(-part.px(), -part.py(), -part.pz());
				}
				drawPart(g, part, light, tier == 10, level);
				if (part.spin() != 0) {
					g.pop();
				}
			}
		}
		if (cable > 0) {
			float size = CRATE_SIZES[Math.max(0, Math.min(CRATE_SIZES.length - 1, tier - 1))];
			float top = -cable;
			// the case where it always hung; its sling above it, flatter when the winch has it up close
			float sling = Math.max(0, Math.min(size * 0.54f, cable - 0.06f));
			crate(g, 0, top - size, 0, size, sling, FLEET_WHITE);
			if (top + sling < 0.05f) {
				g.color = 0xFF202226;
				g.box(-0.012, top + sling, -0.012, 0.012, 0.05, 0.012, FLEET_WHITE);
			}
		}
		g.color = 0xFFFFFFFF;
		g.reset();
	}

	/** Which kept shape a drone at rest is: its parts (one set a tier and pose), the detail and whether it's the golden one. */
	record Kept(DroneMesh.Part[] parts, int level, boolean gold) {
		@Override
		public boolean equals(Object o) {
			return o instanceof Kept k && k.parts == parts && k.level == level && k.gold == gold;
		}

		@Override
		public int hashCode() {
			return (System.identityHashCode(parts) * 31 + level) * 2 + (gold ? 1 : 0);
		}
	}

	/**
	 * A drone's cargo case (Justin, 8 November 2026, look A of the cargo box sheet: "the dark box idea is good but they
	 * look bad"): a chamfered charcoal hard case {@code s} blocks across standing on (x, bottom, z), ribbed sides, rubber
	 * corner guards, a reflective orange band round the middle, steel latches front and back, carry handles on the ends,
	 * a label; with {@code sling} above zero, four webbing straps from its top corners to a steel ring and the hook
	 * {@code sling} over its top. {@code w} is where the bound texture is white (colours are the vertices').
	 */
	static void crate(DroneDepotRenderer.Geometry g, float x, float bottom, float z, float s, float sling, float[] w) {
		float h = s / 2, y0 = bottom, y1 = bottom + s;
		g.push();
		g.translate(x, 0, z);
		g.color = 0xFF2B2F33;
		chamfered(g, -h, y0, -h, h, y1, h, s * 0.07f, w);
		// ribs: two raised up each face
		g.color = 0xFF212428;
		float rw = s * 0.03f, rp = s * 0.018f, ra = y0 + s * 0.12f, rb = y1 - s * 0.12f;
		for (float a : new float[] {-0.22f * s, 0.22f * s}) {
			g.box(a - rw, ra, -h - rp, a + rw, rb, -h + 0.002f, w);
			g.box(a - rw, ra, h - 0.002f, a + rw, rb, h + rp, w);
			g.box(-h - rp, ra, a - rw, -h + 0.002f, rb, a + rw, w);
			g.box(h - 0.002f, ra, a - rw, h + rp, rb, a + rw, w);
		}
		// the band, between the corner guards
		g.color = 0xFFE0781F;
		float b0 = y1 - s * 0.58f, b1 = y1 - s * 0.44f, e = s * 0.024f, in = h - s * 0.1f;
		g.box(-h - e, b0, -in, -h + 0.002f, b1, in, w);
		g.box(h - 0.002f, b0, -in, h + e, b1, in, w);
		g.box(-in, b0, -h - e, in, b1, -h + 0.002f, w);
		g.box(-in, b0, h - 0.002f, in, b1, h + e, w);
		// rubber corner guards
		g.color = 0xFF131416;
		float c = s * 0.16f, o = Math.min(0.014f, s * 0.03f);
		for (int k = 0; k < 8; k++) {
			float cx = (k & 1) == 0 ? -h : h, cy = (k & 2) == 0 ? y0 : y1, cz = (k & 4) == 0 ? -h : h;
			float xa = cx < 0 ? cx - o : cx - c, xb = cx < 0 ? cx + c : cx + o;
			float ya = cy == y0 ? cy - o : cy - c, yb = cy == y0 ? cy + c : cy + o;
			float za = cz < 0 ? cz - o : cz - c, zb = cz < 0 ? cz + c : cz + o;
			chamfered(g, xa, ya, za, xb, yb, zb, s * 0.035f, w);
		}
		// steel latches over the band, front and back; the carry handles on the ends
		g.color = 0xFF9EA6AD;
		for (float lz : new float[] {-1, 1}) {
			for (float lx : new float[] {-0.27f, 0.27f}) {
				float zf = lz * (h + e), zo = lz * s * 0.03f;
				chamfered(g, lx * s - s * 0.05f, b0 - s * 0.05f, Math.min(zf, zf + zo), lx * s + s * 0.05f, b1 + s * 0.04f, Math.max(zf, zf + zo), s * 0.01f, w);
			}
		}
		float hy = y1 - s * 0.25f, hr = s * 0.016f, hw = s * 0.16f, ho = s * 0.035f;
		for (float sx : new float[] {-1, 1}) {
			float xf = sx * (h + ho);
			g.box(Math.min(xf, xf) - hr, hy - hr, -hw, xf + hr, hy + hr, hw, w);
			g.box(Math.min(sx * h, xf), hy - hr * 0.8f, -hw - hr * 0.8f, Math.max(sx * h, xf), hy + hr * 0.8f, -hw + hr * 0.8f, w);
			g.box(Math.min(sx * h, xf), hy - hr * 0.8f, hw - hr * 0.8f, Math.max(sx * h, xf), hy + hr * 0.8f, hw + hr * 0.8f, w);
		}
		// the label on the front, above the band: a pale plate with two dark lines of lettering
		g.color = 0xFFE4E4DA;
		g.box(-s * 0.2f, y1 - s * 0.36f, -h - 0.004f, s * 0.2f, y1 - s * 0.16f, -h + 0.001f, w);
		g.color = 0xFF2A2A2A;
		g.box(-s * 0.15f, y1 - s * 0.235f, -h - 0.005f, s * 0.15f, y1 - s * 0.21f, -h - 0.003f, w);
		g.box(-s * 0.13f, y1 - s * 0.305f, -h - 0.005f, s * 0.13f, y1 - s * 0.28f, -h - 0.003f, w);
		if (sling > 0.01f) {
			float ringY = y1 + sling * 0.78f;
			g.color = 0xFF17181A;
			for (int k = 0; k < 4; k++) {
				float sx = (k & 1) == 0 ? -1 : 1, sz = (k & 2) == 0 ? -1 : 1;
				strap(g, sx * (h - s * 0.08f), y1 + 0.005f, sz * (h - s * 0.08f), sx * s * 0.03f, ringY, sz * s * 0.03f, s * 0.06f, 0.006f, w);
			}
			g.color = 0xFF9EA6AD;
			chamfered(g, -s * 0.06f, ringY - s * 0.02f, -s * 0.06f, s * 0.06f, ringY + s * 0.03f, s * 0.06f, s * 0.02f, w);
			g.box(-s * 0.025f, ringY + s * 0.03f, -s * 0.012f, s * 0.025f, y1 + sling, s * 0.012f, w);
		}
		g.pop();
	}

	/** A flat strap (thin box) from a to b, {@code w} wide and {@code t} thick, its face turned outward from the middle. */
	private static void strap(DroneDepotRenderer.Geometry g, float ax, float ay, float az, float bx, float by, float bz, float wide, float t, float[] w) {
		float dx = bx - ax, dy = by - ay, dz = bz - az;
		float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (len < 1e-4f) {
			return;
		}
		g.push();
		g.translate(ax, ay, az);
		// turn about the vertical so the strap's local x lies across it (its face to the outside), then tip it over to b
		g.yaw(Math.atan2(dx, dz));
		g.pitch(Math.atan2(Math.sqrt(dx * dx + dz * dz), dy));
		g.box(-wide / 2, 0, -t / 2, wide / 2, len, t / 2, w);
		g.pop();
	}

	/** A box with every edge cut back by {@code ch} (its six faces, twelve bevels and eight corner facets). */
	static void chamfered(DroneDepotRenderer.Geometry g, float x0, float y0, float z0, float x1, float y1, float z1, float ch, float[] w) {
		ch = Math.min(ch, Math.min(x1 - x0, Math.min(y1 - y0, z1 - z0)) * 0.45f);
		if (ch <= 1e-4f) {
			g.box(x0, y0, z0, x1, y1, z1, w);
			return;
		}
		float[] u = w;
		float xa = x0 + ch, xb = x1 - ch, ya = y0 + ch, yb = y1 - ch, za = z0 + ch, zb = z1 - ch;
		// faces (counter-clockwise from outside, as Geometry.box)
		g.quad(xa, y1, za, xa, y1, zb, xb, y1, zb, xb, y1, za, 0, 1, 0, u);
		g.quad(xa, y0, zb, xa, y0, za, xb, y0, za, xb, y0, zb, 0, -1, 0, u);
		g.quad(xa, ya, z0, xa, yb, z0, xb, yb, z0, xb, ya, z0, 0, 0, -1, u);
		g.quad(xb, ya, z1, xb, yb, z1, xa, yb, z1, xa, ya, z1, 0, 0, 1, u);
		g.quad(x0, ya, zb, x0, yb, zb, x0, yb, za, x0, ya, za, -1, 0, 0, u);
		g.quad(x1, ya, za, x1, yb, za, x1, yb, zb, x1, ya, zb, 1, 0, 0, u);
		float r = 0.70710677f;
		// the four bevels round the top and the bottom
		g.quad(xa, yb, z0, xa, y1, za, xb, y1, za, xb, yb, z0, 0, r, -r, u);
		g.quad(xb, yb, z1, xb, y1, zb, xa, y1, zb, xa, yb, z1, 0, r, r, u);
		g.quad(x0, yb, zb, xa, y1, zb, xa, y1, za, x0, yb, za, -r, r, 0, u);
		g.quad(x1, yb, za, xb, y1, za, xb, y1, zb, x1, yb, zb, r, r, 0, u);
		g.quad(xa, y0, za, xa, ya, z0, xb, ya, z0, xb, y0, za, 0, -r, -r, u);
		g.quad(xb, y0, zb, xb, ya, z1, xa, ya, z1, xa, y0, zb, 0, -r, r, u);
		g.quad(xa, y0, zb, x0, ya, zb, x0, ya, za, xa, y0, za, -r, -r, 0, u);
		g.quad(xb, y0, za, x1, ya, za, x1, ya, zb, xb, y0, zb, r, -r, 0, u);
		// the four upright bevels
		g.quad(x0, ya, za, x0, yb, za, xa, yb, z0, xa, ya, z0, -r, 0, -r, u);
		g.quad(xb, ya, z0, xb, yb, z0, x1, yb, za, x1, ya, za, r, 0, -r, u);
		g.quad(x1, ya, zb, x1, yb, zb, xb, yb, z1, xb, ya, z1, r, 0, r, u);
		g.quad(xa, ya, z1, xa, yb, z1, x0, yb, zb, x0, ya, zb, -r, 0, r, u);
		// the eight corner facets (triangles: a quad with its last corner twice)
		float k = 0.57735026f;
		g.quad(x0, yb, za, xa, y1, za, xa, yb, z0, xa, yb, z0, -k, k, -k, u);
		g.quad(xb, yb, z0, xb, y1, za, x1, yb, za, x1, yb, za, k, k, -k, u);
		g.quad(x1, yb, zb, xb, y1, zb, xb, yb, z1, xb, yb, z1, k, k, k, u);
		g.quad(xa, yb, z1, xa, y1, zb, x0, yb, zb, x0, yb, zb, -k, k, k, u);
		g.quad(xa, ya, z0, xa, y0, za, x0, ya, za, x0, ya, za, -k, -k, -k, u);
		g.quad(x1, ya, za, xb, y0, za, xb, ya, z0, xb, ya, z0, k, -k, -k, u);
		g.quad(xb, ya, z1, xb, y0, zb, x1, ya, zb, x1, ya, zb, k, -k, k, u);
		g.quad(x0, ya, zb, xa, y0, zb, xa, ya, z1, xa, ya, z1, -k, -k, k, u);
	}

	private static void drawPart(DroneDepotRenderer.Geometry g, DroneMesh.Part part, int light, boolean gold, int level) {
		Ready r = level == 0 ? ready(part) : coarse(part, level);
		g.prepared(r.corners(), gold ? r.gold() : r.colours(), r.glows(), light);
	}

	/**
	 * Detail by distance (Justin's frame rate, 7 November 2026, "Less detail far away"): beyond {@link #LOD_NEAR} blocks a
	 * drone is drawn from its mesh with the corners closer together than {@link #CELL}[1] blocks run into one (the
	 * triangles that shrink to nothing left out), beyond {@link #LOD_FAR} with {@link #CELL}[2]; the shape and colours
	 * stay, the fine detail a pixel or two across on screen from there goes. Worked out once a part and level.
	 */
	static final float LOD_NEAR = 24, LOD_FAR = 48;
	static final float[] CELL = {0, 0.035f, 0.07f};
	private static final java.util.Map<DroneMesh.Part, Ready[]> COARSE = java.util.Collections.synchronizedMap(new java.util.IdentityHashMap<>());

	static Ready coarse(DroneMesh.Part part, int level) {
		Ready[] r = COARSE.computeIfAbsent(part, p -> new Ready[3]);
		if (r[level] == null) {
			r[level] = cluster(part, CELL[level]);
		}
		return r[level];
	}

	/** The part with its corners gathered to the middles of the {@code cell}-sized cubes they fall in. */
	static Ready cluster(DroneMesh.Part part, float cell) {
		float[] pos = part.pos(), uv = part.uv();
		byte[] nb = part.normal(), glow = part.glow();
		int[] color = part.color();
		int verts = color.length, tris = verts / 3;
		java.util.HashMap<Long, Integer> ids = new java.util.HashMap<>();
		int[] id = new int[verts];
		float[] sum = new float[verts * 3];
		int[] count = new int[verts];
		for (int i = 0; i < verts; i++) {
			long kx = (long) Math.floor(pos[i * 3] / cell), ky = (long) Math.floor(pos[i * 3 + 1] / cell), kz = (long) Math.floor(pos[i * 3 + 2] / cell);
			long key = (kx & 0x1FFFFF) << 42 | (ky & 0x1FFFFF) << 21 | (kz & 0x1FFFFF);
			Integer c = ids.get(key);
			if (c == null) {
				c = ids.size();
				ids.put(key, c);
			}
			id[i] = c;
			sum[c * 3] += pos[i * 3];
			sum[c * 3 + 1] += pos[i * 3 + 1];
			sum[c * 3 + 2] += pos[i * 3 + 2];
			count[c]++;
		}
		java.util.HashSet<String> seen = new java.util.HashSet<>();
		float[] p2 = new float[verts * 3], uv2 = new float[verts * 2];
		byte[] n2 = new byte[verts * 3], g2 = new byte[verts];
		int[] c2 = new int[verts];
		int n = 0;
		for (int t = 0; t < tris; t++) {
			int a = id[3 * t], b = id[3 * t + 1], c = id[3 * t + 2];
			if (a == b || b == c || a == c) {
				continue;
			}
			int lo = Math.min(a, Math.min(b, c)), hi = Math.max(a, Math.max(b, c)), mid = a + b + c - lo - hi;
			if (!seen.add(lo + ":" + mid + ":" + hi + ":" + color[3 * t] + ":" + (nb[9 * t] > 0 ? 1 : 0) + (nb[9 * t + 1] > 0 ? 1 : 0) + (nb[9 * t + 2] > 0 ? 1 : 0))) {
				continue;
			}
			for (int k = 0; k < 3; k++) {
				int i = 3 * t + k, cl = id[i];
				p2[n * 3] = sum[cl * 3] / count[cl];
				p2[n * 3 + 1] = sum[cl * 3 + 1] / count[cl];
				p2[n * 3 + 2] = sum[cl * 3 + 2] / count[cl];
				uv2[n * 2] = uv[i * 2];
				uv2[n * 2 + 1] = uv[i * 2 + 1];
				n2[n * 3] = nb[i * 3];
				n2[n * 3 + 1] = nb[i * 3 + 1];
				n2[n * 3 + 2] = nb[i * 3 + 2];
				g2[n] = glow[i];
				c2[n] = color[i];
				n++;
			}
		}
		// the same pairing into quads as the full mesh (corners shared now, so more of them pair)
		return prepare(new DroneMesh.Part(part.spin(), part.px(), part.py(), part.pz(), part.ax(), part.ay(), part.az(), java.util.Arrays.copyOf(p2, n * 3),
				java.util.Arrays.copyOf(n2, n * 3), java.util.Arrays.copyOf(c2, n), java.util.Arrays.copyOf(g2, n), java.util.Arrays.copyOf(uv2, n * 2)));
	}

	/**
	 * A drone part worked out once and kept (Justin's frame rate, 6 November 2026: "Build pieces once"; nineteen drones
	 * stand about the tower's hangars and halls, some twenty thousand triangles each for the biggest): per corner its
	 * place, texture and unit normal (8 floats), its colour (and in the golden drone's tint) and whether it glows. Its
	 * triangles go two to a quad wherever two in a row share an edge (the same two triangles, drawn as one quad split along
	 * that edge), which takes a third to a half off the corners drawn; a triangle left over is a quad with its last corner
	 * twice, as before.
	 */
	record Ready(float[] corners, int[] colours, int[] gold, boolean[] glows) {
	}

	private static final java.util.Map<DroneMesh.Part, Ready> READY = java.util.Collections.synchronizedMap(new java.util.IdentityHashMap<>());

	static Ready ready(DroneMesh.Part part) {
		Ready r = READY.get(part);
		if (r == null) {
			r = prepare(part);
			READY.put(part, r);
		}
		return r;
	}

	static Ready prepare(DroneMesh.Part part) {
		int tris = part.color().length / 3;
		int[] order = new int[tris * 4];
		int n = 0;
		for (int t = 0; t < tris; ) {
			int[] quad = t + 1 < tris ? pair(part, t, t + 1) : null;
			if (quad != null) {
				System.arraycopy(quad, 0, order, n, 4);
				t += 2;
			} else {
				order[n] = 3 * t;
				order[n + 1] = 3 * t + 1;
				order[n + 2] = 3 * t + 2;
				order[n + 3] = 3 * t + 2;
				t += 1;
			}
			n += 4;
		}
		float[] pos = part.pos(), uv = part.uv();
		byte[] nb = part.normal(), glow = part.glow();
		int[] color = part.color();
		float[] corners = new float[n * 8];
		int[] colours = new int[n], gold = new int[n];
		boolean[] glows = new boolean[n];
		for (int k = 0; k < n; k++) {
			int i = order[k], o = k * 8;
			corners[o] = pos[i * 3];
			corners[o + 1] = pos[i * 3 + 1];
			corners[o + 2] = pos[i * 3 + 2];
			corners[o + 3] = uv[i * 2];
			corners[o + 4] = uv[i * 2 + 1];
			float nx = nb[i * 3] / 127f, ny = nb[i * 3 + 1] / 127f, nz = nb[i * 3 + 2] / 127f;
			float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
			if (l > 1e-6f) {
				nx /= l;
				ny /= l;
				nz /= l;
			}
			corners[o + 5] = nx;
			corners[o + 6] = ny;
			corners[o + 7] = nz;
			int c = color[i];
			colours[k] = c;
			gold[k] = 0xFF000000 | Math.min(255, ((c >> 16) & 0xFF) * 2 + 60) << 16 | Math.min(255, ((c >> 8) & 0xFF) * 3 / 2 + 40) << 8 | (c & 0xFF) / 2;
			glows[k] = glow[i] != 0;
		}
		return new Ready(corners, colours, gold, glows);
	}

	/**
	 * Triangles {@code a} and {@code b} as one quad, if they share an edge (the same two corners, everything about them
	 * alike) and face the same way: its corners (indices into the part) ordered so the quad's split, corner 0 to corner 2,
	 * is that edge; null otherwise.
	 */
	static int[] pair(DroneMesh.Part part, int a, int b) {
		for (int i = 0; i < 3; i++) {
			int bi = 3 * a + i, ci = 3 * a + (i + 1) % 3, ai = 3 * a + (i + 2) % 3;
			int jb = -1, jc = -1;
			for (int j = 0; j < 3; j++) {
				if (same(part, bi, 3 * b + j)) {
					jb = j;
				} else if (same(part, ci, 3 * b + j)) {
					jc = j;
				}
			}
			// b runs the shared edge the other way (C then B), as a neighbour facing the same way does
			if (jb >= 0 && jc >= 0 && jb == (jc + 1) % 3) {
				int q = 3 * b + (jb + 1) % 3;
				return new int[] {bi, q, ci, ai};
			}
		}
		return null;
	}

	/** Whether corners i and j of a part are the same in every way (place, normal, colour, glow, texture). */
	private static boolean same(DroneMesh.Part part, int i, int j) {
		float[] p = part.pos(), uv = part.uv();
		byte[] n = part.normal();
		return p[i * 3] == p[j * 3] && p[i * 3 + 1] == p[j * 3 + 1] && p[i * 3 + 2] == p[j * 3 + 2] && n[i * 3] == n[j * 3] && n[i * 3 + 1] == n[j * 3 + 1]
				&& n[i * 3 + 2] == n[j * 3 + 2] && part.color()[i] == part.color()[j] && part.glow()[i] == part.glow()[j] && uv[i * 2] == uv[j * 2]
				&& uv[i * 2 + 1] == uv[j * 2 + 1];
	}
}
