package io.github.jimbozoomer.jugcraft.jimtafacts.mixin;
import io.github.jimbozoomer.jugcraft.jimtafacts.JimtaAbilities;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.PowderSnowBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(PowderSnowBlock.class)
public abstract class JimtaSnowMixin {
    @Inject(method="canEntityWalkOnPowderSnow",at=@At("HEAD"),cancellable=true)
    private static void jimta$snowshoes(Entity entity,CallbackInfoReturnable<Boolean> cir){if(JimtaAbilities.wearing(entity,"snowshoes"))cir.setReturnValue(true);}
}
