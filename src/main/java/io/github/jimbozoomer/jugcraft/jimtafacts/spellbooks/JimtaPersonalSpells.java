package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import java.util.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.Vec3;

/** Native source copies, weapon echoes, growing spikes and life-stealing jaw. */
public final class JimtaPersonalSpells {
    public static final Set<String> NAMES=Set.of("blood_step","frost_step","frostbite","echoing_strikes","devour","ice_spikes");
    private record Airborne(UUID owner,long until){}
    private static final Map<LivingEntity,Airborne> AIRBORNE=new WeakHashMap<>();
    private JimtaPersonalSpells(){}
    public static void register(){
        JimtaApparition.register();
        ServerLivingEntityEvents.AFTER_DAMAGE.register((target,source,base,damage,blocked)->{
            if(blocked||damage<=0||!(source.getEntity() instanceof ServerPlayer p)||!target.isAlive()||!JimtaSpellStatuses.has(p,"echoing_strikes"))return;
            // Source echoes apply only to weapon strikes, never spell damage attributed to a player.
            boolean arrow=source.getDirectEntity() instanceof AbstractArrow;
            if(!arrow&&(!source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)||source.getDirectEntity()!=p))return;
            if(!TwoHanded.allowed(p,p.level(),target)||!ConcordanceEffects.mayHarm(p,target)||JimtaSpellcasting.ally(p,target))return;
            int power=Math.clamp((int)Math.ceil(damage*.75),1,40);boolean made;
            Vec3 around=target.getBoundingBox().getCenter().add(new Vec3(2.5,0,0).yRot(p.getRandom().nextFloat()*(float)Math.PI*2));
            if(arrow){var shot=JimtaShot.launch(p,"echo_arrow",around,target.getBoundingBox().getCenter().subtract(around).normalize().scale(.8),power);made=shot!=null;if(shot!=null)shot.follow(target);}
            else made=JimtaApparition.create(p,"echoing_sword",around,target,power,1,0)!=null;
            if(made){var holder=JimtaSpellStatuses.EFFECTS.get("echoing_strikes");var old=p.getEffect(holder);p.removeEffect(holder);if(old!=null&&old.getAmplifier()>0)p.addEffect(new MobEffectInstance(holder,old.getDuration(),old.getAmplifier()-1));}
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((victim,source)->{
            if(source.getEntity() instanceof ServerPlayer p&&JimtaSpellStatuses.has(p,"frostbitten_strikes")&&victim.isFullyFrozen()&&!JimtaSpellcasting.ally(p,victim)&&TwoHanded.allowed(p,p.level(),victim))JimtaApparition.create(p,"frozen_copy",victim.position(),victim,6,1,0);
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{for(var it=AIRBORNE.entrySet().iterator();it.hasNext();){var entry=it.next();var victim=entry.getKey();var lease=entry.getValue();if(!(victim.level() instanceof net.minecraft.server.level.ServerLevel level)||!victim.isAlive()||level.getGameTime()>lease.until()){it.remove();continue;}if(victim.horizontalCollision&&victim.getDeltaMovement().horizontalDistance()>.1){var owner=Authority.present(level,lease.owner());if(owner!=null)JimtaShot.hit(owner,victim,5,"",victim.getBoundingBox().getCenter());it.remove();}}});
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->AIRBORNE.clear());
    }
    /** Gust's source damage is delayed until a thrown target hits a wall, not a direct blast. */
    public static void airborne(ServerPlayer p,LivingEntity target){AIRBORNE.put(target,new Airborne(p.getUUID(),p.level().getGameTime()+60));}
    public static boolean cast(ServerPlayer p,String name){
        switch(name){
            case "echoing_strikes"->{return p.addEffect(new MobEffectInstance(JimtaSpellStatuses.EFFECTS.get("echoing_strikes"),2400,2));}
            case "frostbite"->{return p.addEffect(new MobEffectInstance(JimtaSpellStatuses.EFFECTS.get("frostbitten_strikes"),600,0));}
            case "devour"->{var target=JimtaSpellcasting.target(p,9);return target!=null&&!JimtaSpellcasting.ally(p,target)&&TwoHanded.allowed(p,p.level(),target)&&ConcordanceEffects.mayHarm(p,target)&&JimtaApparition.create(p,"devour_jaw",target.position(),target,6,1,0)!=null;}
            case "blood_step","frost_step"->{Vec3 before=p.position();boolean moved=false;if(name.equals("blood_step")){var target=JimtaSpellcasting.target(p,8);if(target!=null&&!JimtaSpellcasting.ally(p,target)){Vec3 back=target.getLookAngle().multiply(1,0,1).normalize().scale(-1.6);for(int i=0;i<8&&!moved;i++){var at=target.position().add(back.yRot(i*(float)Math.PI/4));if(p.distanceToSqr(at)<=64)moved=JimtaAttackSpells.safeTeleport(p,at);}}}if(!moved)moved=JimtaSpellcasting.teleport(p,8);if(!moved)return false;if(name.equals("frost_step"))JimtaApparition.create(p,"frozen_copy",before,p,4,1,0);else JimtaWardSpells.trueInvisibility(p,100);return true;}
            case "ice_spikes"->{Vec3 forward=p.getLookAngle().multiply(1,0,1).normalize();if(forward.lengthSqr()<.1)return false;Vec3 start=p.position().add(forward.scale(1.5));var target=JimtaSpellcasting.target(p,10);double distance=target==null?8:Math.max(1,target.position().subtract(start).horizontalDistance());boolean any=false;for(int i=0;i<8;i++){boolean last=i==7||i+1>distance;float size=last?4.5F:1+2*(float)Math.max(i/8.0,(i+1)/distance);Vec3 at=start.add(forward.scale(i));var ground=BlockPos.containing(at).above(2);for(int j=0;j<11;j++,ground=ground.below()){if(!p.level().hasChunkAt(ground))break;if(p.level().getBlockState(ground.below()).isFaceSturdy(p.level(),ground.below(),Direction.UP)&&p.level().getBlockState(ground).getCollisionShape(p.level(),ground).isEmpty()){any|=JimtaApparition.create(p,"ice_spike",Vec3.atBottomCenterOf(ground),null,last?12:6,size,i)!=null;break;}}if(last)break;}return any;}
        }return false;
    }
}
