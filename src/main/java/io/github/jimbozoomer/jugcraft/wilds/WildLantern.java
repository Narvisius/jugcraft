package io.github.jimbozoomer.jugcraft.wilds;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** A passive forest lantern propelled by the original timed puffs, not continuous hovering. */
public final class WildLantern extends PathfinderMob {
    public static EntityType<WildLantern> TYPE;
    private static final EntityDataAccessor<Integer> PUFF=SynchedEntityData.defineId(WildLantern.class,EntityDataSerializers.INT);
    private Vec3 destination;
    private int groundDistance=1,puffAge=28;
    public WildLantern(EntityType<? extends WildLantern> type,Level l){super(type,l);setNoGravity(true);xpReward=1;}
    static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("wilds_lantern"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<WildLantern>of(WildLantern::new,MobCategory.AMBIENT).sized(1,1).clientTrackingRange(8).updateInterval(1).build(key));FabricDefaultAttributeRegistry.register(TYPE,Mob.createMobAttributes().add(Attributes.MAX_HEALTH,4).add(Attributes.MOVEMENT_SPEED,.3).add(Attributes.KNOCKBACK_RESISTANCE,.2));SpawnPlacements.register(TYPE,WildSpawns.SOLID_FLOOR,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(t,l,reason,p,r)->p.getY()>=60&&net.minecraft.world.entity.monster.Monster.isDarkEnoughToSpawn(l,p,r)&&(l.getBlockState(p.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON)||l.getBlockState(p.below()).is(BlockTags.LEAVES)||l.getBlockState(p.below()).is(BlockTags.LOGS)));BiomeModifications.addSpawn(BiomeSelectors.includeByKey(net.minecraft.world.level.biome.Biomes.DARK_FOREST),MobCategory.AMBIENT,TYPE,5,2,4);}
    @Override protected void registerGoals(){}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(PUFF,-1000);}
    public float puffTime(float partial){return level().getGameTime()-entityData.get(PUFF)+partial;}
    @Override public void travel(Vec3 input){move(MoverType.SELF,getDeltaMovement());setDeltaMovement(getDeltaMovement().scale(isInWater()?.8:isInLava()?.5:.91));calculateEntityAnimation(false);}
    @Override public void tick(){super.tick();if(!(level() instanceof ServerLevel l)||!isAlive())return;
        if(tickCount%5==0){groundDistance=0;for(int i=0;i<16;i++){var p=blockPosition().below(i);if(!l.hasChunkAt(p)||!l.isEmptyBlock(p))break;groundDistance++;}}
        if(destination==null||distanceToSqr(destination)<1||tickCount%80==0){destination=position().add((random.nextDouble()*2-1)*16,(random.nextDouble()*2-1)*8,(random.nextDouble()*2-1)*16);if(!l.hasChunkAt(BlockPos.containing(destination))||!l.getWorldBorder().isWithinBounds(BlockPos.containing(destination)))destination=position();}
        if(puffAge>=28&&(groundDistance<5||groundDistance<16&&random.nextInt(13)==0)){puffAge=0;entityData.set(PUFF,(int)l.getGameTime());}
        if(++puffAge==7){Vec3 direction=destination.subtract(position()).normalize();boolean clear=true;int distance=Math.min(24,(int)Math.ceil(destination.distanceTo(position())));for(int i=1;i<=distance;i++)if(!l.hasChunkAt(BlockPos.containing(position().add(direction.scale(i))))||!l.noCollision(this,getBoundingBox().move(direction.scale(i)))){clear=false;break;}
            setDeltaMovement(getDeltaMovement().add(0,.2+.2/Math.max(1,groundDistance),0).add(clear?direction.scale(.2):Vec3.ZERO));needsSync=true;playSound(OwnerWilds.sound("lantern.puff"),.6F,1+random.nextFloat()*.2F);l.sendParticles(ParticleTypes.HAPPY_VILLAGER,getX(),getY()+.3,getZ(),8,.2,.1,.2,.02);}
        if(groundDistance>=2)setDeltaMovement(getDeltaMovement().add(0,-.0055,0));
    }
    @Override public boolean causeFallDamage(double d,float m,net.minecraft.world.damagesource.DamageSource source){return false;}
    @Override protected void tickDeath(){deathTime++;if(deathTime==2&&!level().isClientSide())playSound(OwnerWilds.sound("lantern.pop"),1,.8F+random.nextFloat()*.4F);if(deathTime>=25&&!level().isClientSide())remove(RemovalReason.KILLED);}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);setNoGravity(true);}
}
