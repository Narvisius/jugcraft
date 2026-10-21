package io.github.jimbozoomer.jugcraft.client;

import static io.github.jimbozoomer.jugcraft.client.ExchangeScreen.*;

import io.github.jimbozoomer.jugcraft.finance.CasinoPayload;
import io.github.jimbozoomer.jugcraft.finance.FinanceActionPayload.Action;
import io.github.jimbozoomer.jugcraft.finance.FinanceBook;
import io.github.jimbozoomer.jugcraft.finance.Roulette;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * The American roulette table: the wheel on the left, turning, with the ball spiralling down into the pocket the
 * server drew; the layout on the right. Click a number for a straight bet, a line between two numbers for a split, a
 * corner for four, the strip under a column for its street (or a six line between two), the zeros' strip for the
 * five-number bet, and the outside boxes for dozens, columns and even money. The picked chip is what goes down.
 */
public class RouletteScreen extends CasinoScreen {
	private static final int CELL_W = 22;
	private static final int CELL_H = 20;
	private static final int STREET = 6;

	private record BetView(long numbers, long amount, boolean mine) {
	}

	private int result = -1;
	private long spins;
	private final List<Integer> history = new ArrayList<>();
	private final List<BetView> bets = new ArrayList<>();
	private double wheelAngle;
	private long hoverNumbers;

	public RouletteScreen(CasinoPayload data) {
		super(data);
		parse();
	}

	private void parse() {
		CasinoPayload.Reader r = new CasinoPayload.Reader(data.data());
		result = r.nextInt();
		spins = r.next();
		int h = r.nextInt();
		history.clear();
		for (int i = 0; i < h; i++) {
			history.add(r.nextInt());
		}
		int n = r.nextInt();
		bets.clear();
		for (int i = 0; i < n; i++) {
			bets.add(new BetView(r.next(), r.next(), r.nextBool()));
		}
	}

	private Roulette.Phase phase() {
		return Roulette.Phase.values()[Math.max(0, Math.min(Roulette.Phase.values().length - 1, data.phase()))];
	}

	private String shape = "";

	@Override
	protected boolean needsRebuild(CasinoPayload before, CasinoPayload after) {
		parse();
		String now = phase() + "/" + bets.stream().anyMatch(BetView::mine);
		boolean differ = !now.equals(shape);
		shape = now;
		return differ;
	}

	@Override
	protected int preferredHeight() {
		return 290;
	}

	// ------------------------------------------------------------------ layout

	private int wheelR() {
		int byWidth = (gridX() - CELL_W - 10 - (left + FRAME + 14)) / 2;
		return Math.max(24, Math.min(58, Math.min(byWidth, (tableBottom() - tableTop()) / 2 - 22)));
	}

	@Override
	protected int preferredWidth() {
		return 470;
	}

	private int wheelX() {
		return left + FRAME + 14 + wheelR();
	}

	private int wheelY() {
		return tableTop() + 8 + wheelR();
	}

	/** The left of the number grid (the zeros sit to its left). */
	private int gridX() {
		return left + panelWidth - FRAME - 12 - CELL_W - 12 * CELL_W;
	}

	private int gridY() {
		return tableTop() + 22;
	}

	/** The number at grid column i (0..11) and screen row r (0 top: 3, 6, 9 ...). */
	private static int numberAt(int i, int r) {
		return 3 * i + (3 - r);
	}

	@Override
	protected void initGame() {
		shape = phase() + "/" + bets.stream().anyMatch(BetView::mine);
		int y = tableBottom() - 16;
		int x = addChipRack(left + FRAME + 8, y);
		boolean open = phase() != Roulette.Phase.SPINNING;
		boolean mine = bets.stream().anyMatch(BetView::mine);
		Plate clear = addRenderableWidget(new Plate(x + 6, y, 74, 14, "Take back", Plate.Kind.WOOD,
				b -> send(Action.CASINO_ACT, 0, "", 0)));
		clear.active = open && mine;
		Plate spin = addRenderableWidget(new Plate(x + 84, y, 50, 14, "Spin", Plate.Kind.BRASS,
				b -> send(Action.CASINO_ACT, 1, "", 0)));
		spin.active = phase() == Roulette.Phase.BETTING && mine;
	}

