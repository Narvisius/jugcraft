package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * A storage tank plate: the shell and roof of a storage tank MBS ({@link StorageTankPortBlockEntity}). Loose plates
 * and an unfinished tank show grey primer; when the tank's outlet forms the tank it paints every plate by where it
 * is: the white wall, the checker band round the top course, the dark rim at the roof's edge, the roof.
 */
public class StorageTankPlateBlock extends Block {
	public enum Band implements StringRepresentable {
		WALL("wall"), BAND("band"), RIM("rim"), ROOF("roof");

		private final String name;

		Band(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public static final BooleanProperty FORMED = BooleanProperty.create("formed");
	public static final EnumProperty<Band> BAND = EnumProperty.create("band", Band.class);

	public StorageTankPlateBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FORMED, false).setValue(BAND, Band.WALL));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FORMED, BAND);
	}
}
