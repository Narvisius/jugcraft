package io.github.jimbozoomer.jugcraft.mixin;

import java.util.UUID;
import net.minecraft.world.entity.item.ItemEntity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read vanilla's private pickup reservation so attraction cannot move someone else's reserved loot. */
@Mixin(ItemEntity.class)
public interface AlchemicalItemAccess {
    @Accessor("target") @Nullable UUID jugcraft$pickupTarget();
}
