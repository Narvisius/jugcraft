package io.github.jimbozoomer.jugcraft.finance;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * The arcade cabinet's five games, one player at a time, all against the house:
 * <ul>
 * <li><b>Video Poker</b>, Jacks or Better on the full-pay 9/6 table (99.54% with perfect play): deal five, hold any,
 * draw once.</li>
 * <li><b>Keno</b>: pick 1 to 10 of 80 numbers, twenty are drawn; casino-style paytables returning 75% to 92%.</li>
 * <li><b>Mines</b>: a 5x5 grid hides 1 to 24 TNT. Each safe tile raises the payout to 99% of fair odds; cash out any
 * time; TNT loses the bet. Wins are capped at 1,000 times the bet.</li>
 * <li><b>Crash</b>: a firework climbs; cash out before it bursts. The burst point is drawn so that cashing out at any
 * multiplier x wins with chance 0.99/x (a 1% edge); it is capped at 1,000x.</li>
 * <li><b>Hi-Lo</b>: guess whether the next card is higher or lower (equal counts as right); each right guess pays 98%
 * of fair odds and the chain multiplies; cash out any time. Capped at 1,000x.</li>
 * </ul>
 * Payouts round down to whole Jugs. Plain Java.
 */
public class ArcadeGame {
	public enum Mode {
		VIDEO_POKER("Video Poker"), KENO("Keno"), MINES("Mines"), CRASH("Crash"), HILO("Hi-Lo");

		public final String title;

		Mode(String title) {
			this.title = title;
		}
	}

	public enum Phase { IDLE, PLAYING, DONE }

	public static final long CAP = 1_000;
	private static final int IDLE_RELEASE = 20 * 60;

	// Video poker pays (for 1, the stake included): pair of jacks or better 1, two pair 2, three of a kind 3, straight
	// 4, flush 6, full house 9, four of a kind 25, straight flush 50, royal flush 800.
	public static final String[] POKER_HANDS = {"Jacks or Better", "Two Pair", "Three of a Kind", "Straight", "Flush",
			"Full House", "Four of a Kind", "Straight Flush", "Royal Flush"};
	public static final int[] POKER_PAYS = {1, 2, 3, 4, 6, 9, 25, 50, 800};

	/** Keno pays, times the bet, by spots picked then numbers caught ({catch, pays, ...}). */
	public static final int[][] KENO = {
			{},
			{1, 3},
			{2, 14},
			{2, 2, 3, 44},
			{2, 1, 3, 5, 4, 120},
			{3, 2, 4, 15, 5, 800},
			{3, 1, 4, 6, 5, 90, 6, 1_600},
			{3, 1, 4, 4, 5, 20, 6, 300, 7, 5_000},
			{4, 2, 5, 15, 6, 80, 7, 1_500, 8, 10_000},
			{4, 1, 5, 7, 6, 44, 7, 300, 8, 4_000, 9, 25_000},
			{0, 3, 5, 3, 6, 20, 7, 130, 8, 1_000, 9, 5_000, 10, 100_000}};

	public Mode mode = Mode.VIDEO_POKER;
	public Phase phase = Phase.IDLE;
	public UUID player;
	public String playerName = "";
	public long bet;
	public long lastWin;
	public long rounds;
	public final List<String> log = new ArrayList<>();

	// Video poker.
	public int[] hand = new int[5];
	public int pokerResult = -1;
	private int[] deck = new int[52];
	private int dealt;

	// Keno.
	public long kenoPicks;
	public long kenoDrawn;

	// Mines.
	public int mineCount = 3;
	public long minesHidden;
	public long minesRevealed;
	public boolean mineHit;

	// Crash.
	public double crashPoint;
	public int crashTicks;
	public double autoCashout;
	public double cashedAt;

	// Hi-Lo.
	public int card = -1;
	public double hiloMultiplier = 1;
	public int hiloSteps;
	public final List<Integer> hiloCards = new ArrayList<>();

	private final CasinoHouse house;
	private final Random random;
	private int idle;

	public ArcadeGame(CasinoHouse house, Random random) {
		this.house = house;
		this.random = random;
	}

