package io.github.jimbozoomer.jugcraft.rocketry;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A zipline anchor (batch 40, docs/features/zipline.md): a steel post with a pulley on top. Use it with an empty hand
 * to ride its line to the other anchor ({@link ZiplineRider}). Its line lives in {@link ZiplineAnchorBlockEntity}.
 */
public class ZiplineAnchorBlock extends BaseEntityBlock {
	private static final VoxelShape SHAPE = Shapes.or(Block.box(6.0, 0.0, 6.0, 10.0, 16.0, 10.0),
			Block.box(4.0, 0.0, 4.0, 12.0, 1.0, 12.0), Block.box(5.0, 12.0, 5.0, 11.0, 15.0, 11.0));

	public ZiplineAnchorBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ZiplineAnchorBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		if (!(level.getBlockEntity(pos) instanceof ZiplineAnchorBlockEntity anchor) || anchor.link() == null) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.zipline.no_line"));
			return InteractionResult.SUCCESS;
		}
		if (!ZiplineRider.ride(server, pos, player)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.zipline.cannot_ride"));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}
}
