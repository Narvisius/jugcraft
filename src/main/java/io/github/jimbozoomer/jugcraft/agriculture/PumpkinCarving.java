package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.StreamCodec;

/**
 * What has been carved into a pumpkin: four 16x16 faces, one per side, each pixel {@link #SKIN},
 * {@link #SHAVED} (the skin peeled off, so light glows through) or {@link #CUT} (a hole).
 *
 * <p>Faces are numbered from the pumpkin's front clockwise seen from above ({@link #faceIndex}), so a
 * carved pumpkin keeps its design when it is picked up and placed facing another way. A pixel is
 * {@code (x, y)} as someone standing in front of that face sees it: x from left to right, y from the
 * top. Each face row is one int of sixteen 2-bit pixels, pixel x at bits {@code 2x..2x+1}.
 *
 * <p>Immutable; equal designs are equal objects, so clients can share one texture between them.
 */
public final class PumpkinCarving {
	public static final int SIZE = 16;
	public static final int FACES = 4;
	public static final int SKIN = 0;
	public static final int SHAVED = 1;
	public static final int CUT = 2;
	public static final PumpkinCarving BLANK = new PumpkinCarving(new int[FACES * SIZE]);

	/** Saved as an int array of the 64 rows (front face first, top row first). */
	public static final Codec<PumpkinCarving> CODEC = Codec.INT_STREAM.comapFlatMap(
			stream -> of(stream.toArray()), carving -> Arrays.stream(carving.rows));
	/** Sent as exactly 64 ints: there is no length to forge. */
	public static final StreamCodec<ByteBuf, PumpkinCarving> STREAM_CODEC = StreamCodec.of(
			(buffer, carving) -> {
				for (int row : carving.rows) {
					buffer.writeInt(row);
				}
			},
			buffer -> {
				int[] rows = new int[FACES * SIZE];
				for (int i = 0; i < rows.length; i++) {
					rows[i] = buffer.readInt();
				}
				return of(rows).getOrThrow();
			});
	/** One face: exactly 16 rows. */
	public static final StreamCodec<ByteBuf, int[]> FACE_STREAM_CODEC = StreamCodec.of(
			(buffer, face) -> {
				for (int row : face) {
					buffer.writeInt(row);
				}
			},
			buffer -> {
				int[] face = new int[SIZE];
				for (int i = 0; i < SIZE; i++) {
					face[i] = buffer.readInt();
				}
				return face;
			});

	private final int[] rows;
	private final int hash;

	private PumpkinCarving(int[] rows) {
		this.rows = rows;
		this.hash = Arrays.hashCode(rows);
	}

	/** A design from 64 rows, or an error if a row holds a depth deeper than {@link #CUT}. */
	public static DataResult<PumpkinCarving> of(int[] rows) {
		if (rows.length != FACES * SIZE) {
			return DataResult.error(() -> "A pumpkin carving has " + FACES * SIZE + " rows, not " + rows.length);
		}
		for (int row : rows) {
			if (!isValidRow(row)) {
				return DataResult.error(() -> "Pumpkin carving pixels are 0 (skin), 1 (shaved) or 2 (cut)");
			}
		}
		return DataResult.success(Arrays.equals(rows, BLANK.rows) ? BLANK : new PumpkinCarving(rows.clone()));
	}

	/** Whether every pixel of a row is skin, shaved or cut (the fourth 2-bit value means nothing). */
	public static boolean isValidRow(int row) {
		for (int x = 0; x < SIZE; x++) {
			if (pixel(row, x) > CUT) {
				return false;
			}
		}
		return true;
	}

	/** Whether a face is 16 valid rows. */
	public static boolean isValidFace(int[] face) {
		if (face.length != SIZE) {
			return false;
		}
		for (int row : face) {
			if (!isValidRow(row)) {
				return false;
			}
		}
		return true;
	}

	public static int pixel(int row, int x) {
		return (row >>> (2 * x)) & 3;
	}

	public static int withPixel(int row, int x, int depth) {
		return (row & ~(3 << (2 * x))) | (depth << (2 * x));
	}

	/** The face number of {@code side} on a pumpkin whose front faces {@code facing}: 0 front, then clockwise. */
	public static int faceIndex(Direction facing, Direction side) {
		int index = 0;
		Direction at = facing;
		while (at != side && index < FACES) {
			at = at.getClockWise();
			index++;
		}
		return index;
	}

	/** The side a face number is on, for a pumpkin whose front faces {@code facing}. */
	public static Direction side(Direction facing, int face) {
		Direction at = facing;
		for (int i = 0; i < face; i++) {
			at = at.getClockWise();
		}
		return at;
	}

	public int depth(int face, int x, int y) {
		return pixel(rows[face * SIZE + y], x);
	}

	/** A copy of one face's 16 rows. */
	public int[] face(int face) {
		return Arrays.copyOfRange(rows, face * SIZE, face * SIZE + SIZE);
	}

	/** This design with one face replaced. The face must be valid ({@link #isValidFace}). */
	public PumpkinCarving withFace(int face, int[] faceRows) {
		int[] copy = rows.clone();
		System.arraycopy(faceRows, 0, copy, face * SIZE, SIZE);
		return of(copy).getOrThrow();
	}

	public boolean isBlank() {
		return this == BLANK;
	}

	public boolean isBlank(int face) {
		for (int y = 0; y < SIZE; y++) {
			if (rows[face * SIZE + y] != 0) {
				return false;
			}
		}
		return true;
	}

	/** How many pixels of every face are at {@code depth}. */
	public int count(int depth) {
		int count = 0;
		for (int row : rows) {
			for (int x = 0; x < SIZE; x++) {
				if (pixel(row, x) == depth) {
					count++;
				}
			}
		}
		return count;
	}

	/**
	 * How brightly a candle-lit pumpkin with this design glows: nothing for an uncarved pumpkin, then 4 plus
	 * a level for every 3 holes and every 12 shaved pixels, up to 15. A vanilla-sized face (about 33 holes) gives 15.
	 */
	public int glow() {
		if (isBlank()) {
			return 0;
		}
		return Math.min(15, 4 + count(CUT) / 3 + count(SHAVED) / 12);
	}

	/** Whether {@code after} only carves deeper than {@code before}: a knife cannot put skin back. */
	public static boolean deepensOnly(int[] before, int[] after) {
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				if (pixel(after[y], x) < pixel(before[y], x)) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof PumpkinCarving carving && hash == carving.hash && Arrays.equals(rows, carving.rows);
	}

	@Override
	public int hashCode() {
		return hash;
	}
}
