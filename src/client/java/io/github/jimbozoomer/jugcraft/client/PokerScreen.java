package io.github.jimbozoomer.jugcraft.client;

import static io.github.jimbozoomer.jugcraft.client.ExchangeScreen.*;

import io.github.jimbozoomer.jugcraft.finance.FinanceActionPayload;
import io.github.jimbozoomer.jugcraft.finance.FinanceActionPayload.Action;
import io.github.jimbozoomer.jugcraft.finance.FinanceBook;
import io.github.jimbozoomer.jugcraft.finance.PokerGame;
import io.github.jimbozoomer.jugcraft.finance.PokerHands;
import io.github.jimbozoomer.jugcraft.finance.PokerTablePayload;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The poker table's overlay: the table seen from above, in the Exchange's walnut and brass. Six seats round a felt
 * oval (your own seat at the bottom), the board and the pot in the middle, the dealer button, and the player to act
 * lit in gold with their time running down. Along the bottom: sitting down with a buy-in, or, on your turn, Fold,
 * Check or Call, Raise to an amount (with Min, Half pot and Pot), and All in. Everything comes from the server
 * ({@link PokerTablePayload}); no card is shown here that the server did not send.
 */
public class PokerScreen extends Screen {
	private static final int MAX_WIDTH = 440;
	private static final int MAX_HEIGHT = 270;
	private static final int FRAME = 4;
	private static final int HEADER = 22;
	private static final int BAR = 46;
	private static final int SEAT_W = 74;
	private static final int SEAT_H = 26;
	private static final int FELT = 0xFF2F5B3A;
	private static final int FELT_LIGHT = 0xFF3A6B47;
	private static final int RAIL = 0xFF5A1F17;
	private static final int CARD_FACE = 0xFFF2EBDD;
	private static final int CARD_RED = 0xFFC0362C;
	private static final int CARD_BLACK = 0xFF1E1E22;
	private static final int CARD_BACK = 0xFF8E2A2A;
	private static final String[] TITLES = {"The Gilded Oval", "The Saloon", "The Clockwork Dealer"};

	private PokerTablePayload data;
	private int left;
	private int top;
	private int panelWidth;
	private int panelHeight;
	private String raiseText = "";
	private String buyInText = "";
	private int timer;
	private boolean closing;
	/** The owner's settings panel: blinds, buy-ins and the turn clock (blank for the server's). */
	private boolean settingsOpen;
	private final String[] settingText = {"", "", "", "", ""};
	private static final String[] SETTING_NAMES = {"Small blind", "Big blind", "Least buy-in", "Most buy-in", "Turn clock"};

	public PokerScreen(PokerTablePayload data) {
		super(Component.literal("Poker"));
		this.data = data;
		this.timer = data.timerTicks();
		if (myTurn()) {
			raiseText = String.valueOf(data.minRaiseTo());
		}
	}

	public long pos() {
		return data.pos();
	}

	/** A fresh view from the server. */
	public void update(PokerTablePayload payload) {
		boolean myTurnBefore = myTurn();
		int seatBefore = data.mySeat();
		int phaseBefore = data.phase();
		data = payload;
		timer = payload.timerTicks();
		if (myTurn() && !myTurnBefore) {
			raiseText = String.valueOf(payload.minRaiseTo());
		}
		// Rebuild only when the buttons change, so typing in a box is not interrupted.
		if (!settingsOpen && (myTurn() != myTurnBefore || payload.mySeat() != seatBefore || payload.phase() != phaseBefore
				|| buttonsDiffer())) {
			rebuildWidgets();
		}
	}

	private int shownSeats = -1;

	private boolean buttonsDiffer() {
		int occupied = 0;
		for (int i = 0; i < data.seats().size(); i++) {
			if (data.seats().get(i).occupied()) {
				occupied |= 1 << i;
			}
		}
		boolean differ = occupied != shownSeats;
		shownSeats = occupied;
		return differ;
	}

	private boolean myTurn() {
		return data != null && data.mySeat() >= 0 && data.actor() == data.mySeat()
				&& data.phase() >= PokerGame.Phase.PREFLOP.ordinal() && data.phase() <= PokerGame.Phase.RIVER.ordinal();
	}

	private void send(Action action, int period, long amount) {
		ClientPlayNetworking.send(new FinanceActionPayload(action, "", amount, period, "", data.pos()));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		if (timer > 0) {
			timer--;
		}
	}

	@Override
	public void onClose() {
		if (!closing) {
			closing = true;
			send(Action.POKER_CLOSE, 0, 0);
		}
		super.onClose();
	}

