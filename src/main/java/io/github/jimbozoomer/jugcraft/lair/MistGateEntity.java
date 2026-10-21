package io.github.jimbozoomer.jugcraft.lair;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
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
 * and smoke, through which followers sink into the Cinder Kiln ({@link KilnSealRite#follow}); and where the Hag Stone was
 * looked through, mist rising from the water with a marsh-light bobbing in it, which closes round followers and takes them
 * to the Lantern Fen ({@link HagStoneRite#follow}); over a sarcophagus the Writ of Exhumation opened, grey mist and
 * cold blue fire welling up out of the dark under its lid, souls rising in it, down which followers go to the Archive of
 * Last Names ({@link ExhumationRite#follow}); and over a Field Telephone Sealed Orders were used on, the open line:
 * sparks crackling off it and a wisp of smoke, its bell ringing, down which followers go over the top into the Foundry
 * Trench ({@link OrdersRite#follow}); and where the Howling Totem howled, Grey Mist curling up out of the ground, pale
 * ash drifting in it like moonlit motes and a howl answering far off now and then, which closes round followers and
 * brings them to in the Moonlit Ring's camp ({@link HowlingTotemRite#follow}); and red mist over the Ancient Altar
 * leads to Mars ({@link AltarKeystoneRite#follow}).
 */
public class MistGateEntity extends Entity {
	/** How often a howl answers from far off through the Howling Totem's mist (ticks). */
	private static final int HOWL_TICKS = 120;
	/** The red mist's dust and its column of light (the Cydonian Sanctum's gate). */
	private static final DustParticleOptions RED_MIST = new DustParticleOptions(0xC0402A, 1.6F);
	private static final DustParticleOptions RED_LIGHT = new DustParticleOptions(0xFF5A2A, 2.4F);
	/** How high the red light's column rises over the altar (blocks). */
	private static final int COLUMN = 24;
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
		boolean fen = lair == Lair.LANTERN_FEN;
		boolean tomb = lair == Lair.CRYPT_ARCHIVE;
		boolean line = lair == Lair.FOUNDRY_TRENCH;
		boolean howl = lair == Lair.MOONLIT_RING;
		boolean mars = lair == Lair.CYDONIAN_SANCTUM;
		if (instance() == null || level.getGameTime() >= until) {
			if (mars) {
				level.sendParticles(RED_MIST, getX(), getY() + 1.0, getZ(), 40, 0.6, 0.8, 0.6, 0.02);
			} else if (vent) {
				level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.6, getZ(), 24, 0.4, 0.6, 0.4, 0.03);
			} else if (line) {
				// The line goes dead: a last crackle off the telephone.
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() - 0.3, getZ(), 12, 0.3, 0.2, 0.3, 0.05);
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
		} else if (fen) {
			// Mist rising off the still water, and in it a marsh-light bobbing and circling slowly.
			if (tickCount % 2 == 0) {
				level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 2, 0.5, 0.3, 0.5, 0.0);
			}
			double angle = tickCount * 0.08;
			level.sendParticles(ParticleTypes.GLOW, getX() + Math.cos(angle) * 0.4, getY() + 1.3 + Math.sin(tickCount * 0.15) * 0.25,
					getZ() + Math.sin(angle) * 0.4, 1, 0.0, 0.0, 0.0, 0.0);
		} else if (mars) {
			// The red mist over the altar, and a column of red light rising out of it into the sky.
			if (tickCount % 2 == 0) {
				level.sendParticles(RED_MIST, getX(), getY() + 0.8, getZ(), 3, 0.45, 0.6, 0.45, 0.0);
				level.sendParticles(RED_LIGHT, getX(), getY() + 2.0 + level.getRandom().nextDouble() * COLUMN, getZ(), 2, 0.12, 1.0, 0.12, 0.0);
			}
			if (tickCount % 5 == 0) {
				level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.5, getZ(), 1, 0.1, 0.2, 0.1, 0.06);
			}
		} else if (tomb) {
			// The open tomb: grey mist and cold blue fire welling up out of it, and now and then a soul rising.
			if (tickCount % 2 == 0) {
				level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.2, getZ(), 2, 0.4, 0.15, 0.4, 0.0);
				level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.05, getZ(), 1, 0.35, 0.02, 0.35, 0.01);
			}
			if (tickCount % 7 == 0) {
				level.sendParticles(ParticleTypes.SOUL, getX(), getY() + 0.3, getZ(), 1, 0.2, 0.1, 0.2, 0.02);
			}
		} else if (howl) {
			// Grey mist curling up out of the ground, pale ash drifting in it.
			if (tickCount % 2 == 0) {
				level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 2, 0.45, 0.4, 0.45, 0.0);
			}
			if (tickCount % 4 == 0) {
				level.sendParticles(ParticleTypes.WHITE_ASH, getX(), getY() + 1.2, getZ(), 3, 0.6, 0.8, 0.6, 0.0);
			}
		} else if (line) {
			// The open line: sparks crackling off the telephone under it, a wisp of smoke going up.
			if (tickCount % 3 == 0) {
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() - 0.3, getZ(), 2, 0.25, 0.15, 0.25, 0.05);
			}
			if (tickCount % 5 == 0) {
				level.sendParticles(ParticleTypes.SMOKE, getX(), getY() - 0.2, getZ(), 1, 0.1, 0.05, 0.1, 0.01);
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
		if (howl) {
			// Something far off answers, now and then.
			if (tickCount % HOWL_TICKS == 0) {
				level.playSound(null, blockPosition(), HowlingTotemRite.sound("entity.wolf.howl", SoundEvents.ZOMBIE_AMBIENT), SoundSource.AMBIENT,
						0.6F, 0.5F);
			}
		} else if (line) {
			if (tickCount % OrdersRite.BELL_TICKS == 0) {
				level.playSound(null, blockPosition().below(), SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 0.8F, 1.8F);
			}
		} else if (tickCount % 40 == 0) {
			if (snow) {
				level.playSound(null, blockPosition(), SoundEvents.BREEZE_IDLE_AIR, SoundSource.AMBIENT, 0.8F, 0.6F);
			} else if (vent) {
				level.playSound(null, blockPosition(), SoundEvents.FIRE_AMBIENT, SoundSource.AMBIENT, 1.0F, 0.5F);
			} else if (fen) {
				level.playSound(null, blockPosition(), SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.AMBIENT, 0.6F, 0.5F);
			} else if (mars) {
				level.playSound(null, blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.AMBIENT, 0.5F, 0.5F);
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
		} else if (open.lair == Lair.LANTERN_FEN) {
			HagStoneRite.follow(server, open);
		} else if (open.lair == Lair.CRYPT_ARCHIVE) {
			ExhumationRite.follow(server, open);
		} else if (open.lair == Lair.FOUNDRY_TRENCH) {
			OrdersRite.follow(server, open);
		} else if (open.lair == Lair.MOONLIT_RING) {
			HowlingTotemRite.follow(server, open);
		} else if (open.lair == Lair.CYDONIAN_SANCTUM) {
			AltarKeystoneRite.follow(server, open);
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
