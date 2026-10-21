package io.github.jimbozoomer.jugcraft.finance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * American roulette: 38 pockets (0, 00 and 1 to 36), every standard bet at its casino payout. A bet covering k numbers
 * pays 36/k - 1 to 1 (straight 35, split 17, street 11, corner 8, six line 5, dozen or column 2, even money 1), except
 * the five-number bet (0, 00, 1, 2, 3), which pays 6 to 1. The house edge is 2/38 = 5.26% on every bet but the five,
 * where it is 7.89%. Plain Java; numbers are 0..36 and 37 for 00, a bet's numbers are bits of a long.
 *
 * <p>Bets open when the first chip goes down and close when the betting clock runs out; then the wheel spins, the
 * ball lands, and the table pays.
 */
public class Roulette {
	public static final int DOUBLE_ZERO = 37;
	public static final int POCKETS = 38;
	public static final int[] WHEEL = {0, 28, 9, 26, 30, 11, 7, 20, 32, 17, 5, 22, 34, 15, 3, 24, 36, 13, 1, DOUBLE_ZERO,
			27, 10, 25, 29, 12, 8, 19, 31, 18, 6, 21, 33, 16, 4, 23, 35, 14, 2};
	public static final int MAX_BETS_EACH = 60;
	public static final int SPIN_TICKS = 120;
	private static final int RESULT_TICKS = 80;
	private static final java.util.Set<Integer> REDS = java.util.Set.of(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25,
			27, 30, 32, 34, 36);
	/** Every legal bet: its numbers and its name. */
	private static final Map<Long, String> LEGAL = new HashMap<>();

	public enum Phase { IDLE, BETTING, SPINNING, RESULT }

	public record Bet(UUID player, String name, long numbers, long amount) {
	}

	public Phase phase = Phase.IDLE;
	public int timer;
	public int betTicks = 20 * 20;
	public int result = -1;
	public final List<Bet> bets = new ArrayList<>();
	public final List<Integer> history = new ArrayList<>();
	public final List<String> log = new ArrayList<>();
	public long spins;

	private final CasinoHouse house;
	private final Random random;

	static {
		for (int n = 0; n < POCKETS; n++) {
			legal(bit(n), "Straight " + label(n));
		}
		for (int row = 0; row < 12; row++) {
			int first = row * 3 + 1;
			legal(bits(first, first + 1, first + 2), "Street " + first + "-" + (first + 2));
			for (int col = 0; col < 3; col++) {
				int n = first + col;
				if (col < 2) {
					legal(bits(n, n + 1), "Split " + n + "/" + (n + 1));
				}
				if (row < 11) {
					legal(bits(n, n + 3), "Split " + n + "/" + (n + 3));
				}
				if (col < 2 && row < 11) {
					legal(bits(n, n + 1, n + 3, n + 4), "Corner " + n + "-" + (n + 4));
				}
			}
			if (row < 11) {
				legal(bits(first, first + 1, first + 2, first + 3, first + 4, first + 5), "Line " + first + "-" + (first + 5));
			}
		}
		legal(bits(0, DOUBLE_ZERO), "Split 0/00");
		legal(bits(0, 1), "Split 0/1");
		legal(bits(0, 2), "Split 0/2");
		legal(bits(DOUBLE_ZERO, 2), "Split 00/2");
		legal(bits(DOUBLE_ZERO, 3), "Split 00/3");
		legal(bits(0, 1, 2), "Trio 0-1-2");
		legal(bits(0, DOUBLE_ZERO, 2), "Trio 0-00-2");
		legal(bits(DOUBLE_ZERO, 2, 3), "Trio 00-2-3");
		legal(bits(0, DOUBLE_ZERO, 1, 2, 3), "Five 0-00-1-2-3");
		for (int d = 0; d < 3; d++) {
			long dozen = 0;
			long column = 0;
			for (int k = 0; k < 12; k++) {
				dozen |= bit(d * 12 + k + 1);
				column |= bit(k * 3 + d + 1);
			}
			legal(dozen, (d == 0 ? "1st" : d == 1 ? "2nd" : "3rd") + " dozen");
			legal(column, "Column " + (d + 1));
		}
		long low = 0;
		long high = 0;
		long even = 0;
		long odd = 0;
		long red = 0;
		long black = 0;
		for (int n = 1; n <= 36; n++) {
			if (n <= 18) {
				low |= bit(n);
			} else {
				high |= bit(n);
			}
			if (n % 2 == 0) {
				even |= bit(n);
			} else {
				odd |= bit(n);
			}
			if (REDS.contains(n)) {
				red |= bit(n);
			} else {
				black |= bit(n);
			}
		}
		legal(low, "1 to 18");
		legal(high, "19 to 36");
		legal(even, "Even");
		legal(odd, "Odd");
		legal(red, "Red");
		legal(black, "Black");
	}

