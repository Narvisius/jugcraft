package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Evergreen fruit and flower harvests; low shrubs may bear on exposed sides. */
public final class TropicalFruitLeavesBlock extends FruitingLeavesBlock {
    private final String harvest;
    private final boolean edgeFruiting;

    public TropicalFruitLeavesBlock(String harvest, boolean edgeFruiting, Properties properties) {
        super(properties);
        this.harvest = harvest;
        this.edgeFruiting = edgeFruiting;
    }

    @Override protected int fruitChance() { return 10; }
    @Override protected ItemStack fruit(RandomSource random) {
        return new ItemStack(JugcraftAgriculture.item(harvest), 1 + random.nextInt(2));
    }
    private BlockPos dropSpace(Level level, BlockPos pos) {
        if (level.isEmptyBlock(pos.below())) return pos.below();
        if (edgeFruiting) for (var direction : Direction.Plane.HORIZONTAL) {
            BlockPos side = pos.relative(direction);
            if (level.isEmptyBlock(side)) return side;
        }
        return pos;
    }
    @Override protected boolean hasFruitSpace(Level level, BlockPos pos) { return !dropSpace(level, pos).equals(pos); }
    @Override protected BlockPos fruitDropPos(Level level, BlockPos pos) { return dropSpace(level, pos); }
    @Override public boolean pick(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return !level.isClientSide() && state.is(this) && canFruit(state, level, pos) && super.pick(level, pos);
    }
}
