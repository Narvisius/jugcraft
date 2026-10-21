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
 * The baccarat table: Player's and Banker's hands dealt one card at a time across the top, the three betting boxes
 * (Player 1:1, Banker 0.95:1, Tie 8:1) on the felt below, and the bead road of past results. Click a box to bet.
 */
public class BaccaratScreen extends CasinoScreen {
	private static final int[] SIDE_COLOURS = {0xFF3A6EA5, 0xFFC0362C, 0xFF2E8B46};

	private record BetView(int side, long amount, boolean mine) {
	}

	private int winner = -1;
	private final List<Integer> playerCards = new ArrayList<>();
	private final List<Integer> bankerCards = new ArrayList<>();
	private final List<Integer> history = new ArrayList<>();
	private final List<BetView> bets = new ArrayList<>();
	private String shape = "";

	public BaccaratScreen(CasinoPayload data) {
		super(data);
		parse();
	}

	private void parse() {
		CasinoPayload.Reader r = new CasinoPayload.Reader(data.data());
		winner = r.nextInt();
		r.next();
		int p = r.nextInt();
		playerCards.clear();
		for (int i = 0; i < p; i++) {
			playerCards.add(r.nextInt());
		}
		int b = r.nextInt();
		bankerCards.clear();
		for (int i = 0; i < b; i++) {
			bankerCards.add(r.nextInt());
		}
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

	private ShowGames.Baccarat.Phase phase() {
		return ShowGames.Baccarat.Phase.values()[Math.max(0, Math.min(3, data.phase()))];
	}

	@Override
	protected boolean needsRebuild(CasinoPayload before, CasinoPayload after) {
		parse();
		String now = phase() + "/" + bets.stream().anyMatch(BetView::mine);
		return !now.equals(shape);
	}

	@Override
	protected int preferredWidth() {
		return 440;
	}

	@Override
	protected int preferredHeight() {
		return 290;
	}

	private int[] sideBox(int side) {
		int x0 = left + FRAME + 20;
		int w = (panelWidth - 2 * FRAME - 40 - 16) / 3;
		int y = tableTop() + 104;
		int order = side == ShowGames.Baccarat.TIE ? 1 : side == ShowGames.Baccarat.PLAYER ? 0 : 2;
		return new int[] {x0 + order * (w + 8), y, x0 + order * (w + 8) + w, y + 40};
	}

	@Override
	protected void initGame() {
		shape = phase() + "/" + bets.stream().anyMatch(BetView::mine);
		int by = tableBottom() - 18;
		int x = addChipRack(left + FRAME + 8, by);
		boolean mine = bets.stream().anyMatch(BetView::mine);
		addRenderableWidget(new Plate(x + 6, by, 70, 14, "Take back", Plate.Kind.WOOD,
				b -> send(Action.CASINO_ACT, 0, "", 0))).active = mine && phase() != ShowGames.Baccarat.Phase.DEALING;
		addRenderableWidget(new Plate(x + 80, by, 50, 14, "Deal", Plate.Kind.BRASS,
				b -> send(Action.CASINO_ACT, 1, "", 0))).active = mine && phase() == ShowGames.Baccarat.Phase.BETTING;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (settingsOpen()) {
			return super.mouseClicked(event, doubleClick);
		}
		if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && phase() != ShowGames.Baccarat.Phase.DEALING) {
			for (int side = 0; side < 3; side++) {
				int[] r = sideBox(side);
				if (event.x() >= r[0] && event.x() < r[2] && event.y() >= r[1] && event.y() < r[3]) {
					send(Action.CASINO_BET, side, "", chip);
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	protected void drawTableBack(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		int x0 = left + FRAME + 8;
		int x1 = left + panelWidth - FRAME - 8;
		int y0 = tableTop();
		int y1 = tableBottom() - 22;
		g.fill(x0 - 3, y0 - 3, x1 + 3, y1 + 3, RAIL);
		g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, GOLD_DIM);
		g.fill(x0, y0, x1, y1, FELT);
		for (int side = 0; side < 3; side++) {
			int[] r = sideBox(side);
			boolean hot = mouseX >= r[0] && mouseX < r[2] && mouseY >= r[1] && mouseY < r[3];
			g.fill(r[0] - 1, r[1] - 1, r[2] + 1, r[3] + 1, WHITE);
			g.fill(r[0], r[1], r[2], r[3], hot ? FELT_LIGHT : FELT_DARK);
			g.fill(r[0], r[1], r[0] + 4, r[3], SIDE_COLOURS[side]);
		}
	}

	/** How many of the dealt cards to show so far: one every twenty ticks while dealing. */
	private int shown() {
		if (phase() != ShowGames.Baccarat.Phase.DEALING) {
			return 6;
		}
		return Math.min(6, 1 + (ShowGames.Baccarat.DEAL_TICKS - timer) / 16);
	}

	@Override
	protected void drawTable(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		int shown = shown();
		// Deal order: P, B, P, B, then Player's third, then Banker's.
		List<Integer> p = new ArrayList<>();
		List<Integer> b = new ArrayList<>();
		int dealt = 0;
		for (int i = 0; i < 2; i++) {
			if (dealt++ < shown && i < playerCards.size()) {
				p.add(playerCards.get(i));
			}
			if (dealt++ < shown && i < bankerCards.size()) {
				b.add(bankerCards.get(i));
			}
		}
		if (playerCards.size() > 2 && dealt++ < shown) {
			p.add(playerCards.get(2));
		}
		if (bankerCards.size() > 2 && dealt < shown) {
			b.add(bankerCards.get(2));
		}
		int midX = left + panelWidth / 2;
		drawHand(g, "PLAYER", p, midX - 110, tableTop() + 14, SIDE_COLOURS[0], winner == ShowGames.Baccarat.PLAYER && shown >= 6);
		drawHand(g, "BANKER", b, midX + 20, tableTop() + 14, SIDE_COLOURS[1], winner == ShowGames.Baccarat.BANKER && shown >= 6);
		for (int side = 0; side < 3; side++) {
			int[] r = sideBox(side);
			String name = ShowGames.Baccarat.SIDES[side].toUpperCase(java.util.Locale.ROOT);
			String pays = side == ShowGames.Baccarat.BANKER ? "pays 0.95 to 1" : side == ShowGames.Baccarat.TIE ? "pays 8 to 1"
					: "pays 1 to 1";
			g.text(font, name, (r[0] + r[2]) / 2 - font.width(name) / 2, r[1] + 6, WHITE, true);
			g.text(font, pays, (r[0] + r[2]) / 2 - font.width(pays) / 2, r[1] + 17, 0xFF9DB89F, false);
			long total = 0;
			long mine = 0;
			for (BetView bet : bets) {
				if (bet.side() == side) {
					total += bet.amount();
					if (bet.mine()) {
						mine += bet.amount();
					}
				}
			}
			if (total > 0) {
				chip(g, r[0] + 8, r[3] - 12, total, false);
				String on = mine > 0 ? "yours " + shortAmount(mine) : shortAmount(total);
				g.text(font, on, r[0] + 20, r[3] - 11, mine > 0 ? GOLD : TAN, false);
			}
		}
		String status = switch (phase()) {
			case IDLE -> "Bet Player, Banker or Tie.";
			case BETTING -> "Cards in " + seconds() + "s";
			case DEALING -> "Dealing...";
			case RESULT -> winner < 0 ? "" : winner == ShowGames.Baccarat.TIE ? "A tie!" : ShowGames.Baccarat.SIDES[winner] + " wins!";
		};
		g.text(font, status, midX - font.width(status) / 2, tableTop() + 88, phase() == ShowGames.Baccarat.Phase.RESULT ? GOLD : TAN,
				true);
		// Bead road.
		int hx = left + FRAME + 14;
		int hy = sideBox(0)[3] + 10;
		g.text(font, "Road:", hx, hy + 1, DIM, false);
		for (int i = 0; i < history.size(); i++) {
			int x = hx + 30 + i * 12;
			oval(g, x + 5, hy + 5, 5, 5, SIDE_COLOURS[history.get(i)]);
		}
		long mineTotal = 0;
		for (BetView bet : bets) {
			if (bet.mine()) {
				mineTotal += bet.amount();
			}
		}
		if (mineTotal > 0) {
			String on = "Your bets: " + FinanceBook.amount(mineTotal);
			g.text(font, on, left + panelWidth - FRAME - 14 - font.width(on), hy + 1, GOLD, false);
		}
	}

	private void drawHand(GuiGraphicsExtractor g, String title, List<Integer> cards, int x, int y, int colour, boolean won) {
		g.text(font, title, x, y, won && (ticks / 4) % 2 == 0 ? GOLD : colour, true);
		if (!cards.isEmpty()) {
			String total = String.valueOf(ShowGames.Baccarat.total(cards));
			g.text(font, total, x + 82 - font.width(total), y, CREAM, true);
		}
		for (int i = 0; i < cards.size(); i++) {
			card(g, x + i * 30, y + 12, 26, 36, cards.get(i));
		}
	}

	private int lastShown;

	@Override
	public void tick() {
		super.tick();
		int now = phase() == ShowGames.Baccarat.Phase.DEALING ? shown() : 0;
		if (now > lastShown) {
			CasinoSound.card(data.pos());
		}
		lastShown = now;
	}

	@Override
	protected CasinoTheme theme() {
		return CasinoTheme.EMERALD;
	}
}
