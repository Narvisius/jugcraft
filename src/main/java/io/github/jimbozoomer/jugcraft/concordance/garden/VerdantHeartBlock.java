package io.github.jimbozoomer.jugcraft.concordance.garden;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The Verdant Heart's block: set it on a Verdant Bed. Everything it does is {@link VerdantHeartBlockEntity}. */
public class VerdantHeartBlock extends LivingDeviceBlock {
	public VerdantHeartBlock(Properties properties) {
		super(properties, Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0), true);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new VerdantHeartBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel)) {
			return null;
		}
		return createTickerHelper(type, Garden.HEART_ENTITY, (tickLevel, pos, tickState, heart) -> heart.serverTick((ServerLevel) tickLevel));
	}

	@Override
	protected void useEmpty(LivingDeviceBlockEntity device, ServerPlayer player, ServerLevel level) {
		if (device instanceof VerdantHeartBlockEntity heart) {
			player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.garden.heart", heart.verdance(),
					VerdantHeartBlockEntity.CAPACITY, Garden.status(heart.status())));
			if (level.getBlockEntity(heart.getBlockPos().below()) instanceof VerdantBedBlockEntity bed) {
				bed.report(level, player);
			}
		}
	}
}
