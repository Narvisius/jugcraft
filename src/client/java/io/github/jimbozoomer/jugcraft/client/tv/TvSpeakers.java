package io.github.jimbozoomer.jugcraft.client.tv;

import io.github.jimbozoomer.jugcraft.tv.FlatTvBlock;
import io.github.jimbozoomer.jugcraft.tv.TvSpeakerBlock;
import io.github.jimbozoomer.jugcraft.tv.TvSpeakerBlockEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The surround speakers this game has loaded (tv/TvSpeakerBlockEntity#LOADED), by the screen each plays: the screen's
 * controller now (found again from the block it was linked to, so a screen that has grown keeps its speakers). Worked
 * out four times a second. Each screen's player plays its sound from its speakers near the listener too
 * ({@link TvPlayer#speakers}); the speakers' renderer asks here how their screen's sound is going (their lights, their
 * woofers' cones).
 */
public final class TvSpeakers {
	private static final long REFRESH_MS = 250;
	/** A speaker further than this from the listener isn't played here (it's heard to {@link TvPlayer#SPEAKER_HEARD}). */
	private static final double NEAR = TvPlayer.SPEAKER_HEARD + 8;
	private static Map<BlockPos, List<TvPlayer.SpeakerAt>> byScreen = Map.of();
	private static Map<BlockPos, BlockPos> screenOf = Map.of();
	private static long refreshed;
	private static @Nullable ClientLevel refreshedIn;

	private TvSpeakers() {
	}

	/** The speakers near the listener playing the screen whose controller is {@code controller}. */
	static List<TvPlayer.SpeakerAt> of(BlockPos controller) {
		refresh();
		return byScreen.getOrDefault(controller, List.of());
	}

	/** The controller of the screen the speaker plays (null: unlinked, or that screen isn't there). */
	public static @Nullable BlockPos screenOf(TvSpeakerBlockEntity speaker) {
		refresh();
		return screenOf.get(speaker.getBlockPos());
	}

	/** Whether the screen's sound is playing in this game. */
	public static boolean sounding(@Nullable BlockPos controller) {
		TvPlayer p = controller == null ? null : TvPlayers.get(controller);
		return p != null && !p.closed && p.sounding();
	}

	/** The screen's sound's low end where it's playing (-1 to 1; 0 silent): a speaker's cone moves with it. */
	public static float bass(@Nullable BlockPos controller) {
		TvPlayer p = controller == null ? null : TvPlayers.get(controller);
		return p == null || p.closed ? 0 : p.bass();
	}

	/** Which way a speaker turned {@code rotation} (sixteen steps, as a sign) faces: {x, z}. */
	public static float[] facing(int rotation) {
		double a = Math.toRadians(rotation * 22.5);
		return new float[] {(float) -Math.sin(a), (float) Math.cos(a)};
	}

	private static void refresh() {
		Minecraft mc = Minecraft.getInstance();
		long now = System.currentTimeMillis();
		ClientLevel level = mc.level;
		if (now - refreshed < REFRESH_MS && level == refreshedIn) {
			return;
		}
		refreshed = now;
		refreshedIn = level;
		Map<BlockPos, List<TvPlayer.SpeakerAt>> by = new HashMap<>();
		Map<BlockPos, BlockPos> of = new HashMap<>();
		if (level != null) {
			Vec3 eye = mc.gameRenderer.mainCamera().position();
			// (one left from another world or dimension isn't this one's)
			TvSpeakerBlockEntity.LOADED.removeIf(s -> s.getLevel() != level || s.isRemoved());
			for (TvSpeakerBlockEntity s : TvSpeakerBlockEntity.LOADED) {
				BlockPos linked = s.screen();
				if (linked == null || !level.isLoaded(linked)) {
					continue;
				}
				BlockState tv = level.getBlockState(linked);
				if (!(tv.getBlock() instanceof FlatTvBlock)) {
					continue;
				}
				BlockPos controller = FlatTvBlock.controllerOf(level, linked, tv).immutable();
				of.put(s.getBlockPos(), controller);
				Vec3 at = s.cabinet();
				if (at.distanceToSqr(eye) > NEAR * NEAR) {
					continue;
				}
				BlockState state = s.getBlockState();
				float[] f = facing(state.hasProperty(TvSpeakerBlock.ROTATION) ? state.getValue(TvSpeakerBlock.ROTATION) : 0);
				by.computeIfAbsent(controller, k -> new ArrayList<>()).add(new TvPlayer.SpeakerAt(s.getBlockPos().immutable(), at, f[0], f[1]));
			}
		}
		byScreen = by;
		screenOf = of;
	}

	/** Let go of everything (the world left). */
	static void clear() {
		TvSpeakerBlockEntity.LOADED.clear();
		byScreen = Map.of();
		screenOf = Map.of();
		refreshedIn = null;
	}
}
