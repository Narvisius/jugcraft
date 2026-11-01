package io.github.jimbozoomer.jugcraft.mixin;
import java.util.List;
import io.github.jimbozoomer.jugcraft.enchanting.OwnerEnchantmentMining;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Block.class)
public abstract class OwnerEnchantmentDropsMixin {
    @Inject(method="getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemInstance;)Ljava/util/List;",at=@At("RETURN"),cancellable=true)
    private static void jugcraft$reforged(BlockState state,ServerLevel level,BlockPos pos,BlockEntity be,Entity entity,ItemInstance tool,CallbackInfoReturnable<List<ItemStack>> cir){cir.setReturnValue(io.github.jimbozoomer.jugcraft.arcane.ArcaneSources.yields(state,level,pos,entity,tool,OwnerEnchantmentMining.drops(state,level,pos,entity,tool,cir.getReturnValue())));}
}
