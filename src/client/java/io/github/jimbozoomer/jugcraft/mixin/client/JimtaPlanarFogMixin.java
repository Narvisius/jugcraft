package io.github.jimbozoomer.jugcraft.mixin.client;

import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaWardSpells;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The supplied Planar Sight fog distances, applied only to its viewer and never clearing environmental hazards. */
@Mixin(FogRenderer.class)
public abstract class JimtaPlanarFogMixin {
    @Inject(method="setupFog",at=@At("RETURN"))
    private void jugcraft$planarFog(Camera camera,int distance,DeltaTracker delta,float darkness,ClientLevel level,CallbackInfoReturnable<FogData> cir){
        if(camera.getFluidInCamera()!=FogType.NONE||!(camera.entity() instanceof LivingEntity viewer)
                ||!JimtaWardSpells.has(viewer,"planar_sight")||viewer.hasEffect(MobEffects.BLINDNESS)||viewer.hasEffect(MobEffects.DARKNESS))return;
        var data=cir.getReturnValue();
        if(data.environmentalEnd>=160){data.environmentalStart=-80;data.environmentalEnd=160;}
        data.skyEnd=Math.min(data.skyEnd,40);
        data.cloudEnd=Math.min(data.cloudEnd,160);
    }
}
