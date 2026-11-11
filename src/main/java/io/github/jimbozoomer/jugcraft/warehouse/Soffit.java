package io.github.jimbozoomer.jugcraft.warehouse;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * What a warehouse floor tile's slab shows underneath (part W3; tools/warehouse_data.py SOFFITS): nothing on the ground
 * floor (NONE: the tile is a whole block); over another floor, the trapezoidal metal deck with its ribs along x or z,
 * or the deck with a steel girder across it on a column line (along z or x). A tile with a soffit is only its topping
 * over it, so it hides nothing round it, but it lets no light through.
 */
public enum Soffit implements StringRepresentable {
	NONE("none"), DECK_X("deck_x"), DECK_Z("deck_z"), GIRDER_Z("girder_z"), GIRDER_X("girder_x");

	public static final EnumProperty<Soffit> PROPERTY = EnumProperty.create("soffit", Soffit.class);
	private final String name;

	Soffit(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	/** Turned with a blueprint: a quarter turn swaps x and z. */
	public Soffit rotate(Rotation rotation) {
		if (rotation != Rotation.CLOCKWISE_90 && rotation != Rotation.COUNTERCLOCKWISE_90) {
			return this;
		}
		return switch (this) {
			case DECK_X -> DECK_Z;
			case DECK_Z -> DECK_X;
			case GIRDER_Z -> GIRDER_X;
			case GIRDER_X -> GIRDER_Z;
			default -> NONE;
		};
	}
}
