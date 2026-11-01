package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Only a successful direct torch hit ignites; blocked attacks and the optional offhand rule do not. */
@Mixin(LivingEntity.class)
public abstract class OwnerAmendmentTorchMixin {
    @Inject(method="hurtServer",at=@At("RETURN"))
    private void jugcraft$torch(ServerLevel level,DamageSource source,float amount,CallbackInfoReturnable<Boolean> cir){
        if(!cir.getReturnValueZ()||!source.is(DamageTypes.PLAYER_ATTACK)||!(source.getDirectEntity() instanceof ServerPlayer player))return;
        var held=player.getMainHandItem();var target=(LivingEntity)(Object)this;
        if((held.is(Items.TORCH)||held.is(Items.SOUL_TORCH))&&TwoHanded.allowed(player,level,target))target.igniteForSeconds(2);
    }
}
