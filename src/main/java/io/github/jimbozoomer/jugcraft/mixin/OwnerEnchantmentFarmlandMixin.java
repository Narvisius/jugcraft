package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.enchanting.OwnerEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(FarmlandBlock.class)
public abstract class OwnerEnchantmentFarmlandMixin {
    @Inject(method="fallOn",at=@At("HEAD"),cancellable=true)
    private void jugcraft$delicate(Level level,BlockState state,BlockPos pos,Entity entity,double distance,CallbackInfo ci){
        if(entity instanceof LivingEntity e&&(OwnerEnchantments.level(e,e.getMainHandItem(),"delicate_touch")>0||OwnerEnchantments.level(e,e.getOffhandItem(),"delicate_touch")>0)){
            // Preserve landing damage while preventing only the farmland replacement.
            entity.causeFallDamage(distance,1,level.damageSources().fall());ci.cancel();
        }
    }
}
