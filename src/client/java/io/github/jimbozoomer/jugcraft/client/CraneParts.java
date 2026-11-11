package io.github.jimbozoomer.jugcraft.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.client.NuclearRenderer.Frame;
import io.github.jimbozoomer.jugcraft.client.NuclearRenderer.Mesh;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind;
import io.github.jimbozoomer.jugcraft.nuclear.HallCrane;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;

/**
 * The turbine hall's overhead crane as the game draws it (turbine hall part 3, tools/hall_crane.py; nuclear/HallCrane.java
 * moves it): its bridge shifted along its runway, the trolley along the bridge, each hook let down below its rest with
 * its falls of rope stretched down to it from the trolley, the runway's rails, conductor bars and end stops laid a block
 * at a time along as far as the bridge can run. The floodlights under the girders are lit while the crane has power, the
 * travel beacons flash while the bridge or trolley runs. Each part's mesh is made once (its plates and stencils on the
 * turbine's legend texture in a pass of their own) and drawn where the crane's motions are at that moment.
 */
final class CraneParts {
	private static final EquipmentKind KIND = EquipmentKind.OVERHEAD_CRANE;
	/** The parts' meshes by detail (index 1 close up), and their legends. */
	private static final Mesh[][] BRIDGE = new Mesh[2][], TROLLEY = new Mesh[2][], HOOK = new Mesh[2][], AUX = new Mesh[2][];
	/** The whole runway between its ends (blocks along it), as last laid. */
	private static final java.util.Map<Long, Mesh> RUNWAY_ALL = new java.util.LinkedHashMap<>(8, 0.75F, true) {
		@Override
		protected boolean removeEldestEntry(java.util.Map.Entry<Long, Mesh> eldest) {
			return size() > 16;
		}
	};
	/** The conductor bars' bracket every this many blocks along the runway. */
	static final int BRACKET_EVERY = 3;
	static final double ROPE = 0.022;

	private CraneParts() {
	}

	/** Where the crane is as it is drawn: its motions (blocks), the runway's ends, its power, what is running. */
	record View(double bridge, double trolley, double main, double aux, double lo, double hi, boolean power, boolean travelling) {
		static View of(EquipmentBlockEntity e, double now) {
			boolean travel = HallCrane.moving(e, HallCrane.BRIDGE, now) || HallCrane.moving(e, HallCrane.TROLLEY, now);
			return new View(HallCrane.at(e, HallCrane.BRIDGE, now), HallCrane.at(e, HallCrane.TROLLEY, now), HallCrane.at(e, HallCrane.MAIN, now),
					HallCrane.at(e, HallCrane.AUX, now), e.craneLo, e.craneHi, e.cranePower, travel);
		}
	}

	private static JsonObject live() {
		return ControlBoards.live(KIND);
	}

	/** A group's operations as a mesh: the sheet's materials, and its legends. */
	private static Mesh[] group(JsonArray ops, boolean detail) {
		Mesh still = new Mesh(), legend = new Mesh();
		if (ops != null) {
			ControlBoards.ops(new Frame(still, detail), ops);
			Frame leg = new Frame(legend, true);
			leg.legends = true;
			if (detail) {
				ControlBoards.ops(leg, ops);
			}
		}
		return new Mesh[] {still, legend};
	}

	private static Mesh[] bridge(boolean detail) {
		int i = detail ? 1 : 0;
		if (BRIDGE[i] == null) {
			Mesh still = new Mesh(), legend = new Mesh();
			ControlBoards.still(KIND, new Frame(still, detail));
			Frame leg = new Frame(legend, true);
			leg.legends = true;
			if (detail) {
				ControlBoards.still(KIND, leg);
			}
			BRIDGE[i] = new Mesh[] {still, legend};
		}
		return BRIDGE[i];
	}

	private static Mesh[] part(Mesh[][] cache, String key, boolean detail) {
		int i = detail ? 1 : 0;
		if (cache[i] == null) {
			JsonObject live = live();
			cache[i] = group(live != null && live.has(key) ? live.getAsJsonObject(key).getAsJsonArray("still") : null, detail);
		}
		return cache[i];
	}

	/** The runway's parts laid from block lo to block hi along it (made once for those ends). */
	private static Mesh runwayMesh(int lo, int hi) {
		return RUNWAY_ALL.computeIfAbsent((long) lo << 32 | (hi & 0xFFFFFFFFL), key -> {
			Mesh mesh = new Mesh();
			JsonObject live = live();
			if (live == null || !live.has("runway")) {
				return mesh;
			}
			JsonObject parts = live.getAsJsonObject("runway");
			Frame f = new Frame(mesh, true);
			for (int k = lo; k <= hi; k++) {
				f.oz = k;
				ControlBoards.ops(f, parts.getAsJsonArray("rail"));
				ControlBoards.ops(f, parts.getAsJsonArray("bars"));
				if (Math.floorMod(k, BRACKET_EVERY) == 0) {
					ControlBoards.ops(f, parts.getAsJsonArray("bracket"));
				}
			}
			f.oz = lo;
			ControlBoards.ops(f, parts.getAsJsonArray("stop_low"));
			f.oz = hi;
			ControlBoards.ops(f, parts.getAsJsonArray("stop_high"));
			return mesh;
		});
	}

	private static double top(String key, double fallback) {
		JsonObject live = live();
		return live != null && live.has(key) ? live.getAsJsonObject(key).get("top").getAsDouble() : fallback;
	}

	private static double dx(String key, double fallback) {
		JsonObject live = live();
		return live != null && live.has(key) ? live.getAsJsonObject(key).get("dx").getAsDouble() : fallback;
	}

