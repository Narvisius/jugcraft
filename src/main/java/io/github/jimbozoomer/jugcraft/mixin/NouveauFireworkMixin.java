package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.nouveau.NouveauFireworks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireworkRocketEntity.class)
public abstract class NouveauFireworkMixin {
    @Inject(method="dealExplosionDamage",at=@At("HEAD"),cancellable=true)
    private void jugcraft$spellDamage(ServerLevel level,CallbackInfo ci){
        if(NouveauFireworks.explode((FireworkRocketEntity)(Object)this))ci.cancel();
    }
}
