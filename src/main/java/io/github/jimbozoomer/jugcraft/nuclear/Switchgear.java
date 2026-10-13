package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.energy.EnergyNetworks;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
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
 * The 480 V load centre (round 25; Justin: "make sure they actually hold charge in game too, I want it to actually
 * work"): the switchgear lineup is a working power board. Its bus holds a charge ({@link #CAPACITY} JE) and keeps it.
 * Power comes in by cable to the back of its transformer section (the left two blocks as you face it), through the
 * main breaker. Its ten feeder breakers each send the bus's power out of their own section: the upper breaker's out of
 * the top of the section at the back, the lower breaker's down through the floor at the back. Right-clicking a
 * breaker's door opens or closes it (the owner, or their party in Party mode); anywhere else tells you the bus's state.
 * Its lamps, flags and meters are drawn live (client/NuclearRenderer.java) from what is synced here: the breakers, the
 * charge and each feeder's flow. In the medium plant both load centres in the control building are fed from the main
 * transformer by a buried cable, and their DC MCC breakers feed the DC rooms' motor control centres, which feed the
 * battery chargers (round 26: nuclear/MotorControlCenter.java).
 *
 * <p>The layout matches tools/control_boards.py load_center: in the lineup's frame (x to the right as you face it,
 * y up, z away from you), the transformer section is x 0..2, the main breaker's section x 2..3, then five feeder
 * sections a block wide; a section's upper breaker door is y 1.16..1.93, its lower y 0.14..1.13.
 */
public final class Switchgear {
	/** The bus's charge (JE). */
	public static final long CAPACITY = 2_000_000;
	/** What each feeder sends out a tick at most (JE/t), and what the incoming side takes in. */
	public static final long FEEDER_RATE = 2_048, INCOMING_RATE = 16_384;
	/** The main breaker, then the ten feeders (upper and lower of each section, from the left). */
	public static final int BREAKERS = 11;
	public static final String[] NAMES = {"MAIN BKR", "MCC 1", "MCC 2", "MCC 3", "MCC 4", "INVERTER", "DC MCC", "HVAC", "LIGHTING", "SPARE",
			"SPARE"};
	/** Every breaker closed but the two spares. */
	public static final int DEFAULT_BREAKERS = (1 << BREAKERS) - 1 & ~(1 << 9) & ~(1 << 10);

	private Switchgear() {
	}

	public static boolean closed(EquipmentBlockEntity e, int breaker) {
		return (e.breakers & 1 << breaker) != 0;
	}

	/** Where feeder {@code f} (1..10) sends its power out: its section's back cell, at the top or the bottom. */
	static BlockPos terminal(int f) {
		int section = (f - 1) / 2;
		return new BlockPos(3 + section, (f - 1) % 2 == 0 ? 2 : 0, 1);
	}

