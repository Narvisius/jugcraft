package io.github.jimbozoomer.jugcraft.finance;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The slot machines' reels and paytables. Plain Java, shared by the server (which spins) and the client (which draws
 * the reels the server stopped on). Each reel is a strip of symbols built from fixed counts, so the odds are exact:
 * {@link #returnToPlayer} works out the payback of a machine by going through every stop of every reel, and the
 * counts were chosen so that comes to the advertised figure.
 *
 * <ul>
 * <li>Fruit: three reels, one line, 94.97%. Cherries pay anywhere on the line; any three bars pay 5.</li>
 * <li>Clockwork: three reels, one line, 94.00%. The clock is wild and doubles a win for each one in it.</li>
 * <li>Gem Mine: five reels, three rows, nine lines, 92.00%. Three, four or five of an ore from the left.</li>
 * <li>Progressive: three reels, one line, 88.1% from the paytable plus the jackpot: two in every hundred Jugs bet go
 * to the jackpot meter, which three jackpot symbols win (in proportion to the bet against the machine's top bet).</li>
 * </ul>
 */
public final class Slots {
	public enum Machine {
		FRUIT("Fruit", 3, 1, 1),
		CLOCKWORK("Clockwork", 3, 1, 1),
		GEM("Gem Mine", 5, 3, 9),
		PROGRESSIVE("Progressive Jackpot", 3, 1, 1),
		NETHER("Nether Furnace", 3, 1, 1),
		TREASURE("Sunken Treasure", 5, 3, 20),
		CREEPER("Creeper Blast", 5, 3, 10),
		PUB("Pub Fruit", 3, 1, 1),
		SHARK("Shark Frenzy", 5, 3, 20),
		/** Golden Dragon pays 243 ways, not lines: {@code lines} is the 25 coins a spin bets. */
		DRAGON("Golden Dragon", 5, 3, 25);

		public final String title;
		public final int reels;
		public final int rows;
		public final int lines;

		Machine(String title, int reels, int rows, int lines) {
			this.title = title;
			this.reels = reels;
			this.rows = rows;
			this.lines = lines;
		}
	}

	// Symbols. Each machine uses its own set; the client draws them by these numbers.
	public static final int BLANK = 0;
	public static final int CHERRY = 1;
	public static final int BAR = 2;
	public static final int DOUBLE_BAR = 3;
	public static final int TRIPLE_BAR = 4;
	public static final int SEVEN = 5;
	public static final int COG = 6;
	public static final int KEY = 7;
	public static final int BELL = 8;
	public static final int CROWN = 9;
	public static final int CLOCK = 10;
	public static final int COAL = 11;
	public static final int IRON = 12;
	public static final int REDSTONE = 13;
	public static final int LAPIS = 14;
	public static final int GOLD = 15;
	public static final int EMERALD = 16;
	public static final int DIAMOND = 17;
	public static final int JACKPOT = 18;
	public static final int NUGGET = 19;
	public static final int MAGMA = 20;
	public static final int GHAST = 21;
	public static final int BLAZE = 22;
	public static final int NETHERITE = 23;
	public static final int ROD = 24;
	public static final int SHELL = 25;
	public static final int FISH = 26;
	public static final int TURTLE = 27;
	public static final int TRIDENT = 28;
	public static final int HEART = 29;
	public static final int CHEST = 30;
	public static final int STRING = 31;
	public static final int BONE = 32;
	public static final int ARROW = 33;
	public static final int SPIDER_EYE = 34;
	public static final int SWORD = 35;
	public static final int CREEPER_HEAD = 36;
	public static final int LEMON = 37;
	public static final int PLUM = 38;
	public static final int MELON = 39;
	public static final int OCTOPUS = 40;
	public static final int DIVER = 41;
	public static final int SHARK = 42;
	public static final int COIN = 43;
	public static final int LANTERN = 44;
	public static final int KOI = 45;
	public static final int INGOT = 46;
	public static final int DRAGON = 47;
	public static final int PEARL = 48;
	public static final String[] NAMES = {"Blank", "Cherry", "Bar", "Double Bar", "Triple Bar", "Seven", "Cog", "Key",
			"Bell", "Crown", "Clock", "Coal", "Iron", "Redstone", "Lapis", "Gold", "Emerald", "Diamond", "Jackpot", "Gold Nugget",
			"Magma Cube", "Ghast", "Blaze", "Netherite", "Blaze Rod", "Shell", "Fish", "Turtle", "Trident", "Heart of the Sea",
			"Treasure Chest", "String", "Bone", "Arrow", "Spider Eye", "Diamond Sword", "Creeper", "Lemon", "Plum", "Melon",
			"Octopus", "Diver", "Shark", "Gold Coin", "Lantern", "Koi", "Gold Ingot", "Dragon", "Pearl"};

	/** Two in every hundred Jugs bet on the Progressive go to its jackpot. */
	public static final int JACKPOT_SHARE_BP = 200;

	/** Symbol counts per reel: {symbol, count, symbol, count, ...}. */
	private static final int[][][] COUNTS = {
			// Fruit: 64 stops a reel.
			{{BLANK, 24, CHERRY, 1, BAR, 18, DOUBLE_BAR, 3, TRIPLE_BAR, 8, SEVEN, 10},
					{BLANK, 18, CHERRY, 2, BAR, 10, DOUBLE_BAR, 9, TRIPLE_BAR, 24, SEVEN, 1},
					{BLANK, 32, CHERRY, 7, BAR, 8, DOUBLE_BAR, 8, TRIPLE_BAR, 1, SEVEN, 8}},
			// Clockwork: 64 stops a reel.
			{{BLANK, 24, COG, 16, KEY, 20, BELL, 1, CROWN, 1, CLOCK, 2},
					{BLANK, 29, COG, 16, KEY, 4, BELL, 8, CROWN, 5, CLOCK, 2},
					{BLANK, 24, COG, 16, KEY, 12, BELL, 9, CROWN, 1, CLOCK, 2}},
			// Gem Mine: 40 stops a reel.
			{{COAL, 21, IRON, 6, REDSTONE, 5, LAPIS, 5, GOLD, 1, EMERALD, 1, DIAMOND, 1},
					{COAL, 17, IRON, 7, REDSTONE, 7, LAPIS, 6, GOLD, 1, EMERALD, 1, DIAMOND, 1},
					{COAL, 19, IRON, 6, REDSTONE, 5, LAPIS, 4, GOLD, 3, EMERALD, 2, DIAMOND, 1},
					{COAL, 28, IRON, 4, REDSTONE, 3, LAPIS, 2, GOLD, 1, EMERALD, 1, DIAMOND, 1},
					{COAL, 24, IRON, 6, REDSTONE, 6, LAPIS, 1, GOLD, 1, EMERALD, 1, DIAMOND, 1}},
			// Progressive: 64 stops a reel, two jackpot symbols on each.
			{{BLANK, 37, CHERRY, 2, BAR, 16, SEVEN, 7, JACKPOT, 2},
					{BLANK, 33, CHERRY, 3, BAR, 14, SEVEN, 12, JACKPOT, 2},
					{BLANK, 36, CHERRY, 1, BAR, 20, SEVEN, 5, JACKPOT, 2}},
			// Nether Furnace: 64 stops a reel; the blaze rod (the wheel bonus) only on the third.
			{{BLANK, 23, NUGGET, 10, MAGMA, 12, GHAST, 8, BLAZE, 7, NETHERITE, 4},
					{BLANK, 23, NUGGET, 8, MAGMA, 13, GHAST, 8, BLAZE, 6, NETHERITE, 6},
					{BLANK, 21, NUGGET, 11, MAGMA, 11, GHAST, 8, BLAZE, 7, NETHERITE, 4, ROD, 2}},
			// Sunken Treasure: 40 stops a reel; chests are the scatter.
			{{SHELL, 11, FISH, 8, TURTLE, 6, TRIDENT, 6, HEART, 4, CHEST, 1, BLANK, 4},
					{SHELL, 16, FISH, 7, TURTLE, 6, TRIDENT, 5, HEART, 2, CHEST, 2, BLANK, 2},
					{SHELL, 14, FISH, 9, TURTLE, 6, TRIDENT, 5, HEART, 3, CHEST, 1, BLANK, 2},
					{SHELL, 15, FISH, 10, TURTLE, 7, TRIDENT, 3, HEART, 3, CHEST, 1, BLANK, 1},
					{SHELL, 15, FISH, 9, TURTLE, 8, TRIDENT, 4, HEART, 1, CHEST, 1, BLANK, 2}},
			// Creeper Blast: 32 stops a reel; a creeper on reels 2 to 4 blows its whole reel wild.
			{{STRING, 18, BONE, 8, ARROW, 8, SPIDER_EYE, 3, SWORD, 3},
					{STRING, 16, BONE, 12, ARROW, 6, SPIDER_EYE, 3, SWORD, 2, CREEPER_HEAD, 1},
					{STRING, 16, BONE, 10, ARROW, 7, SPIDER_EYE, 3, SWORD, 3, CREEPER_HEAD, 1},
					{STRING, 16, BONE, 9, ARROW, 8, SPIDER_EYE, 4, SWORD, 2, CREEPER_HEAD, 1},
					{STRING, 17, BONE, 13, ARROW, 5, SPIDER_EYE, 3, SWORD, 2}},
			// Pub Fruit: 32 stops a reel, no blanks.
			{{CHERRY, 7, LEMON, 5, PLUM, 6, MELON, 4, BELL, 4, BAR, 3, SEVEN, 3},
					{CHERRY, 6, LEMON, 6, PLUM, 4, MELON, 7, BELL, 3, BAR, 3, SEVEN, 3},
					{CHERRY, 6, LEMON, 5, PLUM, 5, MELON, 5, BELL, 4, BAR, 4, SEVEN, 3}},
			// Shark Frenzy: 40 stops a reel; the shark (on reels 2 to 4) turns its reel and the one to its left wild.
			{{SHELL, 15, FISH, 11, OCTOPUS, 7, CHEST, 5, DIVER, 2},
					{SHELL, 14, FISH, 11, OCTOPUS, 7, CHEST, 5, DIVER, 2, SHARK, 1},
					{SHELL, 14, FISH, 11, OCTOPUS, 7, CHEST, 5, DIVER, 2, SHARK, 1},
					{SHELL, 14, FISH, 11, OCTOPUS, 7, CHEST, 5, DIVER, 2, SHARK, 1},
					{SHELL, 15, FISH, 11, OCTOPUS, 7, CHEST, 5, DIVER, 2}},
			// Golden Dragon: 40 stops a reel; the dragon is wild on reels 2 to 4; pearls start Hold & Spin.
			{{COIN, 14, LANTERN, 10, KOI, 7, INGOT, 4, PEARL, 5},
					{COIN, 13, LANTERN, 9, KOI, 7, INGOT, 4, DRAGON, 2, PEARL, 5},
					{COIN, 13, LANTERN, 9, KOI, 7, INGOT, 4, DRAGON, 2, PEARL, 5},
					{COIN, 13, LANTERN, 9, KOI, 7, INGOT, 4, DRAGON, 2, PEARL, 5},
					{COIN, 14, LANTERN, 10, KOI, 7, INGOT, 4, PEARL, 5}}};

	/** Shark Frenzy pays per line bet for 3, 4 and 5 from the left. */
	private static final int[][] SHARK_PAYS = {
			{SHELL, 1, 4, 12}, {FISH, 2, 5, 16}, {OCTOPUS, 3, 8, 30}, {CHEST, 6, 20, 80}, {DIVER, 15, 60, 500}};
	/** Golden Dragon pays per coin for each way: 3, 4 and 5 reels from the left (a spin is 25 coins). */
	private static final int[][] DRAGON_PAYS = {{COIN, 1, 2, 5}, {LANTERN, 1, 3, 10}, {KOI, 3, 8, 24}, {INGOT, 6, 20, 80}};
	/** Hold & Spin: a pearl on every reel (five) starts it. Each pearl carries a prize, in total bets, drawn from these. */
	public static final int PEARLS_TO_START = 5;
	public static final int[] PEARL_VALUES = {1, 2, 3, 5, 10, 20, 50};
	private static final int[] PEARL_WEIGHTS = {40, 25, 15, 10, 6, 3, 1};
	/** The chance each empty spot lands a pearl on a respin, and the GRAND for filling all fifteen, in total bets. */
	public static final double PEARL_CHANCE = 0.07;
	public static final int GRAND = 500;
	public static final int RESPINS = 3;

	/** The twenty Sunken Treasure lines (the Creeper Blast plays the first ten). */
	public static final int[][] LINES_20 = {
			{1, 1, 1, 1, 1}, {0, 0, 0, 0, 0}, {2, 2, 2, 2, 2}, {0, 1, 2, 1, 0}, {2, 1, 0, 1, 2}, {0, 0, 1, 2, 2}, {2, 2, 1, 0, 0},
			{1, 0, 0, 0, 1}, {1, 2, 2, 2, 1}, {1, 0, 1, 0, 1}, {1, 2, 1, 2, 1}, {0, 1, 0, 1, 0}, {2, 1, 2, 1, 2}, {1, 1, 0, 1, 1},
			{1, 1, 2, 1, 1}, {0, 1, 1, 1, 0}, {2, 1, 1, 1, 2}, {0, 2, 0, 2, 0}, {2, 0, 2, 0, 2}, {0, 2, 2, 2, 0}};
	/** Sunken Treasure pays per line bet for 3, 4 and 5 from the left. */
	private static final int[][] TREASURE_PAYS = {
			{SHELL, 5, 15, 50}, {FISH, 8, 20, 80}, {TURTLE, 10, 30, 120}, {TRIDENT, 20, 60, 250}, {HEART, 50, 200, 1000}};
	/** Chests anywhere on 3, 4 or 5 reels pay this many total bets (and start 10 free spins). */
	public static final int[] TREASURE_SCATTER = {0, 0, 0, 2, 10, 50};
	public static final int FREE_SPINS = 10;
	public static final int FREE_SPIN_MULTIPLIER = 3;
	private static final int[][] CREEPER_PAYS = {
			{STRING, 2, 5, 20}, {BONE, 3, 8, 30}, {ARROW, 5, 15, 60}, {SPIDER_EYE, 10, 40, 200}, {SWORD, 25, 150, 1500}};
	/** The Nether Furnace's prize wheel: times the bet. */
	public static final int[] WHEEL = {2, 3, 5, 5, 8, 10, 20, 50};
	/** Pub Fruit: the chance of nudges after a losing spin, and of a hold after a losing spin with none. */
	public static final double NUDGE_CHANCE = 1.0 / 8;
	public static final double HOLD_CHANCE = 1.0 / 4;

	/** The nine Gem Mine lines: the row on each of the five reels (0 top, 1 middle, 2 bottom). */
	public static final int[][] GEM_LINES = {
			{1, 1, 1, 1, 1}, {0, 0, 0, 0, 0}, {2, 2, 2, 2, 2}, {0, 1, 2, 1, 0}, {2, 1, 0, 1, 2},
			{0, 0, 1, 2, 2}, {2, 2, 1, 0, 0}, {1, 0, 0, 0, 1}, {1, 2, 2, 2, 1}};

	/** Gem Mine pays for three, four and five of a kind from the left, per line bet. */
	private static final int[][] GEM_PAYS = {
			{COAL, 2, 5, 15}, {IRON, 3, 8, 25}, {REDSTONE, 4, 10, 40}, {LAPIS, 5, 15, 60}, {GOLD, 10, 30, 120},
			{EMERALD, 20, 80, 400}, {DIAMOND, 50, 250, 2000}};

	private static final int[][][] STRIPS = new int[COUNTS.length][][];

	static {
		for (int m = 0; m < COUNTS.length; m++) {
			STRIPS[m] = new int[COUNTS[m].length][];
			for (int r = 0; r < COUNTS[m].length; r++) {
				STRIPS[m][r] = spread(COUNTS[m][r], m * 7 + r);
			}
		}
	}

	private Slots() {
	}

	/** Lays the symbols out round a reel, spread evenly (the order changes nothing about the odds). */
	private static int[] spread(int[] counts, int seed) {
		int total = 0;
		for (int i = 1; i < counts.length; i += 2) {
			total += counts[i];
		}
		int[] strip = new int[total];
		java.util.Arrays.fill(strip, -1);
		// Rarest first, each at even steps into the free places.
		List<int[]> order = new ArrayList<>();
		for (int i = 0; i < counts.length; i += 2) {
			order.add(new int[] {counts[i], counts[i + 1]});
		}
		order.sort((a, b) -> Integer.compare(a[1], b[1]));
		int offset = seed;
		for (int[] entry : order) {
			List<Integer> free = new ArrayList<>();
			for (int i = 0; i < total; i++) {
				if (strip[i] < 0) {
					free.add(i);
				}
			}
			for (int k = 0; k < entry[1]; k++) {
				int at = free.get((int) ((offset + (long) k * free.size() / entry[1]) % free.size()));
				strip[at] = entry[0];
			}
			offset += 3;
		}
		return strip;
	}

	public static int[] strip(Machine machine, int reel) {
		return STRIPS[machine.ordinal()][reel];
	}

	/** The symbol showing on a reel stopped at {@code stop}, {@code row} rows down from the top of the window. */
	public static int symbolAt(Machine machine, int reel, int stop, int row) {
		int[] strip = strip(machine, reel);
		int middle = machine.rows / 2;
		return strip[Math.floorMod(stop + row - middle, strip.length)];
	}

	/** Random stops, one per reel. */
	public static int[] spin(Machine machine, Random random) {
		int[] stops = new int[machine.reels];
		for (int r = 0; r < machine.reels; r++) {
			stops[r] = random.nextInt(strip(machine, r).length);
		}
		return stops;
	}

	/** What a spin pays, in multiples of the line bet (the whole bet for one-line machines), jackpot aside. */
	public static long pays(Machine machine, int[] stops) {
		return switch (machine) {
			case GEM -> {
				long total = 0;
				for (int[] line : GEM_LINES) {
					total += gemLine(stops, line);
				}
				yield total;
			}
			case TREASURE -> {
				long total = 0;
				for (int[] line : LINES_20) {
					total += runPay(machine, stops, line, 0);
				}
				yield total + (long) TREASURE_SCATTER[chests(stops)] * machine.lines;
			}
			case CREEPER -> {
				long total = 0;
				int wild = wildReels(stops);
				for (int i = 0; i < machine.lines; i++) {
					total += runPay(machine, stops, LINES_20[i], wild);
				}
				yield total;
			}
			case SHARK -> {
				long total = 0;
				int wild = sharkWild(stops);
				for (int i = 0; i < machine.lines; i++) {
					total += runPay(machine, stops, LINES_20[i], wild);
				}
				yield total;
			}
			case DRAGON -> ways(stops);
			default -> line(machine, symbolAt(machine, 0, stops[0], 0), symbolAt(machine, 1, stops[1], 0),
					symbolAt(machine, 2, stops[2], 0));
		};
	}

	/**
	 * Shark Frenzy: the reels (bits) gone wild. A shark showing on reels 2 to 4 makes its own reel wild and swims one
	 * reel left, making that one wild too (never the first reel, which a line starts from).
	 */
	public static int sharkWild(int[] stops) {
		int bits = 0;
		for (int reel = 1; reel <= 3; reel++) {
			for (int row = 0; row < 3; row++) {
				if (symbolAt(Machine.SHARK, reel, stops[reel], row) == SHARK) {
					bits |= 1 << reel;
					if (reel - 1 >= 1) {
						bits |= 1 << (reel - 1);
					}
					break;
				}
			}
		}
		return bits;
	}

	/**
	 * Golden Dragon's 243 ways, in coins: for each symbol, the number of its copies (the dragon standing in on reels 2 to 4)
	 * on each reel from the left, multiplied, times its pay for how many reels in a row it reached.
	 */
	public static long ways(int[] stops) {
		long total = 0;
		for (int[] pay : DRAGON_PAYS) {
			long count = 1;
			int run = 0;
			for (int reel = 0; reel < 5; reel++) {
				int here = 0;
				for (int row = 0; row < 3; row++) {
					int symbol = symbolAt(Machine.DRAGON, reel, stops[reel], row);
					if (symbol == pay[0] || (symbol == DRAGON && reel >= 1 && reel <= 3)) {
						here++;
					}
				}
				if (here == 0) {
					break;
				}
				count *= here;
				run++;
			}
			if (run >= 3) {
				total += pay[run - 2] * count;
			}
		}
		return total;
	}

	/** Golden Dragon: where the pearls are, as bits (bit reel * 3 + row). */
	public static int pearls(int[] stops) {
		int bits = 0;
		for (int reel = 0; reel < 5; reel++) {
			for (int row = 0; row < 3; row++) {
				if (symbolAt(Machine.DRAGON, reel, stops[reel], row) == PEARL) {
					bits |= 1 << (reel * 3 + row);
				}
			}
		}
		return bits;
	}

	/**
	 * Hold & Spin, played out: the pearls that started it stay, each with a prize; three respins, each empty spot
	 * landing a pearl with {@link #PEARL_CHANCE}, and every new pearl sets the respins back to three. Returns the prize on
	 * each of the fifteen spots in total bets (0 for none); filling them all adds the GRAND to the last one filled.
	 */
	public static int[] holdAndSpin(int startBits, java.util.Random random) {
		int[] values = new int[15];
		for (int i = 0; i < 15; i++) {
			if ((startBits & (1 << i)) != 0) {
				values[i] = pearlValue(random);
			}
		}
		int respins = RESPINS;
		while (respins > 0) {
			respins--;
			boolean landed = false;
			int filled = 0;
			for (int i = 0; i < 15; i++) {
				if (values[i] == 0 && random.nextDouble() < PEARL_CHANCE) {
					values[i] = pearlValue(random);
					landed = true;
				}
				if (values[i] != 0) {
					filled++;
				}
			}
			if (filled == 15) {
				values[14] += GRAND;
				break;
			}
			if (landed) {
				respins = RESPINS;
			}
		}
		return values;
	}

	private static int pearlValue(java.util.Random random) {
		int roll = random.nextInt(100);
		for (int i = 0; i < PEARL_VALUES.length; i++) {
			roll -= PEARL_WEIGHTS[i];
			if (roll < 0) {
				return PEARL_VALUES[i];
			}
		}
		return PEARL_VALUES[0];
	}

	/** The Gem Mine lines a spin wins on, as bits (bit i is line i). */
	public static int winningLines(Machine machine, int[] stops) {
		if (machine == Machine.DRAGON) {
			return 0;
		}
		if (machine == Machine.TREASURE || machine == Machine.CREEPER || machine == Machine.SHARK) {
			int bits = 0;
			int wild = machine == Machine.CREEPER ? wildReels(stops) : machine == Machine.SHARK ? sharkWild(stops) : 0;
			for (int i = 0; i < machine.lines; i++) {
				if (runPay(machine, stops, LINES_20[i], wild) > 0) {
					bits |= 1 << i;
				}
			}
			return bits;
		}
		if (machine != Machine.GEM) {
			return pays(machine, stops) > 0 || jackpot(machine, stops) ? 1 : 0;
		}
		int bits = 0;
		for (int i = 0; i < GEM_LINES.length; i++) {
			if (gemLine(stops, GEM_LINES[i]) > 0) {
				bits |= 1 << i;
			}
		}
		return bits;
	}

	/** Three jackpot symbols on the Progressive's line. */
	public static boolean jackpot(Machine machine, int[] stops) {
		return machine == Machine.PROGRESSIVE && symbolAt(machine, 0, stops[0], 0) == JACKPOT
				&& symbolAt(machine, 1, stops[1], 0) == JACKPOT && symbolAt(machine, 2, stops[2], 0) == JACKPOT;
	}

	/** How many reels show a treasure chest anywhere in the window. */
	public static int chests(int[] stops) {
		int count = 0;
		for (int reel = 0; reel < 5; reel++) {
			for (int row = 0; row < 3; row++) {
				if (symbolAt(Machine.TREASURE, reel, stops[reel], row) == CHEST) {
					count++;
					break;
				}
			}
		}
		return count;
	}

	/** Creeper Blast: the reels (bits) a creeper has blown wild. */
	public static int wildReels(int[] stops) {
		int bits = 0;
		for (int reel = 1; reel <= 3; reel++) {
			for (int row = 0; row < 3; row++) {
				if (symbolAt(Machine.CREEPER, reel, stops[reel], row) == CREEPER_HEAD) {
					bits |= 1 << reel;
					break;
				}
			}
		}
		return bits;
	}

	/** Nether Furnace: the blaze rod on the third reel's line starts the wheel. */
	public static boolean wheelBonus(Machine machine, int[] stops) {
		return machine == Machine.NETHER && symbolAt(machine, 2, stops[2], 0) == ROD;
	}

	/** A five-reel line: three, four or five of a symbol from the left, wild reels matching anything. */
	private static long runPay(Machine machine, int[] stops, int[] rows, int wild) {
		int first = symbolAt(machine, 0, stops[0], rows[0]);
		int run = 1;
		while (run < 5 && ((wild & (1 << run)) != 0 || symbolAt(machine, run, stops[run], rows[run]) == first)) {
			run++;
		}
		if (run < 3) {
			return 0;
		}
		int[][] table = machine == Machine.TREASURE ? TREASURE_PAYS : machine == Machine.SHARK ? SHARK_PAYS : CREEPER_PAYS;
		for (int[] pay : table) {
			if (pay[0] == first) {
				return pay[run - 2];
			}
		}
		return 0;
	}

	private static long gemLine(int[] stops, int[] rows) {
		int first = symbolAt(Machine.GEM, 0, stops[0], rows[0]);
		int run = 1;
		while (run < 5 && symbolAt(Machine.GEM, run, stops[run], rows[run]) == first) {
			run++;
		}
		if (run < 3) {
			return 0;
		}
		for (int[] pay : GEM_PAYS) {
			if (pay[0] == first) {
				return pay[run - 2];
			}
		}
		return 0;
	}

	/** A three-reel line's pay, per coin. */
	static long line(Machine machine, int a, int b, int c) {
		switch (machine) {
			case FRUIT -> {
				if (a == SEVEN && b == SEVEN && c == SEVEN) {
					return 200;
				}
				if (a == TRIPLE_BAR && b == TRIPLE_BAR && c == TRIPLE_BAR) {
					return 80;
				}
				if (a == DOUBLE_BAR && b == DOUBLE_BAR && c == DOUBLE_BAR) {
					return 40;
				}
				if (a == BAR && b == BAR && c == BAR) {
					return 20;
				}
				if (bar(a) && bar(b) && bar(c)) {
					return 5;
				}
				int cherries = (a == CHERRY ? 1 : 0) + (b == CHERRY ? 1 : 0) + (c == CHERRY ? 1 : 0);
				return new int[] {0, 2, 5, 10}[cherries];
			}
			case CLOCKWORK -> {
				int wilds = (a == CLOCK ? 1 : 0) + (b == CLOCK ? 1 : 0) + (c == CLOCK ? 1 : 0);
				if (wilds == 3) {
					return 1000;
				}
				long best = 0;
				int[][] table = {{CROWN, 100}, {BELL, 40}, {KEY, 20}, {COG, 10}};
				for (int[] entry : table) {
					if (match(a, entry[0]) && match(b, entry[0]) && match(c, entry[0])) {
						best = Math.max(best, (long) entry[1] << wilds);
					}
				}
				if (best == 0 && match(a, COG) && match(b, COG)) {
					best = 3L << wilds;
				}
				return best;
			}
			case NETHER -> {
				if (a == b && b == c) {
					return switch (a) {
						case NETHERITE -> 250;
						case BLAZE -> 60;
						case GHAST -> 30;
						case MAGMA -> 15;
						case NUGGET -> 8;
						default -> 0;
					};
				}
				if (a == NUGGET && b == NUGGET) {
					return 3;
				}
				return a == NUGGET ? 1 : 0;
			}
			case PUB -> {
				if (a == b && b == c) {
					return switch (a) {
						case SEVEN -> 100;
						case BAR -> 40;
						case BELL -> 20;
						case MELON -> 12;
						case PLUM -> 8;
						case LEMON -> 6;
						case CHERRY -> 5;
						default -> 0;
					};
				}
				if (a == CHERRY && b == CHERRY) {
					return 2;
				}
				return a == CHERRY ? 1 : 0;
			}
			case PROGRESSIVE -> {
				if (a == JACKPOT && b == JACKPOT && c == JACKPOT) {
					return 0;
				}
				if (a == SEVEN && b == SEVEN && c == SEVEN) {
					return 100;
				}
				if (a == BAR && b == BAR && c == BAR) {
					return 20;
				}
				if ((a == BAR || a == SEVEN) && (b == BAR || b == SEVEN) && (c == BAR || c == SEVEN)) {
					return 5;
				}
				int cherries = (a == CHERRY ? 1 : 0) + (b == CHERRY ? 1 : 0) + (c == CHERRY ? 1 : 0);
				return new int[] {0, 2, 4, 8}[cherries];
			}
			default -> {
				return 0;
			}
		}
	}

	private static boolean bar(int s) {
		return s == BAR || s == DOUBLE_BAR || s == TRIPLE_BAR;
	}

	private static boolean match(int s, int wanted) {
		return s == wanted || s == CLOCK;
	}

	/** The most one spin can pay, per coin (line bet), jackpot aside. */
	public static long topPay(Machine machine) {
		// The top prize on one line (a free spin's tripled one on the Sunken Treasure), per line bet: what a game must be
		// able to cover to take a bet. A rarer spin paying more is still paid; the bank lends the house the rest.
		return switch (machine) {
			case FRUIT -> 200;
			case CLOCKWORK -> 1000;
			case GEM -> 2000;
			case PROGRESSIVE -> 100;
			case NETHER -> 300;
			case TREASURE -> 3000;
			case CREEPER -> 1500;
			case PUB -> 100;
			case SHARK -> 1000;
			// The GRAND, in coins (a spin is 25).
			case DRAGON -> (long) GRAND * 25;
		};
	}

	/** One pay line of the paytable for the overlay: "Seven Seven Seven: 200". */
	public static List<String> paytable(Machine machine) {
		return switch (machine) {
			case FRUIT -> List.of("7 7 7: 200", "3 Triple Bars: 80", "3 Double Bars: 40", "3 Bars: 20", "Any 3 bars: 5",
					"Cherries 1/2/3: 2/5/10");
			case CLOCKWORK -> List.of("3 Clocks: 1000", "3 Crowns: 100", "3 Bells: 40", "3 Keys: 20", "3 Cogs: 10",
					"Cog Cog: 3", "Clock is wild: x2 each");
			case GEM -> List.of("Diamond 50/250/2000", "Emerald 20/80/400", "Gold 10/30/120", "Lapis 5/15/60",
					"Redstone 4/10/40", "Iron 3/8/25", "Coal 2/5/15");
			case PROGRESSIVE -> List.of("3 Jackpots: the meter", "7 7 7: 100", "3 Bars: 20", "Any 3 bars/7s: 5",
					"Cherries 1/2/3: 2/4/8");
			case NETHER -> List.of("3 Netherite: 250", "3 Blazes: 60", "3 Ghasts: 30", "3 Magma: 15", "3 Nuggets: 8",
					"Nugget Nugget: 3", "Nugget: 1", "Blaze rod on reel 3: spin the wheel, 2x to 50x");
			case TREASURE -> List.of("Heart 50/200/1000", "Trident 20/60/250", "Turtle 10/30/120", "Fish 8/20/80",
					"Shell 5/15/50", "Chests 3/4/5: 2/10/50 x bet and 10 free spins at 3x");
			case CREEPER -> List.of("Sword 25/150/1500", "Spider eye 10/40/200", "Arrow 5/15/60", "Bone 3/8/30",
					"String 2/5/20", "Creeper on reels 2-4: whole reel wild");
			case SHARK -> List.of("Diver 25/120/1000", "Chest 10/40/150", "Octopus 5/15/60", "Fish 3/8/30", "Shell 2/6/20",
					"Shark on reels 2-4: its reel and the one left go wild");
			case DRAGON -> List.of("243 ways, per coin (25 a spin)", "Ingot 8/25/100", "Koi 4/10/30", "Lantern 2/5/15",
					"Coin 1/3/8", "Dragon wild on reels 2-4", "A pearl on every reel: HOLD & SPIN", "Fill all 15: GRAND 500x");
			case PUB -> List.of("7 7 7: 100", "3 Bars: 40", "3 Bells: 20", "3 Melons: 12", "3 Plums: 8", "3 Lemons: 6",
					"3 Cherries: 5", "Cherry Cherry: 2, Cherry: 1", "Holds and nudges now and then");
		};
	}

	/**
	 * The exact payback of a machine's paytable (jackpot aside): every combination of stops, each weighed by its
	 * chance. For the Gem Mine one line is enough, since every line sees each reel's strip evenly.
	 */
	private static final java.util.Map<Machine, Double> RTP = new java.util.EnumMap<>(Machine.class);

	/** Pub Fruit pays back 95.04% with the holds and nudges played as well as they can be (worked out exactly when the
	 * reels were set; the tests play it out). */
	public static final double PUB_RETURN = 0.9504;
	/**
	 * Shark Frenzy pays back 94.96%, worked out exactly over every stop of its reels. Golden Dragon's reels pay 65.08%
	 * exactly; Hold & Spin starts on 0.742% of spins (also exact) and averages 37.0 total bets (20 million bonus rounds
	 * played), adding 27.44%: 92.52% in all.
	 */
	public static final double SHARK_RETURN = 0.9496;
	public static final double DRAGON_RETURN = 0.9252;

	public static double returnToPlayer(Machine machine) {
		return RTP.computeIfAbsent(machine, Slots::computeReturn);
	}

	private static int[] window(Machine machine, int reel, int stop) {
		return new int[] {symbolAt(machine, reel, stop, 0), symbolAt(machine, reel, stop, 1), symbolAt(machine, reel, stop, 2)};
	}

	private static double computeReturn(Machine machine) {
		switch (machine) {
			case SHARK -> {
				return SHARK_RETURN;
			}
			case DRAGON -> {
				return DRAGON_RETURN;
			}
			case PUB -> {
				return PUB_RETURN;
			}
			case NETHER -> {
				int[] a = strip(machine, 0);
				int[] b = strip(machine, 1);
				int[] c = strip(machine, 2);
				double wheel = 0;
				for (int w : WHEEL) {
					wheel += w;
				}
				wheel /= WHEEL.length;
				double total = 0;
				for (int x : a) {
					for (int y : b) {
						for (int z : c) {
							total += line(machine, x, y, z) + (z == ROD ? wheel : 0);
						}
					}
				}
				return total / ((double) a.length * b.length * c.length);
			}
			case TREASURE -> {
				double line = 0;
				for (int[] pay : TREASURE_PAYS) {
					double[] p = new double[5];
					for (int r = 0; r < 5; r++) {
						p[r] = share(strip(machine, r), pay[0]);
					}
					double three = p[0] * p[1] * p[2];
					line += three * (1 - p[3]) * pay[1] + three * p[3] * (1 - p[4]) * pay[2] + three * p[3] * p[4] * pay[3];
				}
				double[] dist = {1};
				for (int r = 0; r < 5; r++) {
					int[] st = strip(machine, r);
					int seen = 0;
					for (int stop = 0; stop < st.length; stop++) {
						for (int v : window(machine, r, stop)) {
							if (v == CHEST) {
								seen++;
								break;
							}
						}
					}
					double q = seen / (double) st.length;
					double[] next = new double[dist.length + 1];
					for (int k = 0; k < dist.length; k++) {
						next[k] += dist[k] * (1 - q);
						next[k + 1] += dist[k] * q;
					}
					dist = next;
				}
				double scatter = 0;
				double trigger = 0;
				for (int k = 3; k <= 5; k++) {
					scatter += dist[k] * TREASURE_SCATTER[k];
					trigger += dist[k];
				}
				double base = line + scatter;
				double free = FREE_SPINS * FREE_SPIN_MULTIPLIER * base / (1 - FREE_SPINS * trigger);
				return base + trigger * free;
			}
			case CREEPER -> {
				double[] wild = new double[5];
				double[][][] rows = new double[5][3][NAMES.length];
				for (int r = 0; r < 5; r++) {
					int[] st = strip(machine, r);
					int wilds = 0;
					for (int stop = 0; stop < st.length; stop++) {
						int[] w = window(machine, r, stop);
						boolean blown = r >= 1 && r <= 3 && (w[0] == CREEPER_HEAD || w[1] == CREEPER_HEAD || w[2] == CREEPER_HEAD);
						if (blown) {
							wilds++;
							continue;
						}
						for (int row = 0; row < 3; row++) {
							rows[r][row][w[row]]++;
						}
					}
					wild[r] = wilds / (double) st.length;
					int kept = st.length - wilds;
					for (int row = 0; row < 3; row++) {
						for (int sym = 0; sym < NAMES.length; sym++) {
							rows[r][row][sym] /= kept;
						}
					}
				}
				double total = 0;
				for (int i = 0; i < machine.lines; i++) {
					int[] line = LINES_20[i];
					for (int[] pay : CREEPER_PAYS) {
						double first = rows[0][line[0]][pay[0]];
						double[] m = new double[4];
						for (int r = 1; r < 5; r++) {
							m[r - 1] = wild[r] + (1 - wild[r]) * rows[r][line[r]][pay[0]];
						}
						double three = first * m[0] * m[1];
						total += three * (1 - m[2]) * pay[1] + three * m[2] * (1 - m[3]) * pay[2] + three * m[2] * m[3] * pay[3];
					}
				}
				return total / machine.lines;
			}
			default -> {
				return legacyReturn(machine);
			}
		}
	}

	private static double share(int[] strip, int symbol) {
		int count = 0;
		for (int s : strip) {
			if (s == symbol) {
				count++;
			}
		}
		return count / (double) strip.length;
	}

	private static double legacyReturn(Machine machine) {
		if (machine == Machine.GEM) {
			int[] rows = GEM_LINES[0];
			double total = 0;
			int[][] strips = new int[5][];
			for (int r = 0; r < 5; r++) {
				strips[r] = strip(machine, r);
			}
			// Per symbol: the chance of a run of 3, 4 or 5 from the left.
			for (int[] pay : GEM_PAYS) {
				double[] p = new double[5];
				for (int r = 0; r < 5; r++) {
					int count = 0;
					for (int s : strips[r]) {
						if (s == pay[0]) {
							count++;
						}
					}
					p[r] = count / (double) strips[r].length;
				}
				double three = p[0] * p[1] * p[2];
				total += three * (1 - p[3]) * pay[1] + three * p[3] * (1 - p[4]) * pay[2] + three * p[3] * p[4] * pay[3];
			}
			return total;
		}
		int[] a = strip(machine, 0);
		int[] b = strip(machine, 1);
		int[] c = strip(machine, 2);
		long total = 0;
		for (int x : a) {
			for (int y : b) {
				for (int z : c) {
					total += line(machine, x, y, z);
				}
			}
		}
		return total / ((double) a.length * b.length * c.length);
	}

	/** The chance of the jackpot on one spin. */
	public static double jackpotChance(Machine machine) {
		if (machine != Machine.PROGRESSIVE) {
			return 0;
		}
		double p = 1;
		for (int r = 0; r < 3; r++) {
			int count = 0;
			for (int s : strip(machine, r)) {
				if (s == JACKPOT) {
					count++;
				}
			}
			p *= count / (double) strip(machine, r).length;
		}
		return p;
	}
}
