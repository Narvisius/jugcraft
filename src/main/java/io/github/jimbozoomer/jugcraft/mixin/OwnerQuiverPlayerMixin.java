package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.menagerie.OwnerQuiver;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Player.class)
public abstract class OwnerQuiverPlayerMixin {
 @Inject(method="getProjectile",at=@At("RETURN"),cancellable=true)
 private void jugcraft$quiver(ItemStack weapon,CallbackInfoReturnable<ItemStack> ci){ci.setReturnValue(OwnerQuiver.ammunition((Player)(Object)this,weapon,ci.getReturnValue()));}
}
