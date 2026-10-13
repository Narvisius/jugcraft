package io.github.jimbozoomer.jugcraft.concordance.courier;

import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.spirits.Workers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Courier Post's block (roadmap step 18). Used with an item: its owner's party files a request for a stack of
 * exactly that item (it stays in hand). Used with an empty hand: anyone reads its recent history and what is open.
 * A Porter Key used on it is bound to it (the key does that). Everything else is {@link CourierPostBlockEntity}.
 */
public class CourierPostBlock extends BaseEntityBlock {
	public CourierPostBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CourierPostBlockEntity(pos, state);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level instanceof ServerLevel && placer instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof CourierPostBlockEntity post) {
			post.setOwner(player.getUUID());
		}
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (stack.is(Workers.PORTER_KEY)) {
			return InteractionResult.PASS;
		}
		if (!(player instanceof ServerPlayer server) || !(level instanceof ServerLevel serverLevel)
				|| !(level.getBlockEntity(pos) instanceof CourierPostBlockEntity post)) {
			return InteractionResult.SUCCESS;
		}
		if (!RateGate.allow(server, "courier", 10)) {
			return InteractionResult.FAIL;
		}
		Couriers.request(server, serverLevel, post, stack, stack.getMaxStackSize());
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer server && level instanceof ServerLevel serverLevel
				&& level.getBlockEntity(pos) instanceof CourierPostBlockEntity post) {
			if (!RateGate.allow(server, "courier", 4)) {
				return InteractionResult.FAIL;
			}
			Couriers.describe(server, serverLevel, post);
		}
		return InteractionResult.SUCCESS;
	}
}
