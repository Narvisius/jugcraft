package local.peepo;

import com.mojang.serialization.Codec;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.*;

/** Authoritative, dormant entities. Item copies carry claims, never a second copy of a mob. */
public final class TransportLedger extends SavedData {
    public static final Codec<TransportLedger> CODEC=Codec.unboundedMap(Codec.STRING,CompoundTag.CODEC)
        .xmap(TransportLedger::new,d->d.entries);
    private static final SavedDataType<TransportLedger> TYPE=new SavedDataType<>(PeepoMod.id("transport_mobs"),TransportLedger::new,CODEC,null);
    private final Map<String,CompoundTag> entries;
    public TransportLedger(){this(Map.of());}
    private TransportLedger(Map<String,CompoundTag> entries){this.entries=new HashMap<>(entries);}
    public static TransportLedger get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(TYPE);}
    public String add(CompoundTag mob){String id=UUID.randomUUID().toString();entries.put(id,mob.copy());setDirty();return id;}
    public CompoundTag get(String id){var tag=entries.get(id);return tag==null?null:tag.copy();}
    public void remove(String id){if(entries.remove(id)!=null)setDirty();}
}
