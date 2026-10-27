package io.github.jimbozoomer.jugcraft.herobrine;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

/** Server calendar. November 3 remains an event day, including after the DST change. */
public record HerobrineSeason(LocalDate lastActiveDay, ZoneId zone) {
	public static final HerobrineSeason DEFAULT = new HerobrineSeason(LocalDate.of(2026, 11, 3),
			ZoneId.of("America/New_York"));
	public HerobrineSeason {
		Objects.requireNonNull(lastActiveDay);
		Objects.requireNonNull(zone);
	}
	public Instant quietStarts() { return lastActiveDay.plusDays(1).atStartOfDay(zone).toInstant(); }
	public boolean eventActive(Instant now) { return now.isBefore(quietStarts()); }
}
