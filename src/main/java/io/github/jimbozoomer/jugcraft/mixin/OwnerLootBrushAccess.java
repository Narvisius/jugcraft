package io.github.jimbozoomer.jugcraft.mixin;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(BrushableBlockEntity.class)
public interface OwnerLootBrushAccess {
    @Accessor("item") void jugcraft$hiddenItem(net.minecraft.world.item.ItemStack stack);
    @Accessor("lootTable") ResourceKey<LootTable> jugcraft$lootTable();
    @Accessor("lootTableSeed") long jugcraft$lootSeed();
}
