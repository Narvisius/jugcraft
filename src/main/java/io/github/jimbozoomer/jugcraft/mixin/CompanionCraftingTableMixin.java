package io.github.jimbozoomer.jugcraft.mixin;

import local.peepo.CraftingTableCompanionBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

/** Adds saved companion state without replacing the vanilla crafting menu or registering a ticker. */
@Mixin(CraftingTableBlock.class)
public abstract class CompanionCraftingTableMixin implements EntityBlock {
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){
        // SmithingTableBlock inherits CraftingTableBlock, but has no companion inventory.
        // The registered block-entity type supports only the ordinary crafting table.
        return state.is(Blocks.CRAFTING_TABLE) ? new CraftingTableCompanionBlockEntity(pos,state) : null;
    }
}
