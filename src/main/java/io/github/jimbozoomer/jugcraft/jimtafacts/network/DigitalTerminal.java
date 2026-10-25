package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;

/** Server-owned requests. Catalog icons are never takeable container slots. */
public final class DigitalTerminal {
    public static MenuType<Menu> TYPE;
    public static MenuType<DriveMenu> DRIVE;
    private DigitalTerminal() {}
    public static void register() {
        TYPE = Registry.register(BuiltInRegistries.MENU, Jugcraft.id("jimta_me_terminal"), new MenuType<>(Menu::new, FeatureFlags.DEFAULT_FLAGS));
        DRIVE = Registry.register(BuiltInRegistries.MENU, Jugcraft.id("jimta_me_drive"), new MenuType<>(DriveMenu::new, FeatureFlags.DEFAULT_FLAGS));
        PayloadTypeRegistry.serverboundPlay().register(Search.TYPE, Search.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Search.TYPE, (packet, context) -> {
            var p = context.player();
            if (p.containerMenu instanceof Menu m && m.containerId == packet.menu && m.stillValid(p) && RateGate.allow(p, "jimta_search", 2)) {
                m.search = packet.query.strip().toLowerCase(Locale.ROOT); m.page = 0; m.refresh(); m.broadcastFullState();
            }
        });
    }
    public record Search(int menu, String query) implements CustomPacketPayload {
        public static final Type<Search> TYPE = new Type<>(Jugcraft.id("jimta_me_search"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Search> CODEC = new StreamCodec<>() {
            public Search decode(RegistryFriendlyByteBuf b) { return new Search(b.readVarInt(), b.readUtf(48)); }
            public void encode(RegistryFriendlyByteBuf b, Search p) { b.writeVarInt(p.menu); b.writeUtf(p.query, 48); }
        };
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    private record Key(ItemStack item) {
        @Override public int hashCode() { return ItemStack.hashItemAndComponents(item); }
        @Override public boolean equals(Object other) { return other instanceof Key k && ItemStack.isSameItemSameComponents(item, k.item); }
    }
    public static final class Menu extends AbstractContainerMenu {
        public static final int GRID = 36, RESULT = GRID + 9, PLAYER = RESULT + 1;
        private final Inventory inventory;
        private final DigitalNode node;
        private final boolean wireless, craftingEnabled;
        private PortableCells.Held portable;
        private final java.util.List<net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant> fluids = new java.util.ArrayList<>();
        private final SimpleContainer catalog = new SimpleContainer(GRID);
        private final TransientCraftingContainer crafting = new TransientCraftingContainer(this, 3, 3);
        private final ResultContainer result = new ResultContainer();
        private final ContainerData data = new SimpleContainerData(4 + GRID * 2);
        private String search = "";
        private int page;
        private boolean patterns;
        private long refreshed = -20;
        public Menu(int id, Inventory inventory) { this(id, inventory, null); }
        public static Menu portable(int id, Inventory inventory, ItemStack held) {
            var menu = new Menu(id, inventory); menu.portable = new PortableCells.Held(inventory.player, held); menu.refresh(); return menu;
        }
        public Menu(int id, Inventory inventory, DigitalNode node) { this(id, inventory, node, false, node != null && node.kind().equals("crafting_terminal")); }
        public Menu(int id, Inventory inventory, DigitalNode node, boolean wireless, boolean craftingEnabled) {
            super(TYPE, id); this.inventory = inventory; this.node = node; this.wireless = wireless; this.craftingEnabled = craftingEnabled;
            patterns = node != null && node.kind().equals("pattern_access_terminal");
            for (int i = 0; i < GRID; i++) addSlot(new Slot(catalog, i, 18 + i % 9 * 18, 50 + i / 9 * 18) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
                @Override public boolean mayPickup(Player p) { return false; }
            });
            for (int i = 0; i < 9; i++) addSlot(new Slot(crafting, i, 222 + i % 3 * 18, 72 + i / 3 * 18) {
                @Override public boolean mayPlace(ItemStack stack) { return canCraft(); }
                @Override public boolean isActive() { return canCraft(); }
            });
            addSlot(new ResultSlot(inventory.player, crafting, result, 0, 294, 90) {
                @Override public boolean mayPickup(Player p) { return stillValid(p) && canCraft(); }
                @Override public boolean isActive() { return canCraft(); }
            });
            for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, 9 + row * 9 + col, 18 + col * 18, 156 + row * 18));
            for (int i = 0; i < 9; i++) addSlot(new Slot(inventory, i, 18 + i * 18, 214));
            addDataSlots(data); refresh();
        }
        public int page() { return data.get(0); }
        public int pages() { return Math.max(1, data.get(1)); }
        public boolean online() { return data.get(2) > 0; }
        public boolean canCraft() { return (data.get(3) & 1) != 0; }
        public boolean patternsMode() { return (data.get(3) & 2) != 0; }
        public boolean fluidMode() { return (data.get(3) & 4) != 0; }
        public boolean portableMode() { return (data.get(3) & 8) != 0; }
        public int count(int i) { return (data.get(4 + i * 2) & 65535) | (data.get(5 + i * 2) << 16); }
        private DigitalNetwork network() { return node != null && inventory.player instanceof ServerPlayer p && stillValid(p) ? DigitalNetwork.find(node, p) : new DigitalNetwork(List.of(), false); }
        private boolean powered() { return portable == null ? network().online() : portable.online(); }
        private net.fabricmc.fabric.api.transfer.v1.storage.Storage<net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant> fluidPort(ServerPlayer player) { return portable == null ? new FluidCells.Port(node, player) : portable; }
        private void refresh() {
            if (!(inventory.player instanceof ServerPlayer)) return;
            if (portable != null && portable.fluid() || node != null && node.kind().equals("fluid_terminal")) { refreshFluids((ServerPlayer) inventory.player); return; }
            var network = network(); var grouped = new LinkedHashMap<Key, Integer>();
            var visible = new ArrayList<CellContents.Entry>(patterns ? DigitalPatterns.available(network, ((ServerPlayer) inventory.player).level()).stream()
                    .map(p -> { var result = p.output(((ServerPlayer) inventory.player).level()); return new CellContents.Entry(result, result.getCount()); }).toList() : portable == null ? network.contents() : portable.contents());
            if (patterns) DigitalProcessing.available(network).forEach(b -> b.pattern().outputs().forEach(s -> visible.add(new CellContents.Entry(s, s.getCount()))));
            for (var e : visible) {
                var sample = e.sample(); String name = sample.getHoverName().getString().toLowerCase(Locale.ROOT);
                if (!DigitalCellCards.visible(node, sample)) continue;
                if (!search.isEmpty() && !name.contains(search) && !BuiltInRegistries.ITEM.getKey(sample.getItem()).toString().contains(search)) continue;
                grouped.merge(new Key(sample), e.count(), (a, b) -> (int) Math.min(Integer.MAX_VALUE, (long) a + b));
            }
            var entries = new ArrayList<>(grouped.entrySet()); entries.sort(Comparator.comparing(e -> e.getKey().item.getHoverName().getString()));
            int pages = Math.max(1, (entries.size() + GRID - 1) / GRID); page = Math.clamp(page, 0, pages - 1);
            data.set(0, page); data.set(1, pages); data.set(2, powered() ? 1 : 0); data.set(3, (craftingEnabled ? 1 : 0) | (patterns ? 2 : 0) | (portable != null ? 8 : 0));
            for (int i = 0; i < GRID; i++) {
                int at = page * GRID + i, n = at < entries.size() ? entries.get(at).getValue() : 0;
                catalog.setItem(i, at < entries.size() ? entries.get(at).getKey().item.copyWithCount(1) : ItemStack.EMPTY);
                data.set(4 + i * 2, n & 65535); data.set(5 + i * 2, n >>> 16);
            }
            refreshed = inventory.player.level().getGameTime();
        }
        private void refreshFluids(ServerPlayer player) {
            var entries = new ArrayList<>((portable == null ? new FluidCells.Port(node, player).amounts() : portable.amounts()).entrySet());
            entries.removeIf(e -> !search.isEmpty() && !fluidName(e.getKey()).toLowerCase(Locale.ROOT).contains(search));
            entries.sort(Comparator.comparing(e -> fluidName(e.getKey())));
            int pages = Math.max(1, (entries.size() + GRID - 1) / GRID); page = Math.clamp(page, 0, pages - 1);
            data.set(0, page); data.set(1, pages); data.set(2, powered() ? 1 : 0); data.set(3, 4 | (portable != null ? 8 : 0)); fluids.clear();
            for (int i = 0; i < GRID; i++) {
                int at = page * GRID + i; var fluid = at < entries.size() ? entries.get(at).getKey() : null;
                fluids.add(fluid); ItemStack icon = ItemStack.EMPTY; int amount = 0;
                if (fluid != null) {
                    Item bucket = fluid.getFluid().getBucket(); icon = new ItemStack(bucket == Items.AIR ? Items.GLASS_BOTTLE : bucket);
                    long mb = entries.get(at).getValue() / (net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET / 1000);
                    icon.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal(fluidName(fluid) + " · " + mb + " mB"));
                    amount = (int) Math.min(Integer.MAX_VALUE, mb);
                }
                catalog.setItem(i, icon); data.set(4 + i * 2, amount & 65535); data.set(5 + i * 2, amount >>> 16);
            }
            refreshed = player.level().getGameTime();
        }
        private static String fluidName(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant fluid) {
            var bucket = fluid.getFluid().getBucket(); return bucket == Items.AIR ? BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString() : new ItemStack(bucket).getHoverName().getString();
        }
        private void moveFluid(ServerPlayer player, int index) {
            if (getCarried().isEmpty()) { player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Pick up an empty or filled container, then click the fluid grid.")); return; }
            var carried = net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext.ofPlayerCursor(player, this).find(net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.ITEM);
            if (carried == null) return;
            var port = fluidPort(player); long maximum = net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET;
            try (var tx = Transaction.openOuter()) {
                long moved = net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil.move(carried, port, f -> true, maximum, tx);
                if (moved == 0 && index < fluids.size() && fluids.get(index) != null) {
                    var fluid = fluids.get(index);
                    moved = net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil.move(port, carried, fluid::equals, maximum, tx);
                }
                if (moved > 0) tx.commit();
            }
            refresh(); broadcastFullState();
        }
        private ItemStack withdraw(ItemStack sample, int maximum) {
            if (sample.isEmpty() || maximum <= 0) return ItemStack.EMPTY;
            try (Transaction tx = Transaction.openOuter()) {
                int n = (int) (portable == null ? network().extract(sample, Math.min(sample.getMaxStackSize(), maximum), tx) : portable.item(sample, Math.min(sample.getMaxStackSize(), maximum), tx, false));
                if (n == 0) return ItemStack.EMPTY;
                tx.commit(); return sample.copyWithCount(n);
            }
        }
        private boolean deposit(ItemStack stack) {
            if (stack.isEmpty()) return false;
            try (Transaction tx = Transaction.openOuter()) {
                int n = (int) (portable == null ? network().insert(stack, stack.getCount(), tx) : portable.item(stack, stack.getCount(), tx, true));
                if (n == 0) return false;
                tx.commit(); stack.shrink(n); return true;
            }
        }
        private int room(ItemStack stack) {
            int n = 0; for (int i = 0; i < 36; i++) { var old = inventory.getItem(i); if (old.isEmpty()) n += stack.getMaxStackSize(); else if (ItemStack.isSameItemSameComponents(old, stack)) n += Math.max(0, old.getMaxStackSize() - old.getCount()); } return n;
        }
        @Override public boolean stillValid(Player p) { return p == inventory.player && p.isAlive() && !p.isSpectator() && (!(p instanceof ServerPlayer sp) || (portable != null ? portable.valid() : node != null && (wireless ? WirelessTerminal.held(sp, node) : node.stillValid(p)))); }
        @Override public void clicked(int slot, int button, ContainerInput input, Player p) {
            if (!stillValid(p)) return;
            if (slot >= 0 && slot < GRID) {
                if (!(p instanceof ServerPlayer sp) || !RateGate.allow(sp, "jimta_transfer", 1)) return;
                if (fluidMode()) { if (input == ContainerInput.PICKUP && button >= 0 && button <= 1) moveFluid(sp, slot); return; }
                if (input == ContainerInput.QUICK_MOVE) { quickMoveStack(p, slot); return; }
                if (input != ContainerInput.PICKUP || button < 0 || button > 1) return;
                if (patterns) { request(catalog.getItem(slot), button == 1 ? 16 : 1); return; }
                if (!getCarried().isEmpty()) { deposit(getCarried()); if (getCarried().isEmpty()) setCarried(ItemStack.EMPTY); }
                else { var selected = catalog.getItem(slot); if (!selected.isEmpty()) setCarried(withdraw(selected, button == 1 ? 1 : selected.getMaxStackSize())); }
                refresh(); broadcastFullState(); return;
            }
            super.clicked(slot, button, input, p);
        }
        @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) { return slot.container != catalog && slot.container != result && super.canTakeItemForPickAll(stack, slot); }
        @Override public void slotsChanged(Container changed) {
            if (!(inventory.player instanceof ServerPlayer p)) return;
            var input = crafting.asCraftInput(); var recipe = p.level().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, p.level()).orElse(null);
            result.setRecipeUsed(recipe); result.setItem(0, recipe == null ? ItemStack.EMPTY : recipe.value().assemble(input)); broadcastChanges();
        }
        @Override public void broadcastChanges() { if (inventory.player instanceof ServerPlayer && inventory.player.level().getGameTime() - refreshed >= 10) refresh(); super.broadcastChanges(); }
        @Override public boolean clickMenuButton(Player p, int id) {
            if (!(p instanceof ServerPlayer sp) || !stillValid(p) || !getCarried().isEmpty() || !RateGate.allow(sp, "jimta_page", 3)) return false;
            if (id == 0) page--; else if (id == 1) page++;
            else if (id == 2) { if (fluidMode()) return false; for (int i = 9; i < 36; i++) deposit(inventory.getItem(i)); inventory.setChanged(); }
            else if (id == 3) { if (fluidMode() || portable != null) return false; patterns = !patterns; page = 0; }
            else if (id == 4) { if (!encode(sp)) return false; }
            else return false;
            refresh(); broadcastFullState(); return true;
        }
        @Override public ItemStack quickMoveStack(Player p, int index) {
            if (!(p instanceof ServerPlayer) || !stillValid(p) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
            if (fluidMode()) return ItemStack.EMPTY;
            if (index < GRID) {
                var example = catalog.getItem(index); if (example.isEmpty()) return ItemStack.EMPTY;
                if (patterns) { request(example, 16); return ItemStack.EMPTY; }
                var taken = withdraw(example, Math.min(example.getMaxStackSize(), room(example))); var copy = taken.copy();
                inventory.add(taken); if (!taken.isEmpty()) inventory.placeItemBackInInventory(taken, net.minecraft.util.Prediction.SERVER_ONLY);
                refresh(); broadcastFullState(); return copy;
            }
            Slot slot = slots.get(index); if (!slot.hasItem() || !slot.mayPickup(p)) return ItemStack.EMPTY;
            var stack = slot.getItem(); var copy = stack.copy();
            if (index >= PLAYER) { if (!deposit(stack)) return ItemStack.EMPTY; }
            else { if (index == RESULT && room(stack) < stack.getCount()) return ItemStack.EMPTY; if (!moveItemStackTo(stack, PLAYER, slots.size(), true)) return ItemStack.EMPTY; }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged(); slot.onTake(p, copy); if (index == RESULT) slotsChanged(crafting); return copy;
        }
        private boolean encode(ServerPlayer player) {
            if (!canCraft() || result.getItem(0).isEmpty()) return false;
            int blank = -1;
            for (int i = 0; i < 36; i++) if (inventory.getItem(i).is(DigitalStorage.ITEMS.get("blank_pattern"))) { blank = i; break; }
            if (blank < 0) { player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Carry a blank pattern to encode this recipe.")); return false; }
            var pattern = new DigitalPatterns.Pattern(java.util.stream.IntStream.range(0, 9).mapToObj(crafting::getItem).toList());
            if (pattern.output(player.level()).isEmpty()) return false;
            var encoded = new ItemStack(DigitalStorage.ITEMS.get("crafting_pattern")); encoded.set(DigitalPatterns.PATTERN, pattern);
            inventory.removeItem(blank, 1); inventory.placeItemBackInInventory(encoded, net.minecraft.util.Prediction.SERVER_ONLY); inventory.setChanged();
            player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Pattern encoded. Put it in a connected Pattern Provider.")); return true;
        }
        private void request(ItemStack output, int crafts) {
            if (output.isEmpty() || !(inventory.player instanceof ServerPlayer player)) return;
            var network = network(); if (!network.online()) return;
            for (var binding : DigitalProcessing.available(network)) {
                if (binding.pattern().outputs().stream().anyMatch(s -> ItemStack.isSameItemSameComponents(s, output)) && DigitalProcessing.request(binding, crafts)) {
                    player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Queued " + crafts + " machine batches. Provider must face the powered machine.")); return;
                }
            }
            for (var pattern : DigitalPatterns.available(network, player.level())) {
                if (!ItemStack.isSameItemSameComponents(output, pattern.output(player.level()))) continue;
                for (var cpu : network.nodes()) if (cpu.request(pattern, crafts)) { player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Queued " + crafts + " craft(s); waiting for ingredients and storage space.")); return; }
            }
            player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Connect a Crafting Unit with a free request slot."));
        }
        @Override public void removed(Player p) { super.removed(p); if (p instanceof ServerPlayer) clearContainer(p, crafting); result.clearContent(); }
    }

    /** Explicit cell slots keep arbitrary items out without discarding unexpected stacks. */
    public static final class DriveMenu extends AbstractContainerMenu {
        private final Container contents;
        private final ContainerData data = new SimpleContainerData(8);
        private static final java.util.List<String> KINDS = java.util.stream.Stream.concat(java.util.Arrays.stream(DigitalStorage.DEVICES), java.util.stream.Stream.of("import_bus", "export_bus", "storage_bus", "fluid_import_bus", "fluid_export_bus", "fluid_storage_bus", "formation_plane", "annihilation_plane")).distinct().toList();
        public String kind() { int index = data.get(0); return index >= 0 && index < KINDS.size() ? KINDS.get(index) : ""; }
        public int energy() { return (data.get(1) & 65535) | (data.get(2) << 16); }
        public int capacity() { return (data.get(3) & 65535) | (data.get(4) << 16); }
        public boolean singularities() { return data.get(5) != 0; }
        public int matter() { return (data.get(6) & 65535) | (data.get(7) << 16); }
        private boolean visible(int slot) {
            if (DigitalUpgrades.port(kind())) return slot < 14;
            return switch (kind()) {
                case "drive" -> slot < 10;
                case "me_chest", "vibration_chamber", "molecular_assembler", "wireless_access_point" -> slot == 0;
                case "condenser" -> slot < 3;
                case "charger", "io_port" -> slot < 2;
                case "inscriber" -> slot < 5;
                default -> true;
            };
        }
        public DriveMenu(int id, Inventory inventory) { this(id, inventory, new SimpleContainer(18)); }
        public DriveMenu(int id, Inventory inventory, Container contents) {
            super(DRIVE, id); this.contents = contents;
            data.set(0, contents instanceof DigitalNode node ? KINDS.indexOf(node.kind()) : -1);
            addDataSlots(data);
            for (int i = 0; i < 18; i++) { final int slot = i; addSlot(new Slot(contents, i, 8 + i % 9 * 18, 30 + i / 9 * 18) {
                @Override public boolean mayPlace(ItemStack stack) { return contents.canPlaceItem(slot, stack); }
                @Override public int getMaxStackSize() { return DigitalUpgrades.port(kind()) ? 1 : super.getMaxStackSize(); }
                @Override public boolean isActive() { return visible(slot); }
            }); }
            for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, 9 + row * 9 + col, 8 + col * 18, 80 + row * 18));
            for (int i = 0; i < 9; i++) addSlot(new Slot(inventory, i, 8 + i * 18, 138));
        }
        @Override public boolean stillValid(Player p) { return p.level().isClientSide() || contents.stillValid(p); }
        @Override public void broadcastChanges() {
            if (contents instanceof DigitalNode node) {
                long amount = node.energy.getAmount(), capacity = node.energy.getCapacity();
                data.set(1, (int) amount & 65535); data.set(2, (int) (amount >>> 16));
                data.set(3, (int) capacity & 65535); data.set(4, (int) (capacity >>> 16));
                data.set(5, node.emitBelow ? 1 : 0);
                int matter = (int) (node.condensed / net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET);
                data.set(6, matter & 65535); data.set(7, matter >>> 16);
            }
            super.broadcastChanges();
        }
        @Override public void clicked(int slot, int button, ContainerInput input, Player p) { if (stillValid(p)) super.clicked(slot, button, input, p); }
        @Override public boolean clickMenuButton(Player p, int id) {
            if (id != 0 || !(p instanceof ServerPlayer sp) || !stillValid(p) || !(contents instanceof DigitalNode node) || !(node.kind().equals("condenser")||node.kind().equals("io_port")) || !RateGate.allow(sp, "jimta_cell_mode", 3)) return false;
            node.emitBelow = !node.emitBelow; node.setChanged(); broadcastChanges(); return true;
        }
        @Override public ItemStack quickMoveStack(Player p, int i) {
            if (!stillValid(p) || i < 0 || i >= slots.size()) return ItemStack.EMPTY;
            if (contents instanceof DigitalNode node) DigitalWorkshops.configureCell(node);
            var slot = slots.get(i); if (!slot.hasItem()) return ItemStack.EMPTY;
            var stack = slot.getItem(); var copy = stack.copy();
            if (i < 18 ? !moveItemStackTo(stack, 18, slots.size(), true) : !moveItemStackTo(stack, 0, 18, false)) return ItemStack.EMPTY;
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged(); slot.onTake(p, copy); return copy;
        }
    }
}
