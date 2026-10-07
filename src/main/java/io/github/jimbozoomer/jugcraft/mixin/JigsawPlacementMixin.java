package io.github.jimbozoomer.jugcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.jimbozoomer.jugcraft.world.RetroShopPlacement;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;

/**
 * One Retro Game Shop per village (see RetroShopPlacement). JigsawPlacement's private {@code addPieces} grows a whole
 * structure from its start piece with a new jigsaw placer; it is the only place a placer is made. This marks where a
 * structure begins and ends, and when a village was laid out without its shop, runs the placer again from the start
 * piece for a new layout.
 */
@Mixin(JigsawPlacement.class)
public abstract class JigsawPlacementMixin {
	// JigsawPlacement has two methods named addPieces; this is the private one that runs the placer.
	@WrapMethod(method = "addPieces(Lnet/minecraft/world/level/levelgen/RandomState;IZLnet/minecraft/world/level/chunk/ChunkGenerator;"
			+ "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplateManager;Lnet/minecraft/world/level/LevelHeightAccessor;"
			+ "Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/Registry;Lnet/minecraft/world/level/levelgen/structure/PoolElementStructurePiece;"
			+ "Ljava/util/List;Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/world/level/levelgen/structure/pools/alias/PoolAliasLookup;"
			+ "Lnet/minecraft/world/level/levelgen/structure/templatesystem/LiquidSettings;)V", remap = false)
	private static void jugcraft$oneShopPerVillage(RandomState randomState, int maxDepth, boolean useExpansionHack,
			ChunkGenerator chunkGenerator, StructureTemplateManager templates, LevelHeightAccessor heightAccessor, RandomSource random,
			Registry<StructureTemplatePool> pools, PoolElementStructurePiece start, List<PoolElementStructurePiece> pieces, VoxelShape free,
			PoolAliasLookup aliases, LiquidSettings liquids, Operation<Void> original) {
		RetroShopPlacement.beginStructure();
		try {
			do {
				original.call(randomState, maxDepth, useExpansionHack, chunkGenerator, templates, heightAccessor, random, pools, start, pieces,
						free, aliases, liquids);
			} while (RetroShopPlacement.relayout(start, pieces));
		} finally {
			RetroShopPlacement.endStructure();
		}
	}
}
