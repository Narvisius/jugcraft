package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.effect.*;
import java.util.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/** Source three-stack infernal chestplate detonation, bounded by shared authority and trigger depth. */
public final class JimtaImmolate {
    private record Credit(UUID owner,long earliest,int depth){}
    private static final Map<LivingEntity,Credit> CREDIT=new WeakHashMap<>();
    private static final Map<ServerPlayer,Long> COOLDOWN=new WeakHashMap<>();
    public static void after(ConcordanceEffects.Context context,EffectSpec effect,LivingEntity target){
        if(effect.kind()!=EffectKind.DAMAGE||!(context.actor() instanceof ServerPlayer p)||!JimtaSpecialArmor.wearing(p,"infernal_sorcerer_chestplate")||!context.cause().source().startsWith("jugcraft:jimtafacts/"))return;
        var working=JimtaSpellcasting.SPELLS.get(JimtaSpellPower.resolve(context.cause().source().substring("jugcraft:jimtafacts/".length())));
        if(working==null||!working.school().equals("fire")||COOLDOWN.getOrDefault(p,Long.MIN_VALUE)>p.level().getGameTime())return;
        COOLDOWN.put(p,p.level().getGameTime()+20);add(p,target,0);
    }
    private static void add(ServerPlayer p,LivingEntity target,int depth){
        var type=JimtaSpellStatuses.EFFECTS.get("immolate");var previous=target.getEffect(type);
        target.addEffect(new MobEffectInstance(type,300,Math.min(2,previous==null?0:previous.getAmplifier()+1),false,true,true));
        CREDIT.put(target,new Credit(p.getUUID(),p.level().getGameTime()+4,depth));
    }
    public static boolean tick(ServerLevel level,LivingEntity center,int amplifier){
        var credit=CREDIT.get(center);if(!JimtaSpellbooks.enabled()||credit==null)return false;
        if(amplifier<2||level.getGameTime()<credit.earliest())return true;
        CREDIT.remove(center);var p=Authority.present(level,credit.owner());if(p==null||credit.depth()>2)return false;
        for(var victim:level.getEntitiesOfClass(LivingEntity.class,center.getBoundingBox().inflate(6),LivingEntity::isAlive).stream().limit(16).toList()){
            double distance=victim.distanceToSqr(center);if(distance>=36)continue;
            int power=Math.max(1,(int)Math.round(10*(1-distance/36)));
            if(JimtaShot.hit(p,"immolate",victim,power,"ignite",center.getBoundingBox().getCenter())&&victim!=center&&credit.depth()<2)add(p,victim,credit.depth()+1);
        }
        level.sendParticles(ParticleTypes.FLAME,center.getX(),center.getY()+1,center.getZ(),40,1,1,1,.2);
        return false;
    }
}
