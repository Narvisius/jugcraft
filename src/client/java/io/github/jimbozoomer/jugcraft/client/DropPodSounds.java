package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.tower.squad.DropPod;
import io.github.jimbozoomer.jugcraft.tower.squad.JugcraftSquads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * A drop pod's sounds on its way down (tower/squad/DropPod tells the client as its flight goes on), all from real
 * recordings (tools/drop_pod_sounds.py; Justin, 10 November 2026: "take sounds from like real life re entry of stuff"):
 * as it sets off, its roar follows it down its path in two layers, the far one (a launch's rumble from miles off) and
 * the near one (a launch's roar and crackle up close) taking over as it closes, the way the air takes the highs out of
 * a far sound; louder as it nears, pitched by the Doppler shift of its speed toward you; its sonic boom a moment after it
 * sets off (a Falcon Heavy's boosters coming home); its retros kicking in at the last second (a Falcon 9 lighting).
 * (The impact, the hatch's bolts and the self-destruct are the server's.)
 */
final class DropPodSounds {
	/** How loud the retros are (their reach: 96 blocks a unit). */
	private static final float RETRO = 2.0F;
	/** The speed of sound (blocks a second) for the Doppler shift, and how far the shift may go. */
	private static final float SOUND_SPEED = 343, DOPPLER_MIN = 0.85F, DOPPLER_MAX = 1.15F;

	private DropPodSounds() {
	}

	static void register() {
		DropPod.clientSounds = (pod, what) -> {
			Minecraft client = Minecraft.getInstance();
			float pitch = 0.95F + pod.getRandom().nextFloat() * 0.1F;
			if (what == 0) {
				client.getSoundManager().play(new Roar(pod, JugcraftSquads.POD_ROAR_FAR, false, pitch));
				client.getSoundManager().play(new Roar(pod, JugcraftSquads.POD_ROAR_NEAR, true, pitch));
			} else if (what == 2) {
				Vector3f at = here(pod, new Vector3f());
				client.getSoundManager().play(new SimpleSoundInstance(JugcraftSquads.POD_BOOM, SoundSource.NEUTRAL, 0.95F, 1.0F,
						RandomSource.create(), pod.getX() + at.x, pod.getY() + at.y, pod.getZ() + at.z));
			} else {
				client.getSoundManager().play(new SimpleSoundInstance(JugcraftSquads.POD_RETRO, SoundSource.NEUTRAL, RETRO, pitch,
						RandomSource.create(), pod.getX(), pod.getY() + 2, pod.getZ()));
			}
		};
	}

	/** Where the pod is drawn now, from where it lands. */
	private static Vector3f here(DropPod pod, Vector3f out) {
		ClientLevel level = Minecraft.getInstance().level;
		return pod.at(level == null ? 0 : level.getGameTime() - pod.start(), out);
	}

	private static float smooth(float x) {
		x = Mth.clamp(x, 0, 1);
		return x * x * (3 - 2 * x);
	}

	/**
	 * One layer of the roar, where the pod is drawn. The far layer: 0.9 x (50 / (50 + d))^0.8, never under 0.08; the
	 * near: 40 / (40 + d), faded in from 150 blocks to 40 (d: blocks from you). Both fade in over a second and a half
	 * as it sets off and out over the retro burn. (The sounds carry over the whole area, sounds.json: the loudness here
	 * is all of it.)
	 */
	private static final class Roar extends AbstractTickableSoundInstance {
		private final DropPod pod;
		private final boolean near;
		private final float base;
		private final Vector3f at = new Vector3f();
		private double lastDistance = -1;

		Roar(DropPod pod, SoundEvent sound, boolean near, float pitch) {
			super(sound, SoundSource.NEUTRAL, RandomSource.create());
			this.pod = pod;
			this.near = near;
			this.base = pitch;
			this.looping = true;
			this.delay = 0;
			this.pitch = pitch;
			follow();
		}

		private void follow() {
			ClientLevel level = Minecraft.getInstance().level;
			float age = level == null ? 0 : level.getGameTime() - pod.start();
			pod.at(age, at);
			x = pod.getX() + at.x;
			y = pod.getY() + at.y;
			z = pod.getZ() + at.z;
			float k = smooth((age - pod.setsOff()) / 30F) * (1 - Mth.clamp((age - (pod.land() - DropPod.RETRO)) / 8F, 0, 1));
			Minecraft client = Minecraft.getInstance();
			double d = client.player == null ? 60 : client.player.getEyePosition().distanceTo(new Vec3(x, y, z));
			float g = near ? 40F / (40F + (float) d) * smooth((150 - (float) d) / 110F)
					: Math.max(0.08F, 0.9F * (float) Math.pow(50 / (50 + d), 0.8));
			volume = Math.max(0.0001F, Math.min(1, g * k));
			// the Doppler shift: higher while it closes on you, lower as it draws away
			if (lastDistance >= 0) {
				float toward = (float) (lastDistance - d) * 20;
				pitch = base * Mth.clamp(1 / (1 - toward / SOUND_SPEED), DOPPLER_MIN, DOPPLER_MAX);
			}
			lastDistance = d;
		}

		@Override
		public boolean canStartSilent() {
			return true;
		}

		@Override
		public void tick() {
			ClientLevel level = Minecraft.getInstance().level;
			if (pod.isRemoved() || level == null || level.getGameTime() - pod.start() > pod.land()) {
				stop();
				return;
			}
			follow();
		}
	}
}
