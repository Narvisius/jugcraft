package io.github.jimbozoomer.jugcraft.tower;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A cell of the Tri-Pillar's atrium dome (tools/atrium_dome.py; Justin's inside-pillars option A, 27 October 2026): the
 * concrete of a pillar foot where the dome's sphere passes through it, {@code index} its place in the dome
 * ({@link DomeShellData}). It looks like the concrete outside the sphere, smooth (client/DomeShellModel, baked into the
 * chunk), and is that solid at a quarter block. Placed by the tower only (no item); it breaks to reinforced concrete.
 */
public class DomeShellBlock extends Block {
	public static final IntegerProperty INDEX = IntegerProperty.create("index", 0, Math.max(1, DomeShellData.get().count - 1));

	public DomeShellBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(INDEX, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(INDEX);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return DomeShellData.get().shape(state.getValue(INDEX));
	}
}
