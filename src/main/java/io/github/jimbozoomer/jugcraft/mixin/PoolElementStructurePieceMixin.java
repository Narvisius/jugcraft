package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.world.RetroShopPlacement;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The jigsaw placer creates a piece for each element it places: note when that is the Retro Game Shop. */
@Mixin(PoolElementStructurePiece.class)
public abstract class PoolElementStructurePieceMixin {
	@Inject(method = "<init>*", at = @At("RETURN"))
	private void jugcraft$notePlacedShop(CallbackInfo callback) {
		RetroShopPlacement.placed(((PoolElementStructurePiece) (Object) this).getElement());
	}
}
