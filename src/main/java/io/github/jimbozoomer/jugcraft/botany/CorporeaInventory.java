package io.github.jimbozoomer.jugcraft.botany;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.properties.ChestType;

/** Transactions encompass both source extraction and destination insertion. No extract-then-drop loss. */
final class CorporeaInventory {
    record Entry(ItemVariant variant,long amount){}
    private record Source(Storage<ItemVariant> storage,boolean creative){}
    private static List<Source> sources(ServerPlayer p,CorporeaSpark start){
        var nodes=CorporeaSpark.network(p,start);var seen=new HashSet<BlockPos>();var sources=new ArrayList<Source>();
        for(var node:nodes){var pos=node.anchor();if(!ClaimTransfers.allowed(p.level(),start.anchor(),pos)||!Authority.mayChange(p.level(),p,pos))continue;
            var be=p.level().getBlockEntity(pos);if(be instanceof BotanicalWorkshop||be instanceof BaseContainerBlockEntity locked&&locked.isLocked()||be instanceof RandomizableContainerBlockEntity loot&&loot.getLootTable()!=null)continue;
            var state=p.level().getBlockState(pos);var key=pos;
            if(state.getBlock() instanceof ChestBlock&&state.getValue(ChestBlock.TYPE)!=ChestType.SINGLE){var other=pos.relative(ChestBlock.getConnectedDirection(state));if(!p.level().hasChunkAt(other)||!Authority.mayChange(p.level(),p,other)||!ClaimTransfers.allowed(p.level(),pos,other))continue;var otherBe=p.level().getBlockEntity(other);if(otherBe instanceof BaseContainerBlockEntity otherLocked&&otherLocked.isLocked()||otherBe instanceof RandomizableContainerBlockEntity otherLoot&&otherLoot.getLootTable()!=null)continue;if(other.asLong()<pos.asLong())key=other;}
            if(!seen.add(key))continue;var storage=ItemStorage.SIDED.find(p.level(),pos,Direction.UP);if(storage!=null&&storage.supportsExtraction())sources.add(new Source(storage,node.kind()==2));
        }return sources;
    }
    static List<Entry> catalog(ServerPlayer p,CorporeaSpark start){
        var amounts=new LinkedHashMap<ItemVariant,Long>();int views=0;
        outer:for(var source:sources(p,start))for(var view:source.storage){if(++views>8192)break outer;if(view.isResourceBlank()||view.getAmount()<=0)continue;var item=view.getResource();long amount=source.creative?1000000000L:Math.min(1000000000L,view.getAmount());amounts.merge(item,amount,(a,b)->Math.min(1000000000L,a+b));}
        return amounts.entrySet().stream().map(e->new Entry(e.getKey(),e.getValue())).sorted(Comparator.comparing(e->e.variant.toStack().getHoverName().getString(),String.CASE_INSENSITIVE_ORDER)).toList();
    }
    static long count(ServerPlayer p,CorporeaSpark start,ItemStack sample){if(sample.isEmpty())return 0;var variant=ItemVariant.of(sample);return catalog(p,start).stream().filter(e->e.variant.equals(variant)).mapToLong(Entry::amount).sum();}
    static int transfer(ServerPlayer p,CorporeaSpark start,ItemVariant variant,int requested,Storage<ItemVariant> destination){
        if(destination==null||variant.isBlank()||requested<=0)return 0;requested=Math.min(4096,requested);int delivered=0;
        try(var transaction=Transaction.openOuter()){
            for(var source:sources(p,start)){
                if(delivered>=requested)break;
                try(var part=transaction.openNested()){
                    long available=0;int viewed=0;for(var view:source.storage){if(++viewed>4096)break;if(view.getResource().equals(variant)){available=source.creative?requested:Math.min(requested,available+view.getAmount());if(available>=requested-delivered)break;}}
                    long inserted=destination.insert(variant,Math.min(requested-delivered,available),part);if(inserted<=0)continue;
                    long extracted=source.creative?inserted:source.storage.extract(variant,inserted,part);if(extracted!=inserted)continue;part.commit();delivered+=(int)inserted;
                }
            }transaction.commit();
        }
        return delivered;
    }
}
