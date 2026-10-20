package io.github.jimbozoomer.jugcraft.vehiclebay;

import java.util.List;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.Footprint;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;

/** Inputs stay in locked slots until completion. Cancel/break returns the actual inputs, never an unearned vehicle. */
public class VehicleBayBlockEntity extends BaseContainerBlockEntity implements ExtendedMenuProvider<BlockPos>, WorldlyContainer {
    public static final int INPUTS = 9, REFIT = 9, OUTPUT = 10, SLOTS = 11, DATA = 10, DRAW = 64;
    public static final int READY = 0, MATERIALS = 1, POWER = 2, BLOCKED = 3, WORKING = 4, DISABLED = 5, WRONG_KIT = 6, STRUCTURE = 7;
    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    public final SimpleEnergyStorage energy = new SimpleEnergyStorage(64_000, 512, 0, this::setChanged);
    private int selection, armor, drive, progress, total, status;
    private boolean intact;
    private int check;
    public VehicleBayBlockEntity(BlockPos pos, BlockState state) {
        super(VehicleBays.ENTITY, pos, state); selection = tank() ? 3 : 0;
    }
    public boolean tank() { return ((VehicleBayBlock)getBlockState().getBlock()).bayHeight == 3; }
    public boolean busy() { return total > 0; }
    public int progress() { return progress; }
    public int status() { return status; }
    public int plan() { int refit = VehiclePlans.plan(items.get(REFIT)); return refit >= 0 ? refit : selection; }
    public List<VehiclePlans.Need> needs() {
        return VehiclePlans.needs(plan(), armor, drive, !items.get(REFIT).isEmpty(), VehicleFitout.read(items.get(REFIT)));
    }
    public final ContainerData data = new ContainerData() {
        public int get(int i) { return switch(i) {
            case 0 -> plan(); case 1 -> armor; case 2 -> drive; case 3 -> progress; case 4 -> total;
            case 5 -> (int)energy.getAmount() & 65535; case 6 -> (int)(energy.getAmount() >>> 16);
            case 7 -> status; case 8 -> tank() ? 1 : 0; case 9 -> items.get(REFIT).isEmpty() ? 0 : 1; default -> 0;
        }; }
        public void set(int i, int value) { }
        public int getCount() { return DATA; }
    };
    public boolean select(int button) {
        if (busy()) return false;
        if (button >= 0 && button <= 3 && (tank() ? button == 3 : button < 3)) selection = button;
        else if (button == 10 || button == 11) armor = button - 10;
        else if (button >= 20 && button <= 22) drive = button - 20;
        else return false;
        setChanged(); return true;
    }
    public boolean start() {
        if (level == null || level.isClientSide() || busy()) return false;
        intact = structureIntact(); status = readiness();
        if (status != READY) return false;
        total = items.get(REFIT).isEmpty() ? 200 : 100; progress = 0;
        setChanged(); return true;
    }
    public void cancel() { total = 0; progress = 0; setChanged(); }
    private int readiness() {
        if (!JugcraftConfig.isFeatureEnabled("machines")) return DISABLED;
        if (!intact) return STRUCTURE;
        int plan = plan();
        if ((!items.get(REFIT).isEmpty() && VehiclePlans.plan(items.get(REFIT)) < 0) || (tank() ? plan != 3 : plan == 3)) return WRONG_KIT;
        if (!items.get(OUTPUT).isEmpty()) return BLOCKED;
        if (payment() == null) return MATERIALS;
        return energy.getAmount() < DRAW ? POWER : READY;
    }
    /** Simulate the entire bill against copies, including repeated steel requirements. */
    private NonNullList<ItemStack> payment() {
        var after = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < SLOTS; i++) after.set(i, items.get(i).copy());
        for (var need : needs()) {
            int remaining = need.count();
            for (int slot = 0; slot < INPUTS && remaining > 0; slot++) if (need.matches(after.get(slot))) {
                int take = Math.min(remaining, after.get(slot).getCount()); after.get(slot).shrink(take); remaining -= take;
            }
            if (remaining != 0) return null;
        }
        return after;
    }
    public boolean structureIntact() {
        if (level == null) return false;
        var block = (VehicleBayBlock)getBlockState().getBlock(); var facing = getBlockState().getValue(MachineBlock.FACING);
        var footprint = block.footprint(getBlockState());
        for (int y = 0; y < block.bayHeight; y++) for (int z = 0; z < 6; z++) for (int x = 0; x < 6; x++) {
            var offset = new Vec3i(-x, y, z); var pos = worldPosition.offset(Footprint.rotate(offset, facing));
            if (!level.isLoaded(pos)) return false;
            var there = level.getBlockState(pos); int part = footprint.offsets().indexOf(offset);
            if (part >= 0) {
                if (!there.is(block) || block.part(there) != part || there.getValue(MachineBlock.FACING) != facing) return false;
            } else if (!there.isAir()) return false;
        }
        return true;
    }
    public void tick() {
        if (++check >= 20 || check == 1) { intact = structureIntact(); if (check >= 20) check = 0; }
        if (busy() && progress + 1 >= total) intact = structureIntact();
        status = readiness();
        boolean running = busy() && status == READY;
        if (running) {
            energy.setAmount(energy.getAmount() - DRAW); progress++; status = WORKING;
            if (progress >= total) {
                var after = payment();
                if (after != null && items.get(OUTPUT).isEmpty()) {
                    ItemStack output = items.get(REFIT).isEmpty() ? VehiclePlans.product(plan()) : items.get(REFIT).copyWithCount(1);
                    int fuel = VehicleFitout.fuel(output);
                    // A chassis conversion preserves the fuel of the walker used as a construction input.
                    if (items.get(REFIT).isEmpty()) for (int i = 0; i < INPUTS; i++)
                        if (VehiclePlans.plan(items.get(i)) >= 0) fuel = VehicleFitout.fuel(items.get(i));
                    for (int i = 0; i < INPUTS; i++) items.set(i, after.get(i));
                    if (!items.get(REFIT).isEmpty()) items.get(REFIT).shrink(1);
                    items.set(OUTPUT, new VehicleFitout(armor, drive).write(output, fuel));
                    total = 0; progress = 0;
                }
            }
            setChanged();
        }
        if (getBlockState().getValue(MachineBlock.LIT) != running)
            level.setBlock(worldPosition, getBlockState().setValue(MachineBlock.LIT, running), Block.UPDATE_CLIENTS);
    }
    @Override public boolean stillValid(Player player) {
        return level != null && !isRemoved() && level.getBlockEntity(worldPosition) == this
            && player.distanceToSqr(worldPosition.getX()+.5, worldPosition.getY()+.5, worldPosition.getZ()+.5) <= 144
            && (!(player instanceof ServerPlayer server) || Authority.mayChange(server.level(), player, worldPosition));
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        if (busy() || slot == OUTPUT) return false;
        return slot != REFIT || (tank() ? VehiclePlans.plan(stack) == 3 : VehiclePlans.plan(stack) >= 0 && VehiclePlans.plan(stack) < 3);
    }
    @Override public void setItem(int slot, ItemStack stack) {
        super.setItem(slot, stack);
        if (slot == REFIT && !stack.isEmpty() && !busy()) {
            var installed = VehicleFitout.read(stack);
            armor = installed.armor(); drive = installed.drive(); setChanged();
        }
    }
    @Override protected Component getDefaultName() { return getBlockState().getBlock().getName(); }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> items) { this.items = items; }
    @Override public int getContainerSize() { return SLOTS; }
    // This is a manually supervised vehicle workstation. Never expose locked materials through hopper fallbacks.
    @Override public int[] getSlotsForFace(Direction side) { return new int[0]; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inv) { return new VehicleBayMenu(id, inv, this, data); }
    @Override public BlockPos getScreenOpeningData(ServerPlayer player) { return worldPosition; }
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !level.isClientSide()) { Containers.dropContents(level, pos, this); clearContent(); }
        super.preRemoveSideEffects(pos, state);
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output); ContainerHelper.saveAllItems(output, items);
        output.putInt("Plan", selection); output.putInt("Armor", armor); output.putInt("Drive", drive);
        output.putInt("Progress", progress); output.putInt("Total", total); output.putLong("Energy", energy.getAmount());
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input); items = NonNullList.withSize(SLOTS, ItemStack.EMPTY); ContainerHelper.loadAllItems(input, items);
        selection = tank() ? 3 : Math.clamp(input.getIntOr("Plan", 0), 0, 2);
        armor = Math.clamp(input.getIntOr("Armor", 0), 0, 1); drive = Math.clamp(input.getIntOr("Drive", 0), 0, 2);
        total = Math.clamp(input.getIntOr("Total", 0), 0, 200); progress = Math.clamp(input.getIntOr("Progress", 0), 0, total);
        energy.setAmount(input.getLongOr("Energy", 0));
    }
}
