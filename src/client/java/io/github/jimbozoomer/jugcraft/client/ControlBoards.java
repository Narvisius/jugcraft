package io.github.jimbozoomer.jugcraft.client;

import static io.github.jimbozoomer.jugcraft.client.NuclearRenderer.v;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.client.NuclearRenderer.Frame;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import net.minecraft.world.phys.Vec3;

/**
 * The main control room's boards and the reactor control console (round 23), drawn from the operations
 * tools/control_boards.py writes to assets/jugcraft/control_boards.json.gz: every switch, meter, recorder, lamp and
 * annunciator window of each board as boxes and quads in the equipment sheet's materials. The operations' format is
 * described in tools/control_boards.py; the frame is the equipment's own (x right, y up, z away from the operators).
 */
final class ControlBoards {
	/** How far behind a flashing window its unlit face is drawn (tools/control_boards.py BLINK_DEPTH). */
	private static final double BLINK_DEPTH = 0.004;
	private static Map<String, JsonObject> boards;

	private ControlBoards() {
	}

	private static Map<String, JsonObject> boards() {
		if (boards == null) {
			Map<String, JsonObject> out = new HashMap<>();
			try (InputStream in = ControlBoards.class.getResourceAsStream("/assets/jugcraft/control_boards.json.gz")) {
				if (in != null) {
					JsonObject all = JsonParser.parseReader(new InputStreamReader(new GZIPInputStream(in), StandardCharsets.UTF_8)).getAsJsonObject();
					for (Map.Entry<String, JsonElement> e : all.entrySet()) {
						out.put(e.getKey(), e.getValue().getAsJsonObject());
					}
				}
			} catch (Exception e) {
				io.github.jimbozoomer.jugcraft.Jugcraft.LOGGER.error("Could not read the control boards", e);
			}
			boards = out;
		}
		return boards;
	}

	/** Whether this equipment is drawn from the boards' operations. */
	static boolean has(EquipmentKind kind) {
		return boards().containsKey(kind.id);
	}

	/** The board's body and parts in the room's light (far away only its body), and its flashing windows unlit. */
	static void still(EquipmentKind kind, Frame f) {
		JsonObject board = boards().get(kind.id);
		if (board == null) {
			return;
		}
		JsonArray still = board.getAsJsonArray("still");
		int count = f.detail ? still.size() : Math.min(still.size(), board.get("core").getAsInt());
		for (int i = 0; i < count; i++) {
			op(f, still.get(i).getAsJsonArray());
		}
		if (f.detail) {
			for (JsonElement e : board.getAsJsonArray("blink")) {
				JsonArray q = e.getAsJsonArray();
				quad(f, corner(q, 1, BLINK_DEPTH), corner(q, 4, BLINK_DEPTH), corner(q, 7, BLINK_DEPTH), corner(q, 10, BLINK_DEPTH), q.get(13), false);
			}
		}
	}

	/** The board's lit parts (screens, lamps, lit windows, recorder charts), drawn at full brightness. */
	static void lit(EquipmentKind kind, Frame f) {
		JsonObject board = boards().get(kind.id);
		if (board != null) {
			for (JsonElement e : board.getAsJsonArray("lit")) {
				op(f, e.getAsJsonArray());
			}
		}
	}

	/** The alarm windows flashing now and then, lit in front of their unlit faces. */
	static void blink(EquipmentKind kind, Frame f, float time) {
		JsonObject board = boards().get(kind.id);
		int tick = (int) (time / 8);
		if (board == null || !f.detail || (tick / 40) % 3 != 0 || tick % 2 != 0) {
			return;
		}
		for (JsonElement e : board.getAsJsonArray("blink")) {
			JsonArray q = e.getAsJsonArray();
			quad(f, corner(q, 1, 0), corner(q, 4, 0), corner(q, 7, 0), corner(q, 10, 0), q.get(14), false);
		}
	}

	// ------------------------------------------------------------------ the operations

