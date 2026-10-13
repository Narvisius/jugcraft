package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * The leaves of an orchard's fruit tree ({@link OrchardTree}; {@link FruitingLeavesBlock}, as the apple tree's): they
 * blossom ({@link #FRUIT} 1) and then hang with ripe fruit ({@link #FRUIT} 2), one step in {@link #FRUIT_CHANCE} random
 * ticks, about a Minecraft day in all. A right-click picks the ripe fruit: {@link OrchardTree#pickMin} to
 * {@link OrchardTree#pickMax} drop below. Broken, they drop their seed now and then, sticks, and the ripe fruit.
 */
public class OrchardLeavesBlock extends FruitingLeavesBlock {
	/** One fruit stage in this many random ticks, as the apple tree's. Keep in sync with tools/orchard.py. */
	public static final int FRUIT_CHANCE = 10;

	private final OrchardTree tree;

	public OrchardLeavesBlock(OrchardTree tree, Properties properties) {
		super(properties);
		this.tree = tree;
	}

	public OrchardTree tree() {
		return tree;
	}

	@Override
	protected int fruitChance() {
		return FRUIT_CHANCE;
	}

	@Override
	protected ItemStack fruit(RandomSource random) {
		return new ItemStack(JugcraftAgriculture.item(tree.id), tree.pickMin + random.nextInt(tree.pickMax - tree.pickMin + 1));
	}
}
