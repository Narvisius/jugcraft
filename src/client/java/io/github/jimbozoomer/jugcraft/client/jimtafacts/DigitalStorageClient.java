package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import io.github.jimbozoomer.jugcraft.jimtafacts.network.DigitalTerminal;
import io.github.jimbozoomer.jugcraft.jimtafacts.network.DigitalControls;
import io.github.jimbozoomer.jugcraft.jimtafacts.network.PatternEncoding;
import io.github.jimbozoomer.jugcraft.jimtafacts.network.DigitalJobs;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** A compact engineering terminal: searchable stock, real counts, crafting and inventory in one view. */
public final class DigitalStorageClient {
    private DigitalStorageClient() {}
    public static void register() { DigitalExplorationClient.register(); MenuScreens.register(DigitalTerminal.TYPE, TerminalScreen::new); MenuScreens.register(DigitalTerminal.DRIVE, DriveScreen::new); MenuScreens.register(DigitalControls.TYPE, ControlScreen::new); MenuScreens.register(PatternEncoding.TYPE, PatternScreen::new); MenuScreens.register(DigitalJobs.TYPE, JobsScreen::new); }
    private static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFF090E15);
        g.fill(x, y, x + w, y + h, 0xFF8996A0);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFF26343F);
        g.fill(x + 5, y + 5, x + w - 5, y + 22, 0xFF101B25);
        g.fill(x + 5, y + 22, x + w - 5, y + 23, 0xFF67C8D4);
    }
    private static void slot(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF5A7181);
        g.fill(x, y, x + 16, y + 16, 0xFF101A24);
    }
    public static final class TerminalScreen extends AbstractContainerScreen<DigitalTerminal.Menu> {
        private EditBox search;
        private Button previous, next, view, encode, store;
        private String query = "";
        private int debounce;
        public TerminalScreen(DigitalTerminal.Menu menu, Inventory inv, Component title) { super(menu, inv, title, 344, 240); }
        @Override protected void init() {
            super.init();
            search = addRenderableWidget(new EditBox(font, leftPos + 18, topPos + 28, 162, 18, Component.literal("Search storage")));
            search.setMaxLength(48); search.setHint(Component.literal("Find an item…")); search.setValue(query); search.setResponder(text -> { query = text; debounce = 4; });
            previous = button(0, 18, 126, 76, "Previous"); next = button(1, 104, 126, 76, "Next");
            view = button(3, 210, 126, 116, "Stored items");
            encode = button(4, 210, 192, 116, "Encode pattern");
            store = button(2, 210, 212, 116, "Store backpack"); store.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Stores inventory items. Keeps your hotbar, armor and equipped accessories.")));
        }
        private Button button(int id, int x, int y, int w, String label) {
            return addRenderableWidget(Button.builder(Component.literal(label), b -> { if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id); }).bounds(leftPos + x, topPos + y, w, 18).build());
        }
        @Override protected void containerTick() { super.containerTick(); if (debounce > 0 && --debounce == 0) ClientPlayNetworking.send(new DigitalTerminal.Search(menu.containerId, query)); }
        @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
            previous.active = menu.page() > 0; next.active = menu.page() + 1 < menu.pages();
            view.setMessage(Component.literal(menu.patternsMode() ? "Craftable recipes" : "Stored items")); encode.active = menu.canCraft();
            view.visible = !menu.fluidMode() && !menu.portableMode(); encode.visible = !menu.fluidMode() && !menu.portableMode(); store.visible = !menu.fluidMode();
            extractBackground(g, mx, my, partial); super.extractRenderState(g, mx, my, partial);
            for (int i = 0; i < DigitalTerminal.Menu.GRID; i++) {
                int n = menu.count(i); if (n <= 0) continue;
                String label = n >= 1_000_000 ? String.format(java.util.Locale.ROOT, "%.1fM", n / 1_000_000.0) : n >= 10_000 ? n / 1000 + "k" : Integer.toString(n);
                var slot = menu.slots.get(i); g.text(font, label, leftPos + slot.x + 17 - font.width(label), topPos + slot.y + 10, 0xFFE6FBFF, true);
            }
            extractTooltip(g, mx, my);
        }
        @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float partial) {
            g.fill(0, 0, width, height, 0xB0081017); panel(g, leftPos, topPos, imageWidth, imageHeight);
            for (var s : menu.slots) if (s.isActive()) slot(g, leftPos + s.x, topPos + s.y);
            int color = menu.online() ? 0xFF65D9AB : 0xFFDD8B70;
            g.fill(leftPos + 210, topPos + 30, leftPos + 216, topPos + 36, color);
            g.text(font, menu.portableMode() ? (menu.online() ? "Cell charged" : "Cell needs charging") : (menu.online() ? "Network online" : "No power / connection"), leftPos + 221, topPos + 29, color, false);
            String help = menu.patternsMode() ? "Click: craft once\nRight-click: craft ×16\nNeeds a Pattern Provider\nand Crafting Unit" : "Click: take a stack\nRight-click: take one\nHeld item: store\nCost: 1 JE per item";
            if (menu.fluidMode()) help = "Hold a container and click a fluid to fill it. Click the grid with a filled container to store it.\nVolumes shown in mB.";
            var lines = font.split(Component.literal(help), 116); for (int i = 0; i < lines.size(); i++) g.text(font, lines.get(i), leftPos + 210, topPos + 150 + i * 10, 0xFFB8C9D4, false);
        }
        @Override protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) {
            g.text(font, menu.portableMode() ? "PORTABLE ME STORAGE" : menu.fluidMode() ? "ME FLUID STORAGE" : "ME STORAGE", 14, 10, 0xFFDBF9FF, false);
            g.text(font, "Page " + (menu.page() + 1) + " / " + menu.pages(), 210, 48, 0xFFB8C9D4, false);
            g.text(font, menu.canCraft() ? "Crafting" : "Storage terminal", 215, 60, 0xFF87D6E0, false);
            g.text(font, "Your inventory", 18, 146, 0xFFB8C9D4, false);
        }
    }
    public static final class JobsScreen extends AbstractContainerScreen<DigitalJobs.Menu> {
        private final Button[] cancels = new Button[DigitalJobs.Menu.ROWS];
        private Button previous, next;
        public JobsScreen(DigitalJobs.Menu menu, Inventory inv, Component title) { super(menu, inv, title, 360, 264); }
        @Override protected void init() {
            super.init();
            for (int i = 0; i < cancels.length; i++) { final int id = 10 + i; cancels[i] = button(id, 302, 36 + i * 24, 44, "Cancel"); }
            previous = button(0, 18, 235, 74, "Previous"); next = button(1, 272, 235, 74, "Next");
        }
        private Button button(int id, int x, int y, int w, String text) { return addRenderableWidget(Button.builder(Component.literal(text), b -> { if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id); }).bounds(leftPos + x, topPos + y, w, 18).build()); }
        @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
            for (int i = 0; i < cancels.length; i++) cancels[i].visible = menu.batches(i) > 0;
            previous.active = menu.page() > 0; next.active = menu.page() + 1 < menu.pages();
            extractBackground(g, mx, my, partial); super.extractRenderState(g, mx, my, partial); extractTooltip(g, mx, my);
        }
        @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float partial) {
            g.fill(0, 0, width, height, 0xB0081017); panel(g, leftPos, topPos, imageWidth, imageHeight);
            for (int i = 0; i < DigitalJobs.Menu.ROWS; i++) {
                if (menu.batches(i) <= 0) continue; var s = menu.slots.get(i); int y = topPos + 36 + i * 24;
                g.fill(leftPos + 12, y - 2, leftPos + 348, y + 20, i % 2 == 0 ? 0xFF1B2933 : 0xFF22323C); slot(g, leftPos + s.x, topPos + s.y);
                String name = s.getItem().getHoverName().getString() + " × " + menu.batches(i) + " batches";
                g.text(font, font.plainSubstrByWidth(name, 252), leftPos + 42, y, 0xFFE8F5FA, false);
                g.text(font, font.plainSubstrByWidth(menu.status(i), 252), leftPos + 42, y + 11, 0xFF9EB3C0, false);
            }
            if (menu.jobs() == 0) g.text(font, "No queued jobs on this powered network.", leftPos + 18, topPos + 42, 0xFFB8C9D4, false);
            g.text(font, "Page " + (menu.page() + 1) + " / " + menu.pages(), leftPos + 138, topPos + 240, 0xFFB8C9D4, false);
        }
        @Override protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) { g.text(font, "ME CRAFTING JOBS", 14, 10, 0xFFE8F5FA, false); }
    }
    public static final class PatternScreen extends AbstractContainerScreen<PatternEncoding.Menu> {
        private Button mode;
        public PatternScreen(PatternEncoding.Menu menu, Inventory inv, Component title) { super(menu, inv, title, 198, 242); }
        @Override protected void init() {
            super.init();
            mode = addRenderableWidget(Button.builder(Component.literal(menu.modeName()), b -> send(2)).bounds(leftPos + 90, topPos + 7, 94, 18).build());
            addRenderableWidget(Button.builder(Component.literal("Encode batch"), b -> send(0)).bounds(leftPos + 18, topPos + 131, 100, 18).build());
            addRenderableWidget(Button.builder(Component.literal("Clear"), b -> send(1)).bounds(leftPos + 122, topPos + 131, 58, 18).build());
        }
        private void send(int id) { if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id); }
        @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) { extractBackground(g, mx, my, partial); super.extractRenderState(g, mx, my, partial); extractTooltip(g, mx, my); }
        @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float partial) {
            g.fill(0, 0, width, height, 0xB0081017); panel(g, leftPos, topPos, imageWidth, imageHeight);
            for (var s : menu.slots) slot(g, leftPos + s.x, topPos + s.y);
            g.text(font, "Inputs", leftPos + 24, topPos + 30, 0xFF87D6E0, false);
            g.text(font, "Outputs", leftPos + 114, topPos + 47, 0xFF87D6E0, false);
            g.text(font, "→", leftPos + 91, topPos + 64, 0xFFDBF9FF, false);
            mode.setMessage(Component.literal(menu.modeName()));
            var hint = switch(menu.mode()) { case 1 -> "First input + chosen output. Current stonecutting recipe; uses 1 blank."; case 2 -> "Top row: template, base, addition. Current recipe; uses 1 blank."; default -> "Copy held stack to a slot. Right-click clears. Uses 1 blank pattern."; };
            var lines = font.split(Component.literal(hint), 164);
            for (int i = 0; i < lines.size(); i++) g.text(font, lines.get(i), leftPos + 18, topPos + 98 + i * 10, 0xFFB8C9D4, false);
        }
        @Override protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) { g.text(font, "PATTERN", 14, 12, 0xFFE8F5FA, false); }
    }
    public static final class ControlScreen extends AbstractContainerScreen<DigitalControls.Menu> {
        private Button mode;
        public ControlScreen(DigitalControls.Menu menu, Inventory inv, Component title) { super(menu, inv, title, 196, 214); }
        @Override protected void init() {
            super.init(); String[] labels = {"−64", "−1", "+1", "+64", "−1k", "+1k"};
            for (int i = 0; i < labels.length; i++) { final int id = i; addRenderableWidget(Button.builder(Component.literal(labels[i]), b -> send(id)).bounds(leftPos + 17 + i % 4 * 40, topPos + 66 + i / 4 * 21, 38, 18).build()); }
            mode = addRenderableWidget(Button.builder(Component.empty(), b -> send(6)).bounds(leftPos + 97, topPos + 87, 78, 18).build());
            mode.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Below emits when supplies run low. At least emits when the target stock is reached.")));
        }
        private void send(int id) { if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id); }
        @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
            mode.setMessage(Component.literal(menu.below() ? "Below" : "At least"));
            extractBackground(g, mx, my, partial); super.extractRenderState(g, mx, my, partial); extractTooltip(g, mx, my);
        }
        @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float partial) {
            g.fill(0, 0, width, height, 0xB0081017); panel(g, leftPos, topPos, imageWidth, imageHeight);
            for (var s : menu.slots) slot(g, leftPos + s.x, topPos + s.y);
            g.text(font, "Target: " + menu.threshold() + (menu.energy() ? " JE" : ""), leftPos + 44, topPos + 38, 0xFF87D6E0, false);
            g.text(font, "Stored: " + menu.amount(), leftPos + 44, topPos + 49, 0xFFB8C9D4, false);
            g.text(font, menu.energy() ? "Measures network energy" : "Sample: click held item; right = all", leftPos + 17, topPos + 111, 0xFFB8C9D4, false);
        }
        @Override protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) { g.text(font, "ME LEVEL CONTROL", 14, 10, 0xFFE8F5FA, false); }
    }
    public static final class DriveScreen extends AbstractContainerScreen<DigitalTerminal.DriveMenu> {
        private Button mode;
        public DriveScreen(DigitalTerminal.DriveMenu menu, Inventory inv, Component title) { super(menu, inv, title, 330, 164); }
        @Override protected void init() { super.init(); mode = addRenderableWidget(Button.builder(Component.literal("Matter balls"), b -> { if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0); }).bounds(leftPos + 185, topPos + 120, 133, 18).build()); }
        @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) { boolean io=menu.kind().equals("io_port");mode.visible = io||menu.kind().equals("condenser"); mode.setMessage(Component.literal(io?(menu.singularities()?"Fill cell":"Empty cell"):(menu.singularities() ? "Singularities" : "Matter balls"))); extractBackground(g, mx, my, partial); super.extractRenderState(g, mx, my, partial); extractTooltip(g, mx, my); }
        @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float partial) {
            g.fill(0, 0, width, height, 0xB0081017); panel(g, leftPos, topPos, imageWidth, imageHeight);
            for (var s : menu.slots) if (s.isActive()) slot(g, leftPos + s.x, topPos + s.y);
            String help = switch (menu.kind()) {
                case "import_bus", "export_bus", "storage_bus", "fluid_import_bus", "fluid_export_bus", "fluid_storage_bus", "formation_plane", "annihilation_plane" -> "Top: real filter samples (kept). Bottom: removable cards. Capacity unlocks 4 more samples per card. Redstone card: run only with a signal. Without it: signal pauses.";
                case "drive", "me_chest" -> "Insert item or fluid cells. Remove a cell to carry its stored contents. Connect a powered network to use them.";
                case "inscriber" -> "Print: die + material (100 JE). Join: printed circuit + redstone + printed silicon (200 JE). Result: fifth slot. Dies are reusable. Older four-input recipes still work.";
                case "condenser" -> "Left: cell component. Middle: material consumed. Right: result. 256 items/buckets per matter ball; 256,000 per singularity. Stored: " + menu.matter();
                case "wireless_access_point" -> "Insert wireless boosters. Base coverage: 64 blocks, +2 per booster, up to 192. Requires a powered loaded network. Redstone disables coverage.";
                case "charger" -> "Certus crystal → charged crystal (1,000 JE). Also recharges JE tools and batteries in the first slot.";
                case "vibration_chamber" -> "Insert coal, charcoal or coke. Produces 8 JE/t while there is room. Redstone pauses the chamber.";
                case "cell_workbench" -> "First: item or fluid cell. Other slots: kept filter samples (filled buckets for fluids). Empty filters allow all. Stored contents are preserved.";
                case "io_port" -> "First: item or fluid cell. Toggle fill / empty below. Second: finished cell. 64 items or 1000 mB per second; paid JE. Filters apply when filling.";
                case "molecular_assembler" -> "Insert an encoded pattern to repeat its recipe. Stops when ingredients, power or storage space run out. Redstone pauses work.";
                case "pattern_provider" -> "Insert crafting or machine patterns. Face the machine's input port; use an Import Bus for a separate output port. View jobs at a Crafting Unit or Monitor.";
                default -> "Items inserted here are moved into the powered network. Fluid pipes and containers can use this interface too.";
            };
            var lines = font.split(Component.literal(help), 133);
            for (int i = 0; i < lines.size(); i++) g.text(font, lines.get(i), leftPos + 185, topPos + 32 + i * 10, 0xFFB8C9D4, false);
            if (io.github.jimbozoomer.jugcraft.jimtafacts.network.DigitalNode.poweredKind(menu.kind())) g.text(font, menu.energy() + " / " + menu.capacity() + " JE", leftPos + 185, topPos + 143, 0xFF87D6E0, false);
        }
        @Override protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) { g.text(font, title, 8, 10, 0xFFE8F5FA, false); g.text(font, "Your inventory", 8, 69, 0xFFB8C9D4, false); }
    }
}
