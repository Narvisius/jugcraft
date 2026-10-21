package io.github.jimbozoomer.jugcraft.client;

import static io.github.jimbozoomer.jugcraft.client.ExchangeScreen.*;

import io.github.jimbozoomer.jugcraft.finance.ArcadeGame;
import io.github.jimbozoomer.jugcraft.finance.CasinoPayload;
import io.github.jimbozoomer.jugcraft.finance.FinanceActionPayload.Action;
import io.github.jimbozoomer.jugcraft.finance.FinanceBook;
import io.github.jimbozoomer.jugcraft.finance.PokerHands;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * The arcade cabinet: a glowing screen with five games picked from tabs along its top (Video Poker, Keno, Mines,
 * Crash, Hi-Lo), the chip rack and the game's buttons below. The server decides everything; what is still secret (the
 * TNT, the burst point) is only sent once the round is over.
 */
public class ArcadeScreen extends CasinoScreen {
	private static final int SCREEN = 0xFF0A0F14;
	private static final int SCREEN_EDGE = 0xFF1E3A4A;
	private static final int GLOW = 0xFF5AE0FF;

	private ArcadeGame.Mode mode = ArcadeGame.Mode.VIDEO_POKER;
	private ArcadeGame.Phase phase = ArcadeGame.Phase.IDLE;
	private long bet;
	private long lastWin;
	private long rounds = -1;
	private boolean busy;
	private final int[] hand = new int[5];
	private int pokerResult = -1;
	private long kenoPicks;
	private long kenoPicksHigh;
	private long kenoDrawn;
	private long kenoDrawnHigh;
	private int kenoCaught;
	private int mineCount = 3;
	private long minesRevealed;
	private long minesHidden;
	private boolean mineHit;
	private int crashTicks;
	private double crashPoint;
	private double cashedAt;
	private int card = -1;
	private double hiloMultiplier = 1;
	private int hiloSteps;
	private final List<Integer> hiloCards = new ArrayList<>();
	// What the player is choosing before playing.
	private int holds;
	private long pickLow;
	private long pickHigh;
	private int chosenMines = 3;
	private String autoText = "2.00";
	/** Local ticks since the last view, to run the firework smoothly between updates. */
	private int sinceView;
	private String shape = "";

	public ArcadeScreen(CasinoPayload data) {
		super(data);
		parse();
	}

	/** What the last view showed, to hear and celebrate each change once. */
	private ArcadeGame.Phase shownPhase;
	private long shownRounds = -1;
	private long shownRevealed;

	private void parse() {
		CasinoPayload.Reader r = new CasinoPayload.Reader(data.data());
		mode = ArcadeGame.Mode.values()[Math.max(0, Math.min(4, r.nextInt()))];
		phase = ArcadeGame.Phase.values()[Math.max(0, Math.min(2, data.phase()))];
		bet = r.next();
		lastWin = r.next();
		long newRounds = r.next();
		busy = r.nextBool();
		for (int i = 0; i < 5; i++) {
			hand[i] = r.nextInt();
		}
		pokerResult = r.nextInt();
		kenoPicks = r.next();
		kenoPicksHigh = r.next();
		kenoDrawn = r.next();
		kenoDrawnHigh = r.next();
		kenoCaught = r.nextInt();
		mineCount = r.nextInt();
		minesRevealed = r.next();
		minesHidden = r.next();
		mineHit = r.nextBool();
		crashTicks = r.nextInt();
		crashPoint = r.next() / 100.0;
		r.next();
		cashedAt = r.next() / 100.0;
		card = r.nextInt();
		hiloMultiplier = r.next() / 10_000.0;
		hiloSteps = r.nextInt();
		int n = r.nextInt();
		hiloCards.clear();
		for (int i = 0; i < n; i++) {
			hiloCards.add(r.nextInt());
		}
		if (newRounds != rounds && mode == ArcadeGame.Mode.VIDEO_POKER && phase == ArcadeGame.Phase.PLAYING) {
			holds = 0;
		}
		rounds = newRounds;
		sinceView = 0;
		sounds();
	}

