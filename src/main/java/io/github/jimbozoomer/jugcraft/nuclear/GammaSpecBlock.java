package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The chemistry lab's gamma spectroscopy station (ChemLab; tools/admin_rooms.py chem_gamma_spec; three blocks across,
 * two high, against the wall): the lead cave on its stand over the liquid nitrogen dewar, the bench with the NIM bin and
 * the two screens. A click counts the next sample (the plant's coolant as it stands), or says how far the count is.
 */
public class GammaSpecBlock extends LivePanelBlock {
	public GammaSpecBlock(Properties properties) {
		super(properties, 3, 2, 1);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		Direction facing = state.getValue(FACING);
		int p = state.getValue(partProperty()), c = p % width, r = p / width;
		if (r == 0) {
			return c == 0 ? turned(new double[] {0.8, 0, 1.6, 15.2, 16, 15.2}, facing) : turned(new double[] {0, 0, 0.5, 16, 14.5, 16}, facing);
		}
		return switch (c) {
			case 0 -> turned(new double[] {1.8, 0, 2.2, 14.2, 13.8, 14.6}, facing);
			case 1 -> Shapes.or(turned(new double[] {1.4, 0, 3, 10.6, 3, 10.6}, facing), turned(new double[] {11, 0.6, 3.8, 16, 7.4, 6.6}, facing));
			default -> turned(new double[] {0, 0.6, 3.8, 15.6, 7.4, 6.6}, facing);
		};
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		BlockPos origin = pos.subtract(offset(state.getValue(FACING), state.getValue(partProperty())));
		if (level.getBlockEntity(origin) instanceof LivePanelBlockEntity g) {
			ChemLab.click(g, (ServerLevel) level, player);
		}
		return InteractionResult.SUCCESS_SERVER;
	}
}
