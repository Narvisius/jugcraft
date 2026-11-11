package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.mixin.client.ChannelAccessor;
import io.github.jimbozoomer.jugcraft.mixin.client.SoundEngineAccessor;
import io.github.jimbozoomer.jugcraft.mixin.client.SoundManagerAccessor;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.EXTEfx;

/**
 * Sound through walls (Justin's round 13): every sound out in the world is muffled by the blocks between it and you,
 * as it starts and again every tick as you (or it) move. One block of wall lets through about a tenth of its loudness
 * and almost none of its high end, so you barely hear a muffled version; two blocks stop it altogether. A closed door
 * counts nearly as a wall, glass a little less, a pane, slab or piece of furniture a third; wool deadens more than
 * stone; open doors, bars, fences, plants and the big machines' frames let it through. A block that doesn't fill its
 * cell (a door, a sofa, a TV's panel, a slab) only counts where the line goes through what's there, not past it over a
 * seat or in front of a panel (Justin, 10 November 2026: the TV's sound cut by the upstairs sofa in front of it). The
 * sound is followed along a few lines (to points a little round it), and the clearest counts, so it is not lost behind
 * a corner it only just touches.
 * <p>Done with OpenAL's low-pass filter on each sound's own source (the EFX extension, which the game's OpenAL Soft
 * has), so it composes with the game's own volume and needs nothing from the sounds themselves. Sounds on you (the
 * Geiger counter, music, the interface) are left alone.
 */
public final class SoundOcclusion {
	/** One block of wall lets this much of a sound's loudness through... */
	static final double ONE_WALL = 0.1;
	/** ...and this much of its high end (the muffling). */
	static final double ONE_WALL_HF = 0.04;
	/** Past this a sound is out of hearing anyway. */
	static final double RANGE = 64;
	private static final float[] OPEN = {1, 1};
	/** What each playing sound's filter was last set to (gain, high-end gain). */
	private static final Map<SoundInstance, float[]> APPLIED = new WeakHashMap<>();
	/** The low-pass filter, made and used on the sound thread only: -1 not made yet, 0 not available. */
	private static int filter = -1;
	private static long filterContext;

	private SoundOcclusion() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(SoundOcclusion::tick);
	}

	/** A sound has just started (SoundEngineMixin): filter it for the walls in the way before it is heard. */
	public static void started(SoundInstance sound, ChannelAccess.@Nullable ChannelHandle handle) {
		if (handle == null) {
			return;
		}
		float[] o = filterFor(sound);
		APPLIED.put(sound, o);
		apply(handle, o);
	}

	private static void tick(Minecraft mc) {
		if (mc.level == null) {
			APPLIED.clear();
			return;
		}
		Map<SoundInstance, ChannelAccess.ChannelHandle> channels = ((SoundEngineAccessor) (Object) engine(mc)).jugcraft_getChannels();
		for (Map.Entry<SoundInstance, ChannelAccess.ChannelHandle> e : List.copyOf(channels.entrySet())) {
			SoundInstance sound = e.getKey();
			if (!positional(sound)) {
				continue;
			}
			float[] target = filterFor(sound);
			float[] last = APPLIED.getOrDefault(sound, OPEN);
			// Eased over a few ticks: through a doorway the sound opens up rather than jumping.
			float[] next = {ease(last[0], target[0]), ease(last[1], target[1])};
			if (Math.abs(next[0] - last[0]) > 0.002F || Math.abs(next[1] - last[1]) > 0.002F) {
				APPLIED.put(sound, next);
				apply(e.getValue(), next);
			}
		}
	}

	private static float ease(float from, float to) {
		return Math.abs(to - from) < 0.01F ? to : from + (to - from) * 0.5F;
	}

	private static SoundEngine engine(Minecraft mc) {
		return ((SoundManagerAccessor) (Object) mc.getSoundManager()).jugcraft_getEngine();
	}

	static boolean positional(SoundInstance sound) {
		return !sound.isRelative() && sound.getAttenuation() != SoundInstance.Attenuation.NONE;
	}

	/** The filter for a sound where it is now: (gain, high-end gain). */
	static float[] filterFor(SoundInstance sound) {
		Minecraft mc = Minecraft.getInstance();
		if (!positional(sound) || mc.level == null) {
			return OPEN;
		}
		Vec3 ear = engine(mc).getListenerTransform().position();
		Vec3 at = new Vec3(sound.getX(), sound.getY(), sound.getZ());
		if (at.distanceToSqr(ear) > RANGE * RANGE) {
			return OPEN;
		}
		return forWalls(walls(mc.level, ear, at));
	}

	/** One block: a tenth of the loudness, muffled; on to nothing at two. */
	public static float[] forWalls(double walls) {
		if (walls < 0.02) {
			return OPEN;
		}
		if (walls >= 2) {
			return new float[] {0, 0};
		}
		double gain = walls <= 1 ? Math.pow(ONE_WALL, walls) : ONE_WALL * (2 - walls);
		double hf = Math.pow(ONE_WALL_HF, Math.min(1, walls));
		return new float[] {(float) gain, (float) hf};
	}

	/** How much wall stands between the ear and a sound (in blocks of solid wall): sound/SoundWalls, the clearest of a few lines to it. */
	public static double walls(Level level, Vec3 ear, Vec3 at) {
		return io.github.jimbozoomer.jugcraft.sound.SoundWalls.walls(level, ear, at);
	}

	private static void apply(ChannelAccess.ChannelHandle handle, float[] o) {
		float gain = o[0], hf = o[1];
		handle.execute(channel -> setFilter(((ChannelAccessor) (Object) channel).jugcraft_getSource(), gain, hf));
	}

	/** On the sound thread: the source's direct path through the low-pass filter (or straight, when open). */
	private static void setFilter(int source, float gain, float hf) {
		long context = ALC10.alcGetCurrentContext();
		if (filter < 0 || context != filterContext) {
			filterContext = context;
			filter = 0;
			long device = context == 0 ? 0 : ALC10.alcGetContextsDevice(context);
			if (device != 0 && ALC10.alcIsExtensionPresent(device, "ALC_EXT_EFX")) {
				int made = EXTEfx.alGenFilters();
				EXTEfx.alFilteri(made, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
				filter = AL10.alGetError() == AL10.AL_NO_ERROR ? made : 0;
			}
			if (filter == 0) {
				Jugcraft.LOGGER.warn("OpenAL has no EFX filters here: sounds through walls will not be muffled");
			}
		}
		if (filter == 0) {
			return;
		}
		if (gain >= 0.999F && hf >= 0.999F) {
			AL10.alSourcei(source, EXTEfx.AL_DIRECT_FILTER, EXTEfx.AL_FILTER_NULL);
		} else {
			// The filter's settings are copied into the source as it is attached, so one filter serves every source.
			EXTEfx.alFilterf(filter, EXTEfx.AL_LOWPASS_GAIN, Math.max(0, gain));
			EXTEfx.alFilterf(filter, EXTEfx.AL_LOWPASS_GAINHF, Math.max(0, hf));
			AL10.alSourcei(source, EXTEfx.AL_DIRECT_FILTER, filter);
		}
		AL10.alGetError();
	}
}
