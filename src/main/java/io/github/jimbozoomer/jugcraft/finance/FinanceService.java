package io.github.jimbozoomer.jugcraft.finance;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;

/**
 * Server side of the Exchange: answers players' requests, runs the daily cycle and writes the chat ledger. Everything
 * here runs on the server thread and re-checks what the client sent.
 */
public final class FinanceService {
	/** Minecraft days are 24,000 ticks of world time (game time: sleeping and /time set do not skip it). */
	public static final long TICKS_PER_DAY = 24_000L;
	/** The daily cycle is checked this often. */
	public static final int CHECK_INTERVAL = 100;
	/**
	 * Milliseconds between two actions from one player, and between two refreshes. Real time, not ticks: a
	 * singleplayer world is paused while the Exchange is open, so its tick count stands still.
	 */
	static final long ACTION_COOLDOWN_MS = 150;
	static final long REFRESH_COOLDOWN_MS = 900;
	static final int HISTORY_LINES = 12;

	private static final Map<UUID, Long> LAST_ACTION = new HashMap<>();

	private FinanceService() {
	}

	/** The economy's day: world game time in days, plus any days the operators closed early. */
	public static long day(MinecraftServer server) {
		return server.overworld().getGameTime() / TICKS_PER_DAY + Ledger.get(server).book().dayOffset();
	}

	/** Operators (game masters and above) run the central bank. */
	public static boolean isAdmin(Player player) {
		return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
	}

	// ------------------------------------------------------------------ events

	static void onJoin(ServerPlayer player, MinecraftServer server) {
		Ledger ledger = Ledger.get(server);
		FinanceBook book = ledger.book();
		book.account(player.getUUID(), player.getName().getString());
		ledger.setDirty();
        // Welcome books are for clients that can read the Exchange. Do not inject items into
        // automation/mock player inventories that have no compatible client connection.
        if (ServerPlayNetworking.canSend(player,FinanceSnapshotPayload.TYPE)) ExchangeGuide.giveOnce(player);
		if (book.starterAvailable(player.getUUID())) {
			player.sendSystemMessage(Component.literal("[Exchange] ").withStyle(ChatFormatting.GOLD).append(Component.literal(
					"Welcome! The central bank offers you a starter loan of " + book.format(book.borrowLimit())
					+ " to buy your first goods. Use /jugcraft exchange to open the Exchange (default key: semicolon).").withStyle(ChatFormatting.YELLOW)));
		} else if (book.overdue(player.getUUID(), day(server))) {
			player.sendSystemMessage(Component.literal("[Exchange] ").withStyle(ChatFormatting.GOLD).append(Component.literal(
					"Your loan is overdue. Sell on the market to repay it: until then you cannot buy.").withStyle(ChatFormatting.RED)));
		}
	}

	static void onLeave(ServerPlayer player) {
		LAST_ACTION.remove(player.getUUID());
	}

	static void tick(MinecraftServer server) {
		releaseShouts(server);
		if (server.getTickCount() % CHECK_INTERVAL != 0) {
			return;
		}
		runDays(server);
	}

	/** Runs any days that have passed: loan interest, contract payments, auction closes and the inflation measure. */
	static void runDays(MinecraftServer server) {
		Ledger ledger = Ledger.get(server);
		long day = day(server);
        if (ledger.book().payPrizeDebts(day)) { ledger.setDirty(); announce(server,ledger); }
		if (ledger.book().advanceTo(day) > 0) {
			Market.get(server).newDay(server, ledger.book(), day);
			LandRegistry.get(server).newDay(ledger.book(), day);
			ledger.setDirty();
			announce(server, ledger);
		}
	}

	// ------------------------------------------------------------------ requests

