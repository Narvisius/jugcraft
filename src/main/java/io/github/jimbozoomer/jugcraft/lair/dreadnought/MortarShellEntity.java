package io.github.jimbozoomer.jugcraft.lair.dreadnought;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A mortar shell (docs/features/iron-dreadnought.md): lobbed from the mortar on the Iron Dreadnought's back. It flies a
 * fixed arc, {@value #SHELL_PEAK} blocks higher than its ends, from his back to the mark the red smoke showed, over
 * {@value IronDreadnoughtEntity#SHELL_FLIGHT} ticks, and lands on it: {@value IronDreadnoughtEntity#MORTAR_DAMAGE} within
 * {@value IronDreadnoughtEntity#MORTAR_REACH} blocks ({@link IronDreadnoughtEntity#shellLands}). It passes over cover, breaks
 * no block, goes with him, and is never saved.
 */
public class MortarShellEntity extends Entity implements GeoEntity {
	/** Where it leaves his back: behind his feet and over them (tools/iron_dreadnought.py SHELL). */
	public static final double MORTAR_UP = 5.0625;
	public static final double MORTAR_BACK = 0.5;
	public static final double SHELL_PEAK = 8.0;
	private static final RawAnimation FLIGHT = RawAnimation.begin().thenPlayAndHold("animation.mortar_shell.flight");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable UUID owner;
	private Vec3 from = Vec3.ZERO;
	private Vec3 mark = Vec3.ZERO;
	private int flight = IronDreadnoughtEntity.SHELL_FLIGHT;
	private int age;

	public MortarShellEntity(EntityType<? extends MortarShellEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	/** {@code dreadnought} lobs a shell from {@code from} onto {@code mark}, landing {@code flight} ticks later. */
	public static MortarShellEntity lob(ServerLevel level, IronDreadnoughtEntity dreadnought, Vec3 from, Vec3 mark, int flight) {
		MortarShellEntity shell = new MortarShellEntity(JugcraftDreadnought.MORTAR_SHELL, level);
		shell.owner = dreadnought.getUUID();
		shell.from = from;
		shell.mark = mark;
		shell.flight = Math.max(1, flight);
		shell.snapTo(from.x, from.y, from.z, 0.0F, 0.0F);
		Vec3 way = mark.subtract(from);
		shell.setYRot((float) (Math.toDegrees(Math.atan2(way.z, way.x)) - 90.0));
		level.addFreshEntity(shell);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, from.x, from.y, from.z, 12, 0.3, 0.3, 0.3, 0.05);
		level.playSound(null, from.x, from.y, from.z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.HOSTILE, 3.0F, 0.4F);
		return shell;
	}

	public Vec3 mark() {
		return mark;
	}

	/** Where it is on its arc, {@code share} of the way along. */
	public Vec3 along(double share) {
		Vec3 line = from.add(mark.subtract(from).scale(share));
		return line.add(0.0, SHELL_PEAK * 4.0 * share * (1.0 - share), 0.0);
	}

	private @Nullable IronDreadnoughtEntity dreadnought(ServerLevel level) {
		return owner != null && level.getEntity(owner) instanceof IronDreadnoughtEntity found && found.isAlive() ? found : null;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		IronDreadnoughtEntity dreadnought = dreadnought(level);
		if (dreadnought == null) {
			discard();
			return;
		}
		age++;
		Vec3 at = along(Math.min(1.0, age / (double) flight));
		setPos(at.x, at.y, at.z);
		if (age % 2 == 0) {
			level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.2, getZ(), 2, 0.05, 0.05, 0.05, 0.0);
		}
		if (age >= flight) {
			land(level, dreadnought);
		}
	}

	/** It lands on the mark and bursts. */
	public void land(ServerLevel level, IronDreadnoughtEntity dreadnought) {
		dreadnought.shellLands(level, this, mark);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, mark.x, mark.y + 0.5, mark.z, 1, 0.0, 0.0, 0.0, 0.0);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, mark.x, mark.y + 0.5, mark.z, 24, 1.2, 0.6, 1.2, 0.04);
		level.playSound(null, mark.x, mark.y, mark.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3.0F, 0.8F);
		discard();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<MortarShellEntity>("main", 0, test -> test.setAndContinue(FLIGHT)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
