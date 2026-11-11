package io.github.jimbozoomer.jugcraft.tv;

import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A flat screen TV block. Every block remembers who placed it; the screen's bottom-left block (its controller) holds
 * what the screen plays: the link, the server's clock when the video was at its start (or where it is paused), the
 * volume, whether it starts again when it ends (replay) and plays a playlist link's whole list, and whose party may
 * change it. The server keeps only that; each player's game plays the video itself and
 * works out where it should be from the server's clock, which the update packet carries ("now").
 */
public class TvBlockEntity extends BlockEntity {
	public static final int DEFAULT_VOLUME = 70;
	/** The longest link kept (the codec refuses longer). */
	public static final int MAX_URL = 1024;

	private @Nullable UUID placer;
	/** The screen's size in blocks on its controller (0 on its other blocks): set by {@link FlatTvBlock#rejoin}. */
	private int w, h;
	private @Nullable UUID owner;
	private String url = "";
	/** The server's clock (ms) when the video was at 0, while playing. */
	private long start;
	private boolean paused;
	/** How far into the video (ms) it is paused. */
	private long pausedAt;
	private int volume = DEFAULT_VOLUME;
	/**
	 * Held for the player who set it: a link just set (held at 0), or a seek made while it played (held at the new
	 * moment), until that player's game has the picture there; then it plays for everyone.
	 */
	private boolean autoplay;
	/** Replay: when the video (or the playlist) ends it starts again from the beginning. */
	private boolean loop;
	/** A playlist link plays its whole list, one video after another (else only the video the link names). */
	private boolean playlist;
	/** Counts the links played (PLAY LINK, even the same link again): each game starts its player over on a new one. */
	private int session;
	/** A removed controller's screen, handed to this block until the screen is joined up again. */
	private @Nullable Snapshot orphan;
	/** The server's clock less this game's (ms), from the last update (client only). */
	private long clockOffset;

	/**
	 * What a screen plays, carried over when screens join or split. area: the screen's size, the bigger kept; session:
	 * its link count (the same count, each game's player goes on playing on the joined-up screen).
	 */
	public record Snapshot(@Nullable UUID owner, String url, long start, boolean paused, long pausedAt, int volume, boolean autoplay, boolean loop,
			boolean playlist, int area, int session) {
	}

