package io.github.jimbozoomer.jugcraft.lair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * One fight with a lair boss (Concordance Chunk A, A-05): who took part and how, kept by the boss while it lives. A
 * player qualifies for the boss's reward by any one of these:
 * <ul>
 * <li><b>damage</b>: any blow to the boss or a creature that counts for it (Vesperine's skulls and harvest souls, Madame
 * Tatterlace's egg sacs and spiderlings; not Vesperine's Grave Thralls), as before Chunk A, or by a creature they
 * tamed;</li>
 * <li><b>healing</b>: at least {@value #HEALING} health given back to other players the fight had hurt, counting no
 * more than the hurt not yet healed;</li>
 * <li><b>warding</b>: at least {@value #WARDING} of the fight's damage soaked by a ward they put on another player;</li>
 * <li><b>an objective</b>: a deed the boss names, such as a ward relit in Vesperine's Harvest or a fellow player cut
 * from Madame Tatterlace's thread, each counted once a fight.</li>
 * </ul>
 * Healing yourself, healing hurt the fight did not do (a fall, another player's blow), healing past full health, a ward
 * that soaks nothing and a deed done again all count for nothing; past qualifying, more of anything adds nothing. The
 * fight's hurt is health a player lost to a creature's blow (not a player's, nor a tamed creature's) within
 * {@value #REACH} blocks of the boss's arena while the fight was on, measured from their health and absorption before
 * and after each blow, and no more than the health they lack (what rest, food or a potion healed is no longer there to
 * heal); a ward's soak is the absorption a blow took, up to what the ward gave. Healing and wards are seen where the
 * Concordance applies them (its effect boundary), so a potion, food or another mod's healing is not counted. At most
 * {@value #MAX_PLAYERS} players are recorded a fight. Players who die within its reach while it is on are excused, and paid wherever they are if the boss
 * falls; players who leave the game there are paid later if they are still away when it falls, and otherwise like anyone
 * else (see {@link LairRewards}). Nothing here is saved: a boss that unloads mid-fight forgets who took part, as
 * before, and keeps a fresh record from when it is back.
 */
public final class Encounter {
	/** The most players one fight records (a party is at most 16; room for re-entries and helpers). */
	public static final int MAX_PLAYERS = 32;
	/** Healing of others' hurt from the fight that qualifies alone (two hearts). */
	public static final float HEALING = 4.0F;
	/** The fight's damage soaked by a ward on another player that qualifies alone (two hearts). */
	public static final float WARDING = 4.0F;
	/** How far from the arena's centre a player counts as in the fight (the reward's own reach, as before). */
	public static final double REACH = 64.0;

	/** Fights in progress (server thread; one per awake boss). */
	private static final List<Encounter> ACTIVE = new CopyOnWriteArrayList<>();
	/** Health and absorption before the blow being dealt, by player (taken as it starts, compared after). */
	private static final Map<LivingEntity, float[]> BEFORE = new IdentityHashMap<>();

	private final Entity boss;
	private final Supplier<Vec3> centre;
	private UUID fight = UUID.randomUUID();
	private boolean open;
	private boolean paid;
	private final Map<UUID, Share> shares = new LinkedHashMap<>();
	private final Set<String> deeds = new HashSet<>();
	private final Map<UUID, Float> hurt = new HashMap<>();
	private final Map<UUID, Ward> wards = new HashMap<>();
	private final Set<UUID> excused = new HashSet<>();
	/** Left the game within reach while the fight was on (paid later only if still away when the boss falls). */
	private final Set<UUID> away = new HashSet<>();

	/** One player's part in a fight. */
	public static final class Share {
		private float damage;
		private float healing;
		private float warding;
		private int deeds;

		public float damage() {
			return damage;
		}

		public float healing() {
			return healing;
		}

		public float warding() {
			return warding;
		}

		public int deeds() {
			return deeds;
		}

		public boolean qualifies() {
			return damage > 0.0F || healing >= HEALING || warding >= WARDING || deeds > 0;
		}
	}

	/** A ward one player put on another: how much more of the fight's damage it may be credited for, and until when. */
	private record Ward(UUID caster, float left, long until) {
	}

	/** A fight for {@code boss}, whose arena is round {@code centre}. */
	public Encounter(Entity boss, Supplier<Vec3> centre) {
		this.boss = boss;
		this.centre = centre;
	}

	/** Watches blows, deaths and departures for every fight (from Lairs.register, once). */
	static void register() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayer && !ACTIVE.isEmpty()) {
				BEFORE.put(entity, new float[] {entity.getHealth(), entity.getAbsorptionAmount()});
			} else {
				// A blow refused after this event (another mod, a hurt cooldown) leaves its record; none is kept stale.
				BEFORE.remove(entity);
			}
			return true;
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
			float[] before = BEFORE.remove(entity);
			if (before == null || !(entity instanceof ServerPlayer player)) {
				return;
			}
			// Only a creature's blow is the fight's: not a player's or a tamed creature's (no farming hurt between friends),
			// not a fall.
			boolean fights = source.getEntity() instanceof LivingEntity && striker(source) == null;
			float lost = Math.max(0.0F, before[0] - player.getHealth());
			float soaked = Math.max(0.0F, before[1] - player.getAbsorptionAmount());
			for (Encounter fight : ACTIVE) {
				if (fight.contains(player)) {
					// Whatever healed them since the fight last hurt them (rest, food, a potion) is no longer the fight's.
					fight.settle(player, before[0]);
					if (fights) {
						fight.hurt(player, lost);
						fight.soaked(player, soaked);
					}
				}
			}
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			BEFORE.remove(entity);
			if (entity instanceof ServerPlayer player) {
				excuse(player);
			}
		});
		// On the server thread, before the leaving player is removed from their level (Fabric's disconnect event may come
		// on a network thread).
		ServerPlayerEvents.LEAVE.register(player -> {
			BEFORE.remove(player);
			for (Encounter fight : ACTIVE) {
				if (fight.contains(player)) {
					fight.away.add(player.getUUID());
				}
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			ACTIVE.clear();
			BEFORE.clear();
		});
	}

	/** {@code player} died: excused from every fight they were in. */
	private static void excuse(ServerPlayer player) {
		for (Encounter fight : ACTIVE) {
			if (fight.contains(player)) {
				fight.excused.add(player.getUUID());
			}
		}
	}

	/**
	 * Who a blow to a boss, or to a creature that counts for it, is credited to: the player who struck it (by hand, or
	 * with what they shot or cast), or the owner of a tamed creature that struck it (a wolf fights for its owner). No
	 * one, for anything else.
	 */
	public static @Nullable ServerPlayer striker(DamageSource source) {
		if (source.getEntity() instanceof ServerPlayer player) {
			return player;
		}
		return source.getEntity() instanceof OwnableEntity pet && pet.getOwner() instanceof ServerPlayer owner ? owner : null;
	}

	/** The fights in progress round {@code player} (their healers and wards may be credited there). */
	public static List<Encounter> around(ServerPlayer player) {
		List<Encounter> out = new ArrayList<>();
		for (Encounter fight : ACTIVE) {
			if (fight.contains(player)) {
				out.add(fight);
			}
		}
		return out;
	}

	/**
	 * The Concordance applied a helpful effect: {@code amount} health restored, or a ward that may soak {@code amount},
	 * lasting {@code ticks}. Credited in every fight round the patient (the boundary's observer calls this).
	 */
	public static void helped(@Nullable ServerPlayer helper, LivingEntity patient, boolean healing, float amount, int ticks) {
		if (helper == null || !(patient instanceof ServerPlayer player) || helper == player || amount <= 0.0F) {
			return;
		}
		for (Encounter fight : around(player)) {
			if (!fight.contains(helper)) {
				continue;
			}
			if (healing) {
				fight.healed(helper, player, amount);
			} else {
				fight.warded(helper, player, amount, ticks);
			}
		}
	}

	/** This fight's own identity; a new one each time the boss wakes. */
	public UUID fight() {
		return fight;
	}

	/** Whether the fight is on (begun, and neither over nor left). */
	public boolean underway() {
		return open;
	}

	/** The boss wakes: a new fight, with no one's part from the last. */
	public void begin() {
		forget();
		paid = false;
		open = true;
		if (!ACTIVE.contains(this)) {
			ACTIVE.add(this);
		}
	}

	/** The fight is over (the boss fell, or a test is done): nothing more is credited, and who took part is kept. */
	public void end() {
		open = false;
		ACTIVE.remove(this);
	}

	/** The boss was left alone and went back to its seat: the fight is over, and everyone's part in it forgotten. */
	public void abandon() {
		end();
		forget();
	}

	private void forget() {
		fight = UUID.randomUUID();
		shares.clear();
		deeds.clear();
		hurt.clear();
		wards.clear();
		excused.clear();
		away.clear();
	}

	/** Whether the fight is on and {@code player} is in it: alive or not, in the boss's level within reach of the arena. */
	public boolean contains(ServerPlayer player) {
		if (!open || boss.isRemoved()) {
			ACTIVE.remove(this);
			return false;
		}
		return player.level() == boss.level() && player.position().distanceTo(centre.get()) <= REACH;
	}

	/** The boss, or a creature that counts for it, took {@code amount} from {@code player}. */
	public void damage(ServerPlayer player, float amount) {
		Share share = share(player.getUUID());
		if (share != null && amount > 0.0F) {
			share.damage += amount;
		}
	}

	/** {@code player} did a deed the boss names ({@code key}, unique within the fight); false if it was already done. */
	public boolean deed(ServerPlayer player, String key) {
		Share share = share(player.getUUID());
		if (share == null || !deeds.add(key)) {
			return false;
		}
		share.deeds++;
		return true;
	}

	/** The fight hurt {@code player} by {@code health}; kept to be healed, up to their whole health. */
	public void hurt(ServerPlayer player, float health) {
		if (health > 0.0F) {
			hurt.merge(player.getUUID(), health, (was, more) -> Math.min(player.getMaxHealth(), was + more));
		}
	}

	/**
	 * {@code player} had {@code health} as a blow of any kind began: the fight's hurt they still carry is at most the
	 * health they lacked, so what rest, food or a potion healed since is no longer there to be credited.
	 */
	void settle(ServerPlayer player, float health) {
		hurt.computeIfPresent(player.getUUID(), (id, owed) -> Math.min(owed, Math.max(0.0F, player.getMaxHealth() - health)));
	}

	/**
	 * {@code healer} gave {@code patient} back {@code gained} health (already given): credited up to the fight's hurt not
	 * yet healed, which is at most what they lacked before it.
	 */
	public void healed(ServerPlayer healer, ServerPlayer patient, float gained) {
		float lacked = Math.max(0.0F, patient.getMaxHealth() - (patient.getHealth() - gained));
		float owed = Math.min(hurt.getOrDefault(patient.getUUID(), 0.0F), lacked);
		if (owed <= 0.0F || healer == patient) {
			return;
		}
		Share share = share(healer.getUUID());
		if (share == null) {
			return;
		}
		float credit = Math.min(owed, gained);
		hurt.put(patient.getUUID(), owed - credit);
		share.healing += credit;
	}

	/** {@code caster} put a ward on {@code patient} that may soak {@code amount} for {@code ticks}. */
	public void warded(ServerPlayer caster, ServerPlayer patient, float amount, int ticks) {
		if (caster != patient && share(caster.getUUID()) != null) {
			wards.put(patient.getUUID(), new Ward(caster.getUUID(), amount, patient.level().getGameTime() + Math.max(1, ticks)));
		}
	}

	/** A blow from the fight took {@code absorbed} of {@code patient}'s absorption: their ward's caster is credited. */
	public void soaked(ServerPlayer patient, float absorbed) {
		Ward ward = wards.get(patient.getUUID());
		if (ward == null || absorbed <= 0.0F) {
			return;
		}
		if (ward.until() < patient.level().getGameTime() || ward.left() <= 0.0F) {
			wards.remove(patient.getUUID());
			return;
		}
		Share share = share(ward.caster());
		float credit = Math.min(absorbed, ward.left());
		wards.put(patient.getUUID(), new Ward(ward.caster(), ward.left() - credit, ward.until()));
		if (share != null) {
			share.warding += credit;
		}
	}

	/** {@code player}'s part in this fight, if any. */
	public @Nullable Share share(ServerPlayer player) {
		return shares.get(player.getUUID());
	}

	/** Everyone who qualifies, in the order they joined the fight. */
	public List<UUID> qualified() {
		List<UUID> out = new ArrayList<>();
		shares.forEach((player, share) -> {
			if (share.qualifies()) {
				out.add(player);
			}
		});
		return out;
	}

	/** Excuses {@code player} from this fight, as dying within its reach does (tests call it too). */
	public void excuse(UUID player) {
		excused.add(player);
	}

	/** Whether {@code player} died in this fight (and so is paid wherever they are). */
	public boolean excused(UUID player) {
		return excused.contains(player);
	}

	/** Records that {@code player} left the game within this fight's reach, as leaving does (tests call it too). */
	public void left(UUID player) {
		away.add(player);
	}

	/** Whether {@code player} left the game in this fight (paid later only if still away when the boss falls). */
	public boolean away(UUID player) {
		return away.contains(player);
	}

	/**
	 * The boss fell: true the first time this fight, false after, so however often its fall is reported its reward is
	 * given once. The fight is over either way.
	 */
	public boolean claim() {
		end();
		if (paid) {
			return false;
		}
		paid = true;
		return true;
	}

	/** {@code player}'s share, made if there is room for another player. */
	private @Nullable Share share(UUID player) {
		Share share = shares.get(player);
		if (share == null && shares.size() < MAX_PLAYERS) {
			share = new Share();
			shares.put(player, share);
		}
		return share;
	}
}
