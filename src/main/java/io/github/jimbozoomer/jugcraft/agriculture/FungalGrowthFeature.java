package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/** Small, bounded growth plans: reject obstacles before placing any part of a fungus or bush. */
public record FungalGrowthFeature(String shape) implements Feature {
	public static final Set<String> SHAPES = Set.of("glowcap_dome", "glowcap_parasol", "blight", "marrow", "bramble");
	public static final MapCodec<FungalGrowthFeature> CODEC = Codec.STRING.validate(s -> SHAPES.contains(s)
			? DataResult.success(s) : DataResult.error(() -> "Unknown fungal shape: " + s))
			.fieldOf("shape").xmap(FungalGrowthFeature::new, FungalGrowthFeature::shape);
	@Override public MapCodec<FungalGrowthFeature> codec() { return CODEC; }
	public static boolean soil(String shape, BlockState floor) {
		if (shape.startsWith("glowcap")) return floor.is(Blocks.MYCELIUM) || floor.is(Blocks.MOSS_BLOCK)
				|| floor.is(Blocks.MUD) || floor.is(BlockTags.NYLIUM);
		if (shape.equals("blight")) return floor.is(Blocks.SOUL_SOIL) || floor.is(Blocks.BLACKSTONE);
		if (shape.equals("marrow")) return floor.is(Blocks.NETHER_WART_BLOCK);
		return floor.is(BlockTags.NYLIUM) || floor.is(Blocks.SOUL_SOIL) || floor.is(Blocks.SOUL_SAND);
	}
	@Override public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		if (!soil(shape, level.getBlockState(origin.below()))) return false;
		boolean bush = shape.equals("bramble"), glow = shape.startsWith("glowcap"), marrow = shape.equals("marrow");
		int height = bush ? 2 + random.nextInt(2) : glow ? 6 + random.nextInt(5) : 4 + random.nextInt(10);
		int radius = bush ? 2 : glow ? 2 + random.nextInt(2) : 2;
		BlockState stem = (bush ? Blocks.CRIMSON_STEM : glow ? Blocks.MUSHROOM_STEM : marrow ? Blocks.BONE_BLOCK
				: JugcraftAgriculture.block("dead_log")).defaultBlockState();
		BlockState cap = (bush ? JugcraftAgriculture.block("bramble_bush_leaves") : glow ? JugcraftAgriculture.block("glowcap_block")
				: marrow ? Blocks.NETHER_WART_BLOCK : JugcraftAgriculture.block("blight_wart_block")).defaultBlockState();
		Map<BlockPos, BlockState> plan = new LinkedHashMap<>();
		int layers = bush ? height : shape.equals("glowcap_parasol") ? 1 : 3;
		for (int layer = 0; layer < layers; layer++) {
			int r = layer == layers - 1 && layers > 1 ? Math.max(1, radius - 1) : radius;
			for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
				if (Math.abs(x) == r && Math.abs(z) == r) continue;
				// A few ragged lower edges; retain the solid upper layer that joins the whole cap.
				if (!glow && !bush && layer == 0 && Math.max(Math.abs(x), Math.abs(z)) == r && random.nextInt(4) == 0) continue;
				BlockPos at = origin.offset(x, height - layers + layer, z);
				if (bush && at.equals(origin)) continue;
				BlockState state = bush ? cap.setValue(LeavesBlock.DISTANCE, Math.abs(x) + Math.abs(z) + at.getY() - origin.getY()) : cap;
				if (marrow && layer == 0 && Math.abs(x) + Math.abs(z) == 2 && random.nextInt(5) == 0) state = Blocks.SHROOMLIGHT.defaultBlockState();
				plan.put(at, state);
			}
		}
		for (int y = 0; y < (bush ? 1 : height - 1); y++) plan.put(origin.above(y), stem);
		for (var entry : plan.entrySet()) {
			var at = entry.getKey();
			if (at.getY() < level.getMinY() || at.getY() >= level.getMaxY() || !level.ensureCanWrite(at)) return false;
			var existing = level.getBlockState(at);
			// Existing ground decoration must not veto every bush: Netherbrush plants small brambles
			// during its earlier ground pass. Preserve trees, fluids and solid terrain/buildings.
			boolean groundCover = existing.getFluidState().isEmpty() && !existing.is(BlockTags.LEAVES)
					&& (existing.is(BlockTags.REPLACEABLE_BY_TREES) || existing.getBlock() instanceof FloorPlantBlock
					|| existing.is(Blocks.WARPED_FUNGUS) || existing.is(Blocks.CRIMSON_FUNGUS)
					|| existing.is(Blocks.BROWN_MUSHROOM) || existing.is(Blocks.RED_MUSHROOM));
			if (!existing.isAir() && !groundCover) return false;
		}
		plan.forEach((at, state) -> {
			if (state.getBlock() instanceof HugeMushroomBlock) for (Direction side : Direction.values()) {
				var neighbor = plan.get(at.relative(side));
				state = state.setValue(net.minecraft.world.level.block.PipeBlock.PROPERTY_BY_DIRECTION.get(side), neighbor == null || !neighbor.is(state.getBlock()));
			}
			level.setBlock(at, state, Block.UPDATE_CLIENTS);
		});
		return true;
	}
}
