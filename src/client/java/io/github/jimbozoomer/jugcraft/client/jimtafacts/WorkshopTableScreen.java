package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.WorkshopTables;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.WorkshopRobots;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Retains the supplied table GUI and adds a no-typing program chooser and exact power/status feedback. */
public final class WorkshopTableScreen extends AbstractContainerScreen<WorkshopTables.Menu> {
    public WorkshopTableScreen(WorkshopTables.Menu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, menu.programming ? 230 : 154);
    }
    @Override protected void init() {
        super.init(); titleLabelY = inventoryLabelY = 10000;
        if (menu.programming) for (int i = 0; i < WorkshopRobots.PROGRAMS.size(); i++) {
            int program = i; String name = WorkshopRobots.PROGRAMS.get(i);
            var card = new ItemStack(WorkshopRobots.BOARDS.get(name));
            var button = addRenderableWidget(Button.builder(card.getHoverName(), b -> {
                if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, program);
            }).bounds(leftPos + 43 + i % 6 * 18, topPos + 36 + i / 6 * 18, 17, 17).createNarration(supplier -> Component.empty().append(card.getHoverName()).append(". Select this robot program.")).build());
            button.setMessage(Component.empty());
            button.setTooltip(Tooltip.create(Component.empty().append(card.getHoverName()).append("\n")
                    .append(Component.translatable("tooltip.jugcraft.robot_program." + name)).append("\nConsumes one blank/old card and 2,000 JE.")));
        }
        var help = addRenderableWidget(Button.builder(Component.literal("?"), b -> {}).bounds(leftPos + 154, topPos + 5, 16, 16).build());
        help.setTooltip(Tooltip.create(Component.literal(menu.programming
                ? "Top-left: blank or old program card. Click a program. Bottom-left: finished cards. Pipes insert from top/sides and extract underneath. Redstone pauses work."
                : "Place powered equipment in the center. Charges at 128 JE/tick from cables or a workshop laser. Pipes insert from top/sides and extract fully charged items underneath. Redstone pauses work.")));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
        extractBackground(g, mx, my, dt); super.extractRenderState(g, mx, my, dt);
        // Icons sit above the empty buttons; real cards remain exclusively in the input/output slots.
        if (menu.programming) for (int i = 0; i < WorkshopRobots.PROGRAMS.size(); i++) {
            int x = leftPos + 43 + i % 6 * 18, y = topPos + 36 + i / 6 * 18;
            if (menu.data.get(0) == i) g.fill(x - 1, y - 1, x + 18, y + 18, 0xff43b3ad);
            g.item(new ItemStack(WorkshopRobots.BOARDS.get(WorkshopRobots.PROGRAMS.get(i))), x, y);
        }
        extractTooltip(g, mx, my);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float dt) {
        String kind = menu.programming ? "programming" : "charging"; int body = menu.programming ? 208 : 132;
        g.blit(RenderPipelines.GUI_TEXTURED, Jugcraft.id("textures/gui/jimtafacts/" + kind + "_table.png"), leftPos, topPos, 0, 0, 176, body, 256, 256);
        g.fill(leftPos + 1, topPos + body, leftPos + 175, topPos + imageHeight - 1, 0xff202c31);
        g.text(font, menu.programming ? "Program a robot card" : "Equipment charger", leftPos + 7, topPos + 7, 0xff333333, false);
        int amount = menu.data.get(1) & 65535;
        String[] statuses = {"Idle", "Owner access unavailable", "Redstone pause", "Insert an item", "Ready", "Output full", "Needs power", "Working"};
        g.text(font, statuses[Math.clamp(menu.data.get(2), 0, 7)], leftPos + 7, topPos + body + 2, 0xffe2ecef, false);
        g.text(font, amount + " / 40,000 JE", leftPos + 7, topPos + body + 12, 0xff9edbd3, false);
        if (menu.programming) {
            int height = amount * 70 / 40000;
            g.fill(leftPos + 164, topPos + 106 - height, leftPos + 167, topPos + 106, 0xff46a9ac);
        } else {
            int width = menu.data.get(3) * 148 / 1000;
            g.fill(leftPos + 14, topPos + 39, leftPos + 162, topPos + 42, 0xff555555);
            g.fill(leftPos + 14, topPos + 39, leftPos + 14 + width, topPos + 42, 0xff247e73);
        }
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) {}
}
