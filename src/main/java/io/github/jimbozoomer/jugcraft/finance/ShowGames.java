package io.github.jimbozoomer.jugcraft.finance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * The casino's showpiece games, plain Java: Plinko, the Big Six prize wheel, Baccarat and the lottery with its scratch
 * cards. (The coin pusher is {@link CoinPusher}.) Each takes stakes through a {@link CasinoHouse}.
 */
public final class ShowGames {
	private ShowGames() {
	}

	private static void trim(List<String> log) {
		while (log.size() > 8) {
			log.remove(0);
		}
	}

	// ================================================================== Plinko

	/**
	 * Plinko: a ball drops through twelve rows of pegs, bouncing left or right at each, into one of thirteen slots. The
	 * slots pay times the bet (in tenths) by the risk chosen; all three tables return about 99% (exactly 98.98%,
	 * 98.99% and 99.12%), before payouts round down to whole Jugs (bets of 10 or more lose almost nothing to that).
	 */
	public static final class Plinko {
		public static final int ROWS = 12;
		public static final String[] RISKS = {"Low", "Medium", "High"};
		public static final int[][] TENTHS = {
				{100, 30, 16, 14, 11, 10, 5, 10, 11, 14, 16, 30, 100},
				{330, 110, 40, 20, 11, 6, 3, 6, 11, 20, 40, 110, 330},
				{1_700, 240, 81, 20, 7, 2, 2, 2, 7, 20, 81, 240, 1_700}};
		public static final int DROP_TICKS = 40;

		public UUID player;
		public String playerName = "";
		public int risk = 1;
		/** The last drop: its path (bit i: right at row i), slot, bet and win. */
		public int path;
		public int slot = -1;
		public long bet;
		public long win;
		public long drops;
		public final List<String> log = new ArrayList<>();
		/** The latest drops, oldest first, so several balls can be in the air at once: {drop number, path, slot, risk, bet, win}. */
		public final List<long[]> recent = new ArrayList<>();
		public static final int RECENT = 12;
		/** The risks the owner lets players pick: bit 0 Low, 1 Medium, 2 High. */
		public int allowedRisks = 7;
		private int cooldown;
		private int idle;
		private final CasinoHouse house;
		private final Random random;

		public Plinko(CasinoHouse house, Random random) {
			this.house = house;
			this.random = random;
		}

		public static double returnFor(int risk) {
			double total = 0;
			for (int k = 0; k <= ROWS; k++) {
				double p = 1;
				for (int i = 0; i < k; i++) {
					p = p * (ROWS - i) / (i + 1);
				}
				total += p / 4096.0 * TENTHS[risk][k] / 10.0;
			}
			return total;
		}

		public FinanceBook.Result drop(UUID id, String name, long amount, int chosenRisk) {
			if (player != null && !player.equals(id)) {
				return FinanceBook.Result.fail(playerName + " is playing this board.");
			}
			if (cooldown > 0) {
				return FinanceBook.Result.fail("");
			}
			if (chosenRisk < 0 || chosenRisk > 2) {
				return FinanceBook.Result.fail("Pick a risk.");
			}
			if ((allowedRisks & (1 << chosenRisk)) == 0) {
				return FinanceBook.Result.fail("The owner has closed " + RISKS[chosenRisk] + " risk on this board.");
			}
			if (amount < house.minBet() || amount > house.maxBet()) {
				return FinanceBook.Result.fail("Bets here are " + FinanceBook.amount(house.minBet()) + " to "
						+ FinanceBook.amount(house.maxBet()) + ".");
			}
			if (amount * TENTHS[chosenRisk][0] / 10 > house.cover(id)) {
				return FinanceBook.Result.fail("The house cannot cover the top prize at that bet.");
			}
			String failed = house.stake(id, amount);
			if (failed != null) {
				return FinanceBook.Result.fail(failed);
			}
			player = id;
			playerName = name;
			idle = 0;
			risk = chosenRisk;
			path = 0;
			int k = 0;
			for (int i = 0; i < ROWS; i++) {
				if (random.nextBoolean()) {
					path |= 1 << i;
					k++;
				}
			}
			slot = k;
			bet = amount;
			win = amount * TENTHS[risk][k] / 10;
			drops++;
			recent.add(new long[] {drops, path, slot, risk, amount, win});
			while (recent.size() > RECENT) {
				recent.remove(0);
			}
			cooldown = 4;
			String big = TENTHS[risk][k] >= 1_000 && win >= 500 ? name + " hit " + TENTHS[risk][k] / 10 + "x at Plinko for "
					+ FinanceBook.amount(win) + " Jugs." : null;
			house.settle(id, name, amount, win, big);
			log.add(String.format(java.util.Locale.ROOT, "%.1fx: %s", TENTHS[risk][k] / 10.0, FinanceBook.amount(win)));
			trim(log);
			return new FinanceBook.Result(true, "");
		}

