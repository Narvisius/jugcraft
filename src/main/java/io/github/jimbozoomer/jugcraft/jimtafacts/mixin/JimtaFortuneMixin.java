package io.github.jimbozoomer.jugcraft.jimtafacts.mixin;
import io.github.jimbozoomer.jugcraft.jimtafacts.JimtaAbilities;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(ApplyBonusCount.class)
public abstract class JimtaFortuneMixin {
    @Shadow @Final private Holder<Enchantment> enchantment;
    @ModifyExpressionValue(method="run",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getItemEnchantmentLevel(Lnet/minecraft/core/Holder;Lnet/minecraft/world/item/ItemInstance;)I"))
    private int jimta$fortune(int level,ItemStack stack,LootContext context){return level+(enchantment.is(Enchantments.FORTUNE)&&JimtaAbilities.wearing(context.getOptional(LootContextParams.THIS_ENTITY),"lucky_scarf")?1:0);}
}
