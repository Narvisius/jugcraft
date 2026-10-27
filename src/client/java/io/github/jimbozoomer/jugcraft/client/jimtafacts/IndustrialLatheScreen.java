package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.MachineScreenThemes;
import io.github.jimbozoomer.jugcraft.client.ScreenKit;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialLathe;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The industrial lathe, in the industrial look (docs/features/new-screen-styles.md): the blank, tool and shavings slots
 * and the blank's five-segment profile on the glass, with a cut key over each segment; the stored KE, the state and
 * Finish in a strip under the inventory. The status lamp is green when it is ready or done, amber when it waits on
 * something. No typing: five explicit cut keys.
 */
public final class IndustrialLatheScreen extends AbstractContainerScreen<IndustrialLathe.Menu> implements ScreenKit.Fitted {
    private static final String[] STATUS = {"Ready", "Redstone pause", "Insert blank/tool", "At minimum", "Needs 1,000 KE", "Clear shavings",
            "Cut complete", "Profile unmatched", "Finished"};
    private final MachineScreenThemes.Theme theme = MachineScreenThemes.screen("industrial_lathe");

    public IndustrialLatheScreen(IndustrialLathe.Menu menu, Inventory inv, Component title) { super(menu, inv, title, 176, 192); }

    private void action(int id) { if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id); }

    @Override protected void init() {
        super.init(); titleLabelY = inventoryLabelY = 10000;
        for (int i = 0; i < 5; i++) {
            int part = i;
            var cut = addRenderableWidget(new ScreenKit.TextButton(leftPos + 40 + i * 24, topPos + 15, 23, 18, theme, Component.literal("−"), b -> action(part)));
            cut.setTooltip(Tooltip.create(Component.literal("Cut segment " + (i + 1) + " by one step. Costs 1,000 KE and one tool use. Cuts cannot be undone.")));
        }
        var finish = addRenderableWidget(new ScreenKit.TextButton(leftPos + 116, topPos + 168, 52, 18, theme, Component.literal("Finish"), b -> action(5)));
        finish.setTooltip(Tooltip.create(Component.literal("Finish a matching profile into its component. Take it from the blank slot.")));
        var help = addRenderableWidget(new ScreenKit.TextButton(leftPos + 28, topPos + 57, 12, 16, theme, Component.literal("?"), b -> {}));
        help.setTooltip(Tooltip.create(Component.literal(
            "Top slot: blank. Middle slot: lathing tool. Bottom slot: shavings. Supply KE through a shaft; redstone pauses.\nIron profiles:\n4·3·4·3·4 — fluted iron shaft\n3·2·3·2·3 — thin fluted axle\n4·3·2·2·2 — handle\n5·4·3·2·1 — drill head\nWood: 2·3·2·3·2 — four rods\nAn over-cut blank keeps its shape; it cannot regrow material.")));
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) { extractBackground(g, mx, my, dt); super.extractRenderState(g, mx, my, dt); extractTooltip(g, mx, my); }

    @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float dt) {
        int status = Math.clamp(menu.data.get(1), 0, STATUS.length - 1);
        ScreenKit.frame(g, theme, leftPos, topPos, imageWidth, imageHeight, menu);
        ScreenKit.pane(g, theme, leftPos + 3, topPos + 13, imageWidth - 6, 64);
        ScreenKit.pane(g, theme, leftPos + 4, topPos + 166, imageWidth - 8, 22);
        ScreenKit.slots(g, theme, leftPos, topPos, menu);
        ScreenKit.title(g, font, theme, leftPos, topPos, title,
                status == 0 || status == 6 || status == 8 ? theme.ok() : status == 2 || status == 3 || status == 7 ? theme.dim() : theme.warn(), imageWidth - 8);
        // The blank's profile: each segment's width, drawn to scale in its wood or iron.
        var blank = menu.contents.getItem(1); int max = IndustrialLathe.width(blank); int[] widths = IndustrialLathe.profile(blank);
        if (max > 0) {
            var texture = Jugcraft.id("textures/gui/jimtafacts/turnable_" + (max == 3 ? "wood" : "iron") + ".png");
            for (int i = 0; i < 5; i++) {
                int size = Math.round(32F * widths[i] / max), offset = Math.round((32 - size) / 2F);
                g.text(font, widths[i] + "/" + max, leftPos + 42 + i * 24, topPos + 35, theme.text(), false);
                g.blit(RenderPipelines.GUI_TEXTURED, texture, leftPos + 40 + i * 24, topPos + 44 + offset, i * 24, offset, 24, size, 256, 256);
            }
        }
        g.text(font, menu.data.get(0) + " / 10,000 KE", leftPos + 9, topPos + 169, theme.text(), false);
        g.text(font, STATUS[status], leftPos + 9, topPos + 178, theme.dim(), false);
    }

    @Override protected void extractLabels(GuiGraphicsExtractor g, int x, int y) {}

    @Override public ScreenRectangle panelArea() { return new ScreenRectangle(leftPos, topPos, imageWidth, imageHeight); }
}
