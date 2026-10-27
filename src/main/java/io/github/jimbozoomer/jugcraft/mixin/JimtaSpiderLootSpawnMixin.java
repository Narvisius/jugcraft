package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaSpiderMealAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class JimtaSpiderLootSpawnMixin {
    @Inject(method="spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/entity/item/ItemEntity;",at=@At("HEAD"),cancellable=true)
    private void jugcraft$consumePreyFood(ServerLevel level,ItemStack stack,Vec3 offset,CallbackInfoReturnable<ItemEntity> cir){
        if((Object)this instanceof JimtaSpiderMealAccess access&&access.jugcraft$isSpiderMeal()&&stack.has(DataComponents.FOOD))cir.setReturnValue(null);
    }
}
