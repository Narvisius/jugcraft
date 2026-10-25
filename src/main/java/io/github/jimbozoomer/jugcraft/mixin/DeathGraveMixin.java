package io.github.jimbozoomer.jugcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.jimbozoomer.jugcraft.grave.DeathGraves;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Prediction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class DeathGraveMixin {
    @WrapMethod(method="dropAllDeathLoot")
    private void jugcraft$deathDrops(ServerLevel level, DamageSource source, Operation<Void> original) {
        if ((Object)this instanceof ServerPlayer player) DeathGraves.duringDeath(player, () -> original.call(level, source));
        else original.call(level, source);
    }
    @Inject(method="createItemStackToDrop",at=@At("HEAD"),cancellable=true)
    private void jugcraft$storeDrop(ItemStack stack, boolean randomly, boolean thrown, CallbackInfoReturnable<ItemEntity> cir) {
        if (DeathGraves.capture((LivingEntity)(Object)this, stack)) cir.setReturnValue(null);
    }
}
