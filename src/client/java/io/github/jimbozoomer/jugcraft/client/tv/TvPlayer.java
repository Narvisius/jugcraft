package io.github.jimbozoomer.jugcraft.client.tv;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.net.InetAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.EXTEfx;
import org.lwjgl.system.MemoryUtil;

/**
 * One screen's video as this game plays it (docs/features/flat-screen-tv.md). A YouTube link is first turned into its
 * streams by the helper ({@link TvHelpers}); then two FFmpeg readers ({@link TvMedia}, fetched on first use by
 * {@link TvCodec}) run on their own threads, one decoding
 * the picture into a few frames ready ahead, one the sound (mono, 48 kHz) into short blocks. The game's render thread
 * shows the frame due at the screen's position (the server's clock: everyone sees the same moment) in a texture and
 * feeds the sound to an OpenAL source at the screen, heard out to 24 blocks and muffled by the walls between it and
 * the listener as the game's own sounds are (client/SoundOcclusion: a wall lets a tenth through, two none, so the next
 * room doesn't hear your videos), and from every surround speaker linked to the screen ({@link TvSpeakers}: a source
 * each, fed the same sound, started and stopped with the screen's). When the screen's clock jumps (a seek, a replay round, a late joiner) the readers jump
 * there: a little past it, as far as a jump has been taking here, so they're ready when the clock gets there; FFmpeg
 * lands on the key frame before that and the picture's reader decodes on, unshown, up to the moment due (it never jumps
 * again for being behind a key frame: that was the stuck picture). A short way on ({@link #SKIP_AHEAD}: the 10s button)
 * isn't a seek at all: the readers decode on through it from where they are, asking the stream for nothing new. The
 * screen says what holds it up meanwhile ({@link #status}: SEEKING TO, BUFFERING), and a reader that stops or stays
 * stuck has its player started over by {@link TvPlayers} (the streams found again). A YouTube video's streams that
 * won't open (YouTube refusing them) are asked for again another way ({@link TvHelpers#WAYS}: other streams, fetched
 * in pieces through {@link TvRelay}, as other YouTube apps) before the screen gives up; the way that worked is tried
 * first next time.
 */
final class TvPlayer {
	private static final int MAX_FRAMES = 3, AL_BUFFERS = 16, BLOCK = 4096, RATE = TvMedia.RATE;
	/** OpenAL's per-source distance model (AL_EXT_source_distance_model) and linear fall-off, as the game's own sounds. */
	private static final int AL_DISTANCE_MODEL = 0xD000, AL_LINEAR_DISTANCE = 0xD003;
	/** The screen's clock moved by more than this (ms) from how it runs: a jump (the readers go there). */
	private static final long JUMP = 1000;
	/** The picture this far behind (ms) with nothing new decoded: the stream isn't keeping up (BUFFERING). */
	private static final long LATE = 800;
	/** The readers this far behind (ms) after catching up as they could: they jump ahead instead. */
	private static final long FAR_BEHIND = 15_000;
	/** Stuck this long (ms: SEEKING or BUFFERING) and the player is started over. */
	static final long STUCK_MS = 25_000;
	/** A jump's lead: no more than this (ms). */
	private static final long MAX_LEAD = 12_000;
	/** A reader this far ahead (ms) with no jump in hand (beyond a jump's lead) has run on: it jumps back. */
	private static final long FAR_AHEAD = 4_000;
	/** A jump no further on than this (ms) from where a reader is: it decodes on through it rather than seeking the stream. */
	private static final long SKIP_AHEAD = 25_000;
	/**
	 * The sound this far (ms) from the picture's clock is set again where the clock is, from what it has queued (no gap).
	 * A live stream's sound chunks this far behind are kept while it plays on (it has nothing to catch it up with).
	 */
	private static final long SOUND_DRIFT = 120, LIVE_DRIFT = 600;
	/**
	 * A live stream's picture this far (ms) behind its clock (decoding slower than it plays, a stall) is decoded on
	 * unshown until it's within {@link #LIVE_PICTURE_CAUGHT} of it.
	 */
	private static final long LIVE_PICTURE_BEHIND = 100, LIVE_PICTURE_CAUGHT = 20;
	/**
	 * The sound (on average) this far (ms) from the clock is played a little slower or faster (at most {@link #NUDGE}) until
	 * it's within {@link #NUDGE_DONE}: kept in step without a jump (a sound card's clock is never quite the game's).
	 */
	private static final double NUDGE_FROM = 35, NUDGE_DONE = 10, NUDGE = 0.005;
	/** How often (ms) a player playing a live stream logs how its sound and picture line up. */
	private static final long SYNC_LOG_MS = 30_000;
	/** Heard out to this many blocks (falling off evenly). */
	static final float HEARD = 24;
	/** A surround speaker is heard out to this many blocks; at most this many play a screen's sound. */
	static final float SPEAKER_HEARD = 16;
	static final int MAX_SPEAKERS = 8;
	/** A speaker's sound this far (ms) from the screen's is set again from the screen's. */
	private static final long SPEAKER_DRIFT = 40;
	private static int nextId;

	final BlockPos pos;
	final String url;
	final Identifier textureId;
	/** What the screen says while there's no picture ("" once there is). */
	volatile String state = "LOADING";
	/** The words while it loads: LOADING, or RECONNECTING when started over after its stream stopped. */
	private volatile String loading = "LOADING";
	volatile String title = "";
	volatile String info = "";
	volatile long durationMs;
	volatile boolean live;
	volatile boolean ended;
	/** The first picture is in: the video is ready to play. */
	volatile boolean ready;
	volatile boolean closed;
	/** The reader stopped (or stayed stuck) after the video had played: {@link TvPlayers} starts the screen over. */
	volatile boolean broken;
	/** Where the readers should be (ms) and whether it's paused: set each frame from the screen's clock. */
	private volatile long target;
	private volatile boolean pausedTarget;
	private final int maxW, maxH;
	/** A screen of one or two blocks: no more than 480p (a bigger screen's player is a new one). */
	final boolean small;
	/** Heard out to this many blocks from the middle: {@link #HEARD} plus half the screen's longer side (a wall screen's edges). */
	private float heard;
	long seen;
	/** Jumps of the screen's clock seen (the render thread counts them; each reader goes there when it sees one more). */
	private volatile int jumps;
	private long lastTarget = Long.MIN_VALUE, lastWall;
	/** How long a jump has been taking here (ms): the readers jump this much past the screen's moment. */
	private volatile long seekCost = 2500;
	/** A jump being made by the picture's reader: to where (ms), since when (wall ms); -1 none. Its count (frames carry it). */
	private volatile long seekingTo = -1, seekStart;
	private volatile int gen;
	/** The jumps the picture's reader has set out on (the screen in place once it's {@link #jumps} and the picture's up). */
	private volatile int placedJumps;
	/** The ways of asking YouTube this player leaves out (a bit each, {@link TvHelpers#WAYS}): ones that wouldn't go there. */
	private final int avoidWays;
	/** The way it plays by ({@link TvHelpers#WAYS}; -1 not YouTube, or not yet). */
	volatile int way = -1;
	/** Watched full screen here ({@link TvFullScreen}): its sound is played at the listener, not at the screen. */
	volatile boolean atListener;
	/** The picture last shown: its time (ms); since when the picture has been late (wall ms, 0 it isn't). */
	private volatile long shownTs = -1, lateSince;

