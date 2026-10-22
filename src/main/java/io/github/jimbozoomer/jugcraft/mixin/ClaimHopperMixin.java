package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public abstract class ClaimHopperMixin {
    @Inject(method = "ejectItems", at = @At("HEAD"), cancellable = true)
    private static void jugcraft$eject(Level level, BlockPos pos, HopperBlockEntity hopper, CallbackInfoReturnable<Boolean> cir) {
        var to = pos.relative(hopper.getBlockState().getValue(HopperBlock.FACING));
        if (!ClaimTransfers.allowed(level, pos, to)) cir.setReturnValue(false);
    }
    @Inject(method = "suckInItems", at = @At("HEAD"), cancellable = true)
    private static void jugcraft$intake(Level level, Hopper hopper, CallbackInfoReturnable<Boolean> cir) {
        var pos = BlockPos.containing(hopper.getLevelX(), hopper.getLevelY(), hopper.getLevelZ());
        if (!ClaimTransfers.allowed(level, pos, pos.above())) cir.setReturnValue(false);
    }

    @Inject(method = "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/entity/item/ItemEntity;)Z", at = @At("HEAD"), cancellable = true)
    private static void jugcraft$looseItem(net.minecraft.world.Container container, net.minecraft.world.entity.item.ItemEntity item, CallbackInfoReturnable<Boolean> cir) {
        if (container instanceof Hopper hopper) {
            var pos = BlockPos.containing(hopper.getLevelX(), hopper.getLevelY(), hopper.getLevelZ());
            if (!ClaimTransfers.allowed(item.level(), pos, item.blockPosition())) cir.setReturnValue(false);
        }
    }
}
