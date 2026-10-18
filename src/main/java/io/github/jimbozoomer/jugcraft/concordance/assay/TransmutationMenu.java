package io.github.jimbozoomer.jugcraft.concordance.assay;

import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.equivalence.Assay;
import io.github.jimbozoomer.jugcraft.concordance.equivalence.Material;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Vanilla menu transactions: real input, retained sample, take-only output. No client-provided item or amount. */
public final class TransmutationMenu extends AbstractContainerMenu {
    public static final int INPUT = 0, SAMPLE = 1, OUTPUT = 2;
    private final Player owner;
    private final java.util.function.BooleanSupplier access;
    private final SimpleContainer contents = new SimpleContainer(3);
    private final SimpleContainerData data = new SimpleContainerData(9);
    private ItemStack confirmation = ItemStack.EMPTY;
    private long confirmUntil;

    public TransmutationMenu(int id, Inventory inventory) {
        this(id, inventory, () -> inventory.player.getInventory().contains(stack -> stack.is(AlchemicalGear.STONE))
                || inventory.player.getOffhandItem().is(AlchemicalGear.STONE));
    }
    public TransmutationMenu(int id, Inventory inventory, java.util.function.BooleanSupplier access) {
        super(AlchemicalGear.MENU, id);
        owner = inventory.player;
        this.access = access;
        addSlot(new Slot(contents, INPUT, 56, 70) {
            @Override public boolean mayPlace(ItemStack stack) { return !stack.is(AlchemicalGear.STONE); }
        });
        addSlot(new Slot(contents, SAMPLE, 199, 70) {
            @Override public boolean mayPlace(ItemStack stack) { return !stack.is(AlchemicalGear.STONE); }
            @Override public int getMaxStackSize() { return 1; }
        });
        addSlot(new Slot(contents, OUTPUT, 270, 70) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        addStandardInventorySlots(inventory, 74, 150);
        addDataSlots(data);
        refresh();
    }

    private Material material(int slot) {
        ItemStack stack = contents.getItem(slot);
        return stack.isEmpty() || !Assaying.eligibility(stack).isEmpty() ? null
                : Assaying.catalog().material(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }
    private void put(int offset, long value) { data.set(offset, (int) value & 0xffff); data.set(offset + 1, (int) (value >>> 16)); }
    private long number(int offset) { return (data.get(offset) & 0xffffL) | ((data.get(offset + 1) & 0xffffL) << 16); }
    public long balance() { return number(0); }
    public long gain() { return number(2); }
    public long cost() { return number(4); }
    public boolean canDissolve() { return (data.get(6) & 1) != 0; }
    public boolean canForm() { return (data.get(6) & 2) != 0; }
    public boolean confirming() { return data.get(7) != 0; }
    /** 0 ready, 1 no sample, 2 refused sample, 3 insufficient balance, 4 output full. */
    public int formStatus() { return data.get(8); }

    private boolean outputFits(ItemStack sample) {
        ItemStack out = contents.getItem(OUTPUT);
        return out.isEmpty() || ItemStack.isSameItemSameComponents(out, sample) && out.getCount() < out.getMaxStackSize();
    }

    private void refresh() {
        if (!(owner instanceof ServerPlayer player)) return;
        if (!ItemStack.matches(confirmation, contents.getItem(INPUT)) || player.level().getGameTime() > confirmUntil)
            confirmation = ItemStack.EMPTY;
        long balance = Assaying.balance(player);
        Material input = material(INPUT), sample = material(SAMPLE);
        long gain = input == null || !input.dissolvable() ? 0 : Assay.dissolve(input, Math.min(64, contents.getItem(INPUT).getCount()));
        long cost = sample == null || !sample.formable() ? 0 : Assay.form(sample, 1);
        boolean valid = stillValid(player);
        int status = contents.getItem(SAMPLE).isEmpty() ? 1 : cost == 0 ? 2 : cost > balance ? 3
                : !outputFits(contents.getItem(SAMPLE)) ? 4 : 0;
        put(0, balance); put(2, gain); put(4, cost);
        data.set(6, (valid && gain > 0 && balance + gain <= Assay.MAX_BALANCE ? 1 : 0) | (valid && status == 0 ? 2 : 0));
        data.set(7, confirmation.isEmpty() ? 0 : 1); data.set(8, status);
    }

    @Override public void broadcastChanges() { refresh(); super.broadcastChanges(); }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (player != owner || !(player instanceof ServerPlayer server) || id < 0 || id > 2 || !stillValid(player)
                || !RateGate.allow(server, "transmutation", 4)) return false;
        refresh();
        if (id == 2) {
            server.openMenu(new net.minecraft.world.SimpleMenuProvider((menuId, inv, who) ->
                    new AlchemyPatternMenu(menuId, inv, access, ItemStack.EMPTY), net.minecraft.network.chat.Component.translatable("screen.jugcraft.pattern.title")));
            return true;
        }
        if (id == 0 && canDissolve()) {
            if (!confirming()) {
                confirmation = contents.getItem(INPUT).copy();
                confirmUntil = server.level().getGameTime() + Assaying.CONFIRM_TICKS;
            } else {
                AlchemyPatterns.learn(server, contents.getItem(INPUT));
                Assaying.dissolve(server, contents.getItem(INPUT), material(INPUT));
                confirmation = ItemStack.EMPTY;
            }
        } else if (id == 1 && canForm()) {
            // Validate, then debit and deliver within this one server transaction. Never spill paid output.
            long price = cost();
            ItemStack out = contents.getItem(OUTPUT);
            if (out.isEmpty()) contents.setItem(OUTPUT, new ItemStack(contents.getItem(SAMPLE).getItem()));
            else out.grow(1);
            Assaying.setBalance(player, Assaying.balance(player) - price);
        }
        contents.setChanged();
        broadcastChanges();
        return true;
    }

    @Override public boolean stillValid(Player player) {
        return player == owner && (!(player instanceof ServerPlayer server) || AlchemicalGear.ready(server))
                && (!(player instanceof ServerPlayer) || access.getAsBoolean());
    }
    @Override public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer) clearContainer(player, contents);
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        if (index < 3) { if (!moveItemStackTo(stack, 3, slots.size(), true)) return ItemStack.EMPTY; }
        else if (stack.is(AlchemicalGear.STONE) || !moveItemStackTo(stack, INPUT, INPUT + 1, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return copy;
    }
}
