package io.github.jimbozoomer.jugcraft.finance;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Runs a poker table's game ({@link PokerGame}) and moves its money. Chips are Jugs held in the Exchange's escrow:
 * sitting down holds the buy-in, standing up releases the player's chips, so the Exchange's money is never changed by
 * playing. Players who open the table's overlay are its watchers and get a fresh view whenever anything changes.
 *
 * <p>A seated player who goes offline or walks away is folded by the turn timer and stood up between hands, so their
 * chips are never stuck. If the world closes in the middle of a hand, the hand is called off when the table loads:
 * every chip goes back to whoever put it in. Breaking the table cashes everyone out the same way.
 */
public class PokerTableBlockEntity extends BlockEntity {
	/** Watchers further away than this, or seated players away for longer than this, are let go. */
	private static final double REACH_SQ = 16 * 16;
	private static final int AWAY_TICKS = 20 * 60;

	private final PokerGame game;
	/** Players with the overlay open, and the tick they last asked for it. */
	private final Map<UUID, Long> watchers = new HashMap<>();
	/** Chips owed to players who left during a hand that was later called off, paid out on the next tick. */
	private final Map<UUID, Long> owed = new HashMap<>();
	private final Map<UUID, String> owedNames = new HashMap<>();
	private String shown = "";
	private long ticks;
	private boolean loadedMidHand;
	/** Who placed the table: they set its blinds, buy-ins and clock. Tables placed before owners were kept have none. */
	private UUID owner;
	private String ownerName = "";
	/** The owner's settings; 0 means the server's. */
	private long ownSmallBlind;
	private long ownBigBlind;
	private long ownMinBuyIn;
	private long ownMaxBuyIn;
	private int ownTurnSeconds;

	public PokerTableBlockEntity(BlockPos pos, BlockState state) {
		super(ExchangeBlocks.POKER_TABLE_ENTITY, pos, state);
		this.game = new PokerGame(this::cashOut, new Random());
	}

	public PokerGame game() {
		return game;
	}

	public PokerTableBlock.Design design() {
		return getBlockState().getBlock() instanceof PokerTableBlock block ? block.design() : PokerTableBlock.Design.OVAL;
	}

	// ------------------------------------------------------------------ owner and settings

	void setOwner(ServerPlayer player) {
		owner = player.getUUID();
		ownerName = player.getName().getString();
		setChanged();
	}

	public boolean isOwner(ServerPlayer player) {
		return owner != null && owner.equals(player.getUUID());
	}

	public String ownerName() {
		return ownerName;
	}

	public long adminMinBuyIn(FinanceBook book) {
		return Math.max(1, ExchangeSettings.value(book, ExchangeSettings.POKER_MIN_BUYIN));
	}

	public long adminMaxBuyIn(FinanceBook book) {
		return Math.max(adminMinBuyIn(book), ExchangeSettings.value(book, ExchangeSettings.POKER_MAX_BUYIN));
	}

	/** The least buy-in here: the owner's, kept within the server's range. */
	public long minBuyIn(FinanceBook book) {
		long min = ownMinBuyIn > 0 ? ownMinBuyIn : adminMinBuyIn(book);
		return Math.max(adminMinBuyIn(book), Math.min(adminMaxBuyIn(book), min));
	}

	public long maxBuyIn(FinanceBook book) {
		long max = ownMaxBuyIn > 0 ? ownMaxBuyIn : adminMaxBuyIn(book);
		return Math.max(minBuyIn(book), Math.min(adminMaxBuyIn(book), max));
	}

	public long bigBlind(FinanceBook book) {
		long big = ownBigBlind > 0 ? ownBigBlind : ExchangeSettings.value(book, ExchangeSettings.POKER_BIG_BLIND);
		return Math.max(1, big);
	}

	public long smallBlind(FinanceBook book) {
		long small = ownSmallBlind > 0 ? ownSmallBlind : ExchangeSettings.value(book, ExchangeSettings.POKER_SMALL_BLIND);
		return Math.max(1, Math.min(bigBlind(book), small));
	}

	public int turnSeconds(FinanceBook book) {
		return (int) Math.max(10, ownTurnSeconds > 0 ? ownTurnSeconds : ExchangeSettings.value(book, ExchangeSettings.POKER_TURN_SECONDS));
	}

