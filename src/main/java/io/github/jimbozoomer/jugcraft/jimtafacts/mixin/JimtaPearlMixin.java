package io.github.jimbozoomer.jugcraft.jimtafacts.mixin;
import io.github.jimbozoomer.jugcraft.jimtafacts.JimtaAbilities;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(EnderpearlItem.class)
public abstract class JimtaPearlMixin {
    @Redirect(method="use",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;consume(ILnet/minecraft/world/entity/LivingEntity;)V"))
    private void jimta$hungerForPearl(ItemStack stack,int amount,LivingEntity entity){
        if(entity instanceof Player player&&JimtaAbilities.wearing(player,"warp_drive")&&player.getFoodData().getFoodLevel()>=2){
            if(!player.level().isClientSide()&&!player.isCreative())player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel()-2);
        }else stack.consume(amount,entity);
    }
}
