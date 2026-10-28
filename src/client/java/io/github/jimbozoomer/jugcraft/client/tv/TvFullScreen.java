package io.github.jimbozoomer.jugcraft.client.tv;

import io.github.jimbozoomer.jugcraft.tv.FlatTvBlock;
import io.github.jimbozoomer.jugcraft.tv.TvBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * A flat screen's video over the whole game window (Justin, 27 October 2026: "shift right clicks on the flat screen,
 * even from a distance ... up to 25 blocks away, it'll just put the stream or video on full screen"): sneak and
 * right-click a screen that's on with an empty hand ({@link TvFarUse#tryFullscreen}). It shows the picture this game
 * is already playing for that screen (in step with everyone, as on the screen), letterboxed on black, with its sound
 * heard as if close by wherever the player stands. The title, the time and how far along come up for a few seconds when
 * the mouse moves. Escape or a right-click leaves it; it closes by itself when the screen is turned off or goes.
 */
public final class TvFullScreen extends Screen {
	/** How far off a screen can be put full screen (blocks). */
	public static final double REACH = 25;
	/** How long the title and time stay up after the mouse last moved (ms). */
	private static final long SHOW_MS = 3000;
	private BlockPos pos;
	private long shownSince = System.currentTimeMillis();
	private double mouseX = -1, mouseY = -1;

	public TvFullScreen(BlockPos controller) {
		super(Component.literal("SCREEN"));
		pos = controller.immutable();
	}

	private @Nullable TvBlockEntity tv() {
		return minecraft != null && minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof TvBlockEntity tv ? tv : null;
	}

	@Override
	protected void init() {
		TvPlayers.fullscreen(pos);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		TvBlockEntity tv = tv();
		Minecraft mc = Minecraft.getInstance();
		// The screen made bigger or smaller, its bottom-left block moved: follow it.
		if (tv != null && !tv.isController() && mc.level != null && tv.getBlockState().getBlock() instanceof FlatTvBlock) {
			BlockPos c = FlatTvBlock.controllerOf(mc.level, pos, tv.getBlockState());
			if (!c.equals(pos) && mc.level.getBlockEntity(c) instanceof TvBlockEntity moved && moved.isController()) {
				pos = c.immutable();
				tv = moved;
			}
		}
		if (tv == null || !tv.isController() || tv.url().isEmpty()) {
			onClose();
			return;
		}
		TvPlayers.fullscreen(pos);
	}

	@Override
	public void removed() {
		TvPlayers.fullscreen(null);
		super.removed();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		g.fill(0, 0, width, height, 0xFF000000);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
		super.extractRenderState(g, mx, my, delta);
		if (mx != mouseX || my != mouseY) {
			if (mouseX >= 0) {
				shownSince = System.currentTimeMillis();
			}
			mouseX = mx;
			mouseY = my;
		}
		TvBlockEntity tv = tv();
		TvPlayers.Slot slot = TvPlayers.slot(pos);
		TvPlayer p = slot == null ? null : slot.player;
		// The picture, as big as the window takes it in its own shape.
		String status;
		if (p != null && p.hasPicture()) {
			float a = p.aspect();
			int w = width, h = Math.round(width / a);
			if (h > height) {
				h = height;
				w = Math.round(height * a);
			}
			int x = (width - w) / 2, y = (height - h) / 2;
			g.blit(p.textureId, x, y, x + w, y + h, 0, 1, 0, 1);
			status = slot.ended ? "ENDED" : p.status();
		} else {
			status = p != null ? p.state.isEmpty() ? "LOADING" : p.state : slot != null && !slot.state.isEmpty() ? slot.state : "LOADING";
		}
		if (!status.isEmpty()) {
			int tw = font.width(status);
			int cy = height / 2;
			g.fill(width / 2 - tw / 2 - 10, cy - 9, width / 2 + tw / 2 + 10, cy + 9, 0xC0000000);
			g.centeredText(font, status, width / 2, cy - 4, status.startsWith("STREAM STOPPED") || status.startsWith("CAN'T") ? TvGui.RED : TvGui.WHITE);
		}
		// The title and time, for a few seconds after the mouse moves (and when it opens).
		long since = System.currentTimeMillis() - shownSince;
		if (since < SHOW_MS && tv != null) {
			int alpha = since < SHOW_MS - 500 ? 0xFF : (int) (0xFF * (SHOW_MS - since) / 500);
			int band = alpha * 0xB0 / 0xFF << 24;
			g.fill(0, height - 34, width, height, band);
			String title = p != null && !p.title.isEmpty() ? p.title : slot != null && !slot.entryTitle.isEmpty() ? slot.entryTitle : "";
			g.text(font, TvGui.fit(font, title, width - 140), 10, height - 28, fade(TvGui.WHITE, alpha), false);
			boolean live = p != null && p.live;
			long at = slot == null ? tv.position() : slot.offset, dur = slot == null ? 0 : slot.durationMs;
			String time = live ? "LIVE" : TvPlayer.clock(dur > 0 ? Math.min(at, dur) : at) + (dur > 0 ? "  /  " + TvPlayer.clock(dur) : "");
			g.text(font, time, width - 10 - font.width(time), height - 28, fade(live ? TvGui.RED : TvGui.CYAN, alpha), false);
			if (!live && dur > 0) {
				int bx0 = 10, bx1 = width - 10, by = height - 13;
				g.fill(bx0, by, bx1, by + 3, fade(0xFF223A4E, alpha));
				int fill = bx0 + (int) ((bx1 - bx0) * Math.min(1, at / (double) dur));
				g.fill(bx0, by, fill, by + 3, fade(TvGui.CYAN, alpha));
			}
			String leave = "ESC TO LEAVE";
			g.text(font, leave, width - 10 - font.width(leave), 8, fade(TvGui.GREY, alpha), false);
		}
	}

	private static int fade(int argb, int alpha) {
		return (alpha * (argb >>> 24) / 0xFF) << 24 | argb & 0xFFFFFF;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 1) {
			onClose();
			return true;
		}
		shownSince = System.currentTimeMillis();
		return true;
	}
}
