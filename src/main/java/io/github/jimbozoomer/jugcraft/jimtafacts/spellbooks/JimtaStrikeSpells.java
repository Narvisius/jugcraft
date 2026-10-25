package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.effect.*;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauElemental;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import java.util.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.*;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** Individual source attack shapes, bounded travel effects, shared damage/protection. */
public final class JimtaStrikeSpells {
    public static final Set<String> NAMES=Set.of("divine_smite","flaming_strike","shadow_slash","burning_dash","volt_strike","ascension","chain_lightning","lightning_bolt","fang_strike","fang_ward","stomp");
    private record Ground(ConcordanceEffects.Context context,Vec3 point,long due,int damage,boolean fang){}
    private record Dash(String name,long end,Vec3 previous,Set<UUID> struck){}
    private static final List<Ground> GROUND=new ArrayList<>();
    private static final Map<ServerPlayer,Dash> DASHES=new WeakHashMap<>();
    private static final Map<ServerPlayer,Long> FALL=new WeakHashMap<>();
    private JimtaStrikeSpells(){}
    public static void register(){
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{GROUND.clear();DASHES.clear();FALL.clear();});
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((victim,source,amount)->!(victim instanceof ServerPlayer p&&source.is(DamageTypeTags.IS_FALL)&&JimtaSpellbooks.enabled()&&FALL.getOrDefault(p,0L)>p.level().getGameTime()));
        ServerTickEvents.END_SERVER_TICK.register(server->{
            for(var iterator=GROUND.iterator();iterator.hasNext();){var g=iterator.next();var c=g.context();if(c.level().getGameTime()<g.due())continue;iterator.remove();var p=ConcordanceEffects.person(c);
                if(p==null||!p.isAlive()||p.level()!=c.level()||!JimtaSpellbooks.enabled()||!c.level().hasChunkAt(BlockPos.containing(g.point())))continue;
                for(var e:c.level().getEntitiesOfClass(LivingEntity.class,AABB.ofSize(g.point(),g.fang()?1.5:2,2,2),e->e.isAlive()&&(g.fang()||e.onGround())).stream().limit(8).toList())strike(c,p,e,g.damage(),"",g.point());
            }
            for(var entry:List.copyOf(DASHES.entrySet())){var p=entry.getKey();var d=entry.getValue();
                if(!p.isAlive()||p.isSpectator()||!JimtaSpellbooks.enabled()||p.level().getGameTime()>=d.end()||!p.level().hasChunkAt(p.blockPosition())||!Authority.mayChange(p.level(),p,p.blockPosition())){DASHES.remove(p);continue;}
                var c=context(p,d.name());var swept=p.getBoundingBox().expandTowards(d.previous().subtract(p.position())).inflate(.8);
                for(var e:p.level().getEntitiesOfClass(LivingEntity.class,swept,LivingEntity::isAlive).stream().limit(16).toList())if(!d.struck().contains(e.getUUID())&&strike(c,p,e,d.name().equals("burning_dash")?10:6,d.name().equals("burning_dash")?"ignite":"",p.position()))d.struck().add(e.getUUID());
                p.level().sendParticles(d.name().equals("burning_dash")?ParticleTypes.FLAME:ParticleTypes.ELECTRIC_SPARK,p.getX(),p.getY()+.5,p.getZ(),8,.4,.5,.4,.04);
                DASHES.put(p,new Dash(d.name(),d.end(),p.position(),d.struck()));
            }
        });
    }
    private static ConcordanceEffects.Context context(ServerPlayer p,String name){return new ConcordanceEffects.Context(p.level(),Cause.of(p.getUUID(),Cause.Origin.SPELL,"jugcraft:jimtafacts/"+name,ConcordanceEffects.nextSerial()),p,new Ledger(new Ledger.Limits(16,96,0)),name,p.position());}
    private static boolean clear(ServerPlayer p,Vec3 from,Vec3 to){return p.level().clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getType()!=HitResult.Type.BLOCK;}
    private static boolean strike(ConcordanceEffects.Context c,ServerPlayer p,LivingEntity e,int damage,String secondary,Vec3 origin){
        if(e==p||JimtaSpellcasting.ally(p,e)||!TwoHanded.allowed(p,p.level(),e)||!clear(p,origin.add(0,1,0),e.getBoundingBox().getCenter()))return false;
        String name=c.step();String school=switch(JimtaSpellcasting.SPELLS.get(name).school()){case "fire","lightning"->JimtaSpellcasting.SPELLS.get(name).school();case "holy"->"healing";default->"arcane";};
        var context=new ConcordanceEffects.Context(c.level(),c.cause(),p,c.ledger(),name,origin);
        if(!ConcordanceEffects.apply(context,new EffectSpec(EffectKind.DAMAGE,Intent.HARMFUL,Math.clamp(damage,1,40),0,null,Stacking.STRONGEST,"spell_power:"+school),e).applied())return false;
        if(secondary.equals("ignite"))ConcordanceEffects.apply(new ConcordanceEffects.Context(c.level(),c.cause(),p,c.ledger(),name+"/ignite",origin),new EffectSpec(EffectKind.NOUVEAU_CREATURE,Intent.HARMFUL,1,80,"jugcraft:nouveau/ignite",Stacking.STRONGEST,null),e);
        return true;
    }
    private static int weapon(ServerPlayer p){return (int)Math.clamp(p.getOffhandItem().getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY).compute(Attributes.ATTACK_DAMAGE,1,EquipmentSlot.MAINHAND),0,20);}
    private static void motion(ServerPlayer p,Vec3 velocity,int fallTicks){
        var hit=p.level().clip(new ClipContext(p.getEyePosition(),p.getEyePosition().add(velocity.scale(3)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        double scale=hit.getType()==HitResult.Type.BLOCK?Math.clamp(hit.getLocation().distanceTo(p.getEyePosition())/(velocity.length()*3+.001),0,1):1;
        var destination=p.position().add(velocity.scale(scale*3));
        if(!p.level().hasChunkAt(BlockPos.containing(destination))||!Authority.mayChange(p.level(),p,BlockPos.containing(destination)))return;
        p.setDeltaMovement(velocity.scale(scale));p.connection.send(new ClientboundSetEntityMotionPacket(p));FALL.put(p,p.level().getGameTime()+fallTicks);
    }
    private static void lightning(ServerPlayer p,Vec3 at){var bolt=EntityTypes.LIGHTNING_BOLT.create(p.level(),EntitySpawnReason.TRIGGERED);if(bolt!=null){bolt.setVisualOnly(true);bolt.snapTo(at);p.level().addFreshEntity(bolt);}}
    public static boolean cast(ServerPlayer p,String name){
        if(!Authority.mayChange(p.level(),p,p.blockPosition()))return false;
        var c=context(p,name);Vec3 look=p.getLookAngle(),eye=p.getEyePosition(),origin=p.position();boolean changed=false;
        if(name.equals("burning_dash")||name.equals("volt_strike")){
            if(DASHES.containsKey(p))return false;
            motion(p,look.multiply(3,1,3).normalize().add(0,.2,0).scale(name.equals("burning_dash")?1.25:1.6),40);
            DASHES.put(p,new Dash(name,p.level().getGameTime()+(name.equals("burning_dash")?15:10),p.position(),new HashSet<>()));return true;
        }
        if(name.equals("chain_lightning")){
            var target=JimtaSpellcasting.target(p,32);if(target==null)return false;Set<UUID> visited=new HashSet<>();Vec3 previous=eye;
            for(int count=0;count<4&&target!=null;count++){
                if(!strike(c,p,target,6,"",previous))break;changed=true;visited.add(target.getUUID());Vec3 end=target.getBoundingBox().getCenter();
                for(int n=0;n<12;n++){var point=previous.lerp(end,n/11.0);p.level().sendParticles(ParticleTypes.ELECTRIC_SPARK,point.x,point.y,point.z,2,.04,.04,.04,0);}previous=end;
                LivingEntity from=target;target=p.level().getEntitiesOfClass(LivingEntity.class,target.getBoundingBox().inflate(4),e->e.isAlive()&&!visited.contains(e.getUUID())&&!JimtaSpellcasting.ally(p,e)&&TwoHanded.allowed(p,p.level(),e)&&ConcordanceEffects.mayHarm(p,e)&&clear(p,from.getEyePosition(),e.getBoundingBox().getCenter())).stream().filter(e->from.distanceToSqr(e)<=16).min(Comparator.comparingDouble(from::distanceToSqr)).orElse(null);
            }return changed;
        }
        if(name.startsWith("fang_")||name.equals("stomp")){
            int total=name.equals("fang_ward")?11:name.equals("fang_strike")?8:5;
            for(int i=0;i<total&&GROUND.size()<256;i++){
                Vec3 point;if(name.equals("fang_ward")){int ring=i<5?0:1,index=ring==0?i:i-5,count=ring==0?5:6;double angle=Math.PI*2*index/count;point=origin.add(Math.cos(angle)*1.5*(ring+1),1,Math.sin(angle)*1.5*(ring+1));}
                else point=origin.add(look.multiply(1,0,1).normalize().scale((i+1)*(name.equals("fang_strike")?1.2:1))).add(0,1,0);
                var ground=p.level().clip(new ClipContext(point,point.add(0,-5,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));if(ground.getType()!=HitResult.Type.BLOCK)continue;point=ground.getLocation();
                if(!p.level().hasChunkAt(BlockPos.containing(point))||!Authority.mayChange(p.level(),p,BlockPos.containing(point)))continue;
                int delay=name.equals("fang_ward")?(i<5?0:1):i*2;
                if(!name.equals("stomp"))NouveauElemental.displayFang(p.level(),p,point,p.getYRot(),delay);
                else p.level().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,p.level().getBlockState(ground.getBlockPos())),point.x,point.y+.1,point.z,8,.7,.1,.7,.08);
                GROUND.add(new Ground(c,point,p.level().getGameTime()+8+delay,name.equals("stomp")?8:6,!name.equals("stomp")));changed=true;
            }return changed;
        }
        Vec3 center=origin;double radius=3.25;int damage=5+weapon(p);boolean forward=true,falloff=false;
        switch(name){
            case "divine_smite"->{center=origin.add(look.multiply(1.7,0,1.7));radius=2.2;damage=8+weapon(p);forward=false;}
            case "shadow_slash"->{var aimed=JimtaSpellcasting.target(p,12);if(aimed!=null)center=aimed.position().add(look.scale(1.25));else center=eye.add(look.scale(4));radius=2.5;damage=5+weapon(p);forward=false;
                for(var projectile:p.level().getEntitiesOfClass(Projectile.class,AABB.ofSize(center,5,5,5),e->!e.isRemoved()&&e.getOwner()!=p&&e.getDeltaMovement().lengthSqr()>.01).stream().limit(8).toList())if(Authority.mayChange(p.level(),p,projectile.blockPosition())&&(!(projectile.getOwner() instanceof LivingEntity shooter)||TwoHanded.allowed(p,p.level(),shooter)&&ConcordanceEffects.mayHarm(p,shooter))&&clear(p,eye,projectile.position())){projectile.setOwner(p);projectile.shoot(look.x,look.y,look.z,(float)Math.max(.5,projectile.getDeltaMovement().length()),0);changed=true;}
                motion(p,center.subtract(eye).scale(1.0/6).add(0,.1,0),20);
            }
            case "ascension"->{radius=5;damage=5;forward=false;falloff=true;lightning(p,origin);motion(p,look.multiply(1,0,1).normalize().add(0,5,0).scale(.125),100);changed=true;}
            case "lightning_bolt"->{var target=JimtaSpellcasting.target(p,32);var hit=p.level().clip(new ClipContext(eye,eye.add(look.scale(32)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));if(target==null&&hit.getType()!=HitResult.Type.BLOCK)return false;center=target!=null?target.position():hit.getLocation();if(!Authority.mayChange(p.level(),p,BlockPos.containing(center)))return false;radius=4;damage=10;forward=false;falloff=true;lightning(p,center);changed=true;}
        }
        for(var e:p.level().getEntitiesOfClass(LivingEntity.class,AABB.ofSize(center,radius*2,radius*3,radius*2),LivingEntity::isAlive).stream().limit(16).toList()){
            double distance=e.distanceToSqr(center);if(distance>radius*radius||forward&&e.getBoundingBox().getCenter().subtract(eye).dot(look)<0)continue;
            int dealt=falloff?(int)Math.ceil(damage*(1-distance/(radius*radius))):damage;if(dealt<=0)continue;
            changed|=strike(c,p,e,dealt,name.equals("flaming_strike")?"ignite":"",center);
        }
        p.level().sendParticles(name.equals("flaming_strike")?ParticleTypes.FLAME:name.equals("shadow_slash")?ParticleTypes.PORTAL:ParticleTypes.ELECTRIC_SPARK,center.x,center.y+.8,center.z,24,radius/2,.4,radius/2,.05);
        return changed;
    }
}
