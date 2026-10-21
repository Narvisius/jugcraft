package io.github.jimbozoomer.jugcraft.finance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Casino craps. The shooter's come-out roll wins the pass line on 7 or 11 and loses it on 2, 3 or 12 (don't pass the
 * reverse, 12 a push); any other number becomes the point, and the line wins if the point comes again before a 7
 * (seven out, and the dice pass to the next shooter). Come and don't come work the same from any roll while a point
 * is on. Line and come bets take odds behind them at true odds (3, 4 and 5 times the bet on 4/10, 5/9 and 6/8; lay up
 * to 6 times), which the house has no edge on. Place bets pay 9:5 on 4 and 10, 7:5 on 5 and 9, 7:6 on 6 and 8; the
 * field 2:1 on 2 and 3:1 on 12, even money on 3, 4, 9, 10 and 11; hardways 7:1 (4, 10) and 9:1 (6, 8); any seven 4:1,
 * any craps 7:1, eleven 15:1, aces and twelve 30:1. Place bets and hardways are off on the come-out roll; come bet odds
 * too. Payouts round down to whole Jugs. Plain Java.
 */
public class Craps {
	public static final int ROLL_TICKS = 40;
	public static final int MAX_BETS_EACH = 30;
	private static final int SHOOTER_DELAY = 60;

	public enum Kind { PASS, DONT_PASS, COME, DONT_COME, PLACE, FIELD, ANY_SEVEN, ANY_CRAPS, HARD, YO, ACES, TWELVE }

	public static final class Bet {
		public final UUID player;
		public final String name;
		public final Kind kind;
		/** The point of a line or come bet once set, the number of a place or hardway bet; 0 otherwise. */
		public int number;
		public long amount;
		public long odds;

		Bet(UUID player, String name, Kind kind, int number, long amount) {
			this.player = player;
			this.name = name;
			this.kind = kind;
			this.number = number;
			this.amount = amount;
		}
	}

	public enum Phase { IDLE, BETTING, ROLLING }

	public Phase phase = Phase.IDLE;
	public int timer;
	public int betTicks = 15 * 20;
	public int point;
	public int die1;
	public int die2;
	public long rolls;
	public final List<Bet> bets = new ArrayList<>();
	public final List<UUID> players = new ArrayList<>();
	public final Map<UUID, String> names = new HashMap<>();
	public int shooter;
	public final List<Integer> history = new ArrayList<>();
	public final List<String> log = new ArrayList<>();
	/** Ticks since the betting window opened (the shooter may roll after a moment). */
	public int open;

	private final CasinoHouse house;
	private final Random random;

	public Craps(CasinoHouse house, Random random) {
		this.house = house;
		this.random = random;
	}

	// ------------------------------------------------------------------ odds

	/** Pass and come odds pay this (numerator, denominator) on a point. */
	public static long[] trueOdds(int number) {
		return switch (number) {
			case 4, 10 -> new long[] {2, 1};
			case 5, 9 -> new long[] {3, 2};
			default -> new long[] {6, 5};
		};
	}

	public static int maxOddsTimes(int number) {
		return switch (number) {
			case 4, 10 -> 3;
			case 5, 9 -> 4;
			default -> 5;
		};
	}

	static long[] placeOdds(int number) {
		return switch (number) {
			case 4, 10 -> new long[] {9, 5};
			case 5, 9 -> new long[] {7, 5};
			default -> new long[] {7, 6};
		};
	}

	private static boolean dont(Kind kind) {
		return kind == Kind.DONT_PASS || kind == Kind.DONT_COME;
	}

	private static boolean line(Kind kind) {
		return kind == Kind.PASS || kind == Kind.DONT_PASS || kind == Kind.COME || kind == Kind.DONT_COME;
	}

	private static long times(long amount, long[] odds) {
		return amount * odds[0] / odds[1];
	}

	/** What an odds bet behind a line bet wins. */
	static long oddsWin(Bet bet) {
		long[] o = trueOdds(bet.number);
		return dont(bet.kind) ? bet.odds * o[1] / o[0] : times(bet.odds, o);
	}

	/** The most a bet can win. */
	static long maxWin(Bet bet) {
		long win = switch (bet.kind) {
			case PASS, DONT_PASS, COME, DONT_COME -> bet.amount;
			case PLACE -> times(bet.amount, placeOdds(bet.number));
			case FIELD -> bet.amount * 3;
			case ANY_SEVEN -> bet.amount * 4;
			case ANY_CRAPS -> bet.amount * 7;
			case HARD -> bet.amount * (bet.number == 4 || bet.number == 10 ? 7 : 9);
			case YO -> bet.amount * 15;
			case ACES, TWELVE -> bet.amount * 30;
		};
		if (bet.odds > 0) {
			win += bet.number == 0 ? bet.odds * 2 : oddsWin(bet);
		}
		return win;
	}

