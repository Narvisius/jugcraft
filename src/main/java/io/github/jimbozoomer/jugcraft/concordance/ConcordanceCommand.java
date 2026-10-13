package io.github.jimbozoomer.jugcraft.concordance;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.jimbozoomer.jugcraft.concordance.compose.Authored;
import io.github.jimbozoomer.jugcraft.concordance.compose.Compiler;
import io.github.jimbozoomer.jugcraft.concordance.compose.CompositionParser;
import io.github.jimbozoomer.jugcraft.concordance.compose.Instrument;
import io.github.jimbozoomer.jugcraft.concordance.compose.Plan;
import io.github.jimbozoomer.jugcraft.concordance.compose.Slot;
import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import io.github.jimbozoomer.jugcraft.concordance.journal.Journal;
import io.github.jimbozoomer.jugcraft.concordance.rules.ConcordanceRules;
import io.github.jimbozoomer.jugcraft.concordance.rules.Definitions;
import io.github.jimbozoomer.jugcraft.concordance.rules.FocusPool;
import io.github.jimbozoomer.jugcraft.concordance.rules.Knowledge;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * {@code /jugcraft concordance}: anyone can see their own research ({@code status}), compose spells for the
 * instrument in their main hand ({@code compose check|inscribe <spell>}, {@code compose show}, {@code compose clear}):
 * the composer every player has, which explains what a spell does and costs or exactly why it cannot work, and tune
 * the invocations they know on that instrument ({@code tune}, {@code tune <invocation> <modifier>|clear}), and read
 * the circle round a Circle Anchor ({@code circle <pos>}). Operators
 * (permission level 2) can see anyone's research, list the problems found in the loaded rules ({@code diagnose}), and
 * for testing and support set a research state ({@code grant}), forget a player's research ({@code reset}) or set their
 * Focus ({@code focus}).
 */
public final class ConcordanceCommand {
	/** The most often a player may check or inscribe a composition. Keep equal to COMPOSE_RATE_TICKS in tools/concordance.py. */
	public static final int COMPOSE_RATE_TICKS = 20;
	/** How near an anchor a player must be to read its circle with {@code circle}. */
	public static final int CIRCLE_READ_RANGE = 16;

	private ConcordanceCommand() {
	}

