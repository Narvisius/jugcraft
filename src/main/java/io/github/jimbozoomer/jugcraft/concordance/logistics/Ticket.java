package io.github.jimbozoomer.jugcraft.concordance.logistics;

import java.util.UUID;

/**
 * What a request asks for, fixed when it is filed (roadmap step 18): who asked, at which post, delivered where, which
 * item ({@code item} is its key: the item and every component it carries, so a named or enchanted item is never
 * mistaken for a plain one) and how many.
 */
public record Ticket(long id, UUID requester, Place post, Place destination, String item, int wanted, long filed) {
}
