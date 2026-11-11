package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A pump house's controller (towers and pump houses build, 19 October 2026; claude/jugcraft-pump-houses.md): the anchor
 * of its compiled site (WaterWorksBlock), keeping what its panels and switches do.
 *
 * <p>The lake intake (lake_intake): six vertical pumps, in the site's order MUP-A, SWP-1A, SWP-1B, SWP-2A, SWP-2B and
 * MUP-B (normally MUP-A and the two A pumps run: make-up 15,000 gpm, service water 12,000 gpm a train), each started
 * and stopped from its pistol grip on LCP-1, its discharge valve (MOV) opened and shut from the buttons on its operator;
 * a pump running against its shut valve trips on low flow after a minute. The four travelling screens turn (their
 * panels creep up through the water, the sprays wash them, the sluice carries the trash away) and stop from their
 * local stations; a screen stopped in a bay that is pumping clogs (DP HI). The strainers backwash now and then. The
 * annunciator's windows come in flashing, go steady when acknowledged and out when cleared (TEST lights them all,
 * RESET clears the trips). The towers' make-up and the diesels' and component cooling's service water come from here
 * (WaterWorks).
 *
 * <p>The circulating water pump house (cw_pump_house): four pumps (all normally run, 120,000 gpm each), each
 * started and stopped on LCP-CW, its butterfly valve opening twenty seconds after its pump starts; the condensers whose
 * conduit trench passes the controller count its pumps delivering. The forebay is the towers' basins: with them low
 * the pumps lose suction and trip, the 86 lockout holding them off until RESET.
 *
 * <p>Both: the hall's lights from dusk, the roll-up door (its station's button), the personnel doors.
 */
public class WaterWorksBlockEntity extends BlockEntity implements SiteAnchor {
	public static final String[] INTAKE_PUMPS = {"MUP-A", "SWP-1A", "SWP-1B", "SWP-2A", "SWP-2B", "MUP-B"};
	public static final String[] INTAKE_MOVS = {"MOV-MU-A", "MOV-SW-1A", "MOV-SW-1B", "MOV-SW-2A", "MOV-SW-2B", "MOV-MU-B"};
	public static final String[] CW_PUMPS = {"CWP-1A", "CWP-1B", "CWP-1C", "CWP-1D"};
	/** The pumps running when a pump house is built: MUP-A, SWP-1A, SWP-2A; all four circ pumps. */
	public static final int INTAKE_RUN = 0b001011, CIRC_RUN = 0b1111;
	/** Each bay's pumps (intake bays 1 to 4: MU-A, SW-A, SW-B, MU-B). */
	static final int[] BAY_PUMPS = {0b000001, 0b000110, 0b011000, 0b100000};
	/** Ticks a pump may run against its shut discharge valve before its low flow trip. */
	public static final int LOW_FLOW_TRIP = 1200;
	/** Ticks from a circ pump's start to its valve standing open. */
	public static final int VALVE_OPENS = 400;
	/** Ticks the roll-up door takes to go up or down. */
	public static final int ROLL_TRAVEL = 200;
	/** The towers' basin level the forebay's LO alarm comes in at. */
	public static final float FOREBAY_LO = 0.6F;
	static final String[] INTAKE_DOORS = {"hall_s", "hall_n", "er1", "er2"};
	static final String[] CIRC_DOORS = {"cw_door", "er1", "er2"};

	private int running, tripped, movShut, twsStopped, doors, annAcked, annSeen;
	/** Diesels make real power: the pumps stopped by their bus going dead (bits): they start again when it comes back. */
	private int shed;
	private boolean rollOpen;
	private double rollFrom;
	private long rollAt = -100000, annTest;
	private final long[] startedAt = new long[6];
	private final long[] shutSince = new long[6];
	/** The circ pump house's forebay: its towers' basin level (the highest; 1 if none found). Synced. */
	private float forebay = 1F;
	private boolean changedState;

