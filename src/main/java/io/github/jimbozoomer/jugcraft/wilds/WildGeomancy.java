package io.github.jimbozoomer.jugcraft.wilds;
import net.minecraft.world.level.ClipContext;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.*;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.core.particles.*;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Temporary ground magic; no blocks are replaced or collected, including inside claims. */
public final class WildGeomancy {
    public static EntityType<Fissure> FISSURE;
    public static EntityType<Crack> CRACK;
    public static EntityType<Spike> SPIKE;
    public static java.util.function.Consumer<Fissure> rumble=e->{};
    private WildGeomancy(){}
    private static <T extends Entity> EntityType<T> type(String name,EntityType.EntityFactory<T> factory,float width,float height){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("wilds_"+name));return Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.of(factory,MobCategory.MISC).sized(width,height).clientTrackingRange(10).updateInterval(1).build(key));}
    static void register(){FISSURE=type("fissure",Fissure::new,.8F,.2F);CRACK=type("fissure_crack",Crack::new,4,.1F);SPIKE=type("earth_spike",Spike::new,1.2F,2.2F);}
    static void fire(LivingEntity owner,Vec3 aim){var e=FISSURE.create(owner.level(),EntitySpawnReason.TRIGGERED);if(e==null)return;e.setOwner(owner);e.setPos(owner.position());var v=aim.multiply(1,0,1).normalize().scale(.4);e.setDeltaMovement(v);e.setYRot((float)-Math.toDegrees(Math.atan2(v.x,v.z)));owner.level().addFreshEntity(e);}
    private static boolean allowed(LivingEntity owner,LivingEntity target){if(owner instanceof ServerPlayer p)return target!=owner&&target.isAlive()&&!owner.isAlliedTo(target)&&!(target instanceof WildBluff)&&!(target instanceof TamableAnimal pet&&p.getUUID().equals((pet.getOwnerReference()==null?null:pet.getOwnerReference().getUUID())))&&io.github.jimbozoomer.jugcraft.weapons.TwoHanded.allowed(p,(ServerLevel)p.level(),target);return WildBluff.victim(target)||target instanceof WildSculptor||owner instanceof WildBluff bluff&&bluff.getTarget()==target&&WildSolar.canHarm(owner,target);}
    public static final class Fissure extends Projectile {
        public Fissure(EntityType<? extends Fissure> t,Level l){super(t,l);}
        @Override protected void defineSynchedData(SynchedEntityData.Builder b){}
        @Override public void tick(){super.tick();if(!(level() instanceof ServerLevel l)){if(tickCount==1)rumble.accept(this);return;}if(!(getOwner() instanceof LivingEntity caster)||!caster.isAlive()||l.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL&&caster instanceof WildBluff){discard();return;}
            var next=position().add(getDeltaMovement());var pos=BlockPos.containing(next);if(!l.hasChunkAt(pos)||!l.getWorldBorder().isWithinBounds(pos)){discard();return;}
            var floor=l.clip(new ClipContext(next.add(0,1.1,0),next.add(0,-1.1,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
            if(floor.getType()!=HitResult.Type.BLOCK||floor.isInside()){finish(l,caster);return;}setPos(floor.getLocation());
            if(tickCount%5==1){var piece=CRACK.create(l,EntitySpawnReason.TRIGGERED);if(piece!=null){piece.setPos(position());piece.setYRot(getYRot());l.addFreshEntity(piece);}}
            if(tickCount%2==0)l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,l.getBlockState(blockPosition().below())),getX(),getY()+.05,getZ(),4,.35,.04,.35,.04);
            var hit=new ArrayList<LivingEntity>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),getBoundingBox().inflate(.3,1,.3),e->allowed(caster,e),hit,8);if(!hit.isEmpty()||tickCount>=60)finish(l,caster);
        }
        private void finish(ServerLevel l,LivingEntity caster){var spike=SPIKE.create(l,EntitySpawnReason.TRIGGERED);if(spike!=null){spike.setPos(position());spike.setYRot(getYRot());spike.setOwner(caster);var at=blockPosition().below();var material=l.getBlockState(at);if(material.isAir()||material.hasBlockEntity()||material.getDestroySpeed(l,at)<0||!material.isSolidRender()||!material.getFluidState().isEmpty())material=Blocks.STONE.defaultBlockState();spike.material(material);l.addFreshEntity(spike);}discard();}
        @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putInt("age",tickCount);}
        @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);tickCount=Math.clamp(in.getIntOr("age",0),0,61);}
    }
    public static final class Crack extends Entity {
        public Crack(EntityType<? extends Crack> t,Level l){super(t,l);}
        @Override public boolean hurtServer(ServerLevel l,net.minecraft.world.damagesource.DamageSource s,float amount){return false;}
        @Override protected void defineSynchedData(SynchedEntityData.Builder b){}
        @Override public void tick(){super.tick();if(tickCount>=180&&!level().isClientSide())discard();}
        @Override protected void addAdditionalSaveData(ValueOutput out){out.putInt("age",tickCount);}
        @Override protected void readAdditionalSaveData(ValueInput in){tickCount=Math.clamp(in.getIntOr("age",0),0,180);}
    }
    public static final class Spike extends Projectile implements GeoEntity {
        private static final EntityDataAccessor<BlockState> MATERIAL=SynchedEntityData.defineId(Spike.class,EntityDataSerializers.BLOCK_STATE);
        private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
        private boolean struck;
        public Spike(EntityType<? extends Spike> t,Level l){super(t,l);}
        public BlockState material(){return entityData.get(MATERIAL);}void material(BlockState state){entityData.set(MATERIAL,state);}
        @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(MATERIAL,Blocks.STONE.defaultBlockState());}
        @Override public void tick(){super.tick();if(!(level() instanceof ServerLevel l))return;
            if(tickCount==1)playSound(OwnerWilds.sound("bluff.spike_emerge"),1,1);
            if(tickCount>=3&&!struck){struck=true;if(getOwner() instanceof LivingEntity caster&&caster.isAlive()){
                var victims=new ArrayList<LivingEntity>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),getBoundingBox().inflate(.4),e->allowed(caster,e),victims,32);
                for(var e:victims){if(l.clip(new ClipContext(position().add(0,.2,0),e.getBoundingBox().getCenter(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()!=HitResult.Type.MISS)continue;if(e.hurtServer(l,damageSources().indirectMagic(this,caster),caster instanceof WildBluff?18:10)){if(e.onGround()){double resistance=e.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);e.setDeltaMovement(e.getDeltaMovement().add(0,.15*(1-resistance),0));e.needsSync=true;}}}
            }}
            if(tickCount>=180){l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,material()),getX(),getY()+1,getZ(),20,.6,.8,.6,.08);playSound(OwnerWilds.sound("bluff.spike_explode"),1,1);discard();}
        }
        @Override public void registerControllers(AnimatableManager.ControllerRegistrar out){out.add(new AnimationController<Spike>("emerge",0,s->s.setAndContinue(RawAnimation.begin().thenPlayAndHold("emerge"))));}
        @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
        @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putInt("age",tickCount);out.putBoolean("struck",struck);out.store("material",BlockState.CODEC,material());}
        @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);tickCount=Math.clamp(in.getIntOr("age",0),0,180);struck=in.getBooleanOr("struck",tickCount>=3);material(in.read("material",BlockState.CODEC).orElse(Blocks.STONE.defaultBlockState()));}
    }
}
