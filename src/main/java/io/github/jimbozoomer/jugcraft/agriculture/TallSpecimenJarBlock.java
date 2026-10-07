package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * The Tall Specimen Jar: the Specimen Jar made {@value #SCALE} times bigger, one block across and two tall
 * ({@link SpecimenVesselBlock}), glowing at light {@value #LIGHT}.
 */
public class TallSpecimenJarBlock extends SpecimenVesselBlock {
	public static final int SCALE = 2;
	public static final int LIGHT = 8;
	private static final int[][] CELLS = box(1, 2, 1);
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, CELLS.length - 1);

	public TallSpecimenJarBlock(Properties properties) {
		super(properties, SCALE, CELLS);
	}

	@Override
	public int[][] cells() {
		return CELLS;
	}

	@Override
	public IntegerProperty partProperty() {
		return PART;
	}
}
