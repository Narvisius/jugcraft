package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;

/** Bounded face-connected forks: high parasols, low tamarisk sprays, and upturned Joshua arms. */
public final class DryCountryTrunkPlacer extends TrunkPlacer {
	private static final Codec<String> SHAPE = Codec.STRING.validate(s -> Set.of("umbrella", "tamarisk", "joshua", "small", "dragonblood", "end_shrub").contains(s)
			? DataResult.success(s) : DataResult.error(() -> "Unknown dry-country trunk shape: " + s));
	public static final MapCodec<DryCountryTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(i -> trunkPlacerParts(i)
			.and(SHAPE.fieldOf("shape").forGetter(p -> p.shape)).apply(i, DryCountryTrunkPlacer::new));
	public static final TrunkPlacerType<DryCountryTrunkPlacer> TYPE = new TrunkPlacerType<>(CODEC);
	private final String shape;
	public DryCountryTrunkPlacer(int height, int randomA, int randomB, String shape) {
		super(height, randomA, randomB); this.shape = shape;
	}
	@Override protected TrunkPlacerType<?> type() { return TYPE; }
	@Override public List<FoliagePlacer.FoliageAttachment> placeTrunk(WorldGenLevel level, BiConsumer<BlockPos, BlockState> setter,
			RandomSource random, int height, BlockPos origin, TreeFeature tree) {
		var crowns = new ArrayList<FoliagePlacer.FoliageAttachment>();
		boolean umbrella = shape.equals("umbrella"), joshua = shape.equals("joshua") || shape.equals("dragonblood");
		if (!shape.equals("dragonblood") && !shape.equals("end_shrub")) placeBelowTrunkBlock(level, setter, random, origin.below(), tree);
		int trunkHeight = shape.equals("dragonblood") ? Math.max(2, height - 3) + 1 : joshua ? 2 : height;
		BlockPos top = null;
		for (int y = 0; y < trunkHeight; y++) {
			BlockPos next = origin.above(y);
			if (!placeLog(level, setter, random, next, tree)) break;
			top = next;
		}
		if (top == null) return crowns;
		if (shape.equals("end_shrub")) { crowns.add(new FoliagePlacer.FoliageAttachment(top.above(), 0, false)); return crowns; }
		if (!joshua) crowns.add(new FoliagePlacer.FoliageAttachment(top.above(), 0, false));
		Direction first = Direction.Plane.HORIZONTAL.getRandomDirection(random);
		int count = 2 + random.nextInt(2);
		for (int branch = 0; branch < count; branch++) {
			Direction side = first; for (int turn = 0; turn < branch; turn++) side = side.getClockWise();
			int baseY = shape.equals("dragonblood") ? Math.max(2, height - 3) : umbrella ? Math.max(0, height - 3) : 1;
			if (origin.getY() + baseY > top.getY()) continue;
			BlockPos end = origin.above(baseY);
			int length = umbrella || shape.equals("small") ? 1 : branch == 0 ? 2 : 1 + random.nextInt(2);
			boolean extended = false;
			for (int step = 0; step < length; step++) {
				BlockPos next = end.relative(side); final var axis = side.getAxis();
				if (!placeLog(level, setter, random, next, tree, s -> s.setValue(RotatedPillarBlock.AXIS, axis))) break;
				end = next; extended = true;
			}
			if (!extended) continue;
			int targetY = origin.getY() + height - 1 - (branch == 0 ? 0 : random.nextInt(2));
			while (end.getY() < targetY) {
				if (!placeLog(level, setter, random, end.above(), tree)) break;
				end = end.above();
			}
			crowns.add(new FoliagePlacer.FoliageAttachment(end.above(), 0, false));
		}
		if (crowns.isEmpty()) crowns.add(new FoliagePlacer.FoliageAttachment(top.above(), 0, false));
		return crowns;
	}
}
