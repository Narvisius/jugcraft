package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.workshop.*;
import java.util.ArrayList;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** A recipe picker, actual inventory, recipe preview and toggleable Work button. */
public final class WorkshopScreen extends AbstractContainerScreen<WorkshopMenu> {
    /** Keep the optional map's HUD out of the station inventory without changing saved map settings. */
    public static void registerOverlayCompatibility() {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("diorama")) return;
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STARTED.register(client ->
            net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.replaceElement(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("diorama", "minimap"),
                original -> (graphics, delta) -> {
                    if (!(client.gui.screen() instanceof WorkshopScreen)) original.extractRenderState(graphics, delta);
                }));
    }
    private boolean working;
    private Button work, cancel;
    private final ArrayList<Button> recipes = new ArrayList<>();
    public WorkshopScreen(WorkshopMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 300, 236); inventoryLabelX = 116; inventoryLabelY = 134;
    }
    @Override protected void init() {
        super.init(); recipes.clear();
        for (int i = 0; i < Math.min(9, menu.catalog.entries().size()); i++) {
            int index = i; var recipe = menu.catalog.entries().get(i).recipe();
            var button = Button.builder(recipe.result().create().getHoverName(), b -> press(WorkshopMenu.SELECT + index))
                .bounds(leftPos + 8, topPos + 24 + i * 17, 105, 16).build();
            button.setTooltip(Tooltip.create(recipe.result().create().getHoverName().copy().append(" ×" + recipe.result().create().getCount())));
            recipes.add(addRenderableWidget(button));
        }
        work = addRenderableWidget(Button.builder(Component.translatable("screen.jugcraft.workshop.work"), b -> {
            working = !working;
            press(working ? WorkshopMenu.PULSE : WorkshopMenu.RELEASE);
        })
            .bounds(leftPos + 8, topPos + 188, 105, 20).build());
        work.visible = menu.catalog.station() == 0;
        cancel = addRenderableWidget(Button.builder(Component.translatable("screen.jugcraft.workshop.cancel"), b -> { working = false; press(WorkshopMenu.RELEASE); press(WorkshopMenu.CANCEL); })
            .bounds(leftPos + 8, topPos + 211, 105, 16).build());
    }
    private void press(int id) { if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id); }
    @Override public void removed() { working = false; press(WorkshopMenu.RELEASE); super.removed(); }
    @Override protected void containerTick() {
        super.containerTick();
        if (working) press(WorkshopMenu.PULSE);
        for (int i = 0; i < recipes.size(); i++) recipes.get(i).active = !menu.busy() && i != menu.data(5);
        cancel.active = menu.busy();
        work.setMessage(Component.translatable(working ? "screen.jugcraft.workshop.working" : "screen.jugcraft.workshop.work"));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        extractBackground(g, mx, my, delta); super.extractRenderState(g, mx, my, delta); extractTooltip(g, mx, my);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float delta) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF30271F);
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFFD4C7B4);
        g.fill(x + 5, y + 21, x + 114, y + 184, 0xFFB4A18A);
        for (var slot : menu.slots) if (slot.isActive()) {
            g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, 0xFF594B3D);
            g.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, 0xFF998B79);
        }
        int width = 138 * menu.data(0) / Math.max(1, menu.data(1));
        g.fill(x + 132, y + 116, x + 270, y + 120, 0xFF594B3D);
        g.fill(x + 132, y + 116, x + 132 + width, y + 120, 0xFFCA873D);
        int index = menu.data(5);
        if (index >= 0 && index < menu.catalog.entries().size()) {
            var r = menu.catalog.entries().get(index).recipe();
            for (int i = 0; i < r.parts().size(); i++) {
                var part = r.parts().get(i);
                var item = part.ingredient().items().findFirst();
                if (item.isPresent()) {
                    var stack = new net.minecraft.world.item.ItemStack(item.get(), part.count());
                    g.item(stack, x + 132 + i * 20, y + 34); g.itemDecorations(font, stack, x + 132 + i * 20, y + 34);
                }
            }
            var result = r.result().create(); g.item(result, x + 252, y + 34); g.itemDecorations(font, result, x + 252, y + 34);
            if (!r.toolStack().isEmpty() && menu.slots.get(3).getItem().isEmpty()) g.item(r.toolStack(), x + 212, y + 34);
        }
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) {
        super.extractLabels(g, mx, my);
        g.text(font, Component.translatable("screen.jugcraft.workshop.preview"), 132, 23, 0xFF453727, false);
        g.text(font, Component.translatable("screen.jugcraft.workshop.heat", menu.heat() / 20), 171, 75, 0xFF453727, false);
        g.text(font, Component.translatable("screen.jugcraft.workshop.status." + menu.data(4)), 122, 122, 0xFF453727, false);
        if (menu.catalog.station() == 2) {
            g.text(font, Component.translatable("screen.jugcraft.workshop.lining"), 207, 106, 0xFF453727, false);
            g.text(font, Component.translatable("screen.jugcraft.workshop.vessel"), 249, 106, 0xFF453727, false);
        }
    }
}
