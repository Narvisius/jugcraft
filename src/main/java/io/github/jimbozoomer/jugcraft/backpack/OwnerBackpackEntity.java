package io.github.jimbozoomer.jugcraft.backpack;

import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import io.github.jimbozoomer.jugcraft.concordance.Authority;

/** Placed and held forms use the same serialized carrier; opening never generates a second copy. */
public final class OwnerBackpackEntity extends BlockEntity {
    private ItemStack carrier=ItemStack.EMPTY;
    private OwnerBackpackInventory inventory;
    private java.util.UUID owner;
    final OwnerBackpackAutomation.Items itemPort=new OwnerBackpackAutomation.Items(this);
    final OwnerBackpackAutomation.Fluids fluidPort=new OwnerBackpackAutomation.Fluids(this);
    public void tick(){
        OwnerBackpackMusic.track(this);
        if(!(level instanceof net.minecraft.server.level.ServerLevel server)||server.getServer().getTickCount()%10!=0||!mayAutomate())return;
        var actor=Authority.present(server,owner);if(actor!=null)OwnerBackpackUpgrades.placed(actor,this);
    }
    public void owner(java.util.UUID id){owner=id;setChanged();}
    void refreshInventory(){inventory=null;}
    void restoreCarrier(ItemStack item){carrier=item.copy();inventory=null;}
    public boolean mayAutomate(){
        if(isRemoved()||viewed()||!(level instanceof net.minecraft.server.level.ServerLevel server)||level.getBlockEntity(worldPosition)!=this)return false;
        var actor=Authority.answering(server,owner);return actor!=null&&Authority.mayChange(server,actor,worldPosition);
    }
    private final java.util.Set<java.util.UUID> viewers=new java.util.HashSet<>();
    public OwnerBackpackEntity(BlockPos p,BlockState s){super(OwnerBackpacks.ENTITY,p,s);}
    public ItemStack carrier(){if(carrier.isEmpty())carrier=new ItemStack(getBlockState().getBlock());return carrier;}
    public void setCarrier(ItemStack stack){carrier=stack.copyWithCount(1);inventory=null;setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),net.minecraft.world.level.block.Block.UPDATE_CLIENTS);}
    public OwnerBackpackInventory inventory(){if(inventory==null)inventory=new OwnerBackpackInventory(carrier(),this::setChanged);return inventory;}
    public boolean mayUse(ServerPlayer player){return !isRemoved()&&level!=null&&level.getBlockEntity(worldPosition)==this&&player.isAlive()&&!player.isSpectator()&&player.level()==level&&player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition))<=64&&Authority.mayChange(player.level(),player,worldPosition);}
    public void open(ServerPlayer player){
        if(!mayUse(player))return;
        viewers.removeIf(id->{var p=player.level().getServer().getPlayerList().getPlayer(id);return p==null||!(p.containerMenu instanceof OwnerBackpackMenu menu)||!menu.uses(carrier());});
        if(viewed()){player.sendOverlayMessage(Component.literal("Someone is using this backpack."));return;}
        player.openMenu(new SimpleMenuProvider((id,inv,p)->new OwnerBackpackMenu(id,inv,carrier(),-1,this),carrier().getHoverName()));
    }
    public void start(ServerPlayer p){viewers.add(p.getUUID());}
    public void stop(ServerPlayer p){viewers.remove(p.getUUID());}
    public boolean viewed(){
        if(level instanceof net.minecraft.server.level.ServerLevel server)return server.getServer().getPlayerList().getPlayers().stream().anyMatch(p->p.containerMenu instanceof OwnerBackpackMenu m&&m.uses(carrier())||OwnerBackpackWorkstations.uses(p,carrier())||OwnerBackpackCooking.uses(p,carrier()));
        return !viewers.isEmpty();
    }
    @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);out.store("Carrier",ItemStack.CODEC,carrier());if(owner!=null)out.putString("Owner",owner.toString());}
    @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);carrier=in.read("Carrier",ItemStack.CODEC).orElse(ItemStack.EMPTY);inventory=null;viewers.clear();try{owner=java.util.UUID.fromString(in.getStringOr("Owner",""));}catch(IllegalArgumentException e){owner=null;}}
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider registry){
        // Only appearance is public; never broadcast the stored items or upgrade filters.
        var display=new ItemStack(carrier().getItem());var color=carrier().get(net.minecraft.core.component.DataComponents.DYED_COLOR);
        if(color!=null)display.set(net.minecraft.core.component.DataComponents.DYED_COLOR,color);
        var tag=new net.minecraft.nbt.CompoundTag();tag.store("Carrier",ItemStack.CODEC,registry.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),display);return tag;
    }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
