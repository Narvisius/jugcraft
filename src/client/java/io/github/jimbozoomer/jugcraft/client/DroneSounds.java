package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.drone.DepotView;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.FlightPath;
import io.github.jimbozoomer.jugcraft.drone.FlightScheduler;
import io.github.jimbozoomer.jugcraft.drone.JugcraftDrones;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Rotor sound for flying drones. Drones are not entities (the client works their flight out from the depot's
 * {@link DepotView}), so this follows each flight with a looping sound of its own, entirely on the client.
 *
 * <p>The sounds play in the {@link SoundSource#NEUTRAL} category ("Friendly Creatures" in Music &amp; Sounds),
 * so turning that slider down quietens or mutes them. Only the {@link #MAX_SOUNDS} nearest flying drones within
 * {@link #RANGE} blocks get a sound; docked and recharging drones are silent.
 */
public final class DroneSounds {
	public static final int MAX_SOUNDS = 12;
	public static final double RANGE = 48;

	/** Terminals whose drones were drawn recently (the renderer reports them; weak so unloaded ones drop out). */
	private static final Set<DroneTerminalBlockEntity> TERMINALS = Collections.newSetFromMap(new WeakHashMap<>());
	/** Where each sounding drone is this tick, by key. */
	private static final Map<Long, double[]> POSITIONS = new HashMap<>();
	private static final Map<Long, DroneSound> PLAYING = new HashMap<>();

	private DroneSounds() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(DroneSounds::tick);
	}

	/** Called by {@link DroneDepotRenderer} for every terminal it draws. */
	public static void seen(DroneTerminalBlockEntity terminal) {
		TERMINALS.add(terminal);
	}

	/** How many drone sounds are playing (game tests). */
	public static int playing() {
		return PLAYING.size();
	}

	private record Candidate(long key, int tier, double x, double y, double z, double distance) {
	}

	private static void tick(Minecraft client) {
		POSITIONS.clear();
		if (client.level == null || client.player == null) {
			PLAYING.values().forEach(DroneSound::end);
			PLAYING.clear();
			TERMINALS.clear();
			return;
		}
		Vec3 ear = client.player.getEyePosition();
		double now = client.level.getGameTime();
		List<Candidate> candidates = new ArrayList<>();
		for (DroneTerminalBlockEntity terminal : List.copyOf(TERMINALS)) {
			if (terminal.isRemoved() || terminal.getLevel() != client.level) {
				TERMINALS.remove(terminal);
				continue;
			}
			DepotView view = terminal.view();
			if (view == null) {
				continue;
			}
			long origin = terminal.getBlockPos().asLong();
			for (DepotView.FlightView flight : view.flights) {
				FlightPath path = flight.path();
				double t = flight.ticksAt(now, view.syncTime);
				if (t >= path.totalTicks() - FlightScheduler.RECHARGE_TICKS) {
					continue;
				}
				FlightPath.Pose pose = path.poseAt(t);
				double x = terminal.getBlockPos().getX() + pose.x();
				double y = terminal.getBlockPos().getY() + pose.y();
				double z = terminal.getBlockPos().getZ() + pose.z();
				double distance = ear.distanceToSqr(x, y, z);
				if (distance < RANGE * RANGE) {
					candidates.add(new Candidate(origin * 31 + flight.drone(), flight.tier(), x, y, z, distance));
				}
			}
		}
		candidates.sort(Comparator.comparingDouble(Candidate::distance));
		for (int i = 0; i < Math.min(MAX_SOUNDS, candidates.size()); i++) {
			Candidate c = candidates.get(i);
			POSITIONS.put(c.key(), new double[] {c.x(), c.y(), c.z()});
			DroneSound sound = PLAYING.get(c.key());
			if (sound == null || sound.isStopped()) {
				sound = new DroneSound(c.key(), c.tier(), c.x(), c.y(), c.z());
				PLAYING.put(c.key(), sound);
				client.getSoundManager().play(sound);
			}
		}
		PLAYING.values().removeIf(DroneSound::isStopped);
	}

	/** One drone's rotor loop: follows the drone, fades in and out, and pitches up a little while it climbs. */
	private static final class DroneSound extends AbstractTickableSoundInstance {
		private static final float FADE = 0.08F;
		private final long key;
		private final float loudness;
		private final float basePitch;
		private boolean ending;

		DroneSound(long key, int tier, double x, double y, double z) {
			super(tier >= 5 && tier <= 9 ? JugcraftDrones.DRONE_HUM_HEAVY : JugcraftDrones.DRONE_HUM, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
			this.key = key;
			this.looping = true;
			this.delay = 0;
			this.attenuation = Attenuation.LINEAR;
			this.loudness = (tier >= 5 && tier <= 9 ? 1.0F : 0.55F + tier * 0.08F) * 0.5F; // half the first version (play-test feedback)
			// Smaller drones spin faster; every drone is slightly different so a swarm does not phase.
			this.basePitch = (tier == 10 ? 1.3F : tier >= 5 ? 1.15F - (tier - 5) * 0.07F : 1.2F - (tier - 1) * 0.08F) + (float) ((key % 7) * 0.012);
			this.pitch = basePitch;
			this.volume = 0.01F;
			this.x = x;
			this.y = y;
			this.z = z;
		}

		@Override
		public boolean canStartSilent() {
			return true;
		}

		void end() {
			ending = true;
		}

		@Override
		public void tick() {
			double[] at = POSITIONS.get(key);
			if (at == null || ending) {
				volume -= FADE;
				if (volume <= 0) {
					stop();
				}
				return;
			}
			double climb = at[1] - y;
			x = at[0];
			y = at[1];
			z = at[2];
			volume = Math.min(loudness, volume + FADE);
			float target = basePitch + (float) Math.max(-0.08, Math.min(0.12, climb * 0.25));
			pitch += (target - pitch) * 0.2F;
		}
	}
}
