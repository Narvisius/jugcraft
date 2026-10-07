package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * The pictures spooky fireworks burst into ({@link SpookyRocket}): each a little pixel picture, every pixel a spark of
 * its colour, a dot ({@code .}) none. Each client draws the burst facing the player watching it, so the picture reads
 * the right way round from wherever it is seen. Dark details (eyes, sockets, a mouth) are holes, since dark sparks
 * would vanish against the night sky.
 */
public enum FireworkShape {
	BAT("bat", 0x9B59D0, Map.of('w', 0x7E44C0, 'b', 0xB07AE8, 'e', 0xFF4A1C),
			"w.............w",
			"ww...........ww",
			"www..b...b..www",
			"wwww.bbbbb.wwww",
			"wwwwwbebebwwwww",
			"wwwwwbbbbbwwwww",
			"wwwwwbbbbbwwwww",
			".www.wbbbw.www.",
			"..w...b.b...w.."),
	PUMPKIN("pumpkin", 0xFF8A1C, Map.of('o', 0xFF8A1C, 'r', 0xC85A10, 'g', 0x58C83C, 'y', 0xFFE45A),
			".....gg......",
			"......g......",
			"...ooorooo...",
			".ooooorooooo.",
			"ooyyoorooyyoo",
			"oyyyyoroyyyyo",
			"ooooooyoooooo",
			"oooooyyyooooo",
			"oyoyyyyyyyoyo",
			"ooyyyoyoyyyoo",
			".ooooorooooo.",
			"...ooooooo..."),
	GHOST("ghost", 0xF2F4FF, Map.of('w', 0xF2F4FF, 'b', 0xA8C8FF),
			"...wwwww...",
			"..wwwwwww..",
			".wwwwwwwww.",
			".ww..w..ww.",
			"www..w..www",
			"wwwwwwwwwww",
			"wwwww.wwwww",
			"wwww...wwww",
			"wwwww.wwwww",
			"wwwwwwwwwww",
			"bwwwwwwwwwb",
			"bbw.bwb.wbb",
			".b...b...b."),
	SKULL("skull", 0xEDE3C4, Map.of('w', 0xEDE3C4, 'g', 0xB8AC8C),
			"...wwwww...",
			".wwwwwwwww.",
			"wwwwwwwwwww",
			"wwwwwwwwwww",
			"ww...w...ww",
			"ww...w...ww",
			"ww...w...ww",
			"wwwww.wwwww",
			".wwww.wwww.",
			"..gwwwwwg..",
			"..w.w.w.w..",
			"..wwwwwww..");

	private final String id;
	private final int colour;
	private final Map<Character, Integer> palette;
	private final String[] rows;

	FireworkShape(String id, int colour, Map<Character, Integer> palette, String... rows) {
		this.id = id;
		this.colour = colour;
		this.palette = palette;
		this.rows = rows;
	}

	public String id() {
		return id;
	}

	/** The firework item that bursts into this picture. */
	public String item() {
		return id + "_firework";
	}

	/** The picture's main colour (the rocket's cap, and the stray sparks round the picture). */
	public int colour() {
		return colour;
	}

	public int width() {
		return rows[0].length();
	}

	public int height() {
		return rows.length;
	}

	/** The colour of the spark at ({@code column}, {@code row}), from the top left, or -1 where there is none. */
	public int colourAt(int column, int row) {
		if (row < 0 || row >= rows.length || column < 0 || column >= rows[row].length()) {
			return -1;
		}
		Integer found = palette.get(rows[row].charAt(column));
		return found == null ? -1 : found;
	}

	/** How many sparks make up the picture. */
	public int sparks() {
		int count = 0;
		for (int row = 0; row < height(); row++) {
			for (int column = 0; column < width(); column++) {
				count += colourAt(column, row) >= 0 ? 1 : 0;
			}
		}
		return count;
	}

	/** Whether every row is as wide as the first and every pixel is a known colour or a dot. */
	public boolean wellFormed() {
		for (String row : rows) {
			if (row.length() != width()) {
				return false;
			}
			for (char pixel : row.toCharArray()) {
				if (pixel != '.' && !palette.containsKey(pixel)) {
					return false;
				}
			}
		}
		return true;
	}

	public static @Nullable FireworkShape byOrdinal(int ordinal) {
		FireworkShape[] all = values();
		return ordinal >= 0 && ordinal < all.length ? all[ordinal] : null;
	}
}
