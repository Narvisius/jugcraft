package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaSpellbookCurios;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** This ring reveals invisible entities to its wearer; it does not grant invisibility. */
@Mixin(Entity.class)
public abstract class JimtaCurioVisibilityMixin {
    @Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
    private void jimta$seeInvisible(Player observer, CallbackInfoReturnable<Boolean> info) {
        if (observer != null && JimtaSpellbookCurios.has(observer, "invisibility_ring")) info.setReturnValue(false);
    }
}