	long exposure() {
		long total = 0;
		for (Bet bet : bets) {
			total += maxWin(bet);
		}
		return total;
	}

	// ------------------------------------------------------------------ betting

	/**
	 * Places a bet. {@code number}: the place or hardway number; for {@link #odds}, see there. Rules: pass and don't
	 * pass only on a come-out; come and don't come only while a point is on.
	 */
	public FinanceBook.Result bet(UUID player, String name, Kind kind, int number, long amount) {
		if (phase == Phase.ROLLING) {
			return FinanceBook.Result.fail("Wait: the dice are rolling.");
		}
		switch (kind) {
			case PASS, DONT_PASS -> {
				if (point != 0) {
					return FinanceBook.Result.fail("Line bets go down on the come-out roll; bet Come now.");
				}
				number = 0;
			}
			case COME, DONT_COME -> {
				if (point == 0) {
					return FinanceBook.Result.fail("Come bets wait for a point; bet the pass line now.");
				}
				number = 0;
			}
			case PLACE -> {
				if (number != 4 && number != 5 && number != 6 && number != 8 && number != 9 && number != 10) {
					return FinanceBook.Result.fail("Place bets go on 4, 5, 6, 8, 9 or 10.");
				}
			}
			case HARD -> {
				if (number != 4 && number != 6 && number != 8 && number != 10) {
					return FinanceBook.Result.fail("Hardways are 4, 6, 8 and 10.");
				}
			}
			default -> number = 0;
		}
		Bet same = null;
		int count = 0;
		long mine = 0;
		for (Bet bet : bets) {
			if (bet.player.equals(player)) {
				count++;
				mine += bet.amount + bet.odds;
				// Chips on a bet already there add to it (not to a line bet that has travelled to its point).
				if (bet.kind == kind && bet.number == number && (!line(kind) || bet.number == 0)) {
					same = bet;
				}
			}
		}
		if (same == null && count >= MAX_BETS_EACH) {
			return FinanceBook.Result.fail("That is as many bets as one player may have.");
		}
		long size = (same == null ? 0 : same.amount) + amount;
		if (amount <= 0 || size < house.minBet()) {
			return FinanceBook.Result.fail("The least bet here is " + FinanceBook.amount(house.minBet()) + ".");
		}
		if (size > house.maxBet()) {
			return FinanceBook.Result.fail("The most on one bet here is " + FinanceBook.amount(house.maxBet()) + ".");
		}
		Bet probe = new Bet(player, name, kind, number, amount);
		if (exposure() + maxWin(probe) > house.cover(player)) {
			return FinanceBook.Result.fail("The house cannot cover that bet.");
		}
		String failed = house.stake(player, amount);
		if (failed != null) {
			return FinanceBook.Result.fail(failed);
		}
		if (same != null) {
			same.amount += amount;
		} else {
			bets.add(probe);
		}
		join(player, name);
		openTable();
		return new FinanceBook.Result(true, "");
	}

	/** Takes odds behind a line or come bet that has a point ({@code index} into {@link #bets}). */
	public FinanceBook.Result odds(UUID player, int index, long amount) {
		if (phase == Phase.ROLLING) {
			return FinanceBook.Result.fail("Wait: the dice are rolling.");
		}
		if (index < 0 || index >= bets.size()) {
			return FinanceBook.Result.fail("That bet is gone.");
		}
		Bet bet = bets.get(index);
		if (!bet.player.equals(player) || !line(bet.kind) || bet.number == 0) {
			return FinanceBook.Result.fail("Odds go behind your own line or come bet once it has a point.");
		}
		long most = dont(bet.kind) ? bet.amount * 6 : bet.amount * maxOddsTimes(bet.number);
		if (amount <= 0 || bet.odds + amount > most) {
			return FinanceBook.Result.fail("Odds here may be up to " + FinanceBook.amount(most) + ".");
		}
		long before = maxWin(bet);
		bet.odds += amount;
		long more = maxWin(bet) - before;
		bet.odds -= amount;
		if (exposure() + more > house.cover(player)) {
			return FinanceBook.Result.fail("The house cannot cover those odds.");
		}
		String failed = house.stake(player, amount);
		if (failed != null) {
			return FinanceBook.Result.fail(failed);
		}
		bet.odds += amount;
		openTable();
		return new FinanceBook.Result(true, "");
	}

