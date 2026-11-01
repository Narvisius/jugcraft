package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.aquatic.OwnerAquaculture;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(AbstractArrow.class)
public abstract class OwnerAquaticArrowMixin {
    @Inject(method="getWaterInertia",at=@At("HEAD"),cancellable=true)
    private void jugcraft$seaBow(CallbackInfoReturnable<Float> cir){var bow=((AbstractArrow)(Object)this).getWeaponItem();if(bow!=null&&bow.is(OwnerAquaculture.ITEMS.get("neptunium_bow")))cir.setReturnValue(1f);}
}
