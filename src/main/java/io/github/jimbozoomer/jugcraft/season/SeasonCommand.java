package io.github.jimbozoomer.jugcraft.season;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * {@code /jugcraft season}: anyone can see the season and the events running. Operators (permission level 2) can
 * change it until the server stops, without touching the config file:
 * <ul>
 * <li>{@code set <auto|spring|summer|autumn|winter|off>}: follow the date again, hold a season, or switch off;</li>
 * <li>{@code date <MM-DD>}: act as if it were that date (season and events), for previews; {@code date today} ends it;</li>
 * <li>{@code snow <on|off>}: winter snow on or off.</li>
 * </ul>
 */
public final class SeasonCommand {
	private SeasonCommand() {
	}

	static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("jugcraft").then(Commands.literal("season")
				.executes(context -> show(context.getSource()))
				.then(Commands.literal("set").requires(SeasonCommand::isOperator)
						.then(Commands.argument("season", StringArgumentType.word())
								.suggests((context, builder) -> {
									for (SeasonCalendar.Mode mode : SeasonCalendar.Mode.values()) {
										builder.suggest(mode.name().toLowerCase(Locale.ROOT));
									}
									return builder.buildFuture();
								})
								.executes(SeasonCommand::set)))
				.then(Commands.literal("date").requires(SeasonCommand::isOperator)
						.then(Commands.argument("date", StringArgumentType.word()).executes(SeasonCommand::date)))
				.then(Commands.literal("snow").requires(SeasonCommand::isOperator)
						.then(Commands.literal("on").executes(context -> snow(context.getSource(), true)))
						.then(Commands.literal("off").executes(context -> snow(context.getSource(), false))))));
	}

	private static boolean isOperator(CommandSourceStack source) {
		return Commands.LEVEL_GAMEMASTERS.check(source.permissions());
	}

	private static int show(CommandSourceStack source) {
		source.sendSuccess(() -> Component.literal(describe()), false);
		return JugcraftSeasons.today();
	}

	/** One line: the season, its day, where the date comes from, the events running and the snow. */
	public static String describe() {
		SeasonCalendar.Settings settings = JugcraftSeasons.settings();
		LocalDate today = JugcraftSeasons.date();
		int day = settings.dayOn(today);
		List<SeasonCalendar.Event> events = settings.eventsOn(today);
		String source = settings.fixedDate() != null ? "previewing " + settings.effectiveDate(today)
				: settings.mode() == SeasonCalendar.Mode.AUTO ? today + " in " + settings.zone()
				: "held at " + settings.mode().name().toLowerCase(Locale.ROOT);
		return String.format(Locale.ROOT, "Season: %s (day %d of %d, %s, %s hemisphere). Events: %s. Winter snow: %s.",
				SeasonCalendar.seasonName(day), day, SeasonCalendar.DAYS, source, settings.southern() ? "southern" : "northern",
				events.isEmpty() ? "none" : events.stream().map(event -> event.display).collect(Collectors.joining(", ")),
				!settings.snow() ? "off" : SeasonState.serverSnowing() ? "falling" : "on (not winter)");
	}

	private static int set(CommandContext<CommandSourceStack> context) {
		String text = StringArgumentType.getString(context, "season");
		SeasonCalendar.Mode mode;
		try {
			mode = SeasonCalendar.Mode.parse(text);
		} catch (IllegalArgumentException e) {
			context.getSource().sendFailure(Component.literal("Not a season: " + text + " (auto, spring, summer, autumn, winter or off)"));
			return 0;
		}
		JugcraftSeasons.setMode(context.getSource().getServer(), mode);
		return show(context.getSource());
	}

	private static int date(CommandContext<CommandSourceStack> context) {
		String text = StringArgumentType.getString(context, "date");
		MonthDay date = null;
		if (!text.equalsIgnoreCase("today")) {
			try {
				date = MonthDay.parse("--" + text);
			} catch (DateTimeException e) {
				context.getSource().sendFailure(Component.literal("Not a date: " + text + " (MM-DD, for example 11-26, or today)"));
				return 0;
			}
		}
		JugcraftSeasons.setFixedDate(context.getSource().getServer(), date);
		return show(context.getSource());
	}

	private static int snow(CommandSourceStack source, boolean on) {
		JugcraftSeasons.setSnow(source.getServer(), on);
		return show(source);
	}
}
