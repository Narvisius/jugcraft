package io.github.jimbozoomer.jugcraft.mixin.client;
import io.github.jimbozoomer.jugcraft.client.jimtafacts.JimtaWardClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Viewer-local outlines, never a global glowing status visible to other players. */
@Mixin(Minecraft.class)
public abstract class JimtaPlanarSightMixin {
    @Inject(method="shouldEntityAppearGlowing",at=@At("RETURN"),cancellable=true)
    private void jimta$planarSight(Entity e,CallbackInfoReturnable<Boolean> result){if(JimtaWardClient.sees(e))result.setReturnValue(true);}
}
