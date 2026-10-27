package io.github.jimbozoomer.jugcraft.concordance.rime;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Frostward ({@code jugcraft:frostward}; Concordance Chunk B, B-08): the status a Rime brew gives (Winter Brine,
 * tools/concordance_alchemy.py PROPERTIES["rime"]). While it lasts the creature is never frozen through: its own effect
 * tick clears whatever frost it has (the Yeti Mitten's check, lair/yeti/YetiMitten.java, done by each creature under the
 * status, so the work is one look at each such creature a tick), and the Yeti King's chill does not take
 * ({@code YetiKingEntity.chill}). Powder snow, the Frost Horn's arrival and a snow werewolf's frostbite leave no frost
 * past the creature's next tick (in powder snow, which adds a tick of frost after the effect tick, at most one tick of
 * frost); their other effects (the werewolf's Slowness, sinking in powder snow) are untouched. Timed as Ember's Fire
 * Resistance brew is (B-P19); milk ends it; the Mitten, held, stays the lasting answer (04:141). Registered whatever the
 * Concordance switch, so a saved Frostward keeps its id.
 */
public final class Frostward {
	/** Its id: the Rime property's status (data/jugcraft/concordance/property/rime.json). */
	public static final String STATUS = "jugcraft:frostward";
	public static Holder<MobEffect> FROSTWARD;

	private Frostward() {
	}

	/** The status: helpful, no attribute change; each tick it clears gathered frost. */
	private static final class Effect extends MobEffect {
		Effect() {
			super(MobEffectCategory.BENEFICIAL, 0x73DAD8);
		}

		@Override
		public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
			return true;
		}

		@Override
		public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
			if (entity.getTicksFrozen() > 0) {
				entity.setTicksFrozen(0);
			}
			return true; // it never ends itself: it lasts its time
		}
	}

	static void register() {
		FROSTWARD = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Jugcraft.id("frostward"), new Effect());
	}

	/** Whether {@code entity} is under Frostward now. */
	public static boolean warded(LivingEntity entity) {
		return FROSTWARD != null && entity.hasEffect(FROSTWARD);
	}
}
