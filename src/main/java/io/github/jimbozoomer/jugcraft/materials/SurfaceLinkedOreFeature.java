package io.github.jimbozoomer.jugcraft.materials;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Optional;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.OreFeature;

/** A finite native ore pocket selected by its surface biome, with an optional, linked surface outcrop. */
public record SurfaceLinkedOreFeature(OreFeature ore, TagKey<Biome> surfaceBiomes, BlockState outcrop,
                                     int outcropChance, Optional<TagKey<Biome>> outcropBiomes, int outcropMax) implements Feature {
	public static final MapCodec<SurfaceLinkedOreFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			OreFeature.CODEC.codec().fieldOf("ore").forGetter(SurfaceLinkedOreFeature::ore),
			TagKey.codec(Registries.BIOME).fieldOf("surface_biomes").forGetter(SurfaceLinkedOreFeature::surfaceBiomes),
			BlockState.CODEC.fieldOf("outcrop").forGetter(SurfaceLinkedOreFeature::outcrop),
			Codec.intRange(0, 1024).fieldOf("outcrop_chance").forGetter(SurfaceLinkedOreFeature::outcropChance),
			TagKey.codec(Registries.BIOME).optionalFieldOf("outcrop_biomes").forGetter(SurfaceLinkedOreFeature::outcropBiomes),
            Codec.intRange(3, 8).optionalFieldOf("outcrop_max", 8).forGetter(SurfaceLinkedOreFeature::outcropMax)
	).apply(i, SurfaceLinkedOreFeature::new));

	/** Existing mineral definitions use the same biome policy above and below ground. */
	public SurfaceLinkedOreFeature(OreFeature ore, TagKey<Biome> surfaceBiomes, BlockState outcrop, int outcropChance) {
		this(ore, surfaceBiomes, outcrop, outcropChance, Optional.empty(), 8);
	}

    public SurfaceLinkedOreFeature(OreFeature ore, TagKey<Biome> surfaceBiomes, BlockState outcrop,
            int outcropChance, Optional<TagKey<Biome>> outcropBiomes) {
        this(ore, surfaceBiomes, outcrop, outcropChance, outcropBiomes, 8);
    }

	@Override public MapCodec<SurfaceLinkedOreFeature> codec() { return CODEC; }

	@Override public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		// Biome modifications can also reach other dimensions reusing Overworld biomes. Stay in the Overworld.
		if (!level.getLevel().dimension().equals(Level.OVERWORLD)) return false;
		int surfaceY = level.getHeight(Heightmap.Types.OCEAN_FLOOR, origin.getX(), origin.getZ());
		if (!level.getBiome(new BlockPos(origin.getX(), surfaceY, origin.getZ())).is(surfaceBiomes)) return false;
		if (!ore.place(level, generator, random, origin)) return false;
		if (outcropChance > 0 && random.nextInt(outcropChance) == 0
				&& level.getBiome(new BlockPos(origin.getX(), surfaceY, origin.getZ())).is(outcropBiomes.orElse(surfaceBiomes))) {
			placeOutcrop(level, random, origin);
		}
		return true;
	}

	private void placeOutcrop(WorldGenLevel level, RandomSource random, BlockPos origin) {
		var structures = level instanceof WorldGenRegion region
				? level.getLevel().structureManager().forWorldGenRegion(region) : level.getLevel().structureManager();
		// At most eight 6x6 candidates. Plan every block before writing, including buried foundations.
		for (int attempt = 0; attempt < 8; attempt++) {
			int dx = random.nextInt(7) - 3, dz = random.nextInt(7) - 3;
			int width = 4 + random.nextInt(3), depth = 4 + random.nextInt(3);
			var columns = new ArrayList<OutcropColumn>();
			boolean valid = true;
			int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
			for (int ix = 0; ix < width && valid; ix++) for (int iz = 0; iz < depth && valid; iz++) {
				double nx = (ix - (width - 1) / 2.0) / (width / 2.0);
				double nz = (iz - (depth - 1) / 2.0) / (depth / 2.0);
				double radius = nx * nx + nz * nz;
				if (radius > 1.0 || (radius > 0.65 && random.nextInt(4) == 0)) continue;
				int x = origin.getX() + dx + ix - width / 2, z = origin.getZ() + dz + iz - depth / 2;
				int ox = x - origin.getX(), oz = z - origin.getZ();
				if (ox * ox + oz * oz > 64) { valid = false; break; }
				BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z), z);
				minY = Math.min(minY, pos.getY()); maxY = Math.max(maxY, pos.getY());
				int height = radius < 0.45 && random.nextInt(4) != 0 ? 2 : 1;
				int roots = radius < 0.65 ? 2 : 1;
				if (maxY - minY > 2 || !level.getBiome(pos).is(outcropBiomes.orElse(surfaceBiomes))) { valid = false; break; }
				for (int y = -roots - 1; y <= height; y++) {
					BlockPos cell = pos.offset(0, y, 0);
					if (!level.ensureCanWrite(cell)) { valid = false; break; }
					BlockState old = level.getBlockState(cell);
					if (!old.getFluidState().isEmpty() || level.getBlockEntity(cell) != null
							|| (y < 0 ? !naturalGround(old) : !(old.isAir() || (y == 0 && old.is(Blocks.SNOW))))
							|| structures.getStructureWithPieceAt(cell, holder -> true).isValid()) { valid = false; break; }
				}
				columns.add(new OutcropColumn(pos, height, roots));
			}
			if (!valid || columns.size() < 10) continue;
			// Follow gentle terrain, but never build a stone curtain over a ledge.
			for (var a : columns) for (var b : columns) {
				if (Math.abs(a.base.getX() - b.base.getX()) + Math.abs(a.base.getZ() - b.base.getZ()) == 1
						&& Math.abs(a.base.getY() - b.base.getY()) > 1) valid = false;
			}
			if (!valid) continue;
			var blocks = new LinkedHashMap<BlockPos, BlockState>();
			var exposed = new ArrayList<BlockPos>();
			for (var column : columns) {
				for (int y = -column.roots; y < column.height; y++) blocks.put(column.base.offset(0, y, 0), Blocks.STONE.defaultBlockState());
			}
			for (var column : columns) {
				BlockPos top = column.base.above(column.height - 1);
				for (var direction : Direction.Plane.HORIZONTAL) {
					BlockPos side = top.relative(direction);
					if (!blocks.containsKey(side) && level.ensureCanWrite(side) && level.getBlockState(side).isAir()) {
						exposed.add(top); break;
					}
				}
			}
			if (exposed.size() < 3) continue;
			// Tops with an open side stay visible after snow settles. Every ore keeps a stone support.
			int aboveGround = columns.stream().mapToInt(OutcropColumn::height).sum();
			int count = Math.min(Math.min(exposed.size(), aboveGround / 3), 3 + random.nextInt(outcropMax - 2));
			for (int n = 0; n < count; n++) blocks.put(exposed.remove(random.nextInt(exposed.size())), outcrop);
			blocks.forEach((pos, state) -> level.setBlock(pos, state, 2));
			return;
		}
	}

	private record OutcropColumn(BlockPos base, int height, int roots) { }

	private static boolean naturalGround(BlockState state) {
		return state.is(BlockTags.STONE_ORE_REPLACEABLES) || state.is(BlockTags.DIRT) || state.is(BlockTags.GRASS_BLOCKS)
				|| state.is(Blocks.GRAVEL) || state.is(Blocks.SNOW_BLOCK);
	}
}