	private static void op(Frame f, JsonArray op) {
		switch (op.get(0).getAsString()) {
			case "b" -> {
				double x0 = d(op, 1), y0 = d(op, 2), z0 = d(op, 3), x1 = d(op, 4), y1 = d(op, 5), z1 = d(op, 6);
				box(f, op.get(7), (a, b, c) -> v(a == 0 ? x0 : x1, b == 0 ? y0 : y1, c == 0 ? z0 : z1));
			}
			case "o" -> {
				Vec3 c = v(d(op, 1), d(op, 2), d(op, 3));
				Vec3 u = v(d(op, 4), d(op, 5), d(op, 6));
				Vec3 w0 = v(d(op, 7), d(op, 8), d(op, 9));
				double hu = d(op, 10), hv = d(op, 11), hw = d(op, 12);
				Vec3 w = u.cross(w0);
				box(f, op.get(13), (a, b, k) -> c.add(u.scale(hu * (2 * a - 1))).add(w0.scale(hv * (2 * b - 1))).add(w.scale(hw * (2 * k - 1))));
			}
			case "q" -> quad(f, corner(op, 1, 0), corner(op, 4, 0), corner(op, 7, 0), corner(op, 10, 0), op.get(13), false);
			case "c" -> {
				Vec3 a = v(d(op, 1), d(op, 2), d(op, 3)), b = v(d(op, 4), d(op, 5), d(op, 6));
				double r = d(op, 7);
				int sides = f.sides(op.get(8).getAsInt());
				f.revolve(a, b, new double[][] {{0, r}, {a.distanceTo(b), r}}, sides, region(op.get(9)), false, false);
				f.disc(b, b.subtract(a).normalize(), r, sides, region(op.get(10)));
			}
			default -> {
			}
		}
	}

	private interface Corner {
		Vec3 at(int a, int b, int c);
	}

	/** A box's six faces (front at -z, back, left at -x, right, top, bottom), each seen from outside; null skips one. */
	private static void box(Frame f, JsonElement faces, Corner p) {
		JsonElement[] mats = new JsonElement[6];
		boolean one = faces.isJsonPrimitive() || (faces.getAsJsonArray().size() == 5 && faces.getAsJsonArray().get(1).isJsonPrimitive()
				&& faces.getAsJsonArray().get(1).getAsJsonPrimitive().isNumber());
		for (int i = 0; i < 6; i++) {
			mats[i] = one ? faces : faces.getAsJsonArray().get(i);
		}
		quad(f, p.at(0, 1, 0), p.at(0, 0, 0), p.at(1, 0, 0), p.at(1, 1, 0), mats[0], true);
		quad(f, p.at(1, 1, 1), p.at(1, 0, 1), p.at(0, 0, 1), p.at(0, 1, 1), mats[1], true);
		quad(f, p.at(0, 1, 1), p.at(0, 0, 1), p.at(0, 0, 0), p.at(0, 1, 0), mats[2], true);
		quad(f, p.at(1, 1, 0), p.at(1, 0, 0), p.at(1, 0, 1), p.at(1, 1, 1), mats[3], true);
		quad(f, p.at(0, 1, 1), p.at(0, 1, 0), p.at(1, 1, 0), p.at(1, 1, 1), mats[4], true);
		quad(f, p.at(0, 0, 0), p.at(0, 0, 1), p.at(1, 0, 1), p.at(1, 0, 0), mats[5], true);
	}

	/** One quad: a whole material repeats every block over a large face (as the renderer's own boxes do). */
	private static void quad(Frame f, Vec3 tl, Vec3 bl, Vec3 br, Vec3 tr, JsonElement mat, boolean tile) {
		if (mat == null || mat.isJsonNull()) {
			return;
		}
		Vec3 n = tr.subtract(tl).cross(bl.subtract(tl));
		if (n.lengthSqr() < 1e-14) {
			return;
		}
		n = n.normalize();
		float[] uv = region(mat);
		if (mat.isJsonPrimitive()) {
			f.tiled(tl, bl, br, tr, n, uv, 1.0);
		} else {
			f.quad(tl, bl, br, tr, n, uv[0], uv[1], uv[2], uv[3]);
		}
	}

	/** A material's place on the sheet: its whole cell, or [name, u0, v0, u1, v1] for part of it. */
	private static float[] region(JsonElement mat) {
		if (mat.isJsonPrimitive()) {
			return NuclearRenderer.material(mat.getAsString());
		}
		JsonArray a = mat.getAsJsonArray();
		float[] cell = NuclearRenderer.material(a.get(0).getAsString());
		float du = cell[2] - cell[0], dv = cell[3] - cell[1];
		return new float[] {cell[0] + du * a.get(1).getAsFloat(), cell[1] + dv * a.get(2).getAsFloat(),
				cell[0] + du * a.get(3).getAsFloat(), cell[1] + dv * a.get(4).getAsFloat()};
	}

	private static Vec3 corner(JsonArray op, int at, double dz) {
		return v(d(op, at), d(op, at + 1), d(op, at + 2) + dz);
	}

	private static double d(JsonArray op, int i) {
		return op.get(i).getAsDouble();
	}
}
