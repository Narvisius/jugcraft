package io.github.jimbozoomer.jugcraft.client;

import static io.github.jimbozoomer.jugcraft.client.ExchangeScreen.*;

import io.github.jimbozoomer.jugcraft.finance.CasinoPayload;
import io.github.jimbozoomer.jugcraft.finance.Derby;
import io.github.jimbozoomer.jugcraft.finance.FinanceActionPayload.Action;
import io.github.jimbozoomer.jugcraft.finance.FinanceBook;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * The Derby Table's betting screen: the track from above with the six runners (the same race the little horses run
 * on the table), and the race card: each horse's silks, name, form and live Win, Place and Show prices. Click a price
 * to bet the picked chip on that horse in that pool. The prices are what a one-Jug ticket would pay back now, stake
 * included; they move as people bet and are settled when the race is run.
 */
public class DerbyScreen extends CasinoScreen {
	private record MyBet(int pool, int horse, long amount) {
	}

	private long raceNumber;
	private long seed;
	private long startTime;
	private int cut = 15;
	private final int[] names = new int[Derby.HORSES];
	private final int[] ratings = new int[Derby.HORSES];
	private final String[] form = new String[Derby.HORSES];
	private final int[] order = new int[Derby.HORSES];
	private boolean ordered;
	private final double[][] prices = new double[3][Derby.HORSES];
	private final List<Integer> history = new ArrayList<>();
	private final List<MyBet> mine = new ArrayList<>();
	private String shape = "";
	private Derby.Phase lastPhase;

	public DerbyScreen(CasinoPayload data) {
		super(data);
		parse();
		lastPhase = phase();
	}

	private void parse() {
		CasinoPayload.Reader r = new CasinoPayload.Reader(data.data());
		raceNumber = r.next();
		seed = r.next();
		startTime = r.next();
		r.next();
		cut = r.nextInt();
		ordered = true;
		for (int h = 0; h < Derby.HORSES; h++) {
			names[h] = Math.floorMod(r.nextInt(), Derby.NAMES.length);
			ratings[h] = r.nextInt();
			form[h] = String.valueOf(r.next());
			int o = r.nextInt();
			ordered &= o >= 0;
			order[h] = Math.max(0, o);
		}
		for (int pool = 0; pool < 3; pool++) {
			for (int h = 0; h < Derby.HORSES; h++) {
				prices[pool][h] = r.next() / 100.0;
			}
		}
		history.clear();
		int n = r.nextInt();
		for (int i = 0; i < n; i++) {
			history.add(r.nextInt());
		}
		mine.clear();
		int m = r.nextInt();
		for (int i = 0; i < m; i++) {
			mine.add(new MyBet(r.nextInt(), r.nextInt(), r.next()));
		}
	}

	private Derby.Phase phase() {
		return Derby.Phase.values()[Math.max(0, Math.min(Derby.Phase.values().length - 1, data.phase()))];
	}

	@Override
	protected boolean needsRebuild(CasinoPayload before, CasinoPayload after) {
		parse();
		return !shape().equals(shape);
	}

	private String shape() {
		return phase() + "/" + !mine.isEmpty();
	}

	@Override
	protected int preferredWidth() {
		return 480;
	}

	@Override
	protected int preferredHeight() {
		return 300;
	}

	@Override
	protected CasinoTheme theme() {
		return CasinoTheme.TURF;
	}

	/** The race's results wait for the horses to cross the line. */
	@Override
	protected boolean revealing() {
		return phase() == Derby.Phase.RACING;
	}

	// ------------------------------------------------------------------ layout

	private int trackX0() {
		return left + FRAME + 8;
	}

	private int trackY0() {
		return tableTop() + 4;
	}

	private int trackW() {
		return 176;
	}

	private int trackH() {
		return 132;
	}

	private int cardX0() {
		return trackX0() + trackW() + 10;
	}

	private int cardX1() {
		return left + panelWidth - FRAME - 8;
	}

	private int rowY(int h) {
		return tableTop() + 20 + h * 21;
	}

	/** Where the Form column starts: just left of the price cells, wide enough for five figures. */
	private int formX() {
		return priceCell(0, 0)[0] - font.width("00000") - 6;
	}

	private int[] priceCell(int pool, int h) {
		int w = 44;
		int x1 = cardX1();
		int x = x1 - (3 - pool) * (w + 2);
		return new int[] {x, rowY(h), x + w, rowY(h) + 18};
	}

