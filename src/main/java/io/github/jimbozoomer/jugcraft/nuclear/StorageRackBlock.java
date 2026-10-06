package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A storage rack: pallet racking three bays long and three levels high, a block deep, placed and broken as one. It is
 * a store of 54 slots (six to each bay, the bays filled bottom row first, left to right): right-click any part to
 * open it. What is in each bay shows on its pallet (client/StorageRackRenderer.java). The bottom middle block holds
 * the store.
 */
public class StorageRackBlock extends MultiFurnitureBlock implements EntityBlock {
	public StorageRackBlock(Properties properties) {
		super(properties, 3, 3, true);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(partProperty()) == origin() ? new StorageRackBlockEntity(pos, state) : null;
	}

	/** The block holding the store, from any part. */
	public BlockPos originOf(BlockState state, BlockPos pos) {
		Direction facing = state.getValue(FACING);
		return pos.subtract(offset(facing, state.getValue(partProperty())));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(originOf(state, pos)) instanceof StorageRackBlockEntity rack) {
			player.openMenu(rack);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.block();
	}
}
