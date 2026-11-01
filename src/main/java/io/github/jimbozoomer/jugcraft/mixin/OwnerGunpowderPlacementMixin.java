package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.menagerie.OwnerGunpowder;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Item.class)
public abstract class OwnerGunpowderPlacementMixin {
    @Inject(method="useOn",at=@At("HEAD"),cancellable=true)
    private void jugcraft$fuse(UseOnContext context,CallbackInfoReturnable<InteractionResult> result){if(context.getItemInHand().is(Items.GUNPOWDER))result.setReturnValue(OwnerGunpowder.place(context));}
}
