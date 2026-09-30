package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.prospecting.OreSurvey;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The Geo-Resonance Prospector's screen: a brass instrument with an amber CRT. Readings warm up like
 * valve tubes when it opens, a scan line sweeps the glass, and a needle gauge on the nameplate shows
 * the overall resonance. Every reading is deliberately approximate (see {@link OreSurvey}).
 */
public class ProspectorScreen extends Screen {
	private static final int WIDTH = 268;
	private static final int HEIGHT = 196;
	private static final int ROWS = 6;
	private static final int COLUMN_WIDTH = 118;

	private static final int BRASS_DARK = 0xFF5E4418;
	private static final int BRASS = 0xFFB48A3C;
	private static final int BRASS_LIGHT = 0xFFE2C27A;
	private static final int RIVET = 0xFF7A5A22;
	private static final int GLASS = 0xFF0C0A06;
	private static final int AMBER = 0xFFFFB340;
	private static final int AMBER_DIM = 0xFF5C3A12;
	/** Secondary text: dimmer than the readings but still legible on the glass. */
	private static final int AMBER_MID = 0xFFC8862C;
	private static final int AMBER_GLOW = 0x33FF9A20;
	private static final int SCANLINE = 0x44000000;
	private static final String[] DEPTHS = {"SHALLOW", "MIDDLE", "DEEP"};

	private final List<OreSurvey.Reading> readings;
	private final long openedAt = System.currentTimeMillis();
	private int left;
	private int top;

	public ProspectorScreen(List<OreSurvey.Reading> readings) {
		super(Component.translatable("item.jugcraft.prospector"));
		this.readings = readings;
	}

	@Override
	protected void init() {
		left = (width - WIDTH) / 2;
		top = (height - HEIGHT) / 2;
	}

	private float seconds() {
		return (System.currentTimeMillis() - openedAt) / 1000.0F;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		// Brass body with a darker rim and rivets.
		graphics.fill(left - 2, top - 2, left + WIDTH + 2, top + HEIGHT + 2, BRASS_DARK);
		graphics.fill(left, top, left + WIDTH, top + HEIGHT, BRASS);
		graphics.fill(left, top, left + WIDTH, top + 1, BRASS_LIGHT);
		graphics.fill(left, top, left + 1, top + HEIGHT, BRASS_LIGHT);
		for (int x : new int[] {left + 5, left + WIDTH / 2, left + WIDTH - 7}) {
			rivet(graphics, x, top + 4);
			rivet(graphics, x, top + HEIGHT - 6);
		}
		for (int y : new int[] {top + HEIGHT / 3, top + 2 * HEIGHT / 3}) {
			rivet(graphics, left + 4, y);
			rivet(graphics, left + WIDTH - 6, y);
		}
		// Nameplate.
		graphics.fill(left + 10, top + 8, left + WIDTH - 52, top + 22, BRASS_DARK);
		graphics.fill(left + 11, top + 9, left + WIDTH - 53, top + 21, BRASS_LIGHT);
		graphics.text(font, "GEO-RESONANCE PROSPECTOR", left + 16, top + 11, BRASS_DARK, false);
		// The CRT: bezel, glass, glow.
		int sx = left + 10;
		int sy = top + 28;
		graphics.fill(sx - 3, sy - 3, sx + WIDTH - 20 + 3, top + HEIGHT - 10 + 3, BRASS_DARK);
		graphics.fill(sx, sy, sx + WIDTH - 20, top + HEIGHT - 10, GLASS);
		graphics.fill(sx + 2, sy + 2, sx + WIDTH - 22, top + HEIGHT - 12, AMBER_GLOW);
	}

