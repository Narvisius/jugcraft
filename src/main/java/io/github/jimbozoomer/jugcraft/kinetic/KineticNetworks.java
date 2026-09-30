package io.github.jimbozoomer.jugcraft.kinetic;

import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Kinetic power: rotation carried by shafts and gearboxes, measured in KE (kinetic energy) per tick.
 * <ul>
 * <li>A shaft passes rotation along its axis only; a gearbox passes it out of all six sides; a belt passes it
 * from one pulley to the pulley it links.</li>
 * <li>A source (steam engine, hand crank) pushes KE out of one face. The network is every shaft and
 * gearbox reachable from that face and every {@link KineticConsumer} at its ends: machines take KE
 * as if it were JE, and the dynamo turns it into JE for cables.</li>
 * <li>Networks are found by a bounded search and cached per level; any shaft, gearbox or source
 * placement, removal or neighbor change clears the cache.</li>
 * </ul>
 * Shafts and gearboxes show a turning texture while a source drives them (see {@link #markTurning}).
 */
public final class KineticNetworks {
	/** Largest number of shafts and gearboxes one network may contain. */
	public static final int MAX_PARTS = 256;
	/** Ticks a shaft keeps turning after the last push through it. */
	public static final int SPIN_DOWN = 10;

	private static final Map<Level, Map<Key, Network>> CACHE = new WeakHashMap<>();
	private static final Map<Level, Map<BlockPos, Long>> TURNED = new WeakHashMap<>();

	private record Key(BlockPos pos, Direction side) {
	}

	private record Network(List<BlockPos> parts, List<BlockPos> consumers, List<Direction> sides) {
	}

	private KineticNetworks() {
	}

	public static void invalidate(Level level) {
		CACHE.remove(level);
	}

	/** Whether this block passes rotation between {@code from} and the opposite side. */
	static boolean carries(BlockState state, Direction travel) {
		if (state.getBlock() instanceof ShaftBlock) {
			return state.getValue(ShaftBlock.AXIS) == travel.getAxis();
		}
		return state.getBlock() instanceof GearboxBlock;
	}

	/**
	 * Pushes up to {@code amount} KE out of {@code side} of the source at {@code pos}, split evenly between
	 * the consumers of the network there. Returns how much was taken. The network turns while the source
	 * runs, whether or not anything takes power.
	 */
	public static long push(ServerLevel level, BlockPos pos, Direction side, long amount) {
		if (amount <= 0) {
			return 0;
		}
		Network network = CACHE.computeIfAbsent(level, l -> new HashMap<>())
				.computeIfAbsent(new Key(pos.immutable(), side), key -> discover(level, key.pos(), key.side()));
		markTurning(level, network.parts());
		List<KineticConsumer> consumers = new ArrayList<>();
		List<Direction> sides = new ArrayList<>();
		for (int i = 0; i < network.consumers().size(); i++) {
			if (level.getBlockEntity(network.consumers().get(i)) instanceof KineticConsumer consumer) {
				consumers.add(consumer);
				sides.add(network.sides().get(i));
			}
		}
		long given = 0;
		// Two passes: an even share each, then what is left to whoever still has room.
		for (int pass = 0; pass < 2 && given < amount && !consumers.isEmpty(); pass++) {
			long share = pass == 0 ? Math.max(1, amount / consumers.size()) : amount - given;
			for (int i = 0; i < consumers.size() && given < amount; i++) {
				given += consumers.get(i).acceptKinetic(sides.get(i), Math.min(share, amount - given));
			}
		}
		return given;
	}

	private static Network discover(Level level, BlockPos source, Direction side) {
		List<BlockPos> parts = new ArrayList<>();
		List<BlockPos> consumers = new ArrayList<>();
		List<Direction> sides = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		seen.add(source);
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		ArrayDeque<Direction> travel = new ArrayDeque<>();
		queue.add(source.relative(side));
		travel.add(side);
		while (!queue.isEmpty() && parts.size() < MAX_PARTS) {
			BlockPos pos = queue.poll();
			Direction direction = travel.poll();
			BlockState state = level.getBlockState(pos);
			if (carries(state, direction)) {
				if (!seen.add(pos)) {
					continue;
				}
				parts.add(pos);
				if (state.getBlock() instanceof GearboxBlock) {
					for (Direction out : Direction.values()) {
						if (out != direction.getOpposite()) {
							queue.add(pos.relative(out));
							travel.add(out);
						}
					}
				} else {
					queue.add(pos.relative(direction));
					travel.add(direction);
					// A belt carries the rotation on to the linked pulley, which passes it both ways along its axis.
					if (level.getBlockEntity(pos) instanceof BeltPulleyBlockEntity pulley && pulley.link() != null
							&& seen.add(pulley.link())) {
						BlockPos other = pulley.link();
						parts.add(other);
						Direction.Axis axis = level.getBlockState(other).getValue(ShaftBlock.AXIS);
						for (Direction out : Direction.values()) {
							if (out.getAxis() == axis) {
								queue.add(other.relative(out));
								travel.add(out);
							}
						}
					}
				}
			} else if (!seen.contains(pos)) {
				// Any block of a multi-block machine passes power to the machine's master block.
				MachineBlockEntity machine = MachineBlock.machineAt(level, pos, state);
				BlockPos target = machine != null ? machine.getBlockPos() : pos;
				if (!consumers.contains(target) && level.getBlockEntity(target) instanceof KineticConsumer) {
					consumers.add(target);
					sides.add(direction.getOpposite());
				}
			}
		}
		return new Network(parts, consumers, sides);
	}

	/** Records that these shafts and gearboxes turned this tick, and switches on their turning look. */
	private static void markTurning(ServerLevel level, List<BlockPos> parts) {
		Map<BlockPos, Long> turned = TURNED.computeIfAbsent(level, l -> new HashMap<>());
		long now = level.getGameTime();
		for (BlockPos pos : parts) {
			turned.put(pos, now);
			BlockState state = level.getBlockState(pos);
			if (state.hasProperty(ShaftBlock.TURNING) && !state.getValue(ShaftBlock.TURNING)) {
				// Clients only: a look change must not look like a neighbor change to the network cache.
				level.setBlock(pos, state.setValue(ShaftBlock.TURNING, true), Block.UPDATE_CLIENTS);
				level.scheduleTick(pos, state.getBlock(), SPIN_DOWN * 2);
			}
		}
	}

	/** Whether a source pushed through this shaft or gearbox in the last {@link #SPIN_DOWN} ticks. */
	static boolean recentlyTurned(ServerLevel level, BlockPos pos) {
		Long last = TURNED.computeIfAbsent(level, l -> new HashMap<>()).get(pos);
		if (last == null || level.getGameTime() - last > SPIN_DOWN) {
			TURNED.get(level).remove(pos);
			return false;
		}
		return true;
	}
}
