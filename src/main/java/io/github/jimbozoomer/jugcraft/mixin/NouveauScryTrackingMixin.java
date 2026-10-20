package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauScrying;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(targets="net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class NouveauScryTrackingMixin {
    @Redirect(method="updatePlayer",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerPlayer;position()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 nouveau$view(ServerPlayer p){return NouveauScrying.viewPosition(p);}
}
