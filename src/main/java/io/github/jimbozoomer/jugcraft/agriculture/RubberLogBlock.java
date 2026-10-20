package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Only tree-generated logs carry living tissue; collected/replaced logs cannot produce latex. */
public final class RubberLogBlock extends StrippableLogBlock {
    public static final BooleanProperty NATURAL = BooleanProperty.create("natural");
    public RubberLogBlock(Properties properties) {
        super(properties, "unused");
        registerDefaultState(defaultBlockState().setValue(NATURAL, false));
    }
    @Override public Block stripped() { return Blocks.STRIPPED_JUNGLE_LOG; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(NATURAL);
    }
}
