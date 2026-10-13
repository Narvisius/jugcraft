package io.github.jimbozoomer.jugcraft.lair.tatterlace;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.lair.LairBosses;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Madame Tatterlace's Lace Snare: a spinning net of lace flicked from her palps. It arcs to where her foe stood and lies
 * there as a snare {@value #SIZE} blocks square for {@value #TICKS} ticks: whoever stands in it has Slowness IV and cannot
 * jump. Where her lace is gone it falls away. It is drawn as its own particles, and never saved.
 *
 * <p>Stopping a player's jump is a transient change to their jump strength, which they get back half a second after no
 * snare holds them, however the snare went (it ended, the lair closed, or they left), and on every restart.
 */
public class LaceSnareEntity extends Entity {
	public static final int TICKS = 120;
	public static final int SIZE = 3;
	public static final int SLOWNESS = 3;
	public static final double SPEED = 0.6;
	private static final double GRAVITY = 0.05;
	private static final Identifier NO_JUMP = Jugcraft.id("lace_snare");
	/** Who a snare holds, and the game time it last did. */
	private static final Map<UUID, Long> SNARED = new HashMap<>();

	private @Nullable UUID owner;
	private Vec3 velocity = Vec3.ZERO;
	private double floor;
	private boolean landed;
	private int lying;

	public LaceSnareEntity(EntityType<? extends LaceSnareEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	static void register() {
		ServerTickEvents.END_SERVER_TICK.register(LaceSnareEntity::release);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> SNARED.clear());
	}

	/** {@code boss} flicks a snare from her palps toward {@code at}. */
	public static LaceSnareEntity fling(ServerLevel level, TatterlaceEntity boss, Vec3 at) {
		LaceSnareEntity snare = new LaceSnareEntity(JugcraftTatterlace.LACE_SNARE, level);
		snare.owner = boss.getUUID();
		snare.floor = boss.floorY();
		Vec3 from = boss.position().add(boss.facing().scale(1.5)).add(0.0, 1.0, 0.0);
		double dx = at.x - from.x;
		double dz = at.z - from.z;
		double ticks = Math.max(6.0, Math.sqrt(dx * dx + dz * dz) / SPEED);
		snare.velocity = new Vec3(dx / ticks, (snare.floor - from.y + 0.5 * GRAVITY * ticks * ticks) / ticks, dz / ticks);
		snare.snapTo(from.x, from.y, from.z, 0.0F, 0.0F);
		level.addFreshEntity(snare);
		level.playSound(null, from.x, from.y, from.z, SoundEvents.WOOL_HIT, SoundSource.HOSTILE, 1.2F, 1.4F);
		return snare;
	}

	public boolean landed() {
		return landed;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		boolean bound = owner != null && level.getEntity(owner) instanceof TatterlaceEntity boss && boss.isAlive();
		if (!bound || getY() < floor - 8.0) {
			end();
			return;
		}
		if (!landed) {
			velocity = velocity.add(0.0, -GRAVITY, 0.0);
			Vec3 next = position().add(velocity);
			BlockPos under = BlockPos.containing(next.x, floor - 0.05, next.z);
			if (next.y <= floor && velocity.y < 0.0 && !level.getBlockState(under).getCollisionShape(level, under).isEmpty()) {
				landed = true;
				setPos(next.x, floor, next.z);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.WOOL_PLACE, SoundSource.HOSTILE, 1.5F, 0.8F);
				return;
			}
			setPos(next.x, next.y, next.z);
			level.sendParticles(ParticleTypes.ITEM_COBWEB, getX(), getY(), getZ(), 2, 0.4, 0.1, 0.4, 0.0);
			return;
		}
		if (++lying > TICKS) {
			end();
			return;
		}
		long now = level.getGameTime();
		for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area())) {
			if (LairBosses.eligible(player)) {
				snare(player, now);
			}
		}
		if (lying % 5 == 0) {
			double half = SIZE / 2.0;
			for (int i = 0; i <= 6; i++) {
				double along = -half + SIZE * i / 6.0;
				for (double[] point : new double[][] {{along, -half}, {along, half}, {-half, along}, {half, along}}) {
					level.sendParticles(ParticleTypes.ITEM_COBWEB, getX() + point[0], floor + 0.1, getZ() + point[1], 1, 0.0, 0.0, 0.0, 0.0);
				}
			}
		}
	}

	/** Where it lies: {@value #SIZE} blocks square on the lace. */
	public AABB area() {
		double half = SIZE / 2.0;
		return new AABB(getX() - half, floor - 0.5, getZ() - half, getX() + half, floor + 1.0, getZ() + half);
	}

	/** Holds {@code player}: Slowness IV, and no jumping until no snare has held them for a moment. */
	public static void snare(Player player, long now) {
		player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 10, SLOWNESS, false, false, true));
		AttributeInstance jump = player.getAttribute(Attributes.JUMP_STRENGTH);
		if (jump != null && !jump.hasModifier(NO_JUMP)) {
			jump.addTransientModifier(new AttributeModifier(NO_JUMP, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
		SNARED.put(player.getUUID(), now);
	}

	/** Whether a snare holds {@code player}'s jump now. */
	public static boolean snared(Player player) {
		AttributeInstance jump = player.getAttribute(Attributes.JUMP_STRENGTH);
		return jump != null && jump.hasModifier(NO_JUMP);
	}

	private static void free(Player player) {
		AttributeInstance jump = player.getAttribute(Attributes.JUMP_STRENGTH);
		if (jump != null) {
			jump.removeModifier(NO_JUMP);
		}
	}

	/** Twice a second, whoever no snare has held for a moment can jump again. */
	private static void release(MinecraftServer server) {
		if (server.getTickCount() % 10 != 0 || SNARED.isEmpty()) {
			return;
		}
		long now = server.overworld().getGameTime();
		SNARED.entrySet().removeIf(entry -> {
			if (now - entry.getValue() <= 5) {
				return false;
			}
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player != null) {
				free(player);
			}
			return true;
		});
	}

	/** The snare is gone: whoever it held can jump again at once. */
	public void end() {
		if (level() instanceof ServerLevel level && landed) {
			for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area())) {
				SNARED.remove(player.getUUID());
				free(player);
			}
		}
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
}