	// ------------------------------------------------------------------ seat

	public FinanceBook.Result use(UUID id, String name) {
		if (player != null && !player.equals(id)) {
			return FinanceBook.Result.fail(playerName + " is playing this cabinet.");
		}
		player = id;
		playerName = name;
		idle = 0;
		return new FinanceBook.Result(true, "");
	}

	/** The player walks away: a round in play is finished in their favour where it can be (cashed out) or played out. */
	public void release(UUID id) {
		if (!id.equals(player)) {
			return;
		}
		if (phase == Phase.PLAYING) {
			switch (mode) {
				case VIDEO_POKER -> draw(id, 31);
				case MINES -> {
					if (minesRevealed != 0) {
						cashOut(id);
					} else {
						refundRound();
					}
				}
				case CRASH -> cashOut(id);
				case HILO -> {
					if (hiloSteps > 0) {
						cashOut(id);
					} else {
						refundRound();
					}
				}
				default -> {
				}
			}
		}
		player = null;
		playerName = "";
	}

	private void refundRound() {
		house.refund(player, bet);
		phase = Phase.IDLE;
	}

	public FinanceBook.Result setMode(UUID id, String name, Mode next) {
		FinanceBook.Result using = use(id, name);
		if (!using.ok()) {
			return using;
		}
		if (phase == Phase.PLAYING) {
			return FinanceBook.Result.fail("Finish this round first.");
		}
		mode = next;
		phase = Phase.IDLE;
		return new FinanceBook.Result(true, "");
	}

	private FinanceBook.Result begin(UUID id, String name, long amount, long maxWin) {
		FinanceBook.Result using = use(id, name);
		if (!using.ok()) {
			return using;
		}
		if (phase == Phase.PLAYING) {
			return FinanceBook.Result.fail("Finish this round first.");
		}
		if (amount < house.minBet()) {
			return FinanceBook.Result.fail("The least bet here is " + FinanceBook.amount(house.minBet()) + ".");
		}
		if (amount > house.maxBet()) {
			return FinanceBook.Result.fail("The most this cabinet takes is " + FinanceBook.amount(house.maxBet()) + ".");
		}
		if (maxWin > house.cover(id)) {
			return FinanceBook.Result.fail("The house cannot cover the top prize at that bet.");
		}
		String failed = house.stake(id, amount);
		if (failed != null) {
			return FinanceBook.Result.fail(failed);
		}
		bet = amount;
		lastWin = 0;
		rounds++;
		return null;
	}

	private void pay(long payout, String what) {
		lastWin = payout;
		String big = payout >= bet * 100 && payout >= 500 ? playerName + " won " + FinanceBook.amount(payout) + " Jugs at "
				+ mode.title + "." : null;
		house.settle(player, playerName, bet, payout, big);
		phase = Phase.DONE;
		note(what);
	}

	// ------------------------------------------------------------------ video poker

	public FinanceBook.Result deal(UUID id, String name, long amount) {
		FinanceBook.Result fail = begin(id, name, amount, amount * POKER_PAYS[8]);
		if (fail != null) {
			return fail;
		}
		mode = Mode.VIDEO_POKER;
		for (int i = 0; i < 52; i++) {
			deck[i] = i;
		}
		for (int i = 51; i > 0; i--) {
			int j = random.nextInt(i + 1);
			int t = deck[i];
			deck[i] = deck[j];
			deck[j] = t;
		}
		for (int i = 0; i < 5; i++) {
			hand[i] = deck[i];
		}
		dealt = 5;
		pokerResult = -1;
		phase = Phase.PLAYING;
		return new FinanceBook.Result(true, "");
	}

	/** Draws for every card not held (bit i holds card i). */
	public FinanceBook.Result draw(UUID id, int holds) {
		if (mode != Mode.VIDEO_POKER || phase != Phase.PLAYING || !id.equals(player)) {
			return FinanceBook.Result.fail("Deal first.");
		}
		for (int i = 0; i < 5; i++) {
			if ((holds & (1 << i)) == 0) {
				hand[i] = deck[dealt++];
			}
		}
		pokerResult = pokerHand(hand);
		long payout = pokerResult < 0 ? 0 : bet * POKER_PAYS[pokerResult];
		pay(payout, pokerResult < 0 ? "No win." : POKER_HANDS[pokerResult] + "! " + FinanceBook.amount(payout));
		return new FinanceBook.Result(true, "");
	}

