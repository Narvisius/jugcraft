package io.github.jimbozoomer.jugcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.jimbozoomer.jugcraft.nouveau.additions.ArsAdditionCharms;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Enderman.class)
public abstract class ArsCharmEndermanMixin {
    @ModifyReturnValue(method="isBeingStaredBy",at=@At("RETURN"))
    private boolean jugcraft$mask(boolean original,Player player){return original&&!ArsAdditionCharms.protect(player,ArsAdditionCharms.Kind.ENDER_MASK,1,20);}
}
