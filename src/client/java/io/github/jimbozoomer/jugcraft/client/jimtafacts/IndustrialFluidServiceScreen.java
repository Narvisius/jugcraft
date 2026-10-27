package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import io.github.jimbozoomer.jugcraft.client.MachineScreenThemes;
import io.github.jimbozoomer.jugcraft.client.ScreenKit;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialFluidServices;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The industrial pumps, routers and distillers, in the industrial look (docs/features/new-screen-styles.md): the fluid,
 * its tank gauges and the power on the glass, and under the inventory the rate and mode keys (pumps and routers) or the
 * state (distillers). The status lamp is green while it works and amber when something stops it. Click-only controls.
 */
public final class IndustrialFluidServiceScreen extends AbstractContainerScreen<IndustrialFluidServices.Menu> implements ScreenKit.Fitted {
    private static final String[] STATES = {"Working", "Access denied", "Redstone pause", "Needs power", "Input / output blocked", "Needs open daylight"};
    private ScreenKit.TextButton slower, faster, mode;
    private final MachineScreenThemes.Theme theme = MachineScreenThemes.screen("industrial_fluid_service");

    public IndustrialFluidServiceScreen(IndustrialFluidServices.Menu menu, Inventory inv, Component title) { super(menu, inv, title, 176, 218); }

    private void action(int id) { if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id); }

    @Override protected void init() {
        super.init(); titleLabelY = inventoryLabelY = 10000;
        slower = addRenderableWidget(new ScreenKit.TextButton(leftPos + 8, topPos + 170, 22, 18, theme, Component.literal("−"), b -> action(0)));
        faster = addRenderableWidget(new ScreenKit.TextButton(leftPos + 146, topPos + 170, 22, 18, theme, Component.literal("+"), b -> action(1)));
        mode = addRenderableWidget(new ScreenKit.TextButton(leftPos + 8, topPos + 192, 160, 18, theme, Component.empty(), b -> action(2)));
    }

    @Override protected void containerTick() {
        super.containerTick(); int kind = menu.data.get(0); boolean routing = kind < 2;
        slower.visible = faster.visible = mode.visible = routing;
        slower.active = menu.data.get(4) > 0; faster.active = menu.data.get(4) < IndustrialFluidServices.RATES.length - 1;
        mode.setMessage(Component.literal(kind == 0 ? (menu.data.get(5) == 0 ? "Per second" : "Per tick") : (menu.data.get(5) == 0 ? "Spread across outlets" : "One outlet at a time")));
        mode.setTooltip(Tooltip.create(Component.literal(kind == 0 ? "10 JE per successful delivery. The front is the output; all other faces take fluid. Redstone pauses."
                : "The front is the inlet. Other faces are outlets. Rotates priority each second; never loads chunks. Redstone pauses.")));
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) { extractBackground(g, mx, my, dt); super.extractRenderState(g, mx, my, dt); extractTooltip(g, mx, my); }

    @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float dt) {
        int kind = Math.clamp(menu.data.get(0), 0, 3);
        int state = Math.clamp(menu.data.get(6), 0, STATES.length - 1);
        ScreenKit.frame(g, theme, leftPos, topPos, imageWidth, imageHeight, menu);
        ScreenKit.pane(g, theme, leftPos + 4, topPos + 14, imageWidth - 8, 63);
        ScreenKit.pane(g, theme, leftPos + 4, topPos + 166, imageWidth - 8, 48);
        ScreenKit.slots(g, theme, leftPos, topPos, menu);
        int lamp = kind >= 2 ? (state == 0 ? theme.ok() : theme.warn()) : menu.data.get(1) > 0 ? theme.ok() : theme.dim();
        ScreenKit.title(g, font, theme, leftPos, topPos, title, lamp, imageWidth - 8);
        String fluid = menu.data.get(7) == 0 ? "Empty" : BuiltInRegistries.FLUID.getKey(BuiltInRegistries.FLUID.byId(menu.data.get(7))).getPath().replace('_', ' ');
        g.text(font, ScreenKit.fit(font, fluid, 156), leftPos + 10, topPos + 18, theme.text(), false);
        g.text(font, "In: " + menu.data.get(1) + " / 10,000 mB", leftPos + 10, topPos + 29, theme.text(), false);
        ScreenKit.bar(g, theme, leftPos + 10, topPos + 39, 156, 5, menu.data.get(1) / 10000.0, 0xff527eac);
        if (kind >= 2) {
            g.text(font, "Out: " + menu.data.get(2) + " mB distilled", leftPos + 10, topPos + 50, theme.text(), false);
            ScreenKit.bar(g, theme, leftPos + 10, topPos + 60, 156, 5, menu.data.get(2) / 10000.0, 0xffa0c8db);
        } else {
            g.text(font, kind == 0 ? menu.data.get(3) + " / 10,000 JE" : "No electricity required", leftPos + 10, topPos + 52, theme.dim(), false);
        }
        if (kind < 2) {
            int rate = IndustrialFluidServices.RATES[Math.clamp(menu.data.get(4), 0, 6)];
            String text = rate + " mB / delivery";
            g.text(font, text, leftPos + 88 - font.width(text) / 2, topPos + 175, theme.text(), false);
        } else {
            g.text(font, STATES[state], leftPos + 9, topPos + 171, state == 0 ? theme.text() : theme.warn(), false);
            g.text(font, kind == 2 ? "1 water → 1 distilled / 72 ticks" : "10 steam → 1 distilled", leftPos + 9, topPos + 185, theme.dim(), false);
            g.text(font, "Input: sides · Output: bottom", leftPos + 9, topPos + 199, theme.dim(), false);
        }
    }

    @Override protected void extractLabels(GuiGraphicsExtractor g, int x, int y) {}

    @Override public ScreenRectangle panelArea() { return new ScreenRectangle(leftPos, topPos, imageWidth, imageHeight); }
}
