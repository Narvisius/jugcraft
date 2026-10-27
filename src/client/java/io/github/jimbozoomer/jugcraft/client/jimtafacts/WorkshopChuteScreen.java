package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import io.github.jimbozoomer.jugcraft.client.MachineScreenThemes;
import io.github.jimbozoomer.jugcraft.client.ScreenKit;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.WorkshopChute;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The workshop chute, in the workshop look (docs/features/new-screen-styles.md): its buffer slots and stored JE on the
 * glass, and a key for each face (the input face dimmed and marked IN, open outputs lit) and for collection. The status
 * lamp is lit while it holds energy.
 */
public final class WorkshopChuteScreen extends AbstractContainerScreen<WorkshopChute.Menu> implements ScreenKit.Fitted {
    private final ScreenKit.TextButton[] buttons = new ScreenKit.TextButton[7];
    private final MachineScreenThemes.Theme theme = MachineScreenThemes.screen("workshop_chute");

    public WorkshopChuteScreen(WorkshopChute.Menu menu, Inventory inv, Component title) { super(menu, inv, title, 176, 196); }

    @Override protected void init() {
        super.init(); titleLabelY = inventoryLabelY = 10000;
        for (int i = 0; i < 7; i++) {
            int action = i;
            buttons[i] = addRenderableWidget(new ScreenKit.TextButton(leftPos + 8 + (i % 3) * 54, topPos + (i < 6 ? 43 + (i / 3) * 20 : 84), i == 6 ? 106 : 52, 18, theme,
                    Component.empty(), b -> { if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action); }));
            buttons[i].setTooltip(Tooltip.create(Component.literal(i == 6 ? "Collect loose items or pull from the inventory at the wide opening. Redstone pauses the chute."
                    : "Toggle this output. Available outputs share one transfer budget and take turns. The wide opening is the input.")));
        }
    }

    @Override protected void containerTick() {
        super.containerTick();
        for (int i = 0; i < 6; i++) {
            boolean input = menu.data.get(3) == i, open = (menu.data.get(0) & (1 << i)) != 0;
            buttons[i].active = !input;
            buttons[i].setSelected(!input && open);
            buttons[i].setMessage(Component.literal(Direction.values()[i].getName() + (input ? " IN" : open ? " +" : " −")));
        }
        buttons[6].setSelected(menu.data.get(1) == 1);
        buttons[6].setMessage(Component.literal(menu.data.get(1) == 1 ? "Collection: ON" : "Collection: OFF"));
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) { extractBackground(g, mx, my, dt); super.extractRenderState(g, mx, my, dt); extractTooltip(g, mx, my); }

    @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float dt) {
        ScreenKit.frame(g, theme, leftPos, topPos, imageWidth, imageHeight, menu);
        ScreenKit.pane(g, theme, leftPos + 4, topPos + 14, imageWidth - 8, ScreenKit.inventoryTop(menu) - 21);
        ScreenKit.slots(g, theme, leftPos, topPos, menu);
        ScreenKit.title(g, font, theme, leftPos, topPos, title, menu.data.get(2) > 0 ? theme.ok() : theme.dim(), imageWidth - 8);
        String energy = menu.data.get(2) + " JE";
        g.text(font, energy, leftPos + 141 - font.width(energy) / 2, topPos + 89, theme.text(), false);
    }

    @Override protected void extractLabels(GuiGraphicsExtractor g, int x, int y) {}

    @Override public ScreenRectangle panelArea() { return new ScreenRectangle(leftPos, topPos, imageWidth, imageHeight); }
}
