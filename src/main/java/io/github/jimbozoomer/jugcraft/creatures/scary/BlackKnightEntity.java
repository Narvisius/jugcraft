package io.github.jimbozoomer.jugcraft.creatures.scary;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.*;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class BlackKnightEntity extends Monster {
    private static final EntityDataAccessor<Boolean> CHASING=SynchedEntityData.defineId(BlackKnightEntity.class,EntityDataSerializers.BOOLEAN);
    private int moanCooldown, sinceMoan=100;
    public BlackKnightEntity(EntityType<? extends BlackKnightEntity> type,Level level) {
        super(type,level);moanCooldown=idleDelay();
    }
    private int idleDelay() {return 360+random.nextInt(241);}
    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,36).add(Attributes.MOVEMENT_SPEED,.22)
            .add(Attributes.ATTACK_DAMAGE,7).add(Attributes.ARMOR,8).add(Attributes.FOLLOW_RANGE,28).add(Attributes.KNOCKBACK_RESISTANCE,.3);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {super.defineSynchedData(b);b.define(CHASING,false);}
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new MeleeAttackGoal(this,1.1,false));
        goalSelector.addGoal(2,new WaterAvoidingRandomStrollGoal(this,.7));
        goalSelector.addGoal(3,new LookAtPlayerGoal(this,Player.class,10));
        goalSelector.addGoal(4,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
    }
    public boolean isChasing() {return entityData.get(CHASING);}
    @Override public boolean doHurtTarget(net.minecraft.server.level.ServerLevel level,Entity target) { boolean hit=super.doHurtTarget(level,target); if(hit)playSound(ScaryMod.KNIGHT_ATTACK,1F,1F); return hit; }
    @Override public void tick() {
        super.tick();
        if(!level().isClientSide() && isAlive()) {
            var target=getTarget();
            boolean chase=target!=null && target.isAlive() && !target.isSpectator() && !(target instanceof Player p && p.isCreative());
            if(chase!=isChasing())moanCooldown=chase?Math.max(0,100-sinceMoan):idleDelay();
            entityData.set(CHASING,chase);sinceMoan=Math.min(100,sinceMoan+1);
            if(moanCooldown>0)moanCooldown--;
            if(moanCooldown==0) {
                playSound(chase?ScaryMod.KNIGHT_CHASE:ScaryMod.KNIGHT_AMBIENT,chase?1.25F:.85F, .96F+random.nextFloat()*.08F);
                sinceMoan=0;moanCooldown=chase?120+random.nextInt(61):idleDelay();
            }
        }
    }
    @Override public void playAmbientSound() {}
    @Override protected SoundEvent getHurtSound(DamageSource source) {return ScaryMod.KNIGHT_HURT;}
    @Override protected SoundEvent getDeathSound() {return ScaryMod.KNIGHT_DEATH;}
    @Override protected void playStepSound(BlockPos pos,BlockState state) {playSound(ScaryMod.KNIGHT_STEP,.45F,.8F+random.nextFloat()*.15F);}
    @Override protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        ScaryDrops.drop(this, level, killedByPlayer, ScaryDrops.Kind.BLACK_KNIGHT);
    }
}
