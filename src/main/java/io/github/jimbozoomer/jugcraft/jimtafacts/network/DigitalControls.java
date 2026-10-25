package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Live stock/energy thresholds. The filter is a ghost sample, never a second source of inventory. */
public final class DigitalControls {
    public static final BooleanProperty SIGNAL = BooleanProperty.create("signal");
    public static MenuType<Menu> TYPE;
    private DigitalControls() {}
    public static void register() { TYPE = Registry.register(BuiltInRegistries.MENU, Jugcraft.id("jimta_level_control"), new MenuType<>(Menu::new, FeatureFlags.DEFAULT_FLAGS)); }
    public static boolean controls(String kind) { return kind.equals("level_emitter") || kind.equals("energy_level_emitter"); }
    public static void update(DigitalNode node, DigitalNetwork network) {
        if (!controls(node.kind())) return;
        long amount = node.kind().equals("energy_level_emitter") ? network.nodes().stream().filter(DigitalNode::poweredSource).mapToLong(n -> n.energy.getAmount()).sum()
                : network.contents().stream().filter(e -> node.filter().isEmpty() || ItemStack.isSameItemSameComponents(node.filter(), e.sample())).mapToLong(CellContents.Entry::count).sum();
        node.observed = amount;
        boolean signal = network.online() && (node.emitBelow ? amount < node.threshold : amount >= node.threshold);
        var level = node.getLevel(); var state = node.getBlockState();
        if (level != null && state.getValue(SIGNAL) != signal) node.updateSignal(state.setValue(SIGNAL,signal));
    }
    public static final class Emitter extends DigitalNodeBlock {
        public Emitter(Properties p, String kind) { super(p, kind); }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { super.createBlockStateDefinition(b); b.add(SIGNAL); }
        @Override protected boolean isSignalSource(BlockState state) { return true; }
        @Override protected int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction side) { return state.getValue(SIGNAL) ? 15 : 0; }
        @Override protected int getDirectSignal(BlockState state, BlockGetter world, BlockPos pos, Direction side) { return getSignal(state, world, pos, side); }
    }
    public static final class Menu extends AbstractContainerMenu {
        private final DigitalNode node;
        private final SimpleContainer sample = new SimpleContainer(1);
        private final ContainerData data = new SimpleContainerData(6);
        public Menu(int id, Inventory inventory) { this(id, inventory, null); }
        public Menu(int id, Inventory inventory, DigitalNode node) {
            super(TYPE, id); this.node = node; addDataSlots(data);
            addSlot(new Slot(sample, 0, 20, 39) { @Override public boolean mayPlace(ItemStack s) { return false; } @Override public boolean mayPickup(Player p) { return false; } });
            for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, 9 + row * 9 + col, 17 + col * 18, 130 + row * 18));
            for (int i = 0; i < 9; i++) addSlot(new Slot(inventory, i, 17 + i * 18, 188));
            refresh();
        }
        public int threshold() { return (data.get(0) & 65535) | (data.get(1) << 16); }
        public int amount() { return (data.get(2) & 65535) | (data.get(3) << 16); }
        public boolean below() { return data.get(4) == 1; }
        public boolean energy() { return data.get(5) == 1; }
        private void refresh() {
            if (node == null) return;
            data.set(0, node.threshold & 65535); data.set(1, node.threshold >>> 16);
            int n = (int) Math.min(Integer.MAX_VALUE, node.observed); data.set(2, n & 65535); data.set(3, n >>> 16);
            data.set(4, node.emitBelow ? 1 : 0); data.set(5, node.kind().equals("energy_level_emitter") ? 1 : 0);
            sample.setItem(0, node.filter());
        }
        @Override public boolean stillValid(Player p) { return p.level().isClientSide() || node != null && node.stillValid(p); }
        @Override public void broadcastChanges() { refresh(); super.broadcastChanges(); }
        @Override public void clicked(int slot, int button, ContainerInput input, Player p) {
            if (!stillValid(p)) return;
            if (slot == 0) {
                if (p instanceof ServerPlayer sp && node != null && input == ContainerInput.PICKUP && !energy() && RateGate.allow(sp, "jimta_level_filter", 2)) {
                    node.filter(button == 1 ? ItemStack.EMPTY : getCarried()); refresh(); broadcastFullState();
                }
                return;
            }
            super.clicked(slot, button, input, p);
        }
        @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) { return slot.container != sample && super.canTakeItemForPickAll(stack, slot); }
        @Override public ItemStack quickMoveStack(Player p, int slot) { return ItemStack.EMPTY; }
        @Override public boolean clickMenuButton(Player p, int id) {
            if (!(p instanceof ServerPlayer sp) || node == null || !stillValid(p) || !RateGate.allow(sp, "jimta_level_button", 2)) return false;
            int delta = switch (id) { case 0 -> -64; case 1 -> -1; case 2 -> 1; case 3 -> 64; case 4 -> -1000; case 5 -> 1000; default -> 0; };
            if (id == 6) node.emitBelow = !node.emitBelow;
            else if (delta != 0) node.threshold = Math.clamp(node.threshold + delta, 1, 1_000_000);
            else return false;
            node.setChanged(); DigitalControls.update(node, DigitalNetwork.find(node, sp)); refresh(); broadcastFullState(); return true;
        }
    }
}
