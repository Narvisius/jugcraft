package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * An open locker with a coat on its hanger (a lab coat, or the security jacket in the guards' lockers). Right-click
 * with an empty hand takes the coat out to wear; right-click holding a lab coat or jacket hangs it back up. The coat
 * belongs to the top block of a stack of lockers (where its hanging rail is); either block can be clicked. Lockers in
 * the pre-made plants start with their coat; one placed by a player starts empty.
 */
public class LockerBlock extends StackingFurnitureBlock implements EntityBlock {
	public final UniformItem.Kind kind;

	public LockerBlock(Properties properties, UniformItem.Kind kind, double[]... boxes) {
		super(properties, boxes);
		this.kind = kind;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new LockerBlockEntity(pos, state);
	}

	private @Nullable LockerBlockEntity top(Level level, BlockPos pos) {
		BlockPos at = pos;
		while (level.getBlockState(at.above()).is(this)) {
			at = at.above();
		}
		return level.getBlockEntity(at) instanceof LockerBlockEntity locker ? locker : null;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (!(stack.getItem() instanceof UniformItem)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		LockerBlockEntity locker = top(level, pos);
		if (locker == null || !locker.coat().isEmpty()) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide()) {
			locker.setCoat(stack.split(1));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		LockerBlockEntity locker = top(level, pos);
		if (locker == null || locker.coat().isEmpty()) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			ItemStack coat = locker.coat();
			locker.setCoat(ItemStack.EMPTY);
			if (!player.getInventory().add(coat)) {
				Block.popResource(level, pos, coat);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (placer != null && level.getBlockEntity(pos) instanceof LockerBlockEntity locker) {
			locker.setCoat(ItemStack.EMPTY);
		}
	}
}
