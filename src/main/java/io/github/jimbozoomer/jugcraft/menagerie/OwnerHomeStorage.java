package io.github.jimbozoomer.jugcraft.menagerie;

import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;

/** Source capacities: 24-slot storage counters and nine-slot desks/drawers. */
public final class OwnerHomeStorage extends RandomizableContainerBlockEntity {
    private NonNullList<ItemStack> items;
    private int viewers;
    public OwnerHomeStorage(BlockPos p, BlockState s) { super(OwnerHome.STORAGE,p,s); items=NonNullList.withSize(getContainerSize(),ItemStack.EMPTY); }
    @Override public int getContainerSize() { return ((OwnerHomeBlock)getBlockState().getBlock()).function.equals("storage_counter")?24:9; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> next) { items=next; }
    @Override protected Component getDefaultName() { return Component.translatable(getBlockState().getBlock().getDescriptionId()); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inv) { return new OwnerHomeMenu(id,inv,this); }
    @Override public boolean stillValid(Player p) { return super.stillValid(p)&&Menagerie.mayChange(level,p,worldPosition); }
    private void opened(boolean open) {
        if(level!=null&&!level.isClientSide()) level.setBlockAndUpdate(worldPosition,HouseholdBlock.set(getBlockState(),"open",Boolean.toString(open)));
    }
    @Override public void startOpen(ContainerUser user) { if(!user.getLivingEntity().isSpectator()){viewers++;opened(true);} }
    @Override public void stopOpen(ContainerUser user) { if(!user.getLivingEntity().isSpectator()){viewers=Math.max(0,viewers-1);opened(viewers>0);} }
    @Override protected void saveAdditional(ValueOutput out) { super.saveAdditional(out); if(!trySaveLootTable(out))ContainerHelper.saveAllItems(out,items); }
    @Override protected void loadAdditional(ValueInput in) { super.loadAdditional(in); items=NonNullList.withSize(getContainerSize(),ItemStack.EMPTY);if(!tryLoadLootTable(in))ContainerHelper.loadAllItems(in,items); }
}
