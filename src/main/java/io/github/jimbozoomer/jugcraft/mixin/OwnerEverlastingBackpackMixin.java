package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.backpack.OwnerBackpacks;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Preserve the existing dropped item and its contents, rather than creating a replacement entity. */
@Mixin(ItemEntity.class)
public abstract class OwnerEverlastingBackpackMixin {
    private boolean jugcraft$everlasting(){var self=(ItemEntity)(Object)this;return OwnerBackpacks.tier(self.getItem())>=0&&OwnerBackpacks.has(self.getItem(),"everlasting_upgrade");}
    @Inject(method="tick",at=@At("HEAD"))
    private void jugcraft$keep(CallbackInfo ci){
        var self=(ItemEntity)(Object)this;if(self.level().isClientSide()||!jugcraft$everlasting())return;
        self.setUnlimitedLifetime();self.clearFire();
        int floor=self.level().getMinY()+1;
        if(self.getY()<floor){self.setPos(self.getX(),floor,self.getZ());self.setDeltaMovement(0,.12,0);}
        else if(self.isInWater()||self.isInLava())self.setDeltaMovement(self.getDeltaMovement().x,.12,self.getDeltaMovement().z);
    }
    @Inject(method="playerTouch",at=@At("HEAD"),cancellable=true)
    private void jugcraft$adminPickup(net.minecraft.world.entity.player.Player player,CallbackInfo ci){var self=(ItemEntity)(Object)this;if(!self.level().isClientSide()&&OwnerBackpacks.tier(self.getItem())>=0&&!io.github.jimbozoomer.jugcraft.backpack.OwnerBackpackInfinity.mayEdit(player,self.getItem()))ci.cancel();}
    @Inject(method="hurtServer",at=@At("HEAD"),cancellable=true)
    private void jugcraft$protect(net.minecraft.server.level.ServerLevel level,net.minecraft.world.damagesource.DamageSource source,float amount,CallbackInfoReturnable<Boolean> cir){if(jugcraft$everlasting())cir.setReturnValue(false);}
}
