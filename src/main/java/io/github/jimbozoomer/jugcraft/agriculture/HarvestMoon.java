package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * The Harvest Moon: the nights of Halloween itself ({@link HalloweenSeason#harvestMoon()}, 31 October by default,
 * in the operator's time zone, on the server's clock) while the Halloween event runs. Under it, every Jugcraft crop,
 * gourd stem and giant pumpkin vine grows {@link #GROWTH_BONUS} times as fast, giant pumpkins swell twice as fast,
 * and lit carvings throw off sparks. Night is the overworld's ({@link #DUSK} to {@link #DAWN}).
 *
 * <p>The server works it out every {@link #CHECK_TICKS} ticks and keeps the answer, so a crop's random tick only
 * reads a field. When it changes, players are told (the moon rising or setting) and clients get a
 * {@link Payload} for the sparks; a player who joins gets the current state.
 */
public final class HarvestMoon {
	public static final float GROWTH_BONUS = 2.0F;
	public static final long DUSK = 13000;
	public static final long DAWN = 23000;
	public static final int CHECK_TICKS = 100;

	private static boolean active;
	/** What the server last told this client (set by the client's receiver; read by block particles). */
	public static volatile boolean clientActive;

	private HarvestMoon() {
	}

	/** Server to client: whether the Harvest Moon is up. */
	public record Payload(boolean active) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(Jugcraft.id("harvest_moon"));
		public static final StreamCodec<ByteBuf, Payload> CODEC = ByteBufCodecs.BOOL.map(Payload::new, Payload::active);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	static void register() {
		PayloadTypeRegistry.clientboundPlay().register(Payload.TYPE, Payload.CODEC);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % CHECK_TICKS == 0) {
				update(server);
			}
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> send(handler.getPlayer(), active));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> active = false);
	}

	/** Whether the Harvest Moon is up, as last worked out on the server. */
	public static boolean active() {
		return active;
	}

	/** Whether the Harvest Moon is up at {@code dayTime} on the overworld clock, on today's date (server clock). */
	public static boolean rising(long dayTime) {
		long hour = Math.floorMod(dayTime, TrickOrTreat.DAY);
		return HalloweenSeason.active() && JugcraftConfig.isFeatureEnabled(JugcraftAgriculture.FEATURE)
				&& HalloweenSeason.today().equals(HalloweenSeason.harvestMoon()) && hour >= DUSK && hour < DAWN;
	}

	/** Works the state out again; if it changed, tells every player. Returns the new state. */
	public static boolean update(MinecraftServer server) {
		return update(server, server.overworld().getOverworldClockTime());
	}

	/** Works the state out as if the overworld clock read {@code dayTime}; if it changed, tells every player. */
	public static boolean update(MinecraftServer server, long dayTime) {
		boolean now = rising(dayTime);
		if (now != active) {
			active = now;
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				player.sendSystemMessage(Component.translatable(now ? "message.jugcraft.harvest_moon.rises" : "message.jugcraft.harvest_moon.sets"));
				send(player, now);
			}
		}
		return active;
	}

	private static void send(ServerPlayer player, boolean state) {
		if (ServerPlayNetworking.canSend(player, Payload.TYPE)) {
			ServerPlayNetworking.send(player, new Payload(state));
		}
	}
}
