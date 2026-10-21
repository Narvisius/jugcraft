package io.github.jimbozoomer.jugcraft.client;

import static io.github.jimbozoomer.jugcraft.client.ExchangeScreen.*;

import io.github.jimbozoomer.jugcraft.finance.CasinoPayload;
import io.github.jimbozoomer.jugcraft.finance.Craps;
import io.github.jimbozoomer.jugcraft.finance.FinanceActionPayload.Action;
import io.github.jimbozoomer.jugcraft.finance.FinanceBook;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * The craps layout: place numbers across the top (with the puck on the point), Come, the Field, Don't Pass and the
 * Pass Line below, and the proposition bets on the right. Click an area to put the picked chip there. Your bets are
 * listed under the felt with buttons for odds and for taking down what may come down. The shooter rolls; otherwise the
 * dice go when the betting clock runs out.
 */
public class CrapsScreen extends CasinoScreen {
	private static final int[] PLACES = {4, 5, 6, 8, 9, 10};

	private record BetView(int index, Craps.Kind kind, int number, long amount, long odds, boolean mine) {
	}

	private record Area(int x0, int y0, int x1, int y1, Craps.Kind kind, int number, String label) {
		boolean contains(double x, double y) {
			return x >= x0 && x < x1 && y >= y0 && y < y1;
		}
	}

	private int point;
	private int die1;
	private int die2;
	private boolean shooter;
	private boolean canRoll;
	private final List<Integer> history = new ArrayList<>();
	private final List<BetView> bets = new ArrayList<>();
	private final List<Area> areas = new ArrayList<>();
	private String shape = "";

	public CrapsScreen(CasinoPayload data) {
		super(data);
		parse();
	}

	private void parse() {
		CasinoPayload.Reader r = new CasinoPayload.Reader(data.data());
		point = r.nextInt();
		die1 = r.nextInt();
		die2 = r.nextInt();
		r.next();
		shooter = r.nextBool();
		canRoll = r.nextBool();
		int h = r.nextInt();
		history.clear();
		for (int i = 0; i < h; i++) {
			history.add(r.nextInt());
		}
		int n = r.nextInt();
		bets.clear();
		for (int i = 0; i < n; i++) {
			int kind = r.nextInt();
			bets.add(new BetView(i, Craps.Kind.values()[Math.max(0, Math.min(Craps.Kind.values().length - 1, kind))], r.nextInt(),
					r.next(), r.next(), r.nextBool()));
		}
	}

	private Craps.Phase phase() {
		return Craps.Phase.values()[Math.max(0, Math.min(Craps.Phase.values().length - 1, data.phase()))];
	}

	private String buttonShape() {
		StringBuilder out = new StringBuilder().append(phase()).append(point).append(shooter).append(canRoll);
		for (BetView bet : bets) {
			if (bet.mine()) {
				out.append('|').append(bet.index()).append(bet.kind()).append(bet.number()).append(bet.odds() > 0);
			}
		}
		return out.toString();
	}

	@Override
	protected boolean needsRebuild(CasinoPayload before, CasinoPayload after) {
		parse();
		return !buttonShape().equals(shape);
	}

	@Override
	protected int preferredWidth() {
		return 470;
	}

	@Override
	protected int preferredHeight() {
		return 310;
	}

	// ------------------------------------------------------------------ layout

	private int feltX0() {
		return left + FRAME + 8;
	}

	private int feltX1() {
		return left + panelWidth - FRAME - 8;
	}

	private int feltY0() {
		return tableTop() + 2;
	}

	private int feltY1() {
		return feltY0() + 132;
	}

