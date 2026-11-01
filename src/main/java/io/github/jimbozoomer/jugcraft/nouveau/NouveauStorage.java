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
        return accessible(level,player,pos,false);
    }
    public static @Nullable Container accessibleRemote(ServerLevel level,ServerPlayer player,BlockPos pos){return accessible(level,player,pos,true);}
    private static @Nullable Container accessible(ServerLevel level,ServerPlayer player,BlockPos pos,boolean remote){
        if (!level.isLoaded(pos) || player.isSpectator() || !player.isAlive() || !Authority.mayChange(level, player, pos)) return null;
        var entity = level.getBlockEntity(pos);
        if(entity instanceof NouveauWorkshop workshop)return workshop.storage()&&(remote?workshop.belongs(player.getUUID()):workshop.permitted(player))?workshop:null;
        if (!(entity instanceof ChestBlockEntity) && !(entity instanceof BarrelBlockEntity)) return null;
        var state = entity.getBlockState();
        if (!(entity instanceof NouveauChest.Chest) && !state.is(Blocks.CHEST) && !state.is(Blocks.TRAPPED_CHEST) && !state.is(Blocks.BARREL)) return null;
        var chest = (BaseContainerBlockEntity)entity;
        return chest.isLocked()||chest instanceof net.minecraft.world.RandomizableContainer random&&random.getLootTable()!=null ? null : chest;
    }
}
