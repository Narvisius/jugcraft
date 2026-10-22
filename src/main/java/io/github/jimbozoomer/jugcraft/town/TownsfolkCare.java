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
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (entity instanceof Townsfolk person && person.spot() >= 0 && entity.getRemovalReason() != null
                    && entity.getRemovalReason().shouldDestroy()) {
                TownState.get(level).removeTownsperson(person.spot(), person.getUUID());
            }
        });
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
			if ((pos.getX() >> 4) == chunk.x() && (pos.getZ() >> 4) == chunk.z()) {
                UUID id = state.townsperson(place.index());
                if (id == null) adoptOrSpawn(level, state, place, pos);
                else if (level.getEntity(id) instanceof Townsfolk person) person.refresh(place, pos);
			}
		}
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % CHECK_TICKS != 0) {
			return;
		}
        checkResidents(server.overworld());
    }

    public static void checkResidents(ServerLevel level) {
        TownState state = TownState.get(level);
		BlockPos origin = state.origin();
		if (origin == null) {
			return;
		}
		for (TownData.Spot place : TownData.get().spots) {
			BlockPos pos = origin.offset(place.pos());
			UUID id = state.townsperson(place.index());
			if (!level.isPositionEntityTicking(pos)
					|| !state.built(ChunkPos.containing(pos).pack())) {
				continue;
			}
            if (id != null) {
                if (level.getEntity(id) instanceof Townsfolk person) person.refresh(place, pos);
                // A recorded UUID in an unloaded bedroom is still a resident, not a vacancy.
                continue;
            }
            if (TownUpgrades.version(level, ChunkPos.containing(pos).pack()) < TownUpgrades.CURRENT
                && place.index() >= TownUpgrades.historical(TownUpgrades.version(level, ChunkPos.containing(pos).pack())).spots.size()) continue;
            adoptOrSpawn(level, state, place, pos);
        }
	}

    private static void adoptOrSpawn(ServerLevel level, TownState state, TownData.Spot place, BlockPos pos) {
        var found = level.getEntitiesOfClass(Townsfolk.class, new AABB(pos).inflate(128), t -> t.spot() == place.index());
        if (!found.isEmpty()) {
            var person = found.getFirst();
            state.setTownsperson(place.index(), person.getUUID());person.refresh(place, pos);
        } else spawn(level, state, place, pos);
    }

	private static void spawn(ServerLevel level, TownState state, TownData.Spot place, BlockPos pos) {
		Townsfolk person = (place.role().equals("mecha_guard") ? JugcraftTown.GUARD_MECHA : JugcraftTown.TOWNSFOLK).create(level, EntitySpawnReason.STRUCTURE);
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
