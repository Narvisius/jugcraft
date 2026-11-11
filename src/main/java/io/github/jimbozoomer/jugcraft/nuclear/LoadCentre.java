package io.github.jimbozoomer.jugcraft.nuclear;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The 480 V load centre rebuilt (older rooms round 1, the switchgear room; Justin: "Real 480 V switchgear", working
 * "Control switches", "Rack breakers out", "Overload trips", "Transformer fans", feeder amps from "Plant loads").
 * tools/load_centre.py draws it; nuclear/Switchgear.java still moves its power. What is kept here, on the lineup's block
 * entity (saved, and synced for client/LoadCentreParts.java):
 *
 * <ul>
 * <li>each breaker's racking position (CONNECT, TEST, DISCONNECT): only a racked-in breaker carries power; in TEST it
 * still opens and closes on its control power, out at DISCONNECT it is dead;
 * <li>each breaker's closing spring (discharged for a few seconds after it closes, while its motor charges it), its
 * operations count, its trip unit's TRIPPED target (an overload trip: it will not close until reset at the trip unit),
 * its control switch's handle (turned to TRIP or CLOSE for a moment, then back to NORMAL);
 * <li>each breaker's amps: the power it really carries (1 JE/t reads {@link #AMPS_PER_JE} A) plus the plant load it
 * feeds, worked out from the plant (its motor control centres' pumps and fans with the reactor's power, the control
 * room HVAC train's machines running, the lighting by the time of day); these loads draw no power of the bus, but they
 * heat the transformer and count toward the trip units' long delay (an overload trips a feeder after a while above its
 * 225 A pickup: the hotter, the sooner);
 * <li>the transformer's winding temperature (lagging behind its load), its fans (AUTO: on over 110 °C, off under 90 °C;
 * MANUAL: running), the high temperature alarm.
 * </ul>
 */
public final class LoadCentre {
	/** The breakers (Switchgear.BREAKERS: the main, then the ten feeders). */
	public static final int N = Switchgear.BREAKERS;
	public static final int CONNECT = 0, TEST = 1, DISCONNECT = 2;
	public static final String[] POSITIONS = {"CONNECT", "TEST", "DISCONNECT"};
	/** A closing spring's charge after it closes (ticks), a control switch's handle held turned (ticks). */
	public static final int SPRING_TICKS = 80, HANDLE_TICKS = 16;
	/** Amps a JE/t of real power reads; a feeder's long delay pickup; the transformer's full load (1500 kVA at 480 V). */
	public static final double AMPS_PER_JE = 1800.0 / Switchgear.INCOMING_RATE, PICKUP = 225, FULL_LOAD = 1800;
	/** The long delay's trip (pickup-seconds: above its pickup the unit counts (I / pickup)^2 - 1 a second). */
	public static final double LT_TRIP = 30;
	public static final double FANS_ON = 110, FANS_OFF = 90, ALARM = 150, TRIP_TEMP = 180, AMBIENT = 32;
	/** The tags of the lineups (the anchor's variant): LC 1A, 1B (Class 1E, Trains A and B), 1C..1E. */
	public static final String[] TAGS = {"1A", "1B", "1C", "1D", "1E"};

	/** One lineup's state (on its block entity). */
	public static final class State {
		public final int[] rack = new int[N];
		public final int[] dir = new int[N];
		public final int[] ops = new int[N];
		public final int[] amps = new int[N];
		public final long[] closedAt = new long[N];
		public final long[] handleAt = new long[N];
		public final int[] handle = new int[N];
		final double[] lt = new double[N];
		final double[] plant = new double[N];
		public int tripped, pickup;
		public double temp = AMBIENT + 8;
		public boolean fans, fanManual, alarm;
		public int volts;
		/**
		 * Its 125 VDC control power cut (older rooms round 3: its train's DC switchboard's feeder 05 open or its bus dead,
		 * DcRoom): its breakers can't be closed or tripped from their switches and its lamps go dark. LC 1A and 1B only.
		 */
		public boolean dcOff;

