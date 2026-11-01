package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Snowball.class)
public abstract class OwnerAmendmentSnowballMixin {
    @Inject(method="onHitEntity",at=@At("TAIL"))
    private void jugcraft$chill(EntityHitResult hit,CallbackInfo ci){
        var ball=(Snowball)(Object)this;
        if(ball.level() instanceof ServerLevel level&&ball.getOwner() instanceof ServerPlayer player&&hit.getEntity() instanceof LivingEntity target&&target.canFreeze()&&TwoHanded.allowed(player,level,target))
            target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze()+35,target.getTicksFrozen()+35));
    }
}