	@Override
	protected void initGame() {
		shape = shape();
		int by = tableBottom() - 18;
		int x = addChipRack(left + FRAME + 8, by);
		boolean racing = phase() == Derby.Phase.RACING;
		addRenderableWidget(new Plate(x + 6, by, 70, 14, "Take back", Plate.Kind.WOOD,
				b -> send(Action.CASINO_ACT, 0, "", 0))).active = !mine.isEmpty() && !racing;
		addRenderableWidget(new Plate(x + 80, by, 70, 14, "Start race", Plate.Kind.BRASS,
				b -> send(Action.CASINO_ACT, 1, "", 0))).active = !mine.isEmpty() && phase() == Derby.Phase.BETTING;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (settingsOpen()) {
			return super.mouseClicked(event, doubleClick);
		}
		if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && phase() != Derby.Phase.RACING) {
			for (int pool = 0; pool < 3; pool++) {
				for (int h = 0; h < Derby.HORSES; h++) {
					int[] c = priceCell(pool, h);
					if (event.x() >= c[0] && event.x() < c[2] && event.y() >= c[1] && event.y() < c[3]) {
						send(Action.CASINO_BET, pool, String.valueOf(h), chip);
						return true;
					}
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public void tick() {
		super.tick();
		long pos = data.pos();
		Derby.Phase now = phase();
		if (now == Derby.Phase.RACING && lastPhase != Derby.Phase.RACING) {
			// And they're off: the bell.
			CasinoSound.play(pos, SoundEvents.NOTE_BLOCK_BELL.value(), 0.9F, 1.2F);
		}
		if (now == Derby.Phase.RACING && raceFraction() < 1.0 && ticks % 4 == 0) {
			CasinoSound.play(pos, SoundEvents.HORSE_GALLOP, 0.25F, 1.6F);
		}
		lastPhase = now;
	}

	/** How far through the race we are: 0 at the off, 1 when the last horse is home. */
	private double raceFraction() {
		Minecraft mc = Minecraft.getInstance();
		long time = mc.level == null ? startTime : mc.level.getGameTime();
		return (time - startTime + partial) / (double) Derby.RACE_TICKS;
	}

	// ------------------------------------------------------------------ drawing

	@Override
	protected void drawTableBack(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		// The track from above: the green infield, the dirt oval and its rails.
		int x0 = trackX0();
		int y0 = trackY0();
		g.fill(x0 - 2, y0 - 2, x0 + trackW() + 2, y0 + trackH() + 2, 0xFF0C1A0C);
		g.fill(x0, y0, x0 + trackW(), y0 + trackH(), 0xFF2E722C);
		for (int[] r : trackPicture()) {
			g.fill(x0 + r[0], y0 + r[1], x0 + r[0] + r[2], y0 + r[1] + r[3], r[4]);
		}
		// The finish line across the near straight.
		int[] a = toScreen(32, 25 - 18.4);
		int[] b = toScreen(32, 25 - 9.0);
		g.fill(a[0], b[1], a[0] + 1, a[1], 0xFFFFFFFF);
		// The race card's rows.
		for (int h = 0; h < Derby.HORSES; h++) {
			int y = rowY(h);
			g.fill(cardX0(), y, cardX1(), y + 18, h % 2 == 0 ? 0xFF14301A : 0xFF10281A);
			for (int pool = 0; pool < 3; pool++) {
				int[] c = priceCell(pool, h);
				boolean hot = phase() != Derby.Phase.RACING && mouseX >= c[0] && mouseX < c[2] && mouseY >= c[1] && mouseY < c[3];
				g.fill(c[0], c[1], c[2], c[3], hot ? 0xFF3A6A3A : 0xFF1E3A22);
				g.fill(c[0], c[1], c[2], c[1] + 1, GOLD_DIM);
			}
		}
	}

	/** The track's picture as a short list of rectangles {x, y, w, h, colour}, worked out once rather than every frame. */
	private static java.util.List<int[]> trackPicture;

	private java.util.List<int[]> trackPicture() {
		if (trackPicture != null) {
			return trackPicture;
		}
		int w = trackW();
		int h = trackH();
		java.util.List<int[]> rects = new java.util.ArrayList<>();
		java.util.Map<Long, int[]> open = new java.util.HashMap<>();
		for (int y = 0; y < h; y++) {
			// This row as runs of one colour.
			java.util.List<int[]> runs = new java.util.ArrayList<>();
			int[] run = null;
			for (int x = 0; x < w; x++) {
				double mx = 4 + x * 56.0 / w;
				double mz = 4 + (h - y) * 42.0 / h;
				double cx = Math.max(19, Math.min(45, mx));
				double d = Math.hypot(mx - cx, mz - 25);
				int c;
				if (d < 8.8) {
					c = (((x & ~1) / 8 + y / 8) % 2 == 0) ? 0xFF3C8C3A : 0xFF347E32;
				} else if (d < 9.3 || (d > 18.1 && d < 18.6)) {
					c = 0xFFF4EEDC;
				} else if (d < 18.1) {
					c = 0xFFA87442;
				} else {
					c = 0;
				}
				if (run != null && run[4] == c) {
					run[2]++;
				} else {
					run = new int[] {x, y, 1, 1, c};
					if (c != 0) {
						runs.add(run);
					}
				}
			}
			// Runs the same as the one just above grow downwards instead of starting a new rectangle.
			java.util.Map<Long, int[]> next = new java.util.HashMap<>();
			for (int[] r : runs) {
				if (r[4] == 0) {
					continue;
				}
				long key = ((long) r[0] << 40) | ((long) r[2] << 32) | (r[4] & 0xFFFFFFFFL);
				int[] above = open.get(key);
				if (above != null) {
					above[3]++;
					next.put(key, above);
				} else {
					rects.add(r);
					next.put(key, r);
				}
			}
			open = next;
		}
		trackPicture = rects;
		return rects;
	}

	/** Model pixels (x across, z from the near edge) to the screen's track view. */
	private int[] toScreen(double mx, double mz) {
		int x = trackX0() + (int) Math.round((mx - 4) * trackW() / 56.0);
		int y = trackY0() + trackH() - (int) Math.round((mz - 4) * trackH() / 42.0);
		return new int[] {x, y};
	}

	@Override
	protected void drawTable(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		Derby.Phase phase = phase();
		boolean running = phase == Derby.Phase.RACING || phase == Derby.Phase.RESULT;
		double t = phase == Derby.Phase.RESULT ? 1.2 : Math.max(0, Math.min(1.2, raceFraction()));
		int[] place = Derby.places(order);
		// The runners on the track, the leader drawn last (on top).
		Integer[] byPlace = new Integer[Derby.HORSES];
		for (int h = 0; h < Derby.HORSES; h++) {
			byPlace[h] = h;
		}
		java.util.Arrays.sort(byPlace, (p, q) -> Integer.compare(place[q], place[p]));
		for (int h : byPlace) {
			double lap = running && ordered ? Math.min(1.06, Derby.progress(seed, h, place[h], t)) : -0.012;
			double[] at = Derby.lanePoint(lap, h);
			int[] s = toScreen(at[0], at[1]);
			g.fill(s[0] - 3, s[1] - 3, s[0] + 4, s[1] + 4, 0xFF0A0A0A);
			g.fill(s[0] - 2, s[1] - 2, s[0] + 3, s[1] + 3, Derby.SILKS[h]);
		}
		// Status over the track.
		String status = switch (phase) {
			case IDLE -> "Race " + (raceNumber + 1) + ": place your bets";
			case BETTING -> "Race " + (raceNumber + 1) + " - off in " + seconds() + "s";
			case RACING -> t < 1.0 ? "And they're off!" : "Photo finish...";
			case RESULT -> ordered ? "Winner: " + Derby.NAMES[names[order[0]]] : "";
		};
		g.text(font, font.plainSubstrByWidth(status, trackW()), trackX0(), trackY0() + trackH() + 5, phase == Derby.Phase.RESULT ? GOLD : TAN,
				true);
		if (phase == Derby.Phase.RESULT && ordered) {
			String podium = "2nd " + Derby.NAMES[names[order[1]]] + " · 3rd " + Derby.NAMES[names[order[2]]];
			g.text(font, font.plainSubstrByWidth(podium, trackW()), trackX0(), trackY0() + trackH() + 16, DIM, false);
		}
		// The card's header.
		int hy = tableTop() + 8;
		g.text(font, "Horse", cardX0() + 14, hy, DIM, false);
		g.text(font, "Form", formX(), hy, DIM, false);
		for (int pool = 0; pool < 3; pool++) {
			int[] c = priceCell(pool, 0);
			g.text(font, Derby.POOLS[pool], (c[0] + c[2]) / 2 - font.width(Derby.POOLS[pool]) / 2, hy, CREAM, false);
		}
		for (int h = 0; h < Derby.HORSES; h++) {
			int y = rowY(h);
			boolean won = phase == Derby.Phase.RESULT && ordered && order[0] == h && (ticks / 4) % 2 == 0;
			g.fill(cardX0() + 2, y + 4, cardX0() + 10, y + 14, Derby.SILKS[h]);
			String name = font.plainSubstrByWidth(Derby.NAMES[names[h]], formX() - cardX0() - 18);
			g.text(font, name, cardX0() + 14, y + 5, won ? GOLD : CREAM, false);
			g.text(font, form[h], formX(), y + 5, DIM, false);
			for (int pool = 0; pool < 3; pool++) {
				int[] c = priceCell(pool, h);
				String price = String.format(java.util.Locale.ROOT, "x%.1f", prices[pool][h]);
				long stake = 0;
				for (MyBet bet : mine) {
					if (bet.pool() == pool && bet.horse() == h) {
						stake += bet.amount();
					}
				}
				g.text(font, price, (c[0] + c[2]) / 2 - font.width(price) / 2, c[1] + (stake > 0 ? 2 : 5), CREAM, false);
				if (stake > 0) {
					String on = shortAmount(stake);
					g.text(font, on, (c[0] + c[2]) / 2 - font.width(on) / 2, c[1] + 10, GOLD, false);
				}
			}
		}
		// Recent winners, in their silks.
		int wx = cardX0();
		int wy = rowY(Derby.HORSES - 1) + 24;
		g.text(font, "Last:", wx, wy, DIM, false);
		for (int i = 0; i < history.size(); i++) {
			int x = wx + 30 + i * 10;
			g.fill(x, wy, x + 8, wy + 8, Derby.SILKS[history.get(history.size() - 1 - i)]);
		}
		// The rule, on the same line at the right, so it never runs into the buttons below.
		String rule = "Stake back incl. Cut " + cut + "%";
		int ruleX = cardX1() - font.width(rule);
		if (ruleX > wx + 30 + history.size() * 10 + 6) {
			g.text(font, rule, ruleX, wy, DIM, false);
		}
	}
}
