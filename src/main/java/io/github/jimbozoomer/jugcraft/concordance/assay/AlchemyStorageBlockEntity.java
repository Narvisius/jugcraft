package io.github.jimbozoomer.jugcraft.concordance.assay;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.Saved;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import org.jspecify.annotations.Nullable;

/** Manual party storage: no hopper or unsided bypass; ordinary mining spills the real contents exactly once. */
public final class AlchemyStorageBlockEntity extends BlockEntity implements WorldlyContainer {
    private NonNullList<ItemStack> items=NonNullList.withSize(54,ItemStack.EMPTY);
    private @Nullable UUID owner;
    public AlchemyStorageBlockEntity(BlockPos pos,BlockState state){super(AlchemyStorage.ENTITY,pos,state);}
    public void placedBy(ServerPlayer player){owner=player.getUUID();setChanged();}
    public boolean mayUse(ServerPlayer player){return owner!=null&&(owner.equals(player.getUUID())||JugcraftParties.sameParty(owner,player.getUUID()))
            &&player.isAlive()&&!player.isSpectator()&&level instanceof ServerLevel server&&Authority.mayChange(server,player,worldPosition);}
    @Override public boolean stillValid(Player player){return level!=null&&level.getBlockEntity(worldPosition)==this&&player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition))<=64&&(!(player instanceof ServerPlayer p)||mayUse(p));}
    public int viewers;
    public final AlchemyLid lid=new AlchemyLid();
    public static void tick(net.minecraft.world.level.Level level,BlockPos pos,BlockState state,AlchemyStorageBlockEntity be){
        be.lid.tick(level,pos,be.viewers);
        if(level instanceof ServerLevel server&&Math.floorMod(level.getGameTime()+pos.asLong(),20)==0&&be.viewers==0&&!level.hasNeighborSignal(pos)){
            var p=Authority.present(server,be.owner);
            if(p!=null&&be.mayUse(p)&&AlchemyAutomation.pulse(p,be,net.minecraft.world.phys.Vec3.atCenterOf(pos)))be.setChanged();
        }
    }
    @Override public int[] getSlotsForFace(Direction side){return new int[0];}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,@Nullable Direction side){return false;}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){return false;}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return AlchemyStorage.canStore(stack);}
    @Override public int getContainerSize(){return 54;}
    @Override public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}
    @Override public ItemStack getItem(int slot){return items.get(slot);}
    @Override public ItemStack removeItem(int slot,int count){var out=ContainerHelper.removeItem(items,slot,count);if(!out.isEmpty())setChanged();return out;}
    @Override public ItemStack removeItemNoUpdate(int slot){return ContainerHelper.takeItem(items,slot);}
    @Override public void setItem(int slot,ItemStack stack){items.set(slot,stack);setChanged();}
    @Override public void clearContent(){items.clear();setChanged();}
    @Override public void preRemoveSideEffects(BlockPos pos,BlockState state){if(level instanceof ServerLevel server){Containers.dropContents(server,pos,this);clearContent();}}
    @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);items=NonNullList.withSize(54,ItemStack.EMPTY);ContainerHelper.loadAllItems(in,items);owner=in.read("owner",UUIDUtil.CODEC).orElse(null);}
    @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);Saved.stamp(out,1);ContainerHelper.saveAllItems(out,items);if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);}
}
