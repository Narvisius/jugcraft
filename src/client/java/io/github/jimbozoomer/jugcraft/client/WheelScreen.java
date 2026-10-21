package io.github.jimbozoomer.jugcraft.client;

import static io.github.jimbozoomer.jugcraft.client.ExchangeScreen.*;

import io.github.jimbozoomer.jugcraft.finance.CasinoPayload;
import io.github.jimbozoomer.jugcraft.finance.FinanceActionPayload.Action;
import io.github.jimbozoomer.jugcraft.finance.FinanceBook;
import io.github.jimbozoomer.jugcraft.finance.ShowGames;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * The prize wheel (Big Six): the big wheel on the left with its 54 segments and a leather clapper at the top, the
 * betting board of seven symbols on the right. Click a symbol to bet the picked chip; the wheel spins when the clock
 * runs out (or Spin) and slows into the segment the server drew.
 */
public class WheelScreen extends CasinoScreen {
	private static final int[] COLOURS = {0xFFEFE2C2, 0xFF5F93C6, 0xFF7FC96A, 0xFFE0624F, 0xFF9C7AE0, 0xFF1C1A1A, 0xFFE8C463};

	private record BetView(int symbol, long amount, boolean mine) {
	}

	private int result = -1;
	private long spins = -1;
	private final List<Integer> history = new ArrayList<>();
	private final List<BetView> bets = new ArrayList<>();
	private String shape = "";
	private double restAngle;

