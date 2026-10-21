package io.github.jimbozoomer.jugcraft.client;

import static io.github.jimbozoomer.jugcraft.client.ExchangeScreen.*;

import io.github.jimbozoomer.jugcraft.finance.FinanceBook;
import io.github.jimbozoomer.jugcraft.finance.ScratchPayload;
import io.github.jimbozoomer.jugcraft.finance.ShowGames;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * A scratch card in your hands: a printed ticket with a shimmering foil edge, its prize list down the side, and nine
 * panels under a silver coating. Hold the left button and rub: the coating comes off in fine flakes wherever the coin
 * goes. Three of the same amount win it. The prize was drawn and paid when the card was used; this only reveals it.
 */
public class ScratchScreen extends Screen {
	/** The coating is kept in 2x2 pixel flakes. */
	private static final int FLAKE = 2;
	private static final int PANEL_W = 44;
	private static final int PANEL_H = 34;
	private static final int COLS = PANEL_W / FLAKE;
	private static final int ROWS = PANEL_H / FLAKE;
	private static final int BRUSH = 6;
	private static final int W = 250;
	private static final int H = 196;
	private static final int[] FACES = {0xFFB86A3A, 0xFF9EA2AC, 0xFFE2B84E};
	private static final int[] DARKS = {0xFF5A2E14, 0xFF4A4C54, 0xFF7A5618};
	private static final int[] LIGHTS = {0xFFE09A62, 0xFFD4D8E0, 0xFFFFE08A};
	private static final String[] QUESTION = {".XXX.", "X...X", "....X", "...X.", "..X..", "..X..", ".....", "..X.."};

	private record Fleck(double x, double y, double vx, double vy, int born) {
	}

	private final ScratchPayload card;
	private final ShowGames.ScratchCard kind;
	private final boolean[][] scratched = new boolean[9][COLS * ROWS];
	private final List<Fleck> flecks = new ArrayList<>();
	private boolean scratching;
	private boolean revealed;
	private int revealedAt;
	private int left;
	private int top;
	private int ticks;
	private float partial;

	public ScratchScreen(ScratchPayload card) {
		super(Component.literal("Scratch Card"));
		this.card = card;
		this.kind = ShowGames.ScratchCard.values()[Math.max(0, Math.min(ShowGames.ScratchCard.values().length - 1, card.card()))];
	}

	static void receive(ScratchPayload payload) {
		Minecraft.getInstance().gui.setScreen(new ScratchScreen(payload));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		ticks++;
		flecks.removeIf(f -> ticks - f.born() > 24);
		if (!revealed && allScratched()) {
			// Most of every panel is off: brush away what is left and show the result.
			for (boolean[] panel : scratched) {
				java.util.Arrays.fill(panel, true);
			}
			revealed = true;
			revealedAt = ticks;
		}
	}

	@Override
	protected void init() {
		left = (width - W) / 2;
		top = (height - H - 24) / 2;
		addRenderableWidget(new Plate(left + 20, top + H + 6, 96, 16, "Reveal all", Plate.Kind.WOOD, b -> {
			for (boolean[] panel : scratched) {
				java.util.Arrays.fill(panel, true);
			}
		}));
		addRenderableWidget(new Plate(left + W - 116, top + H + 6, 96, 16, "Done", Plate.Kind.BRASS, b -> onClose()));
	}

	// ------------------------------------------------------------------ layout

	private int gridX() {
		return left + 14;
	}

	private int gridY() {
		return top + 42;
	}

	private int[] panel(int i) {
		int x = gridX() + (i % 3) * (PANEL_W + 6);
		int y = gridY() + (i / 3) * (PANEL_H + 6);
		return new int[] {x, y, x + PANEL_W, y + PANEL_H};
	}

	private int prizesX() {
		return gridX() + 3 * PANEL_W + 2 * 6 + 10;
	}

	// ------------------------------------------------------------------ scratching

