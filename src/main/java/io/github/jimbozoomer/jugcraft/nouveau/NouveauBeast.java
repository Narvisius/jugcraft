package io.github.jimbozoomer.jugcraft.nouveau;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.*;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.effect.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Wilden attacks telegraph before impact; awakened Wealds share the owner's protection boundary. */
public class NouveauBeast extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(NouveauBeast.class,EntityDataSerializers.INT);
    private final String species;
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bar=new ServerBossEvent(UUID.randomUUID(),Component.literal("Wilden Chimera"),BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS);
    private @Nullable UUID owner;
    private @Nullable BlockPos guard;
    private int attackClock,attackIndex;
    private boolean converted;
    private Vec3 aimed=Vec3.ZERO;
    public NouveauBeast(EntityType<? extends PathfinderMob> t,Level l,String kind){super(t,l);species=kind;installGoals();if(boss())setPersistenceRequired();}
    public static final class Wilden extends NouveauBeast implements Enemy {
        public Wilden(EntityType<? extends PathfinderMob> t,Level l,String kind){super(t,l,kind);}
    }
    public String species(){return species;}
    public boolean boss(){return "wilden_chimera".equals(species);}
    public boolean weald(){return species.contains("weald");}
    public boolean mayCommand(ServerPlayer p){return owner!=null&&(owner.equals(p.getUUID())||io.github.jimbozoomer.jugcraft.party.JugcraftParties.sameParty(owner,p.getUUID()))&&Authority.mayChange(p.level(),p,blockPosition());}
    public boolean waddler(){return species.endsWith("waddler");}
    public void guard(UUID owner,BlockPos pos){this.owner=owner;guard=pos.immutable();setPersistenceRequired();}
    public static AttributeSupplier.Builder attributes(String name){
        boolean boss=name.equals("wilden_chimera"),walker=name.endsWith("walker"),small=name.endsWith("waddler");
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,boss?240:walker?60:small?16:24)
            .add(Attributes.MOVEMENT_SPEED,small?.23:.29).add(Attributes.ATTACK_DAMAGE,boss?12:walker?7:4).add(Attributes.FOLLOW_RANGE,32).add(Attributes.KNOCKBACK_RESISTANCE,boss?.8:walker?.6:0);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(ACTION,0);}
    @Override protected void registerGoals(){}
    private void installGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(5,new WaterAvoidingRandomStrollGoal(this,.7));
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,10));if(waddler())return;
        goalSelector.addGoal(2,new MeleeAttackGoal(this,1,true));targetSelector.addGoal(1,new HurtByTargetGoal(this));
        if(weald())targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,LivingEntity.class,10,true,false,(e,level)->e instanceof Enemy&&!(e instanceof NouveauBeast n&&n.weald())));
        else targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
    }
    @Override protected void customServerAiStep(ServerLevel level){
        if(!JugNouveau.enabled()){setTarget(null);getNavigation().stop();return;}super.customServerAiStep(level);
        if(boss()){bar.setProgress(getHealth()/getMaxHealth());bar.setName(Component.literal(getHealth()>getMaxHealth()*.66F?"Wilden Chimera · Hunter":getHealth()>getMaxHealth()*.33F?"Wilden Chimera · Defender":"Wilden Chimera · Stalker"));}
        if(weald()&&tickCount%100==0)heal(1);
        if(guard!=null&&blockPosition().distSqr(guard)>24*24){setTarget(null);getNavigation().moveTo(guard.getX()+.5,guard.getY(),guard.getZ()+.5,1);return;}
        if(waddler()||getTarget()==null||!getTarget().isAlive()){entityData.set(ACTION,0);attackClock=0;return;}
        var target=getTarget();
        if(++attackClock==60){aimed=target.position();entityData.set(ACTION,boss()?1+Math.floorMod(attackIndex++,getHealth()<getMaxHealth()*.33F?3:2):1);getNavigation().stop();}
        if(attackClock>60&&attackClock<80){getNavigation().stop();if(attackClock%5==0)level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,aimed.x,aimed.y+.2,aimed.z,8,1,.1,1,.01);}
        if(attackClock>=80){attackClock=0;strike(level,target);entityData.set(ACTION,0);}
        if(species.equals("wilden_stalker")&&!onGround())setDeltaMovement(getDeltaMovement().multiply(1,.8,1));
    }
    private void strike(ServerLevel l,LivingEntity target){
        ServerPlayer actor=owner==null?null:Authority.present(l,owner);if(owner!=null&&actor==null)return;
        var cause=Cause.of(owner==null?getUUID():owner,Cause.Origin.CREATURE,"jugcraft:nouveau_"+species,ConcordanceEffects.nextSerial());var ledger=new Ledger(new Ledger.Limits(8,32,0));
        var hit=new ConcordanceEffects.Context(l,cause,actor==null?this:actor,ledger,"impact",position());
        if(boss()){
            int action=entityData.get(ACTION);
            if(action==2){for(int i=-2;i<=2;i++){Vec3 velocity=target.getEyePosition().subtract(getEyePosition()).normalize().yRot(i*.12F).scale(.75);NouveauSpine.launch(l,this,velocity);}return;}
            if(action==3){l.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,aimed.x,aimed.y+.5,aimed.z,1,0,0,0,0);l.playSound(null,BlockPos.containing(aimed),net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),net.minecraft.sounds.SoundSource.HOSTILE,1,.8F);
                for(var witness:l.getEntitiesOfClass(ServerPlayer.class,getBoundingBox().inflate(32),p->p.isAlive()&&!p.isSpectator()&&hasLineOfSight(p)).stream().limit(32).toList())NouveauAdvancements.award(witness,"wilden_explosion");}
            Vec3 toward=aimed.subtract(position());if(toward.lengthSqr()>0)setDeltaMovement(toward.normalize().scale(action==3?1.2:.95).add(0,action==3?1:.15,0));
            for(var e:l.getEntitiesOfClass(LivingEntity.class,new net.minecraft.world.phys.AABB(aimed,aimed).inflate(action==3?3:2),e->e!=this&&e.isAlive()).stream().limit(8).toList())ConcordanceEffects.apply(hit,EffectSpec.of(EffectKind.DAMAGE,Intent.HARMFUL,action==3?12:9,0),e);
        }else if(hasLineOfSight(target)&&distanceToSqr(target)<196){
            if(species.equals("wilden_guardian")){NouveauSpine.launch(l,this,target.getEyePosition().subtract(getEyePosition()).normalize().scale(.65));return;}
            if(species.equals("wilden_hunter")&&tickCount%240<100&&l.getEntitiesOfClass(net.minecraft.world.entity.animal.wolf.Wolf.class,getBoundingBox().inflate(16),Entity::isAlive).size()<4)NouveauSummons.enemyWolf(l,this,target);
            if(species.equals("wilden_stalker")){setDeltaMovement(target.position().subtract(position()).normalize().scale(.7).add(0,.7,0));NouveauSpine.launch(l,this,target.getEyePosition().subtract(getEyePosition()).normalize().scale(.6));}
            boolean applied=ConcordanceEffects.apply(hit,EffectSpec.of(EffectKind.DAMAGE,Intent.HARMFUL,weald()?5:3,0),target).applied();
            if(applied&&actor!=null){
                if(species.startsWith("blazing")){target.igniteForSeconds(4);NouveauStatuses.give(target,"blasting",60,0,actor);}
                if(species.startsWith("cascading")){NouveauStatuses.give(target,"freezing",100,2,actor);target.setTicksFrozen(300);}
                if(species.startsWith("flourishing"))NouveauStatuses.give(target,"snared",80,0,actor);
                if(species.startsWith("vexing")){NouveauStatuses.give(target,"hex",100,0,actor);target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER,100,1),actor);}
            }
        }
    }
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){
        if(player.getItemInHand(hand).is(JugNouveau.WAND))return JugNouveau.WAND.interactLivingEntity(player.getItemInHand(hand),player,this,hand);
        if(player instanceof ServerPlayer p&&waddler()&&mayCommand(p)&&player.getItemInHand(hand).is(NouveauCatalog.item("source_gem"))){var walker=NouveauEncounters.TYPES.get(species.replace("waddler","walker")).create(p.level(),EntitySpawnReason.CONVERSION);if(walker==null)return InteractionResult.FAIL;walker.guard(owner,guard==null?blockPosition():guard);walker.snapTo(getX(),getY(),getZ(),getYRot(),getXRot());if(!p.level().noCollision(walker)||!p.level().addFreshEntity(walker))return InteractionResult.FAIL;player.getItemInHand(hand).consume(1,player);discard();return InteractionResult.SUCCESS;}return super.mobInteract(player,hand);
    }
    @Override public boolean doHurtTarget(ServerLevel l,Entity target){
        if(waddler()||!JugNouveau.enabled()||!(target instanceof LivingEntity living))return false;
        var actor=owner==null?null:Authority.present(l,owner);if(owner!=null&&actor==null)return false;
        var cause=Cause.of(owner==null?getUUID():owner,Cause.Origin.CREATURE,"jugcraft:nouveau_"+species,ConcordanceEffects.nextSerial());
        return ConcordanceEffects.apply(new ConcordanceEffects.Context(l,cause,actor==null?this:actor,new Ledger(new Ledger.Limits(1,4,0)),"melee",position()),EffectSpec.of(EffectKind.DAMAGE,Intent.HARMFUL,(int)getAttributeValue(Attributes.ATTACK_DAMAGE),0),living).applied();
    }
    @Override public boolean hurtServer(ServerLevel l,DamageSource source,float amount){
        if(owner!=null&&source.getEntity() instanceof Player p&&!p.getUUID().equals(owner)&&!Authority.mayStrike((ServerPlayer)p,this))return false;
        if(boss()&&source.is(net.minecraft.tags.DamageTypeTags.IS_FREEZING))amount*=.25F;
        if(species.equals("wilden_guardian")&&source.getEntity()!=null){Vec3 incoming=source.getEntity().position().subtract(position()).normalize();if(getLookAngle().dot(incoming)>.5)amount*=.35F;}
        return super.hurtServer(l,source,amount);
    }
    @Override public void die(DamageSource source){
        if(!converted&&level() instanceof ServerLevel l&&weald()&&!waddler()){
            converted=true;
            var baby=NouveauEncounters.TYPES.get(species.replace("walker","waddler")).create(l,EntitySpawnReason.CONVERSION);if(baby!=null){if(owner!=null)baby.guard(owner,guard==null?blockPosition():guard);baby.snapTo(getX(),getY(),getZ(),getYRot(),0);l.addFreshEntity(baby);}
        }super.die(source);
    }
    @Override protected void dropCustomDeathLoot(ServerLevel l,DamageSource s,boolean player){
        super.dropCustomDeathLoot(l,s,player);if(player&&boss())spawnAtLocation(l,new ItemStack(NouveauCatalog.item("wilden_tribute")));
    }
    @Override public void startSeenByPlayer(ServerPlayer p){super.startSeenByPlayer(p);if(boss())bar.addPlayer(p);}
    @Override public void stopSeenByPlayer(ServerPlayer p){super.stopSeenByPlayer(p);bar.removePlayer(p);}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);if(guard!=null)out.store("guard",BlockPos.CODEC,guard);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);owner=in.read("owner",UUIDUtil.CODEC).orElse(null);guard=in.read("guard",BlockPos.CODEC).orElse(null);}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new AnimationController<NouveauBeast>("body",5,s->{
        String clip=entityData.get(ACTION)>0?(boss()?switch(entityData.get(ACTION)){case 2->"horn_attack";case 3->"dive";default->"charge_prep";}:weald()?"cast":species.equals("wilden_guardian")?"defending":"idle"):
            s.isMoving()?(species.equals("wilden_hunter")||weald()&&!waddler()?"run_master":"run"):"idle";
        if(waddler())clip="run";return s.setAndContinue(RawAnimation.begin().thenLoop(clip));}));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
