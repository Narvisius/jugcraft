package io.github.jimbozoomer.jugcraft.fluid;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * Joined tanks (batch 20): tinplate and glass tanks touching face to face act as one tank of one fluid. Each block
 * still holds its own share (so a broken tank drops with what is in it, and old saves need nothing); this view fills
 * the lowest tanks first and drains the highest first, like one tall tank, and only takes the fluid the group
 * already holds. Pipes, pumps, buckets and comparators all see the group.
 */
public final class TankGroup extends CombinedStorage<FluidVariant, SingleFluidStorage> {
	/** Most tanks one group joins. */
	public static final int MAX_TANKS = 64;

	private TankGroup(List<SingleFluidStorage> parts) {
		super(parts);
	}

	/** The tanks joined to the tank at {@code start}, lowest first (just that one if it stands alone). */
	public static List<FluidTankBlockEntity> tanks(Level level, BlockPos start) {
		List<FluidTankBlockEntity> found = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(start.immutable());
		seen.add(start.immutable());
		while (!queue.isEmpty() && found.size() < MAX_TANKS) {
			BlockPos pos = queue.poll();
			if (!(level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank)) {
				continue;
			}
			found.add(tank);
			for (Direction direction : Direction.values()) {
				BlockPos next = pos.relative(direction);
				if (seen.add(next) && level.getBlockEntity(next) instanceof FluidTankBlockEntity) {
					queue.add(next);
				}
			}
		}
		found.sort(Comparator.comparingInt((FluidTankBlockEntity tank) -> tank.getBlockPos().getY())
				.thenComparingLong(tank -> tank.getBlockPos().asLong()));
		return found;
	}

	/** The joined tanks at {@code pos} as one storage. */
	public static TankGroup at(Level level, BlockPos pos) {
		return new TankGroup(tanks(level, pos).stream().map(tank -> tank.storage).toList());
	}

	/** The fluid the group holds: the lowest tank's that is not empty, or blank when all are empty. */
	public FluidVariant fluid() {
		for (SingleFluidStorage part : parts) {
			if (!part.isResourceBlank()) {
				return part.variant;
			}
		}
		return FluidVariant.blank();
	}

	public long amount() {
		return parts.stream().mapToLong(part -> part.amount).sum();
	}

	public long capacity() {
		return parts.stream().mapToLong(SingleFluidStorage::getCapacity).sum();
	}

	@Override
	public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
		FluidVariant held = fluid();
		if (!held.isBlank() && !held.equals(resource)) {
			return 0;
		}
		return super.insert(resource, maxAmount, transaction);
	}

	@Override
	public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
		long taken = 0;
		for (int i = parts.size() - 1; i >= 0 && taken < maxAmount; i--) {
			taken += parts.get(i).extract(resource, maxAmount - taken, transaction);
		}
		return taken;
	}
}
