package io.github.jimbozoomer.jugcraft.blueprint;

import java.util.Arrays;
import java.util.BitSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A placed blueprint's cells in the world, kept compact so a whole power plant (a third of a million blocks) costs a
 * few megabytes: absolute positions bottom layer first, where each layer starts, a sorted copy for finding a
 * position, and which cells are known to be done. Used by the stake on the server and by its hologram on the client.
 */
public final class BuildSite {
	public final Blueprint blueprint;
	public final Rotation rotation;
	public final BlockPos origin;
	private final long[] pos;
	private final BlockState[] states;
	/** layerStart[k] is the first cell of layer k (y = minY + k); the last entry is the cell count. */
	private final int[] layerStart;
	public final int minY;
	private final long[] sortedPos;
	private final int[] sortedIdx;
	/** Cells known to be in place (set when one is seen or filled; checked again from time to time). */
	public final BitSet done;

	/** Water (pools, basins) in the blueprint: not built by drones; the stake fills it when the build is finished. */
	private final long[] fluidPos;
	private final BlockState[] fluidStates;

	public BuildSite(Blueprint blueprint, Rotation rotation, BlockPos origin) {
		this.blueprint = blueprint;
		this.rotation = rotation;
		this.origin = origin.immutable();
		int total = blueprint.size();
		int fluids = 0;
		for (int i = 0; i < total; i++) {
			if (fluid(blueprint.state(i, Rotation.NONE))) fluids++;
		}
		int n = total - fluids;
		pos = new long[n];
		states = new BlockState[n];
		fluidPos = new long[fluids];
		fluidStates = new BlockState[fluids];
		int lowest = Integer.MAX_VALUE, highest = Integer.MIN_VALUE;
		for (int i = 0; i < total; i++) {
			if (!fluid(blueprint.state(i, Rotation.NONE))) {
				lowest = Math.min(lowest, origin.getY() + blueprint.offsetY(i));
				highest = Math.max(highest, origin.getY() + blueprint.offsetY(i));
			}
		}
		if (n == 0) {
			lowest = highest = origin.getY();
		}
		minY = lowest;
		layerStart = new int[highest - lowest + 2];
		int layer = 0;
		int k0 = 0, f = 0;
		for (int i = 0; i < total; i++) {
			BlockState st = blueprint.state(i, rotation);
			BlockPos p = origin.offset(blueprint.offset(i, rotation));
			if (fluid(st)) {
				fluidPos[f] = p.asLong();
				fluidStates[f++] = st;
				continue;
			}
			int idx = k0++;
			pos[idx] = p.asLong();
			states[idx] = st;
			int k = p.getY() - lowest;
			while (layer <= k) {
				layerStart[layer++] = idx;
			}
		}
		while (layer < layerStart.length) {
			layerStart[layer++] = n;
		}
		Integer[] order = new Integer[n];
		for (int i = 0; i < n; i++) {
			order[i] = i;
		}
		Arrays.sort(order, (a, b) -> Long.compare(pos[a], pos[b]));
		sortedPos = new long[n];
		sortedIdx = new int[n];
		for (int i = 0; i < n; i++) {
			sortedPos[i] = pos[order[i]];
			sortedIdx[i] = order[i];
		}
		done = new BitSet(n);
	}

	public int size() {
		return pos.length;
	}

	/** A block the drones never place: a still liquid (the blueprint's water). */
	public static boolean fluid(BlockState state) {
		return state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock;
	}

	/** Puts the blueprint's water in (where there is air or another liquid): the build's commissioning. */
	public void fillFluids(net.minecraft.world.level.Level level) {
		for (int i = 0; i < fluidPos.length; i++) {
			BlockPos p = BlockPos.of(fluidPos[i]);
			if (level.isLoaded(p) && (level.getBlockState(p).isAir() || !level.getBlockState(p).getFluidState().isEmpty())) {
				level.setBlock(p, fluidStates[i], net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
			}
		}
	}

	public long pos(int i) {
		return pos[i];
	}

	public BlockPos blockPos(int i) {
		return BlockPos.of(pos[i]);
	}

	public BlockState state(int i) {
		return states[i];
	}

	/** Number of layers (bottom to top). */
	public int layers() {
		return layerStart.length - 1;
	}

	/** First cell of layer {@code k} (clamped); {@code layerFrom(layers())} is the cell count. */
	public int layerFrom(int k) {
		return layerStart[Math.max(0, Math.min(layerStart.length - 1, k))];
	}

	public int layerOf(int i) {
		return BlockPos.getY(pos[i]) - minY;
	}

	/** The cell at a world position, or -1. */
	public int find(BlockPos p) {
		return find(p.asLong());
	}

	public int find(long p) {
		int k = Arrays.binarySearch(sortedPos, p);
		return k < 0 ? -1 : sortedIdx[k];
	}

	public boolean matches(BlockState world, int i) {
		return SurveyStakeBlockEntity.matches(world, states[i]);
	}
}