	/** The crane in one piece, its trolley at home and its hooks up (its item, its ghost where it would be placed). */
	static void still(Frame f) {
		ControlBoards.still(KIND, f);
		JsonObject live = live();
		if (live == null) {
			return;
		}
		double ox = f.ox;
		for (String key : new String[] {"trolley", "hook", "aux"}) {
			if (live.has(key)) {
				f.ox = ox + HallCrane.TROLLEY_HOME + (key.equals("trolley") ? 0 : dx(key, 0));
				ControlBoards.ops(f, live.getAsJsonObject(key).getAsJsonArray("still"));
			}
		}
		f.ox = ox;
	}

	/** The crane's body and live parts (the sheet's materials), its lamps lit at full brightness. */
	static void draw(PoseStack.Pose pose, VertexConsumer consumer, Direction facing, int light, View v, boolean detail, double time) {
		float bz = (float) v.bridge();
		Mesh[] b = bridge(detail);
		b[0].replay(pose, consumer, facing, light, 0, 0, bz);
		Mesh[] t = part(TROLLEY, "trolley", detail);
		t[0].replay(pose, consumer, facing, light, (float) v.trolley(), 0, bz);
		part(HOOK, "hook", detail)[0].replay(pose, consumer, facing, light, (float) (v.trolley() + dx("hook", HallCrane.MAIN_DX)), (float) -v.main(), bz);
		part(AUX, "aux", detail)[0].replay(pose, consumer, facing, light, (float) (v.trolley() + dx("aux", HallCrane.AUX_DX)), (float) -v.aux(), bz);
		// The runway a block at a time, from the bridge's lowest stop to the far side of its highest.
		int lo = (int) Math.floor(v.lo()), hi = (int) Math.ceil(v.hi()) + KIND.deep - 1;
		runwayMesh(lo, hi).replay(pose, consumer, facing, light);
		// The falls of rope from the trolley's drums and sheaves down to the hook blocks' tops.
		Mesh ropes = new Mesh(), still = new Mesh(), lit = new Mesh();
		JsonObject live = live();
		if (live == null) {
			return;
		}
		Frame rf = new Frame(ropes, detail);
		float[] rope = live.has("mats") && live.getAsJsonObject("mats").has("rope") ? ControlBoards.region(live.getAsJsonObject("mats").get("rope"))
				: NuclearRenderer.STAINLESS_DARK;
		if (live.has("trolley") && live.getAsJsonObject("trolley").has("ropes")) {
			double mainTop = top("hook", 3.0) - v.main(), auxTop = top("aux", 3.1) - v.aux();
			for (JsonElement e : live.getAsJsonObject("trolley").getAsJsonArray("ropes")) {
				JsonObject r = e.getAsJsonObject();
				boolean main = "main".equals(r.get("hook").getAsString());
				double x = v.trolley() + r.get("x").getAsDouble(), z = v.bridge() + r.get("z").getAsDouble(), y1 = r.get("y").getAsDouble();
				double y0 = main ? mainTop : auxTop;
				if (y1 - y0 > 0.01) {
					rf.box(x - ROPE, y0, z - ROPE, x + ROPE, y1, z + ROPE, rope);
				}
			}
		}
		if (ropes.vertices() > 0) {
			ropes.replay(pose, consumer, facing, light);
		}
		// The lamps: the floodlights with power, the travel beacons flashing while it runs.
		boolean flash = ((long) (time / 6)) % 2 == 0;
		for (JsonElement e : live.getAsJsonArray("lamps")) {
			JsonObject lamp = e.getAsJsonObject();
			String name = lamp.get("name").getAsString();
			boolean on = v.power() && (name.equals("crane_beacon") ? v.travelling() && flash : true);
			JsonArray box = lamp.getAsJsonArray("box"), faces = lamp.getAsJsonArray("faces");
			JsonElement mat = lamp.get(on ? "on" : "off");
			JsonArray mats = new JsonArray();
			for (int k = 0; k < 6; k++) {
				mats.add(faces.get(k).getAsInt() != 0 ? mat : JsonNull.INSTANCE);
			}
			Frame f = new Frame(on ? lit : still, detail);
			f.oz = v.bridge();
			ControlBoards.box(f, box.get(0).getAsDouble(), box.get(1).getAsDouble(), box.get(2).getAsDouble(), box.get(3).getAsDouble(),
					box.get(4).getAsDouble(), box.get(5).getAsDouble(), mats);
		}
		if (still.vertices() > 0) {
			still.replay(pose, consumer, facing, light);
		}
		if (lit.vertices() > 0) {
			lit.replay(pose, consumer, facing, LightCoordsUtil.FULL_BRIGHT);
		}
	}

	/** The crane's plates and stencils on the legend texture (TurbineParts.LEGEND_TYPE), where its parts are. */
	static void legends(PoseStack.Pose pose, VertexConsumer consumer, Direction facing, int light, View v) {
		float bz = (float) v.bridge();
		bridge(true)[1].replay(pose, consumer, facing, light, 0, 0, bz);
		part(TROLLEY, "trolley", true)[1].replay(pose, consumer, facing, light, (float) v.trolley(), 0, bz);
		part(HOOK, "hook", true)[1].replay(pose, consumer, facing, light, (float) (v.trolley() + dx("hook", HallCrane.MAIN_DX)), (float) -v.main(), bz);
		part(AUX, "aux", true)[1].replay(pose, consumer, facing, light, (float) (v.trolley() + dx("aux", HallCrane.AUX_DX)), (float) -v.aux(), bz);
	}
}
