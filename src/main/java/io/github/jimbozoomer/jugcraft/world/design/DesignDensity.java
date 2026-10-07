package io.github.jimbozoomer.jugcraft.world.design;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Interval;
import net.minecraft.world.level.levelgen.densityfunction.*;

/** Native noise-generator extension: shapes land before surfaces, ores and biome features run. */
public record DesignDensity(DesignGrid grid, DensityFunction input, boolean surface) implements DensityFunction {
	public static final MapCodec<DesignDensity> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			DesignGrid.CODEC.fieldOf("grid").forGetter(DesignDensity::grid),
			DensityFunction.CODEC.fieldOf("input").forGetter(DesignDensity::input),
			Codec.BOOL.optionalFieldOf("surface", false).forGetter(DesignDensity::surface)
	).apply(i, DesignDensity::new));
	public float transform(float original, int x, int y, int z) {
		float weight = grid.weight(x, z);
		if (weight == 0) return original;
		float target = grid.height(x, z);
		if (!surface) {
			// Preserve the original deep caves and bedrock; transition to authored land between Y=8 and Y=32.
			weight *= Math.clamp((y - 8) / 24f, 0, 1);
			target = Math.clamp((target - y - 0.5f) / 8, -64, 64);
		}
		return original * (1 - weight) + target * weight;
	}
	@Override public DensitySampler compileSampler(CompileContext context) {
		DensitySampler base = input.compileSampler(context);
		return new DensitySampler() {
			@Override public float sampleValue(SamplerContext context, int x, int y, int z) {
				return transform(base.sampleValue(context, x, y, z), x, y, z);
			}
			@Override public void sampleVolume(SamplerContext context, DensityBuffer output, DensityVolume volume) {
				base.sampleVolume(context, output, volume);
				for (int x = 0; x < volume.sizeX(); x++) for (int z = 0; z < volume.sizeZ(); z++) {
					int bx = volume.blockX(x), bz = volume.blockZ(z);
					if (grid.weight(bx, bz) == 0) continue;
					for (int y = 0; y < volume.sizeY(); y++) {
						int index = volume.indexUnchecked(x, y, z);
						output.set(index, transform(output.get(index), bx, volume.blockY(y), bz));
					}
				}
			}
		};
	}
	@Override public DensityFunction rewriteChildren(DfRewriteRule rule) { return new DesignDensity(grid, rule.rewrite(input), surface); }
	@Override public Interval range() { return Interval.encapsulating(input.range(), Interval.of(-64, 256)); }
	@Override public int domainAxes() { return surface ? input.domainAxes() | AXIS_X | AXIS_Z : ALL_AXES; }
	@Override public MapCodec<? extends DensityFunction> codec() { return CODEC; }
}
