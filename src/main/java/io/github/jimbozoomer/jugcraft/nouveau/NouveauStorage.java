package io.github.jimbozoomer.jugcraft.nouveau;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.*;
import org.jspecify.annotations.Nullable;

/** Shared live-storage boundary for the owner's library helpers. Never loads chunks or opens private machine slots. */
public final class NouveauStorage {
    private NouveauStorage() {}
    public static @Nullable Container accessible(ServerLevel level, ServerPlayer player, BlockPos pos) {
        if (!level.isLoaded(pos) || player.isSpectator() || !player.isAlive() || !Authority.mayChange(level, player, pos)) return null;
        var entity = level.getBlockEntity(pos);
        if(entity instanceof NouveauWorkshop workshop)return workshop.storage()&&workshop.permitted(player)?workshop:null;
        if (!(entity instanceof ChestBlockEntity) && !(entity instanceof BarrelBlockEntity)) return null;
        var state = entity.getBlockState();
        if (!(entity instanceof NouveauChest.Chest) && !state.is(Blocks.CHEST) && !state.is(Blocks.TRAPPED_CHEST) && !state.is(Blocks.BARREL)) return null;
        var chest = (BaseContainerBlockEntity)entity;
        return chest.isLocked() ? null : chest;
    }
}
