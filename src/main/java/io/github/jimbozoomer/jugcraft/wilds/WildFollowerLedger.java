package io.github.jimbozoomer.jugcraft.wilds;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.*;
/** Counts unloaded companions too; disconnecting or changing dimensions never frees a summon slot. */
final class WildFollowerLedger extends SavedData {
    record Pack(UUID owner,List<UUID> members){static final Codec<Pack> CODEC=RecordCodecBuilder.create(i->i.group(UUIDUtil.CODEC.fieldOf("owner").forGetter(Pack::owner),UUIDUtil.CODEC.listOf(0,10).fieldOf("members").forGetter(Pack::members)).apply(i,Pack::new));}
    private static final Codec<WildFollowerLedger> CODEC=Pack.CODEC.listOf().xmap(WildFollowerLedger::new,d->d.packs.entrySet().stream().map(e->new Pack(e.getKey(),List.copyOf(e.getValue()))).toList());
    private static final SavedDataType<WildFollowerLedger> TYPE=new SavedDataType<>(Jugcraft.id("wilds_followers"),()->new WildFollowerLedger(List.of()),CODEC,null);
    private final Map<UUID,Set<UUID>> packs=new HashMap<>();
    private WildFollowerLedger(List<Pack> saved){for(var p:saved)packs.put(p.owner,new HashSet<>(p.members));}
    static WildFollowerLedger get(ServerLevel l){return l.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);}
    boolean reserve(UUID owner,UUID follower){var pack=packs.computeIfAbsent(owner,id->new HashSet<>());if(pack.size()>=10&&!pack.contains(follower))return false;pack.add(follower);setDirty();return true;}
    void release(UUID owner,UUID follower){var pack=packs.get(owner);if(pack==null)return;if(pack.remove(follower)){if(pack.isEmpty())packs.remove(owner);setDirty();}}
}
