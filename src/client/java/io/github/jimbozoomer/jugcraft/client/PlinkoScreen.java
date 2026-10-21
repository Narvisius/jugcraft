package io.github.jimbozoomer.jugcraft.client;

import static io.github.jimbozoomer.jugcraft.client.ExchangeScreen.*;

import io.github.jimbozoomer.jugcraft.finance.CasinoPayload;
import io.github.jimbozoomer.jugcraft.finance.FinanceActionPayload.Action;
import io.github.jimbozoomer.jugcraft.finance.FinanceBook;
import io.github.jimbozoomer.jugcraft.finance.ShowGames;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The Plinko board: twelve rows of brass pegs on a lit board, thirteen prize slots along the bottom coloured by how
 * much they pay, and the balls bouncing down the paths the server drew. Pick a risk (Low, Medium, High), a chip, DROP;
 * press DROP again while balls are still falling to send more after them.
 */
public class PlinkoScreen extends CasinoScreen {
	/** How many ticks a ball takes per row, and how long it stays lit in its slot after landing. */
	private static final int TICKS_PER_ROW = 3;
	private static final int LANDED_TICKS = 50;

	/** One ball on the board: its path (bit i: right at row i), slot, risk, bet, win and ticks since it was dropped. */
	private static final class Ball {
		final int path;
		final int slot;
		final int risk;
		final long bet;
		final long win;
		int age;

		Ball(int path, int slot, int risk, long bet, long win, int age) {
			this.path = path;
			this.slot = slot;
			this.risk = risk;
			this.bet = bet;
			this.win = win;
			this.age = age;
		}

		boolean landed() {
			return age > ShowGames.Plinko.ROWS * TICKS_PER_ROW;
		}
	}

	private int risk = 1;
	private long lastSeen = -1;
	private boolean busy;
	private final java.util.List<Ball> balls = new java.util.ArrayList<>();
	/** The newest ball that has landed (shown in the status line), and the total won by the balls of this run. */
	private Ball lastLanded;
	private long runWon;
	private long runBet;
	private int runBalls;
	private String shape = "";

	public PlinkoScreen(CasinoPayload data) {
		super(data);
		parse(true);
	}

	private void parse(boolean first) {
		CasinoPayload.Reader r = new CasinoPayload.Reader(data.data());
		int serverRisk = r.nextInt();
		r.next();
		r.next();
		r.next();
		r.next();
		long drops = r.next();
		busy = r.nextBool();
		int count = r.nextInt();
		if (first) {
			risk = serverRisk;
		}
		for (int i = 0; i < count; i++) {
			long number = r.next();
			int path = r.nextInt();
			int slot = r.nextInt();
			int ballRisk = r.nextInt();
			long bet = r.next();
			long win = r.next();
			if (first) {
				// Opening the screen shows the last result without replaying old balls.
				if (i == count - 1) {
					lastLanded = new Ball(path, slot, ballRisk, bet, win, 1000);
				}
			} else if (number > lastSeen) {
				if (balls.stream().allMatch(Ball::landed)) {
					runWon = 0;
					runBet = 0;
					runBalls = 0;
				}
				balls.add(new Ball(path, slot, ballRisk, bet, win, 0));
				runWon += win;
				runBet += bet;
				runBalls++;
			}
		}
		lastSeen = Math.max(lastSeen, drops);
	}

	@Override
	protected boolean needsRebuild(CasinoPayload before, CasinoPayload after) {
		parse(false);
		fixRisk();
		String now = busy + "/" + risk + "/" + table(7);
		return !now.equals(shape);
	}

	@Override
	protected boolean ownCelebration() {
		return true;
	}

	/** Balls still falling: their results wait until they land. */
	@Override
	protected boolean revealing() {
		return balls.stream().anyMatch(b -> !b.landed());
	}

	@Override
	public void tick() {
		super.tick();
		boolean pegged = false;
		for (Ball ball : balls) {
			boolean was = ball.landed();
			ball.age++;
			if (!was && ball.landed()) {
				lastLanded = ball;
				// The ball drops into its slot: a chime, or a fanfare for a big multiplier.
				celebrate(ball.win, ball.bet, false);
			} else if (!ball.landed() && ball.age % TICKS_PER_ROW == 0 && !pegged) {
				// One peg strike a tick at most, however many balls are falling; higher as the ball goes down.
				pegged = true;
				CasinoSound.peg(data.pos(), 0.7F + (ball.age / TICKS_PER_ROW) * 0.09F);
			}
		}
		balls.removeIf(b -> b.age > ShowGames.Plinko.ROWS * TICKS_PER_ROW + LANDED_TICKS);
	}

