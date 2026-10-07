package io.github.jimbozoomer.jugcraft.creatures.scary;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.*;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class PhantomShadowEntity extends Monster {
    private static final EntityDataAccessor<Boolean> CHASING=SynchedEntityData.defineId(PhantomShadowEntity.class,EntityDataSerializers.BOOLEAN);
    private int voiceCooldown=240, attackCooldown;
    private double desiredAltitude=Double.NaN;
    public PhantomShadowEntity(EntityType<? extends PhantomShadowEntity> type,Level level) {
        super(type,level);moveControl=new FlyingMoveControl<>(this,20,true);setNoGravity(true);xpReward=7;
    }
    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,26).add(Attributes.MOVEMENT_SPEED,.25)
            .add(Attributes.FLYING_SPEED,.32).add(Attributes.ATTACK_DAMAGE,5).add(Attributes.FOLLOW_RANGE,28);
    }
    @Override protected PathNavigation createNavigation(Level level) {return new FlyingPathNavigation(this,level);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {super.defineSynchedData(b);b.define(CHASING,false);}
    @Override protected void registerGoals() {
        goalSelector.addGoal(1,new HoverGoal());
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
    }
    public boolean isChasing() {return entityData.get(CHASING);}
    @Override public boolean causeFallDamage(double distance,float multiplier,DamageSource source) {return false;}
    @Override public void travel(Vec3 input) {
        setNoGravity(true);
        moveRelative(.035F,new Vec3(input.x,0,input.z));
        Vec3 v=getDeltaMovement();double vy=v.y;
        if(!level().isClientSide() && !isNoAi() && Double.isFinite(desiredAltitude)) {
            // Navigation uses whole-block nodes. Smooth the actual altitude separately
            // so settling does not oscillate or dip below the intended hover band.
            double wanted=Mth.clamp((desiredAltitude-getY())*.08,-.09,.14);
            vy=vy*.75+wanted*.25;
        }
        setDeltaMovement(v.x,Mth.clamp(vy,-.09,.14),v.z);
        move(MoverType.SELF,getDeltaMovement());
        v=getDeltaMovement();setDeltaMovement(v.x*.91,v.y*.98,v.z*.91);
    }
    // Scan actual support below the ghost, including roofs and cave floors.
    private double groundY() {
        BlockPos.MutableBlockPos p=blockPosition().mutable();
        for(int y=Mth.floor(getY());y>=level().getMinY();y--) {
            p.setY(y);var shape=level().getBlockState(p).getCollisionShape(level(),p);
            if(!shape.isEmpty())return y+shape.max(net.minecraft.core.Direction.Axis.Y);
        }
        return level().getMinY();
    }
    @Override public void tick() {
        super.tick();
        if(!level().isClientSide() && isAlive() && !isNoAi()) {
            var t=getTarget();
            boolean chase=t!=null && t.isAlive() && !t.isSpectator() && !(t instanceof Player p && p.isCreative());
            if(!chase && t!=null)setTarget(null);
            if(chase && !isChasing())voiceCooldown=Math.min(voiceCooldown,10);
            entityData.set(CHASING,chase);
            if(attackCooldown>0)attackCooldown--;
            if(tickCount%(chase?60:100)==0 && getDeltaMovement().lengthSqr()>.002)
                playSound(ScaryMod.SHADOW_CHAIN,.5F,.9F+random.nextFloat()*.2F);
            if(--voiceCooldown<=0) {
                playSound(chase?ScaryMod.SHADOW_CHASE:ScaryMod.SHADOW_AMBIENT,chase?1.2F:.8F,.95F+random.nextFloat()*.1F);
                voiceCooldown=chase?100+random.nextInt(61):300+random.nextInt(241);
            }
        }
    }
    @Override public void playAmbientSound() {}
    @Override protected SoundEvent getHurtSound(DamageSource source) {return ScaryMod.SHADOW_HURT;}
    @Override protected SoundEvent getDeathSound() {return ScaryMod.SHADOW_DEATH;}

    private final class HoverGoal extends Goal {
        private int repath,phase;
        private Vec3 destination;
        HoverGoal() {setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        @Override public boolean canUse() {return true;}
        @Override public boolean requiresUpdateEveryTick() {return true;}
        @Override public void stop() {getNavigation().stop();}
        @Override public void tick() {
            var t=getTarget();phase++;
            boolean attack=t!=null && t.isAlive() && !(t instanceof Player p && (p.isCreative()||p.isSpectator()));
            double ground=groundY();
            // Circle at 3–5 blocks, then spend most of each cycle gliding down to attack.
            double height=4+Math.sin(phase*.025)*.8;
            double dx=getX(),dz=getZ(),dy=ground+height;
            if(attack) {
                dx=t.getX();dz=t.getZ();
                if(phase%240>=65)dy=Math.min(ground+5,t.getY()+.1);
                getLookControl().setLookAt(t,30,30);
                if(getBoundingBox().inflate(.7,.35,.7).intersects(t.getBoundingBox()) && hasLineOfSight(t) && attackCooldown==0) {
                    doHurtTarget(getServerLevel(level()),t);swingForAttack(net.minecraft.world.InteractionHand.MAIN_HAND);
                    playSound(ScaryMod.SHADOW_ATTACK,1,.95F+random.nextFloat()*.1F);attackCooldown=30;
                }
            } else {
                if(destination==null || phase%100==0)destination=position().add(random.nextDouble()*8-4,0,random.nextDouble()*8-4);
                dx=destination.x;dz=destination.z;
            }
            if(--repath<=0) {
                getNavigation().moveTo(dx,dy,dz,attack?1:.55);repath=10;
            }
            desiredAltitude=dy;
            // Vertical takeoff/settling also works when the path has already ended.
            if(getNavigation().isDone())getMoveControl().setWantedPosition(dx,dy,dz,attack?1:.55);
        }
    }
}
