package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * The entrance hall's security post (older rooms round 6: AccessControl; three blocks wide, two high). Its hitbox: the
 * counter, its ledge and glass to the hall, the officer's desk with his screens and the gate panel. A click on the
 * duress button sets off the alarm; anywhere else says who is inside and the last passage.
 */
public class AcStationBlock extends LivePanelBlock {
	public AcStationBlock(Properties properties) {
		super(properties, 3, 2, 1);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		Direction facing = state.getValue(FACING);
		if (state.getValue(partProperty()) / width == 0) {
			return Shapes.or(turned(new double[] {0, 0, 0, 16, 16, 4.6}, facing), turned(new double[] {0, 0, 4.6, 16, 12.2, 15.4}, facing),
					turned(new double[] {0, 12.2, 4.6, 16, 16, 8.4}, facing));
		}
		return Shapes.or(turned(new double[] {0, 0, 0, 16, 16, 2.6}, facing), turned(new double[] {0, 0, 4.6, 16, 3.4, 8.4}, facing));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		Direction facing = state.getValue(FACING);
		BlockPos origin = pos.subtract(offset(facing, state.getValue(partProperty())));
		if (!(level.getBlockEntity(origin) instanceof LivePanelBlockEntity post)) {
			return InteractionResult.PASS;
		}
		double[] at = local(facing, origin, hit.getLocation());
		double x = at[0] + 16 * (origin() % width);
		if (at[2] > 6.0 && AccessControl.duressAt(x, at[1])) {
			level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.6F, 0.8F);
			AccessControl.duress((ServerLevel) level, post, player);
			return InteractionResult.SUCCESS_SERVER;
		}
		player.sendOverlayMessage(AccessControl.status(post));
		return InteractionResult.SUCCESS_SERVER;
	}
}
