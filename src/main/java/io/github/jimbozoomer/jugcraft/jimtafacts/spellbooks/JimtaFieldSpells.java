package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.effect.*;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;

/** Small lifetime-limited fields; never ticket chunks or write terrain, and recheck every target on every pulse. */
public final class JimtaFieldSpells {
    public record Profile(double radius,int lifetime,int interval,int burst,int pulse,String secondary,boolean follow,double drift){}
    public static final Map<String,Profile> PROFILES=new LinkedHashMap<>();
    private static final List<Field> ACTIVE=new ArrayList<>();
    private static final class Field {
        final ResourceKey<Level> level;final UUID owner;final String name;final Vec3 direction;final long expires,serial;
        Vec3 center;long next;
        Field(ServerPlayer p,String n,Vec3 at,Profile profile){level=p.level().dimension();owner=p.getUUID();name=n;center=at;direction=p.getLookAngle().multiply(1,0,1).normalize();expires=p.level().getGameTime()+profile.lifetime();next=p.level().getGameTime()+profile.interval();serial=ConcordanceEffects.nextSerial();}
    }
    private JimtaFieldSpells(){}
    public static void register(){
        JimtaSpellbooks.data("fields").entrySet().forEach(e->{var r=e.getValue().getAsJsonObject();PROFILES.put(e.getKey(),new Profile(r.get("radius").getAsDouble(),r.get("lifetime").getAsInt(),r.get("interval").getAsInt(),r.get("burst").getAsInt(),r.get("pulse").getAsInt(),r.get("secondary").getAsString(),r.get("follow").getAsBoolean(),r.get("drift").getAsDouble()));});
        ServerTickEvents.END_SERVER_TICK.register(JimtaFieldSpells::tick);ServerLifecycleEvents.SERVER_STOPPED.register(s->ACTIVE.clear());
    }
    public static boolean start(ServerPlayer p,String name){
        var spec=PROFILES.get(name);if(spec==null||ACTIVE.size()>=32||ACTIVE.stream().filter(f->f.owner.equals(p.getUUID())).count()>=4)return false;
        Vec3 at=p.position();
        if(!spec.follow()){
            var target=JimtaSpellcasting.target(p,32);
            if(target!=null)at=target.position();
            else{var hit=p.level().clip(new ClipContext(p.getEyePosition(),p.getEyePosition().add(p.getLookAngle().scale(32)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));if(hit.getType()!=HitResult.Type.BLOCK)return false;at=hit.getLocation().add(0,.1,0);}
        }
        if(!p.level().hasChunkAt(BlockPos.containing(at))||!p.level().getWorldBorder().isWithinBounds(BlockPos.containing(at))||!spec.follow()&&!Authority.mayChange(p.level(),p,BlockPos.containing(at)))return false;
        var field=new Field(p,name,at,spec);ACTIVE.add(field);if(spec.burst()>0)pulse(p.level(),p,field,spec,true);show(p.level(),field,spec);return true;
    }
    private static void tick(MinecraftServer server){
        for(var it=ACTIVE.iterator();it.hasNext();){var f=it.next();var l=server.getLevel(f.level);var p=server.getPlayerList().getPlayer(f.owner);var spec=PROFILES.get(f.name);
            if(!JimtaSpellbooks.enabled()||l==null||p==null||p.level()!=l||!p.isAlive()||p.isSpectator()||l.getGameTime()>=f.expires||!l.hasChunkAt(BlockPos.containing(f.center))){it.remove();continue;}
            if(spec.follow())f.center=p.position();
            else if(spec.drift()>0){var next=f.center.add(f.direction.scale(spec.drift()));if(l.hasChunkAt(BlockPos.containing(next))&&Authority.mayChange(l,p,BlockPos.containing(next)))f.center=next;}
            if(l.getGameTime()%10==0)show(l,f,spec);
            if(l.getGameTime()>=f.next){f.next=l.getGameTime()+spec.interval();pulse(l,p,f,spec,false);}
        }
    }
    private static void pulse(ServerLevel l,ServerPlayer p,Field f,Profile spec,boolean burst){
        boolean heal=spec.secondary().equals("heal");var ledger=new Ledger(new Ledger.Limits(16,64,0));
        var cause=Cause.of(p.getUUID(),Cause.Origin.SPELL,"jugcraft:jimtafacts/"+f.name,f.serial);
        var bounds=new AABB(f.center,f.center).inflate(spec.radius(),spec.secondary().equals("storm")?12:3,spec.radius());
        for(var target:l.getEntitiesOfClass(LivingEntity.class,bounds,e->e.isAlive()&&!e.isSpectator()).stream().limit(32).toList()){
            Vec3 offset=target.position().subtract(f.center);if(offset.horizontalDistanceSqr()>spec.radius()*spec.radius())continue;
            if(heal?!JimtaSpellcasting.ally(p,target):JimtaSpellcasting.ally(p,target)||!TwoHanded.allowed(p,l,target))continue;
            if(spec.secondary().equals("quake")&&!target.onGround())continue;
            if(l.clip(new ClipContext(f.center.add(0,1,0),target.getBoundingBox().getCenter(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getType()==HitResult.Type.BLOCK)continue;
            String school=switch(JimtaSpellcasting.SPELLS.get(f.name).school()){case "fire"->"fire";case "ice"->"frost";case "lightning"->"lightning";case "holy"->"healing";default->"arcane";};
            var ctx=new ConcordanceEffects.Context(l,cause,p,ledger,"pulse",f.center);int amount=burst?spec.burst():spec.pulse();
            if(amount>0)ConcordanceEffects.apply(ctx,new EffectSpec(heal?EffectKind.RESTORATION:EffectKind.DAMAGE,heal?Intent.HELPFUL:Intent.HARMFUL,amount,0,null,Stacking.STRONGEST,"spell_power:"+school),target);
            var secondary=new ConcordanceEffects.Context(l,cause,p,ledger,"secondary",f.center);
            switch(spec.secondary()){
                case "poison" -> ConcordanceEffects.apply(secondary,new EffectSpec(EffectKind.STATUS,Intent.HARMFUL,0,140,"minecraft:poison",Stacking.STRONGEST,null),target);
                case "ignite" -> ConcordanceEffects.apply(secondary,new EffectSpec(EffectKind.NOUVEAU_CREATURE,Intent.HARMFUL,1,60,"jugcraft:nouveau/ignite",Stacking.STRONGEST,null),target);
                case "quake" -> ConcordanceEffects.apply(secondary,new EffectSpec(EffectKind.STATUS,Intent.HARMFUL,0,40,"minecraft:slowness",Stacking.STRONGEST,null),target);
                case "blizzard" -> {
                    boolean affected=ConcordanceEffects.apply(secondary,new EffectSpec(EffectKind.NOUVEAU_CREATURE,Intent.HARMFUL,1,40,"jugcraft:nouveau/freeze",Stacking.STRONGEST,null),target).applied();
                    if(affected&&offset.horizontalDistanceSqr()>4){Vec3 radial=offset.multiply(1,0,1).normalize();Vec3 tangent=new Vec3(-radial.z,0,radial.x);target.setDeltaMovement(target.getDeltaMovement().add(tangent.scale(.09)).add(radial.scale(-.03)));if(target instanceof ServerPlayer other)other.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(target));}
                }
                case "storm" -> {for(int i=0;i<8;i++)l.sendParticles(ParticleTypes.ELECTRIC_SPARK,target.getX(),target.getY()+i*.75,target.getZ(),1,.04,.15,.04,.01);}
                default -> {}
            }
        }
    }
    private static void show(ServerLevel l,Field f,Profile spec){
        var particle=switch(spec.secondary()){case "heal"->ParticleTypes.HEART;case "blizzard"->ParticleTypes.SNOWFLAKE;case "ignite"->ParticleTypes.FLAME;case "storm"->ParticleTypes.ELECTRIC_SPARK;case "poison"->ParticleTypes.SPORE_BLOSSOM_AIR;default->ParticleTypes.POOF;};
        for(int i=0;i<12;i++){double angle=i*Math.PI/6+l.getGameTime()*.03;l.sendParticles(particle,f.center.x+Math.cos(angle)*spec.radius(),f.center.y+.3,f.center.z+Math.sin(angle)*spec.radius(),1,0,.05,0,.01);}
    }
}