	public Roulette(CasinoHouse house, Random random) {
		this.house = house;
		this.random = random;
	}

	private static void legal(long numbers, String name) {
		LEGAL.put(numbers, name);
	}

	public static long bit(int n) {
		return 1L << n;
	}

	public static long bits(int... numbers) {
		long out = 0;
		for (int n : numbers) {
			out |= bit(n);
		}
		return out;
	}

	public static String label(int n) {
		return n == DOUBLE_ZERO ? "00" : String.valueOf(n);
	}

	public static boolean red(int n) {
		return REDS.contains(n);
	}

	public static boolean isLegal(long numbers) {
		return LEGAL.containsKey(numbers);
	}

	public static String name(long numbers) {
		return LEGAL.getOrDefault(numbers, "?");
	}

	/** What a bet pays to 1 when it wins. */
	public static int ratio(long numbers) {
		int k = Long.bitCount(numbers);
		return k == 5 ? 6 : 36 / k - 1;
	}

	/** The most the house could owe if the ball lands badly for it, on these bets plus one more. */
	long worstCase(long extraNumbers, long extraAmount) {
		long worst = 0;
		for (int n = 0; n < POCKETS; n++) {
			long owed = 0;
			for (Bet bet : bets) {
				if ((bet.numbers() & bit(n)) != 0) {
					owed += bet.amount() * ratio(bet.numbers());
				}
			}
			if ((extraNumbers & bit(n)) != 0) {
				owed += extraAmount * ratio(extraNumbers);
			}
			worst = Math.max(worst, owed);
		}
		return worst;
	}

	public FinanceBook.Result bet(UUID player, String name, long numbers, long amount) {
		if (phase == Phase.SPINNING) {
			return FinanceBook.Result.fail("No more bets: the ball is rolling.");
		}
		if (!isLegal(numbers)) {
			return FinanceBook.Result.fail("That is not a bet on this table.");
		}
		long mine = 0;
		int count = 0;
		for (Bet bet : bets) {
			if (bet.player().equals(player)) {
				mine += bet.amount();
				count++;
			}
		}
		if (count >= MAX_BETS_EACH) {
			return FinanceBook.Result.fail("That is as many bets as one player may place.");
		}
		if (amount < house.minBet()) {
			return FinanceBook.Result.fail("The least chip here is " + FinanceBook.amount(house.minBet()) + ".");
		}
		if (mine + amount > house.maxBet()) {
			return FinanceBook.Result.fail("The table limit is " + FinanceBook.amount(house.maxBet()) + " a spin.");
		}
		if (worstCase(numbers, amount) > house.cover(player)) {
			return FinanceBook.Result.fail("The house cannot cover that bet.");
		}
		String failed = house.stake(player, amount);
		if (failed != null) {
			return FinanceBook.Result.fail(failed);
		}
		if (phase == Phase.RESULT || phase == Phase.IDLE) {
			phase = Phase.BETTING;
			timer = betTicks;
		}
		// The same bet again adds to the chips already there.
		for (int i = 0; i < bets.size(); i++) {
			Bet bet = bets.get(i);
			if (bet.player().equals(player) && bet.numbers() == numbers) {
				bets.set(i, new Bet(player, name, numbers, bet.amount() + amount));
				return new FinanceBook.Result(true, "");
			}
		}
		bets.add(new Bet(player, name, numbers, amount));
		return new FinanceBook.Result(true, "");
	}

