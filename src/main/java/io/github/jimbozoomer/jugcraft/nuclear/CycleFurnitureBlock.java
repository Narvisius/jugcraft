package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Furniture whose versions are picked, not random (turbine hall part 3: the workshop's bench, pegboard, lathe and
 * gantry each with three loads of tools and work on them, tools/shop_room.py; the hall's floor marking tiles, its yellow
 * lines and the laydown bay's stencils, tools/hall_fittings.py): placed as the first, and used with an empty hand it
 * steps to the next (the plants place their own).
 */
public class CycleFurnitureBlock extends VariantFurnitureBlock {
	private final int count;

	public static CycleFurnitureBlock create(Properties properties, int count, double[]... boxes) {
		preparing(count);
		return new CycleFurnitureBlock(properties, count, boxes);
	}

	protected CycleFurnitureBlock(Properties properties, int count, double[]... boxes) {
		super(properties, count, boxes);
		this.count = count;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return super.getStateForPlacement(context).setValue(property(count), 0);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.getMainHandItem().isEmpty()) {
			// (Something in hand is used as it would be: a block put down on a floor marking.)
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			level.setBlock(pos, state.setValue(property(count), (state.getValue(property(count)) + 1) % count), 3);
			level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.6F, 1.2F);
		}
		return InteractionResult.SUCCESS;
	}
}