	private static void rivet(GuiGraphicsExtractor graphics, int x, int y) {
		graphics.fill(x, y, x + 3, y + 3, RIVET);
		graphics.fill(x, y, x + 1, y + 1, BRASS_LIGHT);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		float t = seconds();
		int sx = left + 10;
		int sy = top + 28;
		int sw = WIDTH - 20;
		int sh = HEIGHT - 38;
		// Phosphor flicker: text brightness wobbles very slightly.
		int text = t < 0.4F ? AMBER_DIM : AMBER;

		graphics.text(font, "SURVEY 3x3 CHUNKS", sx + 6, sy + 5, text, false);
		String count = readings.size() + " SIGNALS";
		graphics.text(font, count, sx + sw - 6 - font.width(count), sy + 5, text, false);
		graphics.fill(sx + 4, sy + 15, sx + sw - 4, sy + 16, AMBER_DIM);

		if (readings.isEmpty()) {
			if ((int) (t * 2) % 2 == 0) {
				String none = "NO RESONANCE DETECTED";
				graphics.text(font, none, sx + (sw - font.width(none)) / 2, sy + sh / 2 - 4, AMBER, false);
			}
		}
		int shown = Math.min(readings.size(), ROWS * 2);
		for (int i = 0; i < shown; i++) {
			OreSurvey.Reading reading = readings.get(i);
			int x = sx + 6 + (i / ROWS) * COLUMN_WIDTH;
			int y = sy + 20 + (i % ROWS) * 20;
			ItemStack icon = new ItemStack(BuiltInRegistries.ITEM.getValue(reading.icon()));
			graphics.item(icon, x, y);
			String name = icon.getHoverName().getString().replace(" Ore", "").toUpperCase(Locale.ROOT);
			graphics.text(font, font.plainSubstrByWidth(name, COLUMN_WIDTH - 24), x + 19, y, text, false);
			// Valve-tube bars warm up one after another as the screen opens.
			float warm = Math.max(0.0F, t * 4.0F - i * 0.35F);
			for (int bar = 0; bar < 5; bar++) {
				boolean lit = bar < reading.signal() && warm > bar;
				int bx = x + 19 + bar * 11;
				graphics.fill(bx, y + 10, bx + 9, y + 15, lit ? AMBER : AMBER_DIM);
				if (lit) {
					graphics.fill(bx + 1, y + 10, bx + 8, y + 11, 0xFFFFE0A0);
				}
			}
			graphics.text(font, DEPTHS[Math.max(0, Math.min(2, reading.depth()))], x + 76, y + 9, AMBER_MID, false);
			if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
				graphics.setTooltipForNextFrame(font, icon, mouseX, mouseY);
			}
		}
		if (readings.size() > shown) {
			String more = "+" + (readings.size() - shown) + " FAINTER";
			graphics.text(font, more, sx + sw - 6 - font.width(more), sy + sh - 11, AMBER_MID, false);
		}
		graphics.text(font, "READINGS ARE APPROXIMATE", sx + 6, sy + sh - 11, AMBER_MID, false);

		// Scanlines and a sweeping scan beam.
		for (int y = sy; y < sy + sh; y += 2) {
			graphics.fill(sx, y, sx + sw, y + 1, SCANLINE);
		}
		int beam = sy + (int) ((t * 60) % sh);
		graphics.fill(sx, beam, sx + sw, beam + 1, 0x55FFC060);
		graphics.fill(sx, beam + 1, sx + sw, beam + 3, 0x22FFA030);

		gauge(graphics, left + WIDTH - 28, top + 17, 13, t);
	}

	/** A round brass gauge whose needle swings to the total resonance (and settles as the screen warms up). */
	private void gauge(GuiGraphicsExtractor graphics, int cx, int cy, int r, float t) {
		disc(graphics, cx, cy, r + 2, BRASS_DARK);
		disc(graphics, cx, cy, r, 0xFFF1E6CB);
		for (int tick = 0; tick <= 10; tick++) {
			double angle = Math.toRadians(-225 + tick * 27);
			int tx = cx + (int) Math.round(Math.cos(angle) * (r - 2));
			int ty = cy + (int) Math.round(Math.sin(angle) * (r - 2));
			graphics.fill(tx, ty, tx + 1, ty + 1, tick >= 8 ? 0xFFB02020 : 0xFF3A2A1A);
		}
		int total = 0;
		for (OreSurvey.Reading reading : readings) {
			total += reading.signal();
		}
		float target = Math.min(1.0F, total / 30.0F);
		float settle = Math.min(1.0F, t * 1.5F);
		float wobble = (float) Math.sin(t * 9) * 0.03F * (1 - settle);
		double angle = Math.toRadians(-225 + (target * settle + wobble) * 270);
		for (int step = 0; step < r - 2; step++) {
			int nx = cx + (int) Math.round(Math.cos(angle) * step);
			int ny = cy + (int) Math.round(Math.sin(angle) * step);
			graphics.fill(nx, ny, nx + 1, ny + 1, 0xFF8A1A10);
		}
		graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, BRASS_DARK);
	}

	private static void disc(GuiGraphicsExtractor graphics, int cx, int cy, int r, int color) {
		for (int dy = -r; dy <= r; dy++) {
			int half = (int) Math.sqrt(r * r - dy * dy);
			graphics.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
		}
	}
}