	@Override
	protected int preferredWidth() {
		return 420;
	}

	@Override
	protected int preferredHeight() {
		return 310;
	}

	private int boardTop() {
		return tableTop() + 14;
	}

	private int boardBottom() {
		return tableBottom() - 2;
	}

	/** The side columns: risk and chips on the left, DROP and the payback on the right. */
	private static final int SIDE = 88;

	private int sideLeft() {
		return left + FRAME + 8;
	}

	private int sideRight() {
		return left + panelWidth - FRAME - 8 - SIDE;
	}

	/** Pegs across: as wide as the screen allows, so the prize slots have room for their labels. */
	private int pegGap() {
		return Math.max(8, Math.min(26, (panelWidth - 2 * (FRAME + 8 + SIDE + 8) - 16) / 14));
	}

	/** Pegs down: the board is squashed to fit a short screen rather than shrunk all over. */
	private int rowGap() {
		return Math.max(6, Math.min(pegGap() + 4, (boardBottom() - boardTop() - 52) / 12));
	}

	private int centreX() {
		return left + panelWidth / 2;
	}

	/** Where peg (row, k) sits; row 0 has three pegs. */
	private int[] peg(int row, int k) {
		int gap = pegGap();
		int x = centreX() + (int) Math.round((k - (row + 2) / 2.0) * gap);
		int y = boardTop() + 10 + row * rowGap();
		return new int[] {x, y};
	}

	private int slotX(int k) {
		return centreX() + (int) Math.round((k - 6) * pegGap());
	}

	private int slotY() {
		return boardTop() + 10 + 12 * rowGap();
	}

	@Override
	protected void initGame() {
		fixRisk();
		shape = busy + "/" + risk + "/" + table(7);
		int x = sideLeft();
		int y = tableTop() + 14;
		for (int r = 0; r < 3; r++) {
			int chosen = r;
			Plate plate = addRenderableWidget(new Plate(x, y + r * 17, SIDE, 15, ShowGames.Plinko.RISKS[r],
					r == risk ? Plate.Kind.TAB_OPEN : Plate.Kind.TAB, b -> {
						risk = chosen;
						rebuildWidgets();
					}));
			// Risks the owner has closed stay greyed out.
			plate.active = r != risk && allowed(r);
		}
		// The chips, three to a row, under the risks.
		int cy = y + 3 * 17 + 14;
		java.util.List<Long> values = chips();
		for (int i = 0; i < values.size(); i++) {
			long v = values.get(i);
			Plate plate = addRenderableWidget(new Plate(x + (i % 3) * 30, cy + (i / 3) * 16, 28, 14, shortAmount(v),
					v == chip ? Plate.Kind.TAB_OPEN : Plate.Kind.WOOD, b -> {
						chip = v;
						rebuildWidgets();
					}));
			plate.active = v != chip;
		}
		addRenderableWidget(new Plate(sideRight(), y, SIDE, 22, "DROP", Plate.Kind.BRASS,
				b -> send(Action.CASINO_ACT, 0, String.valueOf(risk), chip))).active = !busy;
	}

	private static int slotColour(int tenths) {
		if (tenths >= 1_000) {
			return 0xFFE0302A;
		}
		if (tenths >= 100) {
			return 0xFFE07A2A;
		}
		if (tenths >= 20) {
			return 0xFFE0C03A;
		}
		if (tenths >= 10) {
			return 0xFF7ABE4A;
		}
		return 0xFF3A7A8A;
	}

	@Override
	protected void drawTableBack(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		int gap = pegGap();
		int x0 = centreX() - 7 * gap - 8;
		int x1 = centreX() + 7 * gap + 8;
		g.fill(x0 - 3, boardTop() - 3, x1 + 3, slotY() + 40, GOLD_DIM);
		g.fill(x0 - 2, boardTop() - 2, x1 + 2, slotY() + 39, 0xFF141228);
		g.fill(x0, boardTop(), x1, slotY() + 37, 0xFF1C1A3A);
		for (int row = 0; row < ShowGames.Plinko.ROWS; row++) {
			for (int k = 0; k < row + 3; k++) {
				int[] p = peg(row, k);
				g.fill(p[0] - 1, p[1] - 1, p[0] + 2, p[1] + 2, 0xFFE8C463);
				g.fill(p[0] - 1, p[1] - 1, p[0], p[1], 0xFFFFF0B0);
			}
		}
		int[] tenths = ShowGames.Plinko.TENTHS[risk];
		for (int k = 0; k < 13; k++) {
			int sx = slotX(k);
			int slotK = k;
			boolean lit = balls.stream().anyMatch(b -> b.slot == slotK && b.landed() && (b.age / 4) % 2 == 0);
			g.fill(sx - gap / 2 + 1, slotY() + 4, sx + gap / 2 - 1, slotY() + 15, lit ? 0xFFFFFFFF : slotColour(tenths[k]));
		}
	}

