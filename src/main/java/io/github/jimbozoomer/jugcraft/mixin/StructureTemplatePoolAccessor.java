package io.github.jimbozoomer.jugcraft.mixin;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets the Retro Game Shop join the plains village houses pool (Fabric API has no village pool API). */
@Mixin(StructureTemplatePool.class)
public interface StructureTemplatePoolAccessor {
	/** The weighted list the jigsaw placer draws from: each element appears once per point of weight. */
	@Accessor("templates")
	ObjectArrayList<StructurePoolElement> jugcraft$templates();
}
