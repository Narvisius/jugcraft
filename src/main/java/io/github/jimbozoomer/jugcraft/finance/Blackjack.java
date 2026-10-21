package io.github.jimbozoomer.jugcraft.finance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Blackjack against the house, casino rules: six decks, reshuffled once three quarters are dealt; the dealer stands on
 * all 17s; blackjack pays 3 to 2; double on any two cards (also after a split); split pairs of the same value up to
 * four hands, split aces get one card each; insurance against a dealer's ace pays 2 to 1; no surrender. Payouts are
 * rounded down to whole Jugs. Plain Java (cards as in {@link PokerHands}: rank = card % 13, 12 is an ace), driven by
 * {@link #tick()} twenty times a second.
 *
 * <p>Players bet into a seat while bets are open; the deal comes when the betting clock runs out or everyone betting
 * presses Deal. Each hand is played in turn with a clock (standing when it runs out); then the dealer draws.
 */
public class Blackjack {
	public static final int SEATS = 5;
	public static final int DECKS = 6;
	public static final int MAX_HANDS = 4;
	private static final int DEALER_STEP = 16;
	private static final int RESULT_TICKS = 100;
	private static final int INSURANCE_TICKS = 200;

	public enum Phase { IDLE, BETTING, INSURANCE, PLAYING, DEALER, DONE }

	public enum Move { HIT, STAND, DOUBLE, SPLIT, INSURE, NO_INSURANCE, DEAL, CLEAR, LEAVE }

	public static final class Hand {
		public final List<Integer> cards = new ArrayList<>();
		public long bet;
		public boolean doubled;
		public boolean splitAces;
		public boolean fromSplit;
		public boolean done;
		public boolean settled;
		/** -1 lost, 0 push, 1 won, 2 blackjack, unset 9. */
		public int outcome = 9;
	}

	public static final class Seat {
		public UUID player;
		public String name = "";
		public long bet;
		public long lastBet;
		public boolean ready;
		public long insurance;
		public boolean insuranceDecided;
		public final List<Hand> hands = new ArrayList<>();
		public int away;

		public boolean occupied() {
			return player != null;
		}

		void clearRound() {
			bet = 0;
			ready = false;
			insurance = 0;
			insuranceDecided = false;
			hands.clear();
		}
	}

	public final Seat[] seats = new Seat[SEATS];
	public final List<Integer> dealer = new ArrayList<>();
	public boolean holeShown;
	public Phase phase = Phase.IDLE;
	public int timer;
	public int actorSeat = -1;
	public int actorHand = -1;
	public int betTicks = 20 * 20;
	public int turnTicks = 20 * 20;
	public final List<String> log = new ArrayList<>();
	public long round;

	private final CasinoHouse house;
	private final Random random;
	private int[] shoe = new int[0];
	private int next;

	public Blackjack(CasinoHouse house, Random random) {
		this.house = house;
		this.random = random;
		for (int i = 0; i < SEATS; i++) {
			seats[i] = new Seat();
		}
		shuffle();
	}

	// ------------------------------------------------------------------ cards

	private void shuffle() {
		shoe = new int[52 * DECKS];
		for (int i = 0; i < shoe.length; i++) {
			shoe[i] = i % 52;
		}
		for (int i = shoe.length - 1; i > 0; i--) {
			int j = random.nextInt(i + 1);
			int t = shoe[i];
			shoe[i] = shoe[j];
			shoe[j] = t;
		}
		next = 0;
		note("The dealer shuffles a fresh shoe.");
	}

	/** Cards left before the cut card. */
	public int shoeLeft() {
		return Math.max(0, shoe.length * 3 / 4 - next);
	}

	private int draw() {
		if (next >= shoe.length) {
			shuffle();
		}
		return shoe[next++];
	}

	public static int value(int card) {
		int rank = card % 13;
		return rank == 12 ? 11 : Math.min(10, rank + 2);
	}

	/** The best total of some cards: aces count 11 unless that busts. */
	public static int total(List<Integer> cards) {
		int sum = 0;
		int aces = 0;
		for (int card : cards) {
			sum += value(card);
			if (card % 13 == 12) {
				aces++;
			}
		}
		while (sum > 21 && aces > 0) {
			sum -= 10;
			aces--;
		}
		return sum;
	}

	/** True when an ace still counts 11. */
	public static boolean soft(List<Integer> cards) {
		int sum = 0;
		boolean ace = false;
		for (int card : cards) {
			sum += value(card) == 11 ? 1 : value(card);
			ace |= card % 13 == 12;
		}
		return ace && sum + 10 <= 21;
	}

	public static boolean natural(List<Integer> cards) {
		return cards.size() == 2 && total(cards) == 21;
	}

	// ------------------------------------------------------------------ seats and bets

	public int seatOf(UUID player) {
		for (int i = 0; i < SEATS; i++) {
			if (player.equals(seats[i].player)) {
				return i;
			}
		}
		return -1;
	}

	private boolean betting() {
		return phase == Phase.IDLE || phase == Phase.BETTING || phase == Phase.DONE;
	}

	/** Most the house could owe on everything now on the table, plus {@code extra}. */
	long exposure() {
		long total = 0;
		for (Seat seat : seats) {
			if (seat.hands.isEmpty()) {
				total += seat.bet + (seat.bet + 1) / 2;
			}
			for (Hand hand : seat.hands) {
				if (!hand.settled) {
					total += natural(hand.cards) && !hand.fromSplit ? hand.bet + (hand.bet + 1) / 2 : hand.bet;
				}
			}
			total += seat.insurance * 2;
		}
		return total;
	}

	/** Puts {@code amount} more on a seat for the next deal. */
	public FinanceBook.Result bet(UUID player, String name, int seatIndex, long amount) {
		if (!betting()) {
			return FinanceBook.Result.fail("Wait for the next deal to bet.");
		}
		if (phase == Phase.DONE) {
			startBetting();
		}
		int mine = seatOf(player);
		if (seatIndex < 0 || seatIndex >= SEATS) {
			return FinanceBook.Result.fail("Pick a seat.");
		}
		Seat seat = seats[seatIndex];
		if (seat.occupied() && !player.equals(seat.player)) {
			return FinanceBook.Result.fail("That seat is taken.");
		}
		if (!seat.occupied() && mine >= 0) {
			return FinanceBook.Result.fail("You already have a seat.");
		}
		long total = seat.bet + amount;
		if (amount <= 0 || total < house.minBet()) {
			return FinanceBook.Result.fail("The least bet here is " + FinanceBook.amount(house.minBet()) + ".");
		}
		if (total > house.maxBet()) {
			return FinanceBook.Result.fail("The most you may bet here is " + FinanceBook.amount(house.maxBet()) + ".");
		}
		if (exposure() + amount + (amount + 1) / 2 > house.cover(player)) {
			return FinanceBook.Result.fail("The house cannot cover that bet.");
		}
		String failed = house.stake(player, amount);
		if (failed != null) {
			return FinanceBook.Result.fail(failed);
		}
		seat.player = player;
		seat.name = name;
		seat.bet = total;
		seat.lastBet = total;
		seat.away = 0;
		if (phase == Phase.IDLE) {
			startBetting();
		}
		return new FinanceBook.Result(true, "");
	}

	private void startBetting() {
		phase = Phase.BETTING;
		timer = betTicks;
		dealer.clear();
		holeShown = false;
		actorSeat = -1;
		actorHand = -1;
		for (Seat seat : seats) {
			if (!seat.hands.isEmpty()) {
				seat.clearRound();
			}
		}
	}

	/** Takes a seat's bet back before the deal. */
	public FinanceBook.Result clear(UUID player) {
		int index = seatOf(player);
		if (index < 0 || !(phase == Phase.BETTING || phase == Phase.IDLE)) {
			return FinanceBook.Result.fail("You have no bet to take back.");
		}
		Seat seat = seats[index];
		if (seat.bet > 0) {
			house.refund(player, seat.bet);
			seat.bet = 0;
		}
		return new FinanceBook.Result(true, "");
	}

	/** Leaves the table between hands (a bet not yet dealt comes back). */
	public FinanceBook.Result leave(UUID player) {
		int index = seatOf(player);
		if (index < 0) {
			return FinanceBook.Result.fail("You are not at this table.");
		}
		Seat seat = seats[index];
		if (!seat.hands.isEmpty() && (phase == Phase.INSURANCE || phase == Phase.PLAYING || phase == Phase.DEALER)) {
			return FinanceBook.Result.fail("Finish this hand first.");
		}
		if (seat.bet > 0 && seat.hands.isEmpty()) {
			house.refund(player, seat.bet);
		}
		seats[index] = new Seat();
		return new FinanceBook.Result(true, "");
	}

	// ------------------------------------------------------------------ moves

	public FinanceBook.Result act(UUID player, Move move) {
		int index = seatOf(player);
		if (index < 0) {
			return FinanceBook.Result.fail("You are not at this table.");
		}
		Seat seat = seats[index];
		switch (move) {
			case CLEAR -> {
				return clear(player);
			}
			case LEAVE -> {
				return leave(player);
			}
			case DEAL -> {
				if (phase != Phase.BETTING || seat.bet <= 0) {
					return FinanceBook.Result.fail("Bet first.");
				}
				seat.ready = true;
				boolean all = true;
				for (Seat s : seats) {
					all &= s.bet <= 0 || s.ready;
				}
				if (all) {
					deal();
				}
				return new FinanceBook.Result(true, "");
			}
			case INSURE, NO_INSURANCE -> {
				return insurance(player, seat, move == Move.INSURE);
			}
			default -> {
			}
		}
		if (phase != Phase.PLAYING || actorSeat != index) {
			return FinanceBook.Result.fail("It is not your turn.");
		}
		Hand hand = seat.hands.get(actorHand);
		switch (move) {
			case HIT -> {
				hand.cards.add(draw());
				int total = total(hand.cards);
				if (total > 21) {
					bust(seat, hand);
				} else if (total == 21) {
					hand.done = true;
				}
			}
			case STAND -> hand.done = true;
			case DOUBLE -> {
				if (hand.cards.size() != 2 || hand.splitAces) {
					return FinanceBook.Result.fail("You may double only on your first two cards.");
				}
				FinanceBook.Result more = extra(player, hand.bet);
				if (!more.ok()) {
					return more;
				}
				hand.bet *= 2;
				hand.doubled = true;
				hand.cards.add(draw());
				hand.done = true;
				if (total(hand.cards) > 21) {
					bust(seat, hand);
				}
				note(seat.name + " doubles to " + FinanceBook.amount(hand.bet) + ".");
			}
			case SPLIT -> {
				if (!canSplit(seat, hand)) {
					return FinanceBook.Result.fail("Only a pair of the same value splits, up to four hands.");
				}
				FinanceBook.Result more = extra(player, hand.bet);
				if (!more.ok()) {
					return more;
				}
				boolean aces = hand.cards.get(0) % 13 == 12;
				Hand second = new Hand();
				second.bet = hand.bet;
				second.fromSplit = true;
				second.cards.add(hand.cards.remove(1));
				hand.fromSplit = true;
				hand.cards.add(draw());
				second.cards.add(draw());
				seat.hands.add(actorHand + 1, second);
				if (aces) {
					hand.splitAces = true;
					second.splitAces = true;
					hand.done = true;
					second.done = true;
				}
				note(seat.name + " splits.");
			}
			default -> {
				return FinanceBook.Result.fail("You cannot do that now.");
			}
		}
		if (total(hand.cards) >= 21 && !hand.done) {
			hand.done = true;
		}
		advance();
		return new FinanceBook.Result(true, "");
	}

	public static boolean canSplit(Seat seat, Hand hand) {
		return hand.cards.size() == 2 && value(hand.cards.get(0)) == value(hand.cards.get(1)) && seat.hands.size() < MAX_HANDS
				&& !hand.splitAces;
	}

	/** Takes another stake for a double or a split, if the house can cover it. */
	private FinanceBook.Result extra(UUID player, long amount) {
		if (exposure() + amount > house.cover(player)) {
			return FinanceBook.Result.fail("The house cannot cover that.");
		}
		String failed = house.stake(player, amount);
		return failed == null ? new FinanceBook.Result(true, "") : FinanceBook.Result.fail(failed);
	}

	private FinanceBook.Result insurance(UUID player, Seat seat, boolean take) {
		if (phase != Phase.INSURANCE || seat.hands.isEmpty() || seat.insuranceDecided) {
			return FinanceBook.Result.fail("Insurance is not offered now.");
		}
		if (take) {
			long cost = seat.hands.get(0).bet / 2;
			if (cost <= 0) {
				return FinanceBook.Result.fail("Your bet is too small to insure.");
			}
			if (exposure() + cost * 2 > house.cover(player)) {
				return FinanceBook.Result.fail("The house cannot cover insurance now.");
			}
			String failed = house.stake(player, cost);
			if (failed != null) {
				return FinanceBook.Result.fail(failed);
			}
			seat.insurance = cost;
		}
		seat.insuranceDecided = true;
		boolean all = true;
		for (Seat s : seats) {
			all &= s.hands.isEmpty() || s.insuranceDecided;
		}
		if (all) {
			peek();
		}
		return new FinanceBook.Result(true, "");
	}

	private void bust(Seat seat, Hand hand) {
		hand.done = true;
		hand.outcome = -1;
		hand.settled = true;
		house.settle(seat.player, seat.name, hand.bet, 0, null);
		note(seat.name + " busts with " + total(hand.cards) + ".");
	}

	// ------------------------------------------------------------------ the round

	private void deal() {
		round++;
		if (shoeLeft() <= 0) {
			shuffle();
		}
		dealer.clear();
		holeShown = false;
		for (Seat seat : seats) {
			seat.hands.clear();
			if (seat.bet > 0) {
				Hand hand = new Hand();
				hand.bet = seat.bet;
				seat.hands.add(hand);
			}
		}
		for (int pass = 0; pass < 2; pass++) {
			for (Seat seat : seats) {
				if (!seat.hands.isEmpty()) {
					seat.hands.get(0).cards.add(draw());
				}
			}
			dealer.add(draw());
		}
		if (dealer.get(0) % 13 == 12) {
			phase = Phase.INSURANCE;
			timer = INSURANCE_TICKS;
			note("The dealer shows an ace: insurance?");
		} else {
			peek();
		}
	}

	/** The dealer checks for blackjack (with an ace or a ten up), then play begins. */
	private void peek() {
		boolean dealerNatural = natural(dealer) && value(dealer.get(0)) >= 10;
		for (Seat seat : seats) {
			if (seat.insurance > 0) {
				house.settle(seat.player, seat.name, seat.insurance, dealerNatural ? seat.insurance * 3 : 0, null);
				seat.insurance = 0;
			}
		}
		if (dealerNatural) {
			holeShown = true;
			note("The dealer has blackjack.");
			for (Seat seat : seats) {
				for (Hand hand : seat.hands) {
					settle(seat, hand, natural(hand.cards) ? 0 : -1);
				}
			}
			finish();
			return;
		}
		// Players' blackjacks are paid at once.
		for (Seat seat : seats) {
			for (Hand hand : seat.hands) {
				if (natural(hand.cards)) {
					hand.done = true;
					settle(seat, hand, 2);
				}
			}
		}
		phase = Phase.PLAYING;
		actorSeat = -1;
		actorHand = -1;
		advance();
	}

	/** Moves the turn to the next hand still to play, or to the dealer. */
	private void advance() {
		if (phase != Phase.PLAYING) {
			return;
		}
		int startSeat = Math.max(0, actorSeat);
		for (int s = startSeat; s < SEATS; s++) {
			Seat seat = seats[s];
			for (int h = 0; h < seat.hands.size(); h++) {
				if (!seat.hands.get(h).done) {
					timer = turnTicks;
					actorSeat = s;
					actorHand = h;
					return;
				}
			}
		}
		actorSeat = -1;
		actorHand = -1;
		phase = Phase.DEALER;
		holeShown = true;
		timer = DEALER_STEP;
	}

	private void settle(Seat seat, Hand hand, int outcome) {
		if (hand.settled) {
			return;
		}
		hand.settled = true;
		hand.outcome = outcome;
		long payout = switch (outcome) {
			case 2 -> hand.bet + hand.bet * 3 / 2;
			case 1 -> hand.bet * 2;
			case 0 -> hand.bet;
			default -> 0;
		};
		String big = outcome == 2 && hand.bet >= 100 ? seat.name + " hit blackjack for " + FinanceBook.amount(payout - hand.bet)
				+ " Jugs." : null;
		house.settle(seat.player, seat.name, hand.bet, payout, big);
		String what = switch (outcome) {
			case 2 -> " has blackjack: wins " + FinanceBook.amount(payout - hand.bet);
			case 1 -> " wins " + FinanceBook.amount(payout - hand.bet);
			case 0 -> " pushes";
			default -> " loses " + FinanceBook.amount(hand.bet);
		};
		note(seat.name + what + ".");
	}

	private boolean anyLive() {
		for (Seat seat : seats) {
			for (Hand hand : seat.hands) {
				if (!hand.settled) {
					return true;
				}
			}
		}
		return false;
	}

	private void finish() {
		phase = Phase.DONE;
		timer = RESULT_TICKS;
		actorSeat = -1;
		actorHand = -1;
		for (Seat seat : seats) {
			seat.bet = 0;
			seat.ready = false;
		}
	}

	public void tick() {
		switch (phase) {
			case IDLE -> {
			}
			case BETTING -> {
				boolean any = false;
				for (Seat seat : seats) {
					any |= seat.bet > 0;
				}
				if (!any) {
					phase = Phase.IDLE;
				} else if (--timer <= 0) {
					deal();
				}
			}
			case INSURANCE -> {
				if (--timer <= 0) {
					for (Seat seat : seats) {
						seat.insuranceDecided = true;
					}
					peek();
				}
			}
			case PLAYING -> {
				if (--timer <= 0 && actorSeat >= 0) {
					Seat seat = seats[actorSeat];
					seat.hands.get(actorHand).done = true;
					note(seat.name + " ran out of time and stands.");
					advance();
				}
			}
			case DEALER -> {
				if (--timer > 0) {
					return;
				}
				timer = DEALER_STEP;
				if (!anyLive()) {
					finish();
					return;
				}
				int total = total(dealer);
				if (total < 17) {
					dealer.add(draw());
					return;
				}
				note("The dealer has " + (total > 21 ? "bust (" + total + ")" : String.valueOf(total)) + ".");
				for (Seat seat : seats) {
					for (Hand hand : seat.hands) {
						if (hand.settled) {
							continue;
						}
						int mine = total(hand.cards);
						settle(seat, hand, total > 21 || mine > total ? 1 : mine == total ? 0 : -1);
					}
				}
				finish();
			}
			case DONE -> {
				if (--timer <= 0) {
					phase = Phase.IDLE;
					for (Seat seat : seats) {
						seat.clearRound();
					}
					dealer.clear();
					holeShown = false;
				}
			}
		}
	}

	/** Players who leave or log off: their seat goes once they have nothing on the table. */
	public void away(UUID player) {
		int index = seatOf(player);
		if (index < 0) {
			return;
		}
		Seat seat = seats[index];
		boolean inHand = !seat.hands.isEmpty() && (phase == Phase.INSURANCE || phase == Phase.PLAYING || phase == Phase.DEALER);
		if (!inHand) {
			leave(player);
		}
	}

	/** Calls everything off: every stake on the table goes back. */
	public void cancelAll() {
		for (int i = 0; i < SEATS; i++) {
			Seat seat = seats[i];
			if (!seat.occupied()) {
				continue;
			}
			if (seat.hands.isEmpty() && seat.bet > 0) {
				house.refund(seat.player, seat.bet);
			}
			for (Hand hand : seat.hands) {
				if (!hand.settled) {
					house.refund(seat.player, hand.bet);
					hand.settled = true;
				}
			}
			if (seat.insurance > 0) {
				house.refund(seat.player, seat.insurance);
			}
			seats[i] = new Seat();
		}
		phase = Phase.IDLE;
		dealer.clear();
	}

	/** Every stake still on the table, by player (saved so a world closing mid-hand gives them back). */
	public Map<UUID, Long> outstanding() {
		Map<UUID, Long> out = new HashMap<>();
		for (Seat seat : seats) {
			if (!seat.occupied()) {
				continue;
			}
			long sum = seat.insurance;
			if (seat.hands.isEmpty()) {
				sum += seat.bet;
			}
			for (Hand hand : seat.hands) {
				if (!hand.settled) {
					sum += hand.bet;
				}
			}
			if (sum > 0) {
				out.merge(seat.player, sum, Long::sum);
			}
		}
		return out;
	}

	private void note(String line) {
		log.add(line);
		while (log.size() > 8) {
			log.remove(0);
		}
	}
}
