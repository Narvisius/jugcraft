package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * The diesel generator rooms' panels' controls (round 31 part 2; Justin: controls "Engine and breaker"; a LOCAL start's
 * breaker "Closes itself"; the 86G trips from "Handle and relay tests"; the emergency stop "Latches in"; a click on the
 * set "Show its status"). Laid out by tools/diesel_panels.py (model pixels of the piece facing north: x across the
 * whole piece, y up from the floor); what they show is client/EdgTelemetry.java's.
 *
 * <ul>
 * <li>The engine panel: MODE (OFF: nothing starts it, and it stops; LOCAL: START and STOP here only; AUTO: the control
 * room and safety injection start it), START, STOP, the emergency stop (stops it at once, from anywhere, and stays in
 * until pulled out), LAMP TEST.</li>
 * <li>The generator panel: the breaker control switch, TRIP on its left and CLOSE on its right (springing back to the
 * middle). Tripped, the loads drop and the engine runs on light; closed again (once up to speed), the load sequencer
 * starts the loads again from its first step.</li>
 * <li>The protection panel: each relay's test switch tests it (its target drops and the 86G lockout trips), the relay
 * itself resets its target; the 86G's handle trips it or resets it. Tripped, the 86G opens the breaker, stops the engine
 * and keeps it from starting or the breaker from closing until it is reset.</li>
 * </ul>
 */
public final class DieselPanels {
	/** MODE. */
	public static final int OFF = 0, LOCAL = 1, AUTO = 2;
	/** Where a start or stop comes from: the control room (or safety injection), the engine panel, a trip. */
	public static final int REMOTE = 0, PANEL = 1, FORCE = 2;
	/** The panels (EdgPanelBlock). */
	public static final int ENGINE = 0, GENERATOR = 1, PROTECTION = 2;
	public static final String[] MODES = {"OFF", "LOCAL", "AUTO"};
	public static final String[] RELAYS = {"87G", "51V", "32", "40", "46", "81", "59", "64G"};
	/** How long (ticks) the breaker switch is held turned, and LAMP TEST lights everything. */
	public static final int HELD = 10, LAMP_TEST = 60;

	// Where things are (tools/diesel_panels.py: EP_*, GP_BKR, PR_*).
	static final double[] MODE_HUB = {2.95, 5.45}, START = {6.55, 5.95}, STOP = {8.95, 5.95}, ESTOP = {12.15, 5.6}, TEST = {12.7, 8.8};
	static final double[] BREAKER = {16.0, 9.35};
	static final double[] RELAY_X = {1.4, 8.8}, RELAY_TOP = {27.3, 22.5, 17.7, 12.9}, LOR = {8.0, 6.05};
	static final double RELAY_W = 5.8, RELAY_H = 3.7, RELAY_TS = 0.85;

	private DieselPanels() {
	}

	/** Its name: EDG 1A, or 1B (its anchor's variant, as its signs). */
	public static String name(EquipmentBlockEntity edg) {
		return "EDG 1" + (StartingAir.label(edg) == 1 ? "B" : "A");
	}

	/** Whether the control room (and safety injection) can start it: MODE at AUTO, its 86G and emergency stop reset. */
	public static boolean remoteReady(EquipmentBlockEntity edg) {
		return edg.edgMode == AUTO && !edg.lockout && !edg.estop;
	}

	/** Why a start or stop {@code from} there can't be done (null: it can). */
	public static @Nullable Component refuse(EquipmentBlockEntity edg, boolean run, int from) {
		if (from == FORCE) {
			return run ? Component.translatable("message.jugcraft.nuclear.edg_mode_off", name(edg)) : null;
		}
		if (run && edg.lockout) {
			return Component.translatable("message.jugcraft.nuclear.edg_lockout", name(edg));
		}
		if (run && edg.estop) {
			return Component.translatable("message.jugcraft.nuclear.edg_estop_in", name(edg));
		}
		if (run && edg.co2Phase == Co2System.DISCHARGED) {
			// (Round 31 part 3: no air for it to run on until the CO2 system is reset.)
			return Component.translatable("message.jugcraft.nuclear.edg_co2", name(edg));
		}
		if (run && edg.edgMode == OFF) {
			return Component.translatable("message.jugcraft.nuclear.edg_mode_off", name(edg));
		}
		if (run && edg.getLevel() instanceof net.minecraft.server.level.ServerLevel level
				&& !DcRoom.feederLive(level, edg.getBlockPos(), StartingAir.label(edg), DcRoom.EDG_CTL)) {
			// (Older rooms round 3: its 125 VDC control power, its train's DC switchboard's feeder 07.)
			return Component.translatable("message.jugcraft.nuclear.edg_no_dc", name(edg), "72-1" + (StartingAir.label(edg) == 1 ? "B" : "A") + "-07");
		}
		if (from == REMOTE && edg.edgMode != AUTO) {
			return Component.translatable("message.jugcraft.nuclear.edg_not_auto", name(edg), MODES[edg.edgMode]);
		}
		if (from == PANEL && edg.edgMode != LOCAL) {
			return Component.translatable("message.jugcraft.nuclear.edg_not_local", name(edg), MODES[edg.edgMode]);
		}
		return null;
	}