	@Override
	public void removed() {
		if (!closing) {
			closing = true;
			send(Action.POKER_CLOSE, 0, 0);
		}
		super.removed();
	}

	// ------------------------------------------------------------------ layout

	private int tableTop() {
		return top + FRAME + HEADER + 6;
	}

	private int tableBottom() {
		return top + panelHeight - FRAME - BAR - 4;
	}

	private int centreX() {
		return left + panelWidth / 2;
	}

	private int centreY() {
		return (tableTop() + tableBottom()) / 2;
	}

	/** The felt oval's half sizes. */
	private int radiusX() {
		return panelWidth / 2 - SEAT_W / 2 - 18;
	}

	private int radiusY() {
		return (tableBottom() - tableTop()) / 2 - SEAT_H / 2 - 4;
	}

	/** Where a seat's plaque goes, counting from the player's own seat (shown at the bottom left). */
	private int[] seatXY(int seat) {
		int view = data.mySeat() >= 0 ? Math.floorMod(seat - data.mySeat(), PokerGame.SEATS) : seat;
		int cx = centreX();
		int cy = centreY();
		int rx = radiusX();
		int ry = radiusY();
		int bottom = cy + ry - SEAT_H / 2 + 6;
		int topY = cy - ry - SEAT_H / 2 - 6;
		return switch (view) {
			case 0 -> new int[] {cx - rx / 2 - SEAT_W / 2, bottom};
			case 1 -> new int[] {cx + rx / 2 - SEAT_W / 2, bottom};
			case 2 -> new int[] {cx + rx - SEAT_W / 2 + 10, cy - SEAT_H / 2};
			case 3 -> new int[] {cx + rx / 2 - SEAT_W / 2, topY};
			case 4 -> new int[] {cx - rx / 2 - SEAT_W / 2, topY};
			default -> new int[] {cx - rx - SEAT_W / 2 - 10, cy - SEAT_H / 2};
		};
	}

