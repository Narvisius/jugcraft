package io.github.jimbozoomer.jugcraft.concordance.garden;

import io.github.jimbozoomer.jugcraft.concordance.Illumination;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** The Mulch Maw's block: use plant matter on it to feed it. Everything it does is {@link MulchMawBlockEntity}. */
public class MulchMawBlock extends LivingDeviceBlock {
	public MulchMawBlock(Properties properties) {
		super(properties, Block.box(1.0, 0.0, 1.0, 15.0, 12.0, 15.0), true);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new MulchMawBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel)) {
			return null;
		}
		return createTickerHelper(type, Garden.MAW_ENTITY, (tickLevel, pos, tickState, maw) -> maw.serverTick((ServerLevel) tickLevel));
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (stack.isEmpty() || Garden.mulch(stack) == 0) {
			return super.useItemOn(stack, state, level, pos, player, hand, hit);
		}
		if (player instanceof ServerPlayer server && level instanceof ServerLevel serverLevel
				&& level.getBlockEntity(pos) instanceof MulchMawBlockEntity maw) {
			if (!RateGate.allow(server, "garden", 4) || !Illumination.mayChange(serverLevel, player, pos)) {
				return InteractionResult.FAIL;
			}
			int taken = maw.feed(stack);
			if (taken == 0) {
				return InteractionResult.FAIL;
			}
			stack.consume(taken, player);
			level.playSound(null, pos, SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 0.8F, 1.0F);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void useEmpty(LivingDeviceBlockEntity device, ServerPlayer player, ServerLevel level) {
		if (device instanceof MulchMawBlockEntity maw) {
			player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.garden.maw", maw.food().getCount(),
					maw.held(), Garden.status(maw.status())));
		}
	}
}
