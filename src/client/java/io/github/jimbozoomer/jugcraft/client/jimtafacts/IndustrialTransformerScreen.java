package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import io.github.jimbozoomer.jugcraft.client.MachineScreenThemes;
import io.github.jimbozoomer.jugcraft.client.ScreenKit;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialTransformers;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The industrial transformer, in the electric look (docs/features/new-screen-styles.md): which face is in and which
 * out, the rates and the stored JE on the glass, and the three modes as keys, the one in use lit. The status lamp is
 * lit while it holds energy.
 */
public final class IndustrialTransformerScreen extends AbstractContainerScreen<IndustrialTransformers.Menu> implements ScreenKit.Fitted {
    private final ScreenKit.TextButton[] modes = new ScreenKit.TextButton[3];
    private final MachineScreenThemes.Theme theme = MachineScreenThemes.screen("industrial_transformer");

    public IndustrialTransformerScreen(IndustrialTransformers.Menu menu, Inventory inv, Component title) { super(menu, inv, title, 176, 219); }

    @Override protected void init() {
        super.init(); titleLabelY = inventoryLabelY = 10000;
        String[] names = {"Automatic (redstone)", "Split front → other faces", "Combine faces → front"};
        for (int i = 0; i < 3; i++) {
            int action = i;
            modes[i] = addRenderableWidget(new ScreenKit.TextButton(leftPos + 8, topPos + 66 + 20 * i, 160, 18, theme, Component.literal(names[i]),
                    b -> { if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action); }));
            modes[i].setTooltip(Tooltip.create(Component.literal(i == 0 ? "No signal splits; redstone combines. The marked face is the high-rate port."
                    : "All energy remains JE. No conversion gain, explosion or voltage requirement. Rates are shared across ports each tick.")));
        }
    }

    @Override protected void containerTick() {
        super.containerTick();
        // The mode in use is lit (and has nothing to do when pressed again).
        for (int i = 0; i < 3; i++) { modes[i].active = i != menu.data.get(2); modes[i].setSelected(i == menu.data.get(2)); }
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) { extractBackground(g, mx, my, dt); super.extractRenderState(g, mx, my, dt); extractTooltip(g, mx, my); }

    @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float dt) {
        int low = menu.data.get(0), high = low * 4;
        ScreenKit.frame(g, theme, leftPos, topPos, imageWidth, imageHeight, menu);
        ScreenKit.pane(g, theme, leftPos + 4, topPos + 15, imageWidth - 8, 46);
        ScreenKit.slots(g, theme, leftPos, topPos, menu);
        ScreenKit.title(g, font, theme, leftPos, topPos, title, menu.data.get(1) > 0 ? theme.ok() : theme.dim(), imageWidth - 8);
        g.text(font, menu.data.get(3) == 0 ? "Front IN · other faces OUT" : "Front OUT · other faces IN", leftPos + 10, topPos + 19, theme.text(), false);
        g.text(font, high + " total JE/t · " + low + " per side", leftPos + 10, topPos + 32, theme.dim(), false);
        g.text(font, (menu.data.get(1) * 4) + " / " + (high * 8) + " JE stored", leftPos + 10, topPos + 45, theme.dim(), false);
    }

    @Override protected void extractLabels(GuiGraphicsExtractor g, int x, int y) {}

    @Override public ScreenRectangle panelArea() { return new ScreenRectangle(leftPos, topPos, imageWidth, imageHeight); }
}
