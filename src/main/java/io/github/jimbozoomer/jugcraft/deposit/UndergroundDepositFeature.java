package io.github.jimbozoomer.jugcraft.deposit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/** Bounded, connected bulk bodies selected by the surface biome, even underneath a cave biome. */
public record UndergroundDepositFeature(Family family, TagKey<Biome> surfaceBiomes) implements Feature {
	public static final MapCodec<UndergroundDepositFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.stringResolver(Family::id, Family::byId).fieldOf("family").forGetter(UndergroundDepositFeature::family),
			TagKey.codec(Registries.BIOME).fieldOf("surface_biomes").forGetter(UndergroundDepositFeature::surfaceBiomes)
	).apply(i, UndergroundDepositFeature::new));

	public enum Family {
		COAL(16, 48, 80, 12, 20, 6, 10, 1, 3, 64, 128),
		IRON(-48, -16, 32, 8, 14, 6, 10, 3, 6, 48, 96),
		COPPER(-16, 16, 48, 10, 18, 4, 8, 2, 5, 40, 80),
		TIN(-40, -8, 24, 8, 14, 3, 6, 2, 4, 24, 64);
		public final int minY, peakY, maxY, minLength, maxLength, minWidth, maxWidth, minHeight, maxHeight, minBlocks, maxBlocks;
		Family(int minY, int peakY, int maxY, int minLength, int maxLength, int minWidth, int maxWidth,
				int minHeight, int maxHeight, int minBlocks, int maxBlocks) {
			this.minY = minY; this.peakY = peakY; this.maxY = maxY;
			this.minLength = minLength; this.maxLength = maxLength; this.minWidth = minWidth; this.maxWidth = maxWidth;
			this.minHeight = minHeight; this.maxHeight = maxHeight; this.minBlocks = minBlocks; this.maxBlocks = maxBlocks;
		}
		public String id() { return name().toLowerCase(Locale.ROOT); }
		static Family byId(String id) { for (var value : values()) if (value.id().equals(id)) return value; return null; }
		public int centerY(RandomSource random) {
			double u = random.nextDouble(), span = maxY - minY, split = (peakY - minY) / span;
			return (int) Math.round(u < split ? minY + Math.sqrt(u * span * (peakY - minY))
					: maxY - Math.sqrt((1 - u) * span * (maxY - peakY)));
		}
	}

	@Override public MapCodec<UndergroundDepositFeature> codec() { return CODEC; }

	@Override public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		if (!level.getLevel().dimension().equals(Level.OVERWORLD)) return false;
		int surface = level.getHeight(Heightmap.Types.OCEAN_FLOOR, origin.getX(), origin.getZ());
		if (!level.getBiome(new BlockPos(origin.getX(), surface, origin.getZ())).is(surfaceBiomes)) return false;
		return placeBody(level, random, new BlockPos(origin.getX(), family.centerY(random), origin.getZ()));
	}

	/** Also used by the inspection gallery; normal placement chooses the depth first. */
	public boolean placeBody(WorldGenLevel level, RandomSource random, BlockPos center) {
		var structures = level instanceof WorldGenRegion region
				? level.getLevel().structureManager().forWorldGenRegion(region) : level.getLevel().structureManager();
		Map<BlockPos, BlockState> valid = new LinkedHashMap<>();
		Map<Long, Integer> surfaces = new HashMap<>();
		for (BlockPos pos : plan(family, random, center)) {
			if (!level.ensureCanWrite(pos)) continue;
			int surface = surfaces.computeIfAbsent(BlockPos.asLong(pos.getX(), 0, pos.getZ()),
					key -> level.getHeight(Heightmap.Types.OCEAN_FLOOR, pos.getX(), pos.getZ()));
			BlockState old = level.getBlockState(pos);
			if (surface - 1 - pos.getY() < 12 || !old.getFluidState().isEmpty() || level.getBlockEntity(pos) != null
					|| structures.getStructureWithPieceAt(pos, holder -> true).isValid()) continue;
			boolean deep = old.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
			if (!deep && !old.is(BlockTags.STONE_ORE_REPLACEABLES)) continue;
			valid.put(pos, JugcraftDeposits.BLOCKS.get((deep ? "deepslate_" : "") + family.id() + "_deposit").defaultBlockState());
		}
		// Cave/ore/structure exclusions may split a body. Keep only the largest connected component, atomically.
		Set<BlockPos> largest = new LinkedHashSet<>(), remaining = new LinkedHashSet<>(valid.keySet());
		while (!remaining.isEmpty()) {
			Set<BlockPos> component = new LinkedHashSet<>();
			ArrayDeque<BlockPos> queue = new ArrayDeque<>();
			BlockPos first = remaining.iterator().next(); remaining.remove(first); queue.add(first);
			while (!queue.isEmpty()) {
				BlockPos pos = queue.remove(); component.add(pos);
				for (Direction direction : Direction.values()) if (remaining.remove(pos.relative(direction))) queue.add(pos.relative(direction));
			}
			if (component.size() > largest.size()) largest = component;
		}
		if (largest.size() < family.minBlocks) return false;
		for (BlockPos pos : largest) level.setBlock(pos, valid.get(pos), 2);
		return true;
	}

	/** Grow a face-connected body through a seeded preference field. At most 20*10*6 candidates, no world reads. */
	public static List<BlockPos> plan(Family family, RandomSource random, BlockPos center) {
		int length = between(random, family.minLength, family.maxLength), width = between(random, family.minWidth, family.maxWidth);
		int height = between(random, family.minHeight, family.maxHeight);
		int count = Math.min(length * width * height, between(random, family.minBlocks, family.maxBlocks));
		boolean rotate = random.nextBoolean();
		double phase = random.nextDouble() * Math.PI * 2, tilt = (random.nextBoolean() ? 1 : -1) * (height - 1) / 2.0;
		Map<BlockPos, Double> field = new HashMap<>();
		for (int x = 0; x < length; x++) for (int z = 0; z < width; z++) for (int y = 0; y < height; y++) {
			double nx = (2.0 * x + 1 - length) / length, nz = (2.0 * z + 1 - width) / width;
			double spineZ = Math.sin(nx * 3 + phase) * (family == Family.COAL ? 0.10 : 0.30);
			double spineY = (height - 1) / 2.0 + nx * tilt;
			double dy = (y - spineY) / Math.max(0.5, height / 2.0);
			double score = nx * nx * 0.3 + Math.pow(nz - spineZ, 2) + dy * dy;
			if (family == Family.IRON) score -= 0.45 * Math.cos(nx * 7 + phase); // linked lobes
			if (family == Family.COPPER) score = Math.min(score, nx * nx * 0.4 + Math.pow(nz + nx * 0.65, 2) + dy * dy);
			if (family == Family.TIN) score += Math.sin(nx * 8 + phase) * 0.22; // pockets
			field.put(new BlockPos(x - length / 2, y - height / 2, z - width / 2), score + random.nextDouble() * 0.55);
		}
		Comparator<BlockPos> order = Comparator.comparingDouble((BlockPos p) -> field.get(p)).thenComparingLong(BlockPos::asLong);
		PriorityQueue<BlockPos> frontier = new PriorityQueue<>(order);
		Set<BlockPos> queued = new HashSet<>();
		frontier.add(BlockPos.ZERO); queued.add(BlockPos.ZERO);
		List<BlockPos> result = new ArrayList<>();
		while (result.size() < count && !frontier.isEmpty()) {
			BlockPos pos = frontier.remove();
			result.add(center.offset(rotate ? pos.getZ() : pos.getX(), pos.getY(), rotate ? pos.getX() : pos.getZ()));
			for (Direction direction : Direction.values()) {
				BlockPos next = pos.relative(direction);
				if (field.containsKey(next) && queued.add(next)) frontier.add(next);
			}
		}
		return result;
	}

	private static int between(RandomSource random, int min, int max) { return min + random.nextInt(max - min + 1); }
}
