package io.github.jimbozoomer.jugcraft.concordance.resource;

import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Who may take from and put into a container. An owner and the members they trust may always do both; anyone else may
 * put in only where the container is open for offerings ({@code publicInsert}, as a lamp anyone may top up), and may
 * never take out. A container with no owner (world-made, or from before ownership was recorded) is open to all.
 */
public record Ownership(@Nullable UUID owner, Set<UUID> members, boolean publicInsert) {
	public static final Ownership NONE = new Ownership(null, Set.of(), true);

	public Ownership {
		members = Set.copyOf(members);
	}

	public static Ownership of(UUID owner, boolean publicInsert) {
		return new Ownership(owner, Set.of(), publicInsert);
	}

	private boolean trusted(UUID actor) {
		return owner == null || owner.equals(actor) || members.contains(actor);
	}

	public boolean mayExtract(UUID actor) {
		return trusted(actor);
	}

	public boolean mayInsert(UUID actor) {
		return publicInsert || trusted(actor);
	}
}
