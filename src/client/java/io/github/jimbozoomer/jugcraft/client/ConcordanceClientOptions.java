package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.sign.Presentation;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

/**
 * This player's own Arcane Concordance display settings ({@code config/jugcraft-client.properties}): whether the
 * Focus line shows while an instrument is held, reduced motion (fewer, calmer particles and no sliding in the journal),
 * the visual intensity (how many particles signs and blocks draw: {@link Presentation.Intensity}), whether the journal
 * shows exact values beside its descriptions, whether it always opens as the simple screen, and whether this player's
 * worn trinkets (Wayfaring's belt and boots) are drawn on them. Presentation only. The server sees only the last, which
 * it shows to everyone who sees this player ({@link WayfaringClient#sendChoice}); it never sees the others. Changed from
 * {@link ConcordanceSettingsScreen} and the journal.
 */
public final class ConcordanceClientOptions {
	private static final String FILE_NAME = "jugcraft-client.properties";
	private static boolean hud = true;
	private static boolean reducedMotion;
	private static boolean exactValues;
	private static boolean simpleJournal;
	private static boolean wornTrinkets = true;
	private static Presentation.Intensity intensity = Presentation.Intensity.FULL;

	private ConcordanceClientOptions() {
	}

	public static boolean hud() {
		return hud;
	}

	public static boolean reducedMotion() {
		return reducedMotion;
	}

	public static boolean exactValues() {
		return exactValues;
	}

	public static boolean simpleJournal() {
		return simpleJournal;
	}

	/**
	 * Whether this player's worn trinkets are drawn on them, for everyone who sees them: sent to the server
	 * ({@link WayfaringClient#sendChoice}), which tells every client
	 * (io.github.jimbozoomer.jugcraft.concordance.trinket.WornDisplay).
	 */
	public static boolean wornTrinkets() {
		return wornTrinkets;
	}

	public static Presentation.Intensity intensity() {
		return intensity;
	}

	public static void set(boolean showHud, boolean calm, boolean exact, boolean simple, boolean trinkets, Presentation.Intensity chosen) {
		hud = showHud;
		reducedMotion = calm;
		exactValues = exact;
		simpleJournal = simple;
		wornTrinkets = trinkets;
		intensity = chosen;
		Presentation.configure(intensity, reducedMotion);
		save();
		WayfaringClient.sendChoice();
	}

	/** Whether this player's worn trinkets are drawn (the settings screen's switch, also turned by the client game test). */
	public static void setWornTrinkets(boolean shown) {
		wornTrinkets = shown;
		save();
		WayfaringClient.sendChoice();
	}

	/** The journal's own switch for exact values (kept with the other settings). */
	public static void setExactValues(boolean exact) {
		exactValues = exact;
		save();
	}

	static void load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
		Properties properties = new Properties();
		if (Files.isRegularFile(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				properties.load(reader);
			} catch (IOException e) {
				Jugcraft.LOGGER.error("Could not read {}; using defaults", path, e);
			}
		}
		hud = !"false".equalsIgnoreCase(properties.getProperty("concordance.hud", "true").trim());
		reducedMotion = Boolean.parseBoolean(properties.getProperty("concordance.reduced_motion", "false"));
		exactValues = Boolean.parseBoolean(properties.getProperty("concordance.exact_values", "false"));
		simpleJournal = Boolean.parseBoolean(properties.getProperty("concordance.simple_journal", "false"));
		// The two settings on at first are off only when they say so: a mistyped value leaves them on.
		wornTrinkets = !"false".equalsIgnoreCase(properties.getProperty("concordance.worn_trinkets", "true").trim());
		intensity = Presentation.Intensity.fromId(properties.getProperty("concordance.visual_intensity", "full"));
		Presentation.configure(intensity, reducedMotion);
		save();
	}

	private static void save() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
		Properties properties = new Properties();
		properties.setProperty("concordance.hud", Boolean.toString(hud));
		properties.setProperty("concordance.reduced_motion", Boolean.toString(reducedMotion));
		properties.setProperty("concordance.exact_values", Boolean.toString(exactValues));
		properties.setProperty("concordance.simple_journal", Boolean.toString(simpleJournal));
		properties.setProperty("concordance.worn_trinkets", Boolean.toString(wornTrinkets));
		properties.setProperty("concordance.visual_intensity", intensity.id());
		try (Writer writer = Files.newBufferedWriter(path)) {
			properties.store(writer, "Jugcraft client display settings (kept on this computer): concordance.hud shows Focus while"
					+ " a Concordance instrument is held; concordance.reduced_motion makes Kindled light, the bench and the journal"
					+ " calmer; concordance.exact_values shows exact figures in the journal; concordance.simple_journal always"
					+ " opens the plain journal screen; concordance.worn_trinkets draws your worn trinkets (Wayfaring's belt and"
					+ " boots) on you, unless armour covers them, and everyone who sees you sees the same;"
					+ " concordance.visual_intensity (full, reduced or minimal) is how many particles the Concordance draws"
					+ " (warnings always show).");
		} catch (IOException e) {
			Jugcraft.LOGGER.warn("Could not write {}", path, e);
		}
	}
}
