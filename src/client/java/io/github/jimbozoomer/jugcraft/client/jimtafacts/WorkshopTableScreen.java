package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import io.github.jimbozoomer.jugcraft.client.MachineScreenThemes;
import io.github.jimbozoomer.jugcraft.client.ScreenKit;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.WorkshopRobots;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.WorkshopTables;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * The workshop tables (the robot programming table and the equipment charger), in the workshop look
 * (docs/features/new-screen-styles.md): the card or equipment slots on the glass, the programs as keys showing their
 * cards (the chosen one lit) and the charge, and the state and stored JE in a strip under the inventory. The status
 * lamp is green while it works or is ready, amber when something stops it. No typing: a program chooser.
 */
public final class WorkshopTableScreen extends AbstractContainerScreen<WorkshopTables.Menu> implements ScreenKit.Fitted {
    private static final String[] STATUSES = {"Idle", "Owner access unavailable", "Redstone pause", "Insert an item", "Ready", "Output full", "Needs power", "Working"};
    private final MachineScreenThemes.Theme theme = MachineScreenThemes.screen("workshop_table");
    private final java.util.List<ScreenKit.TextButton> programs = new java.util.ArrayList<>();

    public WorkshopTableScreen(WorkshopTables.Menu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, menu.programming ? 230 : 154);
    }

    @Override protected void init() {
        super.init(); titleLabelY = inventoryLabelY = 10000;
        programs.clear();
        if (menu.programming) for (int i = 0; i < WorkshopRobots.PROGRAMS.size(); i++) {
            int program = i; String name = WorkshopRobots.PROGRAMS.get(i);
            var card = new ItemStack(WorkshopRobots.BOARDS.get(name));
            var button = addRenderableWidget(new ScreenKit.TextButton(leftPos + 43 + i % 6 * 18, topPos + 36 + i / 6 * 18, 17, 17, theme, Component.empty(), b -> {
                if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, program);
            }));
            button.setTooltip(Tooltip.create(Component.empty().append(card.getHoverName()).append("\n")
                    .append(Component.translatable("tooltip.jugcraft.robot_program." + name)).append("\nConsumes one blank/old card and 2,000 JE.")));
            programs.add(button);
        }
        var help = addRenderableWidget(new ScreenKit.TextButton(leftPos + imageWidth - 17, topPos + 1, 14, 11, theme, Component.literal("?"), b -> {}));
        help.setTooltip(Tooltip.create(Component.literal(menu.programming
                ? "Top-left: blank or old program card. Click a program. Bottom-left: finished cards. Pipes insert from top/sides and extract underneath. Redstone pauses work."
                : "Place powered equipment in the center. Charges at 128 JE/tick from cables or a workshop laser. Pipes insert from top/sides and extract fully charged items underneath. Redstone pauses work.")));
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
        // The chosen program's key is lit.
        for (int i = 0; i < programs.size(); i++) programs.get(i).setSelected(menu.data.get(0) == i);
        extractBackground(g, mx, my, dt); super.extractRenderState(g, mx, my, dt);
        // Each program's card over its key; real cards stay in the input and output slots.
        if (menu.programming) for (int i = 0; i < WorkshopRobots.PROGRAMS.size(); i++)
            g.item(new ItemStack(WorkshopRobots.BOARDS.get(WorkshopRobots.PROGRAMS.get(i))), leftPos + 43 + i % 6 * 18, topPos + 36 + i / 6 * 18);
        extractTooltip(g, mx, my);
    }

    @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float dt) {
        int inventory = ScreenKit.inventoryTop(menu), status = Math.clamp(menu.data.get(2), 0, 7);
        // The state strip sits under the hotbar (58 below the inventory's top, 16 high).
        int strip = inventory + 77;
        ScreenKit.frame(g, theme, leftPos, topPos, imageWidth, imageHeight, menu);
        ScreenKit.pane(g, theme, leftPos + 4, topPos + 14, imageWidth - 8, inventory - 21);
        ScreenKit.pane(g, theme, leftPos + 4, topPos + strip, imageWidth - 8, imageHeight - strip - 3);
        ScreenKit.slots(g, theme, leftPos, topPos, menu);
        int lamp = status == 4 || status == 7 ? theme.ok() : status == 0 || status == 3 ? theme.dim() : theme.warn();
        ScreenKit.title(g, font, theme, leftPos, topPos, Component.literal(menu.programming ? "Program a robot card" : "Equipment charger"), lamp, imageWidth - 26);
        int amount = menu.data.get(1) & 65535;
        g.text(font, STATUSES[status], leftPos + 9, topPos + strip + 3, lamp == theme.warn() ? theme.warn() : theme.text(), false);
        g.text(font, amount + " / 40,000 JE", leftPos + 9, topPos + strip + 12, theme.dim(), false);
        if (menu.programming) {
            int height = amount * 70 / 40000;
            g.fill(leftPos + 163, topPos + 36, leftPos + 167, topPos + 106, theme.slotDark());
            g.fill(leftPos + 164, topPos + 106 - height, leftPos + 166, topPos + 106, theme.energy());
        } else {
            ScreenKit.bar(g, theme, leftPos + 14, topPos + 38, 148, 4, menu.data.get(3) / 1000.0, theme.energy());
        }
    }

    @Override protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) {}

    @Override public ScreenRectangle panelArea() { return new ScreenRectangle(leftPos, topPos, imageWidth, imageHeight); }
}