	/** Cards dealt, tiles turned, a crash, and a celebration when a round ends with a win. */
	private void sounds() {
		long pos = data.pos();
		boolean first = shownPhase == null;
		if (!first && !busy) {
			if (phase == ArcadeGame.Phase.PLAYING && (shownPhase != ArcadeGame.Phase.PLAYING || rounds != shownRounds)) {
				if (mode == ArcadeGame.Mode.VIDEO_POKER || mode == ArcadeGame.Mode.HILO) {
					CasinoSound.card(pos);
				} else {
					CasinoSound.lever(pos);
				}
			}
			if (Long.bitCount(minesRevealed) > Long.bitCount(shownRevealed)) {
				CasinoSound.play(pos, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, 0.5F,
						1.0F + Long.bitCount(minesRevealed) * 0.06F);
			}
			if (phase == ArcadeGame.Phase.DONE && shownPhase != ArcadeGame.Phase.DONE) {
				if (lastWin > 0) {
					celebrate(lastWin, Math.max(1, bet), false);
				} else if (mode == ArcadeGame.Mode.CRASH || mode == ArcadeGame.Mode.MINES) {
					CasinoSound.play(pos, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), 0.4F, 1.3F);
				}
			}
		}
		shownPhase = phase;
		shownRounds = rounds;
		shownRevealed = minesRevealed;
	}

	@Override
	protected boolean ownCelebration() {
		return true;
	}

	@Override
	protected boolean needsRebuild(CasinoPayload before, CasinoPayload after) {
		parse();
		String now = mode + "/" + phase + "/" + busy + "/" + hiloSteps + "/" + Long.bitCount(minesRevealed);
		return !now.equals(shape);
	}

	@Override
	public void tick() {
		super.tick();
		sinceView++;
	}

	@Override
	protected int preferredWidth() {
		return 460;
	}

	@Override
	protected int preferredHeight() {
		return 300;
	}

	// ------------------------------------------------------------------ layout

	private int sx0() {
		return left + FRAME + 10;
	}

	private int sx1() {
		return left + panelWidth - FRAME - 10;
	}

	private int sy0() {
		return tableTop() + 18;
	}

	private int sy1() {
		return tableBottom() - 24;
	}

	private boolean playing() {
		return phase == ArcadeGame.Phase.PLAYING;
	}

	@Override
	protected void initGame() {
		shape = mode + "/" + phase + "/" + busy + "/" + hiloSteps + "/" + Long.bitCount(minesRevealed);
		// Mode tabs along the top of the screen.
		int tw = (sx1() - sx0()) / 5;
		for (ArcadeGame.Mode m : ArcadeGame.Mode.values()) {
			Plate tab = addRenderableWidget(new Plate(sx0() + m.ordinal() * tw, tableTop(), tw - 2, 15, m.title,
					m == mode ? Plate.Kind.TAB_OPEN : Plate.Kind.TAB, b -> send(Action.CASINO_ACT, 10, String.valueOf(m.ordinal()), 0)));
			tab.active = m != mode && !playing();
		}
		int by = tableBottom() - 18;
		int x = addChipRack(left + FRAME + 8, by);
		int right = sx1();
		switch (mode) {
			case VIDEO_POKER -> addRenderableWidget(new Plate(right - 70, by, 70, 16, playing() ? "DRAW" : "DEAL", Plate.Kind.BRASS,
					b -> {
						if (playing()) {
							send(Action.CASINO_ACT, 12, String.valueOf(holds), 0);
						} else {
							send(Action.CASINO_ACT, 11, "", chip);
						}
					}));
			case KENO -> {
				addRenderableWidget(new Plate(right - 70, by, 70, 16, "PLAY", Plate.Kind.BRASS,
						b -> send(Action.CASINO_ACT, 13, pickLow + "," + pickHigh, chip))).active = spots() > 0;
				addRenderableWidget(new Plate(right - 132, by, 58, 16, "Clear", Plate.Kind.WOOD, b -> {
					pickLow = 0;
					pickHigh = 0;
					rebuildWidgets();
				}));
			}
			case MINES -> {
				if (playing()) {
					addRenderableWidget(new Plate(right - 110, by, 110, 16, minesRevealed == 0 ? "Take back" : "CASH OUT", Plate.Kind.BRASS,
							b -> send(Action.CASINO_ACT, 19, "", 0)));
				} else {
					addRenderableWidget(new Plate(right - 150, by, 16, 16, "-", Plate.Kind.WOOD, b -> chosenMines = Math.max(1, chosenMines - 1)));
					addRenderableWidget(new Plate(right - 92, by, 16, 16, "+", Plate.Kind.WOOD, b -> chosenMines = Math.min(24, chosenMines + 1)));
					addRenderableWidget(new Plate(right - 70, by, 70, 16, "START", Plate.Kind.BRASS,
							b -> send(Action.CASINO_ACT, 14, String.valueOf(chosenMines), chip)));
				}
			}
			case CRASH -> {
				if (playing()) {
					addRenderableWidget(new Plate(right - 110, by, 110, 16, "CASH OUT", Plate.Kind.BRASS,
							b -> send(Action.CASINO_ACT, 19, "", 0)));
				} else {
					EditBox auto = new EditBox(font, right - 128, by + 1, 50, 14, Component.literal("Auto"));
					auto.setValue(autoText);
					auto.setMaxLength(7);
					auto.setResponder(t -> autoText = t);
					addRenderableWidget(auto);
					addRenderableWidget(new Plate(right - 70, by, 70, 16, "LAUNCH", Plate.Kind.BRASS, b -> {
						double target = 0;
						try {
							target = Double.parseDouble(autoText.trim());
						} catch (NumberFormatException ignored) {
							// no automatic cash out
						}
						send(Action.CASINO_ACT, 16, String.valueOf(Math.round(target * 100)), chip);
					}));
				}
			}
			case HILO -> {
				if (playing()) {
					int rank = PokerHands.rank(card);
					double hi = ArcadeGame.hiloChance(rank, true);
					double lo = ArcadeGame.hiloChance(rank, false);
					addRenderableWidget(new Plate(x + 6, by, 88, 16, String.format(java.util.Locale.ROOT, "HIGHER %.2fx", 0.98 / hi),
							Plate.Kind.BRASS, b -> send(Action.CASINO_ACT, 18, "1", 0))).active = hi < 1;
					addRenderableWidget(new Plate(x + 98, by, 88, 16, String.format(java.util.Locale.ROOT, "LOWER %.2fx", 0.98 / lo),
							Plate.Kind.BRASS, b -> send(Action.CASINO_ACT, 18, "0", 0))).active = lo < 1;
					addRenderableWidget(new Plate(right - 70, by, 70, 16, hiloSteps == 0 ? "Take back" : "CASH OUT", Plate.Kind.WOOD,
							b -> send(Action.CASINO_ACT, 19, "", 0)));
				} else {
					addRenderableWidget(new Plate(right - 70, by, 70, 16, "DEAL", Plate.Kind.BRASS,
							b -> send(Action.CASINO_ACT, 17, "", chip)));
				}
			}
		}
	}

	private int spots() {
		return Long.bitCount(pickLow) + Long.bitCount(pickHigh);
	}

	// ------------------------------------------------------------------ clicks on the screen

	/** A video poker card: the five share the room left of the paytable (which starts 140 from the right), shrinking to fit. */
	private int[] cardRect(int i) {
		int room = sx1() - sx0() - 156;
		int gap = 6;
		int w = Math.max(24, Math.min(52, (room - 4 * gap) / 5));
		int h = w * 72 / 52;
		int total = 5 * w + 4 * gap;
		int x = sx0() + 6 + Math.max(0, (room - total) / 2);
		return new int[] {x + i * (w + gap), sy0() + 24, x + i * (w + gap) + w, sy0() + 24 + h};
	}

	private int[] kenoCell(int n) {
		int cols = 10;
		int cw = Math.min(22, (sx1() - sx0() - 150) / cols);
		int ch = Math.min(15, (sy1() - sy0() - 14) / 8);
		int x = sx0() + 8 + (n % cols) * cw;
		int y = sy0() + 8 + (n / cols) * ch;
		return new int[] {x, y, x + cw - 2, y + ch - 2};
	}

	private int[] mineCell(int i) {
		int size = Math.min(30, (sy1() - sy0() - 16) / 5);
		int total = 5 * size;
		int x0 = sx0() + 20;
		int y0 = sy0() + (sy1() - sy0() - total) / 2;
		int x = x0 + (i % 5) * size;
		int y = y0 + (i / 5) * size;
		return new int[] {x, y, x + size - 2, y + size - 2};
	}

	private static boolean in(int[] r, double x, double y) {
		return x >= r[0] && x < r[2] && y >= r[1] && y < r[3];
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (settingsOpen()) {
			return super.mouseClicked(event, doubleClick);
		}
		double x = event.x();
		double y = event.y();
		if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && !busy) {
			switch (mode) {
				case VIDEO_POKER -> {
					if (playing()) {
						for (int i = 0; i < 5; i++) {
							if (in(cardRect(i), x, y)) {
								holds ^= 1 << i;
								return true;
							}
						}
					}
				}
				case KENO -> {
					for (int n = 0; n < 80; n++) {
						if (in(kenoCell(n), x, y)) {
							boolean on = n < 64 ? (pickLow & (1L << n)) != 0 : (pickHigh & (1L << (n - 64))) != 0;
							if (!on && spots() >= 10) {
								return true;
							}
							if (n < 64) {
								pickLow ^= 1L << n;
							} else {
								pickHigh ^= 1L << (n - 64);
							}
							rebuildWidgets();
							return true;
						}
					}
				}
				case MINES -> {
					if (playing()) {
						for (int i = 0; i < 25; i++) {
							if (in(mineCell(i), x, y) && (minesRevealed & (1L << i)) == 0) {
								send(Action.CASINO_ACT, 15, String.valueOf(i), 0);
								return true;
							}
						}
					}
				}
				default -> {
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	// ------------------------------------------------------------------ drawing

	@Override
	protected void drawTableBack(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		// The cabinet's bezel and its glowing glass.
		int x0 = sx0() - 6;
		int x1 = sx1() + 6;
		int y0 = sy0() - 6;
		int y1 = sy1() + 6;
		g.fill(x0 - 2, y0 - 2, x1 + 2, y1 + 2, 0xFF2A2A30);
		g.fill(x0, y0, x1, y1, 0xFF14141A);
		g.fill(sx0() - 1, sy0() - 1, sx1() + 1, sy1() + 1, SCREEN_EDGE);
		g.fill(sx0(), sy0(), sx1(), sy1(), SCREEN);
		// Scanlines.
		for (int y = sy0(); y < sy1(); y += 3) {
			g.fill(sx0(), y, sx1(), y + 1, 0x14FFFFFF);
		}
	}

	@Override
	protected void drawTable(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		if (busy) {
			String who = (data.text().isEmpty() ? "Someone" : data.text().get(0)) + " is playing this cabinet.";
			g.text(font, who, (sx0() + sx1()) / 2 - font.width(who) / 2, (sy0() + sy1()) / 2, LOSS, true);
			return;
		}
		switch (mode) {
			case VIDEO_POKER -> drawPoker(g);
			case KENO -> drawKeno(g, mouseX, mouseY);
			case MINES -> drawMines(g, mouseX, mouseY);
			case CRASH -> drawCrash(g);
			case HILO -> drawHilo(g);
		}
		String last = phase == ArcadeGame.Phase.DONE ? (lastWin > 0 ? "WIN " + FinanceBook.amount(lastWin) : "No win") : "";
		if (!last.isEmpty()) {
			g.text(font, last, sx1() - 8 - font.width(last), sy1() - 12, lastWin > 0 ? GAIN : DIM, true);
		}
	}

	private void bigCard(GuiGraphicsExtractor g, int[] r, int c, boolean lit) {
		if (lit) {
			// A held card glows gold round its edge.
			g.fill(r[0] - 2, r[1] - 2, r[2] + 2, r[3] + 2, GOLD);
		}
		CardArt.draw(g::fill, r[0], r[1], r[2] - r[0], r[3] - r[1], c);
	}

	private void drawPoker(GuiGraphicsExtractor g) {
		boolean dealt = phase != ArcadeGame.Phase.IDLE && hand[0] >= 0 && rounds > 0;
		for (int i = 0; i < 5; i++) {
			int[] r = cardRect(i);
			boolean held = playing() && (holds & (1 << i)) != 0;
			bigCard(g, r, dealt ? hand[i] : -2, held);
			if (held) {
				g.text(font, "HELD", (r[0] + r[2]) / 2 - font.width("HELD") / 2, r[1] - 11, GOLD, true);
			}
		}
		String hint = playing() ? "Click cards to hold, then DRAW." : "Pick a bet and DEAL. Jacks or Better.";
		int[] first = cardRect(0);
		g.text(font, hint, first[0], first[3] + 8, GLOW, false);
		// Paytable.
		int px = sx1() - 140;
		int py = sy0() + 8;
		g.text(font, "PAYS (x bet)", px, py, GOLD, false);
		for (int i = ArcadeGame.POKER_HANDS.length - 1; i >= 0; i--) {
			py += 10;
			boolean won = phase == ArcadeGame.Phase.DONE && pokerResult == i;
			int colour = won ? ((ticks / 4) % 2 == 0 ? GOLD : 0xFFFFFFFF) : TAN;
			g.text(font, ArcadeGame.POKER_HANDS[i], px, py, colour, false);
			String pays = String.valueOf(ArcadeGame.POKER_PAYS[i]);
			g.text(font, pays, sx1() - 8 - font.width(pays), py, colour, false);
		}
	}

	private void drawKeno(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		boolean done = phase == ArcadeGame.Phase.DONE;
		for (int n = 0; n < 80; n++) {
			int[] r = kenoCell(n);
			boolean picked = n < 64 ? (pickLow & (1L << n)) != 0 : (pickHigh & (1L << (n - 64))) != 0;
			boolean drawn = done && (n < 64 ? (kenoDrawn & (1L << n)) != 0 : (kenoDrawnHigh & (1L << (n - 64))) != 0);
			int fill = picked && drawn ? GOLD : drawn ? 0xFF2A6A8A : picked ? 0xFF3A7A3A : 0xFF16222A;
			if (!picked && in(r, mouseX, mouseY)) {
				fill = 0xFF24343E;
			}
			g.fill(r[0], r[1], r[2], r[3], fill);
			String label = String.valueOf(n + 1);
			g.text(font, label, (r[0] + r[2]) / 2 - font.width(label) / 2, (r[1] + r[3]) / 2 - 3,
					picked && drawn ? 0xFF1A0A0A : 0xFFDDEEFF, false);
		}
		int px = sx1() - 136;
		int py = sy0() + 8;
		int spots = spots();
		g.text(font, "Spots: " + spots + "/10", px, py, GLOW, false);
		if (done) {
			g.text(font, "Caught " + kenoCaught, px, py + 10, GOLD, false);
		}
		if (spots > 0) {
			py += 24;
			g.text(font, "Catch  Pays", px, py, GOLD, false);
			int[] table = ArcadeGame.KENO[spots];
			for (int i = table.length - 2; i >= 0; i -= 2) {
				py += 10;
				g.text(font, table[i] + "      " + table[i + 1] + "x", px, py, TAN, false);
			}
			py += 14;
			g.text(font, String.format(java.util.Locale.ROOT, "Pays back %.1f%%", ArcadeGame.kenoReturn(spots) * 100), px, py, DIM,
					false);
		} else {
			g.text(font, "Click 1 to 10 numbers.", px, py + 12, DIM, false);
		}
	}

	private void drawMines(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		boolean done = phase == ArcadeGame.Phase.DONE;
		for (int i = 0; i < 25; i++) {
			int[] r = mineCell(i);
			boolean open = (minesRevealed & (1L << i)) != 0;
			boolean tnt = (minesHidden & (1L << i)) != 0;
			int fill;
			if (open && tnt) {
				fill = 0xFFC0302A;
			} else if (open) {
				fill = 0xFF2A6A3A;
			} else if (done && tnt) {
				fill = 0xFF5A2020;
			} else {
				fill = in(r, mouseX, mouseY) && playing() ? 0xFF3A4A5A : 0xFF24303A;
			}
			g.fill(r[0], r[1], r[2], r[3], fill);
			g.fill(r[0], r[1], r[2], r[1] + 1, 0x33FFFFFF);
			int cx = (r[0] + r[2]) / 2;
			int cy = (r[1] + r[3]) / 2;
			if (open && !tnt) {
				diamond(g, cx, cy, Math.max(3, (r[2] - r[0]) / 4), 0xFF5CE0E6);
			} else if (tnt && (open || done)) {
				g.fill(cx - 5, cy - 4, cx + 5, cy + 5, 0xFFC0302A);
				g.fill(cx - 5, cy - 1, cx + 5, cy + 2, 0xFFEFE8D6);
				g.fill(cx, cy - 7, cx + 1, cy - 4, 0xFF3A3A3A);
			}
		}
		int px = mineCell(4)[2] + 24;
		int py = sy0() + 10;
		int mines = playing() || done ? mineCount : chosenMines;
		g.text(font, "TNT hidden: " + mines, px, py, GLOW, false);
		int safe = Long.bitCount(minesRevealed & ~minesHidden);
		double now = safe == 0 ? 1 : ArcadeGame.minesMultiplier(mines, safe);
		double next = ArcadeGame.minesMultiplier(mines, safe + 1);
		if (playing()) {
			g.text(font, String.format(java.util.Locale.ROOT, "Now: %.2fx (%s)", now, FinanceBook.amount((long) Math.floor(bet * now))),
					px, py + 14, GOLD, false);
			g.text(font, String.format(java.util.Locale.ROOT, "Next tile: %.2fx", next), px, py + 26, TAN, false);
			g.text(font, String.format(java.util.Locale.ROOT, "Safe chance: %.0f%%", 100.0 * (25 - mines - safe) / (25 - safe)), px,
					py + 38, DIM, false);
		} else {
			g.text(font, "Each safe tile pays more.", px, py + 14, DIM, false);
			g.text(font, "Cash out before the TNT.", px, py + 26, DIM, false);
			g.text(font, String.format(java.util.Locale.ROOT, "1 tile: %.2fx", ArcadeGame.minesMultiplier(mines, 1)), px, py + 42, TAN,
					false);
		}
	}

	private void drawCrash(GuiGraphicsExtractor g) {
		int x0 = sx0() + 30;
		int x1 = sx1() - 20;
		int y0 = sy0() + 14;
		int y1 = sy1() - 18;
		// Axes.
		g.fill(x0, y0, x0 + 1, y1, 0xFF2A4A5A);
		g.fill(x0, y1, x1, y1 + 1, 0xFF2A4A5A);
		int t = playing() ? crashTicks + sinceView : crashTicks;
		double m = Math.floor(ArcadeGame.crashAt(t) * 100) / 100.0;
		boolean burst = phase == ArcadeGame.Phase.DONE && cashedAt <= 0;
		if (burst) {
			m = crashPoint;
		} else if (phase == ArcadeGame.Phase.DONE) {
			m = cashedAt;
		}
		// The firework's trail: multiplier against time, scaled to fit.
		double top = Math.max(2, m * 1.2);
		int span = Math.max(120, t);
		int px = x0;
		int py = y1;
		for (int k = 0; k <= t; k += Math.max(1, t / 120)) {
			double v = ArcadeGame.crashAt(k);
			int x = x0 + (int) ((long) (x1 - x0) * k / span);
			int y = y1 - (int) ((y1 - y0) * Math.min(1, (v - 1) / (top - 1)));
			g.fill(Math.min(px, x), Math.min(py, y), Math.max(px, x) + 1, Math.max(py, y) + 1, burst ? 0xFFC0302A : 0xFFFFC040);
			px = x;
			py = y;
		}
		if (playing() || phase == ArcadeGame.Phase.DONE) {
			// The rocket, or its burst.
			if (burst) {
				for (int k = 0; k < 10; k++) {
					double a = k * Math.PI / 5;
					int bx = px + (int) (Math.cos(a) * 8);
					int by = py + (int) (Math.sin(a) * 8);
					g.fill(bx - 1, by - 1, bx + 2, by + 2, k % 2 == 0 ? 0xFFFF6A3A : 0xFFFFE070);
				}
			} else {
				g.fill(px - 2, py - 4, px + 3, py + 3, 0xFFE8E2D0);
				g.fill(px - 1, py - 6, px + 2, py - 4, 0xFFC0302A);
			}
		}
		String big = String.format(java.util.Locale.ROOT, "%.2fx", phase == ArcadeGame.Phase.IDLE ? 1.0 : m);
		int colour = burst ? LOSS : phase == ArcadeGame.Phase.DONE ? GAIN : 0xFFFFFFFF;
		g.text(font, big, (x0 + x1) / 2 - font.width(big) / 2, y0 + 10, colour, true);
		String note = phase == ArcadeGame.Phase.IDLE ? "Set an auto cash out (or 0) and LAUNCH." : burst
				? String.format(java.util.Locale.ROOT, "Burst at %.2fx", crashPoint) : phase == ArcadeGame.Phase.DONE
				? String.format(java.util.Locale.ROOT, "Cashed out at %.2fx", cashedAt) : "Cash out before it bursts!";
		g.text(font, note, (x0 + x1) / 2 - font.width(note) / 2, y0 + 22, burst ? LOSS : TAN, false);
		if (!playing()) {
			g.text(font, "Auto:", sx1() - 160, tableBottom() - 14, DIM, false);
		}
	}

	private void drawHilo(GuiGraphicsExtractor g) {
		int cx = (sx0() + sx1()) / 2;
		int[] r = {cx - 30, sy0() + 18, cx + 30, sy0() + 100};
		bigCard(g, r, playing() || phase == ArcadeGame.Phase.DONE ? card : -2, false);
		String m = String.format(java.util.Locale.ROOT, "%.2fx", playing() ? hiloMultiplier : 1.0);
		if (playing()) {
			g.text(font, "Multiplier " + m + " (" + FinanceBook.amount((long) Math.floor(bet * hiloMultiplier)) + ")",
					cx - font.width("Multiplier " + m + " (" + FinanceBook.amount((long) Math.floor(bet * hiloMultiplier)) + ")") / 2,
					r[3] + 8, GOLD, false);
			g.text(font, "Equal counts as right.", cx - font.width("Equal counts as right.") / 2, r[3] + 20, DIM, false);
		} else {
			String hint = "Deal a card, then guess higher or lower.";
			g.text(font, hint, cx - font.width(hint) / 2, r[3] + 8, GLOW, false);
		}
		// The cards so far.
		int hx = sx0() + 10;
		int hy = sy1() - 30;
		for (int i = 0; i < hiloCards.size(); i++) {
			card(g, hx + i * 20, hy, 18, 24, hiloCards.get(i));
		}
	}

	@Override
	protected CasinoTheme theme() {
		return CasinoTheme.ARCADE;
	}
}
