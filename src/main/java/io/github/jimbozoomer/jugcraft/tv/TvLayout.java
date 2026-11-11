package io.github.jimbozoomer.jugcraft.tv;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * How flat screen TV blocks side by side on one wall join into screens (docs/features/flat-screen-tv.md). Cells are
 * counted in the wall's plane: column c to the viewer's right, row r up. The blocks are taken from the bottom left: each
 * screen runs right as far as there are free blocks (at most {@value #MAX_W}), then up as far as every one of its
 * columns has a block (at most {@value #MAX_H}). A full rectangle up to 48 x 27 is one screen; a bigger one, or an odd
 * shape, is the fewest such pieces this finds. Pure, so it is checked without a world (TvLayoutCheck).
 */
public final class TvLayout {
	/** As big as a screen gets (Justin, 27 October 2026: "as tall or as wide as you want ... going up to that limit"):
	 * 48 x 27 blocks, a 16:9 wall of 1,296 blocks; the picture stays 720p. */
	public static final int MAX_W = 48, MAX_H = 27;

	/** One block of the wall: its column (to the viewer's right) and row (up). */
	public record Cell(int c, int r) {
	}

	/** A screen: its bottom-left block and its size in blocks. */
	public record Screen(int c0, int r0, int w, int h) {
		public boolean contains(Cell cell) {
			return cell.c() >= c0 && cell.c() < c0 + w && cell.r() >= r0 && cell.r() < r0 + h;
		}

		public Cell controller() {
			return new Cell(c0, r0);
		}
	}

	private TvLayout() {
	}

	/** The screens the cells make, each cell in exactly one. */
	public static List<Screen> tile(Set<Cell> cells) {
		List<Cell> order = new ArrayList<>(cells);
		order.sort(Comparator.comparingInt(Cell::r).thenComparingInt(Cell::c));
		Set<Cell> used = new HashSet<>();
		List<Screen> out = new ArrayList<>();
		for (Cell s : order) {
			if (used.contains(s)) {
				continue;
			}
			int w = 1;
			while (w < MAX_W && free(cells, used, s.c() + w, s.r())) {
				w++;
			}
			int h = 1;
			grow:
			while (h < MAX_H) {
				for (int k = 0; k < w; k++) {
					if (!free(cells, used, s.c() + k, s.r() + h)) {
						break grow;
					}
				}
				h++;
			}
			for (int dc = 0; dc < w; dc++) {
				for (int dr = 0; dr < h; dr++) {
					used.add(new Cell(s.c() + dc, s.r() + dr));
				}
			}
			out.add(new Screen(s.c(), s.r(), w, h));
		}
		return out;
	}

	private static boolean free(Set<Cell> cells, Set<Cell> used, int c, int r) {
		Cell cell = new Cell(c, r);
		return cells.contains(cell) && !used.contains(cell);
	}
}