	static void handle(FinanceActionPayload request, ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		if (server == null || !player.isAlive() || player.isSpectator()) {
			return;
		}
		if (request.action() == FinanceActionPayload.Action.POKER_CLOSE) {
			// Closing a table's overlay is never too fast: stop sending it views.
			pokerAction(player, request);
			return;
		}
		if (request.action() == FinanceActionPayload.Action.CASINO_CLOSE) {
			casinoAction(player, request);
			return;
		}
		long now = System.currentTimeMillis();
		Long last = LAST_ACTION.get(player.getUUID());
		// A Market Chest's own refresh is a refresh too, so it never makes a click right after it "too fast".
		boolean refresh = request.action() == FinanceActionPayload.Action.REFRESH
				|| request.action() == FinanceActionPayload.Action.CHEST_INFO
				|| request.action() == FinanceActionPayload.Action.POKER_VIEW
				|| request.action() == FinanceActionPayload.Action.POKER_CLOSE
				|| request.action() == FinanceActionPayload.Action.CASINO_VIEW;
		long cooldown = refresh ? REFRESH_COOLDOWN_MS : ACTION_COOLDOWN_MS;
		if (last != null && now - last >= 0 && now - last < cooldown) {
			return;
		}
		LAST_ACTION.put(player.getUUID(), now);

		Ledger ledger = Ledger.get(server);
		FinanceBook book = ledger.book();
		long day = day(server);
		book.account(player.getUUID(), player.getName().getString());
		Market market = Market.get(server);
		FinanceBook.Result result = switch (request.action()) {
			case OPEN, REFRESH -> null;
			case BORROW -> book.borrow(player.getUUID(), request.amount(), day);
			case REPAY -> book.repay(player.getUUID(), request.amount(), day);
			case PAY -> book.pay(player.getUUID(), request.target(), request.amount(),
					FinanceBook.Period.byId(request.period()), request.note(), day);
			case CANCEL_CONTRACT -> book.cancel(player.getUUID(), request.amount(), day);
			case ADMIN_FEE -> isAdmin(player) ? book.setFee((int) Math.max(-1, Math.min(Integer.MAX_VALUE, request.amount())), day) : denied();
			case ADMIN_TERM -> isAdmin(player) ? book.setTerm((int) Math.max(-1, Math.min(Integer.MAX_VALUE, request.amount())), day) : denied();
			case STARTER_LOAN -> book.starterLoan(player.getUUID(), day);
			case LAND_BUY -> LandRegistry.get(server).buy(player, book, request.amount(), request.extra(), day);
			case LAND_SELL -> LandRegistry.get(server).sell(player, book, request.amount(), request.extra(), day);
			case LAND_ABANDON -> LandRegistry.get(server).abandon(player, book, request.amount(), isAdmin(player), day);
			case LAND_TRUST -> LandRegistry.get(server).trust(player, book, request.target(), request.period() == 1);
			case LAND_ZONE -> LandRegistry.get(server).zone(player, book, request.amount(), request.extra(), day);
			case CORP_CHARTER -> Equities.get(server).charter(player, book, request.target(), request.note(), request.amount(),
					request.period(), request.extra(), logo(request.data()), uploaded(request.data()), day);
			case CORP_ORDER -> Equities.get(server).order(player, book, request.amount(), request.period() == 1,
					parseLong(request.note()), request.extra(), day);
			case CORP_CANCEL -> Equities.get(server).cancel(player, book, request.amount(), isAdmin(player), day);
			case CORP_DIVIDEND -> Equities.get(server).dividend(player, book, request.amount(), request.extra(), day);
			case CORP_LOGO -> Equities.get(server).setLogo(player, request.amount(), logo(request.data()), uploaded(request.data()));
			case ADMIN_LOGO -> isAdmin(player) ? Equities.get(server).approveLogo(request.amount(), request.period() == 1) : denied();
			case ADMIN_LIMIT -> isAdmin(player) ? book.setLimit(request.amount(), day) : denied();
			case LIST_ITEM -> market.list(player, book, quantity(request.extra()), request.amount(), request.period() == 1,
					parseIndex(request.target()), parseLong(request.note()), day);
			case BUY -> market.buy(player, book, request.amount(), quantity(request.extra()), day);
			case BID -> market.bid(player, book, request.amount(), request.extra(), day);
			case CANCEL_LISTING -> market.cancelListing(player, book, request.amount(), isAdmin(player), day);
			case COLLECT -> market.collect(player);
			case POST_REQUEST -> market.postRequest(player, book, request.target(), quantity(request.extra()), request.amount(), day);
			case FILL_REQUEST -> market.fill(player, book, request.amount(), quantity(request.extra()), day);
			case CANCEL_REQUEST -> market.cancelRequest(player, book, request.amount(), isAdmin(player), day);
			case ADMIN_SET -> isAdmin(player) ? ExchangeSettings.set(server, request.target(), request.note(), day) : denied();
			case CORP_ISSUE -> Equities.get(server).issue(player, book, request.amount(), parseLong(request.note()),
					request.extra(), day);
			case LAND_AUCTION -> LandRegistry.get(server).auction(player, book, request.amount(), request.extra(),
					request.period(), day);
			case LAND_BID -> LandRegistry.get(server).bid(player, book, request.amount(), request.extra(), day);
			case CHEST_BUY -> ExchangeBlocks.buyChest(player, book, request.amount(), day);
			case POKER_VIEW, POKER_CLOSE, POKER_SIT, POKER_STAND, POKER_ACT, POKER_SETTINGS -> {
				pokerAction(player, request);
				yield null;
			}
			case CASINO_VIEW, CASINO_CLOSE, CASINO_BET, CASINO_ACT, CASINO_LIMITS -> {
				casinoAction(player, request);
				yield null;
			}
			case CHEST_INFO, CHEST_PRICE, CHEST_PAUSE, CHEST_TAKE_BACK, CHEST_COMPANY -> {
				chestAction(player, request, server);
				yield null;
			}
		};
		if (chestPanel(request.action()) || request.action().name().startsWith("POKER_")
				|| request.action().name().startsWith("CASINO_")) {
			ledger.setDirty();
			announce(server, ledger);
			return;
		}
		ledger.setDirty();
		announce(server, ledger);
		send(player, ledger, request.action() == FinanceActionPayload.Action.OPEN, result);
	}

