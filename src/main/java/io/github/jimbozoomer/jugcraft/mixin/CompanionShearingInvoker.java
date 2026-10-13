package io.github.jimbozoomer.jugcraft.mixin;

import java.util.function.BiConsumer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Collect the normal shearing loot into transactional cargo instead of spawning loose items. */
@Mixin(LivingEntity.class)
public interface CompanionShearingInvoker {
    @Invoker("dropFromShearingLootTable")
    void jugcraft$collectShearing(ServerLevel level, ResourceKey<LootTable> table, ItemInstance tool, BiConsumer<ServerLevel,ItemStack> output);
}
