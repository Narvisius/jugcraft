package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.enchanting.OwnerEnchantments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(LivingEntity.class)
public abstract class OwnerEnchantmentDamageMixin {
    @ModifyVariable(method="hurtServer",at=@At("HEAD"),argsOnly=true)
    private float jugcraft$reforged(float amount,ServerLevel level,DamageSource source,float original){return OwnerEnchantments.damage((LivingEntity)(Object)this,source,amount);}
}
