package io.github.jimbozoomer.jugcraft.town;

import io.github.jimbozoomer.jugcraft.artillery.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** City-owned variant of the mining walker: mobile guard, not a recoverable player vehicle. */
public class TownGuardMecha extends Townsfolk {
    private static final EntityDataAccessor<Integer> SHOT = SynchedEntityData.defineId(TownGuardMecha.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> PITCH = SynchedEntityData.defineId(TownGuardMecha.class, EntityDataSerializers.FLOAT);
    private LivingEntity gunTarget;
    private float gunYaw;
    private int lastShot = -100, clientShot = -100;
    private float stride, oldStride, weight, oldWeight;
    private Vec3 previousStridePosition;
    public TownGuardMecha(EntityType<? extends TownGuardMecha> type, Level level) { super(type, level); }
    @Override public boolean isGuard() { return true; }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder); builder.define(SHOT, -100); builder.define(PITCH, 0F);
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (SHOT.equals(key) && level().isClientSide()) clientShot = tickCount;
    }
    public float stride(float partial) { return net.minecraft.util.Mth.lerp(partial, oldStride, stride); }
    public float strideWeight(float partial) { return net.minecraft.util.Mth.lerp(partial, oldWeight, weight); }
    public int sinceShot() { return tickCount - (level().isClientSide() ? clientShot : lastShot); }
    public float gunPitch() { return entityData.get(PITCH); }
    @Override public void tick() {
        super.tick();
        if (!level().isClientSide() && gunTarget != null && gunTarget.isAlive()) {
            setYRot(gunYaw); yBodyRot = gunYaw; yHeadRot = gunYaw;
        }
        oldStride = stride; oldWeight = weight;
        Vec3 now = position();
        double step = previousStridePosition == null ? 0 : now.subtract(previousStridePosition).horizontalDistance();
        previousStridePosition = now;
        if (step > 2) step = 0; // Teleports and initial network placement are not walking.
        stride += (float) Math.min(step, .5);
        weight += ((float)Math.min(1, step * 6) - weight) * .3F;
    }
    @Override protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        Vec3 pivot = position().add(0, 2.3, 0);
        if (tickCount % 20 == 0) gunTarget = TownDefense.target(level, this, pivot);
        if (gunTarget == null || !AutomatedDefense.hostile(gunTarget) || gunTarget.distanceToSqr(pivot) > TownDefense.RANGE * TownDefense.RANGE) return;
        // Navigation/look goals must not reset the cannon's turn every tick or
        // march underneath an airship before the first shot is lined up.
        getNavigation().stop();
        Vec3 delta = gunTarget.getBoundingBox().getCenter().subtract(pivot);
        float yaw = (float)Math.toDegrees(Math.atan2(-delta.x, delta.z));
        gunYaw = net.minecraft.util.Mth.approachDegrees(gunYaw, yaw, 6);
        setYRot(gunYaw); yBodyRot = gunYaw; yHeadRot = gunYaw;
        Float pitch = Ballistics.solve(JugcraftArtillery.FLAK_SPEED, JugcraftArtillery.FLAK_GRAVITY,
                Math.max(1, delta.horizontalDistance() - 2), delta.y, false, -35, 85);
        if (pitch == null) return;
        entityData.set(PITCH, pitch);
        if (tickCount - lastShot < 20 || Math.abs(net.minecraft.util.Mth.wrapDegrees(yaw - gunYaw)) > 5
                || !TownDefense.visible(level, this, pivot, gunTarget.getBoundingBox().getCenter())) return;
        double yr = Math.toRadians(yaw), pr = Math.toRadians(pitch);
        Vec3 dir = new Vec3(-Math.sin(yr) * Math.cos(pr), Math.sin(pr), Math.cos(yr) * Math.cos(pr));
        Vec3 across = new Vec3(Math.cos(yr), 0, Math.sin(yr));
        Vec3 muzzle = pivot.add(across.scale(-1.31)).add(dir.scale(2));
        var shell = new ArtilleryShell(JugcraftArtillery.FLAK_SHELL, level, this);
        shell.setAutomatedDefense(true); shell.setPos(muzzle);
        shell.shoot(dir.x, dir.y, dir.z, (float)JugcraftArtillery.FLAK_SPEED, 0);
        level.addFreshEntity(shell);
        level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 3, .1, .1, .1, .02);
        level.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.NEUTRAL, .5F, 1.8F);
        lastShot = tickCount; entityData.set(SHOT, tickCount);
    }
}
