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

/** The Gleaner's block: put a hopper under it. Everything it does is {@link GleanerBlockEntity}. */
public class GleanerBlock extends LivingDeviceBlock {
	public GleanerBlock(Properties properties) {
		super(properties, Block.box(4.0, 0.0, 4.0, 12.0, 14.0, 12.0), true);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new GleanerBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel)) {
			return null;
		}
		return createTickerHelper(type, Garden.GLEANER_ENTITY, (tickLevel, pos, tickState, gleaner) -> gleaner.serverTick((ServerLevel) tickLevel));
	}

	@Override
	protected void useEmpty(LivingDeviceBlockEntity device, ServerPlayer player, ServerLevel level) {
		if (device instanceof GleanerBlockEntity gleaner) {
			player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.garden.gleaner", gleaner.verdance(),
					GleanerBlockEntity.CAPACITY, Garden.status(gleaner.status(), GleanerBlockEntity.HEART_REACH)));
		}
	}
}
