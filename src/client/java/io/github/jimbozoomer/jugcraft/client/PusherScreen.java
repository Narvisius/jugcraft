package io.github.jimbozoomer.jugcraft.client;

import static io.github.jimbozoomer.jugcraft.client.ExchangeScreen.*;

import io.github.jimbozoomer.jugcraft.finance.CasinoPayload;
import io.github.jimbozoomer.jugcraft.finance.CoinPusher;
import io.github.jimbozoomer.jugcraft.finance.FinanceActionPayload.Action;
import io.github.jimbozoomer.jugcraft.finance.FinanceBook;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The coin pusher seen through its glass: the shelf heaped with Jugs, the brass pusher sliding back and forth, and the
 * tray below. Drop a coin of the picked size; now and then the pusher shoves a share of the heap over the edge, nine
 * tenths of it into your tray.
 */
public class PusherScreen extends CasinoScreen {
	private long shelf;
	private long drops = -1;
	private long lastWin;
	private long lastSpill;
	/** The size of the coin dropped last (for how big a win the catch was). */
	private long lastDrop = 1;
	/** Ticks since the last drop (the coin falls, then any spill tumbles). */
	private int age = 1000;

	public PusherScreen(CasinoPayload data) {
		super(data);
		parse(true);
	}

	private void parse(boolean first) {
		CasinoPayload.Reader r = new CasinoPayload.Reader(data.data());
		shelf = r.next();
		long newDrops = r.next();
		lastWin = r.next();
		lastSpill = r.next();
		if (!first && newDrops != drops) {
			age = 0;
		}
		drops = newDrops;
	}

	@Override
	protected boolean needsRebuild(CasinoPayload before, CasinoPayload after) {
		parse(false);
		return false;
	}

	/** The coin is falling and the heap settling: what was caught shows once it lands in the tray. */
	@Override
	protected boolean revealing() {
		return age < 30;
	}

	@Override
	public void tick() {
		super.tick();
		age++;
		long pos = data.pos();
		if (age == 30) {
			celebrate(lastWin, Math.max(1, lastDrop), false);
		}
		if (age == 1) {
			CasinoSound.coin(pos, 0.8F);
		} else if (age == 12) {
			// The coin lands on the heap.
			CasinoSound.play(pos, net.minecraft.sounds.SoundEvents.CHAIN_PLACE, 0.4F, 1.8F);
		} else if (lastSpill > 0 && age >= 14 && age < 40 && age % 3 == 0) {
			// Coins tumbling over the edge into the tray.
			CasinoSound.coin(pos, 0.9F + (age % 7) * 0.08F);
		}
	}

	@Override
	protected boolean ownCelebration() {
		return true;
	}

	@Override
	protected int preferredWidth() {
		return 380;
	}

	@Override
	protected int preferredHeight() {
		return 290;
	}

	@Override
	protected void initGame() {
		int by = tableBottom() - 18;
		addChipRack(left + FRAME + 8, by);
		addRenderableWidget(new Plate(left + panelWidth - FRAME - 80, by - 4, 72, 20, "DROP", Plate.Kind.BRASS, b -> {
			lastDrop = chip;
			send(Action.CASINO_ACT, 0, "", chip);
		}));
	}

	private int gx0() {
		return left + FRAME + 30;
	}

	private int gx1() {
		return left + panelWidth - FRAME - 30;
	}

	private int shelfY() {
		return tableTop() + 100;
	}

