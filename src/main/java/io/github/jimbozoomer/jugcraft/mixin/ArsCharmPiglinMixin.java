package io.github.jimbozoomer.jugcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.jimbozoomer.jugcraft.nouveau.additions.ArsAdditionCharms;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PiglinAi.class)
public abstract class ArsCharmPiglinMixin {
    @ModifyReturnValue(method="isWearingSafeArmor",at=@At("RETURN"))
    private static boolean jugcraft$gold(boolean original,LivingEntity entity){return original||ArsAdditionCharms.protect(entity,ArsAdditionCharms.Kind.GOLDEN,1,20);}
}
