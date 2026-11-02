package io.github.jimbozoomer.jugcraft.town;

import com.mojang.authlib.GameProfile;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jspecify.annotations.Nullable;

/**
 * A custom NPC's player skin, from a Minecraft player's name (docs/features/custom-npcs.md).
 *
 * <p>The name is resolved through the server's own profile resolver ({@code server.services().profileResolver()}, the
 * one player heads use), never on the server thread: {@link ResolvableProfile#resolveProfile} runs it off-thread and
 * the reply is handed back to the server thread ({@code whenCompleteAsync(.., server)}) before anything is changed.
 *
 * <ul>
 * <li><b>Bounded.</b> At most {@link #MAX_OUTSTANDING} different names are looked up at once; more is refused (try
 * again shortly). Asking for a name already on its way joins that lookup.</li>
 * <li><b>Timed out.</b> A lookup that has not answered in {@link #TIMEOUT_SECONDS} seconds fails.</li>
 * <li><b>Stale replies are dropped.</b> Each request carries the NPC's request number ({@code Data.skinRequest});
 * a reply for an NPC that has since been given another skin, removed or unloaded changes nothing. A reply that arrives
 * after the server stopped (when it would otherwise run on the lookup thread) is dropped too.</li>
 * <li><b>Cached.</b> Found profiles are kept in memory for {@link #CACHE_MINUTES} minutes ({@link #CACHE_SIZE} names),
 * and names that were not found for one minute. The profile itself (textures included) is saved with the NPC
 * ({@code ResolvableProfile.CODEC}), so a restart never looks it up again.</li>
 * </ul>
 *
 * <p>Server thread only, except the lookup itself.
 */
public final class NpcSkins {
	public static final int MAX_OUTSTANDING = 4;
	public static final int TIMEOUT_SECONDS = 15;
	public static final int CACHE_SIZE = 128;
	public static final long CACHE_MINUTES = 360, MISS_MINUTES = 1;
	private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");

	/** Looks a name up off the server thread. The real one is {@link #SERVER}; tests swap in their own. */
	@FunctionalInterface
	public interface Resolver {
		CompletableFuture<GameProfile> resolve(MinecraftServer server, String name);
	}

	public static final Resolver SERVER = (server, name) -> ResolvableProfile.createUnresolved(name)
			.resolveProfile(server.services().profileResolver());

	public enum Result {
		/** Worn at once (cached, or the player is online with a skin). */
		APPLIED,
		/** Looking it up; the asker is told when it is done. */
		STARTED,
		/** That name is already being looked up; this NPC gets it too. */
		JOINED,
		/** Too many lookups at once: nothing changed. */
		BUSY,
		/** Not a Minecraft player name, or it was not found a moment ago: nothing changed. */
		INVALID,
		/** Not a custom NPC. */
		NOT_CUSTOM
	}

	private record Waiter(UUID npc, Townsfolk original, int request, @Nullable UUID asker) {
	}

	private static final class Lookup {
		final String name;
		final MinecraftServer server;
		final long started = System.nanoTime();
		final List<Waiter> waiting = new ArrayList<>();

		Lookup(String name, MinecraftServer server) {
			this.name = name;
			this.server = server;
		}
	}

	/** A found profile ({@code profile} non-null) or a miss, and when it was looked up. */
	private record Cached(@Nullable ResolvableProfile profile, String name, long at) {
		boolean fresh() {
			long age = System.nanoTime() - at;
			return age >= 0 && age < TimeUnit.MINUTES.toNanos(profile != null ? CACHE_MINUTES : MISS_MINUTES);
		}
	}