	/** {owner's small blind, big blind, least and most buy-in, turn seconds (each 0 for the server's), server least and most buy-in}. */
	public long[] ownSettings(FinanceBook book) {
		return new long[] {ownSmallBlind, ownBigBlind, ownMinBuyIn, ownMaxBuyIn, ownTurnSeconds, adminMinBuyIn(book), adminMaxBuyIn(book)};
	}

	/** The owner changes the table's blinds, buy-ins and clock; new blinds start with the next hand. 0 means the server's. */
	PokerGame.Result configure(ServerPlayer player, long small, long big, long minBuy, long maxBuy, int turn) {
		if (owner == null) {
			return PokerGame.Result.fail("This table was placed before tables had owners: break it and place it again to set it up.");
		}
		if (!isOwner(player)) {
			return PokerGame.Result.fail("Only " + ownerName + " changes this table's settings.");
		}
		FinanceBook book = Ledger.get(player.level().getServer()).book();
		if (small < 0 || big < 0 || minBuy < 0 || maxBuy < 0) {
			return PokerGame.Result.fail("Settings cannot be negative.");
		}
		long effBig = big > 0 ? big : ExchangeSettings.value(book, ExchangeSettings.POKER_BIG_BLIND);
		long effSmall = small > 0 ? small : ExchangeSettings.value(book, ExchangeSettings.POKER_SMALL_BLIND);
		if (effSmall > effBig) {
			return PokerGame.Result.fail("The small blind cannot be bigger than the big blind.");
		}
		long lo = adminMinBuyIn(book);
		long hi = adminMaxBuyIn(book);
		long effMin = minBuy > 0 ? minBuy : lo;
		long effMax = maxBuy > 0 ? maxBuy : hi;
		if (effMin < lo || effMax > hi || effMin > effMax) {
			return PokerGame.Result.fail("Buy-ins here must lie within the server's " + lo + " to " + hi + ", least first.");
		}
		if (effBig > effMin) {
			return PokerGame.Result.fail("The big blind must be no more than the least buy-in.");
		}
		if (turn != 0 && (turn < 10 || turn > 300)) {
			return PokerGame.Result.fail("The turn clock runs 10 to 300 seconds (or 0 for the server's).");
		}
		ownSmallBlind = small;
		ownBigBlind = big;
		ownMinBuyIn = minBuy;
		ownMaxBuyIn = maxBuy;
		ownTurnSeconds = turn;
		setChanged();
		return new PokerGame.Result(true, "Blinds " + smallBlind(book) + "/" + bigBlind(book) + " from the next hand; buy-ins " + minBuyIn(book)
				+ " to " + maxBuyIn(book) + "; " + turnSeconds(book) + " seconds a turn.");
	}

	// ------------------------------------------------------------------ money

	private void cashOut(UUID player, String name, long chips) {
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		MinecraftServer mc = server.getServer();
		Ledger ledger = Ledger.get(mc);
		if (chips > 0) {
			ledger.book().release(player, chips, FinanceService.day(mc), name + " cashed out " + ledger.book().format(chips)
					+ " from the poker table.");
		}
		ledger.setDirty();
		FinanceService.announce(mc, ledger);
		setChanged();
	}

	/** Seats a player, taking their buy-in from their Jugs. */
	PokerGame.Result sit(ServerPlayer player, int seat, long buyIn) {
		MinecraftServer mc = player.level().getServer();
		Ledger ledger = Ledger.get(mc);
		FinanceBook book = ledger.book();
		long min = minBuyIn(book);
		long max = maxBuyIn(book);
		if (buyIn < min || buyIn > max) {
			return PokerGame.Result.fail("Buy in for between " + min + " and " + max + " Jugs.");
		}
		if (seat < 0 || seat >= PokerGame.SEATS || game.seats[seat].occupied()) {
			return PokerGame.Result.fail("That seat is taken.");
		}
		if (game.seatOf(player.getUUID()) >= 0) {
			return PokerGame.Result.fail("You are already at this table.");
		}
		String name = player.getName().getString();
		FinanceBook.Result held = book.hold(player.getUUID(), buyIn, FinanceService.day(mc), name + " sat down at a poker table with "
				+ book.format(buyIn) + ".");
		if (!held.ok()) {
			return PokerGame.Result.fail(held.message());
		}
		ledger.setDirty();
		FinanceService.announce(mc, ledger);
		setChanged();
		return game.sit(seat, player.getUUID(), name, buyIn);
	}

