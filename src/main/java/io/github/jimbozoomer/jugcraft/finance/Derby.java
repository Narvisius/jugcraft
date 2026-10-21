package io.github.jimbozoomer.jugcraft.finance;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.SplittableRandom;
import java.util.UUID;

/**
 * The Derby Table: six miniature horses race once round an oval under a glass dome, and players bet on them through
 * tote pools, as at a real racetrack. Plain Java, shared by the server (which runs the races and settles the pools) and
 * the client (which draws the race from the same seed and finishing order).
 *
 * <ul>
 * <li>Each race has a field of six named horses with ratings and a form line. The finishing order is drawn when the
 * race starts, horse by horse in proportion to each one's strength (so a horse's chance to win is its strength over
 * the field's).</li>
 * <li>Three pools: Win (first), Place (first or second) and Show (top three). The house takes its cut of each pool
 * ({@link #cutPercent}, set by the owner) and the rest is shared among the winning tickets in proportion to their stakes.
 * Place and show pay each placed horse an equal share of the profit, as tracks do. No winning ticket ever gets back less
 * than its stake, and none more than {@link #MAX_MULTIPLE} times it.</li>
 * <li>So that odds are sensible however few people bet, the house backs every pool with a large stake (200 of the
 * table's most bets) spread over the horses in proportion to their true chances. Those stakes are fair bets, so the
 * prices sit close to fair less the cut and move as players bet, like a bookmaker's; the house's edge is its cut, and
 * like a bookmaker it carries the risk of each race (a bet is only taken if the house can pay it at the current
 * price).</li>
 * </ul>
 */
public final class Derby {
	public static final int HORSES = 6;
	public static final String[] POOLS = {"Win", "Place", "Show"};
	public static final int WIN = 0;
	public static final int PLACE = 1;
	public static final int SHOW = 2;
	/** How long a race runs, start to the last horse home, and how long the result stands before the next card. */
	public static final int RACE_TICKS = 20 * 22;
	public static final int RESULT_TICKS = 20 * 10;
	public static final int MAX_MULTIPLE = 200;
	/** The jockeys' silks, by lane: red, blue, yellow, green, purple and silver. */
	public static final int[] SILKS = {0xFFC0362C, 0xFF3A8EE0, 0xFFFFD840, 0xFF3AA04A, 0xFF9A4AC0, 0xFFD0D4DA};
	public static final String[] NAMES = {"Clover Dash", "Midnight Jug", "Copper Kettle", "Redstone Rush", "Emerald Arrow",
			"Lucky Pickaxe", "Creeper's Shadow", "Thunder Bucket", "Golden Carrot", "Diamond Hoof", "Ender Pearl", "Nether Star",
			"Blaze Runner", "Cobble King", "Lapis Lass", "Saddle Up", "Hay Bale Harry", "Sugar Cane", "Pumpkin Spice",
			"Moon Shard", "Iron Will", "Bamboo Boy", "Frost Walker", "Coral Queen", "Lantern Light", "Amethyst Echo",
			"Glow Berry", "Piston Pete", "Minecart Mike", "Torchbearer", "Beacon Bright", "Warped Wonder"};

	public enum Phase { IDLE, BETTING, RACING, RESULT }

	public record Bet(UUID player, String name, int pool, int horse, long amount) {
	}

	private final CasinoHouse house;
	private final Random random;
	public Phase phase = Phase.IDLE;
	public int timer;
	public int betTicks = 20 * 30;
	public int cutPercent = 15;
	/** The race card: each horse's name (an index into {@link #NAMES}), rating and recent finishes (1-6, latest last). */
	public final int[] names = new int[HORSES];
	public final int[] ratings = new int[HORSES];
	public final String[] form = new String[HORSES];
	public long raceNumber;
	/** The race's seed (the pace of every horse) and finishing order (horse indices, winner first); set at the start. */
	public long seed;
	public final int[] order = new int[HORSES];
	/** The server's game time when the race started, so every client draws the same moment of it. */
	public long startTime;
	public final List<Bet> bets = new ArrayList<>();
	public final List<Integer> history = new ArrayList<>();
	public final List<String> log = new ArrayList<>();

	public Derby(CasinoHouse house, Random random) {
		this.house = house;
		this.random = random;
		newCard();
	}

	// ------------------------------------------------------------------ the card

