package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.menagerie.OwnerQuiver;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ProjectileWeaponItem.class)
public abstract class OwnerQuiverDrawMixin {
 @Inject(method="draw",at=@At("HEAD"),cancellable=true)
 private static void jugcraft$start(ItemStack weapon,ItemStack ammo,LivingEntity shooter,CallbackInfoReturnable<List<ItemStack>> ci){if(!OwnerQuiver.beginDraw(ammo,shooter)){OwnerQuiver.clearSelection();ci.setReturnValue(List.of());}}
 @Inject(method="draw",at=@At("RETURN"))
 private static void jugcraft$finish(ItemStack weapon,ItemStack ammo,LivingEntity shooter,CallbackInfoReturnable<List<ItemStack>> ci){OwnerQuiver.finishDraw(ammo,shooter);}
}