	@Override
	protected void init() {
		panelWidth = Math.min(MAX_WIDTH, width - 16);
		panelHeight = Math.min(MAX_HEIGHT, height - 16);
		left = (width - panelWidth) / 2;
		top = (height - panelHeight) / 2;
		if (data.isOwner()) {
			int sw = font.width("Settings") + 12;
			addRenderableWidget(new Plate(left + panelWidth - FRAME - sw - 6, top + FRAME + 4, sw, 13, settingsOpen ? "Close" : "Settings",
					Plate.Kind.WOOD, b -> {
						settingsOpen = !settingsOpen;
						long[] own = data.own();
						for (int i = 0; i < 5; i++) {
							settingText[i] = own != null && i < own.length && own[i] > 0 ? String.valueOf(own[i]) : "";
						}
						rebuildWidgets();
					}));
			if (settingsOpen) {
				int x = settingsX();
				int y = settingsY();
				for (int i = 0; i < 5; i++) {
					int k = i;
					EditBox box = new EditBox(font, x + 80, y + 14 + i * 18, 60, 14, Component.literal(SETTING_NAMES[i]));
					box.setMaxLength(9);
					box.setValue(settingText[i]);
					box.setResponder(t -> settingText[k] = t);
					addRenderableWidget(box);
				}
				addRenderableWidget(new Plate(x + 230, y + 104, 64, 16, "Save", Plate.Kind.BRASS, b -> {
					long[] v = new long[5];
					for (int i = 0; i < 5; i++) {
						v[i] = settingText[i].trim().isEmpty() ? 0 : Math.max(0, FinanceBook.parseAmount(settingText[i]));
					}
					ClientPlayNetworking.send(new FinanceActionPayload(Action.POKER_SETTINGS, v[1] + "," + v[2] + "," + v[3] + "," + v[4],
							v[0], 0, "", data.pos()));
					settingsOpen = false;
					rebuildWidgets();
				}));
				return;
			}
		}
		int seated = data.mySeat();
		// Empty seats offer a place while the player is not seated.
		for (int i = 0; i < data.seats().size(); i++) {
			if (!data.seats().get(i).occupied() && seated < 0) {
				int seat = i;
				int[] xy = seatXY(i);
				addRenderableWidget(new Plate(xy[0] + 8, xy[1] + 6, SEAT_W - 16, 14, "Sit here", Plate.Kind.BRASS, b -> {
					long buyIn = FinanceBook.parseAmount(buyInText);
					send(Action.POKER_SIT, seat, buyIn > 0 ? buyIn : defaultBuyIn());
				}));
			}
		}
		int bx = left + FRAME + 6;
		int by = top + panelHeight - FRAME - BAR + 8;
		int bw = panelWidth - 2 * FRAME - 12;
		if (seated < 0) {
			if (buyInText.isEmpty()) {
				buyInText = String.valueOf(defaultBuyIn());
			}
			EditBox buyIn = new EditBox(font, bx + 52, by + 14, 70, 14, Component.literal("Buy-in"));
			buyIn.setMaxLength(9);
			buyIn.setValue(buyInText);
			buyIn.setResponder(text -> buyInText = text);
			addRenderableWidget(buyIn);
			return;
		}
		if (myTurn()) {
			int w = 58;
			int gap = 4;
			long toCall = data.toCall();
			addRenderableWidget(new Plate(bx, by, w, 16, "Fold", Plate.Kind.WOOD, b -> send(Action.POKER_ACT, PokerGame.Move.FOLD.ordinal(), 0)));
			addRenderableWidget(new Plate(bx + w + gap, by, w + 10, 16, toCall > 0 ? "Call " + FinanceBook.amount(toCall) : "Check",
					Plate.Kind.BRASS, b -> send(Action.POKER_ACT, (toCall > 0 ? PokerGame.Move.CALL : PokerGame.Move.CHECK).ordinal(), 0)));
			int rx = bx + 2 * w + 10 + 2 * gap;
			EditBox raise = new EditBox(font, rx, by + 1, 54, 14, Component.literal("Raise to"));
			raise.setMaxLength(9);
			raise.setValue(raiseText);
			raise.setResponder(text -> raiseText = text);
			addRenderableWidget(raise);
			boolean canRaise = data.maxRaiseTo() > data.currentBet();
			Plate raiseButton = addRenderableWidget(new Plate(rx + 56, by, 50, 16, data.currentBet() > 0 ? "Raise" : "Bet",
					Plate.Kind.BRASS, b -> {
						long to = FinanceBook.parseAmount(raiseText);
						if (to > 0) {
							send(Action.POKER_ACT, PokerGame.Move.RAISE.ordinal(), to);
						}
					}));
			raiseButton.active = canRaise;
			Plate allIn = addRenderableWidget(new Plate(bx + bw - 56, by, 56, 16, "All in", Plate.Kind.WOOD,
					b -> send(Action.POKER_ACT, PokerGame.Move.ALL_IN.ordinal(), 0)));
			allIn.active = data.maxRaiseTo() > 0;
			// Quick sizes for the raise box.
			int qx = rx;
			int qy = by + 19;
			String[] labels = {"Min", "1/2 pot", "Pot"};
			for (int i = 0; i < labels.length; i++) {
				int which = i;
				Plate quick = addRenderableWidget(new Plate(qx + i * 36, qy, 34, 11, labels[i], Plate.Kind.WOOD, b -> {
					long to = switch (which) {
						case 0 -> data.minRaiseTo();
						case 1 -> data.currentBet() + Math.max(data.bigBlind(), (data.pot() + data.toCall()) / 2);
						default -> data.currentBet() + Math.max(data.bigBlind(), data.pot() + data.toCall());
					};
					raiseText = String.valueOf(Math.max(data.minRaiseTo(), Math.min(data.maxRaiseTo(), to)));
					rebuildWidgets();
				}));
				quick.active = canRaise;
			}
		} else {
			addRenderableWidget(new Plate(bx + bw - 70, by + 6, 70, 16, "Stand up", Plate.Kind.WOOD,
					b -> send(Action.POKER_STAND, 0, 0)));
		}
	}

	private long defaultBuyIn() {
		long most = Math.min(data.maxBuyIn(), data.cash());
		return Math.max(data.minBuyIn(), Math.min(most, data.bigBlind() * 50));
	}

