package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.menagerie.OwnerSliceMaps;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.*;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MapItem.class)
public abstract class OwnerSliceMapMixin {
 @Inject(method="inventoryTick",at=@At("HEAD"),cancellable=true)
 private void jugcraft$slice(ItemStack stack,ServerLevel level,Entity carrier,EquipmentSlot slot,CallbackInfo ci){if(OwnerSliceMaps.tick(stack,level,carrier,slot))ci.cancel();}
}