	// The picture: frames decoded ahead (their buffers), the texture showing one.
	private final Object lock = new Object();
	private final ArrayDeque<VFrame> frames = new ArrayDeque<>();
	private final ArrayDeque<ByteBuffer> free = new ArrayDeque<>();
	private volatile int vw, vh;
	private @Nullable DynamicTexture texture;
	private boolean shown;
	/** The jump count of the picture last shown (paused, one picture of a new jump is shown, however far ahead). */
	private int shownGen = -1;
	/** A live stream: its clock (frame time less this game's) from its first frame. */
	private volatile long liveOffset = Long.MIN_VALUE;

	// The sound: blocks decoded ahead, the OpenAL source and the blocks queued on it.
	private final ConcurrentLinkedQueue<AChunk> chunks = new ConcurrentLinkedQueue<>();
	private volatile int flushes;
	private int flushed;
	private int source;
	private final ArrayDeque<Integer> spare = new ArrayDeque<>();
	private final ArrayDeque<AChunk> queued = new ArrayDeque<>();
	/** Chunks taken back off the source to set the sound again where the clock is (played before {@link #chunks}). */
	private final ArrayDeque<AChunk> again = new ArrayDeque<>();
	/** The last line-up logged (wall ms), the pictures shown since. */
	private long syncLogAt;
	private int shownSince;
	/** The picture's reader: pictures decoded unshown to catch a live stream's clock up. */
	private volatile int liveSkipped;
	/** The sound's speed (1: as it comes) while it's nudged back in step; how far it has been off lately (ms, eased). */
	private float pitch = 1;
	private boolean nudging;
	private double offAvg;
	private final Muffle muffled = new Muffle();
	/** The surround speakers to play from too (set each frame), and their sources. */
	private volatile List<SpeakerAt> speakersWanted = List.of();
	private final Map<BlockPos, Voice> voices = new HashMap<>();
	/**
	 * The sound where it's playing, with the volume: how loud (0 to 1) and its low end (the mean of the next 10 ms, -1 to
	 * 1: a speaker's cone is pushed out and pulled in by it); 0 silent or paused. Whether the screen's source is playing.
	 */
	private volatile float loudness, bass;
	private volatile boolean sounding;

	private record VFrame(long ts, ByteBuffer pixels, int gen) {
	}

	private record AChunk(long ts, short[] pcm) {
		long end() {
			return ts + pcm.length * 1000L / RATE;
		}

		/** The rest of it from sample {@code from}. */
		AChunk from(int from) {
			return from <= 0 ? this : new AChunk(ts + from * 1000L / RATE, java.util.Arrays.copyOfRange(pcm, Math.min(from, pcm.length), pcm.length));
		}
	}

	/** A surround speaker linked to the screen ({@link TvSpeakers}): its block, the middle of its cabinet, which way it faces. */
	record SpeakerAt(BlockPos pos, Vec3 at, float dirX, float dirZ) {
	}

	/**
	 * A speaker's own source and buffers, fed what the screen's source is fed and started, paused and stopped with it.
	 * Its queue is the screen's while it's {@link #synced}; one that isn't (new, out of buffers, drifted) is set again from
	 * the screen's queue, at the sample the screen's is playing.
	 */
	private static final class Voice {
		final int source;
		final ArrayDeque<Integer> spare = new ArrayDeque<>();
		final ArrayDeque<AChunk> queued = new ArrayDeque<>();
		final Muffle muffled = new Muffle();
		boolean synced;

		private Voice(int source) {
			this.source = source;
		}

		/** A new one, or null when OpenAL has no more sources. */
		static @Nullable Voice make() {
			AL10.alGetError();
			int s = AL10.alGenSources();
			if (AL10.alGetError() != AL10.AL_NO_ERROR || s == 0) {
				return null;
			}
			Voice v = new Voice(s);
			AL10.alSourcei(s, AL_DISTANCE_MODEL, AL_LINEAR_DISTANCE);
			AL10.alSourcef(s, AL10.AL_MAX_DISTANCE, SPEAKER_HEARD);
			AL10.alSourcef(s, AL10.AL_ROLLOFF_FACTOR, 1);
			AL10.alSourcef(s, AL10.AL_REFERENCE_DISTANCE, 0);
			AL10.alSourcei(s, AL10.AL_SOURCE_RELATIVE, 0);
			// (a speaker is loudest in front: behind it, about half)
			AL10.alSourcef(s, AL10.AL_CONE_INNER_ANGLE, 120);
			AL10.alSourcef(s, AL10.AL_CONE_OUTER_ANGLE, 300);
			AL10.alSourcef(s, AL10.AL_CONE_OUTER_GAIN, 0.55F);
			// (two more than the screen's: one just behind it never runs short)
			for (int i = 0; i < AL_BUFFERS + 2; i++) {
				v.spare.add(AL10.alGenBuffers());
			}
			AL10.alGetError();
			return v;
		}

		void processed() {
			for (int done = AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED); done > 0; done--) {
				spare.add(AL10.alSourceUnqueueBuffers(source));
				queued.poll();
			}
		}

		void stop() {
			AL10.alSourceStop(source);
			processed();
			queued.clear();
		}

		boolean queue(AChunk c) {
			Integer b = spare.poll();
			if (b == null) {
				return false;
			}
			fill(b, c.pcm());
			AL10.alSourceQueueBuffers(source, b);
			queued.add(c);
			return true;
		}

		int state() {
			return AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
		}

		/** The time (ms) of the sound it's playing; MIN_VALUE nothing queued. */
		long at() {
			AChunk c = queued.peek();
			return c == null ? Long.MIN_VALUE : c.ts() + AL10.alGetSourcei(source, AL11.AL_SAMPLE_OFFSET) * 1000L / RATE;
		}

