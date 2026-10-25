package io.github.jimbozoomer.jugcraft.furniture;

import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;

public final class DesignedFurnitureEntity extends RandomizableContainerBlockEntity {
    private FurnitureDesign design=FurnitureDesign.stool();
    private NonNullList<ItemStack> items=NonNullList.withSize(27,ItemStack.EMPTY);
    public DesignedFurnitureEntity(BlockPos pos,BlockState state){super(ArtisanFurniture.ENTITY,pos,state);}
    public FurnitureDesign design(){return design;}
    public void design(FurnitureDesign d){design=d;setChanged();if(level!=null){var state=getBlockState();level.setBlockAndUpdate(worldPosition,state.setValue(DesignedFurnitureBlock.LIGHT,d.light()));level.sendBlockUpdated(worldPosition,state,getBlockState(),3);}}
    @Override public int getContainerSize(){return design.storage()?27:0;}
    @Override protected NonNullList<ItemStack> getItems(){return items;}
    @Override protected void setItems(NonNullList<ItemStack> value){items=value;}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return design.storage()&&slot>=0&&slot<27;}
    @Override protected Component getDefaultName(){return Component.literal(design.name());}
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inventory){return ChestMenu.threeRows(id,inventory,this);}
    @Override public boolean stillValid(Player p){return design.storage()&&super.stillValid(p)&&ArtisanFurniture.mayUse(level,p,worldPosition);}
    @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);out.putString("design",design.json());ContainerHelper.saveAllItems(out,items);}
    @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);design=FurnitureDesign.safe(in.getString("design").orElse(""));items=NonNullList.withSize(27,ItemStack.EMPTY);ContainerHelper.loadAllItems(in,items);}
    @Override protected void applyImplicitComponents(DataComponentGetter components){super.applyImplicitComponents(components);design=FurnitureDesign.safe(components.getOrDefault(ArtisanFurniture.DESIGN,""));}
    @Override protected void collectImplicitComponents(DataComponentMap.Builder components){super.collectImplicitComponents(components);components.set(ArtisanFurniture.DESIGN,design.json());}
    @Override public void removeComponentsFromTag(ValueOutput out){super.removeComponentsFromTag(out);out.discard("design");}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){var tag=new CompoundTag();tag.putString("design",design.json());return tag;}
}
