package io.github.jimbozoomer.jugcraft.client.tv;

import io.github.jimbozoomer.jugcraft.tv.FlatTvBlock;
import io.github.jimbozoomer.jugcraft.tv.JugcraftTv;
import io.github.jimbozoomer.jugcraft.tv.TvBlockEntity;
import io.github.jimbozoomer.jugcraft.tv.TvPayloads;
import java.net.URI;
import java.util.Locale;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The SCREEN window (right-click a flat screen TV; the mockup r22b/tv/gui.png): what's on the screen live, what's
 * playing and how many are watching, the seek bar, back and forward 10 s, pause or play, stop, the volume, and the
 * VIDEO LINK to play a YouTube link or a video file's address; REPLAY WHEN IT ENDS, and (a playlist link) PLAY WHOLE
 * PLAYLIST, greyed for a link that isn't one (Justin, 27 October 2026). With a playlist the window shows which of its
 * videos is on, and the seek bar is that video's. Only the owner's party works the controls (the server checks too);
 * everyone else sees them greyed. The YouTube helper's download is offered here. It opens from as far as
 * {@link FlatTvBlock#FAR_REACH} blocks from the screen's nearest part (client/tv/TvFarUse) and stays open while the
 * player is within that and a little more; the time and the bar show what's on the screen (while it catches up after a
 * seek or buffers, the picture's own time), and a seek made while it plays holds the screen there (SEEKING) until this
 * game has the picture, then it plays on from exactly there.
 */
public class TvScreen extends Screen {
	private static final int W = 308, H = 238;
	/** The tick boxes' row, and where each starts. */
	private static final int TICK_Y = 200, LOOP_X = 10, LIST_X = 150;
	/** The screen's bottom-left block (it follows it when the screen is made bigger or smaller). */
	private BlockPos pos;
	private final boolean control;
	private final String owner;
	private int x0, y0, x1, y1;
	private EditBox link;
	/** What's being dragged: 1 the seek bar, 2 the volume; and where to. */
	private int drag;
	private float dragAt;
	private String note = "";

	public TvScreen(TvPayloads.Open open) {
		super(Component.literal("SCREEN"));
		pos = open.pos();
		control = open.control();
		owner = open.owner();
	}

	private @Nullable TvBlockEntity tv() {
		return minecraft != null && minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof TvBlockEntity tv ? tv : null;
	}

	private int[] size() {
		TvBlockEntity tv = tv();
		if (tv == null || minecraft == null || minecraft.level == null || !(tv.getBlockState().getBlock() instanceof FlatTvBlock)) {
			return new int[] {1, 1};
		}
		return FlatTvBlock.sizeFrom(minecraft.level, pos, tv.getBlockState());
	}

	@Override
	protected void init() {
		x0 = (width - W) / 2;
		y0 = (height - H) / 2;
		x1 = x0 + W;
		y1 = y0 + H;
		String was = link == null ? null : link.getValue();
		link = addRenderableWidget(new EditBox(font, x0 + 14, y0 + 170, (x1 - 76) - (x0 + 14) - 4, 10, Component.literal("VIDEO LINK")));
		link.setBordered(false);
		link.setMaxLength(TvBlockEntity.MAX_URL);
		link.setTextColor(TvGui.WHITE);
		link.setTextColorUneditable(TvGui.GREY);
		TvBlockEntity tv = tv();
		link.setValue(was != null ? was : tv == null ? "" : tv.url());
		link.setEditable(control);
		link.setHint(Component.literal("https://www.youtube.com/watch?v=..."));
		if (control && link.getValue().isEmpty()) {
			setInitialFocus(link);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	/** How far into what's playing it is (the playlist's video, the video on this round of replay). */
	private long position(TvBlockEntity tv, TvPlayers.@Nullable Slot slot) {
		if (slot == null) {
			return tv.position();
		}
		return slot.durationMs > 0 ? Math.min(slot.offset, slot.durationMs) : slot.offset;
	}

	/** The length of what's playing (0 not known). */
	private static long duration(TvPlayers.@Nullable Slot slot) {
		return slot == null ? 0 : slot.durationMs;
	}

	/** Where to seek on the screen's clock for {@code ms} into what's playing. */
	private static long seekTo(TvPlayers.@Nullable Slot slot, long ms) {
		return Math.max(0, (slot == null ? 0 : slot.entryStart) + ms);
	}

	/** Whether the playlist tick box can be ticked: the link playing, or the one typed, names a playlist. */
	private boolean playlistLink(@Nullable TvBlockEntity tv) {
		return tv != null && TvHelpers.isPlaylist(tv.url()) || TvHelpers.isPlaylist(link.getValue().trim());
	}

	private static void tickBox(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font, int x, int y, String label, boolean on, boolean enabled,
			boolean hover) {
		int edge = !enabled ? TvGui.DIM : hover ? TvGui.WHITE : TvGui.CYAN;
		g.fill(x, y, x + 9, y + 9, TvGui.WELL);
		g.outline(x, y, 9, 9, edge);
		if (on) {
			g.fill(x + 2, y + 2, x + 7, y + 7, enabled ? TvGui.CYAN : TvGui.DIM);
		}
		g.text(font, label, x + 13, y + 1, enabled ? on ? TvGui.WHITE : TvGui.GREY : TvGui.DIM, false);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractBackground(g, mouseX, mouseY, delta);
		TvBlockEntity tv = tv();
		TvPlayer p = TvPlayers.get(pos);
		int[] size = size();
		boolean on = tv != null && !tv.url().isEmpty();
		TvGui.window(g, font, x0, y0, x1, y1, "SCREEN", "NARV-TECH  ·  " + size[0] + " × " + size[1]);
		// held for whoever set it: a new link (held at its start) LOADING, a seek (held at the new moment) SEEKING; held by
		// this game for its own picture, why (RECONNECTING, BUFFERING)
		TvPlayers.Slot held = TvPlayers.slot(pos);
		String chip = !on ? "OFF" : tv.paused() && tv.autoplay() ? held != null && !held.holdWhy.isEmpty() ? held.holdWhy
				: tv.position() > 1500 ? "SEEKING" : "LOADING" : tv.paused() ? "PAUSED" : "PLAYING";
		TvGui.chip(g, font, x1 - 8, y0 + 4, chip, !on ? TvGui.GREY : chip.equals("PLAYING") ? TvGui.GREEN : TvGui.AMBER);
		// What's on the screen, live.
		TvGui.box(g, font, x0 + 8, y0 + 28, x0 + 140, y0 + 104, "ON THE SCREEN", "");
		int px0 = x0 + 18, py0 = y0 + 39, px1 = px0 + 112, py1 = py0 + 63;
		g.fill(px0, py0, px1, py1, 0xFF06070A);
		if (p != null && p.hasPicture()) {
			float a = p.aspect();
			int w = 112, h = Math.round(112 / a);
			if (h > 63) {
				h = 63;
				w = Math.round(63 * a);
			}
			int ix = px0 + (112 - w) / 2, iy = py0 + (63 - h) / 2;
			g.blit(p.textureId, ix, iy, ix + w, iy + h, 0, 1, 0, 1);
		} else {
			String s = on ? p == null ? "LOADING" : p.state.isEmpty() ? "LOADING" : p.state : "STANDBY";
			g.centeredText(font, TvGui.fit(font, s, 106), (px0 + px1) / 2, (py0 + py1) / 2 - 4, TvGui.DIM);
		}
		g.outline(px0, py0, px1 - px0, py1 - py0, 0xFF28465A);
		TvGui.box(g, font, x0 + 146, y0 + 28, x1 - 8, y0 + 104, "NOW PLAYING", "");
		// The link field's well.
		g.fill(x0 + 10, y0 + 166, x1 - 76, y0 + 182, TvGui.WELL);
		g.outline(x0 + 10, y0 + 166, x1 - 76 - (x0 + 10), 16, link.isFocused() ? TvGui.CYAN : TvGui.LINE);
		g.fill(x0 + 8, y0 + 217, x1 - 8, y0 + 218, 0xFF223A4E);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		TvBlockEntity tv = tv();
		TvPlayers.Slot slot = TvPlayers.slot(pos);
		TvPlayer p = slot == null ? null : slot.player;
		boolean on = tv != null && !tv.url().isEmpty();
		int tx = x0 + 152, tw = x1 - 14 - tx;
		// What's playing (a playlist's: which of its videos).
		TvProgram program = on && slot != null ? slot.program : null;
		String title = p != null && !p.title.isEmpty() ? p.title : program != null && !slot.entryTitle.isEmpty() ? slot.entryTitle
				: on ? host(tv.url()) : "Nothing playing";
		marquee(g, title, tx, y0 + 42, tw, TvGui.WHITE);
		String from = program != null ? "VIDEO " + slot.number + " OF " + program.size() + (program.title.isEmpty() ? "" : "  ·  " + program.title)
				: on ? source(tv.url()) : "Paste a link below to play it";
		g.text(font, TvGui.fit(font, from, tw), tx, y0 + 53, program != null ? TvGui.CYAN : TvGui.GREY, false);
		boolean yt = on && TvHelpers.needsHelper(tv.url()) || TvHelpers.needsHelper(link.getValue());
		if (yt && !TvHelpers.installed()) {
			String why = TvHelpers.busy ? TvHelpers.status + "  " + TvHelpers.progress + "%"
					: TvHelpers.failure != null ? "Couldn't get it: " + TvHelpers.failure : "Needs the yt-dlp helper (about 65 MB)";
			g.text(font, TvGui.fit(font, why, tw), tx, y0 + 66, TvHelpers.failure != null ? TvGui.RED : TvGui.AMBER, false);
			if (!TvHelpers.busy) {
				boolean hover = TvGui.in(mouseX, mouseY, tx, y0 + 77, tx + 70, y0 + 90);
				TvGui.button(g, font, tx, y0 + 77, tx + 70, y0 + 90, "GET HELPER", false, hover, true);
			}
		} else {
			// what holds the picture up (SEEKING TO, BUFFERING, a stopped stream), else the picture's size
			String held = p != null && p.ready ? p.status() : "";
			String info = !held.isEmpty() ? held : p != null && !p.info.isEmpty() ? p.info : on ? "" : "YouTube, Rumble, Kick, MP4 or WebM";
			g.text(font, TvGui.fit(font, info, tw), tx, y0 + 66, held.isEmpty() ? TvGui.DIM : held.startsWith("STREAM STOPPED") ? TvGui.RED : TvGui.AMBER, false);
			g.text(font, TvGui.fit(font, "in step for everyone", tw), tx, y0 + 77, TvGui.DIM, false);
		}
		g.text(font, "WATCHING  " + watching(tv), tx, y0 + 91, TvGui.CYAN, false);
		// The seek bar (the playlist's video's).
		long dur = duration(slot);
		boolean live = p != null && p.live;
		// what's on the screen: the picture's own time while it catches up or buffers, else the screen's clock
		long lag = p == null ? -1 : p.lagMs();
		long at = tv == null || !on ? 0 : lag >= 0 ? lag : position(tv, slot);
		float frac = drag == 1 ? dragAt : dur > 0 ? Math.min(1, at / (float) dur) : 0;
		g.text(font, live ? "LIVE" : time(drag == 1 ? (long) (dragAt * dur) : at), x0 + 10, y0 + 114, live ? TvGui.RED : TvGui.WHITE, false);
		String total = dur > 0 ? time(dur) : "--:--";
		g.text(font, total, x1 - 10 - font.width(total), y0 + 114, TvGui.GREY, false);
		slider(g, x0 + 42, y0 + 118, x1 - 44, frac, on && dur > 0 && control);
		// The transport and the volume.
		// (held for a seek or a new link it's as good as playing: PAUSE keeps it paused there)
		boolean playing = on && (!tv.paused() || tv.autoplay());
		String[] labels = {"◀◀ 10s", playing ? "❙❙ PAUSE" : "▶ PLAY", "10s ▶▶", "■ STOP"};
		int[] widths = {44, 54, 44, 40};
		int bx = x0 + 10;
		for (int k = 0; k < 4; k++) {
			boolean enabled = control && on && (k != 0 && k != 2 || dur > 0);
			boolean hover = TvGui.in(mouseX, mouseY, bx, y0 + 130, bx + widths[k], y0 + 146);
			TvGui.button(g, font, bx, y0 + 130, bx + widths[k], y0 + 146, labels[k], k == 1 && playing, hover, enabled);
			bx += widths[k] + 5;
		}
		int vx = bx + 8;
		g.text(font, "VOL", vx, y0 + 134, TvGui.CYAN, false);
		float vol = drag == 2 ? dragAt : tv == null ? 0.7F : tv.volume() / 100F;
		slider(g, vx + 22, y0 + 138, x1 - 36, vol, control);
		String pct = Math.round(vol * 100) + "%";
		g.text(font, pct, x1 - 10 - font.width(pct), y0 + 134, TvGui.WHITE, false);
		// The link.
		g.text(font, "VIDEO LINK", x0 + 10, y0 + 156, TvGui.CYAN, false);
		boolean hoverPlay = TvGui.in(mouseX, mouseY, x1 - 70, y0 + 166, x1 - 10, y0 + 182);
		TvGui.button(g, font, x1 - 70, y0 + 166, x1 - 10, y0 + 182, "PLAY LINK", true, hoverPlay, control);
		String hint = !note.isEmpty() ? note : "Paste a YouTube, Rumble or Kick link, or MP4/WebM";
		g.text(font, TvGui.fit(font, hint, W - 20), x0 + 10, y0 + 188, note.isEmpty() ? TvGui.DIM : TvGui.AMBER, false);
		// Replay, and the whole playlist (greyed for a link that isn't one).
		boolean loopOn = tv != null && tv.loop();
		tickBox(g, font, x0 + LOOP_X, y0 + TICK_Y, "REPLAY WHEN IT ENDS", loopOn, control,
				TvGui.in(mouseX, mouseY, x0 + LOOP_X, y0 + TICK_Y - 2, x0 + LIST_X - 8, y0 + TICK_Y + 11));
		boolean listable = playlistLink(tv);
		tickBox(g, font, x0 + LIST_X, y0 + TICK_Y, "PLAY WHOLE PLAYLIST", tv != null && tv.playlist() && listable, control && listable,
				TvGui.in(mouseX, mouseY, x0 + LIST_X, y0 + TICK_Y - 2, x1 - 10, y0 + TICK_Y + 11));
		// Who can change it (the whole line: a long name scrolls).
		String who = owner.isEmpty() ? "Anyone can change it" : "Only " + owner + "'s party can change it";
		marquee(g, who, x0 + 10, y0 + 224, W - 20, control ? TvGui.GREY : TvGui.AMBER);
	}

	/** A line of text in {@code width}: as it is if it fits, else scrolling slowly round (never cut short). */
	private void marquee(GuiGraphicsExtractor g, String text, int x, int y, int width, int colour) {
		int w = font.width(text);
		if (w <= width) {
			g.text(font, text, x, y, colour, false);
			return;
		}
		int gap = 30, period = w + gap;
		int off = (int) ((System.currentTimeMillis() / 40) % period);
		g.enableScissor(x, y - 1, x + width, y + 9);
		g.text(font, text, x - off, y, colour, false);
		g.text(font, text, x - off + period, y, colour, false);
		g.disableScissor();
	}

	private static void slider(GuiGraphicsExtractor g, int x0, int y, int x1, float frac, boolean enabled) {
		frac = Math.max(0, Math.min(1, frac));
		g.fill(x0, y - 2, x1, y + 2, 0xFF080E14);
		g.outline(x0, y - 2, x1 - x0, 4, TvGui.CYAN_DIM);
		int k = x0 + Math.round((x1 - x0) * frac);
		g.fill(x0 + 1, y - 1, k, y + 1, enabled ? TvGui.CYAN : TvGui.DIM);
		g.fill(k - 2, y - 4, k + 2, y + 4, 0xFF0C1420);
		g.outline(k - 2, y - 4, 4, 8, enabled ? TvGui.CYAN : TvGui.DIM);
	}

	private int watching(@Nullable TvBlockEntity tv) {
		if (tv == null || minecraft == null || minecraft.level == null) {
			return 0;
		}
		int[] s = size();
		Vec3 middle = TvPlayers.middle(tv, s[0], s[1]);
		int n = 0;
		for (Player pl : minecraft.level.players()) {
			if (pl.position().distanceToSqr(middle) <= TvPlayer.HEARD * TvPlayer.HEARD) {
				n++;
			}
		}
		return n;
	}

	static String time(long ms) {
		long s = Math.max(0, ms / 1000);
		return s >= 3600 ? String.format(Locale.ROOT, "%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60) : String.format(Locale.ROOT, "%02d:%02d", s / 60, s % 60);
	}

	private static String host(String url) {
		try {
			String h = URI.create(url).getHost();
			return h == null ? url : h;
		} catch (Exception e) {
			return url;
		}
	}

	private static String source(String url) {
		TvHelpers.Site site = TvHelpers.site(url);
		return site != null ? site.name() : host(url);
	}

	// ------------------------------------------------------------------------------------------------- input

	private void send(int action, String text, long value) {
		ClientPlayNetworking.send(new TvPayloads.Control(pos, action, text, value));
	}

	/** A seek: this game starts the screen again once it has the picture there (the server holds it till then). */
	private void seek(long to) {
		TvPlayers.setByMe(pos);
		send(TvPayloads.Control.SEEK, "", to);
	}

	private void playLink() {
		String url = link.getValue().trim();
		if (!JugcraftTv.validLink(url)) {
			note = "That isn't a web address (it starts https://).";
			return;
		}
		note = "";
		if (TvHelpers.needsHelper(url)) {
			TvHelpers.installAsync();
		}
		TvPlayers.setByMe(pos);
		send(TvPayloads.Control.LINK, url, 0);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x(), my = event.y();
		TvBlockEntity tv = tv();
		TvPlayers.Slot slot = TvPlayers.slot(pos);
		boolean on = tv != null && !tv.url().isEmpty();
		long dur = duration(slot);
		int tx = x0 + 152;
		boolean yt = on && TvHelpers.needsHelper(tv.url()) || TvHelpers.needsHelper(link.getValue());
		if (yt && !TvHelpers.installed() && !TvHelpers.busy && TvGui.in(mx, my, tx, y0 + 77, tx + 70, y0 + 90)) {
			TvHelpers.installAsync();
			return true;
		}
		if (control && TvGui.in(mx, my, x1 - 70, y0 + 166, x1 - 10, y0 + 182)) {
			playLink();
			return true;
		}
		if (control && tv != null && TvGui.in(mx, my, x0 + LOOP_X, y0 + TICK_Y - 2, x0 + LIST_X - 8, y0 + TICK_Y + 11)) {
			send(TvPayloads.Control.LOOP, "", tv.loop() ? 0 : 1);
			return true;
		}
		if (control && tv != null && playlistLink(tv) && TvGui.in(mx, my, x0 + LIST_X, y0 + TICK_Y - 2, x1 - 10, y0 + TICK_Y + 11)) {
			send(TvPayloads.Control.PLAYLIST, "", tv.playlist() ? 0 : 1);
			return true;
		}
		if (control && on) {
			int[] widths = {44, 54, 44, 40};
			int bx = x0 + 10;
			long at = position(tv, slot);
			for (int k = 0; k < 4; k++) {
				if (TvGui.in(mx, my, bx, y0 + 130, bx + widths[k], y0 + 146)) {
					switch (k) {
						case 0 -> {
							if (dur > 0) {
								seek(seekTo(slot, Math.max(0, at - 10_000)));
							}
						}
						case 1 -> send(tv.paused() && !tv.autoplay() ? TvPayloads.Control.PLAY : TvPayloads.Control.PAUSE, "", 0);
						case 2 -> {
							if (dur > 0) {
								seek(seekTo(slot, Math.min(dur, at + 10_000)));
							}
						}
						default -> {
							send(TvPayloads.Control.STOP, "", 0);
							link.setValue("");
						}
					}
					return true;
				}
				bx += widths[k] + 5;
			}
			if (dur > 0 && TvGui.in(mx, my, x0 + 40, y0 + 112, x1 - 42, y0 + 124)) {
				drag = 1;
				dragAt = frac(mx, x0 + 42, x1 - 44);
				return true;
			}
		}
		int vx = x0 + 10 + 44 + 54 + 44 + 40 + 20 + 8;
		if (control && TvGui.in(mx, my, vx + 20, y0 + 132, x1 - 34, y0 + 144)) {
			drag = 2;
			dragAt = frac(mx, vx + 22, x1 - 36);
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	private static float frac(double mx, int x0, int x1) {
		return (float) Math.max(0, Math.min(1, (mx - x0) / (x1 - x0)));
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (drag == 1) {
			dragAt = frac(event.x(), x0 + 42, x1 - 44);
			return true;
		}
		if (drag == 2) {
			int vx = x0 + 10 + 44 + 54 + 44 + 40 + 20 + 8;
			dragAt = frac(event.x(), vx + 22, x1 - 36);
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (drag != 0) {
			TvPlayers.Slot slot = TvPlayers.slot(pos);
			if (drag == 1 && duration(slot) > 0) {
				seek(seekTo(slot, (long) (dragAt * duration(slot))));
			} else if (drag == 2) {
				send(TvPayloads.Control.VOLUME, "", Math.round(dragAt * 100));
			}
			drag = 0;
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		// Enter in the link field plays it.
		if (link.isFocused() && control && (event.input() == com.mojang.blaze3d.platform.InputConstants.KEY_RETURN || event.input() == com.mojang.blaze3d.platform.InputConstants.KEY_NUMPADENTER)) {
			playLink();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void tick() {
		super.tick();
		TvBlockEntity tv = tv();
		Minecraft mc = Minecraft.getInstance();
		// The screen made bigger or smaller, its bottom-left block moved: the window follows it.
		if (tv != null && !tv.isController() && mc.level != null && tv.getBlockState().getBlock() instanceof FlatTvBlock) {
			BlockPos c = FlatTvBlock.controllerOf(mc.level, pos, tv.getBlockState());
			if (!c.equals(pos) && mc.level.getBlockEntity(c) instanceof TvBlockEntity moved && moved.isController()) {
				pos = c.immutable();
				tv = moved;
			}
		}
		// Closed when the screen goes, or the player walks away from it (from its nearest part: a big screen's corner can be far off).
		if (tv == null || mc.player == null || tv.screenDistance(mc.player.getEyePosition()) > FlatTvBlock.FAR_REACH + FlatTvBlock.WINDOW_SLACK) {
			onClose();
		}
	}
}
