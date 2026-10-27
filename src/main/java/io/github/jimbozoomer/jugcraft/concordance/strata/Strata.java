package io.github.jimbozoomer.jugcraft.concordance.strata;

import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.Rituals;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.gear.ThalliteGear;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Strata (tools/concordance_strata.py): Stratawrighting's practice, ways of anchoring counted once by key. Brace is {@link Brace}. */
public final class Strata {
	public static final String ACTIVITY = "jugcraft:anchoring";
	public static final String RESEARCH = "jugcraft:stratawrighting";
	public static final String KEY_LIVING = "ground:living";
	public static final String KEY_EARTHEN = "ground:earthen";
	public static final String KEY_HELD = "held";
	public static final String KEY_ALLY = "ally";
	public static final String KEY_FOUNDATION = "foundation";
	public static final String KEY_ROOTED = "rooted";

	private Strata() {
	}

	public static void register() {
		Brace.register();
		Rituals.listen((level, anchor, ritual, present) -> present.forEach(player -> practise(player, KEY_FOUNDATION)));
		ServerLivingEntityEvents.AFTER_DAMAGE.register(Strata::struck);
	}

	public static boolean understands(Player player) {
		return ConcordanceProgress.knowledge(player).state(RESEARCH).atLeast(ResearchState.UNDERSTOOD);
	}

	public static boolean practise(@Nullable ServerPlayer person, String key) {
		return person != null && understands(person)
				&& ConcordanceProgress.record(person, new Evidence.Practiced(ACTIVITY, key)).recorded();
	}

	/** Living ground, other natural (earthen) ground, or null (worked blocks, air, water). ThalliteGear.java:44-46. */
	public static @Nullable String ground(@Nullable BlockState footing) {
		if (footing == null) {
			return null;
		}
		if (footing.is(ThalliteGear.LIVING_GROUND)) {
			return KEY_LIVING;
		}
		return footing.is(ThalliteGear.EARTHEN_GROUND) ? KEY_EARTHEN : null;
	}

	/** A Brace took on {@code target} (Brace.braced): its ground, and an ally braced, are the caster's practice. */
	static void braced(@Nullable ServerPlayer person, LivingEntity target) {
		if (person == null) {
			return;
		}
		String ground = ground(ThalliteGear.footing(target));
		if (ground != null) {
			practise(person, ground);
		}
		if (target instanceof ServerPlayer && target != person) {
			practise(person, KEY_ALLY);
		}
	}

	/** A Brace held against a push: the person who put it on (Brace.held). */
	static void held(@Nullable ServerPlayer caster) {
		practise(caster, KEY_HELD);
	}

	/**
	 * An Earthbound wearer struck by a creature on natural ground: Rooted holding (02:141; B-P26). Nothing while the
	 * Concordance is switched off: armour is worn whatever the switch.
	 */
	private static void struck(LivingEntity entity, DamageSource source, float base, float taken, boolean blocked) {
		if (!JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE)) {
			return;
		}
		if (entity instanceof ServerPlayer player && !blocked && source.getEntity() != null && ThalliteGear.earthbound(player) > 0
				&& ground(ThalliteGear.footing(player)) != null) {
			practise(player, KEY_ROOTED);
		}
	}
}
