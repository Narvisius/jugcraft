package io.github.jimbozoomer.jugcraft.tower;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * An Inset Floor Light (Justin's walk through, redesign 2: "a solid light", round, approved): the floor's own block with
 * a brushed steel square trim plate flush in its top and a round frosted lens a hair down in it, glowing (the neon
 * lines' light); the tower has one wherever its layout had a floor line on its own (tools/neon_lines.shape).
 * {@link Phased}: one of a TowerPanelBlock material (concrete, steel), its texture's quadrant by position as theirs.
 */
public class InsetFloorLightBlock extends Block {
	public InsetFloorLightBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return TowerPanelBlock.at(defaultBlockState(), context.getClickedPos());
	}

	/** An inset light of a phased material. */
	public static final class Phased extends InsetFloorLightBlock {
		public Phased(Properties properties) {
			super(properties);
			registerDefaultState(defaultBlockState().setValue(TowerPanelBlock.PHASE, 0));
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
			super.createBlockStateDefinition(builder);
			builder.add(TowerPanelBlock.PHASE);
		}
	}
}
