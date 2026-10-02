package io.github.jimbozoomer.jugcraft.blueprint;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Survey Stake: where a placed blueprint stands. Right-click opens its screen (progress, what is still
 * needed, Personal/Party, rotate, remove);
 * sneak-right-click (the placer, or the party leader for a Party blueprint) switches Personal/Party.
 * Breaking it gives the blueprint back; it also pops off by itself when the build is complete.
 */
public class SurveyStakeBlock extends BaseEntityBlock {
	private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 14, 11);

	public SurveyStakeBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SurveyStakeBlockEntity(pos, state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel)) {
			return null;
		}
		return createTickerHelper(type, JugcraftBlueprints.STAKE_ENTITY, (tickLevel, pos, tickState, stake) -> stake.serverTick((ServerLevel) tickLevel));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SurveyStakeBlockEntity stake) {
			if (player.isShiftKeyDown()) {
				if (stake.mayChange(player)) {
					stake.toggleMode(player);
				} else {
					player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.jugcraft.blueprint.not_owner"));
				}
			} else if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
				net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(serverPlayer, stake.info(player, true));
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SurveyStakeBlockEntity stake && !stake.blueprintId().isEmpty()) {
			Block.popResource(level, pos, BlueprintItem.stack(stake.blueprintId(), stake.blueprint() == null ? "" : stake.blueprint().name));
		}
		return super.playerWillDestroy(level, pos, state, player);
	}
}