		public void tick() {
			if (cooldown > 0) {
				cooldown--;
			}
			if (player != null && ++idle > 20 * 60) {
				player = null;
				playerName = "";
			}
		}

		public void release(UUID id) {
			if (id.equals(player)) {
				player = null;
				playerName = "";
			}
		}
	}

	// ================================================================== Big Six

	/**
	 * The Big Six wheel (a casino's Wheel of Fortune): 54 segments. Bets go on a symbol, which pays its number to 1: 1
	 * (24 segments, house edge 11.1%), 2 (15, 16.7%), 5 (7, 22.2%), 10 (4, 18.5%), 20 (2, 22.2%), and the Joker and the
	 * Jug logo (one each, 40 to 1, 24.1%), as at a casino. Bets close when the clock runs out, then the wheel spins.
	 */
	public static final class BigSix {
		public static final int[] PAYS = {1, 2, 5, 10, 20, 40, 40};
		public static final String[] NAMES = {"1", "2", "5", "10", "20", "Joker", "Jug"};
		public static final int[] COUNTS = {24, 15, 7, 4, 2, 1, 1};
		/** The wheel's 54 segments in order (symbol indices), spread round like a real wheel. */
		public static final int[] WHEEL = new int[54];
		public static final int SPIN_TICKS = 140;

		static {
			// Highest first, spaced out, then the ones fill the gaps.
			java.util.Arrays.fill(WHEEL, -1);
			int[] order = {5, 6, 4, 3, 2, 1, 0};
			int offset = 0;
			for (int symbol : order) {
				List<Integer> free = new ArrayList<>();
				for (int i = 0; i < 54; i++) {
					if (WHEEL[i] < 0) {
						free.add(i);
					}
				}
				for (int k = 0; k < COUNTS[symbol]; k++) {
					WHEEL[free.get((offset + k * free.size() / COUNTS[symbol]) % free.size())] = symbol;
				}
				offset += 2;
			}
		}

		public record Bet(UUID player, String name, int symbol, long amount) {
		}

		public enum Phase { IDLE, BETTING, SPINNING, RESULT }

		public Phase phase = Phase.IDLE;
		public int timer;
		public int betTicks = 20 * 20;
		public int result = -1;
		public long spins;
		public final List<Bet> bets = new ArrayList<>();
		public final List<Integer> history = new ArrayList<>();
		public final List<String> log = new ArrayList<>();
		private final CasinoHouse house;
		private final Random random;

		public BigSix(CasinoHouse house, Random random) {
			this.house = house;
			this.random = random;
		}

		/** The house's edge on a symbol. */
		public static double edge(int symbol) {
			return 1 - COUNTS[symbol] * (PAYS[symbol] + 1) / 54.0;
		}

		private long worstCase(int symbol, long amount) {
			long worst = 0;
			for (int s = 0; s < PAYS.length; s++) {
				long owed = s == symbol ? amount * PAYS[s] : 0;
				for (Bet bet : bets) {
					if (bet.symbol() == s) {
						owed += bet.amount() * PAYS[s];
					}
				}
				worst = Math.max(worst, owed);
			}
			return worst;
		}

