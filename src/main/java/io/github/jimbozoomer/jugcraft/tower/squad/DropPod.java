package io.github.jimbozoomer.jugcraft.tower.squad;

import java.util.UUID;
import java.util.function.BiConsumer;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * One soldier's drop pod (sheet 3's dart; its flight from sheet 5). It stands where it will land from the start (the
 * server never moves it): the client draws it on its way, from its own point high in the sky near the flare over the
 * mark ({@code flare}: from where it lands) weaving down as if dodging fire ({@link #path}), the retro motors firing hard
 * at the last second. The squad's pods set off one after another ({@code delay}: Justin, 10 November 2026, "the ships
 * shouldn't land in sync, it should be randomized within like 7 seconds"), each down its own way, kept clear of the
 * others' (SquadDrop.launch). On landing ({@link #land}: {@link JugcraftSquads#FLARE_TICKS}, its delay and
 * {@link JugcraftSquads#FALL_TICKS} after the launch) it buries itself a block in the ground,
 * its hatch blows a moment later and its soldier staggers out ({@link Legionary}); once the soldier is
 * {@link JugcraftSquads#CLEAR} blocks away (or gone) it blows itself apart. Nothing it does breaks a block or hurts
 * anyone.
 */
public class DropPod extends Entity {
	private static final EntityDataAccessor<Long> START = SynchedEntityData.defineId(DropPod.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Vector3fc> FLARE = SynchedEntityData.defineId(DropPod.class, EntityDataSerializers.VECTOR3);
	private static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(DropPod.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> FACING = SynchedEntityData.defineId(DropPod.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> PLACE = SynchedEntityData.defineId(DropPod.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DELAY = SynchedEntityData.defineId(DropPod.class, EntityDataSerializers.INT);
	/** The most a squad's pods are set off apart (ticks): their landings spread over about seven seconds. */
	public static final int SPREAD_TICKS = 140;
	/** Ticks from landing till the hatch blows; how long a pod waits for its soldier to get clear at most. */
	public static final int HATCH_AFTER = 12, WAIT_MAX = 2400;
	/** Ticks after it sets off that its sonic boom is heard. */
	public static final int BOOM_AFTER = 16;
	/** The soldier's feet in the pod (tools/drop_pod/dart.py SOLDIER) and how deep the pod buries itself. */
	public static final Vec3 SOLDIER = new Vec3(0, 1.05, -0.5);
	public static final float BURIED = 1.0F;
	/** The client's sounds as a pod's flight goes on (client/DropPodSounds): the pod and what (0 it sets off down its way:
	 * its roar, 1 the retros fire, 2 its sonic boom, {@link #BOOM_AFTER} ticks after it sets off). */
	public static BiConsumer<DropPod, Integer> clientSounds = (pod, what) -> {
	};

	private @Nullable UUID owner, squad, soldier;
	private int squadIndex, place;
	private boolean landed, opened;
	private final boolean[] heard = new boolean[3];

	public DropPod(EntityType<? extends DropPod> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	void setUp(UUID owner, UUID squad, int squadIndex, int place, long start, int delay, Vector3f flare, int seed, float facing) {
		this.owner = owner;
		this.squad = squad;
		this.squadIndex = squadIndex;
		this.place = place;
		entityData.set(START, start);
		entityData.set(FLARE, flare);
		entityData.set(SEED, seed);
		entityData.set(FACING, facing);
		entityData.set(PLACE, place);
		entityData.set(DELAY, delay);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(START, 0L);
		builder.define(FLARE, new Vector3f(0, 200, 0));
		builder.define(SEED, 0);
		builder.define(FACING, 0F);
		builder.define(PLACE, 0);
		builder.define(DELAY, 0);
	}

	public long start() {
		return entityData.get(START);
	}

	public Vector3fc flare() {
		return entityData.get(FLARE);
	}

	public int seed() {
		return entityData.get(SEED);
	}

	/** The way its hatch faces (radians about y: the pod's -z turned by it). */
	public float facing() {
		return entityData.get(FACING);
	}

	/** Its place in the squad (0: the leader's, which the client draws the flare for). */
	public int place() {
		return entityData.get(PLACE);
	}

	/** Ticks from a pod's own launch (the squad's, less its delay) till it lands. */
	public static final int FLIGHT = JugcraftSquads.FLARE_TICKS + JugcraftSquads.FALL_TICKS;

	/** Ticks from the squad's launch till this pod sets off (after the flare's and its own delay). */
	public int delay() {
		return entityData.get(DELAY);
	}

	/** Ticks from the squad's launch till it sets off down its way. */
	public int setsOff() {
		return JugcraftSquads.FLARE_TICKS + delay();
	}

	/** Ticks from the squad's launch till it lands. */
	public int land() {
		return setsOff() + JugcraftSquads.FALL_TICKS;
	}

	/** Where it is {@code age} ticks after the squad's launch, from where it lands (blocks). */
	public Vector3f at(float age, Vector3f out) {
		return path(flare(), seed(), age - delay(), out);
	}

	public @Nullable UUID owner() {
		return owner;
	}

	/**
	 * Where a pod is {@code age} ticks after its own launch (the squad's, less its delay), from where it lands (blocks):
	 * at its point in the sky ({@code flare}) until it sets off, then down a curve that leaves it diving toward the
	 * ground and comes in at the end straight down, weaving from side to side as if dodging fire (two waves across it,
	 * each pod its own, fading to nothing at both ends so it leaves its point cleanly and lands just where it should),
	 * fast at first and slowing hard at the end (the retro burn).
	 */
	public static Vector3f path(Vector3fc flare, int seed, float age, Vector3f out) {
		float s = along(Mth.clamp((age - JugcraftSquads.FLARE_TICKS) / JugcraftSquads.FALL_TICKS, 0, 1));
		float cx = 0, cy = flare.y() * 0.6F, cz = 0;               // the control point, high over where it lands
		float u = 1 - s;
		float bx = u * u * flare.x() + 2 * u * s * cx, by = u * u * flare.y() + 2 * u * s * cy, bz = u * u * flare.z() + 2 * u * s * cz;
		java.util.Random r = new java.util.Random(seed);
		float a1 = 7 + r.nextFloat() * 7, a2 = 5 + r.nextFloat() * 6;
		float f1 = 1.2F + r.nextFloat() * 1.4F, f2 = 1.0F + r.nextFloat() * 1.6F;
		float p1 = r.nextFloat() * Mth.TWO_PI, p2 = r.nextFloat() * Mth.TWO_PI;
		float env = Mth.sin(Mth.PI * s) * (float) Math.pow(1 - s, 0.6);
		float w1 = a1 * env * Mth.sin(Mth.TWO_PI * f1 * s + p1), w2 = a2 * env * Mth.sin(Mth.TWO_PI * f2 * s + p2);
		return out.set(bx + w1, by, bz + w2);
	}

	/** The flight's progress along its curve at time {@code t} (0 to 1 of the fall): splitting off the flare (speeding up
	 * over the first {@value #SPLIT} of it), falling at speed (about 100 blocks a second since 11 November 2026), then the
	 * retro burn (the last {@value #RETRO} ticks, from about 48 blocks up) braking it hard to {@value #TOUCHDOWN} of that as
	 * it sets down. */
	public static final float SPLIT = 0.08F, TOUCHDOWN = 0.08F;
	public static final int RETRO = 16;

	static float along(float t) {
		float tb = 1 - (float) RETRO / JugcraftSquads.FALL_TICKS;
		float a = 0.7F * SPLIT, b = tb - SPLIT, c = (1 + TOUCHDOWN) / 2 * (1 - tb), total = a + b + c;
		float done;
		if (t < SPLIT) {
			done = 0.4F * t + 0.3F * t * t / SPLIT;
		} else if (t < tb) {
			done = a + (t - SPLIT);
		} else {
			float u = t - tb;
			done = a + b + u - (1 - TOUCHDOWN) / 2 * u * u / (1 - tb);
		}
		return done / total;
	}

	@Override
	public void tick() {
		super.tick();
		long age = level().getGameTime() - start();
		if (!(level() instanceof ServerLevel level)) {
			if (age >= setsOff() && age < land() && !heard[0]) {
				heard[0] = true;
				clientSounds.accept(this, 0);
			}
			if (age >= land() - RETRO && age < land() && !heard[1]) {
				heard[1] = true;
				clientSounds.accept(this, 1);
			}
			if (age >= setsOff() + BOOM_AFTER && age < setsOff() + BOOM_AFTER + 40 && !heard[2]) {
				heard[2] = true;
				clientSounds.accept(this, 2);
			}
			// landed: smoke off the scorched pod a while, thinning
			long since = age - land();
			if (since >= 0 && since < 200 && random.nextInt(since < 40 ? 1 : since < 100 ? 3 : 8) == 0) {
				double a = random.nextDouble() * Mth.TWO_PI, r = 0.8 + random.nextDouble() * 0.6;
				level().addParticle(since < 60 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE, getX() + Math.cos(a) * r,
						getY() + 0.3 + random.nextDouble() * 3.0, getZ() + Math.sin(a) * r, 0, 0.04, 0);
			}
			return;
		}
		if (tickCount % 100 == 50 && owner != null && !SquadAccounts.get(level).listed(owner, getUUID())) {
			discard();                                      // recalled with its squad
			return;
		}
		if (!landed && age >= land()) {
			landed = true;
			impact(level);
		}
		if (landed && !opened && age >= land() + HATCH_AFTER) {
			opened = true;
			open(level);
		}
		if (opened) {
			Entity s = soldier == null ? null : level.getEntity(soldier);
			if (s == null || !s.isAlive() || s.distanceTo(this) >= JugcraftSquads.CLEAR || age > land() + HATCH_AFTER + WAIT_MAX) {
				destruct(level);
			}
		}
	}

	/** Landing: the thump, the ground thrown up, smoke. */
	private void impact(ServerLevel level) {
		level.playSound(null, getX(), getY(), getZ(), JugcraftSquads.POD_IMPACT, SoundSource.NEUTRAL, 3.0F, 0.9F + random.nextFloat() * 0.2F);
		BlockPos below = blockPosition().below();
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(below)), getX(), getY() + 0.2, getZ(), 80, 1.2, 0.3, 1.2, 0.25);
		level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.5, getZ(), 3, 0.8, 0.2, 0.8, 0.0);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.5, getZ(), 40, 1.4, 0.4, 1.4, 0.04);
	}

	/** The hatch blows and its soldier comes out, dazed, just outside the doorway. */
	private void open(ServerLevel level) {
		Vec3 out = toWorld(SOLDIER.x, SOLDIER.y - BURIED, SOLDIER.z - 1.35);
		level.playSound(null, out.x, out.y + 1, out.z, JugcraftSquads.POD_HATCH, SoundSource.NEUTRAL, 2.0F, 1.0F);
		level.sendParticles(ParticleTypes.POOF, out.x, out.y + 1.2, out.z, 20, 0.5, 0.6, 0.5, 0.05);
		if (owner == null || squad == null) {
			return;
		}
		Legionary l = JugcraftSquads.LEGIONARY.create(level, EntitySpawnReason.TRIGGERED);
		if (l == null) {
			return;
		}
		float yaw = (float) Math.toDegrees(-facing());
		l.snapTo(out.x, out.y, out.z, yaw, 0);
		l.setUp(owner, squad, squadIndex, place == 0, place);
		l.setCalledAt(start());                             // their day in the field counts from the call-in
		level.addFreshEntity(l);
		soldier = l.getUUID();
		SquadAccounts.get(level).out(owner, l.getUUID());
	}

	/** Once its soldier is clear: it blows itself apart (no harm to anything; the debris and smoke only). */
	private void destruct(ServerLevel level) {
		Vec3 c = toWorld(0, 2.0 - BURIED, 0);
		level.playSound(null, c.x, c.y, c.z, JugcraftSquads.POD_DESTRUCT, SoundSource.NEUTRAL, 3.0F, 0.95F + random.nextFloat() * 0.1F);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 1, 0, 0, 0, 0.0);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y, c.z, 60, 1.2, 1.6, 1.2, 0.06);
		level.sendParticles(ParticleTypes.FLAME, c.x, c.y, c.z, 30, 1.0, 1.2, 1.0, 0.08);
		level.sendParticles(new net.minecraft.core.particles.ItemParticleOption(ParticleTypes.ITEM,
				net.minecraft.world.item.Items.IRON_INGOT), c.x, c.y, c.z, 40, 0.8, 1.2, 0.8, 0.35);
		if (owner != null) {
			SquadAccounts.get(level).back(owner, getUUID());
		}
		discard();
	}

	/** A point of the pod's own frame (y up from its shield's bottom, the hatch at -z) in the world, landed. */
	public Vec3 toWorld(double x, double y, double z) {
		float a = facing();
		double c = Math.cos(a), s = Math.sin(a);
		return new Vec3(getX() + x * c + z * s, getY() + y, getZ() - x * s + z * c);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return true;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		if (owner != null) {
			output.store("owner", UUIDUtil.CODEC, owner);
		}
		if (squad != null) {
			output.store("squad", UUIDUtil.CODEC, squad);
		}
		if (soldier != null) {
			output.store("soldier", UUIDUtil.CODEC, soldier);
		}
		output.putInt("squad_index", squadIndex);
		output.putInt("place", place);
		output.putLong("start", start());
		output.store("flare", Vec3.CODEC, new Vec3(flare().x(), flare().y(), flare().z()));
		output.putInt("seed", seed());
		output.putInt("delay", delay());
		output.putFloat("facing", facing());
		output.putBoolean("landed", landed);
		output.putBoolean("opened", opened);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
		squad = input.read("squad", UUIDUtil.CODEC).orElse(null);
		soldier = input.read("soldier", UUIDUtil.CODEC).orElse(null);
		squadIndex = input.getIntOr("squad_index", 0);
		place = input.getIntOr("place", 0);
		entityData.set(PLACE, place);
		entityData.set(START, input.getLongOr("start", 0L));
		Vec3 f = input.read("flare", Vec3.CODEC).orElse(new Vec3(0, 200, 0));
		entityData.set(FLARE, new Vector3f((float) f.x, (float) f.y, (float) f.z));
		entityData.set(SEED, input.getIntOr("seed", 0));
		entityData.set(DELAY, input.getIntOr("delay", 0));
		entityData.set(FACING, input.getFloatOr("facing", 0F));
		landed = input.getBooleanOr("landed", false);
		opened = input.getBooleanOr("opened", false);
	}
}
