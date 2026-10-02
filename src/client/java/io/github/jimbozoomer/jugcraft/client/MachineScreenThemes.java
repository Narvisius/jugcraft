package io.github.jimbozoomer.jugcraft.client;

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
 * The look of each machine's screen (batch 22): which theme it uses and the theme's colours, from
 * assets/jugcraft/gui/machine_themes.json (written by tools/gui_textures.py, which also draws the backgrounds).
 */
public final class MachineScreenThemes {
	private static final Identifier FILE = Jugcraft.id("gui/machine_themes.json");
	/** Used when the file is missing or a machine is not in it: the dieselpunk theme's colours. */
	public static final Theme FALLBACK = new Theme(Jugcraft.id("textures/gui/machine_diesel.png"), 0xFFFFB040, 0xFFB07028,
			0xFFFF6040, 0xFFF0A020, 0xFFFFC860, 0xFFE8C890, 0xFF1C1612, 0xFF28201A, 0xFF6C5846);
	private static @Nullable Map<String, Theme> machines;

	private MachineScreenThemes() {
	}

	/** Background texture and colours (ARGB): terminal text, dim text, warnings, energy and progress fills, labels, slots. */
	public record Theme(Identifier texture, int text, int dim, int warn, int energy, int progress, int label,
			int slotDark, int slotFace, int slotLight) {
	}

	public static Theme of(MachineKind kind) {
		if (machines == null) {
			machines = load();
		}
		return machines.getOrDefault(kind.id, FALLBACK);
	}

	private static Map<String, Theme> load() {
		Map<String, Theme> out = new HashMap<>();
		Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(FILE);
		if (resource.isEmpty()) {
			Jugcraft.LOGGER.warn("Missing {}: machine screens use the default theme", FILE);
			return out;
		}
		try (Reader reader = resource.get().openAsReader()) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			Map<String, Theme> themes = new HashMap<>();
			for (Map.Entry<String, com.google.gson.JsonElement> entry : root.getAsJsonObject("themes").entrySet()) {
				JsonObject t = entry.getValue().getAsJsonObject();
				themes.put(entry.getKey(), new Theme(Jugcraft.id("textures/gui/machine_" + entry.getKey() + ".png"),
						color(t, "text"), color(t, "dim"), color(t, "warn"), color(t, "energy"), color(t, "progress"),
						color(t, "label"), color(t, "slot_dark"), color(t, "slot_face"), color(t, "slot_light")));
			}
			for (Map.Entry<String, com.google.gson.JsonElement> entry : root.getAsJsonObject("machines").entrySet()) {
				Theme theme = themes.get(entry.getValue().getAsString());
				if (theme != null) {
					out.put(entry.getKey(), theme);
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
}