	public WaterWorksBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftNuclear.WATER_WORKS_ENTITY, pos, state);
		running = state.getValue(WaterWorksBlock.WORKS) == WaterWorksBlock.Works.INTAKE ? INTAKE_RUN : CIRC_RUN;
		java.util.Arrays.fill(startedAt, -100000L);
	}

	public WaterWorksBlock.Works works() {
		return getBlockState().getValue(WaterWorksBlock.WORKS);
	}

	public boolean intake() {
		return works() == WaterWorksBlock.Works.INTAKE;
	}

	// ------------------------------------------------------------------ what it does (WaterWorks reads these)

	public boolean running(int p) {
		return (running & 1 << p) != 0;
	}

	public boolean tripped(int p) {
		return (tripped & 1 << p) != 0;
	}

	/** An intake pump's discharge valve open. */
	public boolean movOpen(int p) {
		return (movShut & 1 << p) == 0;
	}

	/** A pump putting water into its line: running with its discharge open. */
	public boolean delivering(int p) {
		if (!running(p)) return false;
		if (intake()) return movOpen(p);
		return level != null && level.getGameTime() - startedAt[p] >= VALVE_OPENS;
	}

	/**
	 * Diesels make real power (Justin, 27 October 2026): what has power at the pumps' buses, from the plant's vessel each
	 * second (PlantPower): the plant's own buses (the make-up and circulating water pumps), safety bus 1A (SWP-1A and 1B)
	 * and 1B (SWP-2A and 2B), each with its load sequencer past the service water step when it's on its diesel. A pump
	 * whose bus goes dead stops; it starts again by itself when its bus comes back (a safety bus's by its sequencer).
	 */
	public void power(ServerLevel level, boolean plant, boolean trainA, boolean trainB) {
		long now = level.getGameTime();
		int live;
		if (intake()) {
			live = (plant ? 1 | 1 << 5 : 0) | (trainA ? 1 << 1 | 1 << 2 : 0) | (trainB ? 1 << 3 | 1 << 4 : 0);
		} else {
			live = plant ? 15 : 0;
		}
		int dead = running & ~live;
		if (dead != 0) {
			running &= ~dead;
			shed |= dead;
			changedState = true;
		}
		int back = shed & live;
		if (back != 0) {
			running |= back;
			shed &= ~back;
			for (int p = 0; p < startedAt.length; p++) {
				if ((back & 1 << p) != 0) {
					startedAt[p] = now;
					shutSince[p] = 0;
				}
			}
			changedState = true;
		}
	}

	/** The pumps stopped by their bus going dead (bits). */
	public int shed() {
		return shed;
	}

	/** Make-up flowing to the towers: a make-up pump delivering. */
	public boolean makeup() {
		return intake() && (delivering(0) || delivering(5));
	}

	/** Service water train t (0: train 1 / A, 1: train 2 / B) flowing: one of its pumps delivering. */
	public boolean serviceWater(int t) {
		return intake() && (t == 0 ? delivering(1) || delivering(2) : delivering(3) || delivering(4));
	}

	/** The circ pumps delivering (each moves water for half a standard unit's heat, as an old circulating water pump). */
	public int pumpsDelivering() {
		if (intake()) return 0;
		int n = 0;
		for (int p = 0; p < 4; p++) {
			if (delivering(p)) n++;
		}
		return n;
	}

	public int pumpsRunning() {
		return Integer.bitCount(running);
	}

	public float forebay() {
		return forebay;
	}

	/** A screen turning. */
	public boolean turning(int i) {
		return (twsStopped & 1 << i) == 0;
	}

	public boolean doorOpen(String name) {
		String[] names = intake() ? INTAKE_DOORS : CIRC_DOORS;
		for (int i = 0; i < names.length; i++) {
			if (names[i].equals(name)) return (doors & 1 << i) != 0;
		}
		return false;
	}

	/** The roll-up door's travel now (0 shut, 1 open). */
	public double rollPos() {
		double t = level == null ? 1e9 : (level.getGameTime() - rollAt) / (double) ROLL_TRAVEL;
		return Math.max(0, Math.min(1, rollFrom + (rollOpen ? t : -t)));
	}

	/** Whether a strainer (0: train 1's, 1: train 2's) is backwashing: half a minute each ten, while its train flows. */
	public boolean backwashing(int t) {
		if (level == null || !serviceWater(t)) return false;
		return Math.floorMod(level.getGameTime() / 20 + t * 300, 600) < 30;
	}

	/** The annunciator's windows in alarm now (bit k: window k). */
	public int alarms() {
		int a = 0;
		if (intake()) {
			for (int i = 0; i < 4; i++) {
				if (!turning(i) && (running & BAY_PUMPS[i]) != 0) a |= 1 << 4 + i;                   // TWS n DP HI
			}
			int[] tripWindow = {12, 14, 15, 16, 17, 13};                                           // MUP-A, SWP-1A ... MUP-B TRIP
			for (int p = 0; p < 6; p++) {
				if (tripped(p)) a |= 1 << tripWindow[p];
			}
			if (!serviceWater(0) || !serviceWater(1)) a |= 1 << 20;                                  // SW HDR PRESS LO
			if (!makeup()) a |= 1 << 21;                                                           // MU FLOW LO
		} else {
			for (int p = 0; p < 4; p++) {
				if (tripped(p)) a |= 1 << p;                                                       // CWP-1x TRIP
			}
			if (forebay < FOREBAY_LO) a |= 1 << 14;                                                  // FOREBAY LVL LO
			if (forebay < CoolingTowerBlockEntity.LOW_LOW) a |= 1 << 15;                             // FOREBAY LO LO
			if (pumpsDelivering() == 0) a |= 1 << 16;                                                // DISCH PRESS LO
			if (level instanceof ServerLevel server && !WaterWorks.serviceWaterAny(server, getBlockPos())) a |= 1 << 17;   // BRG CLG WTR LO
			if (tripped != 0) a |= 1 << 20;                                                        // 86 LOCKOUT
		}
		return a;
	}

	/** The alarms as last worked out (each second, on the server; synced). */
	private int alarmsSeen() {
		return annSeen;
	}

	/** A window lit: in alarm (flashing until acknowledged), or the lamp test. */
	private boolean annLit(int k) {
		if (level == null) return false;
		long now = level.getGameTime();
		if (now < annTest) return true;
		int a = alarmsSeen();
		if ((a & 1 << k) == 0) return false;
		return (annAcked & 1 << k) != 0 || Math.floorMod(now / 10, 2) == 0;
	}

	/** Dusk to dawn, by the photocell. */
	private boolean night() {
		if (level == null) return false;
		long t = Math.floorMod(level.getOverworldClockTime(), 24000L);
		return t > 12600 && t < 23400;
	}

	// ------------------------------------------------------------------ ticking

	public void serverTick(ServerLevel level) {
		if (Math.floorMod(level.getGameTime() + getBlockPos().hashCode(), 20) == 0) {
			serverSecond(level);
		}
	}

	/** Its second's work (each second, spread by position; the game tests call it straight). */
	public void serverSecond(ServerLevel level) {
		long now = level.getGameTime();
		if (intake()) {
			// A pump running against its shut discharge valve heats the water it churns: its low flow trip.
			for (int p = 0; p < 6; p++) {
				if (running(p) && !movOpen(p)) {
					if (shutSince[p] == 0) shutSince[p] = now;
					if (now - shutSince[p] >= LOW_FLOW_TRIP) {
						trip(level, p, "low_flow");
					}
				} else {
					shutSince[p] = 0;
				}
			}
		} else {
			float was = forebay;
			forebay = WaterWorks.forebay(level, getBlockPos());
			if (Math.abs(was - forebay) > 0.004F) changedState = true;
			if (forebay < CoolingTowerBlockEntity.LOW_LOW) {
				for (int p = 0; p < 4; p++) {
					if (running(p)) trip(level, p, "forebay");
				}
			}
		}
		int a = alarms();
		// Cleared windows forget their acknowledgement; a new one sounds the horn.
		int acked = annAcked & a;
		if ((a & ~annSeen & ~annAcked) != 0) {
			horn(level);
		}
		if (acked != annAcked || a != annSeen) {
			annAcked = acked;
			annSeen = a;
			changedState = true;
		}
		if (changedState) {
			changedState = false;
			sync();
		}
	}

	private void trip(ServerLevel level, int p, String why) {
		running &= ~(1 << p);
		tripped |= 1 << p;
		shutSince[p] = 0;
		changedState = true;
		level.playSound(null, getBlockPos(), SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.6F);
	}

	private void horn(ServerLevel level) {
		BlockPos at = zoneBlock("ann_ack");
		level.playSound(null, at != null ? at : getBlockPos(), SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 1.2F, 0.7F);
	}

	/** The world block a control zone of the site stands in. */
	private @Nullable BlockPos zoneBlock(String name) {
		SiteData d = data();
		if (d == null || level == null) return null;
		for (SiteData.Zone z : d.zones) {
			if (z.name().equals(name)) {
				net.minecraft.world.phys.Vec3 c = z.box().getCenter();
				SiteRegistry.Placement p = new SiteRegistry.Placement(getBlockPos(), siteFacing(), d, new net.minecraft.world.phys.AABB(getBlockPos()));
				return p.world((int) Math.floor(c.x), (int) Math.floor(c.y), (int) Math.floor(c.z));
			}
		}
		return null;
	}

	// ------------------------------------------------------------------ the controls

	@Override
	public void onZone(ServerLevel level, Player player, String zone) {
		long now = level.getGameTime();
		int colon = zone.indexOf(':');
		String head = colon < 0 ? zone : zone.substring(0, colon);
		String arg = colon < 0 ? "" : zone.substring(colon + 1);
		switch (head) {
			case "pump", "cwp" -> {
				int p = Integer.parseInt(arg);
				String name = intake() ? INTAKE_PUMPS[p] : CW_PUMPS[p];
				if (running(p)) {
					running &= ~(1 << p);
					player.sendOverlayMessage(WaterWorksBlock.line("ww_pump_stop", name));
				} else if (!intake() && tripped != 0) {
					player.sendOverlayMessage(WaterWorksBlock.line("ww_pump_locked", name));
					return;
				} else if (!intake() && forebay < CoolingTowerBlockEntity.LOW_LOW) {
					player.sendOverlayMessage(WaterWorksBlock.line("ww_pump_suction", name));
					return;
				} else {
					running |= 1 << p;
					tripped &= ~(1 << p);
					startedAt[p] = now;
					shutSince[p] = 0;
					player.sendOverlayMessage(WaterWorksBlock.line("ww_pump_start", name));
				}
				level.playSound(null, player.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.8F, 0.9F);
			}
			case "mov_open", "mov_close" -> {
				int p = Integer.parseInt(arg);
				boolean open = head.equals("mov_open");
				movShut = open ? movShut & ~(1 << p) : movShut | 1 << p;
				player.sendOverlayMessage(WaterWorksBlock.line(open ? "ww_mov_open" : "ww_mov_shut", INTAKE_MOVS[p]));
				level.playSound(null, player.blockPosition(), SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.6F, 1.0F);
			}
			case "tws_start", "tws_stop" -> {
				int i = Integer.parseInt(arg);
				boolean start = head.equals("tws_start");
				twsStopped = start ? twsStopped & ~(1 << i) : twsStopped | 1 << i;
				player.sendOverlayMessage(WaterWorksBlock.line(start ? "ww_tws_start" : "ww_tws_stop", "TWS-" + (i + 1)));
				level.playSound(null, player.blockPosition(), SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.6F, 1.0F);
			}
			case "door" -> {
				String[] names = intake() ? INTAKE_DOORS : CIRC_DOORS;
				for (int i = 0; i < names.length; i++) {
					if (names[i].equals(arg)) {
						doors ^= 1 << i;
						boolean open = (doors & 1 << i) != 0;
						level.playSound(null, player.blockPosition(), open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE,
								SoundSource.BLOCKS, 1, 1);
					}
				}
			}
			case "rollup" -> {
				rollFrom = rollPos();
				rollOpen = !rollOpen;
				rollAt = now;
				level.playSound(null, player.blockPosition(), SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.6F, 1.0F);
				level.playSound(null, player.blockPosition(), SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5F, 0.5F);
			}
			case "ann_ack" -> {
				annAcked = alarms();
				player.sendOverlayMessage(WaterWorksBlock.line("ww_ann_ack"));
			}
			case "ann_silence" -> player.sendOverlayMessage(WaterWorksBlock.line("ww_ann_silence"));
			case "ann_test" -> annTest = now + 60;
			case "ann_reset" -> {
				if (!intake() && forebay < CoolingTowerBlockEntity.LOW_LOW) {
					player.sendOverlayMessage(WaterWorksBlock.line("ww_reset_refused"));
					return;
				}
				tripped = 0;
				player.sendOverlayMessage(WaterWorksBlock.line("ww_ann_reset"));
			}
			default -> {
				return;
			}
		}
		sync();
	}

	/** What its controller says when clicked. */
	public Component status() {
		if (intake()) {
			return WaterWorksBlock.line("ww_intake_status", makeup() ? "15,000" : "0", serviceWater(0) ? "12,000" : "0",
					serviceWater(1) ? "12,000" : "0");
		}
		int n = pumpsDelivering();
		return WaterWorksBlock.line("ww_circ_status", n, String.format(java.util.Locale.ROOT, "%,d", n * 120000),
				String.format(java.util.Locale.ROOT, "%.2f", 7.0 * Math.min(1, forebay / 0.92F)));
	}

	// ------------------------------------------------------------------ the site

	private @Nullable SiteData data() {
		return SiteData.get(works().site);
	}

	@Override
	public @Nullable String siteKey() {
		return works().site;
	}

	@Override
	public Direction siteFacing() {
		return getBlockState().getValue(WaterWorksBlock.FACING);
	}

	private static final java.util.Map<String, SiteData.Group[][]> TABLES = new java.util.concurrent.ConcurrentHashMap<>();

	@Override
	public SiteData.Group[][] groups() {
		SiteData d = data();
		if (d == null) return new SiteData.Group[0][];
		return TABLES.computeIfAbsent(d.key, k -> d.groupTable());
	}

	/** Each variable of a site: what drives it (by its name, worked out once) and its True value's index. */
	private record Slot(int kind, int arg, String name, int yes, int no) {
	}

	private static final int PUMP = 0, MOV = 1, BW = 2, WASH = 3, TWS = 4, PHASE = 5, LIGHTS = 6, ROLL = 7, DOOR = 8, ANN = 9, VALVE = 10,
			OTHER = 11;
	private static final java.util.Map<String, Slot[]> SLOTS = new java.util.concurrent.ConcurrentHashMap<>();

	private static Slot[] slots(SiteData d) {
		return SLOTS.computeIfAbsent(d.key, k -> {
			Slot[] out = new Slot[d.variables.size()];
			for (int i = 0; i < out.length; i++) {
				SiteData.Variable v = d.variables.get(i);
				String n = v.name();
				int kind = OTHER, arg = 0;
				String name = "";
				if (n.startsWith("pump") || n.startsWith("cwp")) {
					kind = PUMP;
					arg = Integer.parseInt(n.substring(n.startsWith("pump") ? 4 : 3));
				} else if (n.startsWith("mov")) {
					kind = MOV;
					arg = Integer.parseInt(n.substring(3));
				} else if (n.startsWith("cwv")) {
					kind = VALVE;
					arg = Integer.parseInt(n.substring(3));
				} else if (n.startsWith("bw")) {
					kind = BW;
					arg = Integer.parseInt(n.substring(2));
				} else if (n.equals("wash")) {
					kind = WASH;
				} else if (n.startsWith("tws")) {
					kind = TWS;
					arg = Integer.parseInt(n.substring(3));
				} else if (n.startsWith("phase")) {
					kind = PHASE;
					arg = Integer.parseInt(n.substring(5));
				} else if (n.equals("lights")) {
					kind = LIGHTS;
				} else if (n.equals("roll")) {
					kind = ROLL;
				} else if (n.startsWith("door_")) {
					kind = DOOR;
					name = n.substring(5);
				} else if (n.startsWith("ann") && n.length() > 3 && Character.isDigit(n.charAt(3))) {
					kind = ANN;
					arg = Integer.parseInt(n.substring(3));
				}
				out[i] = new Slot(kind, arg, name, v.indexOf("True"), v.indexOf("False"));
			}
			return out;
		});
	}

	@Override
	public int value(int v) {
		SiteData d = data();
		if (d == null || level == null || v >= d.variables.size()) return 0;
		Slot s = slots(d)[v];
		return switch (s.kind) {
			case PUMP -> running(s.arg) ? s.yes : s.no;
			case MOV -> movOpen(s.arg) ? s.yes : s.no;
			case VALVE -> delivering(s.arg) ? s.yes : s.no;
			case BW -> backwashing(s.arg) ? s.yes : s.no;
			case WASH -> twsStopped != 0b1111 ? s.yes : s.no;
			case TWS -> turning(s.arg) ? s.yes : s.no;
			// The panels creep up 50 mm a step, a step each half second (6 m a minute), the screens a little out of step.
			case PHASE -> turning(s.arg) ? (int) Math.floorMod(level.getGameTime() / 10 + s.arg * 5L, d.variables.get(v).values().length) : 0;
			case LIGHTS -> night() ? s.yes : s.no;
			case ROLL -> d.variables.get(v).nearest(rollPos());
			case DOOR -> doorOpen(s.name) ? s.yes : s.no;
			case ANN -> annLit(s.arg) ? s.yes : s.no;
			default -> 0;
		};
	}

	// ------------------------------------------------------------------ saving and syncing

	private void sync() {
		setChanged();
		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
		saveAdditional(out);
		return out.buildResult();
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		running = in.getIntOr("running", intake() ? INTAKE_RUN : CIRC_RUN);
		tripped = in.getIntOr("tripped", 0);
		movShut = in.getIntOr("mov_shut", 0);
		twsStopped = in.getIntOr("tws_stopped", 0);
		doors = in.getIntOr("doors", 0);
		annAcked = in.getIntOr("ann_acked", 0);
		annSeen = in.getIntOr("ann_seen", 0);
		annTest = in.getLongOr("ann_test", 0L);
		rollOpen = in.getBooleanOr("roll_open", false);
		rollFrom = in.getDoubleOr("roll_from", 0);
		rollAt = in.getLongOr("roll_at", -100000L);
		forebay = in.getFloatOr("forebay", 1F);
		shed = in.getIntOr("shed", 0);
		for (int p = 0; p < startedAt.length; p++) {
			startedAt[p] = in.getLongOr("started" + p, -100000L);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		out.putInt("running", running);
		out.putInt("shed", shed);
		out.putInt("tripped", tripped);
		out.putInt("mov_shut", movShut);
		out.putInt("tws_stopped", twsStopped);
		out.putInt("doors", doors);
		out.putInt("ann_acked", annAcked);
		out.putInt("ann_seen", annSeen);
		out.putLong("ann_test", annTest);
		out.putBoolean("roll_open", rollOpen);
		out.putDouble("roll_from", rollFrom);
		out.putLong("roll_at", rollAt);
		out.putFloat("forebay", forebay);
		for (int p = 0; p < startedAt.length; p++) {
			out.putLong("started" + p, startedAt[p]);
		}
	}

	// ------------------------------------------------------------------ for the game tests

	/** Sets the pumps running (bits) and starts their clocks long ago (their valves open). */
	public void setRunning(int bits) {
		running = bits;
		tripped = 0;
		for (int p = 0; p < startedAt.length; p++) {
			startedAt[p] = -100000L;
		}
		sync();
	}

	public void setMovOpen(int p, boolean open) {
		movShut = open ? movShut & ~(1 << p) : movShut | 1 << p;
		sync();
	}
}
