package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauScrying;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(ChunkMap.class)
public abstract class NouveauScryChunksMixin {
    @Redirect(method="updateChunkTracking",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerPlayer;chunkPosition()Lnet/minecraft/world/level/ChunkPos;"))
    private ChunkPos nouveau$view(ServerPlayer p){return NouveauScrying.viewChunk(p);}
}
