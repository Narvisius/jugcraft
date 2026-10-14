package io.github.jimbozoomer.jugcraft.mixin;
import local.peepo.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(CampfireBlockEntity.class)
public abstract class CompanionCampfireMixin {
 @Redirect(method="cookTick",at=@At(value="INVOKE",target="Lnet/minecraft/world/Containers;dropItemStack(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V"))
 private static void peepo$capture(Level dropLevel,double x,double y,double z,ItemStack item,ServerLevel level,BlockPos pos,BlockState state,CampfireBlockEntity fire,RecipeManager.CachedCheck<SingleRecipeInput,CampfireCookingRecipe> recipes){
  var port=((VanillaCompanionAccess)fire).peepo$existingPort();if(port==null || !port.capture(item))Containers.dropItemStack(dropLevel,x,y,z,item);
 }
 @Inject(method="preRemoveSideEffects",at=@At("HEAD")) private void peepo$drop(BlockPos pos,BlockState state,CallbackInfo ci){var port=((VanillaCompanionAccess)this).peepo$existingPort();if(port!=null)port.drop();}
}
