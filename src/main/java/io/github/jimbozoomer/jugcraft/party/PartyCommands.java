package io.github.jimbozoomer.jugcraft.party;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /party} commands. Every check happens in {@link PartyManager}; this class only turns
 * results into messages. Any player may use them (no operator level needed).
 */
final class PartyCommands {
	private PartyCommands() {
	}

	static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("party")
				.executes(context -> info(context.getSource()))
				.then(Commands.literal("info").executes(context -> info(context.getSource())))
				.then(Commands.literal("create").executes(context -> create(context.getSource())))
				.then(Commands.literal("invite")
						.then(Commands.argument("player", EntityArgument.player())
								.executes(context -> invite(context.getSource(), EntityArgument.getPlayer(context, "player")))))
				.then(Commands.literal("accept").executes(context -> accept(context.getSource())))
				.then(Commands.literal("decline").executes(context -> decline(context.getSource())))
				.then(Commands.literal("leave").executes(context -> leave(context.getSource())))
				.then(Commands.literal("kick")
						.then(Commands.argument("name", StringArgumentType.word())
								.executes(context -> kick(context.getSource(), StringArgumentType.getString(context, "name")))))
				.then(Commands.literal("leader")
						.then(Commands.argument("name", StringArgumentType.word())
								.executes(context -> leader(context.getSource(), StringArgumentType.getString(context, "name")))))
				.then(Commands.literal("disband").executes(context -> disband(context.getSource()))));
	}

	private static PartyManager manager() {
		return JugcraftParties.manager();
	}

	private static ServerPlayer player(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		manager().rememberName(player.getUUID(), player.getName().getString());
		return player;
	}

	private static int info(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = player(source);
		PartyManager manager = manager();
		if (!manager.isEnabled()) {
			return fail(source, PartyManager.Result.DISABLED);
		}
		var party = manager.partyOf(player.getUUID());
		if (party.isEmpty()) {
			int invites = manager.pendingInvites(player.getUUID(), System.currentTimeMillis()).size();
			source.sendSuccess(() -> Component.translatable("message.jugcraft.party.none", invites), false);
			return 1;
		}
		List<UUID> members = party.get().members();
		UUID leader = party.get().leader();
		MinecraftServer server = source.getServer();
		StringBuilder roster = new StringBuilder();
		for (UUID member : members) {
			if (!roster.isEmpty()) {
				roster.append(", ");
			}
			roster.append(name(member));
			if (member.equals(leader)) {
				roster.append(" ★");
			}
			if (server.getPlayerList().getPlayer(member) == null) {
				roster.append(" (offline)");
			}
		}
		String text = roster.toString();
		source.sendSuccess(() -> Component.translatable("message.jugcraft.party.roster", members.size(), manager.maxSize(), text), false);
		return 1;
	}

	private static int create(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = player(source);
		PartyManager.Result result = manager().create(player.getUUID());
		if (result != PartyManager.Result.OK) {
			return fail(source, result);
		}
		source.sendSuccess(() -> Component.translatable("message.jugcraft.party.created"), false);
		return 1;
	}

	private static int invite(CommandSourceStack source, ServerPlayer target) throws CommandSyntaxException {
		ServerPlayer player = player(source);
		manager().rememberName(target.getUUID(), target.getName().getString());
		PartyManager.Result result = manager().invite(player.getUUID(), target.getUUID(), System.currentTimeMillis());
		if (result != PartyManager.Result.OK) {
			return fail(source, result);
		}
		String targetName = target.getName().getString();
		source.sendSuccess(() -> Component.translatable("message.jugcraft.party.invite_sent", targetName), false);
		target.sendSystemMessage(Component.translatable("message.jugcraft.party.invite_received", player.getName().getString()));
		return 1;
	}

	private static int accept(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = player(source);
		PartyManager.Result result = manager().accept(player.getUUID(), System.currentTimeMillis());
		if (result != PartyManager.Result.OK) {
			return fail(source, result);
		}
		notifyParty(source.getServer(), player.getUUID(), Component.translatable("message.jugcraft.party.joined", player.getName().getString()));
		return 1;
	}

	private static int decline(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = player(source);
		PartyManager.Result result = manager().decline(player.getUUID(), System.currentTimeMillis());
		if (result != PartyManager.Result.OK) {
			return fail(source, result);
		}
		source.sendSuccess(() -> Component.translatable("message.jugcraft.party.declined"), false);
		return 1;
	}

	private static int leave(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = player(source);
		List<UUID> before = JugcraftParties.partyMembers(player.getUUID());
		PartyManager.Result result = manager().leave(player.getUUID());
		if (result != PartyManager.Result.OK) {
			return fail(source, result);
		}
		source.sendSuccess(() -> Component.translatable("message.jugcraft.party.left"), false);
		notifyPlayers(source.getServer(), before, player.getUUID(), Component.translatable("message.jugcraft.party.member_left", player.getName().getString()));
		return 1;
	}

	private static int kick(CommandSourceStack source, String name) throws CommandSyntaxException {
		ServerPlayer player = player(source);
		var target = manager().findMemberByName(player.getUUID(), name);
		if (target.isEmpty()) {
			return fail(source, PartyManager.Result.NOT_MEMBER);
		}
		PartyManager.Result result = manager().kick(player.getUUID(), target.get());
		if (result != PartyManager.Result.OK) {
			return fail(source, result);
		}
		notifyParty(source.getServer(), player.getUUID(), Component.translatable("message.jugcraft.party.kicked", name));
		ServerPlayer kicked = source.getServer().getPlayerList().getPlayer(target.get());
		if (kicked != null) {
			kicked.sendSystemMessage(Component.translatable("message.jugcraft.party.you_were_kicked"));
		}
		return 1;
	}

	private static int leader(CommandSourceStack source, String name) throws CommandSyntaxException {
		ServerPlayer player = player(source);
		var target = manager().findMemberByName(player.getUUID(), name);
		if (target.isEmpty()) {
			return fail(source, PartyManager.Result.NOT_MEMBER);
		}
		PartyManager.Result result = manager().transferLeader(player.getUUID(), target.get());
		if (result != PartyManager.Result.OK) {
			return fail(source, result);
		}
		notifyParty(source.getServer(), player.getUUID(), Component.translatable("message.jugcraft.party.new_leader", name));
		return 1;
	}

	private static int disband(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = player(source);
		List<UUID> before = JugcraftParties.partyMembers(player.getUUID());
		PartyManager.Result result = manager().disband(player.getUUID());
		if (result != PartyManager.Result.OK) {
			return fail(source, result);
		}
		notifyPlayers(source.getServer(), before, null, Component.translatable("message.jugcraft.party.disbanded"));
		return 1;
	}

	private static int fail(CommandSourceStack source, PartyManager.Result result) {
		source.sendFailure(Component.translatable("message.jugcraft.party.error." + result.name().toLowerCase(Locale.ROOT)));
		return 0;
	}

	private static String name(UUID player) {
		return manager().nameOf(player).orElse(player.toString().substring(0, 8));
	}

	/** Tells every online member of {@code player}'s party. */
	private static void notifyParty(MinecraftServer server, UUID player, Component message) {
		notifyPlayers(server, JugcraftParties.partyMembers(player), null, message);
	}

	private static void notifyPlayers(MinecraftServer server, List<UUID> players, UUID except, Component message) {
		for (UUID member : players) {
			if (member.equals(except)) {
				continue;
			}
			ServerPlayer online = server.getPlayerList().getPlayer(member);
			if (online != null) {
				online.sendSystemMessage(message);
			}
		}
	}
}
