package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.tower.JugcraftTower;
import io.github.jimbozoomer.jugcraft.tower.crew.CrewShip;
import io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight;
import org.jspecify.annotations.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * The crew drop ship's sound, from NASA launch recordings (STS-131's launch for the roar, an Atlas V's for the
 * rumble; tools/crew_ship_sounds.py): the roar as it comes in, the thrusters' burn swelling and easing with their
 * thrust, a low rumble while it idles in a hover or on the ground, the roar again as it lifts off. Each follows the
 * ship where it's drawn (far out as it comes in, so it swells as it nears and fades as it goes, the game's own falling
 * off with distance), and the idle is kept low. Client only: each player's game plays its own from the timeline.
 *
 * <p>6 November 2026 (Justin: "when it was landing, it didn't play any of the sounds from the rocket ... I want you to be
 * able to hear it when it's coming in from a distance"): the burn and the idle start silent and swell, and the game
 * drops a sound that starts at no volume unless it says it may (canStartSilent), so they never played; they do now. The
 * roar carries 800 blocks as it comes in and lifts off, the burn 320 (sounds.json).
 *
 * <p>8 November 2026 (C3: "doesn't get louder as it comes in"): over that long a reach the game's falling off barely
 * changes from far to near, so each sound is also made quieter with distance ({@link Follow#near}): it swells as it nears.
 */
final class CrewShipSounds {
	/** How loud each is at full (before the falling off with distance). */
	static final float ROAR = 1.0F, IDLE = 0.45F, MOTOR = 0.7F;
	/** The distance (blocks) over which the swell as it nears is halved, about ({@link Follow#near}). */
	static final double NEAR = 40.0;

	private CrewShipSounds() {
	}

	/** One ship's sounds. */
	private static final class Set {
		@Nullable Follow burn, idle;
		final @Nullable Follow[] motor = new Follow[2];
		int phase = -1;
	}

	/** A sound that follows a ship's drawn position, its loudness set each tick. */
	private static final class Follow extends AbstractTickableSoundInstance {
		private final CrewShip ship;
		// 0 the burn (loop), 1 the idle (loop), 2 a one-shot (entry, lift-off), 3 and 4 the door guns' motors (loops: the
		// whine of the barrels' drive, its pitch and loudness with how fast they turn; Justin, 9 November 2026: "make sure
		// sounds and animations are accurately lined up")
		private final int kind;
		private final CrewShipFlight.Pose p = new CrewShipFlight.Pose();
		private float level;

		Follow(CrewShip ship, SoundEvent event, int kind) {
			super(event, SoundSource.NEUTRAL, RandomSource.create());
			this.ship = ship;
			this.kind = kind;
			this.looping = kind < 2;
			this.delay = 0;
			follow();
			this.volume = kind == 2 ? ROAR * near() : 0.0F;
		}

		private void follow() {
			ClientLevel lv = Minecraft.getInstance().level;
			long now = lv == null ? ship.phaseStart() : lv.getGameTime();
			ship.pose(now - ship.phaseStart(), p);
			Vec3 at = kind >= 3 ? ship.world(p, CrewShip.gunX(kind - 3, p.sides), CrewShip.GUNS[kind - 3].y() + 0.75, CrewShip.GUNS[kind - 3].z())
					: ship.world(p, 0, CrewShipFlight.G + 4, 6);
			x = at.x;
			y = at.y;
			z = at.z;
		}

		/** The loops start silent and swell: without this the game would drop them unplayed. */
		@Override
		public boolean canStartSilent() {
			return true;
		}

		@Override
		public void tick() {
			if (ship.isRemoved() || ship.phase() >= CrewShipFlight.GONE) {
				stop();
				return;
			}
			follow();
			if (kind >= 3) {                              // a door gun's motor: with its barrels' speed, gone once they stop
				float full = CrewShip.SPIN_STEP / (CrewShip.GUN_COOLDOWN[0] + 1), r = ship.spinRate[kind - 3] / full;
				if (r <= 0) {
					stop();
					return;
				}
				volume = Math.max(0.0001F, MOTOR * Math.min(1, r * 1.5F));
				pitch = 0.5F + 0.5F * r;
				return;
			}
			float want = switch (kind) {
				case 0 -> ROAR * Mth.clamp((p.thrust - 0.5F) / 0.5F + p.entry, 0, 1);
				case 1 -> IDLE * (p.thrust > 0.01F ? 1 - Mth.clamp((p.thrust - 0.6F) / 0.4F, 0, 1) : 0);
				default -> ROAR;
			};
			level += Mth.clamp(want - level, -0.05F, 0.05F);       // eased, never jumping
			volume = Math.max(0.0001F, level * near());
		}

		/**
		 * How much nearer sounds louder (C3, Justin, 8 November 2026: the landing "doesn't get louder as it comes in"):
		 * the game's own falling off is a straight line over the whole 800 (or 320) blocks a sound here carries, so from
		 * 200 blocks out to overhead it only doubled; this adds the way a real engine swells as it nears, about 20 dB from
		 * 400 blocks to overhead, never under an eighth so it's still heard coming from far off.
		 */
		private float near() {
			Minecraft client = Minecraft.getInstance();
			if (client.player == null) {
				return 1.0F;
			}
			double d = client.player.getEyePosition().distanceTo(new Vec3(x, y, z));
			return Mth.clamp((float) Math.pow(NEAR / (NEAR + d), 0.8), 0.12F, 1.0F);
		}
	}

	/** Each client tick: sounds started for each drop ship that has none, and its one-shots as its phases begin. */
	static void tick(Minecraft client) {
		ClientLevel level = client.level;
		if (level == null || client.isPaused()) {
			return;
		}
		for (Entity e : level.entitiesForRendering()) {
			if (!(e instanceof CrewShip ship) || ship.phase() >= CrewShipFlight.GONE) {
				continue;
			}
			Set set = ship.clientSound instanceof Set s ? s : new Set();
			ship.clientSound = set;
			if (set.burn == null || set.burn.isStopped()) {
				set.burn = new Follow(ship, JugcraftTower.SHIP_THRUST, 0);
				client.getSoundManager().play(set.burn);
			}
			if (set.idle == null || set.idle.isStopped()) {
				set.idle = new Follow(ship, JugcraftTower.SHIP_IDLE, 1);
				client.getSoundManager().play(set.idle);
			}
			for (int i = 0; i < 2; i++) {
				Follow mo = set.motor[i];
				if (ship.spinRate[i] > 0 && (mo == null || mo.isStopped())) {
					set.motor[i] = new Follow(ship, JugcraftTower.SHIP_GUN_MOTOR, 3 + i);
					client.getSoundManager().play(set.motor[i]);
				}
			}
			int phase = ship.phase();
			if (phase != set.phase) {
				long into = level.getGameTime() - ship.phaseStart();
				if (into < 40 && (phase == CrewShipFlight.ENTRY || phase == CrewShipFlight.LIFT)) {
					client.getSoundManager().play(new Follow(ship, phase == CrewShipFlight.ENTRY ? JugcraftTower.SHIP_ENTRY : JugcraftTower.SHIP_LIFTOFF, 2));
				}
				set.phase = phase;
			}
		}
	}
}
