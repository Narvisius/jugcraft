package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Evergreen cultivated olive. Wild olive foliage is a separate, ordinary non-fruiting leaf block. */
public final class OliveLeavesBlock extends FruitingLeavesBlock {
	public OliveLeavesBlock(Properties properties) { super(properties); }
	@Override protected int fruitChance() { return 10; }
	@Override protected ItemStack fruit(RandomSource random) {
		return new ItemStack(JugcraftAgriculture.item("olives"), 1 + random.nextInt(2));
	}
	@Override public boolean pick(Level level, BlockPos pos) {
		var state = level.getBlockState(pos);
		return !level.isClientSide() && state.is(this) && canFruit(state, level, pos) && super.pick(level, pos);
	}
}