	// ------------------------------------------------------------------ drawing

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractBackground(g, mouseX, mouseY, delta);
		int right = left + panelWidth;
		int bottom = top + panelHeight;
		g.fill(left - 3, top - 3, right + 3, bottom + 3, WALNUT_DARK);
		g.fill(left - 2, top - 2, right + 2, bottom + 2, BRASS);
		g.fill(left - 1, top - 1, right + 1, bottom + 1, WALNUT_DARK);
		raised(g, left, top, right, bottom, WALNUT, WALNUT_LIGHT, WALNUT_DARK);
		sunken(g, left + FRAME, top + FRAME, right - FRAME, top + FRAME + HEADER, PLAQUE, PLAQUE_DARK, PLAQUE_LIGHT);
		rivet(g, left + FRAME + 2, top + FRAME + 2);
		rivet(g, right - FRAME - 4, top + FRAME + 2);
		sunken(g, left + FRAME, bottom - FRAME - BAR, right - FRAME, bottom - FRAME, PLAQUE, PLAQUE_DARK, PLAQUE_LIGHT);
		// The table: a gold rim, a leather rail and the felt, drawn as filled ovals.
		int cx = centreX();
		int cy = centreY();
		int rx = radiusX();
		int ry = radiusY();
		oval(g, cx, cy, rx + 9, ry + 9, BRASS_DARK);
		oval(g, cx, cy, rx + 8, ry + 8, GOLD);
		oval(g, cx, cy, rx + 7, ry + 7, RAIL);
		oval(g, cx, cy, rx, ry, GOLD_DIM);
		oval(g, cx, cy, rx - 1, ry - 1, FELT);
		oval(g, cx, cy - 2, rx - 10, ry - 10, FELT_LIGHT);
		oval(g, cx, cy, rx - 11, ry - 11, FELT);
		filigree(g, left - 2, top - 2, 1, 1);
		filigree(g, right + 1, top - 2, -1, 1);
		filigree(g, left - 2, bottom + 1, 1, -1);
		filigree(g, right + 1, bottom + 1, -1, -1);
		// Seat plaques (under their buttons).
		for (int i = 0; i < data.seats().size(); i++) {
			PokerTablePayload.SeatView seat = data.seats().get(i);
			int[] xy = seatXY(i);
			boolean acting = i == data.actor();
			int edge = acting ? GOLD : seat.me() ? BRASS : WALNUT_LIGHT;
			g.fill(xy[0] - 1, xy[1] - 1, xy[0] + SEAT_W + 1, xy[1] + SEAT_H + 1, edge);
			g.fill(xy[0], xy[1], xy[0] + SEAT_W, xy[1] + SEAT_H, seat.occupied() ? (seat.folded() ? 0xFF241C16 : PLAQUE) : 0xAA1A110A);
		}
		if (settingsOpen) {
			// The settings panel's backing goes under the widgets, so its boxes and buttons show on top of it.
			int x = settingsX();
			int y = settingsY();
			g.fill(x - 9, y - 9, x + 309, y + 129, GOLD_DIM);
			g.fill(x - 8, y - 8, x + 308, y + 128, 0xFF241A12);
		}
	}

	private static void oval(GuiGraphicsExtractor g, int cx, int cy, int rx, int ry, int color) {
		if (rx <= 0 || ry <= 0) {
			return;
		}
		for (int dy = -ry; dy < ry; dy++) {
			double t = (dy + 0.5) / ry;
			int half = (int) Math.round(rx * Math.sqrt(Math.max(0, 1 - t * t)));
			g.fill(cx - half, cy + dy, cx + half, cy + dy + 1, color);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		// Header.
		String title = TITLES[Math.max(0, Math.min(TITLES.length - 1, data.design()))].toUpperCase(java.util.Locale.ROOT);
		g.text(font, title, left + FRAME + 10, top + FRAME + 7, GOLD, true);
		String info = "Blinds " + FinanceBook.amount(data.smallBlind()) + "/" + FinanceBook.amount(data.bigBlind())
				+ (data.hand() > 0 ? " · Hand " + data.hand() : "");
		int infoRight = left + panelWidth - FRAME - 10 - (data.isOwner() ? font.width("Settings") + 18 : 0);
		g.text(font, info, infoRight - font.width(info), top + FRAME + 7, TAN, false);
		if (settingsOpen) {
			drawSettings(g);
			return;
		}

		// Board and pot.
		int cx = centreX();
		int cy = centreY();
		int cw = 20;
		int ch = 28;
		int gap = 4;
		int bx = cx - (5 * cw + 4 * gap) / 2;
		for (int i = 0; i < 5; i++) {
			if (data.board()[i] >= 0) {
				card(g, bx + i * (cw + gap), cy - ch / 2 + 4, cw, ch, data.board()[i]);
			} else {
				g.fill(bx + i * (cw + gap), cy - ch / 2 + 4, bx + i * (cw + gap) + cw, cy + ch / 2 + 4, 0x22000000);
			}
		}
		String pot = data.pot() > 0 ? "Pot " + FinanceBook.amount(data.pot()) : phaseText();
		g.text(font, pot, cx - font.width(pot) / 2, cy - ch / 2 - 8, data.pot() > 0 ? GOLD : CREAM, true);

		// Seats.
		for (int i = 0; i < data.seats().size(); i++) {
			drawSeat(g, i);
		}

		// Bottom bar.
		int barX = left + FRAME + 6;
		int barY = top + panelHeight - FRAME - BAR + 8;
		int barW = panelWidth - 2 * FRAME - 12;
		if (data.mySeat() < 0) {
			g.text(font, "Buy in:", barX, barY + 17, DIM, false);
			String range = "Jugs (" + FinanceBook.amount(data.minBuyIn()) + " to " + FinanceBook.amount(data.maxBuyIn())
					+ "; you have " + FinanceBook.amount(data.cash()) + "). Pick a seat to sit.";
			g.text(font, font.plainSubstrByWidth(range, barW - 128), barX + 128, barY + 17, DIM, false);
			g.text(font, "Take a seat to play Texas hold'em.", barX, barY, CREAM, false);
		} else if (myTurn()) {
			String clock = "Your turn: " + Math.max(0, timer / 20) + "s";
			g.text(font, clock, left + panelWidth - FRAME - 10 - font.width(clock), barY + 21, timer < 200 ? LOSS : GAIN, false);
		} else {
			String status = statusLine();
			g.text(font, font.plainSubstrByWidth(status, barW - 80), barX, barY, CREAM, false);
			String last = data.log().isEmpty() ? "" : data.log().get(data.log().size() - 1);
			g.text(font, font.plainSubstrByWidth(last, barW - 80), barX, barY + 12, DIM, false);
		}
		if (!data.notice().isEmpty()) {
			String notice = font.plainSubstrByWidth(data.notice(), panelWidth - 40);
			g.text(font, notice, cx - font.width(notice) / 2, tableBottom() + 1, data.noticeOk() ? GAIN : LOSS, false);
		}
		// The log, top left of the table, while it is not your turn.
		int ly = tableTop() + 2;
		List<String> log = data.log();
		// Only the corner left of the top seats, so the log never runs under a seat.
		int room = seatXY(data.mySeat() >= 0 ? Math.floorMod(data.mySeat() + 4, PokerGame.SEATS) : 4)[0] - (left + FRAME + 8) - 6;
		for (int i = Math.max(0, log.size() - 3); i < log.size() && !myTurn() && room >= 40; i++) {
			String line = font.plainSubstrByWidth(log.get(i), room);
			g.text(font, line, left + FRAME + 8, ly, 0xFF8E8370, false);
			ly += 9;
		}
	}

	private String phaseText() {
		PokerGame.Phase phase = PokerGame.Phase.values()[Math.max(0, Math.min(5, data.phase()))];
		return switch (phase) {
			case WAITING -> "Waiting for players";
			case SHOWDOWN -> "Showdown";
			default -> "";
		};
	}

	private String statusLine() {
		if (data.phase() == PokerGame.Phase.WAITING.ordinal()) {
			int seated = 0;
			for (PokerTablePayload.SeatView seat : data.seats()) {
				if (seat.occupied()) {
					seated++;
				}
			}
			return seated < 2 ? "Waiting for another player to sit down." : "The next hand starts in a moment.";
		}
		if (data.actor() >= 0 && data.actor() < data.seats().size()) {
			return "Waiting for " + data.seats().get(data.actor()).name() + " (" + Math.max(0, timer / 20) + "s).";
		}
		return "";
	}

	private void drawSeat(GuiGraphicsExtractor g, int index) {
		PokerTablePayload.SeatView seat = data.seats().get(index);
		int[] xy = seatXY(index);
		int x = xy[0];
		int y = xy[1];
		if (!seat.occupied()) {
			if (data.mySeat() >= 0) {
				g.text(font, "Empty", x + (SEAT_W - font.width("Empty")) / 2, y + 9, 0xFF6B5A44, false);
			}
			return;
		}
		int nameColor = seat.me() ? GOLD : seat.folded() ? 0xFF7A6B55 : CREAM;
		g.text(font, font.plainSubstrByWidth(seat.name(), SEAT_W - 6), x + 4, y + 3, nameColor, false);
		String stack = seat.allIn() ? "All in" : FinanceBook.amount(seat.stack());
		chip(g, x + 4, y + 15, 0xFFC94A3A);
		g.text(font, stack, x + 12, y + 14, seat.allIn() ? LOSS : TAN, false);
		// Cards beside the plaque, toward the middle; bigger for your own.
		boolean mine = seat.me();
		int cw = mine ? 16 : 11;
		int chh = mine ? 22 : 15;
		int cardsX = x + SEAT_W - 2 * cw - 3;
		int cardsY = y + SEAT_H - chh - 2;
		if (seat.card1() != -1) {
			card(g, cardsX, cardsY - (mine ? 6 : 0), cw, chh, seat.card1());
			card(g, cardsX + cw + 1, cardsY - (mine ? 6 : 0), cw, chh, seat.card2());
		}
		if (seat.folded() && seat.inHand()) {
			g.text(font, "Folded", x + SEAT_W - font.width("Folded") - 3, y + 14, 0xFF7A6B55, false);
		}
		// The bet in front of the seat, toward the middle.
		int cy = centreY();
		boolean below = y > cy;
		if (seat.bet() > 0) {
			String bet = FinanceBook.amount(seat.bet());
			int bx = x + SEAT_W / 2 - font.width(bet) / 2;
			int by = below ? y - 12 : y + SEAT_H + 3;
			if (y < cy + 4 && y > cy - SEAT_H) {
				by = y + SEAT_H + 3;
			}
			chip(g, bx - 9, by + 1, 0xFF3A6EA5);
			g.text(font, bet, bx, by, CREAM, true);
		}
		if (index == data.button()) {
			int dx = below ? x - 9 : x + SEAT_W + 2;
			int dy = y + 2;
			g.fill(dx, dy, dx + 8, dy + 8, 0xFFEFE2C2);
			g.fill(dx + 1, dy + 1, dx + 7, dy + 7, 0xFFFFFFFF);
			g.text(font, "D", dx + 2, dy, 0xFF1A110A, false);
		}
		if (index == data.actor() && data.timerTicks() > 0) {
			int total = Math.max(1, data.timerTicks());
			int fill = (int) ((SEAT_W - 2) * (long) Math.max(0, timer) / total);
			g.fill(x + 1, y + SEAT_H - 2, x + SEAT_W - 1, y + SEAT_H - 1, 0xFF3D2814);
			g.fill(x + 1, y + SEAT_H - 2, x + 1 + fill, y + SEAT_H - 1, timer < 200 ? LOSS : GOLD);
		}
	}

	private static void chip(GuiGraphicsExtractor g, int x, int y, int color) {
		g.fill(x + 1, y, x + 6, y + 7, color);
		g.fill(x, y + 1, x + 7, y + 6, color);
		g.fill(x + 2, y + 2, x + 5, y + 5, 0xFFEFE2C2);
		g.fill(x + 3, y + 3, x + 4, y + 4, color);
	}

	/** One card: face up (rank and suit), face down (-2), or nothing (-1). */
	private void card(GuiGraphicsExtractor g, int x, int y, int w, int h, int card) {
		CardArt.draw(g::fill, x, y, w, h, card);
	}

	/** Opens or updates the overlay for a table's view. */
	static void receive(PokerTablePayload payload) {
		Minecraft client = Minecraft.getInstance();
		if (client.gui.screen() instanceof PokerScreen screen && screen.pos() == payload.pos()) {
			screen.update(payload);
		} else if (payload.open()) {
			client.gui.setScreen(new PokerScreen(payload));
		}
	}

	private int settingsX() {
		return left + panelWidth / 2 - 150;
	}

	private int settingsY() {
		return top + panelHeight / 2 - 64;
	}

	/** The owner's panel over the table. Blank boxes use the server's setting. */
	private void drawSettings(GuiGraphicsExtractor g) {
		int x = settingsX();
		int y = settingsY();
		g.text(font, "Table settings", x, y - 2, GOLD, false);
		long[] own = data.own();
		for (int i = 0; i < 5; i++) {
			g.text(font, SETTING_NAMES[i], x, y + 17 + i * 18, TAN, false);
			String hint = i == 4 ? "seconds" : "Jugs";
			if (settingText[i].trim().isEmpty()) {
				hint += " (server's)";
			}
			g.text(font, hint, x + 146, y + 17 + i * 18, 0xFF8E8370, false);
		}
		long lo = own != null && own.length > 5 ? own[5] : 0;
		long hi = own != null && own.length > 6 ? own[6] : 0;
		g.text(font, "Server allows buy-ins of " + FinanceBook.amount(lo) + " to " + FinanceBook.amount(hi) + ".", x, y + 106,
				0xFF8E8370, false);
		g.text(font, "New blinds start with the next hand.", x, y + 117, 0xFF8E8370, false);
	}
}
