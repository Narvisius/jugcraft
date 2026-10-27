package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import io.github.jimbozoomer.jugcraft.client.MachineScreenThemes;
import io.github.jimbozoomer.jugcraft.client.ScreenKit;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.WorkshopEngines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The workshop engines (stirling, combustion, electric, dynamo and the rest), in the workshop look
 * (docs/features/new-screen-styles.md): the stored JE and what the engine burns or turns on the glass, and its state in a
 * strip under the inventory. The status lamp is green while it runs, amber while it cools after overheating.
 */
public final class WorkshopEngineScreen extends AbstractContainerScreen<WorkshopEngines.Menu> implements ScreenKit.Fitted {
    private final MachineScreenThemes.Theme theme = MachineScreenThemes.screen("workshop_engine");

    public WorkshopEngineScreen(WorkshopEngines.Menu menu, Inventory inv, Component title) { super(menu, inv, title, 176, 202); }

    @Override protected void init() { super.init(); titleLabelY = inventoryLabelY = 10000; }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) { extractBackground(g, mx, my, dt); super.extractRenderState(g, mx, my, dt); extractTooltip(g, mx, my); }

    @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float dt) {
        int kind = Math.clamp(menu.data.get(0), 0, 5);
        boolean cooling = menu.data.get(6) == 1, unpowered = menu.data.get(8) == 0;
        ScreenKit.frame(g, theme, leftPos, topPos, imageWidth, imageHeight, menu);
        ScreenKit.pane(g, theme, leftPos + 4, topPos + 14, imageWidth - 8, 73);
        ScreenKit.pane(g, theme, leftPos + 4, topPos + 174, imageWidth - 8, 25);
        ScreenKit.slots(g, theme, leftPos, topPos, menu);
        ScreenKit.title(g, font, theme, leftPos, topPos, title, cooling ? theme.warn() : unpowered ? theme.dim() : theme.ok(), imageWidth - 8);
        g.text(font, menu.data.get(1) + " / 32,000 JE", leftPos + 8, topPos + 21, theme.text(), false);
        if (kind == 1) {
            g.text(font, "Burn: " + menu.data.get(2) + " ticks", leftPos + 31, topPos + 44, theme.text(), false);
            g.text(font, "Ash output", leftPos + 31, topPos + 64, theme.dim(), false);
        } else if (kind == 2) {
            g.text(font, "Fuel: " + menu.data.get(3) + " mB", leftPos + 8, topPos + 39, theme.text(), false);
            g.text(font, "Water: " + menu.data.get(4) + " mB (bottom)", leftPos + 8, topPos + 52, theme.text(), false);
            g.text(font, "Heat: " + menu.data.get(5) + " / 100", leftPos + 8, topPos + 66, theme.text(), false);
        } else {
            String[] lines = kind == 5 ? new String[] {menu.data.get(7) + " / 4,096 KE", "Back shaft → JE at 85%"}
                    : kind == 0 ? new String[] {"1 JE per second", "Starter redstone piston"}
                    : kind == 4 ? new String[] {"2,048 JE per tick", "Creative owner required"}
                    : new String[] {"JE in: other faces", "JE out: piston end"};
            for (int i = 0; i < lines.length; i++) g.text(font, lines[i], leftPos + 8, topPos + 43 + 14 * i, i == 0 ? theme.text() : theme.dim(), false);
        }
        String status = cooling ? "Cooling after overheat" : unpowered ? "Supply redstone to run" : "Running when resources fit";
        g.text(font, status, leftPos + 9, topPos + 177, cooling ? theme.warn() : theme.text(), false);
        g.text(font, "Power leaves the piston end", leftPos + 9, topPos + 188, theme.dim(), false);
    }

    @Override protected void extractLabels(GuiGraphicsExtractor g, int x, int y) {}

    @Override public ScreenRectangle panelArea() { return new ScreenRectangle(leftPos, topPos, imageWidth, imageHeight); }
}
