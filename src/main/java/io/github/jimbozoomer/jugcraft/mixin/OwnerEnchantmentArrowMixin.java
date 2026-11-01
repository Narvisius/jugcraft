package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.enchanting.OwnerEnchantmentProjectiles;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AbstractArrow.class)
public abstract class OwnerEnchantmentArrowMixin implements OwnerEnchantmentProjectiles.Flight {
    @Unique private Vec3 jugcraft$start;
    @Unique private boolean jugcraft$redirected,jugcraft$salvaged;
    public Vec3 jugcraft$start(){return jugcraft$start;}
    public boolean jugcraft$redirected(){return jugcraft$redirected;}
    public void jugcraft$redirect(){jugcraft$redirected=true;}
    public boolean jugcraft$salvaged(){return jugcraft$salvaged;}
    public void jugcraft$salvage(){jugcraft$salvaged=true;}
    @Inject(method="tick",at=@At("HEAD"))
    private void jugcraft$start(CallbackInfo ci){if(jugcraft$start==null)jugcraft$start=((AbstractArrow)(Object)this).position();}
    @Inject(method="onHitEntity",at=@At("HEAD"),cancellable=true)
    private void jugcraft$ricochet(EntityHitResult hit,CallbackInfo ci){if(hit.getEntity() instanceof LivingEntity target&&OwnerEnchantmentProjectiles.redirect((AbstractArrow)(Object)this,target))ci.cancel();}
    @Inject(method="addAdditionalSaveData",at=@At("TAIL"))
    private void jugcraft$save(ValueOutput out,CallbackInfo ci){out.putBoolean("JugcraftRicochet",jugcraft$redirected);out.putBoolean("JugcraftSalvaged",jugcraft$salvaged);if(jugcraft$start!=null)out.store("JugcraftShotOrigin",Vec3.CODEC,jugcraft$start);}
    @Inject(method="readAdditionalSaveData",at=@At("TAIL"))
    private void jugcraft$load(ValueInput in,CallbackInfo ci){jugcraft$redirected=in.getBooleanOr("JugcraftRicochet",false);jugcraft$salvaged=in.getBooleanOr("JugcraftSalvaged",false);jugcraft$start=in.read("JugcraftShotOrigin",Vec3.CODEC).orElse(null);}
}
