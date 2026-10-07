package io.github.jimbozoomer.jugcraft.party;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * The shared party API. Other features call these static methods and never reach into the party
 * internals. See docs/features/parties.md.
 *
 * <ul>
 * <li>{@link #sameParty}, {@link #isLeader}, {@link #partyMembers}: who is teamed up.</li>
 * <li>{@link #mayServe}: the one Personal/Party rule every automated system uses.</li>
 * <li>{@link #addListener}: be told when membership changes (for example to drop cached jobs).</li>
 * </ul>
 *
 * Party data is server-side only and saved in the world folder at {@code jugcraft/parties.txt}.
 */
public final class JugcraftParties {
	public static final String FEATURE = "parties";
	/** Save at most this often when something changed (and always when the server stops). */
	private static final int SAVE_INTERVAL_TICKS = 20 * 30;
	private static final int EXPIRE_INTERVAL_TICKS = 20 * 60;

	private static final PartyManager MANAGER = new PartyManager();
	private static Path file;
	private static int ticks;
	/** The running server, for telling players' Party screens about changes; null when none is running. */
	private static MinecraftServer server;

	private JugcraftParties() {
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(JugcraftParties::load);
		ServerLifecycleEvents.SERVER_STOPPING.register(stopping -> {
			save();
			server = null;
		});
		ServerTickEvents.END_SERVER_TICK.register(JugcraftParties::tick);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> PartyCommands.register(dispatcher));
		PartyNetwork.register();
		MANAGER.addListener(affected -> {
			if (server != null) {
				PartyNetwork.send(server, affected);
			}
		});
	}

	/** The live manager. Public for game tests and for features that need party details. */
	public static PartyManager manager() {
		return MANAGER;
	}

	public static boolean sameParty(UUID a, UUID b) {
		return MANAGER.sameParty(a, b);
	}

	public static boolean isLeader(UUID player) {
		return MANAGER.isLeader(player);
	}

	/** The player's party members in join order, or just the player when not in a party. */
	public static List<UUID> partyMembers(UUID player) {
		return MANAGER.partyOf(player).map(PartyManager.Party::members).orElse(List.of(player));
	}

	public static Optional<UUID> partyId(UUID player) {
		return MANAGER.partyOf(player).map(PartyManager.Party::id);
	}

	/** See {@link PartyManager#mayServe}. */
	public static boolean mayServe(UUID systemOwner, UseMode systemMode, UUID jobOwner, UseMode jobMode) {
		return MANAGER.mayServe(systemOwner, systemMode, jobOwner, jobMode);
	}

	public static void addListener(PartyManager.Listener listener) {
		MANAGER.addListener(listener);
	}

	private static void load(MinecraftServer started) {
		server = started;
		MANAGER.setEnabled(JugcraftConfig.isFeatureEnabled(FEATURE));
		MANAGER.setLimits(limit("parties.max_size", PartyManager.DEFAULT_MAX_SIZE, 2, 64),
				limit("parties.invite_minutes", (int) (PartyManager.DEFAULT_INVITE_TTL_MILLIS / 60_000), 1, 60) * 60_000L,
				limit("parties.invites_per_minute", PartyManager.DEFAULT_INVITES_PER_MINUTE, 1, 60));
		file = started.getWorldPath(LevelResource.ROOT).resolve(Jugcraft.MOD_ID).resolve(PartyStore.FILE_NAME);
		try {
			PartyStore.load(MANAGER, file);
			Jugcraft.LOGGER.info("Loaded {} parties", MANAGER.parties().size());
		} catch (IOException e) {
			// Never crash or wipe the file: start with no parties and leave the file for an operator to inspect.
			Jugcraft.LOGGER.error("Could not read {}; starting with no parties. The file was left untouched.", file, e);
			file = file.resolveSibling(PartyStore.FILE_NAME + ".recovered");
		}
		ticks = 0;
	}

	/** A whole-number party limit from the config; a bad value is logged and the default kept. */
	private static int limit(String key, int fallback, int min, int max) {
		String text = JugcraftConfig.textOption(key);
		try {
			int value = Integer.parseInt(text.trim());
			if (value >= min && value <= max) {
				return value;
			}
		} catch (NumberFormatException e) {
			// logged below
		}
		Jugcraft.LOGGER.warn("{} \"{}\" is not a number from {} to {}; using {}", key, text, min, max, fallback);
		return fallback;
	}

	private static void tick(MinecraftServer ticking) {
		ticks++;
		if (ticks % EXPIRE_INTERVAL_TICKS == 0) {
			MANAGER.expireInvites(System.currentTimeMillis());
		}
		if (ticks % SAVE_INTERVAL_TICKS == 0 && MANAGER.isDirty()) {
			save();
		}
	}

	private static void save() {
		if (file == null || !MANAGER.isDirty()) {
			return;
		}
		try {
			PartyStore.save(MANAGER, file);
		} catch (IOException e) {
			Jugcraft.LOGGER.error("Could not save parties to {}", file, e);
		}
	}
}
