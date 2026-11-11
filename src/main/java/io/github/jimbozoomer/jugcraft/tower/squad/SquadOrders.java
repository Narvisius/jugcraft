package io.github.jimbozoomer.jugcraft.tower.squad;

import io.github.jimbozoomer.jugcraft.tower.JugcraftTower;
import io.github.jimbozoomer.jugcraft.tower.crew.CrewShip;
import io.github.jimbozoomer.jugcraft.tower.crew.CrewShipSite;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;

/**
 * The leader's orders (release (b); sheet 4's orders panel): his player uses him (right-click) for the panel
 * (client/SquadOrdersScreen: the squad's roster and health, its order, the patrol's radius) and picks FOLLOW ME, HOLD THIS
 * POSITION, PATROL AREA (in a radius of 50 to 400 blocks) or SEND HOME. Each is the squad's: given to every one of them
 * near ({@link Legionary#command}). SEND HOME isn't taken in a fight, nor with nowhere near for the ship to land; else a
 * drop ship is sent for them ({@link CrewShip#setUpPickup}, landing where {@link CrewShipSite#near} finds room).
 */
public final class SquadOrders {
	/** What became of an order (the panel says). */
	public static final int OK = 0, NOT_YOURS = 1, TOO_FAR = 2, IN_COMBAT = 3, NO_ZONE = 4, ON_THE_WAY = 5;
	/** How near (blocks) the player must be to the leader to give an order. */
	public static final double REACH = 10;

	private SquadOrders() {
	}

	/** The panel's notes for each result (message.jugcraft.squad.order.*), the first none. */
	static final String[] NOTES = {"", "not_yours", "too_far", "in_combat", "no_zone", "on_the_way"};

	/** {@code player} gives {@code leader} order {@code order} (patrolling: in a radius of {@code radius}): one of the
	 * results above. */
	public static int order(ServerPlayer player, Legionary leader, int order, int radius) {
		if (!player.getUUID().equals(leader.owner()) || !leader.leader()) {
			return NOT_YOURS;
		}
		if (player.distanceTo(leader) > REACH) {
			return TOO_FAR;
		}
		return order(player.level(), leader, order, radius, null);
	}

	/** As {@link #order(ServerPlayer, Legionary, int, int)}, but from the server's side (no player's checks); sent home,
	 * the ship lands at {@code site} if given (a game test's), else where there's room. */
	public static int order(ServerLevel level, Legionary leader, int order, int radius, CrewShipSite.@Nullable Site site) {
		List<Legionary> squad = leader.squadNear();
		Vec3 at = leader.position();
		if (order == Legionary.HOME) {
			if (leader.order() == Legionary.HOME && leader.anchor() != null && homeShip(level, leader) != null) {
				return ON_THE_WAY;
			}
			for (Legionary l : squad) {
				if (l.inCombat()) {
					return IN_COMBAT;
				}
			}
			CrewShipSite.Site s = site != null ? site : CrewShipSite.near(level, at, CrewShipSite.PICKUP_REACH);
			if (s == null) {
				return NO_ZONE;
			}
			CrewShip ship = JugcraftTower.CREW_SHIP.create(level, EntitySpawnReason.TRIGGERED);
			if (ship == null || leader.owner() == null || leader.squad() == null) {
				return NO_ZONE;
			}
			ship.setUpPickup(s, leader.owner(), leader.squad(), level.getGameTime());
			level.addFreshEntity(ship);
			for (Legionary l : squad) {
				l.command(Legionary.HOME, radius, at, ship.getUUID());
			}
			return OK;
		}
		int o = order == Legionary.HOLD || order == Legionary.PATROL ? order : Legionary.FOLLOW;
		for (Legionary l : squad) {
			l.command(o, radius, at, null);
		}
		return OK;
	}

	/** The ship sent for {@code leader}'s squad, still on its way or down. */
	private static @Nullable CrewShip homeShip(ServerLevel level, Legionary leader) {
		for (CrewShip s : level.getEntitiesOfClass(CrewShip.class, leader.getBoundingBox().inflate(CrewShipSite.PICKUP_REACH + 40), CrewShip::isAlive)) {
			if (s.pickup() && leader.squad() != null && leader.squad().equals(s.pickupSquad())) {
				return s;
			}
		}
		return null;
	}

	/** Sends {@code player} {@code leader}'s panel (opening it, or a refresh with a note on their last order). */
	public static void page(ServerPlayer player, Legionary leader, boolean open, String note) {
		List<Legionary> squad = leader.squadNear();
		squad.sort((a, b) -> a.leader() != b.leader() ? (a.leader() ? -1 : 1) : Integer.compare(a.place(), b.place()));
		List<String> roster = new ArrayList<>();
		boolean fight = false;
		for (Legionary l : squad) {
			roster.add(l.rankKey() + ";" + l.given() + ";" + Math.round(100 * l.getHealth() / l.getMaxHealth()));
			fight |= l.inCombat();
		}
		int size = JugcraftSquads.SIZES[Math.max(0, Math.min(JugcraftSquads.SIZES.length - 1, leader.squadIndex()))];
		ServerPlayNetworking.send(player, new SquadPayloads.Orders(leader.getId(), leader.rankKey() + ";" + leader.given(), size, roster,
				leader.order(), leader.radius(), fight, note, leader.recallAt(), open));
	}
}
