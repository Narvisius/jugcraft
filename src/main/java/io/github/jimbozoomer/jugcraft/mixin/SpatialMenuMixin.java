package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.jimtafacts.network.SpatialReservation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class SpatialMenuMixin {
    @Inject(method="clicked",at=@At("HEAD"),cancellable=true)
    private void jugcraft$holdReservedMenu(int slot,int button,ContainerInput input,Player player,CallbackInfo info){
        if(((AbstractContainerMenu)(Object)this).slots.stream().anyMatch(s->SpatialReservation.containerReserved(s.container)))info.cancel();
    }
}
