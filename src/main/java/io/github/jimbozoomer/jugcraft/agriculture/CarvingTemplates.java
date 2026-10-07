package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.Arrays;
import java.util.List;

/**
 * Starter faces for the carving screen: '.' is skin, 's' shaved and '#' cut. On a server that turns off
 * free drawing ({@code carving.free_draw=false} in config/jugcraft.properties) these are the only faces
 * that can be carved. Their names are {@code carving.jugcraft.template.<id>} in the language file.
 */
public final class CarvingTemplates {
	public record Template(String id, int[] face) {
	}

	public static final List<Template> ALL = List.of(
			template("classic",
					"................",
					"................",
					"................",
					"...#........#...",
					"..###......###..",
					".#####....#####.",
					"................",
					".......##.......",
					"......####......",
					"................",
					".##############.",
					".##.########.##.",
					"..###.####.###..",
					"...##########...",
					"................",
					"................"),
			template("cat",
					"................",
					"................",
					"................",
					"..###......###..",
					".##s##....##s##.",
					"..###......###..",
					"................",
					".......##.......",
					"ssss..s##s..ssss",
					"......#..#......",
					".......##.......",
					"ssss........ssss",
					"................",
					"................",
					"................",
					"................"),
			template("ghost",
					"................",
					"......ssss......",
					"....ssssssss....",
					"...ssssssssss...",
					"..ssssssssssss..",
					"..ss##ssss##ss..",
					"..ss##ssss##ss..",
					"..ssssssssssss..",
					"..sssss##sssss..",
					"..sssss##sssss..",
					"..ssssssssssss..",
					"..ssssssssssss..",
					"..ssssssssssss..",
					"..s.ss.ss.ss.s..",
					"................",
					"................"),
			template("spooky",
					"................",
					"................",
					"................",
					".##..........##.",
					".####......####.",
					"..####....####..",
					"...##......##...",
					"................",
					"................",
					".#..#..##..#..#.",
					".##.##.##.##.##.",
					".##############.",
					"..############..",
					"...#..#..#..#...",
					"................",
					"................"));

	private CarvingTemplates() {
	}

	/** Whether a face is exactly one of the starter faces. */
	public static boolean isTemplate(int[] face) {
		return isTemplate(face, PumpkinCarving.SIZE);
	}

	/** Whether a face of {@code size} is a starter face, blown up to that size (a giant pumpkin's are 3 times as big). */
	public static boolean isTemplate(int[] face, int size) {
		for (Template template : ALL) {
			if (Arrays.equals(CarvingFace.scale(template.face(), size), face)) {
				return true;
			}
		}
		return false;
	}

	private static Template template(String id, String... lines) {
		if (lines.length != PumpkinCarving.SIZE) {
			throw new IllegalArgumentException(id + ": a face has " + PumpkinCarving.SIZE + " rows");
		}
		int[] face = new int[PumpkinCarving.SIZE];
		for (int y = 0; y < lines.length; y++) {
			if (lines[y].length() != PumpkinCarving.SIZE) {
				throw new IllegalArgumentException(id + ": row " + y + " is not " + PumpkinCarving.SIZE + " wide");
			}
			for (int x = 0; x < PumpkinCarving.SIZE; x++) {
				int depth = switch (lines[y].charAt(x)) {
					case '.' -> PumpkinCarving.SKIN;
					case 's' -> PumpkinCarving.SHAVED;
					case '#' -> PumpkinCarving.CUT;
					default -> throw new IllegalArgumentException(id + ": unknown pixel " + lines[y].charAt(x));
				};
				face[y] = PumpkinCarving.withPixel(face[y], x, depth);
			}
		}
		return new Template(id, face);
	}
}
