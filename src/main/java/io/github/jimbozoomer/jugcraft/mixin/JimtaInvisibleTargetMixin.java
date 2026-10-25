package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaWardSpells;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Mob.class)
public abstract class JimtaInvisibleTargetMixin {
    @Inject(method="setTarget",at=@At("HEAD"),cancellable=true)
    private void jimta$concealedTarget(LivingEntity target,CallbackInfo info){if(target!=null&&JimtaWardSpells.has(target,"true_invisibility"))info.cancel();}
}
