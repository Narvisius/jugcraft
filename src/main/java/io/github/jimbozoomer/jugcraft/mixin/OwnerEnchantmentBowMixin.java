package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.enchanting.OwnerEnchantments;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(BowItem.class)
public abstract class OwnerEnchantmentBowMixin {
    @ModifyVariable(method="releaseUsing",at=@At("HEAD"),argsOnly=true)
    private int jugcraft$quickdraw(int remaining,ItemStack bow,Level level,LivingEntity player,int original){
        int rank=OwnerEnchantments.level(player,bow,"quickdraw"),duration=bow.getUseDuration(player),ticks=duration-remaining;
        return rank==0?remaining:duration-Math.min(20,(int)Math.ceil(Math.max(0,ticks)*(1+Math.min(.3,.1*rank))));
    }
}
