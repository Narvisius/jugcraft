package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * A corn maze's plan: a perfect maze (one way through, every part reachable) of square cells, carved by a
 * depth-first walk from a seed so the same seed always gives the same maze. The plan is a grid {@link #side} blocks
 * square: walls between the cells, the entrance in the middle of its near edge (where the Corn Maze Gate stands) and
 * the exit in the middle of its far edge (where the finish post goes). Sizes are {@link #CELLS} cells across.
 */
public final class CornMaze {
	/** Cells across for each size: tiny, small, medium, large (7, 11, 15 and 19 blocks). */
	public static final int[] CELLS = {3, 5, 7, 9};
	public static final String[] SIZES = {"tiny", "small", "medium", "large"};

	private final int cells;
	private final boolean[][] wall;

	private CornMaze(int cells, boolean[][] wall) {
		this.cells = cells;
		this.wall = wall;
	}

	/** Carves a maze {@code cells} cells across from {@code seed}. */
	public static CornMaze carve(int cells, long seed) {
		int side = 2 * cells + 1;
		boolean[][] wall = new boolean[side][side];
		for (boolean[] row : wall) {
			java.util.Arrays.fill(row, true);
		}
		Random random = new Random(seed);
		boolean[][] visited = new boolean[cells][cells];
		Deque<int[]> path = new ArrayDeque<>();
		int[] start = {cells / 2, 0};
		visited[start[0]][start[1]] = true;
		wall[2 * start[0] + 1][2 * start[1] + 1] = false;
		path.push(start);
		int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		while (!path.isEmpty()) {
			int[] here = path.peek();
			List<int[]> next = new ArrayList<>();
			for (int[] step : steps) {
				int x = here[0] + step[0];
				int y = here[1] + step[1];
				if (x >= 0 && y >= 0 && x < cells && y < cells && !visited[x][y]) {
					next.add(new int[] {x, y});
				}
			}
			if (next.isEmpty()) {
				path.pop();
				continue;
			}
			Collections.shuffle(next, random);
			int[] to = next.get(0);
			visited[to[0]][to[1]] = true;
			wall[here[0] + to[0] + 1][here[1] + to[1] + 1] = false;
			wall[2 * to[0] + 1][2 * to[1] + 1] = false;
			path.push(to);
		}
		wall[cells][0] = false;
		wall[cells][side - 1] = false;
		return new CornMaze(cells, wall);
	}

	public int cells() {
		return cells;
	}

	/** How many blocks across (and deep) the maze is. */
	public int side() {
		return 2 * cells + 1;
	}

	/** Whether a wall stands at grid ({@code across}, {@code deep}); outside the grid counts as open. */
	public boolean wall(int across, int deep) {
		return across >= 0 && deep >= 0 && across < side() && deep < side() && wall[across][deep];
	}

	/** The world position of grid ({@code across}, {@code deep}) for a gate at {@code gate} facing into the maze. */
	public BlockPos at(BlockPos gate, Direction facing, int across, int deep) {
		return gate.relative(facing, deep).relative(facing.getClockWise(), across - cells);
	}

	/** Where the finish post stands: the exit, in the middle of the far edge. */
	public BlockPos finish(BlockPos gate, Direction facing) {
		return at(gate, facing, cells, side() - 1);
	}

	/** The fewest steps from the entrance to the exit through open squares, or -1 if there is no way. */
	public int shortest() {
		int side = side();
		int[][] distance = new int[side][side];
		for (int[] row : distance) {
			java.util.Arrays.fill(row, -1);
		}
		Deque<int[]> queue = new ArrayDeque<>();
		distance[cells][0] = 0;
		queue.add(new int[] {cells, 0});
		int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		while (!queue.isEmpty()) {
			int[] here = queue.poll();
			if (here[0] == cells && here[1] == side - 1) {
				return distance[here[0]][here[1]];
			}
			for (int[] step : steps) {
				int x = here[0] + step[0];
				int y = here[1] + step[1];
				if (x >= 0 && y >= 0 && x < side && y < side && !wall[x][y] && distance[x][y] < 0) {
					distance[x][y] = distance[here[0]][here[1]] + 1;
					queue.add(new int[] {x, y});
				}
			}
		}
		return -1;
	}

	/** Every wall square, as grid ({@code across}, {@code deep}) pairs. */
	public List<int[]> walls() {
		List<int[]> all = new ArrayList<>();
		for (int across = 0; across < side(); across++) {
			for (int deep = 0; deep < side(); deep++) {
				if (wall[across][deep]) {
					all.add(new int[] {across, deep});
				}
			}
		}
		return all;
	}
}
