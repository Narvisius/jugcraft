package io.github.jimbozoomer.jugcraft.concordance.assay;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.UUID;
import net.minecraft.server.level.*;
import org.jspecify.annotations.Nullable;

/** Loaded factory blocks may use their own buffers offline. This actor only answers protection checks. */
final class AlchemyOwner {
    private AlchemyOwner(){}
    static @Nullable ServerPlayer factory(ServerLevel level,@Nullable UUID owner,boolean authorized){
        var here=Authority.present(level,owner);if(here!=null||owner==null||!authorized)return here;
        var online=level.getServer().getPlayerList().getPlayer(owner);
        if(online!=null&&(!online.isAlive()||online.isSpectator()))return null;
        return Authority.loadedAlchemyFactory(level,owner,authorized);
    }
}