	/** A fresh field: six horses with ratings from 60 to 100 and a form line to match. */
	public void newCard() {
		List<Integer> pool = new ArrayList<>();
		for (int i = 0; i < NAMES.length; i++) {
			pool.add(i);
		}
		for (int h = 0; h < HORSES; h++) {
			names[h] = pool.remove(random.nextInt(pool.size()));
			ratings[h] = 60 + random.nextInt(41);
		}
		double[] p = winChances(ratings);
		for (int h = 0; h < HORSES; h++) {
			StringBuilder line = new StringBuilder();
			for (int k = 0; k < 5; k++) {
				// Better horses tend to have finished higher.
				double roll = random.nextDouble();
				int place = 1 + (int) Math.min(5, Math.floor(roll * (1 - p[h]) * 7));
				line.append(place);
			}
			form[h] = line.toString();
		}
		for (int h = 0; h < HORSES; h++) {
			order[h] = h;
		}
	}

	/** A horse's strength from its rating: every 8 points of rating doubles it, roughly. */
	public static double strength(int rating) {
		return Math.exp((rating - 80) / 11.5);
	}

	public static double[] winChances(int[] ratings) {
		double total = 0;
		double[] w = new double[ratings.length];
		for (int i = 0; i < ratings.length; i++) {
			w[i] = strength(ratings[i]);
			total += w[i];
		}
		for (int i = 0; i < w.length; i++) {
			w[i] /= total;
		}
		return w;
	}

	/**
	 * Each horse's chance to finish first, in the top two and in the top three: exactly, by going through every possible
	 * first three, each weighed as the draw weighs it.
	 */
	public static double[][] chances(int[] ratings) {
		int n = ratings.length;
		double[] w = new double[n];
		double total = 0;
		for (int i = 0; i < n; i++) {
			w[i] = strength(ratings[i]);
			total += w[i];
		}
		double[][] out = new double[3][n];
		for (int a = 0; a < n; a++) {
			double pa = w[a] / total;
			out[0][a] += pa;
			for (int b = 0; b < n; b++) {
				if (b == a) {
					continue;
				}
				double pb = pa * w[b] / (total - w[a]);
				for (int c = 0; c < n; c++) {
					if (c == a || c == b) {
						continue;
					}
					double pc = pb * w[c] / (total - w[a] - w[b]);
					out[1][a] += pc;
					out[1][b] += pc;
					out[2][a] += pc;
					out[2][b] += pc;
					out[2][c] += pc;
				}
			}
		}
		return out;
	}

	// ------------------------------------------------------------------ pools

	/** What the house backs each pool with in all: 200 of the table's most bets (at least 60 least bets). */
	public long seedTotal() {
		return Math.max(Math.max(60, 60 * house.minBet()), 200 * house.maxBet());
	}

	/** Every stake on each horse in a pool, the house's seed included (fractional seeds kept exact as doubles). */
	public double[] poolStakes(int pool) {
		double[][] chance = chances(ratings);
		double[] out = new double[HORSES];
		double seed = seedTotal();
		for (int h = 0; h < HORSES; h++) {
			out[h] = seed * chance[pool][h] / (pool + 1);
		}
		for (Bet bet : bets) {
			if (bet.pool() == pool) {
				out[bet.horse()] += bet.amount();
			}
		}
		return out;
	}

	/** What a one-Jug ticket on this horse in this pool would pay back if it came in now (stake included): the live odds. */
	public double payoutPerJug(int pool, int horse) {
		double[] stakes = poolStakes(pool);
		double total = 0;
		for (double s : stakes) {
			total += s;
		}
		double net = total * (100 - cutPercent) / 100.0;
		if (pool == WIN) {
			return clamp(net / Math.max(1e-9, stakes[horse]));
		}
		// For place and show, assume the other placed horses are the likeliest ones: an estimate, as at a track.
		int places = pool + 1;
		double[] chance = chances(ratings)[pool];
		double others = 0;
		boolean[] used = new boolean[HORSES];
		used[horse] = true;
		for (int k = 1; k < places; k++) {
			int best = -1;
			for (int h = 0; h < HORSES; h++) {
				if (!used[h] && (best < 0 || chance[h] > chance[best])) {
					best = h;
				}
			}
			used[best] = true;
			others += stakes[best];
		}
		double profit = Math.max(0, net - stakes[horse] - others) / places;
		return clamp(1 + profit / Math.max(1e-9, stakes[horse]));
	}

	private static double clamp(double multiple) {
		return Math.max(1.0, Math.min(MAX_MULTIPLE, multiple));
	}

	// ------------------------------------------------------------------ betting

