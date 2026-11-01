package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import io.github.jimbozoomer.jugcraft.town.TownProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(FallingBlockEntity.class)
public abstract class OwnerLanternLandingMixin {
    @Redirect(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean jugcraft$landing(Level level,BlockPos pos,BlockState state){
        var falling=(FallingBlockEntity)(Object)this;
        if(state.getBlock() instanceof LanternBlock&&(!ClaimTransfers.allowed(level,falling.getStartPos(),pos)||TownProtection.shieldsBlock(level,pos)))return false;
        return level.setBlockAndUpdate(pos,state);
    }
}
