package local.peepo;

import com.mojang.serialization.Codec;
import java.util.*;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.*;

/** Deferred cleanup when a carrier is removed while its station is unloaded. Never loads that chunk. */
final class ProductionDeliveryCancellations extends SavedData {
    private static final Codec<ProductionDeliveryCancellations> CODEC=Codec.unboundedMap(Codec.STRING,Codec.STRING.listOf())
        .xmap(ProductionDeliveryCancellations::new,d->d.entries);
    private static final SavedDataType<ProductionDeliveryCancellations> TYPE=new SavedDataType<>(PeepoMod.id("production_delivery_cancellations"),ProductionDeliveryCancellations::new,CODEC,null);
    private final Map<String,List<String>> entries;
    ProductionDeliveryCancellations(){this(Map.of());}
    private ProductionDeliveryCancellations(Map<String,List<String>> entries){this.entries=new HashMap<>(entries);}
    static ProductionDeliveryCancellations get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(TYPE);}
    private static String key(GlobalPos at){return at.dimension().identifier()+"|"+at.pos().asLong();}
    void cancel(GlobalPos at,UUID worker){
        var next=new ArrayList<>(entries.getOrDefault(key(at),List.of()));String id=worker.toString();
        if(!next.contains(id)){next.add(id);entries.put(key(at),List.copyOf(next));setDirty();}
    }
    List<String> take(GlobalPos at){var result=entries.remove(key(at));if(result==null)return List.of();setDirty();return result;}
}
