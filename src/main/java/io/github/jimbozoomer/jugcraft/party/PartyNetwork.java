package io.github.jimbozoomer.jugcraft.party;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Keeps each player's Party screen current: the state is sent when they join, when they ask (opening the
 * screen), whenever their party changes, and when they get or answer an invite.
 */
final class PartyNetwork {
	/** Requests closer together than this are ignored; the screen asks once per open or click. */
	private static final int REQUEST_INTERVAL_TICKS = 5;
	/** Server tick of each online player's last answered request. */
	private static final Map<UUID, Integer> LAST_REQUEST = new HashMap<>();

	private PartyNetwork() {
	}

	static void register() {
		PayloadTypeRegistry.clientboundPlay().register(PartyStatePayload.TYPE, PartyStatePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(PartyRequestPayload.TYPE, PartyRequestPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(PartyRequestPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			int now = player.level().getServer().getTickCount();
			Integer last = LAST_REQUEST.get(player.getUUID());
			if (last != null && now - last < REQUEST_INTERVAL_TICKS) {
				return;
			}
			LAST_REQUEST.put(player.getUUID(), now);
			send(player);
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			JugcraftParties.manager().rememberName(handler.player.getUUID(), handler.player.getName().getString());
			send(handler.player);
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> LAST_REQUEST.remove(handler.player.getUUID()));
	}

	/** Sends the current state to each of these players who is online. */
	static void send(MinecraftServer server, Collection<UUID> players) {
		for (UUID uuid : players) {
			ServerPlayer player = server.getPlayerList().getPlayer(uuid);
			if (player != null) {
				send(player);
			}
		}
	}

	static void send(ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, PartyStatePayload.TYPE)) {
			ServerPlayNetworking.send(player, state(player.level().getServer(), player.getUUID()));
		}
	}

	static PartyStatePayload state(MinecraftServer server, UUID player) {
		PartyManager manager = JugcraftParties.manager();
		List<PartyStatePayload.Member> members = new ArrayList<>();
		manager.partyOf(player).ifPresent(party -> {
			for (UUID member : party.members()) {
				members.add(new PartyStatePayload.Member(name(manager, member), member.equals(party.leader()),
						server.getPlayerList().getPlayer(member) != null, member.equals(player)));
			}
		});
		List<String> invites = new ArrayList<>();
		for (UUID partyId : manager.pendingInvites(player, System.currentTimeMillis())) {
			manager.party(partyId).ifPresent(party -> invites.add(name(manager, party.leader())));
		}
		return new PartyStatePayload(manager.isEnabled(), manager.maxSize(), members, invites);
	}

	static String name(PartyManager manager, UUID player) {
		return manager.nameOf(player).orElse(player.toString().substring(0, 8));
	}
}
