package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.botany.BotanicalDropCooldown;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.storage.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemEntity.class)
public abstract class BotanicalDropCooldownMixin implements BotanicalDropCooldown {
    @Unique private int jugcraft$flowerDelay;
    public int jugcraft$flowerDelay(){return jugcraft$flowerDelay;}
    public void jugcraft$flowerDelay(int ticks){jugcraft$flowerDelay=Math.clamp(ticks,0,200);}
    @Inject(method="tick",at=@At("HEAD")) private void jugcraft$tick(CallbackInfo ci){if(jugcraft$flowerDelay>0)jugcraft$flowerDelay--;}
    @Inject(method="tryToMerge",at=@At("HEAD"),cancellable=true) private void jugcraft$merge(ItemEntity other,CallbackInfo ci){if(jugcraft$flowerDelay!=((BotanicalDropCooldown)other).jugcraft$flowerDelay())ci.cancel();}
    @Inject(method="addAdditionalSaveData",at=@At("TAIL")) private void jugcraft$save(ValueOutput out,CallbackInfo ci){out.putInt("JugcraftFlowerDelay",jugcraft$flowerDelay);}
    @Inject(method="readAdditionalSaveData",at=@At("TAIL")) private void jugcraft$load(ValueInput in,CallbackInfo ci){jugcraft$flowerDelay(in.getIntOr("JugcraftFlowerDelay",0));}
}
