package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(BrewingStandBlockEntity.class)
public abstract class NouveauBrewingMixin {
    @Inject(method="doBrew",at=@At("RETURN"))
    private static void jugcraft$brewingSource(ServerLevel level,BlockPos pos,BrewingStandBlockEntity stand,CallbackInfo ci){int doses=0;for(int i=0;i<3;i++)if(!stand.getItem(i).isEmpty())doses++;if(doses>0)NouveauSource.brewed(level,pos,doses);}
}
