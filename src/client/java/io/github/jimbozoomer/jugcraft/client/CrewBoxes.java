package io.github.jimbozoomer.jugcraft.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The tower crew's gear and props as tools/crew_skins.py lays them out (assets/jugcraft/crew/gear.json and props.json):
 * groups of boxes in model space, each group a part of its own hung off a body part (or the root) at its pivot, the boxes
 * at their texture offsets. Read when the model layers are built, so the boxes and the paint come from one place.
 */
final class CrewBoxes {
	record Box(float[] from, float[] size, int u, int v) {
	}

	/**
	 * bit: the gear bit that shows it (gear.json; on props.json, worn kit: security's helmet and armour); prop: the prop it
	 * is (props.json; 0 with no bit, a print on the uniform, always shown); level: kept level in the hand; scale: the part's
	 * (a box drawn finer than the skin's pixels: its size in texels, the part scaled down); hide: a gear bit that hides it
	 * (the uniform's print under the armour); standing: only while they stand (the armour's back frame); holstered: worn
	 * kit shown only while the baton isn't in hand (the baton in its holster); extend: drawn out along it as it lights (the
	 * baton's plasma, scaled along its length); rot: the part's turn at rest (degrees about x, y, z: a squad leader's
	 * plumes splayed out), kept under its posing.
	 */
	record Group(String name, String parent, float[] pivot, int bit, int prop, boolean level, float scale, int hide, boolean standing,
			boolean holstered, boolean extend, float[] rot, List<Box> boxes) {
		/** The rest turn in radians (x, y, z). */
		float rotX() {
			return rot[0] * net.minecraft.util.Mth.DEG_TO_RAD;
		}

		float rotY() {
			return rot[1] * net.minecraft.util.Mth.DEG_TO_RAD;
		}
	}

	private static List<Group> gear, props;
	private static int[] propsTexture = {128, 256};

	private CrewBoxes() {
	}

	static synchronized List<Group> gear() {
		if (gear == null) {
			gear = load("/assets/jugcraft/crew/gear.json");
		}
		return gear;
	}

	static synchronized List<Group> props() {
		if (props == null) {
			props = load("/assets/jugcraft/crew/props.json");
		}
		return props;
	}

	/** The props texture's size (props.json "texture"). */
	static synchronized int[] propsTexture() {
		props();
		return propsTexture;
	}

	/** Adds a group's part (named for the group) under {@code parent}. */
	static void add(PartDefinition parent, Group group) {
		CubeListBuilder cubes = CubeListBuilder.create();
		for (Box b : group.boxes()) {
			cubes.texOffs(b.u(), b.v()).addBox(b.from()[0], b.from()[1], b.from()[2], b.size()[0], b.size()[1], b.size()[2]);
		}
		float k = net.minecraft.util.Mth.DEG_TO_RAD;
		PartPose pose = PartPose.offsetAndRotation(group.pivot()[0], group.pivot()[1], group.pivot()[2], group.rot()[0] * k, group.rot()[1] * k,
				group.rot()[2] * k);
		parent.addOrReplaceChild(group.name(), cubes, group.scale() != 1F ? pose.withScale(group.scale()) : pose);
	}

	private static List<Group> load(String path) {
		try (InputStream raw = CrewBoxes.class.getResourceAsStream(path)) {
			if (raw == null) {
				throw new IllegalStateException("missing " + path);
			}
			JsonObject root = JsonParser.parseReader(new InputStreamReader(raw, StandardCharsets.UTF_8)).getAsJsonObject();
			if (path.endsWith("props.json") && root.has("texture")) {
				JsonArray t = root.getAsJsonArray("texture");
				propsTexture = new int[] {t.get(0).getAsInt(), t.get(1).getAsInt()};
			}
			List<Group> out = new ArrayList<>();
			for (JsonElement e : root.getAsJsonArray("groups")) {
				JsonObject g = e.getAsJsonObject();
				List<Box> boxes = new ArrayList<>();
				for (JsonElement be : g.getAsJsonArray("boxes")) {
					JsonObject b = be.getAsJsonObject();
					JsonArray uv = b.getAsJsonArray("uv");
					boxes.add(new Box(floats(b.getAsJsonArray("from")), floats(b.getAsJsonArray("size")), uv.get(0).getAsInt(), uv.get(1).getAsInt()));
				}
				out.add(new Group(g.get("name").getAsString(), g.get("parent").getAsString(), floats(g.getAsJsonArray("pivot")),
						g.has("bit") ? g.get("bit").getAsInt() : 0, g.has("prop") ? g.get("prop").getAsInt() : 0,
						g.has("level") && g.get("level").getAsBoolean(), g.has("scale") ? g.get("scale").getAsFloat() : 1F,
						g.has("hide") ? g.get("hide").getAsInt() : 0, g.has("standing") && g.get("standing").getAsBoolean(),
						g.has("holstered") && g.get("holstered").getAsBoolean(), g.has("extend") && g.get("extend").getAsBoolean(),
						g.has("rot") ? floats(g.getAsJsonArray("rot")) : new float[3], List.copyOf(boxes)));
			}
			return List.copyOf(out);
		} catch (Exception e) {
			Jugcraft.LOGGER.error("Could not read the tower crew's boxes ({})", path, e);
			return List.of();
		}
	}

	private static float[] floats(JsonArray a) {
		float[] out = new float[a.size()];
		for (int i = 0; i < out.length; i++) {
			out[i] = a.get(i).getAsFloat();
		}
		return out;
	}
}
