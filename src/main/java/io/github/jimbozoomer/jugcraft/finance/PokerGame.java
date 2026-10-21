package io.github.jimbozoomer.jugcraft.finance;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * One poker table's game: no-limit Texas hold'em for up to six players, dealt automatically. Plain Java, so the rules
 * are tested without a game; {@link PokerTableBlockEntity} drives it once a tick and moves the money.
 *
 * <p>Chips are Jugs. A player buys in when they sit (the table holds the Jugs in the Exchange's escrow) and cashes out
 * when they stand; the game itself only moves chips between seats, so no money is made or lost. {@link Bank} is how
 * it pays players who leave.
 *
 * <p>A hand starts by itself a few seconds after two or more players with chips are seated. The button moves round;
 * the two seats after it post the blinds (heads-up, the button posts the small blind and acts first before the flop).
 * Each player has a time limit to act; when it runs out they check if they may, else they fold. Players act with fold,
 * check, call, raise (to an amount) and all-in. Side pots are worked out from what each player put in.
 */
public final class PokerGame {
	public static final int SEATS = 6;
	/** Ticks between hands, and the time the cards stay face up after a showdown. */
	public static final int START_DELAY = 60;
	public static final int SHOWDOWN_TICKS = 120;
	public static final int LOG_LINES = 6;

	public enum Phase {
		WAITING, PREFLOP, FLOP, TURN, RIVER, SHOWDOWN
	}

	public enum Move {
		FOLD, CHECK, CALL, RAISE, ALL_IN
	}

	/** Pays a player who leaves the table. */
	public interface Bank {
		void cashOut(UUID player, String name, long chips);
	}

	/** A seat. {@code bet} is what they put in this betting round; the hand's total is in {@link #contributed}. */
	public static final class Seat {
		public UUID player;
		public String name = "";
		public long stack;
		public long bet;
		public int card1 = -1;
		public int card2 = -1;
		public boolean inHand;
		public boolean folded;
		public boolean allIn;
		public boolean acted;
		public boolean shown;
		/** Ticks the player has been away (offline or out of reach) while seated. */
		public int away;

		public boolean occupied() {
			return player != null;
		}

		/** Still in the hand and able to act. */
		boolean canAct() {
			return inHand && !folded && !allIn;
		}

		boolean live() {
			return inHand && !folded;
		}

		void clearHand() {
			bet = 0;
			card1 = -1;
			card2 = -1;
			inHand = false;
			folded = false;
			allIn = false;
			acted = false;
			shown = false;
		}
	}

	public record Result(boolean ok, String message) {
		static Result fail(String message) {
			return new Result(false, message);
		}
	}

	public final Seat[] seats = new Seat[SEATS];
	/** What each seat put into the pot this hand, kept apart from the seat so it stays if the player leaves. */
	public final long[] contributed = new long[SEATS];
	/** Who put {@link #contributed} in, by seat (it stays when they leave mid-hand). */
	public final UUID[] contributor = new UUID[SEATS];
	public final String[] contributorName = new String[SEATS];
	public final int[] board = {-1, -1, -1, -1, -1};
	public Phase phase = Phase.WAITING;
	public int button = -1;
	public int actor = -1;
	public long currentBet;
	public long minRaise;
	public long smallBlind = 1;
	public long bigBlind = 2;
	public int turnTicks = 600;
	/** Ticks left for the actor, or before the next hand / the end of a showdown. */
	public int timer;
	public long handNumber;
	public final List<String> log = new ArrayList<>();
	private final int[] deck = new int[52];
	private int deckPos;
	private final Random random;
	private final Bank bank;

	public PokerGame(Bank bank, Random random) {
		this.bank = bank;
		this.random = random;
		for (int i = 0; i < SEATS; i++) {
			seats[i] = new Seat();
		}
	}

	// ------------------------------------------------------------------ reading

	public int boardCount() {
		return switch (phase) {
			case FLOP -> 3;
			case TURN -> 4;
			case RIVER -> 5;
			case SHOWDOWN -> countBoard();
			default -> 0;
		};
	}

	private int countBoard() {
		int n = 0;
		for (int card : board) {
			if (card >= 0) {
				n++;
			}
		}
		return n;
	}

	public long pot() {
		long sum = 0;
		for (long c : contributed) {
			sum += c;
		}
		return sum;
	}

	public int seatOf(UUID player) {
		for (int i = 0; i < SEATS; i++) {
			if (player.equals(seats[i].player)) {
				return i;
			}
		}
		return -1;
	}

	public boolean handRunning() {
		return phase != Phase.WAITING && phase != Phase.SHOWDOWN;
	}

	/** Every chip on the table: stacks and the pot. */
	public long chipsOnTable() {
		long sum = pot();
		for (Seat seat : seats) {
			sum += seat.stack;
		}
		return sum;
	}

	/** What the actor must add to call. */
	public long toCall(int seat) {
		return Math.max(0, Math.min(currentBet - seats[seat].bet, seats[seat].stack));
	}

	/** The least a raise may go to (the most is all the actor's chips). */
	public long minRaiseTo() {
		return currentBet + Math.max(minRaise, bigBlind);
	}

	// ------------------------------------------------------------------ seats

	/** Seats a player with {@code chips} (already taken from their Jugs by the caller). */
	public Result sit(int index, UUID player, String name, long chips) {
		if (index < 0 || index >= SEATS || seats[index].occupied()) {
			return Result.fail("That seat is taken.");
		}
		if (seatOf(player) >= 0) {
			return Result.fail("You are already at this table.");
		}
		Seat seat = seats[index];
		seat.clearHand();
		seat.player = player;
		seat.name = name;
		seat.stack = chips;
		seat.away = 0;
		note(name + " sat down with " + chips + ".");
		if (phase == Phase.WAITING) {
			timer = Math.max(timer, START_DELAY);
		}
		return new Result(true, "You sat down with " + chips + " chips.");
	}

	/** A player leaves: they fold if in a hand, and their chips are cashed out at once. */
	public Result stand(UUID player) {
		int index = seatOf(player);
		if (index < 0) {
			return Result.fail("You are not seated here.");
		}
		Seat seat = seats[index];
		if (seat.live() && handRunning()) {
			if (index == actor) {
				apply(index, Move.FOLD, 0);
			} else {
				seat.folded = true;
				note(seat.name + " folds.");
				if (liveCount() == 1) {
					awardToLast();
				}
			}
		}
		long chips = seat.stack;
		String name = seat.name;
		UUID id = seat.player;
		seat.clearHand();
		seat.player = null;
		seat.name = "";
		seat.stack = 0;
		bank.cashOut(id, name, chips);
		note(name + " left the table with " + chips + ".");
		if (handRunning() && actor == index) {
			advance();
		}
		return new Result(true, "You left the table with " + chips + " chips.");
	}

	// ------------------------------------------------------------------ time

	/** One game tick. */
	public void tick() {
		switch (phase) {
			case WAITING -> {
				if (playersWithChips() < 2) {
					timer = START_DELAY;
					return;
				}
				if (--timer <= 0) {
					startHand();
				}
			}
			case SHOWDOWN -> {
				if (--timer <= 0) {
					finishHand();
				}
			}
			default -> {
				if (actor >= 0 && --timer <= 0) {
					Seat seat = seats[actor];
					Move move = seat.bet >= currentBet ? Move.CHECK : Move.FOLD;
					note(seat.name + " ran out of time.");
					apply(actor, move, 0);
				}
			}
		}
	}

	private int playersWithChips() {
		int n = 0;
		for (Seat seat : seats) {
			if (seat.occupied() && seat.stack > 0) {
				n++;
			}
		}
		return n;
	}

	// ------------------------------------------------------------------ dealing

	private void startHand() {
		handNumber++;
		for (int i = 0; i < SEATS; i++) {
			seats[i].clearHand();
			contributed[i] = 0;
			contributor[i] = null;
			contributorName[i] = null;
			if (seats[i].occupied() && seats[i].stack > 0) {
				seats[i].inHand = true;
				contributor[i] = seats[i].player;
				contributorName[i] = seats[i].name;
			}
		}
		java.util.Arrays.fill(board, -1);
		for (int i = 0; i < 52; i++) {
			deck[i] = i;
		}
		for (int i = 51; i > 0; i--) {
			int j = random.nextInt(i + 1);
			int t = deck[i];
			deck[i] = deck[j];
			deck[j] = t;
		}
		deckPos = 0;
		button = nextInHand(button);
		int players = inHandCount();
		int sb = players == 2 ? button : nextInHand(button);
		int bb = nextInHand(sb);
		phase = Phase.PREFLOP;
		currentBet = 0;
		minRaise = bigBlind;
		post(sb, smallBlind);
		post(bb, bigBlind);
		currentBet = Math.max(bigBlind, Math.max(seats[sb].bet, seats[bb].bet));
		for (int round = 0; round < 2; round++) {
			int seat = button;
			for (int k = 0; k < players; k++) {
				seat = nextInHand(seat);
				if (round == 0) {
					seats[seat].card1 = deck[deckPos++];
				} else {
					seats[seat].card2 = deck[deckPos++];
				}
			}
		}
		note("Hand " + handNumber + ": " + seats[sb].name + " posts " + seats[sb].bet + ", " + seats[bb].name + " posts "
				+ seats[bb].bet + ".");
		actor = bb;
		advance();
	}

	private void post(int index, long amount) {
		Seat seat = seats[index];
		long paid = Math.min(amount, seat.stack);
		seat.stack -= paid;
		seat.bet += paid;
		contributed[index] += paid;
		if (seat.stack == 0) {
			seat.allIn = true;
		}
	}

	/** The next seat after {@code from} that is dealt into this hand. */
	private int nextInHand(int from) {
		for (int k = 1; k <= SEATS; k++) {
			int seat = Math.floorMod(from + k, SEATS);
			if (phase == Phase.WAITING ? seats[seat].occupied() && seats[seat].stack > 0 : seats[seat].inHand) {
				return seat;
			}
		}
		return from;
	}

	private int inHandCount() {
		int n = 0;
		for (Seat seat : seats) {
			if (seat.inHand) {
				n++;
			}
		}
		return n;
	}

	private int liveCount() {
		int n = 0;
		for (Seat seat : seats) {
			if (seat.live()) {
				n++;
			}
		}
		return n;
	}

	// ------------------------------------------------------------------ acting

	/** A seated player's move. {@code raiseTo} is the total bet a raise goes to. */
	public Result act(UUID player, Move move, long raiseTo) {
		int index = seatOf(player);
		if (index < 0 || !handRunning() || index != actor) {
			return Result.fail("It is not your turn.");
		}
		return apply(index, move, raiseTo);
	}

	private Result apply(int index, Move move, long raiseTo) {
		Seat seat = seats[index];
		long owe = currentBet - seat.bet;
		switch (move) {
			case FOLD -> {
				seat.folded = true;
				note(seat.name + " folds.");
			}
			case CHECK -> {
				if (owe > 0) {
					return Result.fail("You cannot check: " + owe + " to call.");
				}
				note(seat.name + " checks.");
			}
			case CALL -> {
				if (owe <= 0) {
					note(seat.name + " checks.");
				} else {
					long paid = Math.min(owe, seat.stack);
					pay(index, paid);
					note(seat.name + (seat.allIn ? " calls all in with " : " calls ") + paid + ".");
				}
			}
			case RAISE, ALL_IN -> {
				long most = seat.bet + seat.stack;
				long target = move == Move.ALL_IN ? most : raiseTo;
				if (target > most) {
					return Result.fail("You have only " + most + " in all.");
				}
				if (target <= currentBet) {
					if (move == Move.ALL_IN) {
						// All in for no more than a call.
						return apply(index, Move.CALL, 0);
					}
					return Result.fail("Raise to more than " + currentBet + ".");
				}
				if (target < minRaiseTo() && target < most) {
					return Result.fail("Raise to at least " + minRaiseTo() + ".");
				}
				long raise = target - currentBet;
				pay(index, target - seat.bet);
				if (raise >= minRaise) {
					minRaise = raise;
				}
				currentBet = target;
				for (int i = 0; i < SEATS; i++) {
					if (i != index && seats[i].canAct()) {
						seats[i].acted = false;
					}
				}
				note(seat.name + (seat.allIn ? " goes all in to " : (owe > 0 || currentBet > bigBlind ? " raises to " : " bets "))
						+ target + ".");
			}
		}
		seat.acted = true;
		if (liveCount() == 1) {
			awardToLast();
			return new Result(true, "");
		}
		advance();
		return new Result(true, "");
	}

	private void pay(int index, long amount) {
		Seat seat = seats[index];
		seat.stack -= amount;
		seat.bet += amount;
		contributed[index] += amount;
		if (seat.stack == 0) {
			seat.allIn = true;
		}
	}

	/** Finds the next player to act, or ends the betting round. */
	private void advance() {
		for (int k = 1; k <= SEATS; k++) {
			int seat = Math.floorMod(actor + k, SEATS);
			Seat s = seats[seat];
			if (s.canAct() && (!s.acted || s.bet < currentBet)) {
				// A lone player who has nothing to call need not act on a board nobody can bet against.
				if (canActCount() == 1 && s.bet >= currentBet) {
					break;
				}
				actor = seat;
				timer = turnTicks;
				return;
			}
		}
		endRound();
	}

	private int canActCount() {
		int n = 0;
		for (Seat seat : seats) {
			if (seat.canAct()) {
				n++;
			}
		}
		return n;
	}

	private void endRound() {
		for (Seat seat : seats) {
			seat.bet = 0;
			seat.acted = false;
		}
		currentBet = 0;
		minRaise = bigBlind;
		if (phase == Phase.RIVER) {
			showdown();
			return;
		}
		phase = switch (phase) {
			case PREFLOP -> Phase.FLOP;
			case FLOP -> Phase.TURN;
			default -> Phase.RIVER;
		};
		int dealt = phase == Phase.FLOP ? 3 : 1;
		int from = phase == Phase.FLOP ? 0 : phase == Phase.TURN ? 3 : 4;
		deckPos++; // burn a card
		StringBuilder cards = new StringBuilder();
		for (int i = 0; i < dealt; i++) {
			board[from + i] = deck[deckPos++];
			cards.append(i == 0 ? "" : " ").append(PokerHands.text(board[from + i]));
		}
		note((phase == Phase.FLOP ? "Flop: " : phase == Phase.TURN ? "Turn: " : "River: ") + cards + ".");
		actor = button;
		if (canActCount() <= 1) {
			// Everyone (or all but one) is all in: deal the rest of the board without betting.
			endRound();
			return;
		}
		advance();
	}

	// ------------------------------------------------------------------ winning

	private void awardToLast() {
		int winner = -1;
		for (int i = 0; i < SEATS; i++) {
			if (seats[i].live()) {
				winner = i;
			}
		}
		long pot = pot();
		seats[winner].stack += pot;
		java.util.Arrays.fill(contributed, 0);
		note(seats[winner].name + " wins " + pot + ".");
		phase = Phase.SHOWDOWN;
		actor = -1;
		timer = SHOWDOWN_TICKS / 2;
	}

	private void showdown() {
		// Deal any board still missing (when everyone was all in).
		int dealt = countBoard();
		for (int i = dealt; i < 5; i++) {
			deckPos++;
			board[i] = deck[deckPos++];
		}
		phase = Phase.SHOWDOWN;
		actor = -1;
		timer = SHOWDOWN_TICKS;
		int[] score = new int[SEATS];
		for (int i = 0; i < SEATS; i++) {
			if (seats[i].live()) {
				seats[i].shown = true;
				score[i] = PokerHands.best(new int[] {seats[i].card1, seats[i].card2, board[0], board[1], board[2], board[3], board[4]});
				note(seats[i].name + " shows " + PokerHands.text(seats[i].card1) + " " + PokerHands.text(seats[i].card2) + ": "
						+ PokerHands.describe(score[i]) + ".");
			}
		}
		// Side pots: each level that a live player put in is a pot for the live players who put in at least that much.
		List<Long> levels = new ArrayList<>();
		for (int i = 0; i < SEATS; i++) {
			if (seats[i].live() && contributed[i] > 0 && !levels.contains(contributed[i])) {
				levels.add(contributed[i]);
			}
		}
		java.util.Collections.sort(levels);
		long previous = 0;
		for (long level : levels) {
			long slice = 0;
			for (int i = 0; i < SEATS; i++) {
				slice += Math.min(contributed[i], level) - Math.min(contributed[i], previous);
			}
			awardSlice(slice, level, score);
			previous = level;
		}
		// Chips a folded player put in above every live player's level go to the best live hand.
		long rest = 0;
		for (int i = 0; i < SEATS; i++) {
			rest += Math.max(0, contributed[i] - previous);
		}
		if (rest > 0) {
			awardSlice(rest, previous, score);
		}
		java.util.Arrays.fill(contributed, 0);
	}

	/** Gives one pot to the best live hand among those who put in at least {@code level}; ties split it. */
	private void awardSlice(long slice, long level, int[] score) {
		if (slice <= 0) {
			return;
		}
		int best = -1;
		List<Integer> winners = new ArrayList<>();
		for (int k = 1; k <= SEATS; k++) {
			int i = Math.floorMod(button + k, SEATS);
			if (!seats[i].live() || contributed[i] < level) {
				continue;
			}
			if (score[i] > best) {
				best = score[i];
				winners.clear();
			}
			if (score[i] == best) {
				winners.add(i);
			}
		}
		if (winners.isEmpty()) {
			return;
		}
		long share = slice / winners.size();
		long odd = slice % winners.size();
		for (int w = 0; w < winners.size(); w++) {
			int i = winners.get(w);
			long won = share + (w < odd ? 1 : 0);
			seats[i].stack += won;
			note(seats[i].name + " wins " + won + (winners.size() > 1 ? " (split)" : "") + " with "
					+ PokerHands.describe(score[i]).toLowerCase(java.util.Locale.ROOT) + ".");
		}
	}

	private void finishHand() {
		phase = Phase.WAITING;
		actor = -1;
		for (Seat seat : seats) {
			seat.clearHand();
		}
		java.util.Arrays.fill(board, -1);
		// Busted players stand up (with nothing to cash out).
		for (Seat seat : seats) {
			if (seat.occupied() && seat.stack <= 0) {
				note(seat.name + " is out of chips.");
				bank.cashOut(seat.player, seat.name, 0);
				seat.player = null;
				seat.name = "";
			}
		}
		timer = START_DELAY;
	}

	/**
	 * Calls off a hand in progress (the table was broken or the world closed): every chip put in this hand goes back to
	 * whoever put it in. Returns the chips owed to seats now empty, by seat, for the caller to pay out.
	 */
	public long[] cancelHand() {
		long[] owed = new long[SEATS];
		for (int i = 0; i < SEATS; i++) {
			if (seats[i].occupied() && seats[i].player.equals(contributor[i])) {
				seats[i].stack += contributed[i];
			} else {
				owed[i] = contributed[i];
			}
			contributed[i] = 0;
			seats[i].clearHand();
		}
		java.util.Arrays.fill(board, -1);
		phase = Phase.WAITING;
		actor = -1;
		timer = START_DELAY;
		return owed;
	}

	private void note(String line) {
		log.add(line);
		while (log.size() > LOG_LINES) {
			log.remove(0);
		}
	}
}
