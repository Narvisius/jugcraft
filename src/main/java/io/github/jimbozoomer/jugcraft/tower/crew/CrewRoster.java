package io.github.jimbozoomer.jugcraft.tower.crew;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * The tower's hiring pool (data/jugcraft/drone_tower/tower_crew.json, from tools/crew_skins.py): each job's skins (with
 * the gear they wear and whether their arms are slim) and the names new hands are hired under. The janitors are women,
 * everyone else men (Justin, 5 November 2026).
 */
public final class CrewRoster {
	/** One skin of a job's pool. */
	public record Look(String skin, int gear, boolean slim) {
	}

	/** Who a post hires: their look and their name. */
	public record Hire(Look look, String name) {
	}

	private static CrewRoster instance;

	private final Map<String, List<Look>> jobs = new HashMap<>();
	private final Set<String> women = new HashSet<>();
	private final List<String> men = new ArrayList<>(), womenNames = new ArrayList<>(), surnames = new ArrayList<>();

	private CrewRoster() {
		String path = "/data/jugcraft/drone_tower/tower_crew.json";
		try (InputStream raw = CrewRoster.class.getResourceAsStream(path)) {
			if (raw == null) {
				throw new IllegalStateException("missing " + path);
			}
			JsonObject root = JsonParser.parseReader(new InputStreamReader(raw, StandardCharsets.UTF_8)).getAsJsonObject();
			for (Map.Entry<String, JsonElement> job : root.getAsJsonObject("jobs").entrySet()) {
				List<Look> looks = new ArrayList<>();
				for (JsonElement e : job.getValue().getAsJsonArray()) {
					JsonObject o = e.getAsJsonObject();
					looks.add(new Look(o.get("skin").getAsString(), o.get("gear").getAsInt(), o.get("slim").getAsBoolean()));
				}
				jobs.put(job.getKey(), List.copyOf(looks));
			}
			root.getAsJsonArray("women").forEach(e -> women.add(e.getAsString()));
			JsonObject names = root.getAsJsonObject("names");
			names.getAsJsonArray("men").forEach(e -> men.add(e.getAsString()));
			names.getAsJsonArray("women").forEach(e -> womenNames.add(e.getAsString()));
			names.getAsJsonArray("surnames").forEach(e -> surnames.add(e.getAsString()));
		} catch (Exception e) {
			Jugcraft.LOGGER.error("Could not read the tower crew's hiring pool", e);
			throw new IllegalStateException("tower crew pool unreadable", e);
		}
	}

	public static synchronized CrewRoster get() {
		if (instance == null) {
			instance = new CrewRoster();
		}
		return instance;
	}

	public List<Look> looks(String job) {
		return jobs.getOrDefault(job, List.of());
	}

	/** The look a skin is in the pool (its gear and arms as the pool has them now), or null if the pool hasn't it. */
	public @org.jspecify.annotations.Nullable Look look(String skin) {
		for (List<Look> pool : jobs.values()) {
			for (Look l : pool) {
				if (l.skin().equals(skin)) {
					return l;
				}
			}
		}
		return null;
	}

	/**
	 * Who a post hires next: a look of its job's pool nobody in this tower wears now (the post's own turn round the pool,
	 * so a replacement is a new face), and a name.
	 */
	public Hire hire(String job, String post, int hires, long tower, Collection<String> worn) {
		List<Look> pool = looks(job);
		if (pool.isEmpty()) {
			throw new IllegalArgumentException("no skins for " + job);
		}
		int start = Math.floorMod(post.hashCode() + hires, pool.size());
		Look look = pool.get(start);
		for (int i = 0; i < pool.size(); i++) {
			Look candidate = pool.get((start + i) % pool.size());
			if (!worn.contains(candidate.skin())) {
				look = candidate;
				break;
			}
		}
		List<String> first = women.contains(job) ? womenNames : men;
		int seed = post.hashCode() * 31 + Long.hashCode(tower) + hires * 7919;
		String name = first.get(Math.floorMod(seed, first.size())) + " " + surnames.get(Math.floorMod(seed / 7 + hires, surnames.size()));
		return new Hire(look, name);
	}
}
