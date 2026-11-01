package io.github.jimbozoomer.jugcraft.botany;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.*;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Finite original manastorm sequence; absent owners pause instead of bypassing claims. */
public final class BotanicalManaStorm extends Entity {
    public static EntityType<BotanicalManaStorm> TYPE;
    private static final EntityDataAccessor<Integer> COLOR=SynchedEntityData.defineId(BotanicalManaStorm.class,EntityDataSerializers.INT);
    private UUID owner;private int age,fired,dying;
    public BotanicalManaStorm(EntityType<?> type,Level level){super(type,level);setNoGravity(true);}
    static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("botanical_mana_storm"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<BotanicalManaStorm>of(BotanicalManaStorm::new,MobCategory.MISC).sized(.5F,.5F).clientTrackingRange(12).updateInterval(5).build(key));}
    static boolean ignite(ServerLevel l,ServerPlayer p,BlockPos at,int color){
        if(!Authority.mayChange(l,p,at)||!l.getBlockState(at).is(Jugtania.block("mana_bomb"))||l.getEntitiesOfClass(BotanicalManaStorm.class,new AABB(at).inflate(128)).size()>=4)return false;
        var e=new BotanicalManaStorm(TYPE,l);e.owner=p.getUUID();e.entityData.set(COLOR,color);e.setPos(Vec3.atCenterOf(at));
        if(!l.addFreshEntity(e))return false;if(!l.setBlockAndUpdate(at,Blocks.AIR.defaultBlockState())){e.discard();return false;}return true;
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(COLOR,5);}
    @Override public boolean hurtServer(ServerLevel l,DamageSource source,float amount){return false;}
    @Override public void tick(){
        super.tick();if(level().isClientSide()){for(int i=0;i<3;i++)level().addParticle(new DustParticleOptions(net.minecraft.world.item.DyeColor.byId(entityData.get(COLOR)).getTextureDiffuseColor(),1.5F),getX()+(level().getRandom().nextDouble()-.5),getY()+(level().getRandom().nextDouble()-.5),getZ()+(level().getRandom().nextDouble()-.5),0,.01,0);return;}
        var l=(ServerLevel)level();var p=Authority.answering(l,owner);if(p==null)return;if(!Authority.mayChange(l,p,blockPosition())){discard();return;}
        age++;if(fired<250&&age%Math.max(1,30-age/45)==0){BotanicalBurst.storm(p,position(),entityData.get(COLOR));fired++;}
        if(fired>=250&&++dying>=200){
            var center=blockPosition();boolean safe=true;
            for(var pos:BlockPos.betweenClosed(center.offset(-18,-18,-18),center.offset(18,18,18)))if(!l.hasChunkAt(pos)||!l.getWorldBorder().isWithinBounds(pos)||!Authority.mayChange(l,p,pos)){safe=false;break;}
            if(safe)for(var target:l.getEntitiesOfClass(LivingEntity.class,new AABB(center).inflate(18)))if(!Authority.mayStrike(p,target)){safe=false;break;}
            if(safe)l.explode(this,getX(),getY(),getZ(),8,Level.ExplosionInteraction.BLOCK);discard();
        }
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);out.putInt("age",age);out.putInt("fired",fired);out.putInt("dying",dying);out.putInt("color",entityData.get(COLOR));}
    @Override protected void readAdditionalSaveData(ValueInput in){owner=in.read("owner",UUIDUtil.CODEC).orElse(null);age=Math.clamp(in.getIntOr("age",0),0,20000);fired=Math.clamp(in.getIntOr("fired",0),0,250);dying=Math.clamp(in.getIntOr("dying",0),0,200);entityData.set(COLOR,Math.clamp(in.getIntOr("color",5),0,15));}
}