	private void layoutAreas() {
		areas.clear();
		int x0 = feltX0() + 4;
		int x1 = feltX1() - 4;
		int propsW = 116;
		int mx1 = x1 - propsW - 6;
		int y = feltY0() + 4;
		int w = (mx1 - x0) / 6;
		for (int i = 0; i < 6; i++) {
			int n = PLACES[i];
			areas.add(new Area(x0 + i * w, y, x0 + (i + 1) * w - 2, y + 28, Craps.Kind.PLACE, n,
					n == 6 ? "SIX" : n == 9 ? "NINE" : String.valueOf(n)));
		}
		y += 31;
		areas.add(new Area(x0, y, mx1, y + 24, Craps.Kind.COME, 0, "COME"));
		y += 27;
		areas.add(new Area(x0, y, mx1, y + 22, Craps.Kind.FIELD, 0, "FIELD  2·3·4·9·10·11·12"));
		y += 25;
		areas.add(new Area(x0, y, mx1 - 70, y + 14, Craps.Kind.DONT_PASS, 0, "DON'T PASS BAR 12"));
		areas.add(new Area(mx1 - 68, y, mx1, y + 14, Craps.Kind.DONT_COME, 0, "DON'T COME"));
		y += 17;
		areas.add(new Area(x0, y, mx1, y + 16, Craps.Kind.PASS, 0, "PASS LINE"));
		// Propositions.
		int px = mx1 + 6;
		int py = feltY0() + 4;
		areas.add(new Area(px, py, x1, py + 16, Craps.Kind.ANY_SEVEN, 0, "ANY SEVEN 4:1"));
		py += 18;
		int hw = propsW / 2 - 1;
		int[] hards = {6, 10, 8, 4};
		for (int i = 0; i < 4; i++) {
			int hx = px + (i % 2) * (hw + 2);
			int hy = py + (i / 2) * 22;
			int n = hards[i];
			areas.add(new Area(hx, hy, hx + hw, hy + 20, Craps.Kind.HARD, n, "HARD " + n + " " + (n == 4 || n == 10 ? "7" : "9") + ":1"));
		}
		py += 46;
		int tw = propsW / 3 - 1;
		areas.add(new Area(px, py, px + tw, py + 20, Craps.Kind.ACES, 0, "2 30:1"));
		areas.add(new Area(px + tw + 2, py, px + 2 * tw + 2, py + 20, Craps.Kind.YO, 0, "11 15:1"));
		areas.add(new Area(px + 2 * tw + 4, py, x1, py + 20, Craps.Kind.TWELVE, 0, "12 30:1"));
		py += 22;
		areas.add(new Area(px, py, x1, py + 16, Craps.Kind.ANY_CRAPS, 0, "ANY CRAPS 7:1"));
	}

	private Area areaFor(Craps.Kind kind, int number) {
		for (Area area : areas) {
			if (area.kind() == kind && (area.number() == number || area.number() == 0)) {
				return area;
			}
		}
		return null;
	}

