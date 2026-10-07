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

public final class SpaceKookEntity extends Monster {
    private static final EntityDataAccessor<Boolean> CHASING = SynchedEntityData.defineId(SpaceKookEntity.class, EntityDataSerializers.BOOLEAN);
    private int laughCooldown;
    private int ticksSinceLaugh = 80;
    public SpaceKookEntity(EntityType<? extends SpaceKookEntity> type, Level level) {
        super(type, level);
        laughCooldown = passiveLaughDelay();
    }
    private int passiveLaughDelay() { return 400 + random.nextInt(201); }
    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 36).add(Attributes.MOVEMENT_SPEED, .25)
            .add(Attributes.ATTACK_DAMAGE, 6).add(Attributes.FOLLOW_RANGE, 28).add(Attributes.ARMOR, 5)
            .add(Attributes.KNOCKBACK_RESISTANCE, .45);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { super.defineSynchedData(builder); builder.define(CHASING, false); }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15, false));
        goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, .7));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 12));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }
    public boolean isChasing() { return entityData.get(CHASING); }
    @Override public void tick() {
        super.tick();
        if (!level().isClientSide() && isAlive()) {
            LivingEntity target = getTarget();
            boolean chase = target != null && target.isAlive() && !target.isSpectator()
                && !(target instanceof Player p && p.isCreative());
            // Start a chase laugh promptly, while leaving four seconds between clip starts.
            // Returning to idle restores the longer passive interval.
            if (chase != isChasing()) {
                laughCooldown = chase ? Math.max(0, 80 - ticksSinceLaugh) : passiveLaughDelay();
            }
            entityData.set(CHASING, chase);
            ticksSinceLaugh = Math.min(80, ticksSinceLaugh + 1);
            if (laughCooldown > 0) laughCooldown--;
            if (laughCooldown == 0) {
                playSound(ScaryMod.LAUGH, chase ? 1.6F : 1.25F, chase ? 1F : .96F);
                ticksSinceLaugh = 0;
                laughCooldown = chase ? 100 + random.nextInt(61) : passiveLaughDelay();
            }
        }
    }
    // tick() owns both intervals; vanilla's ambient scheduler must not add extra laughs.
    @Override public void playAmbientSound() {}
    @Override protected SoundEvent getHurtSound(DamageSource source) { return ScaryMod.HURT; }
    @Override protected SoundEvent getDeathSound() { return ScaryMod.DEATH; }
    @Override protected void playStepSound(BlockPos pos, BlockState block) { playSound(ScaryMod.STEP, .35F, .9F + random.nextFloat()*.15F); }
    @Override protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        ScaryDrops.drop(this, level, killedByPlayer, ScaryDrops.Kind.SPACE_KOOK);
    }
}
