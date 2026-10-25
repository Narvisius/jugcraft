package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import java.util.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

/** A no-typing template editor. Samples describe one machine batch and are never real inventory outputs. */
public final class PatternEncoding {
    public static MenuType<Menu> TYPE;
    private PatternEncoding() {}
    public static void register() { TYPE = Registry.register(BuiltInRegistries.MENU, Jugcraft.id("jimta_pattern_encoding"), new MenuType<>(Menu::new, FeatureFlags.DEFAULT_FLAGS)); }
    public static final class Menu extends AbstractContainerMenu {
        private final DigitalNode node;
        private final Inventory inventory;
        private final SimpleContainer samples = new SimpleContainer(12);
        private final ContainerData settings = new SimpleContainerData(1);
        public int mode() { return settings.get(0); }
        public String modeName() { return switch(mode()) { case 1 -> "Stonecutting"; case 2 -> "Smithing"; default -> "Machine batch"; }; }
        public Menu(int id, Inventory inventory) { this(id, inventory, null); }
        public Menu(int id, Inventory inventory, DigitalNode node) {
            super(TYPE, id); this.node = node; this.inventory = inventory;
            addDataSlots(settings);
            for (int i = 0; i < 12; i++) { int x = i < 9 ? 24 + i % 3 * 18 : 114 + (i - 9) * 18; int y = i < 9 ? 42 + i / 3 * 18 : 60;
                addSlot(new Slot(samples, i, x, y) { @Override public boolean mayPlace(ItemStack s) { return false; } @Override public boolean mayPickup(Player p) { return false; } });
            }
            for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, 9 + row * 9 + col, 18 + col * 18, 158 + row * 18));
            for (int i = 0; i < 9; i++) addSlot(new Slot(inventory, i, 18 + i * 18, 216));
        }
        @Override public boolean stillValid(Player p) { return p.level().isClientSide() || node != null && node.stillValid(p); }
        @Override public boolean canTakeItemForPickAll(ItemStack s, Slot slot) { return slot.container != samples && super.canTakeItemForPickAll(s, slot); }
        @Override public ItemStack quickMoveStack(Player p, int i) { return ItemStack.EMPTY; }
        @Override public void clicked(int slot, int button, ContainerInput input, Player p) {
            if (!stillValid(p)) return;
            if (slot >= 0 && slot < 12) {
                if (p instanceof ServerPlayer sp && input == ContainerInput.PICKUP && RateGate.allow(sp, "jimta_pattern_sample", 2)) {
                    var held = getCarried(); samples.setItem(slot, button == 1 || held.isEmpty() ? ItemStack.EMPTY : mode() == 0 ? new ItemStack(held.getItem(), held.getCount()) : held.copyWithCount(1)); broadcastFullState();
                }
                return;
            }
            super.clicked(slot, button, input, p);
        }
        @Override public boolean clickMenuButton(Player p, int id) {
            if (!(p instanceof ServerPlayer sp) || !stillValid(p) || !getCarried().isEmpty() || !RateGate.allow(sp, "jimta_pattern_encode", 5)) return false;
            if (id == 1) { samples.clearContent(); broadcastFullState(); return true; }
            if (id == 2) { settings.set(0, (mode() + 1) % 3); samples.clearContent(); broadcastFullState(); return true; }
            if (id != 0) return false;
            List<ItemStack> inputs = new ArrayList<>(), outputs = new ArrayList<>();
            for (int i = 0; i < 12; i++) if (!samples.getItem(i).isEmpty()) (i < 9 ? inputs : outputs).add(samples.getItem(i).copy());
            if (inputs.isEmpty() || mode() == 0 && outputs.isEmpty()) { sp.sendOverlayMessage(Component.literal("Select the ingredients and results of one real recipe.")); return false; }
            int blank = -1;
            for (int i = 0; i < 36; i++) if (inventory.getItem(i).is(DigitalStorage.ITEMS.get("blank_pattern"))) { blank = i; break; }
            if (blank < 0) { sp.sendOverlayMessage(Component.literal("Carry a blank pattern to encode this batch.")); return false; }
            ItemStack encoded;
            if (mode() > 0) {
                var grid = java.util.stream.IntStream.range(0, 9).mapToObj(i -> samples.getItem(i).copy()).toList();
                var pattern = DigitalSpecialPatterns.encode(mode(), grid, samples.getItem(9), sp.level());
                if (pattern == null) { sp.sendOverlayMessage(Component.literal("No matching recipe. Stonecutting uses the first input and selected output; smithing uses top-row template, base and addition.")); return false; }
                encoded = new ItemStack(DigitalStorage.ITEMS.get(mode() == 1 ? "stonecutting_pattern" : "smithing_table_pattern")); encoded.set(DigitalPatterns.PATTERN, pattern);
            } else { encoded = new ItemStack(DigitalStorage.ITEMS.get("processing_pattern")); encoded.set(DigitalProcessing.PATTERN, new DigitalProcessing.Pattern(inputs, outputs)); }
            inventory.removeItem(blank, 1); inventory.placeItemBackInInventory(encoded, net.minecraft.util.Prediction.SERVER_ONLY); inventory.setChanged();
            sp.sendOverlayMessage(Component.literal(mode() == 0 ? "Encoded. Put in a provider facing the correct powered machine." : "Encoded current recipe. Put in a pattern provider or molecular assembler.")); broadcastFullState(); return true;
        }
    }
}