	public FinanceBook.Result bet(UUID id, String name, int pool, int horse, long amount) {
		if (phase == Phase.RACING) {
			return FinanceBook.Result.fail("They're off: bets are closed until the next race.");
		}
		if (pool < 0 || pool > 2 || horse < 0 || horse >= HORSES) {
			return FinanceBook.Result.fail("Pick a horse and a pool.");
		}
		if (amount < house.minBet() || amount > house.maxBet()) {
			return FinanceBook.Result.fail("Bets here are " + FinanceBook.amount(house.minBet()) + " to "
					+ FinanceBook.amount(house.maxBet()) + ".");
		}
		if (house.cover(id) < (long) Math.ceil(amount * payoutPerJug(pool, horse))) {
			return FinanceBook.Result.fail("The house cannot cover that bet at its price.");
		}
		if (phase == Phase.RESULT) {
			// The next card goes up as soon as anyone bets.
			nextRace();
		}
		String failed = house.stake(id, amount);
		if (failed != null) {
			return FinanceBook.Result.fail(failed);
		}
		bets.add(new Bet(id, name, pool, horse, amount));
		if (phase == Phase.IDLE) {
			phase = Phase.BETTING;
			timer = betTicks;
		}
		return new FinanceBook.Result(true, "");
	}

	/** Takes back this player's bets before the race starts. */
	public FinanceBook.Result clear(UUID id) {
		if (phase == Phase.RACING) {
			return FinanceBook.Result.fail("They're off: bets stand until the finish.");
		}
		long back = 0;
		for (int i = bets.size() - 1; i >= 0; i--) {
			if (bets.get(i).player().equals(id)) {
				back += bets.get(i).amount();
				bets.remove(i);
			}
		}
		if (back > 0) {
			house.refund(id, back);
		}
		if (bets.isEmpty() && phase == Phase.BETTING) {
			phase = Phase.IDLE;
		}
		return new FinanceBook.Result(true, "");
	}

	/** Starts the race now (anyone with a bet on it may). */
	public FinanceBook.Result startNow(UUID id, long gameTime) {
		if (phase != Phase.BETTING) {
			return FinanceBook.Result.fail("Place a bet first.");
		}
		if (bets.stream().noneMatch(b -> b.player().equals(id))) {
			return FinanceBook.Result.fail("Only someone with a bet on this race can start it.");
		}
		start(gameTime);
		return new FinanceBook.Result(true, "");
	}

	private void nextRace() {
		newCard();
		phase = Phase.IDLE;
		timer = 0;
	}

	/** Draws the finishing order (each place in proportion to the strength of the horses left) and starts the race. */
	void start(long gameTime) {
		double[] w = new double[HORSES];
		for (int h = 0; h < HORSES; h++) {
			w[h] = strength(ratings[h]);
		}
		boolean[] placed = new boolean[HORSES];
		for (int place = 0; place < HORSES; place++) {
			double total = 0;
			for (int h = 0; h < HORSES; h++) {
				if (!placed[h]) {
					total += w[h];
				}
			}
			double roll = random.nextDouble() * total;
			int pick = -1;
			for (int h = 0; h < HORSES; h++) {
				if (placed[h]) {
					continue;
				}
				pick = h;
				roll -= w[h];
				if (roll < 0) {
					break;
				}
			}
			placed[pick] = true;
			order[place] = pick;
		}
		seed = random.nextLong();
		startTime = gameTime;
		raceNumber++;
		phase = Phase.RACING;
		timer = RACE_TICKS;
	}

	public void tick(long gameTime) {
		if (timer > 0) {
			timer--;
		}
		if (phase == Phase.BETTING && timer == 0) {
			start(gameTime);
		} else if (phase == Phase.RACING && timer == 0) {
			settle();
			phase = Phase.RESULT;
			timer = RESULT_TICKS;
		} else if (phase == Phase.RESULT && timer == 0) {
			nextRace();
		}
	}

	/** Pays every pool on the finishing order. */
	void settle() {
		double[][] stakes = {poolStakes(WIN), poolStakes(PLACE), poolStakes(SHOW)};
		List<Bet> settled = new ArrayList<>(bets);
		bets.clear();
		for (Bet bet : settled) {
			long payout = payout(stakes, bet.pool(), bet.horse(), bet.amount());
			String big = payout - bet.amount() >= 1_000 ? bet.name() + " won " + FinanceBook.amount(payout - bet.amount())
					+ " Jugs on " + NAMES[names[bet.horse()]] + " at the Derby Table." : null;
			house.settle(bet.player(), bet.name(), bet.amount(), payout, big);
			if (payout > 0) {
				log.add(bet.name() + " wins " + FinanceBook.amount(payout - bet.amount()) + " (" + POOLS[bet.pool()] + ", "
						+ NAMES[names[bet.horse()]] + ").");
			}
		}
		log.add(NAMES[names[order[0]]] + " wins, " + NAMES[names[order[1]]] + " second, " + NAMES[names[order[2]]] + " third.");
		while (log.size() > 8) {
			log.remove(0);
		}
		history.add(order[0]);
		while (history.size() > 10) {
			history.remove(0);
		}
	}

