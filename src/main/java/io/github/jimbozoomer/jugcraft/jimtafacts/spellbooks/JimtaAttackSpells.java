package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.effect.*;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauSummons;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.*;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;

/** Distinct source volleys, paid-once recasts, lobs, displacement and sacrifice. */
public final class JimtaAttackSpells {
    public static final Set<String> NAMES=Set.of("soulfire_ray","eldritch_blast","flaming_barrage","gust","throw","sacrifice","snowball","magma_bomb","lob_creeper","chain_creeper","firecracker","arrow_volley","raise_hell","thunder_step");
    public static final Set<String> RECASTS=Set.of("eldritch_blast","flaming_barrage","raise_hell","thunder_step");
    private record Volley(ServerPlayer owner,Vec3 target,Vec3 start,long startTick,net.minecraft.server.level.ServerLevel level){}
    private record Eruption(ServerPlayer owner,Vec3 center,long startTick,int damage,Set<UUID> struck,net.minecraft.server.level.ServerLevel level){}
    private static final List<Volley> VOLLEYS=new ArrayList<>();
    private static final List<Eruption> ERUPTIONS=new ArrayList<>();
    private static final Map<ServerPlayer,UUID> ORBS=new WeakHashMap<>();
    private JimtaAttackSpells(){}
    public static void register(){JimtaShot.register();JimtaRecasts.register();ServerLifecycleEvents.SERVER_STOPPED.register(s->{VOLLEYS.clear();ERUPTIONS.clear();ORBS.clear();});ServerTickEvents.END_SERVER_TICK.register(server->{
        for(var it=VOLLEYS.iterator();it.hasNext();){var v=it.next();var p=v.owner();long age=p.level().getGameTime()-v.startTick();if(!valid(p)||p.level()!=v.level()||age>=20){it.remove();continue;}if(age%4==0)for(int i=0;i<5;i++){Vec3 side=v.target().subtract(v.start()).cross(new Vec3(0,1,0)).normalize().scale((i-2)*.7);Vec3 at=v.start().add(side);JimtaShot.launch(p,"arrow_volley",at,v.target().add(side).subtract(at).normalize().scale(1.8),2);}}
        for(var it=ERUPTIONS.iterator();it.hasNext();){var eruption=it.next();var p=eruption.owner();long age=p.level().getGameTime()-eruption.startTick();if(!valid(p)||p.level()!=eruption.level()||age>30){it.remove();continue;}double radius=Math.min(8,age*.4);if(age%2==0){for(int i=0;i<20;i++){double a=i*Math.PI/10;p.level().sendParticles(ParticleTypes.FLAME,eruption.center().x+Math.cos(a)*radius,eruption.center().y+.4,eruption.center().z+Math.sin(a)*radius,2,.1,.3,.1,.03);}for(var target:p.level().getEntitiesOfClass(LivingEntity.class,new AABB(eruption.center(),eruption.center()).inflate(radius,2,radius),e->e.isAlive()&&e.onGround()).stream().limit(16).toList())if(!eruption.struck().contains(target.getUUID())&&target.position().subtract(eruption.center()).horizontalDistanceSqr()<=radius*radius&&JimtaShot.hit(p,"flaming_strike",target,eruption.damage(),"ignite",eruption.center().add(0,1,0)))eruption.struck().add(target.getUUID());}}
    });}
    private static boolean valid(ServerPlayer p){return p.isAlive()&&!p.isRemoved()&&!p.isSpectator()&&JimtaSpellbooks.enabled();}
    private static Vec3 aim(ServerPlayer p,int range){var target=JimtaSpellcasting.target(p,range);return target!=null?target.getBoundingBox().getCenter():p.level().clip(new ClipContext(p.getEyePosition(),p.getEyePosition().add(p.getLookAngle().scale(range)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getLocation();}
    public static int weapon(ServerPlayer p){return (int)Math.clamp(p.getOffhandItem().getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY).compute(Attributes.ATTACK_DAMAGE,1,EquipmentSlot.MAINHAND),0,20);}
    private static void recast(ServerPlayer p,String name,int count,int duration){if(JimtaRecasts.isPending(p,name))JimtaRecasts.consume(p,name);else JimtaRecasts.begin(p,name,count,duration);}
    public static boolean cast(ServerPlayer p,String name){
        Vec3 eye=p.getEyePosition(),look=p.getLookAngle(),origin=eye.add(look.scale(.7));
        switch(name){
            case "soulfire_ray","eldritch_blast"->{Vec3 end=aim(p,30);var victim=JimtaSpellcasting.target(p,30);if(victim!=null)JimtaShot.hit(p,name,victim,name.equals("soulfire_ray")?12:15,"",eye);for(int i=0;i<32;i++){Vec3 at=eye.lerp(end,i/31.0);p.level().sendParticles(name.equals("soulfire_ray")?ParticleTypes.SOUL_FIRE_FLAME:ParticleTypes.SCULK_SOUL,at.x,at.y,at.z,1,.025,.025,.025,0);}if(name.equals("eldritch_blast"))recast(p,name,3,80);return true;}
            case "flaming_barrage"->{Vec3 velocity=look.add(p.getRandom().nextGaussian()*.15,.2+p.getRandom().nextGaussian()*.15,p.getRandom().nextGaussian()*.15).normalize().scale(.8);if(JimtaShot.launch(p,name,origin,velocity,3)==null)return false;recast(p,name,5,120);return true;}
            case "snowball","magma_bomb","lob_creeper","throw"->{if(name.equals("throw")&&p.getOffhandItem().isEmpty())return false;int damage=name.equals("throw")?1+weapon(p):name.equals("magma_bomb")?8:name.equals("lob_creeper")?6:0;double speed=name.equals("magma_bomb")?.65:name.equals("lob_creeper")?.88:name.equals("throw")?1.5:1;return JimtaShot.launch(p,name,origin,look.scale(speed).add(0,name.equals("snowball")||name.equals("magma_bomb")?.2:0,0),damage)!=null;}
            case "chain_creeper"->{Vec3 at=aim(p,32);boolean any=false;for(int i=0;i<3;i++)any|=JimtaShot.launch(p,name,at,new Vec3(Math.cos(i*Math.PI*2/3)*.3,.4,Math.sin(i*Math.PI*2/3)*.3),5)!=null;return any;}
            case "sacrifice"->{var target=JimtaSpellcasting.target(p,25);if(target==null||!NouveauSummons.ownedBy(target,p.getUUID())||!Authority.mayChange(p.level(),p,target.blockPosition()))return false;double radius=3*(1+.5*target.getHealth()/target.getMaxHealth());int power=12+(int)(target.getHealth()*.5);Vec3 at=target.getBoundingBox().getCenter();target.discard();for(var e:p.level().getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(radius),LivingEntity::isAlive).stream().limit(16).toList()){double d=e.distanceToSqr(at)/(radius*radius);if(d<1)JimtaShot.hit(p,name,e,Math.max(1,(int)(power*(1-d*d*d))),"",at);}p.level().sendParticles(ParticleTypes.CRIMSON_SPORE,at.x,at.y,at.z,60,.7,.6,.7,.25);return true;}
            case "gust"->{boolean changed=false;for(var target:p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(8),LivingEntity::isAlive).stream().limit(16).toList()){Vec3 delta=target.getBoundingBox().getCenter().subtract(eye);if(delta.lengthSqr()>64||delta.normalize().dot(look)<.8)continue;if(!JimtaSpellcasting.ally(p,target)&&io.github.jimbozoomer.jugcraft.weapons.TwoHanded.allowed(p,p.level(),target)&&ConcordanceEffects.mayHarm(p,target)&&p.level().clip(new ClipContext(eye,target.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getType()!=HitResult.Type.BLOCK){JimtaPersonalSpells.airborne(p,target);target.setDeltaMovement(target.getDeltaMovement().add(look.scale(2*(1-delta.length()/10))).add(0,.2,0));target.syncVelocity=true;if(target instanceof ServerPlayer other)other.connection.send(new ClientboundSetEntityMotionPacket(other));changed=true;}}var wall=p.level().clip(new ClipContext(eye,eye.add(look.scale(3.5)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));double kick=Math.clamp(1/(wall.getLocation().distanceToSqr(eye)+1)-.11,0,.95);if(kick>0){p.setDeltaMovement(p.getDeltaMovement().subtract(look.scale(kick*.25)));p.connection.send(new ClientboundSetEntityMotionPacket(p));changed=true;}for(int i=0;i<12;i++){var at=eye.add(look.scale(i*.65));p.level().sendParticles(ParticleTypes.CLOUD,at.x,at.y,at.z,2,.25,.25,.25,.07);}return changed;}
            case "firecracker"->{Vec3 at=aim(p,23);if(!p.level().hasChunkAt(BlockPos.containing(at))||!Authority.mayChange(p.level(),p,BlockPos.containing(at)))return false;for(var target:p.level().getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(3),LivingEntity::isAlive).stream().limit(16).toList())if(target.distanceToSqr(at)<9)JimtaShot.hit(p,name,target,4,"",at);p.level().sendParticles(ParticleTypes.FIREWORK,at.x,at.y,at.z,45,.3,.3,.3,.3);return true;}
            case "arrow_volley"->{if(VOLLEYS.size()>=16)return false;Vec3 at=aim(p,100);var backwards=at.subtract(p.position()).multiply(1,0,1).normalize().scale(-4);Vec3 high=at.add(backwards).add(0,6,0);var blocked=p.level().clip(new ClipContext(at,high,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));Vec3 start=at.lerp(blocked.getLocation(),.9);if(!p.level().hasChunkAt(BlockPos.containing(start)))return false;VOLLEYS.add(new Volley(p,at,start,p.level().getGameTime(),p.level()));return true;}
            case "raise_hell"->{if(ERUPTIONS.size()>=16)return false;Vec3 at=p.position().add(look.multiply(1,0,1).scale(1.7));if(!p.level().hasChunkAt(BlockPos.containing(at))||!Authority.mayChange(p.level(),p,BlockPos.containing(at)))return false;ERUPTIONS.add(new Eruption(p,at,p.level().getGameTime(),15+weapon(p),new HashSet<>(),p.level()));recast(p,name,3,80);return true;}
            case "thunder_step"->{
                if(JimtaRecasts.isPending(p,name)){UUID id=ORBS.get(p);var orb=id==null?null:p.level().getEntity(id);if(!(orb instanceof JimtaShot)||orb.distanceToSqr(p)>32*32)return false;Vec3 at=orb.position();if(!safeTeleport(p,at))return false;for(var target:p.level().getEntitiesOfClass(LivingEntity.class,new AABB(eye,at).inflate(1),LivingEntity::isAlive).stream().limit(16).toList())if(target.getBoundingBox().inflate(1).clip(eye,at).isPresent())JimtaShot.hit(p,name,target,10,"",eye);orb.discard();ORBS.remove(p);JimtaRecasts.consume(p,name);return true;}
                var orb=JimtaShot.launch(p,name,eye,look.scale(.45),0);if(orb==null)return false;ORBS.put(p,orb.getUUID());JimtaRecasts.begin(p,name,2,100);return true;
            }
        }return false;
    }
    public static boolean safeTeleport(ServerPlayer p,Vec3 at){if(p.isPassenger()||!Authority.mayChange(p.level(),p,p.blockPosition())||!p.level().hasChunkAt(BlockPos.containing(at))||!p.level().getWorldBorder().isWithinBounds(BlockPos.containing(at))||!Authority.mayChange(p.level(),p,BlockPos.containing(at))||!p.level().noCollision(p,p.getBoundingBox().move(at.subtract(p.position()))))return false;p.teleportTo(p.level(),at.x,at.y,at.z,Set.of(),p.getYRot(),p.getXRot(),true);p.resetFallDistance();JimtaSpellbookCurios.afterTeleport(p);return true;}
}
