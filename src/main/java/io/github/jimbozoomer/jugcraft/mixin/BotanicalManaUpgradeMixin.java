package io.github.jimbozoomer.jugcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.jimbozoomer.jugcraft.botany.BotanicalManaItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({ShapedRecipe.class,ShapelessRecipe.class})
public abstract class BotanicalManaUpgradeMixin {
    @ModifyReturnValue(method="assemble(Lnet/minecraft/world/item/crafting/CraftingInput;)Lnet/minecraft/world/item/ItemStack;",at=@At("RETURN"))
    private ItemStack jugcraft$retainPortableMana(ItemStack output,CraftingInput input){
        int capacity=BotanicalManaItems.capacity(output);if(capacity<=0)return output;
        long total=0;for(var item:input.items())total+=BotanicalManaItems.stored(item);
        if(total>0){output=output.copy();BotanicalManaItems.stored(output,(int)Math.min(capacity,total));}return output;
    }
}
