package io.github.jimbozoomer.jugcraft.lair;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** The lair's harvest moon, a disc of these hung beyond the island: pale gold, or red when the fight turns it. */
public class LairMoonBlock extends Block {
	public static final BooleanProperty RED = BooleanProperty.create("red");

	public LairMoonBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(RED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(RED);
	}
}
