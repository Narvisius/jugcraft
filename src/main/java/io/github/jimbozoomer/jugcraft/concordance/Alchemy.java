package io.github.jimbozoomer.jugcraft.concordance;

import io.github.jimbozoomer.jugcraft.concordance.alchemy.AlchemyCatalog;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Outcome;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Property;
import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import io.github.jimbozoomer.jugcraft.concordance.effect.Cause;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectKind;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.effect.Intent;
import io.github.jimbozoomer.jugcraft.concordance.effect.Ledger;
import io.github.jimbozoomer.jugcraft.concordance.effect.Stacking;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The server side of alchemy shared by the crucible and the brews it makes (roadmap step 13): who may work a crucible,
 * turning an outcome into a draught or salve, and applying one through the shared effect executor.
 */
public final class Alchemy {
	public static final String RESEARCH = "jugcraft:alembic_arts";
	/** The practice a bottled outcome records ({@code Evidence.Practiced}); distinct outcomes count for mastery. */
	public static final String ACTIVITY = "jugcraft:alchemy";
	public static final String SOURCE = "jugcraft:brew";

	private Alchemy() {
	}

	public static AlchemyCatalog catalog() {
		return ConcordanceData.rules().alchemy();
	}

	public static ResearchState state(Player player) {
		return ConcordanceProgress.knowledge(player).state(RESEARCH);
	}

	/** Whether the player may work a crucible: the Alembic Arts understood. */
	public static boolean knows(Player player) {
		return state(player).atLeast(ResearchState.UNDERSTOOD);
	}

	/** A draught or salve ({@code form}) carrying an outcome's effects. */
	public static ItemStack brew(Item form, Outcome outcome) {
		List<Brew.Dose> doses = new ArrayList<>();
		for (Outcome.Effect effect : outcome.effects()) {
			doses.add(new Brew.Dose(effect.property().status(), effect.amplifier(), effect.ticks(),
					effect.property().intent() == Intent.HARMFUL));
		}
		ItemStack stack = new ItemStack(form);
		stack.set(JugcraftConcordance.BREW, new Brew(doses, outcome.key()));
		return stack;
	}

	/**
	 * Applies a brew to {@code target} as one event: each dose a status effect through {@link ConcordanceEffects},
	 * bounded by the shared limits whatever the item says. {@code actor} is who applied it to someone else (a salve),
	 * so the friendly-fire rules hold; a draught drunk, or a salve on oneself, has none. Returns how many took effect.
	 */
	public static int apply(ServerLevel level, Brew brew, LivingEntity target, @Nullable Entity actor) {
		Cause cause = Cause.of(actor == null ? null : actor.getUUID(), Cause.Origin.POTION, SOURCE, ConcordanceEffects.nextSerial());
		Ledger ledger = new Ledger(new Ledger.Limits(1, Brew.MAX_EFFECTS * EffectKind.STATUS.work, 0));
		int applied = 0;
		for (int i = 0; i < brew.effects().size(); i++) {
			Brew.Dose dose = brew.effects().get(i);
			EffectSpec spec = new EffectSpec(EffectKind.STATUS, harmful(dose) ? Intent.HARMFUL : Intent.HELPFUL,
					Math.clamp(dose.amplifier(), 0, EffectSpec.MAX_AMPLIFIER), Math.clamp(dose.ticks(), 1, EffectSpec.MAX_DURATION),
					dose.status(), Stacking.STRONGEST, null);
			ConcordanceEffects.Context context = new ConcordanceEffects.Context(level, cause, actor, ledger, "dose" + i, target.position());
			if (ConcordanceEffects.apply(context, spec, target).applied()) {
				applied++;
			}
		}
		return applied;
	}

	/**
	 * Whether a dose harms (roadmap step 28): the server decides from the status itself (its category, or unknown), so an
	 * edited item cannot pass a harmful status off as helpful and slip past the friendly-fire rules; the item's own word
	 * can only make it stricter.
	 */
	public static boolean harmful(Brew.Dose dose) {
		Identifier id = Identifier.tryParse(dose.status());
		MobEffect effect = id == null ? null : BuiltInRegistries.MOB_EFFECT.getValue(id);
		return dose.harmful() || effect == null || effect.getCategory() == MobEffectCategory.HARMFUL;
	}

	/** Sends a list of the simulation's lines (an assay, an outcome's reasons) to a player. */
	public static void tell(ServerPlayer player, List<Text> lines) {
		for (Text line : lines) {
			player.sendSystemMessage(ComposeText.show(line));
		}
	}

	/** Whether {@code id} names the contaminant's property rather than an axis. */
	public static boolean contaminant(Property property) {
		return Property.CONTAMINANT.equals(property.id());
	}

	static Component name(String item) {
		return ComposeText.name(new Text.Ref("item", item));
	}
}
