package io.github.jimbozoomer.jugcraft.concordance.resource;

import java.util.UUID;

/**
 * One Bound Will record: an agreement with a named counterpart (a spirit, a construct), held by one player. It has an
 * identity of its own and is never merged, split or counted as an amount; it moves only as itself, and only if its
 * terms allow it to change hands.
 */
public record BoundWill(UUID id, String agreement, String counterpart, UUID holder, long sealedAt, boolean transferable) {
	public BoundWill withHolder(UUID newHolder) {
		return new BoundWill(id, agreement, counterpart, newHolder, sealedAt, transferable);
	}
}