	/** How it stands, for a click on the set: running or not, its breaker, air, MODE, and what keeps it from starting. */
	public static Component status(EquipmentBlockEntity edg, long now) {
		String state;
		if (edg.running) {
			state = edg.breakerClosed(now) ? "running, breaker closed" : "running, breaker open";
		} else {
			state = "stopped";
		}
		StringBuilder flags = new StringBuilder();
		if (edg.lockout) {
			flags.append(", 86G TRIPPED");
		}
		if (edg.estop) {
			flags.append(", EMERG STOP IN");
		}
		if (edg.fuelOut || edg.airFail) {
			flags.append(", FAIL TO START");
		}
		flags.append(Co2System.state(edg));
		return Component.translatable("message.jugcraft.nuclear.edg_status", name(edg), state, String.format(Locale.ROOT, "%.0f", edg.air),
				MODES[edg.edgMode], flags.toString());
	}

	/**
	 * A click on one of the panels ({@code panel}: ENGINE, GENERATOR, PROTECTION) at (x, y) on its front (model pixels of
	 * the whole piece facing north), for its generator {@code edg}.
	 */
	public static InteractionResult click(ServerLevel level, BlockPos at, Player player, EquipmentBlockEntity edg, int panel, double x, double y) {
		long now = level.getGameTime();
		switch (panel) {
			case ENGINE -> {
				if (near(x, y, MODE_HUB, 1.6)) {
					mode(level, at, player, edg, player.isShiftKeyDown());
				} else if (near(x, y, START, 0.95)) {
					sound(level, at, SoundEvents.STONE_BUTTON_CLICK_ON, 1.3F);
					EquipmentPlacement.dieselSwitch(level, edg, player, true, PANEL);
				} else if (near(x, y, STOP, 0.95)) {
					sound(level, at, SoundEvents.STONE_BUTTON_CLICK_ON, 1.1F);
					if (edg.running) {
						EquipmentPlacement.dieselSwitch(level, edg, player, false, PANEL);
					} else {
						player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.edg_stopped", name(edg)));
					}
				} else if (Math.abs(x - ESTOP[0]) <= 1.6 && Math.abs(y - ESTOP[1]) <= 1.75) {
					estop(level, at, player, edg);
				} else if (near(x, y, TEST, 0.75)) {
					sound(level, at, SoundEvents.STONE_BUTTON_CLICK_ON, 1.5F);
					edg.lampTestUntil = now + LAMP_TEST;
					edg.changed();
				} else {
					player.sendOverlayMessage(status(edg, now));
				}
			}
			case GENERATOR -> {
				if (Math.abs(x - BREAKER[0]) <= 2.1 && y >= BREAKER[1] - 2.4 && y <= BREAKER[1] + 1.9) {
					breakerSwitch(level, at, player, edg, x >= BREAKER[0]);
				} else {
					player.sendOverlayMessage(status(edg, now));
				}
			}
			default -> {
				if (Math.abs(x - LOR[0]) <= 1.9 && y >= LOR[1] - 2.05 && y <= LOR[1] + 1.95) {
					if (edg.lockout) {
						resetLockout(level, at, player, edg);
					} else {
						tripLockout(level, at, edg, player, Component.translatable("message.jugcraft.nuclear.edg_lor_manual", name(edg)));
					}
					return InteractionResult.SUCCESS_SERVER;
				}
				for (int i = 0; i < RELAYS.length; i++) {
					double x0 = RELAY_X[i % 2], top = RELAY_TOP[i / 2], y0 = top - RELAY_H;
					if (x < x0 || x > x0 + RELAY_W) {
						continue;
					}
					if (y >= y0 - 0.15 - RELAY_TS && y < y0) {
						relayTest(level, at, player, edg, i);
						return InteractionResult.SUCCESS_SERVER;
					}
					if (y >= y0 && y <= top) {
						relayReset(level, at, player, edg, i);
						return InteractionResult.SUCCESS_SERVER;
					}
				}
				player.sendOverlayMessage(status(edg, now));
			}
		}
		return InteractionResult.SUCCESS_SERVER;
	}

	private static boolean near(double x, double y, double[] c, double r) {
		return (x - c[0]) * (x - c[0]) + (y - c[1]) * (y - c[1]) <= r * r;
	}

	private static void sound(ServerLevel level, BlockPos at, SoundEvent sound, float pitch) {
		level.playSound(null, at, sound, SoundSource.BLOCKS, 0.6F, pitch);
	}

