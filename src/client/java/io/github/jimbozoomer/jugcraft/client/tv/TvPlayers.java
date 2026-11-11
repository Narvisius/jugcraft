package io.github.jimbozoomer.jugcraft.client.tv;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.tv.FlatTvBlock;
import io.github.jimbozoomer.jugcraft.tv.TvBlockEntity;
import io.github.jimbozoomer.jugcraft.tv.TvPayloads;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The screens this game is playing: one {@link Slot} a screen that's on and near (the renderer asks each frame), at
 * most {@value #MAX} at once (the nearest), each let go a moment after it's no longer drawn. A slot works out what its
 * screen shows at the screen's clock: the link's video, or (a playlist link, the whole playlist ticked) the playlist's
 * video due and how far into it ({@link TvProgram}); on replay round again from the start. Its {@link TvPlayer} plays
 * that, and the playlist's next video is made ready in the background a few seconds before it is due. PLAY LINK
 * (the screen's link played again, the same one too) starts the slot over: its player is let go and the streams are
 * found afresh. A player whose stream stopped or stayed stuck ({@link TvPlayer#broken}) is started over the same way,
 * at the screen's moment, up to {@value #RESTARTS} times in {@value #RESTART_WINDOW_MS} ms. While this game's picture is
 * stuck (started over, or buffering {@value #HOLD_LATE_MS} ms), the screen's clock is held at the last picture shown
 * here and started again once the picture's back (Justin, 27 October 2026: the clock ran on under RECONNECTING with
 * nothing playing). A screen made bigger or
 * smaller whose bottom-left block moved (blocks added to its left or below, or its corner taken out) keeps its slot: the
 * slot moves to the new bottom-left block and its player plays on (it isn't opened again, which YouTube would soon refuse).
 */
public final class TvPlayers {
	static final int MAX = 4;
	private static final long LINGER_MS = 1500;
	/** The next video of a playlist is found this long before it's due, and opened (paused at its start) this long. */
	private static final long FIND_AHEAD_MS = 30_000, OPEN_AHEAD_MS = 8_000;
	/** A broken player is started over at most this many times in this long (then it says why it stopped). */
	private static final int RESTARTS = 3;
	private static final long RESTART_WINDOW_MS = 120_000;
	private static final Map<BlockPos, Slot> SLOTS = new HashMap<>();
	/**
	 * Screens whose link this player set, or that this player moved on or back (a seek): when the picture's in place
	 * there, this game starts it for everyone.
	 */
	private static final Map<BlockPos, Long> MINE = new HashMap<>();
	/**
	 * How long another game waits for the setter's to start a held screen (ms): then any game here with the picture ready
	 * starts it (the setter left, their game couldn't play it). The setter's own game starts it only once it has the
	 * picture there (Justin, 27 October 2026: after 15 s the clock ran on while the jump to 33:39 was still loading).
	 */
	private static final long AUTOPLAY_GRACE_MS = 60_000, MINE_MS = 300_000;
	/** The picture this long behind while it plays (BUFFERING): the clock is held for it; at most once in {@link #HOLD_GAP_MS}. */
	static final long HOLD_LATE_MS = 3000;
	private static final long HOLD_GAP_MS = 10_000;
	/**
	 * {@link #moved}: the screen's old bottom-left block isn't let go here yet (its update still to come): a slot with
	 * nothing in it for the frame (never kept).
	 */
	private static final Slot WAIT = new Slot(BlockPos.ZERO);
	/** Playlists found (or being found) by link: null while yt-dlp lists one. */
	private static final Map<String, @Nullable TvProgram> PROGRAMS = new HashMap<>();
	private static final Map<String, String> PROGRAM_FAILED = new HashMap<>();
	/** The screen this game is watching full screen ({@link TvFullScreen}; null none): its sound is heard as if close by. */
	private static @Nullable BlockPos fullscreen;

	/** One screen as this game plays it. */
	static final class Slot {
		BlockPos pos;
		/** The screen as last drawn: its link, size and facing (to know it again when its bottom-left block moves). */
		String link = "";
		int w = 1, h = 1;
		Direction facing = Direction.NORTH;
		@Nullable TvPlayer player;
		/** The playlist's next video, opened ahead (paused at its start, silent). */
		@Nullable TvPlayer next;
		long seen;
		/** What the screen is at: how far into what the player plays, and where that started on the screen's clock. */
		long offset, entryStart;
		/** The length of what's playing (the playlist's listed length, else the player's own); 0 not known yet. */
		long durationMs;
		boolean ended;
		/** The playlist (null for a single video), the video's number in it (from 1) and its title. */
		@Nullable TvProgram program;
		int number;
		String entryTitle = "";
		/** What the screen says while there's no player yet ("LOADING THE PLAYLIST"). */
		String state = "";
		/** The screen's link count this slot plays ({@link TvBlockEntity#session}); MIN_VALUE not seen yet. */
		int session = Integer.MIN_VALUE;
		/** Starts over after a stop: how many lately, since when. */
		int restarts;
		long restartsFrom;
		/** The ways of asking YouTube the next player leaves out (a bit each): ones that wouldn't go where the screen is. */
		int avoidWays;
		/** The next player is for a jump (it says SEEKING TO while it loads). */
		boolean jumpStart;
		/**
		 * Since when it has been held (a new link at its start, a seek at its new moment; 0: it isn't) and where; when
		 * this game last asked to start it.
		 */
		long heldSince, heldAt, playSent;
		/** This game held the clock for its own picture (RECONNECTING, BUFFERING; "" it didn't), and when. */
		String holdWhy = "";
		long holdSent;

		Slot(BlockPos pos) {
			this.pos = pos;
		}

		void close() {
			if (player != null) {
				player.close();
				player = null;
			}
			if (next != null) {
				next.close();
				next = null;
			}
		}
	}

	private TvPlayers() {
	}

	/**
	 * The slot of the screen at {@code be} (its controller), worked out for this frame; null when too many play nearer
	 * (an empty one for a frame while the screen's bottom-left block moves here).
	 */
	static @Nullable Slot frame(TvBlockEntity be, int w, int h) {
		BlockPos pos = be.getBlockPos().immutable();
		Slot slot = SLOTS.get(pos);
		Vec3 middle = middle(be, w, h);
		Direction facing = facing(be);
		if (slot == null) {
			slot = moved(be, pos, w, h, facing);
			if (slot == WAIT) {
				return WAIT;   // (nothing on the screen for a frame or so)
			}
		}
		if (slot == null) {
			if (SLOTS.size() >= MAX && !evictFartherThan(middle)) {
				return null;
			}
			slot = new Slot(pos);
			SLOTS.put(pos, slot);
		}
		slot.seen = System.currentTimeMillis();
		String url = be.url();
		slot.link = url;
		slot.w = w;
		slot.h = h;
		slot.facing = facing;
		if (slot.session != be.session()) {
			if (slot.session != Integer.MIN_VALUE) {
				// PLAY LINK: start over (a stuck stream let go, the streams found afresh, from the start)
				slot.close();
				TvHelpers.forgetAll();
				PROGRAMS.remove(url);
				PROGRAM_FAILED.remove(url);
				slot.restarts = 0;
			}
			slot.session = be.session();
		}
		long now = System.currentTimeMillis();
		if (be.autoplay() && be.paused()) {
			// (held again somewhere else, a seek on from a seek: the wait starts again)
			if (slot.heldSince == 0 || slot.heldAt != be.position()) {
				slot.heldSince = now;
				slot.heldAt = be.position();
			}
		} else {
			slot.heldSince = 0;
		}
		long position = be.position();
		boolean loop = be.loop();
		// What's due: the playlist's video, or the link's.
		String play = url;
		slot.program = null;
		slot.state = "";
		if (be.playlist() && TvHelpers.isPlaylist(url)) {
			TvProgram program = program(url);
			if (program != null) {
				TvProgram.At at = program.at(position, loop);
				slot.program = program;
				slot.number = at.number();
				slot.entryTitle = at.entry().title();
				slot.offset = at.offset();
				slot.entryStart = at.entryStart();
				slot.durationMs = at.entry().durationMs();
				slot.ended = at.ended();
				play = at.entry().url();
				ahead(slot, program, at, loop, w, h);
			} else if (!PROGRAM_FAILED.containsKey(url)) {
				slot.close();
				slot.state = TvHelpers.installed() ? "LOADING THE PLAYLIST" : "VIDEO HELPER NEEDED: RIGHT-CLICK THE TV";
				return slot;
			}
		}
		if (slot.player != null && !slot.player.url.equals(play)) {
			slot.player.close();
			slot.player = null;
			// the playlist's next video, opened ahead, takes over
			if (slot.next != null && slot.next.url.equals(play)) {
				slot.player = slot.next;
				slot.next = null;
			}
		}
		if (slot.player != null && slot.player.broken && !slot.player.closed) {
			if (now - slot.restartsFrom > RESTART_WINDOW_MS) {
				slot.restarts = 0;
				slot.restartsFrom = now;
			}
			if (slot.restarts < RESTARTS) {
				// the stream stopped or stayed stuck: find it again and go on from the screen's moment (a jump that stayed
				// stuck: another way of asking YouTube than the one that didn't get there)
				slot.restarts++;
				Jugcraft.LOGGER.info("TV at {}: {} stopped ({}); starting it over", pos.toShortString(), play, slot.player.status());
				hold(be, slot, slot.player, "RECONNECTING", now);
				if (slot.player.stuckSeeking() && slot.player.way >= 0) {
					slot.avoidWays |= 1 << slot.player.way;
					slot.jumpStart = true;
				}
				slot.player.close();
				slot.player = null;
				TvHelpers.forgetAll();
			}
		}
		if (slot.player == null) {
			slot.player = new TvPlayer(pos, play, w, h, slot.avoidWays);
			long at = slot.program != null ? slot.offset : position;
			if (slot.jumpStart && at > 1500) {
				slot.player.startsAt(at);
			} else if (slot.restarts > 0) {
				slot.player.reconnecting();
			}
			slot.jumpStart = false;
			slot.player.start();
		}
		TvPlayer p = slot.player;
		p.resize(w, h);
		if (slot.avoidWays != 0 && p.inPlace()) {
			slot.avoidWays = 0;   // (there: the next jump may ask any way again, the one that worked first)
		}
		if (slot.program == null && slot.next != null) {
			slot.next.close();
			slot.next = null;
		}
		if (slot.program == null) {
			// A single video: on replay, round again from its start once its length is known.
			long dur = p.durationMs;
			slot.durationMs = dur;
			slot.entryStart = 0;
			slot.number = 0;
			slot.entryTitle = "";
			if (loop && dur > 0 && !p.live) {
				slot.offset = position % dur;
				slot.entryStart = position - slot.offset;
				slot.ended = false;
			} else {
				slot.offset = position;
				slot.ended = dur > 0 && !p.live && position >= dur;
			}
		}
		p.seen = slot.seen;
		p.atListener = pos.equals(fullscreen);
		// (the surround speakers linked to it play its sound too)
		p.speakers(TvSpeakers.of(pos));
		p.update(slot.offset, be.paused(), middle, be.volume() / 100F * TvPlayer.gameVolume());
		if (p.lateFor() > HOLD_LATE_MS && now - slot.holdSent > HOLD_GAP_MS) {
			hold(be, slot, p, "BUFFERING", now);
		}
		if (!slot.holdWhy.isEmpty() && now - slot.holdSent > 2000 && !(be.autoplay() && be.paused())) {
			slot.holdWhy = "";   // (started again)
		}
		if (slot.next != null) {
			slot.next.seen = slot.seen;
			slot.next.update(0, true, middle, 0);
		}
		// Held (a new link at its start, a seek at its new moment) until the game of whoever did it has the picture there:
		// this game starts it if it did it and the picture's in place, or if it has been held a while (they left, their game
		// couldn't play it, the game was started again); the server lets only the screen's party start it.
		Long setAt = MINE.get(pos);
		boolean mine = setAt != null && now - setAt < MINE_MS;
		if (p.ready && be.autoplay() && be.paused() && (mine ? p.inPlace() : slot.heldSince != 0 && now - slot.heldSince > AUTOPLAY_GRACE_MS)
				&& now - slot.playSent > 3000) {
			MINE.remove(pos);
			slot.playSent = now;
			ClientPlayNetworking.send(new TvPayloads.Control(pos, TvPayloads.Control.PLAY));
		}
		return slot;
	}

	/**
	 * Holds the screen's clock at the last picture this game showed while its picture is stuck ({@code why}: started
	 * over, buffering), as the server holds it for a seek: this game starts it again once the picture's in place there
	 * (other games after {@link #AUTOPLAY_GRACE_MS}). Only while it plays (not paused, held already, live or ended); the
	 * server takes it only from the screen's party.
	 */
	private static void hold(TvBlockEntity be, Slot slot, TvPlayer p, String why, long now) {
		if (be.paused() || p.live || slot.ended) {
			return;
		}
		long shown = p.shownMs();
		long at = shown >= 0 ? slot.entryStart + shown : be.position();
		at = Math.max(0, Math.min(at, be.position()));
		slot.holdSent = now;
		slot.holdWhy = why;
		MINE.put(slot.pos.immutable(), now);
		ClientPlayNetworking.send(new TvPayloads.Control(slot.pos, TvPayloads.Control.SEEK, "", at));
	}

	/**
	 * The slot of a screen whose bottom-left block moved to {@code pos} (blocks added to its left or below, its corner taken
	 * out): one playing the same link (the same link count) whose screen and this one overlap. It moves here, its player
	 * playing on; {@link #WAIT} while the old bottom-left block is still a screen's here (its update to come); null: none.
	 */
	private static @Nullable Slot moved(TvBlockEntity be, BlockPos pos, int w, int h, Direction facing) {
		String url = be.url();
		if (url.isEmpty()) {
			return null;
		}
		for (Slot s : SLOTS.values()) {
			if (s.pos.equals(pos) || s.session != be.session() || !s.link.equals(url) || s.facing != facing) {
				continue;
			}
			boolean overlap = FlatTvBlock.screenDistance(pos, facing, w, h, Vec3.atCenterOf(s.pos)) == 0
					|| FlatTvBlock.screenDistance(s.pos, facing, s.w, s.h, Vec3.atCenterOf(pos)) == 0;
			if (!overlap) {
				continue;
			}
			if (be.getLevel() != null && be.getLevel().getBlockEntity(s.pos) instanceof TvBlockEntity old && old.isController()
					&& old.session() == s.session && old.url().equals(url)) {
				return WAIT;
			}
			if (s.player != null && s.player.small != TvPlayer.small(w, h)) {
				return null;   // (grown past two blocks: a new player, for the sharper picture)
			}
			SLOTS.remove(s.pos);
			Long mine = MINE.remove(s.pos);
			if (mine != null) {
				MINE.put(pos, mine);
			}
			s.pos = pos;
			SLOTS.put(pos, s);
			return s;
		}
		return null;
	}

	private static Direction facing(TvBlockEntity be) {
		return be.getBlockState().getBlock() instanceof FlatTvBlock ? be.getBlockState().getValue(FlatTvBlock.FACING) : Direction.NORTH;
	}

	/** The playlist's next video: found a while before it's due, and opened (paused at its start) just before. */
	private static void ahead(Slot slot, TvProgram program, TvProgram.At at, boolean loop, int w, int h) {
		long left = at.entry().durationMs() - at.offset();
		if (at.ended() || !program.hasNext(at.k(), loop)) {
			return;
		}
		String nextUrl = program.entry(at.k() + 1).url();
		if (nextUrl.equals(at.entry().url())) {
			return;
		}
		if (left < FIND_AHEAD_MS) {
			TvHelpers.prefetch(nextUrl);
		}
		if (left < OPEN_AHEAD_MS && (slot.next == null || !slot.next.url.equals(nextUrl))) {
			if (slot.next != null) {
				slot.next.close();
			}
			slot.next = new TvPlayer(slot.pos, nextUrl, w, h);
			slot.next.start();
		}
	}

	/** The playlist the link names, found in the background the first time it's asked for; null until then. */
	private static @Nullable TvProgram program(String url) {
		if (PROGRAMS.containsKey(url)) {
			return PROGRAMS.get(url);
		}
		if (!TvHelpers.installed()) {
			if (TvHelpers.consented) {
				TvHelpers.installAsync();
			}
			return null;
		}
		PROGRAMS.put(url, null);
		Thread t = new Thread(() -> {
			try {
				TvProgram found = TvHelpers.playlist(url);
				Minecraft.getInstance().execute(() -> PROGRAMS.put(url, found));
			} catch (Exception e) {
				Jugcraft.LOGGER.warn("TV: couldn't list the playlist {}: {}", url, e.toString());
				// played as the single video the link names, and not asked again this session
				Minecraft.getInstance().execute(() -> PROGRAM_FAILED.put(url, e.getMessage() == null ? e.toString() : e.getMessage()));
			}
		}, "Jugcraft TV playlist");
		t.setDaemon(true);
		t.start();
		return null;
	}

	/** The screen at {@code pos} was turned off: stop its picture and sound now. */
	static void stop(BlockPos pos) {
		Slot s = SLOTS.remove(pos);
		if (s != null) {
			s.close();
		}
	}

	static @Nullable Slot slot(BlockPos pos) {
		return SLOTS.get(pos);
	}

	static @Nullable TvPlayer get(BlockPos pos) {
		Slot s = SLOTS.get(pos);
		return s == null ? null : s.player;
	}

	/** The screen this game watches full screen (null: none). */
	static void fullscreen(@Nullable BlockPos pos) {
		fullscreen = pos == null ? null : pos.immutable();
	}

	/** This player set the screen's link: start it for everyone once it's ready here. */
	static void setByMe(BlockPos pos) {
		MINE.put(pos.immutable(), System.currentTimeMillis());
	}

	private static boolean evictFartherThan(Vec3 middle) {
		Vec3 eye = Minecraft.getInstance().gameRenderer.mainCamera().position();
		Slot far = null;
		double farD = eye.distanceToSqr(middle);
		for (Slot s : SLOTS.values()) {
			double d = eye.distanceToSqr(Vec3.atCenterOf(s.pos));
			if (d > farD) {
				far = s;
				farD = d;
			}
		}
		if (far == null) {
			return false;
		}
		far.close();
		SLOTS.remove(far.pos);
		return true;
	}

	/** The middle of the screen's glass, in the world (where its sound comes from). */
	static Vec3 middle(TvBlockEntity be, int w, int h) {
		Direction facing = facing(be);
		Direction right = facing.getClockWise();
		BlockPos p = be.getBlockPos();
		return new Vec3(p.getX() + 0.5 + right.getStepX() * (w - 1) / 2.0 - facing.getStepX() * 0.3, p.getY() + h / 2.0,
				p.getZ() + 0.5 + right.getStepZ() * (w - 1) / 2.0 - facing.getStepZ() * 0.3);
	}

	/** Each client tick: lets go of the screens no longer drawn (out of range, turned off, broken). */
	static void tick() {
		long now = System.currentTimeMillis();
		for (Iterator<Slot> it = SLOTS.values().iterator(); it.hasNext(); ) {
			Slot s = it.next();
			if (now - s.seen > LINGER_MS) {
				s.close();
				it.remove();
			}
		}
	}

	static void closeAll() {
		for (Slot s : SLOTS.values()) {
			s.close();
		}
		SLOTS.clear();
		MINE.clear();
		PROGRAMS.clear();
		PROGRAM_FAILED.clear();
		TvSpeakers.clear();
	}
}
