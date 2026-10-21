package io.github.jimbozoomer.jugcraft.finance;

import java.util.Random;
import java.util.UUID;

/**
 * One slot machine: who is playing it, the last spin, and (on the Progressive) the jackpot meter. A spin takes the bet,
 * stops the reels ({@link Slots}) and pays at once; the reels' turning is only drawn by the client. On the Gem Mine
 * the bet is per line, nine lines a spin. Plain Java.
 */
public class SlotGame {
	/** A spin can come this often (the client's reels take about this long to stop). */
	public static final int SPIN_TICKS = 36;
	private static final int IDLE_RELEASE = 20 * 60;

	public final Slots.Machine machine;
	public UUID player;
	public String playerName = "";
	public int[] stops;
	public long lastBet;
	public long lastWin;
	public int lastLines;
	public boolean lastJackpot;
	/** Nether Furnace: the wheel segment the last spin landed on, or -1. */
	public int lastWheel = -1;
	/** Creeper Blast: the reels the last spin blew wild (bits). */
	public int lastWild;
	/** Golden Dragon: the last Hold & Spin's pearls (each spot's prize in total bets, 0 for none) and what it paid. */
	public int[] lastPearls = new int[15];
	public long lastBonus;
	/** Sunken Treasure: free spins left, at this line bet, and what they have paid. */
	public int freeSpins;
	public long freeBet;
	public long freeWon;
	/** Pub Fruit: nudges to use now, and whether the next spin may hold reels. */
	public int nudges;
	public long nudgeBet;
	public boolean holdOffered;
	public int lastHolds;
	public long spins;
	/** The Progressive's jackpot, owed by the house to whoever hits it. */
	public long jackpot;
	public final java.util.List<String> log = new java.util.ArrayList<>();
	private int cooldown;
	private int idle;

	private final CasinoHouse house;
	private final Random random;

	public SlotGame(Slots.Machine machine, CasinoHouse house, Random random) {
		this.machine = machine;
		this.house = house;
		this.random = random;
		this.stops = new int[machine.reels];
	}

	/** The most one spin at {@code bet} can pay. */
	public long maxPayout(long bet) {
		long top = Slots.topPay(machine) * bet;
		return top + jackpotFor(bet);
	}

	/** The share of the meter a bet wins: all of it at the machine's top bet. */
	public long jackpotFor(long bet) {
		if (machine != Slots.Machine.PROGRESSIVE || house.maxBet() <= 0) {
			return 0;
		}
		return jackpot * Math.min(bet, house.maxBet()) / house.maxBet();
	}

	/** Claims the machine for a player (one at a time; it frees itself when they stop). */
	public FinanceBook.Result use(UUID id, String name) {
		if (player != null && !player.equals(id)) {
			return FinanceBook.Result.fail(playerName + " is playing this machine.");
		}
		player = id;
		playerName = name;
		idle = 0;
		return new FinanceBook.Result(true, "");
	}

	public void release(UUID id) {
		if (id.equals(player)) {
			settleFreeSpins();
			nudges = 0;
			holdOffered = false;
			player = null;
			playerName = "";
		}
	}

	/** One spin: {@code bet} a spin (per line on the many-line machines); {@code holds} the Pub Fruit's held reels. */
	public FinanceBook.Result spin(UUID id, String name, long bet, long jackpotSeed, int holds) {
		FinanceBook.Result using = use(id, name);
		if (!using.ok()) {
			return using;
		}
		if (cooldown > 0) {
			return FinanceBook.Result.fail("");
		}
		if (freeSpins > 0) {
			freeSpin(id, name);
			return new FinanceBook.Result(true, "");
		}
		if (bet < house.minBet()) {
			return FinanceBook.Result.fail("The least bet here is " + FinanceBook.amount(house.minBet()) + ".");
		}
		if (bet > house.maxBet()) {
			return FinanceBook.Result.fail("The most this machine takes is " + FinanceBook.amount(house.maxBet()) + ".");
		}
		long stake = bet * machine.lines;
		if (machine == Slots.Machine.PROGRESSIVE && jackpot < jackpotSeed) {
			jackpot = jackpotSeed;
		}
		if (maxPayout(bet) > house.cover(id)) {
			return FinanceBook.Result.fail("The house cannot cover the top prize at that bet.");
		}
		String failed = house.stake(id, stake);
		if (failed != null) {
			return FinanceBook.Result.fail(failed);
		}
		// Pub Fruit: a hold keeps the chosen reels where they stopped (only when the machine offered it).
		boolean holdSpin = machine == Slots.Machine.PUB && holdOffered && (holds & 7) != 0;
		int[] before = stops.clone();
		nudges = 0;
		stops = Slots.spin(machine, random);
		if (holdSpin) {
			for (int reel = 0; reel < 3; reel++) {
				if ((holds & (1 << reel)) != 0 && reel < before.length) {
					stops[reel] = before[reel];
				}
			}
		}
		lastHolds = holdSpin ? holds & 7 : 0;
		boolean offered = holdOffered;
		holdOffered = false;
		long payout = Slots.pays(machine, stops) * bet;
		lastJackpot = Slots.jackpot(machine, stops);
		if (lastJackpot) {
			long won = jackpotFor(bet);
			payout += won;
			jackpot = Math.max(jackpotSeed, jackpot - won);
		}
		if (machine == Slots.Machine.PROGRESSIVE) {
			jackpot += Math.max(1, stake * Slots.JACKPOT_SHARE_BP / 10_000);
		}
		lastWheel = -1;
		if (Slots.wheelBonus(machine, stops)) {
			lastWheel = random.nextInt(Slots.WHEEL.length);
			payout += Slots.WHEEL[lastWheel] * bet;
		}
		lastWild = machine == Slots.Machine.CREEPER ? Slots.wildReels(stops) : machine == Slots.Machine.SHARK ? Slots.sharkWild(stops) : 0;
		lastPearls = new int[15];
		lastBonus = 0;
		if (machine == Slots.Machine.DRAGON) {
			int pearls = Slots.pearls(stops);
			if (Integer.bitCount(pearls) >= Slots.PEARLS_TO_START) {
				// Hold & Spin, played out at once; the screen shows it pearl by pearl.
				lastPearls = Slots.holdAndSpin(pearls, random);
				long totalBets = 0;
				for (int v : lastPearls) {
					totalBets += v;
				}
				lastBonus = totalBets * bet * machine.lines;
				payout += lastBonus;
				int filled = 0;
				for (int v : lastPearls) {
					filled += v > 0 ? 1 : 0;
				}
				log("HOLD & SPIN: " + filled + " pearls, " + FinanceBook.amount(lastBonus) + (filled == 15 ? " with the GRAND!" : "!"));
			}
		}
		if (machine == Slots.Machine.TREASURE && Slots.chests(stops) >= 3) {
			freeSpins += Slots.FREE_SPINS;
			freeBet = bet;
			freeWon = 0;
			log("10 FREE SPINS at 3x!");
		}
		// Pub Fruit: after a plain losing spin, nudges now and then; failing that, a hold for the next spin.
		if (machine == Slots.Machine.PUB && !offered && payout == 0) {
			if (random.nextDouble() < Slots.NUDGE_CHANCE) {
				nudges = 1 + random.nextInt(3);
				nudgeBet = bet;
				log(nudges + " nudge" + (nudges > 1 ? "s" : "") + "!");
			} else if (random.nextDouble() < Slots.HOLD_CHANCE) {
				holdOffered = true;
				log("HOLD offered on your next spin.");
			}
		}
		finish(id, name, stake, payout, bet);
		return new FinanceBook.Result(true, "");
	}

