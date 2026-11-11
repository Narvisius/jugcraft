package io.github.jimbozoomer.jugcraft.warehouse;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Blocks never placed, whose models the client draws moving things with (as the plant doors' leaves are drawn):
 * warehouse_part (PART: the drones' parts, a case on its own, the film), warehouse_load (LOAD: a pallet and its load,
 * as a rack's LOAD). They have no item.
 */
public class HiddenBlock extends Block {
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, 63);
	public static final IntegerProperty LOAD = RackBlock.LOAD;
	private static IntegerProperty building;
	private final IntegerProperty property;

	private static Properties prepare(Properties properties, IntegerProperty property) {
		building = property;
		return properties;
	}

	public HiddenBlock(IntegerProperty property, Properties properties) {
		super(prepare(properties, property));
		this.property = property;
		registerDefaultState(stateDefinition.any().setValue(property, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(building);
	}

	public BlockState with(int value) {
		return defaultBlockState().setValue(property, value);
	}
}
