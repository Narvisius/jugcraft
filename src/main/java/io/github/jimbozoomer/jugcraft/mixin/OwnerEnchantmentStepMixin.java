package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.enchanting.OwnerEnchantments;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LevelAccessor.class)
public interface OwnerEnchantmentStepMixin {
    @Inject(method="gameEvent(Lnet/minecraft/core/Holder;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/gameevent/GameEvent$Context;)V",at=@At("HEAD"),cancellable=true)
    default void jugcraft$featherstep(Holder<GameEvent> event,BlockPos pos,GameEvent.Context context,CallbackInfo ci){
        if(event.equals(GameEvent.STEP)&&context.sourceEntity() instanceof ServerPlayer p&&p.getRandom().nextFloat()<Math.min(.75,OwnerEnchantments.level(p,p.getItemBySlot(EquipmentSlot.FEET),"featherstep")))ci.cancel();
    }
}
