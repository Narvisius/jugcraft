package io.github.jimbozoomer.jugcraft.town;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

/**
 * Keeps the town's people: a townsperson appears at each place of tools/town.py when the chunk with that place is
 * built, and if one goes missing (only {@code /kill} can do it), a new one appears there the next time the place is
 * in a loaded, ticking area. The town remembers who stands for each place, so there is never more than one.
 */
public final class TownsfolkCare {
	/** How often (ticks) missing townsfolk are looked for. */
	public static final int CHECK_TICKS = 1200;

	private TownsfolkCare() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(TownsfolkCare::tick);
	}

	/** Brings out the townsfolk whose places are in a chunk just built. */
	public static void chunkBuilt(ServerLevel level, ChunkPos chunk) {
		TownState state = TownState.get(level);
		BlockPos origin = state.origin();
		if (origin == null) {
			return;
		}
		for (TownData.Spot place : TownData.get().spots) {
			BlockPos pos = origin.offset(place.pos());
			if ((pos.getX() >> 4) == chunk.x() && (pos.getZ() >> 4) == chunk.z() && state.townsperson(place.index()) == null) {
				spawn(level, state, place, pos);
			}
		}
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % CHECK_TICKS != 0) {
			return;
		}
		ServerLevel level = server.overworld();
		TownState state = TownState.get(level);
		BlockPos origin = state.origin();
		if (origin == null) {
			return;
		}
		for (TownData.Spot place : TownData.get().spots) {
			BlockPos pos = origin.offset(place.pos());
			UUID id = state.townsperson(place.index());
			if (id == null || level.getEntity(id) != null || !level.isPositionEntityTicking(pos)
					|| !state.built(ChunkPos.containing(pos).pack())) {
				continue;
			}
			// Missing: not loaded anywhere, and no one else stands for this place nearby either.
			boolean found = !level.getEntitiesOfClass(Townsfolk.class, new AABB(pos).inflate(128), t -> t.spot() == place.index()).isEmpty();
			if (!found) {
				spawn(level, state, place, pos);
			}
		}
	}

	private static void spawn(ServerLevel level, TownState state, TownData.Spot place, BlockPos pos) {
		Townsfolk person = JugcraftTown.TOWNSFOLK.create(level, EntitySpawnReason.STRUCTURE);
		if (person == null) {
			return;
		}
		person.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
		person.setUp(place, pos);
		if (level.addFreshEntity(person)) {
			state.setTownsperson(place.index(), person.getUUID());
		} else {
			Jugcraft.LOGGER.warn("Town: could not bring out {} at {}", place.name(), pos);
		}
	}
}