	/** Takes down a bet that may come down: place bets, hardways, odds, and line bets not yet on a point. */
	public FinanceBook.Result takeDown(UUID player, int index) {
		if (phase == Phase.ROLLING) {
			return FinanceBook.Result.fail("Wait: the dice are rolling.");
		}
		if (index < 0 || index >= bets.size() || !bets.get(index).player.equals(player)) {
			return FinanceBook.Result.fail("That bet is gone.");
		}
		Bet bet = bets.get(index);
		boolean contract = (bet.kind == Kind.PASS || bet.kind == Kind.COME) && bet.number != 0;
		if (contract) {
			if (bet.odds <= 0) {
				return FinanceBook.Result.fail("A line bet stays until it is decided; only its odds come down.");
			}
			house.refund(player, bet.odds);
			bet.odds = 0;
			return new FinanceBook.Result(true, "");
		}
		bets.remove(index);
		house.refund(player, bet.amount + bet.odds);
		return new FinanceBook.Result(true, "");
	}

	private void join(UUID player, String name) {
		if (!players.contains(player)) {
			players.add(player);
		}
		names.put(player, name);
	}

	private void openTable() {
		if (phase == Phase.IDLE) {
			phase = Phase.BETTING;
			timer = betTicks;
			open = 0;
		}
	}

	public UUID shooter() {
		return players.isEmpty() ? null : players.get(Math.floorMod(shooter, players.size()));
	}

	/** The shooter rolls (after a moment, so others can bet). */
	public FinanceBook.Result roll(UUID player) {
		if (phase != Phase.BETTING) {
			return FinanceBook.Result.fail("Bet first.");
		}
		if (!player.equals(shooter())) {
			return FinanceBook.Result.fail("Only the shooter rolls; the dice come round to you.");
		}
		if (open < SHOOTER_DELAY) {
			return FinanceBook.Result.fail("Give the table a moment to bet.");
		}
		throwDice();
		return new FinanceBook.Result(true, "");
	}

	private void throwDice() {
		phase = Phase.ROLLING;
		timer = ROLL_TICKS;
		die1 = random.nextInt(6) + 1;
		die2 = random.nextInt(6) + 1;
		rolls++;
	}

	public void tick() {
		switch (phase) {
			case IDLE -> {
			}
			case BETTING -> {
				open++;
				if (bets.isEmpty()) {
					phase = Phase.IDLE;
				} else if (--timer <= 0) {
					throwDice();
				}
			}
			case ROLLING -> {
				if (--timer <= 0) {
					resolve(die1, die2);
					phase = bets.isEmpty() ? Phase.IDLE : Phase.BETTING;
					timer = betTicks;
					open = 0;
				}
			}
		}
	}

	// ------------------------------------------------------------------ the roll

