package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import net.spell_engine.api.spell.event.SpellHandlers;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.SpellExecution;
import net.spell_power.api.SpellPower;

/** Source converging needle geometry and widening slash, with original animated renderers adapted on the client. */
public final class JimtaBloodProjectile extends Projectile {
    public static EntityType<JimtaBloodProjectile> TYPE;
    private static final EntityDataAccessor<String> SPELL=SynchedEntityData.defineId(JimtaBloodProjectile.class,EntityDataSerializers.STRING);
    private final Set<UUID> struck=new HashSet<>();
    public JimtaBloodProjectile(EntityType<? extends Projectile> type,Level level){super(type,level);setNoGravity(true);}
    public String spell(){return entityData.get(SPELL);}
    public boolean slash(){return spell().equals("blood_slash");}
    public static void register(){
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("jimtafacts_blood_projectile"));
        TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<JimtaBloodProjectile>of(JimtaBloodProjectile::new,MobCategory.MISC).sized(.3F,.3F).clientTrackingRange(12).updateInterval(1).build(key));
        SpellHandlers.registerCustomDelivery(Jugcraft.id("jimtafacts_blood"),(level,spell,caster,targets,context,location)->{
            if(!(caster instanceof ServerPlayer p))return false;String name=JimtaSpellcasting.name(spell);if(!Set.of("blood_needles","acupuncture","blood_slash").contains(name))return false;
            Vec3 eye=p.getEyePosition(),look=p.getLookAngle();var target=JimtaSpellcasting.target(p,32);if(name.equals("acupuncture")&&target==null)return false;
            Vec3 center=target!=null?target.position().add(0,target.getEyeHeight()/2,0):level.clip(new ClipContext(eye,eye.add(look.scale(32)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getLocation();
            int count=name.equals("blood_slash")?1:5;boolean any=false;
            for(int i=0;i<count;i++){
                double angle=Math.PI*2*i/count;Vec3 at;
                if(name.equals("acupuncture"))at=center.add(new Vec3(Math.cos(angle),.45+level.getRandom().nextDouble()*.5,Math.sin(angle)).normalize().scale(target.getBbWidth()+2.75));
                else if(name.equals("blood_needles")){var right=new Vec3(0,1,0).cross(look).normalize();if(right.lengthSqr()<.1)right=new Vec3(1,0,0);var up=look.cross(right).normalize();at=eye.add(right.scale(Math.cos(angle)*1.5)).add(up.scale(Math.sin(angle)*1.5));}
                else at=eye.add(look.scale(.5));
                if(!level.hasChunkAt(BlockPos.containing(at))||!Authority.mayChange(p.level(),p,BlockPos.containing(at))||level.clip(new ClipContext(eye,at,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getType()==HitResult.Type.BLOCK)continue;
                var projectile=TYPE.create(level,EntitySpawnReason.TRIGGERED);if(projectile==null)continue;projectile.entityData.set(SPELL,name);projectile.setOwner(p);projectile.snapTo(at.x,at.y,at.z,p.getYRot(),p.getXRot());
                Vec3 direction=name.equals("blood_slash")?look:center.subtract(at).normalize();double speed=name.equals("acupuncture")?.35:name.equals("blood_slash")?.8:1.2;
                projectile.setDeltaMovement(direction.scale(speed));any|=level.addFreshEntity(projectile);
            }return any;
        });
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){builder.define(SPELL,"blood_needles");}
    @Override protected void addAdditionalSaveData(ValueOutput output){super.addAdditionalSaveData(output);output.putString("JimtaSpell",spell());output.putInt("JimtaAge",tickCount);output.store("JimtaStruck",UUIDUtil.CODEC.listOf(),new ArrayList<>(struck));}
    @Override protected void readAdditionalSaveData(ValueInput input){super.readAdditionalSaveData(input);String name=input.getStringOr("JimtaSpell","blood_needles");entityData.set(SPELL,Set.of("blood_needles","acupuncture","blood_slash").contains(name)?name:"blood_needles");tickCount=input.getIntOr("JimtaAge",0);struck.addAll(input.read("JimtaStruck",UUIDUtil.CODEC.listOf()).orElse(List.of()).stream().limit(16).toList());}
    @Override public void tick(){
        super.tick();Vec3 start=position(),end=start.add(getDeltaMovement());
        if(level().isClientSide()){setPos(end);return;}
        if(!(getOwner() instanceof ServerPlayer p)||!p.isAlive()||p.level()!=level()||!JimtaSpellbooks.enabled()||tickCount>100||distanceToSqr(p)>80*80||!level().hasChunkAt(BlockPos.containing(end))){discard();return;}
        var wall=level().clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));Vec3 stop=wall.getType()==HitResult.Type.BLOCK?wall.getLocation():end;
        double radius=slash()?Math.min(2,.3+tickCount*.05):.2;
        var entry=SpellRegistry.from(level()).get(Jugcraft.id("jimtafacts/"+spell())).orElse(null);if(entry==null){discard();return;}
        for(var target:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().expandTowards(stop.subtract(start)).inflate(radius),e->e.isAlive()&&e!=p&&!struck.contains(e.getUUID())).stream().limit(16).toList()){
            if(!target.getBoundingBox().inflate(radius).contains(start)&&target.getBoundingBox().inflate(radius).clip(start,stop).isEmpty())continue;
            if(level().clip(new ClipContext(start,target.getBoundingBox().getCenter(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()==HitResult.Type.BLOCK)continue;
            struck.add(target.getUUID());var power=SpellPower.getSpellPower(entry.value().school,p);
            var context=new SpellExecution.ImpactContext().position(target.position());JimtaCombatSpells.impact(entry,power,p,target,context);
            if(!slash()){discard();return;}
        }
        setPos(stop);if(wall.getType()==HitResult.Type.BLOCK||struck.size()>=16)discard();
    }
}
