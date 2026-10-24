package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.menagerie.CaveWorkshop;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The cave survey desk, in the workshop look (docs/features/bench-screens.md): the expeditions as keys in one pane, the
 * survey's progress and result on the map glass under them, and a help strip. The status lamp is lit while it surveys,
 * green when it found a cave and red when it could not. Survey controls are menu buttons; progress and location are kept
 * in the authoritative menu.
 */
public final class CaveWorkshopScreen extends AbstractContainerScreen<CaveWorkshop> implements ScreenKit.Fitted {
    private static final int SURVEY_Y = 119;
    private final MachineScreenThemes.Theme theme = MachineScreenThemes.screen("cave_survey");

    public CaveWorkshopScreen(CaveWorkshop menu, Inventory inv, Component title) { super(menu, inv, title, 300, 220); }

    private static Component t(String key, Object... args) { return Component.translatable("screen.jugcraft.cave_survey." + key, args); }

    @Override protected void init() {
        super.init();
        for (int i = 0; i < CaveWorkshop.NAMES.size(); i++) {
            final int choice = i;
            addRenderableWidget(new ScreenKit.TextButton(leftPos + 12 + (i % 2) * 140, topPos + 35 + (i / 2) * 26, 134, 22, theme,
                    Component.literal(CaveWorkshop.NAMES.get(i)),
                    b -> { if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, choice); }));
        }
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float partial) { extractBackground(g, x, y, partial); super.extractRenderState(g, x, y, partial); }

    @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float partial) {
        int x = leftPos, y = topPos;
        ScreenKit.body(g, theme, x, y, imageWidth, imageHeight);
        ScreenKit.pane(g, font, theme, x + 6, y + 15, imageWidth - 12, SURVEY_Y - 19, t("choose").getString());
        // The survey on the map glass: what it is doing, and how far it has got.
        ScreenKit.pane(g, font, theme, x + 6, y + SURVEY_Y, imageWidth - 12, 64, t("heading").getString());
        ScreenKit.bar(g, theme, x + 12, y + SURVEY_Y + 33, imageWidth - 24, 6, menu.status() == 2 ? 1 : menu.progress() / 100.0,
                menu.status() == 2 ? theme.ok() : theme.progress());
        ScreenKit.help(g, font, theme, x + 6, y + SURVEY_Y + 68, imageWidth - 12, imageHeight - SURVEY_Y - 74, t("help"));
        int status = menu.status();
        MachineSkin.lamp(g, x + 4, y + 3, status == 1 ? theme.progress() : status == 2 ? theme.ok() : status == 3 || status >= 5 ? theme.warn() : theme.dim());
    }

    @Override protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) {
        g.text(font, t("title"), 12, 2, theme.label(), false);
        int status = menu.status();
        if (status == 1) ScreenKit.chip(g, font, theme, imageWidth - 4, 1, menu.progress() + "%", theme.progress());
        else if (status == 2) ScreenKit.chip(g, font, theme, imageWidth - 4, 1, t("found").getString(), theme.ok());
        Component line = switch (status) {
            case 1 -> t("status.1", menu.progress());
            case 2 -> t("status.2", menu.targetX(), menu.targetZ());
            case 3, 5, 6 -> t("status." + status);
            default -> t("status.0");
        };
        int colour = status == 2 ? theme.ok() : status == 3 || status >= 5 ? theme.warn() : theme.text();
        g.text(font, ScreenKit.fit(font, line.getString(), imageWidth - 24), 12, SURVEY_Y + 20, colour, false);
    }

    @Override public ScreenRectangle panelArea() { return new ScreenRectangle(leftPos, topPos, imageWidth, imageHeight); }
}
