package io.github.jimbozoomer.jugcraft.lair;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * One open instance of a lair: its slot, who has come in, and since when nobody has been inside. Instances live only
 * while the server runs: a restart closes them all, and anyone found inside a closed one is sent home.
 */
public final class LairInstance {
	public final Lair lair;
	public final int slot;
	public final UUID id = UUID.randomUUID();
	public final long opened;
	/** Everyone who has come in (online or not), for the party size. */
	final Set<UUID> members = new HashSet<>();
	/** The game time from which nobody has been inside, or -1. */
	long emptySince = -1L;
	/** Set when the lair's boss has fallen: the instance closes once everyone has left. */
	boolean ended;
	/** The gate open at the ritual's site (an entity, the Last Rites' mist), and until when the gate is open. */
	@Nullable UUID gate;
	long gateUntil;
	/** Where the ritual was performed, when its gate is a block there (the Cursed Spindle's wheel). */
	@Nullable GlobalPos site;
	boolean open = true;

	LairInstance(Lair lair, int slot, long opened) {
		this.lair = lair;
		this.slot = slot;
		this.opened = opened;
	}

	public BlockPos origin() {
		return lair.origin(slot);
	}

	/** Where players arrive, in the world. */
	public Vec3 arrival() {
		BlockPos origin = origin();
		return new Vec3(origin.getX() + lair.arrival.x, origin.getY() + lair.arrival.y, origin.getZ() + lair.arrival.z);
	}

	/** The template's box, with room above and below for anything thrown about. */
	public AABB box() {
		BlockPos origin = origin();
		return new AABB(origin.getX() - 32, origin.getY() - 64, origin.getZ() - 64, origin.getX() + lair.width + 32,
				origin.getY() + lair.height + 64, origin.getZ() + lair.length + 32);
	}

	public boolean isOpen() {
		return open;
	}

	public boolean ended() {
		return ended;
	}

	public @Nullable GlobalPos site() {
		return site;
	}

	/** The game time until which the ritual's gate is open. */
	public long gateUntil() {
		return gateUntil;
	}

	public Set<UUID> members() {
		return Set.copyOf(members);
	}

	/** Whether {@code player} may come in: they have been in before, or the party ({@code lairs.party_size}) has room. */
	public boolean hasRoom(UUID player) {
		return members.contains(player) || members.size() < Lairs.partySize();
	}

	/** Counts {@code player} into the party. */
	public void admit(UUID player) {
		members.add(player);
	}
}
