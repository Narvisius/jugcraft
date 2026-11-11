package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.Locale;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

/**
 * The switchgear room's log desk laptop (older rooms round 1; tools/switchgear_room.py laptop_front): the trip unit
 * viewer, reading the nearest load centre's breakers (LoadCentre): its title bar the lineup and its bus volts, a line a
 * breaker (its name, its state, its amps), the transformer's line at its foot. The client writes it on the screen
 * (client/EdgTelemetry.java sgr); a click on the laptop says it in a line.
 */
public final class SwitchgearRoom {
	public static final String LAPTOP = "sgr_desk_laptop";
	/** How far the laptop looks for its load centre (the room's two face each other across the aisle). */
	public static final int REACH = 24;
	/** The breakers on the screen: the main and the eight working feeders (the spares left off). */
	public static final int ROWS = 9;
	public static final int CLOSED = 0, OPEN = 1, TRIPPED = 2, TEST = 3, DISCONNECTED = 4, PICKUP = 5;
	static final String[] WORDS = {"CLOSED", "OPEN", "TRIPPED", "TEST", "DISC", "PICKUP"};
	/** The colour each state is written in (tools/switchgear_room.py: g green, w white, y amber). */
	static final char[] COLOURS = {'g', 'w', 'y', 'y', 'w', 'y'};

	private SwitchgearRoom() {
	}

	public static boolean isLaptop(BlockState state) {
		return state.getBlock() == PlantInterior.BLOCKS.get(LAPTOP);
	}

	/** The load centre nearest a spot within reach (its chunks' block entities), or null. */
	public static @Nullable EquipmentBlockEntity nearest(Level level, BlockPos near) {
		EquipmentBlockEntity best = null;
		double bestD = (double) REACH * REACH;
		int r = (REACH >> 4) + 1;
		for (int cx = (near.getX() >> 4) - r; cx <= (near.getX() >> 4) + r; cx++) {
			for (int cz = (near.getZ() >> 4) - r; cz <= (near.getZ() >> 4) + r; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity be : chunk.getBlockEntities().values()) {
					if (be instanceof EquipmentBlockEntity e && e.lc != null && e.kind() == EquipmentKind.SWITCHGEAR_LINEUP) {
						double d = e.middle().distanceToSqr(near.getX() + 0.5, near.getY() + 0.5, near.getZ() + 0.5);
						if (d <= bestD) {
							bestD = d;
							best = e;
						}
					}
				}
			}
		}
		return best;
	}

	/** A breaker's state as its trip unit shows it. */
	public static int status(EquipmentBlockEntity e, int b) {
		LoadCentre.State s = e.lc;
		boolean closed = Switchgear.closed(e, b);
		if (s.rack[b] == LoadCentre.DISCONNECT) {
			return DISCONNECTED;
		}
		if (!closed && (s.tripped >> b & 1) != 0) {
			return TRIPPED;
		}
		if (s.rack[b] == LoadCentre.TEST) {
			return TEST;
		}
		if (closed && (s.pickup >> b & 1) != 0) {
			return PICKUP;
		}
		return closed ? CLOSED : OPEN;
	}

	/** A breaker's name on the screen: its number and its load (the lineup's tag in the title bar). */
	public static String name(String tag, int b) {
		String load = switch (b) {
			case 0 -> "MAIN";
			case 1, 2, 3, 4 -> "MCC " + tag + b;
			case 5 -> "INVERTER";
			case 6 -> "DC MCC";
			case 7 -> "CR HVAC";
			case 8 -> "LIGHTING";
			default -> "SPARE";
		};
		return String.format(Locale.ROOT, "%02d %s", b + 1, load);
	}

	/** The screen's lines into a panel's text channels (sgr_t, sgr_v; sgr_n, sgr_g / sgr_w / sgr_y and sgr_a a row; sgr_f, sgr_x). */
	public static void screen(@Nullable EquipmentBlockEntity e, Map<String, String> t) {
		if (e == null || e.lc == null) {
			t.put("sgr_t", "TRIP UNIT VIEWER");
			t.put("sgr_n0", "NO LOAD CENTER IN REACH");
			return;
		}
		LoadCentre.State s = e.lc;
		String tag = LoadCentre.tag(e);
		t.put("sgr_t", "LC " + tag + "  TRIP UNITS");
		t.put("sgr_v", s.volts > 0 ? s.volts + " V" : "BUS DEAD");
		for (int b = 0; b < ROWS; b++) {
			int st = status(e, b);
			t.put("sgr_n" + b, name(tag, b));
			t.put("sgr_" + COLOURS[st] + b, WORDS[st]);
			t.put("sgr_a" + b, st == CLOSED || st == PICKUP ? s.amps[b] + " A" : "--");
		}
		t.put("sgr_f", String.format(Locale.ROOT, "XFMR %d C  FANS %s", Math.round(s.temp), s.fanManual ? "MAN" : s.fans ? "ON" : "AUTO"));
		if (s.alarm) {
			t.put("sgr_x", "ALARM");
		}
	}

	/** What a click on the laptop says: the lineup, its main, what is tripped or out, the transformer. */
	public static String describe(@Nullable EquipmentBlockEntity e) {
		if (e == null || e.lc == null) {
			return "no load center in reach";
		}
		LoadCentre.State s = e.lc;
		String tag = LoadCentre.tag(e);
		StringBuilder out = new StringBuilder("LC " + tag + ", main " + WORDS[status(e, 0)] + " " + s.amps[0] + " A");
		int tripped = 0, test = 0;
		for (int b = 1; b < LoadCentre.N; b++) {
			int st = status(e, b);
			tripped += st == TRIPPED ? 1 : 0;
			test += st == TEST ? 1 : 0;
		}
		if (tripped > 0) {
			out.append(", ").append(tripped).append(" tripped");
		}
		if (test > 0) {
			out.append(", ").append(test).append(" in TEST");
		}
		out.append(String.format(Locale.ROOT, ", transformer %d C%s", Math.round(s.temp), s.alarm ? " (ALARM)" : ""));
		return out.toString();
	}
}
