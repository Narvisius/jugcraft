package io.github.jimbozoomer.jugcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.jimbozoomer.jugcraft.nouveau.additions.ArsAdditionCharms;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.PowderSnowBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PowderSnowBlock.class)
public abstract class ArsCharmSnowMixin {
    @ModifyReturnValue(method="canEntityWalkOnPowderSnow",at=@At("RETURN"))
    private static boolean jugcraft$snow(boolean original,Entity entity){return original||entity instanceof net.minecraft.world.entity.LivingEntity wearer&&io.github.jimbozoomer.jugcraft.botany.BotanicalAccessories.has(wearer,"ice_pendant")||ArsAdditionCharms.snowWalk(entity)||entity instanceof net.minecraft.world.entity.LivingEntity living&&living.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).is(io.github.jimbozoomer.jugcraft.botany.Jugtania.item("manaweave_boots"));}
}
