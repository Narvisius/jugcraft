package io.github.jimbozoomer.jugcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.botany.BotanicalArmor;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Use the actual native critical decision; bonuses never apply to sweeps/projectiles. */
@Mixin(Player.class)
public abstract class BotanicalAncientWillMixin {
    @WrapOperation(method="attack",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean jugcraft$ancientWills(Entity target,DamageSource source,float amount,Operation<Boolean> original,@Local(index=9) boolean critical){
        if(!critical||!((Object)this instanceof ServerPlayer p)||!(target instanceof LivingEntity victim)||!Authority.mayStrike(p,victim)||!Authority.mayChange(p.level(),p,victim.blockPosition()))return original.call(target,source,amount);
        if(BotanicalArmor.will(p,1))amount*=1+(1-p.getHealth()/p.getMaxHealth())*.5F;
        if(BotanicalArmor.will(p,4))source=new DamageSource(p.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE,Jugcraft.id("botanical_armor_piercing"))),p);
        float before=victim.getHealth()+victim.getAbsorptionAmount();boolean hit=original.call(target,source,amount);
        if(hit){
            if(BotanicalArmor.will(p,0))victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,20,1),p);
            if(BotanicalArmor.will(p,3))victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,60,1),p);
            if(BotanicalArmor.will(p,5))victim.addEffect(new MobEffectInstance(MobEffects.WITHER,60,1),p);
            if(BotanicalArmor.will(p,2))p.heal(Math.max(0,before-victim.getHealth()-victim.getAbsorptionAmount())*.25F);
        }return hit;
    }
}