	/** The bet under the mouse, or 0. */
	private long betAt(double mx, double my) {
		int gx = gridX();
		int gy = gridY();
		// Zeros: 00 on the top half, 0 on the bottom.
		if (mx >= gx - CELL_W && mx < gx && my >= gy && my < gy + 3 * CELL_H) {
			double v = (my - gy) / (3.0 * CELL_H);
			if (Math.abs(v - 0.5) < 0.06) {
				return Roulette.bits(0, Roulette.DOUBLE_ZERO);
			}
			return Roulette.bit(v < 0.5 ? Roulette.DOUBLE_ZERO : 0);
		}
		// The five-number bet: the strip under the zeros.
		if (mx >= gx - CELL_W && mx < gx && my >= gy + 3 * CELL_H && my < gy + 3 * CELL_H + STREET) {
			return Roulette.bits(0, Roulette.DOUBLE_ZERO, 1, 2, 3);
		}
		double fx = (mx - gx) / CELL_W;
		double fy = (my - gy) / CELL_H;
		if (fx >= -0.2 && fx < 12 && my >= gy && my < gy + 3 * CELL_H + STREET) {
			int i = (int) Math.floor(Math.max(0, fx));
			double u = fx - i;
			if (fx < 0) {
				u = 0;
			}
			boolean street = my >= gy + 3 * CELL_H;
			if (street) {
				if (u > 0.8 && i < 11) {
					return line(i);
				}
				if (u < 0.2 && i > 0) {
					return line(i - 1);
				}
				return Roulette.bits(numberAt(i, 2), numberAt(i, 1), numberAt(i, 0));
			}
			int r = (int) Math.floor(fy);
			double v = fy - r;
			int n = numberAt(i, r);
			boolean leftEdge = u < 0.2;
			boolean rightEdge = u > 0.8 && i < 11;
			boolean topEdge = v < 0.22 && r > 0;
			boolean bottomEdge = v > 0.78 && r < 2;
			if (leftEdge && i == 0) {
				// Splits onto the zeros: 00-3, 00-2 / 0-2, 0-1.
				return switch (r) {
					case 0 -> Roulette.bits(Roulette.DOUBLE_ZERO, 3);
					case 1 -> Roulette.bits(v < 0.5 ? Roulette.DOUBLE_ZERO : 0, 2);
					default -> Roulette.bits(0, 1);
				};
			}
			int across = leftEdge ? -1 : rightEdge ? 1 : 0;
			int down = topEdge ? -1 : bottomEdge ? 1 : 0;
			if (across != 0 && down != 0) {
				return Roulette.bits(n, numberAt(i + across, r), numberAt(i, r + down), numberAt(i + across, r + down));
			}
			if (across != 0) {
				return Roulette.bits(n, numberAt(i + across, r));
			}
			if (down != 0) {
				return Roulette.bits(n, numberAt(i, r + down));
			}
			return Roulette.bit(n);
		}
		// Columns: "2 to 1" at the end of each row.
		int ex = gx + 12 * CELL_W;
		if (mx >= ex && mx < ex + CELL_W && my >= gy && my < gy + 3 * CELL_H) {
			int r = (int) ((my - gy) / CELL_H);
			long out = 0;
			for (int i = 0; i < 12; i++) {
				out |= Roulette.bit(numberAt(i, r));
			}
			return out;
		}
		// Dozens, then the even-money boxes.
		int oy = gy + 3 * CELL_H + STREET;
		if (mx >= gx && mx < gx + 12 * CELL_W && my >= oy && my < oy + 2 * CELL_H) {
			int i = (int) ((mx - gx) / CELL_W);
			if (my < oy + CELL_H) {
				int d = i / 4;
				return range(d * 12 + 1, d * 12 + 12);
			}
			return switch (i / 2) {
				case 0 -> range(1, 18);
				case 1 -> parity(0);
				case 2 -> colour(true);
				case 3 -> colour(false);
				case 4 -> parity(1);
				default -> range(19, 36);
			};
		}
		return 0;
	}

