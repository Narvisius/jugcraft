package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import java.util.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.*;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.*;

/** Source defensive/travel spells on Jugcraft's shared Focus, claims and party authority. */
public final class JimtaWardSpells {
    public static final Set<String> NAMES=Set.of("abyssal_shroud","arcane_shackle","heartstop","planar_sight","pocket_dimension","telekinesis","evasion","portal","recall","invisibility","scapegoat","shield");
    public static final Map<String,Holder<MobEffect>> EFFECTS=new LinkedHashMap<>();
    public static AttachmentType<Float> HEART_DEBT;
    public static AttachmentType<Boolean> CONCEALED;
    private static final ResourceKey<DamageType> DEBT=ResourceKey.create(Registries.DAMAGE_TYPE,Jugcraft.id("jimtafacts_heartstop"));
    private static final Map<ServerPlayer,Grip> GRIPS=new WeakHashMap<>();
    private record Grip(UUID target,double distance,long lastPulse){}
    private JimtaWardSpells(){}
    public static void register(){
        for(String name:List.of("abyssal_shroud","heartstop","planar_sight","evasion","true_invisibility"))
            EFFECTS.put(name,Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,Jugcraft.id("jimtafacts_"+name),new MobEffect(MobEffectCategory.BENEFICIAL,name.equals("heartstop")?0xac244e:0x9674c9){}));
        HEART_DEBT=AttachmentRegistry.<Float>builder().persistent(Codec.floatRange(0,1_000_000)).buildAndRegister(Jugcraft.id("jimtafacts_heart_debt"));
        CONCEALED=AttachmentRegistry.<Boolean>builder().syncWith(net.minecraft.network.codec.ByteBufCodecs.BOOL,AttachmentSyncPredicate.all()).buildAndRegister(Jugcraft.id("jimtafacts_concealed"));
        JimtaWardEntity.register();JimtaPortalFrames.register();JimtaScapegoat.register();JimtaPocketRooms.register();
        ServerLifecycleEvents.SERVER_STOPPED.register(server->GRIPS.clear());
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(JimtaWardSpells::allowDamage);
        ServerLivingEntityEvents.AFTER_DAMAGE.register((target,source,base,damage,blocked)->{if(damage>0&&source.getEntity() instanceof LivingEntity attacker)attacker.removeEffect(EFFECTS.get("true_invisibility"));});
        ServerTickEvents.END_SERVER_TICK.register(server->{for(var p:server.getPlayerList().getPlayers()){
            settleDebt(p);var grip=GRIPS.get(p);if(grip!=null&&(p.level().getGameTime()-grip.lastPulse()>4||!p.isAlive()))endTelekinesis(p);
            if(has(p,"true_invisibility")&&p.tickCount%10==0)clearTargets(p);
        }});
    }
    public static boolean has(LivingEntity e,String name){var effect=EFFECTS.get(name);return effect!=null&&JimtaSpellbooks.enabled()&&e.hasEffect(effect);}
    public static boolean trueInvisibility(LivingEntity e,int ticks){boolean result=e.addEffect(new MobEffectInstance(EFFECTS.get("true_invisibility"),Math.clamp(ticks,1,1200),0,false,false,true));if(e.level() instanceof ServerLevel){refreshConcealment(e);clearTargets(e);}return result;}
    public static boolean concealed(LivingEntity e){return CONCEALED!=null&&e.getAttachedOrElse(CONCEALED,false);}
    public static void refreshConcealment(LivingEntity e){if(CONCEALED==null||!(e.level() instanceof ServerLevel))return;boolean actual=has(e,"true_invisibility");if(actual!=concealed(e))e.setAttached(CONCEALED,actual);}
    private static void clearTargets(LivingEntity e){if(!(e.level() instanceof ServerLevel l))return;for(var mob:l.getEntitiesOfClass(Mob.class,e.getBoundingBox().inflate(40),m->m.getTarget()==e).stream().limit(128).toList()){mob.setTarget(null);mob.getNavigation().stop();mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);}}
    public static boolean cast(ServerPlayer p,String name){
        return switch(name){
            case "abyssal_shroud"->effect(p,"abyssal_shroud",120,0);
            case "heartstop"->!has(p,"heartstop")&&p.getAttachedOrElse(HEART_DEBT,0F)<=0&&effect(p,"heartstop",200,0);
            case "planar_sight"->effect(p,"planar_sight",800,0);
            case "evasion"->effect(p,"evasion",1200,0);
            case "invisibility"->trueInvisibility(p,200);
            case "recall"->recall(p);
            case "pocket_dimension"->JimtaPocketRooms.enter(p);
            case "portal"->JimtaPortalFrames.handles(p)?JimtaPortalFrames.cast(p):JimtaWardEntity.cast(p,name);
            case "shield","arcane_shackle"->JimtaWardEntity.cast(p,name);
            case "scapegoat"->JimtaScapegoat.cast(p);
            case "telekinesis"->telekinesisPulse(p);
            default->false;
        };
    }
    private static boolean effect(ServerPlayer p,String name,int ticks,int amp){return p.addEffect(new MobEffectInstance(EFFECTS.get(name),ticks,amp,false,false,true));}
    /** Called after armor/magic reduction, before health is lost. No doubling of reductions at repayment. */
    public static float defer(LivingEntity entity,DamageSource source,float amount){
        if(!(entity instanceof ServerPlayer p)||!has(p,"heartstop")||amount<=0||source.is(DEBT)||source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))return amount;
        float old=p.getAttachedOrElse(HEART_DEBT,0F),saved=Math.min(amount,1_000_000-old);p.setAttached(HEART_DEBT,old+saved);return amount-saved;
    }
    public static void settleDebt(ServerPlayer p){
        float debt=p.getAttachedOrElse(HEART_DEBT,0F);if(debt<=0||has(p,"heartstop"))return;
        if(!p.isAlive()){p.removeAttached(HEART_DEBT);return;}
        // Clear before hurt: totems/death callbacks cannot recursively replay the same debt.
        p.removeAttached(HEART_DEBT);p.damageCooldownTime=0;
        p.hurtServer(p.level(),new DamageSource(p.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DEBT)),debt);
    }
    private static boolean allowDamage(LivingEntity target,DamageSource source,float amount){
        if(!source.is(DEBT)&&!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)&&JimtaWardEntity.blocks(target,source,amount))return false;
        if(source.is(DEBT)||source.is(DamageTypeTags.IS_FALL)||source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)||!(target instanceof ServerPlayer p))return true;
        if(!has(p,"abyssal_shroud")&&!has(p,"evasion"))return true;
        boolean abyss=has(p,"abyssal_shroud");
        if(dodge(p,abyss?1.5:6,abyss?8:24)){
            if(!abyss){var old=p.getEffect(EFFECTS.get("evasion"));p.removeEffect(EFFECTS.get("evasion"));if(old!=null&&old.getAmplifier()>0)p.addEffect(new MobEffectInstance(EFFECTS.get("evasion"),old.getDuration(),old.getAmplifier()-1));}
            return false;
        }return true;
    }
    private static boolean dodge(ServerPlayer p,double radius,int attempts){
        for(int i=0;i<attempts;i++){
            double angle=Math.toRadians(p.getYRot()+180)+(p.getRandom().nextDouble()-.5)*Math.min(Math.PI*2,Math.PI/4*(i+1));
            double r=Math.min(12,radius+i*.25);var option=p.position().add(-Math.sin(angle)*r,2,Math.cos(angle)*r);var pos=BlockPos.containing(option);
            for(int d=0;d<5&&p.level().hasChunkAt(pos)&&p.level().getBlockState(pos.below()).isAir();d++)pos=pos.below();
            Vec3 at=Vec3.atBottomCenterOf(pos);if(!safe(p, p.level(),at)||p.level().getBlockState(pos.below()).isAir())continue;
            p.stopRiding();p.level().sendParticles(ParticleTypes.PORTAL,p.getX(),p.getY()+1,p.getZ(),20,.3,.6,.3,.1);p.teleportTo(at.x,at.y,at.z);p.resetFallDistance();JimtaSpellbookCurios.afterTeleport(p);return true;
        }return false;
    }
    public static boolean permitted(ServerPlayer p,LivingEntity e){return e.isAlive()&&!JimtaSpellcasting.ally(p,e)&&Authority.mayChange(p.level(),p,e.blockPosition())&&TwoHanded.allowed(p,p.level(),e)&&ConcordanceEffects.mayHarm(p,e);}
    public static boolean safe(ServerPlayer p,ServerLevel level,Vec3 at){
        var box=p.getBoundingBox().move(at.subtract(p.position()));
        for(var corner:List.of(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ)))if(!level.hasChunkAt(corner)||!level.getWorldBorder().isWithinBounds(corner)||!Authority.mayChange(level,p,corner))return false;
        return level.noCollision(p,box)&&!level.containsAnyLiquid(box);
    }
    public static boolean outOfCombat(ServerPlayer p){return p.getLastDamageSource(200)==null;}
    private static boolean recall(ServerPlayer p){
        if(!outOfCombat(p)||p.isPassenger()||p.level().dimension().equals(JimtaPocketRooms.DIMENSION))return false;
        var transition=p.findRespawnPositionAndUseSpawnBlock(true,TeleportTransition.DO_NOTHING);
        if(transition==null)return false;
        // This is explicit player travel, so loading the saved bed/spawn destination is intentional.
        transition.newLevel().getChunk(BlockPos.containing(transition.position()));
        if(!safe(p,transition.newLevel(),transition.position()))return false;
        if(p.teleport(transition)==null)return false;JimtaSpellbookCurios.afterTeleport(p);return true;
    }
    public static boolean telekinesisPulse(ServerPlayer p){
        var grip=GRIPS.get(p);LivingEntity victim=grip==null?JimtaSpellcasting.target(p,14):p.level().getEntity(grip.target()) instanceof LivingEntity e?e:null;
        if(victim==null||!permitted(p,victim)||p.distanceToSqr(victim)>24*24||!p.hasLineOfSight(victim)){endTelekinesis(p);return false;}
        double actual=p.distanceTo(victim),locked=grip==null?actual:grip.distance();locked+=(actual-locked)*(actual>locked?.05:.02);locked=Math.clamp(locked,1,14);
        Vec3 force=p.position().add(p.getLookAngle().scale(locked)).subtract(victim.position()).scale(.6*(1-Math.clamp(victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE),0,.9)));
        if(force.length()>1.5)force=force.normalize().scale(1.5);
        Vec3 movement=victim.getDeltaMovement().lerp(force,.25),dest=victim.position().add(movement);
        if(!p.level().hasChunkAt(BlockPos.containing(dest))||!Authority.mayChange(p.level(),p,BlockPos.containing(dest))){endTelekinesis(p);return false;}
        victim.setDeltaMovement(movement);victim.syncVelocity=true;if(movement.y>=0)victim.resetFallDistance();
        GRIPS.put(p,new Grip(victim.getUUID(),locked,p.level().getGameTime()));return true;
    }
    public static void endTelekinesis(ServerPlayer p){GRIPS.remove(p);}
}
