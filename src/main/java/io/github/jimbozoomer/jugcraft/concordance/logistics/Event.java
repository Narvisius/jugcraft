package io.github.jimbozoomer.jugcraft.concordance.logistics;

/**
 * One line of a post's request history (roadmap step 18): at game time {@code time}, request {@code request} was
 * {@code kind} (filed, claimed, reserved, picked_up, taken_up, delivered, done, released, stranded, returning,
 * returned, cancelled, recovered), for {@code amount} items, with a note (a reason or a place).
 */
public record Event(long time, long request, String kind, int amount, String note) {
}
