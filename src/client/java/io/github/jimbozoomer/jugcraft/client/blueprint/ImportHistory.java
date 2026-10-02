package io.github.jimbozoomer.jugcraft.client.blueprint;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.blueprint.Blueprint;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;

/**
 * The blueprints this player has imported, kept on their own computer ({@code .minecraft/jugcraft/imported_blueprints/},
 * one {@code .jugbp.json} each), so the Blueprint Table's IMPORT tab lists them on any server and can import them
 * again. Client side only: nothing here reaches the server until the player presses IMPORT.
 */
public final class ImportHistory {
	/** One saved import: its slug (file name), the blueprint read from it, and its text. */
	public record Entry(String slug, Blueprint blueprint, String text, long saved) {
	}

	private ImportHistory() {
	}

	static Path folder() {
		return Minecraft.getInstance().gameDirectory.toPath().resolve("jugcraft").resolve("imported_blueprints");
	}

	static String slug(String name) {
		String slug = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
		return slug.isEmpty() ? "blueprint" : slug.length() > 40 ? slug.substring(0, 40) : slug;
	}

	/** Every saved import, newest first; files that no longer read as blueprints are skipped. */
	public static List<Entry> list() {
		List<Entry> out = new ArrayList<>();
		Path folder = folder();
		if (!Files.isDirectory(folder)) {
			return out;
		}
		try (Stream<Path> files = Files.list(folder)) {
			for (Path file : files.filter(f -> f.getFileName().toString().endsWith(".jugbp.json")).toList()) {
				String slug = file.getFileName().toString().replace(".jugbp.json", "");
				try {
					String text = Files.readString(file, StandardCharsets.UTF_8);
					out.add(new Entry(slug, Blueprint.parse("history/" + slug, text, "imported"), text, Files.getLastModifiedTime(file).toMillis()));
				} catch (Exception e) {
					Jugcraft.LOGGER.warn("Skipping saved blueprint {}: {}", file, e.getMessage());
				}
			}
		} catch (Exception e) {
			Jugcraft.LOGGER.warn("Could not read the imported blueprints in {}", folder, e);
		}
		out.sort(Comparator.comparingLong(Entry::saved).reversed());
		return out;
	}

	/** Saves an import the server accepted. Returns its slug. */
	public static String save(String text) {
		try {
			String slug = slug(Blueprint.parse("history/probe", text, "imported").name);
			Files.createDirectories(folder());
			Files.writeString(folder().resolve(slug + ".jugbp.json"), text, StandardCharsets.UTF_8);
			return slug;
		} catch (Exception e) {
			Jugcraft.LOGGER.warn("Could not save the imported blueprint", e);
			return "";
		}
	}

	public static void remove(String slug) {
		try {
			Files.deleteIfExists(folder().resolve(slug + ".jugbp.json"));
		} catch (Exception e) {
			Jugcraft.LOGGER.warn("Could not remove saved blueprint {}", slug, e);
		}
	}
}
