package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.jimtafacts.industry.CompactReactor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The compact (component) reactor, in the advanced look (docs/features/new-screen-styles.md): the chamber's component
 * grid on the glass (columns not yet unlocked by adjacent chambers shaded), the heat gauge beside it, and the start/stop
 * key with the output and heat under it. The status lamp is green while it runs and red on a fault. Laid out at
 * 232 x 244 and scaled down to fit a smaller window.
 */
public final class CompactReactorScreen extends FittedContainerScreen<CompactReactor.Menu> {
    private ScreenKit.TextButton control;
    private final MachineScreenThemes.Theme theme = MachineScreenThemes.screen("compact_reactor");

    public CompactReactorScreen(CompactReactor.Menu menu, Inventory inventory, Component title) { super(menu, inventory, title, 232, 244); }

    @Override protected void init() {
        super.init(); titleLabelY = inventoryLabelY = 10000;
        control = addRenderableWidget(new ScreenKit.TextButton(leftPos + 6, topPos + 138, 70, 15, theme, Component.literal("Start"),
                button -> { if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0); }));
        refresh();
    }

    @Override protected void containerTick() { super.containerTick(); refresh(); }

    private void refresh() {
        boolean running = menu.data.get(4) != 0;
        control.setMessage(Component.literal(running ? "Stop" : menu.data.get(5) != 0 ? "Reset / start" : "Start"));
        control.setSelected(running);
        control.setTooltip(Tooltip.create(Component.literal("Stop before changing components. Redstone pauses fuel. Cool below 50% after a fault. "
                + (menu.data.get(7) == 1 ? "Fluid vessel: " + menu.data.get(8) + " mB coolant / " + menu.data.get(9) + " mB hot coolant. No JE is generated in this mode."
                : "Electrical mode. A complete 5×5×5 vessel enables coolant heat production."))));
    }

    @Override protected void background(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        boolean fault = menu.data.get(5) != 0, running = menu.data.get(4) != 0;
        ScreenKit.frame(g, theme, leftPos, topPos, imageWidth, imageHeight, menu);
        ScreenKit.pane(g, theme, leftPos + 21, topPos + 19, 170, 117);
        ScreenKit.pane(g, theme, leftPos + 197, topPos + 19, 28, 117);
        ScreenKit.slots(g, theme, leftPos, topPos, menu);
        ScreenKit.title(g, font, theme, leftPos, topPos, Component.literal("Component Reactor"), fault ? theme.warn() : running ? theme.ok() : theme.dim(), imageWidth - 8);
        // Columns not yet unlocked by adjacent chambers.
        int columns = menu.data.get(3);
        for (int c = columns; c < 9; c++) g.fill(leftPos + 25 + c * 18, topPos + 24, leftPos + 43 + c * 18, topPos + 132, (theme.frame().screen() & 0x00FFFFFF) | 0xC0000000);
        // The heat gauge: green below half, red from half up.
        int heat = menu.data.get(0), max = Math.max(1, menu.data.get(1));
        g.fill(leftPos + 204, topPos + 23, leftPos + 218, topPos + 131, theme.slotDark());
        g.fill(leftPos + 205, topPos + 24, leftPos + 217, topPos + 130, theme.slotFace());
        int fill = Math.clamp(heat * 104L / max, 0, 104);
        g.fill(leftPos + 206, topPos + 129 - fill, leftPos + 216, topPos + 129, heat * 2 >= max ? theme.warn() : theme.ok());
    }

    @Override protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        boolean fault = menu.data.get(5) != 0;
        String status = fault ? "FAULT — cool down" : (menu.data.get(7) == 1 ? (menu.data.get(2) * 200) + " heat/s  " : (menu.data.get(2) * 5) + " JE/t  ")
                + (menu.data.get(0) * 100 / Math.max(1, menu.data.get(1))) + "% heat";
        g.text(font, status, 82, 142, fault ? theme.warn() : theme.text(), false);
    }
}
