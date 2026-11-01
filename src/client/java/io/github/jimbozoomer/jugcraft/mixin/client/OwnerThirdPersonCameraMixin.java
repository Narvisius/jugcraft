package io.github.jimbozoomer.jugcraft.mixin.client;
import io.github.jimbozoomer.jugcraft.client.OwnerThirdPerson;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
@Mixin(Camera.class)
public abstract class OwnerThirdPersonCameraMixin {
 @WrapOperation(method="alignWithEntity",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"))
 private float jugcraft$yaw(Entity e,float delta,Operation<Float> original){return OwnerThirdPerson.yaw(e,original.call(e,delta));}
 @WrapOperation(method="alignWithEntity",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"))
 private float jugcraft$pitch(Entity e,float delta,Operation<Float> original){return OwnerThirdPerson.pitch(e,original.call(e,delta));}
}
