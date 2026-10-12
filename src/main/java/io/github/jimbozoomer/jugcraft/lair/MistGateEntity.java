package io.github.jimbozoomer.jugcraft.lair;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The gate of grey mist a ritual opens at its site: for {@code lairs.gate_seconds} anyone who uses it follows into the
 * instance, while there is room. Nobody is pulled in by standing near it. It is never saved (a restart closes the
 * instance too), cannot be harmed, and is drawn only as its mist (particles the server sends): over the Last Rites' grave
 * grey mist and soul flames; in the Frost Horn's snow a whirl of white mist and snowflakes, through which followers fall
 * into the Glacier Hall as the horn's blower did ({@link FrostHornRite#follow}); on the Kiln Seal's magma a vent of sparks
 * and smoke, through which followers sink into the Cinder Kiln ({@link KilnSealRite#follow}).
 */
public class MistGateEntity extends Entity {
	private @Nullable Lair lair;
	private int slot;
	private @Nullable UUID instance;
	private long until;

	public MistGateEntity(EntityType<? extends MistGateEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	/** Opens a gate over {@code grave} into {@code instance} for {@code ticks}. */
	public static MistGateEntity open(ServerLevel level, BlockPos grave, LairInstance instance, int ticks) {
		MistGateEntity gate = new MistGateEntity(JugcraftLairs.MIST_GATE, level);
		gate.lair = instance.lair;
		gate.slot = instance.slot;
		gate.instance = instance.id;
		gate.until = level.getGameTime() + ticks;
		gate.snapTo(grave.getX() + 0.5, grave.getY() + 1.0, grave.getZ() + 0.5, 0.0F, 0.0F);
		level.addFreshEntity(gate);
		instance.gate = gate.getUUID();
		instance.gateUntil = gate.until;
		return gate;
	}

	/** The lair this gate leads into (null only for a gate that was never opened). */
	public @Nullable Lair lair() {
		return lair;
	}

	/** The instance this gate leads into, while it is open. */
	public @Nullable LairInstance instance() {
		LairInstance open = lair == null ? null : Lairs.instance(lair, slot);
		return open != null && open.id.equals(instance) ? open : null;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		boolean snow = lair == Lair.GLACIER_HALL;
		boolean vent = lair == Lair.CINDER_KILN;
		if (instance() == null || level.getGameTime() >= until) {
			if (vent) {
				level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.6, getZ(), 24, 0.4, 0.6, 0.4, 0.03);
			} else {
				level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1.2, getZ(), 24, 0.4, 0.8, 0.4, 0.02);
			}
			if (snow) {
				level.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.6, getZ(), 30, 0.6, 0.5, 0.6, 0.05);
			}
			discard();
			return;
		}
		if (vent) {
			// A vent: smoke welling up from the cracked magma, sparks thrown up out of it.
			level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.2, getZ(), 1, 0.25, 0.1, 0.25, 0.03);
			if (tickCount % 3 == 0) {
				level.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.1, getZ(), 1, 0.3, 0.05, 0.3, 0.0);
				level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.1, getZ(), 2, 0.3, 0.05, 0.3, 0.02);
			}
		} else if (tickCount % 2 == 0) {
			if (snow) {
				// A whirl: three flakes circling and rising round it, white mist welling up in it.
				for (int i = 0; i < 3; i++) {
					double angle = tickCount * 0.35 + i * (Math.PI * 2.0 / 3.0);
					double rise = (tickCount % 30) / 15.0;
					level.sendParticles(ParticleTypes.SNOWFLAKE, getX() + Math.cos(angle) * 0.9, getY() + 0.1 + rise, getZ() + Math.sin(angle) * 0.9,
							1, 0.0, 0.0, 0.0, 0.0);
				}
				level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.4, getZ(), 2, 0.3, 0.3, 0.3, 0.01);
			} else {
				level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1.2, getZ(), 3, 0.35, 0.9, 0.35, 0.0);
				level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.1, getZ(), 1, 0.45, 0.05, 0.45, 0.0);
			}
		}
		if (tickCount % 40 == 0) {
			if (snow) {
				level.playSound(null, blockPosition(), SoundEvents.BREEZE_IDLE_AIR, SoundSource.AMBIENT, 0.8F, 0.6F);
			} else if (vent) {
				level.playSound(null, blockPosition(), SoundEvents.FIRE_AMBIENT, SoundSource.AMBIENT, 1.0F, 0.5F);
			} else {
				level.playSound(null, blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.AMBIENT, 0.6F, 0.6F);
			}
		}
	}

	/** Using the gate: follow into the lair, while it is open and has room. */
	public InteractionResult use(Player player, InteractionHand hand) {
		if (!(player instanceof ServerPlayer server)) {
			return InteractionResult.SUCCESS;
		}
		LairInstance open = instance();
		if (open == null) {
			server.sendOverlayMessage(Component.translatable("message.jugcraft.lair.gate_closed"));
			return InteractionResult.SUCCESS;
		}
		if (open.lair == Lair.GLACIER_HALL) {
			FrostHornRite.follow(server, open);
		} else if (open.lair == Lair.CINDER_KILN) {
			KilnSealRite.follow(server, open);
		} else {
			Lairs.enter(server, open);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean isPickable() {
		return true;
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
}