		/** As built: every breaker racked in but the two spares (racked out to DISCONNECT, as a real lineup's spares). */
		public State() {
			for (int i = 0; i < N; i++) {
				ops[i] = 1200 + (i * 731) % 900;
				dir[i] = 1;
			}
			rack[9] = rack[10] = DISCONNECT;
			dir[9] = dir[10] = -1;
		}

		public boolean rackedIn(int b) {
			return rack[b] == CONNECT;
		}

		public boolean controlPower(int b) {
			return !dcOff && rack[b] != DISCONNECT;
		}

		public boolean charged(int b, long now) {
			return now - closedAt[b] >= SPRING_TICKS;
		}

		public boolean target(int b) {
			return (tripped & 1 << b) != 0;
		}

		/** The control switch's handle at this time: -1 TRIP, +1 CLOSE, 0 NORMAL. */
		public int handle(int b, long now) {
			return now - handleAt[b] < HANDLE_TICKS ? handle[b] : 0;
		}

		/** The three windings' temperatures (the middle phase the hottest). */
		public double phase(int k) {
			return temp - (k == 1 ? 0 : k == 0 ? 2.5 : 1.5);
		}

		int hash() {
			return java.util.Objects.hash(java.util.Arrays.hashCode(rack), java.util.Arrays.hashCode(ops), java.util.Arrays.hashCode(amps), tripped, pickup,
					(int) temp, fans, fanManual, alarm, volts, dcOff, java.util.Arrays.hashCode(handle), java.util.Arrays.hashCode(handleAt),
					java.util.Arrays.hashCode(closedAt));
		}

		void save(ValueOutput out) {
			out.putIntArray("lc_rack", rack);
			out.putIntArray("lc_dir", dir);
			out.putIntArray("lc_ops", ops);
			out.putIntArray("lc_amps", amps);
			out.putIntArray("lc_handle", handle);
			out.store("lc_closed_at", Codec.LONG.listOf(), longs(closedAt));
			out.store("lc_handle_at", Codec.LONG.listOf(), longs(handleAt));
			out.store("lc_lt", Codec.DOUBLE.listOf(), doubles(lt));
			out.putInt("lc_tripped", tripped);
			out.putInt("lc_pickup", pickup);
			out.putDouble("lc_temp", temp);
			out.putBoolean("lc_fans", fans);
			out.putBoolean("lc_fan_manual", fanManual);
			out.putBoolean("lc_alarm", alarm);
			out.putInt("lc_volts", volts);
			out.putBoolean("lc_dc_off", dcOff);
		}

		void load(ValueInput in) {
			in.getIntArray("lc_rack").ifPresent(a -> copy(a, rack));
			in.getIntArray("lc_dir").ifPresent(a -> copy(a, dir));
			in.getIntArray("lc_ops").ifPresent(a -> copy(a, ops));
			in.getIntArray("lc_amps").ifPresent(a -> copy(a, amps));
			in.getIntArray("lc_handle").ifPresent(a -> copy(a, handle));
			in.read("lc_closed_at", Codec.LONG.listOf()).ifPresent(l -> {
				for (int i = 0; i < N && i < l.size(); i++) closedAt[i] = l.get(i);
			});
			in.read("lc_handle_at", Codec.LONG.listOf()).ifPresent(l -> {
				for (int i = 0; i < N && i < l.size(); i++) handleAt[i] = l.get(i);
			});
			in.read("lc_lt", Codec.DOUBLE.listOf()).ifPresent(l -> {
				for (int i = 0; i < N && i < l.size(); i++) lt[i] = l.get(i);
			});
			tripped = in.getIntOr("lc_tripped", 0);
			pickup = in.getIntOr("lc_pickup", 0);
			temp = in.getDoubleOr("lc_temp", AMBIENT + 8);
			fans = in.getBooleanOr("lc_fans", false);
			fanManual = in.getBooleanOr("lc_fan_manual", false);
			alarm = in.getBooleanOr("lc_alarm", false);
			volts = in.getIntOr("lc_volts", 0);
			dcOff = in.getBooleanOr("lc_dc_off", false);
		}

		private static void copy(int[] from, int[] to) {
			System.arraycopy(from, 0, to, 0, Math.min(from.length, to.length));
		}

		private static List<Long> longs(long[] a) {
			List<Long> out = new ArrayList<>();
			for (long v : a) out.add(v);
			return out;
		}