	static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("jugcraft").then(Commands.literal("concordance")
				.then(Commands.literal("status")
						.executes(context -> status(context.getSource(), context.getSource().getPlayerOrException()))
						.then(Commands.argument("player", EntityArgument.player()).requires(ConcordanceCommand::isOperator)
								.executes(context -> status(context.getSource(), EntityArgument.getPlayer(context, "player")))))
				.then(Commands.literal("compose")
						.then(Commands.literal("check").then(Commands.argument("spell", StringArgumentType.greedyString())
								.suggests(ConcordanceCommand::suggestComponents)
								.executes(context -> compose(context.getSource(), StringArgumentType.getString(context, "spell"), false))))
						.then(Commands.literal("inscribe").then(Commands.argument("spell", StringArgumentType.greedyString())
								.suggests(ConcordanceCommand::suggestComponents)
								.executes(context -> compose(context.getSource(), StringArgumentType.getString(context, "spell"), true))))
						.then(Commands.literal("show").executes(context -> show(context.getSource())))
						.then(Commands.literal("clear").executes(context -> clear(context.getSource()))))
				.then(Commands.literal("tune")
						.executes(context -> tunings(context.getSource()))
						.then(Commands.argument("invocation", IdentifierArgument.id())
								.suggests(ConcordanceCommand::suggestInvocations)
								.then(Commands.literal("clear")
										.executes(context -> tune(context.getSource(), IdentifierArgument.getId(context, "invocation").toString(), null)))
								.then(Commands.argument("modifier", IdentifierArgument.id())
										.suggests(ConcordanceCommand::suggestTunings)
										.executes(context -> tune(context.getSource(), IdentifierArgument.getId(context, "invocation").toString(),
												IdentifierArgument.getId(context, "modifier").toString())))))
				.then(Commands.literal("circle").then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(context -> circle(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos")))))
				.then(Commands.literal("diagnose").requires(ConcordanceCommand::isOperator)
						.executes(context -> diagnose(context.getSource())))
				.then(Commands.literal("grant").requires(ConcordanceCommand::isOperator)
						.then(Commands.argument("player", EntityArgument.player())
								.then(Commands.argument("research", IdentifierArgument.id())
										.suggests((context, builder) -> {
											ConcordanceData.rules().research().keySet().forEach(builder::suggest);
											return builder.buildFuture();
										})
										.then(Commands.argument("state", StringArgumentType.word())
												.suggests((context, builder) -> {
													for (ResearchState state : ResearchState.values()) {
														builder.suggest(state.id());
													}
													return builder.buildFuture();
												})
												.executes(ConcordanceCommand::grant)))))
				.then(Commands.literal("reset").requires(ConcordanceCommand::isOperator)
						.then(Commands.argument("player", EntityArgument.player())
								.executes(context -> reset(context.getSource(), EntityArgument.getPlayer(context, "player")))))
				.then(Commands.literal("focus").requires(ConcordanceCommand::isOperator)
						.then(Commands.argument("player", EntityArgument.player())
								.then(Commands.argument("amount", IntegerArgumentType.integer(0, FocusPool.MAX))
										.executes(context -> focus(context.getSource(), EntityArgument.getPlayer(context, "player"),
												IntegerArgumentType.getInteger(context, "amount"))))))));
	}

	private static boolean isOperator(CommandSourceStack source) {
		return Commands.LEVEL_GAMEMASTERS.check(source.permissions());
	}

	private static int status(CommandSourceStack source, ServerPlayer player) {
		Knowledge knowledge = ConcordanceProgress.knowledge(player);
		source.sendSuccess(() -> Component.translatable("message.jugcraft.concordance.focus",
				ConcordanceProgress.currentFocus(player), FocusPool.MAX), false);
		// The Concordance Journal's research report: names and states in words, the exact figures after them.
		Journal.research(player, (text, exact) -> source.sendSuccess(() -> exact == null ? text
				: text.copy().append(" ").append(exact.copy().withStyle(ChatFormatting.GRAY)), false));
		String invocations = knowledge.invocations().isEmpty() ? "none" : knowledge.invocations().toString();
		source.sendSuccess(() -> Component.literal("Invocations (spell=Focus): " + invocations), false);
		return knowledge.entries().size();
	}

	/**
	 * Compiles a composition for the player and the instrument in their main hand, and either explains it or names
	 * every problem; with {@code inscribe}, a valid one is written on the instrument (replacing what was there).
	 */
	private static int compose(CommandSourceStack source, String text, boolean inscribe) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		if (!JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE)) {
			source.sendFailure(Component.translatable("message.jugcraft.concordance.disabled"));
			return 0;
		}
		Instrument instrument = ComposedSpells.instrument(player);
		if (instrument == null) {
			source.sendFailure(Component.translatable("compose.jugcraft.problem.no_instrument"));
			return 0;
		}
		if (!RateGate.allow(player, "compose", COMPOSE_RATE_TICKS)) {
			source.sendFailure(Component.translatable("compose.jugcraft.problem.too_fast"));
			return 0;
		}
		Compiler.Compilation compiled = ComposedSpells.compile(player, instrument, text);
		Plan plan = compiled.plan();
		if (plan == null) {
			source.sendFailure(Component.translatable("compose.jugcraft.invalid", text.trim()));
			for (Text problem : compiled.problems()) {
				source.sendFailure(Component.literal("- ").append(ComposeText.show(problem)));
			}
			return 0;
		}
		explain(source, plan, instrument);
		if (inscribe) {
			ItemStack held = player.getMainHandItem();
			held.set(JugcraftConcordance.INSCRIPTION, new Inscription(plan.text(), plan.focus(), plan.cooldown()));
			source.sendSuccess(() -> Component.translatable("compose.jugcraft.inscribed", held.getHoverName(), plan.text()), false);
		}
		return plan.focus();
	}

	private static void explain(CommandSourceStack source, Plan plan, Instrument instrument) {
		source.sendSuccess(() -> Component.translatable("compose.jugcraft.valid", plan.text()), false);
		for (Text line : plan.explain(instrument)) {
			source.sendSuccess(() -> Component.literal("- ").append(ComposeText.show(line)), false);
		}
	}

	/** Explains the spell inscribed on the held instrument, compiled as it would be cast now. */
	private static int show(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		Instrument instrument = ComposedSpells.instrument(player);
		Inscription inscription = ComposedSpells.inscription(player.getMainHandItem());
		if (instrument == null || inscription == null) {
			source.sendFailure(Component.translatable(instrument == null ? "compose.jugcraft.problem.no_instrument"
					: "compose.jugcraft.nothing_inscribed"));
			return 0;
		}
		Compiler.Compilation compiled = ComposedSpells.compile(player, instrument, inscription.text());
		if (compiled.plan() == null) {
			source.sendFailure(Component.translatable("compose.jugcraft.invalid", inscription.text()));
			for (Text problem : compiled.problems()) {
				source.sendFailure(Component.literal("- ").append(ComposeText.show(problem)));
			}
			return 0;
		}
		explain(source, compiled.plan(), instrument);
		return compiled.plan().focus();
	}

	private static int clear(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		ItemStack held = player.getMainHandItem();
		if (ComposedSpells.inscription(held) == null) {
			source.sendFailure(Component.translatable("compose.jugcraft.nothing_inscribed"));
			return 0;
		}
		held.remove(JugcraftConcordance.INSCRIPTION);
		source.sendSuccess(() -> Component.translatable("compose.jugcraft.cleared"), false);
		return 1;
	}

	/** Lists, for the instrument in hand, each invocation the player knows, its Focus and its tuning. */
	private static int tunings(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		Instrument instrument = ComposedSpells.instrument(player);
		if (instrument == null) {
			source.sendFailure(Component.translatable("compose.jugcraft.problem.no_instrument"));
			return 0;
		}
		ConcordanceRules rules = ConcordanceData.rules();
		Knowledge knowledge = ConcordanceProgress.knowledge(player);
		if (knowledge.invocations().isEmpty()) {
			source.sendFailure(Component.translatable("message.jugcraft.concordance.no_invocations"));
			return 0;
		}
		Tunings tunings = Invocations.tunings(player.getMainHandItem());
		int shown = 0;
		for (Map.Entry<String, Integer> known : knowledge.invocations().entrySet()) {
			Definitions.Invocation invocation = rules.invocationForSpell(known.getKey());
			Authored form = invocation == null ? null : rules.authored(invocation.id(), instrument.id());
			if (form == null) {
				continue;
			}
			Component name = Component.translatable(Invocations.nameKey(known.getKey()));
			Tunings.Tuning tuning = tunings.get(known.getKey());
			if (tuning != null && form.tunings().containsKey(tuning.modifier())) {
				int cost = known.getValue() + form.tuningFocus(tuning.modifier());
				source.sendSuccess(() -> Component.translatable("message.jugcraft.concordance.tune.list", name, cost,
						ComposeText.name(Text.component(tuning.modifier()))), false);
			} else {
				source.sendSuccess(() -> Component.translatable("message.jugcraft.concordance.tune.list_untuned", name, known.getValue(),
						names(invocation.tunings())), false);
			}
			shown++;
		}
		return shown;
	}

	/**
	 * Tunes an invocation on the instrument in hand with one of the modifiers it offers ({@code modifier} null clears
	 * it). The modifier must be one the invocation offers, one the player could use in a composition of their own, and
	 * the tuned invocation must fit the instrument. The cast checks all of this again on the server, from its own rules.
	 */
	private static int tune(CommandSourceStack source, String invocationId, @Nullable String modifier) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		if (!JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE)) {
			source.sendFailure(Component.translatable("message.jugcraft.concordance.disabled"));
			return 0;
		}
		Instrument instrument = ComposedSpells.instrument(player);
		if (instrument == null) {
			source.sendFailure(Component.translatable("compose.jugcraft.problem.no_instrument"));
			return 0;
		}
		if (!RateGate.allow(player, "compose", COMPOSE_RATE_TICKS)) {
			source.sendFailure(Component.translatable("compose.jugcraft.problem.too_fast"));
			return 0;
		}
		ConcordanceRules rules = ConcordanceData.rules();
		Knowledge knowledge = ConcordanceProgress.knowledge(player);
		Definitions.Invocation invocation = rules.invocation(invocationId);
		int cost = invocation == null ? -1 : knowledge.invocationCost(invocation.spell());
		if (invocation == null || cost < 0) {
			source.sendFailure(Component.translatable("message.jugcraft.concordance.tune.unknown", invocationId));
			return 0;
		}
		Component name = Component.translatable(Invocations.nameKey(invocation.spell()));
		Authored form = rules.authored(invocation.id(), instrument.id());
		if (form == null) {
			source.sendFailure(Component.translatable("message.jugcraft.concordance.invocation.no_instrument", name));
			return 0;
		}
		ItemStack held = player.getMainHandItem();
		Tunings tunings = Invocations.tunings(held);
		if (modifier == null) {
			held.set(JugcraftConcordance.TUNINGS, tunings.with(invocation.spell(), null));
			source.sendSuccess(() -> Component.translatable("message.jugcraft.concordance.tune.cleared", name, cost), false);
			return cost;
		}
		Component modifierName = ComposeText.name(Text.component(modifier));
		if (!invocation.tunings().contains(modifier)) {
			source.sendFailure(Component.translatable("message.jugcraft.concordance.tune.not_offered", name, modifierName,
					names(invocation.tunings())));
			return 0;
		}
		if (!Invocations.mayUse(knowledge, rules, modifier)) {
			source.sendFailure(Component.translatable("message.jugcraft.concordance.tune.unlearned", modifierName));
			return 0;
		}
		Plan tuned = form.plan(modifier);
		if (tuned == null) {
			source.sendFailure(Component.translatable("message.jugcraft.concordance.tune.does_not_fit", name, modifierName));
			return 0;
		}
		int extra = form.tuningFocus(modifier);
		held.set(JugcraftConcordance.TUNINGS, tunings.with(invocation.spell(), new Tunings.Tuning(modifier, extra)));
		source.sendSuccess(() -> Component.translatable("message.jugcraft.concordance.tune.set", name, modifierName, cost + extra), false);
		for (Text line : tuned.explain(instrument)) {
			source.sendSuccess(() -> Component.literal("- ").append(ComposeText.show(line)), false);
		}
		return cost + extra;
	}

	private static Component names(List<String> components) {
		net.minecraft.network.chat.MutableComponent out = Component.empty();
		for (int i = 0; i < components.size(); i++) {
			if (i > 0) {
				out.append(", ");
			}
			out.append(ComposeText.name(Text.component(components.get(i))));
		}
		return out;
	}

	/** Suggests the invocations this player knows, by id. */
	private static CompletableFuture<Suggestions> suggestInvocations(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
		if (context.getSource().getPlayer() instanceof ServerPlayer player) {
			ConcordanceRules rules = ConcordanceData.rules();
			for (String spell : ConcordanceProgress.knowledge(player).invocations().keySet()) {
				Definitions.Invocation invocation = rules.invocationForSpell(spell);
				if (invocation != null && !invocation.tunings().isEmpty()) {
					builder.suggest(invocation.id());
				}
			}
		}
		return builder.buildFuture();
	}

	/** Suggests the modifiers the named invocation offers. */
	private static CompletableFuture<Suggestions> suggestTunings(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
		Definitions.Invocation invocation = ConcordanceData.rules().invocation(IdentifierArgument.getId(context, "invocation").toString());
		if (invocation != null) {
			invocation.tunings().forEach(builder::suggest);
		}
		return builder.buildFuture();
	}

	/** Suggests component names (and {@code then}) for the word being typed, after a space or a {@code +}. */
	private static CompletableFuture<Suggestions> suggestComponents(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
		String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
		int start = Math.max(typed.lastIndexOf(' '), typed.lastIndexOf('+')) + 1;
		String partial = typed.substring(start);
		SuggestionsBuilder word = builder.createOffset(builder.getStart() + start);
		boolean joined = start > 0 && typed.charAt(start - 1) == '+';
		ConcordanceData.rules().catalog().names().forEach((slot, names) -> {
			if (joined == (slot == Slot.MODIFIER)) {
				names.stream().filter(name -> name.startsWith(partial)).forEach(word::suggest);
			}
		});
		if (!joined && start > 0 && CompositionParser.THEN.startsWith(partial)) {
			word.suggest(CompositionParser.THEN);
		}
		return word.buildFuture();
	}

	/**
	 * The whole report on the circle round an anchor: its phase and every fault, with positions. Anyone may read a
	 * circle within {@value #CIRCLE_READ_RANGE} blocks; operators, any loaded one.
	 */
	private static int circle(CommandSourceStack source, BlockPos pos) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		ServerLevel level = source.getLevel();
		if (!(level.getBlockEntity(pos) instanceof CircleAnchorBlockEntity anchor)) {
			source.sendFailure(Component.translatable("message.jugcraft.concordance.circle.not_anchor"));
			return 0;
		}
		if (!isOperator(source) && player.distanceToSqr(Vec3.atCenterOf(pos)) > (double) CIRCLE_READ_RANGE * CIRCLE_READ_RANGE) {
			source.sendFailure(Component.translatable("message.jugcraft.concordance.circle.too_far"));
			return 0;
		}
		anchor.status(player, level);
		return 1;
	}

	private static int diagnose(CommandSourceStack source) {
		ConcordanceRules rules = ConcordanceData.rules();
		source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
				"Concordance rules: %d research, %d invocations, %d workings, %d conversions, %d structures, %d rituals; entry points %s",
				rules.research().size(), rules.invocations().size(), rules.workings().size(),
				rules.conversions().conversions().size(), rules.structures().size(), rules.rituals().size(), rules.entryPoints())), false);
		List<String> problems = rules.problems();
		if (problems.isEmpty()) {
			source.sendSuccess(() -> Component.literal("No problems"), false);
		}
		for (String problem : problems) {
			source.sendFailure(Component.literal(problem));
		}
		return problems.size();
	}

	private static int grant(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = EntityArgument.getPlayer(context, "player");
		String research = IdentifierArgument.getId(context, "research").toString();
		ResearchState state = ResearchState.fromId(StringArgumentType.getString(context, "state").toLowerCase(Locale.ROOT));
		if (ConcordanceData.rules().research(research) == null || state == null) {
			context.getSource().sendFailure(Component.literal("Unknown research or state"));
			return 0;
		}
		ConcordanceProgress.grant(player, research, state);
		Map<String, Integer> invocations = ConcordanceProgress.knowledge(player).invocations();
		context.getSource().sendSuccess(() -> Component.literal(player.getName().getString() + ": " + research + " set to "
				+ state.id() + "; invocations " + invocations), true);
		return 1;
	}

	private static int reset(CommandSourceStack source, ServerPlayer player) {
		ConcordanceProgress.reset(player);
		source.sendSuccess(() -> Component.literal(player.getName().getString() + ": Concordance research forgotten"), true);
		return 1;
	}

	private static int focus(CommandSourceStack source, ServerPlayer player, int amount) {
		ConcordanceProgress.setFocus(player, amount);
		source.sendSuccess(() -> Component.literal(player.getName().getString() + ": Focus set to " + amount), true);
		return amount;
	}
}
