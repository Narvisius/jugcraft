package io.github.jimbozoomer.jugcraft.lair;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Crowfield corn (docs/features/crowfield.md): the standing corn that walls the Pumpkin King's field, three blocks tall
 * and too thick to pass; the top block of each stand is in tassel ({@link #TOP}). A lair block: unbreakable, no item, no
 * drops.
 */
public class CrowfieldCornBlock extends Block {
	public static final BooleanProperty TOP = BooleanProperty.create("top");

	public CrowfieldCornBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(TOP, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(TOP);
	}
}
