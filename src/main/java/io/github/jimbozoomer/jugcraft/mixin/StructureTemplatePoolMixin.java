package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.world.RetroShopPlacement;
import io.github.jimbozoomer.jugcraft.world.RetroTrader;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Puts the Retro Game Shop first among a village's house candidates until it is placed (see RetroShopPlacement). */
@Mixin(StructureTemplatePool.class)
public abstract class StructureTemplatePoolMixin {
	@Inject(method = "getShuffledTemplates", at = @At("RETURN"), cancellable = true)
	private void jugcraft$oneShopPerVillage(RandomSource random, CallbackInfoReturnable<List<StructurePoolElement>> callback) {
		// Only the village houses pools that hold the shop; every other jigsaw pool is left alone at once.
		if (!RetroTrader.isExtended((StructureTemplatePool) (Object) this)) {
			return;
		}
		List<StructurePoolElement> reordered = RetroShopPlacement.reorder(callback.getReturnValue());
		if (reordered != null) {
			callback.setReturnValue(reordered);
		}
	}
}
