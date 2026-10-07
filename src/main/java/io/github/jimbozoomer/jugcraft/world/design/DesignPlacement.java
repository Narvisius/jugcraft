package io.github.jimbozoomer.jugcraft.world.design;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

/** One structure start chunk, still subject to that structure's biome/terrain constraints. */
public record DesignPlacement(int chunkX, int chunkZ) implements StructurePlacement {
	public static final MapCodec<DesignPlacement> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.intRange(-62500, 62500).fieldOf("chunk_x").forGetter(DesignPlacement::chunkX),
			Codec.intRange(-62500, 62500).fieldOf("chunk_z").forGetter(DesignPlacement::chunkZ)
	).apply(i, DesignPlacement::new));
	@Override public boolean isStructureChunk(ChunkGeneratorStructureState state, int x, int z) {
		return x == chunkX && z == chunkZ;
	}
	@Override public MapCodec<? extends StructurePlacement> codec() { return CODEC; }
}
