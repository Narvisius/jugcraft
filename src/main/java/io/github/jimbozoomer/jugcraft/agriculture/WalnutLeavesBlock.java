package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.menagerie.Menagerie;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Both saved walnut leaf IDs share one seasonal nut and husk harvest. */
public final class WalnutLeavesBlock extends SeasonalFruitLeavesBlock {
    public static final SeasonalLeavesBlock.Schedule SCHEDULE = JugcraftAgriculture.WALNUT_LEAVES;
    public WalnutLeavesBlock(Properties properties) { super(SCHEDULE, "ecology_walnut", properties); }
    @Override protected ItemStack fruit(RandomSource random) {
        return new ItemStack(Menagerie.item("ecology_walnut"), 1 + random.nextInt(2));
    }
    @Override public boolean pick(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (!state.is(this) || !canFruit(state, level, pos)
                || state.getValue(SeasonalLeavesBlock.SEASON) == SeasonalLeavesBlock.Foliage.BARE) return false;
        if (!super.pick(level, pos)) return false;
        Block.popResource(level, fruitDropPos(level, pos), new ItemStack(JugcraftAgriculture.item("walnut_husk")));
        return true;
    }
    @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
        var now = level.getBlockState(pos);
        if (now.is(this) && now.getValue(SeasonalLeavesBlock.SEASON) == SeasonalLeavesBlock.Foliage.BARE
                && now.getValue(FRUIT) != 0) level.setBlock(pos, now.setValue(FRUIT, 0), Block.UPDATE_CLIENTS);
    }
}
