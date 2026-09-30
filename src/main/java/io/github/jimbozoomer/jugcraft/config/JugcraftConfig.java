package io.github.jimbozoomer.jugcraft.config;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Server-side feature switches, read once at startup from config/jugcraft.properties.
 * Switches disable acquisition (worldgen and recipes); they never unregister content.
 */
public final class JugcraftConfig {
	private static final String FILE_NAME = "jugcraft.properties";
	private static final String TIN_KEY = "tin.enabled";

	private static boolean tinEnabled = true;

	private JugcraftConfig() {
	}

	public static boolean tinEnabled() {
		return tinEnabled;
	}

	public static boolean isFeatureEnabled(String feature) {
		return switch (feature) {
			case "tin" -> tinEnabled;
			default -> false;
		};
	}

	public static void load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
		Properties properties = new Properties();

		if (Files.isRegularFile(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				properties.load(reader);
			} catch (IOException e) {
				Jugcraft.LOGGER.error("Could not read {}; using defaults", path, e);
			}
		}

		tinEnabled = Boolean.parseBoolean(properties.getProperty(TIN_KEY, "true"));
		properties.setProperty(TIN_KEY, Boolean.toString(tinEnabled));

		try (Writer writer = Files.newBufferedWriter(path)) {
			properties.store(writer, "Jugcraft feature switches. false stops new worldgen and recipes; existing items and blocks stay.");
		} catch (IOException e) {
			Jugcraft.LOGGER.warn("Could not write {}", path, e);
		}
	}
}
