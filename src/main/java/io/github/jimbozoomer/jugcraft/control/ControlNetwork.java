package io.github.jimbozoomer.jugcraft.control;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * A control network (batch 36): the data cables joined to a block (a logic controller, or a monitor looking for
 * one), and the sensors, switches (relays and alarms) and controllers touching them or that block. Found by a walk
 * along the cables, at most {@link #MAX_CABLES} long; devices do not pass the network on, so two separate cable runs stay separate. Unloaded chunks end the walk.
 */
public final class ControlNetwork {
	public static final int MAX_CABLES = 1_024;

	public record Devices(List<BlockPos> sensors, List<BlockPos> switches, List<BlockPos> controllers, int cables) {
	}

	private ControlNetwork() {
	}

	public static Devices find(Level level, BlockPos controller) {
		List<BlockPos> sensors = new ArrayList<>();
		List<BlockPos> switches = new ArrayList<>();
		List<BlockPos> controllers = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		seen.add(controller);
		queue.add(controller);
		int cables = 0;
		while (!queue.isEmpty()) {
			BlockPos at = queue.poll();
			for (Direction direction : Direction.values()) {
				BlockPos next = at.relative(direction);
				if (!seen.add(next) || !level.isLoaded(next)) {
					continue;
				}
				Block block = level.getBlockState(next).getBlock();
				if (block instanceof DataCableBlock && cables < MAX_CABLES) {
					cables++;
					queue.add(next);
				} else if (block instanceof SensorBlock) {
					sensors.add(next);
				} else if (block instanceof ChannelSwitch) {
					switches.add(next);
				} else if (block instanceof LogicControllerBlock) {
					controllers.add(next);
				}
			}
		}
		return new Devices(sensors, switches, controllers, cables);
	}
}
