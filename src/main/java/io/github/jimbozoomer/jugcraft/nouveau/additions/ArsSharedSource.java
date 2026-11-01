package io.github.jimbozoomer.jugcraft.nouveau.additions;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.*;

/** One persistent balance per owner, shared across dimensions; jar copies never copy the balance. */
public final class ArsSharedSource extends SavedData {
    public static final long CAPACITY=1000;
    private final Map<UUID,Long> balances;
    private static final Codec<ArsSharedSource> CODEC=Codec.unboundedMap(UUIDUtil.STRING_CODEC,Codec.LONG.validate(v->v>=0&&v<=CAPACITY?com.mojang.serialization.DataResult.success(v):com.mojang.serialization.DataResult.error(()->"Shared source out of range"))).xmap(ArsSharedSource::new,d->Map.copyOf(d.balances));
    private static final SavedDataType<ArsSharedSource> TYPE=new SavedDataType<>(Jugcraft.id("ars_shared_source"),()->new ArsSharedSource(Map.of()),CODEC,null);
    private ArsSharedSource(Map<UUID,Long> values){balances=new HashMap<>(values);}
    private static ArsSharedSource data(ServerLevel level){return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);}
    public static long held(ServerLevel level,UUID owner){return owner==null?0:data(level).balances.getOrDefault(owner,0L);}
    public static long fill(ServerLevel level,UUID owner,long amount){
        if(owner==null||amount<=0)return 0;var data=data(level);long held=data.balances.getOrDefault(owner,0L),accepted=Math.min(amount,CAPACITY-held);
        if(accepted>0){data.balances.put(owner,held+accepted);data.setDirty();}return accepted;
    }
    public static boolean draw(ServerLevel level,UUID owner,long amount){
        if(owner==null||amount<0)return false;var data=data(level);long held=data.balances.getOrDefault(owner,0L);if(held<amount)return false;
        if(amount>0){data.balances.put(owner,held-amount);data.setDirty();}return true;
    }
}
