package io.github.jimbozoomer.jugcraft.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.nio.file.Files;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;

/** Opt-in, per-dimension review setting. Ordinary worlds keep their normal mob behavior. */
public final class ReviewWorldSettings extends SavedData {
	public static final Codec<ReviewWorldSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.BOOL.optionalFieldOf("suppress_slimes", false).forGetter(state -> state.suppressSlimes),
			Codec.BOOL.optionalFieldOf("configured", false).forGetter(state -> state.configured))
			.apply(i, ReviewWorldSettings::new));
	private static final SavedDataType<ReviewWorldSettings> TYPE = new SavedDataType<>(Jugcraft.id("review_settings"),
			() -> new ReviewWorldSettings(false, false), CODEC, null);
	private boolean suppressSlimes;
	private boolean configured;

	private ReviewWorldSettings(boolean suppressSlimes, boolean configured) {
		this.suppressSlimes = suppressSlimes;
		this.configured = configured;
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			// The owner's running review world is configured for its next normal start, never edited offline.
			if (!server.isSingleplayer() || !isFlat(server.overworld())) return;
			var path = FabricLoader.getInstance().getConfigDir().resolve("jugcraft-review-worlds.json");
			try {
				if (!Files.isRegularFile(path) || Files.size(path) > 16_384) return;
				String name = server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString();
				if (parseStartupWorlds(Files.readString(path)).contains(name) && !get(server.overworld()).configured) {
					int removed = setSlimesSuppressed(server.overworld(), true);
					Jugcraft.LOGGER.info("Slime cleanup enabled for flat review world {} ({} loaded slimes removed)", name, removed);
				}
			} catch (java.io.IOException | RuntimeException error) {
				Jugcraft.LOGGER.warn("Could not read flat review-world settings: {}", error.getMessage());
			}
		});
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			// Discard also covers saved slimes when their chunks load. It does not split them or spill loot.
			if (entity.getType() == EntityTypes.SLIME && suppressesSlimes(level)) entity.discard();
		});
	}

	/** Explicit local world-folder names only; no global switch or dimension wildcard. */
	public static Set<String> parseStartupWorlds(String json) {
		if (json.length() > 16_384) throw new IllegalArgumentException("Review setting is too large");
		var root = JsonParser.parseString(json).getAsJsonObject();
		var array = root.getAsJsonArray("suppress_slimes_in_flat_worlds");
		if (array == null) return Set.of();
		if (array.size() > 64) throw new IllegalArgumentException("Too many review worlds");
		var names = new HashSet<String>();
		for (var value : array) {
			String name = value.getAsString();
			if (name.isBlank() || name.length() > 128 || name.contains("/") || name.contains("\\") || name.contains(":"))
				throw new IllegalArgumentException("Expected a local world-folder name");
			names.add(name);
		}
		return Set.copyOf(names);
	}

	public static ReviewWorldSettings get(ServerLevel level) { return level.getDataStorage().computeIfAbsent(TYPE); }
	public static boolean isFlat(ServerLevel level) { return level.getChunkSource().getGenerator() instanceof FlatLevelSource; }
	public static boolean suppressesSlimes(ServerLevel level) { return isFlat(level) && get(level).suppressSlimes; }

	/** One cleanup of loaded entities on an operator request; no recurring entity scan. */
	public static int setSlimesSuppressed(ServerLevel level, boolean suppressed) {
		if (!isFlat(level)) throw new IllegalArgumentException("Slime suppression is only available in a flat review world");
		var settings = get(level);
		settings.suppressSlimes = suppressed;
		settings.configured = true;
		settings.setDirty();
		if (!suppressed) return 0;
		var slimes = new ArrayList<Entity>();
		for (Entity entity : level.getAllEntities()) if (entity.getType() == EntityTypes.SLIME) slimes.add(entity);
		slimes.forEach(Entity::discard);
		return slimes.size();
	}
}
