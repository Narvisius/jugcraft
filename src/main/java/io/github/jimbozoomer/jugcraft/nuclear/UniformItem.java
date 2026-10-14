package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.world.item.Item;

/**
 * Work clothes you can wear (chest slot) and hang back in a locker: the lab coat and the security jacket. Worn, they
 * are drawn in 3D on the wearer by client/UniformLayer.java; hung up, client/LockerRenderer.java draws the same coat
 * on its hanger in the locker.
 */
public class UniformItem extends Item {
	public enum Kind {
		LAB_COAT, SECURITY_JACKET
	}

	public final Kind kind;

	public UniformItem(Properties properties, Kind kind) {
		super(properties);
		this.kind = kind;
	}
}