		private static List<Double> doubles(double[] a) {
			List<Double> out = new ArrayList<>();
			for (double v : a) out.add(v);
			return out;
		}
	}

	private LoadCentre() {
	}

	/** The lineup's tag by its anchor's variant (LC 1A, 1B, ...). */
	public static String tag(EquipmentBlockEntity e) {
		int v = e.getBlockState().hasProperty(EquipmentAnchorBlock.VARIANT) ? e.getBlockState().getValue(EquipmentAnchorBlock.VARIANT) : 0;
		return TAGS[Math.max(0, Math.min(TAGS.length - 1, v))];
	}

	/** A breaker's tag (52-1A-03) and its load (tools/load_centre.py feeders). */
	public static String breakerTag(String tag, int b) {
		return String.format("52-%s-%02d", tag, b + 1);
	}

	public static String load(String tag, int b) {
		return switch (b) {
			case 0 -> "MAIN BREAKER";
			case 1, 2, 3, 4 -> "MCC " + tag + b;
			case 5 -> "INVERTER " + tag;
			case 6 -> "DC MCC " + tag;
			case 7 -> "CR HVAC " + tag;
			case 8 -> "LIGHTING " + tag;
			default -> "SPARE";
		};
	}

	// ------------------------------------------------------------------ ticking (server)

	/** After Switchgear.tick (its flows counted): the amps, the trip units, the springs, the transformer, the volts. */
	public static void tick(ServerLevel level, EquipmentBlockEntity e) {
		State s = e.lc;
		if (s == null || e.bus == null) {
			return;
		}
		long now = level.getGameTime();
		if (now % 20 == (e.getBlockPos().hashCode() & 15)) {
			plantLoads(level, e, s);
		}
		boolean live = e.bus.getAmount() > 0;
		double total = 0;
		int pickup = 0;
		for (int b = 1; b < N; b++) {
			double a = 0;
			if (live && Switchgear.carrying(e, b)) {
				a = e.flows[b] * AMPS_PER_JE + s.plant[b];
			}
			s.amps[b] = (int) Math.round(a);
			total += a;
			// The long delay: above its pickup the unit counts toward a trip, below it forgets.
			double x = a / PICKUP;
			if (x > 1) {
				pickup |= 1 << b;
				s.lt[b] += (x * x - 1) / 20.0;
				if (s.lt[b] >= LT_TRIP) {
					trip(level, e, s, b, now);
				}
			} else {
				s.lt[b] = Math.max(0, s.lt[b] - 0.05);
			}
		}
		s.pickup = pickup;
		double main = live && Switchgear.carrying(e, 0) ? Math.max(total, e.flows[0] * AMPS_PER_JE) : 0;
		s.amps[0] = (int) Math.round(main);
		// The transformer: its windings head for their temperature at this load (fan cooled: a third more load for the
		// same rise), a minute's lag; its fans by their switch.
		double load = main / FULL_LOAD;
		double target = AMBIENT + 150 * Math.pow(load, 1.6) / (s.fans ? 1.33 : 1.0);
		s.temp += (target - s.temp) / 1200.0;
		s.fans = s.fanManual || (s.fans ? s.temp > FANS_OFF : s.temp > FANS_ON);
		s.alarm = s.temp > ALARM;
		s.volts = live && Switchgear.carrying(e, 0) ? (int) Math.round(480 - 9 * load) : 0;
	}

	private static void trip(ServerLevel level, EquipmentBlockEntity e, State s, int b, long now) {
		e.breakers &= ~(1 << b);
		s.tripped |= 1 << b;
		s.lt[b] = 0;
		level.playSound(null, at(e, b), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.6F);
		e.changed();
	}