	static boolean canAccess(ServerPlayer player, net.minecraft.core.BlockPos pos, double radiusSquared) {
        return player.isAlive() && !player.isSpectator()
                && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) <= radiusSquared
                && player.level().isInWorldBounds(pos) && player.level().hasChunkAt(pos)
                && LandRegistry.mayBuild(player, player.level(), pos);
    }

	private static int quantity(long requested) {
		return (int) Math.max(0, Math.min(Market.MAX_QUANTITY, requested));
	}

	private static int parseIndex(String text) {
		try {
			return Integer.parseInt(text.trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static byte[] logo(byte[] data) {
		return java.util.Arrays.copyOf(data, Math.min(data.length, Equities.LOGO_PIXELS));
	}

	private static boolean uploaded(byte[] data) {
		return data.length > Equities.LOGO_PIXELS && data[Equities.LOGO_PIXELS] == 1;
	}

	private static long parseLong(String text) {
		try {
			return Long.parseLong(text.trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static FinanceBook.Result denied() {
		return new FinanceBook.Result(false, "Only the server's operators run the central bank.");
	}

	// ------------------------------------------------------------------ market chests

	// ------------------------------------------------------------------ poker

	/** Runs a poker table request from its overlay, then sends the player the table's view. */
	private static void pokerAction(ServerPlayer player, FinanceActionPayload request) {
		net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.of(request.extra());
		if (!canAccess(player, pos, 256) || !(player.level().getBlockEntity(pos) instanceof PokerTableBlockEntity table)
				|| player.distanceToSqr(pos.getX() + 2, pos.getY(), pos.getZ() + 1) > 16 * 16) {
			return;
		}
		PokerGame.Result result = switch (request.action()) {
			case POKER_VIEW -> {
				table.watch(player);
				yield null;
			}
			case POKER_CLOSE -> {
				table.unwatch(player);
				yield null;
			}
			case POKER_SIT -> table.sit(player, request.period(), request.amount());
			case POKER_STAND -> table.stand(player);
			case POKER_SETTINGS -> {
				String[] parts = request.target().split(",");
				if (parts.length != 4) {
					yield PokerGame.Result.fail("Fill in every setting.");
				}
				long[] v = new long[4];
				for (int i = 0; i < 4; i++) {
					v[i] = parseLong(parts[i].trim());
				}
				yield table.configure(player, request.amount(), v[0], v[1], v[2], (int) Math.min(Integer.MAX_VALUE, v[3]));
			}
			case POKER_ACT -> {
				int move = request.period();
				yield move < 0 || move >= PokerGame.Move.values().length ? null
						: table.act(player, PokerGame.Move.values()[move], request.amount());
			}
			default -> null;
		};
		if (request.action() != FinanceActionPayload.Action.POKER_CLOSE) {
			sendPoker(player, table, false, result == null || result.message().isEmpty() ? null
					: new FinanceBook.Result(result.ok(), result.message()));
		}
	}

	/** Sends a player their view of a poker table: their own cards, and others' only once shown. */
	static void sendPoker(ServerPlayer player, PokerTableBlockEntity table, boolean open, FinanceBook.Result result) {
		MinecraftServer server = player.level().getServer();
		FinanceBook book = Ledger.get(server).book();
		PokerGame game = table.game();
		UUID id = player.getUUID();
		int mine = game.seatOf(id);
		boolean showdown = game.phase == PokerGame.Phase.SHOWDOWN;
		List<PokerTablePayload.SeatView> seats = new ArrayList<>();
		for (int i = 0; i < PokerGame.SEATS; i++) {
			PokerGame.Seat seat = game.seats[i];
			boolean see = i == mine || (showdown && seat.shown);
			seats.add(new PokerTablePayload.SeatView(seat.occupied() ? FinanceBook.clean(seat.name, 32) : "", seat.stack, seat.bet,
					seat.inHand, seat.folded, seat.allIn, see ? seat.card1 : (seat.inHand && !seat.folded ? -2 : -1),
					see ? seat.card2 : (seat.inHand && !seat.folded ? -2 : -1), i == mine));
		}
		long toCall = mine >= 0 && game.handRunning() ? game.toCall(mine) : 0;
		long maxTo = mine >= 0 ? game.seats[mine].bet + game.seats[mine].stack : 0;
		ServerPlayNetworking.send(player, new PokerTablePayload(table.getBlockPos().asLong(), table.design().ordinal(), open,
				game.phase.ordinal(), game.handNumber, seats, game.board.clone(), game.pot(), game.currentBet, toCall,
				Math.min(game.minRaiseTo(), maxTo), maxTo, game.actor, game.button, mine, game.actor >= 0 ? game.timer : 0,
				game.smallBlind, game.bigBlind, table.minBuyIn(book), table.maxBuyIn(book), book.cash(id), new ArrayList<>(game.log),
				result == null ? "" : FinanceBook.clean(result.message(), 150), result != null && result.ok(),
				FinanceBook.clean(table.ownerName(), 32), table.isOwner(player), table.turnSeconds(book), table.ownSettings(book)));
	}

	// ------------------------------------------------------------------ casino games

	/** Runs a casino game request from its screen, then sends the player the game's view. */
	private static void casinoAction(ServerPlayer player, FinanceActionPayload request) {
		net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.of(request.extra());
		if (!canAccess(player, pos, 256) || !(player.level().getBlockEntity(pos) instanceof CasinoBlockEntity casino)
				|| player.distanceToSqr(pos.getX() + 1, pos.getY(), pos.getZ() + 1) > 16 * 16) {
			return;
		}
		UUID id = player.getUUID();
		String name = player.getName().getString();
		long amount = request.amount();
		int move = request.period();
		long detail = parseLong(request.target());
		FinanceBook.Result result = switch (request.action()) {
			case CASINO_VIEW -> {
				casino.watch(player);
				yield null;
			}
			case CASINO_CLOSE -> {
				casino.unwatch(player);
				yield null;
			}
			case CASINO_LIMITS -> move == 2 ? casino.resetTakings(player) : casino.setLimits(player, amount, detail, move == 1, request.note());
			case CASINO_BET -> switch (casino.game()) {
				case BLACKJACK -> casino.blackjack().bet(id, name, move, amount);
				case ROULETTE -> casino.roulette().bet(id, name, detail, amount);
				case CRAPS -> move < 0 || move >= Craps.Kind.values().length ? null
						: casino.craps().bet(id, name, Craps.Kind.values()[move], (int) detail, amount);
				case PRIZE_WHEEL -> casino.bigSix().bet(id, name, move, amount);
				case BACCARAT -> casino.baccarat().bet(id, name, move, amount);
				case DERBY -> casino.derby().bet(id, name, move, (int) detail, amount);
				default -> null;
			};
			case CASINO_ACT -> switch (casino.game()) {
				case BLACKJACK -> move < 0 || move >= Blackjack.Move.values().length ? null
						: casino.blackjack().act(id, Blackjack.Move.values()[move]);
				case ROULETTE -> move == 1 ? casino.roulette().spinNow(id) : casino.roulette().clear(id);
				case CRAPS -> switch (move) {
					case 0 -> casino.craps().roll(id);
					case 1 -> casino.craps().odds(id, (int) detail, amount);
					default -> casino.craps().takeDown(id, (int) detail);
				};
				case ARCADE -> arcadeAction(casino.arcade(), id, name, move, request.target(), detail, amount);
				case PLINKO -> casino.plinko().drop(id, name, amount, (int) detail);
				case PRIZE_WHEEL -> move == 1 ? casino.bigSix().spinNow() : casino.bigSix().clear(id);
				case BACCARAT -> move == 1 ? casino.baccarat().dealNow() : casino.baccarat().clear(id);
				case DERBY -> move == 1 ? casino.derby().startNow(id, player.level().getGameTime()) : casino.derby().clear(id);
				case COIN_PUSHER -> casino.pusherDrop(player, amount);
				case LOTTERY -> move == 1 ? casino.buyScratch(player, (int) detail) : casino.buyTickets(player, (int) detail);
				default -> casino.slot() == null ? null : switch (move) {
					case 1 -> casino.slot().nudge(id, name, (int) detail);
					case 2 -> casino.slot().collect(id);
					default -> casino.slot().spin(id, name, amount, ExchangeSettings.value(Ledger.get(player.level().getServer()).book(),
							ExchangeSettings.JACKPOT_SEED), (int) detail);
				};
			};
			default -> null;
		};
		if (request.action() != FinanceActionPayload.Action.CASINO_CLOSE) {
			sendCasino(player, casino, false, result == null || result.message().isEmpty() ? null : result);
		}
	}

	/** The arcade cabinet's moves (see {@link FinanceActionPayload.Action#CASINO_ACT}). */
	private static FinanceBook.Result arcadeAction(ArcadeGame game, UUID id, String name, int move, String target, long detail,
			long amount) {
		return switch (move) {
			case 10 -> detail < 0 || detail >= ArcadeGame.Mode.values().length ? null
					: game.setMode(id, name, ArcadeGame.Mode.values()[(int) detail]);
			case 11 -> game.deal(id, name, amount);
			case 12 -> game.draw(id, (int) detail);
			case 13 -> {
				String[] parts = target.split(",", 2);
				yield parts.length == 2 ? game.keno(id, name, amount, parseLong(parts[0]), parseLong(parts[1])) : null;
			}
			case 14 -> game.minesStart(id, name, amount, (int) detail);
			case 15 -> game.reveal(id, (int) detail);
			case 16 -> game.crashStart(id, name, amount, detail / 100.0);
			case 17 -> game.hiloStart(id, name, amount);
			case 18 -> game.guess(id, detail == 1);
			case 19 -> game.cashOut(id);
			default -> null;
		};
	}

	/** A short text of everything a casino game's watchers see: a change means a new view goes out. */
	static String casinoSignature(CasinoBlockEntity casino) {
		StringBuilder out = new StringBuilder();
		out.append(casino.minBet()).append('/').append(casino.maxBet()).append('/');
		switch (casino.game()) {
			case BLACKJACK -> {
				Blackjack g = casino.blackjack();
				out.append(g.phase).append(g.timer / 20).append(g.actorSeat).append(g.actorHand).append(g.dealer).append(g.holeShown)
						.append(g.log.size() > 0 ? g.log.get(g.log.size() - 1) : "");
				for (Blackjack.Seat seat : g.seats) {
					out.append('|').append(seat.player).append(seat.bet).append(seat.ready).append(seat.insurance)
							.append(seat.insuranceDecided);
					for (Blackjack.Hand hand : seat.hands) {
						out.append(hand.cards).append(hand.bet).append(hand.outcome).append(hand.done);
					}
				}
			}
			case ROULETTE -> {
				Roulette g = casino.roulette();
				out.append(g.phase).append(g.timer / 20).append(g.spins).append(g.bets.size());
				for (Roulette.Bet bet : g.bets) {
					out.append(bet.amount());
				}
			}
			case CRAPS -> {
				Craps g = casino.craps();
				out.append(g.phase).append(g.timer / 20).append(g.rolls).append(g.point).append(g.shooter).append(g.players)
						.append(g.open >= 60);
				for (Craps.Bet bet : g.bets) {
					out.append('|').append(bet.kind).append(bet.number).append(bet.amount).append(bet.odds);
				}
			}
			case ARCADE -> {
				ArcadeGame g = casino.arcade();
				out.append(g.mode).append(g.phase).append(g.rounds).append(g.player).append(g.minesRevealed).append(g.hiloSteps)
						.append(g.crashTicks / 4).append(g.card);
			}
			case PLINKO -> out.append(casino.plinko().drops).append(casino.plinko().player);
			case PRIZE_WHEEL -> {
				ShowGames.BigSix g = casino.bigSix();
				out.append(g.phase).append(g.timer / 20).append(g.spins).append(g.bets.size());
				for (ShowGames.BigSix.Bet bet : g.bets) {
					out.append(bet.amount());
				}
			}
			case BACCARAT -> {
				ShowGames.Baccarat g = casino.baccarat();
				out.append(g.phase).append(g.timer / 20).append(g.rounds).append(g.bets.size());
				for (ShowGames.Baccarat.Bet bet : g.bets) {
					out.append(bet.amount());
				}
			}
			case COIN_PUSHER -> out.append(casino.pusher().drops).append(casino.pusher().shelf);
			case DERBY -> {
				Derby g = casino.derby();
				out.append(g.phase).append(g.timer / 20).append(g.raceNumber).append(g.bets.size());
				for (Derby.Bet bet : g.bets) {
					out.append(bet.amount());
				}
			}
			case LOTTERY -> out.append(casino.lottery().pot).append(casino.lottery().tickets.size()).append(casino.lottery().lastDraw);
			default -> {
				SlotGame g = casino.slot();
				if (g != null) {
					out.append(g.spins).append(g.player).append(g.jackpot).append(g.freeSpins).append(g.nudges).append(g.holdOffered);
				}
			}
		}
		return out.toString();
	}

	/** Sends a player their view of a casino game (laid out as each game's screen reads it). */
	static void sendCasino(ServerPlayer player, CasinoBlockEntity casino, boolean open, FinanceBook.Result result) {
		MinecraftServer server = player.level().getServer();
		FinanceBook book = Ledger.get(server).book();
		UUID id = player.getUUID();
		List<Long> d = new ArrayList<>();
		List<String> text = new ArrayList<>();
		List<String> log = List.of();
		int phase = 0;
		int timer = 0;
		switch (casino.game()) {
			case BLACKJACK -> {
				Blackjack g = casino.blackjack();
				phase = g.phase.ordinal();
				timer = g.timer;
				log = g.log;
				d.add(g.round);
				d.add(g.holeShown ? 1L : 0L);
				d.add((long) g.dealer.size());
				for (int i = 0; i < g.dealer.size(); i++) {
					d.add(i == 1 && !g.holeShown ? -2L : g.dealer.get(i));
				}
				d.add((long) g.actorSeat);
				d.add((long) g.actorHand);
				d.add((long) g.seatOf(id));
				d.add((long) g.shoeLeft());
				for (Blackjack.Seat seat : g.seats) {
					text.add(seat.occupied() ? FinanceBook.clean(seat.name, 32) : "");
					d.add(seat.occupied() ? 1L : 0L);
					d.add(seat.bet);
					d.add(seat.lastBet);
					d.add(seat.ready ? 1L : 0L);
					d.add(seat.insurance);
					d.add(seat.insuranceDecided ? 1L : 0L);
					d.add((long) seat.hands.size());
					for (Blackjack.Hand hand : seat.hands) {
						d.add(hand.bet);
						d.add(hand.doubled ? 1L : 0L);
						d.add((long) hand.outcome);
						d.add(hand.done ? 1L : 0L);
						d.add(hand.splitAces ? 1L : 0L);
						d.add((long) hand.cards.size());
						for (int card : hand.cards) {
							d.add((long) card);
						}
					}
				}
			}
			case ROULETTE -> {
				Roulette g = casino.roulette();
				phase = g.phase.ordinal();
				timer = g.timer;
				log = g.log;
				d.add((long) g.result);
				d.add(g.spins);
				d.add((long) g.history.size());
				for (int n : g.history) {
					d.add((long) n);
				}
				int count = Math.min(g.bets.size(), 400);
				d.add((long) count);
				for (int i = 0; i < count; i++) {
					Roulette.Bet bet = g.bets.get(i);
					d.add(bet.numbers());
					d.add(bet.amount());
					d.add(bet.player().equals(id) ? 1L : 0L);
				}
			}
			case CRAPS -> {
				Craps g = casino.craps();
				phase = g.phase.ordinal();
				timer = g.timer;
				log = g.log;
				d.add((long) g.point);
				d.add((long) g.die1);
				d.add((long) g.die2);
				d.add(g.rolls);
				d.add(id.equals(g.shooter()) ? 1L : 0L);
				d.add(g.open >= 60 ? 1L : 0L);
				d.add((long) g.history.size());
				for (int n : g.history) {
					d.add((long) n);
				}
				int count = Math.min(g.bets.size(), 600);
				d.add((long) count);
				for (int i = 0; i < count; i++) {
					Craps.Bet bet = g.bets.get(i);
					d.add((long) bet.kind.ordinal());
					d.add((long) bet.number);
					d.add(bet.amount);
					d.add(bet.odds);
					d.add(bet.player.equals(id) ? 1L : 0L);
				}
				UUID shooter = g.shooter();
				text.add(shooter == null ? "" : FinanceBook.clean(g.names.getOrDefault(shooter, "?"), 32));
			}
			case ARCADE -> {
				ArcadeGame g = casino.arcade();
				log = g.log;
				phase = g.phase.ordinal();
				boolean live = g.phase == ArcadeGame.Phase.PLAYING;
				d.add((long) g.mode.ordinal());
				d.add(g.bet);
				d.add(g.lastWin);
				d.add(g.rounds);
				d.add(g.player != null && !g.player.equals(id) ? 1L : 0L);
				for (int c : g.hand) {
					d.add((long) c);
				}
				d.add((long) g.pokerResult);
				d.add(g.kenoPicks);
				d.add(g.kenoPicksHigh);
				d.add(g.kenoDrawn);
				d.add(g.kenoDrawnHigh);
				d.add((long) g.kenoCaught);
				d.add((long) g.mineCount);
				d.add(g.minesRevealed);
				// Where the TNT is stays secret until the round is over.
				d.add(live ? 0L : g.minesHidden);
				d.add(g.mineHit ? 1L : 0L);
				d.add((long) g.crashTicks);
				d.add(live ? 0L : Math.round(g.crashPoint * 100));
				d.add(Math.round(g.autoCashout * 100));
				d.add(Math.round(g.cashedAt * 100));
				d.add((long) g.card);
				d.add(Math.round(g.hiloMultiplier * 10_000));
				d.add((long) g.hiloSteps);
				d.add((long) g.hiloCards.size());
				for (int c : g.hiloCards) {
					d.add((long) c);
				}
				text.add(g.player == null ? "" : FinanceBook.clean(g.playerName, 32));
			}
			case PLINKO -> {
				ShowGames.Plinko g = casino.plinko();
				log = g.log;
				d.add((long) g.risk);
				d.add((long) g.path);
				d.add((long) g.slot);
				d.add(g.bet);
				d.add(g.win);
				d.add(g.drops);
				d.add(g.player != null && !g.player.equals(id) ? 1L : 0L);
				text.add(g.player == null ? "" : FinanceBook.clean(g.playerName, 32));
				d.add((long) g.recent.size());
				for (long[] drop : g.recent) {
					for (long v : drop) {
						d.add(v);
					}
				}
			}
			case PRIZE_WHEEL -> {
				ShowGames.BigSix g = casino.bigSix();
				log = g.log;
				phase = g.phase.ordinal();
				timer = g.timer;
				d.add((long) g.result);
				d.add(g.spins);
				d.add((long) g.history.size());
				for (int n : g.history) {
					d.add((long) n);
				}
				d.add((long) Math.min(200, g.bets.size()));
				for (int i = 0; i < Math.min(200, g.bets.size()); i++) {
					ShowGames.BigSix.Bet bet = g.bets.get(i);
					d.add((long) bet.symbol());
					d.add(bet.amount());
					d.add(bet.player().equals(id) ? 1L : 0L);
				}
			}
			case BACCARAT -> {
				ShowGames.Baccarat g = casino.baccarat();
				log = g.log;
				phase = g.phase.ordinal();
				timer = g.timer;
				boolean show = g.phase == ShowGames.Baccarat.Phase.DEALING || g.phase == ShowGames.Baccarat.Phase.RESULT;
				d.add((long) (show ? g.winner : -1));
				d.add(g.rounds);
				d.add((long) (show ? g.playerCards.size() : 0));
				for (int c : show ? g.playerCards : List.<Integer>of()) {
					d.add((long) c);
				}
				d.add((long) (show ? g.bankerCards.size() : 0));
				for (int c : show ? g.bankerCards : List.<Integer>of()) {
					d.add((long) c);
				}
				d.add((long) g.history.size());
				for (int n : g.history) {
					d.add((long) n);
				}
				d.add((long) Math.min(200, g.bets.size()));
				for (int i = 0; i < Math.min(200, g.bets.size()); i++) {
					ShowGames.Baccarat.Bet bet = g.bets.get(i);
					d.add((long) bet.side());
					d.add(bet.amount());
					d.add(bet.player().equals(id) ? 1L : 0L);
				}
			}
			case COIN_PUSHER -> {
				CoinPusher g = casino.pusher();
				log = g.log;
				d.add(g.shelf);
				d.add(g.drops);
				d.add(g.lastWin);
				d.add(g.lastSpill);
			}
			case LOTTERY -> {
				ShowGames.Lottery g = casino.lottery();
				log = g.log;
				d.add(g.pot);
				d.add((long) g.tickets.size());
				d.add((long) g.ticketsOf(id));
				d.add(casino.ticketPrice());
				d.add(casino.nextDraw(server) - day(server));
				d.add(g.lastPrize);
				for (ShowGames.ScratchCard card : ShowGames.ScratchCard.values()) {
					d.add(card.price);
				}
				text.add(FinanceBook.clean(g.lastWinner, 32));
			}
			case DERBY -> {
				Derby g = casino.derby();
				log = g.log;
				phase = g.phase.ordinal();
				timer = g.timer;
				d.add(g.raceNumber);
				d.add(g.seed);
				d.add(g.startTime);
				d.add(player.level().getGameTime());
				d.add((long) g.cutPercent);
				for (int h = 0; h < Derby.HORSES; h++) {
					d.add((long) g.names[h]);
					d.add((long) g.ratings[h]);
					d.add(parseLong(g.form[h]));
					// The finishing order only once the race has started (it is drawn then).
					d.add(g.phase == Derby.Phase.RACING || g.phase == Derby.Phase.RESULT ? (long) g.order[h] : -1L);
				}
				for (int pool = 0; pool < 3; pool++) {
					for (int h = 0; h < Derby.HORSES; h++) {
						d.add(Math.round(g.payoutPerJug(pool, h) * 100));
					}
				}
				d.add((long) g.history.size());
				for (int winner : g.history) {
					d.add((long) winner);
				}
				List<Derby.Bet> mine = new ArrayList<>();
				for (Derby.Bet bet : g.bets) {
					if (bet.player().equals(id)) {
						mine.add(bet);
					}
				}
				d.add((long) mine.size());
				for (Derby.Bet bet : mine) {
					d.add((long) bet.pool());
					d.add((long) bet.horse());
					d.add(bet.amount());
				}
			}
			default -> {
				SlotGame g = casino.slot();
				if (g != null) {
					log = g.log;
					d.add((long) g.machine.ordinal());
					for (int stop : g.stops) {
						d.add((long) stop);
					}
					d.add(g.lastBet);
					d.add(g.lastWin);
					d.add((long) g.lastLines);
					d.add(g.lastJackpot ? 1L : 0L);
					d.add(g.spins);
					d.add(g.jackpot);
					d.add(g.player != null && !g.player.equals(id) ? 1L : 0L);
					d.add(g.jackpotFor(casino.maxBet()));
					d.add((long) g.lastWheel);
					d.add((long) g.lastWild);
					d.add((long) g.freeSpins);
					d.add(g.freeBet);
					d.add(g.freeWon);
					d.add((long) g.nudges);
					d.add(g.holdOffered ? 1L : 0L);
					d.add((long) g.lastHolds);
					d.add(g.lastBonus);
					for (int v : g.lastPearls) {
						d.add((long) v);
					}
					text.add(g.player == null ? "" : FinanceBook.clean(g.playerName, 32));
				}
			}
		}
		long[] data = new long[d.size()];
		for (int i = 0; i < data.length; i++) {
			data[i] = d.get(i);
		}
		List<String> lines = new ArrayList<>();
		for (String line : log) {
			lines.add(FinanceBook.clean(line, 150));
		}
		long limitMin = casino.adminMin();
		long limitMax = casino.adminMax();
		ServerPlayNetworking.send(player, new CasinoPayload(casino.getBlockPos().asLong(), casino.game().ordinal(), open,
				FinanceBook.clean(casino.ownerName(), 32), casino.isOwner(player), casino.bankHouse(), casino.minBet(), casino.maxBet(), limitMin,
				limitMax, book.cash(id), casino.cover(), phase, timer, data, text, lines,
				result == null ? "" : FinanceBook.clean(result.message(), 150), result != null && result.ok(),
				casino.tableInfo(casino.isOwner(player), book)));
	}

	/** Actions from a Market Chest's own screen: they answer with its panel, not the Exchange's. */
	private static boolean chestPanel(FinanceActionPayload.Action action) {
		return switch (action) {
			case CHEST_INFO, CHEST_PRICE, CHEST_PAUSE, CHEST_TAKE_BACK, CHEST_COMPANY -> true;
			default -> false;
		};
	}

	/** Runs a Market Chest request from its panel, then sends the panel back. */
	private static void chestAction(ServerPlayer player, FinanceActionPayload request, MinecraftServer server) {
		net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.of(request.extra());
		if (!canAccess(player, pos, 64) || !(player.level().getBlockEntity(pos) instanceof MarketChestBlockEntity chest)
				|| player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64) {
			return;
		}
		if (!chest.mayUse(player)) {
			sendChest(player, chest, FinanceBook.Result.fail("Only the company's controller runs this chest."));
			return;
		}
		Market market = Market.get(server);
		FinanceBook.Result result = switch (request.action()) {
			case CHEST_PRICE -> {
				long price = request.amount();
				if (price < 0 || price > FinanceBook.MAX_AMOUNT / Market.MAX_QUANTITY) {
					yield FinanceBook.Result.fail("That price is out of range.");
				}
				String key = FinanceBook.clean(request.target(), 120);
				chest.setPrice(key, price);
				yield new FinanceBook.Result(true, price > 0 ? "Price set: " + FinanceBook.format(price, FinanceBook.CURRENCY)
						+ " each." : "No longer selling that.");
			}
			case CHEST_PAUSE -> {
				chest.setPaused(request.period() == 1);
				yield new FinanceBook.Result(true, request.period() == 1 ? "Selling paused." : "Selling resumed.");
			}
			case CHEST_TAKE_BACK -> {
				int back = market.takeBack(chest.ref(), chest::insert);
				yield new FinanceBook.Result(back > 0, back > 0 ? "Took back " + back + " items." : "Nothing came back (no unsold"
						+ " goods, or the chest is full).");
			}
			case CHEST_COMPANY -> {
				Equities equities = Equities.get(server);
				Equities.Corporation company = equities.corp(request.amount());
				if (company == null || (!player.getUUID().equals(equities.controller(company)) && !isAdmin(player))) {
					yield FinanceBook.Result.fail("You do not control that company.");
				}
				String refused = chest.choose(server, company.id);
				yield refused == null ? new FinanceBook.Result(true, "This chest now sells for " + company.ticker + ".")
						: FinanceBook.Result.fail(refused);
			}
			default -> null;
		};
		sendChest(player, chest, result);
	}

	/** Sends a player the panel of the Market Chest they have open. */
	static void sendChest(ServerPlayer player, MarketChestBlockEntity chest, FinanceBook.Result result) {
		MinecraftServer server = player.level().getServer();
		Market market = Market.get(server);
		Equities equities = Equities.get(server);
		FinanceBook book = Ledger.get(server).book();
		Equities.Corporation company = equities.corp(chest.corp());
		List<MarketChestPayload.Choice> choices = new ArrayList<>();
		for (Equities.Corporation c : isAdmin(player) ? new ArrayList<>(equities.corporations()) : equities.controlledBy(player.getUUID())) {
			choices.add(new MarketChestPayload.Choice(c.id, c.name + " (" + c.ticker + ")"));
		}
		List<MarketChestPayload.Row> rows = new ArrayList<>();
		for (String key : chest.kinds(market)) {
			int listed = 0;
			for (Market.Listing listing : market.chestListings(chest.ref())) {
				if (Market.itemKey(listing.item).equals(key)) {
					listed += listing.quantity;
				}
			}
			rows.add(new MarketChestPayload.Row(chest.sample(key, market), key, chest.prices().getOrDefault(key, 0L), listed,
					chest.inChest(key)));
		}
		ServerPlayNetworking.send(player, new MarketChestPayload(chest.getBlockPos().asLong(),
				company == null ? "" : company.name + " (" + company.ticker + ")", chest.corp(), choices, chest.paused(), rows,
				company == null ? 0 : book.cash(company.treasury), result == null ? "" : FinanceBook.clean(result.message(), 180),
				result != null && result.ok()));
	}

	/** Sends {@code player} their view of the Exchange. */
	public static void send(ServerPlayer player, Ledger ledger, boolean open, FinanceBook.Result result) {
		FinanceBook book = ledger.book();
		MinecraftServer server = player.level().getServer();
		UUID id = player.getUUID();
		FinanceBook.Account account = book.account(id, player.getName().getString());

		List<FinanceSnapshotPayload.Rank> rich = new ArrayList<>();
		for (FinanceBook.Rich r : book.richList()) {
			rich.add(new FinanceSnapshotPayload.Rank(r.name(), r.cash()));
		}
		List<FinanceSnapshotPayload.ContractView> contracts = new ArrayList<>();
		List<FinanceBook.Contract> all = book.contracts();
		for (int i = all.size() - 1; i >= 0 && contracts.size() < FinanceSnapshotPayload.MAX_LIST; i--) {
			FinanceBook.Contract c = all.get(i);
			boolean outgoing = id.equals(c.payer);
			if (!outgoing && !id.equals(c.payee)) {
				continue;
			}
			FinanceBook.Account other = book.find(outgoing ? c.payee : c.payer);
			contracts.add(new FinanceSnapshotPayload.ContractView(c.id, outgoing, other == null ? "?" : other.name,
					c.amount, c.period.ordinal(), FinanceBook.clean(c.note, 120), c.nextDay, c.active));
		}
		List<String> history = new ArrayList<>();
        long awaiting=book.prizeDebts().stream().filter(d->id.equals(d.player())).mapToLong(FinanceBook.PrizeDebt::amount).sum();
        long owing=book.prizeDebts().stream().filter(d->id.equals(d.house())).mapToLong(FinanceBook.PrizeDebt::amount).sum();
        if(awaiting>0) history.add("Saved winnings awaiting funding: "+book.format(awaiting)+". Paid automatically when funded.");
        if(owing>0) history.add("Your house owes saved prizes: "+book.format(owing)+". New covered bets are paused until paid.");
		for (FinanceBook.Entry entry : book.historyOf(id, HISTORY_LINES)) {
			history.add("Day " + entry.day() + ": " + FinanceBook.clean(entry.text(), 220));
		}
		Market market = server == null ? null : Market.get(server);
		List<FinanceSnapshotPayload.ListingView> listings = new ArrayList<>();
		List<FinanceSnapshotPayload.RequestView> requests = new ArrayList<>();
		if (market != null) {
			// Newest first, up to MAX_MARKET of each.
			List<Market.Listing> lots = market.listings();
			for (int i = lots.size() - 1; i >= 0 && listings.size() < FinanceSnapshotPayload.MAX_MARKET; i--) {
				Market.Listing l = lots.get(i);
				listings.add(new FinanceSnapshotPayload.ListingView(l.id, l.item, l.quantity, l.price, l.auction, l.endDay,
						l.bid, l.auction ? Market.minimumBid(l, book) : l.price, Market.sellsIt(server, id, l), id.equals(l.bidder),
						FinanceBook.clean(l.sellerName, 40), l.corp > 0));
			}
			List<Market.Request> wants = market.requests();
			for (int i = wants.size() - 1; i >= 0 && requests.size() < FinanceSnapshotPayload.MAX_MARKET; i--) {
				Market.Request r = wants.get(i);
				requests.add(new FinanceSnapshotPayload.RequestView(r.id, r.item, r.quantity, r.price, r.owner.equals(id),
						FinanceBook.clean(r.ownerName, 40)));
			}
		}
		ServerPlayNetworking.send(player, new FinanceSnapshotPayload(open,
				result == null ? "" : FinanceBook.clean(result.message(), 200), result != null && result.ok(),
				book.currency(), server == null ? 0 : day(server), book.feeBp(), book.borrowLimit(), book.reserve(),
				book.circulation(), book.cash(id), account.debt, book.borrowable(id), isAdmin(player),
				rich, contracts, history, book.escrow(), market == null ? 0 : market.goodsValue(),
				market == null ? 0 : market.inflationBp(), market != null && market.inflationKnown(),
				market == null ? 0 : market.waiting(id), listings, requests, account.dueDay,
				server != null && book.overdue(id, day(server)), book.creditLimit(id), book.starterAvailable(id),
				book.termDays(), book.shareBp(), server == null ? FinanceSnapshotPayload.LandView.NONE : landView(player, server, book),
				server == null ? FinanceSnapshotPayload.EquityView.NONE : equityView(player, server, book),
				server == null || !isAdmin(player) ? List.of() : settingsView(server)));
	}

	/** Every admin setting and its value, for an operator's Settings page. */
	private static List<FinanceSnapshotPayload.SettingView> settingsView(MinecraftServer server) {
		List<FinanceSnapshotPayload.SettingView> out = new ArrayList<>();
		for (ExchangeSettings.Setting setting : ExchangeSettings.all()) {
			out.add(new FinanceSnapshotPayload.SettingView(setting.key(), setting.group(), setting.label(), setting.unit().ordinal(),
					setting.get(server)));
		}
		return out;
	}

	/** The land map around the player (only in the Overworld, where land can be claimed). */
	private static FinanceSnapshotPayload.LandView landView(ServerPlayer player, MinecraftServer server, FinanceBook book) {
		if (player.level().dimension() != Level.OVERWORLD) {
			return FinanceSnapshotPayload.LandView.NONE;
		}
		LandRegistry land = LandRegistry.get(server);
		int cx = player.blockPosition().getX() >> 4;
		int cz = player.blockPosition().getZ() >> 4;
		UUID id = player.getUUID();
		List<FinanceSnapshotPayload.Plot> plots = new ArrayList<>();
		for (LandRegistry.Claim claim : land.around(cx, cz, LandRegistry.MAP_RADIUS)) {
			plots.add(new FinanceSnapshotPayload.Plot(LandRegistry.keyX(claim.key) - cx, LandRegistry.keyZ(claim.key) - cz,
					FinanceBook.clean(claim.ownerName, 40), LandRegistry.actsFor(server, id, claim.owner),
					land.trustedBy(claim.owner).contains(id), claim.price, claim.auctionEnd, claim.bid,
					claim.onAuction() ? LandRegistry.minimumBid(claim, book) : 0, id.equals(claim.bidder)));
		}
		List<String> trusted = new ArrayList<>();
		for (UUID other : land.trustedBy(id)) {
			FinanceBook.Account account = book.find(other);
			trusted.add(account == null ? "?" : account.name);
		}
		return new FinanceSnapshotPayload.LandView(true, cx, cz, land.nextPrice(id, book), land.basePrice(), land.owned(id), plots, trusted);
	}

	/** Every listed corporation, with its book and this player's orders. */
	private static FinanceSnapshotPayload.EquityView equityView(ServerPlayer player, MinecraftServer server, FinanceBook book) {
		Equities equities = Equities.get(server);
		UUID id = player.getUUID();
		boolean admin = isAdmin(player);
		List<FinanceSnapshotPayload.CorpView> corps = new ArrayList<>();
		for (Equities.Corporation c : equities.corporations()) {
			UUID controller = equities.controller(c);
			boolean controls = id.equals(controller);
			List<FinanceSnapshotPayload.Holding> holders = new ArrayList<>();
			for (var holder : equities.holders(c)) {
				String name = holder.getKey().equals(c.treasury) ? c.ticker + " (unsold)" : nameOf(book, holder.getKey());
				holders.add(new FinanceSnapshotPayload.Holding(name, holder.getValue(), holder.getKey().equals(id)));
			}
			java.util.TreeMap<Long, Long> asks = new java.util.TreeMap<>();
			java.util.TreeMap<Long, Long> bids = new java.util.TreeMap<>(java.util.Comparator.reverseOrder());
			List<FinanceSnapshotPayload.MyOrder> mine = new ArrayList<>();
			for (Equities.Order o : equities.orders(c.id)) {
				(o.buy ? bids : asks).merge(o.price, o.quantity, Long::sum);
				boolean company = o.owner.equals(c.treasury) && controls;
				if (o.owner.equals(id) || company || admin) {
					mine.add(new FinanceSnapshotPayload.MyOrder(o.id, o.buy, o.price, o.quantity, !o.owner.equals(id)));
				}
			}
			List<FinanceSnapshotPayload.Level> askLevels = new ArrayList<>();
			asks.forEach((price, qty) -> askLevels.add(new FinanceSnapshotPayload.Level(price, qty)));
			List<FinanceSnapshotPayload.Level> bidLevels = new ArrayList<>();
			bids.forEach((price, qty) -> bidLevels.add(new FinanceSnapshotPayload.Level(price, qty)));
			boolean approval = ExchangeSettings.value(book, ExchangeSettings.LOGO_APPROVAL) == 1;
			boolean pending = approval && c.logoUploaded && !c.logoApproved;
			corps.add(new FinanceSnapshotPayload.CorpView(c.id, c.name, c.ticker, controls || admin ? c.logo : c.visibleLogo(approval),
					pending, c.lastPrice, c.totalShares, c.lastDividend, book.cash(c.treasury),
					controller == null ? "nobody" : nameOf(book, controller), controls, equities.sharesOf(c, id), holders,
					askLevels, bidLevels, mine));
		}
		return new FinanceSnapshotPayload.EquityView(equities.charterFee(),
				ExchangeSettings.value(book, ExchangeSettings.ISSUE_FEE), ExchangeSettings.value(book, ExchangeSettings.CHEST_PRICE),
				corps);
	}

	private static String nameOf(FinanceBook book, UUID id) {
		FinanceBook.Account account = book.find(id);
		return account == null ? "?" : FinanceBook.clean(account.name, 40);
	}

	// ------------------------------------------------------------------ chat ledger

	/**
	 * Writes new ledger lines to the server log. Only big wins at the casino (and lottery jackpots) go to chat: to
	 * everyone, or only to the winner when an admin has set the chat log to parties. Everything else stays in each
	 * player's history on the Exchange.
	 */
	static void announce(MinecraftServer server, Ledger ledger) {
		FinanceBook book = ledger.book();
		for (FinanceBook.Entry entry : book.drainNotices()) {
			Jugcraft.LOGGER.info("[Exchange] {}", entry.text());
			if (!book.takeLoud(entry)) {
				continue;
			}
			// Held a few seconds, so chat does not tell a slot's or a Plinko ball's result before its screen shows it.
			SHOUTS.add(new Shout(server.getTickCount() + SHOUT_DELAY_TICKS, entry, book.broadcast()));
		}
	}

	/** A big win waiting to be told in chat. */
	private record Shout(int at, FinanceBook.Entry entry, boolean everyone) {
	}

	/** About as long as the slowest reveal (the Nether Furnace's reels and wheel). */
	private static final int SHOUT_DELAY_TICKS = 100;
	private static final java.util.ArrayDeque<Shout> SHOUTS = new java.util.ArrayDeque<>();

	private static void releaseShouts(MinecraftServer server) {
		// A world reopened in the same game starts its tick count again: let go of anything left from before.
		if (!SHOUTS.isEmpty() && SHOUTS.peekFirst().at() > server.getTickCount() + SHOUT_DELAY_TICKS) {
			SHOUTS.clear();
		}
		while (!SHOUTS.isEmpty() && SHOUTS.peekFirst().at() <= server.getTickCount()) {
			Shout shout = SHOUTS.pollFirst();
			Component line = Component.literal("[Casino] ").withStyle(ChatFormatting.GOLD)
					.append(Component.literal(shout.entry().text()).withStyle(ChatFormatting.YELLOW));
			if (shout.everyone()) {
				server.getPlayerList().broadcastSystemMessage(line, false);
				continue;
			}
			UUID winner = shout.entry().first();
			ServerPlayer player = winner == null ? null : server.getPlayerList().getPlayer(winner);
			if (player != null) {
				player.sendSystemMessage(line);
			}
		}
	}
}
