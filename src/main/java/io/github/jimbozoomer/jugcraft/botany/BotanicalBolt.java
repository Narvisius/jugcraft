package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.*;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.*;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Server-authoritative botanical weapon flights; no explosions or terrain griefing. */
public final class BotanicalBolt extends Projectile {
    public static EntityType<BotanicalBolt> TYPE;
    private static final EntityDataAccessor<Integer> STYLE=SynchedEntityData.defineId(BotanicalBolt.class,EntityDataSerializers.INT);
    private boolean enteredAir;
    public BotanicalBolt(EntityType<? extends Projectile> type,Level level){super(type,level);setNoGravity(true);}
    static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("botanical_bolt"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<BotanicalBolt>of(BotanicalBolt::new,MobCategory.MISC).sized(.25F,.25F).clientTrackingRange(12).updateInterval(1).build(key));}
    public static boolean launch(ServerPlayer player,Vec3 from,Vec3 motion,boolean star){
        if(!player.level().hasChunkAt(BlockPos.containing(from)))return false;var bolt=new BotanicalBolt(TYPE,player.level());bolt.setOwner(player);bolt.entityData.set(STYLE,star?1:0);bolt.setPos(from);bolt.setDeltaMovement(motion);return player.level().addFreshEntity(bolt);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){builder.define(STYLE,0);}
    @Override public void tick(){
        super.tick();boolean star=entityData.get(STYLE)==1;
        if(level().isClientSide()){var d=getDeltaMovement();for(int i=0;i<5;i++)level().addParticle(new DustParticleOptions(star?0xFF75EC:0x20FF20,.8F),getX()-d.x*i/5,getY()-d.y*i/5,getZ()-d.z*i/5,0,0,0);return;}
        if(!(level() instanceof ServerLevel level)||!(getOwner() instanceof ServerPlayer p)||!p.isAlive()||p.isSpectator()||tickCount>65){discard();return;}
        Vec3 from=position(),next=from.add(getDeltaMovement());
        int steps=Math.max(1,(int)Math.ceil(from.distanceTo(next)*2));for(int i=0;i<=steps;i++){var at=BlockPos.containing(from.lerp(next,i/(double)steps));if(!level.hasChunkAt(at)||!level.getWorldBorder().isWithinBounds(at)){discard();return;}}
        if(level.getBlockState(blockPosition()).isAir())enteredAir=true;
        var hit=level.clip(new ClipContext(from,next,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
        Vec3 end=hit.getType()==HitResult.Type.BLOCK&&(!star||enteredAir)?hit.getLocation():next;
        LivingEntity closest=null;double distance=Double.MAX_VALUE;
        for(var target:level.getEntitiesOfClass(LivingEntity.class,new AABB(from,end).inflate(star?.5:1),e->e!=p&&e.isAlive())){
            var intersection=target.getBoundingBox().inflate(star?.25:.5).clip(from,end);if(intersection.isEmpty())continue;double d=from.distanceToSqr(intersection.get());
            if(d<distance&&Authority.mayStrike(p,target)&&Authority.mayChange(level,p,target.blockPosition())){closest=target;distance=d;}
        }
        if(closest!=null){closest.hurtServer(level,level.damageSources().playerAttack(p),star?(random.nextFloat()<.25?10:5):8);discard();return;}
        setPos(end);if(hit.getType()==HitResult.Type.BLOCK&&(!star||enteredAir))discard();
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putInt("style",entityData.get(STYLE));out.putBoolean("entered_air",enteredAir);out.putInt("age",tickCount);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);entityData.set(STYLE,Math.clamp(in.getIntOr("style",0),0,1));enteredAir=in.getBooleanOr("entered_air",false);tickCount=Math.clamp(in.getIntOr("age",66),0,66);}
}
