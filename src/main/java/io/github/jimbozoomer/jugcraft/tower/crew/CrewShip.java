package io.github.jimbozoomer.jugcraft.tower.crew;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * The crew drop ship (the drop ship sheet, take 4, approved 6 November 2026): Narv-Tech's lander that brings a
 * tower's new crew in. One entity standing on its landing point the whole visit; its flight is drawn on each player's
 * screen from {@link CrewShipFlight} (the phase and when it began are all that is sent).
 *
 * <p>While it hovers in and stands there its guns ({@link #GUNS}: a twin plasma gun in each side door, a turret on
 * top, one in the bay over the ramp) shoot hostile mobs within {@link #RANGE} blocks they can see (the door guns rapid
 * fire, the turrets heavy bolts: {@link #GUN_COOLDOWN}, {@link #GUN_DAMAGE}), never the crew or tamed animals; players only if they've hurt one of the Narv-Tech crew
 * near it ({@link #ANGER_TICKS}) or come inside its keep-out ring while it's down (the red STAY BACK ring on the
 * ground, {@link CrewShipFlight#ZONE_A}); not in creative or spectator. While it's down nothing outside that ring is
 * shot at all (the ring is its kill radius). The floodlights put invisible light blocks in
 * the air round the landing zone ({@link CrewShipLights}). The crew it carries ride in seated in the bay ({@link #SEATS};
 * the client draws them in flight), and are in their seats as it opens up; with the ramp down they get up and come off
 * one by one, security first (they hold the ramp's foot facing out till it lifts off), waiting while hostiles are near
 * the ramp, and walk down it and on inside; then it closes up and goes.
 *
 * <p>A pickup ({@link #setUpPickup}; drop squads, release (b): a squad sent home) comes the same way to wherever there's
 * room near the squad, its ramp toward them; with the ramp down it calls them aboard one by one (the leader last), each
 * walking up the ramp to a seat (tower/squad/Legionary), drawn seated from there as it closes up and leaves.
 */
public class CrewShip extends Entity {
	private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(CrewShip.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> PHASE_START = SynchedEntityData.defineId(CrewShip.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(CrewShip.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Boolean> ROOF = SynchedEntityData.defineId(CrewShip.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Float> REACH = SynchedEntityData.defineId(CrewShip.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> CEILING = SynchedEntityData.defineId(CrewShip.class, EntityDataSerializers.FLOAT);
	/** Who it brings, in their seats' order: "skin;slim;gear" each, joined by "|" (the renderer seats them in flight). */
	private static final EntityDataAccessor<String> PASSENGERS = SynchedEntityData.defineId(CrewShip.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Boolean> PICKUP = SynchedEntityData.defineId(CrewShip.class, EntityDataSerializers.BOOLEAN);
	/** The door guns' barrels spinning (bit 0 port, 1 starboard), and the heavy guns' shots so far (four bits each, guns 2
	 * to 5: the client kicks the barrel that fired back, the left one first, then right, ...). */
	private static final EntityDataAccessor<Byte> SPIN = SynchedEntityData.defineId(CrewShip.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Integer> SHOTS = SynchedEntityData.defineId(CrewShip.class, EntityDataSerializers.INT);
	@SuppressWarnings("unchecked")
	private static final EntityDataAccessor<Vector3fc>[] AIMS = new EntityDataAccessor[] {
			SynchedEntityData.defineId(CrewShip.class, EntityDataSerializers.VECTOR3), SynchedEntityData.defineId(CrewShip.class, EntityDataSerializers.VECTOR3),
			SynchedEntityData.defineId(CrewShip.class, EntityDataSerializers.VECTOR3), SynchedEntityData.defineId(CrewShip.class, EntityDataSerializers.VECTOR3),
			SynchedEntityData.defineId(CrewShip.class, EntityDataSerializers.VECTOR3), SynchedEntityData.defineId(CrewShip.class, EntityDataSerializers.VECTOR3)};

	/** The guns: name, where it stands (the ship's frame; the door guns run out), hung under (bay) or not, its size, its
	 * barrels' length, its resting aim (yaw, pitch), and half the spacing of its barrels' muzzles (a rotary's one in the
	 * middle). The same as tools/crew_ship.py's (TURRET_SCALE, BARREL, TURRET_SPREAD). */
	public record Gun(String name, float x, float y, float z, boolean hung, float scale, float barrel, float yaw, float pitch, float spread) {
	}

	public static final float G = CrewShipFlight.G;
	public static final Gun[] GUNS = {new Gun("door_l", -10.65F, G + 2.35F, -0.2F, false, 1.0F, 3.0F, Mth.HALF_PI, 0, 0),
			new Gun("door_r", 10.65F, G + 2.35F, -0.2F, false, 1.0F, 3.0F, -Mth.HALF_PI, 0, 0),
			new Gun("top", 0, G + 8.1F, -1.0F, false, 1.3F, 3.0F, 0, 0.06F, 0.3F),
			new Gun("bay", 0, G + 5.8F, 12.5F, true, 0.85F, 2.6F, Mth.PI, -0.05F, 0),
			// the ball turrets under the wings' cranks (the rework, 8 November 2026)
			new Gun("wing_l", -11.6F, G + 1.12F, -12.6F, true, 0.85F, 2.05F, 0, 0, 0.22F),
			new Gun("wing_r", 11.6F, G + 1.12F, -12.6F, true, 0.85F, 2.05F, 0, 0, 0.22F)};
	/** How far the door guns' booms run out of the side doors (blocks, across). */
	public static final float GUN_SWING = 2.65F;
	/**
	 * A door gun's stowed yaw (times its side: port negative), down its bay toward the bay's inner forward corner, and
	 * how far in (of the boom's run) it has turned there (Justin, 9 November 2026: "work on the side gun mounts"; the
	 * gun stowed poked out through the shut door). The same as tools/crew_ship.py's DOOR_GUN_STOW.
	 */
	public static final float DOOR_GUN_STOW = 0.94F, STOW_BY = 0.2F;
	/** The door guns' barrels: ticks spinning up before the first shot, the cluster's turn a shot (radians: one barrel
	 * on, a sixth of a turn) and ticks spinning down once they stop (the client turns them; tools/crew_ship.py's spin). */
	public static final int SPIN_UP = 8, SPIN_DOWN = 20;
	public static final float SPIN_STEP = Mth.TWO_PI / 6;

	/** The door guns' booms' run out (0 in, 1 out) at the side doors' state: the door opens over the first half, then the
	 * boom runs out; going in, the boom comes in first, then the door shuts (it slid through the gun as both moved at once). */
	public static float boom(float sides) {
		return Mth.clamp((sides - 0.5F) * 2, 0, 1);
	}

	/** A side door's opening (0 shut, 1 open) at the side doors' state (see {@link #boom}). */
	public static float door(float sides) {
		return Mth.clamp(sides * 2, 0, 1);
	}

	/** Door gun {@code i}'s (0 port, 1 starboard) x in the ship's frame at the side doors' state. */
	public static float gunX(int i, float sides) {
		return GUNS[i].x() + (i == 0 ? 1 : -1) * GUN_SWING * (1 - boom(sides));
	}

	/** How far door gun {@code i} has turned to its stowed yaw (0 its aim, 1 stowed) at the side doors' state. */
	public static float stowed(float sides) {
		return Mth.clamp((1 - boom(sides)) / STOW_BY, 0, 1);
	}

	/** Gun {@code i}'s yaw as drawn and fired: its aim, the door guns turned (the short way) to their stowed yaw as their
	 * booms come in. */
	public static float drawnYaw(int i, float yaw, float sides) {
		if (i >= 2) {
			return yaw;
		}
		float stow = (i == 0 ? -1 : 1) * DOOR_GUN_STOW;
		return yaw + Mth.wrapDegrees((stow - yaw) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD * stowed(sides);
	}

	/** Gun {@code i}'s pitch as drawn: level as a door gun stows. */
	public static float drawnPitch(int i, float pitch, float sides) {
		return i >= 2 ? pitch : pitch * (1 - stowed(sides));
	}
	/** How far the guns see and how often each fires (ticks); how fast they turn (radians a tick). */
	public static final double RANGE = 32;
	public static final int COOLDOWN = 15;
	/**
	 * Each gun's ticks between shots and its bolts' damage (Justin, 6 November 2026: "the damage from the cannons on the
	 * ship specifically need to be buffed, as it's not killing some of the mobs that are really tough ... the rotary
	 * cannons, like those would shoot way faster"): the door guns spit bolts like rotary cannons, ten a second each, the
	 * top and bay turrets hit hard twice a second; a bolt ignores the target's moment of grace after a hit (PlasmaBolt).
	 */
	public static final int[] GUN_COOLDOWN = {2, 2, 10, 10, 8, 8};
	public static final float[] GUN_DAMAGE = {14.0F, 14.0F, 60.0F, 60.0F, 40.0F, 40.0F};
	static final float TURN = 0.2F;
	/** The floodlights (the ship's frame, as tools/crew_ship.py has them): where each is and which way it shines. Under the
	 * belly, in the chin and over the ramp, down; and since 6 November 2026 (Justin: "when it lands, have lights that go
	 * outward, so they point in all directions, so people can see") out all round: the nose lamps ahead, two on each
	 * sponson out and fore and aft, one under each wing tip out and ahead, one on each thruster pylon's cap out and astern. */
	public static final float[][] LAMPS = {{-3.2F, 2.52F, -6.0F, -0.32F, -0.93F, -0.19F}, {3.2F, 2.52F, -6.0F, 0.32F, -0.93F, -0.19F},
			{-3.2F, 2.52F, 6.5F, -0.32F, -0.93F, 0.19F}, {3.2F, 2.52F, 6.5F, 0.32F, -0.93F, 0.19F}, {0, 2.6F, -14.5F, 0, -0.91F, -0.41F},
			{-2.4F, 8.2F, 14.62F, 0, -0.8F, 0.6F}, {2.4F, 8.2F, 14.62F, 0, -0.8F, 0.6F},
			{-0.85F, 2.45F, -19.02F, -0.11F, -0.3F, -0.95F}, {0.85F, 2.45F, -19.02F, 0.11F, -0.3F, -0.95F},
			{-8.95F, 7.0F, -7.8F, -0.82F, -0.42F, -0.38F}, {8.95F, 7.0F, -7.8F, 0.82F, -0.42F, -0.38F},
			{-9.35F, 6.55F, 6.3F, -0.82F, -0.42F, 0.38F}, {9.35F, 6.55F, 6.3F, 0.82F, -0.42F, 0.38F},
			{-14.4F, 3.02F, -12.3F, -0.55F, -0.45F, -0.7F}, {14.4F, 3.02F, -12.3F, 0.55F, -0.45F, -0.7F},
			{-14.5F, 6.8F, 12.5F, -0.62F, -0.45F, 0.64F}, {14.5F, 6.8F, 12.5F, 0.62F, -0.45F, 0.64F}};
	/** How far a floodlight's beam reaches at most (blocks along it), and how much it spreads (its radius a block out). */
	public static final float BEAM_REACH = 34, BEAM_SPREAD = 0.3F;
	/**
	 * The bay's seats (the crew list's C4, Justin, 9 November 2026: "crew already seated in the ship before it lands, then
	 * disembark in order with security covering"; his pick "seated, security stay"): the jump seats down both walls
	 * (tools/crew_ship.py jump_seat: a cushion's top G + 2.02, the sitter's hips a little in from the wall) and the six
	 * facing aft by the flight deck's door, aft first: the ship's frame x, hips' height, z, and the way the sitter faces
	 * (x, z). Security take the seats nearest the ramp, so they're off first; anyone more than the seats stands in the aisle.
	 */
	public static final float[][] SEATS = seats();
	/** How many of its own security stand guard at the ramp's foot (the rest go on in). */
	public static final int ESCORTS = 6;
	/** Ticks between the crew coming off; how near (blocks) a hostile to the ramp's foot keeps them aboard; walking speed. */
	public static final int CREW_EVERY = 30;
	/** How long a player who hurt the crew stays its target (ticks), and how near (blocks) the crew must be for it to care. */
	public static final int ANGER_TICKS = 1200;
	public static final double ALERT = 128;
	public static final double HOLD_NEAR = 14;
	/** A pickup's: how near the ramp's foot (blocks) a monster holds the boarding, and how soon (ticks) after one
	 * soldier is called up the next is (Justin, 10 November 2026: "Board faster"). */
	public static final double PICKUP_HOLD_NEAR = 6;
	public static final int PICKUP_EVERY = 10;
	public static final double WALK = 0.12;

	private BlockPos core = BlockPos.ZERO;
	private Vec3 entrance = Vec3.ZERO;
	private final List<String> manifest = new ArrayList<>();
	/** Who comes for each post of the manifest (chosen as the ship is sent, so they can be seen in their seats). */
	private final List<CrewRoster.Hire> hires = new ArrayList<>();
	/** The security who came with it standing guard so far (each to the next spot at the ramp's foot); not saved. */
	private int escorts;
	private int delivered;
	private long lastOff;
	private final List<UUID> crew = new ArrayList<>();
	private final List<BlockPos> lights = new ArrayList<>();
	private boolean lightsOn;
	// the guns' own state (the server's): what each is after, its cooldown, which barrel next
	private final @Nullable LivingEntity[] targets = new LivingEntity[GUNS.length];
	private final int[] cooldown = new int[GUNS.length];
	private final boolean[] barrel = new boolean[GUNS.length];
	/** The door guns' barrels: ticks spun up (they fire only once spun up) and ticks still to spin with nothing to shoot. */
	private final int[] spunUp = new int[2], spinLeft = new int[2];
	private final float[] aimYaw = new float[GUNS.length], aimPitch = new float[GUNS.length];
	private final CrewShipFlight.Pose pose = new CrewShipFlight.Pose();
	/** Players it's after (until when: game time). */
	private final java.util.Map<UUID, Long> angry = new java.util.HashMap<>();
	/** The client's own: its engine sounds and the light round it (the renderer's). Unused on the server. */
	public @Nullable Object clientSound, clientLight;
	/** When the client last sampled the light round it (game time; the renderer's). */
	public long clientLightAt = Long.MIN_VALUE / 2;
	/**
	 * The client's own, as drawn: the door guns' barrel clusters' turn and how fast they're turning (radians, radians a
	 * tick: at full, one barrel on for each shot, so the cluster and the shots keep step), the heavy guns' barrels' kick
	 * (ticks since each fired: guns 2 to 5, left and right), and the flight deck's door's opening (0 shut, 1 open: it
	 * slides aside as a player comes near), each with the last tick's for between ticks.
	 */
	public final float[] spinAngle = new float[2], spinAngleO = new float[2], spinRate = new float[2];
	public final int[] kickAge = {99, 99, 99, 99, 99, 99, 99, 99};
	private final int[] seenShots = {-1, -1, -1, -1};
	public float fdoor, fdoorO;
	/** The flight deck's door: where its doorway is (the ship's frame), how near (blocks) a player opens it, and how far
	 * it slides aside (tools/crew_ship.py DOOR_SLIDE); a tick's travel open or shut. */
	public static final float FDOOR_X = 0, FDOOR_Y = G + 3.25F, FDOOR_Z = -11.125F, FDOOR_NEAR = 2.8F, DOOR_SLIDE = 2.32F, FDOOR_STEP = 0.12F;
	/** Its hull in the world while it's down (CrewShipHull), and what it was built for. */
	CrewShipHull.@Nullable Shape hull;
	/** A pickup's: the squad it's come for and whose it is, who it's called aboard so far (in seat order), and who's
	 * aboard in each seat (skin;slim;gear, empty while that seat's still to be filled). */
	private @Nullable UUID pickupSquad, pickupOwner;
	private final List<UUID> called = new ArrayList<>();
	private final List<String> aboard = new ArrayList<>();
	/** How long (ticks, with the ramp down) a pickup waits for its squad at most. */
	public static final int PICKUP_WAIT = 3000;

	public CrewShip(EntityType<? extends CrewShip> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
		for (int i = 0; i < GUNS.length; i++) {
			aimYaw[i] = GUNS[i].yaw();
			aimPitch[i] = GUNS[i].pitch();
		}
	}

	/** A ship for the tower at {@code core} bringing {@code posts} ({@code hires}: who, for each), to land at {@code site}. */
	public void setUp(BlockPos core, CrewShipSite.Site site, List<String> posts, List<CrewRoster.Hire> hires, long now) {
		this.core = core.immutable();
		this.entrance = site.entrance();
		manifest.clear();
		manifest.addAll(posts);
		this.hires.clear();
		this.hires.addAll(hires);
		entityData.set(PASSENGERS, passengers());
		snapTo(site.land().x, site.land().y, site.land().z, 0, 0);
		entityData.set(YAW, site.yaw());
		entityData.set(ROOF, site.roof());
		entityData.set(REACH, site.reach());
		entityData.set(CEILING, (float) Math.max(60, Math.min(CrewShipFlight.CEILING, level().getMaxY() + 60 - site.land().y)));
		entityData.set(PHASE, CrewShipFlight.ENTRY);
		entityData.set(PHASE_START, now);
	}

	/** A ship picking up drop squad {@code squad} (player {@code owner}'s), to land at {@code site} (its ramp toward them). */
	public void setUpPickup(CrewShipSite.Site site, UUID owner, UUID squad, long now) {
		setUp(BlockPos.ZERO, site, List.of(), List.of(), now);
		pickupOwner = owner;
		pickupSquad = squad;
		called.clear();
		aboard.clear();
		entityData.set(PICKUP, true);
		entityData.set(PASSENGERS, "");
	}

	/** Whether it's come to pick up a drop squad. */
	public boolean pickup() {
		return entityData.get(PICKUP);
	}

	public @Nullable UUID pickupSquad() {
		return pickupSquad;
	}

	/** A ship set down to stay at {@code land} facing {@code yaw} (its stern toward (sin yaw, cos yaw)): down, open, lit,
	 * on guard, never leaving. */
	public void park(Vec3 land, float yaw, long now) {
		core = BlockPos.ZERO;
		entrance = land;
		manifest.clear();
		snapTo(land.x, land.y, land.z, 0, 0);
		entityData.set(YAW, yaw);
		entityData.set(ROOF, false);
		entityData.set(REACH, CrewShipFlight.HINGE_Y);
		entityData.set(PHASE, CrewShipFlight.PARKED);
		entityData.set(PHASE_START, now);
	}

	public boolean parked() {
		return phase() == CrewShipFlight.PARKED;
	}

	/** A player hurt one of the crew: after them a while. */
	public void angerAt(Player player, long until) {
		angry.merge(player.getUUID(), until, Math::max);
	}

	/** A player hurt crew member {@code hurt}: every drop ship near goes after them. */
	public static void alert(ServerLevel level, Entity hurt, Player player) {
		if (player.isCreative() || player.isSpectator()) {
			return;
		}
		for (CrewShip s : level.getEntitiesOfClass(CrewShip.class, hurt.getBoundingBox().inflate(ALERT), CrewShip::isAlive)) {
			s.angerAt(player, level.getGameTime() + ANGER_TICKS);
		}
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(PHASE, CrewShipFlight.ENTRY);
		builder.define(PHASE_START, 0L);
		builder.define(YAW, 0.0F);
		builder.define(ROOF, false);
		builder.define(REACH, CrewShipFlight.HINGE_Y);
		builder.define(CEILING, CrewShipFlight.CEILING);
		builder.define(PASSENGERS, "");
		builder.define(PICKUP, false);
		builder.define(SPIN, (byte) 0);
		builder.define(SHOTS, 0);
		for (int i = 0; i < AIMS.length; i++) {
			builder.define(AIMS[i], new Vector3f(GUNS[i].yaw(), GUNS[i].pitch(), 0));
		}
	}

	public int phase() {
		return entityData.get(PHASE);
	}

	public long phaseStart() {
		return entityData.get(PHASE_START);
	}

	public float shipYaw() {
		return entityData.get(YAW);
	}

	public boolean roof() {
		return entityData.get(ROOF);
	}

	public float reach() {
		return entityData.get(REACH);
	}

	public float ceiling() {
		return entityData.get(CEILING);
	}

	public BlockPos core() {
		return core;
	}

	/** Who it brings (or, a pickup, who's aboard), seat by seat (the client's: skin, slim arms, gear bits; null for a seat
	 * nobody's in); empty for a ship that carries nobody. */
	public List<CrewRoster.@Nullable Look> passengerLooks() {
		List<CrewRoster.@Nullable Look> out = new ArrayList<>();
		String all = entityData.get(PASSENGERS);
		if (all.isEmpty()) {
			return out;
		}
		for (String one : all.split("\\|", -1)) {
			String[] f = one.split(";");
			CrewRoster.Look look = null;
			if (f.length == 3) {
				try {
					look = new CrewRoster.Look(f[0], Integer.parseInt(f[2]), Boolean.parseBoolean(f[1]));
				} catch (NumberFormatException ignored) {
					// (not one of ours)
				}
			}
			out.add(look);
		}
		return out;
	}

	private String passengers() {
		StringBuilder b = new StringBuilder();
		for (CrewRoster.Hire h : hires) {
			if (b.length() > 0) {
				b.append('|');
			}
			b.append(h.look().skin()).append(';').append(h.look().slim()).append(';').append(h.look().gear());
		}
		return b.toString();
	}

	/** The seats, aft first (see {@link #SEATS}). */
	private static float[][] seats() {
		List<float[]> out = new ArrayList<>();
		float[] wall = {15.4F, 14.2F, 13.0F, 11.8F, 10.6F, 9.4F, 8.2F, 7.0F, 5.8F, 4.6F, 3.4F, -3.8F, -5.0F, -6.2F, -7.4F, -8.6F, -9.8F};
		for (float z : wall) {
			for (int side : new int[] {-1, 1}) {
				out.add(new float[] {side * 4.86F, G + 2.02F, z, -side, 0});
			}
		}
		for (float x : new float[] {2.1F, 3.25F, 4.4F}) {
			for (int side : new int[] {-1, 1}) {
				out.add(new float[] {side * x, G + 2.05F, -10.68F, 0, 1});
			}
		}
		return out.toArray(new float[0][]);
	}

	/** Place {@code i} aboard, in the ship's frame: x, y (a seat's cushion top; past the seats, the bay's floor in the aisle),
	 * z, and the way they face (x, z). */
	public static float[] place(int i) {
		if (i < SEATS.length) {
			return SEATS[i];
		}
		int k = i - SEATS.length;
		return new float[] {(k % 2 == 0 ? -1 : 1) * 1.2F, G + 1.0F, 15.0F - (k / 2) * 1.1F, 0, 1};
	}

	/** Where place {@code i} is (world), landed. */
	public Vec3 seatAt(int i) {
		float[] s = place(i);
		return world(landedPose(), s[0], s[1], s[2]);
	}

	/** The way the one in place {@code i} faces (degrees), landed. */
	public float seatYaw(int i) {
		CrewShipFlight.Pose landed = landedPose();
		float[] s = place(i);
		Vec3 a = world(landed, s[0], 0, s[2]), b = world(landed, s[0] + s[3], 0, s[2] + s[4]);
		return (float) (Mth.atan2(b.z - a.z, b.x - a.x) * Mth.RAD_TO_DEG) - 90.0F;
	}

	/** How near (blocks, from the eyes) a player must be to sit in a seat. */
	public static final double SIT_REACH = 4.5;

	/** Whether players may sit in the bay's seats (Justin's pick, 9 November 2026: "Right-click to sit"): while it's down
	 * with the ramp open; everyone is stood up as it closes, before it lifts off. */
	public boolean seatsOpen() {
		int ph = phase();
		return ph == CrewShipFlight.UNLOAD || ph == CrewShipFlight.PARKED || testHold && ph >= CrewShipFlight.OPEN && ph < CrewShipFlight.CLOSE;
	}

	/** Seats {@code player} in seat {@code i} (a right-click on it, CrewShipSeatPayload), if it's free, near enough and the
	 * seats are open. */
	public boolean sit(Player player, int i) {
		if (i < 0 || i >= SEATS.length || !seatsOpen() || player.isPassenger()) {
			return false;
		}
		Vec3 at = seatAt(i);
		if (player.getEyePosition().distanceToSqr(at) > SIT_REACH * SIT_REACH) {
			return false;
		}
		float[] s = place(i);
		Vec3 up = world(landedPose(), s[0] + s[3] * 0.75F, G + 1.0F, s[2] + s[4] * 0.75F);
		return io.github.jimbozoomer.jugcraft.tower.SeatEntity.sitInShip(level(), player, at, seatYaw(i), up, getId());
	}

	private CrewShipFlight.Pose landedPose() {
		return CrewShipFlight.pose(CrewShipFlight.UNLOAD, 0, shipYaw(), roof(), reach(), ceiling(), new CrewShipFlight.Pose());
	}

	/** The way off from place {@code i} (world): a step out into the bay, across to the aisle, along it aft to the ramp, down
	 * it, and a step past its end. */
	public List<Vec3> wayFrom(int i) {
		CrewShipFlight.Pose landed = landedPose();
		float[] s = place(i);
		boolean seated = i < SEATS.length;
		double z = seated ? s[2] + s[4] * 0.8 : s[2];
		List<Vec3> out = new ArrayList<>();
		out.add(world(landed, seated ? s[0] + s[3] * 0.8 : s[0], G + 1.0, z));
		out.add(world(landed, 0, G + 1.0, z));
		List<Vec3> off = wayOff();
		for (int k = z > 6.0 ? 1 : 0; k < off.size(); k++) {
			out.add(off.get(k));
		}
		return out;
	}

	/** Whether it's still on the ground (its guards keep the ramp's foot till it lifts off). */
	public boolean guarded() {
		return isAlive() && phase() < CrewShipFlight.LIFT;
	}

	/** The next guard spot at the ramp's foot for the security who came with it. */
	public int nextEscort() {
		return escorts++;
	}

	/** Gun {@code i}'s aim (yaw, pitch; radians, the ship's frame). */
	public Vector3fc aim(int i) {
		return entityData.get(AIMS[i]);
	}

	/** The pose {@code t} ticks into the phase. */
	public CrewShipFlight.Pose pose(float t, CrewShipFlight.Pose out) {
		return CrewShipFlight.pose(phase(), t, shipYaw(), roof(), reach(), ceiling(), out);
	}

	/** A point of the ship's frame in the world at {@code p}. */
	public Vec3 world(CrewShipFlight.Pose p, double x, double y, double z) {
		return position().add(p.world(x, y, z));
	}

	// ---------------------------------------------------------------- the visit

	@Override
	public void tick() {
		super.tick();
		CrewShipHull.track(this);                         // solid while it's down (both sides: players move on theirs)
		if (!(level() instanceof ServerLevel level)) {
			clientTick();
			return;
		}
		long now = level.getGameTime();
		int phase = phase();
		int t = (int) (now - phaseStart());
		int length = phase < CrewShipFlight.LENGTH.length ? CrewShipFlight.LENGTH[phase] : 0;
		if (phase == CrewShipFlight.PARKED || testHold) {
			// set down to stay (or a game test holding it where it is): nothing to do but stand guard
		} else if (phase == CrewShipFlight.UNLOAD) {
			if (pickup() ? load(level, now) : unload(level, now)) {
				next(level, CrewShipFlight.CLOSE, now);
				return;
			}
		} else if (phase >= CrewShipFlight.GONE) {
			discard();
			return;
		} else if (t >= length) {
			next(level, phase + 1, now);
			return;
		}
		pose(t, pose);
		if (pose.lights && !lightsOn) {
			lightsOn = true;
			if (lights.isEmpty()) {
				lights.addAll(CrewShipLights.get(level).place(level, lightCells(level)));
			} else {
				CrewShipLights.get(level).place(level, new ArrayList<>(lights));
			}
		} else if (!pose.lights && lightsOn) {
			lightsOff(level);
		}
		guns(level, now);
	}

	/** The client's moving parts, each tick: the door guns' barrels, the heavy guns' kick, the flight deck's door. */
	private void clientTick() {
		float full = SPIN_STEP / (GUN_COOLDOWN[0] + 1);          // a barrel on a shot, a shot every cooldown + 1 ticks
		for (int i = 0; i < 2; i++) {
			if (spinAngle[i] > 1000) {
				spinAngle[i] -= Mth.TWO_PI * 100;
			}
			spinAngleO[i] = spinAngle[i];
			spinRate[i] = spinning(i) ? Math.min(full, spinRate[i] + full / SPIN_UP) : Math.max(0, spinRate[i] - full / SPIN_DOWN);
			spinAngle[i] += spinRate[i];
		}
		for (int k = 0; k < kickAge.length; k++) {
			kickAge[k] = Math.min(99, kickAge[k] + 1);
		}
		for (int i = 2; i < GUNS.length; i++) {
			int n = shots(i), seen = seenShots[i - 2];
			if (seen >= 0 && n != seen) {
				// (the server fires the right barrel on odd shots, the left on even; a gun with one barrel kicks its one)
				boolean right = (n & 1) == 1 && GUNS[i].spread() > 0;
				kickAge[(i - 2) * 2 + (right ? 1 : 0)] = 0;
			}
			seenShots[i - 2] = n;
		}
		fdoorO = fdoor;
		boolean near = false;
		if (phase() >= CrewShipFlight.TOUCHDOWN && phase() <= CrewShipFlight.CLOSE || phase() == CrewShipFlight.PARKED) {
			Vec3 at = world(landedPose(), FDOOR_X, FDOOR_Y, FDOOR_Z);
			for (Player p : level().players()) {
				Vec3 c = p.position();
				if (Math.abs(c.y + 0.9 - at.y) < 2.5 && Mth.square(c.x - at.x) + Mth.square(c.z - at.z) < FDOOR_NEAR * FDOOR_NEAR) {
					near = true;
					break;
				}
			}
		}
		fdoor = Mth.clamp(fdoor + (near ? FDOOR_STEP : -FDOOR_STEP), 0, 1);
	}

	private void next(ServerLevel level, int phase, long now) {
		entityData.set(PHASE, phase);
		entityData.set(PHASE_START, now);
		if (phase == CrewShipFlight.OPEN || phase == CrewShipFlight.CLOSE) {
			// the stern door and the ramp's hydraulics
			Vec3 at = world(CrewShipFlight.pose(CrewShipFlight.UNLOAD, 0, shipYaw(), roof(), reach(), ceiling(), new CrewShipFlight.Pose()), 0, G + 3, CrewShipFlight.STERN);
			level.playSound(null, at.x, at.y, at.z, io.github.jimbozoomer.jugcraft.tower.JugcraftTower.SHIP_RAMP, SoundSource.NEUTRAL, 1.6F, 1.0F);
		}
		if (phase == CrewShipFlight.OPEN) {
			board(level);
			callEscorts(level);
		} else if (phase == CrewShipFlight.UNLOAD) {
			lastOff = now - CREW_EVERY;
		}
	}

	private void lightsOff(ServerLevel level) {
		lightsOn = false;
		CrewShipLights.get(level).remove(level, lights);
		lights.clear();
	}

	@Override
	public void remove(RemovalReason reason) {
		if (reason.shouldDestroy() && level() instanceof ServerLevel level && lightsOn) {
			lightsOff(level);
		}
		super.remove(reason);
		CrewShipHull.untrack(this);
	}

	@Override
	public void onClientRemoval() {
		super.onClientRemoval();
		CrewShipHull.untrack(this);
	}

	/** Where its floodlights' light goes: under each lamp's beam, in the bay, at the ramp's foot, and a spread over the
	 * landing zone, the ramp and its approach every 6 blocks (each on the ground there, or the deck). */
	List<BlockPos> lightCells(ServerLevel level) {
		CrewShipFlight.Pose landed = CrewShipFlight.pose(CrewShipFlight.UNLOAD, 0, shipYaw(), roof(), reach(), ceiling(), new CrewShipFlight.Pose());
		List<BlockPos> out = new ArrayList<>();
		for (float[] l : LAMPS) {
			Vec3 p = world(landed, l[0], l[1], l[2]);
			Vec3 d = landed.world(l[3], l[4], l[5]).subtract(landed.world(0, 0, 0)).normalize();
			double down = Math.max(1, p.y - ground(level, p.x, p.z));
			double along = Math.min(BEAM_REACH, down / Math.max(0.05, -d.y));
			Vec3 hit = p.add(d.scale(along));
			out.add(onGround(level, hit.x, hit.z));
			// an outward beam lights its whole pool, and the ground on the way out to it
			Vec3 flat = new Vec3(d.x, 0, d.z);
			double reach = Math.hypot(hit.x - p.x, hit.z - p.z);
			if (reach > 6 && flat.lengthSqr() > 1e-4) {
				flat = flat.normalize();
				Vec3 side = new Vec3(-flat.z, 0, flat.x);
				for (double f : new double[] {0.5, 0.75}) {
					Vec3 w = p.add(flat.scale(reach * f));
					out.add(onGround(level, w.x, w.z));
				}
				for (double[] o : new double[][] {{4, 0}, {0, 4}, {0, -4}}) {
					Vec3 w = hit.add(flat.scale(o[0])).add(side.scale(o[1]));
					out.add(onGround(level, w.x, w.z));
				}
			}
		}
		out.add(BlockPos.containing(world(landed, 0, G + 2, 0)));
		out.add(BlockPos.containing(world(landed, 0, G + 2, 12)));
		Vec3 foot = world(landed, 0, 0, CrewShipFlight.HINGE + CrewShipFlight.LEAF * 0.95);
		out.add(onGround(level, foot.x, foot.z));
		for (float lx = -15; lx <= 15.01F; lx += 6) {
			for (float lz = -33; lz <= 45.01F; lz += 6) {
				if ((lx / 17) * (lx / 17) + ((lz - 6) / 41) * ((lz - 6) / 41) <= 1) {
					Vec3 w = world(landed, lx, 0, lz);
					out.add(onGround(level, w.x, w.z));
				}
			}
		}
		return out;
	}

	private static int ground(ServerLevel level, double x, double z) {
		return level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
	}

	private static BlockPos onGround(ServerLevel level, double x, double z) {
		return new BlockPos(Mth.floor(x), ground(level, x, z), Mth.floor(z));
	}

	// ---------------------------------------------------------------- the crew

	/** The way off (world): from the bay, along the deck to the ramp's hinge, down the ramp, a step past its end. */
	public List<Vec3> wayOff() {
		CrewShipFlight.Pose landed = CrewShipFlight.pose(CrewShipFlight.UNLOAD, 0, shipYaw(), roof(), reach(), ceiling(), new CrewShipFlight.Pose());
		float a = CrewShipFlight.leafDrop(reach());
		double endY = CrewShipFlight.rampY(a, CrewShipFlight.LEAF), endZ = CrewShipFlight.rampZ(a, CrewShipFlight.LEAF);
		return List.of(world(landed, 0, G + 1.0, 6.0), world(landed, 0, G + 1.0, CrewShipFlight.HINGE - 0.6),
				world(landed, 0, CrewShipFlight.rampY(a, 0.4), CrewShipFlight.rampZ(a, 0.4)), world(landed, 0, endY, endZ),
				world(landed, 0, endY, endZ + 0.9));
	}

	/**
	 * Seats who it brings, as it touches down and before the ramp opens (C4): each in their seat (security nearest the
	 * ramp), hired now in the faces they were seen in, sitting till called off.
	 */
	private void board(ServerLevel level) {
		if (!crew.isEmpty() || !(level.getBlockEntity(core) instanceof TowerCoreBlockEntity tower)) {
			return;
		}
		delivered = 0;
		for (int i = 0; i < manifest.size(); i++) {
			TowerCrew c = TowerCrewCare.board(level, tower, manifest.get(i), i < hires.size() ? hires.get(i) : null, seatAt(i), seatYaw(i),
					i < SEATS.length, getUUID());
			crew.add(c == null ? new UUID(0, 0) : c.getUUID());
		}
	}

	/** Calls the crew off their seats one by one, in order (waiting while hostiles are near the ramp); true once they're all off it. */
	private boolean unload(ServerLevel level, long now) {
		if (!(level.getBlockEntity(core) instanceof TowerCoreBlockEntity)) {
			return true;                                // the tower's gone: nobody to bring (they get up when it goes)
		}
		List<Vec3> way = wayOff();
		Vec3 foot = way.get(way.size() - 1);
		if (delivered < crew.size()) {
			if (now - lastOff >= CREW_EVERY && !hostileNear(level, foot, HOLD_NEAR)) {
				int i = delivered++;
				lastOff = now;
				if (level.getEntity(crew.get(i)) instanceof TowerCrew c && c.isAlive() && c.aboard()) {
					c.disembark(wayFrom(i), entrance, getUUID());
				}
			}
			return false;
		}
		for (UUID id : crew) {
			if (level.getEntity(id) instanceof TowerCrew c && c.isAlive() && (c.onRamp() || c.aboard())) {
				return now - lastOff > 1200;            // someone still coming down (or stuck: go anyway)
			}
		}
		return now - lastOff >= CREW_EVERY;
	}

	// ---------------------------------------------------------------- a pickup

	/** The pickup's squad near (sent home to it: their order and this ship), not yet called aboard. */
	private List<io.github.jimbozoomer.jugcraft.tower.squad.Legionary> squadWaiting(ServerLevel level) {
		List<io.github.jimbozoomer.jugcraft.tower.squad.Legionary> out = level.getEntitiesOfClass(io.github.jimbozoomer.jugcraft.tower.squad.Legionary.class,
				getBoundingBox().inflate(160), l -> l.isAlive() && pickupSquad != null && pickupSquad.equals(l.squad()) && l.goingHome(getUUID())
						&& !called.contains(l.getUUID()));
		// the leader last, the rest in their order
		out.sort((a, b) -> a.leader() != b.leader() ? (a.leader() ? 1 : -1) : Integer.compare(a.place(), b.place()));
		return out;
	}

	/**
	 * Calls its squad aboard one by one, in order, the leader last, each {@value #PICKUP_EVERY} ticks after the last is on
	 * the ramp (waiting only while a monster is within {@value #PICKUP_HOLD_NEAR} blocks of the ramp's foot); true once
	 * they're all aboard (or gone, or given up), or it's waited {@value #PICKUP_WAIT} ticks.
	 */
	private boolean load(ServerLevel level, long now) {
		if (now - phaseStart() > PICKUP_WAIT) {
			return true;
		}
		List<io.github.jimbozoomer.jugcraft.tower.squad.Legionary> waiting = squadWaiting(level);
		Vec3 foot = wayOff().get(wayOff().size() - 1);
		if (!waiting.isEmpty()) {
			io.github.jimbozoomer.jugcraft.tower.squad.Legionary next = waiting.get(0);
			// one at a time to the ramp: the next once the last is on it
			for (UUID id : called) {
				if (level.getEntity(id) instanceof io.github.jimbozoomer.jugcraft.tower.squad.Legionary l && l.isAlive() && l.toRamp()) {
					return false;
				}
			}
			// (a soldier in a fight is called all the same: boarding comes before the rifle; only a monster at the ramp
			// itself holds them)
			if (now - lastOff >= PICKUP_EVERY && !next.dazed() && !hostileNear(level, foot, PICKUP_HOLD_NEAR)) {
				lastOff = now;
				int seat = called.size();
				called.add(next.getUUID());
				next.embark(getUUID(), wayOn(seat));
			}
			return false;
		}
		for (UUID id : called) {
			if (level.getEntity(id) instanceof io.github.jimbozoomer.jugcraft.tower.squad.Legionary l && l.isAlive() && l.boarding()) {
				return false;                               // someone still on their way up
			}
		}
		return now - lastOff >= PICKUP_EVERY && (now - phaseStart() > 100 || !called.isEmpty());
	}

	/** The way aboard to place {@code i} (world): from just past the ramp's end, up it, along the deck and the aisle to the
	 * seat (the way off it, backward). */
	public List<Vec3> wayOn(int i) {
		List<Vec3> way = new ArrayList<>(wayFrom(i));
		java.util.Collections.reverse(way);
		return way;
	}

	/** {@code l} has come up to their seat: aboard (seen sitting there from now on: the client draws them). */
	public void aboard(io.github.jimbozoomer.jugcraft.tower.squad.Legionary l) {
		int seat = called.indexOf(l.getUUID());
		if (seat < 0) {
			return;
		}
		while (aboard.size() <= seat) {
			aboard.add("");
		}
		aboard.set(seat, l.skin() + ";" + l.slim() + ";" + l.gear());
		entityData.set(PASSENGERS, String.join("|", aboard));
		l.discard();
	}

	/** Calls out up to two of the tower's security to stand at the ramp's foot while the crew come off and till it lifts
	 * off, if none come with it. */
	private void callEscorts(ServerLevel level) {
		if (!(level.getBlockEntity(core) instanceof TowerCoreBlockEntity tower)) {
			return;
		}
		for (String id : manifest) {
			for (CrewPost p : tower.layout().crew()) {
				if (p.id().equals(id) && p.job().equals("security")) {
					return;                             // they bring their own
				}
			}
		}
		List<TowerCrew> guards = level.getEntitiesOfClass(TowerCrew.class, getBoundingBox().inflate(160),
				c -> c.isAlive() && c.core().equals(core) && c.job().equals("security") && !TowerCrew.asleep(c.act()) && !c.isPassenger());
		guards.sort((a, b) -> Double.compare(a.distanceToSqr(this), b.distanceToSqr(this)));
		for (int i = 0; i < Math.min(2, guards.size()); i++) {
			guards.get(i).escort(getUUID(), escortSpot(level, nextEscort()), entrance);
		}
	}

	/** Where escort {@code i} stands: beside the ramp's foot, facing out. */
	public Vec3 escortSpot(ServerLevel level, int i) {
		CrewShipFlight.Pose landed = CrewShipFlight.pose(CrewShipFlight.UNLOAD, 0, shipYaw(), roof(), reach(), ceiling(), new CrewShipFlight.Pose());
		float z = CrewShipFlight.HINGE + CrewShipFlight.LEAF + 0.5F;
		Vec3 w = world(landed, (i % 2 == 0 ? -1 : 1) * 3.4, 0, z + (i / 2) * 2);
		return new Vec3(w.x, ground(level, w.x, w.z), w.z);
	}

	private static boolean hostileNear(ServerLevel level, Vec3 at, double r) {
		return !level.getEntitiesOfClass(Mob.class, new AABB(at, at).inflate(r), m -> m.isAlive() && m instanceof Enemy).isEmpty();
	}

	// ---------------------------------------------------------------- the guns

	private void guns(ServerLevel level, long now) {
		for (int i = 0; i < GUNS.length; i++) {
			Gun g = GUNS[i];
			// (the door guns only once their booms are right out of the doors; 4, 5: the wings')
			boolean active = pose.guns && (i >= 2 || boom(pose.sides) > 0.85F) && (i != 3 || CrewShipFlight.rampOpen(pose));
			LivingEntity target = targets[i];
			if (!active) {
				target = null;
			} else if (target == null || !target.isAlive() || target.isRemoved() || !wanted(target, now)
					|| (now + i) % 10 == 0 && !canShoot(level, i, target)) {
				target = (now + i) % 5 == 0 ? pick(level, i, now) : null;
			}
			targets[i] = target;
			float wantYaw = g.yaw(), wantPitch = g.pitch();
			if (target != null) {
				Vec3 d = toShip(target.getBoundingBox().getCenter()).subtract(g.x(), g.y(), g.z());
				wantYaw = (float) Math.atan2(-d.x, -d.z);
				wantPitch = (float) Math.atan2(d.y, Math.hypot(d.x, d.z));
			}
			float dy = Mth.wrapDegrees((wantYaw - aimYaw[i]) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
			float dp = wantPitch - aimPitch[i];
			aimYaw[i] += Mth.clamp(dy, -TURN, TURN);
			aimPitch[i] += Mth.clamp(dp, -TURN * 0.75F, TURN * 0.75F);
			Vector3fc old = entityData.get(AIMS[i]);
			if (Math.abs(old.x() - aimYaw[i]) > 0.005F || Math.abs(old.y() - aimPitch[i]) > 0.005F) {
				entityData.set(AIMS[i], new Vector3f(aimYaw[i], aimPitch[i], 0));
			}
			boolean ready = true;
			if (i < 2) {                                    // the door guns' barrels spin up before the first shot, and run on a while
				spinLeft[i] = target != null ? SPIN_DOWN : Math.max(0, spinLeft[i] - 1);
				spunUp[i] = spinLeft[i] > 0 ? Math.min(SPIN_UP, spunUp[i] + 1) : 0;
				ready = spunUp[i] >= SPIN_UP;
			}
			if (cooldown[i] > 0) {
				cooldown[i]--;
			} else if (ready && target != null && Math.abs(dy) < 0.06F && Math.abs(dp) < 0.06F) {
				fire(level, i, target);
				cooldown[i] = GUN_COOLDOWN[i];
			}
		}
		byte spin = (byte) ((spinLeft[0] > 0 ? 1 : 0) | (spinLeft[1] > 0 ? 2 : 0));
		if (entityData.get(SPIN) != spin) {
			entityData.set(SPIN, spin);
		}
	}

	/** Whether door gun {@code i}'s barrels are spinning (the client's view). */
	public boolean spinning(int i) {
		return (entityData.get(SPIN) & (1 << i)) != 0;
	}

	/** Heavy gun {@code i}'s (2 to 5) shots so far, four bits (the client's view). */
	public int shots(int i) {
		return (entityData.get(SHOTS) >> (4 * (i - 2))) & 15;
	}

	/** A world point in the ship's frame (as it stands now). */
	private Vec3 toShip(Vec3 w) {
		Vec3 r = w.subtract(position()).subtract(pose.x, pose.y, pose.z);
		double c = Math.cos(pose.yaw), s = Math.sin(pose.yaw);
		return new Vec3(r.x * c - r.z * s, r.y, r.x * s + r.z * c);
	}

	/** Gun {@code i}'s position in the world (where it turns), the door guns where their arms put them. */
	private Vec3 gunAt(int i) {
		Gun g = GUNS[i];
		return world(pose, i < 2 ? gunX(i, pose.sides) : g.x(), g.y(), g.z());
	}

	/** The muzzle of gun {@code i}'s barrel {@code right} (world), aimed as it is now. */
	private Vec3 muzzle(int i, boolean right) {
		Gun g = GUNS[i];
		float k = g.scale(), sy = g.hung() ? -1 : 1;
		Vector3f m = new Vector3f((right ? g.spread() : -g.spread()) * k, 0.76F * sy * k, -(g.barrel() + 0.2F) * k);
		Vector3f pv = new Vector3f(0, 0.75F * sy * k, -0.05F * k);
		m.sub(pv).rotateX(drawnPitch(i, aimPitch[i], pose.sides)).add(pv).rotateY(drawnYaw(i, aimYaw[i], pose.sides));
		double x = i < 2 ? gunX(i, pose.sides) : g.x();
		return world(pose, x + m.x, g.y() + m.y, g.z() + m.z);
	}

	/** Whether gun {@code i} could shoot {@code e}: in range, on its side (the door guns), far enough out for the top
	 * turret to see past the hull, behind the ship for the bay's, and nothing solid in the way. */
	private boolean canShoot(ServerLevel level, int i, LivingEntity e) {
		Vec3 c = e.getBoundingBox().getCenter();
		Vec3 at = gunAt(i);
		if (c.distanceToSqr(at) > RANGE * RANGE) {
			return false;
		}
		Vec3 s = toShip(c);
		if (pose.zone && !CrewShipFlight.inZone(s.x, s.z)) {
			return false;                               // while it's down, nothing outside the STAY BACK ring
		}
		boolean side = switch (i) {
			case 0 -> s.x < -2;
			case 1 -> s.x > 2;
			case 2 -> Math.hypot(s.x, s.z) > 18;
			case 4 -> s.x < -4 && s.z < 10;                     // the wings' ball turrets: their own side, ahead of the thrusters
			case 5 -> s.x > 4 && s.z < 10;
			default -> s.z > CrewShipFlight.STERN + 1;
		};
		if (!side) {
			return false;
		}
		Vec3 from = at.add(c.subtract(at).normalize().scale(GUNS[i].barrel() * GUNS[i].scale() + 0.5));
		return level.clip(new ClipContext(from, c, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty())).getType() == HitResult.Type.MISS;
	}

	/** Whether it's after {@code e}: a hostile mob, or a player who hurt the crew or is inside its ring (not in creative
	 * or spectator). */
	private boolean wanted(LivingEntity e, long now) {
		if (e instanceof Player p) {
			if (p.isCreative() || p.isSpectator()) {
				return false;
			}
			Long until = angry.get(p.getUUID());
			if (until != null && until > now) {
				return true;
			}
			if (pickup()) {
				return false;                               // (a pickup, called by a player: nobody shot for coming near)
			}
			Vec3 s = toShip(p.position());
			return pose.zone && CrewShipFlight.inZone(s.x, s.z) && s.y < 12;
		}
		return e instanceof Enemy;
	}

	private @Nullable LivingEntity pick(ServerLevel level, int i, long now) {
		Vec3 at = gunAt(i);
		LivingEntity best = null;
		double bestD = Double.MAX_VALUE;
		angry.values().removeIf(until -> until <= now);
		for (LivingEntity m : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(RANGE),
				m -> m.isAlive() && (m instanceof Enemy || m instanceof Player) && !(m instanceof TowerCrew))) {
			double d = m.distanceToSqr(at);
			if (d < bestD && wanted(m, now) && canShoot(level, i, m)) {
				bestD = d;
				best = m;
			}
		}
		return best;
	}

	private void fire(ServerLevel level, int i, LivingEntity target) {
		barrel[i] = !barrel[i];
		if (i >= 2) {                                   // (the client kicks the barrel that fired back)
			int sh = 4 * (i - 2), all = entityData.get(SHOTS);
			entityData.set(SHOTS, (all & ~(15 << sh)) | ((((all >> sh) & 15) + 1) & 15) << sh);
		}
		Vec3 from = muzzle(i, barrel[i]);
		Vec3 to = target.getBoundingBox().getCenter();
		PlasmaBolt bolt = new PlasmaBolt(level, this, from, target);
		bolt.damage = GUN_DAMAGE[i];
		Vec3 d = to.subtract(from);
		bolt.shoot(d.x, d.y, d.z, PlasmaRifle.SPEED, i < 2 ? 1.2F : 0.4F);
		level.addFreshEntity(bolt);
		// the rotary door guns' rattle a little quieter and higher than the turrets' heavy cracks
		level.playSound(null, from.x, from.y, from.z, PlasmaRifle.FIRE, SoundSource.NEUTRAL, i < 2 ? 1.2F : 2.4F,
				(i < 2 ? 1.25F : 0.8F) + level.getRandom().nextFloat() * 0.12F);
	}

	// ---------------------------------------------------------------- the entity

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean canBeCollidedWith(@Nullable Entity entity) {
		return false;
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	@Override
	public boolean canUsePortal(boolean allowPassengers) {
		return false;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return distance < 640 * 640;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.store("core", BlockPos.CODEC, core);
		output.store("entrance", Vec3.CODEC, entrance);
		output.store("manifest", Codec.STRING.listOf(), new ArrayList<>(manifest));
		output.store("hires", Codec.STRING.listOf(), hires.stream().map(h -> h.look().skin() + ";" + h.look().slim() + ";" + h.look().gear() + ";" + h.name()).toList());
		output.putInt("delivered", delivered);
		output.store("crew", UUIDUtil.CODEC.listOf(), new ArrayList<>(crew));
		output.store("lights", BlockPos.CODEC.listOf(), new ArrayList<>(lights));
		output.putInt("phase", phase());
		output.putLong("phase_start", phaseStart());
		output.putFloat("yaw", shipYaw());
		output.putBoolean("roof", roof());
		output.putFloat("reach", reach());
		output.putInt("ramp", 2);                     // (reach: the ramp's hinge over the ground since the rework)
		output.putFloat("ceiling", ceiling());
		if (pickup() && pickupSquad != null && pickupOwner != null) {
			output.store("pickup_squad", UUIDUtil.CODEC, pickupSquad);
			output.store("pickup_owner", UUIDUtil.CODEC, pickupOwner);
			output.store("called", UUIDUtil.CODEC.listOf(), new ArrayList<>(called));
			output.store("aboard", Codec.STRING.listOf(), new ArrayList<>(aboard));
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		core = input.read("core", BlockPos.CODEC).orElse(BlockPos.ZERO);
		entrance = input.read("entrance", Vec3.CODEC).orElse(position());
		manifest.clear();
		manifest.addAll(input.read("manifest", Codec.STRING.listOf()).orElse(List.of()));
		hires.clear();
		for (String h : input.read("hires", Codec.STRING.listOf()).orElse(List.of())) {
			String[] f = h.split(";", 4);
			if (f.length == 4) {
				try {
					hires.add(new CrewRoster.Hire(new CrewRoster.Look(f[0], Integer.parseInt(f[2]), Boolean.parseBoolean(f[1])), f[3]));
				} catch (NumberFormatException ignored) {
					// (a hire this can't read: chosen afresh as they board)
				}
			}
		}
		entityData.set(PASSENGERS, passengers());
		delivered = input.getIntOr("delivered", 0);
		crew.clear();
		crew.addAll(input.read("crew", UUIDUtil.CODEC.listOf()).orElse(List.of()));
		lights.clear();
		lights.addAll(input.read("lights", BlockPos.CODEC.listOf()).orElse(List.of()));
		lightsOn = false;                               // put back on the next tick if it's still down
		entityData.set(PHASE, input.getIntOr("phase", CrewShipFlight.ENTRY));
		entityData.set(PHASE_START, input.getLongOr("phase_start", 0L));
		entityData.set(YAW, input.getFloatOr("yaw", 0.0F));
		entityData.set(ROOF, input.getBooleanOr("roof", false));
		// (a ship saved before the rework, 8 November 2026, kept its reach from the old platform's top, 0.25 higher)
		entityData.set(REACH, input.getFloatOr("reach", CrewShipFlight.HINGE_Y) - (input.getIntOr("ramp", 1) < 2 ? 0.25F : 0));
		entityData.set(CEILING, input.getFloatOr("ceiling", CrewShipFlight.CEILING));
		pickupSquad = input.read("pickup_squad", UUIDUtil.CODEC).orElse(null);
		pickupOwner = input.read("pickup_owner", UUIDUtil.CODEC).orElse(null);
		called.clear();
		called.addAll(input.read("called", UUIDUtil.CODEC.listOf()).orElse(List.of()));
		aboard.clear();
		aboard.addAll(input.read("aboard", Codec.STRING.listOf()).orElse(List.of()));
		entityData.set(PICKUP, pickupSquad != null);
		if (pickupSquad != null) {
			entityData.set(PASSENGERS, String.join("|", aboard));
		}
	}

	/** Game tests: jumps to {@code phase} from now. */
	public void testPhase(int phase) {
		entityData.set(PHASE, phase);
		entityData.set(PHASE_START, level().getGameTime());
	}

	/** Holds it in its phase (game tests); not saved. */
	public void testHold(boolean hold) {
		testHold = hold;
	}

	private boolean testHold;

	/** The light blocks it has put out (game tests). */
	public List<BlockPos> lights() {
		return List.copyOf(lights);
	}

	/** The ship on its way to (or at) the tower at {@code core}, if any. */
	public static @Nullable CrewShip active(ServerLevel level, BlockPos core) {
		for (CrewShip s : level.getEntitiesOfClass(CrewShip.class, new AABB(core).inflate(CrewShipSite.REACH + 80), s -> s.isAlive())) {
			if (s.core.equals(core) && !s.parked()) {
				return s;
			}
		}
		return null;
	}
}
