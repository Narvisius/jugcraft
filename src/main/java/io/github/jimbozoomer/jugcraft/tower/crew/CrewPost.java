package io.github.jimbozoomer.jugcraft.tower.crew;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * One of the Tri-Pillar's crew posts (tools/tripillar_crew.py; the layout's "crew" list): who works there ({@link #job}),
 * how ({@link #kind}: {@code seat}, {@code station} or {@code patrol}), what they do and carry there, and where, all
 * relative to the Tower Core. {@link #step} is the build step that brings the post in (0: the foundation). A seat that
 * isn't a player's seat (an office chair of the room kits, a Pilot Simulator) comes with its point ({@link #seatAt},
 * relative to the core) and the way its sitter faces ({@link #seatYaw}, degrees).
 *
 * <p>Its routine (the night shift and breaks sheet, approved 5 November 2026): {@link #night} if the post is kept on at
 * night, else where its crew member sleeps ({@link #sleep}); and {@link #breaks}, its breaks over a three-day rota
 * ({@link #CYCLE}; Justin, 6 November 2026: a "schedule over like 3 days"): two or three over the three days, never
 * two in a day, at other times each day (half as many since 10 November 2026, his "fewer breaks": before, one each day
 * and a second on two days of the three). {@link #restAt} says which of them, if any, a time of day falls in.
 *
 * <p>A janitor's post has their cart's place (C5, 9 November 2026): {@link #cart}, the parked cart's first cell in the
 * custodian's closet, and {@link #cartStand}, where they stand to take it and to park it back.
 *
 * <p>A {@code perimeter} post is a patrol outside the tower (the security detail round the control building): its spots
 * are on the world's ground there, at whatever height that is ({@link #outside}).
 */
public record CrewPost(String id, String job, String kind, String act, @Nullable String prop, @Nullable BlockPos seat,
		@Nullable BlockPos stand, List<Spot> spots, int step, @Nullable Vec3 seatAt, float seatYaw, boolean night,
		@Nullable Rest sleep, List<Rest> breaks, @Nullable BlockPos cart, @Nullable BlockPos cartStand) {

	/** Ticks in a day. */
	public static final int DAY = 24000;
	/** The days of the break rota: a break's {@link Rest#days} is the world's day count over this many. */
	public static final int CYCLE = 3;

	/** A post without a routine (it works day and night). */
	public CrewPost(String id, String job, String kind, String act, @Nullable String prop, @Nullable BlockPos seat,
			@Nullable BlockPos stand, List<Spot> spots, int step, @Nullable Vec3 seatAt, float seatYaw) {
		this(id, job, kind, act, prop, seat, stand, spots, step, seatAt, seatYaw, true, null, List.of(), null, null);
	}

	/** A post with a routine and no cart. */
	public CrewPost(String id, String job, String kind, String act, @Nullable String prop, @Nullable BlockPos seat,
			@Nullable BlockPos stand, List<Spot> spots, int step, @Nullable Vec3 seatAt, float seatYaw, boolean night,
			@Nullable Rest sleep, List<Rest> breaks) {
		this(id, job, kind, act, prop, seat, stand, spots, step, seatAt, seatYaw, night, sleep, breaks, null, null);
	}

	/** A place to stand (relative to the core) and, at a station, the way to face there (degrees; NaN: any). */
	public record Spot(BlockPos pos, float yaw) {
	}

	/**
	 * A seat to rest on (relative to the core): the seat's cell, the point the sitter settles at and the way they face
	 * (degrees), what they do there ({@code doze}, {@code recline}, {@code lie}; a break's {@code mug}), the free cell beside
	 * it to walk to, and when (ticks of the day, 06:00 = 0: from, to; a sleep runs over midnight). {@link #days}: a
	 * break's day of the rota (0, 1 or 2: the world's day count over {@link #CYCLE}); -1 every day.
	 */
	public record Rest(BlockPos seat, Vec3 at, float yaw, String act, BlockPos stand, int from, int to, int days) {
		/** Whether {@code time} (ticks of the day) on day {@code day} falls in it. */
		public boolean covers(int time, long day) {
			if (days >= 0 && Math.floorMod(day, (long) CYCLE) != days) {
				return false;
			}
			return from <= to ? time >= from && time < to : time >= from || time < to;
		}
	}

	/** The rest {@code dayTime} (the world's clock: ticks since the first morning) falls in: the sleep, a break, or none. */
	public @Nullable Rest restAt(long dayTime) {
		int time = (int) Math.floorMod(dayTime, (long) DAY);
		long day = Math.floorDiv(dayTime, (long) DAY);
		if (sleep != null && sleep.covers(time, day)) {
			return sleep;
		}
		for (Rest b : breaks) {
			if (b.covers(time, day)) {
				return b;
			}
		}
		return null;
	}

	public boolean seated() {
		return kind.equals("seat");
	}

	public boolean patrol() {
		return kind.equals("patrol") || outside();
	}

	/** A patrol outside the tower: its spots on the world's ground round it (their height the ground's there). */
	public boolean outside() {
		return kind.equals("perimeter");
	}

	/** Where a new hand appears and comes back to (relative to the core): beside the chair, or the first spot. */
	public BlockPos home() {
		if (stand != null) {
			return stand;
		}
		return spots.isEmpty() ? BlockPos.ZERO : spots.getFirst().pos();
	}

	public static List<CrewPost> list(JsonArray array) {
		List<CrewPost> out = new ArrayList<>();
		for (JsonElement element : array) {
			JsonObject o = element.getAsJsonObject();
			List<Spot> spots = new ArrayList<>();
			for (JsonElement s : o.getAsJsonArray("spots")) {
				JsonArray a = s.getAsJsonArray();
				spots.add(new Spot(new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt()),
						a.size() > 3 ? a.get(3).getAsFloat() : Float.NaN));
			}
			Vec3 seatAt = null;
			if (o.has("seat_at") && !o.get("seat_at").isJsonNull()) {
				JsonArray a = o.getAsJsonArray("seat_at");
				seatAt = new Vec3(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble());
			}
			List<Rest> breaks = new ArrayList<>();
			if (o.has("breaks")) {
				for (JsonElement b : o.getAsJsonArray("breaks")) {
					breaks.add(rest(b.getAsJsonObject(), "mug"));
				}
			}
			Rest sleep = o.has("sleep") ? rest(o.getAsJsonObject("sleep"), null) : null;
			out.add(new CrewPost(o.get("id").getAsString(), o.get("job").getAsString(), o.get("kind").getAsString(),
					o.get("act").getAsString(), string(o, "prop"), pos(o, "seat"), pos(o, "stand"), List.copyOf(spots),
					o.has("step") ? o.get("step").getAsInt() : 0, seatAt, o.has("seat_yaw") ? o.get("seat_yaw").getAsFloat() : 0F,
					!o.has("night") || o.get("night").getAsBoolean(), sleep, List.copyOf(breaks), pos(o, "cart"), pos(o, "cart_stand")));
		}
		return List.copyOf(out);
	}

	private static Rest rest(JsonObject o, @Nullable String act) {
		JsonArray a = o.getAsJsonArray("seat_at");
		return new Rest(pos(o, "seat"), new Vec3(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()),
				o.get("seat_yaw").getAsFloat(), act != null ? act : o.get("act").getAsString(), pos(o, "stand"), o.get("from").getAsInt(),
				o.get("to").getAsInt(), o.has("days") ? o.get("days").getAsInt() : -1);
	}

	private static @Nullable String string(JsonObject o, String key) {
		return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
	}

	private static @Nullable BlockPos pos(JsonObject o, String key) {
		if (!o.has(key) || o.get(key).isJsonNull()) {
			return null;
		}
		JsonArray a = o.getAsJsonArray(key);
		return new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
	}
}
