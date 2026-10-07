package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a pumpkin boat keeps from the giant pumpkin it was hollowed out of: its weight in kilograms (lighter
 * boats are faster), whether a torch is inside, and, for a barge, the four 48x48 sides as they were carved
 * (index as {@link net.minecraft.core.Direction#get2DDataValue()}, relative to the boat). It rides on the boat
 * item (component {@code jugcraft:pumpkin_boat}) and on the boat entity, so nothing is lost when a boat is
 * broken and placed again.
 *
 * <p>Immutable; equal data are equal objects.
 */
public final class PumpkinBoatData {
	/** Ints in one 48x48 face. */
	private static final int FACE_INTS = CarvingFace.ints(GiantPumpkinBlockEntity.FACE_SIZE);
	private static final int FACES = 4;

	private final int weight;
	private final boolean lit;
	/** The four faces one after another, or empty when nothing is carved. */
	private final int[] faces;
	private final int hash;

	private PumpkinBoatData(int weight, boolean lit, int[] faces) {
		this.weight = weight;
		this.lit = lit;
		this.faces = faces;
		this.hash = (Arrays.hashCode(faces) * 31 + weight) * 2 + (lit ? 1 : 0);
	}

	/** An uncarved boat of this weight. */
	public static PumpkinBoatData plain(int weight) {
		return new PumpkinBoatData(weight, false, new int[0]);
	}

	/** A boat with these four 48x48 faces (null or blank faces stay skin); with nothing carved it keeps no faces. */
	public static PumpkinBoatData of(int weight, boolean lit, int[][] sides) {
		int[] all = new int[FACE_INTS * FACES];
		boolean carved = false;
		for (int i = 0; i < FACES; i++) {
			if (sides[i] != null && !CarvingFace.isBlank(sides[i])) {
				System.arraycopy(sides[i], 0, all, i * FACE_INTS, FACE_INTS);
				carved = true;
			}
		}
		return new PumpkinBoatData(weight, lit && carved, carved ? all : new int[0]);
	}

	private static DataResult<PumpkinBoatData> checked(int weight, boolean lit, int[] faces) {
		if (weight < 0 || weight > GiantPumpkinBlockEntity.MAX_WEIGHT) {
			return DataResult.error(() -> "A pumpkin boat weighs 0 to " + GiantPumpkinBlockEntity.MAX_WEIGHT + " kg, not " + weight);
		}
		if (faces.length != 0 && faces.length != FACE_INTS * FACES) {
			return DataResult.error(() -> "A pumpkin boat has no carving or four 48x48 faces");
		}
		for (int i = 0; i < faces.length / FACE_INTS; i++) {
			if (!CarvingFace.isValid(Arrays.copyOfRange(faces, i * FACE_INTS, (i + 1) * FACE_INTS), GiantPumpkinBlockEntity.FACE_SIZE)) {
				return DataResult.error(() -> "Pumpkin boat carving pixels are 0 (skin), 1 (shaved) or 2 (cut)");
			}
		}
		return DataResult.success(new PumpkinBoatData(weight, lit && faces.length != 0, faces.clone()));
	}

	private record Raw(int weight, boolean lit, Optional<int[]> faces) {
	}

	private static final Codec<Raw> RAW = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.fieldOf("weight").forGetter(Raw::weight),
			Codec.BOOL.optionalFieldOf("lit", false).forGetter(Raw::lit),
			Codec.INT_STREAM.xmap(stream -> stream.toArray(), Arrays::stream).optionalFieldOf("carving").forGetter(Raw::faces))
			.apply(i, Raw::new));

	/** Saved as {weight, lit, carving: the four faces' ints}. */
	public static final Codec<PumpkinBoatData> CODEC = RAW.comapFlatMap(
			raw -> checked(raw.weight(), raw.lit(), raw.faces().orElse(new int[0])),
			data -> new Raw(data.weight, data.lit, data.faces.length == 0 ? Optional.empty() : Optional.of(data.faces)));

	/** Sent as weight, lit, then whether carved and, if so, exactly four faces: there is no length to forge. */
	public static final StreamCodec<ByteBuf, PumpkinBoatData> STREAM_CODEC = StreamCodec.of(
			(buffer, data) -> {
				buffer.writeShort(data.weight);
				buffer.writeBoolean(data.lit);
				buffer.writeBoolean(data.faces.length != 0);
				for (int row : data.faces) {
					buffer.writeInt(row);
				}
			},
			buffer -> {
				int weight = buffer.readShort();
				boolean lit = buffer.readBoolean();
				int[] faces = new int[buffer.readBoolean() ? FACE_INTS * FACES : 0];
				for (int i = 0; i < faces.length; i++) {
					faces[i] = buffer.readInt();
				}
				return checked(weight, lit, faces).getOrThrow();
			});

	public int weight() {
		return weight;
	}

	public boolean lit() {
		return lit;
	}

	public boolean carved() {
		return faces.length != 0;
	}

	/** One side's 48x48 face (all skin if nothing is carved). */
	public int[] face(int index) {
		return faces.length == 0 ? new int[FACE_INTS] : Arrays.copyOfRange(faces, index * FACE_INTS, (index + 1) * FACE_INTS);
	}

	/** The same boat with its torch put in or taken out (only a carved boat can be lit). */
	public PumpkinBoatData withLit(boolean newLit) {
		return new PumpkinBoatData(weight, newLit && carved(), faces);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof PumpkinBoatData data && data.weight == weight && data.lit == lit && Arrays.equals(data.faces, faces);
	}

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public String toString() {
		return "PumpkinBoatData[weight=" + weight + ", lit=" + lit + ", carved=" + carved() + "]";
	}
}
