package io.github.jimbozoomer.jugcraft.menagerie;

import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;

/** Supplied head-count/merge system, bounded to sixteen heads per native slime. */
public final class RecoveredFerrouslime extends Caverns.Creature implements Enemy {
    private static final EntityDataAccessor<Integer> HEADS=SynchedEntityData.defineId(RecoveredFerrouslime.class,EntityDataSerializers.INT);
    private int mergeCooldown=100;
    public RecoveredFerrouslime(EntityType<? extends MenagerieAnimal> type,Level level,Menagerie.Species s){super(type,level,s);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(HEADS,1);}
    public int heads(){return entityData.get(HEADS);}
    @Override protected EntityDimensions getDefaultDimensions(Pose pose){return super.getDefaultDimensions(pose).scale(slimeSize());}
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(key.equals(HEADS))refreshDimensions();}
    public float slimeSize(){return Math.min((float)(Math.log(Math.max(1,heads()))+1)*1.2F,3.2F);}
    public void heads(int count){
        entityData.set(HEADS,Math.clamp(count,1,16));
        if(!level().isClientSide()){
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(Math.clamp(heads()*10,10,100));
            getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Math.clamp(heads()*2,2,10));
            setHealth(Math.min(getHealth(),getMaxHealth()));
        }
    }
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,net.minecraft.world.DifficultyInstance difficulty,EntitySpawnReason reason,SpawnGroupData group){
        var result=super.finalizeSpawn(level,difficulty,reason,group);heads(1+random.nextInt(2));setHealth(getMaxHealth());return result;
    }
    @Override protected void customServerAiStep(ServerLevel level){
        super.customServerAiStep(level);if(mergeCooldown>0)mergeCooldown--;
        if(tickCount%20!=0||mergeCooldown>0||heads()>=16||owner()!=null||isVehicle()||isPassenger())return;
        for(var other:level.getEntitiesOfClass(RecoveredFerrouslime.class,getBoundingBox().inflate(3),e->e!=this&&e.isAlive()&&e.owner()==null&&e.mergeCooldown==0&&!e.isPassenger()&&!e.isVehicle())){
            if(getId()>=other.getId()||heads()+other.heads()>16)continue;
            if(distanceToSqr(other)>2){getNavigation().moveTo(other,1);break;}
            float combined=getHealth()+other.getHealth();heads(heads()+other.heads());setHealth(Math.min(combined,getMaxHealth()));
            mergeCooldown=600;other.discard();break;
        }
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putInt("OriginalSlimeHeads",heads());out.putInt("OriginalSlimeMergeCooldown",mergeCooldown);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);heads(in.getIntOr("OriginalSlimeHeads",1));mergeCooldown=Math.clamp(in.getIntOr("OriginalSlimeMergeCooldown",100),0,1200);}
}
