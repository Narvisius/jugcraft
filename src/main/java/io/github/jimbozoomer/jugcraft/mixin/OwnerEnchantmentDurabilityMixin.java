package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.enchanting.OwnerEnchantments;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import java.util.function.Consumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(ItemStack.class)
public abstract class OwnerEnchantmentDurabilityMixin {
    @ModifyVariable(method="hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V",at=@At("HEAD"),argsOnly=true)
    private int jugcraft$endurance(int amount,int original,ServerLevel level,ServerPlayer player,Consumer<Item> broken){
        var stack=(ItemStack)(Object)this;
        int remaining=OwnerEnchantments.durability(player,stack,amount);
        return io.github.jimbozoomer.jugcraft.botany.BotanicalEquipment.damage(player,stack,remaining);
    }
}
