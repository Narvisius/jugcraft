package local.peepo.client;

import java.util.*;
import local.peepo.*;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;

/** Bounded visual-only entities, never added to a level or ticked. Cleared when the client world changes. */
public final class TransportCratePreview {
    private static Object level;
    private static int nextDisplayId=-2_000_000;
    private static final Map<String,Optional<Mob>> CACHE=new LinkedHashMap<>(64,.75F,true);
    public static void clear(){CACHE.clear();level=null;}
    public static Mob mob(CompoundTag entry){
        var world=Minecraft.getInstance().level;if(world==null)return null;
        if(world!=level){CACHE.clear();level=world;}
        String key=entry.getStringOr("Claim","");if(key.isEmpty())return null;
        var cached=CACHE.get(key);if(cached!=null)return cached.orElse(null);
        Mob mob=null;
        try{
            var tag=MobTransportCrate.preview(entry.getCompoundOrEmpty("Preview"));tag.putString("TransportType",entry.getStringOr("Type",""));
            var id=net.minecraft.resources.Identifier.tryParse(tag.getStringOr("TransportType",""));
            var type=id==null?null:net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
            if(type!=null && type.create(world,net.minecraft.world.entity.EntitySpawnReason.LOAD) instanceof Mob visual){
                // Companion inventory updates need a valid ID even during loading, before renderer extraction.
                mob=visual;mob.setId(nextDisplayId--);
                mob.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,world.registryAccess(),tag));
                mob.setNoAi(true);mob.setYRot(0);mob.yBodyRot=mob.yBodyRotO=mob.yHeadRot=mob.yHeadRotO=0;
            }
        }catch(RuntimeException error){mob=null;io.github.jimbozoomer.jugcraft.Jugcraft.LOGGER.warn("Could not create transport crate preview for {}",entry.getStringOr("Type","unknown"),error);}
        if(CACHE.size()>=64)CACHE.remove(CACHE.keySet().iterator().next());CACHE.put(key,Optional.ofNullable(mob));return mob;
    }
}
