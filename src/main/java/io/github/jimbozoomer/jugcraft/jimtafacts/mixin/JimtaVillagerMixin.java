package io.github.jimbozoomer.jugcraft.jimtafacts.mixin;
import io.github.jimbozoomer.jugcraft.jimtafacts.JimtaAbilities;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(Villager.class)
public abstract class JimtaVillagerMixin {
    @ModifyReturnValue(method="getPlayerReputation",at=@At("RETURN"))
    private int jimta$friendlyHat(int original,Player player){return original+(JimtaAbilities.wearing(player,"villager_hat")?75:0);}
}
