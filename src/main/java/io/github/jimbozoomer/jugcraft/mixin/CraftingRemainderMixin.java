package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.crafting.CraftingChoices;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ResultSlot.class)
public abstract class CraftingRemainderMixin extends Slot {
    @Shadow @Final private Player player;
    private CraftingRemainderMixin(net.minecraft.world.Container c, int i, int x, int y) { super(c,i,x,y); }
    @Inject(method="getRemainingItems", at=@At("HEAD"), cancellable=true)
    private void jugcraft$selectedRemainders(CraftingInput input, Level level, CallbackInfoReturnable<NonNullList<ItemStack>> cir) {
        if (player instanceof ServerPlayer p && container instanceof ResultContainer result) {
            var recipe = CraftingChoices.remainingRecipe(result, input, p);
            if (recipe != null) cir.setReturnValue(recipe.value().getRemainingItems(input));
        }
    }
}
