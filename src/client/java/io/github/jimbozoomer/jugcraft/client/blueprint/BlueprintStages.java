package io.github.jimbozoomer.jugcraft.client.blueprint;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.blueprint.BlueprintItem;
import io.github.jimbozoomer.jugcraft.blueprint.JugcraftBlueprints;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import net.fabricmc.fabric.api.event.client.player.ClientHotbarScrollEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Growth stages a blueprint can preview: for a blueprint that is the start of something bigger (the Drone Tower
 * Foundation), shift+scroll with it in hand steps through what it grows into, so players can see how much room
 * the finished build will need. Stage 0 is the blueprint itself.
 *
 * <p>Data: {@code assets/jugcraft/blueprint_stages/<blueprint id>.json.gz}: the anchor block (where the stages are
 * measured from, inside the blueprint) and per stage a name and columns {x, z, bottom, top} relative to it. Any
 * blueprint with such a file gets stages; others scroll the hotbar as usual. Client only.
 */
public final class BlueprintStages {
	/** One stage: its name and its outer faces, {x0, y0, z0, x1, y1, z1, kind} quads relative to the anchor. */
	public record Stage(String name, List<float[]> faces) {
	}

	public record Set(String anchor, List<Stage> stages) {
	}

	private static final Map<String, @Nullable Set> CACHE = new HashMap<>();
	private static final Map<String, Integer> CURRENT = new HashMap<>();

	private BlueprintStages() {
	}

	public static void register() {
		ClientHotbarScrollEvents.ALLOW.register((inventory, current, next, dx, dy) -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player == null || !minecraft.hasShiftDown() || dy == 0) {
				return true;
			}
			ItemStack held = minecraft.player.getMainHandItem();
			if (!held.is(JugcraftBlueprints.BLUEPRINT)) {
				return true;
			}
			String id = BlueprintItem.idOf(held);
			Set set = stages(id);
			if (set == null) {
				return true;
			}
			int stage = Math.floorMod(stage(id) + (dy > 0 ? 1 : -1), set.stages().size() + 1);
			CURRENT.put(id, stage);
			minecraft.player.sendOverlayMessage(stage == 0
					? Component.translatable("message.jugcraft.blueprint.stage.base")
					: Component.translatable("message.jugcraft.blueprint.stage", set.stages().get(stage - 1).name()));
			return false;
		});
	}

	/** The stage shown for {@code id} (0: the blueprint itself). */
	public static int stage(String id) {
		return CURRENT.getOrDefault(id, 0);
	}

	public static void setStage(String id, int stage) {
		CURRENT.put(id, stage);
	}

	/** The stages of blueprint {@code id}, or null when it has none. */
	public static @Nullable Set stages(String id) {
		if (id == null || id.contains("/")) {
			return null; // imported blueprints have no stages
		}
		return CACHE.computeIfAbsent(id, BlueprintStages::load);
	}

	private static @Nullable Set load(String id) {
		String path = "/assets/jugcraft/blueprint_stages/" + id + ".json.gz";
		try (InputStream raw = BlueprintStages.class.getResourceAsStream(path)) {
			if (raw == null) {
				return null;
			}
			JsonObject root = JsonParser.parseReader(new InputStreamReader(new GZIPInputStream(raw), StandardCharsets.UTF_8)).getAsJsonObject();
			List<Stage> stages = new ArrayList<>();
			for (JsonElement element : root.getAsJsonArray("stages")) {
				JsonObject s = element.getAsJsonObject();
				stages.add(new Stage(s.get("name").getAsString(), faces(s.getAsJsonArray("columns"))));
			}
			return new Set(root.get("anchor").getAsString(), stages);
		} catch (Exception e) {
			Jugcraft.LOGGER.warn("Blueprint stages for {} unreadable: {}", id, e.toString());
			return null;
		}
	}

	/** The outer faces of a set of columns: each column's top, and its sides where the neighbour is lower or missing. */
	private static List<float[]> faces(JsonArray columns) {
		Map<Long, int[]> map = new HashMap<>();
		for (JsonElement c : columns) {
			JsonArray a = c.getAsJsonArray();
			int[] col = {a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt(), a.get(3).getAsInt()};
			map.put(key(col[0], col[1]), col);
		}
		List<float[]> faces = new ArrayList<>();
		int[][] sides = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		for (int[] col : map.values()) {
			int x = col[0], z = col[1], lo = col[2], hi = col[3] + 1;
			faces.add(new float[] {x, hi, z, x + 1, hi, z + 1, 0});
			for (int s = 0; s < 4; s++) {
				int[] n = map.get(key(x + sides[s][0], z + sides[s][1]));
				int from = n == null ? lo : Math.max(lo, n[3] + 1);
				if (from >= hi) {
					continue;
				}
				float fx = sides[s][0] > 0 ? x + 1 : x, fz = sides[s][1] > 0 ? z + 1 : z;
				if (sides[s][0] != 0) {
					faces.add(new float[] {fx, from, z, fx, hi, z + 1, 1 + s});
				} else {
					faces.add(new float[] {x, from, fz, x + 1, hi, fz, 1 + s});
				}
			}
		}
		return faces;
	}

	private static long key(int x, int z) {
		return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
	}
}