	/** The paying hand (index into {@link #POKER_HANDS}), or -1. */
	public static int pokerHand(int[] cards) {
		int score = PokerHands.score5(cards.clone());
		int category = PokerHands.category(score);
		switch (category) {
			case 8 -> {
				boolean royal = true;
				for (int c : cards) {
					royal &= PokerHands.rank(c) >= 8;
				}
				return royal ? 8 : 7;
			}
			case 7 -> {
				return 6;
			}
			case 6 -> {
				return 5;
			}
			case 5 -> {
				return 4;
			}
			case 4 -> {
				return 3;
			}
			case 3 -> {
				return 2;
			}
			case 2 -> {
				return 1;
			}
			case 1 -> {
				int[] count = new int[13];
				for (int c : cards) {
					count[PokerHands.rank(c)]++;
				}
				for (int r = 9; r <= 12; r++) {
					if (count[r] == 2) {
						return 0;
					}
				}
				return -1;
			}
			default -> {
				return -1;
			}
		}
	}

	// ------------------------------------------------------------------ keno

	public static long kenoTop(int spots) {
		long top = 0;
		int[] table = KENO[spots];
		for (int i = 1; i < table.length; i += 2) {
			top = Math.max(top, table[i]);
		}
		return top;
	}

	public static long kenoPays(int spots, int caught) {
		int[] table = KENO[spots];
		for (int i = 0; i < table.length; i += 2) {
			if (table[i] == caught) {
				return table[i + 1];
			}
		}
		return 0;
	}

	/** The exact return of a keno ticket of this many spots. */
	public static double kenoReturn(int spots) {
		double total = 0;
		for (int caught = 0; caught <= spots; caught++) {
			total += kenoPays(spots, caught) * choose(spots, caught) * choose(80 - spots, 20 - caught) / choose(80, 20);
		}
		return total;
	}

	private static double choose(int n, int k) {
		if (k < 0 || k > n) {
			return 0;
		}
		double out = 1;
		for (int i = 1; i <= k; i++) {
			out = out * (n - k + i) / i;
		}
		return out;
	}

	/** Plays a keno ticket: {@code picks} bits 0..79 for numbers 1..80. */
	public FinanceBook.Result keno(UUID id, String name, long amount, long picksLow, long picksHigh) {
		int spots = Long.bitCount(picksLow) + Long.bitCount(picksHigh & 0xFFFFL);
		if (spots < 1 || spots > 10) {
			return FinanceBook.Result.fail("Pick 1 to 10 numbers.");
		}
		FinanceBook.Result fail = begin(id, name, amount, amount * kenoTop(spots));
		if (fail != null) {
			return fail;
		}
		mode = Mode.KENO;
		kenoPicks = picksLow;
		long high = picksHigh & 0xFFFFL;
		// Twenty different numbers of the eighty.
		List<Integer> pool = new ArrayList<>();
		for (int n = 0; n < 80; n++) {
			pool.add(n);
		}
		java.util.Collections.shuffle(pool, random);
		long drawnLow = 0;
		long drawnHigh = 0;
		int caught = 0;
		for (int i = 0; i < 20; i++) {
			int n = pool.get(i);
			if (n < 64) {
				drawnLow |= 1L << n;
				if ((picksLow & (1L << n)) != 0) {
					caught++;
				}
			} else {
				drawnHigh |= 1L << (n - 64);
				if ((high & (1L << (n - 64))) != 0) {
					caught++;
				}
			}
		}
		kenoDrawn = drawnLow;
		kenoDrawnHigh = drawnHigh;
		kenoPicksHigh = high;
		kenoCaught = caught;
		long payout = amount * kenoPays(spots, caught);
		pay(payout, "Caught " + caught + " of " + spots + (payout > 0 ? ": " + FinanceBook.amount(payout) : "."));
		return new FinanceBook.Result(true, "");
	}

