package io.github.jimbozoomer.jugcraft.concordance.hex;

import java.util.ArrayList;
import java.util.List;

/** A ward on its bearer (roadmap step 22): the category it stops and when it ends, in game time. */
public record Ward(WardCategory category, long until) {
	/** How long a ward sigil's ward lasts: twenty minutes. */
	public static final long TICKS = 24_000L;
	/** The most wards one bearer holds: one of each category. */
	public static final int MAX = WardCategory.values().length;

	public boolean active(long now) {
		return now < until;
	}

	/** Whether {@code wards} hold an active ward of {@code category} at {@code now}. */
	public static boolean guards(List<Ward> wards, WardCategory category, long now) {
		for (Ward ward : wards) {
			if (ward.category() == category && ward.active(now)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * {@code wards} with {@code ward} added: one of each category, the later end kept (a fresh sigil renews, never
	 * stacks); expired ones dropped.
	 */
	public static List<Ward> with(List<Ward> wards, Ward ward, long now) {
		List<Ward> next = new ArrayList<>();
		long until = ward.until();
		for (Ward held : wards) {
			if (!held.active(now)) {
				continue;
			}
			if (held.category() == ward.category()) {
				until = Math.max(until, held.until());
			} else {
				next.add(held);
			}
		}
		next.add(new Ward(ward.category(), until));
		return List.copyOf(next);
	}
}
