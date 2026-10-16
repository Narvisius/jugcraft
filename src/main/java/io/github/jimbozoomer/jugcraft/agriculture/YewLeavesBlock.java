package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Evergreen yew: natural exposed needles ripen arils, and ripe supported leaves stop random ticking. */
public final class YewLeavesBlock extends FruitingLeavesBlock {
	public YewLeavesBlock(Properties properties) { super(properties); }
	@Override protected int fruitChance() { return 16; }
	@Override protected ItemStack fruit(RandomSource random) {
		return new ItemStack(JugcraftAgriculture.item("yew_arils"), 1 + random.nextInt(2));
	}
	@Override protected boolean isRandomlyTicking(BlockState state) {
		return !state.getValue(PERSISTENT) && (state.getValue(DISTANCE) == DECAY_DISTANCE || state.getValue(FRUIT) < RIPE);
	}
}
