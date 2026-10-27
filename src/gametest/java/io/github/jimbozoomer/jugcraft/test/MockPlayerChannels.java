package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Field;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;

/**
 * Drops what the server sends to game-test mock players. A mock player's connection ends in Netty's EmbeddedChannel
 * (vanilla's {@code makeMockServerPlayerInLevel} and the tests that build their own player the same way), which keeps
 * every packet written to it until something reads it, and no test does. The players stay logged in for the whole
 * run, so every packet sent to each of them (its command tree and recipes on joining, then every later player's join
 * message and player-list entry) stayed in the heap until the server suite ran out of its 4 GB. Each tick this empties
 * those channels, and every 2,400 ticks logs how many packets it has released and the heap in use; a connection that
 * is not an EmbeddedChannel (a real or integrated client's) is never touched.
 */
public final class MockPlayerChannels implements ModInitializer {
	private static final Field CONNECTION = field(ServerCommonPacketListenerImpl.class, Connection.class);
	private static final Field CHANNEL = field(Connection.class, Channel.class);
	private static long released;

	@Override
	public void onInitialize() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			int mock = 0;
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (channel(player) instanceof EmbeddedChannel embedded) {
					mock++;
					released += embedded.outboundMessages().size();
					embedded.releaseOutbound();
				}
			}
			if (server.getTickCount() % 2400 == 0) {
				// The heap in use beside it shows whether anything else still grows over the run.
				Runtime runtime = Runtime.getRuntime();
				Jugcraft.LOGGER.info("[mock-player-channels] {} mock players online; {} packets to them released so far; heap {} of {} MB in use",
						mock, released, (runtime.totalMemory() - runtime.freeMemory()) >> 20, runtime.maxMemory() >> 20);
			}
		});
	}

	private static Channel channel(ServerPlayer player) {
		if (player.connection == null) {
			return null;
		}
		try {
			return (Channel) CHANNEL.get(CONNECTION.get(player.connection));
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	private static Field field(Class<?> owner, Class<?> type) {
		for (Field field : owner.getDeclaredFields()) {
			if (field.getType() == type) {
				field.setAccessible(true);
				return field;
			}
		}
		throw new IllegalStateException(owner.getName() + " has no " + type.getSimpleName() + " field");
	}
}