	public WheelScreen(CasinoPayload data) {
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
			bets.add(new BetView(r.nextInt(), r.next(), r.nextBool()));
		}
	}

	private ShowGames.BigSix.Phase phase() {
		return ShowGames.BigSix.Phase.values()[Math.max(0, Math.min(3, data.phase()))];
	}

	@Override
	protected boolean needsRebuild(CasinoPayload before, CasinoPayload after) {
		parse();
		String now = phase() + "/" + bets.stream().anyMatch(BetView::mine);
		return !now.equals(shape);
	}

	@Override
	protected int preferredWidth() {
		return 460;
	}

	@Override
	protected int preferredHeight() {
		return 300;
	}

	private int wheelR() {
		return Math.min(100, (tableBottom() - tableTop() - 40) / 2);
	}

	private int wheelX() {
		return left + FRAME + 16 + wheelR();
	}

	private int wheelY() {
		return tableTop() + 12 + wheelR();
	}

	/** A betting row on the board: the seven fit between the status line and the history strip above the chips. */
	private int[] symbolBox(int symbol) {
		int x0 = wheelX() + wheelR() + 24;
		int x1 = left + panelWidth - FRAME - 10;
		int top = tableTop() + 18;
		int bottom = tableBottom() - 24 - 16;
		int gap = 2;
		int h = Math.max(12, Math.min(20, (bottom - top - 6 * gap) / 7));
		int y = top + symbol * (h + gap);
		return new int[] {x0, y, x1, y + h};
	}

	/** The little picture in a row's coloured square: the number, a jester's hat for the Joker, a jug for the Jug. */
	private void symbolIcon(GuiGraphicsExtractor g, int s, int x, int y, int w, int h) {
		int cx = x + w / 2;
		int cy = y + h / 2;
		if (s == 5) {
			// Jester's hat: three points with bells, on a band.
			int c1 = 0xFFC0362C;
			int c2 = 0xFF3A6EA5;
			g.fill(cx - 6, cy + 1, cx + 7, cy + 4, 0xFFE8C463);
			g.fill(cx - 6, cy - 2, cx - 2, cy + 1, c1);
			g.fill(cx - 5, cy - 4, cx - 3, cy - 2, c1);
			g.fill(cx - 2, cy - 3, cx + 3, cy + 1, c2);
			g.fill(cx - 1, cy - 5, cx + 2, cy - 3, c2);
			g.fill(cx + 3, cy - 2, cx + 7, cy + 1, c1);
			g.fill(cx + 4, cy - 4, cx + 6, cy - 2, c1);
			g.fill(cx - 6, cy - 6, cx - 4, cy - 4, 0xFFFFF0A0);
			g.fill(cx, cy - 7, cx + 2, cy - 5, 0xFFFFF0A0);
			g.fill(cx + 5, cy - 6, cx + 7, cy - 4, 0xFFFFF0A0);
		} else if (s == 6) {
			// A brown jug with a cork and a handle.
			int jug = 0xFF8A5A2A;
			g.fill(cx - 1, cy - 6, cx + 2, cy - 4, 0xFFC8A070);
			g.fill(cx - 2, cy - 4, cx + 3, cy - 2, jug);
			g.fill(cx - 4, cy - 2, cx + 5, cy + 5, jug);
			g.fill(cx - 3, cy + 5, cx + 4, cy + 6, jug);
			g.fill(cx + 5, cy - 1, cx + 7, cy + 3, jug);
			g.fill(cx - 3, cy - 1, cx - 1, cy + 3, 0xFFB07A3E);
		} else {
			String name = ShowGames.BigSix.NAMES[s];
			g.text(font, name, cx - font.width(name) / 2 + 1, cy - 3, 0xFF1A0A0A, false);
		}
	}

	@Override
	protected void initGame() {
		shape = phase() + "/" + bets.stream().anyMatch(BetView::mine);
		int by = tableBottom() - 18;
		int x = addChipRack(left + FRAME + 8, by);
		boolean mine = bets.stream().anyMatch(BetView::mine);
		addRenderableWidget(new Plate(x + 6, by, 70, 14, "Take back", Plate.Kind.WOOD,
				b -> send(Action.CASINO_ACT, 0, "", 0))).active = mine && phase() != ShowGames.BigSix.Phase.SPINNING;
		addRenderableWidget(new Plate(x + 80, by, 50, 14, "Spin", Plate.Kind.BRASS,
				b -> send(Action.CASINO_ACT, 1, "", 0))).active = mine && phase() == ShowGames.BigSix.Phase.BETTING;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (settingsOpen()) {
			return super.mouseClicked(event, doubleClick);
		}
		if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && phase() != ShowGames.BigSix.Phase.SPINNING) {
			for (int s = 0; s < 7; s++) {
				int[] r = symbolBox(s);
				if (event.x() >= r[0] && event.x() < r[2] && event.y() >= r[1] && event.y() < r[3]) {
					send(Action.CASINO_BET, s, "", chip);
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	protected void drawTableBack(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		for (int s = 0; s < 7; s++) {
			int[] r = symbolBox(s);
			boolean hot = mouseX >= r[0] && mouseX < r[2] && mouseY >= r[1] && mouseY < r[3];
			g.fill(r[0] - 1, r[1] - 1, r[2] + 1, r[3] + 1, GOLD_DIM);
			g.fill(r[0], r[1], r[2], r[3], hot ? 0xFF3A2A1A : PLAQUE);
			g.fill(r[0] + 2, r[1] + 1, r[0] + 20, r[3] - 1, COLOURS[s]);
		}
	}

	/** The wheel's angle now: it rests, or spins down onto the result over the spin. */
	private double angle() {
		double seg = Math.PI * 2 / 54;
		if (phase() == ShowGames.BigSix.Phase.SPINNING && result >= 0) {
			double left = Math.max(0, timer - partial) / ShowGames.BigSix.SPIN_TICKS;
			double target = -Math.PI / 2 - (result + 0.5) * seg;
			return target + left * left * Math.PI * 2 * 5;
		}
		if (result >= 0) {
			return -Math.PI / 2 - (result + 0.5) * seg;
		}
		return restAngle;
	}

	@Override
	protected void drawTable(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		int cx = wheelX();
		int cy = wheelY();
		int r = wheelR();
		double a0 = angle();
		oval(g, cx, cy, r + 6, r + 6, 0xFF2A1A0E);
		oval(g, cx, cy, r + 4, r + 4, GOLD);
		oval(g, cx, cy, r + 2, r + 2, 0xFF4A301A);
		// Segments, row by row in runs.
		for (int dy = -r; dy <= r; dy++) {
			int runStart = Integer.MIN_VALUE;
			int runColour = 0;
			for (int dx = -r; dx <= r + 1; dx++) {
				int colour = 0;
				double d = Math.hypot(dx + 0.5, dy + 0.5);
				if (dx <= r && d <= r && d >= r * 0.35) {
					double a = Math.atan2(dy + 0.5, dx + 0.5) - a0;
					a = ((a % (Math.PI * 2)) + Math.PI * 2) % (Math.PI * 2);
					double seg = a / (Math.PI * 2) * 54;
					colour = seg - Math.floor(seg) < 0.07 ? GOLD_DIM : COLOURS[ShowGames.BigSix.WHEEL[(int) seg % 54]];
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
		oval(g, cx, cy, (int) (r * 0.35), (int) (r * 0.35), WALNUT);
		int hub = (int) (r * 0.3);
		oval(g, cx, cy, hub, hub, GOLD);
		if (font.width("BIG SIX") <= hub * 2 - 6) {
			g.text(font, "BIG SIX", cx - font.width("BIG SIX") / 2 + 1, cy - 3, 0xFF1A0A0A, false);
		} else {
			g.text(font, "BIG", cx - font.width("BIG") / 2 + 1, cy - 8, 0xFF1A0A0A, false);
			g.text(font, "SIX", cx - font.width("SIX") / 2 + 1, cy + 1, 0xFF1A0A0A, false);
		}
		// The clapper at the top.
		g.fill(cx - 3, cy - r - 10, cx + 4, cy - r + 6, 0xFF7A1A12);
		g.fill(cx - 1, cy - r + 6, cx + 2, cy - r + 10, 0xFF7A1A12);
		// The board.
		for (int s = 0; s < 7; s++) {
			int[] box = symbolBox(s);
			int ty = (box[1] + box[3]) / 2 - 4;
			symbolIcon(g, s, box[0] + 2, box[1] + 1, 18, box[3] - box[1] - 2);
			String pays = (s >= 5 ? ShowGames.BigSix.NAMES[s] + "  " : "") + ShowGames.BigSix.PAYS[s] + " to 1";
			g.text(font, pays, box[0] + 26, ty, CREAM, false);
			long total = 0;
			long mine = 0;
			for (BetView bet : bets) {
				if (bet.symbol() == s) {
					total += bet.amount();
					if (bet.mine()) {
						mine += bet.amount();
					}
				}
			}
			if (total > 0) {
				String on = (mine > 0 ? "yours " + shortAmount(mine) + " · " : "") + shortAmount(total);
				g.text(font, on, box[2] - 6 - font.width(on), ty, mine > 0 ? GOLD : TAN, false);
			}
			boolean won = phase() == ShowGames.BigSix.Phase.RESULT && result >= 0 && ShowGames.BigSix.WHEEL[result] == s;
			if (won && (ticks / 4) % 2 == 0) {
				g.fill(box[0] - 1, box[1] - 1, box[2] + 1, box[1], GOLD);
				g.fill(box[0] - 1, box[3], box[2] + 1, box[3] + 1, GOLD);
			}
		}
		String status = switch (phase()) {
			case IDLE -> "Click a symbol to bet.";
			case BETTING -> "Spins in " + seconds() + "s";
			case SPINNING -> "Round she goes...";
			case RESULT -> result >= 0 ? "It stops on " + ShowGames.BigSix.NAMES[ShowGames.BigSix.WHEEL[result]] + "!" : "";
		};
		int[] first = symbolBox(0);
		g.text(font, status, first[0], tableTop() + 6, phase() == ShowGames.BigSix.Phase.RESULT ? GOLD : TAN, true);
		// History.
		int hx = first[0];
		int hy = symbolBox(6)[3] + 4;
		for (int i = history.size() - 1, k = 0; i >= 0 && k < 12; i--, k++) {
			g.fill(hx + k * 12, hy, hx + k * 12 + 10, hy + 10, COLOURS[history.get(i)]);
		}
		long mineTotal = 0;
		for (BetView bet : bets) {
			if (bet.mine()) {
				mineTotal += bet.amount();
			}
		}
		if (mineTotal > 0) {
			String on = "Your bets: " + FinanceBook.amount(mineTotal);
			g.text(font, on, left + panelWidth - FRAME - 10 - font.width(on), tableBottom() - 32, GOLD, false);
		}
	}

	private int lastSegment = -1;

	@Override
	public void tick() {
		super.tick();
		// The clapper flicking over each peg as the wheel turns.
		int segment = (int) Math.floor(Math.floorMod((long) Math.floor(-angle() / (Math.PI * 2) * 54 * 1000), 54_000) / 1000.0);
		if (phase() == ShowGames.BigSix.Phase.SPINNING && segment != lastSegment) {
			CasinoSound.tick(data.pos(), 1.2F);
		}
		lastSegment = segment;
	}

	@Override
	protected CasinoTheme theme() {
		return CasinoTheme.WHEEL;
	}
}
