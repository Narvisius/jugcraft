package io.github.jimbozoomer.jugcraft.lair.hag;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.LoopType;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A clump of the Mire Hag's roots (docs/features/mire-hag.md): her Root Lanes burst a row of these up out of the water
 * along each lane, a clump every {@value MireHagEntity#CLUMP_STEP} blocks; they stand through the attack's active ticks,
 * then sink for {@value #SINK} ticks and are gone. What the roots do is decided as they burst ({@link MireHagEntity#roots}):
 * the clumps are what players see, and nothing else. They cannot be harmed, go with her, and are never saved.
 *
 * <p>Whoever the roots burst under is held fast ({@link #hold}): Slowness VII, under which nobody moves, and no jumping.
 * Stopping a player's jump is a transient change to their jump strength, which they get back the moment the hold ends,
 * however it ends, and on every restart.
 */
public class HagRootsEntity extends Entity implements GeoEntity {
	public static final int SINK = 10;
	/** Slowness at this amplifier (VII) takes all of a player's speed. */
	public static final int HELD_AMPLIFIER = 6;
	private static final Identifier NO_JUMP = Jugcraft.id("mire_roots");
	/** Who her roots hold, and the ticks left before they are let go. */
	private static final Map<UUID, Integer> HELD = new HashMap<>();
	private static final EntityDataAccessor<Boolean> SINKING = SynchedEntityData.defineId(HagRootsEntity.class, EntityDataSerializers.BOOLEAN);
	private static final RawAnimation STAND = RawAnimation.begin().then("animation.hag_roots.burst", LoopType.PLAY_ONCE)
			.thenLoop("animation.hag_roots.stand");
	private static final RawAnimation SINK_DOWN = RawAnimation.begin().thenPlayAndHold("animation.hag_roots.sink");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable UUID owner;
	private int active;

	public HagRootsEntity(EntityType<? extends HagRootsEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	static void register() {
		ServerTickEvents.END_SERVER_TICK.register(HagRootsEntity::release);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> HELD.clear());
	}

	/** A clump of {@code hag}'s roots bursts up at {@code at} (on the water) and stands {@code active} ticks. */
	public static HagRootsEntity burst(ServerLevel level, MireHagEntity hag, Vec3 at, int active) {
		HagRootsEntity roots = new HagRootsEntity(JugcraftHag.HAG_ROOTS, level);
		roots.owner = hag.getUUID();
		roots.active = active;
		roots.snapTo(at.x, at.y - 0.2, at.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
		level.addFreshEntity(roots);
		level.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 0.1, at.z, 6, 0.3, 0.1, 0.3, 0.1);
		return roots;
	}

	public boolean sinking() {
		return entityData.get(SINKING);
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		boolean bound = owner != null && level.getEntity(owner) instanceof MireHagEntity hag && hag.isAlive();
		if (!bound || tickCount > active + SINK) {
			discard();
			return;
		}
		if (tickCount > active && !sinking()) {
			entityData.set(SINKING, true);
		}
	}

	/** Holds {@code player} fast for {@code ticks}: they cannot move or jump until it ends. */
	public static void hold(Player player, int ticks) {
		player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, HELD_AMPLIFIER, false, false, true));
		AttributeInstance jump = player.getAttribute(Attributes.JUMP_STRENGTH);
		if (jump != null && !jump.hasModifier(NO_JUMP)) {
			jump.addTransientModifier(new AttributeModifier(NO_JUMP, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
		HELD.merge(player.getUUID(), ticks, Math::max);
	}

	/** Whether her roots hold {@code player} fast now. */
	public static boolean held(Player player) {
		AttributeInstance jump = player.getAttribute(Attributes.JUMP_STRENGTH);
		return jump != null && jump.hasModifier(NO_JUMP);
	}

	/** Lets {@code player} go at once: they can move and jump again (a lesser Slowness, her Mire Grasp's, stays). */
	public static void free(Player player) {
		HELD.remove(player.getUUID());
		MobEffectInstance slowness = player.getEffect(MobEffects.SLOWNESS);
		if (slowness != null && slowness.getAmplifier() == HELD_AMPLIFIER) {
			player.removeEffect(MobEffects.SLOWNESS);
		}
		AttributeInstance jump = player.getAttribute(Attributes.JUMP_STRENGTH);
		if (jump != null) {
			jump.removeModifier(NO_JUMP);
		}
	}

	/** Each tick, whoever her roots have held long enough is let go. */
	private static void release(MinecraftServer server) {
		if (HELD.isEmpty()) {
			return;
		}
		HELD.replaceAll((id, ticks) -> ticks - 1);
		HELD.entrySet().removeIf(entry -> {
			if (entry.getValue() > 0) {
				return false;
			}
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player != null) {
				AttributeInstance jump = player.getAttribute(Attributes.JUMP_STRENGTH);
				if (jump != null) {
					jump.removeModifier(NO_JUMP);
				}
			}
			return true;
		});
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
		builder.define(SINKING, false);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<HagRootsEntity>("main", 0,
				test -> test.setAndContinue(test.animatable().sinking() ? SINK_DOWN : STAND)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