	@Override
	protected void drawTableBack(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		int x0 = gx0();
		int x1 = gx1();
		int y0 = tableTop() + 6;
		int y1 = tableBottom() - 26;
		// Cabinet.
		g.fill(x0 - 12, y0 - 6, x1 + 12, y1 + 4, 0xFF5A1A6A);
		g.fill(x0 - 10, y0 - 4, x1 + 10, y1 + 2, 0xFF7A2A8A);
		for (int i = 0; i < (x1 - x0 + 20) / 8; i++) {
			boolean on = (i + ticks / 5) % 3 == 0;
			g.fill(x0 - 8 + i * 8, y0 - 3, x0 - 5 + i * 8, y0, on ? 0xFFFFE070 : 0xFF9A5AAA);
		}
		// Glass back.
		g.fill(x0, y0 + 4, x1, y1 - 2, 0xFF1A1428);
		// The back wall the pusher slides from, and the shelf.
		g.fill(x0, shelfY(), x1, shelfY() + 6, 0xFF8A8A92);
		g.fill(x0, shelfY() + 6, x1, shelfY() + 8, 0xFF5A5A62);
		// Tray.
		g.fill(x0 + 10, y1 - 26, x1 - 10, y1 - 6, 0xFF3A3A42);
		g.fill(x0 + 12, y1 - 24, x1 - 12, y1 - 8, 0xFF24242A);
	}

	private void coin(GuiGraphicsExtractor g, int x, int y) {
		g.fill(x, y, x + 9, y + 3, 0xFF8A6A1C);
		g.fill(x, y, x + 9, y + 2, GOLD);
		g.fill(x + 1, y, x + 3, y + 1, 0xFFFFF0A0);
	}

	@Override
	protected void drawTable(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		int x0 = gx0();
		int x1 = gx1();
		// The heap: more coins the more is on the shelf (a log scale so a big heap still fits).
		int heap = (int) Math.min(120, Math.round(Math.log10(1 + shelf) * 30));
		int across = (x1 - x0 - 20) / 10;
		for (int i = 0; i < heap; i++) {
			int col = (i * 7) % Math.max(1, across);
			int layer = i / Math.max(1, across);
			coin(g, x0 + 10 + col * 10 + (layer % 2) * 5, shelfY() - 3 - layer * 3);
		}
		// The pusher block, sliding.
		double slide = (Math.sin((ticks + partial) * 0.08) + 1) / 2;
		int py = tableTop() + 30 + (int) (slide * 30);
		g.fill(x0 + 4, tableTop() + 14, x1 - 4, py, 0xFFB8913F);
		g.fill(x0 + 4, py - 3, x1 - 4, py, 0xFF8A6A2C);
		// The coin falling onto the shelf.
		if (age < 12) {
			int cy = tableTop() + 14 + age * 7;
			coin(g, (x0 + x1) / 2 - 4, Math.min(cy, shelfY() - 6));
		}
		// The spill tumbling into the tray.
		if (lastSpill > 0 && age >= 10 && age < 50) {
			int n = (int) Math.min(20, 2 + Math.log10(1 + lastSpill) * 5);
			for (int i = 0; i < n; i++) {
				int t = age - 10;
				int cx = x0 + 16 + (i * 37) % (x1 - x0 - 30);
				int cy = Math.min(shelfY() + 10 + t * 4 + (i % 5) * 3, tableBottom() - 52);
				coin(g, cx, cy);
			}
		}
		String shelfText = "On the shelf: " + FinanceBook.amount(shelf);
		g.text(font, shelfText, (x0 + x1) / 2 - font.width(shelfText) / 2, shelfY() + 12, TAN, true);
		String result = age < 1000 && age >= 30 ? (lastWin > 0 ? "CAUGHT " + FinanceBook.amount(lastWin) : "Nothing fell...") : "";
		g.text(font, result, (x0 + x1) / 2 - font.width(result) / 2, tableBottom() - 46, lastWin > 0 ? GAIN : DIM, true);
		String rule = CoinPusher.PLAYER_SHARE_PERCENT + "% of what falls is yours; the rest falls off the sides.";
		g.text(font, font.plainSubstrByWidth(rule, panelWidth - 40), left + FRAME + 12, tableBottom() - 36, DIM,
				false);
	}

	@Override
	protected CasinoTheme theme() {
		return CasinoTheme.PUSHER;
	}
}
