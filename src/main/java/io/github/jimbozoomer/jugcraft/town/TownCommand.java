package io.github.jimbozoomer.jugcraft.town;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * {@code /jugcraft town}: where the town is and today's theme; {@code /jugcraft jugs}: your Jugs. Operators (permission
 * level 2) can also:
 * <ul>
 * <li>{@code town place}: build the town round where they stand, in a world that has none (an existing world, or one
 * made before the town existed);</li>
 * <li>{@code town theme <name|auto>}: hold a decor theme (or follow the calendar again) until the server stops;</li>
 * <li>{@code jugs give <player> <amount>} and {@code jugs take <player> <amount>}.</li>
 * </ul>
 */
public final class TownCommand {
	private TownCommand() {
	}

	static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("jugcraft")
				.then(Commands.literal("town")
						.executes(context -> info(context.getSource()))
						.then(Commands.literal("place").requires(TownCommand::isOperator).executes(TownCommand::place))
						.then(Commands.literal("theme").requires(TownCommand::isOperator)
								.then(Commands.argument("theme", StringArgumentType.word())
										.suggests((context, builder) -> {
											builder.suggest("auto");
											TownData.get().themes.forEach(builder::suggest);
											return builder.buildFuture();
										})
										.executes(TownCommand::theme))))
				.then(Commands.literal("jugs")
						.executes(TownCommand::balance)
						.then(Commands.literal("give").requires(TownCommand::isOperator)
								.then(Commands.argument("player", EntityArgument.player())
										.then(Commands.argument("amount", LongArgumentType.longArg(1))
												.executes(context -> change(context, true)))))
						.then(Commands.literal("take").requires(TownCommand::isOperator)
								.then(Commands.argument("player", EntityArgument.player())
										.then(Commands.argument("amount", LongArgumentType.longArg(1))
												.executes(context -> change(context, false)))))));
	}

	private static boolean isOperator(CommandSourceStack source) {
		return Commands.LEVEL_GAMEMASTERS.check(source.permissions());
	}

	private static int info(CommandSourceStack source) {
		BlockPos origin = TownState.get(source.getServer()).origin();
		if (origin == null) {
			source.sendSuccess(() -> Component.translatable("command.jugcraft.town.none"), false);
			return 0;
		}
		BlockPos centre = Town.centre(origin);
		source.sendSuccess(() -> Component.translatable("command.jugcraft.town.info", centre.getX(), centre.getY(), centre.getZ(),
				Component.translatable("theme.jugcraft." + TownDecor.theme())), false);
		return 1;
	}

	private static int place(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = source.getPlayerOrException();
		ServerLevel level = source.getServer().overworld();
		if (player.level() != level) {
			source.sendFailure(Component.translatable("command.jugcraft.town.overworld"));
			return 0;
		}
		if (TownState.get(level).origin() != null) {
			source.sendFailure(Component.translatable("command.jugcraft.town.exists"));
			return 0;
		}
		BlockPos at = player.blockPosition();
		int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ()) - 1;
		TownPlanner.place(level, TownPlanner.originAround(at.getX(), ground, at.getZ()));
		source.sendSuccess(() -> Component.translatable("command.jugcraft.town.placed"), true);
		return 1;
	}

	private static int theme(CommandContext<CommandSourceStack> context) {
		String theme = StringArgumentType.getString(context, "theme");
		if (theme.equals("auto")) {
			TownDecor.force(null);
		} else if (TownData.get().themes.contains(theme)) {
			TownDecor.force(theme);
		} else {
			context.getSource().sendFailure(Component.translatable("command.jugcraft.town.no_theme", theme));
			return 0;
		}
		context.getSource().sendSuccess(() -> Component.translatable("command.jugcraft.town.theme",
				Component.translatable("theme.jugcraft." + TownDecor.theme())), true);
		return 1;
	}

	private static int balance(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		long jugs = Jugs.balance(context.getSource().getServer(), player.getUUID());
		context.getSource().sendSuccess(() -> Component.translatable("command.jugcraft.jugs.balance", jugs), false);
		return (int) Math.min(Integer.MAX_VALUE, jugs);
	}

	private static int change(CommandContext<CommandSourceStack> context, boolean give) throws CommandSyntaxException {
		ServerPlayer player = EntityArgument.getPlayer(context, "player");
		long amount = LongArgumentType.getLong(context, "amount");
		if (give) {
			Jugs.add(context.getSource().getServer(), player.getUUID(), amount);
		} else if (!Jugs.take(context.getSource().getServer(), player.getUUID(), amount)) {
			context.getSource().sendFailure(Component.translatable("command.jugcraft.jugs.too_few"));
			return 0;
		}
		long jugs = Jugs.balance(context.getSource().getServer(), player.getUUID());
		context.getSource().sendSuccess(() -> Component.translatable("command.jugcraft.jugs.changed", player.getDisplayName(), jugs), true);
		return 1;
	}
}