	/**
	 * What each feeder feeds, worked out from the plant (once a second): a motor control centre's pumps, fans and heaters
	 * with the reactor's power; the vital inverter's steady load; the DC MCC's chargers' standing load (their charging
	 * is real power on top); the control room HVAC train's machines running (by their train, for LC 1A and 1B); the
	 * lighting, more at night. A little wander, as a real load has. Spares and racked-out or open breakers carry none.
	 */
	public static void plantLoads(ServerLevel level, EquipmentBlockEntity e, State s) {
		double power = 0, hvac = 0;
		int variant = e.getBlockState().hasProperty(EquipmentAnchorBlock.VARIANT) ? e.getBlockState().getValue(EquipmentAnchorBlock.VARIANT) : 0;
		BlockPos here = e.getBlockPos();
		for (Map.Entry<BlockPos, EquipmentRegistry.Entry> a : EquipmentRegistry.of(level).anchors().entrySet()) {
			EquipmentKind kind = a.getValue().kind();
			if (a.getKey().distSqr(here) > 256 * 256 || !level.isLoaded(a.getKey())) {
				continue;
			}
			if (kind.reactor() && level.getBlockEntity(a.getKey()) instanceof EquipmentBlockEntity r) {
				power = Math.max(power, r.core.fission);
			} else if (kind.hvac() && variant < 2 && a.getKey().distSqr(here) < 64 * 64 && level.getBlockEntity(a.getKey()) instanceof EquipmentBlockEntity m
					&& m.running) {
				int train = m.getBlockState().hasProperty(EquipmentAnchorBlock.VARIANT) ? m.getBlockState().getValue(EquipmentAnchorBlock.VARIANT) : 0;
				if (train == variant) {
					hvac += switch (kind) {
						case CR_FILTRATION_UNIT -> 22;
						case CR_AIR_HANDLER -> 31;
						case CR_CHILLER -> 52;
						default -> 9;
					};
				}
			}
		}
		power = Math.max(0, Math.min(1.1, power));
		// Its control power (older rooms round 3): LC 1A and 1B from their train's DC switchboard's feeder 05.
		s.dcOff = variant < 2 && !DcRoom.feederLive(level, here, variant, DcRoom.LC_CTL);
		long day = level.getOverworldClockTime() % 24000;
		boolean night = day > 12500 && day < 23500;
		double t = level.getGameTime() / 20.0;
		double[] base = {0, 36 + 72 * power, 48 + 54 * power, 52 + 63 * power, 40 + 49 * power, 38, 14, variant < 2 ? hvac : 58, night ? 62 : 46, 0, 0};
		for (int b = 1; b < N; b++) {
			double w = base[b] > 0 ? 1 + 0.025 * Math.sin(t * 0.37 + b * 1.9) + 0.012 * Math.sin(t * 1.3 + b) : 0;
			s.plant[b] = base[b] * w;
		}
	}

	// ------------------------------------------------------------------ the controls

	/** A working control's place on the lineup's face (tools/load_centre.py live "controls"). */
	public record Control(String id, double x0, double y0, double x1, double y1) {
		boolean contains(double x, double y) {
			return x >= x0 && x <= x1 && y >= y0 && y <= y1;
		}
	}

	private static List<Control> controls;

	/** Where the controls are, from control_boards.json.gz (the switchgear lineup's section). */
	public static List<Control> controls() {
		if (controls == null) {
			List<Control> list = new ArrayList<>();
			try (InputStream in = LoadCentre.class.getResourceAsStream("/assets/jugcraft/control_boards.json.gz")) {
				if (in != null) {
					JsonObject all = JsonParser.parseReader(new InputStreamReader(new GZIPInputStream(in), StandardCharsets.UTF_8)).getAsJsonObject();
					JsonObject live = all.getAsJsonObject(EquipmentKind.SWITCHGEAR_LINEUP.id).getAsJsonObject("live");
					for (JsonElement el : live.getAsJsonArray("controls")) {
						JsonObject c = el.getAsJsonObject();
						list.add(new Control(c.get("id").getAsString(), c.get("x0").getAsDouble(), c.get("y0").getAsDouble(), c.get("x1").getAsDouble(),
								c.get("y1").getAsDouble()));
					}
				}
			} catch (Exception ex) {
				Jugcraft.LOGGER.warn("Load centre controls could not be read", ex);
			}
			controls = List.copyOf(list);
		}
		return controls;
	}

	/** The control at a point of the lineup's front (its frame: x right, y up, z away from you), or null. */
	public static @Nullable String controlAt(double x, double y, double z) {
		if (z > 0.4) {
			return null;
		}
		for (Control c : controls()) {
			if (c.contains(x, y)) {
				return c.id();
			}
		}
		return null;
	}

