package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.finance.LandRegistry;
import io.github.jimbozoomer.jugcraft.town.Town;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Hostile tomb never grants the healing or safety of a player's Ice Tomb spell. */
public final class JimtaSpiderTomb extends Entity {
    public static EntityType<JimtaSpiderTomb> TOMB,SPIKE;
    private static final EntityDataAccessor<Integer> WAIT=SynchedEntityData.defineId(JimtaSpiderTomb.class,EntityDataSerializers.INT);
    private final boolean spike;private UUID owner,victim;private boolean releasing,struck;
    public JimtaSpiderTomb(EntityType<?> t,Level l,boolean spike){super(t,l);this.spike=spike;setNoGravity(spike);}
    public boolean spike(){return spike;}public int waitTicks(){return entityData.get(WAIT);}public boolean restraining(){return !spike&&!releasing&&isAlive()&&JimtaSpellbooks.enabled();}
    public static void register(){
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("jimtafacts_spider_tomb"));TOMB=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<JimtaSpiderTomb>of((t,l)->new JimtaSpiderTomb(t,l,false),MobCategory.MISC).sized(1,2.3F).clientTrackingRange(12).updateInterval(1).build(key));
        var spikes=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("jimtafacts_spider_ice_spike"));SPIKE=Registry.register(BuiltInRegistries.ENTITY_TYPE,spikes,EntityType.Builder.<JimtaSpiderTomb>of((t,l)->new JimtaSpiderTomb(t,l,true),MobCategory.MISC).sized(.6F,2.8F).clientTrackingRange(12).updateInterval(1).build(spikes));
    }
    public static void entomb(JimtaEncounterCreatures.IceSpider spider,LivingEntity target){
        if(!(spider.level() instanceof ServerLevel l)||target.isPassenger()||!JimtaEncounterCreatures.prey(spider,target)||Town.isInside(l,target.blockPosition())||LandRegistry.isClaimed(l,target.blockPosition()))return;
        var tomb=TOMB.create(l,EntitySpawnReason.TRIGGERED);if(tomb==null)return;tomb.owner=spider.getUUID();tomb.victim=target.getUUID();tomb.setPos(target.position());tomb.setDeltaMovement(target.getDeltaMovement().add(spider.getForward().add(0,1,0).scale(.5)));
        // The victim necessarily overlaps its prison. Exclude only that victim,
        // while retaining solid terrain, world-border and other entity collision checks.
        if(!l.noCollision(target,tomb.getBoundingBox())||!l.addFreshEntity(tomb))return;if(!target.startRiding(tomb,true,false))tomb.release();
    }
    public static void spikes(JimtaEncounterCreatures.IceSpider spider,LivingEntity target){
        if(!(spider.level() instanceof ServerLevel l)||!JimtaEncounterCreatures.prey(spider,target)||l.getEntitiesOfClass(JimtaSpiderTomb.class,spider.getBoundingBox().inflate(48),e->spider.getUUID().equals(e.owner)).size()>=16)return;
        Vec3 direction=target.position().subtract(spider.position()).multiply(1,0,1).normalize();
        for(int i=1;i<=8;i++){Vec3 at=spider.position().add(direction.scale(i*1.5));var pos=BlockPos.containing(at);if(!l.hasChunkAt(pos)||Town.isInside(l,pos)||LandRegistry.isClaimed(l,pos))continue;var hit=l.clip(new ClipContext(at.add(0,3,0),at.add(0,-3,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,spider));if(hit.getType()!=HitResult.Type.BLOCK)continue;var e=SPIKE.create(l,EntitySpawnReason.TRIGGERED);if(e==null)continue;e.owner=spider.getUUID();e.victim=target.getUUID();e.setPos(hit.getLocation());e.entityData.set(WAIT,i*2);l.addFreshEntity(e);}
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(WAIT,0);}
    @Override public void tick(){
        super.tick();if(!(level() instanceof ServerLevel l))return;var hunter=owner==null?null:l.getEntity(owner);var target=victim==null?null:l.getEntity(victim);
        if(!(hunter instanceof JimtaEncounterCreatures.IceSpider spider)||!spider.isAlive()||!(target instanceof LivingEntity living)||!JimtaEncounterCreatures.prey(spider,living)||Town.isInside(l,blockPosition())||LandRegistry.isClaimed(l,blockPosition())){release();return;}
        if(spike){int age=tickCount-waitTicks();if(age>76){release();return;}if(age>0&&age<6&&!struck&&getBoundingBox().intersects(living.getBoundingBox())&&living.hurtServer(l,l.damageSources().mobAttack(spider),6)){struck=true;living.setTicksFrozen(Math.min(300,living.getTicksFrozen()+80));living.setDeltaMovement(living.getDeltaMovement().add(0,.6,0));living.syncVelocity=true;}}
        else {if(tickCount>100||!hasPassenger(living)){release();return;}Vec3 next=position().add(getDeltaMovement());if(!l.hasChunkAt(BlockPos.containing(next))||LandRegistry.crossesBoundary(l,blockPosition(),BlockPos.containing(next))){release();return;}setDeltaMovement(getDeltaMovement().add(0,-.08,0).multiply(.95,1,.95));move(MoverType.SELF,getDeltaMovement());living.setTicksFrozen(Math.min(420,living.getTicksFrozen()+5));}
    }
    public void release(){releasing=true;ejectPassengers();discard();}
    @Override public boolean hurtServer(ServerLevel l,DamageSource source,float damage){if(spike||damage<=0)return false;var target=victim==null?null:l.getEntity(victim);if(source.getEntity() instanceof ServerPlayer player&&target instanceof LivingEntity living&&player!=living&&!TwoHanded.allowed(player,l,living))return false;release();return true;}
    @Override public boolean isPickable(){return !spike;}
    @Override protected boolean canAddPassenger(Entity passenger){return !spike&&passenger.getUUID().equals(victim)&&getPassengers().isEmpty();}
    @Override public Vec3 getPassengerRidingPosition(Entity p){return position();}
    @Override protected void addAdditionalSaveData(ValueOutput out){if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);if(victim!=null)out.store("victim",UUIDUtil.CODEC,victim);out.putInt("age",tickCount);out.putInt("wait",waitTicks());out.putBoolean("struck",struck);}
    @Override protected void readAdditionalSaveData(ValueInput in){owner=in.read("owner",UUIDUtil.CODEC).orElse(null);victim=in.read("victim",UUIDUtil.CODEC).orElse(null);tickCount=Math.clamp(in.getIntOr("age",101),0,101);entityData.set(WAIT,Math.clamp(in.getIntOr("wait",0),0,16));struck=in.getBooleanOr("struck",false);}
}
