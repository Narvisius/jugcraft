package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * The Specimen Tank: the Specimen Jar made {@value #SCALE} times bigger, three blocks wide, tall and deep
 * ({@link SpecimenVesselBlock}), glowing at light {@value #LIGHT}.
 */
public class SpecimenTankBlock extends SpecimenVesselBlock {
	public static final int SCALE = 3;
	public static final int LIGHT = 10;
	private static final int[][] CELLS = box(3, 3, 3);
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, CELLS.length - 1);

	public SpecimenTankBlock(Properties properties) {
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
