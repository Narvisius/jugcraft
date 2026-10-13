package io.github.jimbozoomer.jugcraft.concordance.worker;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * A spirit's agreement (roadmap step 17), sealed as a Bound Will record ({@link #record}) held by {@link #holder}: the
 * one kind of work it agreed to, where (within {@link #radius} blocks of its anchor, in its anchor's dimension), when
 * (the world clock's hours {@link #from} to {@link #to}) and how much (at most {@link #quota} tasks a day of game time).
 * Its holder may suspend it (the spirit waits at its anchor) or release it (the spirit departs). A task outside any of
 * these terms is refused with the reason, never done.
 */
public record Agreement(UUID record, UUID holder, String work, String dimension, int x, int y, int z, int radius, int from, int to,
		int quota, long window, int doneToday, boolean suspended) {
	public static final long DAY = 24000L;

	/** Null when {@code task} at that place and time is within the terms; otherwise why not. */
	public @Nullable Status permits(String task, String dim, int px, int py, int pz, long time, long gameTime) {
		if (suspended) {
			return Status.SUSPENDED;
		}
		if (!task.equals(work) || !dim.equals(dimension) || !within(px, py, pz) || !inHours(time)) {
			return Status.OUTSIDE_AGREEMENT;
		}
		if (done(gameTime) >= quota) {
			return Status.FINISHED;
		}
		return null;
	}

	/** Whether a place is inside its area: within the radius across, and as far up or down. */
	public boolean within(int px, int py, int pz) {
		return Math.abs(px - x) <= radius && Math.abs(py - y) <= radius && Math.abs(pz - z) <= radius;
	}

	/** Whether the world clock's hour allows work (the hours may run past midnight). */
	public boolean inHours(long time) {
		int tick = (int) Math.floorMod(time, DAY);
		return from <= to ? tick >= from && tick < to : tick >= from || tick < to;
	}

	/** Tasks done in the day of game time {@code gameTime}. */
	public int done(long gameTime) {
		return Math.floorDiv(gameTime, DAY) == window ? doneToday : 0;
	}

	public Agreement didTask(long gameTime) {
		long day = Math.floorDiv(gameTime, DAY);
		return new Agreement(record, holder, work, dimension, x, y, z, radius, from, to, quota, day, done(gameTime) + 1, suspended);
	}

	public Agreement suspended(boolean value) {
		return new Agreement(record, holder, work, dimension, x, y, z, radius, from, to, quota, window, doneToday, value);
	}
}
