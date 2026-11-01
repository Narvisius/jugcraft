package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.botany.BotanicalInfluence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The ordinary persisted entity tag keeps Tigerseye pacification through chunk/server reloads. */
@Mixin(Creeper.class)
public abstract class BotanicalCreeperMixin implements io.github.jimbozoomer.jugcraft.botany.BotanicalLoonium.Charge {
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final private static net.minecraft.network.syncher.EntityDataAccessor<Boolean> DATA_IS_POWERED;
    @Override public void jugcraft$looniumCharged(){((Creeper)(Object)this).getEntityData().set(DATA_IS_POWERED,true);}
    @Inject(method="registerGoals",at=@At("TAIL"))
    private void jugcraft$avoidPlayers(CallbackInfo ci){
        var creeper=(Creeper)(Object)this;
        creeper.getGoalSelector().addGoal(2,new AvoidEntityGoal<Player>(creeper,Player.class,6,1,1.2){
            @Override public boolean canUse(){return creeper.entityTags().contains(BotanicalInfluence.PACIFIED)&&super.canUse();}
        });
    }
    @Inject(method="setTarget",at=@At("HEAD"),cancellable=true)
    private void jugcraft$pacifiedTarget(LivingEntity target,CallbackInfo ci){
        if(target instanceof Player&&((Creeper)(Object)this).entityTags().contains(BotanicalInfluence.PACIFIED))ci.cancel();
    }
}
