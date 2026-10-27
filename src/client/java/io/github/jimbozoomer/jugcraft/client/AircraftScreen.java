package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.aircraft.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * An aircraft's equipment and cargo, in the industrial look (docs/features/new-screen-styles.md): the source layout's
 * sockets on the glass, each empty one marked with what it takes, and the fuel and engine in a line under them (with
 * the socket under the mouse named at its right). The status lamp is green while the engine runs and amber when there is
 * no fuel. Laid out at the layout's own size and scaled down to fit a smaller window: the cargo airship keeps all 80
 * slots.
 */
public final class AircraftScreen extends FittedContainerScreen<AircraftMenu> {
    private final MachineScreenThemes.Theme theme = MachineScreenThemes.screen("aircraft");

    public AircraftScreen(AircraftMenu menu, Inventory inv, Component title) { super(menu, inv, title, menu.spec.width(), menu.spec.inventoryY() + 80); }

    @Override protected void init() { super.init(); titleLabelY = inventoryLabelY = 10000; }

    @Override protected void background(GuiGraphicsExtractor g, int mx, int my, float delta) {
        int x = leftPos, y = topPos, inventory = menu.spec.inventoryY();
        boolean pedal = menu.spec.name().equals("gyrodyne");
        ScreenKit.frame(g, theme, x, y, imageWidth, imageHeight, menu);
        ScreenKit.pane(g, theme, x + 4, y + 14, imageWidth - 8, inventory - 28);
        ScreenKit.slots(g, theme, x, y, menu);
        ScreenKit.title(g, font, theme, x, y, title, menu.engine() > 0 ? theme.ok() : !pedal && menu.fuel() == 0 ? theme.warn() : theme.dim(), imageWidth - 8);
        String hovered = "";
        for (int i = 0; i < menu.spec.sockets().size(); i++) {
            var s = menu.spec.sockets().get(i);
            if (s.type().equals("inventory")) continue;
            String icon = switch (s.type()) { case "boiler" -> "F"; case "booster" -> "+"; case "upgrade" -> "U"; case "weapon" -> "W"; case "dye" -> "D"; default -> "B"; };
            if (menu.slots.get(i).getItem().isEmpty()) g.text(font, icon, x + s.x() + 5, y + s.y() + 4, theme.dim(), false);
            if (mx >= x + s.x() && mx < x + s.x() + 16 && my >= y + s.y() && my < y + s.y() + 16) hovered = s.type().equals("boiler") ? "Fuel" : s.type();
        }
        String status = (pedal ? "Pedal power" : "Fuel " + menu.fuel() / 20 + "s") + "  ·  Engine " + menu.engine() + "%";
        g.text(font, ScreenKit.fit(font, status, imageWidth - 24 - font.width(hovered)), x + 9, y + inventory - 24, !pedal && menu.fuel() == 0 ? theme.warn() : theme.text(), false);
        if (!hovered.isEmpty()) g.text(font, hovered, x + imageWidth - 9 - font.width(hovered), y + inventory - 24, theme.progress(), false);
    }

    @Override protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) {}
}
