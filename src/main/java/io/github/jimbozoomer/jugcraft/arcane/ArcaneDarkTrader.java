package io.github.jimbozoomer.jugcraft.arcane;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;

/** Supplied character and portal choreography. Source contains no merchant offers. */
public final class ArcaneDarkTrader extends PathfinderMob {
    public static EntityType<ArcaneDarkTrader> TYPE;
    private static final EntityDataAccessor<Integer> VARIANT=SynchedEntityData.defineId(ArcaneDarkTrader.class,EntityDataSerializers.INT),ACTION=SynchedEntityData.defineId(ArcaneDarkTrader.class,EntityDataSerializers.INT),ELAPSED=SynchedEntityData.defineId(ArcaneDarkTrader.class,EntityDataSerializers.INT);
    public ArcaneDarkTrader(EntityType<? extends ArcaneDarkTrader> type,Level level){super(type,level);setPersistenceRequired();}
    static void register(){
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("forbidden_dark_trader"));
        TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<ArcaneDarkTrader>of(ArcaneDarkTrader::new,MobCategory.CREATURE).sized(.6F,1.5F).clientTrackingRange(8).build(key));
        FabricDefaultAttributeRegistry.register(TYPE,Mob.createMobAttributes().add(Attributes.MAX_HEALTH,14).add(Attributes.MOVEMENT_SPEED,.3));
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(VARIANT,0);b.define(ACTION,1);b.define(ELAPSED,0);}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(1,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(2,new RandomLookAroundGoal(this));}
    public String variant(){return switch(entityData.get(VARIANT)){case 1->"idril";case 2->"hohenheim";default->"brook";};}
    public int action(){return entityData.get(ACTION);}public int elapsed(){return entityData.get(ELAPSED);}
    private void action(int value){entityData.set(ACTION,value);entityData.set(ELAPSED,0);}
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,EntitySpawnReason reason,SpawnGroupData data){entityData.set(VARIANT,random.nextInt(3));action(1);return super.finalizeSpawn(level,difficulty,reason,data);}
    @Override public void tick(){
        super.tick();if(level().isClientSide())return;
        int time=elapsed()+1;entityData.set(ELAPSED,time);
        if(action()==1){navigation.stop();setDeltaMovement(0,getDeltaMovement().y,0);if(time>=60)action(0);}
        else if(action()==2){navigation.stop();if(time>=50)discard();}
        else if(action()>=3){if(time>=50)action(0);}
        else if(time>=80&&random.nextInt(120)==0)action(random.nextBoolean()?3:4);
    }
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){
        if(action()==1||action()==2)return InteractionResult.PASS;
        if(player instanceof ServerPlayer p){
            if(!Authority.mayChange(p.level(),p,blockPosition()))return InteractionResult.FAIL;
            action(5);getLookControl().setLookAt(p);
            p.sendOverlayMessage(Component.literal("The traveler greets you, but has no wares to offer yet."));
        }return InteractionResult.SUCCESS;
    }
    @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){
        if(action()==1||action()==2)return false;
        boolean hurt=super.hurtServer(level,source,amount);if(hurt&&isAlive())action(2);return hurt;
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putString("variant",variant());out.putInt("action",action());out.putInt("elapsed",elapsed());}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);String v=in.getStringOr("variant","brook");entityData.set(VARIANT,v.endsWith("idril")?1:v.endsWith("hohenheim")?2:0);entityData.set(ACTION,Math.clamp(in.getIntOr("action",0),0,5));entityData.set(ELAPSED,Math.clamp(in.getIntOr("elapsed",0),0,10000));}
}