		public FinanceBook.Result bet(UUID id, String name, int symbol, long amount) {
			if (phase == Phase.SPINNING) {
				return FinanceBook.Result.fail("No more bets: the wheel is turning.");
			}
			if (symbol < 0 || symbol >= PAYS.length) {
				return FinanceBook.Result.fail("Pick a symbol.");
			}
			long mine = 0;
			for (Bet bet : bets) {
				if (bet.player().equals(id)) {
					mine += bet.amount();
				}
			}
			if (amount < house.minBet() || mine + amount > house.maxBet()) {
				return FinanceBook.Result.fail("Bets here are " + FinanceBook.amount(house.minBet()) + " to "
						+ FinanceBook.amount(house.maxBet()) + " a spin.");
			}
			if (worstCase(symbol, amount) > house.cover(id)) {
				return FinanceBook.Result.fail("The house cannot cover that bet.");
			}
			String failed = house.stake(id, amount);
			if (failed != null) {
				return FinanceBook.Result.fail(failed);
			}
			if (phase != Phase.BETTING) {
				phase = Phase.BETTING;
				timer = betTicks;
			}
			for (int i = 0; i < bets.size(); i++) {
				Bet bet = bets.get(i);
				if (bet.player().equals(id) && bet.symbol() == symbol) {
					bets.set(i, new Bet(id, name, symbol, bet.amount() + amount));
					return new FinanceBook.Result(true, "");
				}
			}
			bets.add(new Bet(id, name, symbol, amount));
			return new FinanceBook.Result(true, "");
		}

		public FinanceBook.Result clear(UUID id) {
			if (phase == Phase.SPINNING) {
				return FinanceBook.Result.fail("No more bets: the wheel is turning.");
			}
			for (int i = bets.size() - 1; i >= 0; i--) {
				if (bets.get(i).player().equals(id)) {
					house.refund(id, bets.remove(i).amount());
				}
			}
			return new FinanceBook.Result(true, "");
		}

		public FinanceBook.Result spinNow() {
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
						result = random.nextInt(54);
						spins++;
					}
				}
				case SPINNING -> {
					if (--timer <= 0) {
						pay();
						phase = Phase.RESULT;
						timer = 80;
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
			int symbol = WHEEL[result];
			log.add("The wheel stops on " + NAMES[symbol] + ".");
			history.add(symbol);
			while (history.size() > 12) {
				history.remove(0);
			}
			for (Bet bet : bets) {
				long payout = bet.symbol() == symbol ? bet.amount() * (PAYS[symbol] + 1) : 0;
				house.settle(bet.player(), bet.name(), bet.amount(), payout, payout >= 1_000 && PAYS[symbol] >= 20
						? bet.name() + " won " + FinanceBook.amount(payout - bet.amount()) + " Jugs on the prize wheel." : null);
				if (payout > 0) {
					log.add(bet.name() + " wins " + FinanceBook.amount(payout - bet.amount()) + ".");
				}
			}
			trim(log);
			bets.clear();
		}

		public void away(UUID id) {
			if (phase != Phase.SPINNING) {
				clear(id);
			}
		}

		public void cancelAll() {
			for (Bet bet : bets) {
				house.refund(bet.player(), bet.amount());
			}
			bets.clear();
			phase = Phase.IDLE;
		}

		public Map<UUID, Long> outstanding() {
			Map<UUID, Long> out = new HashMap<>();
			for (Bet bet : bets) {
				out.merge(bet.player(), bet.amount(), Long::sum);
			}
			return out;
		}
	}

	// ================================================================== Baccarat

	/**
	 * Punto banco: eight decks; Player and Banker each get two cards, a third by the fixed tableau; nearest to 9 wins.
	 * Player pays 1 to 1 (house edge 1.24%), Banker 0.95 to 1 after the 5% commission, rounded down (1.06%), Tie 8 to 1
	 * (14.4%; Player and Banker bets push on a tie). Bets close when the clock runs out.
	 */
	public static final class Baccarat {
		public static final int PLAYER = 0;
		public static final int BANKER = 1;
		public static final int TIE = 2;
		public static final String[] SIDES = {"Player", "Banker", "Tie"};
		public static final int DEAL_TICKS = 100;

		public record Bet(UUID player, String name, int side, long amount) {
		}

		public enum Phase { IDLE, BETTING, DEALING, RESULT }

		public Phase phase = Phase.IDLE;
		public int timer;
		public int betTicks = 20 * 20;
		public final List<Integer> playerCards = new ArrayList<>();
		public final List<Integer> bankerCards = new ArrayList<>();
		public int winner = -1;
		public long rounds;
		public final List<Bet> bets = new ArrayList<>();
		public final List<Integer> history = new ArrayList<>();
		public final List<String> log = new ArrayList<>();
		private final CasinoHouse house;
		private final Random random;
		private int[] shoe = new int[0];
		private int next;

		public Baccarat(CasinoHouse house, Random random) {
			this.house = house;
			this.random = random;
		}

