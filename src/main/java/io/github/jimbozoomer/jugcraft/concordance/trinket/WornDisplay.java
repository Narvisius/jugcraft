package io.github.jimbozoomer.jugcraft.concordance.trinket;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;

/**
 * Whether a player's worn trinkets (Wayfaring's belt and boots) are drawn on them, for everyone who sees them: each
 * player's own choice, their "Show my worn trinkets" setting, which their client sends when it joins a world and whenever
 * it changes ({@link WornDisplayPayload}). The server keeps it on the player as a Fabric data attachment, {@link #HIDDEN},
 * there only while they are hidden, sent to that player and to every client that can see them (and to any that comes to
 * see them later), and kept through death. It is not saved: the client sends it again on joining. A change is taken at
 * most once every {@value #CHANGE_TICKS} ticks, so a client sending as fast as it can makes no more traffic for the
 * players round it than one changing the setting by hand; a newer choice that comes sooner waits for the next allowed
 * tick, so the last one sent is always the one that lands. Presentation only: the render element that draws the belt and
 * boots (client: trinket/UnlessCoveredTrinketElement) reads it, and nothing else in the game does.
 */
public final class WornDisplay {
	/** How often a player's choice may change (ticks). */
	public static final int CHANGE_TICKS = 10;
	/** The RateGate action a change counts against. */
	private static final String ACTION = "worn_trinkets";
	/** On a player whose worn trinkets are hidden; absent (the default) while they show. */
	public static AttachmentType<Unit> HIDDEN;
	/** Choices that came too soon after the last change, by player: each waits for its next allowed tick. */
	private static final Map<UUID, Boolean> PENDING = new HashMap<>();

	private WornDisplay() {
	}

	static void register() {
		HIDDEN = AttachmentRegistry.<Unit>builder().copyOnDeath().syncWith(Unit.STREAM_CODEC, AttachmentSyncPredicate.all())
				.buildAndRegister(Jugcraft.id("worn_trinkets_hidden"));
		PayloadTypeRegistry.serverboundPlay().register(WornDisplayPayload.TYPE, WornDisplayPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(WornDisplayPayload.TYPE, (payload, context) -> choose(context.player(), payload.shown()));
		ServerTickEvents.END_SERVER_TICK.register(WornDisplay::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> PENDING.remove(handler.getPlayer().getUUID()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> PENDING.clear());
	}

	/** Whether {@code wearer} has hidden their worn trinkets (only players choose; anything else shows them). */
	public static boolean hidden(Entity wearer) {
		return wearer.hasAttached(HIDDEN);
	}

	/** Whether a choice from {@code player} is waiting for its allowed tick. */
	public static boolean pending(ServerPlayer player) {
		return PENDING.containsKey(player.getUUID());
	}

	/**
	 * {@code player} chooses whether their worn trinkets are shown (the receiver and tests call this). The newest choice
	 * replaces any still waiting; one that changes nothing is dropped; a change is made now if the last was at least
	 * {@value #CHANGE_TICKS} ticks ago, or else waits.
	 */
	public static void choose(ServerPlayer player, boolean shown) {
		PENDING.remove(player.getUUID());
		if (shown != hidden(player)) {
			return;
		}
		if (RateGate.allow(player, ACTION, CHANGE_TICKS)) {
			set(player, shown);
		} else {
			PENDING.put(player.getUUID(), shown);
		}
	}

	private static void tick(MinecraftServer server) {
		if (PENDING.isEmpty()) {
			return;
		}
		Iterator<Map.Entry<UUID, Boolean>> waiting = PENDING.entrySet().iterator();
		while (waiting.hasNext()) {
			Map.Entry<UUID, Boolean> choice = waiting.next();
			ServerPlayer player = server.getPlayerList().getPlayer(choice.getKey());
			if (player == null) {
				waiting.remove();
			} else if (RateGate.allow(player, ACTION, CHANGE_TICKS)) {
				waiting.remove();
				set(player, choice.getValue());
			}
		}
	}

	private static void set(ServerPlayer player, boolean shown) {
		if (shown) {
			player.removeAttached(HIDDEN);
		} else {
			player.setAttached(HIDDEN, Unit.INSTANCE);
		}
	}
}