	@Override
	protected void drawTable(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		int gap = pegGap();
		int[] tenths = ShowGames.Plinko.TENTHS[risk];
		for (int k = 0; k < 13; k++) {
			// Under the slots, every other one a line lower, so each label has two slots' width.
			String label = tenths[k] % 10 == 0 ? tenths[k] / 10 + "x" : tenths[k] / 10 + "." + tenths[k] % 10;
			if (font.width(label) > 2 * gap - 2) {
				label = label.replace("x", "");
			}
			g.text(font, label, slotX(k) - font.width(label) / 2, slotY() + 18 + (k % 2) * 10, slotColour(tenths[k]), true);
		}
		// Every ball in the air, three ticks a row down its own path.
		for (Ball ball : balls) {
			drawBall(g, ball, gap);
		}
		long falling = balls.stream().filter(b -> !b.landed()).count();
		String status;
		int colour;
		if (busy) {
			status = (data.text().isEmpty() ? "Someone" : data.text().get(0)) + " is playing.";
			colour = LOSS;
		} else if (lastLanded != null) {
			String one = String.format(java.util.Locale.ROOT, "%.1fx: %s", ShowGames.Plinko.TENTHS[lastLanded.risk][lastLanded.slot] / 10.0,
					lastLanded.win > 0 ? "WIN " + FinanceBook.amount(lastLanded.win) : "no win");
			if (falling > 0) {
				status = one + " (" + falling + " falling)";
			} else if (runBalls > 1) {
				status = runBalls + " balls: bet " + FinanceBook.amount(runBet) + ", won " + FinanceBook.amount(runWon);
			} else {
				status = one;
			}
			colour = (falling == 0 && runBalls > 1 ? runWon >= runBet : lastLanded.win >= lastLanded.bet) ? GAIN : DIM;
		} else {
			status = falling > 0 ? falling + " falling" : "";
			colour = DIM;
		}
		g.text(font, status, centreX() - font.width(status) / 2, boardTop() - 12, colour, true);
		g.text(font, ShowGames.Plinko.RISKS[risk] + " risk", sideRight(), tableTop() + 44, DIM, false);
		g.text(font, String.format(java.util.Locale.ROOT, "pays back %.1f%%", ShowGames.Plinko.returnFor(risk) * 100),
				sideRight(), tableTop() + 55, DIM, false);
		g.text(font, "Risk", sideLeft(), tableTop() + 4, DIM, false);
		g.text(font, "Chip", sideLeft(), tableTop() + 14 + 3 * 17 + 3, DIM, false);
	}

	private void drawBall(GuiGraphicsExtractor g, Ball ball, int gap) {
		double t = (ball.age + partial) / (double) TICKS_PER_ROW;
		int row = (int) Math.floor(t);
		double f = t - row;
		int col = 0;
		for (int i = 0; i < Math.min(row, ShowGames.Plinko.ROWS); i++) {
			col += (ball.path >> i) & 1;
		}
		double bx;
		double by;
		if (row >= ShowGames.Plinko.ROWS) {
			bx = slotX(col);
			by = slotY() + 9;
		} else {
			int next = col + ((ball.path >> row) & 1);
			double xa = centreX() + (col - row / 2.0) * gap;
			double xb = centreX() + (next - (row + 1) / 2.0) * gap;
			bx = xa + (xb - xa) * f;
			int down = rowGap();
			by = boardTop() + 4 + row * down + down * f - Math.sin(f * Math.PI) * down * 0.35;
		}
		int x = (int) Math.round(bx);
		int y = (int) Math.round(by);
		g.fill(x - 3, y - 2, x + 3, y + 3, 0xFFE0302A);
		g.fill(x - 2, y - 3, x + 2, y + 4, 0xFFE0302A);
		g.fill(x - 2, y - 2, x, y, 0xFFFFA0A0);
	}

	/** Whether the owner lets players pick this risk (an old server sends nothing: all open). */
	private boolean allowed(int r) {
		long mask = table(7);
		return mask == 0 || (mask & (1L << r)) != 0;
	}

	/** Moves the picked risk to an open one if the owner closed it. */
	private void fixRisk() {
		if (!allowed(risk)) {
			for (int r = 0; r < 3; r++) {
				if (allowed(r)) {
					risk = r;
					return;
				}
			}
		}
	}

	@Override
	protected CasinoTheme theme() {
		return CasinoTheme.PLINKO;
	}
}
