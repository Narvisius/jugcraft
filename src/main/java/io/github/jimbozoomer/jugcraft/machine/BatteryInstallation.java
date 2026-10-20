package io.github.jimbozoomer.jugcraft.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Fixed, directed sockets: no graph walk, shared tank owners or chunk loading. */
public final class BatteryInstallation {
	public static final int MAX_PAIRS = 4;
	public static final long MAX_ENERGY = 320_000_000L;
	public static final int MAX_FLUID_MB = 320_000;
	private BatteryInstallation() {}

	public static int pairs(Level level, BlockPos master, Direction front) {
		int count = 0;
		for (int i = 0; i < MAX_PAIRS; i++) {
			BlockPos socket = master.offset(Footprint.rotate(new Vec3i(-1, 0, 2 + i), front));
			if (!module(level, socket, front, MachineKind.FLOW_TANK_MODULE)) break;
			count++;
		}
		return count;
	}

	public static boolean stack(Level level, BlockPos master, Direction front) {
		return module(level, master.offset(Footprint.rotate(new Vec3i(-1, 3, 0), front)), front, MachineKind.FLOW_STACK_MODULE);
	}

	private static boolean module(Level level, BlockPos pos, Direction front, MachineKind kind) {
		if (!level.hasChunkAt(pos)) return false;
		BlockState state = level.getBlockState(pos);
		return state.getBlock() instanceof MachineBlock machine && machine.kind() == kind
				&& state.getValue(MachineBlock.FACING) == front;
	}
}
