package io.github.jimbozoomer.jugcraft.client.tv;

import java.util.List;

/**
 * A playlist as a screen plays it, one video after another (docs/features/flat-screen-tv.md): the playlist's videos
 * from the one the link names, on to the end of the list (and, on replay, round from its top again). The screen's
 * clock is the playlist's: from where it is ({@link #at}) every game works out the same video and the same moment in
 * it, as they do for a single video, from the lengths YouTube lists.
 */
final class TvProgram {
	/** One of the playlist's videos: its id, its link, its title and its length. */
	record Entry(String id, String url, String title, long durationMs) {
	}

	/**
	 * Where the playlist is: the video ({@code k} in play order; {@code number} its place in the list, from 1), how far
	 * into it, where it started on the screen's clock; {@code ended} past the last video (no replay).
	 */
	record At(int k, Entry entry, int number, long offset, long entryStart, boolean ended) {
	}

	final String title;
	final List<Entry> entries;
	final int start;
	/** The videos from {@link #start} to the end of the list, and how long they run; and the whole list's length. */
	private final int toEnd;
	private final long toEndMs, totalMs;

	TvProgram(String title, List<Entry> entries, int start) {
		this.title = title;
		this.entries = List.copyOf(entries);
		this.start = Math.max(0, Math.min(start, entries.size() - 1));
		toEnd = entries.size() - this.start;
		long sum = 0, all = 0;
		for (int i = 0; i < entries.size(); i++) {
			all += entries.get(i).durationMs();
			if (i >= this.start) {
				sum += entries.get(i).durationMs();
			}
		}
		toEndMs = sum;
		totalMs = all;
	}

	int size() {
		return entries.size();
	}

	/** Whether a video follows the k-th in play order: on replay always (round the list), else up to the list's end. */
	boolean hasNext(int k, boolean loop) {
		return entries.size() > 1 && (loop || k + 1 < toEnd);
	}

	/** The k-th video in play order (round the list). */
	Entry entry(int k) {
		return entries.get((start + k) % entries.size());
	}

	/** Where the playlist is at {@code positionMs} on the screen's clock; on {@code loop} round and round the whole list. */
	At at(long positionMs, boolean loop) {
		long p = Math.max(0, positionMs);
		boolean ended = false;
		if (loop && totalMs > 0) {
			// round the list from the starting video: the videos to the end, then the list from its top, and so on
			if (p >= toEndMs) {
				p = toEndMs + (p - toEndMs) % totalMs;
			}
		} else if (p >= toEndMs) {
			Entry last = entry(toEnd - 1);
			return new At(toEnd - 1, last, (start + toEnd - 1) % entries.size() + 1, last.durationMs(), toEndMs - last.durationMs(), true);
		}
		long base = 0;
		for (int k = 0; ; k++) {
			Entry e = entry(k);
			if (p < base + e.durationMs() || k > 4 * entries.size()) {
				// (where it started on the screen's clock: the same moment less how far into it)
				return new At(k, e, (start + k) % entries.size() + 1, p - base, positionMs - (p - base), ended);
			}
			base += e.durationMs();
		}
	}
}
