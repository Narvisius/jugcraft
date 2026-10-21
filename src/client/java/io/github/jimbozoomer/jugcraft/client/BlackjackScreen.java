package io.github.jimbozoomer.jugcraft.client;

import static io.github.jimbozoomer.jugcraft.client.ExchangeScreen.*;

import io.github.jimbozoomer.jugcraft.finance.Blackjack;
import io.github.jimbozoomer.jugcraft.finance.CasinoPayload;
import io.github.jimbozoomer.jugcraft.finance.FinanceActionPayload.Action;
import io.github.jimbozoomer.jugcraft.finance.FinanceBook;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The blackjack table seen from above: the dealer's cards and shoe at the top, five seats round the arc below (the
 * first seat on the right, as at a casino). Bet into a seat with the picked chip; Deal when ready; then Hit, Stand,
 * Double and Split on your turn, and Insurance when the dealer shows an ace.
 */
public class BlackjackScreen extends CasinoScreen {
	private static final int SEAT_W = 80;

	private record HandView(long bet, boolean doubled, int outcome, boolean done, boolean splitAces, List<Integer> cards) {
	}

	private record SeatView(String name, boolean occupied, long bet, long lastBet, boolean ready, long insurance,
			boolean insuranceDecided, List<HandView> hands) {
	}

	private List<Integer> dealer = new ArrayList<>();
	private boolean holeShown;
	private int actorSeat;
	private int actorHand;
	private int mySeat;
	private int shoeLeft;
	private final List<SeatView> seats = new ArrayList<>();

	public BlackjackScreen(CasinoPayload data) {
		super(data);
		parse();
	}

