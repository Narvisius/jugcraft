package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.botany.BotanicalAccessories;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ServerPlayer.class)
public abstract class BotanicalMagnetDropMixin {
    @Inject(method="drop(Lnet/minecraft/world/item/ItemStack;ZLnet/minecraft/util/Prediction;)Lnet/minecraft/world/entity/item/ItemEntity;",at=@At("RETURN"))
    private void jugcraft$leaveThrownItems(ItemStack stack,boolean scatter,Prediction prediction,CallbackInfoReturnable<ItemEntity> ci){if(ci.getReturnValue()!=null)BotanicalAccessories.tossed((ServerPlayer)(Object)this);}
}
