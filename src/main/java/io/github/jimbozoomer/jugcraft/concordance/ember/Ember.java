package io.github.jimbozoomer.jugcraft.concordance.ember;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectKind;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jspecify.annotations.Nullable;

/**
 * Ember, the Hearthbinders' Principle: fire kept and used with control (docs/features/arcane-concordance-ember.md,
 * tools/concordance_ember.py). Its spells are ordinary Concordance invocations; what it adds to the shared effects is
 * small and listed here:
 * <ul>
 * <li>an alteration in Ember's Spell Power school ({@link #SCHOOL}) kindles a hearth (an unlit campfire, candle or
 * candle cake, as flint and steel would) instead of putting out fire ({@link #kindles}, {@link #kindle});</li>
 * <li>the Smoulder status keeps a creature alight while it lasts ({@link SmoulderEffect});</li>
 * <li>the hearthkeeping practice: each kind of hearth a player kindles counts once towards mastering Hearthbinding.</li>
 * </ul>
 * Fire is never placed: nothing here can spread to the world. Part 2, the Hearthbinder's regalia made from the owner's
 * fire art (the foci, the Fire Bangle and two fire sets, the Pyromaniac's and the Pyromancer's), is {@link EmberGear}.
 */
public final class Ember {
	/** The practice Hearthbinding is mastered by (tools/concordance_ember.py HEARTHKEEPING). */
	public static final String ACTIVITY = "jugcraft:hearthkeeping";
	/** The research the practice needs understood (its gate, data/jugcraft/concordance/practice/hearthkeeping.json). */
	public static final String RESEARCH = "jugcraft:hearthbinding";
	/** Ember's Spell Power school: an alteration in it kindles rather than douses. */
	public static final String SCHOOL = "spell_power:fire";

	/** The Smoulder status ({@code jugcraft:smoulder}). */
	public static Holder<MobEffect> SMOULDER;

	private Ember() {
	}

	public static void register() {
		SMOULDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Jugcraft.id("smoulder"), new SmoulderEffect());
		EmberGear.register();
	}

	/** Whether an effect is Ember's alteration: it kindles a hearth instead of putting out fire. */
	public static boolean kindles(EffectSpec spec) {
		return spec.kind() == EffectKind.ALTERATION && SCHOOL.equals(spec.school());
	}

	/** Whether {@code state} is a hearth Ember can kindle: unlit, and not under water. */
	public static boolean kindleable(BlockState state) {
		return CampfireBlock.canLight(state) || CandleBlock.canLight(state) || CandleCakeBlock.canLight(state);
	}

	/** The kind of hearth {@code state} is, as the practice counts it, or null if it is none. */
	public static @Nullable String hearth(BlockState state) {
		Block block = state.getBlock();
		if (block instanceof CandleCakeBlock) {
			return "candle_cake";
		}
		if (block instanceof CandleBlock) {
			return "candle";
		}
		if (state.is(Blocks.SOUL_CAMPFIRE)) {
			return "soul_campfire";
		}
		return block instanceof CampfireBlock ? "campfire" : null;
	}

	/**
	 * Kindles the hearth at {@code pos}, as flint and steel would; the caller has already asked whether the person
	 * behind it may change that block. Records the hearthkeeping practice for {@code person} once they understand
	 * Hearthbinding. Returns whether anything was kindled.
	 */
	public static boolean kindle(ServerLevel level, @Nullable ServerPlayer person, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!kindleable(state)) {
			return false;
		}
		String hearth = hearth(state);
		level.setBlock(pos, state.setValue(BlockStateProperties.LIT, true), Block.UPDATE_ALL_IMMEDIATE);
		level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.4F + 1.0F);
		level.gameEvent(person, GameEvent.BLOCK_CHANGE, pos);
		if (person != null && hearth != null && ConcordanceProgress.knowledge(person).state(RESEARCH).atLeast(ResearchState.UNDERSTOOD)) {
			ConcordanceProgress.record(person, new Evidence.Practiced(ACTIVITY, hearth));
		}
		return true;
	}
}
