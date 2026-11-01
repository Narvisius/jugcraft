package io.github.jimbozoomer.jugcraft.mixin;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Spawner, egg and split offspring cannot be recycled into natural-spawn mana. */
@Mixin(Mob.class)
public abstract class BotanicalNaturalSlimeMixin {
    @Inject(method="finalizeSpawn",at=@At("RETURN"))
    private void jugcraft$natural(ServerLevelAccessor level,DifficultyInstance difficulty,EntitySpawnReason reason,SpawnGroupData data,CallbackInfoReturnable<SpawnGroupData> cir){
        if((Object)this instanceof Slime slime&&reason==EntitySpawnReason.NATURAL)slime.addTag("jugcraft:botanical_natural_slime");
    }
}