	public TvBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftTv.TV_ENTITY, pos, state);
	}

	// ------------------------------------------------------------------------------------------------- the screen

	public boolean isController() {
		return w > 0;
	}

	public int screenWidth() {
		return w;
	}

	public int screenHeight() {
		return h;
	}

	public @Nullable UUID placer() {
		return placer;
	}

	public void setPlacer(@Nullable UUID placer) {
		this.placer = placer;
		if (isController() && owner == null) {
			owner = placer;
			sync();
		} else {
			setChanged();
		}
	}

	public @Nullable UUID owner() {
		return owner;
	}

	public String url() {
		return url;
	}

	public boolean paused() {
		return paused;
	}

	public int volume() {
		return volume;
	}

	public boolean autoplay() {
		return autoplay;
	}

	public boolean loop() {
		return loop;
	}

	public boolean playlist() {
		return playlist;
	}

	/** Counts the links played: a new count is the screen started over (its streams found again, from the start). */
	public int session() {
		return session;
	}

	/** This block's screen data (a controller's, or one handed to it), or null; the block keeps nothing after. */
	@Nullable Snapshot takeSnapshot() {
		Snapshot s = isController() ? new Snapshot(owner, url, start, paused, pausedAt, volume, autoplay, loop, playlist, w * h, session) : orphan;
		orphan = null;
		return s;
	}

	/** Makes this block the controller of a w x h screen playing what {@code s} played (a fresh screen if null). */
	void adopt(@Nullable Snapshot s, int w, int h) {
		this.w = w;
		this.h = h;
		if (s == null) {
			owner = placer;
			url = "";
			start = 0;
			paused = false;
			pausedAt = 0;
			volume = DEFAULT_VOLUME;
			autoplay = false;
			loop = false;
			playlist = false;
		} else {
			owner = s.owner() != null ? s.owner() : placer;
			url = s.url();
			start = s.start();
			paused = s.paused();
			pausedAt = s.pausedAt();
			volume = s.volume();
			autoplay = s.autoplay();
			loop = s.loop();
			playlist = s.playlist();
			session = s.session();
		}
	}

	/** No longer a controller. */
	void release() {
		w = 0;
		h = 0;
		owner = null;
		url = "";
		start = 0;
		paused = false;
		pausedAt = 0;
		volume = DEFAULT_VOLUME;
		autoplay = false;
		loop = false;
		playlist = false;
	}

	/** How far {@code p} is from the nearest part of this controller's screen (blocks). */
	public double screenDistance(Vec3 p) {
		Direction facing = getBlockState().getBlock() instanceof FlatTvBlock ? getBlockState().getValue(FlatTvBlock.FACING) : Direction.NORTH;
		return FlatTvBlock.screenDistance(worldPosition, facing, Math.max(1, w), Math.max(1, h), p);
	}

	/** Whether {@code player} may change what this screen plays: its owner's party (anyone, if it has none), or creative. */
	public boolean mayControl(Player player) {
		return owner == null || owner.equals(player.getUUID()) || JugcraftParties.sameParty(owner, player.getUUID())
				|| player.getAbilities().instabuild;
	}

	// ------------------------------------------------------------------------------------------------- the clock

	/** Where the video is (ms) at the server's clock {@code now}. */
	public long positionAt(long now) {
		return paused ? pausedAt : Math.max(0, now - start);
	}

	/** The server's clock as this game has it (on the server, its own). */
	public long serverNow() {
		return System.currentTimeMillis() + clockOffset;
	}

	/** Where the video is now (ms), on either side. */
	public long position() {
		return positionAt(serverNow());
	}

	// ------------------------------------------------------------------------------------------------- what players do

	/**
	 * A new link: held at its start until whoever set it has it ready ({@link #play}): 0, or the moment a timestamped
	 * link names ({@link #linkStartMs}). An empty link turns the screen off.
	 */
	void setLink(String link) {
		url = link;
		session++;
		paused = !link.isEmpty();
		pausedAt = linkStartMs(link);
		start = 0;
		autoplay = !link.isEmpty();
		sync();
	}

	/**
	 * Where a YouTube, Rumble or Kick link says to start (ms; 0 none): YouTube's {@code t=} (90, 90s, 1h2m3s) or
	 * {@code start=}, Rumble's {@code start=} (seconds; Justin's 27 October 2026 link had start=8947). Other links' query
	 * strings aren't read (a stream address's t= is often a cache-breaking clock).
	 */
	public static long linkStartMs(String link) {
		String host, query;
		try {
			java.net.URI u = java.net.URI.create(link.trim());
			host = u.getHost();
			query = u.getRawQuery();
		} catch (IllegalArgumentException e) {
			return 0;
		}
		if (host == null || query == null) {
			return 0;
		}
		host = host.toLowerCase(java.util.Locale.ROOT);
		boolean site = false;
		for (String d : new String[] {"youtube.com", "youtu.be", "youtube-nocookie.com", "rumble.com", "kick.com"}) {
			site |= host.equals(d) || host.endsWith("." + d);
		}
		if (!site) {
			return 0;
		}
		for (String part : query.split("&")) {
			int eq = part.indexOf('=');
			if (eq <= 0) {
				continue;
			}
			String key = part.substring(0, eq), value = part.substring(eq + 1).toLowerCase(java.util.Locale.ROOT);
			if (key.equals("t") || key.equals("start")) {
				long s = seconds(value);
				if (s > 0) {
					return Math.min(s, 7L * 24 * 3600) * 1000;
				}
			}
		}
		return 0;
	}

	/** "8947", "90s", "1h2m3s", "2m10s": seconds (0 if it isn't a time). */
	static long seconds(String v) {
		if (v.matches("\\d+(\\.\\d+)?s?")) {
			return (long) Double.parseDouble(v.endsWith("s") ? v.substring(0, v.length() - 1) : v);
		}
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?").matcher(v);
		if (!m.matches()) {
			return 0;
		}
		long t = 0;
		for (int g = 1; g <= 3; g++) {
			if (m.group(g) != null) {
				t += Long.parseLong(m.group(g)) * (g == 1 ? 3600 : g == 2 ? 60 : 1);
			}
		}
		return t;
	}

	void play(long now) {
		if (paused && !url.isEmpty()) {
			start = now - pausedAt;
			paused = false;
		}
		autoplay = false;
		sync();
	}

	void pause(long now) {
		if (!paused && !url.isEmpty()) {
			pausedAt = Math.max(0, now - start);
			paused = true;
		}
		autoplay = false;
		sync();
	}

	/**
	 * A seek: playing, the screen is held at the new moment (as a new link is) until the game of whoever moved it has the
	 * picture there, then it plays on from exactly there for everyone ({@link #play}); paused, it stays paused there.
	 */
	void seek(long now, long to) {
		to = Math.max(0, to);
		if (!paused && !url.isEmpty()) {
			paused = true;
			autoplay = true;
		}
		pausedAt = to;
		sync();
	}

	void setVolume(int v) {
		volume = Math.max(0, Math.min(100, v));
		sync();
	}

	void setLoop(boolean on) {
		loop = on;
		sync();
	}

	void setPlaylist(boolean on) {
		playlist = on;
		sync();
	}

	void sync() {
		setChanged();
		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	/**
	 * A controller about to be removed hands what its screen plays to a block beside it on the same screen, so
	 * breaking the bottom-left block doesn't lose the link ({@link FlatTvBlock#rejoin} takes it up).
	 */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level == null || level.isClientSide() || !isController() || !(state.getBlock() instanceof FlatTvBlock)) {
			return;
		}
		Direction facing = state.getValue(FlatTvBlock.FACING);
		Snapshot s = new Snapshot(owner, url, start, paused, pausedAt, volume, autoplay, loop, playlist, w * h, session);
		Direction d = state.getValue(FlatTvBlock.RIGHT) ? facing.getClockWise() : state.getValue(FlatTvBlock.UP) ? Direction.UP : null;
		if (d != null) {
			if (level.getBlockEntity(pos.relative(d)) instanceof TvBlockEntity next) {
				next.orphan = s;
				return;
			}
		}
	}

	// ------------------------------------------------------------------------------------------------- saving

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		placer = input.read("placer", UUIDUtil.CODEC).orElse(null);
		w = input.getIntOr("w", 0);
		h = input.getIntOr("h", 0);
		owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
		url = input.getStringOr("url", "");
		if (url.length() > MAX_URL) {
			url = "";
		}
		start = input.getLongOr("start", 0);
		paused = input.getBooleanOr("paused", false);
		pausedAt = input.getLongOr("paused_at", 0);
		volume = Math.max(0, Math.min(100, input.getIntOr("volume", DEFAULT_VOLUME)));
		autoplay = input.getBooleanOr("autoplay", false);
		loop = input.getBooleanOr("loop", false);
		playlist = input.getBooleanOr("playlist", false);
		session = input.getIntOr("session", 0);
		long now = input.getLongOr("now", 0);
		if (now != 0) {
			clockOffset = now - System.currentTimeMillis();
		} else if (autoplay && paused && !url.isEmpty()) {
			// Loaded from the save (the server or the game started again) with a new link still held for its setter, who
			// isn't here to start it: it starts now, from where it was held (it stayed held for good before, the screen
			// saying LOADING until it was broken and placed again).
			start = System.currentTimeMillis() - pausedAt;
			paused = false;
			autoplay = false;
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (placer != null) {
			output.store("placer", UUIDUtil.CODEC, placer);
		}
		if (w > 0) {
			output.putInt("w", w);
			output.putInt("h", h);
			if (owner != null) {
				output.store("owner", UUIDUtil.CODEC, owner);
			}
			output.putString("url", url);
			output.putLong("start", start);
			output.putBoolean("paused", paused);
			output.putLong("paused_at", pausedAt);
			output.putInt("volume", volume);
			output.putBoolean("autoplay", autoplay);
			output.putBoolean("loop", loop);
			output.putBoolean("playlist", playlist);
			output.putInt("session", session);
		}
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = saveWithoutMetadata(registries);
		// The server's clock as the packet leaves: the game receiving it sets its offset from it.
		tag.putLong("now", System.currentTimeMillis());
		return tag;
	}
}