	public long kenoDrawnHigh;
	public long kenoPicksHigh;
	public int kenoCaught;

	// ------------------------------------------------------------------ mines

	/** The payout multiplier after {@code safe} safe tiles with {@code mines} hidden: 99% of fair, capped. */
	public static double minesMultiplier(int mines, int safe) {
		double fair = 1;
		for (int i = 0; i < safe; i++) {
			fair *= (25.0 - i) / (25.0 - mines - i);
		}
		return Math.min(CAP, 0.99 * fair);
	}

	public FinanceBook.Result minesStart(UUID id, String name, long amount, int mines) {
		if (mines < 1 || mines > 24) {
			return FinanceBook.Result.fail("Hide 1 to 24 TNT.");
		}
		FinanceBook.Result fail = begin(id, name, amount, (long) Math.ceil(amount * minesMultiplier(mines, 25 - mines)));
		if (fail != null) {
			return fail;
		}
		mode = Mode.MINES;
		mineCount = mines;
		minesHidden = 0;
		minesRevealed = 0;
		mineHit = false;
		List<Integer> cells = new ArrayList<>();
		for (int i = 0; i < 25; i++) {
			cells.add(i);
		}
		java.util.Collections.shuffle(cells, random);
		for (int i = 0; i < mines; i++) {
			minesHidden |= 1L << cells.get(i);
		}
		phase = Phase.PLAYING;
		return new FinanceBook.Result(true, "");
	}

	public FinanceBook.Result reveal(UUID id, int cell) {
		if (mode != Mode.MINES || phase != Phase.PLAYING || !id.equals(player) || cell < 0 || cell >= 25) {
			return FinanceBook.Result.fail("Start a round first.");
		}
		if ((minesRevealed & (1L << cell)) != 0) {
			return new FinanceBook.Result(true, "");
		}
		minesRevealed |= 1L << cell;
		if ((minesHidden & (1L << cell)) != 0) {
			mineHit = true;
			pay(0, "BOOM! The TNT went off.");
			return new FinanceBook.Result(true, "");
		}
		int safe = Long.bitCount(minesRevealed);
		if (safe >= 25 - mineCount || minesMultiplier(mineCount, safe) >= CAP) {
			return cashOut(id);
		}
		return new FinanceBook.Result(true, "");
	}

	// ------------------------------------------------------------------ crash

	/** The firework's multiplier after so many ticks: doubling about every six seconds. */
	public static double crashAt(int ticks) {
		return Math.exp(ticks * Math.log(2) / 120.0);
	}

	public FinanceBook.Result crashStart(UUID id, String name, long amount, double auto) {
		FinanceBook.Result fail = begin(id, name, amount, amount * CAP);
		if (fail != null) {
			return fail;
		}
		mode = Mode.CRASH;
		// P(burst point >= x) = 0.99 / x for x >= 1; about one launch in a hundred bursts at once.
		double u = random.nextDouble();
		crashPoint = Math.min(CAP, Math.max(1.0, Math.floor(99.0 / (1 - u)) / 100.0));
		crashTicks = 0;
		cashedAt = 0;
		autoCashout = auto >= 1.01 ? Math.min(auto, CAP) : 0;
		phase = Phase.PLAYING;
		if (crashPoint <= 1.0) {
			pay(0, "It burst on the launch pad!");
		}
		return new FinanceBook.Result(true, "");
	}

	// ------------------------------------------------------------------ hi-lo

	/** Chance the next card (ranks 0 two .. 12 ace, each equally likely) is at least / at most this one. */
	public static double hiloChance(int rank, boolean higher) {
		return higher ? (13 - rank) / 13.0 : (rank + 1) / 13.0;
	}

	public FinanceBook.Result hiloStart(UUID id, String name, long amount) {
		FinanceBook.Result fail = begin(id, name, amount, amount * CAP);
		if (fail != null) {
			return fail;
		}
		mode = Mode.HILO;
		card = random.nextInt(52);
		hiloMultiplier = 1;
		hiloSteps = 0;
		hiloCards.clear();
		hiloCards.add(card);
		phase = Phase.PLAYING;
		return new FinanceBook.Result(true, "");
	}

