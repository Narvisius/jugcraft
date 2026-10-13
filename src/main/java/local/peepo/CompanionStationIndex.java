package local.peepo;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Shared short-lived loaded-chunk index. Values retain positions, never worlds or block entities. */
final class CompanionStationIndex {
    private record Entry(long expires,List<BlockPos> positions) {}
    private static final Map<Level,LinkedHashMap<Long,Entry>> CACHE=new WeakHashMap<>();
    static List<BlockPos> positions(Level level,BlockPos chunkOrigin) {
        if(!level.hasChunkAt(chunkOrigin))return List.of();
        var cache=CACHE.computeIfAbsent(level,l->new LinkedHashMap<>(32,.75F,true));
        long key=((long)(chunkOrigin.getX()>>4)&0xffffffffL) | ((long)(chunkOrigin.getZ()>>4)<<32);
        Entry entry=cache.get(key);
        if(entry==null || entry.expires<level.getGameTime()) {
            List<BlockPos> positions=new ArrayList<>();
            for(var be:level.getChunkAt(chunkOrigin).getBlockEntities().values())
                if((be instanceof CompanionStation || be instanceof LunchBlockEntity) && !be.isRemoved())positions.add(be.getBlockPos().immutable());
            entry=new Entry(level.getGameTime()+80,List.copyOf(positions));cache.put(key,entry);
            if(cache.size()>512)cache.remove(cache.keySet().iterator().next());
        }
        return entry.positions;
    }
}
