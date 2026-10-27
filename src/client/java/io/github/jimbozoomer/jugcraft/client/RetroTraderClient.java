package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.world.RetroTraderJam;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Iterator;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;

/**
 * The client half of the Retro Trader's stomp-clap ({@link RetroTraderJam}) and his faces: keeps when each trader's
 * jam started (from the server's message), plays the stomps and claps on their beats, and works out his
 * {@link RetroTraderPose} whenever his render state is extracted.
 */
public final class RetroTraderClient {
	/** A jam under way: the client game time it starts and the last of its ticks whose sounds were played. */
	private static final class Jam {
		final long start;
		int played = -1;

		Jam(long start) {
			this.start = start;
		}
	}

	/** Jams by entity id (client thread only). */
	private static final Int2ObjectMap<Jam> JAMS = new Int2ObjectOpenHashMap<>();
	/** How near (blocks) the player must be for him to smile, and to be the one talking while trading. */
	private static final double NEAR = 6.0;
	private static final double TALKING = 5.0;

	private RetroTraderClient() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(RetroTraderJam.Payload.TYPE, (payload, context) -> {
			ClientLevel level = context.client().level;
			if (level != null) {
				JAMS.put(payload.entityId(), new Jam(level.getGameTime() + RetroTraderJam.CLIENT_DELAY));
			}
		});
		ClientTickEvents.END_CLIENT_TICK.register(RetroTraderClient::tick);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> JAMS.clear());
	}

	/** Plays each jam's stomps and claps on their ticks and forgets finished jams. */
	private static void tick(Minecraft client) {
		ClientLevel level = client.level;
		if (level == null) {
			JAMS.clear();
			return;
		}
		long now = level.getGameTime();
		for (Iterator<Int2ObjectMap.Entry<Jam>> it = JAMS.int2ObjectEntrySet().iterator(); it.hasNext();) {
			Int2ObjectMap.Entry<Jam> entry = it.next();
			Jam jam = entry.getValue();
			Entity trader = level.getEntity(entry.getIntKey());
			long elapsed = now - jam.start;
			if (trader == null || elapsed > RetroTraderJam.LENGTH) {
				it.remove();
				continue;
			}
			// Every tick since the last one played, so a skipped client tick does not drop a beat.
			for (int t = Math.max(jam.played + 1, 0); t <= elapsed; t++) {
				play(level, trader, RetroTraderJam.beat(t), t);
			}
			jam.played = (int) Math.max(jam.played, elapsed);
		}
	}

	private static void play(ClientLevel level, Entity trader, RetroTraderJam.Beat beat, int tick) {
		switch (beat) {
			case STOMP_RIGHT, STOMP_LEFT -> sound(level, trader, RetroTraderJam.STOMP, 0.9F, tick);
			case CLAP -> sound(level, trader, RetroTraderJam.CLAP, 0.8F, tick);
			case FINALE -> {
				sound(level, trader, RetroTraderJam.STOMP, 1.0F, tick);
				sound(level, trader, RetroTraderJam.CLAP, 1.0F, tick + 1);
			}
			case NONE -> {
			}
		}
	}

	private static void sound(ClientLevel level, Entity trader, SoundEvent sound, float volume, int tick) {
		// A little pitch wobble, fixed per beat, so the takes do not sound machine-made.
		float pitch = 0.96F + ((tick * 7 + trader.getId()) % 9) * 0.01F;
		level.playLocalSound(trader.getX(), trader.getY(), trader.getZ(), sound, SoundSource.NEUTRAL, volume, pitch, false);
	}

	/** Called at the end of VillagerRenderer#extractRenderState (see RetroTraderRendererMixin). */
	public static void extract(Villager villager, VillagerRenderState state, float partialTick) {
		RetroTraderPose.Holder holder = (RetroTraderPose.Holder) state;
		if (state.isBaby || !RetroTraderJam.isTrader(villager)) {
			holder.jugcraft$setRetroTrader(null);
			return;
		}
		float jam = -1;
		Jam started = JAMS.get(villager.getId());
		if (started != null) {
			jam = villager.level().getGameTime() - started.start + partialTick;
		}
		RetroTraderPose pose = new RetroTraderPose().update(jam, state.ageInTicks, villager.getId(), mood(villager, state));
		if (pose.freeArms) {
			state.heldItem.clear(); // his hands are busy
		}
		holder.jugcraft$setRetroTrader(pose);
	}

	private static RetroTraderPose.Mood mood(Villager villager, VillagerRenderState state) {
		if (state.isUnhappy) {
			return RetroTraderPose.Mood.UNHAPPY;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return RetroTraderPose.Mood.IDLE;
		}
		double distance = villager.distanceToSqr(client.player);
		if (client.gui.screen() instanceof MerchantScreen && distance < TALKING * TALKING) {
			return RetroTraderPose.Mood.TALKING;
		}
		return distance < NEAR * NEAR ? RetroTraderPose.Mood.NEAR : RetroTraderPose.Mood.IDLE;
	}
}
