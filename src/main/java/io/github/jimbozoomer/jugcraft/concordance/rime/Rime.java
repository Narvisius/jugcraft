package io.github.jimbozoomer.jugcraft.concordance.rime;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.CrucibleBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Band;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectKind;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Rime, the Rimekeepers' Principle: cold, stillness and preservation (docs/features/arcane-concordance-rime-strata.md,
 * tools/concordance_rime.py). Rimekeeping is mastered by kinds of cold work, each counted once ({@link #practise}), with
 * one elective kind among them ({@link #KEY_ELECTIVE}), done by a Frost boon arm's slow ({@link #frostBoon}) or by the
 * Yeti King's charge broken on an ice column ({@link #column});
 * Preserve, an alteration in Rime's school ({@link #SCHOOL}), holds an Alembic Crucible's heat in its band instead of
 * putting out fire ({@link #preserves}, {@link #preserve}): never an inventory, an item, food or a machine, and never a
 * block changed. Rimebind is {@link Rimebind}; the winter survey {@link RimeSurvey}. Frostward, Winter Brine's status
 * (Concordance Chunk B, B-08), is {@link Frostward}; the Rimekeeper's travel kit (B-10) is {@link RimeGear}.
 */
public final class Rime {
	/** The practice Rimekeeping is mastered by (tools/concordance_rime.py RIMEWORK). */
	public static final String ACTIVITY = "jugcraft:rimework";
	/** Its gate (data/jugcraft/concordance/practice/rimework.json). */
	public static final String RESEARCH = "jugcraft:rimekeeping";
	public static final String KEY_PRESERVE_COLD = "preserve:cold";
	public static final String KEY_PRESERVE_HEATED = "preserve:heated";
	public static final String KEY_RESTRAINT = "restraint";
	/**
	 * Elective cold work, one kind however it is done (B-P26, B-P53, B-P56; 02:137): a hostile creature slowed by a Frost
	 * boon arm ({@link #frostBoon}), or the Yeti King's charge broken on an ice column its player drew it into
	 * ({@link #column}). One key between them, so together they stand for at most one kind and every mastery still needs
	 * a Preserve, so the Alembic Arts.
	 */
	public static final String KEY_ELECTIVE = "elective";
	/** Rime's Spell Power school: an alteration in it holds a crucible instead of putting out fire. */
	public static final String SCHOOL = "spell_power:frost";
	/** How long Preserve holds a band: 60 seconds (02:77; tools/concordance_rime.py PRESERVE_HOLD_TICKS). */
	public static final int HOLD_TICKS = 1200;
	/** The longest hold a crucible keeps: the effect boundary's longest time (unit 4's water foci stay within it). */
	public static final int MAX_HOLD_TICKS = EffectSpec.MAX_DURATION;
	/** What Preserve may hold: the Alembic Crucible only (#jugcraft:concordance/preservable). */
	public static final TagKey<Block> PRESERVABLE = TagKey.create(Registries.BLOCK, Jugcraft.id("concordance/preservable"));

	private Rime() {
	}

	/** Registered whatever the concordance switch: a saved effect needs its id. */
	public static void register() {
		Rimebind.register();
		RimeSurvey.register();
		Frostward.register();
		RimeGear.register(); // Chunk B unit 4 (B-10): the travel kit
	}

	public static boolean understands(Player player) {
		return ConcordanceProgress.knowledge(player).state(RESEARCH).atLeast(ResearchState.UNDERSTOOD);
	}

	/** One kind of cold work for {@code person}, once they understand Rimekeeping; true if it taught anything new. */
	public static boolean practise(@Nullable ServerPlayer person, String key) {
		return person != null && understands(person)
				&& ConcordanceProgress.record(person, new Evidence.Practiced(ACTIVITY, key)).recorded();
	}

	/**
	 * A Frost boon arm's slow took on {@code target} (weapons/ArmItem.boon; B-P26): restraint by a cold arm, Rimekeeping's
	 * elective cold work ({@link #KEY_ELECTIVE}, the kind the Yeti King's column also records, B-P56), for a wielder who
	 * understands Rimekeeping, on the living creatures Rimebind's restraint counts ({@link Rimebind#restrains}; a killing
	 * blow restrains nothing). It only records: the slow is the boon's own, with its own refresh and no thaw (B-P10), and
	 * nothing here changes it (B-P28). Nothing while the Concordance is switched off: arms work whatever the switch.
	 * Returns whether it taught anything new.
	 */
	public static boolean frostBoon(LivingEntity wielder, LivingEntity target) {
		return JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE) && wielder instanceof ServerPlayer person
				&& target.isAlive() && Rimebind.restrains(target) && practise(person, KEY_ELECTIVE);
	}

	/**
	 * The Yeti King's Avalanche Charge broke on an ice column (lair/yeti/YetiKingEntity.baited, for the player its column
	 * deed names; Concordance Chunk B, B-09; 02:137): Rimekeeping's elective cold work for {@code baiter}, if they
	 * understand Rimekeeping (the practice's gate). It records the elective kind a Frost boon arm's slow records
	 * ({@link #KEY_ELECTIVE}, B-P56), so after either the other adds nothing. It only records, once (the key). Nothing
	 * while the Concordance is switched off: the lairs work whatever the switch. Returns whether it taught anything new.
	 */
	public static boolean column(ServerPlayer baiter) {
		return JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE) && practise(baiter, KEY_ELECTIVE);
	}

	/** The cold work a Preserve on a crucible in {@code band} is: cold, or heated (warm, hot or searing). */
	public static String preserveKey(Band band) {
		return band == Band.COLD ? KEY_PRESERVE_COLD : KEY_PRESERVE_HEATED;
	}

	/** Whether an effect is Preserve's alteration. */
	public static boolean preserves(EffectSpec spec) {
		return spec.kind() == EffectKind.ALTERATION && SCHOOL.equals(spec.school());
	}

	/** Whether Preserve can hold the block at {@code pos}: a listed block that is an Alembic Crucible. */
	public static boolean preservable(ServerLevel level, BlockPos pos, BlockState state) {
		return state.is(PRESERVABLE) && level.getBlockEntity(pos) instanceof CrucibleBlockEntity;
	}

	/**
	 * Holds the crucible at {@code pos} in its band for {@link #HOLD_TICKS}, or longer with a focus of water worn
	 * ({@link RimeGear#holdTicks}, B-P24). The effect boundary has already asked whether the person behind it may change
	 * that block (only ConcordanceEffects calls this, tools/check_mod_data.py). A crucible holding a mixture counts as cold
	 * work for {@code person}. Returns whether anything was held.
	 */
	public static boolean preserve(ServerLevel level, @Nullable ServerPlayer person, BlockPos pos) {
		if (!preservable(level, pos, level.getBlockState(pos)) || !(level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible)) {
			return false;
		}
		crucible.hold(ConcordanceProgress.now(level), RimeGear.holdTicks(person));
		level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6F, 1.6F);
		if (crucible.parts() > 0) {
			practise(person, preserveKey(crucible.band()));
		}
		return true;
	}
}
