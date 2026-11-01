package io.github.jimbozoomer.jugcraft.mixin.client;

import io.github.jimbozoomer.jugcraft.client.OwnerPartyHatLayer;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreeperRenderer.class)
public abstract class OwnerPartyCreeperRenderMixin {
    @Inject(method="extractRenderState(Lnet/minecraft/world/entity/monster/Creeper;Lnet/minecraft/client/renderer/entity/state/CreeperRenderState;F)V",at=@At("TAIL"))
    private void jugcraft$hat(Creeper creeper,CreeperRenderState state,float partial,CallbackInfo ci){OwnerPartyHatLayer.extract(creeper,state);}
}
