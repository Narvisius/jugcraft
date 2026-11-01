package io.github.jimbozoomer.jugcraft.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.jimbozoomer.jugcraft.menagerie.OwnerFollowingPets;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.*;
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class OwnerFollowingPetsMixin {
    @Shadow public ServerPlayer player;
    @WrapMethod(method="teleport(Lnet/minecraft/world/entity/PositionMoveRotation;Ljava/util/Set;)V")
    private void jugcraft$bringFollowingPets(PositionMoveRotation target,Set<Relative> relative,Operation<Void> original){
        var nearby=OwnerFollowingPets.before(player);original.call(target,relative);OwnerFollowingPets.after(player,nearby);
    }
}