	private static Resolver resolver = SERVER;
	/** Whether a resolved profile has a skin to wear: {@link #hasSkin}, except in tests. */
	private static Predicate<GameProfile> skinned = NpcSkins::hasSkin;
	/** Lookups in flight, by lower-case name. */
	private static final Map<String, Lookup> PENDING = new HashMap<>();
	private static final Map<String, Cached> CACHE = new LinkedHashMap<>(16, 0.75F, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Cached> eldest) {
			return size() > CACHE_SIZE;
		}
	};

	private NpcSkins() {
	}

	public static boolean validName(@Nullable String name) {
		return name != null && NAME.matcher(name).matches();
	}

	/**
	 * Gives a custom NPC the skin of the player with this name: at once if known, otherwise when the lookup answers.
	 * The asker (a player, or null for the console) is told how it went.
	 */
	public static Result request(MinecraftServer server, Townsfolk npc, String name, @Nullable ServerPlayer asker) {
		CustomNpcs.Data data = npc.custom();
		if (data == null) {
			return Result.NOT_CUSTOM;
		}
		if (!validName(name)) {
			return Result.INVALID;
		}
		String key = name.toLowerCase(Locale.ROOT);
		Cached cached = CACHE.get(key);
		if (cached != null && cached.fresh()) {
			if (cached.profile == null) {
				return Result.INVALID;
			}
			data.skinRequest++;   // anything still on its way for this NPC no longer applies
			CustomNpcs.setPlayerSkin(npc, cached.name, cached.profile);
			return Result.APPLIED;
		}
		ServerPlayer online = server.getPlayerList().getPlayerByName(name);
		if (online != null && skinned.test(online.getGameProfile())) {
			// Already known to the server, signed textures and all: no lookup needed.
			ResolvableProfile profile = ResolvableProfile.createResolved(online.getGameProfile());
			CACHE.put(key, new Cached(profile, online.getGameProfile().name(), System.nanoTime()));
			data.skinRequest++;
			CustomNpcs.setPlayerSkin(npc, online.getGameProfile().name(), profile);
			return Result.APPLIED;
		}
		Lookup lookup = PENDING.get(key);
		if (lookup != null && (lookup.server != server || expired(lookup))) {
			// Left over from a server that stopped before it answered, or never answered: forget it.
			PENDING.remove(key);
			lookup = null;
		}
		if (lookup != null) {
			lookup.waiting.removeIf(w -> w.npc().equals(npc.getUUID()));
            if (lookup.waiting.size() >= 128) return Result.BUSY;
            lookup.waiting.add(new Waiter(npc.getUUID(), npc, ++data.skinRequest, asker == null ? null : asker.getUUID()));
			return Result.JOINED;
		}
		PENDING.values().removeIf(other -> other.server != server || expired(other));
		if (PENDING.size() >= MAX_OUTSTANDING) {
			return Result.BUSY;
		}
		Lookup started = new Lookup(name, server);
		started.waiting.add(new Waiter(npc.getUUID(), npc, ++data.skinRequest, asker == null ? null : asker.getUUID()));
		PENDING.put(key, started);
		CompletableFuture<GameProfile> future;
		try {
			future = resolver.resolve(server, name);
		} catch (RuntimeException failed) {
			future = CompletableFuture.failedFuture(failed);
		}
		future.orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
				.whenCompleteAsync((profile, error) -> finish(key, started, profile, error), server);
		return Result.STARTED;
	}

	/** The lookup answered (or failed or timed out). On the server thread; dropped otherwise. */
	private static void finish(String key, Lookup lookup, @Nullable GameProfile profile, @Nullable Throwable error) {
		MinecraftServer server = lookup.server;
		if (!server.isSameThread() || PENDING.get(key) != lookup) {
			// The server stopped (so this runs on the lookup's thread), or this lookup was given up: change nothing.
			return;
		}
		PENDING.remove(key);
		boolean found = error == null && profile != null && skinned.test(profile);
		ResolvableProfile resolved = found ? ResolvableProfile.createResolved(profile) : null;
		String shown = found && validName(profile.name()) ? profile.name() : lookup.name;
		CACHE.put(key, new Cached(resolved, shown, System.nanoTime()));
		if (error != null) {
			Jugcraft.LOGGER.debug("NPC skin lookup for {} failed: {}", lookup.name, error.toString());
		}
		for (Waiter waiter : lookup.waiting) {
			Townsfolk npc = loaded(server, waiter.npc());
			CustomNpcs.Data data = npc == null ? null : npc.custom();
			ServerPlayer asker = waiter.asker() == null ? null : server.getPlayerList().getPlayer(waiter.asker());
			if (data == null || npc != waiter.original() || data.skinRequest != waiter.request()) {
				continue;   // removed, unloaded, or given another skin since: stale
			}
			if (resolved != null) {
				CustomNpcs.setPlayerSkin(npc, shown, resolved);
				tell(asker, Component.literal("NPC #" + data.id() + " now wears " + shown + "'s skin."));
			} else {
				tell(asker, Component.literal("No skin found for " + lookup.name
						+ (error != null ? " (the lookup failed or timed out)." : ".") + " NPC #" + data.id() + " is unchanged."));
			}
		}
	}

	private static void tell(@Nullable ServerPlayer player, Component message) {
		if (player != null) {
			player.sendSystemMessage(message);
		} else {
			Jugcraft.LOGGER.info(message.getString());
		}
	}

	private static @Nullable Townsfolk loaded(MinecraftServer server, UUID id) {
		for (ServerLevel level : server.getAllLevels()) {
			if (level.getEntity(id) instanceof Townsfolk npc && npc.isAlive()) {
				return npc;
			}
		}
		return null;
	}

	private static boolean expired(Lookup lookup) {
		return System.nanoTime() - lookup.started > TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS + 10L);
	}

	/** A profile with textures (a skin); an offline-mode or unknown player's profile has none. */
	static boolean hasSkin(GameProfile profile) {
		return profile.properties().containsKey("textures");
	}

	// ---------------------------------------------------------------- for tests

	/**
	 * Swaps the resolver and the "has a skin" test (tests only: their profiles carry no signed textures). Also forgets
	 * every cached and pending name. {@code useResolver(SERVER, null)} restores the real ones.
	 */
	public static void useResolver(Resolver replacement, @Nullable Predicate<GameProfile> hasSkin) {
		resolver = replacement;
		skinned = hasSkin != null ? hasSkin : NpcSkins::hasSkin;
		PENDING.clear();
		CACHE.clear();
	}

	/** Lookups in flight now. */
	public static int outstanding() {
		return PENDING.size();
	}
}
