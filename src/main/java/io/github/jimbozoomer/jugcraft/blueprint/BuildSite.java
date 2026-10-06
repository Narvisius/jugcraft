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

	public BuildSite(Blueprint blueprint, Rotation rotation, BlockPos origin) {
		this.blueprint = blueprint;
		this.rotation = rotation;
		this.origin = origin.immutable();
		int n = blueprint.size();
		pos = new long[n];
		states = new BlockState[n];
		int lowest = n == 0 ? origin.getY() : origin.getY() + blueprint.offsetY(0);
		int highest = n == 0 ? origin.getY() : origin.getY() + blueprint.offsetY(n - 1);
		minY = lowest;
		layerStart = new int[highest - lowest + 2];
		int layer = 0;
		for (int i = 0; i < n; i++) {
			BlockPos p = origin.offset(blueprint.offset(i, rotation));
			pos[i] = p.asLong();
			states[i] = blueprint.state(i, rotation);
			int k = p.getY() - lowest;
			while (layer <= k) {
				layerStart[layer++] = i;
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
