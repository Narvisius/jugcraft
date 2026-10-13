package io.github.jimbozoomer.jugcraft.concordance.dreaming;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.concordance.dream.DreamRules;
import java.util.UUID;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A dream wisp (roadmap step 22): a small light that drifts round a point near the censer while its dreamer dreams,
 * and only its dreamer can catch it, by touching it ({@link Dreaming#catchWisp}). It is never saved and fades the moment
 * its dreamer is not dreaming or not here, so no wisp outlives its dream. Nothing can pick or strike it (no projectile
 * stops at a wisp). It has no brain: it only drifts (SmartBrainLib is for creatures that decide). GeckoLib draws it.
 */
public class DreamWispEntity extends Entity implements GeoEntity {
	private static final RawAnimation DRIFT = RawAnimation.begin().thenLoop("animation.dream_wisp.drift");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable UUID dreamer;
	private Vec3 anchor = Vec3.ZERO;
	private float phase;

	public DreamWispEntity(EntityType<? extends DreamWispEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	/** Gathers a wisp for {@code dreamer} drifting round {@code anchor}. */
	public static DreamWispEntity spawn(ServerLevel level, ServerPlayer dreamer, Vec3 anchor, float phase) {
		DreamWispEntity wisp = new DreamWispEntity(Dreaming.WISP, level);
		wisp.dreamer = dreamer.getUUID();
		wisp.anchor = anchor;
		wisp.phase = phase;
		wisp.snapTo(anchor.x, anchor.y, anchor.z, 0.0F, 0.0F);
		level.addFreshEntity(wisp);
		return wisp;
	}

	public @Nullable UUID dreamer() {
		return dreamer;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		ServerPlayer owner = dreamer == null ? null : level.getServer().getPlayerList().getPlayer(dreamer);
		if (owner == null || owner.level() != level || !Dreaming.dreaming(owner) || tickCount > DreamRules.MAX_TICKS + 200) {
			discard();
			return;
		}
		double t = (tickCount + phase * 200.0) * 0.04;
		setPos(anchor.x + Math.cos(t) * 2.5, anchor.y + Math.sin(t * 1.7) * 0.6, anchor.z + Math.sin(t) * 2.5);
		if (owner.getBoundingBox().inflate(0.5).intersects(getBoundingBox())) {
			Dreaming.catchWisp(owner, this);
		}
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
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
		controllers.add(new AnimationController<DreamWispEntity>("main", 5, test -> test.setAndContinue(DRIFT)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