	private static BlockPos at(EquipmentBlockEntity e, int b) {
		return e.getBlockPos().offset(EquipmentKind.toWorld(new BlockPos(b == 0 ? 2 : 3 + (b - 1) / 2, 1, 0), e.facing()));
	}

	/**
	 * Working the lineup: a breaker's control switch trips it (closed) or closes it (open: if it has control power, its
	 * spring is charged and its trip unit's target is reset); its racking port racks it out a position (CONNECT, TEST,
	 * DISCONNECT) and, from DISCONNECT, back in (only while it is open: the interlock); its trip unit's RESET clears the
	 * target; the transformer's fan switch turns between AUTO and MANUAL; anywhere else tells the bus's state.
	 */
	public static void use(Level level, EquipmentBlockEntity e, Player player, @Nullable Vec3 hit) {
		if (level.isClientSide() || e.bus == null || e.lc == null) {
			return;
		}
		State s = e.lc;
		String id = null;
		if (hit != null) {
			Vec3 p = Switchgear.local(e, hit);
			id = controlAt(p.x, p.y, p.z);
		}
		if (id == null) {
			Switchgear.status(e, player);
			return;
		}
		if (!e.mayUse(player)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.breaker_not_owner"));
			return;
		}
		long now = level.getGameTime();
		String tag = tag(e);
		if (id.equals("fans")) {
			s.fanManual = !s.fanManual;
			s.fans = s.fanManual || s.temp > FANS_ON;
			level.playSound(null, e.getBlockPos(), SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.6F, 1.4F);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.lc_fans", "LC " + tag,
					Component.translatable(s.fanManual ? "message.jugcraft.nuclear.lc_fans_manual" : "message.jugcraft.nuclear.lc_fans_auto")));
			e.changed();
			return;
		}
		int b = Integer.parseInt(id.replaceAll("[a-z]", ""));
		String name = breakerTag(tag, b) + " " + load(tag, b);
		if (id.startsWith("cs")) {
			boolean closed = Switchgear.closed(e, b);
			if (!s.controlPower(b)) {
				level.playSound(null, at(e, b), SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.5F, 0.8F);
				player.sendOverlayMessage(s.dcOff ? Component.translatable("message.jugcraft.nuclear.lc_no_dc", name, "72-" + tag + "-05")
						: Component.translatable("message.jugcraft.nuclear.lc_no_power", name));
				return;
			}
			s.handleAt[b] = now;
			if (closed) {
				s.handle[b] = -1;
				e.breakers &= ~(1 << b);
				level.playSound(null, at(e, b), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.9F, 0.7F);
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.lc_tripped", name));
			} else {
				s.handle[b] = 1;
				if (s.target(b)) {
					player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.lc_target", name));
				} else if (!s.charged(b, now)) {
					player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.lc_spring", name));
				} else {
					e.breakers |= 1 << b;
					s.closedAt[b] = now;
					s.ops[b] = (s.ops[b] + 1) % 100000;
					level.playSound(null, at(e, b), SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.55F);
					player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.lc_closed", name,
							Component.translatable(s.rackedIn(b) ? "message.jugcraft.nuclear.lc_carrying" : "message.jugcraft.nuclear.lc_test_only")));
				}
			}
		} else if (id.startsWith("rack")) {
			if (Switchgear.closed(e, b)) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.lc_interlock", name));
				return;
			}
			if (s.rack[b] == DISCONNECT) {
				s.dir[b] = -1;
			} else if (s.rack[b] == CONNECT) {
				s.dir[b] = 1;
			}
			s.rack[b] = Math.max(CONNECT, Math.min(DISCONNECT, s.rack[b] + s.dir[b]));
			level.playSound(null, at(e, b), SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.8F, 0.7F);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.lc_racked", name, POSITIONS[s.rack[b]]));
		} else if (id.startsWith("reset")) {
			if (s.target(b)) {
				s.tripped &= ~(1 << b);
				s.lt[b] = 0;
				level.playSound(null, at(e, b), SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.5F, 1.6F);
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.lc_reset", name));
			} else {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.lc_reset_none", name));
			}
		}
		e.changed();
	}
}
