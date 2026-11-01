package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.botany.Jugtania;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** The source Crafting Placeholder is reusable, including in ordinary pattern recipes. */
@Mixin(Item.class)
public abstract class BotanicalPlaceholderMixin {
    @Inject(method="getCraftingRemainder",at=@At("HEAD"),cancellable=true)
    private void jugcraft$placeholder(CallbackInfoReturnable<ItemStackTemplate> cir){if((Object)this==Jugtania.ITEMS.get("placeholder"))cir.setReturnValue(new ItemStackTemplate((Item)(Object)this));}
}