	PokerGame.Result stand(ServerPlayer player) {
		setChanged();
		return game.stand(player.getUUID());
	}

	PokerGame.Result act(ServerPlayer player, PokerGame.Move move, long raiseTo) {
		setChanged();
		return game.act(player.getUUID(), move, raiseTo);
	}

	// ------------------------------------------------------------------ watching

	void open(ServerPlayer player) {
		watchers.put(player.getUUID(), ticks);
		FinanceService.sendPoker(player, this, true, null);
	}

	void watch(ServerPlayer player) {
		watchers.put(player.getUUID(), ticks);
	}

	void unwatch(ServerPlayer player) {
		watchers.remove(player.getUUID());
	}

	// ------------------------------------------------------------------ ticking

	void serverTick(ServerLevel level) {
		ticks++;
		MinecraftServer mc = level.getServer();
		if (loadedMidHand) {
			loadedMidHand = false;
			setChanged();
		}
		payOwed(mc);
		FinanceBook book = Ledger.get(mc).book();
		if (game.phase == PokerGame.Phase.WAITING) {
			game.bigBlind = bigBlind(book);
			game.smallBlind = smallBlind(book);
			game.turnTicks = turnSeconds(book) * 20;
		}
		game.tick();
		if (ticks % 20 == 0) {
			checkAway(mc);
		}
		String state = signature();
		if (!state.equals(shown)) {
			shown = state;
			setChanged();
			broadcast(mc);
		}
	}

	/** Seated players who are offline or far away are stood up between hands (their chips go back to their Jugs). */
	private void checkAway(MinecraftServer mc) {
		for (PokerGame.Seat seat : game.seats) {
			if (!seat.occupied()) {
				continue;
			}
			ServerPlayer player = mc.getPlayerList().getPlayer(seat.player);
			boolean here = player != null && player.level() == level
					&& player.distanceToSqr(worldPosition.getX() + 2, worldPosition.getY(), worldPosition.getZ() + 1) <= REACH_SQ * 4;
			seat.away = here ? 0 : seat.away + 20;
			if (seat.away >= AWAY_TICKS && !game.handRunning()) {
				game.stand(seat.player);
			}
		}
		watchers.entrySet().removeIf(entry -> {
			ServerPlayer player = mc.getPlayerList().getPlayer(entry.getKey());
			return player == null || player.level() != level
					|| player.distanceToSqr(worldPosition.getX() + 2, worldPosition.getY(), worldPosition.getZ() + 1) > REACH_SQ;
		});
	}

	private void broadcast(MinecraftServer mc) {
		for (UUID id : watchers.keySet()) {
			ServerPlayer player = mc.getPlayerList().getPlayer(id);
			if (player != null) {
				FinanceService.sendPoker(player, this, false, null);
			}
		}
	}

	/** Everything a watcher sees, as text: a change means a new view goes out. */
	private String signature() {
		StringBuilder out = new StringBuilder().append(game.phase).append(game.actor).append('/').append(game.handNumber)
				.append('/').append(game.currentBet).append('/').append(game.pot()).append('/').append(game.button)
				.append('/').append(game.timer / 20).append('/').append(game.log.size() > 0 ? game.log.get(game.log.size() - 1) : "");
		for (int card : game.board) {
			out.append(',').append(card);
		}
		for (PokerGame.Seat seat : game.seats) {
			out.append('|').append(seat.player).append(seat.stack).append(':').append(seat.bet).append(seat.folded)
					.append(seat.allIn).append(seat.shown);
		}
		return out.toString();
	}

	/** Records chips a called-off hand owes to players who already left (by seat, from {@link PokerGame#cancelHand}). */
	private void owe(long[] bySeat) {
		for (int i = 0; i < bySeat.length; i++) {
			if (bySeat[i] > 0 && game.contributor[i] != null) {
				owed.merge(game.contributor[i], bySeat[i], Long::sum);
				owedNames.put(game.contributor[i], game.contributorName[i] == null ? "A player" : game.contributorName[i]);
			}
		}
	}

	private void payOwed(MinecraftServer mc) {
		if (owed.isEmpty()) {
			return;
		}
		for (Map.Entry<UUID, Long> entry : owed.entrySet()) {
			cashOut(entry.getKey(), owedNames.getOrDefault(entry.getKey(), "A player"), entry.getValue());
		}
		owed.clear();
		owedNames.clear();
	}

