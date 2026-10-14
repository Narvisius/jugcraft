package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.energy.EnergyNetworks;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The 480 V motor control centre (round 26, 8a; Justin: the DC rooms' 480 V lineup should be its own thing, and it must
 * work): four sections a block wide, one deep and three high, standing in a DC equipment room. Like the load centre
 * ({@link Switchgear}) its bus holds a charge ({@link #CAPACITY} JE) and keeps it. Power comes in by cable under its
 * first section (its incoming section), through the main breaker; each feeder breaker sends the bus's power out of
 * its own terminal: a cell of its section, out of the bottom, the top or the back ({@link #TERMINALS}). Right-clicking
 * a unit's door switches its breaker (the owner, or their party in Party mode); anywhere else tells you the bus's
 * state. Its handles, lamps and meters are drawn live (client/NuclearRenderer.java) from what is synced: the breakers,
 * the charge and each feeder's flow. In the medium plant each DC equipment room has one: fed from its train's load
 * centre's DC MCC breaker, its CHGR 1 and CHGR 2 breakers each feeding one of the room's battery chargers.
 *
 * <p>The layout matches tools/control_boards.py motor_control_center: in the lineup's frame (x to the right as you
 * face it, y up, z away from you) section k is x k..k+1; {@link #DOORS} are the units' doors.
 */
public final class MotorControlCenter {
	/** The bus's charge (JE). */
	public static final long CAPACITY = 500_000;
	/** What each feeder sends out a tick at most (JE/t: a battery charger's 150 A), and what the incoming side takes. */
	public static final long FEEDER_RATE = 1_024, INCOMING_RATE = 4_096;
	/** The main breaker, then the feeders, section by section from the left, top down. */
	public static final int BREAKERS = 8;
	public static final String[] NAMES = {"MAIN BKR", "REG XF 1", "REG XF 2", "SPARE", "CHGR 1", "EXH FAN", "CHGR 2", "DC AHU"};
	/** Every breaker closed but the spare. */
	public static final int DEFAULT_BREAKERS = (1 << BREAKERS) - 1 & ~(1 << 3);
	private static final int DOWN = 0, UP = 1, BACK = 2;
	/** Each feeder's terminal: its cell (section, height) and the side it gives out of; null for the main and the spare. */
	private static final int[][] TERMINALS = {null, {1, 2, UP}, {1, 1, BACK}, null, {2, 0, DOWN}, {2, 2, UP}, {3, 0, BACK}, {3, 2, UP}};
	/** Each breaker's unit door in the lineup's frame: x0, y0, x1, y1 (the doors stand out of the face at z 0.1). */
	static final double[][] DOORS = {
			{0.04, 1.186, 0.74, 1.974}, {1.04, 1.586, 1.74, 1.974}, {1.04, 1.186, 1.74, 1.574}, {1.04, 0.786, 1.74, 1.174},
			{2.04, 1.386, 2.74, 1.974}, {2.04, 0.786, 2.74, 1.374}, {3.04, 1.386, 3.74, 1.974}, {3.04, 0.786, 3.74, 1.374}};

	private MotorControlCenter() {
	}

	/** Where feeder {@code f} gives its power out, or null. */
	static @Nullable BlockPos terminal(int f) {
		int[] t = TERMINALS[f];
		return t == null ? null : new BlockPos(t[0], t[1], 0);
	}

	static List<Direction> sides(int f, Direction facing) {
		return List.of(switch (TERMINALS[f][2]) {
			case DOWN -> Direction.DOWN;
			case UP -> Direction.UP;
			default -> facing;
		});
	}

	// ------------------------------------------------------------------ ticking

	/** Sends each closed feeder's share out of its terminal; counts the flows (synced for the meters). */
	public static void tick(ServerLevel level, EquipmentBlockEntity e) {
		if (e.bus == null) {
			return;
		}
		Direction facing = e.facing();
		for (int f = 1; f < BREAKERS; f++) {
			long moved = 0;
			BlockPos t = terminal(f);
			if (t != null && Switchgear.closed(e, f) && e.bus.getAmount() > 0) {
				BlockPos at = e.getBlockPos().offset(EquipmentKind.toWorld(t, facing));
				moved = EnergyNetworks.pushToNeighbors(level, at, e.bus, FEEDER_RATE, sides(f, facing));
			}
			e.flows[f] = (int) moved;
		}
		e.flows[0] = (int) e.incoming;
		e.incoming = 0;
	}

	// ------------------------------------------------------------------ the cables' view

	/** What a cable touching this cell finds: the incoming side (the first section's bottom cell), a feeder's terminal, or nothing. */
	public static @Nullable EnergyStorage storageAt(Level level, BlockPos pos, EquipmentBlockEntity e) {
		if (e.bus == null) {
			return null;
		}
		BlockPos local = EquipmentKind.toLocal(pos.subtract(e.getBlockPos()), e.facing());
		if (local.equals(BlockPos.ZERO)) {
			return e.incomingView();
		}
		for (int f = 1; f < BREAKERS; f++) {
			if (local.equals(terminal(f))) {
				return e.feederView(f);
			}
		}
		return null;
	}

	// ------------------------------------------------------------------ using it

	/** The breaker whose unit door is at this point of the lineup's front (its own frame), or -1. */
	public static int breakerAt(double x, double y, double z) {
		if (z > 0.5) {
			return -1;
		}
		for (int b = 0; b < BREAKERS; b++) {
			double[] d = DOORS[b];
			if (x >= d[0] && x <= d[2] && y >= d[1] && y <= d[3]) {
				return b;
			}
		}
		return -1;
	}

	/** Right-clicking the lineup: a unit's door switches its breaker; anywhere else tells the bus's state. */
	public static void use(Level level, EquipmentBlockEntity e, Player player, @Nullable Vec3 hit) {
		if (level.isClientSide() || e.bus == null) {
			return;
		}
		int breaker = -1;
		if (hit != null) {
			Vec3 p = Switchgear.local(e, hit);
			breaker = breakerAt(p.x, p.y, p.z);
		}
		if (breaker < 0) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.mcc",
					String.format("%,d", e.bus.getAmount()), String.format("%,d", CAPACITY), Math.round(100.0 * e.bus.getAmount() / CAPACITY),
					Component.translatable(Switchgear.closed(e, 0) ? "message.jugcraft.nuclear.breaker_closed" : "message.jugcraft.nuclear.breaker_open"),
					String.format("%,d", e.flows[0])));
			return;
		}
		if (!e.mayUse(player)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.breaker_not_owner"));
			return;
		}
		e.breakers ^= 1 << breaker;
		boolean now = Switchgear.closed(e, breaker);
		double[] d = DOORS[breaker];
		level.playSound(null, e.getBlockPos().offset(EquipmentKind.toWorld(new BlockPos((int) d[0], (int) d[1], 0), e.facing())),
				now ? SoundEvents.IRON_DOOR_CLOSE : SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.8F, now ? 0.7F : 0.85F);
		player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.breaker", NAMES[breaker],
				Component.translatable(now ? "message.jugcraft.nuclear.breaker_closed" : "message.jugcraft.nuclear.breaker_open")));
		e.changed();
	}
}
