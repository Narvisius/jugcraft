package io.github.jimbozoomer.jugcraft.finance;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Runs one casino game and is its house. The player who placed it owns it: every lost bet goes to their Jugs and every
 * win is paid from them, the central bank lending them what they lack ({@link FinanceBook#settleStake}). The owner sets
 * the table's least and most bet within the admins' limits, and may not play their own game.
 *
 * <p>Players who open the game's screen are its watchers and get a fresh view whenever anything changes. Bets still on
 * the table when the world closes, or when the game is broken, go back to their players.
 */
public class CasinoBlockEntity extends BlockEntity {
	private static final double REACH_SQ = 16 * 16;

	private UUID owner;
	private String ownerName = "";
	private long minBet;
	private long maxBet;
	/** True when the owner has made the central bank the house (the owner then neither wins nor loses). */
	private boolean bankHouse;
	/** The owner's betting clock in seconds (0: the server's setting). */
	private int betSeconds;
	/** Which Plinko risks players may pick: bit 0 Low, 1 Medium, 2 High. */
	private int plinkoRisks = 7;
	/** Lottery kiosk: days between draws (0: the server's setting) and the house's percent of each ticket. */
	private int lotteryDays;
	private int lotteryCut = 10;
	/** The takings since the owner last reset them: bets taken, paid back, the biggest single win and how many bets. */
	private long takenIn;
	private long paidOut;
	private long biggestWin;
	private long betCount;
	/** Sounds for the players watching from the world, waiting for the bettor's screen to show the result. */
	private record PendingSound(long at, UUID except, int kind) {
	}

	private final java.util.List<PendingSound> sounds = new java.util.ArrayList<>();
	private static final int SOUND_CHIP = 0;
	private static final int SOUND_LEVER = 1;
	private static final int SOUND_COINS = 2;
	private static final int SOUND_BIG_WIN = 3;
	private final Map<UUID, Long> watchers = new HashMap<>();
	private final Map<UUID, Long> owed = new HashMap<>();
	private Blackjack blackjack;
	private Roulette roulette;
	private Craps craps;
	private SlotGame slot;
	private long jackpotSaved = -1;
	private ArcadeGame arcade;
	private ShowGames.Plinko plinko;
	private ShowGames.BigSix bigSix;
	private ShowGames.Baccarat baccarat;
	private Derby derby;
	/** The Derby Table's house cut of each pool, in percent. */
	private int derbyCut = 15;
	/** The derby's phase when nearby players were last told of it (they draw the race from the block's data). */
	private Derby.Phase derbyShown;
	private CoinPusher pusher;
	private final ShowGames.Lottery lottery = new ShowGames.Lottery();
	private String shown = "";
	private long ticks;
	private final CasinoHouse house = new House();

	public CasinoBlockEntity(BlockPos pos, BlockState state) {
		super(ExchangeBlocks.CASINO_ENTITY, pos, state);
	}

	public CasinoBlock.Game game() {
		return getBlockState().getBlock() instanceof CasinoBlock block ? block.game() : CasinoBlock.Game.BLACKJACK;
	}

	public UUID owner() {
		return owner;
	}

	public String ownerName() {
		return ownerName;
	}

	public boolean isOwner(ServerPlayer player) {
		return player.getUUID().equals(owner);
	}

	void setOwner(ServerPlayer player) {
		owner = player.getUUID();
		ownerName = player.getName().getString();
		Ledger.get(player.level().getServer()).book().account(owner, ownerName);
		setChanged();
	}

	public Blackjack blackjack() {
		if (blackjack == null) {
			blackjack = new Blackjack(house, new Random());
		}
		return blackjack;
	}

	public Roulette roulette() {
		if (roulette == null) {
			roulette = new Roulette(house, new Random());
		}
		return roulette;
	}

	public Craps craps() {
		if (craps == null) {
			craps = new Craps(house, new Random());
		}
		return craps;
	}

	public SlotGame slot() {
		if (slot == null && game().slot()) {
			slot = new SlotGame(game().machine, house, new Random());
			if (jackpotSaved >= 0) {
				slot.jackpot = jackpotSaved;
			}
		}
		return slot;
	}

	public ArcadeGame arcade() {
		if (arcade == null) {
			arcade = new ArcadeGame(house, new Random());
		}
		return arcade;
	}

	public ShowGames.Plinko plinko() {
		if (plinko == null) {
			plinko = new ShowGames.Plinko(house, new Random());
		}
		return plinko;
	}

	public ShowGames.BigSix bigSix() {
		if (bigSix == null) {
			bigSix = new ShowGames.BigSix(house, new Random());
		}
		return bigSix;
	}

	public Derby derby() {
		if (derby == null) {
			derby = new Derby(house, new Random());
		}
		return derby;
	}

	public ShowGames.Baccarat baccarat() {
		if (baccarat == null) {
			baccarat = new ShowGames.Baccarat(house, new Random());
		}
		return baccarat;
	}

	public CoinPusher pusher() {
		if (pusher == null) {
			pusher = new CoinPusher(new Random());
		}
		return pusher;
	}

	public ShowGames.Lottery lottery() {
		return lottery;
	}

	/** Who takes the house's part of money paid to this player: the owner, or the reserve (null). */
	private UUID houseFor(UUID player) {
		return bankHouse || player.equals(owner) ? null : owner;
	}

	// ------------------------------------------------------------------ coin pusher, lottery and scratch cards

	FinanceBook.Result pusherDrop(ServerPlayer player, long amount) {
		if (amount < minBet() || amount > maxBet()) {
			return FinanceBook.Result.fail("Drops here are " + FinanceBook.amount(minBet()) + " to " + FinanceBook.amount(maxBet()) + ".");
		}
		MinecraftServer mc = ((ServerLevel) level).getServer();
		Ledger ledger = Ledger.get(mc);
		FinanceBook book = ledger.book();
		long day = FinanceService.day(mc);
		FinanceBook.Result staked = book.stake(player.getUUID(), amount, day);
		if (!staked.ok()) {
			return staked;
		}
		soundForOthers(player.getUUID(), SOUND_LEVER, 0);
		long spill = pusher().drop(amount);
		long mine = CoinPusher.playerPart(spill);
		book.escrowTo(player.getUUID(), mine);
		book.escrowTo(houseFor(player.getUUID()), spill - mine);
		if (houseFor(player.getUUID()) != null) {
			count(amount, mine);
		}
		if (mine > 0) {
			soundForOthers(player.getUUID(), mine >= 500 ? SOUND_BIG_WIN : SOUND_COINS, revealTicks());
		}
		pusher().lastWin = mine;
		if (mine > 0) {
			pusher().log.add(player.getName().getString() + " caught " + FinanceBook.amount(mine) + ".");
			while (pusher().log.size() > 6) {
				pusher().log.remove(0);
			}
		}
		if (mine >= 500) {
			book.shout(day, player.getUUID(), player.getName().getString() + " caught " + FinanceBook.amount(mine)
					+ " Jugs from a coin pusher.");
			FinanceService.announce(mc, ledger);
		}
		ledger.setDirty();
		setChanged();
		return new FinanceBook.Result(true, "");
	}

	/** A lottery ticket costs the kiosk's least bet. */
	public long ticketPrice() {
		return minBet();
	}

	FinanceBook.Result buyTickets(ServerPlayer player, int count) {
		if (owner == null) {
			return FinanceBook.Result.fail("This kiosk has no owner.");
		}
		if (count < 1 || count > 10) {
			return FinanceBook.Result.fail("Buy 1 to 10 tickets at a time.");
		}
		UUID id = player.getUUID();
		if (lottery.ticketsOf(id) + count > ShowGames.Lottery.MAX_TICKETS_EACH) {
			return FinanceBook.Result.fail("That is as many tickets as one player may hold for a draw.");
		}
		long price = ticketPrice();
		long cut = price * lotteryCut / 100;
		long toPot = price - cut;
		MinecraftServer mc = ((ServerLevel) level).getServer();
		Ledger ledger = Ledger.get(mc);
		FinanceBook book = ledger.book();
		long day = FinanceService.day(mc);
		if (book.cash(id) < price * count) {
			return FinanceBook.Result.fail(count + " tickets cost " + FinanceBook.amount(price * count) + "; you hold "
					+ FinanceBook.amount(book.cash(id)) + ".");
		}
		FinanceBook.Result staked = book.stake(id, toPot * count, day);
		if (!staked.ok()) {
			return staked;
		}
		if (cut > 0) {
			FinanceBook.Result sold = book.sellChance(id, houseFor(id), cut * count, 0, day);
			if (!sold.ok()) {
				book.refundStake(id, toPot * count, day);
				return sold;
			}
		}
		for (int i = 0; i < count; i++) {
			lottery.tickets.add(id);
			lottery.paid.add(toPot);
		}
		lottery.names.put(id, player.getName().getString());
		lottery.pot += toPot * count;
		if (houseFor(id) != null) {
			// The pot is the players' money: the house's takings are its cut of each ticket.
			takenIn += cut * count;
			betCount += count;
		}
		ledger.setDirty();
		setChanged();
		return new FinanceBook.Result(true, "You hold " + lottery.ticketsOf(id) + " ticket" + (lottery.ticketsOf(id) == 1 ? "" : "s")
				+ " for the next draw.");
	}

	FinanceBook.Result buyScratch(ServerPlayer player, int kind) {
		if (owner == null) {
			return FinanceBook.Result.fail("This kiosk has no owner.");
		}
		if (kind < 0 || kind >= ShowGames.ScratchCard.values().length) {
			return FinanceBook.Result.fail("Pick a card.");
		}
		ShowGames.ScratchCard card = ShowGames.ScratchCard.values()[kind];
		MinecraftServer mc = ((ServerLevel) level).getServer();
		Ledger ledger = Ledger.get(mc);
		FinanceBook.Result sold = ledger.book().sellChance(player.getUUID(), houseFor(player.getUUID()), card.price,
				card.bankShare(), FinanceService.day(mc));
		if (!sold.ok()) {
			return sold;
		}
		// The bank's share pays the prizes; the house keeps the rest.
		if (houseFor(player.getUUID()) != null) {
			takenIn += card.price;
			paidOut += Math.min(card.price, card.bankShare());
			betCount++;
		}
		setChanged();
		ledger.setDirty();
		net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(ExchangeBlocks.SCRATCH_CARDS.get(card));
		ScratchTickets.get(mc).issue(stack,kind);
		if (!player.getInventory().add(stack)) {
			player.spawnAtLocation((ServerLevel) level, stack);
		}
		return new FinanceBook.Result(true, "A " + card.title + " is in your inventory: use it to scratch.");
	}

	/** Draws the lottery once its days have passed (the whole pot to one ticket). */
	private void lotteryDraw(MinecraftServer mc) {
		long day = FinanceService.day(mc);
		if (lottery.lastDraw < 0) {
			lottery.lastDraw = day;
			setChanged();
			return;
		}
		long every = drawEvery(mc);
		if (day - lottery.lastDraw < every) {
			return;
		}
		lottery.lastDraw = day;
		setChanged();
		if (lottery.tickets.isEmpty()) {
			return;
		}
		Ledger ledger = Ledger.get(mc);
		UUID winner = lottery.tickets.get(new Random().nextInt(lottery.tickets.size()));
		String name = lottery.names.getOrDefault(winner, "Someone");
		long prize = lottery.pot;
		ledger.book().escrowTo(winner, prize);
		biggestWin = Math.max(biggestWin, prize);
		ledger.book().shout(day, winner, name + " won the lottery at " + ownerName + "'s kiosk: " + FinanceBook.amount(prize)
				+ " Jugs from " + lottery.tickets.size() + " tickets!");
		lottery.log.add(name + " won " + FinanceBook.amount(prize) + ".");
		while (lottery.log.size() > 6) {
			lottery.log.remove(0);
		}
		lottery.lastWinner = name;
		lottery.lastPrize = prize;
		lottery.pot = 0;
		lottery.tickets.clear();
		lottery.paid.clear();
		lottery.names.clear();
		ledger.setDirty();
		FinanceService.announce(mc, ledger);
	}

	/** Days between draws: the owner's setting, else the server's. */
	public long drawEvery(MinecraftServer mc) {
		return lotteryDays > 0 ? lotteryDays : Math.max(1, ExchangeSettings.value(Ledger.get(mc).book(), ExchangeSettings.LOTTERY_DAYS));
	}

	public int lotteryCut() {
		return lotteryCut;
	}

	public long nextDraw(MinecraftServer mc) {
		long every = drawEvery(mc);
		return lottery.lastDraw < 0 ? FinanceService.day(mc) + every : lottery.lastDraw + every;
	}

	// ------------------------------------------------------------------ limits

	private FinanceBook book() {
		return Ledger.get(((ServerLevel) level).getServer()).book();
	}

	long adminMin() {
		return Math.max(1, ExchangeSettings.value(book(), ExchangeSettings.CASINO_MIN_BET));
	}

	long adminMax() {
		return Math.max(adminMin(), ExchangeSettings.value(book(), ExchangeSettings.CASINO_MAX_BET));
	}

	public long minBet() {
		return Math.max(adminMin(), Math.min(adminMax(), minBet <= 0 ? adminMin() : minBet));
	}

	public long maxBet() {
		return Math.max(minBet(), Math.min(adminMax(), maxBet <= 0 ? adminMax() : maxBet));
	}

	public boolean bankHouse() {
		return bankHouse;
	}

	/** Plays a sound at the game for everyone near it but {@code except} (whose own screen plays its own). */
	private void soundForOthers(UUID except, int kind, int delayTicks) {
		sounds.add(new PendingSound(ticks + delayTicks, except, kind));
	}

	/** How long the bettor's screen takes to show a result: the reels, the ball, the coin. */
	private int revealTicks() {
		if (game().slot()) {
			return 60;
		}
		return switch (game()) {
			case PLINKO -> 40;
			case COIN_PUSHER -> 30;
			default -> 0;
		};
	}

	private void playSounds(ServerLevel level) {
		java.util.Iterator<PendingSound> it = sounds.iterator();
		while (it.hasNext()) {
			PendingSound sound = it.next();
			if (sound.at() > ticks) {
				continue;
			}
			it.remove();
			net.minecraft.world.entity.player.Player except = sound.except() == null ? null
					: level.getServer().getPlayerList().getPlayer(sound.except());
			net.minecraft.core.BlockPos at = getBlockPos();
			net.minecraft.sounds.SoundSource source = net.minecraft.sounds.SoundSource.BLOCKS;
			switch (sound.kind()) {
				case SOUND_CHIP -> level.playSound(except, at, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HAT.value(), source, 0.4F, 1.4F);
				case SOUND_LEVER -> level.playSound(except, at, net.minecraft.sounds.SoundEvents.LEVER_CLICK, source, 0.6F, 0.85F);
				case SOUND_COINS -> level.playSound(except, at, net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, source, 0.6F, 1.2F);
				default -> {
					level.playSound(except, at, net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, source, 1.0F, 1.0F);
					level.playSound(except, at, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BELL.value(), source, 1.5F, 1.0F);
				}
			}
		}
	}

	/** Counts a settled bet into the takings. */
	private void count(long stake, long payout) {
		takenIn += Math.max(0, stake);
		paidOut += Math.max(0, payout);
		if (stake > 0) {
			betCount++;
		}
		biggestWin = Math.max(biggestWin, payout - stake);
	}

	/** True for the games that take bets on a clock (cards dealt, dice rolled or the wheel spun when it runs out). */
	public boolean usesClock() {
		return switch (game()) {
			case BLACKJACK, ROULETTE, CRAPS, PRIZE_WHEEL, BACCARAT, DERBY -> true;
			default -> false;
		};
	}

	public int betSeconds() {
		return betSeconds;
	}

	public int plinkoRisks() {
		return plinkoRisks;
	}

	/**
	 * {bet seconds now, the owner's clock setting (0 server), whether the game uses a clock, taken in, paid out, biggest win,
	 * bets, Plinko risks, lottery days now, the owner's lottery days (0 server), the lottery cut percent}.
	 */
	public long[] tableInfo(boolean owner, FinanceBook book) {
		long now = betSeconds > 0 ? betSeconds : Math.max(5, ExchangeSettings.value(book, ExchangeSettings.CASINO_BET_SECONDS));
		long days = lotteryDays > 0 ? lotteryDays : Math.max(1, ExchangeSettings.value(book, ExchangeSettings.LOTTERY_DAYS));
		return new long[] {now, betSeconds, usesClock() ? 1 : 0, owner ? takenIn : 0, owner ? paidOut : 0, owner ? biggestWin : 0,
				owner ? betCount : 0, plinkoRisks, days, lotteryDays, game() == CasinoBlock.Game.DERBY ? derbyCut : lotteryCut};
	}

	FinanceBook.Result resetTakings(ServerPlayer player) {
		if (!isOwner(player)) {
			return FinanceBook.Result.fail("Only the owner resets the takings.");
		}
		takenIn = 0;
		paidOut = 0;
		biggestWin = 0;
		betCount = 0;
		setChanged();
		return new FinanceBook.Result(true, "Takings reset.");
	}

	FinanceBook.Result setLimits(ServerPlayer player, long min, long max, boolean bank, String extra) {
		if (!isOwner(player)) {
			return FinanceBook.Result.fail("Only the owner changes this game's settings.");
		}
		if (min < 1 || max < min) {
			return FinanceBook.Result.fail("Give a least bet of 1 or more and a most bet at least as big.");
		}
		if (bank != bankHouse && !outstanding().isEmpty()) {
			return FinanceBook.Result.fail("Change who is the house once no bets are on the table.");
		}
		int seconds = betSeconds;
		int risks = plinkoRisks;
		int days = lotteryDays;
		int cutPercent = game() == CasinoBlock.Game.DERBY ? derbyCut : lotteryCut;
		for (String part : extra == null ? new String[0] : extra.split(";")) {
			String[] kv = part.split("=", 2);
			if (kv.length != 2) {
				continue;
			}
			int value;
			try {
				value = Integer.parseInt(kv[1].trim());
			} catch (NumberFormatException e) {
				return FinanceBook.Result.fail("Give whole numbers for the clock, draw days and cut.");
			}
			switch (kv[0]) {
				case "clock" -> {
					if (value != 0 && (value < 5 || value > 120)) {
						return FinanceBook.Result.fail("The betting clock runs 5 to 120 seconds (or 0 for the server's).");
					}
					seconds = value;
				}
				case "risks" -> {
					if (value < 1 || value > 7) {
						return FinanceBook.Result.fail("Allow at least one risk level.");
					}
					risks = value;
				}
				case "days" -> {
					if (value < 0 || value > 60) {
						return FinanceBook.Result.fail("Draws run every 1 to 60 days (or 0 for the server's).");
					}
					days = value;
				}
				case "cut" -> {
					if (value < 0 || value > 50) {
						return FinanceBook.Result.fail("The house's cut is 0 to 50%.");
					}
					cutPercent = value;
				}
				default -> {
				}
			}
		}
		minBet = min;
		maxBet = max;
		bankHouse = bank;
		betSeconds = seconds;
		plinkoRisks = risks;
		lotteryDays = days;
		if (game() == CasinoBlock.Game.DERBY) {
			derbyCut = cutPercent;
		} else {
			lotteryCut = cutPercent;
		}
		setChanged();
		return new FinanceBook.Result(true, "Limits now " + FinanceBook.amount(minBet()) + " to " + FinanceBook.amount(maxBet())
				+ "; the house is " + (bankHouse ? "the server's central bank." : "you."));
	}

	public long cover() {
		if (bankHouse) {
			return book().bankGameCover();
		}
		return book().houseCover(owner, FinanceService.day(((ServerLevel) level).getServer()));
	}

	/** The house: the owner's Jugs, through the Exchange's book. */
	private final class House implements CasinoHouse {
		@Override
		public String stake(UUID player, long amount) {
			if (owner == null) {
				return "This game has no owner.";
			}
			MinecraftServer mc = ((ServerLevel) level).getServer();
			Ledger ledger = Ledger.get(mc);
			FinanceBook.Result result = ledger.book().stake(player, amount, FinanceService.day(mc));
			ledger.setDirty();
			setChanged();
			if (result.ok()) {
				soundForOthers(player, game().machine() ? SOUND_LEVER : SOUND_CHIP, 0);
			}
			return result.ok() ? null : result.message();
		}

		@Override
		public void settle(UUID player, String name, long stake, long payout, String what) {
			MinecraftServer mc = ((ServerLevel) level).getServer();
			Ledger ledger = Ledger.get(mc);
			// An owner playing their own game plays against the central bank (the reserve is the house).
			UUID house = houseFor(player);
			long lent = ledger.book().settleStake(player, house, stake, payout, FinanceService.day(mc), what);
			if (payout > stake) {
				soundForOthers(player, what != null ? SOUND_BIG_WIN : SOUND_COINS, revealTicks());
			}
			if (house != null) {
				// Takings are the owner's own money: bets played against the bank (the owner's own, or all of them
				// while the bank is the house) are not counted.
				count(stake, payout);
			}
			ledger.setDirty();
			setChanged();
			if (what != null || lent > 0) {
				FinanceService.announce(mc, ledger);
			}
		}

		@Override
		public void refund(UUID player, long amount) {
			if (amount <= 0) {
				return;
			}
			MinecraftServer mc = ((ServerLevel) level).getServer();
			Ledger ledger = Ledger.get(mc);
			ledger.book().refundStake(player, amount, FinanceService.day(mc));
			ledger.setDirty();
			setChanged();
		}

		@Override
		public long cover(UUID player) {
			if (player.equals(owner)) {
				return book().bankGameCover();
			}
			return CasinoBlockEntity.this.cover();
		}

		@Override
		public long minBet() {
			return CasinoBlockEntity.this.minBet();
		}

		@Override
		public long maxBet() {
			return CasinoBlockEntity.this.maxBet();
		}
	}

	// ------------------------------------------------------------------ watching

	void open(ServerPlayer player) {
		book().account(player.getUUID(), player.getName().getString());
		watchers.put(player.getUUID(), ticks);
		FinanceService.sendCasino(player, this, true, null);
	}

	void watch(ServerPlayer player) {
		watchers.put(player.getUUID(), ticks);
	}

	void unwatch(ServerPlayer player) {
		watchers.remove(player.getUUID());
		if (slot != null) {
			slot.release(player.getUUID());
		}
		if (arcade != null) {
			arcade.release(player.getUUID());
		}
		if (plinko != null) {
			plinko.release(player.getUUID());
		}
	}

	// ------------------------------------------------------------------ ticking

	void serverTick(ServerLevel level) {
		ticks++;
		if (!sounds.isEmpty()) {
			playSounds(level);
		}
		MinecraftServer mc = level.getServer();
		if (!owed.isEmpty()) {
			Ledger ledger = Ledger.get(mc);
			owed.forEach((id, amount) -> ledger.book().refundStake(id, amount, FinanceService.day(mc)));
			owed.clear();
			ledger.setDirty();
			setChanged();
		}
		FinanceBook book = Ledger.get(mc).book();
		int betTicks = (int) (betSeconds > 0 ? betSeconds : Math.max(5, ExchangeSettings.value(book, ExchangeSettings.CASINO_BET_SECONDS))) * 20;
		int turnTicks = (int) Math.max(5, ExchangeSettings.value(book, ExchangeSettings.CASINO_TURN_SECONDS)) * 20;
		switch (game()) {
			case BLACKJACK -> {
				blackjack().betTicks = betTicks;
				blackjack().turnTicks = turnTicks;
				blackjack().tick();
			}
			case ROULETTE -> {
				roulette().betTicks = betTicks;
				roulette().tick();
			}
			case CRAPS -> {
				craps().betTicks = betTicks;
				craps().tick();
			}
			case ARCADE -> arcade().tick();
			case PLINKO -> {
				plinko().allowedRisks = plinkoRisks;
				plinko().tick();
			}
			case PRIZE_WHEEL -> {
				bigSix().betTicks = betTicks;
				bigSix().tick();
			}
			case DERBY -> {
				derby().betTicks = betTicks;
				derby().cutPercent = derbyCut;
				derby().tick(level.getGameTime());
				if (derby().phase != derbyShown) {
					// Tell nearby players: their renderer runs the race from the seed, order and start time.
					derbyShown = derby().phase;
					setChanged();
					level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
				}
			}
			case BACCARAT -> {
				baccarat().betTicks = betTicks;
				baccarat().tick();
			}
			case LOTTERY -> {
				if (ticks % 100 == 0) {
					lotteryDraw(mc);
				}
			}
			case COIN_PUSHER -> {
			}
			default -> {
				if (slot() != null) {
					slot().tick();
				}
			}
		}
		if (ticks % 20 == 0) {
			checkAway(mc);
		}
		String state = FinanceService.casinoSignature(this);
		if (!state.equals(shown)) {
			shown = state;
			for (UUID id : watchers.keySet()) {
				ServerPlayer player = mc.getPlayerList().getPlayer(id);
				if (player != null) {
					FinanceService.sendCasino(player, this, false, null);
				}
			}
		}
	}

	private boolean near(ServerPlayer player) {
		return player != null && player.level() == level
				&& player.distanceToSqr(worldPosition.getX() + 1, worldPosition.getY(), worldPosition.getZ() + 1) <= REACH_SQ;
	}

	/** Players who walk away or log off leave the game: bets not yet in play come back to them. */
	private void checkAway(MinecraftServer mc) {
		watchers.entrySet().removeIf(entry -> !near(mc.getPlayerList().getPlayer(entry.getKey())));
		switch (game()) {
			case BLACKJACK -> {
				for (Blackjack.Seat seat : blackjack().seats) {
					if (!seat.occupied()) {
						continue;
					}
					if (near(mc.getPlayerList().getPlayer(seat.player))) {
						seat.away = 0;
					} else if (++seat.away >= 3) {
						blackjack().away(seat.player);
					}
				}
			}
			case ROULETTE -> {
				for (Roulette.Bet bet : java.util.List.copyOf(roulette().bets)) {
					if (!near(mc.getPlayerList().getPlayer(bet.player()))) {
						roulette().away(bet.player());
					}
				}
			}
			case CRAPS -> {
				for (UUID id : java.util.List.copyOf(craps().players)) {
					if (!near(mc.getPlayerList().getPlayer(id))) {
						craps().away(id);
					}
				}
			}
			case ARCADE -> {
				if (arcade().player != null && !watchers.containsKey(arcade().player)) {
					arcade().release(arcade().player);
				}
			}
			case PLINKO -> {
				if (plinko().player != null && !watchers.containsKey(plinko().player)) {
					plinko().release(plinko().player);
				}
			}
			case PRIZE_WHEEL -> {
				for (ShowGames.BigSix.Bet bet : java.util.List.copyOf(bigSix().bets)) {
					if (!near(mc.getPlayerList().getPlayer(bet.player()))) {
						bigSix().away(bet.player());
					}
				}
			}
			case BACCARAT -> {
				for (ShowGames.Baccarat.Bet bet : java.util.List.copyOf(baccarat().bets)) {
					if (!near(mc.getPlayerList().getPlayer(bet.player()))) {
						baccarat().away(bet.player());
					}
				}
			}
			case DERBY -> {
				for (Derby.Bet bet : java.util.List.copyOf(derby().bets)) {
					if (!near(mc.getPlayerList().getPlayer(bet.player()))) {
						derby().away(bet.player());
					}
				}
			}
			case COIN_PUSHER, LOTTERY -> {
			}
			default -> {
				if (slot() != null && slot().player != null && !watchers.containsKey(slot().player)) {
					slot().release(slot().player);
				}
			}
		}
	}

	// ------------------------------------------------------------------ breaking and saving

	private Map<UUID, Long> outstanding() {
		return switch (game()) {
			case BLACKJACK -> blackjack == null ? Map.of() : blackjack.outstanding();
			case ROULETTE -> roulette == null ? Map.of() : roulette.outstanding();
			case CRAPS -> craps == null ? Map.of() : craps.outstanding();
			case PRIZE_WHEEL -> bigSix == null ? Map.of() : bigSix.outstanding();
			case BACCARAT -> baccarat == null ? Map.of() : baccarat.outstanding();
			case DERBY -> derby == null ? Map.of() : derby.outstanding();
			case ARCADE -> arcade == null || arcade.player == null || arcade.outstanding() == 0 ? Map.of()
					: Map.of(arcade.player, arcade.outstanding());
			default -> Map.of();
		};
	}

	/** Breaking the game calls everything off: every bet on it goes back. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level instanceof ServerLevel server) {
			switch (game()) {
				case BLACKJACK -> blackjack().cancelAll();
				case ROULETTE -> roulette().cancelAll();
				case CRAPS -> craps().cancelAll();
				case PRIZE_WHEEL -> bigSix().cancelAll();
				case BACCARAT -> baccarat().cancelAll();
				case DERBY -> derby().cancelAll();
				case ARCADE -> arcade().cancelAll();
				case COIN_PUSHER -> {
					// The coins on the shelf belong to the house.
					if (pusher().shelf > 0 && owner != null) {
						Ledger.get(server.getServer()).book().escrowTo(bankHouse ? null : owner, pusher().shelf);
						pusher().shelf = 0;
					}
				}
				case LOTTERY -> {
					// Tickets for a draw that will not happen come back.
					FinanceBook book = Ledger.get(server.getServer()).book();
					for (int i = 0; i < lottery.tickets.size(); i++) {
						book.escrowTo(lottery.tickets.get(i), lottery.paid.get(i));
					}
					lottery.tickets.clear();
					lottery.paid.clear();
					lottery.pot = 0;
				}
				default -> {
					if (slot != null && slot.player != null) {
						slot.release(slot.player);
					}
				}
			}
			Ledger.get(server.getServer()).setDirty();
			if (!owed.isEmpty()) {
				Ledger ledger = Ledger.get(server.getServer());
				owed.forEach((id, amount) -> ledger.book().refundStake(id, amount, FinanceService.day(server.getServer())));
				owed.clear();
				ledger.setDirty();
			}
		}
		super.preRemoveSideEffects(pos, state);
	}

	@Override
	public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
		return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		String id = input.getStringOr("owner", "");
		try {
			owner = id.isEmpty() ? null : UUID.fromString(id);
		} catch (IllegalArgumentException e) {
			owner = null;
		}
		ownerName = input.getStringOr("owner_name", "");
		minBet = input.getLongOr("min_bet", 0L);
		maxBet = input.getLongOr("max_bet", 0L);
		bankHouse = input.getBooleanOr("bank_house", false);
		betSeconds = input.getIntOr("bet_seconds", 0);
		plinkoRisks = input.getIntOr("plinko_risks", 7);
		lotteryDays = input.getIntOr("lottery_days", 0);
		lotteryCut = input.getIntOr("lottery_cut", 10);
		derbyCut = input.getIntOr("derby_cut", 15);
		if (game() == CasinoBlock.Game.DERBY) {
			Derby d = derby();
			String card = input.getStringOr("derby_card", "");
			String[] horses = card.isEmpty() ? new String[0] : card.split(";");
			if (horses.length == Derby.HORSES) {
				for (int h = 0; h < Derby.HORSES; h++) {
					String[] f = horses[h].split(",");
					try {
						d.names[h] = Math.floorMod(Integer.parseInt(f[0]), Derby.NAMES.length);
						d.ratings[h] = Integer.parseInt(f[1]);
						d.form[h] = f.length > 2 ? f[2] : "33333";
						d.order[h] = Math.floorMod(Integer.parseInt(f.length > 3 ? f[3] : String.valueOf(h)), Derby.HORSES);
					} catch (NumberFormatException e) {
						// keep the card made when the table was set up
					}
				}
			}
			d.raceNumber = input.getLongOr("derby_race", 0L);
			d.seed = input.getLongOr("derby_seed", 0L);
			d.startTime = input.getLongOr("derby_start", 0L);
			int phase = input.getIntOr("derby_phase", 0);
			d.phase = Derby.Phase.values()[Math.max(0, Math.min(Derby.Phase.values().length - 1, phase))];
			if (level == null || !level.isClientSide()) {
				// The world reopened: bets were handed back on saving, so a race in progress is called off.
				if (d.phase == Derby.Phase.RACING || d.phase == Derby.Phase.BETTING) {
					d.phase = Derby.Phase.IDLE;
				}
			}
		}
		takenIn = input.getLongOr("taken_in", 0L);
		paidOut = input.getLongOr("paid_out", 0L);
		biggestWin = input.getLongOr("biggest_win", 0L);
		betCount = input.getLongOr("bet_count", 0L);
		jackpotSaved = input.getLongOr("jackpot", -1L);
		if (slot != null && jackpotSaved >= 0) {
			slot.jackpot = jackpotSaved;
		}
		pusher().shelf = input.getLongOr("pusher_shelf", 0L);
		lottery.pot = input.getLongOr("lottery_pot", 0L);
		lottery.lastDraw = input.getLongOr("lottery_last", -1L);
		lottery.lastWinner = input.getStringOr("lottery_winner", "");
		lottery.lastPrize = input.getLongOr("lottery_prize", 0L);
		lottery.tickets.clear();
		lottery.paid.clear();
		lottery.names.clear();
		for (String entry : input.getStringOr("lottery_tickets", "").split(";")) {
			String[] parts = entry.split("=", 3);
			if (parts.length == 3) {
				try {
					UUID holder = UUID.fromString(parts[0]);
					lottery.tickets.add(holder);
					lottery.paid.add(Long.parseLong(parts[1]));
					lottery.names.put(holder, parts[2]);
				} catch (IllegalArgumentException ignored) {
					// skip a damaged ticket
				}
			}
		}
		owed.clear();
		for (String entry : input.getStringOr("owed", "").split(";")) {
			String[] parts = entry.split("=", 2);
			if (parts.length == 2) {
				try {
					owed.merge(UUID.fromString(parts[0]), Long.parseLong(parts[1]), Long::sum);
				} catch (IllegalArgumentException ignored) {
					// skip a damaged entry
				}
			}
		}
	}

	/** Bets in play are saved as owed: if the world next loads from this save, they go back to their players. */
	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putString("owner", owner == null ? "" : owner.toString());
		output.putString("owner_name", ownerName);
		output.putLong("min_bet", minBet);
		output.putLong("max_bet", maxBet);
		output.putInt("bet_seconds", betSeconds);
		output.putInt("plinko_risks", plinkoRisks);
		output.putInt("lottery_days", lotteryDays);
		output.putInt("lottery_cut", lotteryCut);
		output.putInt("derby_cut", derbyCut);
		if (derby != null) {
			StringBuilder card = new StringBuilder();
			for (int h = 0; h < Derby.HORSES; h++) {
				card.append(derby.names[h]).append(',').append(derby.ratings[h]).append(',').append(derby.form[h]).append(',')
						.append(derby.order[h]).append(';');
			}
			output.putString("derby_card", card.toString());
			output.putLong("derby_race", derby.raceNumber);
			output.putLong("derby_seed", derby.seed);
			output.putLong("derby_start", derby.startTime);
			output.putInt("derby_phase", derby.phase.ordinal());
		}
		output.putLong("taken_in", takenIn);
		output.putLong("paid_out", paidOut);
		output.putLong("biggest_win", biggestWin);
		output.putLong("bet_count", betCount);
		output.putBoolean("bank_house", bankHouse);
		if (slot != null) {
			output.putLong("jackpot", slot.jackpot);
		} else if (jackpotSaved >= 0) {
			output.putLong("jackpot", jackpotSaved);
		}
		output.putLong("pusher_shelf", pusher == null ? 0L : pusher.shelf);
		output.putLong("lottery_pot", lottery.pot);
		output.putLong("lottery_last", lottery.lastDraw);
		output.putString("lottery_winner", lottery.lastWinner);
		output.putLong("lottery_prize", lottery.lastPrize);
		StringBuilder tickets = new StringBuilder();
		for (int i = 0; i < lottery.tickets.size(); i++) {
			UUID holder = lottery.tickets.get(i);
			tickets.append(holder).append('=').append(lottery.paid.get(i)).append('=')
					.append(lottery.names.getOrDefault(holder, "?").replace(";", "").replace("=", "")).append(';');
		}
		output.putString("lottery_tickets", tickets.toString());
		Map<UUID, Long> save = new HashMap<>(owed);
		outstanding().forEach((id, amount) -> save.merge(id, amount, Long::sum));
		StringBuilder text = new StringBuilder();
		save.forEach((id, amount) -> text.append(id).append('=').append(amount).append(';'));
		output.putString("owed", text.toString());
	}
}
