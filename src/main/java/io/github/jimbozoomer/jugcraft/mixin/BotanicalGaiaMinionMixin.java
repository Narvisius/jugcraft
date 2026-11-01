package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.botany.BotanicalGaia;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ritual adds are encounter hazards, never a repeatable source of ordinary drops or XP. */
@Mixin(LivingEntity.class)
abstract class BotanicalGaiaMinionMixin {
    @Inject(method="addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",at=@At("HEAD"),cancellable=true)
    private void jugcraft$gaiaPotion(net.minecraft.world.effect.MobEffectInstance effect,net.minecraft.world.entity.Entity source,org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> ci){
        if(!BotanicalGaia.allowsEffect((LivingEntity)(Object)this,source))ci.setReturnValue(false);
    }
    @Inject(method="tick",at=@At("HEAD"))
    private void jugcraft$gaiaGuard(CallbackInfo ci){BotanicalGaia.guardMinion((LivingEntity)(Object)this);}
    @Inject(method="dropAllDeathLoot",at=@At("HEAD"),cancellable=true)
    private void jugcraft$noFarm(ServerLevel level,DamageSource source,CallbackInfo ci){
        if(BotanicalGaia.minion((LivingEntity)(Object)this))ci.cancel();
    }
}
