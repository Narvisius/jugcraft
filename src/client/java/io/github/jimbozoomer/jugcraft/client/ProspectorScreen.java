package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.prospecting.OreSurvey;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The Geo-Resonance Prospector's screen, in the workshop look (docs/features/machine-screens.md): a brass instrument
 * with an orange CRT. The title strip has the status lamp (lit when it hears anything) and the number of signals; the
 * glass lists the readings strongest first, each with five valve-tube bars that warm up one after another as it opens
 * and its depth; a scan line sweeps the glass; and under it, a help strip and a needle gauge showing the overall
 * resonance. Every reading is deliberately approximate (see {@link OreSurvey}).
 */
public class ProspectorScreen extends Screen implements ScreenKit.Fitted {
	private static final int WIDTH = 268;
	private static final int HEIGHT = 196;
	private static final int ROWS = 6;
	private static final int COLUMN_WIDTH = 124;
	/** The glass: from the title strip down to the help strip. */
	private static final int GLASS_Y = 15;
	private static final int GLASS_HEIGHT = 150;
	private static final int SCANLINE = 0x44000000;
	private final MachineScreenThemes.Theme theme = MachineScreenThemes.screen("prospector");

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

	private static String t(String key, Object... args) {
		return Component.translatable("screen.jugcraft.prospector." + key, args).getString().toUpperCase(Locale.ROOT);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		ScreenKit.body(graphics, theme, left, top, WIDTH, HEIGHT);
		ScreenKit.title(graphics, font, theme, left, top, title, readings.isEmpty() ? theme.dim() : theme.ok(), WIDTH - 80);
		ScreenKit.chip(graphics, font, theme, left + WIDTH - 4, top + 1, t("signals", readings.size()), theme.text());
		ScreenKit.pane(graphics, theme, left + 6, top + GLASS_Y, WIDTH - 12, GLASS_HEIGHT);
		ScreenKit.help(graphics, font, theme, left + 6, top + GLASS_Y + GLASS_HEIGHT + 4, WIDTH - 44, HEIGHT - GLASS_Y - GLASS_HEIGHT - 10,
				Component.translatable("screen.jugcraft.prospector.help"));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		float t = seconds();
		int sx = left + 6;
		int sy = top + GLASS_Y;
		int sw = WIDTH - 12;
		int sh = GLASS_HEIGHT;
		// The phosphor warms up: dim for the first moment.
		int text = t < 0.4F ? theme.dim() : theme.text();

		graphics.text(font, t("survey"), sx + 6, sy + 5, text, false);
		// Under the heading, the workshop glass's band (a plain rule if the look has none).
		int band = theme.frame().band();
		graphics.fill(sx + 4, sy + 15, sx + sw - 4, sy + 17, band != 0 ? band : (theme.dim() & 0x00FFFFFF) | 0xA0000000);

		if (readings.isEmpty() && (int) (t * 2) % 2 == 0) {
			String none = t("none");
			graphics.text(font, none, sx + (sw - font.width(none)) / 2, sy + sh / 2 - 4, theme.text(), false);
		}
		int shown = Math.min(readings.size(), ROWS * 2);
		for (int i = 0; i < shown; i++) {
			OreSurvey.Reading reading = readings.get(i);
			int x = sx + 6 + (i / ROWS) * COLUMN_WIDTH;
			int y = sy + 20 + (i % ROWS) * 20;
			ItemStack icon = new ItemStack(BuiltInRegistries.ITEM.getValue(reading.icon()));
			graphics.item(icon, x, y);
			String name = (reading.label().isEmpty() ? icon.getHoverName().getString().replace(" Ore", "")
					: Component.translatable(reading.label()).getString()).toUpperCase(Locale.ROOT);
			graphics.text(font, ScreenKit.fit(font, name, COLUMN_WIDTH - 26), x + 19, y, text, false);
			// Valve-tube bars warm up one after another as the screen opens.
			float warm = Math.max(0.0F, t * 4.0F - i * 0.35F);
			for (int bar = 0; bar < 5; bar++) {
				boolean lit = bar < reading.signal() && warm > bar;
				int bx = x + 19 + bar * 9;
				graphics.fill(bx, y + 10, bx + 7, y + 15, lit ? theme.progress() : (theme.dim() & 0x00FFFFFF) | 0x60000000);
				if (lit) {
					graphics.fill(bx + 1, y + 10, bx + 6, y + 11, 0xFFFFE0A0);
				}
			}
			// Its depth, as a tag: brighter the nearer the surface.
			String depth = t("depth." + Math.max(0, Math.min(2, reading.depth())));
			int tag = reading.depth() <= 0 ? theme.text() : reading.depth() == 1 ? theme.progress() : theme.dim();
			graphics.text(font, depth, x + 66, y + 9, tag, false);
			if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
				graphics.setTooltipForNextFrame(font, icon, mouseX, mouseY);
			}
		}
		if (readings.size() > shown) {
			String more = t("fainter", readings.size() - shown);
			graphics.text(font, more, sx + sw - 6 - font.width(more), sy + sh - 11, theme.dim(), false);
		}

		// Scanlines and a sweeping scan beam over the glass.
		for (int y = sy + 1; y < sy + sh - 1; y += 2) {
			graphics.fill(sx + 1, y, sx + sw - 1, y + 1, SCANLINE);
		}
		int beam = sy + 1 + (int) ((t * 60) % (sh - 3));
		graphics.fill(sx + 1, beam, sx + sw - 1, beam + 1, 0x55FFC060);
		graphics.fill(sx + 1, beam + 1, sx + sw - 1, Math.min(beam + 3, sy + sh - 1), 0x22FFA030);

		gauge(graphics, left + WIDTH - 19, top + HEIGHT - 16, 11, t);
	}

	/** A round brass gauge whose needle swings to the total resonance (and settles as the screen warms up). */
	private void gauge(GuiGraphicsExtractor graphics, int cx, int cy, int r, float t) {
		int[] c = theme.frame().casing();
		disc(graphics, cx, cy, r + 2, c[0]);
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
		graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, c[0]);
	}

	private static void disc(GuiGraphicsExtractor graphics, int cx, int cy, int r, int color) {
		for (int dy = -r; dy <= r; dy++) {
			int half = (int) Math.sqrt(r * r - dy * dy);
			graphics.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
		}
	}

	@Override
	public ScreenRectangle panelArea() {
		return new ScreenRectangle(left, top, WIDTH, HEIGHT);
	}
}
