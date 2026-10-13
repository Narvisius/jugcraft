package io.github.jimbozoomer.jugcraft.lair;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** Thread wound on one of the Spindle Loft's giant spools, in the spool's colour. */
public class SpoolThreadBlock extends Block {
	public static final EnumProperty<ThreadColour> COLOUR = EnumProperty.create("colour", ThreadColour.class);

	public SpoolThreadBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(COLOUR, ThreadColour.WHITE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(COLOUR);
	}
}
