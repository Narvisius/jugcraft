package io.github.jimbozoomer.jugcraft.client.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.jimbozoomer.jugcraft.client.ConcordanceSettingsScreen;

/** Loaded only by Mod Menu's optional entrypoint: Jugcraft's config button opens the Concordance's client settings. */
public final class ConcordanceModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return ConcordanceSettingsScreen::create;
	}
}