		public static int value(int card) {
			int rank = card % 13;
			if (rank == 12) {
				return 1;
			}
			return rank >= 8 ? 0 : rank + 2;
		}

		public static int total(List<Integer> cards) {
			int sum = 0;
			for (int c : cards) {
				sum += value(c);
			}
			return sum % 10;
		}

		private int draw() {
			if (next >= shoe.length - 6) {
				shoe = new int[52 * 8];
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
			}
			return shoe[next++];
		}

		/** Deals a whole coup by the tableau; returns the winner (Player, Banker or Tie). Package-visible for tests. */
		int coup() {
			playerCards.clear();
			bankerCards.clear();
			playerCards.add(draw());
			bankerCards.add(draw());
			playerCards.add(draw());
			bankerCards.add(draw());
			int p = total(playerCards);
			int b = total(bankerCards);
			if (p < 8 && b < 8) {
				int third = -1;
				if (p <= 5) {
					third = draw();
					playerCards.add(third);
				}
				boolean bankerDraws;
				if (third < 0) {
					bankerDraws = b <= 5;
				} else {
					int v = value(third);
					bankerDraws = switch (b) {
						case 0, 1, 2 -> true;
						case 3 -> v != 8;
						case 4 -> v >= 2 && v <= 7;
						case 5 -> v >= 4 && v <= 7;
						case 6 -> v == 6 || v == 7;
						default -> false;
					};
				}
				if (bankerDraws) {
					bankerCards.add(draw());
				}
			}
			p = total(playerCards);
			b = total(bankerCards);
			return p > b ? PLAYER : b > p ? BANKER : TIE;
		}

		/** What a bet returns (stake included) when a coup ends with this winner. */
		public static long payout(int side, long amount, int winner) {
			if (winner == TIE) {
				return side == TIE ? amount * 9 : amount;
			}
			if (side != winner) {
				return 0;
			}
			return side == BANKER ? amount + amount * 95 / 100 : amount * 2;
		}

		public FinanceBook.Result bet(UUID id, String name, int side, long amount) {
			if (phase == Phase.DEALING) {
				return FinanceBook.Result.fail("No more bets: the cards are out.");
			}
			if (side < 0 || side > 2) {
				return FinanceBook.Result.fail("Bet Player, Banker or Tie.");
			}
			long mine = 0;
			long[] onSide = new long[3];
			for (Bet bet : bets) {
				if (bet.player().equals(id)) {
					mine += bet.amount();
				}
				onSide[bet.side()] += bet.amount();
			}
			if (amount < house.minBet() || mine + amount > house.maxBet()) {
				return FinanceBook.Result.fail("Bets here are " + FinanceBook.amount(house.minBet()) + " to "
						+ FinanceBook.amount(house.maxBet()) + " a hand.");
			}
			onSide[side] += amount;
			long worst = Math.max(Math.max(onSide[PLAYER], onSide[BANKER]), onSide[TIE] * 8);
			if (worst > house.cover(id)) {
				return FinanceBook.Result.fail("The house cannot cover that bet.");
			}
			String failed = house.stake(id, amount);
			if (failed != null) {
				return FinanceBook.Result.fail(failed);
			}
			if (phase != Phase.BETTING) {
				phase = Phase.BETTING;
				timer = betTicks;
			}
			for (int i = 0; i < bets.size(); i++) {
				Bet bet = bets.get(i);
				if (bet.player().equals(id) && bet.side() == side) {
					bets.set(i, new Bet(id, name, side, bet.amount() + amount));
					return new FinanceBook.Result(true, "");
				}
			}
			bets.add(new Bet(id, name, side, amount));
			return new FinanceBook.Result(true, "");
		}

		public FinanceBook.Result clear(UUID id) {
			if (phase == Phase.DEALING) {
				return FinanceBook.Result.fail("No more bets: the cards are out.");
			}
			for (int i = bets.size() - 1; i >= 0; i--) {
				if (bets.get(i).player().equals(id)) {
					house.refund(id, bets.remove(i).amount());
				}
			}
			return new FinanceBook.Result(true, "");
		}