	/** Takes all of a player's chips back while bets are open. */
	public FinanceBook.Result clear(UUID player) {
		if (phase == Phase.SPINNING) {
			return FinanceBook.Result.fail("No more bets: the ball is rolling.");
		}
		long back = 0;
		for (int i = bets.size() - 1; i >= 0; i--) {
			if (bets.get(i).player().equals(player)) {
				back += bets.remove(i).amount();
			}
		}
		if (back > 0) {
			house.refund(player, back);
		}
		return new FinanceBook.Result(true, "");
	}

	/** Lets the betting clock run out at once (any player at the table may call the spin). */
	public FinanceBook.Result spinNow(UUID player) {
		if (phase != Phase.BETTING) {
			return FinanceBook.Result.fail("Place a bet first.");
		}
		timer = Math.min(timer, 20);
		return new FinanceBook.Result(true, "");
	}

	public void tick() {
		switch (phase) {
			case IDLE -> {
			}
			case BETTING -> {
				if (bets.isEmpty()) {
					phase = Phase.IDLE;
				} else if (--timer <= 0) {
					phase = Phase.SPINNING;
					timer = SPIN_TICKS;
					result = random.nextInt(POCKETS);
					spins++;
				}
			}
			case SPINNING -> {
				if (--timer <= 0) {
					pay();
					phase = Phase.RESULT;
					timer = RESULT_TICKS;
				}
			}
			case RESULT -> {
				if (--timer <= 0) {
					phase = bets.isEmpty() ? Phase.IDLE : Phase.BETTING;
					timer = betTicks;
				}
			}
		}
	}

	private void pay() {
		String colour = result == 0 || result == DOUBLE_ZERO ? "green" : red(result) ? "red" : "black";
		note(label(result) + " " + colour + ".");
		history.add(result);
		while (history.size() > 12) {
			history.remove(0);
		}
		Map<UUID, long[]> totals = new HashMap<>();
		Map<UUID, String> names = new HashMap<>();
		for (Bet bet : bets) {
			boolean won = (bet.numbers() & bit(result)) != 0;
			long payout = won ? bet.amount() * (1 + ratio(bet.numbers())) : 0;
			String big = won && ratio(bet.numbers()) == 35 && bet.amount() >= 20
					? bet.name() + " hit " + label(result) + " straight up at roulette for " + FinanceBook.amount(payout - bet.amount())
					+ " Jugs." : null;
			house.settle(bet.player(), bet.name(), bet.amount(), payout, big);
			long[] t = totals.computeIfAbsent(bet.player(), k -> new long[2]);
			t[0] += bet.amount();
			t[1] += payout;
			names.put(bet.player(), bet.name());
		}
		for (Map.Entry<UUID, long[]> entry : totals.entrySet()) {
			long net = entry.getValue()[1] - entry.getValue()[0];
			note(names.get(entry.getKey()) + (net > 0 ? " wins " + FinanceBook.amount(net) : net < 0 ? " loses "
					+ FinanceBook.amount(-net) : " breaks even") + ".");
		}
		bets.clear();
	}

	public void cancelAll() {
		for (Bet bet : bets) {
			house.refund(bet.player(), bet.amount());
		}
		bets.clear();
		phase = Phase.IDLE;
	}

	/** A player who walks away loses nothing: chips not yet spun on come back. */
	public void away(UUID player) {
		if (phase != Phase.SPINNING) {
			clear(player);
		}
	}

	public Map<UUID, Long> outstanding() {
		Map<UUID, Long> out = new HashMap<>();
		for (Bet bet : bets) {
			out.merge(bet.player(), bet.amount(), Long::sum);
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
