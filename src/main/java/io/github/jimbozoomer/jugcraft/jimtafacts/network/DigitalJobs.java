package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import java.util.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

/** Read-only job rows and exact server-validated cancellation; no client-supplied job IDs or rewards. */
public final class DigitalJobs {
    public static MenuType<Menu> TYPE;
    public static final String[] STATUS = {"Waiting for ingredients / power / room", "Machine batch queued", "Waiting for real machine output", "Machine or permission unavailable", "Pattern removed", "Remove old machine results", "Input room / ingredients / power", "Result room / machine progress / power", "Preparing intermediate ingredients", "Waiting for dependency machine output", "Missing base ingredient", "Dependency limit / busy provider"};
    private DigitalJobs() {}
    public static void register() { TYPE = Registry.register(BuiltInRegistries.MENU, Jugcraft.id("jimta_crafting_jobs"), new MenuType<>(Menu::new, FeatureFlags.DEFAULT_FLAGS)); }
    private record Row(DigitalNode node, int index, DigitalPatterns.Order craft, DigitalProcessing.Job process) {}
    public static final class Menu extends AbstractContainerMenu {
        public static final int ROWS = 8;
        private final DigitalNode node;
        private final Inventory inventory;
        private final SimpleContainer icons = new SimpleContainer(ROWS);
        private final ContainerData data = new SimpleContainerData(3 + ROWS * 2);
        private List<Row> shown = List.of();
        private int page;
        private long refreshed = -20;
        public Menu(int id, Inventory inventory) { this(id, inventory, null); }
        public Menu(int id, Inventory inventory, DigitalNode node) {
            super(TYPE, id); this.node = node; this.inventory = inventory; addDataSlots(data);
            for (int i = 0; i < ROWS; i++) addSlot(new Slot(icons, i, 18, 37 + i * 24) { @Override public boolean mayPlace(ItemStack s) { return false; } @Override public boolean mayPickup(Player p) { return false; } });
            refresh();
        }
        public int page() { return data.get(0); }
        public int pages() { return data.get(1); }
        public int jobs() { return data.get(2); }
        public int batches(int row) { return data.get(3 + row * 2); }
        public String status(int row) { return STATUS[Math.clamp(data.get(4 + row * 2), 0, STATUS.length - 1)]; }
        @Override public boolean stillValid(Player p) { return p.level().isClientSide() || node != null && node.stillValid(p); }
        @Override public ItemStack quickMoveStack(Player p, int i) { return ItemStack.EMPTY; }
        @Override public void clicked(int slot, int button, ContainerInput input, Player p) { /* Icons only; never take, clone, swap or throw them. */ }
        @Override public boolean canTakeItemForPickAll(ItemStack s, Slot slot) { return false; }
        private void refresh() {
            if (!(inventory.player instanceof ServerPlayer p) || !stillValid(p)) return;
            var network = DigitalNetwork.find(node, p); var rows = new ArrayList<Row>();
            if (network.online()) for (var n : network.nodes()) {
                for (int i = 0; i < n.orders.size() && rows.size() < 256; i++) rows.add(new Row(n, i, n.orders.get(i), null));
                for (int i = 0; i < n.processing.size() && rows.size() < 256; i++) rows.add(new Row(n, i, null, n.processing.get(i)));
            }
            int pages = Math.max(1, (rows.size() + ROWS - 1) / ROWS); page = Math.clamp(page, 0, pages - 1);
            data.set(0, page); data.set(1, pages); data.set(2, rows.size()); shown = List.copyOf(rows.subList(Math.min(rows.size(), page * ROWS), Math.min(rows.size(), (page + 1) * ROWS)));
            for (int i = 0; i < ROWS; i++) {
                if (i >= shown.size()) { icons.setItem(i, ItemStack.EMPTY); data.set(3 + i * 2, 0); data.set(4 + i * 2, 0); continue; }
                var r = shown.get(i); var output = r.craft != null ? r.craft.pattern().output(p.level()) : r.process.pattern().outputs().getFirst();
                icons.setItem(i, output.isEmpty() ? new ItemStack(Items.BARRIER) : output.copyWithCount(1));
                data.set(3 + i * 2, r.craft != null ? r.craft.remaining() : r.process.remaining());
                int status = r.craft != null ? 0 : !r.process.delivered() ? 1 : 2;
                if (r.process != null) status = switch (r.node.processingStatus) {
                    case "Machine unavailable or protected", "Point provider toward a machine inventory", "Machine inventory is too large" -> 3;
                    case "Pattern removed" -> 4; case "Remove old results before starting this batch" -> 5;
                    case "Waiting for ingredients, input room or JE" -> 6; case "Waiting for results, storage room or JE" -> 7; default -> status;
                };
                String detail = r.node.processingStatus;
                if (detail.equals("Pattern removed")) status = 4;
                else if (detail.startsWith("Intermediate") || detail.equals("Dependency machine batch queued")) status = 8;
                else if (detail.startsWith("Waiting for real dependency")) status = 9;
                else if (detail.startsWith("Missing base")) status = 10;
                else if (detail.startsWith("Dependency")) status = 11;
                data.set(4 + i * 2, status);
            }
            refreshed = p.level().getGameTime();
        }
        @Override public void broadcastChanges() { if (inventory.player instanceof ServerPlayer && inventory.player.level().getGameTime() - refreshed >= 10) refresh(); super.broadcastChanges(); }
        @Override public boolean clickMenuButton(Player p, int id) {
            if (!(p instanceof ServerPlayer sp) || !stillValid(p) || !RateGate.allow(sp, "jimta_jobs", 3)) return false;
            if (id == 0) page--; else if (id == 1) page++;
            else if (id >= 10 && id < 10 + shown.size()) {
                var r = shown.get(id - 10);
                if (!r.node.permitted(sp) || !DigitalNetwork.find(node, sp).nodes().contains(r.node)) return false;
                if (r.craft != null) {
                    if (r.index >= r.node.orders.size() || !r.node.orders.get(r.index).equals(r.craft)) { refresh(); broadcastFullState(); return false; }
                    DigitalProcessing.cancelTree(DigitalNetwork.find(node,sp),r.craft.root());
                } else {
                    if (r.index >= r.node.processing.size() || !r.node.processing.get(r.index).equals(r.process)) { refresh(); broadcastFullState(); return false; }
                    DigitalProcessing.cancelTree(DigitalNetwork.find(node,sp),r.process.root());
                }
                r.node.setChanged(); sp.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Job and queued dependencies cancelled. Already-paid machine batches finish into storage."));
            } else return false;
            refresh(); broadcastFullState(); return true;
        }
    }
}
