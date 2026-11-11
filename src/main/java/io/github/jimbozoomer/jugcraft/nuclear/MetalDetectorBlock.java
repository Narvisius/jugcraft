package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
 * The gatehouse's walk-through metal detector (SecurityGatehouse; one block wide, two high): you walk between its posts
 * (only they stop you); its head over them. Right-click it for how many it has passed.
 */
public class MetalDetectorBlock extends LivePanelBlock {
	static final double[] LEFT = {0.4, 0, 3.5, 2.4, 16, 12.5}, RIGHT = {13.6, 0, 3.5, 15.6, 16, 12.5};

	public MetalDetectorBlock(Properties properties) {
		super(properties, 1, 2, 1);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		Direction facing = state.getValue(FACING);
		VoxelShape posts = Shapes.or(turned(LEFT, facing), turned(RIGHT, facing));
		return state.getValue(partProperty()) == 0 ? posts : Shapes.or(posts, turned(new double[] {0.4, 13, 3, 15.6, 16, 13}, facing));
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		Direction facing = state.getValue(FACING);
		return Shapes.or(turned(LEFT, facing), turned(RIGHT, facing));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		BlockPos origin = pos.subtract(offset(state.getValue(FACING), state.getValue(partProperty())));
		if (level.getBlockEntity(origin) instanceof LivePanelBlockEntity d) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.wd_status", d.wdCount));
		}
		return InteractionResult.SUCCESS_SERVER;
	}
}
