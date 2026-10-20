package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.Map;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.component.CustomData;

/** Vanilla critters use the same net without adding duplicate entity implementations. */
public final class MenagerieEcology {
    public static final Map<String,EntityType<?>> CRITTERS=Map.of("bee",EntityTypes.BEE,"silverfish",EntityTypes.SILVERFISH,"endermite",EntityTypes.ENDERMITE);
    private MenagerieEcology(){}
    public static void register(){
        Holder<MobEffect> antivenom=net.minecraft.core.Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,Jugcraft.id("wildlife_antivenom"),new MobEffect(MobEffectCategory.BENEFICIAL,0x76AB67){
            @Override public boolean shouldApplyEffectTickThisTick(int duration,int amplifier){return true;}
            @Override public boolean applyEffectTick(ServerLevel level,LivingEntity entity,int amplifier){entity.removeEffect(MobEffects.POISON);return true;}
        });
        potion("antivenom",antivenom);potion("forest_dasher",MobEffects.SPEED);potion("glowing",MobEffects.GLOWING);
        UseEntityCallback.EVENT.register((who,level,hand,target,hit)->{
            String kind=BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).getPath();
            if(!CRITTERS.containsKey(kind)||CRITTERS.get(kind)!=target.getType()||!who.getItemInHand(hand).is(Menagerie.item("wildlife_capture_net")))return InteractionResult.PASS;
            if(!(who instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            if(!target.isAlive()||target.isPassenger()||target.isVehicle()||!Authority.mayChange(p.level(),p,target.blockPosition()))return InteractionResult.FAIL;
            if(p.getInventory().getFreeSlot()<0)return InteractionResult.FAIL;
            var stack=new ItemStack(Menagerie.item("wildlife_"+kind));var tag=new CompoundTag();tag.putString("VanillaCritter",kind);
            if(target instanceof AgeableMob a)tag.putInt("Age",a.getAge());
            if(target instanceof LivingEntity living)tag.putFloat("Health",living.getHealth());
            stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));if(target.hasCustomName())stack.set(DataComponents.CUSTOM_NAME,target.getCustomName());
            if(!p.getInventory().add(stack))return InteractionResult.FAIL;target.discard();return InteractionResult.SUCCESS;
        });
    }
    private static void potion(String name,Holder<MobEffect> effect){
        Registry.registerForHolder(BuiltInRegistries.POTION,Jugcraft.id("wildlife_"+name),new Potion("wildlife_"+name,new MobEffectInstance(effect,3600)));
    }
}