	@Override
	protected void initGame() {
		shape = buttonShape();
		layoutAreas();
		int y = tableBottom() - 16;
		addChipRack(left + FRAME + 8, y);
		Plate roll = addRenderableWidget(new Plate(left + panelWidth - FRAME - 64, y, 56, 14, "Roll", Plate.Kind.BRASS,
				b -> send(Action.CASINO_ACT, 0, "", 0)));
		roll.active = shooter && canRoll && phase() == Craps.Phase.BETTING;
		roll.visible = shooter;
		// Your bets, with odds and take-down buttons.
		int ly = feltY1() + 6;
		int shown = 0;
		for (BetView bet : bets) {
			if (!bet.mine() || shown >= 3) {
				continue;
			}
			int rowY = ly + shown * 15;
			int bx = left + panelWidth - FRAME - 8;
			boolean line = bet.kind() == Craps.Kind.PASS || bet.kind() == Craps.Kind.DONT_PASS || bet.kind() == Craps.Kind.COME
					|| bet.kind() == Craps.Kind.DONT_COME;
			if (line && bet.number() != 0) {
				int index = bet.index();
				addRenderableWidget(new Plate(bx - 130, rowY, 70, 13, "Odds +" + shortAmount(chip), Plate.Kind.WOOD,
						b -> send(Action.CASINO_ACT, 1, String.valueOf(index), chip)));
			}
			boolean contract = (bet.kind() == Craps.Kind.PASS || bet.kind() == Craps.Kind.COME) && bet.number() != 0;
			if (!contract || bet.odds() > 0) {
				int index = bet.index();
				addRenderableWidget(new Plate(bx - 56, rowY, 56, 13, contract ? "Odds off" : "Take down", Plate.Kind.WOOD,
						b -> send(Action.CASINO_ACT, 2, String.valueOf(index), 0)));
			}
			shown++;
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (settingsOpen()) {
			return super.mouseClicked(event, doubleClick);
		}
		if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && phase() != Craps.Phase.ROLLING) {
			for (Area area : areas) {
				if (area.contains(event.x(), event.y())) {
					send(Action.CASINO_BET, area.kind().ordinal(), String.valueOf(area.number()), chip);
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	// ------------------------------------------------------------------ drawing

	@Override
	protected void drawTableBack(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		if (areas.isEmpty()) {
			layoutAreas();
		}
		int x0 = feltX0();
		int x1 = feltX1();
		int y0 = feltY0();
		int y1 = feltY1();
		g.fill(x0 - 4, y0 - 4, x1 + 4, y1 + 4, RAIL);
		g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, GOLD_DIM);
		g.fill(x0, y0, x1, y1, FELT);
		for (Area area : areas) {
			boolean hot = area.contains(mouseX, mouseY) && phase() != Craps.Phase.ROLLING;
			int edge = WHITE;
			int fill = switch (area.kind()) {
				case FIELD -> 0xFF35704A;
				case ANY_SEVEN, ANY_CRAPS, HARD, YO, ACES, TWELVE -> 0xFF2A5236;
				case DONT_PASS, DONT_COME -> 0xFF26472F;
				default -> FELT;
			};
			if (hot) {
				fill = FELT_LIGHT;
			}
			g.fill(area.x0(), area.y0(), area.x1(), area.y1(), edge);
			g.fill(area.x0() + 1, area.y0() + 1, area.x1() - 1, area.y1() - 1, fill);
			if (area.kind() == Craps.Kind.ANY_SEVEN || area.kind() == Craps.Kind.ANY_CRAPS) {
				g.fill(area.x0() + 1, area.y0() + 1, area.x1() - 1, area.y1() - 1, hot ? 0xFFC0463A : 0xFF8E2A22);
			}
		}
	}

	@Override
	protected void drawTable(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		for (Area area : areas) {
			int colour = area.kind() == Craps.Kind.FIELD ? YELLOW() : WHITE;
			String label = font.plainSubstrByWidth(area.label(), area.x1() - area.x0() - 4);
			int ty = area.kind() == Craps.Kind.PLACE ? area.y0() + 4 : area.y0() + (area.y1() - area.y0() - 8) / 2;
			g.text(font, label, (area.x0() + area.x1()) / 2 - font.width(label) / 2, ty, colour, false);
			if (area.kind() == Craps.Kind.PLACE) {
				long[] pay = {area.number() == 4 || area.number() == 10 ? 9 : 7, area.number() == 6 || area.number() == 8 ? 6 : 5};
				String odds = pay[0] + ":" + pay[1];
				g.text(font, odds, (area.x0() + area.x1()) / 2 - font.width(odds) / 2, area.y1() - 10, 0xFF9DB89F, false);
				if (area.number() == point) {
					puck(g, area.x0() + 1, area.y0() + 1, true);
				}
			}
		}
		if (point == 0) {
			Area pass = areaFor(Craps.Kind.PLACE, 4);
			if (pass != null) {
				puck(g, pass.x0() + 1, pass.y0() + 1, false);
			}
		}
		// Chips: line, come and field bets on their areas; come bets with a number on that number's box.
		for (BetView bet : bets) {
			Area area = bet.number() != 0 && (bet.kind() == Craps.Kind.COME || bet.kind() == Craps.Kind.DONT_COME)
					? areaFor(Craps.Kind.PLACE, bet.number()) : areaFor(bet.kind(), bet.number());
			if (area == null) {
				continue;
			}
			int cx = (area.x0() + area.x1()) / 2 + (bet.mine() ? -6 : 6) + (bet.kind() == Craps.Kind.COME ? -10 : 0);
			int cy = area.kind() == Craps.Kind.PLACE ? area.y0() + 13 : (area.y0() + area.y1()) / 2 - 4;
			if (bet.mine()) {
				g.fill(cx - 1, cy - 1, cx + 10, cy + 10, GOLD);
			}
			chip(g, cx, cy, bet.amount(), false);
			if (bet.odds() > 0) {
				chip(g, cx + 3, cy - 3, bet.odds(), false);
			}
		}
		// Dice.
		int dx = feltX1() - 116 - 6 - 44;
		int dy = feltY0() + 62;
		boolean rolling = phase() == Craps.Phase.ROLLING;
		int a = die1;
		int b = die2;
		if (rolling && timer > 12) {
			a = 1 + (ticks * 7 + 3) % 6;
			b = 1 + (ticks * 5 + 1) % 6;
		}
		if (a > 0) {
			die(g, dx, dy + (rolling ? (ticks % 3) : 0), a);
			die(g, dx + 18, dy + 2 - (rolling ? (ticks % 2) : 0), b);
		}
		// Status line above the felt's bottom edge.
		String status = switch (phase()) {
			case IDLE -> "Place a bet to start the game." + (point == 0 ? " Come-out roll next." : "");
			case BETTING -> (shooter ? "You are the shooter: roll when ready. " : "") + "Dice in " + seconds() + "s";
			case ROLLING -> "The dice are out!";
		};
		String who = data.text().isEmpty() || data.text().get(0).isEmpty() ? "" : "Shooter: " + data.text().get(0);
		g.text(font, who, feltX1() - 4 - font.width(who), feltY1() - 11, TAN, true);
		g.text(font, font.plainSubstrByWidth(status, feltX1() - feltX0() - font.width(who) - 12), feltX0() + 4, feltY1() - 11, CREAM,
				true);
		// Your bets, listed.
		int ly = feltY1() + 6;
		int shown = 0;
		int hidden = 0;
		for (BetView bet : bets) {
			if (!bet.mine()) {
				continue;
			}
			if (shown >= 3) {
				hidden++;
				continue;
			}
			String text = describe(bet);
			g.text(font, font.plainSubstrByWidth(text, panelWidth - 160), left + FRAME + 10, ly + shown * 15 + 3, CREAM, false);
			shown++;
		}
		if (hidden > 0) {
			g.text(font, "+" + hidden + " more", left + FRAME + 10, ly + 3 * 15 + 3, DIM, false);
		}
		if (shown == 0) {
			g.text(font, point == 0 ? "Come-out roll: bet the Pass Line or Don't Pass." : "The point is " + point
					+ ": bet Come, place a number, or the props.", left + FRAME + 10, ly + 3, DIM, false);
		}
		// Roll history.
		int hx = left + FRAME + 10;
		int hy = tableBottom() - 32;
		g.text(font, "Rolls:", hx, hy, DIM, false);
		int x = hx + 32;
		for (int i = history.size() - 1; i >= 0 && x < left + panelWidth - 40; i--) {
			int n = history.get(i);
			String label = String.valueOf(n);
			g.text(font, label, x, hy, n == 7 ? LOSS : n == point ? GOLD : CREAM, false);
			x += font.width(label) + 6;
		}
	}

	private static int YELLOW() {
		return 0xFFE0C03A;
	}

	private String describe(BetView bet) {
		String what = switch (bet.kind()) {
			case PASS -> bet.number() == 0 ? "Pass line" : "Pass line on " + bet.number();
			case DONT_PASS -> bet.number() == 0 ? "Don't pass" : "Don't pass against " + bet.number();
			case COME -> bet.number() == 0 ? "Come" : "Come on " + bet.number();
			case DONT_COME -> bet.number() == 0 ? "Don't come" : "Don't come against " + bet.number();
			case PLACE -> "Place " + bet.number();
			case FIELD -> "Field";
			case ANY_SEVEN -> "Any seven";
			case ANY_CRAPS -> "Any craps";
			case HARD -> "Hard " + bet.number();
			case YO -> "Yo (11)";
			case ACES -> "Aces (2)";
			case TWELVE -> "Twelve";
		};
		return what + ": " + FinanceBook.amount(bet.amount()) + (bet.odds() > 0 ? " + odds " + FinanceBook.amount(bet.odds()) : "");
	}

	/** The puck: a round marker, white "ON" on the point or black "OFF" with no point, its word inside it. */
	private void puck(GuiGraphicsExtractor g, int x, int y, boolean on) {
		int w = 15;
		int h = 9;
		int face = on ? WHITE : BLACK;
		int ink = on ? BLACK : WHITE;
		g.fill(x + 1, y, x + w - 1, y + h, 0xFF1A110A);
		g.fill(x, y + 1, x + w, y + h - 1, 0xFF1A110A);
		g.fill(x + 1, y + 1, x + w - 1, y + h - 1, face);
		String word = on ? "ON" : "OFF";
		int tw = word.length() * 4 - 1;
		int tx = x + (w - tw) / 2;
		for (int c = 0; c < word.length(); c++) {
			String[] glyph = switch (word.charAt(c)) {
				case 'O' -> new String[] {"XXX", "X.X", "X.X", "X.X", "XXX"};
				case 'N' -> new String[] {"X.X", "XXX", "XXX", "X.X", "X.X"};
				default -> new String[] {"XXX", "X..", "XX.", "X..", "X.."};
			};
			for (int row = 0; row < 5; row++) {
				for (int col = 0; col < 3; col++) {
					if (glyph[row].charAt(col) == 'X') {
						int px = tx + c * 4 + col;
						g.fill(px, y + 2 + row, px + 1, y + 3 + row, ink);
					}
				}
			}
		}
	}

	private void die(GuiGraphicsExtractor g, int x, int y, int face) {
		g.fill(x, y, x + 15, y + 15, 0xFF1A110A);
		g.fill(x + 1, y + 1, x + 14, y + 14, 0xFFF4F0E4);
		int[][] pips = switch (face) {
			case 1 -> new int[][] {{6, 6}};
			case 2 -> new int[][] {{3, 3}, {9, 9}};
			case 3 -> new int[][] {{3, 3}, {6, 6}, {9, 9}};
			case 4 -> new int[][] {{3, 3}, {9, 3}, {3, 9}, {9, 9}};
			case 5 -> new int[][] {{3, 3}, {9, 3}, {6, 6}, {3, 9}, {9, 9}};
			default -> new int[][] {{3, 3}, {9, 3}, {3, 6}, {9, 6}, {3, 9}, {9, 9}};
		};
		for (int[] p : pips) {
			g.fill(x + 1 + p[0], y + 1 + p[1], x + 3 + p[0], y + 3 + p[1], face == 1 ? 0xFFC0362C : 0xFF1C1A1A);
		}
	}

	private Craps.Phase lastPhase;

	@Override
	public void tick() {
		super.tick();
		long pos = data.pos();
		Craps.Phase now = phase();
		if (now == Craps.Phase.ROLLING && ticks % 3 == 0) {
			// The dice tumbling down the table.
			CasinoSound.dice(pos);
		} else if (lastPhase == Craps.Phase.ROLLING && now != Craps.Phase.ROLLING) {
			CasinoSound.play(pos, net.minecraft.sounds.SoundEvents.WOOD_HIT, 0.7F, 1.2F);
		}
		lastPhase = now;
	}

	@Override
	protected CasinoTheme theme() {
		return CasinoTheme.TABLE;
	}
}