	private static long line(int i) {
		return Roulette.bits(numberAt(i, 0), numberAt(i, 1), numberAt(i, 2), numberAt(i + 1, 0), numberAt(i + 1, 1),
				numberAt(i + 1, 2));
	}

	private static long range(int from, int to) {
		long out = 0;
		for (int n = from; n <= to; n++) {
			out |= Roulette.bit(n);
		}
		return out;
	}

	private static long parity(int odd) {
		long out = 0;
		for (int n = 1; n <= 36; n++) {
			if (n % 2 == odd) {
				out |= Roulette.bit(n);
			}
		}
		return out;
	}

	private static long colour(boolean red) {
		long out = 0;
		for (int n = 1; n <= 36; n++) {
			if (Roulette.red(n) == red) {
				out |= Roulette.bit(n);
			}
		}
		return out;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (settingsOpen()) {
			return super.mouseClicked(event, doubleClick);
		}
		if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && phase() != Roulette.Phase.SPINNING) {
			long numbers = betAt(event.x(), event.y());
			if (numbers != 0 && Roulette.isLegal(numbers)) {
				send(Action.CASINO_BET, 0, String.valueOf(numbers), chip);
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	// ------------------------------------------------------------------ drawing

	@Override
	protected void drawTableBack(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		int x0 = left + FRAME + 4;
		int x1 = left + panelWidth - FRAME - 4;
		int y0 = tableTop() - 2;
		int y1 = tableBottom() - 20;
		g.fill(x0, y0, x1, y1, RAIL);
		g.fill(x0 + 3, y0 + 3, x1 - 3, y1 - 3, GOLD_DIM);
		g.fill(x0 + 4, y0 + 4, x1 - 4, y1 - 4, FELT);
		// Layout cells.
		int gx = gridX();
		int gy = gridY();
		hoverNumbers = phase() == Roulette.Phase.SPINNING ? 0 : betAt(mouseX, mouseY);
		if (!Roulette.isLegal(hoverNumbers)) {
			hoverNumbers = 0;
		}
		for (int i = 0; i < 12; i++) {
			for (int r = 0; r < 3; r++) {
				int n = numberAt(i, r);
				int x = gx + i * CELL_W;
				int y = gy + r * CELL_H;
				g.fill(x, y, x + CELL_W + 1, y + CELL_H + 1, WHITE);
				int colour = Roulette.red(n) ? RED : BLACK;
				if ((hoverNumbers & Roulette.bit(n)) != 0) {
					colour = Roulette.red(n) ? 0xFFE0524A : 0xFF4A4642;
				}
				g.fill(x + 1, y + 1, x + CELL_W, y + CELL_H, colour);
			}
		}
		// Zeros.
		int zx = gx - CELL_W;
		g.fill(zx, gy, gx + 1, gy + 3 * CELL_H + 1, WHITE);
		boolean hz2 = (hoverNumbers & Roulette.bit(Roulette.DOUBLE_ZERO)) != 0;
		boolean hz = (hoverNumbers & Roulette.bit(0)) != 0;
		g.fill(zx + 1, gy + 1, gx, gy + 3 * CELL_H / 2, hz2 ? 0xFF4FB86A : ZERO);
		g.fill(zx + 1, gy + 3 * CELL_H / 2 + 1, gx, gy + 3 * CELL_H, hz ? 0xFF4FB86A : ZERO);
		// Street strip, columns, dozens, even money.
		int ex = gx + 12 * CELL_W;
		for (int r = 0; r < 3; r++) {
			outline(g, ex, gy + r * CELL_H, CELL_W + 1, CELL_H + 1, WHITE);
		}
		int oy = gy + 3 * CELL_H + STREET;
		g.fill(zx, gy + 3 * CELL_H, ex, gy + 3 * CELL_H + 1, WHITE);
		for (int d = 0; d < 3; d++) {
			outline(g, gx + d * 4 * CELL_W, oy, 4 * CELL_W + 1, CELL_H + 1, WHITE);
		}
		for (int k = 0; k < 6; k++) {
			int bx = gx + k * 2 * CELL_W;
			outline(g, bx, oy + CELL_H, 2 * CELL_W + 1, CELL_H + 1, WHITE);
			if (k == 2 || k == 3) {
				int c = k == 2 ? RED : BLACK;
				diamond(g, bx + CELL_W, oy + CELL_H + CELL_H / 2, 5, c);
			}
		}
	}

	private static void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, int colour) {
		g.fill(x, y, x + w, y + 1, colour);
		g.fill(x, y + h - 1, x + w, y + h, colour);
		g.fill(x, y, x + 1, y + h, colour);
		g.fill(x + w - 1, y, x + w, y + h, colour);
	}

	@Override
	protected void drawTable(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		int gx = gridX();
		int gy = gridY();
		for (int i = 0; i < 12; i++) {
			for (int r = 0; r < 3; r++) {
				String label = String.valueOf(numberAt(i, r));
				g.text(font, label, gx + i * CELL_W + (CELL_W - font.width(label)) / 2 + 1, gy + r * CELL_H + 6, WHITE, false);
			}
		}
		int zx = gx - CELL_W;
		g.text(font, "00", zx + (CELL_W - font.width("00")) / 2 + 1, gy + 3 * CELL_H / 4 - 3, WHITE, false);
		g.text(font, "0", zx + (CELL_W - font.width("0")) / 2 + 1, gy + 9 * CELL_H / 4 - 3, WHITE, false);
		int ex = gx + 12 * CELL_W;
		for (int r = 0; r < 3; r++) {
			g.text(font, "2:1", ex + 3, gy + r * CELL_H + 6, WHITE, false);
		}
		int oy = gy + 3 * CELL_H + STREET;
		String[] dozens = {"1st 12", "2nd 12", "3rd 12"};
		for (int d = 0; d < 3; d++) {
			g.text(font, dozens[d], gx + d * 4 * CELL_W + (4 * CELL_W - font.width(dozens[d])) / 2, oy + 6, WHITE, false);
		}
		String[] outside = {"1-18", "EVEN", "", "", "ODD", "19-36"};
		for (int k = 0; k < 6; k++) {
			if (!outside[k].isEmpty()) {
				g.text(font, outside[k], gx + k * 2 * CELL_W + (2 * CELL_W - font.width(outside[k])) / 2, oy + CELL_H + 6, WHITE,
						false);
			}
		}
		// Chips on the layout, one stack per bet (yours gold-ringed).
		Map<Long, long[]> stacks = new LinkedHashMap<>();
		for (BetView bet : bets) {
			long[] s = stacks.computeIfAbsent(bet.numbers(), k -> new long[2]);
			s[0] += bet.amount();
			if (bet.mine()) {
				s[1] = 1;
			}
		}
		for (Map.Entry<Long, long[]> entry : stacks.entrySet()) {
			int[] at = anchor(entry.getKey());
			if (entry.getValue()[1] == 1) {
				g.fill(at[0] - 6, at[1] - 6, at[0] + 6, at[1] + 6, GOLD);
			}
			chip(g, at[0] - 5, at[1] - 5, entry.getValue()[0], false);
		}
		// Hover help.
		if (hoverNumbers != 0) {
			String help = Roulette.name(hoverNumbers) + " pays " + Roulette.ratio(hoverNumbers) + " to 1";
			g.text(font, help, gx - CELL_W, gy - 12, CREAM, true);
		} else {
			String status = switch (phase()) {
				case IDLE -> "Place your chips.";
				case BETTING -> "Bets close in " + seconds() + "s";
				case SPINNING -> "No more bets!";
				case RESULT -> result >= 0 ? "" : "";
			};
			g.text(font, status, gx - CELL_W, gy - 12, phase() == Roulette.Phase.SPINNING ? GOLD : TAN, true);
		}
		drawWheel(g);
		// History.
		int hx = left + FRAME + 10;
		int hy = wheelY() + wheelR() + 8;
		g.text(font, "Last:", hx, hy + 2, DIM, false);
		int x = hx + 26;
		for (int i = history.size() - 1; i >= 0 && x < gridX() - 26; i--) {
			int n = history.get(i);
			String label = Roulette.label(n);
			int w = Math.max(12, font.width(label) + 4);
			g.fill(x, hy, x + w, hy + 11, n == 0 || n == Roulette.DOUBLE_ZERO ? ZERO : Roulette.red(n) ? RED : BLACK);
			g.text(font, label, x + (w - font.width(label)) / 2, hy + 2, WHITE, false);
			x += w + 2;
		}
		long mine = 0;
		for (BetView bet : bets) {
			if (bet.mine()) {
				mine += bet.amount();
			}
		}
		if (mine > 0) {
			String on = "Your chips: " + FinanceBook.amount(mine);
			g.text(font, on, left + panelWidth - FRAME - 10 - font.width(on), tableBottom() - 13, GOLD, false);
		}
	}

	/** Where a bet's chips sit on the layout. */
	private int[] anchor(long numbers) {
		int gx = gridX();
		int gy = gridY();
		int oy = gy + 3 * CELL_H + STREET;
		int k = Long.bitCount(numbers);
		if (k == 12 || k == 18) {
			// Outside bets: find the box by testing their centres.
			for (int r = 0; r < 3; r++) {
				if (betAt(gx + 12 * CELL_W + CELL_W / 2.0, gy + r * CELL_H + CELL_H / 2.0) == numbers) {
					return new int[] {gx + 12 * CELL_W + CELL_W / 2, gy + r * CELL_H + CELL_H / 2};
				}
			}
			for (int d = 0; d < 3; d++) {
				if (betAt(gx + d * 4 * CELL_W + 2 * CELL_W, oy + CELL_H / 2.0) == numbers) {
					return new int[] {gx + d * 4 * CELL_W + 2 * CELL_W, oy + CELL_H / 2};
				}
			}
			for (int b = 0; b < 6; b++) {
				if (betAt(gx + b * 2 * CELL_W + CELL_W, oy + CELL_H * 1.5) == numbers) {
					return new int[] {gx + b * 2 * CELL_W + CELL_W, oy + CELL_H + CELL_H / 2};
				}
			}
		}
		if (k == 5) {
			return new int[] {gx - CELL_W / 2, gy + 3 * CELL_H + STREET / 2};
		}
		// Inside bets: the middle of their numbers; streets and lines on the street strip.
		double sx = 0;
		double sy = 0;
		int count = 0;
		for (int n = 0; n < Roulette.POCKETS; n++) {
			if ((numbers & Roulette.bit(n)) == 0) {
				continue;
			}
			int[] c = centre(n);
			sx += c[0];
			sy += c[1];
			count++;
		}
		int ax = (int) Math.round(sx / count);
		int ay = (int) Math.round(sy / count);
		boolean zeros = (numbers & Roulette.bits(0, Roulette.DOUBLE_ZERO)) != 0;
		if ((k == 3 || k == 6) && !zeros) {
			ay = gy + 3 * CELL_H + STREET / 2;
		}
		return new int[] {ax, ay};
	}

	private int[] centre(int n) {
		int gx = gridX();
		int gy = gridY();
		if (n == Roulette.DOUBLE_ZERO) {
			return new int[] {gx - CELL_W / 2, gy + 3 * CELL_H / 4};
		}
		if (n == 0) {
			return new int[] {gx - CELL_W / 2, gy + 9 * CELL_H / 4};
		}
		int i = (n - 1) / 3;
		int r = 3 - ((n - 1) % 3 + 1);
		return new int[] {gx + i * CELL_W + CELL_W / 2, gy + r * CELL_H + CELL_H / 2};
	}

	// ------------------------------------------------------------------ the wheel

	private void drawWheel(GuiGraphicsExtractor g) {
		int cx = wheelX();
		int cy = wheelY();
		int R = wheelR();
		double time = ticks + partial;
		Roulette.Phase phase = phase();
		double spinLeft = phase == Roulette.Phase.SPINNING ? Math.max(0, timer - partial) / Roulette.SPIN_TICKS : 0;
		// The wheel turns slowly all the time, faster while the ball runs.
		wheelAngle = time * 0.02 + (phase == Roulette.Phase.SPINNING ? spinLeft * spinLeft * 6 : 0);
		oval(g, cx, cy, R + 4, R + 4, WALNUT_DARK);
		oval(g, cx, cy, R + 3, R + 3, GOLD_DIM);
		oval(g, cx, cy, R + 1, R + 1, 0xFF4A301A);
		// Pockets, drawn row by row in runs of one colour.
		int inner = R - 12;
		for (int dy = -R; dy <= R; dy++) {
			int runStart = Integer.MIN_VALUE;
			int runColour = 0;
			for (int dx = -R; dx <= R + 1; dx++) {
				int colour = 0;
				double d = Math.hypot(dx + 0.5, dy + 0.5);
				if (dx <= R && d <= R - 1 && d >= inner) {
					double a = Math.atan2(dy + 0.5, dx + 0.5) - wheelAngle;
					a = ((a % (Math.PI * 2)) + Math.PI * 2) % (Math.PI * 2);
					double slot = a / (Math.PI * 2) * Roulette.POCKETS;
					int pocket = (int) slot % Roulette.POCKETS;
					int n = Roulette.WHEEL[pocket];
					colour = slot - Math.floor(slot) < 0.08 ? GOLD_DIM : n == 0 || n == Roulette.DOUBLE_ZERO ? ZERO
							: Roulette.red(n) ? RED : BLACK;
				}
				if (colour != runColour) {
					if (runColour != 0) {
						g.fill(cx + runStart, cy + dy, cx + dx, cy + dy + 1, runColour);
					}
					runStart = dx;
					runColour = colour;
				}
			}
		}
		// Each pocket's number, upright in a tiny pixel font so two digits fit in a pocket.
		for (int i = 0; i < Roulette.POCKETS; i++) {
			double a = wheelAngle + (i + 0.5) / Roulette.POCKETS * Math.PI * 2;
			double mid = (R - 1 + inner) / 2.0;
			int px = cx + (int) Math.round(Math.cos(a) * mid);
			int py = cy + (int) Math.round(Math.sin(a) * mid);
			tinyNumber(g, Roulette.label(Roulette.WHEEL[i]), px, py, 0xFFF4EEDC);
		}
		oval(g, cx, cy, inner, inner, GOLD_DIM);
		oval(g, cx, cy, inner - 1, inner - 1, 0xFF6A4626);
		oval(g, cx, cy, inner - 10, inner - 10, WALNUT);
		oval(g, cx, cy, 5, 5, GOLD);
		g.fill(cx - 9, cy - 1, cx + 9, cy + 1, GOLD);
		g.fill(cx - 1, cy - 9, cx + 1, cy + 9, GOLD);
		// The ball.
		if (result >= 0 && (phase == Roulette.Phase.SPINNING || phase == Roulette.Phase.RESULT)) {
			int pocket = 0;
			for (int i = 0; i < Roulette.WHEEL.length; i++) {
				if (Roulette.WHEEL[i] == result) {
					pocket = i;
				}
			}
			double settled = wheelAngle + (pocket + 0.5) / Roulette.POCKETS * Math.PI * 2;
			double ballAngle = settled - spinLeft * spinLeft * Math.PI * 2 * 5;
			// The ball runs round the rim, then drops into the pocket track just inside the numbers.
			double radius = phase == Roulette.Phase.SPINNING && spinLeft > 0.25 ? R + 1 : inner - 3;
			int bx = cx + (int) Math.round(Math.cos(ballAngle) * radius);
			int by = cy + (int) Math.round(Math.sin(ballAngle) * radius);
			g.fill(bx - 2, by - 1, bx + 2, by + 2, 0xFFFFFFFF);
			g.fill(bx - 1, by - 2, bx + 1, by + 3, 0xFFFFFFFF);
			if (phase == Roulette.Phase.RESULT) {
				String label = Roulette.label(result) + " " + (result == 0 || result == Roulette.DOUBLE_ZERO ? "green"
						: Roulette.red(result) ? "red" : "black");
				int w = font.width(label) + 10;
				g.fill(cx - w / 2, cy - 6, cx + w / 2, cy + 7, PLAQUE);
				g.text(font, label, cx - font.width(label) / 2, cy - 3, result == 0 || result == Roulette.DOUBLE_ZERO ? GAIN
						: Roulette.red(result) ? 0xFFFF6A5A : CREAM, false);
			}
		}
	}

	private static final String[] DIGITS = {
			"XXX", "X.X", "X.X", "X.X", "XXX",
			".X.", "XX.", ".X.", ".X.", "XXX",
			"XXX", "..X", "XXX", "X..", "XXX",
			"XXX", "..X", "XXX", "..X", "XXX",
			"X.X", "X.X", "XXX", "..X", "..X",
			"XXX", "X..", "XXX", "..X", "XXX",
			"XXX", "X..", "XXX", "X.X", "XXX",
			"XXX", "..X", ".X.", ".X.", ".X.",
			"XXX", "X.X", "XXX", "X.X", "XXX",
			"XXX", "X.X", "XXX", "..X", "XXX"};

	/** A number in a 3x5 pixel font, centred on (cx, cy): small enough for a roulette pocket. */
	private static void tinyNumber(GuiGraphicsExtractor g, String text, int cx, int cy, int colour) {
		int w = text.length() * 4 - 1;
		int x0 = cx - w / 2;
		int y0 = cy - 2;
		for (int c = 0; c < text.length(); c++) {
			int d = text.charAt(c) - '0';
			if (d < 0 || d > 9) {
				continue;
			}
			for (int row = 0; row < 5; row++) {
				String line = DIGITS[d * 5 + row];
				for (int col = 0; col < 3; col++) {
					if (line.charAt(col) == 'X') {
						int x = x0 + c * 4 + col;
						g.fill(x, y0 + row, x + 1, y0 + row + 1, colour);
					}
				}
			}
		}
	}

	private Roulette.Phase lastPhase;

	@Override
	public void tick() {
		super.tick();
		long pos = data.pos();
		Roulette.Phase now = phase();
		if (now == Roulette.Phase.SPINNING) {
			// The ball rattling round the rim, slower as it loses speed.
			double left = (double) timer / Roulette.SPIN_TICKS;
			int every = 2 + (int) ((1 - left) * 7);
			if (ticks % every == 0) {
				CasinoSound.tick(data.pos(), 1.9F - (float) (1 - left) * 0.4F);
			}
		} else if (lastPhase == Roulette.Phase.SPINNING) {
			// It drops into the pocket.
			CasinoSound.play(pos, net.minecraft.sounds.SoundEvents.CHAIN_PLACE, 0.5F, 1.9F);
		}
		lastPhase = now;
	}

	@Override
	protected CasinoTheme theme() {
		return CasinoTheme.TABLE;
	}
}
