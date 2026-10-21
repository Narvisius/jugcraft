package io.github.jimbozoomer.jugcraft.client;

import static io.github.jimbozoomer.jugcraft.client.ExchangeScreen.*;

import io.github.jimbozoomer.jugcraft.finance.CasinoPayload;
import io.github.jimbozoomer.jugcraft.finance.FinanceActionPayload.Action;
import io.github.jimbozoomer.jugcraft.finance.FinanceBook;
import io.github.jimbozoomer.jugcraft.finance.Slots;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * A slot machine, drawn as the cabinet itself filling most of the screen: a lit marquee with chasing bulbs (the
 * Progressive's jackpot meter in it), a chrome-framed reel window with curved, glass-fronted reels and the pay line,
 * LED meters for credits, bet and win, a button deck (bet down, bet up, max bet, spin) and a lever to pull. The
 * paytable sits on a narrow glass beside it. The server decides every spin ({@link Slots}); the reels only turn and
 * stop, one after another, where it said.
 */
public class SlotScreen extends CasinoScreen {
	private static final int[] LADDER = {1, 2, 5, 10, 20, 25, 50, 100, 200, 250, 500, 1_000, 2_000, 5_000, 10_000, 25_000,
			50_000, 100_000};

	private Slots.Machine machine = Slots.Machine.FRUIT;
	private int[] stops = new int[3];
	private long lastWin;
	/** The line bet of the spin shown (a free spin's is the bet that won it). */
	private long lastBet;
	/**
	 * The first reel that is held back for suspense (the rest of the reels stop late, one by one, glowing, with a rising
	 * chime) because the reels already stopped show two of the machine's big symbols; 99 for none.
	 */
	private int anticipateFrom = 99;
	/** A spin has started and its win (if any) is still to be celebrated when the reels and wheel stop. */
	private boolean celebratePending;
	private int lastLines;
	private boolean lastJackpot;
	private long spins = -1;
	private long jackpot;
	private boolean busy;
	private long topJackpot;
	private int wheel = -1;
	private int wild;
	private int freeSpins;
	private long freeBet;
	private long freeWon;
	private int nudges;
	private boolean holdOffered;
	private int lastHolds;
	/** Golden Dragon: the last Hold & Spin's pearls (prizes in total bets, 0 for none) and what it paid. */
	private final int[] pearls = new int[15];
	private long bonus;
	/** Ticks since the Hold & Spin began to show (after the reels stop). */
	private int bonusAge = 1000;
	/** Pub Fruit: the reels the player chose to hold for the next spin. */
	private int holdMask;
	/** Ticks since the Nether Furnace's wheel began to turn. */
	private int wheelAge = 1000;
	private long bet;
	/** Ticks since the latest spin began (the reels stop one by one). */
	private int spinAge = 1000;
	/** Ticks since the lever was pulled (it springs back). */
	private int leverAge = 1000;
	/** The win meter counts up to the win. */
	private long shownWin;
	private boolean nudgeSent;

	public SlotScreen(CasinoPayload data) {
		super(data);
		parse(true);
		bet = Math.max(1, data.minBet());
		shownWin = lastWin;
	}

	private void parse(boolean first) {
		CasinoPayload.Reader r = new CasinoPayload.Reader(data.data());
		machine = Slots.Machine.values()[Math.max(0, Math.min(Slots.Machine.values().length - 1, r.nextInt()))];
		int[] next = new int[machine.reels];
		for (int i = 0; i < machine.reels; i++) {
			next[i] = r.nextInt();
		}
		lastBet = r.next();
		lastWin = r.next();
		lastLines = r.nextInt();
		lastJackpot = r.nextBool();
		long newSpins = r.next();
		jackpot = r.next();
		busy = r.nextBool();
		topJackpot = r.next();
		int newWheel = r.nextInt();
		wild = r.nextInt();
		freeSpins = r.nextInt();
		freeBet = r.next();
		freeWon = r.next();
		nudges = r.nextInt();
		boolean offered = r.nextBool();
		lastHolds = r.nextInt();
		bonus = r.next();
		for (int i = 0; i < 15; i++) {
			pearls[i] = r.nextInt();
		}
		if (!offered) {
			holdMask = 0;
		}
		holdOffered = offered;
		if (!first && newSpins != spins && spins >= 0) {
			// A nudge moves one reel a step: no full spin of the reels for that.
			anticipateFrom = nudgeSent ? 99 : anticipation(next);
			spinAge = nudgeSent ? 1000 : 0;
			shownWin = nudgeSent ? lastWin : 0;
			wheelAge = newWheel >= 0 ? -stopTick(machine.reels - 1) : 1000;
			bonusAge = -stopTick(machine.reels - 1);
			celebratePending = true;
			if (!nudgeSent) {
				CasinoSound.lever(data.pos());
			}
		}
		nudgeSent = false;
		wheel = newWheel;
		spins = newSpins;
		stops = next;
	}

	@Override
	protected boolean needsRebuild(CasinoPayload before, CasinoPayload after) {
		parse(false);
		if (bet < data.minBet() || bet > data.maxBet()) {
			bet = Math.max(1, data.minBet());
		}
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		spinAge++;
		leverAge++;
		wheelAge++;
		bonusAge++;
		if (bonus > 0 && bonusAge >= 0 && bonusAge < bonusLength() && bonusAge % 6 == 0 && bonusAge / 6 < pearlOrder().length) {
			// Each pearl lands with a chime, higher as they fill the grid.
			CasinoSound.anticipation(data.pos(), 0.9F + (bonusAge / 6) * 0.08F);
		}
		long pos = data.pos();
		if (spinning()) {
			// The reels whirring, each one clunking to a stop, and the suspense chime rising while the last ones decide.
			if (spinAge % 2 == 0) {
				CasinoSound.tick(pos, 1.7F);
			}
			for (int reel = 0; reel < machine.reels; reel++) {
				if (spinAge == stopTick(reel)) {
					CasinoSound.reelStop(pos, reel);
				}
			}
			if (anticipating() && spinAge % 4 == 0) {
				int since = spinAge - stopTick(anticipateFrom - 1);
				CasinoSound.anticipation(pos, Math.min(2.0F, 0.8F + since * 0.025F));
			}
		}
		if (celebratePending && !revealing()) {
			celebratePending = false;
			celebrate(lastWin, lastBet * machine.lines, lastJackpot);
		}
		if (!spinning() && shownWin < lastWin) {
			// Count the win up over about a second, a coin sound with each step.
			shownWin = Math.min(lastWin, shownWin + Math.max(1, lastWin / 20));
			if (ticks % 2 == 0) {
				CasinoSound.coin(pos, 1.0F + (float) shownWin / Math.max(1, lastWin) * 0.8F);
			}
		}
	}

	@Override
	protected boolean ownCelebration() {
		return true;
	}

	private int stopTick(int reel) {
		int base = 14 + reel * (machine.reels == 5 ? 4 : 6);
		return reel >= anticipateFrom ? base + 26 * (reel - anticipateFrom + 1) : base;
	}

	/** The reels before the held-back ones have stopped and the held-back ones are still turning. */
	private boolean anticipating() {
		return anticipateFrom < machine.reels && spinAge >= stopTick(anticipateFrom - 1) && spinAge < stopTick(machine.reels - 1);
	}

	/** The machine's big symbol: the one whose line (or scatter) the suspense is for. */
	private int bigSymbol() {
		return switch (machine) {
			case FRUIT, PUB -> Slots.SEVEN;
			case CLOCKWORK -> Slots.CLOCK;
			case GEM -> Slots.DIAMOND;
			case PROGRESSIVE -> Slots.JACKPOT;
			case NETHER -> Slots.NETHERITE;
			case TREASURE -> Slots.CHEST;
			case CREEPER -> Slots.SWORD;
			case SHARK -> Slots.DIVER;
			case DRAGON -> Slots.PEARL;
		};
	}

	/**
	 * Which reel to start holding back: on three reels, the last one when the first two show the big symbol on the
	 * line; on five, the last two when the first three show two or more of it anywhere. The reels are already decided,
	 * so this only changes how long the last ones take to stop.
	 */
	private int anticipation(int[] next) {
		int big = bigSymbol();
		if (machine.reels == 3) {
			int a = Slots.symbolAt(machine, 0, next[0], machine.rows / 2);
			int b = Slots.symbolAt(machine, 1, next[1], machine.rows / 2);
			boolean wild = machine == Slots.Machine.CLOCKWORK;
			boolean hit = (a == big || (wild && a == Slots.CLOCK)) && (b == big || (wild && b == Slots.CLOCK));
			return hit ? 2 : 99;
		}
		int count = 0;
		for (int reel = 0; reel < 3; reel++) {
			int[] strip = Slots.strip(machine, reel);
			for (int row = 0; row < 3; row++) {
				if (strip[Math.floorMod(next[reel] + row - 1, strip.length)] == big) {
					count++;
				}
			}
		}
		// The Dragon needs a pearl on every reel: hold the last two back when the first three all show one.
		return count >= (machine == Slots.Machine.DRAGON ? 3 : 2) ? 3 : 99;
	}

	private boolean spinning() {
		return spinAge < stopTick(machine.reels - 1) + 1;
	}

	/** The reels (and the Nether Furnace's wheel) are still turning: the win stays hidden until they stop. */
	@Override
	protected boolean revealing() {
		return spinning() || (machine == Slots.Machine.NETHER && wheel >= 0 && wheelAge < 80) || (bonus > 0 && bonusAge < bonusLength());
	}

	/** The Hold & Spin's pearls in the order they appear: the five that started it, then the rest. */
	private int[] pearlOrder() {
		int[] out = new int[15];
		int n = 0;
		for (int i = 0; i < 15; i++) {
			if (pearls[i] > 0 && i % 3 == 1) {
				out[n++] = i;
			}
		}
		for (int i = 0; i < 15; i++) {
			if (pearls[i] > 0 && i % 3 != 1) {
				out[n++] = i;
			}
		}
		return java.util.Arrays.copyOf(out, n);
	}

	private int bonusLength() {
		return pearlOrder().length * 6 + 50;
	}

	private boolean canSpin() {
		return !spinning() && !busy && (freeSpins > 0 || bet * machine.lines <= data.cash());
	}

	private int[][] lines() {
		return machine == Slots.Machine.GEM ? Slots.GEM_LINES : Slots.LINES_20;
	}

	private boolean manyLines() {
		return machine.lines > 1;
	}

	/** Pub Fruit's per-reel button (hold or nudge), along the bottom of the reel window. */
	private int[] reelButton(int reel) {
		int rx = windowX() + 2 + reel * (reelW() + 4);
		int y1 = windowY() + windowH() - 3;
		return new int[] {rx + 3, y1 - 13, rx + reelW() - 3, y1 - 1};
	}

	private boolean pubButtons() {
		return machine == Slots.Machine.PUB && !spinning() && !busy && (nudges > 0 || holdOffered);
	}

	@Override
	protected int preferredWidth() {
		return 470;
	}

	@Override
	protected int preferredHeight() {
		return 310;
	}

	@Override
	protected void initGame() {
		// Every control on the machine is drawn as part of the cabinet and handled in mouseClicked.
	}

	// ------------------------------------------------------------------ colours per cabinet

	private int body() {
		return switch (machine) {
			case FRUIT -> 0xFFB3261E;
			case CLOCKWORK -> 0xFF4A301A;
			case GEM -> 0xFF34343A;
			case PROGRESSIVE -> 0xFF4A1E78;
			case NETHER -> 0xFF2A2026;
			case TREASURE -> 0xFF2E6A62;
			case CREEPER -> 0xFF2E7A2A;
			case SHARK -> 0xFF0E2A5A;
			case DRAGON -> 0xFFA81414;
			case PUB -> 0xFF8A6238;
		};
	}

	private int bodyDark() {
		return switch (machine) {
			case FRUIT -> 0xFF7A1612;
			case CLOCKWORK -> 0xFF2E1D0F;
			case GEM -> 0xFF222226;
			case PROGRESSIVE -> 0xFF2C0E4A;
			case NETHER -> 0xFF140C10;
			case TREASURE -> 0xFF1C4440;
			case CREEPER -> 0xFF184A16;
			case SHARK -> 0xFF06163A;
			case DRAGON -> 0xFF5A0808;
			case PUB -> 0xFF5A3E20;
		};
	}

	private int bodyLight() {
		return switch (machine) {
			case FRUIT -> 0xFFD8473C;
			case CLOCKWORK -> 0xFF6A4A2A;
			case GEM -> 0xFF4C4C54;
			case PROGRESSIVE -> 0xFF6E34A8;
			case NETHER -> 0xFF4A2A30;
			case TREASURE -> 0xFF4A9A8A;
			case CREEPER -> 0xFF4CB03C;
			case SHARK -> 0xFF2A5A9A;
			case DRAGON -> 0xFFD8402A;
			case PUB -> 0xFFB08450;
		};
	}

	private int trim() {
		return switch (machine) {
			case FRUIT -> 0xFFD6DAE0;
			case CLOCKWORK -> GOLD;
			case GEM -> 0xFF8A6238;
			case PROGRESSIVE -> 0xFFD8DCE4;
			case NETHER -> 0xFFE05A14;
			case TREASURE -> 0xFF1C4440;
			case CREEPER -> 0xFF1C1C1E;
			case SHARK -> 0xFF8A929C;
			case DRAGON -> 0xFFE8C463;
			case PUB -> 0xFFD6DAE0;
		};
	}

	private int trimDark() {
		return switch (machine) {
			case FRUIT -> 0xFF80868E;
			case CLOCKWORK -> 0xFF8A6A2C;
			case GEM -> 0xFF4E3620;
			case PROGRESSIVE -> 0xFF80868E;
			case NETHER -> 0xFF7A2A10;
			case TREASURE -> 0xFF0E2422;
			case CREEPER -> 0xFF0A0A0A;
			case SHARK -> 0xFF3A424C;
			case DRAGON -> 0xFF8A6A2C;
			case PUB -> 0xFF80868E;
		};
	}

	private int bulbOn() {
		return switch (machine) {
			case FRUIT -> 0xFFFFE070;
			case CLOCKWORK -> 0xFFFFD27A;
			case GEM -> 0xFF7CF2F6;
			case PROGRESSIVE -> 0xFFFF7AF0;
			case NETHER -> 0xFFFFA030;
			case TREASURE -> 0xFFD8F0E8;
			case CREEPER -> 0xFF7CFF6A;
			case SHARK -> 0xFF3AB8FF;
			case DRAGON -> 0xFFFFD840;
			case PUB -> 0xFFFFD840;
		};
	}

	private int bulbOff() {
		return switch (machine) {
			case PROGRESSIVE -> 0xFF6A2A70;
			case GEM -> 0xFF2A5A5C;
			default -> 0xFF7A5A20;
		};
	}

	// ------------------------------------------------------------------ layout

	/** The narrow paytable glass on the right. */
	private int sideW() {
		return Math.max(96, panelWidth / 5);
	}

	private int cabX0() {
		return left + FRAME + 8;
	}

	private int cabX1() {
		return left + panelWidth - FRAME - 10 - sideW() - 10;
	}

	private int cabY0() {
		return tableTop() - 2;
	}

	private int cabY1() {
		return tableBottom() + 2;
	}

	private int marqueeH() {
		return machine == Slots.Machine.PROGRESSIVE ? 40 : 28;
	}

	/** Room on the cabinet's right for the lever. */
	private static final int LEVER_ROOM = 20;

	private int deckH() {
		return 24;
	}

	private int metersH() {
		return 22;
	}

	private int symbolSize() {
		int width = cabX1() - cabX0() - LEVER_ROOM - 36;
		int byWidth = width / machine.reels - 4;
		int height = cabY1() - cabY0() - marqueeH() - metersH() - deckH() - 30;
		int byHeight = height / 3;
		return Math.max(20, Math.min(56, Math.min(byWidth, byHeight)));
	}

	/** Reels are wider than a symbol, like a real machine's drums. */
	private int reelW() {
		int width = cabX1() - cabX0() - LEVER_ROOM - 36;
		return Math.max(symbolSize(), Math.min(width / machine.reels - 4, symbolSize() * 3 / 2));
	}

	private int windowW() {
		return machine.reels * (reelW() + 4) + 4;
	}

	private int windowH() {
		return 3 * symbolSize() + 6;
	}

	private int windowX() {
		int inner0 = cabX0() + 10;
		int inner1 = cabX1() - LEVER_ROOM - 6;
		return (inner0 + inner1) / 2 - windowW() / 2;
	}

	private int windowY() {
		return cabY0() + marqueeH() + 10;
	}

	private int metersY() {
		return windowY() + windowH() + 8;
	}

	private int deckY() {
		return metersY() + metersH() + 4;
	}

	/** The deck buttons: bet down, bet up, max bet, spin. */
	private int[] button(int index) {
		int x0 = cabX0() + 12;
		int x1 = cabX1() - LEVER_ROOM - 8;
		int w = x1 - x0;
		int y = deckY() + 4;
		int h = deckH() - 8;
		int spinW = Math.max(56, w / 3);
		int small = (w - spinW - 12) / 3;
		return switch (index) {
			case 0 -> new int[] {x0, y, x0 + small, y + h};
			case 1 -> new int[] {x0 + small + 4, y, x0 + 2 * small + 4, y + h};
			case 2 -> new int[] {x0 + 2 * small + 8, y, x0 + 3 * small + 8, y + h};
			default -> new int[] {x1 - spinW, y - 1, x1, y + h + 1};
		};
	}

	private int leverX() {
		return cabX1() + 2;
	}

	private int leverTop() {
		return windowY() - 2;
	}

	private int leverBottom() {
		return windowY() + windowH() - 6;
	}

	private static boolean inside(int[] r, double x, double y) {
		return x >= r[0] && x < r[2] && y >= r[1] && y < r[3];
	}

	// ------------------------------------------------------------------ input

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (settingsOpen()) {
			return super.mouseClicked(event, doubleClick);
		}
		if (event.button() != com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
			return super.mouseClicked(event, doubleClick);
		}
		double x = event.x();
		double y = event.y();
		if (pubButtons()) {
			for (int reel = 0; reel < 3; reel++) {
				if (inside(reelButton(reel), x, y)) {
					if (nudges > 0) {
						nudgeSent = true;
						send(Action.CASINO_ACT, 1, String.valueOf(reel), 0);
					} else {
						holdMask ^= 1 << reel;
					}
					return true;
				}
			}
		}
		if (inside(button(0), x, y) && !spinning()) {
			bet = step(-1);
			return true;
		}
		if (inside(button(1), x, y) && !spinning()) {
			bet = step(1);
			return true;
		}
		if (inside(button(2), x, y) && !spinning()) {
			bet = data.maxBet();
			return true;
		}
		boolean lever = x >= leverX() - 4 && x < leverX() + 16 && y >= leverTop() - 8 && y < leverBottom() + 8;
		if ((inside(button(3), x, y) || lever) && canSpin()) {
			if (lever) {
				leverAge = 0;
			}
			send(Action.CASINO_ACT, 0, String.valueOf(holdOffered ? holdMask : 0), bet);
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	/** The next bet up or down the ladder, within the machine's limits. */
	private long step(int direction) {
		long out = bet;
		if (direction > 0) {
			for (long v : LADDER) {
				if (v > bet) {
					out = v;
					break;
				}
			}
		} else {
			for (int i = LADDER.length - 1; i >= 0; i--) {
				if (LADDER[i] < bet) {
					out = LADDER[i];
					break;
				}
			}
		}
		return Math.max(data.minBet(), Math.min(data.maxBet(), out));
	}

	// ------------------------------------------------------------------ the cabinet

	@Override
	protected void drawTableBack(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		int x0 = cabX0();
		int x1 = cabX1();
		int y0 = cabY0();
		int y1 = cabY1();
		// Shadow, chrome edge and body, with a lighter face toward the middle like a rounded cabinet.
		g.fill(x0 + 3, y0 + 4, x1 + 3, y1 + 3, 0x66000000);
		g.fill(x0, y0 + 6, x1, y1, trimDark());
		g.fill(x0 + 1, y0 + 7, x1 - 1, y1 - 1, trim());
		g.fill(x0 + 3, y0 + 9, x1 - 3, y1 - 3, bodyDark());
		g.fill(x0 + 6, y0 + 9, x1 - 6, y1 - 3, body());
		g.fill(x0 + 14, y0 + 9, x1 - 14, y1 - 3, bodyLight());
		g.fill(x0 + 22, y0 + 9, x1 - 22, y1 - 3, body());
		// The marquee: an arched sign along the top, rimmed with chasing bulbs.
		int my0 = y0;
		int my1 = y0 + marqueeH();
		archedBox(g, x0 + 4, my0, x1 - 4, my1, trim());
		archedBox(g, x0 + 6, my0 + 2, x1 - 6, my1 - 2, 0xFF140A08);
		bulbs(g, x0 + 8, my0 + 4, x1 - 8, my1 - 4);
		// The reel window: chrome bezel, a dark surround and the reels behind glass.
		int wx = windowX();
		int wy = windowY();
		int ww = windowW();
		int wh = windowH();
		g.fill(wx - 7, wy - 7, wx + ww + 7, wy + wh + 7, trimDark());
		g.fill(wx - 6, wy - 6, wx + ww + 6, wy + wh + 6, trim());
		g.fill(wx - 4, wy - 4, wx + ww + 4, wy + wh + 4, 0xFF4A4E56);
		g.fill(wx - 3, wy - 3, wx + ww + 3, wy + wh + 3, 0xFF0C0A0A);
		// Meters panel and the button deck.
		int mx0 = x0 + 10;
		int mx1 = x1 - LEVER_ROOM - 6;
		g.fill(mx0 - 1, metersY() - 1, mx1 + 1, metersY() + metersH() + 1, trimDark());
		g.fill(mx0, metersY(), mx1, metersY() + metersH(), 0xFF141414);
		g.fill(x0 + 4, deckY(), x1 - LEVER_ROOM, deckY() + deckH(), trimDark());
		g.fill(x0 + 5, deckY() + 1, x1 - LEVER_ROOM - 1, deckY() + deckH() - 1, bodyDark());
		g.fill(x0 + 5, deckY() + 1, x1 - LEVER_ROOM - 1, deckY() + 3, trim());
		// Per-machine dressing.
		switch (machine) {
			case CLOCKWORK -> {
				gear(g, x0 + 7, y1 - 22, 8);
				gear(g, x0 + 9, wy + wh / 2, 6);
				gear(g, x1 - LEVER_ROOM - 2, y1 - 14, 6);
			}
			case GEM -> {
				// Mine timbers up the sides and across the top of the window.
				g.fill(x0 + 3, y0 + 9, x0 + 9, y1 - 3, 0xFF6B4A2A);
				g.fill(x1 - LEVER_ROOM - 3, y0 + 9, x1 - LEVER_ROOM + 3, y1 - 3, 0xFF6B4A2A);
				g.fill(wx - 10, wy - 12, wx + ww + 10, wy - 8, 0xFF6B4A2A);
				for (int k = 0; k < 6; k++) {
					int gx = x0 + 12 + (k * 37) % Math.max(1, (x1 - x0 - 30));
					int gy = y1 - 10 - (k * 7) % 6;
					g.fill(gx, gy, gx + 3, gy + 2, k % 2 == 0 ? 0xFF5CE0E6 : 0xFF2EC85A);
				}
			}
			default -> {
			}
		}
		// The lever's mount on the right side.
		int lx = leverX();
		g.fill(lx - 2, leverBottom() - 6, lx + 10, leverBottom() + 10, trimDark());
		g.fill(lx - 1, leverBottom() - 5, lx + 9, leverBottom() + 9, trim());
		// The paytable glass.
		int px0 = left + panelWidth - FRAME - 8 - sideW();
		int px1 = left + panelWidth - FRAME - 8;
		g.fill(px0, y0 + 6, px1, y1, GOLD_DIM);
		g.fill(px0 + 1, y0 + 7, px1 - 1, y1 - 1, 0xFF120C08);
		g.fill(px0 + 3, y0 + 9, px1 - 3, y1 - 3, PLAQUE);
	}

	/** A box with its top corners rounded off. */
	private static void archedBox(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int colour) {
		int r = Math.min(8, (y1 - y0) / 2);
		for (int i = 0; i < r; i++) {
			double t = 1 - Math.sqrt(1 - Math.pow((r - i) / (double) r, 2));
			int inset = (int) Math.round(t * r);
			g.fill(x0 + inset, y0 + i, x1 - inset, y0 + i + 1, colour);
		}
		g.fill(x0, y0 + r, x1, y1, colour);
	}

	private void bulbs(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1) {
		boolean celebrate = !spinning() && lastWin > 0;
		int speed = celebrate ? 2 : 6;
		int phase = ticks / speed;
		int i = 0;
		for (int x = x0 + 4; x < x1 - 4; x += 7) {
			bulb(g, x, y0, (i + phase) % 3 == 0 || (celebrate && lastJackpot && ticks % 4 < 2));
			bulb(g, x, y1 - 3, (i + phase + 1) % 3 == 0 || (celebrate && lastJackpot && ticks % 4 < 2));
			i++;
		}
		for (int y = y0 + 7; y < y1 - 7; y += 7) {
			bulb(g, x0, y, (i + phase) % 3 == 0);
			bulb(g, x1 - 3, y, (i + phase + 2) % 3 == 0);
			i++;
		}
	}

	private void bulb(GuiGraphicsExtractor g, int x, int y, boolean on) {
		g.fill(x, y, x + 3, y + 3, on ? bulbOn() : bulbOff());
		if (on) {
			g.fill(x, y, x + 1, y + 1, 0xFFFFFFFF);
		}
	}

	private void gear(GuiGraphicsExtractor g, int cx, int cy, int r) {
		double turn = (ticks + partial) * 0.05;
		for (int i = 0; i < 8; i++) {
			double a = turn + i * Math.PI / 4;
			int tx = cx + (int) Math.round(Math.cos(a) * r);
			int ty = cy + (int) Math.round(Math.sin(a) * r);
			g.fill(tx - 2, ty - 2, tx + 2, ty + 2, GOLD);
		}
		oval(g, cx, cy, r, r, GOLD);
		oval(g, cx, cy, r - 3, r - 3, 0xFFB8913F);
		oval(g, cx, cy, 2, 2, WALNUT_DARK);
	}

	// ------------------------------------------------------------------ the face

	@Override
	protected void drawTable(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		drawMarquee(g);
		drawReels(g);
		drawMeters(g);
		drawDeck(g, mouseX, mouseY);
		drawLever(g, mouseX, mouseY);
		drawPaytable(g);
	}

	private void drawMarquee(GuiGraphicsExtractor g) {
		int x0 = cabX0();
		int x1 = cabX1();
		int cx = (x0 + x1) / 2;
		int y = cabY0();
		String title = machine.title.toUpperCase(java.util.Locale.ROOT);
		if (machine == Slots.Machine.PROGRESSIVE) {
			g.text(font, title, cx - font.width(title) / 2, y + 7, 0xFFFF9AF4, true);
			String meter = FinanceBook.amount(jackpot);
			int w = Math.max(80, font.width(meter) + 20);
			g.fill(cx - w / 2, y + 18, cx + w / 2, y + 33, 0xFF2A0A0A);
			g.fill(cx - w / 2 + 1, y + 19, cx + w / 2 - 1, y + 32, 0xFF100404);
			int colour = (ticks / 8) % 2 == 0 ? 0xFFFF3A2A : 0xFFFF6A3A;
			g.text(font, meter, cx - font.width(meter) / 2, y + 22, colour, true);
		} else {
			g.text(font, title, cx - font.width(title) / 2, y + marqueeH() / 2 - 4, bulbOn(), true);
		}
	}

	private void drawReels(GuiGraphicsExtractor g) {
		int s = symbolSize();
		int wx = windowX();
		int wy = windowY();
		boolean win = !spinning() && lastWin > 0;
		int rw = reelW();
		for (int reel = 0; reel < machine.reels; reel++) {
			int rx = wx + 2 + reel * (rw + 4);
			int[] strip = Slots.strip(machine, reel);
			boolean turning = spinning() && spinAge < stopTick(reel);
			int base = stops[reel];
			if (turning) {
				int remaining = stopTick(reel) - spinAge;
				base = Math.floorMod(stops[reel] - (remaining * remaining) / 2 - remaining, strip.length);
			}
			// A reel held back for suspense glows round its edge, pulsing faster as it slows.
			if (turning && anticipating() && reel >= anticipateFrom) {
				int pulse = (ticks / 2) % 2 == 0 ? 0xFFFFE070 : 0xFFFF8A2A;
				g.fill(rx - 2, wy + 1, rx + rw + 2, wy + 5 + 3 * s, pulse);
			}
			// The reel's face, lighter in the middle like a drum.
			g.fill(rx, wy + 3, rx + rw, wy + 3 + 3 * s, 0xFFE6DECA);
			g.fill(rx + rw / 8, wy + 3, rx + rw - rw / 8, wy + 3 + 3 * s, 0xFFF0EADA);
			g.fill(rx + rw / 4, wy + 3, rx + rw - rw / 4, wy + 3 + 3 * s, 0xFFF8F4EA);
			for (int row = 0; row < 3; row++) {
				int sy = wy + 3 + row * s;
				int symbol = strip[Math.floorMod(base + row - 1, strip.length)];
				boolean lit = win && !manyLines() && row == 1;
				if (lit && (ticks / 5) % 2 == 0) {
					g.fill(rx, sy, rx + rw, sy + s, 0xFFFFF0B0);
				}
				symbol(g, rx + (rw - s) / 2, sy, s, symbol);
				if (turning) {
					// Motion streaks.
					for (int k = 0; k < 3; k++) {
						int ly = sy + (k * s) / 3 + (ticks * 3) % Math.max(1, s / 3);
						g.fill(rx + 1, ly, rx + rw - 1, ly + 1, 0x55FFFFFF);
					}
				}
			}
			// The drum's curve: darker toward the top and bottom.
			for (int k = 0; k < 6; k++) {
				int alpha = (6 - k) * 0x12;
				int a = alpha << 24;
				g.fill(rx, wy + 3 + k * 2, rx + rw, wy + 5 + k * 2, a);
				g.fill(rx, wy + 3 + 3 * s - 2 - k * 2, rx + rw, wy + 3 + 3 * s - k * 2, a);
			}
			// The dividers between reels.
			if (reel < machine.reels - 1) {
				g.fill(rx + rw, wy + 2, rx + rw + 4, wy + windowH() - 2, 0xFF0C0A0A);
				g.fill(rx + rw + 1, wy + 2, rx + rw + 3, wy + windowH() - 2, trimDark());
			}
		}
		// The glass: a soft glare across the top of the window.
		g.fill(wx, wy + 2, wx + windowW(), wy + 4, 0x33FFFFFF);
		g.fill(wx + 4, wy + 6, wx + windowW() / 3, wy + 7, 0x22FFFFFF);
		// The pay line(s).
		if (manyLines()) {
			if (!spinning() && lastLines != 0) {
				for (int i = 0; i < machine.lines; i++) {
					if ((lastLines & (1 << i)) != 0) {
						drawLine(g, lines()[i], i);
					}
				}
			}
			for (int i = 0; i < 3; i++) {
				int ly = wy + 3 + i * s + s / 2;
				g.fill(wx - 6, ly - 1, wx - 3, ly + 2, 0xFFD03030);
				g.fill(wx + windowW() + 3, ly - 1, wx + windowW() + 6, ly + 2, 0xFFD03030);
			}
		} else {
			int ly = wy + 3 + s + s / 2;
			g.fill(wx - 6, ly - 2, wx - 2, ly + 2, 0xFFD03030);
			g.fill(wx + windowW() + 2, ly - 2, wx + windowW() + 6, ly + 2, 0xFFD03030);
			g.fill(wx, ly, wx + windowW(), ly + 1, win ? 0xCCFFD050 : 0x88D03030);
		}
		if (busy) {
			String text = (data.text().isEmpty() ? "Someone" : data.text().get(0)) + " is playing";
			int w = font.width(text) + 12;
			int cx = wx + windowW() / 2;
			int cy = wy + windowH() / 2;
			g.fill(cx - w / 2, cy - 8, cx + w / 2, cy + 8, 0xDD100808);
			g.text(font, text, cx - font.width(text) / 2, cy - 4, LOSS, false);
		}
		if (!spinning() && wild != 0) {
			for (int reel = 1; reel <= 3; reel++) {
				if ((wild & (1 << reel)) != 0) {
					int rx = wx + 2 + reel * (reelW() + 4);
					boolean flash = (ticks / 4) % 2 == 0;
					boolean shark = machine == Slots.Machine.SHARK;
					g.fill(rx, wy + 3, rx + reelW(), wy + 3 + 3 * s, shark ? (flash ? 0x663A8EE0 : 0x441A4A90)
							: (flash ? 0x664CB03C : 0x442A6A22));
					String text = shark ? "FRENZY" : "WILD";
					g.text(font, text, rx + reelW() / 2 - font.width(text) / 2, wy + windowH() / 2 - 4, shark ? 0xFF7AD8FF : 0xFF7CFF6A,
							true);
				}
			}
		}
		if (bonus > 0 && bonusAge >= 0 && !spinning()) {
			drawHoldAndSpin(g, wx, wy, s);
		}
		if (pubButtons()) {
			for (int reel = 0; reel < 3; reel++) {
				int[] r = reelButton(reel);
				boolean held = (holdMask & (1 << reel)) != 0;
				int face = nudges > 0 ? ((ticks / 5) % 2 == 0 ? 0xFFE0A020 : 0xFFC08010) : held ? 0xFFFFD840 : 0xFF6A5A20;
				g.fill(r[0], r[1], r[2], r[3], 0xFF0A0A0A);
				g.fill(r[0] + 1, r[1] + 1, r[2] - 1, r[3] - 1, face);
				String label = nudges > 0 ? "NUDGE" : held ? "HELD" : "HOLD";
				g.text(font, label, (r[0] + r[2]) / 2 - font.width(label) / 2, r[1] + 3, 0xFF1A0A0A, false);
			}
			String note = nudges > 0 ? nudges + " NUDGE" + (nudges > 1 ? "S" : "") + " - tap a reel" : "HOLD reels, then spin";
			int nw = font.width(note) + 10;
			g.fill(wx + windowW() / 2 - nw / 2, wy - 1, wx + windowW() / 2 + nw / 2, wy + 10, 0xEE1A0A0A);
			g.text(font, note, wx + windowW() / 2 - font.width(note) / 2, wy + 1, 0xFFFFD840, false);
		} else if (machine == Slots.Machine.PUB && !spinning() && lastHolds != 0) {
			for (int reel = 0; reel < 3; reel++) {
				if ((lastHolds & (1 << reel)) != 0) {
					int[] r = reelButton(reel);
					g.text(font, "HELD", (r[0] + r[2]) / 2 - font.width("HELD") / 2, r[1] + 3, 0xFFFFD840, true);
				}
			}
		}
		if (freeSpins > 0 && !spinning()) {
			String text = freeSpins + " FREE SPINS · 3x · won " + FinanceBook.amount(freeWon);
			int w = font.width(text) + 12;
			g.fill(wx + windowW() / 2 - w / 2, wy - 1, wx + windowW() / 2 + w / 2, wy + 10, 0xEE0C1C22);
			g.text(font, text, wx + windowW() / 2 - font.width(text) / 2, wy + 1, (ticks / 6) % 2 == 0 ? 0xFFFFD840 : 0xFFD8F0E8,
					false);
		}
		if (machine == Slots.Machine.NETHER && wheel >= 0 && wheelAge >= 0 && wheelAge < 140) {
			drawWheel(g, wx + windowW() / 2, wy + windowH() / 2, Math.min(windowH() / 2 - 4, 46));
		}
		if (!spinning() && lastJackpot) {
			String text = "JACKPOT!";
			int cx = wx + windowW() / 2;
			int cy = wy + windowH() / 2;
			g.fill(cx - 34, cy - 9, cx + 34, cy + 9, 0xEE1A0A1A);
			g.text(font, text, cx - font.width(text) / 2, cy - 4, (ticks / 3) % 2 == 0 ? GOLD : 0xFFFF3A2A, true);
		}
	}

	/** The Nether Furnace's prize wheel: it spins down and stops on the segment the server drew. */
	private void drawWheel(GuiGraphicsExtractor g, int cx, int cy, int r) {
		int n = Slots.WHEEL.length;
		double t = Math.min(1.0, (wheelAge + partial) / 80.0);
		double ease = 1 - Math.pow(1 - t, 3);
		// Segment k sits at angle k/n of the turn; the pointer is at the top.
		double target = -Math.PI / 2 - (wheel + 0.5) * 2 * Math.PI / n;
		double angle = target - (1 - ease) * Math.PI * 2 * 4;
		int[] colours = {0xFFE05A14, 0xFFF2A020, 0xFFC0302A, 0xFFE8C463, 0xFF8A2A6A, 0xFFF2A020, 0xFFE05A14, 0xFFE0E0D0};
		oval(g, cx, cy, r + 3, r + 3, 0xFF1A0A08);
		for (int dy = -r; dy <= r; dy++) {
			int runStart = Integer.MIN_VALUE;
			int runColour = 0;
			for (int dx = -r; dx <= r + 1; dx++) {
				int colour = 0;
				if (dx <= r && dx * dx + dy * dy <= r * r) {
					double a = Math.atan2(dy + 0.5, dx + 0.5) - angle;
					a = ((a % (Math.PI * 2)) + Math.PI * 2) % (Math.PI * 2);
					colour = colours[(int) (a / (Math.PI * 2) * n) % n];
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
		for (int k = 0; k < n; k++) {
			double a = angle + (k + 0.5) * 2 * Math.PI / n;
			String label = Slots.WHEEL[k] + "x";
			int lx = cx + (int) Math.round(Math.cos(a) * r * 0.65);
			int ly = cy + (int) Math.round(Math.sin(a) * r * 0.65);
			g.text(font, label, lx - font.width(label) / 2, ly - 4, 0xFF1A0A08, false);
		}
		oval(g, cx, cy, 5, 5, GOLD);
		g.fill(cx - 3, cy - r - 6, cx + 4, cy - r + 1, GOLD);
		g.fill(cx - 1, cy - r + 1, cx + 2, cy - r + 4, GOLD);
		if (t >= 1) {
			String text = Slots.WHEEL[wheel] + "x THE BET!";
			int w = font.width(text) + 10;
			g.fill(cx - w / 2, cy + r - 2, cx + w / 2, cy + r + 10, 0xEE1A0A08);
			g.text(font, text, cx - w / 2 + 5, cy + r, (ticks / 4) % 2 == 0 ? GOLD : 0xFFFFA030, false);
		}
	}

	private void drawLine(GuiGraphicsExtractor g, int[] rows, int index) {
		int[] colours = {0xFFFF5050, 0xFF50C0FF, 0xFF70E070, 0xFFFFD050, 0xFFE070FF, 0xFFFF9040, 0xFF40E0C0, 0xFFC0C0FF,
				0xFFFFFFFF};
		int colour = colours[index % colours.length];
		int s = symbolSize();
		int wx = windowX();
		int wy = windowY();
		for (int reel = 0; reel < 5; reel++) {
			int cx = wx + 2 + reel * (reelW() + 4) + reelW() / 2;
			int cy = wy + 3 + rows[reel] * s + s / 2;
			g.fill(cx - 2, cy - 2, cx + 3, cy + 3, colour);
			if (reel < 4) {
				int nx = wx + 2 + (reel + 1) * (reelW() + 4) + reelW() / 2;
				int ny = wy + 3 + rows[reel + 1] * s + s / 2;
				int steps = Math.max(Math.abs(nx - cx), Math.abs(ny - cy));
				for (int k = 0; k <= steps; k++) {
					int px = cx + (nx - cx) * k / steps;
					int py = cy + (ny - cy) * k / steps;
					g.fill(px, py, px + 2, py + 2, colour);
				}
			}
		}
	}

	private void drawMeters(GuiGraphicsExtractor g) {
		int x0 = cabX0() + 10;
		int x1 = cabX1() - LEVER_ROOM - 6;
		int w = (x1 - x0) / 3;
		String betText = FinanceBook.amount(bet * machine.lines);
		meter(g, x0, metersY(), w, "CREDITS", FinanceBook.amount(shownCash()), 0xFFFF4A2A);
		meter(g, x0 + w, metersY(), w, freeSpins > 0 ? "FREE SPINS" : machine.lines > 1 ? "BET (" + shortAmount(bet) + "x"
				+ machine.lines + ")" : "BET", freeSpins > 0 ? String.valueOf(freeSpins) : betText, 0xFFFFC23A);
		boolean flash = !spinning() && lastWin > 0 && (ticks / 6) % 2 == 0;
		meter(g, x0 + 2 * w, metersY(), x1 - x0 - 2 * w, "WIN", spinning() ? "" : FinanceBook.amount(shownWin),
				flash ? 0xFFFFFFFF : 0xFF5AFF6A);
	}

	private void meter(GuiGraphicsExtractor g, int x, int y, int w, String label, String value, int colour) {
		g.fill(x + 2, y + 2, x + w - 2, y + metersH() - 2, 0xFF050505);
		g.text(font, font.plainSubstrByWidth(label, w - 6), x + 4, y + 3, 0xFF8E8370, false);
		String v = font.plainSubstrByWidth(value, w - 8);
		g.text(font, v, x + w - 4 - font.width(v), y + 12, colour, false);
	}

	private void drawDeck(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		String[] labels = {"BET -", "BET +", "MAX BET", "SPIN"};
		int[] faces = {0xFF2A6AB0, 0xFF2A6AB0, 0xFFC08A20, 0xFF2E9A40};
		for (int i = 0; i < 4; i++) {
			int[] r = button(i);
			boolean enabled = i == 3 ? canSpin() : !spinning();
			boolean hot = enabled && inside(r, mouseX, mouseY);
			int face = enabled ? faces[i] : 0xFF4A4A4A;
			if (i == 3 && enabled && !hot && (ticks / 10) % 2 == 0) {
				face = 0xFF3CB84E;
			}
			g.fill(r[0], r[1] + 2, r[2], r[3] + 2, 0xFF0A0A0A);
			g.fill(r[0], r[1], r[2], r[3], hot ? lighten(face) : face);
			g.fill(r[0] + 1, r[1] + 1, r[2] - 1, r[1] + 3, 0x55FFFFFF);
			g.fill(r[0], r[3] - 2, r[2], r[3], 0x44000000);
			String label = i == 3 && freeSpins > 0 ? "FREE (" + freeSpins + ")" : labels[i];
			g.text(font, label, (r[0] + r[2]) / 2 - font.width(label) / 2, (r[1] + r[3]) / 2 - 4,
					enabled ? 0xFFFFFFFF : 0xFF9A9A9A, true);
		}
	}

	private static int lighten(int colour) {
		int r = Math.min(255, ((colour >> 16) & 0xFF) + 40);
		int gr = Math.min(255, ((colour >> 8) & 0xFF) + 40);
		int b = Math.min(255, (colour & 0xFF) + 40);
		return 0xFF000000 | (r << 16) | (gr << 8) | b;
	}

	private void drawLever(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		int lx = leverX();
		int top = leverTop();
		int bottom = leverBottom();
		// Pulled down for a moment, then it springs back.
		double pull = leverAge < 6 ? leverAge / 6.0 : leverAge < 14 ? 1 - (leverAge - 6) / 8.0 : 0;
		int knobY = (int) Math.round(top + (bottom - top - 10) * pull);
		int armTop = Math.min(knobY + 4, bottom);
		g.fill(lx + 3, armTop, lx + 6, bottom + 2, 0xFF80868E);
		g.fill(lx + 3, armTop, lx + 4, bottom + 2, 0xFFE6E9EE);
		boolean hot = canSpin() && mouseX >= lx - 4 && mouseX < lx + 16 && mouseY >= top - 8 && mouseY < bottom + 8;
		int knob = switch (machine) {
			case CLOCKWORK -> GOLD;
			case GEM -> 0xFF5CE0E6;
			case PROGRESSIVE -> 0xFFFF7AF0;
			default -> 0xFFD0302A;
		};
		oval(g, lx + 4, knobY, 6, 6, 0xFF1A0A0A);
		oval(g, lx + 4, knobY, 5, 5, hot ? lighten(knob) : knob);
		g.fill(lx + 2, knobY - 3, lx + 4, knobY - 1, 0xAAFFFFFF);
	}

	private void drawPaytable(GuiGraphicsExtractor g) {
		int px = left + panelWidth - FRAME - 8 - sideW() + 7;
		int width = sideW() - 14;
		int py = cabY0() + 13;
		g.text(font, "PAYTABLE", px, py, GOLD, true);
		py += 13;
		for (String line : Slots.paytable(machine)) {
			for (String part : wrap(line, width)) {
				g.text(font, part, px, py, TAN, false);
				py += 9;
			}
			py += 2;
		}
		py += 4;
		g.fill(px, py, px + width, py + 1, GOLD_DIM);
		py += 5;
		String back = "Pays back " + String.format(java.util.Locale.ROOT, "%.1f", Slots.returnToPlayer(machine) * 100) + "%"
				+ (machine == Slots.Machine.PROGRESSIVE ? " + jackpot" : "");
		for (String part : wrap(back, width)) {
			g.text(font, part, px, py, DIM, false);
			py += 9;
		}
		String per = machine.lines > 1 ? machine.lines + " lines; pays per line bet, from the left" : "Pays times the bet, on the line";
		for (String part : wrap(per, width)) {
			g.text(font, part, px, py, DIM, false);
			py += 9;
		}
		if (machine == Slots.Machine.PROGRESSIVE) {
			for (String part : wrap("Max bet wins " + FinanceBook.amount(topJackpot), width)) {
				g.text(font, part, px, py, DIM, false);
				py += 9;
			}
		}
	}

	private java.util.List<String> wrap(String text, int width) {
		java.util.List<String> out = new java.util.ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" ")) {
			String next = line.isEmpty() ? word : line + " " + word;
			if (font.width(next) > width && !line.isEmpty()) {
				out.add(line.toString());
				line = new StringBuilder(word);
			} else {
				line = new StringBuilder(next);
			}
		}
		if (!line.isEmpty()) {
			out.add(line.toString());
		}
		return out;
	}

	// ------------------------------------------------------------------ symbols

	/** A rectangle in the symbol's own 0..26 grid, scaled to the cell. */
	private static void box(GuiGraphicsExtractor g, int x, int y, double k, double a, double b, double c, double d, int colour) {
		g.fill(x + (int) Math.round(a * k), y + (int) Math.round(b * k), x + (int) Math.round(c * k),
				y + (int) Math.round(d * k), colour);
	}

	private static void disc(GuiGraphicsExtractor g, int x, int y, double k, double cx, double cy, double r, int colour) {
		oval(g, x + (int) Math.round(cx * k), y + (int) Math.round(cy * k), Math.max(1, (int) Math.round(r * k)),
				Math.max(1, (int) Math.round(r * k)), colour);
	}

	/** One symbol filling an s-pixel cell (drawn on a 26-unit grid). */
	private void symbol(GuiGraphicsExtractor g, int x, int y, int s, int symbol) {
		double k = s / 26.0;
		switch (symbol) {
			case Slots.CHERRY -> {
				box(g, x, y, k, 8, 5, 10, 15, 0xFF2E7A22);
				box(g, x, y, k, 16, 5, 18, 14, 0xFF2E7A22);
				box(g, x, y, k, 8, 4, 18, 6, 0xFF2E7A22);
				box(g, x, y, k, 12, 2, 17, 5, 0xFF4FB04A);
				disc(g, x, y, k, 9, 18, 5, 0xFF8E1A14);
				disc(g, x, y, k, 9, 18, 4.2, 0xFFD0302A);
				disc(g, x, y, k, 17, 17, 5, 0xFF8E1A14);
				disc(g, x, y, k, 17, 17, 4.2, 0xFFD0302A);
				box(g, x, y, k, 7, 15, 9, 17, 0xFFFFB0A8);
				box(g, x, y, k, 15, 14, 17, 16, 0xFFFFB0A8);
			}
			case Slots.BAR, Slots.DOUBLE_BAR, Slots.TRIPLE_BAR -> {
				int bars = symbol - Slots.BAR + 1;
				double h = 6;
				double gap = 1.5;
				double total = bars * h + (bars - 1) * gap;
				double top = 13 - total / 2;
				for (int i = 0; i < bars; i++) {
					double by = top + i * (h + gap);
					box(g, x, y, k, 2, by, 24, by + h, 0xFF101010);
					box(g, x, y, k, 3, by + 1, 23, by + h - 1, 0xFF2A2A2A);
					box(g, x, y, k, 3, by + 1, 23, by + 2, 0xFF5A5A5A);
					if (s >= 34) {
						g.text(font, "BAR", x + s / 2 - font.width("BAR") / 2, y + (int) Math.round((by + h / 2) * k) - 4, 0xFFF4EEDC,
								false);
					} else {
						box(g, x, y, k, 8, by + 2.5, 18, by + 3.5, 0xFFF4EEDC);
					}
				}
			}
			case Slots.SEVEN -> {
				int red = 0xFFD0302A;
				int dark = 0xFF7A1612;
				box(g, x, y, k, 5, 4, 21, 9, dark);
				box(g, x, y, k, 16, 9, 21, 12, dark);
				box(g, x, y, k, 13, 12, 18, 16, dark);
				box(g, x, y, k, 10, 16, 15, 23, dark);
				box(g, x, y, k, 5.5, 4.5, 20.5, 8.5, red);
				box(g, x, y, k, 16.5, 8.5, 20.5, 11.5, red);
				box(g, x, y, k, 13.5, 11.5, 17.5, 15.5, red);
				box(g, x, y, k, 10.5, 15.5, 14.5, 22.5, red);
				box(g, x, y, k, 6, 5, 20, 6, 0xFFFF8A80);
			}
			case Slots.COG -> {
				for (int i = 0; i < 8; i++) {
					double a = i * Math.PI / 4;
					disc(g, x, y, k, 13 + Math.cos(a) * 9, 13 + Math.sin(a) * 9, 2.4, 0xFF8A6A2C);
				}
				disc(g, x, y, k, 13, 13, 9, 0xFF8A6A2C);
				disc(g, x, y, k, 13, 13, 8, 0xFFB8913F);
				disc(g, x, y, k, 13, 13, 3, 0xFFF4EEDC);
			}
			case Slots.KEY -> {
				disc(g, x, y, k, 8, 13, 5, 0xFF8A6A2C);
				disc(g, x, y, k, 8, 13, 4, GOLD);
				disc(g, x, y, k, 8, 13, 2, 0xFFF4EEDC);
				box(g, x, y, k, 12, 12, 23, 14.5, GOLD);
				box(g, x, y, k, 18, 14, 20, 18, GOLD);
				box(g, x, y, k, 21, 14, 23, 17, GOLD);
			}
			case Slots.BELL -> {
				disc(g, x, y, k, 13, 12, 8, 0xFF8A6A2C);
				disc(g, x, y, k, 13, 12, 7, GOLD);
				box(g, x, y, k, 4, 17, 22, 20, GOLD);
				box(g, x, y, k, 4, 19, 22, 20, 0xFF8A6A2C);
				disc(g, x, y, k, 13, 21.5, 2, 0xFF8A6A2C);
				box(g, x, y, k, 12, 2, 14, 5, 0xFF8A6A2C);
				box(g, x, y, k, 9, 8, 11, 14, 0xFFFFE8A0);
			}
			case Slots.CROWN -> {
				box(g, x, y, k, 4, 12, 22, 20, GOLD);
				box(g, x, y, k, 4, 18, 22, 20, 0xFF8A6A2C);
				box(g, x, y, k, 4, 6, 7, 12, GOLD);
				box(g, x, y, k, 11.5, 4, 14.5, 12, GOLD);
				box(g, x, y, k, 19, 6, 22, 12, GOLD);
				disc(g, x, y, k, 5.5, 5, 1.6, 0xFFD0302A);
				disc(g, x, y, k, 13, 3, 1.6, 0xFF5CE0E6);
				disc(g, x, y, k, 20.5, 5, 1.6, 0xFFD0302A);
				disc(g, x, y, k, 13, 15, 2, 0xFF2EC85A);
			}
			case Slots.CLOCK -> {
				disc(g, x, y, k, 13, 13, 11, 0xFF8A6A2C);
				disc(g, x, y, k, 13, 13, 10, GOLD);
				disc(g, x, y, k, 13, 13, 8.5, 0xFFF8F2E2);
				for (int i = 0; i < 12; i++) {
					double a = i * Math.PI / 6;
					box(g, x, y, k, 13 + Math.cos(a) * 7, 13 + Math.sin(a) * 7, 13 + Math.cos(a) * 7 + 1, 13 + Math.sin(a) * 7 + 1,
							0xFF3A2A1A);
				}
				box(g, x, y, k, 12.5, 7, 13.5, 13.5, 0xFF1C1A1A);
				box(g, x, y, k, 12.5, 12.5, 18, 13.5, 0xFF1C1A1A);
				if (s >= 30) {
					g.text(font, "WILD", x + s / 2 - font.width("WILD") / 2, y + s - 9, 0xFFC0362C, false);
				}
			}
			case Slots.COAL, Slots.IRON, Slots.REDSTONE, Slots.LAPIS, Slots.GOLD, Slots.EMERALD, Slots.DIAMOND -> {
				int[] colours = {0xFF1E1E1E, 0xFFD8AF93, 0xFFE0261C, 0xFF2A52D0, 0xFFF5D040, 0xFF2EC85A, 0xFF5CE0E6};
				int colour = colours[symbol - Slots.COAL];
				// A block of ore: stone with flecks, the precious ones as a cut gem on top.
				box(g, x, y, k, 3, 3, 23, 23, 0xFF5E5E64);
				box(g, x, y, k, 4, 4, 22, 22, 0xFF8A8A90);
				box(g, x, y, k, 4, 4, 22, 6, 0xFFA8A8AE);
				int[][] specks = {{6, 7}, {15, 6}, {10, 12}, {17, 14}, {6, 17}, {13, 18}};
				for (int[] sp : specks) {
					box(g, x, y, k, sp[0], sp[1], sp[0] + 3.5, sp[1] + 2.5, colour);
				}
				if (symbol == Slots.DIAMOND || symbol == Slots.EMERALD || symbol == Slots.GOLD) {
					int cx = x + (int) Math.round(13 * k);
					int cy = y + (int) Math.round(13 * k);
					int r = (int) Math.round(7 * k);
					diamond(g, cx, cy, r + 1, 0xFF101010);
					diamond(g, cx, cy, r, colour);
					diamond(g, cx - r / 3, cy - r / 3, Math.max(1, r / 3), 0xFFFFFFFF);
				}
			}
			case Slots.NUGGET -> {
				disc(g, x, y, k, 13, 14, 6, 0xFF8A6A1C);
				disc(g, x, y, k, 13, 13, 5, 0xFFF5D040);
				box(g, x, y, k, 10, 10, 13, 12, 0xFFFFF0A0);
			}
			case Slots.MAGMA -> {
				box(g, x, y, k, 4, 5, 22, 21, 0xFF3A1208);
				box(g, x, y, k, 5, 6, 21, 20, 0xFF6A2010);
				box(g, x, y, k, 5, 10, 21, 12, 0xFFF06A14);
				box(g, x, y, k, 5, 15, 21, 16, 0xFFE05A14);
				box(g, x, y, k, 8, 7, 11, 9, 0xFFFFC040);
				box(g, x, y, k, 15, 7, 18, 9, 0xFFFFC040);
			}
			case Slots.GHAST -> {
				box(g, x, y, k, 5, 3, 21, 17, 0xFFF0F0F0);
				box(g, x, y, k, 5, 3, 21, 4, 0xFFFFFFFF);
				for (int t = 0; t < 4; t++) {
					box(g, x, y, k, 6 + t * 4, 17, 8 + t * 4, 23 - (t % 2) * 2, 0xFFE0E0E0);
				}
				box(g, x, y, k, 8, 8, 11, 10, 0xFF3A3A3A);
				box(g, x, y, k, 15, 8, 18, 10, 0xFF3A3A3A);
				box(g, x, y, k, 11, 12, 15, 14, 0xFF3A3A3A);
			}
			case Slots.BLAZE -> {
				box(g, x, y, k, 8, 4, 18, 13, 0xFFF2C020);
				box(g, x, y, k, 9, 7, 12, 9, 0xFF3A1A08);
				box(g, x, y, k, 14, 7, 17, 9, 0xFF3A1A08);
				for (int t = 0; t < 4; t++) {
					box(g, x, y, k, 4 + t * 5, 14 + (t % 2) * 2, 6 + t * 5, 23, 0xFFE08A14);
				}
			}
			case Slots.NETHERITE -> {
				box(g, x, y, k, 4, 6, 22, 20, 0xFF2A2428);
				box(g, x, y, k, 5, 7, 21, 19, 0xFF4A4048);
				box(g, x, y, k, 5, 7, 21, 9, 0xFF6A5E66);
				box(g, x, y, k, 7, 11, 19, 13, 0xFF3A3236);
			}
			case Slots.ROD -> {
				for (int t = 0; t < 8; t++) {
					box(g, x, y, k, 17 - t * 1.6, 3 + t * 2.5, 20 - t * 1.6, 6 + t * 2.5, t % 2 == 0 ? 0xFFF2C020 : 0xFFE08A14);
				}
				if (s >= 30) {
					g.text(font, "BONUS", x + s / 2 - font.width("BONUS") / 2, y + s - 9, 0xFFC0362C, false);
				}
			}
			case Slots.SHELL -> {
				disc(g, x, y, k, 13, 15, 8, 0xFFE8B8A0);
				for (int t = 0; t < 5; t++) {
					box(g, x, y, k, 6 + t * 3, 8, 7 + t * 3, 21, 0xFFC08A70);
				}
				box(g, x, y, k, 10, 21, 16, 24, 0xFFE8B8A0);
			}
			case Slots.FISH -> {
				disc(g, x, y, k, 11, 13, 7, 0xFFE88A3A);
				box(g, x, y, k, 17, 9, 23, 17, 0xFFC86A20);
				box(g, x, y, k, 6, 11, 8, 13, 0xFF1C1A1A);
				box(g, x, y, k, 9, 15, 16, 16, 0xFFF4C090);
			}
			case Slots.TURTLE -> {
				disc(g, x, y, k, 13, 14, 8, 0xFF3A7A2A);
				disc(g, x, y, k, 13, 14, 5, 0xFF5AA83A);
				box(g, x, y, k, 11, 3, 15, 7, 0xFF8AC86A);
				box(g, x, y, k, 3, 10, 6, 13, 0xFF8AC86A);
				box(g, x, y, k, 20, 10, 23, 13, 0xFF8AC86A);
			}
			case Slots.TRIDENT -> {
				box(g, x, y, k, 12, 7, 14, 24, 0xFF5AC8C0);
				box(g, x, y, k, 6, 7, 20, 9, 0xFF5AC8C0);
				box(g, x, y, k, 6, 2, 8, 9, 0xFF5AC8C0);
				box(g, x, y, k, 18, 2, 20, 9, 0xFF5AC8C0);
				box(g, x, y, k, 12, 1, 14, 7, 0xFF8AF0E8);
			}
			case Slots.HEART -> {
				disc(g, x, y, k, 13, 13, 9, 0xFF1C4A6A);
				disc(g, x, y, k, 13, 13, 7, 0xFF2A7AB0);
				disc(g, x, y, k, 13, 13, 4, 0xFF6AD0F0);
				box(g, x, y, k, 10, 9, 12, 11, 0xFFFFFFFF);
			}
			case Slots.CHEST -> {
				box(g, x, y, k, 3, 7, 23, 21, 0xFF5A3A14);
				box(g, x, y, k, 4, 8, 22, 20, 0xFFA06A2A);
				box(g, x, y, k, 4, 12, 22, 14, 0xFF5A3A14);
				box(g, x, y, k, 11, 11, 15, 16, GOLD);
				if (s >= 30) {
					g.text(font, "BONUS", x + s / 2 - font.width("BONUS") / 2, y + 1, 0xFFFFD840, true);
				}
			}
			case Slots.STRING -> {
				for (int t = 0; t < 9; t++) {
					box(g, x, y, k, 4 + t * 2, 13 + Math.sin(t) * 4, 6 + t * 2, 15 + Math.sin(t) * 4, 0xFFE8E8E8);
				}
			}
			case Slots.BONE -> {
				box(g, x, y, k, 7, 11, 19, 15, 0xFFE8E2D0);
				disc(g, x, y, k, 6, 11, 3, 0xFFE8E2D0);
				disc(g, x, y, k, 6, 15, 3, 0xFFE8E2D0);
				disc(g, x, y, k, 20, 11, 3, 0xFFE8E2D0);
				disc(g, x, y, k, 20, 15, 3, 0xFFE8E2D0);
			}
			case Slots.ARROW -> {
				for (int t = 0; t < 14; t++) {
					box(g, x, y, k, 5 + t, 20 - t, 7 + t, 22 - t, 0xFF8A6A3A);
				}
				box(g, x, y, k, 17, 3, 23, 9, 0xFFB0B0B8);
				box(g, x, y, k, 3, 18, 7, 23, 0xFFE8E8E8);
			}
			case Slots.SPIDER_EYE -> {
				disc(g, x, y, k, 13, 13, 8, 0xFF8A1A2A);
				disc(g, x, y, k, 13, 13, 6, 0xFFC0304A);
				disc(g, x, y, k, 11, 11, 2, 0xFFFF8090);
			}
			case Slots.SWORD -> {
				for (int t = 0; t < 13; t++) {
					box(g, x, y, k, 8 + t, 15 - t, 11 + t, 18 - t, 0xFF5CE0E6);
				}
				box(g, x, y, k, 5, 15, 12, 18, 0xFF3A2A1A);
				box(g, x, y, k, 3, 19, 8, 23, 0xFF6A4A2A);
				box(g, x, y, k, 6, 13, 9, 21, 0xFF3A2A1A);
			}
			case Slots.CREEPER_HEAD -> {
				box(g, x, y, k, 3, 3, 23, 23, 0xFF2A6A22);
				box(g, x, y, k, 4, 4, 22, 22, 0xFF4CB03C);
				box(g, x, y, k, 6, 7, 11, 12, 0xFF101010);
				box(g, x, y, k, 15, 7, 20, 12, 0xFF101010);
				box(g, x, y, k, 11, 12, 15, 17, 0xFF101010);
				box(g, x, y, k, 8, 15, 11, 21, 0xFF101010);
				box(g, x, y, k, 15, 15, 18, 21, 0xFF101010);
			}
			case Slots.LEMON -> {
				disc(g, x, y, k, 13, 13, 8, 0xFFC8A010);
				disc(g, x, y, k, 13, 13, 7, 0xFFF8E040);
				box(g, x, y, k, 3, 12, 6, 14, 0xFFF8E040);
				box(g, x, y, k, 20, 12, 23, 14, 0xFFF8E040);
				box(g, x, y, k, 10, 9, 12, 11, 0xFFFFFFB0);
			}
			case Slots.PLUM -> {
				disc(g, x, y, k, 13, 15, 8, 0xFF4A1A5A);
				disc(g, x, y, k, 13, 15, 7, 0xFF7A3A9A);
				box(g, x, y, k, 12, 3, 14, 8, 0xFF3C6A22);
				box(g, x, y, k, 14, 4, 19, 7, 0xFF4FB04A);
				box(g, x, y, k, 9, 11, 11, 13, 0xFFC090E0);
			}
			case Slots.MELON -> {
				disc(g, x, y, k, 13, 13, 10, 0xFF2A6A1A);
				disc(g, x, y, k, 13, 13, 9, 0xFF4CA03C);
				for (int t = 0; t < 4; t++) {
					box(g, x, y, k, 6 + t * 4, 5, 7 + t * 4, 21, 0xFF2A6A1A);
				}
			}
			case Slots.OCTOPUS -> {
				disc(g, x, y, k, 13, 10, 8, 0xFF8A2A6A);
				disc(g, x, y, k, 13, 10, 7, 0xFFC04A8A);
				for (int t = 0; t < 4; t++) {
					box(g, x, y, k, 5 + t * 5, 15, 7 + t * 5, 23, 0xFFC04A8A);
				}
				box(g, x, y, k, 9, 8, 11, 11, 0xFFFFFFFF);
				box(g, x, y, k, 15, 8, 17, 11, 0xFFFFFFFF);
			}
			case Slots.DIVER -> {
				disc(g, x, y, k, 13, 12, 9, 0xFFA87A22);
				disc(g, x, y, k, 13, 12, 8, 0xFFE8C463);
				disc(g, x, y, k, 13, 12, 5, 0xFF3A8EE0);
				box(g, x, y, k, 11, 10, 13, 12, 0xFFB8E8FF);
				box(g, x, y, k, 8, 21, 18, 24, 0xFFA87A22);
			}
			case Slots.SHARK -> {
				box(g, x, y, k, 3, 12, 22, 18, 0xFF5A6674);
				box(g, x, y, k, 3, 15, 22, 18, 0xFFC8D0D8);
				box(g, x, y, k, 11, 6, 15, 12, 0xFF5A6674);
				box(g, x, y, k, 22, 9, 25, 14, 0xFF5A6674);
				box(g, x, y, k, 22, 15, 25, 20, 0xFF5A6674);
				box(g, x, y, k, 5, 13, 7, 15, 0xFF101010);
				for (int t = 0; t < 4; t++) {
					box(g, x, y, k, 3 + t * 2, 16, 4 + t * 2, 17, 0xFFFFFFFF);
				}
			}
			case Slots.COIN -> {
				disc(g, x, y, k, 13, 13, 9, 0xFFA87A22);
				disc(g, x, y, k, 13, 13, 8, 0xFFE8C463);
				box(g, x, y, k, 11, 11, 15, 15, 0xFF8A1414);
			}
			case Slots.LANTERN -> {
				box(g, x, y, k, 8, 4, 18, 6, 0xFFE8C463);
				disc(g, x, y, k, 13, 13, 8, 0xFF8A1010);
				disc(g, x, y, k, 13, 13, 7, 0xFFE02A1A);
				box(g, x, y, k, 12, 6, 14, 21, 0xFFFF8A3A);
				box(g, x, y, k, 8, 20, 18, 22, 0xFFE8C463);
				box(g, x, y, k, 12, 22, 14, 25, 0xFFE8C463);
			}
			case Slots.KOI -> {
				disc(g, x, y, k, 12, 13, 7, 0xFFFF8A2A);
				box(g, x, y, k, 18, 9, 23, 17, 0xFFFF8A2A);
				box(g, x, y, k, 8, 10, 12, 14, 0xFFFFFFFF);
				box(g, x, y, k, 6, 12, 8, 14, 0xFF101010);
			}
			case Slots.INGOT -> {
				box(g, x, y, k, 4, 14, 22, 20, 0xFFA87A22);
				box(g, x, y, k, 7, 9, 19, 15, 0xFFE8C463);
				box(g, x, y, k, 4, 18, 22, 20, 0xFFC89A3A);
				box(g, x, y, k, 9, 10, 12, 11, 0xFFFFF4C0);
			}
			case Slots.DRAGON -> {
				box(g, x, y, k, 3, 3, 23, 23, 0xFF1A5A3A);
				box(g, x, y, k, 4, 4, 22, 22, 0xFF2EA86A);
				box(g, x, y, k, 6, 7, 11, 11, 0xFFE8C463);
				box(g, x, y, k, 15, 7, 20, 11, 0xFFE8C463);
				box(g, x, y, k, 8, 8, 10, 10, 0xFFC0362C);
				box(g, x, y, k, 16, 8, 18, 10, 0xFFC0362C);
				box(g, x, y, k, 7, 15, 19, 18, 0xFF0E3A26);
				box(g, x, y, k, 3, 2, 6, 6, 0xFFE8C463);
				box(g, x, y, k, 20, 2, 23, 6, 0xFFE8C463);
				if (s >= 30) {
					g.text(font, "WILD", x + s / 2 - font.width("WILD") / 2, y + s - 9, 0xFFFFD840, true);
				}
			}
			case Slots.PEARL -> {
				disc(g, x, y, k, 13, 13, 9, 0xFF8A6A2C);
				disc(g, x, y, k, 13, 13, 8, 0xFFFFF4D0);
				disc(g, x, y, k, 11, 11, 3, 0xFFFFFFFF);
			}
			case Slots.JACKPOT -> {
				box(g, x, y, k, 2, 6, 24, 20, 0xFF2A0A2A);
				box(g, x, y, k, 2, 6, 24, 7, 0xFFFF7AF0);
				box(g, x, y, k, 2, 19, 24, 20, 0xFFFF7AF0);
				String text = s >= 40 ? "JACKPOT" : "JP";
				g.text(font, text, x + s / 2 - font.width(text) / 2, y + s / 2 - 4, (ticks / 5) % 2 == 0 ? 0xFFFFE070 : 0xFFFF3A2A,
						true);
			}
			default -> {
				// Blank: a faint bar of the reel's print.
				box(g, x, y, k, 9, 12.5, 17, 13.5, 0x22000000);
			}
		}
	}

	/**
	 * Golden Dragon's Hold & Spin over the reels: the five pearls that started it, then each new one as it lands, every
	 * pearl showing its prize; the total, and the GRAND when all fifteen fill.
	 */
	private void drawHoldAndSpin(GuiGraphicsExtractor g, int wx, int wy, int s) {
		int[] order = pearlOrder();
		int shown = Math.min(order.length, bonusAge / 6 + 1);
		g.fill(wx + 1, wy + 2, wx + windowW() - 1, wy + 3 + 3 * s, 0xDD1A0606);
		int rw = reelW();
		for (int k = 0; k < shown; k++) {
			int i = order[k];
			int x = wx + 2 + (i / 3) * (rw + 4);
			int y = wy + 3 + (i % 3) * s;
			boolean fresh = k == shown - 1 && (bonusAge % 6) < 3;
			oval(g, x + rw / 2, y + s / 2, s / 2 - 1, s / 2 - 1, fresh ? 0xFFFFFFFF : 0xFFE8C463);
			oval(g, x + rw / 2, y + s / 2, s / 2 - 3, s / 2 - 3, 0xFFFFF4D0);
			int value = pearls[i] >= Slots.GRAND ? pearls[i] - Slots.GRAND : pearls[i];
			String label = value >= 50 ? "MAJOR" : value >= 20 ? "MINI" : value + "x";
			g.text(font, label, x + rw / 2 - font.width(label) / 2, y + s / 2 - 4, value >= 20 ? 0xFFC0362C : 0xFF5A0808, false);
		}
		String title = "HOLD & SPIN";
		g.text(font, title, wx + windowW() / 2 - font.width(title) / 2, wy - 10, (ticks / 4) % 2 == 0 ? 0xFFFFD840 : 0xFFFF6A3A, true);
		if (shown >= order.length) {
			boolean grand = order.length == 15;
			String total = (grand ? "GRAND! " : "") + FinanceBook.amount(bonus);
			int tw = font.width(total) + 12;
			g.fill(wx + windowW() / 2 - tw / 2, wy + windowH() / 2 - 8, wx + windowW() / 2 + tw / 2, wy + windowH() / 2 + 8, 0xEE5A0808);
			g.text(font, total, wx + windowW() / 2 - font.width(total) / 2, wy + windowH() / 2 - 4, 0xFFFFD840, true);
		}
	}

	@Override
	protected CasinoTheme theme() {
		CasinoTheme.Style style = switch (machine) {
			case FRUIT, PROGRESSIVE -> CasinoTheme.Style.BULBS;
			case CLOCKWORK -> CasinoTheme.Style.GEARS;
			case GEM -> CasinoTheme.Style.STARS;
			case NETHER -> CasinoTheme.Style.LAVA;
			case TREASURE -> CasinoTheme.Style.BUBBLES;
			case CREEPER -> CasinoTheme.Style.PIXELS;
			case SHARK -> CasinoTheme.Style.WAVES;
			case DRAGON -> CasinoTheme.Style.SCALES;
			case PUB -> CasinoTheme.Style.CHROME;
		};
		return CasinoTheme.slot(body(), bodyLight(), bodyDark(), trim(), bulbOn(), bulbOff(), style);
	}
}
