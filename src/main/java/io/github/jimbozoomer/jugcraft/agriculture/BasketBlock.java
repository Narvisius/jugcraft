package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A Wooden or Bamboo Basket (the kitchen and cooking expansion's slice 5; tools/soil.py BASKETS), in the owner's woven
 * art: a storage block of its own (the Foraging Basket is unchanged, as the owner chose), open at the top. Use it to
 * open its {@value BasketBlockEntity#SLOTS} slots; items dropped into it are taken in ({@link BasketBlockEntity}); a
 * comparator reads how full it is.
 */
public class BasketBlock extends BaseEntityBlock {
	/** Woven walls and floor a pixel thick, open at the top so items fall in. */
	private static final VoxelShape SHAPE = Shapes.join(Shapes.block(), Block.box(1.0, 1.0, 1.0, 15.0, 16.0, 15.0), BooleanOp.ONLY_FIRST);

	public BasketBlock(Properties properties) {
		super(properties);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BasketBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != JugcraftAgriculture.BASKET_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> ((BasketBlockEntity) entity).serverTick((ServerLevel) tickLevel);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BasketBlockEntity basket) {
			player.openMenu(basket);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		Containers.updateNeighboursAfterDestroy(state, level, pos);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
	}
}