	/** What a bet gets back on the finishing order ({@link #order}): 0 if its horse did not place in its pool. */
	long payout(double[][] stakes, int pool, int horse, long amount) {
		int places = pool + 1;
		boolean in = false;
		double winning = 0;
		for (int k = 0; k < places; k++) {
			winning += stakes[pool][order[k]];
			in |= order[k] == horse;
		}
		if (!in) {
			return 0;
		}
		double total = 0;
		for (double s : stakes[pool]) {
			total += s;
		}
		double net = total * (100 - cutPercent) / 100.0;
		double multiple;
		if (pool == WIN) {
			multiple = net / stakes[pool][horse];
		} else {
			multiple = 1 + Math.max(0, net - winning) / places / stakes[pool][horse];
		}
		return (long) Math.floor(amount * clamp(multiple));
	}

	/** Gives back every bet still in play (the table broken, or the world closing). */
	public void cancelAll() {
		for (Bet bet : bets) {
			house.refund(bet.player(), bet.amount());
		}
		bets.clear();
		phase = Phase.IDLE;
		timer = 0;
	}

	/** Every bet still riding, by player. */
	public java.util.Map<UUID, Long> outstanding() {
		java.util.Map<UUID, Long> out = new java.util.HashMap<>();
		for (Bet bet : bets) {
			out.merge(bet.player(), bet.amount(), Long::sum);
		}
		return out;
	}

	/** A player who walked away before the off gets their bets back; once the race is running they ride. */
	public void away(UUID id) {
		if (phase != Phase.RACING) {
			clear(id);
		}
	}

	// ------------------------------------------------------------------ the race as it is drawn

	/**
	 * How far round the lap a horse is at {@code t} (0 at the start, 1 the moment the race is over): 0 to 1 is the lap,
	 * a little over 1 is the horse easing up past the post. Each horse finishes at a time set by its place, so they cross
	 * the line in {@link #order}; along the way each surges and fades on its own seeded rhythm, so the lead changes.
	 */
	public static double progress(long seed, int horse, int place, double t) {
		double finish = 0.86 + place * 0.022;
		if (t >= finish) {
			return Math.min(1.08, 1 + (t - finish) * 0.9);
		}
		double u = Math.max(0, t) / finish;
		SplittableRandom r = new SplittableRandom(seed * 31 + horse * 7919L);
		double amp = 0.025 + r.nextDouble() * 0.025;
		int waves = 2 + r.nextInt(2);
		double phase = r.nextDouble() * Math.PI * 2;
		// A quick start, an even run and a drive to the line, with the horse's own surges on top.
		double base = u - 0.04 * Math.sin(Math.PI * 2 * u) / (Math.PI * 2);
		double surge = amp * Math.sin(Math.PI * u) * Math.sin(waves * Math.PI * u + phase);
		return Math.max(0, base + surge);
	}

	/** Each horse's place in {@link #order} (0 the winner). */
	public static int[] places(int[] order) {
		int[] place = new int[order.length];
		for (int k = 0; k < order.length; k++) {
			place[order[k]] = k;
		}
		return place;
	}

	/**
	 * A point on the track, {@code lap} from 0 to 1 round it (from the finish line on the near straight, anticlockwise
	 * seen from above), {@code lane} 0 inside to 5 outside: {x, z} in the table model's pixels (x across, z from the near
	 * edge back). The same oval is printed on the table's top.
	 */
	public static double[] lanePoint(double lap, int lane) {
		double cx = 32;
		double cz = 25;
		double halfStraight = 13;
		double r = 9.6 + lane * 1.5;
		double straight = 2 * halfStraight;
		double bend = Math.PI * r;
		double total = 2 * straight + 2 * bend;
		double d = (((lap % 1.0) + 1.0) % 1.0) * total;
		if (d < straight / 2) {
			return new double[] {cx + d, cz - r};
		}
		d -= straight / 2;
		if (d < bend) {
			double a = -Math.PI / 2 + d / r;
			return new double[] {cx + halfStraight + Math.cos(a) * r, cz + Math.sin(a) * r};
		}
		d -= bend;
		if (d < straight) {
			return new double[] {cx + halfStraight - d, cz + r};
		}
		d -= straight;
		if (d < bend) {
			double a = Math.PI / 2 + d / r;
			return new double[] {cx - halfStraight + Math.cos(a) * r, cz + Math.sin(a) * r};
		}
		d -= bend;
		return new double[] {cx - halfStraight + d, cz - r};
	}
}
