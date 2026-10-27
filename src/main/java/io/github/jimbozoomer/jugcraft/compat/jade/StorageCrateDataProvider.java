package io.github.jimbozoomer.jugcraft.compat.jade;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.storage.StorageCrateBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import snownee.jade.api.*;

/** Optional inspection data: never expose a protected inventory's contents. */
public enum StorageCrateDataProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;
    public static final Identifier ID = Jugcraft.id("storage_crate");
    @Override public Identifier getUid() { return ID; }
    @Override public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof StorageCrateBlockEntity crate && crate.stillValid(accessor.getPlayer())) {
            data.putInt("crate_used", crate.occupiedSlots());
            data.putInt("crate_capacity", crate.getContainerSize());
        }
    }
}
