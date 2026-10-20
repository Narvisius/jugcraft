package io.github.jimbozoomer.jugcraft.nouveau;

import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.effect.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.*;

/** One consumed dose, with shared bounded work for every splash and lingering application. */
public final class NouveauInfusion {
    private record Cloud(ConcordanceEffects.Context context,UUID owner,Vec3 at,PotionContents potion,double radius,long until){}
    private static final List<Cloud> CLOUDS=new ArrayList<>();
    private NouveauInfusion(){}
    public static void register(){
        ServerLifecycleEvents.SERVER_STOPPED.register(s->CLOUDS.clear());
        ServerTickEvents.END_SERVER_TICK.register(server->{if(server.getTickCount()%20!=0)return;for(var cloud:List.copyOf(CLOUDS)){
            var c=cloud.context;var p=server.getPlayerList().getPlayer(cloud.owner);
            if(!JugNouveau.enabled()||p==null||!p.isAlive()||p.level()!=c.level()||c.level().getGameTime()>cloud.until||!c.level().hasChunkAt(BlockPos.containing(cloud.at))){CLOUDS.remove(cloud);continue;}
            splash(c,p,cloud.at,cloud.potion,cloud.radius,(int)(c.level().getGameTime()/20));
        }});
    }
    public static boolean cast(ConcordanceEffects.Context c,EffectSpec e,ServerPlayer p,Vec3 at,LivingEntity direct){
        boolean lingering=e.augment("duration")>0;double radius=Math.min(6,2+e.augment("radius"));
        if(lingering&&(CLOUDS.size()>=64||CLOUDS.stream().filter(x->x.owner.equals(p.getUUID())).count()>=4))return false;
        PotionContents potion=null;
        if(NouveauDevices.automated(c)){
            var source=NouveauDevices.source(c);if(source==null)return false;
            for(var jar:NouveauAlchemy.jars(c.level(),p,source))if(jar.doses()>0&&jar.potion().hasEffects()){potion=jar.potion();jar.drainPotion(1);break;}
        }else{
            List<ItemStack> supplies=new ArrayList<>();supplies.add(p.getOffhandItem());for(int i=0;i<36;i++)supplies.add(p.getInventory().getItem(i));
            for(var stack:supplies)if(NouveauPotions.ammo(stack)){
                potion=stack.get(DataComponents.POTION_CONTENTS);if(potion==null||!potion.hasEffects()){potion=null;continue;}
                if(stack.getItem() instanceof NouveauPotions.Flask){if(!NouveauPotions.consumeDose(stack)){potion=null;continue;}}
                else{stack.consume(1,p);p.getInventory().placeItemBackInInventory(new ItemStack(Items.GLASS_BOTTLE),net.minecraft.util.Prediction.SERVER_ONLY);}break;
            }
        }
        if(potion==null)return false;
        if(lingering)CLOUDS.add(new Cloud(c,p.getUUID(),at,potion,radius,c.level().getGameTime()+Math.min(600,e.duration())));
        else if(e.augment("radius")>0||direct==null)splash(c,p,at,potion,radius,0);
        else NouveauPotions.apply(p,potion,direct,1);
        c.level().levelEvent(2002,BlockPos.containing(at),potion.getColor());return true;
    }
    private static void splash(ConcordanceEffects.Context c,ServerPlayer p,Vec3 at,PotionContents potion,double radius,int pulse){
        for(var target:c.level().getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(radius),x->x.isAlive()&&x.distanceToSqr(at)<=radius*radius).stream().sorted(Comparator.comparingDouble(x->x.distanceToSqr(at))).limit(16).toList()){
            if(!Authority.mayChange(c.level(),p,target.blockPosition()))continue;
            var hit=c.level().clip(new net.minecraft.world.level.ClipContext(at.add(0,.2,0),target.getEyePosition(),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p));
            if(hit.getType()!=HitResult.Type.MISS||!c.ledger().admit(c.step()+"/infuse/"+pulse,"entity:"+target.getUUID(),2).admitted())continue;
            NouveauPotions.apply(p,potion,target,pulse==0?1:.25F);
        }
    }
}
