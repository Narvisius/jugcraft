package io.github.jimbozoomer.jugcraft.blueprint;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;

/**
 * The server's blueprint library: the built-in blueprints, then every blueprint imported at a Blueprint Table.
 * Imported blueprints are saved with the world ({@code <world>/jugcraft/blueprints/*.jugbp.json}); an operator
 * can also drop files there and restart. Files that fail the checks are skipped with a log line.
 */
public final class BlueprintLibrary {
	private BlueprintLibrary() {
	}

	public static Path folder(MinecraftServer server) {
		return server.getWorldPath(LevelResource.ROOT).resolve("jugcraft").resolve("blueprints");
	}

	public static void load(MinecraftServer server) {
		Blueprint.clear(false);
		for (String id : Blueprint.BUILT_IN) {
			Blueprint blueprint = Blueprint.builtIn(id);
			if (blueprint != null) {
				Blueprint.put(blueprint, false);
			}
		}
		Path folder = folder(server);
		if (!Files.isDirectory(folder)) {
			return;
		}
		try (Stream<Path> files = Files.list(folder)) {
			for (Path file : files.filter(f -> f.getFileName().toString().endsWith(".jugbp.json")).sorted().toList()) {
				String slug = file.getFileName().toString().replace(".jugbp.json", "");
				try {
					Blueprint.put(Blueprint.parse("import/" + slug, Files.readString(file, StandardCharsets.UTF_8), "imported"), false);
				} catch (Blueprint.Invalid e) {
					Jugcraft.LOGGER.warn("Skipping blueprint {}: {}", file.getFileName(), e.getMessage());
				}
			}
		} catch (IOException e) {
			Jugcraft.LOGGER.warn("Could not read the blueprint folder {}: {}", folder, e.toString());
		}
	}

	/**
	 * Checks and saves a pasted blueprint, and adds it to the library. Re-importing a blueprint with the same name
	 * replaces it. Returns the new blueprint; {@link Blueprint.Invalid} says what is wrong.
	 */
	public static Blueprint importText(MinecraftServer server, String json) throws Blueprint.Invalid {
		Blueprint probe = Blueprint.parse("import/probe", json, "imported");
		String slug = slug(probe.name);
		Blueprint blueprint = Blueprint.parse("import/" + slug, json, "imported");
		Path folder = folder(server);
		try {
			Files.createDirectories(folder);
			Files.writeString(folder.resolve(slug + ".jugbp.json"), json, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new Blueprint.Invalid("Could not save it: " + e.getMessage());
		}
		Blueprint.put(blueprint, false);
		return blueprint;
	}

	static String slug(String name) {
		String slug = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
		return slug.isEmpty() ? "blueprint" : slug.length() > 40 ? slug.substring(0, 40) : slug;
	}

	public static @Nullable Blueprint get(String id) {
		return Blueprint.get(id, false);
	}

	public static List<Blueprint> all() {
		return Blueprint.all(false);
	}
}
