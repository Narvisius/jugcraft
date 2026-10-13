package io.github.jimbozoomer.jugcraft.lair;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /jugcraft lair leave} sends a player home from any lair (the stranded player's way out); operators also have
 * {@code /jugcraft lair list} and {@code /jugcraft lair close <lair> <slot>}.
 */
public final class LairCommands {
	private LairCommands() {
	}

	static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("jugcraft").then(Commands.literal("lair")
				.then(Commands.literal("leave").executes(context -> leave(context.getSource())))
				.then(Commands.literal("list").requires(LairCommands::isOperator).executes(context -> list(context.getSource())))
				.then(Commands.literal("close").requires(LairCommands::isOperator)
						.then(Commands.argument("lair", StringArgumentType.word())
								.suggests((context, builder) -> SharedSuggestionProvider.suggest(
										java.util.Arrays.stream(Lair.values()).map(lair -> lair.id), builder))
								.then(Commands.argument("slot", IntegerArgumentType.integer(0, Lairs.MAX_INSTANCES - 1))
										.executes(context -> close(context.getSource(), StringArgumentType.getString(context, "lair"),
												IntegerArgumentType.getInteger(context, "slot"))))))));
	}

	private static boolean isOperator(CommandSourceStack source) {
		return Commands.LEVEL_GAMEMASTERS.check(source.permissions());
	}

	private static int leave(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		if (!Lairs.leave(player)) {
			source.sendFailure(Component.translatable("commands.jugcraft.lair.not_inside"));
			return 0;
		}
		return 1;
	}

	private static int list(CommandSourceStack source) {
		int count = 0;
		for (Lair lair : Lair.values()) {
			for (LairInstance instance : Lairs.open(lair)) {
				long seconds = (source.getLevel().getGameTime() - instance.opened) / 20L;
				source.sendSuccess(() -> Component.translatable("commands.jugcraft.lair.list.entry",
						Component.translatable("lair.jugcraft." + lair.id), instance.slot, instance.members.size(), seconds), false);
				count++;
			}
		}
		if (count == 0) {
			source.sendSuccess(() -> Component.translatable("commands.jugcraft.lair.list.none"), false);
		}
		return count;
	}

	private static int close(CommandSourceStack source, String id, int slot) {
		Lair lair = Lair.byId(id);
		LairInstance instance = lair == null ? null : Lairs.instance(lair, slot);
		if (instance == null) {
			source.sendFailure(Component.translatable("commands.jugcraft.lair.close.none", id, slot));
			return 0;
		}
		Lairs.close(source.getServer(), instance);
		source.sendSuccess(() -> Component.translatable("commands.jugcraft.lair.close.done",
				Component.translatable("lair.jugcraft." + lair.id), slot), true);
		return 1;
	}
}