	public FinanceBook.Result guess(UUID id, boolean higher) {
		if (mode != Mode.HILO || phase != Phase.PLAYING || !id.equals(player)) {
			return FinanceBook.Result.fail("Start a round first.");
		}
		int rank = PokerHands.rank(card);
		double chance = hiloChance(rank, higher);
		if (chance >= 1) {
			return FinanceBook.Result.fail("That always wins: guess the other way.");
		}
		int next = random.nextInt(52);
		int nextRank = PokerHands.rank(next);
		card = next;
		hiloCards.add(next);
		while (hiloCards.size() > 8) {
			hiloCards.remove(0);
		}
		boolean right = higher ? nextRank >= rank : nextRank <= rank;
		if (!right) {
			pay(0, "Wrong: " + PokerHands.text(next) + ".");
			return new FinanceBook.Result(true, "");
		}
		hiloSteps++;
		hiloMultiplier = Math.min(CAP, hiloMultiplier * 0.98 / chance);
		if (hiloMultiplier >= CAP) {
			return cashOut(id);
		}
		return new FinanceBook.Result(true, "");
	}

	// ------------------------------------------------------------------ cashing out

	public FinanceBook.Result cashOut(UUID id) {
		if (phase != Phase.PLAYING || !id.equals(player)) {
			return FinanceBook.Result.fail("Nothing to cash out.");
		}
		switch (mode) {
			case MINES -> {
				int safe = Long.bitCount(minesRevealed);
				if (safe == 0) {
					// Nothing revealed yet: the bet comes back.
					refundRound();
					note("Bet taken back.");
					return new FinanceBook.Result(true, "");
				}
				double m = minesMultiplier(mineCount, safe);
				long payout = (long) Math.floor(bet * m);
				pay(payout, String.format(java.util.Locale.ROOT, "Cashed out at %.2fx: %s", m, FinanceBook.amount(payout)));
			}
			case CRASH -> {
				double m = Math.floor(crashAt(crashTicks) * 100) / 100.0;
				if (m >= crashPoint) {
					return FinanceBook.Result.fail("Too late!");
				}
				cashedAt = m;
				long payout = (long) Math.floor(bet * m);
				pay(payout, String.format(java.util.Locale.ROOT, "Cashed out at %.2fx: %s", m, FinanceBook.amount(payout)));
			}
			case HILO -> {
				if (hiloSteps == 0) {
					refundRound();
					note("Bet taken back.");
					return new FinanceBook.Result(true, "");
				}
				long payout = (long) Math.floor(bet * hiloMultiplier);
				pay(payout, String.format(java.util.Locale.ROOT, "Cashed out at %.2fx: %s", hiloMultiplier,
						FinanceBook.amount(payout)));
			}
			default -> {
				return FinanceBook.Result.fail("Nothing to cash out.");
			}
		}
		return new FinanceBook.Result(true, "");
	}

	public void tick() {
		if (mode == Mode.CRASH && phase == Phase.PLAYING) {
			crashTicks++;
			double m = Math.floor(crashAt(crashTicks) * 100) / 100.0;
			if (autoCashout > 0 && m >= autoCashout && autoCashout < crashPoint) {
				// Cash out exactly at the target.
				cashedAt = autoCashout;
				long payout = (long) Math.floor(bet * autoCashout);
				pay(payout, String.format(java.util.Locale.ROOT, "Auto cash out at %.2fx: %s", autoCashout,
						FinanceBook.amount(payout)));
			} else if (m >= crashPoint) {
				pay(0, String.format(java.util.Locale.ROOT, "Burst at %.2fx.", crashPoint));
			}
		}
		if (player != null && phase != Phase.PLAYING && ++idle > IDLE_RELEASE) {
			release(player);
		}
	}

	/** Stakes still in play (a world closing mid-round gives them back). */
	public long outstanding() {
		return phase == Phase.PLAYING ? bet : 0;
	}

	public void cancelAll() {
		if (phase == Phase.PLAYING && player != null) {
			refundRound();
		}
	}

	private void note(String line) {
		log.add(line);
		while (log.size() > 6) {
			log.remove(0);
		}
	}
}
