package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.crafting.CraftingChoices;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.crafting.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(CraftingMenu.class)
public abstract class CraftingChoiceMixin {
    @Inject(method="slotChangedCraftingGrid", at=@At("HEAD"), cancellable=true)
    private static void jugcraft$choose(AbstractContainerMenu menu, ServerLevel level, Player player,
            CraftingContainer grid, ResultContainer result, RecipeHolder<CraftingRecipe> hint, CallbackInfo ci) {
        CraftingChoices.update(menu, (ServerPlayer)player, grid, result, hint); ci.cancel();
    }
}
