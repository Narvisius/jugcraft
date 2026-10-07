package io.github.jimbozoomer.jugcraft.agriculture;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * One carved face of any size that is a multiple of 16: 16x16 on a pumpkin, 48x48 on a full-grown giant
 * pumpkin ({@link #SIZES}). A face is {@code size * size / 16} ints: row {@code y} takes {@code size / 16}
 * ints, and pixel {@code x} of a row sits in int {@code x / 16} at bits {@code 2 (x % 16)}, so a 16x16 face
 * is exactly {@link PumpkinCarving}'s 16 rows. Pixels are {@link PumpkinCarving#SKIN}, {@link PumpkinCarving#SHAVED}
 * or {@link PumpkinCarving#CUT}, seen from in front of the face: x from left to right, y from the top.
 */
public final class CarvingFace {
	/** The face sizes there are: a pumpkin's side, and a giant pumpkin's 3x3 side. */
	public static final int[] SIZES = {PumpkinCarving.SIZE, GiantPumpkinBlock.MAX_SIZE * PumpkinCarving.SIZE};

	/**
	 * A size and its face, sent as the size and then exactly that many ints: a size other than {@link #SIZES}
	 * is refused before anything else is read, so there is no length to forge.
	 */
	public record Sized(int size, int[] face) {
		public static final StreamCodec<ByteBuf, Sized> STREAM_CODEC = StreamCodec.of(
				(buffer, sized) -> {
					buffer.writeByte(sized.size);
					for (int value : sized.face) {
						buffer.writeInt(value);
					}
				},
				buffer -> {
					int size = buffer.readUnsignedByte();
					if (!isSize(size)) {
						throw new IllegalArgumentException("No carving face is " + size + " pixels wide");
					}
					int[] face = new int[ints(size)];
					for (int i = 0; i < face.length; i++) {
						face[i] = buffer.readInt();
					}
					return new Sized(size, face);
				});
	}

	private CarvingFace() {
	}

	public static boolean isSize(int size) {
		for (int known : SIZES) {
			if (known == size) {
				return true;
			}
		}
		return false;
	}

	/** How many ints a face of this size takes. */
	public static int ints(int size) {
		return size * size / PumpkinCarving.SIZE;
	}

	public static int pixel(int[] face, int size, int x, int y) {
		return PumpkinCarving.pixel(face[y * (size / PumpkinCarving.SIZE) + x / PumpkinCarving.SIZE], x % PumpkinCarving.SIZE);
	}

	/** Sets one pixel in place. */
	public static void setPixel(int[] face, int size, int x, int y, int depth) {
		int index = y * (size / PumpkinCarving.SIZE) + x / PumpkinCarving.SIZE;
		face[index] = PumpkinCarving.withPixel(face[index], x % PumpkinCarving.SIZE, depth);
	}

	/** Whether {@code face} is a face of this size with every pixel skin, shaved or cut. */
	public static boolean isValid(int[] face, int size) {
		if (!isSize(size) || face.length != ints(size)) {
			return false;
		}
		for (int value : face) {
			if (!PumpkinCarving.isValidRow(value)) {
				return false;
			}
		}
		return true;
	}

	/** Whether {@code after} only carves deeper than {@code before}: a knife cannot put skin back. */
	public static boolean deepensOnly(int[] before, int[] after, int size) {
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				if (pixel(after, size, x, y) < pixel(before, size, x, y)) {
					return false;
				}
			}
		}
		return true;
	}

	public static boolean isBlank(int[] face) {
		for (int value : face) {
			if (value != 0) {
				return false;
			}
		}
		return true;
	}

	/** How many pixels of a face are at {@code depth}. */
	public static int count(int[] face, int size, int depth) {
		int count = 0;
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				if (pixel(face, size, x, y) == depth) {
					count++;
				}
			}
		}
		return count;
	}

	/** A 16x16 face blown up to {@code size}: each pixel becomes a square of {@code size / 16} pixels. */
	public static int[] scale(int[] face16, int size) {
		int factor = size / PumpkinCarving.SIZE;
		if (factor == 1) {
			return face16.clone();
		}
		int[] out = new int[ints(size)];
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				setPixel(out, size, x, y, PumpkinCarving.pixel(face16[y / factor], x / factor));
			}
		}
		return out;
	}
}
