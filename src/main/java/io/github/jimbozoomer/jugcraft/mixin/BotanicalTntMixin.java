package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.botany.BotanicalExplosives;
import net.minecraft.world.entity.item.PrimedTnt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(PrimedTnt.class)
public abstract class BotanicalTntMixin {
    @Inject(method="tick",at=@At("HEAD"))
    private void jugcraft$inspectPriming(CallbackInfo ci){BotanicalExplosives.inspect((PrimedTnt)(Object)this);}
}
