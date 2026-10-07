package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.ArrayDeque;
import java.util.Deque;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The lantern festival: when {@value #FESTIVAL_LANTERNS} sky lanterns have been let go within {@value #FESTIVAL_RADIUS}
 * blocks of each other in {@value #FESTIVAL_WINDOW} ticks (by one player or many), the sky fills with lanterns: every
 * player within {@value #FESTIVAL_RADIUS} blocks is told so, earns A Sky Full of Wishes and gets Luck for
 * {@value #LUCK_TICKS} ticks. The same place holds no second festival for {@value #FESTIVAL_COOLDOWN} ticks (a day).
 *
 * <p>The server remembers at most {@value #MEMORY} recent releases and the festivals of the last day, in memory only:
 * a restart forgets them (a festival in progress has to start its count again). Each release looks through that short
 * list once.
 */
public final class SkyLanterns {
	public static final int FESTIVAL_LANTERNS = 8;
	public static final int FESTIVAL_RADIUS = 32;
	public static final int FESTIVAL_WINDOW = 2400;
	public static final int FESTIVAL_COOLDOWN = 24000;
	public static final int LUCK_TICKS = 6000;
	public static final int MEMORY = 256;

	private record Release(ResourceKey<Level> dimension, Vec3 at, long time) {
	}

	private static final Deque<Release> RELEASES = new ArrayDeque<>();
	private static final Deque<Release> FESTIVALS = new ArrayDeque<>();

	private SkyLanterns() {
	}

	static void register() {
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> forget());
	}

	/** Forgets every release and festival (when the server stops, so another world starts afresh). */
	public static synchronized void forget() {
		RELEASES.clear();
		FESTIVALS.clear();
	}

	/** Counts a lantern let go at {@code at}; returns whether that started a festival. */
	public static synchronized boolean released(ServerLevel level, Vec3 at) {
		long now = level.getGameTime();
		RELEASES.removeIf(release -> now - release.time() > FESTIVAL_WINDOW || release.time() > now);
		FESTIVALS.removeIf(festival -> now - festival.time() > FESTIVAL_COOLDOWN || festival.time() > now);
		Release release = new Release(level.dimension(), at, now);
		RELEASES.addLast(release);
		while (RELEASES.size() > MEMORY) {
			RELEASES.removeFirst();
		}
		double reach = (double) FESTIVAL_RADIUS * FESTIVAL_RADIUS;
		long near = RELEASES.stream().filter(other -> other.dimension() == release.dimension() && other.at().distanceToSqr(at) <= reach).count();
		boolean recent = FESTIVALS.stream().anyMatch(other -> other.dimension() == release.dimension() && other.at().distanceToSqr(at) <= reach);
		if (near < FESTIVAL_LANTERNS || recent) {
			return false;
		}
		FESTIVALS.addLast(release);
		while (FESTIVALS.size() > MEMORY) {
			FESTIVALS.removeFirst();
		}
		celebrate(level, at);
		return true;
	}

	private static void celebrate(ServerLevel level, Vec3 at) {
		for (ServerPlayer player : level.players()) {
			if (!player.isSpectator() && player.position().distanceToSqr(at) <= (double) FESTIVAL_RADIUS * FESTIVAL_RADIUS) {
				player.sendSystemMessage(Component.translatable("message.jugcraft.sky_lantern.festival"));
				player.addEffect(new MobEffectInstance(MobEffects.LUCK, LUCK_TICKS));
				TrickOrTreat.award(player, "lantern_festival");
			}
		}
	}
}
