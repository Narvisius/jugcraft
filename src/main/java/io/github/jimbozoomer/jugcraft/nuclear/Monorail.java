package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * A diesel room's monorail hoist (round 31 part 3; Justin: "Trolley moves", "One step per click"): its trolley runs
 * along the beam and its hook goes up and down from the pendant station (HoistPendantBlock), a block a click, at a
 * block a second; STOP holds it where it is. Its trolley's place along the beam and the hook's drop below its rest
 * (blocks), each with where it set off from and when, are the monorail's (saved and sent to the players, who draw it
 * moving: client/DieselParts.java hoist).
 */
public final class Monorail {
	/** The trolley's travel along the beam (blocks, the beam's own x) and the hook's drop below its rest. */
	public static final double MIN_X = 0.6, MAX_X = EquipmentKind.EDG_MONORAIL.wide - 0.6, MAX_DROP = 7.0;
	/** Ticks a block of travel takes, and how far from a pendant its monorail may be. */
	public static final int TICKS_PER_BLOCK = 20, REACH = 24;
	public static final int STOP = 0, UP = 1, DOWN = 2, EAST = 3, WEST = 4;

	private Monorail() {
	}

	/** Where something moving from {@code from} at {@code at} to {@code to} is at game time {@code now}. */
	public static double at(double from, double to, long at, double now) {
		double ticks = Math.abs(to - from) * TICKS_PER_BLOCK;
		if (ticks <= 0) {
			return to;
		}
		double f = Math.max(0, Math.min(1, (now - at) / ticks));
		return from + (to - from) * f;
	}

	/** The trolley's place along the beam now. */
	public static double trolley(EquipmentBlockEntity rail, double now) {
		return at(rail.hoistXFrom, rail.hoistX, rail.hoistXAt, now);
	}

	/** The hook's drop below its rest now. */
	public static double hook(EquipmentBlockEntity rail, double now) {
		return at(rail.hoistDropFrom, rail.hoistDrop, rail.hoistDropAt, now);
	}

	/** A pendant button pressed for a monorail: a step (or STOP). */
	public static void press(ServerLevel level, EquipmentBlockEntity rail, int button, @Nullable Player player) {
		long now = level.getGameTime();
		double x = trolley(rail, now), drop = hook(rail, now);
		switch (button) {
			case STOP -> {
				rail.hoistXFrom = rail.hoistX = x;
				rail.hoistDropFrom = rail.hoistDrop = drop;
				rail.hoistXAt = rail.hoistDropAt = now;
			}
			case EAST, WEST -> {
				double to = Math.max(MIN_X, Math.min(MAX_X, Math.round(rail.hoistX + (button == EAST ? 1 : -1))));
				if (to == rail.hoistX) {
					tell(player, Component.translatable("message.jugcraft.nuclear.hoist_end"));
					return;
				}
				rail.hoistXFrom = x;
				rail.hoistX = to;
				rail.hoistXAt = now;
				level.playSound(null, rail.getBlockPos(), SoundEvents.MINECART_RIDING, SoundSource.BLOCKS, 0.25F, 1.4F);
			}
			default -> {
				double to = Math.max(0, Math.min(MAX_DROP, Math.round(rail.hoistDrop + (button == DOWN ? 1 : -1))));
				if (to == rail.hoistDrop) {
					tell(player, Component.translatable(button == DOWN ? "message.jugcraft.nuclear.hoist_low" : "message.jugcraft.nuclear.hoist_high"));
					return;
				}
				rail.hoistDropFrom = drop;
				rail.hoistDrop = to;
				rail.hoistDropAt = now;
				level.playSound(null, rail.getBlockPos(), SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.8F, button == DOWN ? 0.8F : 1.1F);
			}
		}
		rail.changed();
	}

	/** The monorail nearest a pendant station, in reach, or null. */
	public static @Nullable EquipmentBlockEntity near(ServerLevel level, BlockPos pendant) {
		BlockPos best = null;
		for (Map.Entry<BlockPos, EquipmentRegistry.Entry> e : EquipmentRegistry.of(level).anchors().entrySet()) {
			if (e.getValue().kind() == EquipmentKind.EDG_MONORAIL && e.getKey().distSqr(pendant) <= (double) REACH * REACH
					&& (best == null || e.getKey().distSqr(pendant) < best.distSqr(pendant))) {
				best = e.getKey();
			}
		}
		return best != null && level.getBlockEntity(best) instanceof EquipmentBlockEntity rail && rail.kind() == EquipmentKind.EDG_MONORAIL ? rail : null;
	}

	private static void tell(@Nullable Player player, Component message) {
		if (player != null) {
			player.sendOverlayMessage(message);
		}
	}
}
