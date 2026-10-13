package io.github.jimbozoomer.jugcraft.concordance.sky;

import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.celestial.Attunement;
import io.github.jimbozoomer.jugcraft.concordance.celestial.Calendar;
import io.github.jimbozoomer.jugcraft.concordance.celestial.Pattern;
import io.github.jimbozoomer.jugcraft.concordance.resource.AstralLedger;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * The Astrolabe (roadmap step 15): carries up to {@value #CAPACITY} Astral Resonance (component
 * {@code jugcraft:astral_charge}), drawn from an observatory. Used under the open sky while a pattern is up, it attunes
 * its holder to it (Celestial Attunement understood): its cost in resonance, and its effect is theirs while the pattern
 * is up, at most eight minutes. When nothing is up, a master may <b>recall</b> a pattern they have observed: dearer,
 * shorter ({@link Calendar#RECALL_TICKS} ticks), and once per occurrence of that pattern, by the same monotonic ledger,
 * so a recall never stands in for more alignments than the sky has had.
 */
public class AstrolabeItem extends Item {
	public static final int CAPACITY = 32;

	public AstrolabeItem(Properties properties) {
		super(properties);
	}

	public static int charge(ItemStack stack) {
		Integer charge = stack.get(Sky.ASTRAL_CHARGE);
		return charge == null ? 0 : Math.clamp(charge, 0, CAPACITY);
	}

	/** Sets its resonance; one holding any is {@code jugcraft:resonant}, which an optional dynamic light makes glow. */
	public static void setCharge(ItemStack stack, int charge) {
		int clamped = Math.clamp(charge, 0, CAPACITY);
		stack.set(Sky.ASTRAL_CHARGE, clamped);
		if (clamped > 0) {
			stack.set(Sky.RESONANT, Unit.INSTANCE);
		} else {
			stack.remove(Sky.RESONANT);
		}
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(player instanceof ServerPlayer server) || !(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.SUCCESS;
		}
		if (!RateGate.allow(server, "sky", 10)) {
			return InteractionResult.FAIL;
		}
		return attune(server, serverLevel, player.getItemInHand(hand), Sky.time(level), level.getGameTime());
	}

	/** Attunes {@code player} under the real sky where they stand, at the level's own times. */
	public static InteractionResult attune(ServerPlayer player, ServerLevel level, ItemStack astrolabe, long time, long gameTime) {
		return attune(player, level, astrolabe, time, gameTime, pattern -> Sky.obscured(level, player.blockPosition(), pattern));
	}

	/**
	 * Attunes {@code player} at world time {@code time} and game time {@code gameTime}, with {@code sky} saying why a
	 * pattern is hidden from them (null when they can see it; tests pass their own, as for the observatory): to a pattern
	 * up and visible, for as long as it stays up but at most {@link Calendar#MAX_ATTUNEMENT_TICKS} ticks; or else, for a
	 * master, by recalling the pattern they last observed.
	 */
	public static InteractionResult attune(ServerPlayer player, ServerLevel level, ItemStack astrolabe, long time, long gameTime,
			Function<Pattern, @Nullable String> sky) {
		if (!Sky.enabled()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.disabled"));
			return InteractionResult.FAIL;
		}
		ResearchState state = Sky.state(player);
		if (!state.atLeast(ResearchState.UNDERSTOOD)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.sky.unknown"));
			return InteractionResult.FAIL;
		}
		int held = charge(astrolabe);
		for (Pattern pattern : Sky.up(time)) {
			if (sky.apply(pattern) != null) {
				continue;
			}
			if (held < pattern.attuneCost()) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.sky.too_little", pattern.attuneCost()));
				return InteractionResult.FAIL;
			}
			Calendar.Window window = Calendar.next(pattern, time);
			setCharge(astrolabe, held - pattern.attuneCost());
			long lasts = Math.min(Math.max(0L, window.end() - time), Calendar.MAX_ATTUNEMENT_TICKS);
			player.setAttached(Sky.ATTUNEMENT, new Attunement(pattern.id(), gameTime + lasts, false));
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.sky.attuned", Sky.patternName(pattern.id())));
			return InteractionResult.SUCCESS;
		}
		// Nothing up: a recall of the pattern last observed, for a master, once per occurrence.
		if (!state.atLeast(ResearchState.MASTERED)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.sky.nothing_up"));
			return InteractionResult.FAIL;
		}
		Pattern recalled = lastObserved(level, player);
		if (recalled == null) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.sky.nothing_observed"));
			return InteractionResult.FAIL;
		}
		if (held < recalled.recallCost()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.sky.too_little", recalled.recallCost()));
			return InteractionResult.FAIL;
		}
		AstralLedger.Outcome outcome = AstralClaims.of(level.getServer()).claim(player.getUUID(), "recall:" + recalled.id(),
				Calendar.occurrence(recalled, time), gameTime, Calendar.minGap(recalled));
		if (outcome != AstralLedger.Outcome.GRANTED) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.sky.recalled_already", Sky.patternName(recalled.id())));
			return InteractionResult.FAIL;
		}
		setCharge(astrolabe, held - recalled.recallCost());
		player.setAttached(Sky.ATTUNEMENT, new Attunement(recalled.id(), gameTime + Calendar.RECALL_TICKS, true));
		player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.sky.recalled", Sky.patternName(recalled.id())));
		return InteractionResult.SUCCESS;
	}

	/** The pattern the player observed most recently (by game time), if it is still loaded. */
	private static @Nullable Pattern lastObserved(ServerLevel level, ServerPlayer player) {
		Pattern best = null;
		long when = Long.MIN_VALUE;
		for (Map.Entry<String, Long> entry : AstralClaims.of(level.getServer()).observed(player.getUUID()).entrySet()) {
			Pattern pattern = Sky.catalog().pattern(entry.getKey());
			if (pattern != null && entry.getValue() > when) {
				best = pattern;
				when = entry.getValue();
			}
		}
		return best;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.jugcraft.concordance.astrolabe.charge", charge(stack), CAPACITY)
				.withStyle(ChatFormatting.AQUA));
		tooltip.accept(Component.translatable("tooltip.jugcraft.astrolabe").withStyle(ChatFormatting.GRAY));
	}
}
