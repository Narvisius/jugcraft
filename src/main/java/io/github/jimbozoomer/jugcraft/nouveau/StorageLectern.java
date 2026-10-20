package io.github.jimbozoomer.jugcraft.nouveau;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.*;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import org.jspecify.annotations.Nullable;

/** Links never contain copied item stacks. All access resolves the real, currently loaded chest again. */
public final class StorageLectern extends BlockEntity implements GeoBlockEntity {
    public static final int MAX_WYRMS=4, LINKS_PER_WYRM=8, MAX_LINKS=32, RANGE=30;
    private @Nullable UUID owner;
    private @Nullable BlockPos proxy;
    private final List<BlockPos> links=new ArrayList<>();
    private final List<UUID> wyrms=new ArrayList<>();
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public StorageLectern(BlockPos pos,BlockState state){super(JugNouveau.LECTERN_ENTITY,pos,state);}
    public void placedBy(ServerPlayer player){owner=player.getUUID();setChanged();}
    public @Nullable UUID owner(){return owner;}
    public List<BlockPos> links(){return List.copyOf(links);}
    public List<UUID> wyrms(){return List.copyOf(wyrms);}
    public int capacity(){return wyrms.size()*LINKS_PER_WYRM;}
    public boolean mayUse(ServerPlayer player){
        if(owner==null&&JugNouveau.enabled()&&player.isAlive()&&!player.isSpectator()&&level==player.level()&&player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition))<=64&&Authority.mayChange(player.level(),player,worldPosition))placedBy(player);
        return JugNouveau.enabled()&&owner!=null&&(owner.equals(player.getUUID())||JugcraftParties.sameParty(owner,player.getUUID()))
            &&player.isAlive()&&!player.isSpectator()&&level==player.level()&&level instanceof ServerLevel server
            &&Authority.mayChange(server,player,worldPosition);
    }
    /** A chain is resolved afresh, without loading chunks or retaining remote inventory references. */
    public @Nullable StorageLectern root(ServerPlayer player){
        StorageLectern current=this;Set<BlockPos> seen=new HashSet<>();
        for(int hop=0;hop<16;hop++){
            if(!current.mayUse(player)||!seen.add(current.worldPosition))return null;
            if(current.proxy==null)return current;
            if(current.proxy.distSqr(current.worldPosition)>RANGE*RANGE||!player.level().hasChunkAt(current.proxy)||!(player.level().getBlockEntity(current.proxy) instanceof StorageLectern next))return null;
            current=next;
        }return null;
    }
    public boolean proxyTo(ServerPlayer player,StorageLectern target){
        if(target==this||!mayUse(player)||!target.mayUse(player)||!wyrms.isEmpty()||!links.isEmpty()||target.worldPosition.distSqr(worldPosition)>RANGE*RANGE)return false;
        BlockPos previous=proxy;proxy=target.worldPosition.immutable();if(root(player)==null){proxy=previous;return false;}setChanged();return true;
    }
    public static void tick(net.minecraft.world.level.Level world,BlockPos pos,BlockState state,StorageLectern lectern){
        if(!(world instanceof ServerLevel level)||Math.floorMod(level.getGameTime()+pos.asLong(),20)!=0||level.hasNeighborSignal(pos)||!JugNouveau.enabled())return;
        var player=Authority.answering(level,lectern.owner);if(player==null)return;var root=lectern.root(player);if(root==null)return;
        var destinations=new ArrayList<Container>();Set<Container> connected=Collections.newSetFromMap(new IdentityHashMap<>());
        for(int i=0;i<Math.min(root.links.size(),root.capacity());i++){var c=root.linkedContainer(player,i);if(c!=null&&connected.add(c)){destinations.add(c);if(c instanceof NouveauWorkshop station)connected.addAll(station.connectedRepositories());}}
        for(var direction:Direction.values()){
            var source=NouveauStorage.accessible(level,player,pos.relative(direction));if(source==null||connected.contains(source))continue;
            for(int slot=0;slot<Math.min(54,source.getContainerSize());slot++){var stack=source.getItem(slot);if(stack.isEmpty())continue;var transfer=stack.copyWithCount(Math.min(16,stack.getCount()));
                for(var destination:destinations)if(NouveauCrafting.deposit(destination,transfer,true)){
                    var actual=source.removeItem(slot,transfer.getCount());source.setChanged();
                    if(!NouveauCrafting.deposit(destination,actual,false)){player.getInventory().placeItemBackInInventory(actual,net.minecraft.util.Prediction.SERVER_ONLY);}return;
                }
            }
        }
    }
    public boolean bind(ServerPlayer player){
        if(proxy!=null||!mayUse(player)||wyrms.size()>=MAX_WYRMS||!(level instanceof ServerLevel server))return false;
        BookWyrm wyrm=JugNouveau.BOOK_WYRM.create(server,EntitySpawnReason.MOB_SUMMONED);
        if(wyrm==null)return false;
        wyrm.bind(worldPosition,owner,wyrms.size()%JugNouveau.COLORS.length);
        wyrm.snapTo(worldPosition.getX()+.5,worldPosition.getY()+1.6,worldPosition.getZ()+.5,0,0);
        if(!server.addFreshEntity(wyrm))return false;
        wyrms.add(wyrm.getUUID());setChanged();return true;
    }
    public void detach(UUID id){if(wyrms.remove(id))setChanged();}
    public boolean link(ServerPlayer player,BlockPos pos){
        if(proxy!=null||!mayUse(player)||pos.distSqr(worldPosition)>RANGE*RANGE||links.contains(pos)||links.size()>=capacity()
            ||accessible(player,pos)==null)return false;
        links.add(pos.immutable());setChanged();return true;
    }
    public boolean unlink(BlockPos pos){boolean changed=links.remove(pos);if(pos.equals(proxy)){proxy=null;changed=true;}if(changed)setChanged();return changed;}
    /** Only ordinary vanilla storage is accepted: no furnace output slots, private mod inventories or Ender chests. */
    public @Nullable Container accessible(ServerPlayer player,BlockPos pos){
        if(!(level instanceof ServerLevel server)||pos.distSqr(worldPosition)>RANGE*RANGE)return null;
        return NouveauStorage.accessible(server,player,pos);
    }
    public @Nullable Container linkedContainer(ServerPlayer player,int index){
        if(!mayUse(player)||index<0||index>=links.size()||index>=capacity())return null;
        return accessible(player,links.get(index));
    }
    @Override public void preRemoveSideEffects(BlockPos pos,BlockState state){
        if(level instanceof ServerLevel server){
            int count=wyrms.size();var old=List.copyOf(wyrms);wyrms.clear();links.clear();
            for(UUID id:old)if(server.getEntity(id) instanceof BookWyrm wyrm)wyrm.discard();
            if(count>0)Block.popResource(server,pos,new ItemStack(JugNouveau.CHARM,count));
            setChanged();
        }
    }
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);out.putInt("nouveau_schema",2);if(proxy!=null)out.store("proxy",BlockPos.CODEC,proxy);
        if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);
        out.store("links",BlockPos.CODEC.listOf(),links);out.store("wyrms",UUIDUtil.CODEC.listOf(),wyrms);
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);proxy=in.read("proxy",BlockPos.CODEC).orElse(null);owner=in.read("owner",UUIDUtil.CODEC).orElse(null);links.clear();wyrms.clear();
        in.read("links",BlockPos.CODEC.listOf()).orElse(List.of()).stream().distinct().filter(p->p.distSqr(worldPosition)<=RANGE*RANGE)
            .limit(MAX_LINKS).forEach(p->links.add(p.immutable()));
        in.read("wyrms",UUIDUtil.CODEC.listOf()).orElse(List.of()).stream().distinct().limit(MAX_WYRMS).forEach(wyrms::add);
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar out){
        out.add(new AnimationController<StorageLectern>("ledger",4,s->s.setAndContinue(RawAnimation.begin().thenLoop("ledger_float"))));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
