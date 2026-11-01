package io.github.jimbozoomer.jugcraft.arcane;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** Owner's three drifting spirits: gentle idle flight, panic/retreat and essence conversion. */
public final class ArcaneLostSoul extends PathfinderMob {
    public static final Map<String,EntityType<ArcaneLostSoul>> TYPES=new LinkedHashMap<>();
    private static final EntityDataAccessor<Boolean> SCARED=SynchedEntityData.defineId(ArcaneLostSoul.class,EntityDataSerializers.BOOLEAN);
    private int frightened;
    public ArcaneLostSoul(EntityType<? extends ArcaneLostSoul> type,Level level){super(type,level);moveControl=new FlyingMoveControl<>(this,15,true);setNoGravity(true);noPhysics=true;}
    static void register(){for(String name:List.of("lost_soul","corrupt_lost_soul","enchanted_lost_soul")){
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("forbidden_"+name));var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<ArcaneLostSoul>of(ArcaneLostSoul::new,MobCategory.CREATURE).sized(.35F,.6F).clientTrackingRange(8).build(key));TYPES.put(name,type);
        FabricDefaultAttributeRegistry.register(type,Mob.createMobAttributes().add(Attributes.MAX_HEALTH,14).add(Attributes.FLYING_SPEED,.5).add(Attributes.MOVEMENT_SPEED,.3));
        SpawnPlacements.register(type,SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(t,l,reason,pos,random)->true);
    }BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_OVERWORLD),MobCategory.CREATURE,TYPES.get("lost_soul"),35,1,3);}
    public String kind(){return BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath().replace("forbidden_","");}
    public boolean scared(){return entityData.get(SCARED);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(SCARED,false);}
    @Override protected PathNavigation createNavigation(Level l){var nav=new FlyingPathNavigation(this,l);nav.setCanFloat(true);nav.setCanOpenDoors(false);return nav;}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new Drift());goalSelector.addGoal(1,new LookAtPlayerGoal(this,Player.class,6));goalSelector.addGoal(2,new RandomLookAroundGoal(this));}
    private final class Drift extends Goal {
        int remaining;
        Drift(){setFlags(EnumSet.of(Flag.MOVE));}
        @Override public boolean canUse(){return true;}
        @Override public void tick(){
            if(--remaining>0)return;remaining=20+random.nextInt(30);Vec3 destination;
            var danger=getLastHurtByMob();if(scared()&&danger!=null&&danger.isAlive()){var away=position().subtract(danger.position()).normalize();destination=position().add(away.scale(8)).add(0,1,0);remaining=10;}
            else if(random.nextInt(5)==0){navigation.stop();return;}
            else destination=position().add(random.nextInt(13)-6,random.nextInt(7)-3,random.nextInt(13)-6);
            var target=BlockPos.containing(destination);if(!level().hasChunkAt(target)||!level().getWorldBorder().isWithinBounds(target)||target.getY()<level().getMinY()+1||target.getY()>level().getMaxY()-2)return;
            if(!scared()&&!level().isEmptyBlock(target))return;navigation.moveTo(destination.x,destination.y,destination.z,scared()?1.75:1);
        }
    }
    @Override public void tick(){super.tick();if(level().isClientSide()){if(tickCount%10==0)level().addParticle(kind().equals("corrupt_lost_soul")?ParticleTypes.WITCH:kind().equals("enchanted_lost_soul")?ParticleTypes.ENCHANT:ParticleTypes.SOUL,getX(),getY()+.2,getZ(),0,0,0);return;}var danger=getLastHurtByMob();if(danger!=null&&danger.isAlive()&&distanceToSqr(danger)<=225)frightened=80;else if(frightened>0)frightened--;entityData.set(SCARED,frightened>0);}
    @Override public void knockback(double strength,double x,double z,net.minecraft.world.damagesource.DamageSource source,float force,boolean causedByAttack){}
    @Override public boolean causeFallDamage(double distance,float multiplier,net.minecraft.world.damagesource.DamageSource source){return false;}
    @Override protected InteractionResult mobInteract(Player p,InteractionHand hand){
        var held=p.getItemInHand(hand);String next=kind().equals("lost_soul")&&held.is(ForbiddenArcane.item("corrupti_dust"))?"corrupt_lost_soul":held.is(ForbiddenArcane.item("aureal_bottle"))?(kind().equals("corrupt_lost_soul")?"lost_soul":kind().equals("lost_soul")?"enchanted_lost_soul":""):"";
        if(next.isEmpty())return super.mobInteract(p,hand);if(p instanceof ServerPlayer actor){if(!Authority.mayChange(actor.level(),actor,blockPosition())||!Authority.mayStrike(actor,this))return InteractionResult.FAIL;
            var converted=convertTo(TYPES.get(next),ConversionParams.single(this,true,true),EntitySpawnReason.CONVERSION,s->{s.setHealth(Math.min(s.getMaxHealth(),getHealth()));});if(converted==null)return InteractionResult.FAIL;held.consume(1,p);
        }return InteractionResult.SUCCESS;
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putInt("frightened",frightened);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);frightened=Math.clamp(in.getIntOr("frightened",0),0,80);entityData.set(SCARED,frightened>0);}
}
