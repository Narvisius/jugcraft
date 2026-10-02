package io.github.jimbozoomer.jugcraft.tower;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * A hangar's landing pad plate. Laid by the tower, the plates of one hangar floor each show their own part of one
 * big pad (hazard rim, edge lights, touchdown marks, a charging port in the middle), the way the ground landing
 * pads join up: {@link #PART} picks which piece of which layout (tools/drone_tower.py, tools/tower_art.py).
 * Part 0 is a loose plate, as a player places it.
 */
public class HangarPadBlock extends Block {
	/** 0 loose; 1-9 small hangar (3x3); 10-58 large (7x7); 59-82 medium along x (6x4); 83-106 medium along z (4x6). */
	public static final int PARTS = 107;
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, PARTS - 1);

	public HangarPadBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(PART, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(PART);
	}
}
