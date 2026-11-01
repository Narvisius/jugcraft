package io.github.jimbozoomer.jugcraft.botany;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.world.Container;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecartContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.*;

/** Private Spectator hints: loaded inventories, permission checks and bounded inspection budgets. */
public final class BotanicalInspection {
    private static final Map<ServerPlayer,Scan> SCANS=new WeakHashMap<>();
    private static final class Scan {int cursor;Identifier dimension;Item main,off;BlockPos origin;final Set<BlockPos> blocks=new LinkedHashSet<>();}
    static void register(){
        PayloadTypeRegistry.clientboundPlay().register(Marks.TYPE,Marks.CODEC);
        ServerLifecycleEvents.SERVER_STOPPED.register(s->SCANS.clear());
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->SCANS.remove(h.getPlayer()));
        ServerTickEvents.END_SERVER_TICK.register(s->{for(var p:s.getPlayerList().getPlayers())if(p.tickCount%5==0)tick(p);});
    }
    private static boolean matches(ServerPlayer p,ItemStack stack){return !stack.isEmpty()&&(stack.is(p.getMainHandItem().getItem())||stack.is(p.getOffhandItem().getItem()));}
    private static boolean accessible(ServerPlayer p,BlockPos at){return p.level().hasChunkAt(at)&&Authority.mayChange(p.level(),p,at);}
    private static void tick(ServerPlayer p){
        if(!p.isAlive()||!BotanicalAccessories.has(p,"itemfinder")){if(SCANS.remove(p)!=null)send(p,List.of());return;}
        var scan=SCANS.computeIfAbsent(p,k->new Scan());var origin=p.blockPosition();var dimension=p.level().dimension().identifier();
        if(!dimension.equals(scan.dimension)||scan.origin==null||origin.distSqr(scan.origin)>16||scan.main!=p.getMainHandItem().getItem()||scan.off!=p.getOffhandItem().getItem()){
            scan.dimension=dimension;scan.origin=origin;scan.main=p.getMainHandItem().getItem();scan.off=p.getOffhandItem().getItem();scan.cursor=0;scan.blocks.clear();
        }
        // One of nine loaded chunks per pulse, never forcing a chunk or evaluating unopened loot.
        int index=scan.cursor++%9,cx=(scan.origin.getX()>>4)+index%3-1,cz=(scan.origin.getZ()>>4)+index/3-1;
        scan.blocks.removeIf(at->at.getX()>>4==cx&&at.getZ()>>4==cz||at.distSqr(origin)>432||!accessible(p,at));
        var chunk=p.level().getChunkSource().getChunkNow(cx,cz);int views=0,examined=0;
        if(chunk!=null&&(!p.getMainHandItem().isEmpty()||!p.getOffhandItem().isEmpty()))for(var be:chunk.getBlockEntities().values()){
            if(++examined>1024||views>=4096||scan.blocks.size()>=96)break;var at=be.getBlockPos();if(Math.abs(at.getX()-origin.getX())>12||Math.abs(at.getY()-origin.getY())>12||Math.abs(at.getZ()-origin.getZ())>12||!accessible(p,at))continue;
            if(be instanceof BaseContainerBlockEntity locked&&locked.isLocked()||be instanceof RandomizableContainerBlockEntity loot&&loot.getLootTable()!=null)continue;
            var storage=be instanceof Container inventory?net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage.of(inventory,null):ItemStorage.SIDED.find(p.level(),at,Direction.UP);if(storage==null)continue;int local=0;
            for(var view:storage){if(++views>4096||++local>512)break;if(!view.isResourceBlank()&&view.getAmount()>0&&matches(p,view.getResource().toStack())){scan.blocks.add(at.immutable());break;}}
        }
        var visible=new LinkedHashSet<>(scan.blocks);examined=0;
        for(var entity:p.level().getEntities(p,p.getBoundingBox().inflate(24),e->e instanceof ItemEntity||e instanceof Allay||e instanceof Villager||e instanceof AbstractMinecartContainer)){
            if(++examined>128||visible.size()>=128)break;if(!accessible(p,entity.blockPosition()))continue;boolean match=false;
            if(entity instanceof ItemEntity item)match=p.isShiftKeyDown()||matches(p,item.getItem());
            else if(entity instanceof Allay allay)match=matches(p,allay.getMainHandItem());
            else if(entity instanceof Villager villager){int offers=0;for(var offer:villager.getOffers()){if(++offers>32)break;if(matches(p,offer.getResult())){match=true;break;}}}
            else if(entity instanceof AbstractMinecartContainer cart&&cart.getContainerLootTable()==null){for(int i=0;i<Math.min(128,cart.getContainerSize());i++)if(matches(p,cart.getItem(i))){match=true;break;}}
            if(match)visible.add(entity.blockPosition());
        }
        send(p,List.copyOf(visible));
    }
    private static void send(ServerPlayer p,List<BlockPos> blocks){ServerPlayNetworking.send(p,new Marks(p.level().dimension().identifier(),blocks));}
    public record Marks(Identifier dimension,List<BlockPos> blocks) implements CustomPacketPayload {
        public static final Type<Marks> TYPE=new Type<>(Jugcraft.id("botanical_spectator_marks"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Marks> CODEC=StreamCodec.composite(Identifier.STREAM_CODEC,Marks::dimension,BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(128)),Marks::blocks,Marks::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
}
