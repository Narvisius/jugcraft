package io.github.jimbozoomer.jugcraft.client;

import static io.github.jimbozoomer.jugcraft.client.ExchangeScreen.*;

import io.github.jimbozoomer.jugcraft.finance.CasinoPayload;
import io.github.jimbozoomer.jugcraft.finance.FinanceActionPayload.Action;
import io.github.jimbozoomer.jugcraft.finance.FinanceBook;
import io.github.jimbozoomer.jugcraft.finance.ShowGames;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The lottery kiosk: the pot on a lit sign, your tickets and the days to the draw, buttons to buy 1, 5 or 10 tickets,
 * and the three scratch cards for sale with their prizes.
 */
public class LotteryScreen extends CasinoScreen {
	private long pot;
	private long tickets;
	private long mine;
	private long price;
	private long daysLeft;
	private long lastPrize;

	public LotteryScreen(CasinoPayload data) {
		super(data);
		parse();
	}

	private void parse() {
		CasinoPayload.Reader r = new CasinoPayload.Reader(data.data());
		pot = r.next();
		tickets = r.next();
		mine = r.next();
		price = r.next();
		daysLeft = r.next();
		lastPrize = r.next();
	}

	@Override
	protected boolean needsRebuild(CasinoPayload before, CasinoPayload after) {
		parse();
		return before.data().length > 3 && before.data()[3] != price;
	}

	@Override
	protected int preferredWidth() {
		return 420;
	}

	@Override
	protected int preferredHeight() {
		return 290;
	}

	private int cardX(int k) {
		int w = (panelWidth - 2 * FRAME - 40) / 3;
		return left + FRAME + 14 + k * (w + 6);
	}

	private int cardW() {
		return (panelWidth - 2 * FRAME - 40) / 3;
	}

	private int cardsY() {
		return tableTop() + 110;
	}

	@Override
	protected void initGame() {
		int y = tableTop() + 80;
		int x = left + panelWidth / 2 - 120;
		int[] counts = {1, 5, 10};
		for (int i = 0; i < 3; i++) {
			int count = counts[i];
			addRenderableWidget(new Plate(x + i * 82, y, 78, 16, "Buy " + count + " (" + shortAmount(price * count) + ")",
					Plate.Kind.BRASS, b -> send(Action.CASINO_ACT, 0, String.valueOf(count), 0)));
		}
		for (ShowGames.ScratchCard card : ShowGames.ScratchCard.values()) {
			int k = card.ordinal();
			addRenderableWidget(new Plate(cardX(k) + 6, cardsY() + 62, cardW() - 12, 16, "Buy for " + shortAmount(card.price),
					Plate.Kind.WOOD, b -> send(Action.CASINO_ACT, 1, String.valueOf(k), 0)));
		}
	}

	@Override
	protected void drawTableBack(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		int x0 = left + FRAME + 10;
		int x1 = left + panelWidth - FRAME - 10;
		int y0 = tableTop();
		// The pot sign.
		g.fill(x0, y0, x1, y0 + 70, 0xFF0A0A2A);
		g.fill(x0 + 2, y0 + 2, x1 - 2, y0 + 68, 0xFF141448);
		for (int i = 0; i < (x1 - x0) / 8; i++) {
			boolean on = (i + ticks / 4) % 4 == 0;
			g.fill(x0 + 2 + i * 8, y0 + 2, x0 + 5 + i * 8, y0 + 5, on ? 0xFFFFE070 : 0xFF3A3A8A);
			g.fill(x0 + 2 + i * 8, y0 + 65, x0 + 5 + i * 8, y0 + 68, on ? 0xFFFFE070 : 0xFF3A3A8A);
		}
		// The three cards.
		int[] faces = {0xFFB86A3A, 0xFFA8A8B0, 0xFFE8C463};
		for (int k = 0; k < 3; k++) {
			int x = cardX(k);
			g.fill(x, cardsY(), x + cardW(), cardsY() + 82, 0xFF1A110A);
			g.fill(x + 1, cardsY() + 1, x + cardW() - 1, cardsY() + 81, faces[k]);
			g.fill(x + 6, cardsY() + 22, x + cardW() - 6, cardsY() + 58, 0xFFC8CCD2);
			for (int yy = cardsY() + 24; yy < cardsY() + 56; yy += 4) {
				g.fill(x + 8, yy, x + cardW() - 8, yy + 1, 0xFFE6E9EE);
			}
		}
	}

	@Override
	protected void drawTable(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		int cx = left + panelWidth / 2;
		int y0 = tableTop();
		g.text(font, "LOTTERY POT", cx - font.width("LOTTERY POT") / 2, y0 + 10, 0xFFB0B0FF, false);
		String potText = FinanceBook.amount(pot) + " Jugs";
		g.text(font, potText, cx - font.width(potText) / 2, y0 + 24, (ticks / 8) % 2 == 0 ? 0xFFFFE070 : 0xFFFFC040, true);
		String info = tickets + " ticket" + (tickets == 1 ? "" : "s") + " in · you hold " + mine + " · draw in " + daysLeft + " day"
				+ (daysLeft == 1 ? "" : "s");
		g.text(font, info, cx - font.width(info) / 2, y0 + 40, CREAM, false);
		long cut = table(10);
		String rule = "One ticket wins the whole pot. Tickets cost " + FinanceBook.amount(price) + "; " + (100 - cut)
				+ "% of each goes to the pot.";
		g.text(font, font.plainSubstrByWidth(rule, panelWidth - 40), left + FRAME + 16, y0 + 54, DIM, false);
		if (lastPrize > 0 && !data.text().isEmpty()) {
			String last = "Last draw: " + data.text().get(0) + " won " + FinanceBook.amount(lastPrize);
			g.text(font, last, cx - font.width(last) / 2, y0 + 99, TAN, false);
		}
		for (ShowGames.ScratchCard card : ShowGames.ScratchCard.values()) {
			int x = cardX(card.ordinal());
			String title = card.title.replace(" Scratch Card", "");
			g.text(font, title.toUpperCase(java.util.Locale.ROOT), x + cardW() / 2 - font.width(title.toUpperCase(java.util.Locale.ROOT)) / 2,
					cardsY() + 5, 0xFF1A0A0A, false);
			long top = card.prizes[card.prizes.length - 2];
			String win = "Win up to " + shortAmount(top);
			g.text(font, win, x + cardW() / 2 - font.width(win) / 2, cardsY() + 14, 0xFF3A1A0A, false);
			String back = String.format(java.util.Locale.ROOT, "Pays back %.0f%%", card.returnToPlayer() * 100);
			g.text(font, back, x + cardW() / 2 - font.width(back) / 2, cardsY() + 36, 0xFF3A3A42, false);
		}
	}

	@Override
	protected CasinoTheme theme() {
		return CasinoTheme.LOTTERY;
	}
}
