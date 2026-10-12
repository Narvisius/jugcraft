package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.concordance.sign.Presentation;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * The Concordance's client settings screen, built with Cloth Config. Opened from Mod Menu when it is installed, or with
 * the (unbound by default) "Concordance settings" key in Controls.
 */
public final class ConcordanceSettingsScreen {
	private ConcordanceSettingsScreen() {
	}

	public static Screen create(@Nullable Screen parent) {
		boolean[] values = {ConcordanceClientOptions.hud(), ConcordanceClientOptions.reducedMotion(), ConcordanceClientOptions.exactValues(),
				ConcordanceClientOptions.simpleJournal(), ConcordanceClientOptions.wornTrinkets()};
		ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent)
				.setTitle(Component.translatable("screen.jugcraft.concordance.config.title"));
		ConfigEntryBuilder entries = builder.entryBuilder();
		ConfigCategory display = builder.getOrCreateCategory(Component.translatable("screen.jugcraft.concordance.config.title"));
		display.addEntry(entries.startBooleanToggle(Component.translatable("screen.jugcraft.concordance.config.hud"), values[0])
				.setDefaultValue(true)
				.setTooltip(Component.translatable("screen.jugcraft.concordance.config.hud.tooltip"))
				.setSaveConsumer(value -> values[0] = value)
				.build());
		display.addEntry(entries.startBooleanToggle(Component.translatable("screen.jugcraft.concordance.config.reduced_motion"), values[1])
				.setDefaultValue(false)
				.setTooltip(Component.translatable("screen.jugcraft.concordance.config.reduced_motion.tooltip"))
				.setSaveConsumer(value -> values[1] = value)
				.build());
		display.addEntry(entries.startBooleanToggle(Component.translatable("screen.jugcraft.concordance.config.exact"), values[2])
				.setDefaultValue(false)
				.setTooltip(Component.translatable("screen.jugcraft.concordance.config.exact.tooltip"))
				.setSaveConsumer(value -> values[2] = value)
				.build());
		display.addEntry(entries.startBooleanToggle(Component.translatable("screen.jugcraft.concordance.config.simple_journal"), values[3])
				.setDefaultValue(false)
				.setTooltip(Component.translatable("screen.jugcraft.concordance.config.simple_journal.tooltip"))
				.setSaveConsumer(value -> values[3] = value)
				.build());
		display.addEntry(entries.startBooleanToggle(Component.translatable("screen.jugcraft.concordance.config.worn_trinkets"), values[4])
				.setDefaultValue(true)
				.setTooltip(Component.translatable("screen.jugcraft.concordance.config.worn_trinkets.tooltip"))
				.setSaveConsumer(value -> values[4] = value)
				.build());
		Presentation.Intensity[] intensity = {ConcordanceClientOptions.intensity()};
		display.addEntry(entries.startSelector(Component.translatable("screen.jugcraft.concordance.config.intensity"),
						Presentation.Intensity.values(), intensity[0])
				.setDefaultValue(Presentation.Intensity.FULL)
				.setNameProvider(value -> Component.translatable("screen.jugcraft.concordance.config.intensity." + value.id()))
				.setTooltip(Component.translatable("screen.jugcraft.concordance.config.intensity.tooltip"))
				.setSaveConsumer(value -> intensity[0] = value)
				.build());
		builder.setSavingRunnable(() -> ConcordanceClientOptions.set(values[0], values[1], values[2], values[3], values[4], intensity[0]));
		return builder.build();
	}
}