	private void parse() {
		CasinoPayload.Reader r = new CasinoPayload.Reader(data.data());
		r.next();
		holeShown = r.nextBool();
		int count = r.nextInt();
		dealer = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			dealer.add(r.nextInt());
		}
		actorSeat = r.nextInt();
		actorHand = r.nextInt();
		mySeat = r.nextInt();
		shoeLeft = r.nextInt();
		seats.clear();
		for (int s = 0; s < Blackjack.SEATS; s++) {
			boolean occupied = r.nextBool();
			long bet = r.next();
			long lastBet = r.next();
			boolean ready = r.nextBool();
			long insurance = r.next();
			boolean decided = r.nextBool();
			int handCount = r.nextInt();
			List<HandView> hands = new ArrayList<>();
			for (int h = 0; h < handCount; h++) {
				long hb = r.next();
				boolean doubled = r.nextBool();
				int outcome = r.nextInt();
				boolean done = r.nextBool();
				boolean splitAces = r.nextBool();
				int cards = r.nextInt();
				List<Integer> list = new ArrayList<>();
				for (int c = 0; c < cards; c++) {
					list.add(r.nextInt());
				}
				hands.add(new HandView(hb, doubled, outcome, done, splitAces, list));
			}
			String name = s < data.text().size() ? data.text().get(s) : "";
			seats.add(new SeatView(name, occupied, bet, lastBet, ready, insurance, decided, hands));
		}
	}

	private Blackjack.Phase phase() {
		return Blackjack.Phase.values()[Math.max(0, Math.min(Blackjack.Phase.values().length - 1, data.phase()))];
	}

	private boolean myTurn() {
		return phase() == Blackjack.Phase.PLAYING && mySeat >= 0 && actorSeat == mySeat;
	}

	private String shape;

	@Override
	protected void update(CasinoPayload payload) {
		super.update(payload);
	}

	@Override
	protected boolean needsRebuild(CasinoPayload before, CasinoPayload after) {
		parse();
		String now = buttonShape();
		boolean differ = !now.equals(shape);
		return differ;
	}

	/** What decides the buttons: a change rebuilds them. */
	private String buttonShape() {
		StringBuilder out = new StringBuilder().append(phase()).append(mySeat).append(myTurn()).append(actorHand);
		for (SeatView seat : seats) {
			out.append(seat.occupied()).append(seat.bet() > 0).append(seat.ready()).append(seat.insuranceDecided());
		}
		if (myTurn()) {
			HandView hand = seats.get(mySeat).hands().get(actorHand);
			out.append(hand.cards().size());
		}
		return out.toString();
	}

	// ------------------------------------------------------------------ layout

	private int seatX(int seat) {
		int total = Blackjack.SEATS * SEAT_W + (Blackjack.SEATS - 1) * 4;
		int x0 = left + (panelWidth - total) / 2;
		return x0 + (Blackjack.SEATS - 1 - seat) * (SEAT_W + 4);
	}

	private int seatY(int seat) {
		int arc = Math.abs(seat - 2);
		return controlsY() - 78 - (2 - arc) * 6;
	}

	@Override
	protected int preferredWidth() {
		return 470;
	}

	@Override
	protected int preferredHeight() {
		return 300;
	}

	private int controlsY() {
		return tableBottom() - 16;
	}

	@Override
	protected void initGame() {
		shape = buttonShape();
		Blackjack.Phase phase = phase();
		boolean betting = phase == Blackjack.Phase.IDLE || phase == Blackjack.Phase.BETTING || phase == Blackjack.Phase.DONE;
		int bx = left + FRAME + 8;
		int by = controlsY();
		if (betting) {
			int x = addChipRack(bx, by);
			for (int s = 0; s < Blackjack.SEATS; s++) {
				SeatView seat = seats.get(s);
				int seatIndex = s;
				if (!seat.occupied() && mySeat < 0) {
					addRenderableWidget(new Plate(seatX(s) + 10, seatY(s) + 30, SEAT_W - 20, 14, "Sit & bet", Plate.Kind.BRASS,
							b -> send(Action.CASINO_BET, seatIndex, "", chip)));
				}
			}
			if (mySeat >= 0) {
				SeatView me = seats.get(mySeat);
				addRenderableWidget(new Plate(x + 4, by, 40, 14, "Bet", Plate.Kind.BRASS,
						b -> send(Action.CASINO_BET, mySeat, "", chip)));
				int rx = x + 48;
				if (me.bet() <= 0 && me.lastBet() > 0) {
					String rebet = "Rebet " + shortAmount(me.lastBet());
					addRenderableWidget(new Plate(rx, by, font.width(rebet) + 14, 14, rebet, Plate.Kind.WOOD,
							b -> send(Action.CASINO_BET, mySeat, "", me.lastBet())));
					rx += font.width(rebet) + 18;
				}
				if (me.bet() > 0 && phase != Blackjack.Phase.DONE) {
					addRenderableWidget(new Plate(rx, by, 40, 14, "Clear", Plate.Kind.WOOD,
							b -> send(Action.CASINO_ACT, Blackjack.Move.CLEAR.ordinal(), "", 0)));
					rx += 44;
					Plate deal = addRenderableWidget(new Plate(rx, by, 40, 14, "Deal", Plate.Kind.BRASS,
							b -> send(Action.CASINO_ACT, Blackjack.Move.DEAL.ordinal(), "", 0)));
					deal.active = !me.ready();
					rx += 44;
				}
				addRenderableWidget(new Plate(left + panelWidth - FRAME - 50, by, 42, 14, "Leave", Plate.Kind.WOOD,
						b -> send(Action.CASINO_ACT, Blackjack.Move.LEAVE.ordinal(), "", 0)));
			}
			return;
		}
		if (phase == Blackjack.Phase.INSURANCE && mySeat >= 0 && !seats.get(mySeat).hands().isEmpty()
				&& !seats.get(mySeat).insuranceDecided()) {
			long cost = seats.get(mySeat).hands().get(0).bet() / 2;
			addRenderableWidget(new Plate(bx, by, 110, 14, "Insure for " + FinanceBook.amount(cost), Plate.Kind.BRASS,
					b -> send(Action.CASINO_ACT, Blackjack.Move.INSURE.ordinal(), "", 0))).active = cost > 0;
			addRenderableWidget(new Plate(bx + 114, by, 80, 14, "No insurance", Plate.Kind.WOOD,
					b -> send(Action.CASINO_ACT, Blackjack.Move.NO_INSURANCE.ordinal(), "", 0)));
			return;
		}
		if (myTurn()) {
			SeatView me = seats.get(mySeat);
			HandView hand = me.hands().get(actorHand);
			int w = 54;
			addRenderableWidget(new Plate(bx, by, w, 14, "Hit", Plate.Kind.BRASS,
					b -> send(Action.CASINO_ACT, Blackjack.Move.HIT.ordinal(), "", 0)));
			addRenderableWidget(new Plate(bx + w + 4, by, w, 14, "Stand", Plate.Kind.BRASS,
					b -> send(Action.CASINO_ACT, Blackjack.Move.STAND.ordinal(), "", 0)));
			boolean two = hand.cards().size() == 2 && !hand.splitAces();
			addRenderableWidget(new Plate(bx + 2 * (w + 4), by, w, 14, "Double", Plate.Kind.WOOD,
					b -> send(Action.CASINO_ACT, Blackjack.Move.DOUBLE.ordinal(), "", 0))).active = two;
			boolean pair = two && Blackjack.value(hand.cards().get(0)) == Blackjack.value(hand.cards().get(1))
					&& me.hands().size() < Blackjack.MAX_HANDS;
			addRenderableWidget(new Plate(bx + 3 * (w + 4), by, w, 14, "Split", Plate.Kind.WOOD,
					b -> send(Action.CASINO_ACT, Blackjack.Move.SPLIT.ordinal(), "", 0))).active = pair;
		}
	}

	// ------------------------------------------------------------------ drawing

	@Override
	protected void drawTableBack(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		int cx = left + panelWidth / 2;
		int ty = tableTop();
		int rx = panelWidth / 2 - 14;
		int ry = tableBottom() - ty - 6;
		// The half moon: rail and felt below the dealer's straight edge.
		halfMoon(g, cx, ty, rx + 7, ry + 7, RAIL);
		halfMoon(g, cx, ty, rx, ry, GOLD_DIM);
		halfMoon(g, cx, ty, rx - 1, ry - 1, FELT);
		g.fill(cx - rx - 7, ty - 3, cx + rx + 7, ty, WALNUT_DARK);
		// The printed arcs.
		arc(g, cx, ty, rx - 40, ry - 46, GOLD_DIM);
		arc(g, cx, ty, rx - 58, ry - 62, 0xFF6B5A2C);
	}

	private static void halfMoon(GuiGraphicsExtractor g, int cx, int top, int rx, int ry, int color) {
		for (int dy = 0; dy < ry; dy++) {
			double t = (dy + 0.5) / ry;
			int half = (int) Math.round(rx * Math.sqrt(Math.max(0, 1 - t * t)));
			g.fill(cx - half, top + dy, cx + half, top + dy + 1, color);
		}
	}

	private static void arc(GuiGraphicsExtractor g, int cx, int top, int rx, int ry, int color) {
		for (int i = 0; i <= 120; i++) {
			double a = Math.PI * i / 120;
			int x = cx + (int) Math.round(Math.cos(a) * rx);
			int y = top + (int) Math.round(Math.sin(a) * ry);
			g.fill(x, y, x + 1, y + 1, color);
		}
	}

	@Override
	protected void drawTable(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		int cx = left + panelWidth / 2;
		int ty = tableTop();
		// Dealer.
		int cw = 18;
		int ch = 25;
		int dx = cx - (dealer.size() * (cw + 3)) / 2;
		for (int i = 0; i < dealer.size(); i++) {
			card(g, dx + i * (cw + 3), ty + 4, cw, ch, dealer.get(i));
		}
		String dealerText = dealer.isEmpty() ? "BLACKJACK PAYS 3 TO 2" : holeShown ? "Dealer " + Blackjack.total(dealer)
				: "Dealer shows " + Blackjack.value(dealer.get(0));
		g.text(font, dealerText, cx - font.width(dealerText) / 2, ty + 32, dealer.isEmpty() ? GOLD : CREAM, true);
		String rule = "Dealer stands on all 17s · Insurance pays 2 to 1";
		g.text(font, rule, cx - font.width(rule) / 2, ty + 44, 0xFF9DB89F, false);
		// The shoe.
		int sx = cx + panelWidth / 2 - 70;
		g.fill(sx, ty + 6, sx + 22, ty + 22, 0xFF1A110A);
		g.fill(sx + 1, ty + 7, sx + 21, ty + 21, GOLD_DIM);
		g.fill(sx + 3, ty + 9, sx + 19, ty + 19, CARD_BACK);
		String shoe = shoeLeft + " left";
		g.text(font, shoe, sx + 11 - font.width(shoe) / 2, ty + 24, DIM, false);
		// Status.
		Blackjack.Phase phase = phase();
		String status = switch (phase) {
			case IDLE -> mySeat >= 0 ? "Place your bet." : "Pick a chip, then a seat.";
			case BETTING -> "Bets close in " + seconds() + "s";
			case INSURANCE -> "Insurance? " + seconds() + "s";
			case PLAYING -> myTurn() ? "Your turn: " + seconds() + "s" : actorSeat >= 0 ? seats.get(actorSeat).name() + " to play ("
					+ seconds() + "s)" : "";
			case DEALER -> "The dealer draws.";
			case DONE -> "Bet again for the next hand.";
		};
		// The status sits on the felt under the dealer's rules, clear of the buttons.
		g.text(font, status, cx - font.width(status) / 2, ty + 56, myTurn() ? GAIN : GOLD, true);
		// Seats.
		for (int s = 0; s < Blackjack.SEATS; s++) {
			drawSeat(g, s);
		}
	}

	private void drawSeat(GuiGraphicsExtractor g, int s) {
		SeatView seat = seats.get(s);
		int x = seatX(s);
		int y = seatY(s);
		boolean acting = s == actorSeat && phase() == Blackjack.Phase.PLAYING;
		// The betting circle.
		int ccx = x + SEAT_W / 2;
		oval(g, ccx, y + 54, 10, 6, acting ? GOLD : 0xFFCFC3A0);
		oval(g, ccx, y + 54, 9, 5, FELT);
		if (!seat.occupied()) {
			return;
		}
		String name = seat.name() + (s == mySeat ? " (you)" : "");
		g.text(font, font.plainSubstrByWidth(name, SEAT_W), ccx - Math.min(SEAT_W, font.width(name)) / 2, y + 62,
				s == mySeat ? GOLD : CREAM, true);
		if (seat.hands().isEmpty() && seat.bet() > 0) {
			chip(g, ccx - 5, y + 50, seat.bet(), false);
			String bet = FinanceBook.amount(seat.bet()) + (seat.ready() ? " ✔" : "");
			g.text(font, bet, ccx - font.width(bet) / 2, y + 40, CREAM, true);
		}
		// Hands, side by side, the playing one lit.
		int hands = seat.hands().size();
		int cw = 14;
		int chh = 20;
		int handW = Math.max(26, SEAT_W / Math.max(1, hands));
		for (int h = 0; h < hands; h++) {
			HandView hand = seat.hands().get(h);
			int hx = x + h * handW + 2;
			int hy = y + 4;
			boolean lit = acting && h == actorHand;
			int step = Math.min(9, Math.max(4, (handW - cw - 2) / Math.max(1, hand.cards().size() - 1)));
			for (int c = 0; c < hand.cards().size(); c++) {
				card(g, hx + c * step, hy + (c % 2) * 2, cw, chh, hand.cards().get(c));
			}
			if (lit) {
				g.fill(hx - 1, hy + chh + 3, hx + handW - 4, hy + chh + 4, GOLD);
			}
			int total = Blackjack.total(hand.cards());
			String label = hand.outcome() == 2 ? "BJ!" : total > 21 ? "Bust" : String.valueOf(total);
			int colour = switch (hand.outcome()) {
				case 2, 1 -> GAIN;
				case 0 -> TAN;
				case -1 -> LOSS;
				default -> CREAM;
			};
			g.text(font, label, hx, hy + chh + 6, colour, true);
			String bet = shortAmount(hand.bet()) + (hand.doubled() ? "x2" : "");
			chip(g, hx, hy + chh + 16, hand.bet(), false);
			g.text(font, bet, hx + 10, hy + chh + 17, DIM, false);
			if (hand.outcome() != 9) {
				String word = switch (hand.outcome()) {
					case 2 -> "Blackjack";
					case 1 -> "Win";
					case 0 -> "Push";
					default -> "Lose";
				};
				g.text(font, word, hx + 18, hy + chh + 6, colour, false);
			}
		}
		if (seat.insurance() > 0) {
			g.text(font, "Ins " + shortAmount(seat.insurance()), x + 2, y - 8, TAN, false);
		}
	}

	private int lastCards = -1;

	@Override
	public void tick() {
		super.tick();
		int cards = dealer.size();
		for (SeatView seat : seats) {
			for (HandView hand : seat.hands()) {
				cards += hand.cards().size();
			}
		}
		if (cards > lastCards && lastCards >= 0) {
			CasinoSound.card(data.pos());
		}
		lastCards = cards;
	}

	@Override
	protected CasinoTheme theme() {
		return CasinoTheme.TABLE;
	}
}
