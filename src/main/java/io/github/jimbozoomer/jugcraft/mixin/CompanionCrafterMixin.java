package io.github.jimbozoomer.jugcraft.mixin;
import local.peepo.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(CrafterBlock.class)
public abstract class CompanionCrafterMixin {
 @Inject(method="dispenseFrom",at=@At("HEAD"),cancellable=true) private void peepo$capacity(BlockState state,ServerLevel level,BlockPos pos,CallbackInfo ci){if(level.getBlockEntity(pos) instanceof VanillaCompanionAccess access){var port=access.peepo$existingPort();if(port!=null && !port.canCraft())ci.cancel();}}
 @Inject(method="dispenseItem",at=@At("HEAD"),cancellable=true) private void peepo$output(ServerLevel level,BlockPos pos,CrafterBlockEntity crafter,ItemStack item,BlockState state,RecipeHolder<?> recipe,CallbackInfo ci){var port=((VanillaCompanionAccess)crafter).peepo$existingPort();if(port!=null && port.capture(item))ci.cancel();}
}
