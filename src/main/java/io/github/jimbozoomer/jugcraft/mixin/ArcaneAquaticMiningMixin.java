package io.github.jimbozoomer.jugcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.jimbozoomer.jugcraft.arcane.ArcaneModifiers;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class ArcaneAquaticMiningMixin {
    @ModifyReturnValue(method="getDestroySpeed",at=@At("RETURN"))
    private float jugcraft$aquatic(float original){var p=(Player)(Object)this;return !p.onGround()&&p.isEyeInFluid(FluidTags.WATER)&&ArcaneModifiers.has(p.getItemBySlot(EquipmentSlot.HEAD),"aquatic")?original*5:original;}
}
