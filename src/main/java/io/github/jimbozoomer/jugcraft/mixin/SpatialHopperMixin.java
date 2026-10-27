package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.jimtafacts.network.SpatialReservation;
import net.minecraft.core.*;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public abstract class SpatialHopperMixin {
    @Inject(method="getContainerAt(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;DDD)Lnet/minecraft/world/Container;",at=@At("HEAD"),cancellable=true)
    private static void jugcraft$hideReservedStorage(Level level,BlockPos pos,BlockState state,double x,double y,double z,CallbackInfoReturnable<Container> result){if(SpatialReservation.reserved(level,pos))result.setReturnValue(null);}
    @Inject(method="addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;",at=@At("HEAD"),cancellable=true)
    private static void jugcraft$holdReservedTransfer(Container from,Container to,ItemStack stack,Direction side,CallbackInfoReturnable<ItemStack> result){if(reserved(from)||reserved(to))result.setReturnValue(stack);}
    private static boolean reserved(Container container){return SpatialReservation.containerReserved(container);}
}
