package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.computers.ComputerTerminal;
import io.github.jimbozoomer.jugcraft.jimtafacts.computers.WorkshopComputers;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Original supplied computer surround, readable editable program, live output and ordinary inventory. */
public final class ComputerScreen extends AbstractContainerScreen<ComputerTerminal.Menu> {
    private MultiLineEditBox editor;
    private MultiLineEditBox log;
    private String accepted = "", lastOutput = "";
    private boolean received;
    private final java.util.List<Button> actions = new java.util.ArrayList<>();
    private Button previous, next;
    public ComputerScreen(ComputerTerminal.Menu menu, Inventory inventory, Component title) { super(menu, inventory, title, 360, 288); }
    @Override protected void init() {
        super.init(); titleLabelY = 10000; inventoryLabelY = 10000;
        editor = new MultiLineEditBox.Builder().setX(leftPos + 16).setY(topPos + 32)
                .setShowBackground(false).setTextColor(0xFFE0EFE0).setCursorColor(0xFF96E4AE)
                .setPlaceholder(Component.literal("print Hello, Jugcraft!\nstop"))
                .build(font, 155, 125, Component.literal("Native computer program"));
        editor.setCharacterLimit(WorkshopComputers.MAX_PROGRAM); editor.setValue(menu.program); addRenderableWidget(editor);
        log = new MultiLineEditBox.Builder().setX(leftPos + 190).setY(topPos + 32)
                .setShowBackground(false).setTextColor(0xFFA1E7B3).setCursorColor(0xFFA1E7B3)
                .build(font, 155, 125, Component.literal("Computer output"));
        log.setCharacterLimit(8192); log.setValue(menu.output); addRenderableWidget(log);
        actions.clear();
        String[] labels = {"Save", "Run", "Stop", "To disk", "From disk", "Refuel", "Help"};
        for (int i = 0; i < labels.length; i++) {
            int action = i;
            actions.add(addRenderableWidget(Button.builder(Component.literal(labels[i]), button -> {
                ClientPlayNetworking.send(new ComputerTerminal.Edit(menu.containerId, action, action == 0 ? editor.getValue() : ""));
                if (action == 0) accepted = editor.getValue();
            }).bounds(leftPos + 9 + i * 49, topPos + 174, 47, 18).build()));
        }
        previous=addRenderableWidget(Button.builder(Component.literal("Previous page"),b->ClientPlayNetworking.send(new ComputerTerminal.Edit(menu.containerId,7,""))).bounds(leftPos+16,topPos+174,145,18).build());
        next=addRenderableWidget(Button.builder(Component.literal("Next page"),b->ClientPlayNetworking.send(new ComputerTerminal.Edit(menu.containerId,8,""))).bounds(leftPos+190,topPos+174,145,18).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float delta) {
        // Preserve unsaved edits when live output arrives; disk/load updates replace only an unmodified editor.
        if ((!received || editor.getValue().equals(accepted)) && !menu.program.equals(accepted)) {
            editor.setValue(menu.program); accepted = menu.program;
        }
        received = !menu.output.equals("Connecting to the workshop terminal…");
        if (!menu.output.equals(lastOutput)) { log.setValue(menu.output); lastOutput = menu.output; }
        editor.active = !menu.readOnly;
        actions.forEach(button->button.visible=!menu.readOnly);
        previous.visible=next.visible=menu.readOnly;
        extractBackground(g, x, y, delta); super.extractRenderState(g, x, y, delta); extractTooltip(g, x, y);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g, int x, int y, float delta) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF292C2B);
        g.outline(leftPos, topPos, imageWidth, imageHeight, 0xFF848C88);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, Jugcraft.id("jimtafacts/computer_border"), leftPos + 5, topPos + 21, 174, 146, -1);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, Jugcraft.id("jimtafacts/computer_border"), leftPos + 179, topPos + 21, 176, 146, -1);
        g.text(font, title, leftPos + 12, topPos + 8, 0xFFE4EAE6, false);
        g.text(font, menu.running ? "RUNNING" : "READY", leftPos + 291, topPos + 8, menu.running ? 0xFF93E3A8 : 0xFFABB5AD, false);
        g.text(font, menu.hardware ? "Device / disk / turtle storage" : "Portable program editor", leftPos + 11, topPos + 194, 0xFFD1D7D2, false);
        g.text(font, "Inventory", leftPos + 188, topPos + 194, 0xFFD1D7D2, false);
        if (menu.hardware) for (int i = 0; i < 18; i++) slot(g, leftPos + 11 + i % 9 * 18, topPos + 203 + i / 9 * 18);
        for (int i = 0; i < 27; i++) slot(g, leftPos + 187 + i % 9 * 18, topPos + 203 + i / 9 * 18);
        for (int i = 0; i < 9; i++) slot(g, leftPos + 187 + i * 18, topPos + 261);
        g.text(font, menu.hardware ? menu.energy + " / 16000 JE" : "Save preserves this item's program", leftPos + 12, topPos + 251, 0xFF9ACDB1, false);
        if (menu.turtle) g.text(font, "Slots 17/18: left/right upgrades", leftPos + 12, topPos + 267, 0xFFB3C5B8, false);
    }
    private static void slot(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, 0xFF161A18); g.outline(x, y, 18, 18, 0xFF646F67);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {}
}