		void delete() {
			stop();
			AL10.alDeleteSources(source);
			for (int b : spare) {
				AL10.alDeleteBuffers(b);
			}
			spare.clear();
			muffled.delete();
		}
	}

	/**
	 * The walls between the listener and a source (client/SoundOcclusion), worked out ten times a second and eased
	 * there, through the source's own low-pass filter: a wall lets a tenth through, muffled; two, nothing.
	 */
	private static final class Muffle {
		/** The source's low-pass filter: -1 not made yet, 0 none here. */
		int filter = -1;
		/** What it's set to (-1: not yet), when last worked out. */
		float gain = -1, hf = -1;
		long at;

		/** The gain to give the source itself: the filter's own gain does it where there's a filter. */
		float plainGain() {
			return filter > 0 ? 1 : gain < 0 ? 1 : Math.max(0, gain);
		}

		/** Off the source for now (full screen); worked out again the next time. */
		void off(int source) {
			if (filter > 0) {
				AL10.alSourcei(source, EXTEfx.AL_DIRECT_FILTER, EXTEfx.AL_FILTER_NULL);
			}
			gain = hf = -1;
			at = 0;
		}

		void apply(int source, Vec3 from) {
			long now = System.currentTimeMillis();
			Minecraft mc = Minecraft.getInstance();
			if (now - at < 100 || mc == null || mc.level == null) {
				return;
			}
			at = now;
			float[] want = io.github.jimbozoomer.jugcraft.client.SoundOcclusion.forWalls(
					io.github.jimbozoomer.jugcraft.client.SoundOcclusion.walls(mc.level, mc.gameRenderer.mainCamera().position(), from));
			if (filter < 0) {
				filter = 0;
				long context = ALC10.alcGetCurrentContext();
				long device = context == 0 ? 0 : ALC10.alcGetContextsDevice(context);
				if (device != 0 && ALC10.alcIsExtensionPresent(device, "ALC_EXT_EFX")) {
					int made = EXTEfx.alGenFilters();
					EXTEfx.alFilteri(made, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
					filter = AL10.alGetError() == AL10.AL_NO_ERROR ? made : 0;
				}
				gain = hf = -1;   // (set it the first time)
			}
			float g = ease(gain, want[0]), h = ease(hf, want[1]);
			if (Math.abs(g - gain) < 0.002F && Math.abs(h - hf) < 0.002F) {
				return;
			}
			gain = g;
			hf = h;
			if (filter > 0) {
				if (g >= 0.999F && h >= 0.999F) {
					AL10.alSourcei(source, EXTEfx.AL_DIRECT_FILTER, EXTEfx.AL_FILTER_NULL);
				} else {
					EXTEfx.alFilterf(filter, EXTEfx.AL_LOWPASS_GAIN, Math.max(0, g));
					EXTEfx.alFilterf(filter, EXTEfx.AL_LOWPASS_GAINHF, Math.max(0, h));
					AL10.alSourcei(source, EXTEfx.AL_DIRECT_FILTER, filter);
				}
				AL10.alGetError();
			}
		}

		void delete() {
			if (filter > 0) {
				EXTEfx.alDeleteFilters(filter);
			}
			filter = -1;
		}
	}

	/** Mono 16-bit sound into an OpenAL buffer. */
	private static void fill(int buffer, short[] pcm) {
		ShortBuffer data = MemoryUtil.memAllocShort(pcm.length);
		try {
			data.put(pcm).flip();
			AL10.alBufferData(buffer, AL10.AL_FORMAT_MONO16, data, RATE);
		} finally {
			MemoryUtil.memFree(data);
		}
	}

	TvPlayer(BlockPos pos, String url, int screenW, int screenH) {
		this(pos, url, screenW, screenH, 0);
	}

	/** A player that won't ask YouTube the ways in {@code avoidWays} (a bit for each of {@link TvHelpers#WAYS}). */
	TvPlayer(BlockPos pos, String url, int screenW, int screenH, int avoidWays) {
		this.avoidWays = avoidWays;
		this.pos = pos;
		this.url = url;
		this.textureId = Jugcraft.id("dynamic/tv_" + nextId++);
		// A small screen needs no more than 480p.
		small = small(screenW, screenH);
		maxW = small ? 854 : 1280;
		maxH = small ? 480 : 720;
		heard = HEARD + Math.max(screenW, screenH) / 2F;
	}

	static boolean small(int screenW, int screenH) {
		return screenW * screenH <= 2;
	}

	/** The screen grew or shrank (its player carried over): heard as far as the new size is (render thread). */
	void resize(int screenW, int screenH) {
		float now = HEARD + Math.max(screenW, screenH) / 2F;
		if (now != heard) {
			heard = now;
			if (source != 0 && !closed) {
				AL10.alSourcef(source, AL10.AL_MAX_DISTANCE, heard);
			}
		}
	}

	/** A new player for a jump (or started over held at one): says SEEKING TO while it loads. */
	void startsAt(long ms) {
		loading = "SEEKING TO " + clock(ms);
		state = loading;
	}

	/** It was going somewhere and stayed stuck (a jump YouTube didn't answer): started over another way. */
	boolean stuckSeeking() {
		return seekingTo >= 0 && System.currentTimeMillis() - seekStart > STUCK_MS;
	}

	/** Started over after its stream stopped: says RECONNECTING while it loads. */
	void reconnecting() {
		loading = "RECONNECTING";
		state = loading;
	}

	void start() {
		Thread t = new Thread(this::open, "Jugcraft TV " + pos.toShortString());
		t.setDaemon(true);
		t.start();
	}

	/**
	 * Finds the streams (the helper for a YouTube, Rumble or Kick page), then starts the readers; a video whose picture
	 * stream won't open is asked for again the site's next way ({@link TvHelpers.Site#ways}).
	 */
	private void open() {
		try {
			TvHelpers.Site site = TvHelpers.site(url);
			boolean yt = site != null;
			if (yt) {
				while (!TvHelpers.installed() || TvHelpers.busy) {
					if (closed) {
						return;
					}
					if (TvHelpers.busy) {
						state = "GETTING THE VIDEO HELPER " + TvHelpers.progress + "%";
					} else if (TvHelpers.failure != null) {
						state = "COULDN'T GET THE VIDEO HELPER";
					} else if (TvHelpers.consented) {
						TvHelpers.installAsync();
					} else {
						state = "VIDEO HELPER NEEDED: RIGHT-CLICK THE TV";
					}
					Thread.sleep(250);
				}
			} else {
				if (local(url)) {
					state = "LINKS TO THIS COMPUTER'S NETWORK AREN'T PLAYED";
					return;
				}
				title = fileName(url);
			}
			TvMedia media = null;
			java.util.Set<String> tried = new java.util.HashSet<>();
			// the site's ways to ask, the one that last worked first
			TvHelpers.Way[] siteWays = yt ? site.ways() : new TvHelpers.Way[0];
			int first = yt ? TvHelpers.lastGood(site) : 0, ways = yt ? siteWays.length : 1;
			// (every way left out: try them all again rather than none)
			int avoid = avoidWays & (1 << ways) - 1;
			avoid = avoid == (1 << ways) - 1 ? 0 : avoid;
			Exception last = null;
			for (int n = 0; n < ways; n++) {
				int way = n == 0 ? first : n <= first ? n - 1 : n;
				if (yt && (avoid & 1 << way) != 0) {
					continue;   // (this way's stream wouldn't go where the screen is)
				}
				String video = url, audio = url;
				Map<String, String> h = Map.of();
				if (yt) {
					state = loading;
					TvHelpers.Way w = siteWays[way];
					TvHelpers.Resolved r;
					try {
						r = TvHelpers.resolve(url, way);
					} catch (java.io.IOException e) {
						if (last == null) {
							last = e;         // (yt-dlp's first word on the video is the one shown: "Video unavailable"...)
						}
						Jugcraft.LOGGER.info("TV at {}: yt-dlp found nothing for {} as {} ({})", pos.toShortString(), url, w.name(), e.getMessage());
						continue;
					}
					boolean relay = w.relay() && r.files();
					if (!tried.add(relay + " " + r.video())) {
						continue;   // (this way found the same stream as one that wouldn't open)
					}
					video = r.video();
					audio = r.audio();
					h = options(r.headers());
					if (relay) {
						video = TvRelay.serve(r.video(), r.headers());
						audio = r.audio() == null ? null : r.audio().equals(r.video()) ? video : TvRelay.serve(r.audio(), r.headers());
						h = TvRelay.options();
					}
					title = r.title();
					durationMs = r.durationMs();
					live = r.live();
				}
				if (closed) {
					return;
				}
				if (media == null) {
					media = codec();
					if (media == null) {
						return;
					}
				}
				TvMedia.Reader g = media.open(video, h, true);
				Exception refused = null;
				try {
					g.start();
				} catch (Exception e) {
					g.close();
					refused = e;
					if (yt && !closed && shortReason(e).contains("403")) {
						// YouTube refuses a stream it has only just made out for a second or two (Justin's 27 October 2026
						// test): asked once more before another way
						Thread.sleep(2000);
						g = media.open(video, h, true);
						try {
							g.start();
							refused = null;
						} catch (Exception again) {
							g.close();
							refused = again;
						}
					}
				}
				if (refused != null) {
					if (yt && !closed) {
						Jugcraft.LOGGER.info("TV at {}: {}'s stream for {} wouldn't open as {} ({}); asking another way", pos.toShortString(), site.name(),
								url, siteWays[way].name(), shortReason(refused));
						last = refused;
						continue;
					}
					throw refused;
				}
				if (yt) {
					TvHelpers.good(site, way);
					this.way = way;
					Jugcraft.LOGGER.info("TV at {}: playing {} as {}", pos.toShortString(), url, siteWays[way].name());
				}
				final TvMedia m = media;
				final String a = audio;
				final Map<String, String> ah = h;
				final TvMedia.Reader picture = g;
				Thread vt = new Thread(() -> videoLoop(picture), "Jugcraft TV picture");
				vt.setDaemon(true);
				vt.start();
				if (a != null) {
					Thread at = new Thread(() -> audioLoop(m, a, ah), "Jugcraft TV sound");
					at.setDaemon(true);
					at.start();
				}
				return;
			}
			if (last != null) {
				throw last;
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Throwable e) {
			fail(e);
		}
	}

	private void fail(Throwable e) {
		if (closed) {
			return;
		}
		while (e instanceof java.lang.reflect.InvocationTargetException && e.getCause() != null) {
			e = e.getCause();
		}
		Jugcraft.LOGGER.warn("TV at {} couldn't play {}: {}", pos.toShortString(), url, e.toString());
		String why = e.getMessage() == null ? e.getClass().getSimpleName() : shortReason(e);
		if (e instanceof UnsatisfiedLinkError || e instanceof NoClassDefFoundError) {
			why = "the video player didn't load on this computer";
		}
		if (ready) {
			// it had played: the stream stopped mid-way (YouTube let the link lapse, the network dropped)
			state = "STREAM STOPPED: " + why.toUpperCase(Locale.ROOT).substring(0, Math.min(48, why.length()));
			broken = true;
			return;
		}
		state = "CAN'T PLAY THIS: " + why.toUpperCase(Locale.ROOT).substring(0, Math.min(60, why.length()));
	}

	/**
	 * What went wrong, short: FFmpeg's "error -858797304" is HTTP 403 (YouTube refusing the stream), and its message
	 * carries the whole stream address otherwise.
	 */
	private static String shortReason(Throwable e) {
		String m = e.getMessage() == null ? e.toString() : e.getMessage();
		if (m.contains("-858797304") || m.contains("403")) {
			return "the site refused the video's streams (HTTP 403)";
		}
		if (m.contains("-875574520") || m.contains("404")) {
			return "the stream wasn't found (HTTP 404)";
		}
		if (m.contains("Unsupported URL")) {
			return "that link isn't a video YouTube knows (check the whole link was pasted)";
		}
		int q = m.indexOf("Could not open input");
		return q >= 0 ? "the stream wouldn't open" : m.length() > 160 ? m.substring(0, 160) : m;
	}

	/** Whether the link's host is this computer or its own network (not something a screen should reach into). */
	private static boolean local(String link) {
		try {
			String host = URI.create(link).getHost();
			for (InetAddress a : InetAddress.getAllByName(host)) {
				if (a.isLoopbackAddress() || a.isSiteLocalAddress() || a.isLinkLocalAddress() || a.isAnyLocalAddress()
						|| a.getAddress().length == 16 && (a.getAddress()[0] & 0xFE) == 0xFC) {
					return true;
				}
			}
			return false;
		} catch (Exception e) {
			return false;
		}
	}

	private static String fileName(String link) {
		try {
			String path = URI.create(link).getPath();
			String name = path == null ? "" : path.substring(path.lastIndexOf('/') + 1);
			return name.isEmpty() ? URI.create(link).getHost() : java.net.URLDecoder.decode(name, java.nio.charset.StandardCharsets.UTF_8);
		} catch (Exception e) {
			return "";
		}
	}

	/** The decoder, fetched the first time (the screen shows how far along); null if this TV was let go meanwhile. */
	private @Nullable TvMedia codec() throws Exception {
		java.util.concurrent.atomic.AtomicBoolean done = new java.util.concurrent.atomic.AtomicBoolean();
		Thread watch = new Thread(() -> {
			while (!done.get() && !closed) {
				String s = TvCodec.status;
				state = s.isEmpty() ? loading : s + "  " + TvCodec.progress + "%";
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					return;
				}
			}
		}, "Jugcraft TV loading");
		watch.setDaemon(true);
		watch.start();
		try {
			TvMedia media = TvCodec.media();
			return closed ? null : media;
		} finally {
			done.set(true);
			state = loading;
		}
	}

	/** FFmpeg's options for the link: the headers yt-dlp says to send. */
	private static Map<String, String> options(Map<String, String> headers) {
		Map<String, String> o = new LinkedHashMap<>();
		StringBuilder extra = new StringBuilder();
		for (Map.Entry<String, String> e : headers.entrySet()) {
			if (e.getKey().equalsIgnoreCase("User-Agent")) {
				o.put("user_agent", e.getValue());
			} else if (!e.getKey().equalsIgnoreCase("Accept-Encoding")) {
				extra.append(e.getKey()).append(": ").append(e.getValue()).append("\r\n");
			}
		}
		if (!extra.isEmpty()) {
			o.put("headers", extra.toString());
		}
		return o;
	}

	// ------------------------------------------------------------------------------------------------- the readers

	/** The picture's reader (started already, on the open thread) decoding ahead on its own thread. */
	private void videoLoop(TvMedia.Reader g) {
		try {
			if (!g.hasVideo()) {
				state = "SOUND ONLY";
				return;
			}
			int sw = Math.max(2, g.width()), sh = Math.max(2, g.height());
			double k = Math.min(1.0, Math.min(maxW / (double) sw, maxH / (double) sh));
			int w = Math.max(2, (int) Math.round(sw * k) & ~1), h = Math.max(2, (int) Math.round(sh * k) & ~1);
			g.scaleTo(w, h);
			synchronized (lock) {
				for (int i = 0; i < MAX_FRAMES + 1; i++) {
					free.add(ByteBuffer.allocateDirect(w * h * 4));
				}
			}
			vw = w;
			vh = h;
			if (durationMs <= 0 && !live) {
				durationMs = g.lengthMs();
			}
			double fps = g.frameRate();
			info = h + "p" + (fps > 1 ? String.format(Locale.ROOT, "  ·  %d fps", Math.round(fps)) : "");
			long decoded = -1;
			int seenJumps = jumps;
			// placed: the reader is where the screen is (it starts at 0; a screen already further on is jumped to)
			boolean placed = false, catchUp = false, liveCatching = false;
			long aim = 0;
			while (!closed) {
				long t = target;
				boolean paused = pausedTarget;
				int j = jumps;
				if (!placed && (live || t <= 1500)) {
					placed = true;
				}
				boolean runOn = !catchUp && seekingTo < 0 && decoded >= 0 && decoded > t + Math.max(FAR_AHEAD, seekCost + 2500);
				if (!live && (j != seenJumps || !placed || runOn || !catchUp && decoded >= 0 && !paused && t > decoded + FAR_BEHIND)) {
					// Jump: past the screen's moment by as long as a jump takes here, to be there when it gets there.
					seenJumps = j;
					placed = true;
					long to = paused ? t : t + Math.min(MAX_LEAD, seekCost);
					if (durationMs > 0) {
						to = Math.max(0, Math.min(to, durationMs - 1000));
					}
					synchronized (lock) {
						gen++;   // (the frames from here on are the jump's: the first one shown ends the SEEKING words)
						while (!frames.isEmpty()) {
							free.add(frames.poll().pixels());
						}
					}
					seekStart = System.currentTimeMillis();
					seekingTo = t;
					placedJumps = j;
					if (decoded < 0 || to < decoded || to - decoded > SKIP_AHEAD) {
						g.seek(to);
						decoded = -1;
					}
					// (a short way on: decoded on through from here, unshown, as after a seek's key frame)
					aim = to;
					catchUp = true;
					ended = false;
					continue;
				}
				if (live && liveOffset != Long.MIN_VALUE && decoded >= 0 && t - decoded > (liveCatching ? LIVE_PICTURE_CAUGHT : LIVE_PICTURE_BEHIND)) {
					liveCatching = true;
					// A live stream's picture behind its clock (the sound keeps to the clock): decoded on unshown up to it, so the
					// picture is where the sound is (Justin, 27 October 2026: the Kick stream's sound was off from its picture).
					long ts = g.skip();
					if (ts == TvMedia.END) {
						Thread.sleep(50);
					} else {
						decoded = ts;
						liveSkipped++;
					}
					continue;
				}
				liveCatching = false;
				if (!live && placed && (catchUp || !paused && decoded >= 0 && t > decoded + 1000)) {
					// Catching up: from the key frame FFmpeg landed on (or a stretch fallen behind), decoded unshown up to the
					// moment due; never jumped again for it (a key frame several seconds back would jump for ever).
					long want = Math.max(catchUp ? aim : 0, t) - 60;
					if (decoded < want) {
						long ts = g.skip();
						if (ts == TvMedia.END) {
							ended = true;
							catchUp = false;
							seekingTo = -1;
							Thread.sleep(50);
						} else {
							decoded = ts;
						}
						continue;
					}
					if (catchUp) {
						catchUp = false;
						// how long that jump took: the next one leads by about that (quicker ones bring it down slowly)
						long took = System.currentTimeMillis() - seekStart;
						seekCost = Math.max(800, Math.min(20_000, took > seekCost ? took + 500 : (seekCost * 3 + took) / 4));
					}
				}
				ByteBuffer into;
				synchronized (lock) {
					into = frames.size() >= MAX_FRAMES || paused && decoded >= t && !frames.isEmpty() || ended ? null : free.poll();
				}
				if (into == null) {
					Thread.sleep(paused || ended ? 15 : 4);
					continue;
				}
				int g0 = gen;
				long ts = g.picture(into);
				boolean keep = ts != TvMedia.END && ts != TvMedia.NONE;
				if (ts == TvMedia.END) {
					ended = !live;
					seekingTo = -1;
				}
				if (keep) {
					decoded = ts;
					if (live && liveOffset == Long.MIN_VALUE) {
						liveOffset = ts - System.currentTimeMillis();
					}
					// Behind: drop it and catch up.
					keep = live || (paused ? ts >= t - 40 : ts >= t - 120);
				}
				synchronized (lock) {
					if (keep) {
						frames.add(new VFrame(ts, into, g0));
					} else {
						free.add(into);
					}
				}
				if (keep && !ready) {
					ready = true;
					state = "";
				}
				if (ts == TvMedia.END) {
					Thread.sleep(50);
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Throwable e) {
			fail(e);
		} finally {
			g.close();
		}
	}

	private void audioLoop(TvMedia media, String src, Map<String, String> options) {
		TvMedia.Reader g = media.open(src, options, false);
		try {
			g.start();
			if (!g.hasAudio()) {
				return;
			}
			long decoded = -1;
			long[] at = new long[1];
			short[] block = new short[BLOCK];
			int fill = 0;
			long blockTs = 0;
			int seenJumps = jumps;
			boolean placed = false;
			while (!closed) {
				long t = target;
				boolean paused = pausedTarget;
				int j = jumps;
				if (!placed && (live || t <= 1500)) {
					placed = true;
				}
				boolean runOn = decoded >= 0 && decoded - t > Math.max(FAR_AHEAD, seekCost + 2500);
				if (!live && (j != seenJumps || !placed || runOn || decoded >= 0 && !paused && t > decoded + FAR_BEHIND)) {
					// Jump with the picture (as far past the moment): the sound's key frames are close, it lands near enough.
					seenJumps = j;
					placed = true;
					long to = paused ? t : t + Math.min(MAX_LEAD, seekCost);
					if (durationMs > 0) {
						to = Math.max(0, Math.min(to, durationMs - 1000));
					}
					if (decoded < 0 || to < decoded || to - decoded > SKIP_AHEAD) {
						g.seek(to);
						decoded = -1;
					}
					// (a short way on: decoded on through from here, the blocks before the moment dropped)
					chunks.clear();
					flushes++;
					fill = 0;
				}
				// paused: caught up to the moment it's paused at (the sound ready there when it plays), then waits
				if (paused && (decoded >= 0 && decoded >= t - 50 || live) || !live && decoded >= 0 && decoded - t > 900) {
					Thread.sleep(10);
					continue;
				}
				short[] pcm = g.sound(at);
				if (pcm == null) {
					Thread.sleep(50);
					continue;
				}
				long ts = at[0];
				decoded = ts + pcm.length * 1000L / RATE;
				if (!live && decoded < t - 50) {
					fill = 0;
					continue;   // behind: catch up
				}
				for (int i = 0; i < pcm.length; i++) {
					if (fill == 0) {
						blockTs = ts + i * 1000L / RATE;
					}
					block[fill++] = pcm[i];
					if (fill == BLOCK) {
						chunks.add(new AChunk(blockTs, block.clone()));
						fill = 0;
					}
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Throwable e) {
			Jugcraft.LOGGER.warn("TV at {}: no sound ({})", pos.toShortString(), e.toString());
		} finally {
			g.close();
		}
	}

	// ------------------------------------------------------------------------------------------------- the render thread

	/**
	 * Each frame, from the renderer: where the screen is ({@code positionMs}, paused or not), its middle and volume.
	 * Shows the frame due and keeps the sound fed.
	 */
	void update(long positionMs, boolean paused, Vec3 middle, float volume) {
		long now = System.currentTimeMillis();
		long t = live ? (liveOffset == Long.MIN_VALUE ? 0 : now + liveOffset) : durationMs > 0 ? Math.min(positionMs, durationMs) : positionMs;
		// A jump: the clock isn't where it would have run to (a seek, a replay round). (The readers are told where first,
		// then that it's a jump: one seeing the jump sees where to.)
		boolean jump = !live && lastTarget != Long.MIN_VALUE && Math.abs(t - (lastTarget + (pausedTarget ? 0 : now - lastWall))) > JUMP
				&& !(durationMs > 0 && t >= durationMs && lastTarget >= durationMs);
		lastTarget = t;
		lastWall = now;
		target = t;
		pausedTarget = paused && !live;
		if (jump) {
			jumps++;
		}
		VFrame pick = null;
		synchronized (lock) {
			while (!frames.isEmpty() && frames.peek().ts() <= t + 20) {
				if (pick != null) {
					free.add(pick.pixels());
				}
				pick = frames.poll();
			}
			// nothing due yet: the first picture, or paused, the first of a jump (where the screen is paused); never paused
			// pictures one after another (the picture ran on while the sound was paused, and was then ahead of it)
			if (pick == null && !frames.isEmpty() && (!shown || pausedTarget && frames.peek().gen() != shownGen)) {
				pick = frames.poll();
			}
		}
		if (pick != null) {
			show(pick.pixels());
			shownSince++;
			shownTs = pick.ts();
			shownGen = pick.gen();
			if (pick.gen() == gen) {
				seekingTo = -1;   // the jump's first picture is up
			}
			synchronized (lock) {
				free.add(pick.pixels());
			}
		}
		// Late: playing, the picture well behind the moment due (nothing new decoded in time)
		boolean late = ready && !pausedTarget && !ended && !live && (seekingTo >= 0 || shownTs >= 0 && t > shownTs + LATE);
		if (!late) {
			lateSince = 0;
		} else if (lateSince == 0) {
			lateSince = now;
		} else if (now - lateSince > STUCK_MS) {
			broken = true;
		}
		// A jump that hasn't come in for that long, paused or held there too, or before the first picture: started over
		if (seekingTo >= 0 && now - seekStart > STUCK_MS) {
			broken = true;
		}
		try {
			// Only the screen's own pause holds the sound. The game's pause menu doesn't: the picture follows the screen's
			// clock, which runs on (Justin, 27 October 2026: Escape stopped the sound while the picture went on, and it came
			// back behind the picture).
			sound(t, pausedTarget, middle, volume);
		} catch (Throwable e) {
			// no sound device: the picture still plays
		}
	}

	private void show(ByteBuffer pixels) {
		int w = vw, h = vh;
		if (texture == null || texture.getPixels() == null || texture.getPixels().getWidth() != w || texture.getPixels().getHeight() != h) {
			if (texture != null) {
				Minecraft.getInstance().getTextureManager().release(textureId);
			}
			texture = new DynamicTexture(() -> "Jugcraft TV " + pos.toShortString(), w, h, false);
			Minecraft.getInstance().getTextureManager().register(textureId, texture);
		}
		MemoryUtil.memCopy(MemoryUtil.memAddress(pixels), texture.getPixels().getPointer(), (long) w * h * 4);
		texture.upload();
		shown = true;
	}

	/**
	 * What the screen says over its picture: what holds it up (a jump being made, the stream not keeping up, a stopped
	 * stream); "" while it plays.
	 */
	String status() {
		if (!state.isEmpty()) {
			return state;
		}
		long now = System.currentTimeMillis();
		long jumping = seekingTo;
		if (jumping >= 0) {
			long s = (now - seekStart) / 1000;
			return "SEEKING TO " + clock(jumping) + (s >= 2 ? "  ·  " + s + " S" : "") + (s >= 8 ? "  ·  WAITING ON THE STREAM" : "");
		}
		long late = lateSince;
		if (late != 0) {
			long s = (now - late) / 1000;
			return "BUFFERING" + (s >= 2 ? "  ·  " + s + " S" : "") + (s >= 8 ? "  ·  THE STREAM ISN'T KEEPING UP" : "");
		}
		return "";
	}

	/**
	 * Whether the picture is where the screen is: the first picture up, the screen's last jump set out on and its
	 * picture shown (a screen held for a seek starts playing once this game has it).
	 */
	boolean inPlace() {
		return ready && shown && placedJumps == jumps && seekingTo < 0;
	}

	/**
	 * The time (ms) of the picture showing while it lags behind the screen's clock (a jump being made, buffering), for
	 * the window to show what's on the screen; -1 while it's in step.
	 */
	long lagMs() {
		long at = shownTs;
		return ready && at >= 0 && (seekingTo >= 0 || lateSince != 0) ? at : -1;
	}

	/** The time (ms) of the picture last shown; -1 none yet. */
	long shownMs() {
		return shownTs;
	}

	/** How long (ms) the picture has been behind the screen's clock while it plays (BUFFERING); 0 it isn't. */
	long lateFor() {
		long since = lateSince;
		return since == 0 ? 0 : System.currentTimeMillis() - since;
	}

	/** h:mm:ss or m:ss. */
	static String clock(long ms) {
		long s = Math.max(0, ms / 1000);
		return s >= 3600 ? String.format(Locale.ROOT, "%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60) : String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
	}

	/** Whether there's a picture to show, and its shape (width over height). */
	boolean hasPicture() {
		return shown && texture != null;
	}

	float aspect() {
		return vh > 0 ? vw / (float) vh : 16 / 9F;
	}

	/** The surround speakers linked to the screen, to play its sound from too (each frame, before {@link #update}). */
	void speakers(List<SpeakerAt> speakers) {
		speakersWanted = speakers;
	}

	/** How loud the sound playing is (0 to 1, with the volume; 0 silent or paused). */
	float loudness() {
		return loudness;
	}

	/** The sound's low end where it's playing (-1 to 1, with the volume; 0 silent or paused). */
	float bass() {
		return bass;
	}

	/** Whether its sound is playing (not paused, held or run dry). */
	boolean sounding() {
		return sounding;
	}

	private void sound(long t, boolean hold, Vec3 middle, float volume) {
		if (closed || ALC10.alcGetCurrentContext() == 0) {
			quiet();
			return;
		}
		if (source == 0) {
			source = AL10.alGenSources();
			AL10.alSourcei(source, AL_DISTANCE_MODEL, AL_LINEAR_DISTANCE);
			AL10.alSourcef(source, AL10.AL_MAX_DISTANCE, heard);
			AL10.alSourcef(source, AL10.AL_ROLLOFF_FACTOR, 1);
			AL10.alSourcef(source, AL10.AL_REFERENCE_DISTANCE, 0);
			AL10.alSourcei(source, AL10.AL_SOURCE_RELATIVE, 0);
			for (int i = 0; i < AL_BUFFERS; i++) {
				spare.add(AL10.alGenBuffers());
			}
		}
		if (atListener) {
			// Full screen: heard as if sitting in front of it, wherever the player is (no fall-off, no walls).
			AL10.alSourcei(source, AL10.AL_SOURCE_RELATIVE, 1);
			AL10.alSource3f(source, AL10.AL_POSITION, 0, 0, 0);
			muffled.off(source);   // (set again when it's back on the screen)
			AL10.alSourcef(source, AL10.AL_GAIN, volume);
		} else {
			AL10.alSourcei(source, AL10.AL_SOURCE_RELATIVE, 0);
			AL10.alSource3f(source, AL10.AL_POSITION, (float) middle.x, (float) middle.y, (float) middle.z);
			muffled.apply(source, middle);
			// (without the filter the walls still take the loudness down)
			AL10.alSourcef(source, AL10.AL_GAIN, volume * muffled.plainGain());
		}
		// The speakers: made or let go as they're linked, placed (still where they are in the world when it's full screen).
		placeVoices(volume);
		for (int done = AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED); done > 0; done--) {
			spare.add(AL10.alSourceUnqueueBuffers(source));
			queued.poll();
		}
		for (Voice v : voices.values()) {
			v.processed();
		}
		int playing = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
		// A jump (the reader moved), or the sound drifted from the picture: start the sound again where it should be. A live
		// stream's too, with more leeway (its sound had nothing to catch it up: once behind, behind for good).
		boolean drift = false;
		// (a live stream's clock is its picture's: with no picture yet its sound just plays)
		boolean timed = !live || liveOffset != Long.MIN_VALUE;
		long soundAt = Long.MIN_VALUE;
		if (playing == AL10.AL_PLAYING && !queued.isEmpty() && timed) {
			soundAt = queued.peek().ts() + AL10.alGetSourcei(source, AL11.AL_SAMPLE_OFFSET) * 1000L / RATE;
			drift = Math.abs(soundAt - t) > SOUND_DRIFT;
		}
		if (live && timed) {
			logLineUp(t, soundAt);
		}
		float want = 1;
		if (soundAt != Long.MIN_VALUE && !drift) {
			double off = soundAt - t;
			offAvg += (off - offAvg) * 0.08;
			if (Math.abs(offAvg) > NUDGE_FROM) {
				nudging = true;
			} else if (Math.abs(offAvg) < NUDGE_DONE) {
				nudging = false;
			}
			// ahead: a little slower; behind: a little faster
			want = nudging ? (float) (1 - Math.max(-NUDGE, Math.min(NUDGE, offAvg / 4000))) : 1;
		} else {
			nudging = false;
			offAvg = 0;
		}
		setPitch(want);
		if (flushes != flushed) {
			// a jump: the readers have moved on, what was queued is the old place's
			flushed = flushes;
			again.clear();
			stopSound();
			playing = AL10.AL_STOPPED;
		} else if (drift) {
			// drifted (the sound card's clock isn't the game's, the sound ran short): set again where the clock is, from the
			// chunks it had queued (they cover the clock's moment), with no gap
			java.util.List<AChunk> back = new java.util.ArrayList<>(queued);
			stopSound();
			for (int i = back.size() - 1; i >= 0; i--) {
				again.addFirst(back.get(i));
			}
			playing = AL10.AL_STOPPED;
		}
		syncVoices(playing);
		if (hold) {
			if (playing == AL10.AL_PLAYING) {
				AL10.alSourcePause(source);
			}
			for (Voice v : voices.values()) {
				if (v.state() == AL10.AL_PLAYING) {
					AL10.alSourcePause(v.source);
				}
			}
			quiet();
			return;
		}
		while (!spare.isEmpty()) {
			AChunk c = again.isEmpty() ? chunks.peek() : again.peek();
			if (c == null) {
				break;
			}
			// Starting (nothing queued), from the clock's moment: what ends before it is too late. Playing on, a live
			// stream's sound keeps a little behind (nothing would catch it up); a video's drops what's late.
			if (timed && c.end() <= t - (live && !queued.isEmpty() ? LIVE_DRIFT : 0)) {
				nextChunk();   // too late
				continue;
			}
			if (queued.isEmpty() && timed) {
				if (c.ts() > t + 30) {
					break;   // not yet
				}
				nextChunk();
				int skip = (int) Math.max(0, (t - c.ts()) * RATE / 1000);
				if (skip > 0 && skip < c.pcm().length) {
					c = new AChunk(t, java.util.Arrays.copyOfRange(c.pcm(), skip, c.pcm().length));
				}
			} else {
				nextChunk();
			}
			int buffer = spare.poll();
			fill(buffer, c.pcm());
			AL10.alSourceQueueBuffers(source, buffer);
			queued.add(c);
			for (Voice v : voices.values()) {
				if (v.synced && !v.queue(c)) {
					v.synced = false;   // (set again from the screen's next frame)
				}
			}
		}
		if (playing != AL10.AL_PLAYING && !queued.isEmpty()) {
			// the screen's and the speakers' together, at the same moment
			int n = 1;
			int[] all = new int[1 + voices.size()];
			all[0] = source;
			for (Voice v : voices.values()) {
				if (v.synced && !v.queued.isEmpty() && v.state() != AL10.AL_PLAYING) {
					all[n++] = v.source;
				}
			}
			AL10.alSourcePlayv(java.util.Arrays.copyOf(all, n));
			playing = AL10.AL_PLAYING;
		}
		if (playing == AL10.AL_PLAYING) {
			level(volume);
		} else {
			quiet();
		}
	}

	private void quiet() {
		loudness = bass = 0;
		sounding = false;
	}

	/** The sound's speed, the screen's source's and the speakers' alike (they stay in step with it). */
	private void setPitch(float want) {
		if (want == pitch) {
			return;
		}
		pitch = want;
		AL10.alSourcef(source, AL10.AL_PITCH, want);
		for (Voice v : voices.values()) {
			AL10.alSourcef(v.source, AL10.AL_PITCH, want);
		}
	}

	/** Takes the chunk {@link #sound} just looked at: the next of those taken back, else of the sound's reader. */
	private void nextChunk() {
		if (again.poll() == null) {
			chunks.poll();
		}
	}

	/**
	 * A live stream's line-up, every {@link #SYNC_LOG_MS}: how far the sound is from the picture and both from the clock,
	 * the pictures shown and decoded unshown since (play-log.txt; for working out what a stream does on a player's PC).
	 */
	private void logLineUp(long t, long soundAt) {
		long now = System.currentTimeMillis();
		if (syncLogAt == 0) {
			syncLogAt = now;
			return;
		}
		if (now - syncLogAt < SYNC_LOG_MS) {
			return;
		}
		float secs = (now - syncLogAt) / 1000F;
		long pic = shownTs;
		Jugcraft.LOGGER.info("TV at {}: live line-up: sound {} the picture, picture {} ms behind its clock, sound {}; {} pictures a second shown, {} decoded unshown",
				pos.toShortString(), soundAt == Long.MIN_VALUE || pic < 0 ? "not playing against" : String.format(Locale.ROOT, "%+d ms from", soundAt - pic),
				pic < 0 ? "?" : Long.toString(t - pic), soundAt == Long.MIN_VALUE ? "not playing" : String.format(Locale.ROOT, "%+d ms from it", soundAt - t),
				String.format(Locale.ROOT, "%.1f", shownSince / secs), liveSkipped);
		syncLogAt = now;
		shownSince = 0;
		liveSkipped = 0;
	}

	/** The speakers linked now: a source made for each new one (up to {@link #MAX_SPEAKERS}), let go for each gone; placed. */
	private void placeVoices(float volume) {
		List<SpeakerAt> want = speakersWanted;
		if (want.isEmpty() && voices.isEmpty()) {
			return;
		}
		for (Iterator<Map.Entry<BlockPos, Voice>> it = voices.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<BlockPos, Voice> e = it.next();
			boolean still = false;
			for (SpeakerAt s : want) {
				if (s.pos().equals(e.getKey())) {
					still = true;
					break;
				}
			}
			if (!still) {
				e.getValue().delete();
				it.remove();
			}
		}
		for (SpeakerAt s : want) {
			Voice v = voices.get(s.pos());
			if (v == null) {
				if (voices.size() >= MAX_SPEAKERS || (v = Voice.make()) == null) {
					continue;
				}
				voices.put(s.pos(), v);
			}
			AL10.alSourcef(v.source, AL10.AL_PITCH, pitch);
			AL10.alSource3f(v.source, AL10.AL_POSITION, (float) s.at().x, (float) s.at().y, (float) s.at().z);
			AL10.alSource3f(v.source, AL10.AL_DIRECTION, s.dirX(), 0, s.dirZ());
			v.muffled.apply(v.source, s.at());
			AL10.alSourcef(v.source, AL10.AL_GAIN, volume * v.muffled.plainGain());
		}
	}

	/**
	 * The speakers not in step with the screen's source (new ones, ones run short of buffers or drifted): each set again
	 * from the screen's queue at the sample it's at, and started if the screen's is playing (else started with it).
	 */
	private void syncVoices(int playing) {
		if (voices.isEmpty()) {
			return;
		}
		long mainAt = Long.MIN_VALUE;
		int offset = 0;
		if (!queued.isEmpty()) {
			offset = AL10.alGetSourcei(source, AL11.AL_SAMPLE_OFFSET);
			mainAt = queued.peek().ts() + offset * 1000L / RATE;
		}
		for (Voice v : voices.values()) {
			if (v.synced && playing == AL10.AL_PLAYING && mainAt != Long.MIN_VALUE) {
				long at = v.at();
				if (v.state() != AL10.AL_PLAYING || at == Long.MIN_VALUE || Math.abs(at - mainAt) > SPEAKER_DRIFT) {
					v.synced = false;
				}
			}
			if (v.synced) {
				continue;
			}
			v.stop();
			boolean first = true;
			v.synced = true;
			for (AChunk c : queued) {
				if (!v.queue(first ? c.from(offset) : c)) {
					v.synced = false;
					break;
				}
				first = false;
			}
			if (v.synced && playing == AL10.AL_PLAYING && !v.queued.isEmpty()) {
				AL10.alSourcePlay(v.source);
			}
		}
	}

	/** The sound where the screen's source is playing: its loudness (root mean square) and low end (mean) over 10 ms. */
	private void level(float volume) {
		sounding = true;
		AChunk c = queued.peek();
		short[] pcm = c == null ? null : c.pcm();
		int from = pcm == null ? 0 : Math.max(0, Math.min(pcm.length, AL10.alGetSourcei(source, AL11.AL_SAMPLE_OFFSET)));
		int to = pcm == null ? 0 : Math.min(pcm.length, from + RATE / 100);
		if (to <= from) {
			loudness = bass = 0;
			return;
		}
		double sum = 0, sq = 0;
		for (int i = from; i < to; i++) {
			sum += pcm[i];
			sq += (double) pcm[i] * pcm[i];
		}
		int n = to - from;
		loudness = (float) Math.min(1, Math.sqrt(sq / n) / 32768 * volume);
		bass = (float) Math.max(-1, Math.min(1, sum / n / 32768 * volume));
	}

	private static float ease(float from, float to) {
		return from < 0 || Math.abs(to - from) < 0.01F ? to : from + (to - from) * 0.5F;
	}

	private void stopSound() {
		AL10.alSourceStop(source);
		for (int done = AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED); done > 0; done--) {
			spare.add(AL10.alSourceUnqueueBuffers(source));
		}
		queued.clear();
		for (Voice v : voices.values()) {
			v.stop();
			v.synced = true;   // (both empty: in step)
		}
	}

	/** Stops the readers and frees the texture and the sound (render thread). */
	void close() {
		closed = true;
		if (texture != null) {
			Minecraft.getInstance().getTextureManager().release(textureId);
			texture = null;
		}
		try {
			if (source != 0 && ALC10.alcGetCurrentContext() != 0) {
				stopSound();
				AL10.alDeleteSources(source);
				for (int b : spare) {
					AL10.alDeleteBuffers(b);
				}
				muffled.delete();
				for (Voice v : voices.values()) {
					v.delete();
				}
			}
		} catch (Throwable ignored) {
			// the sound device is gone already
		}
		source = 0;
		muffled.filter = -1;
		voices.clear();
		spare.clear();
		quiet();
	}

	/** The game's volume for the TV: the Jukebox/Note Blocks slider (and the master). */
	static float gameVolume() {
		return Minecraft.getInstance().options.getFinalSoundSourceVolume(SoundSource.RECORDS);
	}
}
