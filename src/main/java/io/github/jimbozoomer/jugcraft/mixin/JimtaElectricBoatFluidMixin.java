package io.github.jimbozoomer.jugcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialBoats;
import net.minecraft.tags.*;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.material.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Only the supplied electric hull treats lava as a supporting fluid; ordinary boats are unchanged. */
@Mixin(AbstractBoat.class)
public abstract class JimtaElectricBoatFluidMixin {
    @WrapOperation(method={"checkInWater","isUnderwater","getWaterLevelAbove"},at=@At(value="INVOKE",target="Lnet/minecraft/world/level/material/FluidState;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean jugcraft$electricLavaBuoyancy(FluidState fluid,TagKey<Fluid> tag,Operation<Boolean> original){
        return original.call(fluid,tag)||tag.equals(FluidTags.WATER)&&(Object)this instanceof IndustrialBoats.Hull hull&&hull.lavaHull()&&fluid.is(FluidTags.LAVA);
    }
}
