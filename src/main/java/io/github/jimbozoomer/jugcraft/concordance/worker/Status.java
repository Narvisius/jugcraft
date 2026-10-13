package io.github.jimbozoomer.jugcraft.concordance.worker;

import org.jspecify.annotations.Nullable;

/**
 * What a familiar, spirit or construct is doing, or exactly why it is not (roadmap step 17). Every worker always has
 * one; Jade, its use with an empty hand and the {@code workers} command show it, so a player can tell whether it is
 * waiting for resources, blocked by access, unable to navigate, outside its agreement or finished.
 */
public enum Status {
	// Intents: it is doing something.
	FOLLOWING("following", true),
	SUPPORTING("supporting", true),
	TRAVELLING("travelling", true),
	WORKING("working", true),
	RETURNING("returning", true),
	// Reasons it is not.
	IDLE("idle", false),
	WAITING_FOR_RESOURCES("waiting_for_resources", false),
	BLOCKED_BY_ACCESS("blocked_by_access", false),
	CANNOT_NAVIGATE("cannot_navigate", false),
	OUTSIDE_AGREEMENT("outside_agreement", false),
	FINISHED("finished", false),
	OWNER_OFFLINE("owner_offline", false),
	OTHER_DIMENSION("other_dimension", false),
	DESTINATION_UNLOADED("destination_unloaded", false),
	SUSPENDED("suspended", false),
	NEEDS_REPAIR("needs_repair", false),
	NO_ENERGY("no_energy", false),
	FULL("full", false),
	DISABLED("disabled", false);

	public final String id;
	/** Whether it is acting (an intent) rather than waiting (a reason). */
	public final boolean active;

	Status(String id, boolean active) {
		this.id = id;
		this.active = active;
	}

	public static @Nullable Status fromId(String id) {
		for (Status status : values()) {
			if (status.id.equals(id)) {
				return status;
			}
		}
		return null;
	}
}
