package io.github.jimbozoomer.jugcraft.world;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.netty.buffer.ByteBuf;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;

/**
 * The Retro Trader's stomp-clap: now and then, after a player has traded with him, he breaks into a little folk
 * stomp-and-clap once the trading screen closes. It is purely cosmetic: no items, prices or reputation change.
 *
 * <p>The server decides (a completed trade marks him; when the trading stops, {@link #CHANCE} rolls, at most once per
 * {@link #COOLDOWN}) and tells every client tracking him with a {@link Payload}. Clients play the routine themselves
 * from the shared {@link #beat beat timeline}: the animation, his faces and the stomp and clap sounds. Nothing is
 * saved; a client that starts tracking him halfway through simply does not see that jam.
 */
public final class RetroTraderJam {
	/** The chance that a trading session with at least one completed trade ends in a jam. */
	public static final float CHANCE = 0.35F;
	/** Ticks before the same trader may jam again. */
	public static final int COOLDOWN = 600;
	/** Ticks between beats: 80 beats a minute. */
	public static final int BEAT = 15;
	/** Ticks before the first beat while he unfolds his arms. */
	public static final int INTRO = 12;
	/** Beats in the groove: stomp, clap, stomp, clap, twice over. */
	public static final int BEATS = 8;
	/** The tick of the closing overhead clap, one beat after the groove. */
	public static final int FINALE = INTRO + BEATS * BEAT;
	/** The whole routine, in ticks, including folding his arms again after the finale. */
	public static final int LENGTH = FINALE + 18;
	/** Ticks the client waits after the message, so the trading screen has closed before he starts. */
	public static final int CLIENT_DELAY = 4;

	public static SoundEvent STOMP;
	public static SoundEvent CLAP;

	/** Traders with a completed trade in the current trading session (server thread only). */
	private static final Set<Villager> TRADED = Collections.newSetFromMap(new WeakHashMap<>());
	/** The game time of each trader's last jam (server thread only). */
	private static final Map<Villager, Long> LAST = new WeakHashMap<>();
	/** Game tests set this so the chance always passes. */
	public static boolean alwaysForTests;

	private RetroTraderJam() {
	}

	/** Server to client: the villager with this entity id starts his stomp-clap. */
	public record Payload(int entityId) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(Jugcraft.id("retro_trader_jam"));
		public static final StreamCodec<ByteBuf, Payload> CODEC = ByteBufCodecs.VAR_INT.map(Payload::new, Payload::entityId);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** What happens on one tick of the routine. */
	public enum Beat {
		NONE, STOMP_RIGHT, STOMP_LEFT, CLAP, FINALE
	}

	public static void register() {
		STOMP = sound("entity.retro_trader.stomp");
		CLAP = sound("entity.retro_trader.clap");
		PayloadTypeRegistry.clientboundPlay().register(Payload.TYPE, Payload.CODEC);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			TRADED.clear();
			LAST.clear();
		});
	}

	private static SoundEvent sound(String path) {
		Identifier id = Jugcraft.id(path);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	/**
	 * The routine's beat at {@code tick} ticks after it starts: the groove alternates a stomp (right foot, then left)
	 * and a clap, and the finale is an overhead clap with a stomp.
	 */
	public static Beat beat(int tick) {
		if (tick == FINALE) {
			return Beat.FINALE;
		}
		int sinceFirst = tick - INTRO;
		if (sinceFirst < 0 || sinceFirst % BEAT != 0 || sinceFirst / BEAT >= BEATS) {
			return Beat.NONE;
		}
		int beat = sinceFirst / BEAT;
		if (beat % 2 == 1) {
			return Beat.CLAP;
		}
		return beat % 4 == 0 ? Beat.STOMP_RIGHT : Beat.STOMP_LEFT;
	}

	/** Whether {@code villager} is a Retro Trader. */
	public static boolean isTrader(Villager villager) {
		return villager.getVillagerData().profession().is(RetroTrader.PROFESSION);
	}

	/** A trade with {@code trader} was completed (called on the server by AbstractVillager#notifyTrade). */
	public static void traded(AbstractVillager trader) {
		if (trader instanceof Villager villager && villager.level() instanceof ServerLevel && isTrader(villager)) {
			TRADED.add(villager);
		}
	}

	/**
	 * {@code trader} stopped trading (the screen closed): if he traded this session, roll for a jam. Returns whether one
	 * started.
	 */
	public static boolean stoppedTrading(AbstractVillager trader) {
		if (!(trader instanceof Villager villager) || !TRADED.remove(villager) || !(villager.level() instanceof ServerLevel level)
				|| !villager.isAlive() || !isTrader(villager)) {
			return false;
		}
		long now = level.getGameTime();
		Long last = LAST.get(villager);
		if (last != null && now - last < COOLDOWN) {
			return false;
		}
		if (!alwaysForTests && villager.getRandom().nextFloat() >= CHANCE) {
			return false;
		}
		LAST.put(villager, now);
		villager.getNavigation().stop();
		Payload payload = new Payload(villager.getId());
		for (ServerPlayer player : PlayerLookup.tracking(villager)) {
			ServerPlayNetworking.send(player, payload);
		}
		return true;
	}

	/** The game time of {@code villager}'s last jam, or -1 if he has not jammed on this server run. */
	public static long lastJam(Villager villager) {
		return LAST.getOrDefault(villager, -1L);
	}
}
