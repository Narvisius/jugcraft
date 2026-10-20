package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauRituals;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(NaturalSpawner.class)
public abstract class NouveauSanctuaryMixin {
    @Inject(method="isValidPositionForMob",at=@At("HEAD"),cancellable=true)
    private static void jugcraft$sanctuary(ServerLevel level,Mob mob,double distance,CallbackInfoReturnable<Boolean> cir){if(mob instanceof net.minecraft.world.entity.monster.Enemy&&NouveauRituals.deniesNaturalSpawn(level,mob.blockPosition()))cir.setReturnValue(false);}
}
