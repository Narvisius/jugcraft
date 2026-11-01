package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.arcane.ArcaneBlackHole;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemEntity.class)
abstract class ArcaneDroppedMatterMixin {
    @Inject(method="tick",at=@At("TAIL"))
    private void jugcraft$matter(CallbackInfo ci){ArcaneBlackHole.dropped((ItemEntity)(Object)this);}
}