	/** The sides a feeder's terminal pushes out of: up or down, and back (away from the front). */
	static List<Direction> sides(int f, Direction facing) {
		return List.of((f - 1) % 2 == 0 ? Direction.UP : Direction.DOWN, facing);
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
			if (closed(e, f) && e.bus.getAmount() > 0) {
				BlockPos at = e.getBlockPos().offset(EquipmentKind.toWorld(terminal(f), facing));
				moved = EnergyNetworks.pushToNeighbors(level, at, e.bus, FEEDER_RATE, sides(f, facing));
			}
			e.flows[f] = (int) moved;
		}
		e.flows[0] = (int) e.incoming;
		e.incoming = 0;
	}

	// ------------------------------------------------------------------ the cables' view

	/**
	 * What a cable touching this cell of the lineup finds: the incoming side at the back of the transformer section
	 * (taking power while the main breaker is closed), a feeder's terminal (giving power while its breaker is closed),
	 * or nothing.
	 */
	public static @Nullable EnergyStorage storageAt(Level level, BlockPos pos, EquipmentBlockEntity e) {
		if (e.bus == null) {
			return null;
		}
		BlockPos local = EquipmentKind.toLocal(pos.subtract(e.getBlockPos()), e.facing());
		if (local.getZ() == 1 && local.getX() <= 1) {
			return e.incomingView();
		}
		for (int f = 1; f < BREAKERS; f++) {
			if (terminal(f).equals(local)) {
				return e.feederView(f);
			}
		}
		return null;
	}

	/**
	 * The incoming side: takes power into the bus while the main breaker is closed (a load centre's, or a motor control
	 * centre's with its own size and rate: nuclear/MotorControlCenter.java).
	 */
	static final class Incoming implements EnergyStorage {
		private final EquipmentBlockEntity e;
		private final long capacity, rate;

		Incoming(EquipmentBlockEntity e, long capacity, long rate) {
			this.e = e;
			this.capacity = capacity;
			this.rate = rate;
		}

		@Override
		public boolean supportsInsertion() {
			return closed(e, 0);
		}

		@Override
		public long insert(long maxAmount, TransactionContext transaction) {
			if (!closed(e, 0) || e.bus == null) {
				return 0;
			}
			long in = e.bus.insert(Math.min(maxAmount, rate), transaction);
			if (in > 0) {
				transaction.addCloseCallback((t, result) -> {
					if (result.wasCommitted()) e.incoming += in;
				});
			}
			return in;
		}

		@Override
		public boolean supportsExtraction() {
			return false;
		}

		@Override
		public long extract(long maxAmount, TransactionContext transaction) {
			return 0;
		}

		@Override
		public long getAmount() {
			return e.bus == null ? 0 : e.bus.getAmount();
		}

		@Override
		public long getCapacity() {
			return capacity;
		}
	}

	/** A feeder's terminal: gives the bus's power while its breaker is closed (the lineup pushes it out itself too). */
	static final class Feeder implements EnergyStorage {
		private final EquipmentBlockEntity e;
		private final int f;
		private final long capacity, rate;

		Feeder(EquipmentBlockEntity e, int f, long capacity, long rate) {
			this.e = e;
			this.f = f;
			this.capacity = capacity;
			this.rate = rate;
		}

		@Override
		public boolean supportsInsertion() {
			return false;
		}

		@Override
		public long insert(long maxAmount, TransactionContext transaction) {
			return 0;
		}

		@Override
		public boolean supportsExtraction() {
			return closed(e, f);
		}

		@Override
		public long extract(long maxAmount, TransactionContext transaction) {
			return closed(e, f) && e.bus != null ? e.bus.extract(Math.min(maxAmount, rate), transaction) : 0;
		}

		@Override
		public long getAmount() {
			return e.bus == null ? 0 : e.bus.getAmount();
		}

		@Override
		public long getCapacity() {
			return capacity;
		}
	}

	// ------------------------------------------------------------------ using it

	/** The breaker whose door is at this point of the lineup's front (its own frame), or -1. */
	public static int breakerAt(double x, double y, double z) {
		if (z > 0.6) {
			return -1;
		}
		if (x >= 2.0 && x <= 2.96 && y >= 0.14 && y <= 1.68) {
			return 0;
		}
		double w = (7.96 - 2.98) / 5;
		if (x < 2.98 || x > 7.96) {
			return -1;
		}
		int section = Math.min(4, (int) ((x - 2.98) / w));
		if (y >= 1.16 && y <= 1.93) {
			return 1 + 2 * section;
		}
		if (y >= 0.14 && y <= 1.13) {
			return 2 + 2 * section;
		}
		return -1;
	}

	/** A point in the world in the lineup's frame (x right, y up, z away from its front), anchor's corner at 0. */
	static Vec3 local(EquipmentBlockEntity e, Vec3 at) {
		Direction facing = e.facing(), right = facing.getClockWise();
		BlockPos a = e.getBlockPos();
		double dx = at.x - a.getX() - 0.5, dz = at.z - a.getZ() - 0.5;
		double x = dx * right.getStepX() + dz * right.getStepZ() + 0.5;
		double z = dx * facing.getStepX() + dz * facing.getStepZ() + 0.5;
		return new Vec3(x, at.y - a.getY(), z);
	}

	/** Right-clicking the lineup: a breaker's door opens or closes it; anywhere else tells the bus's state. */
	public static void use(Level level, EquipmentBlockEntity e, Player player, @Nullable Vec3 hit) {
		if (level.isClientSide() || e.bus == null) {
			return;
		}
		int breaker = -1;
		if (hit != null) {
			Vec3 p = local(e, hit);
			breaker = breakerAt(p.x, p.y, p.z);
		}
		if (breaker < 0) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.load_center",
					String.format("%,d", e.bus.getAmount()), String.format("%,d", CAPACITY), Math.round(100.0 * e.bus.getAmount() / CAPACITY),
					Component.translatable(closed(e, 0) ? "message.jugcraft.nuclear.breaker_closed" : "message.jugcraft.nuclear.breaker_open"),
					String.format("%,d", e.flows[0])));
			return;
		}
		if (!e.mayUse(player)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.breaker_not_owner"));
			return;
		}
		e.breakers ^= 1 << breaker;
		boolean now = closed(e, breaker);
		level.playSound(null, e.getBlockPos().offset(EquipmentKind.toWorld(new BlockPos(breaker == 0 ? 2 : 3 + (breaker - 1) / 2, 1, 0), e.facing())),
				now ? SoundEvents.IRON_DOOR_CLOSE : SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.9F, now ? 0.55F : 0.7F);
		player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.breaker", NAMES[breaker],
				Component.translatable(now ? "message.jugcraft.nuclear.breaker_closed" : "message.jugcraft.nuclear.breaker_open")));
		e.changed();
	}
}
