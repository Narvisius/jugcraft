package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.menagerie.OwnerSupplementRopes;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LeashFenceKnotEntity.class)
public abstract class OwnerRopeKnotMixin {
    @Inject(method="survives",at=@At("HEAD"),cancellable=true)
    private void jugcraft$ropeSupport(CallbackInfoReturnable<Boolean> result){
        var knot=(LeashFenceKnotEntity)(Object)this;
        if(knot.level().hasChunkAt(knot.getPos())&&OwnerSupplementRopes.rope(knot.level().getBlockState(knot.getPos())))result.setReturnValue(true);
    }
}
