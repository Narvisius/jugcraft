package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Animal.class)
public abstract class NouveauBreedingMixin {
    @Inject(method="finalizeSpawnChildFromBreeding",at=@At("RETURN"))
    private void jugcraft$breedingSource(ServerLevel level,Animal mate,AgeableMob child,CallbackInfo ci){if(child!=null)NouveauSource.breeding(level,child.blockPosition());}
}