	private void scratchAt(double mx, double my) {
		for (int i = 0; i < 9; i++) {
			int[] r = panel(i);
			if (mx < r[0] - BRUSH || mx >= r[2] + BRUSH || my < r[1] - BRUSH || my >= r[3] + BRUSH) {
				continue;
			}
			for (int c = 0; c < COLS * ROWS; c++) {
				if (scratched[i][c]) {
					continue;
				}
				double cx = r[0] + (c % COLS + 0.5) * FLAKE;
				double cy = r[1] + (c / COLS + 0.5) * FLAKE;
				double d = Math.hypot(cx - mx, cy - my);
				// A ragged edge: the brush is a little rough, like a real coin.
				if (d < BRUSH - ((c * 7 + i * 3) % 3) * 0.6) {
					scratched[i][c] = true;
					if (flecks.size() < 160 && (c + ticks) % 3 == 0) {
						double a = ((c * 37 + ticks * 11) % 360) * Math.PI / 180;
						flecks.add(new Fleck(cx, cy, Math.cos(a) * 1.2, -Math.abs(Math.sin(a)) * 1.5, ticks));
					}
				}
			}
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && onGrid(event.x(), event.y())) {
			scratching = true;
			scratchAt(event.x(), event.y());
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (scratching) {
			// Fill in between the last point and this one, so a fast stroke leaves no gaps.
			int steps = (int) Math.ceil(Math.hypot(dragX, dragY) / 2) + 1;
			for (int s = steps; s >= 0; s--) {
				scratchAt(event.x() - dragX * s / steps, event.y() - dragY * s / steps);
			}
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		scratching = false;
		return super.mouseReleased(event);
	}

	private boolean onGrid(double x, double y) {
		int[] first = panel(0);
		int[] last = panel(8);
		return x >= first[0] - BRUSH && x < last[2] + BRUSH && y >= first[1] - BRUSH && y < last[3] + BRUSH;
	}

	private int cleared(int i) {
		int done = 0;
		for (boolean b : scratched[i]) {
			done += b ? 1 : 0;
		}
		return done;
	}

	private boolean allScratched() {
		for (int i = 0; i < 9; i++) {
			if (cleared(i) < COLS * ROWS * 0.7) {
				return false;
			}
		}
		return true;
	}

	// ------------------------------------------------------------------ drawing

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractBackground(g, mouseX, mouseY, delta);
		int k = kind.ordinal();
		// Drop shadow, then the card.
		g.fill(left + 4, top + 4, left + W + 4, top + H + 4, 0x66000000);
		g.fill(left, top, left + W, top + H, DARKS[k]);
		// A foil edge: stripes with a glint that runs along it.
		for (int i = 0; i < W; i += 2) {
			boolean glint = Math.abs(((i + ticks * 3) % (W + 60)) - 30) < 8;
			int c = glint ? 0xFFFFFFFF : (i / 2) % 2 == 0 ? LIGHTS[k] : FACES[k];
			g.fill(left + i, top, left + i + 2, top + 3, c);
			g.fill(left + W - i - 2, top + H - 3, left + W - i, top + H, c);
		}
		for (int j = 0; j < H; j += 2) {
			int c = (j / 2) % 2 == 0 ? LIGHTS[k] : FACES[k];
			g.fill(left, top + j, left + 3, top + j + 2, c);
			g.fill(left + W - 3, top + j, left + W, top + j + 2, c);
		}
		g.fill(left + 4, top + 4, left + W - 4, top + H - 4, FACES[k]);
		// The printed band across the top.
		g.fill(left + 4, top + 4, left + W - 4, top + 30, DARKS[k]);
		for (int x = left + 6; x < left + W - 6; x += 6) {
			g.fill(x, top + 27, x + 3, top + 28, LIGHTS[k]);
		}
		// Sunburst rays printed faintly behind the grid.
		int cx = gridX() + 3 * PANEL_W / 2 + 6;
		int cy = gridY() + 3 * PANEL_H / 2 + 6;
		for (int ray = 0; ray < 16; ray += 2) {
			double a = ray * Math.PI / 8;
			for (int d = 20; d < 110; d += 2) {
				int x = cx + (int) (Math.cos(a) * d);
				int y = cy + (int) (Math.sin(a) * d * 0.8);
				if (x > left + 5 && x < prizesX() - 6 && y > top + 31 && y < top + H - 6) {
					g.fill(x, y, x + 2, y + 2, LIGHTS[k]);
				}
			}
		}
		// The prize list's panel.
		g.fill(prizesX() - 2, gridY() - 2, left + W - 10, gridY() + 3 * PANEL_H + 14, DARKS[k]);
		g.fill(prizesX() - 1, gridY() - 1, left + W - 11, gridY() + 3 * PANEL_H + 13, 0xFFF6EEDA);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		partial = delta;
		super.extractRenderState(g, mouseX, mouseY, delta);
		int k = kind.ordinal();
		String title = kind.title.toUpperCase(java.util.Locale.ROOT);
		g.text(font, title, left + W / 2 - font.width(title) / 2, top + 9, LIGHTS[k], true);
		g.text(font, "Match 3 to win!", left + W / 2 - font.width("Match 3 to win!") / 2, top + 19, 0xFFFFF6E0, false);
		boolean flash = revealed && card.prize() > 0 && ((ticks - revealedAt) / 5) % 2 == 0;
		for (int i = 0; i < 9; i++) {
			int[] r = panel(i);
			long amount = i < card.panels().size() ? card.panels().get(i) : 0;
			boolean winner = revealed && card.prize() > 0 && amount == card.prize();
			g.fill(r[0] - 1, r[1] - 1, r[2] + 1, r[3] + 1, winner && flash ? 0xFFFFFFFF : DARKS[k]);
			g.fill(r[0], r[1], r[2], r[3], winner ? 0xFFFFF0A0 : 0xFFF6EEDA);
			coin(g, (r[0] + r[2]) / 2, r[1] + 9, winner);
			String text = FinanceBook.amount(amount);
			g.text(font, text, (r[0] + r[2]) / 2 - font.width(text) / 2 + 1, r[1] + 19, winner ? 0xFFC0362C : 0xFF2A1A12, false);
			coating(g, i, r);
		}
		// Flecks of silver kicked up by the coin.
		for (Fleck f : flecks) {
			double t = ticks - f.born() + partial;
			int x = (int) (f.x() + f.vx() * t);
			int y = (int) (f.y() + f.vy() * t + 0.12 * t * t);
			g.fill(x, y, x + 1, y + 1, (int) t % 2 == 0 ? 0xFFE6E9EE : 0xFF9EA4AE);
		}
		// The prize list.
		int px = prizesX() + 3;
		int py = gridY() + 3;
		g.text(font, "PRIZES", px, py, DARKS[k], false);
		long[] prizes = kind.prizes;
		List<Long> values = new ArrayList<>();
		for (int i = 0; i < prizes.length; i += 2) {
			values.add(prizes[i]);
		}
		values.sort(java.util.Comparator.reverseOrder());
		py += 12;
		for (long v : values) {
			boolean won = revealed && card.prize() == v;
			String line = "3 x " + FinanceBook.amount(v);
			g.text(font, line, px, py, won ? 0xFFC0362C : 0xFF3A2A1A, false);
			py += 10;
		}
		String price = "Price " + FinanceBook.amount(kind.price) + (kind.price == 1 ? " Jug" : " Jugs");
		g.text(font, price, px, gridY() + 3 * PANEL_H + 2, 0xFF6A5A4A, false);
		// The result, under the grid and clear of the panels.
		int ry = gridY() + 3 * (PANEL_H + 6) + 2;
		String result;
		int colour;
		if (!revealed) {
			result = scratching ? "Scratching..." : "Hold left click and rub the silver.";
			colour = 0xFF2A1A12;
		} else if (card.prize() > 0) {
			result = "YOU WON " + FinanceBook.amount(card.prize()) + (card.prize() == 1 ? " JUG!" : " JUGS!");
			colour = flash ? 0xFF1E7A3A : 0xFFC0362C;
		} else {
			result = "No win this time.";
			colour = 0xFF2A1A12;
		}
		g.text(font, result, gridX() + (3 * PANEL_W + 12) / 2 - font.width(result) / 2, ry, colour, false);
		// The coin in your fingers, following the mouse over the panels.
		if (!revealed && onGrid(mouseX, mouseY)) {
			int tilt = scratching ? (ticks % 4 < 2 ? 1 : -1) : 0;
			coinCursor(g, mouseX + 3, mouseY + 3 + tilt);
		}
	}

	/** The silver over a panel, flake by flake, with a printed "?" and a sheen that drifts across. */
	private void coating(GuiGraphicsExtractor g, int i, int[] r) {
		for (int c = 0; c < COLS * ROWS; c++) {
			if (scratched[i][c]) {
				continue;
			}
			int col = c % COLS;
			int row = c / COLS;
			int x = r[0] + col * FLAKE;
			int y = r[1] + row * FLAKE;
			int noise = (col * 73 + row * 151 + i * 17) % 7;
			int shade = noise < 2 ? 0xFFB4B8C0 : noise < 5 ? 0xFFC6CAD2 : 0xFFD6D9DF;
			if ((col + row + ticks / 6) % 23 == 0) {
				shade = 0xFFF2F4F8;
			}
			int qx = col - (COLS / 2 - 3);
			int qy = row - (ROWS / 2 - 4);
			if (qx >= 0 && qx < 5 && qy >= 0 && qy < 8 && QUESTION[qy].charAt(qx) == 'X') {
				shade = 0xFF8A8E98;
			}
			g.fill(x, y, x + FLAKE, y + FLAKE, shade);
		}
	}

	private static void coin(GuiGraphicsExtractor g, int cx, int cy, boolean lit) {
		int rim = lit ? 0xFFC88A1A : 0xFFB89A5A;
		int face = lit ? 0xFFFFD34A : 0xFFE6CF8E;
		g.fill(cx - 3, cy - 4, cx + 3, cy + 4, rim);
		g.fill(cx - 4, cy - 3, cx + 4, cy + 3, rim);
		g.fill(cx - 2, cy - 3, cx + 2, cy + 3, face);
		g.fill(cx - 3, cy - 2, cx + 3, cy + 2, face);
		g.fill(cx - 1, cy - 2, cx + 1, cy + 2, rim);
	}

	private static void coinCursor(GuiGraphicsExtractor g, int x, int y) {
		g.fill(x + 1, y, x + 9, y + 10, 0xFF7A5A1A);
		g.fill(x, y + 1, x + 10, y + 9, 0xFF7A5A1A);
		g.fill(x + 1, y + 1, x + 9, y + 9, 0xFFE8C463);
		g.fill(x + 2, y + 2, x + 5, y + 4, 0xFFFFF0A0);
		g.fill(x + 4, y + 4, x + 6, y + 7, 0xFFB8913F);
	}
}
