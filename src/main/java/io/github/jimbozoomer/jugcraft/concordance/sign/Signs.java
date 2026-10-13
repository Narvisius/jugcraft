package io.github.jimbozoomer.jugcraft.concordance.sign;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Sends {@link Sign}s (roadmap step 27). Callers show a sign where, and only when, the thing it shows has happened: a
 * ritual step drawn from its pylons, a mixture searing, an upkeep missed. A sign goes to the players who can see the
 * place (they track its chunk) within {@link Presentation#FAR} blocks; each level sends at most {@value #PER_TICK} a
 * tick, and the same sign at the same block once a tick, so a large installation cannot flood its watchers. Warnings
 * have {@value #WARNING_RESERVE} more places a tick of their own, so a crowd of ordinary signs never crowds one out.
 * Server thread only.
 */
public final class Signs {
	public static final int PER_TICK = 48;
	public static final int WARNING_RESERVE = 16;

	/** A sign the server decided to show (for tests). */
	public record Shown(ServerLevel level, Sign sign, Vec3 at, @Nullable Vec3 from) {
	}

	/**
	 * One level's sending: at most {@value #PER_TICK} signs a tick, then up to {@value #WARNING_RESERVE} more warnings, and
	 * the same sign at the same block once a tick. Server thread only.
	 */
	public static final class Budget {
		private long time = Long.MIN_VALUE;
		private int sent;
		private int warnings;
		private final Set<String> shown = new HashSet<>();

		/** Whether {@code sign} at {@code block} may go at game time {@code now}; counts it if so. */
		public boolean admit(long now, Sign sign, long block) {
			if (now != time) {
				time = now;
				shown.clear();
				sent = 0;
				warnings = 0;
			}
			if (!shown.add(sign.id + "|" + block)) {
				return false;
			}
			if (sent < PER_TICK) {
				sent++;
				return true;
			}
			if (sign.kind.warns() && warnings < WARNING_RESERVE) {
				warnings++;
				return true;
			}
			return false;
		}
	}

	private static final List<Consumer<Shown>> OBSERVERS = new ArrayList<>();
	private static final Map<String, Budget> BUDGETS = new HashMap<>();

	private Signs() {
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(SignPayload.TYPE, SignPayload.CODEC);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> BUDGETS.clear());
	}

	/** Shows {@code sign} at the middle of {@code pos}, a little above it. */
	public static boolean show(ServerLevel level, BlockPos pos, Sign sign) {
		return show(level, Vec3.atCenterOf(pos).add(0.0, 0.6, 0.0), sign, null);
	}

	/** Shows {@code sign} travelling from the middle of {@code from} to the middle of {@code to}: a real transfer. */
	public static boolean flow(ServerLevel level, BlockPos from, BlockPos to) {
		return show(level, Vec3.atCenterOf(to).add(0.0, 0.6, 0.0), Sign.FLOW, Vec3.atCenterOf(from).add(0.0, 0.6, 0.0));
	}

	/**
	 * Shows {@code sign} at {@code at} (from {@code from}, for a transfer): observers learn of it, and it goes to the
	 * players who can see the place if this level's {@link Budget} admits it. Returns whether it was sent.
	 */
	public static boolean show(ServerLevel level, Vec3 at, Sign sign, @Nullable Vec3 from) {
		Shown shown = new Shown(level, sign, at, from);
		for (Consumer<Shown> observer : List.copyOf(OBSERVERS)) {
			observer.accept(shown);
		}
		BlockPos block = BlockPos.containing(at);
		Budget budget = BUDGETS.computeIfAbsent(level.dimension().identifier().toString(), unused -> new Budget());
		if (!budget.admit(level.getGameTime(), sign, block.asLong())) {
			return false;
		}
		SignPayload payload = new SignPayload(sign.ordinal(), at, Optional.ofNullable(from));
		double reach = Presentation.FAR * Presentation.FAR;
		for (ServerPlayer player : PlayerLookup.tracking(level, block)) {
			if (player.distanceToSqr(at) <= reach && ServerPlayNetworking.canSend(player, SignPayload.TYPE)) {
				ServerPlayNetworking.send(player, payload);
			}
		}
		return true;
	}

	/** Watches every sign the server decides to show, sent or not, until the returned handle is closed (game tests). */
	public static AutoCloseable observe(Consumer<Shown> observer) {
		OBSERVERS.add(observer);
		return () -> OBSERVERS.remove(observer);
	}
}
