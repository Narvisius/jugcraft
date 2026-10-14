package local.peepo;

import java.util.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;

/** Tag actual death drops during one synchronous attack; never roll replacement loot or manufacture rewards. */
public final class CompanionSlaughterLoot {
    private record Origin(UUID worker,GlobalPos zone){
        static final Codec<Origin> CODEC=RecordCodecBuilder.create(i->i.group(UUIDUtil.CODEC.fieldOf("Worker").forGetter(Origin::worker),GlobalPos.CODEC.fieldOf("Zone").forGetter(Origin::zone)).apply(i,Origin::new));
    }
    private static final AttachmentType<Origin> ORIGIN=AttachmentRegistry.<Origin>builder().persistent(Origin.CODEC).buildAndRegister(PeepoMod.id("slaughter_produce"));
    private static final AttachmentType<Boolean> OVERFLOW=AttachmentRegistry.<Boolean>builder().persistent(Codec.BOOL).buildAndRegister(PeepoMod.id("slaughter_overflow"));
    public static boolean hasOverflow(PeepoEntity npc){return npc.getAttachedOrElse(OVERFLOW,false);}
    private static final ThreadLocal<Capture> ACTIVE=new ThreadLocal<>();
    public static boolean marked(ItemEntity item){return item.hasAttached(ORIGIN);}
    static boolean belongs(ItemEntity item,PeepoEntity npc,CompanionAssignments.Target target){var origin=item.getAttached(ORIGIN);return origin!=null && origin.worker.equals(npc.getUUID()) && origin.zone.equals(target.at());}
    static void initialize(){ServerEntityEvents.ENTITY_LOAD.register((entity,level)->{
        var capture=ACTIVE.get();if(capture==null || capture.npc.level()!=level || !(entity instanceof ItemEntity item) || item.distanceToSqr(capture.animal)>16)return;
        item.setAttached(ORIGIN,new Origin(capture.npc.getUUID(),capture.target.at()));if(capture.items.size()<64)capture.items.add(item);else capture.npc.setAttached(OVERFLOW,true);
    });}
    static Capture capture(PeepoEntity npc,CompanionAssignments.Target target,Animal animal){return new Capture(npc,target,animal);}
    static final class Capture implements AutoCloseable {
        final PeepoEntity npc;final CompanionAssignments.Target target;final Animal animal;final Capture previous;
        final List<ItemEntity> items=new ArrayList<>();
        Capture(PeepoEntity npc,CompanionAssignments.Target target,Animal animal){this.npc=npc;this.target=target;this.animal=animal;previous=ACTIVE.get();ACTIVE.set(this);}
        public void close(){
            if(previous==null)ACTIVE.remove();else ACTIVE.set(previous);
            for(var item:items){if(!item.isAlive())continue;var stack=item.getItem();
                try(var tx=Transaction.openOuter()){if(npc.garden.storeHarvest(stack,target.at(),tx)){tx.commit();item.discard();}}
                if(item.isAlive())npc.setAttached(OVERFLOW,true);
            }
            // Unusual modded loot that does not fit stays as real, saved, marked ground items for later collection.
        }
    }
}