	private void finish(UUID id, String name, long stake, long payout, long bet) {
		lastBet = bet;
		lastWin = payout;
		lastLines = Slots.winningLines(machine, stops);
		spins++;
		cooldown = SPIN_TICKS;
		String big = lastJackpot ? name + " hit the JACKPOT on a " + machine.title + " machine: " + FinanceBook.amount(payout)
				+ " Jugs!" : payout >= Math.max(stake, bet) * 50 && payout >= 500 ? name + " won " + FinanceBook.amount(payout)
				+ " Jugs on a " + machine.title + " slot machine." : null;
		house.settle(id, name, stake, payout, big);
		if (payout > 0) {
			log((lastJackpot ? "JACKPOT! " : lastWheel >= 0 ? "Wheel " + Slots.WHEEL[lastWheel] + "x! " : "Win: ")
					+ FinanceBook.amount(payout));
		}
	}

	/** A Sunken Treasure free spin: no stake, wins tripled, more chests add ten more. */
	private void freeSpin(UUID id, String name) {
		freeSpins--;
		stops = Slots.spin(machine, random);
		long payout = Slots.pays(machine, stops) * freeBet * Slots.FREE_SPIN_MULTIPLIER;
		if (Slots.chests(stops) >= 3) {
			freeSpins += Slots.FREE_SPINS;
			log("10 more free spins!");
		}
		lastJackpot = false;
		lastWheel = -1;
		lastWild = 0;
		lastHolds = 0;
		freeWon += payout;
		finish(id, name, 0, payout, freeBet);
		if (freeSpins == 0) {
			log("Free spins paid " + FinanceBook.amount(freeWon) + " in all.");
		}
	}

	/** Pub Fruit: nudge a reel down one symbol. A win pays at once and ends the nudges. */
	public FinanceBook.Result nudge(UUID id, String name, int reel) {
		if (!id.equals(player) || nudges <= 0 || reel < 0 || reel > 2) {
			return FinanceBook.Result.fail("No nudges to use.");
		}
		nudges--;
		int[] strip = Slots.strip(machine, reel);
		stops[reel] = Math.floorMod(stops[reel] - 1, strip.length);
		long payout = Slots.pays(machine, stops) * nudgeBet;
		if (payout > 0) {
			nudges = 0;
			lastJackpot = false;
			lastWheel = -1;
			log("Nudged to a win!");
			finish(id, name, 0, payout, nudgeBet);
		} else {
			lastWin = 0;
			lastLines = 0;
			spins++;
		}
		return new FinanceBook.Result(true, "");
	}

	/** Gives up any nudges left. */
	public FinanceBook.Result collect(UUID id) {
		if (id.equals(player)) {
			nudges = 0;
		}
		return new FinanceBook.Result(true, "");
	}

	/** Plays out any free spins left (a player who walks away still gets them). */
	private void settleFreeSpins() {
		UUID id = player;
		String name = playerName;
		while (freeSpins > 0 && id != null) {
			freeSpin(id, name);
		}
		cooldown = 0;
	}

	private void log(String line) {
		log.add(line);
		while (log.size() > 6) {
			log.remove(0);
		}
	}

	public void tick() {
		if (cooldown > 0) {
			cooldown--;
		}
		if (player != null && ++idle > IDLE_RELEASE) {
			release(player);
		}
	}

	public void touch() {
		idle = 0;
	}
}