	/** Decides every bet the dice decide. Package-visible so tests can roll chosen dice. */
	void resolve(int a, int b) {
		int sum = a + b;
		boolean hard = a == b;
		boolean comeOut = point == 0;
		String shooterName = names.getOrDefault(shooter(), "The shooter");
		history.add(sum);
		while (history.size() > 14) {
			history.remove(0);
		}
		Map<UUID, Long> net = new HashMap<>();
		List<Bet> keep = new ArrayList<>();
		for (Bet bet : bets) {
			long payout = -1;  // -1: the bet stays
			switch (bet.kind) {
				case FIELD -> payout = switch (sum) {
					case 2 -> bet.amount * 3;
					case 12 -> bet.amount * 4;
					case 3, 4, 9, 10, 11 -> bet.amount * 2;
					default -> 0;
				};
				case ANY_SEVEN -> payout = sum == 7 ? bet.amount * 5 : 0;
				case ANY_CRAPS -> payout = sum == 2 || sum == 3 || sum == 12 ? bet.amount * 8 : 0;
				case YO -> payout = sum == 11 ? bet.amount * 16 : 0;
				case ACES -> payout = sum == 2 ? bet.amount * 31 : 0;
				case TWELVE -> payout = sum == 12 ? bet.amount * 31 : 0;
				case HARD -> {
					if (!comeOut) {
						if (sum == bet.number && hard) {
							payout = bet.amount + bet.amount * (bet.number == 4 || bet.number == 10 ? 7 : 9);
						} else if (sum == 7 || sum == bet.number) {
							payout = 0;
						}
					}
				}
				case PLACE -> {
					if (!comeOut) {
						if (sum == bet.number) {
							// A place bet stays up: it collects its winnings only.
							long win = times(bet.amount, placeOdds(bet.number));
							house.settle(bet.player, bet.name, 0, win, null);
							net.merge(bet.player, win, Long::sum);
						} else if (sum == 7) {
							payout = 0;
						}
					}
				}
				case PASS, DONT_PASS, COME, DONT_COME -> payout = lineBet(bet, sum, comeOut);
			}
			if (payout < 0) {
				keep.add(bet);
				continue;
			}
			long stake = bet.amount + bet.odds;
			house.settle(bet.player, bet.name, stake, payout, payout - stake >= 500 ? bet.name + " won "
					+ FinanceBook.amount(payout - stake) + " Jugs at craps." : null);
			net.merge(bet.player, payout - stake, Long::sum);
		}
		bets.clear();
		bets.addAll(keep);
		// The point.
		String what = a + " and " + b + ": " + sum + (hard && sum != 2 && sum != 12 ? " the hard way" : "");
		if (comeOut) {
			if (sum == 4 || sum == 5 || sum == 6 || sum == 8 || sum == 9 || sum == 10) {
				point = sum;
				note(shooterName + " rolls " + what + ". The point is " + sum + ".");
			} else {
				note(shooterName + " rolls " + what + (sum == 7 || sum == 11 ? ", a winner on the line." : ", craps."));
			}
		} else if (sum == point) {
			note(shooterName + " rolls " + what + ": the point is made!");
			point = 0;
		} else if (sum == 7) {
			note(shooterName + " sevens out. The dice pass.");
			point = 0;
			shooter++;
		} else {
			note(shooterName + " rolls " + what + ".");
		}
		for (Map.Entry<UUID, Long> entry : net.entrySet()) {
			if (entry.getValue() != 0) {
				note(names.getOrDefault(entry.getKey(), "?") + (entry.getValue() > 0 ? " wins " : " loses ")
						+ FinanceBook.amount(Math.abs(entry.getValue())) + ".");
			}
		}
		// Players with nothing left on the table leave the rail.
		players.removeIf(id -> {
			for (Bet bet : bets) {
				if (bet.player.equals(id)) {
					return false;
				}
			}
			return !id.equals(shooter());
		});
	}

	/** What a line or come bet pays on this roll (stake included), or -1 if it stays. */
	private long lineBet(Bet bet, int sum, boolean comeOut) {
		boolean isLine = bet.kind == Kind.PASS || bet.kind == Kind.DONT_PASS;
		boolean dont = dont(bet.kind);
		if (bet.number == 0) {
			// Its own come-out: pass/come win on 7 or 11; don't on 2 or 3, 12 a push.
			if (isLine && !comeOut) {
				return -1;
			}
			if (sum == 7 || sum == 11) {
				return dont ? 0 : bet.amount * 2;
			}
			if (sum == 2 || sum == 3) {
				return dont ? bet.amount * 2 : 0;
			}
			if (sum == 12) {
				return dont ? bet.amount : 0;
			}
			bet.number = sum;
			return -1;
		}
		// Come bet odds are off on the come-out roll: they come back as they were.
		boolean oddsOff = comeOut && !isLine && !dont;
		if (sum == bet.number) {
			if (dont) {
				return 0;
			}
			return bet.amount * 2 + (oddsOff ? bet.odds : bet.odds + oddsWin(bet));
		}
		if (sum == 7) {
			if (dont) {
				return bet.amount * 2 + bet.odds + oddsWin(bet);
			}
			return oddsOff ? bet.odds : 0;
		}
		return -1;
	}

	public void away(UUID player) {
		if (phase == Phase.ROLLING) {
			return;
		}
		// Bets that may come down come back; contract bets stay until decided (the table settles them as normal).
		boolean left = false;
		for (int i = bets.size() - 1; i >= 0; i--) {
			if (bets.get(i).player.equals(player)) {
				takeDown(player, i);
			}
		}
		for (Bet bet : bets) {
			left |= bet.player.equals(player);
		}
		if (!left) {
			// Off the rail; if they were the shooter, the dice pass on.
			int index = players.indexOf(player);
			if (index >= 0) {
				int current = Math.floorMod(shooter, players.size());
				players.remove(index);
				// The same shooter keeps the dice; if it was this player, the next one along has them.
				shooter = index < current ? current - 1 : current;
			}
		}
	}

	public void cancelAll() {
		for (Bet bet : bets) {
			house.refund(bet.player, bet.amount + bet.odds);
		}
		bets.clear();
		players.clear();
		point = 0;
		phase = Phase.IDLE;
	}

	public Map<UUID, Long> outstanding() {
		Map<UUID, Long> out = new HashMap<>();
		for (Bet bet : bets) {
			out.merge(bet.player, bet.amount + bet.odds, Long::sum);
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
