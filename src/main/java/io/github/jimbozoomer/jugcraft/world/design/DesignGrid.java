package io.github.jimbozoomer.jugcraft.world.design;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

/** Immutable, bounded height field. Heights name the first air block, in world coordinates. */
public record DesignGrid(int originX, int originZ, int step, int width, List<Float> heights) {
	public static final Codec<DesignGrid> CODEC = RecordCodecBuilder.<DesignGrid>create(i -> i.group(
			Codec.intRange(-1000000, 1000000).fieldOf("origin_x").forGetter(DesignGrid::originX),
			Codec.intRange(-1000000, 1000000).fieldOf("origin_z").forGetter(DesignGrid::originZ),
			Codec.intRange(4, 128).fieldOf("step").forGetter(DesignGrid::step),
			Codec.intRange(5, 129).fieldOf("width").forGetter(DesignGrid::width),
			Codec.floatRange(40, 256).listOf(25, 16641).fieldOf("heights").forGetter(DesignGrid::heights)
    ).apply(i, DesignGrid::new)).validate(g -> g.heights.size() == g.width * g.width && (g.step & (g.step - 1)) == 0
            && g.heights.stream().allMatch(Float::isFinite)
			? DataResult.success(g) : DataResult.error(() -> "Design grid needs width squared heights and a power-of-two step"));

	public DesignGrid { heights = List.copyOf(heights); }
	public int extent() { return (width - 1) * step; }
	public boolean contains(int x, int z) { return x >= originX && z >= originZ && x <= originX + extent() && z <= originZ + extent(); }
	/** Two cells at each edge feather to the original generator. Outside is exactly unchanged. */
	public float weight(int x, int z) {
		float edge = Math.min(Math.min(x - originX, z - originZ), Math.min(originX + extent() - x, originZ + extent() - z));
		float t = Math.clamp(edge / (2f * step), 0, 1);
		return t * t * (3 - 2 * t);
	}
	public float height(int x, int z) {
		float gx = Math.clamp((x - originX) / (float) step, 0, width - 1);
		float gz = Math.clamp((z - originZ) / (float) step, 0, width - 1);
		int ix = Math.min((int) gx, width - 2), iz = Math.min((int) gz, width - 2);
		float tx = gx - ix, tz = gz - iz;
		float a = heights.get(iz * width + ix) * (1 - tx) + heights.get(iz * width + ix + 1) * tx;
		float b = heights.get((iz + 1) * width + ix) * (1 - tx) + heights.get((iz + 1) * width + ix + 1) * tx;
		return a * (1 - tz) + b * tz;
	}
	public int cell(int x, int z) {
		return Math.clamp(Math.round((z - originZ) / (float) step), 0, width - 1) * width
				+ Math.clamp(Math.round((x - originX) / (float) step), 0, width - 1);
	}
}
