package io.github.jimbozoomer.jugcraft.botany;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;

/** Loaded tiny planets and equipped pendants attract nearby paid bursts without owning chunk tickets. */
public final class BotanicalPlanets {
    private static final Map<Level,Map<Long,Set<BotanicalWorkshop>>> LIVE=new WeakHashMap<>();
    static void track(BotanicalWorkshop w){if(w.getLevel()==null||!w.kind().equals("tiny_planet_block"))return;synchronized(LIVE){LIVE.computeIfAbsent(w.getLevel(),l->new HashMap<>()).computeIfAbsent(ChunkPos.containing(w.getBlockPos()).pack(),p->Collections.newSetFromMap(new WeakHashMap<>())).add(w);}}
    static void forget(BotanicalWorkshop w){synchronized(LIVE){var map=LIVE.get(w.getLevel());if(map==null)return;long key=ChunkPos.containing(w.getBlockPos()).pack();var set=map.get(key);if(set!=null){set.remove(w);if(set.isEmpty())map.remove(key);}}}
    static void pull(BotanicalBurst burst,ServerPlayer actor){
        if(burst.has("storm"))return;ServerLevel level=actor.level();Vec3 center=null;double nearest=64;
        var chunk=ChunkPos.containing(burst.blockPosition());var candidates=new ArrayList<BotanicalWorkshop>();
        synchronized(LIVE){var map=LIVE.get(level);if(map!=null)for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var set=map.get(ChunkPos.pack(chunk.x()+x,chunk.z()+z));if(set!=null)for(var w:set){if(candidates.size()==128)break;candidates.add(w);}}}
        for(var w:candidates){var at=w.getBlockPos();if(w.isRemoved()||!level.hasChunkAt(at)||level.getBlockEntity(at)!=w||!w.permitted(actor)||!ClaimTransfers.allowed(level,burst.blockPosition(),at))continue;var point=Vec3.atCenterOf(at);double d=point.distanceToSqr(burst.position());if(d<nearest){nearest=d;center=point;}}
        for(var player:level.getEntitiesOfClass(ServerPlayer.class,burst.getBoundingBox().inflate(8),p->p.isAlive()&&!p.isSpectator()))if(BotanicalAccessories.has(player,"tiny_planet")&&Authority.mayChange(level,actor,player.blockPosition())&&ClaimTransfers.allowed(level,burst.blockPosition(),player.blockPosition())){var point=player.getEyePosition();double d=point.distanceToSqr(burst.position());if(d<nearest){nearest=d;center=point;}}
        if(center!=null)burst.orbit(center);
    }
}
