package io.github.jimbozoomer.jugcraft.jimtafacts.mixin;
import io.github.jimbozoomer.jugcraft.jimtafacts.*;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(LivingEntity.class)
public abstract class JimtaLivingMixin {
    @Shadow protected int useItemRemaining;
    @Unique private int jimta$eatenNutrition;
    @ModifyReturnValue(method="getDamageAfterMagicAbsorb",at=@At("RETURN"))
    private float jimta$industrialShield(float amount,@Local(argsOnly=true) net.minecraft.world.damagesource.DamageSource source){
        return io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialGear.absorb((LivingEntity)(Object)this,source,amount);
    }
    @Inject(method="canStandOnFluid",at=@At("HEAD"),cancellable=true)
    private void jimta$walkFluid(FluidState state,CallbackInfoReturnable<Boolean> cir){if(JimtaAbilities.fluidWalking((LivingEntity)(Object)this,state))cir.setReturnValue(true);}
    @ModifyExpressionValue(method="travelInAir",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/Block;getFriction()F"))
    private float jimta$grip(float friction){return friction>.6F&&JimtaAbilities.wearing((LivingEntity)(Object)this,"steadfast_spikes")?.6F:friction;}
    @Inject(method="updateUsingItem",at=@At("HEAD"))
    private void jimta$fasterConsumption(ItemStack stack,CallbackInfo ci){
        var self=(LivingEntity)(Object)this;var animation=stack.getUseAnimation();
        if(useItemRemaining>1&&(animation==ItemUseAnimation.EAT||animation==ItemUseAnimation.DRINK)
                &&(JimtaAbilities.wearing(self,"plastic_drinking_hat")||JimtaAbilities.wearing(self,"novelty_drinking_hat")))useItemRemaining--;
    }
    @Inject(method="completeUsingItem",at=@At("HEAD"))
    private void jimta$rememberFood(CallbackInfo ci){
        var self=(LivingEntity)(Object)this;var food=self.getUseItem().get(DataComponents.FOOD);
        jimta$eatenNutrition=food==null?0:food.nutrition();
    }
    @Inject(method="completeUsingItem",at=@At("RETURN"))
    private void jimta$afterEating(CallbackInfo ci){
        var self=(LivingEntity)(Object)this;
        if(self instanceof ServerPlayer player&&jimta$eatenNutrition>0){
            if(JimtaAbilities.wearing(player,"onion_ring"))player.addEffect(new MobEffectInstance(MobEffects.HASTE,Math.min(2400,jimta$eatenNutrition*200)));
            if(JimtaAbilities.wearing(player,"rooted_boots"))JimtaAbilities.growNearby(player);
        }
        jimta$eatenNutrition=0;
    }
}
