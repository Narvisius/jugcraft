package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.crafting.CraftingChoices;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AbstractContainerMenu.class)
public abstract class CraftingChoiceClickMixin {
    @Inject(method="clicked",at=@At("HEAD"))
    private void jugcraft$revalidate(int slot,int button,ContainerInput input,Player player,CallbackInfo ci) {
        if(player instanceof ServerPlayer p)CraftingChoices.beforeClick((AbstractContainerMenu)(Object)this,p);
    }
}
