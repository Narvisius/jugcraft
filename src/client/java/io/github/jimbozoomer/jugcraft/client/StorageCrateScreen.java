package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.storage.StorageCrateMenu;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** A local view of real, stable menu slots; no virtual inventory or client-authoritative transfers. */
public final class StorageCrateScreen extends AbstractContainerScreen<StorageCrateMenu> {
    private EditBox search;
    private String query = "";
    private List<Integer> matches = List.of();
    private final List<ItemStack> snapshot = new ArrayList<>();
    private int rows, scroll, occupied;
    private boolean draggingBar, holdingMouse;
    private int panelHeight;
    public StorageCrateScreen(StorageCrateMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 202, 260);
    }
    @Override protected void init() {
        super.init();
        rows = Math.clamp((height - 152) / 18, 1, 6);
        panelHeight = 152 + rows * 18;
        topPos = (height - panelHeight) / 2;
        search = addRenderableWidget(new EditBox(font, leftPos + 12, topPos + 24, 151, 18,
            Component.translatable("screen.jugcraft.storage_crate.search")));
        search.setMaxLength(64);
        search.setHint(Component.translatable("screen.jugcraft.storage_crate.search"));
        search.setValue(query);
        search.setResponder(text -> { query = text; scroll = 0; if (!holdingMouse && !isQuickCrafting) refresh(); });
        addRenderableWidget(Button.builder(Component.literal("×"), button -> search.setValue(""))
            .bounds(leftPos + 168, topPos + 23, 22, 20).build())
            .setTooltip(Tooltip.create(Component.translatable("screen.jugcraft.storage_crate.clear")));
        for (int i = menu.tier.slots; i < menu.slots.size(); i++) {
            int j = i - menu.tier.slots;
            menu.positionSlot(i, 12 + j % 9 * 18, 66 + rows * 18 + (j < 27 ? j / 9 * 18 : 58), true);
        }
        refresh();
    }
    public int visibleRows() { return rows; }
    public int scrollRow() { return scroll; }
    public int matchCount() { return matches.size(); }
    public void searchFor(String text) { search.setValue(text); }
    private int maxScroll() { return Math.max(0, (matches.size() + 8) / 9 - rows); }
    private void refresh() {
        var found = new ArrayList<Integer>(); snapshot.clear(); occupied = 0;
        String term = query.strip().toLowerCase(Locale.ROOT);
        for (int i = 0; i < menu.tier.slots; i++) {
            var stack = menu.contents.getItem(i); snapshot.add(stack.copy());
            if (!stack.isEmpty()) occupied++;
            if (term.isEmpty() || !stack.isEmpty() && (stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(term)
                || BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase(Locale.ROOT).contains(term))) found.add(i);
        }
        matches = found; scroll = Math.clamp(scroll, 0, maxScroll()); positionGrid();
    }
    private void positionGrid() {
        int[] positions = new int[menu.tier.slots]; Arrays.fill(positions, -1);
        for (int i = scroll * 9; i < Math.min(matches.size(), (scroll + rows) * 9); i++) positions[matches.get(i)] = i - scroll * 9;
        for (int i = 0; i < positions.length; i++) {
            int p = positions[i]; menu.positionSlot(i, p < 0 ? -10000 : 12 + p % 9 * 18, p < 0 ? -10000 : 48 + p / 9 * 18, p >= 0);
        }
    }
    @Override protected void containerTick() {
        super.containerTick();
        // Freeze the mapping for a complete mouse gesture, including drag distribution.
        if (holdingMouse || isQuickCrafting || draggingBar) return;
        for (int i = 0; i < menu.tier.slots; i++) if (!ItemStack.matches(snapshot.get(i), menu.contents.getItem(i))) { refresh(); break; }
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (!holdingMouse && !isQuickCrafting && isHovering(10, 46, 182, rows * 18 + 2, x, y)) {
            scroll = Math.clamp(scroll - (int) Math.signum(vertical), 0, maxScroll()); positionGrid(); return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
    private void dragBar(double y) {
        scroll = (int) Math.round(Math.clamp((y - topPos - 48 - 6) / (rows * 18.0 - 12), 0, 1) * maxScroll());
        positionGrid();
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && maxScroll() > 0 && isHovering(181, 48, 10, rows * 18, event.x(), event.y()) && !isQuickCrafting) {
            draggingBar = true; dragBar(event.y()); return true;
        }
        boolean handled = super.mouseClicked(event, doubleClick);
        if (isHovering(10, 46, 165, rows * 18 + 2, event.x(), event.y())
            || isHovering(10, 64 + rows * 18, 165, 78, event.x(), event.y())) holdingMouse = true;
        return handled;
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingBar) { dragBar(event.y()); return true; }
        return super.mouseDragged(event, dx, dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingBar) { draggingBar = false; refresh(); return true; }
        boolean handled = super.mouseReleased(event); holdingMouse = false; refresh(); return handled;
    }
    @Override public boolean keyPressed(KeyEvent event) {
        // Typing 'e' or a hotbar number searches instead of closing/swapping items.
        if (search.isFocused() && !event.isEscape() && !event.isCycleFocus()) { search.keyPressed(event); return true; }
        return super.keyPressed(event);
    }
    @Override protected boolean hasClickedOutside(double x, double y, int left, int top) {
        return x < left || y < top || x >= left + imageWidth || y >= top + panelHeight;
    }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float delta) {
        int x = leftPos, y = topPos;
        g.fill(0, 0, width, height, 0xA010141A);
        g.fill(x, y, x + imageWidth, y + panelHeight, 0xFF252D35);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 2, 0xFFB3BDC4);
        g.fill(x + 1, y + 2, x + 2, y + panelHeight - 1, 0xFF87949F);
        g.fill(x + 8, y + 20, x + 194, y + 21, 0xFF586976);
        for (var slot : menu.slots) if (slot.isActive()) {
            int sx = x + slot.x, sy = y + slot.y;
            g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF111820);
            g.fill(sx, sy, sx + 16, sy + 16, 0xFF46515C);
            g.fill(sx, sy + 16, sx + 17, sy + 17, 0xFF7E8D97);
        }
        g.fill(x + 181, y + 48, x + 191, y + 48 + rows * 18, 0xFF172029);
        if (maxScroll() > 0) {
            int sy = y + 48 + (int) ((rows * 18 - 12) * scroll / (double) maxScroll());
            g.fill(x + 182, sy, x + 190, sy + 12, 0xFFBAC8CD);
        }
        if (matches.isEmpty()) g.text(font, Component.translatable("screen.jugcraft.storage_crate.no_matches"), x + 12, y + 53, 0xFFE1E6EA, false);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) {
        g.text(font, font.plainSubstrByWidth(title.getString(), 180), 12, 8, 0xFFF1E8D6, false);
        int base = rows * 18;
        Component status = query.isBlank() ? Component.translatable("screen.jugcraft.storage_crate.used", occupied, menu.tier.slots)
            : Component.translatable("screen.jugcraft.storage_crate.matches", matches.size(), occupied, menu.tier.slots);
        g.text(font, status, 12, base + 52, 0xFFB8C8D5, false);
        if (!query.isBlank()) g.text(font, Component.translatable("screen.jugcraft.storage_crate.deposit_hint"), 12, base + 142, 0xFFB8C8D5, false);
    }
}
