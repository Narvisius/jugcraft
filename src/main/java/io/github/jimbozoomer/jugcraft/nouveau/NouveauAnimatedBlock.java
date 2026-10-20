package io.github.jimbozoomer.jugcraft.nouveau;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.effect.*;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import java.util.*;

/** A moved real block, with one persisted return path; never a generated duplicate of its material. */
public final class NouveauAnimatedBlock extends PathfinderMob {
    public static EntityType<NouveauAnimatedBlock> TYPE;
    private static final EntityDataAccessor<BlockState> BLOCK=SynchedEntityData.defineId(NouveauAnimatedBlock.class,EntityDataSerializers.BLOCK_STATE);
    private UUID owner;private long expires;private boolean returned;
    private boolean projectile;private BlockPos origin;private long movedAt;private int force;
    private ConcordanceEffects.Context casting;
    public static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("nouveau_animated_block"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<NouveauAnimatedBlock>of(NouveauAnimatedBlock::new,MobCategory.MISC).sized(.95F,.95F).clientTrackingRange(10).build(key));FabricDefaultAttributeRegistry.register(TYPE,Mob.createMobAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.MOVEMENT_SPEED,.3).add(Attributes.FOLLOW_RANGE,16));}
    public NouveauAnimatedBlock(EntityType<? extends PathfinderMob> type,Level l){super(type,l);setPersistenceRequired();}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(BLOCK,Blocks.STONE.defaultBlockState());}
    public BlockState block(){return entityData.get(BLOCK);}
    public boolean belongs(ServerPlayer p){return p.getUUID().equals(owner);}
    public static NouveauAnimatedBlock moved(ServerLevel l,ServerPlayer p,BlockPos origin){return l.getEntitiesOfClass(NouveauAnimatedBlock.class,new net.minecraft.world.phys.AABB(origin).inflate(3),e->e.belongs(p)&&origin.equals(e.origin)&&e.movedAt==l.getGameTime()).stream().findFirst().orElse(null);}
    public static boolean launch(ConcordanceEffects.Context c,ServerPlayer p,BlockPos at,int duration,net.minecraft.world.phys.Vec3 motion,int power){
        var l=c.level();if(l.getEntitiesOfClass(NouveauAnimatedBlock.class,p.getBoundingBox().inflate(48),e->e.belongs(p)).size()>=16)return false;
        var state=l.getBlockState(at);if(state.isAir()||state.hasBlockEntity()||state.getDestroySpeed(l,at)<0||!Authority.mayChange(l,p,at))return false;
        var entity=TYPE.create(l,EntitySpawnReason.TRIGGERED);if(entity==null)return false;entity.owner=p.getUUID();entity.expires=l.getGameTime()+Math.clamp(duration,100,2400);entity.entityData.set(BLOCK,state);entity.projectile=true;entity.casting=c;entity.force=power;entity.origin=at.immutable();entity.movedAt=l.getGameTime();entity.snapTo(net.minecraft.world.phys.Vec3.atBottomCenterOf(at));entity.setDeltaMovement(motion);
        if(!l.addFreshEntity(entity))return false;l.removeBlock(at,false);return true;
    }
    public void change(BlockState state){if(!state.hasBlockEntity()&&state.getDestroySpeed(level(),blockPosition())>=0)entityData.set(BLOCK,state);}
    public void breakApart(ServerPlayer p){if(!belongs(p)||returned)return;returned=true;Block.getDrops(block(),p.level(),blockPosition(),null,p,p.getMainHandItem()).forEach(drop->Block.popResource(p.level(),blockPosition(),drop));discard();}
    public static boolean animate(ServerLevel l,ServerPlayer p,BlockPos at,int duration){
        if(l.getEntitiesOfClass(NouveauAnimatedBlock.class,p.getBoundingBox().inflate(32),e->p.getUUID().equals(e.owner)).size()>=8)return false;
        var state=l.getBlockState(at);if(state.isAir()||state.hasBlockEntity()||state.getDestroySpeed(l,at)<0)return false;
        var mob=TYPE.create(l,EntitySpawnReason.MOB_SUMMONED);if(mob==null)return false;mob.owner=p.getUUID();mob.expires=l.getGameTime()+Math.clamp(duration,100,2400);mob.entityData.set(BLOCK,state);mob.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
        if(!l.addFreshEntity(mob))return false;l.removeBlock(at,false);return true;
    }
    @Override protected void customServerAiStep(ServerLevel l){
        super.customServerAiStep(l);
        if(projectile){navigation.stop();if(l.getGameTime()>=expires||tickCount>5&&onGround()&&getDeltaMovement().lengthSqr()<.01){restore(l);return;}
            var p=Authority.answering(l,owner);if(p!=null&&casting!=null&&getDeltaMovement().lengthSqr()>.08&&tickCount%2==0){int damage=Math.clamp((int)(getDeltaMovement().length()*(2+Math.max(0,block().getDestroySpeed(l,blockPosition())))+force),1,20);
                for(var target:l.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(.15),e->e!=this&&e!=p&&e.isAlive()).stream().limit(4).toList()){var hit=new ConcordanceEffects.Context(l,casting.cause(),p,casting.ledger(),casting.step()+"/moving_block",position());if(ConcordanceEffects.apply(hit,EffectSpec.of(EffectKind.DAMAGE,Intent.HARMFUL,damage,0),target)==ConcordanceEffects.Result.APPLIED&&isOnFire())target.igniteForSeconds(3);}}
            return;}
        if(tickCount%10!=0)return;
        if(l.getGameTime()>=expires||!JugNouveau.enabled()){restore(l);return;}var p=Authority.answering(l,owner);if(p==null){navigation.stop();return;}
        var target=l.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(12),e->e instanceof Enemy&&e.isAlive()&&ConcordanceEffects.mayHarm(p,e)&&Authority.mayStrike(p,e)).stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if(target==null){if(distanceToSqr(p)>9)navigation.moveTo(p,1);return;}navigation.moveTo(target,1.2);
        if(tickCount%20==0&&distanceToSqr(target)<4){var cause=Cause.of(owner,Cause.Origin.CREATURE,"jugcraft:nouveau_animated_block",ConcordanceEffects.nextSerial());ConcordanceEffects.apply(new ConcordanceEffects.Context(l,cause,p,new Ledger(new Ledger.Limits(1,4,0)),"slam",position()),EffectSpec.of(EffectKind.DAMAGE,Intent.HARMFUL,5,0),target);}
    }
    public void settle(ServerLevel l){restore(l);}
    private void restore(ServerLevel l){
        if(returned)return;returned=true;var actor=Authority.answering(l,owner);var pos=blockPosition();
        if(actor!=null&&l.hasChunkAt(pos)&&Authority.mayChange(l,actor,pos)&&l.getBlockState(pos).canBeReplaced())net.minecraft.world.entity.item.FallingBlockEntity.fall(l,pos,block());
        else Block.popResource(l,pos,new ItemStack(block().getBlock()));discard();
    }
    @Override public void die(net.minecraft.world.damagesource.DamageSource source){if(level() instanceof ServerLevel l)restore(l);}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);out.store("block",BlockState.CODEC,block());out.putLong("expires",expires);out.putBoolean("returned",returned);out.putBoolean("projectile",projectile);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);owner=in.read("owner",UUIDUtil.CODEC).orElse(null);entityData.set(BLOCK,in.read("block",BlockState.CODEC).orElse(Blocks.STONE.defaultBlockState()));expires=in.getLongOr("expires",0);returned=in.getBooleanOr("returned",false);projectile=in.getBooleanOr("projectile",false);}
}
