package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;

/**
 * A giant pumpkin carried as an item (the {@code jugcraft:giant_pumpkin} component): how wide it is, the growth
 * points and weight it had, which pumpkin it is (so the Harvest Scale knows it again), whether a torch is inside, what
 * is carved into its sides and who carved it. Breaking a giant pumpkin drops it whole with this; placing the item puts
 * the same pumpkin back. A torch inside may be a soul torch ({@code soul}).
 */
public record GiantPumpkinData(int size, int points, int weight, Optional<UUID> id, boolean lit, Map<Direction, int[]> faces,
		Optional<UUID> carverId, String carverName, boolean soul) {
	private static final Codec<int[]> FACE_CODEC = Codec.INT_STREAM.xmap(stream -> stream.toArray(), Arrays::stream);
	public static final Codec<GiantPumpkinData> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.intRange(1, GiantPumpkinBlock.MAX_SIZE).fieldOf("size").forGetter(GiantPumpkinData::size),
			Codec.intRange(0, GiantPumpkinBlockEntity.GROW_TO_THREE).optionalFieldOf("points", 0).forGetter(GiantPumpkinData::points),
			Codec.intRange(0, GiantPumpkinBlockEntity.MAX_WEIGHT).optionalFieldOf("weight", 0).forGetter(GiantPumpkinData::weight),
			UUIDUtil.CODEC.optionalFieldOf("id").forGetter(GiantPumpkinData::id),
			Codec.BOOL.optionalFieldOf("lit", false).forGetter(GiantPumpkinData::lit),
			Codec.unboundedMap(Direction.CODEC, FACE_CODEC).optionalFieldOf("carving", Map.of()).forGetter(GiantPumpkinData::faces),
			UUIDUtil.CODEC.optionalFieldOf("carved_by").forGetter(GiantPumpkinData::carverId),
			Codec.STRING.optionalFieldOf("carved_by_name", "").forGetter(GiantPumpkinData::carverName),
			Codec.BOOL.optionalFieldOf("soul", false).forGetter(GiantPumpkinData::soul)).apply(i, GiantPumpkinData::new));

	/** A full-grown, uncarved giant pumpkin at its starting weight (the item as the creative menu gives it). */
	public static final GiantPumpkinData FULL_GROWN = new GiantPumpkinData(GiantPumpkinBlock.MAX_SIZE, 0, GiantPumpkinBlockEntity.START_WEIGHT,
			Optional.empty(), false, Map.of(), Optional.empty(), "", false);

	public GiantPumpkinData {
		Map<Direction, int[]> valid = new EnumMap<>(Direction.class);
		faces.forEach((side, face) -> {
			if (side.getAxis().isHorizontal() && CarvingFace.isValid(face, GiantPumpkinBlockEntity.FACE_SIZE) && !CarvingFace.isBlank(face)) {
				valid.put(side, face.clone());
			}
		});
		faces = Map.copyOf(valid);
	}

	public boolean carved() {
		return !faces.isEmpty();
	}

	// Faces are arrays: compare what is in them.
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof GiantPumpkinData that) || size != that.size || points != that.points || weight != that.weight || lit != that.lit
				|| soul != that.soul
				|| !id.equals(that.id) || !carverId.equals(that.carverId) || !carverName.equals(that.carverName)
				|| !faces.keySet().equals(that.faces.keySet())) {
			return false;
		}
		for (Map.Entry<Direction, int[]> entry : faces.entrySet()) {
			if (!Arrays.equals(entry.getValue(), that.faces.get(entry.getKey()))) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hash = Objects.hash(size, points, weight, id, lit, carverId, carverName, soul);
		for (Map.Entry<Direction, int[]> entry : faces.entrySet()) {
			hash = 31 * hash + entry.getKey().hashCode() * 17 + Arrays.hashCode(entry.getValue());
		}
		return hash;
	}
}
