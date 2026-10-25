package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaSpellCreature;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Source root restraints end when the breakable roots die, expire or lose authority. */
@Mixin(Entity.class)
public abstract class JimtaRootDismountMixin {
    @Inject(method={"stopRiding","removeVehicle"},at=@At("HEAD"),cancellable=true)
    private void jimtafacts$rootRestraint(CallbackInfo info){
        Entity self=(Entity)(Object)this;
        if(self.isAlive()&&self.getVehicle() instanceof JimtaSpellCreature root&&root.restraining())info.cancel();
    }
}
