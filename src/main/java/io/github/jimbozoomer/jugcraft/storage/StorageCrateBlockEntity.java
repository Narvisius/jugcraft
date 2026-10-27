package io.github.jimbozoomer.jugcraft.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import org.jspecify.annotations.Nullable;

/** A passive mixed-item inventory. Slot indices are integers: vanilla's item-list codec stops at 255. */
public final class StorageCrateBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, ExtendedMenuProvider<Integer> {
    private NonNullList<ItemStack> items;
    private final int[] faces;
    boolean upgrading;
    private record Entry(int slot, ItemStack stack) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(0, 377).fieldOf("slot").forGetter(Entry::slot),
            ItemStack.CODEC.fieldOf("stack").forGetter(Entry::stack)).apply(i, Entry::new));
    }
    public StorageCrateBlockEntity(BlockPos pos, BlockState state) {
        super(JugcraftStorage.STORAGE_CRATE_ENTITY, pos, state);
        items = NonNullList.withSize(tier().slots, ItemStack.EMPTY);
        faces = java.util.stream.IntStream.range(0, tier().slots).toArray();
    }
    public StorageCrateTier tier() { return ((StorageCrateBlock) getBlockState().getBlock()).tier; }
    public int occupiedSlots() { return (int) items.stream().filter(s -> !s.isEmpty()).count(); }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> items) { this.items = items; }
    @Override public int getContainerSize() { return items.size(); }
    @Override protected Component getDefaultName() { return Component.translatable("block.jugcraft." + tier().id()); }
    @Override public boolean stillValid(Player player) {
        return !isRemoved() && !upgrading && super.stillValid(player) && player.isAlive()
            && StorageCrateBlock.mayUse(level, worldPosition, player);
    }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new StorageCrateMenu(id, inventory, this); }
    @Override public Integer getScreenOpeningData(ServerPlayer player) { return tier().ordinal(); }
    @Override public int[] getSlotsForFace(Direction side) { return isRemoved() || upgrading ? new int[0] : faces; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return !isRemoved() && !upgrading; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return !isRemoved() && !upgrading; }
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (!upgrading && level != null && !level.isClientSide()) {
            Containers.dropContents(level, pos, items);
            items.clear();
        }
    }
    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("storage_crate_version", 1);
        var entries = out.list("storage_items", Entry.CODEC);
        for (int i = 0; i < items.size(); i++) if (!items.get(i).isEmpty()) entries.add(new Entry(i, items.get(i)));
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        items = NonNullList.withSize(tier().slots, ItemStack.EMPTY);
        for (var entry : in.listOrEmpty("storage_items", Entry.CODEC)) if (entry.slot < items.size()) items.set(entry.slot, entry.stack);
    }
    @Override protected void collectImplicitComponents(DataComponentMap.Builder components) {
        // Pick-block and loot must never construct vanilla's 256-slot item-container
        // component from this larger inventory. Crate block items stay empty.
        components.set(DataComponents.CUSTOM_NAME, getCustomName());
    }
    @Override public void removeComponentsFromTag(ValueOutput out) {
        super.removeComponentsFromTag(out);
        out.discard("storage_items");
        out.discard("storage_crate_version");
    }
}
