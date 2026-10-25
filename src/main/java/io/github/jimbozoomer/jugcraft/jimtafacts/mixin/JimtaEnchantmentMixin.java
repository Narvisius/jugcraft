package io.github.jimbozoomer.jugcraft.jimtafacts.mixin;
import io.github.jimbozoomer.jugcraft.jimtafacts.JimtaAbilities;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(EnchantmentHelper.class)
public abstract class JimtaEnchantmentMixin {
    @ModifyReturnValue(method="getFishingLuckBonus",at=@At("RETURN"))
    private static int jimta$fishingLuck(int value,ServerLevel level,ItemStack rod,Entity fisher){return value+(JimtaAbilities.wearing(fisher,"anglers_hat")?1:0);}
    @ModifyReturnValue(method="getFishingTimeReduction",at=@At("RETURN"))
    private static float jimta$fishingSpeed(float value,ServerLevel level,ItemStack rod,Entity fisher){return JimtaAbilities.wearing(fisher,"anglers_hat")?Math.max(value,Math.min(25,value+5)):value;}
    @ModifyReturnValue(method="getEnchantmentLevel",at=@At("RETURN"))
    private static int jimta$looting(int value,Holder<Enchantment> enchantment,LivingEntity wearer){return value+(enchantment.is(Enchantments.LOOTING)&&JimtaAbilities.wearing(wearer,"superstitious_hat")?1:0);}
    @ModifyReturnValue(method="processMobExperience",at=@At("RETURN"))
    private static int jimta$experience(int value,ServerLevel level,Entity attacker,Entity victim,int original){return JimtaAbilities.wearing(attacker,"golden_hook")?(int)Math.min(Integer.MAX_VALUE,(long)value*3/2):value;}
}
