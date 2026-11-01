package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.UUID;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** The supplied friendly Wither: original three-headed geometry, no skull attacks or terrain destruction. */
public final class BotanicalPinkWither extends WitherBoss {
    public static EntityType<BotanicalPinkWither> TYPE;
    private UUID owner;
    public BotanicalPinkWither(EntityType<? extends BotanicalPinkWither> type,Level level){super(type,level);setPersistenceRequired();}
    static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("botanical_pink_wither"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<BotanicalPinkWither>of(BotanicalPinkWither::new,MobCategory.CREATURE).sized(.9F,3.5F).clientTrackingRange(10).build(key));FabricDefaultAttributeRegistry.register(TYPE,WitherBoss.createAttributes());}
    @Override protected void registerGoals(){getGoalSelector().addGoal(1,new LookAtPlayerGoal(this,Player.class,8));getGoalSelector().addGoal(2,new RandomLookAroundGoal(this));}
    @Override protected void customServerAiStep(ServerLevel level){
        // Deliberately do not invoke WitherBoss's head attacks, block destruction, spawn explosion or boss bar updates.
        setTarget(null);for(int i=0;i<3;i++)setAlternativeTarget(i,0);setInvulnerableTicks(0);
        if(tickCount%20==0)heal(1);
        var p=Authority.answering(level,owner);if(p==null){setDeltaMovement(getDeltaMovement().scale(.8));return;}
        if(tickCount%60==0){var movement=new Vec3((random.nextDouble()-.5)*.16,(random.nextDouble()-.5)*.06,(random.nextDouble()-.5)*.16);var at=BlockPos.containing(position().add(movement.scale(20)));if(level.hasChunkAt(at)&&Authority.mayChange(level,p,at)&&level.noCollision(this,getBoundingBox().move(movement.scale(20))))setDeltaMovement(movement);else setDeltaMovement(Vec3.ZERO);}
        var next=BlockPos.containing(position().add(getDeltaMovement()));if(!level.hasChunkAt(next)||!Authority.mayChange(level,p,next))setDeltaMovement(Vec3.ZERO);
        if(tickCount%10==0)level.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,getX(),getY()+2.7,getZ(),2,.7,.3,.7,0);
    }
    @Override public void performRangedAttack(LivingEntity target,float power){}
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean playerKill){}
    @Override public void startSeenByPlayer(ServerPlayer p){}
    @Override public void stopSeenByPlayer(ServerPlayer p){}
    @Override public void checkDespawn(){}
    @Override protected InteractionResult mobInteract(Player p,InteractionHand hand){
        if(p.isShiftKeyDown()||p.isSpectator()||!getPassengers().isEmpty())return InteractionResult.PASS;
        if(p instanceof ServerPlayer server){if(!Authority.mayChange(server.level(),server,blockPosition()))return InteractionResult.FAIL;return p.startRiding(this)?InteractionResult.SUCCESS:InteractionResult.FAIL;}return InteractionResult.SUCCESS;
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);if(owner!=null)out.store("botanical_owner",UUIDUtil.CODEC,owner);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);owner=in.read("botanical_owner",UUIDUtil.CODEC).orElse(null);setInvulnerableTicks(0);}
    static final class Pinkinator extends Item {
        Pinkinator(Properties p){super(p);}
        @Override public InteractionResult use(Level level,Player user,InteractionHand hand){
            if(!(user instanceof ServerPlayer p))return InteractionResult.SUCCESS;if(p.isSpectator())return InteractionResult.FAIL;
            var candidates=p.level().getEntitiesOfClass(WitherBoss.class,p.getBoundingBox().inflate(16),w->w.isAlive()&&!(w instanceof BotanicalPinkWither)&&!w.isPassenger()&&!w.isVehicle());candidates.sort(java.util.Comparator.comparingDouble(p::distanceToSqr));
            for(var old:candidates){if(!Authority.mayStrike(p,old)||!Authority.mayChange(p.level(),p,old.blockPosition())||!p.hasLineOfSight(old))continue;
                var pink=new BotanicalPinkWither(TYPE,level);pink.owner=p.getUUID();pink.setPos(old.position());pink.setYRot(old.getYRot());pink.setXRot(old.getXRot());pink.setCustomName(old.getCustomName());pink.setCustomNameVisible(old.isCustomNameVisible());pink.setHealth(pink.getMaxHealth());
                if(!p.level().addFreshEntity(pink))return InteractionResult.FAIL;old.discard();p.getItemInHand(hand).shrink(1);level.playSound(null,pink.blockPosition(),BotanicalSounds.get("pinkinator"),SoundSource.PLAYERS,1,.7F);return InteractionResult.SUCCESS;
            }return InteractionResult.PASS;
        }
    }
}
