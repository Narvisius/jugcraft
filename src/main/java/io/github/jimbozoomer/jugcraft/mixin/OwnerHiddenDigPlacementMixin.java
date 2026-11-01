package io.github.jimbozoomer.jugcraft.mixin;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Block.class)
public abstract class OwnerHiddenDigPlacementMixin {
    @Inject(method="setPlacedBy",at=@At("TAIL"))
    private void jugcraft$paidHiddenItem(Level level,BlockPos pos,BlockState state,LivingEntity who,ItemStack stack,CallbackInfo ci){
        if(level.isClientSide()||!stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getBooleanOr("OwnerHiddenItem",false)||!(level.getBlockEntity(pos) instanceof BrushableBlockEntity brush))return;
        var contents=NonNullList.withSize(1,ItemStack.EMPTY);stack.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).copyInto(contents);brush.setLootTable(null,0);((OwnerLootBrushAccess)brush).jugcraft$hiddenItem(contents.getFirst().copyWithCount(1));brush.setChanged();
    }
}