		public FinanceBook.Result dealNow() {
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
						winner = coup();
						rounds++;
						phase = Phase.DEALING;
						timer = DEAL_TICKS;
					}
				}
				case DEALING -> {
					if (--timer <= 0) {
						settle();
						phase = Phase.RESULT;
						timer = 80;
					}
				}
				case RESULT -> {
					if (--timer <= 0) {
						phase = bets.isEmpty() ? Phase.IDLE : Phase.BETTING;
						timer = betTicks;
						if (phase == Phase.IDLE) {
							playerCards.clear();
							bankerCards.clear();
						}
					}
				}
			}
		}

		private void settle() {
			log.add(SIDES[winner] + (winner == TIE ? "" : " wins") + ", " + total(playerCards) + " to " + total(bankerCards) + ".");
			history.add(winner);
			while (history.size() > 16) {
				history.remove(0);
			}
			for (Bet bet : bets) {
				long payout = payout(bet.side(), bet.amount(), winner);
				house.settle(bet.player(), bet.name(), bet.amount(), payout, null);
			}
			trim(log);
			bets.clear();
		}

		public void away(UUID id) {
			if (phase != Phase.DEALING) {
				clear(id);
			}
		}

		public void cancelAll() {
			for (Bet bet : bets) {
				house.refund(bet.player(), bet.amount());
			}
			bets.clear();
			phase = Phase.IDLE;
		}

		public Map<UUID, Long> outstanding() {
			Map<UUID, Long> out = new HashMap<>();
			for (Bet bet : bets) {
				out.merge(bet.player(), bet.amount(), Long::sum);
			}
			return out;
		}
	}

	// ================================================================== scratch cards

	/**
	 * Scratch cards, sold at a lottery kiosk in three kinds. A card's prize is drawn when it is scratched, from fixed
	 * odds out of 100,000. When a card is sold, the share of its price that prizes return on average goes to the
	 * central bank (which pays the prizes) and the rest to the kiosk's house.
	 */
	public enum ScratchCard {
		COPPER("Copper Scratch Card", 1, new long[] {1, 22_222, 2, 10_000, 5, 2_500, 20, 400, 100, 25}),
		IRON("Iron Scratch Card", 5, new long[] {5, 20_000, 10, 8_000, 25, 2_400, 100, 600, 1_000, 30}),
		GOLD("Gold Scratch Card", 25, new long[] {25, 18_000, 50, 8_000, 100, 3_500, 500, 400, 1_000, 100, 5_000, 30});

		public final String title;
		public final long price;
		/** {prize, chance out of 100,000, ...}. */
		public final long[] prizes;

		ScratchCard(String title, long price, long[] prizes) {
			this.title = title;
			this.price = price;
			this.prizes = prizes;
		}

		public String id() {
			return "scratch_card_" + name().toLowerCase(java.util.Locale.ROOT);
		}

		/** The average prize as a share of the price. */
		public double returnToPlayer() {
			double total = 0;
			for (int i = 0; i < prizes.length; i += 2) {
				total += prizes[i] * (double) prizes[i + 1];
			}
			return total / 100_000.0 / price;
		}

		/** The part of the price that goes to the bank to fund prizes (rounded up, so the bank never loses on average). */
		public long bankShare() {
			return Math.min(price, (long) Math.ceil(price * returnToPlayer()));
		}

		public long draw(Random random) {
			int roll = random.nextInt(100_000);
			for (int i = 0; i < prizes.length; i += 2) {
				roll -= (int) prizes[i + 1];
				if (roll < 0) {
					return prizes[i];
				}
			}
			return 0;
		}
	}

	// ================================================================== lottery

	/**
	 * A kiosk's lottery: tickets at the kiosk's price; nine tenths of every ticket goes into the pot (held by the
	 * Exchange) and a tenth to the house. Every so many days one ticket is drawn and its holder wins the whole pot; with
	 * no tickets, the pot waits for the next draw.
	 */
	public static final class Lottery {
		public static final int MAX_TICKETS_EACH = 100;

		public final List<UUID> tickets = new ArrayList<>();
		/** What each ticket put in the pot (given back if the kiosk is broken before the draw). */
		public final List<Long> paid = new ArrayList<>();
		public final Map<UUID, String> names = new HashMap<>();
		public long pot;
		public long lastDraw = -1;
		public String lastWinner = "";
		public long lastPrize;
		public final List<String> log = new ArrayList<>();

		public int ticketsOf(UUID id) {
			int count = 0;
			for (UUID t : tickets) {
				if (t.equals(id)) {
					count++;
				}
			}
			return count;
		}
	}
}
