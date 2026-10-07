package io.github.jimbozoomer.jugcraft.world.design;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.*;

/** Per-world authored surface biomes, with the existing seeded source outside the design. */
public final class DesignBiomeSource extends BiomeSource {
	public static final MapCodec<DesignBiomeSource> CODEC = RecordCodecBuilder.<DesignBiomeSource>mapCodec(i -> i.group(
			Codec.intRange(1, 1).fieldOf("schema").forGetter(s -> 1),
			BiomeSource.CODEC.fieldOf("base").forGetter(s -> s.base),
			DesignGrid.CODEC.fieldOf("grid").forGetter(s -> s.grid),
			Biome.CODEC.listOf(1, 512).fieldOf("palette").forGetter(s -> s.palette),
			Codec.intRange(0, 512).listOf(25, 16641).fieldOf("cells").forGetter(s -> s.cells),
			BlockPos.CODEC.fieldOf("spawn").forGetter(s -> s.spawn),
			BlockPos.CODEC.optionalFieldOf("town").forGetter(s -> s.town)
	).apply(i, (schema, base, grid, palette, cells, spawn, town) -> new DesignBiomeSource(base, grid, palette, cells, spawn, town)))
			.validate(s -> s.cells.size() == s.grid.width() * s.grid.width() && s.cells.stream().allMatch(v -> v <= s.palette.size())
					&& s.grid.weight(s.spawn.getX(), s.spawn.getZ()) == 1
					&& s.town.map(p -> s.grid.weight(p.getX(), p.getZ()) == 1).orElse(true)
					? DataResult.success(s) : DataResult.error(() -> "Design cells/palette are invalid, or spawn/town is outside the solid design area"));
	private final BiomeSource base;
	private final DesignGrid grid;
	private final List<Holder<Biome>> palette;
	private final List<Integer> cells;
	private final BlockPos spawn;
	private final Optional<BlockPos> town;
	public DesignBiomeSource(BiomeSource base, DesignGrid grid, List<Holder<Biome>> palette, List<Integer> cells, BlockPos spawn, Optional<BlockPos> town) {
		this.base = base; this.grid = grid; this.palette = List.copyOf(palette); this.cells = List.copyOf(cells);
		this.spawn = spawn.immutable(); this.town = town.map(BlockPos::immutable);
	}
	public BlockPos spawn() { return spawn; }
	public Optional<BlockPos> town() { return town; }
	public DesignGrid grid() { return grid; }
	@Override protected MapCodec<? extends BiomeSource> codec() { return CODEC; }
	@Override protected Stream<Holder<Biome>> collectPossibleBiomes() { return Stream.concat(base.possibleBiomes().stream(), palette.stream()).distinct(); }
	@Override public BiomeResolver createResolver(Climate.Sampler sampler) { return overlay(base.createResolver(sampler)); }
	@Override public BiomeResolver createResolverForChunk(Climate.Sampler sampler, int x, int y, int z, int sx, int sy, int sz) {
		return overlay(base.createResolverForChunk(sampler, x, y, z, sx, sy, sz));
	}
	private BiomeResolver overlay(BiomeResolver fallback) {
		return (x, y, z) -> {
			int bx = x * 4, bz = z * 4;
			if (y >= 0 && grid.weight(bx, bz) > 0) {
				int id = cells.get(grid.cell(bx, bz));
				if (id > 0) return palette.get(id - 1);
			}
			return fallback.getNoiseBiome(x, y, z);
		};
	}
}
