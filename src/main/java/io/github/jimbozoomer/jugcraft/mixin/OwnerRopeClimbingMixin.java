package io.github.jimbozoomer.jugcraft.mixin;

import java.util.Optional;
import io.github.jimbozoomer.jugcraft.menagerie.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
abstract class OwnerRopeClimbingMixin {
    @Shadow private Optional<BlockPos> lastClimbablePos;
    @Inject(method="onClimbable",at=@At("HEAD"),cancellable=true)
    private void jugcraft$rope(CallbackInfoReturnable<Boolean> result){
        LivingEntity self=(LivingEntity)(Object)this;var pos=self.blockPosition();var state=self.level().getBlockState(pos);
        if(!OwnerSupplementRopes.rope(state))return;
        boolean climb=!self.isSpectator()&&HouseholdBlock.flag(state,"down")&&(HouseholdBlock.flag(state,"up")||self.getY()-pos.getY()<.8125);
        if(climb)lastClimbablePos=Optional.of(pos);result.setReturnValue(climb);
    }
}