	// ------------------------------------------------------------------ breaking

	/** Breaking the table calls off any hand and cashes every player out. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level instanceof ServerLevel server) {
			if (game.handRunning()) {
				owe(game.cancelHand());
			}
			payOwed(server.getServer());
			for (PokerGame.Seat seat : game.seats) {
				if (seat.occupied()) {
					game.stand(seat.player);
				}
			}
		}
		super.preRemoveSideEffects(pos, state);
	}

	// ------------------------------------------------------------------ saving

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		for (int i = 0; i < PokerGame.SEATS; i++) {
			PokerGame.Seat seat = game.seats[i];
			String id = input.getStringOr("seat" + i, "");
			seat.clearHand();
			seat.player = null;
			seat.stack = 0;
			if (!id.isEmpty()) {
				try {
					seat.player = UUID.fromString(id);
					seat.name = input.getStringOr("name" + i, "?");
					seat.stack = input.getLongOr("stack" + i, 0L);
				} catch (IllegalArgumentException ignored) {
					seat.player = null;
				}
			}
		}
		owed.clear();
		for (String entry : input.getStringOr("owed", "").split(";")) {
			String[] parts = entry.split("=", 3);
			if (parts.length == 3) {
				try {
					owed.merge(UUID.fromString(parts[0]), Long.parseLong(parts[1]), Long::sum);
					owedNames.put(UUID.fromString(parts[0]), parts[2]);
				} catch (IllegalArgumentException ignored) {
					// skip a damaged entry
				}
			}
		}
		game.button = input.getIntOr("button", -1);
		String ownerId = input.getStringOr("owner", "");
		try {
			owner = ownerId.isEmpty() ? null : UUID.fromString(ownerId);
		} catch (IllegalArgumentException e) {
			owner = null;
		}
		ownerName = input.getStringOr("owner_name", "");
		ownSmallBlind = input.getLongOr("small_blind", 0L);
		ownBigBlind = input.getLongOr("big_blind", 0L);
		ownMinBuyIn = input.getLongOr("min_buyin", 0L);
		ownMaxBuyIn = input.getLongOr("max_buyin", 0L);
		ownTurnSeconds = input.getIntOr("turn_seconds", 0);
		loadedMidHand = true;
	}

	/** Saves seats and stacks. A hand in progress is saved as called off: chips in the pot go back to their owners. */
	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		// Chips in the pot from players who already left go back to them when the table next loads.
		Map<UUID, Long> saveOwed = new HashMap<>(owed);
		Map<UUID, String> saveNames = new HashMap<>(owedNames);
		for (int i = 0; i < PokerGame.SEATS; i++) {
			UUID who = game.contributor[i];
			if (game.contributed[i] > 0 && who != null && !who.equals(game.seats[i].player)) {
				saveOwed.merge(who, game.contributed[i], Long::sum);
				saveNames.put(who, game.contributorName[i] == null ? "A player" : game.contributorName[i]);
			}
		}
		StringBuilder owedText = new StringBuilder();
		saveOwed.forEach((id, chips) -> owedText.append(id).append('=').append(chips).append('=')
				.append(saveNames.getOrDefault(id, "A player").replace(";", "").replace("=", "")).append(';'));
		for (int i = 0; i < PokerGame.SEATS; i++) {
			PokerGame.Seat seat = game.seats[i];
			long inPot = game.contributed[i];
			if (seat.occupied()) {
				boolean theirs = seat.player.equals(game.contributor[i]);
				output.putString("seat" + i, seat.player.toString());
				output.putString("name" + i, seat.name);
				output.putLong("stack" + i, seat.stack + (theirs ? inPot : 0));
			} else {
				output.putString("seat" + i, "");
				output.putLong("stack" + i, 0L);
			}
		}
		output.putString("owed", owedText.toString());
		output.putInt("button", game.button);
		output.putString("owner", owner == null ? "" : owner.toString());
		output.putString("owner_name", ownerName);
		output.putLong("small_blind", ownSmallBlind);
		output.putLong("big_blind", ownBigBlind);
		output.putLong("min_buyin", ownMinBuyIn);
		output.putLong("max_buyin", ownMaxBuyIn);
		output.putInt("turn_seconds", ownTurnSeconds);
	}
}
