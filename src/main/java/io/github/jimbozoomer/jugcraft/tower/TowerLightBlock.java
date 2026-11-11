package io.github.jimbozoomer.jugcraft.tower;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * The tower's ceiling lights (the Ceiling Light Panel and the Sealed Linear Ceiling Light) with a night mode (Justin's
 * night lighting sheet, approved 5 November 2026): {@link #MODE} on (light 15), night (12), low (10) or off (0); the
 * diffuser glows at the mode's light, an unlit diffuser when off (tools/tower_night_lights.py writes the models). A
 * Tri-Pillar's core switches the lamps on its crew floors at night ({@link TowerNightLights}); placed, a lamp is on.
 */
public class TowerLightBlock extends FurnitureBlock {
	public enum Mode implements StringRepresentable {
		ON("on", 15), NIGHT("night", 12), LOW("low", 10), OFF("off", 0);

		private final String name;
		private final int light;

		Mode(String name, int light) {
			this.name = name;
			this.light = light;
		}

		public int light() {
			return light;
		}

		@Override
		public String getSerializedName() {
			return name;
		}

		/** The mode by its name (the layout's "night_lights"), or on. */
		public static Mode of(String name) {
			for (Mode m : values()) {
				if (m.name.equals(name)) {
					return m;
				}
			}
			return ON;
		}
	}

	public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);

	/** @param boxes {x0, y0, z0, x1, y1, z1} in pixels, facing north (FurnitureBlock); the light comes from the mode. */
	public TowerLightBlock(Properties properties, double[]... boxes) {
		super(properties.lightLevel(state -> state.getValue(MODE).light()), boxes);
		registerDefaultState(defaultBlockState().setValue(MODE, Mode.ON));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(MODE);
	}
}
