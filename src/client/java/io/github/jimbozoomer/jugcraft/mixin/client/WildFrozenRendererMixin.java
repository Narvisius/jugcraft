package io.github.jimbozoomer.jugcraft.mixin.client;
import io.github.jimbozoomer.jugcraft.client.*;
import io.github.jimbozoomer.jugcraft.wilds.*;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntityRenderer.class)
@SuppressWarnings({"rawtypes","unchecked"})
public abstract class WildFrozenRendererMixin {
    @Shadow protected abstract boolean addLayer(RenderLayer layer);
    @Inject(method="<init>",at=@At("TAIL"))
    private void jugcraft$iceLayer(EntityRendererProvider.Context context,EntityModel model,float shadow,CallbackInfo ci){addLayer(new WildFrozenLayer((RenderLayerParent)(Object)this));}
    @Inject(method="extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",at=@At("TAIL"))
    private void jugcraft$icePose(LivingEntity e,LivingEntityRenderState s,float partial,CallbackInfo ci){
        boolean frozen=WildCold.frozen(e);((WildFrozenState)s).jugcraft$frozen(frozen);
        if(frozen&&e instanceof WildFrozenPose p){s.ageInTicks=p.jugcraft$frozenAge();s.walkAnimationPos=p.jugcraft$frozenWalk();s.walkAnimationSpeed=0;}
    }
}