	/** MODE one step round (OFF, LOCAL, AUTO; back with {@code back}); turned to OFF, it stops. */
	public static void mode(ServerLevel level, BlockPos at, @Nullable Player player, EquipmentBlockEntity edg, boolean back) {
		edg.edgMode = Math.floorMod(edg.edgMode + (back ? -1 : 1), 3);
		edg.changed();
		sound(level, at, SoundEvents.LEVER_CLICK, 1.3F);
		if (player != null) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.edg_mode", name(edg), MODES[edg.edgMode]));
		}
		if (edg.edgMode == OFF && edg.running) {
			EquipmentPlacement.dieselSwitch(level, edg, null, false, FORCE);
		}
	}

	/** The emergency stop: pushed in, it stops the engine and stays in; pushed again, it is pulled out. */
	public static void estop(ServerLevel level, BlockPos at, @Nullable Player player, EquipmentBlockEntity edg) {
		edg.estop = !edg.estop;
		edg.changed();
		sound(level, at, SoundEvents.STONE_BUTTON_CLICK_ON, edg.estop ? 0.6F : 0.9F);
		if (player != null) {
			player.sendOverlayMessage(Component.translatable(edg.estop ? "message.jugcraft.nuclear.edg_estop" : "message.jugcraft.nuclear.edg_estop_out", name(edg)));
		}
		if (edg.estop && edg.running) {
			EquipmentPlacement.dieselSwitch(level, edg, null, false, FORCE);
		}
	}

	/** The breaker control switch turned to CLOSE ({@code close}) or TRIP. */
	public static void breakerSwitch(ServerLevel level, BlockPos at, @Nullable Player player, EquipmentBlockEntity edg, boolean close) {
		long now = level.getGameTime();
		edg.breakerSw = close ? 2 : 1;
		edg.breakerSwAt = now;
		sound(level, at, SoundEvents.LEVER_CLICK, close ? 1.0F : 0.8F);
		String message;
		if (!close) {
			message = edg.breakerAt >= 0 ? "edg_bkr_tripped" : "edg_bkr_open";
			if (edg.breakerAt >= 0) {
				edg.breakerAt = -1;
				level.playSound(null, at, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.8F, 0.7F);
			}
		} else if (edg.lockout) {
			message = "edg_lockout";
		} else if (!edg.running) {
			message = "edg_bkr_not_running";
		} else if (now - edg.runningSince < DieselFuel.BREAKER * 20) {
			message = "edg_bkr_not_ready";
		} else if (edg.breakerClosed(now)) {
			message = "edg_bkr_already";
		} else {
			edg.breakerAt = now;
			message = "edg_bkr_closed";
			level.playSound(null, at, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.8F, 0.9F);
		}
		edg.changed();
		if (player != null) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear." + message, name(edg)));
		}
	}

	/** Relay i tested from its test switch: its target drops and it trips the 86G. */
	public static void relayTest(ServerLevel level, BlockPos at, @Nullable Player player, EquipmentBlockEntity edg, int i) {
		sound(level, at, SoundEvents.LEVER_CLICK, 1.6F);
		edg.relayTargets |= 1 << i;
		tripLockout(level, at, edg, player, Component.translatable("message.jugcraft.nuclear.edg_relay_test", name(edg), RELAYS[i]));
	}

	/** Relay i's target reset (it can drop again). */
	public static void relayReset(ServerLevel level, BlockPos at, @Nullable Player player, EquipmentBlockEntity edg, int i) {
		sound(level, at, SoundEvents.STONE_BUTTON_CLICK_ON, 1.6F);
		boolean was = (edg.relayTargets & 1 << i) != 0;
		edg.relayTargets &= ~(1 << i);
		edg.changed();
		if (player != null) {
			player.sendOverlayMessage(Component.translatable(was ? "message.jugcraft.nuclear.edg_relay_reset" : "message.jugcraft.nuclear.edg_relay_ok",
					name(edg), RELAYS[i]));
		}
	}

	/** The 86G lockout tripped: the breaker opens, the engine stops, and nothing starts it until it is reset. */
	public static void tripLockout(ServerLevel level, BlockPos at, EquipmentBlockEntity edg, @Nullable Player player, Component why) {
		boolean was = edg.lockout;
		edg.lockout = true;
		edg.breakerAt = -1;
		edg.changed();
		if (!was) {
			level.playSound(null, at, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.6F);
		}
		if (edg.running) {
			EquipmentPlacement.dieselSwitch(level, edg, null, false, FORCE);
		}
		if (player != null) {
			player.sendOverlayMessage(why);
		}
	}

	/** The 86G's handle turned back to RESET. */
	public static void resetLockout(ServerLevel level, BlockPos at, @Nullable Player player, EquipmentBlockEntity edg) {
		edg.lockout = false;
		edg.changed();
		sound(level, at, SoundEvents.LEVER_CLICK, 0.9F);
		if (player != null) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.edg_lor_reset", name(edg)));
		}
	}
}
