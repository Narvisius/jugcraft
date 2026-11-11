package io.github.jimbozoomer.jugcraft.tower.crew;

import io.github.jimbozoomer.jugcraft.tower.squad.Legionary;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * Who the tower's armed people take on of their own accord: the security officers and gate guards ({@link TowerCrew})
 * and the squads ({@link Legionary}). One rule for every mob there is, the game's and every mod's (Justin, 11 November
 * 2026: "it shouldn't really be a me thing. It should just be like a, hey, these are hostile mobs, let's kill them"):
 * <ul>
 * <li>a hostile mob ({@link Enemy}: every monster, slime, phantom, ghast, shulker, hoglin and the like), unless it's a
 * neutral one nobody has angered (an enderman, a zombified piglin);</li>
 * <li>any other mob that is going for a player (in survival), one of the crew or a soldier: whatever it is, it's a threat
 * now;</li>
 * <li>never a player (those only when one starts it: the crew's and the squad's own rules), the crew, a soldier, nor
 * anything tamed or owned by someone.</li>
 * </ul>
 */
public final class Hostiles {
	private Hostiles() {
	}

	/** Whether {@code e} is a hostile they go for unprovoked (see the class's note). */
	public static boolean hostile(LivingEntity e) {
		if (!e.isAlive() || e instanceof Player || e instanceof TowerCrew || e instanceof Legionary) {
			return false;
		}
		if (e instanceof OwnableEntity o && o.getOwnerReference() != null) {
			return false;                           // a pet, a summoned helper: someone's
		}
		if (e instanceof Enemy) {
			return !(e instanceof NeutralMob n && !n.isAngry()) || attacking(e);
		}
		return attacking(e);
	}

	/** Whether {@code e} is a mob going for a player (not in creative or spectating), one of the crew or a soldier. */
	public static boolean attacking(LivingEntity e) {
		if (!(e instanceof Mob m)) {
			return false;
		}
		LivingEntity t = m.getTarget();
		return t != null && t.isAlive() && (t instanceof Player p && !p.isCreative() && !p.isSpectator() || t instanceof TowerCrew || t instanceof Legionary);
	}
}
