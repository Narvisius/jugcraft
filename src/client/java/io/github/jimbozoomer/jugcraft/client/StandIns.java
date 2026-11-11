package io.github.jimbozoomer.jugcraft.client;

import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/**
 * Entities made on the client only to be drawn (a seat's rider, a frozen copy, a creature in a preview), never added
 * to the world. Since 26.x an entity has no id until the world gives it one, and asking for it throws ("Tried to access
 * entity ID before ID assignment"); drawing a living thing asks (its held items are resolved by it). Justin's game
 * closed on it as a drop ship came in to land, 10 November 2026: the ship's seated crew are drawn from one stand-in
 * (CrewShipRenderer.riders). Every stand-in gets an id of its own from here, below any the server hands out.
 * tools/check_mod_data.py (check_stand_ins) keeps to it: client code that creates an entity gives it an id, here or
 * with setId, or adds it to the world.
 */
public final class StandIns {
	private static final AtomicInteger NEXT = new AtomicInteger(Integer.MIN_VALUE / 2);

	private StandIns() {
	}

	/** The entity, with an id if it is one (null passes through: a type that can't be made). */
	public static <T extends Entity> @Nullable T id(@Nullable T entity) {
		if (entity != null) {
			entity.setId(NEXT.getAndDecrement());
		}
		return entity;
	}
}
