package io.github.jimbozoomer.jugcraft.nouveau;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.effect.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import java.util.UUID;

/** A visible, colliding Wilden projectile with the supplied spike geometry. */
public final class NouveauSpine extends Entity implements GeoEntity {
    public static EntityType<NouveauSpine> TYPE;
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private UUID shooter;private int damage=4;
    public NouveauSpine(EntityType<?> type,Level level){super(type,level);setNoGravity(true);}
    public static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("nouveau_chimera_spike"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<NouveauSpine>of(NouveauSpine::new,MobCategory.MISC).sized(.25F,.25F).clientTrackingRange(8).updateInterval(1).build(key));}
    public static void launch(ServerLevel level,NouveauBeast owner,Vec3 velocity){
        if(level.getEntitiesOfClass(NouveauSpine.class,owner.getBoundingBox().inflate(40),Entity::isAlive).size()>=48)return;
        var spike=TYPE.create(level,EntitySpawnReason.TRIGGERED);if(spike==null)return;spike.shooter=owner.getUUID();spike.damage=owner.boss()?8:4;spike.snapTo(owner.getEyePosition());spike.setDeltaMovement(velocity);
        spike.setYRot((float)(Math.atan2(velocity.x,velocity.z)*180/Math.PI));spike.setXRot((float)(-Math.atan2(velocity.y,velocity.horizontalDistance())*180/Math.PI));level.addFreshEntity(spike);
    }
    @Override public void tick(){super.tick();if(!(level() instanceof ServerLevel level))return;
        var source=shooter==null?null:level.getEntity(shooter);if(tickCount>40||!JugNouveau.enabled()||source==null||!source.isAlive()){discard();return;}
        var next=position().add(getDeltaMovement());if(!level.hasChunkAt(BlockPos.containing(next))){discard();return;}
        var block=level.clip(new ClipContext(position(),next,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));var stop=block.getType()==HitResult.Type.BLOCK?block.getLocation():next;
        var targets=level.getEntitiesOfClass(LivingEntity.class,new AABB(position(),stop).inflate(.3),e->e!=source&&e.isAlive()&&!(e instanceof NouveauBeast));
        targets.sort(java.util.Comparator.comparingDouble(e->e.distanceToSqr(position())));
        for(var target:targets){if(target.getBoundingBox().inflate(.15).clip(position(),stop).isEmpty()&&!target.getBoundingBox().inflate(.15).contains(position()))continue;
            var cause=Cause.of(shooter,Cause.Origin.CREATURE,"jugcraft:nouveau_wilden_spine",ConcordanceEffects.nextSerial());ConcordanceEffects.apply(new ConcordanceEffects.Context(level,cause,source,new Ledger(new Ledger.Limits(1,4,0)),"spine",position()),EffectSpec.of(EffectKind.DAMAGE,Intent.HARMFUL,damage,0),target);discard();return;
        }
        if(block.getType()==HitResult.Type.BLOCK){discard();return;}setPos(next);
    }
    @Override public boolean hurtServer(ServerLevel level,net.minecraft.world.damagesource.DamageSource source,float damage){return false;}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){}
    @Override protected void addAdditionalSaveData(ValueOutput out){if(shooter!=null)out.store("shooter",UUIDUtil.CODEC,shooter);out.putInt("damage",damage);out.putInt("age",tickCount);}
    @Override protected void readAdditionalSaveData(ValueInput in){shooter=in.read("shooter",UUIDUtil.CODEC).orElse(null);damage=Math.clamp(in.getIntOr("damage",4),1,12);tickCount=in.getIntOr("age",40);}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
