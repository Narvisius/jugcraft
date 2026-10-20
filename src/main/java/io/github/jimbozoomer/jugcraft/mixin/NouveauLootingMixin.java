package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauThreads;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(EnchantmentHelper.class)
public abstract class NouveauLootingMixin {
    @Inject(method="getEnchantmentLevel",at=@At("RETURN"),cancellable=true)
    private static void jugcraft$drygmyLooting(Holder<Enchantment> enchantment,LivingEntity entity,CallbackInfoReturnable<Integer> cir){if(entity instanceof Player p&&enchantment.is(Enchantments.LOOTING)){int extra=NouveauThreads.level(p,"drygmy");if(io.github.jimbozoomer.jugcraft.nouveau.NouveauFamiliars.active(p).equals("drygmy")&&p.getRandom().nextInt(3)==0)extra++;var loot=p.getEffect(io.github.jimbozoomer.jugcraft.nouveau.NouveauStatuses.EFFECTS.get("looting"));if(loot!=null)extra+=loot.getAmplifier()+1;cir.setReturnValue(cir.getReturnValue()+extra);}}
}
