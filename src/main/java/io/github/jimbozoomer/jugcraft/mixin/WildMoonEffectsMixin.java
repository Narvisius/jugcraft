package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.wilds.WildMoonEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(LivingEntity.class)
public abstract class WildMoonEffectsMixin {
    @ModifyVariable(method="hurtServer",at=@At("HEAD"),argsOnly=true)
    private float jugcraft$fragile(float amount,ServerLevel level,DamageSource source,float original){return WildMoonEffects.damage((LivingEntity)(Object)this,source,amount);}
    @Inject(method="canBeAffected",at=@At("HEAD"),cancellable=true)
    private void jugcraft$eclipse(MobEffectInstance effect,CallbackInfoReturnable<Boolean> ci){if(WildMoonEffects.blocks((LivingEntity)(Object)this,effect))ci.setReturnValue(false);}
    @Inject(method="tick",at=@At("HEAD"))
    private void jugcraft$restoreMoonEffects(CallbackInfo ci){WildMoonEffects.tick((LivingEntity)(Object)this);}
}
