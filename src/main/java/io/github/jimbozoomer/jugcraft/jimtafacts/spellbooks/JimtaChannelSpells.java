package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.effect.*;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import java.util.*;
import net.minecraft.core.particles.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.spell_engine.api.spell.event.SpellEvents;

/** Engine-controlled channels stop on release; target loops and Focus costs remain server-authoritative. */
public final class JimtaChannelSpells {
    public record Profile(String shape,double range,int damage,String secondary,int duration){}
    public static final Map<String,Profile> PROFILES=new LinkedHashMap<>();
    private static final Map<ServerPlayer,Vec3> COMET_AREAS=new WeakHashMap<>();
    private JimtaChannelSpells(){}
    public static void register(){
        JimtaSpellbooks.data("channels").entrySet().forEach(e->{var d=e.getValue().getAsJsonObject();PROFILES.put(e.getKey(),new Profile(d.get("shape").getAsString(),d.get("range").getAsDouble(),d.get("damage").getAsInt(),d.get("secondary").getAsString(),d.get("duration").getAsInt()));});
        PROFILES.put("telekinesis",new Profile("telekinesis",20,0,"",0));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->COMET_AREAS.clear());
        SpellEvents.COST_CONSUME.register(args->{if(args.caster() instanceof ServerPlayer p){String name=JimtaSpellcasting.name(args.spell());if(PROFILES.containsKey(name)){JimtaSpellcasting.cooldown(p,name,JimtaSpellcasting.SPELLS.get(name).cooldown());end(p,name);}}});
    }
    public static void end(ServerPlayer p,String name){COMET_AREAS.remove(p);if(name.equals("telekinesis"))JimtaWardSpells.endTelekinesis(p);}
    public static boolean pulse(ServerPlayer player,String name,ItemStack held){
        var profile=PROFILES.get(name);var working=JimtaSpellcasting.SPELLS.get(name);
        if(profile==null||!JimtaSpellbooks.enabled()||!player.isAlive()||player.isSpectator()||!JimtaSpellbooks.spells(held).contains(name))return false;
        int cost=JimtaSpellbooks.focusCost(player,held,working);
        if(ConcordanceProgress.currentFocus(player)<cost)return false;
        ConcordanceProgress.spendFocus(player,cost);
        var level=player.level();Vec3 eye=player.getEyePosition(),look=player.getLookAngle();
        if(name.equals("telekinesis"))return JimtaWardSpells.telekinesisPulse(player);
        if(profile.shape().equals("blaze")){JimtaShot.launch(player,name,eye.add(look.scale(.4)),look.add(player.getRandom().nextGaussian()*.03,player.getRandom().nextGaussian()*.03,player.getRandom().nextGaussian()*.03).normalize().scale(1.85),2);return true;}
        if(profile.shape().equals("comet")){
            Vec3 center=COMET_AREAS.computeIfAbsent(player,p->{var victim=JimtaSpellcasting.target(p,40);return victim==null?level.clip(new ClipContext(eye,eye.add(look.scale(40)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getLocation():victim.position();});
            for(int i=0;i<2;i++){double a=player.getRandom().nextDouble()*Math.PI*2,r=player.getRandom().nextDouble()*6;Vec3 at=center.add(Math.cos(a)*r,0,Math.sin(a)*r),trajectory=new Vec3(.15,-.85,0).normalize();Vec3 high=level.clip(new ClipContext(at,at.subtract(trajectory.scale(12)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player)).getLocation().add(trajectory);JimtaShot.launch(player,name,high,trajectory.scale(1.2),4);}return true;
        }
        var cause=Cause.of(player.getUUID(),Cause.Origin.SPELL,"jugcraft:jimtafacts/"+name,ConcordanceEffects.nextSerial());
        var ledger=new Ledger(new Ledger.Limits(16,64,0));
        List<LivingEntity> targets;
        if(profile.shape().equals("ray")){var aimed=JimtaSpellcasting.target(player,profile.range());targets=aimed==null?List.of():List.of(aimed);}
        else targets=level.getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().inflate(profile.range()),e->e.isAlive()&&player.distanceToSqr(e)<=profile.range()*profile.range()).stream().sorted(Comparator.comparingDouble(player::distanceToSqr)).limit(16).toList();
        boolean allies=profile.shape().equals("allies");
        String school=switch(working.school()){case "ice"->"frost";case "blood"->"soul";case "holy"->"healing";case "fire","lightning"->working.school();default->"arcane";};
        for(var target:targets){
            if(allies!=JimtaSpellcasting.ally(player,target)||!allies&&!TwoHanded.allowed(player,level,target))continue;
            Vec3 center=target.getBoundingBox().getCenter();
            if(profile.shape().equals("cone")&&center.subtract(eye).normalize().dot(look)<.82)continue;
            if(level.clip(new ClipContext(eye,center,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player)).getType()==HitResult.Type.BLOCK)continue;
            var context=new ConcordanceEffects.Context(level,cause,player,ledger,"pulse",eye);
            float before=target.getHealth();
            boolean applied=ConcordanceEffects.apply(context,new EffectSpec(allies?EffectKind.RESTORATION:EffectKind.DAMAGE,allies?Intent.HELPFUL:Intent.HARMFUL,profile.damage(),0,null,Stacking.STRONGEST,allies?null:"spell_power:"+school),target).applied();
            if(!applied)continue;
            if(profile.secondary().equals("siphon")){
                int lost=Math.clamp((int)Math.floor(before-target.getHealth()),0,40);if(lost>0)ConcordanceEffects.apply(new ConcordanceEffects.Context(level,cause,player,ledger,"siphon",eye),EffectSpec.of(EffectKind.RESTORATION,Intent.HELPFUL,lost,0),player);
            }else if(!profile.secondary().isEmpty()&&!allies){
                boolean poison=profile.secondary().equals("poison");
                var effect=new EffectSpec(poison?EffectKind.STATUS:EffectKind.NOUVEAU_CREATURE,Intent.HARMFUL,poison?0:1,profile.duration(),poison?"minecraft:poison":"jugcraft:nouveau/"+profile.secondary(),Stacking.STRONGEST,null);
                ConcordanceEffects.apply(new ConcordanceEffects.Context(level,cause,player,ledger,"secondary",eye),effect,target);
            }
        }
        net.minecraft.core.particles.ParticleOptions particle=switch(name){case "fire_breath"->ParticleTypes.FLAME;case "dragon_breath"->net.minecraft.core.particles.PowerParticleOption.create(ParticleTypes.DRAGON_BREATH,1);case "poison_breath"->ParticleTypes.SPORE_BLOSSOM_AIR;case "cone_of_cold"->ParticleTypes.SNOWFLAKE;case "electrocute"->ParticleTypes.ELECTRIC_SPARK;case "cloud_of_regeneration"->ParticleTypes.HEART;default->ParticleTypes.CRIMSON_SPORE;};
        for(int i=1;i<=8;i++){double distance=profile.range()*i/8;Vec3 at=allies?player.position().add(Math.cos(i*Math.PI/4)*3,1,Math.sin(i*Math.PI/4)*3):eye.add(look.scale(distance));
            if(!allies&&level.clip(new ClipContext(eye,at,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player)).getType()==HitResult.Type.BLOCK)break;
            double spread=profile.shape().equals("cone")?distance*.18:.05;level.sendParticles(particle,at.x,at.y,at.z,3,spread,spread,spread,.015);
        }
        return true;
    }
}
