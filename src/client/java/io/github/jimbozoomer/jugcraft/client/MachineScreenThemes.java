package io.github.jimbozoomer.jugcraft.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.jspecify.annotations.Nullable;

/**
 * The look of each machine's screen (docs/features/machine-screens.md): which of the five looks by tech level it uses
 * (workshop, industrial, electric, lab, advanced) and that look's colours, from assets/jugcraft/gui/machine_themes.json
 * (written by tools/gui_textures.py, which also draws the backgrounds). Industrial forms all use one look, the file's
 * "forms". The other machine screens ({@link ScreenKit}) take theirs from its "screens", by name.
 */
public final class MachineScreenThemes {
	private static final Identifier FILE = Jugcraft.id("gui/machine_themes.json");
	/** Used when the file is missing or a machine is not in it: the industrial look's colours. */
	public static final Theme FALLBACK = new Theme("industrial", Jugcraft.id("textures/gui/machine_industrial.png"), 0xFF60E66E,
			0xFF308C3E, 0xFFFF6046, 0xFF60E66E, 0xFF60E66E, 0xFF96FFA0, 0xFF18191A, 0xFF60E66E, 0xFF0A0B0C, 0xFF1A1C1D,
			0xFF707477, 0xFF24A84C, 0xFF125C28, 0xFF78F096, 0xFFE6FFEC, false, "industrial",
			new Frame(new int[] {0xFF2E3032, 0xFF585B5E, 0xFF7C7F82, 0xFFA0A3A6, 0xFFC4C7C9}, 0xFF18191A, 0xFF101213, 0xFF141716,
					0xFF707477, 0, 0xFF101213));
	private static @Nullable Map<String, Theme> machines;
	private static final Map<String, Theme> screens = new HashMap<>();
	private static Theme forms = FALLBACK;

	private MachineScreenThemes() {
	}

	/**
	 * A look: its name and background, then its colours (ARGB): screen text bright and dim, warnings, all's-well, the
	 * energy and progress fills, the title on the body, the help box's text, the slots (dark edge, face, light edge),
	 * the buttons (face, edge, highlight, icon), and whether slots and buttons are thin outlines (the screen-like
	 * looks) rather than sunken bevels. The other screens also draw with its shape (whose casing and panes: a look's own
	 * name, or "advanced" for the drone screens) and its frame colours.
	 */
	public record Theme(String look, Identifier texture, int text, int dim, int warn, int ok, int energy, int progress, int label,
			int help, int slotDark, int slotFace, int slotLight, int buttonFace, int buttonEdge, int buttonLight, int buttonIcon,
			boolean outlined, String shape, Frame frame) {
	}

	/**
	 * A look's body and pane colours, from the machine backgrounds' palette: the casing's five tones (darkest first),
	 * its outline, a pane's glass, grid lines and rim, the workshop's scan band (0 for none) and the help box.
	 */
	public record Frame(int[] casing, int outline, int screen, int grid, int rim, int band, int helpBox) {
	}

	public static Theme of(MachineKind kind) {
		return load().getOrDefault(kind.id, FALLBACK);
	}

	/** The look every industrial form's screen uses. */
	public static Theme forms() {
		load();
		return forms;
	}

	/** The look of one of the other machine screens ({@link ScreenKit}): "prospector", "rocket_pad" and so on. */
	public static Theme screen(String name) {
		load();
		return screens.getOrDefault(name, FALLBACK);
	}

	private static Map<String, Theme> load() {
		if (machines != null) {
			return machines;
		}
		Map<String, Theme> out = new HashMap<>();
		machines = out;
		Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(FILE);
		if (resource.isEmpty()) {
			Jugcraft.LOGGER.warn("Missing {}: machine screens use the default look", FILE);
			return out;
		}
		try (Reader reader = resource.get().openAsReader()) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			Map<String, Theme> looks = new HashMap<>();
			for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("themes").entrySet()) {
				JsonObject t = entry.getValue().getAsJsonObject();
				looks.put(entry.getKey(), new Theme(entry.getKey(), Jugcraft.id("textures/gui/machine_" + entry.getKey() + ".png"),
						color(t, "text"), color(t, "dim"), color(t, "warn"), color(t, "ok"), color(t, "energy"),
						color(t, "progress"), color(t, "label"), color(t, "help"), color(t, "slot_dark"), color(t, "slot_face"),
						color(t, "slot_light"), color(t, "button_face"), color(t, "button_edge"), color(t, "button_light"),
						color(t, "button_icon"), t.has("outlined") && t.get("outlined").getAsBoolean(),
						t.has("shape") ? t.get("shape").getAsString() : entry.getKey(), frame(t)));
			}
			for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("machines").entrySet()) {
				Theme theme = looks.get(entry.getValue().getAsString());
				if (theme != null) {
					out.put(entry.getKey(), theme);
				}
			}
			if (root.has("forms") && looks.containsKey(root.get("forms").getAsString())) {
				forms = looks.get(root.get("forms").getAsString());
			}
			if (root.has("screens")) {
				for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("screens").entrySet()) {
					Theme theme = looks.get(entry.getValue().getAsString());
					if (theme != null) {
						screens.put(entry.getKey(), theme);
					}
				}
			}
		} catch (Exception e) {
			Jugcraft.LOGGER.warn("Could not read {}", FILE, e);
		}
		return out;
	}

	private static int color(JsonObject theme, String key) {
		return (int) Long.parseLong(theme.get(key).getAsString(), 16);
	}

	/** A look's frame colours; the industrial look's where the file has none. */
	private static Frame frame(JsonObject theme) {
		if (!theme.has("casing")) {
			return FALLBACK.frame();
		}
		int[] casing = new int[5];
		for (int i = 0; i < casing.length; i++) {
			casing[i] = (int) Long.parseLong(theme.getAsJsonArray("casing").get(i).getAsString(), 16);
		}
		return new Frame(casing, color(theme, "outline"), color(theme, "screen"), color(theme, "grid"), color(theme, "frame"),
				theme.has("band") ? color(theme, "band") : 0, color(theme, "help_box"));
	}
}
