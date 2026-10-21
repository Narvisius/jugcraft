package io.github.jimbozoomer.jugcraft.finance;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.function.BiFunction;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

/**
 * The Exchange: one server-wide currency, the central bank and payment contracts, used through a hotkey screen
 * (client {@code ExchangeScreen}) and the {@code /jugcraft exchange} command. See docs/features/finance-exchange.md.
 */
public final class JugcraftFinance {
	private JugcraftFinance() {
	}

	public static void register() {
		ExchangeGuide.register();
		ExchangeBlocks.register();
		PayloadTypeRegistry.serverboundPlay().register(FinanceActionPayload.TYPE, FinanceActionPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(FinanceSnapshotPayload.TYPE, FinanceSnapshotPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(MarketChestPayload.TYPE, MarketChestPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(PokerTablePayload.TYPE, PokerTablePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(CasinoPayload.TYPE, CasinoPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ScratchPayload.TYPE, ScratchPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(FinanceActionPayload.TYPE,
				(payload, context) -> FinanceService.handle(payload, context.player()));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> FinanceService.onJoin(handler.getPlayer(), server));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> FinanceService.onLeave(handler.getPlayer()));
		ServerTickEvents.END_SERVER_TICK.register(FinanceService::tick);
		LandProtection.register();
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));
	}

	// ------------------------------------------------------------------ commands

	private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(literal("jugcraft").then(literal("exchange")
				.executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					FinanceService.send(player, Ledger.get(ctx.getSource().getServer()), true, null);
					return Command.SINGLE_SUCCESS;
				})
				.then(literal("guide").executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					ExchangeGuide.give(player);
					return Command.SINGLE_SUCCESS;
				}))
				.then(literal("admin")
						.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
						.then(literal("fee").then(argument("percent", StringArgumentType.word())
								.executes(ctx -> run(ctx, (book, day) -> book.setFee(
										FinanceBook.parseRate(StringArgumentType.getString(ctx, "percent")), day)))))
						.then(literal("term").then(argument("days", IntegerArgumentType.integer(1, FinanceBook.MAX_TERM_DAYS))
								.executes(ctx -> run(ctx, (book, day) -> book.setTerm(IntegerArgumentType.getInteger(ctx, "days"), day)))))
						.then(literal("salesshare").then(argument("percent", StringArgumentType.word())
								.executes(ctx -> run(ctx, (book, day) -> book.setShare(
										FinanceBook.parseRate(StringArgumentType.getString(ctx, "percent")), day)))))
						.then(literal("forgive").then(argument("player", StringArgumentType.word())
								.executes(ctx -> forPlayer(ctx, (book, id, day) -> book.forgive(id, 0, day)))
								.then(argument("amount", StringArgumentType.word())
										.executes(ctx -> forPlayer(ctx, (book, id, day) -> book.forgive(id, amount(ctx), day))))))
						.then(literal("resetstarter").then(argument("player", StringArgumentType.word())
								.executes(ctx -> forPlayer(ctx, (book, id, day) -> book.resetStarter(id, day)))))
						.then(literal("debts").executes(JugcraftFinance::debts))
						.then(literal("settings").executes(JugcraftFinance::listSettings))
						.then(literal("set").then(argument("setting", StringArgumentType.word())
								.suggests((ctx, builder) -> {
									for (ExchangeSettings.Setting setting : ExchangeSettings.all()) {
										builder.suggest(setting.key());
									}
									return builder.buildFuture();
								})
								.then(argument("value", StringArgumentType.word()).executes(ctx -> run(ctx, (book, day) ->
										ExchangeSettings.set(ctx.getSource().getServer(), StringArgumentType.getString(ctx, "setting"),
												StringArgumentType.getString(ctx, "value"), day))))))
						.then(literal("corp")
								.then(literal("fee").then(argument("amount", StringArgumentType.word())
										.executes(ctx -> run(ctx, (book, day) -> Equities.get(ctx.getSource().getServer())
												.setCharterFee(book, amount(ctx), day)))))
								.then(literal("approvelogo").then(argument("ticker", StringArgumentType.word())
										.executes(ctx -> logo(ctx, true))))
								.then(literal("removelogo").then(argument("ticker", StringArgumentType.word())
										.executes(ctx -> logo(ctx, false)))))
						.then(literal("land")
								.then(literal("price").then(argument("amount", StringArgumentType.word())
										.executes(ctx -> run(ctx, (book, day) -> LandRegistry.get(ctx.getSource().getServer())
												.setBasePrice(book, amount(ctx), day)))))
								.then(literal("unclaim").executes(ctx -> {
									ServerPlayer admin = ctx.getSource().getPlayerOrException();
									long key = LandRegistry.keyOf(admin.blockPosition());
									return run(ctx, (book, day) -> LandRegistry.get(ctx.getSource().getServer()).abandon(admin, book, key, true, day));
								}))
								.then(literal("give").then(argument("player", StringArgumentType.word()).executes(ctx -> {
									ServerPlayer admin = ctx.getSource().getPlayerOrException();
									long key = LandRegistry.keyOf(admin.blockPosition());
									return forPlayer(ctx, (book, id, day) -> LandRegistry.get(ctx.getSource().getServer())
											.assign(book, key, book.find(id), day));
								}))))
						.then(literal("limit").then(argument("amount", StringArgumentType.word())
								.executes(ctx -> run(ctx, (book, day) -> book.setLimit(amount(ctx), day)))))
						.then(literal("print").then(argument("amount", StringArgumentType.word())
								.executes(ctx -> run(ctx, (book, day) -> book.mint(amount(ctx), day)))))
						.then(literal("retire").then(argument("amount", StringArgumentType.word())
								.executes(ctx -> run(ctx, (book, day) -> book.burn(amount(ctx), day)))))
						.then(literal("grant").then(argument("player", EntityArgument.player())
								.then(argument("amount", StringArgumentType.word()).executes(ctx -> {
									ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
									Ledger.get(ctx.getSource().getServer()).book().account(target.getUUID(), target.getName().getString());
									return run(ctx, (book, day) -> book.grant(target.getUUID(), amount(ctx), day));
								}))))
						.then(literal("nextday").executes(ctx -> {
							MinecraftServer server = ctx.getSource().getServer();
							Ledger ledger = Ledger.get(server);
							ledger.book().closeDayEarly();
							FinanceService.runDays(server);
							ctx.getSource().sendSuccess(() -> Component.literal("Closed the books for the day: it is now day "
									+ FinanceService.day(server) + "."), true);
							return Command.SINGLE_SUCCESS;
						}))
						.then(literal("chatlog")
								.then(literal("everyone").executes(ctx -> broadcast(ctx, true)))
								.then(literal("parties").executes(ctx -> broadcast(ctx, false)))))));
	}

	private static long amount(CommandContext<CommandSourceStack> ctx) {
		String text = StringArgumentType.getString(ctx, "amount");
		return "0".equals(text.trim()) ? 0 : FinanceBook.parseAmount(text);
	}

	private static int run(CommandContext<CommandSourceStack> ctx, BiFunction<FinanceBook, Long, FinanceBook.Result> action) {
		MinecraftServer server = ctx.getSource().getServer();
		Ledger ledger = Ledger.get(server);
		FinanceBook.Result result = action.apply(ledger.book(), FinanceService.day(server));
		ledger.setDirty();
		FinanceService.announce(server, ledger);
		if (result.ok()) {
			ctx.getSource().sendSuccess(() -> Component.literal(result.message()), false);
			return Command.SINGLE_SUCCESS;
		}
		ctx.getSource().sendFailure(Component.literal(result.message()));
		return 0;
	}

	/** An admin action on a player named by their account name (works while they are offline). */
	private interface PlayerAction {
		FinanceBook.Result apply(FinanceBook book, java.util.UUID id, long day);
	}

	private static int forPlayer(CommandContext<CommandSourceStack> ctx, PlayerAction action) {
		String name = StringArgumentType.getString(ctx, "player");
		return run(ctx, (book, day) -> {
			FinanceBook.Account account = book.findByName(name);
			return account == null ? new FinanceBook.Result(false, "No account for " + FinanceBook.clean(name, 16) + ".")
					: action.apply(book, account.id, day);
		});
	}

	private static int logo(CommandContext<CommandSourceStack> ctx, boolean approve) {
		Equities equities = Equities.get(ctx.getSource().getServer());
		Equities.Corporation corp = equities.byTicker(StringArgumentType.getString(ctx, "ticker"));
		return run(ctx, (book, day) -> corp == null ? new FinanceBook.Result(false, "No such ticker.")
				: equities.approveLogo(corp.id, approve));
	}

	/** Lists every admin setting with its value and key. */
	private static int listSettings(CommandContext<CommandSourceStack> ctx) {
		MinecraftServer server = ctx.getSource().getServer();
		String group = "";
		for (ExchangeSettings.Setting setting : ExchangeSettings.all()) {
			if (!setting.group().equals(group)) {
				group = setting.group();
				String heading = "-- " + group + " --";
				ctx.getSource().sendSuccess(() -> Component.literal(heading), false);
			}
			String line = setting.label() + ": " + setting.show(setting.get(server)) + "  (" + setting.key() + ")";
			ctx.getSource().sendSuccess(() -> Component.literal(line), false);
		}
		return Command.SINGLE_SUCCESS;
	}

	/** Lists everyone who owes the bank, most first. */
	private static int debts(CommandContext<CommandSourceStack> ctx) {
		MinecraftServer server = ctx.getSource().getServer();
		FinanceBook book = Ledger.get(server).book();
		long day = FinanceService.day(server);
		java.util.List<FinanceBook.Account> owing = book.accounts().stream().filter(a -> a.debt > 0)
				.sorted(java.util.Comparator.comparingLong((FinanceBook.Account a) -> a.debt).reversed()).limit(20).toList();
		if (owing.isEmpty()) {
			ctx.getSource().sendSuccess(() -> Component.literal("Nobody owes the central bank."), false);
			return Command.SINGLE_SUCCESS;
		}
		for (FinanceBook.Account a : owing) {
			String line = a.name + ": " + book.format(a.debt) + ", due day " + a.dueDay + (book.overdue(a.id, day) ? " (OVERDUE)" : "");
			ctx.getSource().sendSuccess(() -> Component.literal(line), false);
		}
		return Command.SINGLE_SUCCESS;
	}

	private static int broadcast(CommandContext<CommandSourceStack> ctx, boolean everyone) {
		Ledger ledger = Ledger.get(ctx.getSource().getServer());
		ledger.book().setBroadcast(everyone);
		ledger.setDirty();
		ctx.getSource().sendSuccess(() -> Component.literal(everyone
				? "Every transaction is now announced to all players."
				: "Transactions are now told only to the players involved."), true);
		return Command.SINGLE_SUCCESS;
	}
}
