package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The cable spreading room's pre-action sprinkler system (punch list #15; Justin: "Working sprinklers", "Alarm and put
 * it out"; tools/spreading_room.py). Its releasing panel (a live panel on the wall by the door) watches the room's
 * smoke and heat detectors: a fire under one (a fire block within {@link #SEE} blocks across and {@link #BELOW} down)
 * puts it in ALARM, its horns sounding, the radiation and ventilation board's FIRE CABLE SPREADING window lit; after
 * {@link #DELAY} seconds it releases: the valve station's pre-action valve trips (its system air to 0, water into the
 * pipes). From then the heads over the fire open (each within {@link #FUSE} blocks across of a fire and above it: a real
 * head's bulb bursts in the heat), stay open and spray, putting out every fire within {@link #SPRAY} blocks across
 * under them; FIRE PUMP RUNNING on the board while any flows. {@link #SETTLE} seconds after the last fire is out the
 * water is shut off and the panel waits for RESET, which brings the air back up and re-arms it. The pull station by the
 * door releases by hand; DRILL tests the lamps and horns without water; SILENCE quiets the horns; ACK acknowledges.
 *
 * <p>Its state is the releasing panel's ({@link LivePanelBlockEntity#csrPhase} and the rest, saved and sent to the
 * players for its lamps and display and the valve station's gauges: client/EdgTelemetry.java). What it watches and
 * wets are the detectors, heads and valve station within {@link #REACH} blocks of it (looked for again every few
 * seconds).
 *
 * <p>Part W3: the same system protects an automated warehouse (its valve station and releasing panel in the control
 * office). A panel with warehouse blocks round it watches the whole building: {@link #WIDE} blocks across and up floor
 * by floor until {@link #GAP} levels with nothing of the system; the racks' in-rack sprinkler lines are heads too
 * (they open over a fire beside or under them), and the roof's smoke vents open on the alarm and close on RESET. A
 * big system's detectors are looked round a quarter at a time.
 */
public final class SpreadingRoomFire {
	public static final String RELEASE = "csr_release_panel", VALVE = "csr_valve_station";
	public static final int NORMAL = 0, ALARM = 1, RELEASED = 2, FIRE_OUT = 3, DRILL = 4;
	/** Flags: the horns silenced, the alarm acknowledged, released by hand, water flowing; trouble: no valve, no detectors, no heads. */
	public static final int SILENCED = 1, ACKED = 2, MANUAL = 4, FLOWING = 8, NO_VALVE = 16, NO_DETECTORS = 32, NO_HEADS = 64;
	/** How far it looks for its pieces (blocks across; up and down {@link #HIGH}), and how often (ticks). */
	static final int REACH = 24, HIGH = 10, RESCAN = 100;
	/** A warehouse's system: how far across, how many empty levels end it going up, rescans of a big one (ticks). */
	static final int WIDE = 56, GAP = 12, RESCAN_BIG = 600, BIG = 200_000;
	/** A detector sees fire this far across and down; a head opens over a fire this far across, and its spray reaches as far. */
	static final int SEE = 4, BELOW = 7, FUSE = 4, SPRAY = 4;
	/** Seconds of alarm before the release, of quiet after the last fire before the water is shut off, of a drill. */
	public static final int DELAY = 10, SETTLE = 20, DRILL_TIME = 10;
	/** The system's air (psi) armed, and how fast it comes back after a reset (psi a second); the supply's water (psi). */
	public static final double AIR = 40, AIR_RATE = 2, WATER = 118, WATER_FLOWING = 96;
	public static final double LOW_AIR = 25;

	private SpreadingRoomFire() {
	}

	/** 0 the releasing panel, 1 the valve station, -1 neither. */
	public static int kind(BlockState state) {
		Block b = state.getBlock();
		return b == PlantInterior.BLOCKS.get(RELEASE) ? 0 : b == PlantInterior.BLOCKS.get(VALVE) ? 1 : -1;
	}

	static boolean is(BlockState state, String... names) {
		for (String n : names) {
			if (state.getBlock() == PlantInterior.BLOCKS.get(n)) {
				return true;
			}
		}
		return false;
	}

	/** Every ten ticks for a releasing panel (its origin block), at game time {@code now}. */
	public static void tick(LivePanelBlockEntity panel, ServerLevel level, long now) {
		if (panel.csrScanAt <= now) {
			panel.csrScanAt = now + (scan(panel, level) > BIG ? RESCAN_BIG : RESCAN);
		}
		List<BlockPos> fires = fires(panel, level);
		long t = now - panel.csrSince;
		int was = panel.csrPhase, flags = panel.csrFlags;
		double air = panel.csrAir;
		switch (panel.csrPhase) {
			case NORMAL -> {
				air = Math.min(AIR, air + AIR_RATE / 2);
				if (!fires.isEmpty()) {
					alarm(panel, level, now);
				}
			}
			case ALARM -> {
				if (t >= DELAY * 20L && (panel.csrFlags & NO_VALVE) == 0) {
					release(panel, level, now);
				}
			}
			case RELEASED -> {
				air = Math.max(0, air - 10);
				if (!fires.isEmpty()) {
					panel.csrQuiet = now;
				}
				spray(panel, level, fires);
				if (now - panel.csrQuiet >= SETTLE * 20L) {
					// The fire's out: the water is shut off (a fire watch would close the valve); RESET re-arms it.
					panel.csrPhase = FIRE_OUT;
					panel.csrSince = now;
					panel.csrHeads.clear();
					panel.csrFlags &= ~FLOWING;
				}
			}
			case FIRE_OUT -> {
				air = 0;
				if (!fires.isEmpty()) {
					// It flared up again: the valve is still tripped, the water back on.
					panel.csrPhase = RELEASED;
					panel.csrSince = now;
					panel.csrQuiet = now;
				}
			}
			case DRILL -> {
				if (t >= DRILL_TIME * 20L) {
					panel.csrPhase = NORMAL;
					panel.csrSince = now;
					panel.csrFlags &= ~(SILENCED | ACKED);
				}
			}
			default -> panel.csrPhase = NORMAL;
		}
		panel.csrAir = air;
		boolean horns = (panel.csrPhase == ALARM || panel.csrPhase == RELEASED || panel.csrPhase == DRILL) && (panel.csrFlags & SILENCED) == 0;
		if (horns && t % 40 < 10) {
			level.playSound(null, panel.getBlockPos(), JugcraftNuclear.CO2_HORN, SoundSource.BLOCKS, 1.4F, 1.15F);
		}
		if (was != panel.csrPhase || flags != panel.csrFlags || Math.abs(air - panel.csrSent) >= 1) {
			panel.csrSent = air;
			panel.changedAndSync(level);
		}
	}

	/** The detectors, heads and valve station round the panel (a warehouse's: its smoke vents too); trouble if any are missing. Returns the blocks looked at. */
	static int scan(LivePanelBlockEntity panel, ServerLevel level) {
		List<BlockPos> detectors = new ArrayList<>(), heads = new ArrayList<>(), vents = new ArrayList<>();
		BlockPos valve = null;
		BlockPos at = panel.getBlockPos();
		boolean warehouse = false;
		int looked = 0;
		for (BlockPos p : BlockPos.betweenClosed(at.offset(-REACH, -HIGH, -REACH), at.offset(REACH, HIGH, REACH))) {
			if (!level.isLoaded(p)) {
				continue;
			}
			looked++;
			BlockState s = level.getBlockState(p);
			if (s.isAir()) {
				continue;
			}
			if (s.getBlock() instanceof io.github.jimbozoomer.jugcraft.warehouse.WarehouseBlock) {
				warehouse = true;
				break;
			}
		}
		int reach = warehouse ? WIDE : REACH;
		// Level by level from under the panel up: a warehouse's system goes on up its floors until GAP levels have none of it.
		int empty = 0;
		for (int y = at.getY() - HIGH; y <= level.getMaxY(); y++) {
			if (!warehouse && y > at.getY() + HIGH || warehouse && y > at.getY() + HIGH && empty >= GAP) {
				break;
			}
			boolean any = false;
			for (BlockPos p : BlockPos.betweenClosed(at.getX() - reach, y, at.getZ() - reach, at.getX() + reach, y, at.getZ() + reach)) {
				if (!level.isLoaded(p)) {
					continue;
				}
				looked++;
				BlockState s = level.getBlockState(p);
				if (s.isAir()) {
					continue;
				}
				if (is(s, "smoke_detector", "heat_detector")) {
					detectors.add(p.immutable());
					any = true;
				} else if (is(s, "sprinkler_pipe_head", "sprinkler_pipe_end", "sprinkler_pipe", "sprinkler_main", "sprinkler_riser")) {
					if (is(s, "sprinkler_pipe_head", "sprinkler_pipe_end")) {
						heads.add(p.immutable());
					}
					any = true;
				} else if (warehouse && s.getBlock() instanceof io.github.jimbozoomer.jugcraft.warehouse.RackBlock
						&& s.getValue(io.github.jimbozoomer.jugcraft.warehouse.RackBlock.SPRINKLER)) {
					// An in-rack line's head over this place.
					heads.add(p.immutable());
					any = true;
				} else if (warehouse && s.getBlock() instanceof io.github.jimbozoomer.jugcraft.exterior.RoofOpeningBlock vent
						&& vent.kind() == io.github.jimbozoomer.jugcraft.exterior.RoofOpeningBlock.Kind.SMOKE_VENT) {
					vents.add(p.immutable());
				} else if (valve == null && kind(s) == 1) {
					valve = p.immutable();
				}
			}
			empty = any ? 0 : empty + 1;
		}
		panel.csrDetectors = detectors;
		panel.csrAllHeads = heads;
		panel.csrVents = vents;
		int flags = panel.csrFlags & ~(NO_VALVE | NO_DETECTORS | NO_HEADS);
		flags |= (valve == null ? NO_VALVE : 0) | (detectors.isEmpty() ? NO_DETECTORS : 0) | (heads.isEmpty() ? NO_HEADS : 0);
		panel.csrFlags = flags;
		panel.csrValve = valve;
		return looked;
	}

	/** The smoke vents open (on an alarm) or shut (on RESET). */
	static void vents(LivePanelBlockEntity panel, ServerLevel level, boolean open) {
		for (BlockPos v : panel.csrVents) {
			if (level.isLoaded(v)) {
				io.github.jimbozoomer.jugcraft.exterior.RoofOpeningBlock.setOpen(level, v, open);
			}
		}
	}

	/**
	 * The fires the detectors see (each detector looks across and down from where it is). A big system's detectors look
	 * a quarter at a time (each every 40 ticks) while all is quiet; once it is in alarm, all of them every time.
	 */
	static List<BlockPos> fires(LivePanelBlockEntity panel, ServerLevel level) {
		Set<BlockPos> out = new HashSet<>();
		List<BlockPos> all = panel.csrDetectors;
		boolean turns = all.size() > 24 && panel.csrPhase == NORMAL;
		int turn = panel.csrTurn = (panel.csrTurn + 1) & 3;
		for (int i = 0; i < all.size(); i++) {
			BlockPos d = all.get(i);
			if (turns && (i & 3) != turn || !level.isLoaded(d)) {
				continue;
			}
			for (BlockPos p : BlockPos.betweenClosed(d.offset(-SEE, -BELOW, -SEE), d.offset(SEE, 0, SEE))) {
				if (level.isLoaded(p) && level.getBlockState(p).is(BlockTags.FIRE)) {
					out.add(p.immutable());
				}
			}
		}
		return new ArrayList<>(out);
	}

	static void alarm(LivePanelBlockEntity panel, ServerLevel level, long now) {
		panel.csrPhase = ALARM;
		panel.csrSince = now;
		panel.csrFlags &= ~(SILENCED | ACKED | MANUAL);
		// A warehouse's smoke vents open to let the smoke and heat out over the fire.
		vents(panel, level, true);
	}

	/** The pre-action valve trips: water into the pipes (the heads open where it is hot). */
	static void release(LivePanelBlockEntity panel, ServerLevel level, long now) {
		panel.csrPhase = RELEASED;
		panel.csrSince = now;
		panel.csrQuiet = now;
		panel.csrReleasedAt = level.getOverworldClockTime();
		panel.csrHeads.clear();
		BlockPos v = panel.csrValve != null ? panel.csrValve : panel.getBlockPos();
		level.playSound(null, v, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1.2F, 0.6F);
		level.playSound(null, v, SoundEvents.WATER_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.2F);
	}

	/** The heads over the fires open; the open heads spray and put out what is under them. */
	static void spray(LivePanelBlockEntity panel, ServerLevel level, List<BlockPos> fires) {
		for (BlockPos f : fires) {
			for (BlockPos h : panel.csrAllHeads) {
				if (h.getY() >= f.getY() && Math.abs(h.getX() - f.getX()) <= FUSE && Math.abs(h.getZ() - f.getZ()) <= FUSE && !panel.csrHeads.contains(h)) {
					panel.csrHeads.add(h);
					level.playSound(null, h, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.6F, 1.8F);
				}
			}
		}
		if (panel.csrHeads.isEmpty()) {
			panel.csrFlags &= ~FLOWING;
			return;
		}
		panel.csrFlags |= FLOWING;
		for (BlockPos h : panel.csrHeads) {
			if (!level.isLoaded(h)) {
				continue;
			}
			double x = h.getX() + 0.5, y = h.getY() + 0.62, z = h.getZ() + 0.5;
			level.sendParticles(ParticleTypes.FALLING_WATER, x, y - 0.5, z, 40, 1.4, 0.6, 1.4, 0.0);
			level.sendParticles(ParticleTypes.SPLASH, x, y - 1.5, z, 30, 1.8, 1.2, 1.8, 0.1);
			for (BlockPos p : BlockPos.betweenClosed(h.offset(-SPRAY, -BELOW - 1, -SPRAY), h.offset(SPRAY, 0, SPRAY))) {
				if (level.isLoaded(p) && level.getBlockState(p).is(BlockTags.FIRE)) {
					level.removeBlock(p, false);
					level.playSound(null, p, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7F, 1.0F);
					level.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.02);
				}
			}
			AABB wet = new AABB(h.getX() - SPRAY, h.getY() - BELOW - 1, h.getZ() - SPRAY, h.getX() + SPRAY + 1, h.getY() + 1, h.getZ() + SPRAY + 1);
			for (Entity e : level.getEntities((Entity) null, wet, Entity::isOnFire)) {
				e.clearFire();
			}
		}
	}

	// ------------------------------------------------------------------------------------------------ the controls

	/** The releasing panel's keys (CsrReleasePanelBlock): 0 ACK, 1 SILENCE, 2 RESET, 3 DRILL. */
	public static void key(LivePanelBlockEntity panel, ServerLevel level, int key, @Nullable Player player) {
		long now = level.getGameTime();
		switch (key) {
			case 0 -> {
				if (panel.csrPhase == NORMAL) {
					tell(player, "csr_nothing");
				} else {
					panel.csrFlags |= ACKED;
					tell(player, "csr_acked");
				}
			}
			case 1 -> {
				if (panel.csrPhase == ALARM || panel.csrPhase == RELEASED || panel.csrPhase == DRILL) {
					panel.csrFlags |= SILENCED;
					tell(player, "csr_silenced");
				} else {
					tell(player, "csr_nothing");
				}
			}
			case 2 -> {
				if (!fires(panel, level).isEmpty()) {
					tell(player, "csr_still_fire");
				} else if (panel.csrPhase == RELEASED && (panel.csrFlags & FLOWING) != 0) {
					tell(player, "csr_still_flowing");
				} else {
					boolean was = panel.csrPhase != NORMAL;
					vents(panel, level, false);
					panel.csrPhase = NORMAL;
					panel.csrSince = now;
					panel.csrHeads.clear();
					panel.csrFlags &= ~(SILENCED | ACKED | MANUAL | FLOWING);
					tell(player, was ? "csr_reset" : "csr_normal");
				}
			}
			default -> {
				if (panel.csrPhase == NORMAL) {
					panel.csrPhase = DRILL;
					panel.csrSince = now;
					panel.csrFlags &= ~(SILENCED | ACKED);
					tell(player, "csr_drill");
				} else {
					tell(player, "csr_busy");
				}
			}
		}
		panel.changedAndSync(level);
	}

	/** The pull station: a release by hand (the nearest releasing panel in reach). */
	public static void pull(ServerLevel level, BlockPos station, @Nullable Player player) {
		LivePanelBlockEntity panel = null;
		double best = Double.MAX_VALUE;
		for (LivePanelBlockEntity p : ControlBoard.panels(level, station, PlantInterior.BLOCKS.get(RELEASE), REACH)) {
			double d = p.getBlockPos().distSqr(station);
			if (d < best) {
				best = d;
				panel = p;
			}
		}
		if (panel == null) {
			tell(player, "csr_no_panel");
			return;
		}
		long now = level.getGameTime();
		if (panel.csrPhase == RELEASED || panel.csrPhase == FIRE_OUT) {
			tell(player, "csr_already");
			return;
		}
		scan(panel, level);
		if ((panel.csrFlags & NO_VALVE) != 0) {
			alarm(panel, level, now);
			panel.csrFlags |= MANUAL;
			tell(player, "csr_no_valve");
		} else {
			alarm(panel, level, now);
			panel.csrFlags |= MANUAL;
			release(panel, level, now);
			tell(player, "csr_pulled");
		}
		panel.changedAndSync(level);
	}

	private static void tell(@Nullable Player player, String key) {
		if (player != null) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear." + key));
		}
	}

	/** The display's two lines (as the client shows them; also the panel's status for a click elsewhere on it). */
	public static String[] display(int phase, int flags, long since, double now, long releasedAt) {
		String zone = "ZONE 1";
		return switch (phase) {
			case ALARM -> new String[] {(flags & MANUAL) != 0 ? "MANUAL RELEASE" : "FIRE ALARM " + zone,
					(flags & NO_VALVE) != 0 ? "NO VALVE STATION" : String.format(java.util.Locale.ROOT, "RELEASE IN %d S",
							Math.max(0, (int) Math.ceil(DELAY - (now - since) / 20.0)))};
			case RELEASED -> new String[] {(flags & MANUAL) != 0 ? "MANUAL RELEASE" : "FIRE ALARM " + zone,
					(flags & FLOWING) != 0 ? "WATER FLOWING" : "RELEASED  " + clock(releasedAt)};
			case FIRE_OUT -> new String[] {"RELEASED  " + clock(releasedAt), "FIRE OUT - RESET"};
			case DRILL -> new String[] {"DRILL IN PROGRESS", "NO DISCHARGE"};
			default -> (flags & NO_VALVE) != 0 ? new String[] {"TROUBLE", "NO VALVE STATION"}
					: (flags & NO_DETECTORS) != 0 ? new String[] {"TROUBLE", "NO DETECTORS"}
					: (flags & NO_HEADS) != 0 ? new String[] {"TROUBLE", "NO SPRINKLER HEADS"}
					: new String[] {"SYSTEM NORMAL", zone + "  ARMED"};
		};
	}

	/** A day time as the panel's clock shows it (HH:MM, the day starting at 06:00). */
	public static String clock(long dayTime) {
		long t = Math.floorMod(dayTime + 6000, 24000);
		return String.format(java.util.Locale.ROOT, "%02d:%02d", t / 1000, (t % 1000) * 60 / 1000);
	}
}
