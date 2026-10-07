package io.github.jimbozoomer.jugcraft.season;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/**
 * Seasons on the server, the one season clock: it reads the operator's settings, works out the season day, the
 * events running (the Harvest Feast and December) and whether winter snow is falling, and tells each player when
 * they join and again whenever any of these change (checked once a minute). An event starting is announced in chat.
 * No item, block, recipe or progression depends on the season, so nothing is lost or gated when one ends; winter's
 * snow (opt-in) melts away in spring.
 */
public final class JugcraftSeasons {
	/** Biomes whose grass and leaves change colour with the season (generated from tools/seasons.py). */
	public static final TagKey<Biome> HAS_SEASONS = TagKey.create(Registries.BIOME, Jugcraft.id("has_seasons"));
	/** Biomes where winter brings snow when {@code seasons.snow} is on (generated from tools/seasons.py). */
	public static final TagKey<Biome> HAS_WINTER_SNOW = TagKey.create(Registries.BIOME, Jugcraft.id("has_winter_snow"));
	/** Ticks between date checks: one minute. */
	public static final int CHECK_TICKS = 1200;

	private static volatile SeasonCalendar.Settings settings = SeasonCalendar.Settings.DEFAULT;
	private static int lastDay = -1;
	private static boolean lastSnowing;
	private static List<SeasonCalendar.Event> lastEvents = List.of();
	private static int ticks;

	private JugcraftSeasons() {
	}

	public static void register() {
		settings = SeasonCalendar.Settings.fromConfig();
		SeasonalSnow.register();
		SeasonCommand.register();
		PayloadTypeRegistry.clientboundPlay().register(SeasonPayload.TYPE, SeasonPayload.CODEC);
		CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> SeasonalBiome.updateFlags(registries));
		ServerLifecycleEvents.SERVER_STARTED.register(JugcraftSeasons::serverStarted);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> SeasonState.serverStopped());
		ServerPlayConnectionEvents.JOIN.register((listener, sender, server) -> {
			if (ServerPlayNetworking.canSend(listener, SeasonPayload.TYPE)) {
				sender.sendPacket(payload());
			}
			List<SeasonCalendar.Event> events = settings.eventsOn(date());
			if (!events.isEmpty() && settings.fixedDate() == null) {
				listener.player.sendSystemMessage(eventsMessage(events, "On now"));
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			// A frozen game (/tick freeze) lays no snow, as it lets no weather or random tick happen either.
			if (server.tickRateManager().runsNormally()) {
				SeasonalSnow.tick(server, settings.snowDepth());
			}
			if (++ticks >= CHECK_TICKS) {
				ticks = 0;
				update(server, true);
			}
		});
	}

	public static SeasonCalendar.Settings settings() {
		return settings;
	}

	/**
	 * What a server starting does (public for tests): it begins from the operator's settings in the config file, so
	 * {@link #setMode}, {@link #setFixedDate} and {@link #setSnow} last until the server stops, also when one game
	 * opens world after world, and then tells every player the season.
	 */
	public static void serverStarted(MinecraftServer server) {
		SeasonState.serverStarted();
		settings = SeasonCalendar.Settings.fromConfig();
		lastDay = -1;
		lastEvents = List.of();
		ticks = 0;
		update(server, false);
	}

	/** Today's date in the configured zone (the real date; see {@link SeasonCalendar.Settings#effectiveDate}). */
	public static LocalDate date() {
		return LocalDate.now(settings.zone());
	}

	/** Today's season day in the configured zone and hemisphere (or the fixed mode's day, or 0 when off). */
	public static int today() {
		return settings.dayOn(date());
	}

	/**
	 * Whether {@code event} is running today (or on the date an operator previews with {@code /jugcraft season date}).
	 * Seasonal content can read this; never trust a client's date.
	 */
	public static boolean isActive(SeasonCalendar.Event event) {
		return settings.eventsOn(date()).contains(event);
	}

	/** Holds a season (or follows the date again, or switches off) until the server stops; tells every player. */
	public static void setMode(MinecraftServer server, SeasonCalendar.Mode mode) {
		settings = settings.withMode(mode);
		update(server, true);
	}

	/**
	 * Acts as if it were {@code date} (null: today again) until the server stops, for previews. Players get the
	 * preview's colours and snow, but no announcement: a preview is not the calendar.
	 */
	public static void setFixedDate(MinecraftServer server, MonthDay date) {
		settings = settings.withFixedDate(date);
		update(server, false);
	}

	/** Winter snow on or off until the server stops (the config file is unchanged); tells every player. */
	public static void setSnow(MinecraftServer server, boolean snow) {
		settings = settings.withSnow(snow);
		update(server, true);
	}

	private static SeasonPayload payload() {
		return new SeasonPayload(today(), SeasonState.serverSnowing());
	}

	/** Recomputes the day, snow and events; tells players what changed and announces events that began. */
	private static void update(MinecraftServer server, boolean announce) {
		LocalDate date = date();
		int day = settings.dayOn(date);
		boolean snowing = settings.snowOn(date);
		SeasonState.setServerSnowing(snowing);
		List<SeasonCalendar.Event> events = settings.eventsOn(date);
		if (day != lastDay || snowing != lastSnowing) {
			SeasonPayload payload = new SeasonPayload(day, snowing);
			for (ServerPlayer player : PlayerLookup.all(server)) {
				if (ServerPlayNetworking.canSend(player, SeasonPayload.TYPE)) {
					ServerPlayNetworking.send(player, payload);
				}
			}
		}
		List<SeasonCalendar.Event> started = events.stream().filter(event -> !lastEvents.contains(event)).toList();
		if (announce && settings.fixedDate() == null && !started.isEmpty()) {
			server.getPlayerList().broadcastSystemMessage(eventsMessage(started, "Starting today"), false);
		}
		lastDay = day;
		lastSnowing = snowing;
		lastEvents = events;
	}

	private static Component eventsMessage(List<SeasonCalendar.Event> events, String lead) {
		StringBuilder text = new StringBuilder(lead).append(": ");
		for (int i = 0; i < events.size(); i++) {
			SeasonCalendar.Event event = events.get(i);
			text.append(i == 0 ? "" : ", ").append("the ").append(event.display);
			if (event == SeasonCalendar.Event.HARVEST_FEAST) {
				LocalDate[] window = settings.feastWindow(settings.effectiveDate(date()).getYear());
				if (window != null) {
					text.append(" (until ").append(window[1].format(DateTimeFormatter.ofPattern("d MMMM", Locale.ENGLISH))).append(")");
				}
			}
		}
		return Component.literal(text.append('.').toString());
	}
}
