package io.github.jimbozoomer.jugcraft.mixin.client;
import io.github.jimbozoomer.jugcraft.client.WildFrozenState;
import io.github.jimbozoomer.jugcraft.wilds.WildCold;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(EntityRenderer.class)
public abstract class WildFrozenExtractMixin {
    @Inject(method="extractRenderState",at=@At("TAIL"))
    private void jugcraft$frozen(Entity entity,EntityRenderState state,float partial,CallbackInfo ci){((WildFrozenState)state).jugcraft$frozen(entity instanceof LivingEntity living&&WildCold.frozen(living)&&!entity.isInvisible());}
}
