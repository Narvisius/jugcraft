package io.github.jimbozoomer.jugcraft.nouveau;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.mixin.AlchemicalItemAccess;
import java.util.*;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/** Explicit hotbar filtering, before pickup; never destroys the sample item used to configure the jar. */
public final class NouveauVoiding {
    public static DataComponentType<Boolean> ACTIVE;
    private NouveauVoiding(){}
    public static void register(){ACTIVE=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("nouveau_void_active"),DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());}
    public static InteractionResult use(ServerPlayer p,ItemStack jar){
        if(p.isShiftKeyDown()){boolean enabled=!jar.getOrDefault(ACTIVE,false);jar.set(ACTIVE,enabled);p.sendOverlayMessage(Component.literal("Void Jar "+(enabled?"enabled":"disabled")));return InteractionResult.SUCCESS;}
        var sample=p.getOffhandItem();if(sample.isEmpty()||sample==jar)return InteractionResult.PASS;
        toggle(jar,sample);p.sendOverlayMessage(Component.literal("Void filter: "+jar.getOrDefault(NouveauUtilities.FILTER,List.of()).size()+" item types"));return InteractionResult.SUCCESS;
    }
    public static void toggle(ItemStack jar,ItemStack sample){var filters=new ArrayList<>(jar.getOrDefault(NouveauUtilities.FILTER,List.of()));String id=BuiltInRegistries.ITEM.getKey(sample.getItem()).toString();if(!filters.remove(id)&&filters.size()<16)filters.add(id);jar.set(NouveauUtilities.FILTER,List.copyOf(filters));}
    public static boolean pickup(ServerPlayer p,ItemEntity entity){
        if(!JugNouveau.enabled()||!p.isAlive()||p.isSpectator()||entity.hasPickUpDelay()||!entity.isAlive()||!Authority.mayChange(p.level(),p,entity.blockPosition()))return false;
        var target=((AlchemicalItemAccess)entity).jugcraft$pickupTarget();if(target!=null&&!target.equals(p.getUUID()))return false;
        var value=entity.getItem();String id=BuiltInRegistries.ITEM.getKey(value.getItem()).toString();
        for(int i=0;i<9;i++){var jar=p.getInventory().getItem(i);if(!jar.is(NouveauCatalog.item("void_jar"))||!jar.getOrDefault(ACTIVE,false)||!jar.getOrDefault(NouveauUtilities.FILTER,List.of()).contains(id))continue;
            int count=value.getCount();p.take(entity,count);entity.discard();p.setAttached(JugcraftConcordance.FOCUS,ConcordanceProgress.focus(p).gain(ConcordanceProgress.now(p.level()),Math.clamp((count+15)/16,1,4)));return true;
        }return false;
    }
}
