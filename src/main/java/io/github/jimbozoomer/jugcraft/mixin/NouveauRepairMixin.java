package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.nouveau.*;
import net.minecraft.server.level.*;
import net.minecraft.world.item.ItemStack;
import java.util.function.Consumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemStack.class)
public abstract class NouveauRepairMixin {
    @Inject(method="hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V",at=@At("HEAD"),cancellable=true)
    private void jugcraft$repairingArmor(int amount,ServerLevel level,ServerPlayer player,Consumer<ItemStack> broken,CallbackInfo ci){if(player!=null&&((ItemStack)(Object)this).getItem() instanceof NouveauGear.Armor&&NouveauThreads.level(player,"repairing")>=3)ci.cancel();}
}
