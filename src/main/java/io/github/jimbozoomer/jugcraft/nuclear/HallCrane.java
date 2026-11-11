package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The turbine hall's overhead crane (turbine hall part 3; Justin: "Works from a pendant"; tools/hall_crane.py draws it):
 * a double girder bridge on its runway with a trolley across it carrying the 160 ton main hoist and the 25 ton
 * auxiliary hoist. It runs from the crane pendant stations (CranePendantBlock) as a real pendant works it: hold a button
 * and it runs that way (the game repeats a held use every four ticks; each press keeps it running {@link #HOLD} ticks
 * more), let go and it stops. NORTH, SOUTH, WEST and EAST run whichever of the bridge (along the runway, the way the
 * crane faces) and the trolley (across it) goes that way; the hooks go up to their rest and down to the first solid
 * block under them; the bridge runs to the runway's ends (the blocks under both end trucks). STOP takes its power off
 * (everything stops where it is) until START; the horn sounds.
 *
 * <p>Each motion's where it set off from, when, which way, until when and where it must stop are the crane's
 * (EquipmentBlockEntity crane*: saved and sent to the players, who draw it moving with {@link #at}: client/CraneParts).
 */
public final class HallCrane {
	public static final int BRIDGE = 0, TROLLEY = 1, MAIN = 2, AUX = 3;
	/** Each motion's speed (blocks a tick): the bridge 2 a second, the trolley 1, the main hoist half a block, the aux 1. */
	public static final double[] SPEED = {0.1, 0.05, 0.025, 0.05};
	/** The trolley's travel across the bridge (its middle, the bridge's x; tools/hall_crane.py TROLLEY_X) and its home. */
	public static final double TROLLEY_MIN = 4.5, TROLLEY_MAX = 37.5, TROLLEY_HOME = 21.0;
	/**
	 * The hooks under the trolley (tools/hall_crane.py): each one's x off the trolley's middle, the z they hang at, and
	 * each one's lowest point at rest (the hook's tip; blocks over the anchor). A hook goes down until that is on a block.
	 */
	public static final double MAIN_DX = -0.35, AUX_DX = -2.0, HOOK_Z = 3.0, MAIN_TIP = 0.7883, AUX_TIP = 1.6792;
	/** How far a hook can go down at most (blocks), and how far the runway is looked along each way. */
	public static final double MAX_DROP = 40;
	static final int RUNWAY_LOOK = 200;
	/** Ticks a press keeps a motion running (the next held press comes four ticks after). */
	public static final int HOLD = 6;
	/** How far from a pendant station its crane may be (blocks): a turbine hall's length. */
	public static final int REACH = 110;
	/** The pendant's buttons in order (tools/hall_fittings.py CP_BUTTONS). */
	public static final String[] BUTTONS = {"stop", "start", "main_up", "main_down", "aux_up", "aux_down", "north", "south", "west", "east", "horn"};
	public static final String PENDANT = "tb_crane_pendant";

	private HallCrane() {
	}

	public static boolean isPendant(BlockState state) {
		return DcRoom.is(state, PENDANT);
	}

	/** Where a motion is at game time {@code now} (blocks): moving from where it set off until it stops or runs out. */
	public static double at(EquipmentBlockEntity crane, int axis, double now) {
		int dir = crane.craneDir[axis];
		double from = crane.craneFrom[axis];
		if (dir == 0) {
			return from;
		}
		double t = Math.max(0, Math.min(now, crane.craneUntil[axis]) - crane.craneAt[axis]);
		double v = from + dir * SPEED[axis] * t;
		return dir > 0 ? Math.min(v, crane.craneStop[axis]) : Math.max(v, crane.craneStop[axis]);
	}

	/** Whether a motion is moving at game time {@code now}. */
	public static boolean moving(EquipmentBlockEntity crane, int axis, double now) {
		return crane.craneDir[axis] != 0 && now < crane.craneUntil[axis] && Math.abs(at(crane, axis, now) - crane.craneStop[axis]) > 1e-6;
	}

	/** Once a second: the runway's ends (for the client to lay it), the first time and whenever they change. */
	static void tick(ServerLevel level, EquipmentBlockEntity crane, long now) {
		if (Math.floorMod(now + crane.getBlockPos().asLong(), 20) != 0) {
			return;
		}
		double[] ends = runway(level, crane);
		if (ends[0] != crane.craneLo || ends[1] != crane.craneHi) {
			crane.craneLo = ends[0];
			crane.craneHi = ends[1];
			crane.changed();
		}
	}

	/**
	 * The runway's ends either side of the anchor (the bridge's offsets along it, whole blocks): as far as there are solid
	 * blocks under both end trucks (the bridge's x 0 and its far side) the whole depth of the bridge.
	 */
	static double[] runway(ServerLevel level, EquipmentBlockEntity crane) {
		int lo = 0, hi = 0;
		while (lo > -RUNWAY_LOOK && supported(level, crane, lo - 1)) {
			lo--;
		}
		while (hi < RUNWAY_LOOK && supported(level, crane, hi + 1)) {
			hi++;
		}
		return new double[] {lo, hi};
	}

	/** Whether the bridge offset {@code k} along the runway has solid blocks under both end trucks. */
	static boolean supported(ServerLevel level, EquipmentBlockEntity crane, int k) {
		EquipmentKind kind = EquipmentKind.OVERHEAD_CRANE;
		Direction facing = crane.facing();
		for (int x : new int[] {0, kind.wide - 1}) {
			for (int z = 0; z < kind.deep; z++) {
				BlockPos under = crane.getBlockPos().offset(EquipmentKind.toWorld(new BlockPos(x, -1, z + k), facing));
				if (!level.isLoaded(under) || level.getBlockState(under).getCollisionShape(level, under).isEmpty()) {
					return false;
				}
			}
		}
		return true;
	}

	/** How far a hook can go down from its rest where the bridge and trolley are now: to the first solid block under it. */
	static double floorUnder(ServerLevel level, EquipmentBlockEntity crane, boolean main, double now) {
		double bridge = at(crane, BRIDGE, now), trolley = at(crane, TROLLEY, now);
		double lx = trolley + (main ? MAIN_DX : AUX_DX), lz = bridge + HOOK_Z;
		double tip = crane.getBlockPos().getY() + (main ? MAIN_TIP : AUX_TIP);
		Direction facing = crane.facing(), right = facing.getClockWise();
		double wx = crane.getBlockPos().getX() + 0.5 + right.getStepX() * (lx - 0.5) + facing.getStepX() * (lz - 0.5);
		double wz = crane.getBlockPos().getZ() + 0.5 + right.getStepZ() * (lx - 0.5) + facing.getStepZ() * (lz - 0.5);
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		for (int y = (int) Math.floor(tip) - 1; y >= tip - MAX_DROP - 1 && y >= level.getMinY(); y--) {
			at.set(Math.floor(wx), y, Math.floor(wz));
			if (!level.isLoaded(at)) {
				break;
			}
			if (!level.getBlockState(at).getCollisionShape(level, at).isEmpty() && level.getBlockState(at).getBlock() != PlantInterior.BLOCKS.get("tb_floor_mark")) {
				return Math.max(0, Math.min(MAX_DROP, tip - (y + 1)));
			}
		}
		return MAX_DROP;
	}

	/** The crane nearest a pendant station, in reach, or null. */
	public static @Nullable EquipmentBlockEntity near(ServerLevel level, BlockPos pendant) {
		BlockPos best = null;
		for (Map.Entry<BlockPos, EquipmentRegistry.Entry> e : EquipmentRegistry.of(level).anchors().entrySet()) {
			if (e.getValue().kind() == EquipmentKind.OVERHEAD_CRANE && e.getKey().distSqr(pendant) <= (double) REACH * REACH
					&& (best == null || e.getKey().distSqr(pendant) < best.distSqr(pendant))) {
				best = e.getKey();
			}
		}
		return best != null && level.getBlockEntity(best) instanceof EquipmentBlockEntity crane && crane.kind() == EquipmentKind.OVERHEAD_CRANE
				? crane : null;
	}

	/** A pendant button pressed (or held: pressed again every four ticks) for a crane. */
	public static void press(ServerLevel level, EquipmentBlockEntity crane, String button, @Nullable Player player) {
		long now = level.getGameTime();
		BlockPos where = crane.getBlockPos();
		switch (button) {
			case "stop" -> {
				for (int k = 0; k < 4; k++) {
					halt(crane, k, now);
				}
				if (crane.cranePower) {
					crane.cranePower = false;
					level.playSound(null, where, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.8F, 0.6F);
				}
				tell(player, Component.translatable("message.jugcraft.nuclear.crane_stopped"));
				crane.changed();
				return;
			}
			case "start" -> {
				if (!crane.cranePower) {
					crane.cranePower = true;
					level.playSound(null, where, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.8F, 0.8F);
					crane.changed();
				}
				tell(player, Component.translatable("message.jugcraft.nuclear.crane_power"));
				return;
			}
			case "horn" -> {
				level.playSound(null, where.above(2), SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), SoundSource.BLOCKS, 3.0F, 0.75F);
				return;
			}
			default -> {
			}
		}
		if (!crane.cranePower) {
			tell(player, Component.translatable("message.jugcraft.nuclear.crane_off"));
			return;
		}
		int axis, dir;
		switch (button) {
			case "main_up" -> {
				axis = MAIN;
				dir = -1;
			}
			case "main_down" -> {
				axis = MAIN;
				dir = 1;
			}
			case "aux_up" -> {
				axis = AUX;
				dir = -1;
			}
			case "aux_down" -> {
				axis = AUX;
				dir = 1;
			}
			default -> {
				// NORTH, SOUTH, WEST, EAST: the bridge if it runs that way (along the way the crane faces), else the trolley.
				Direction d = switch (button) {
					case "north" -> Direction.NORTH;
					case "south" -> Direction.SOUTH;
					case "west" -> Direction.WEST;
					default -> Direction.EAST;
				};
				Direction facing = crane.facing();
				if (d.getAxis() == facing.getAxis()) {
					axis = BRIDGE;
					dir = d == facing ? 1 : -1;
				} else {
					axis = TROLLEY;
					dir = d == facing.getClockWise() ? 1 : -1;
				}
			}
		}
		double here = at(crane, axis, now);
		double stop = switch (axis) {
			case BRIDGE -> {
				double[] ends = runway(level, crane);
				crane.craneLo = ends[0];
				crane.craneHi = ends[1];
				yield dir > 0 ? ends[1] : ends[0];
			}
			case TROLLEY -> dir > 0 ? TROLLEY_MAX : TROLLEY_MIN;
			default -> dir > 0 ? floorUnder(level, crane, axis == MAIN, now) : 0;
		};
		if (dir > 0 ? here >= stop - 1e-6 : here <= stop + 1e-6) {
			tell(player, Component.translatable("message.jugcraft.nuclear.crane_end_" + (axis == BRIDGE ? "bridge" : axis == TROLLEY ? "trolley"
					: dir > 0 ? "floor" : "top")));
			halt(crane, axis, now);
			crane.changed();
			return;
		}
		boolean starting = !moving(crane, axis, now) || crane.craneDir[axis] != dir;
		crane.craneFrom[axis] = here;
		crane.craneAt[axis] = now;
		crane.craneDir[axis] = dir;
		crane.craneUntil[axis] = now + HOLD;
		crane.craneStop[axis] = stop;
		if (starting) {
			level.playSound(null, where, axis <= TROLLEY ? SoundEvents.MINECART_RIDING : SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS,
					axis <= TROLLEY ? 0.35F : 0.9F, axis == BRIDGE ? 0.7F : axis == TROLLEY ? 1.0F : dir > 0 ? 0.7F : 1.0F);
		}
		crane.changed();
	}

	/** Stops a motion where it is now. */
	static void halt(EquipmentBlockEntity crane, int axis, long now) {
		double here = at(crane, axis, now);
		crane.craneFrom[axis] = here;
		crane.craneStop[axis] = here;
		crane.craneAt[axis] = now;
		crane.craneUntil[axis] = now;
		crane.craneDir[axis] = 0;
	}

	/** What the crane says when it is clicked: its power, where its bridge, trolley and hooks are. */
	public static Component status(EquipmentBlockEntity crane, double now) {
		return Component.translatable("message.jugcraft.nuclear.crane_status",
				Component.translatable("message.jugcraft.nuclear." + (crane.cranePower ? "crane_on" : "crane_power_off")),
				String.format(java.util.Locale.ROOT, "%.1f", at(crane, BRIDGE, now)), String.format(java.util.Locale.ROOT, "%.1f", at(crane, TROLLEY, now)),
				String.format(java.util.Locale.ROOT, "%.1f", at(crane, MAIN, now)), String.format(java.util.Locale.ROOT, "%.1f", at(crane, AUX, now)));
	}

	private static void tell(@Nullable Player player, Component message) {
		if (player != null) {
			player.sendOverlayMessage(message);
		}
	}
}
