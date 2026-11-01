package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.arcane.ArcaneBuckets;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Default crafting recipes, including Crafter automation, consume one unit rather than the entire bucket. */
@Mixin(CraftingRecipe.class)
public interface ArcaneBucketRemainderMixin {
    @Inject(method="getRemainingItems",at=@At("RETURN"))
    private void jugcraft$capacity(CraftingInput input,CallbackInfoReturnable<NonNullList<ItemStack>> ci){
        var remainders=ci.getReturnValue();for(int i=0;i<input.size();i++){var stack=input.getItem(i);if(ArcaneBuckets.amount(stack)>0)remainders.set(i,ArcaneBuckets.craftingRemainder(stack));}
    }
}
