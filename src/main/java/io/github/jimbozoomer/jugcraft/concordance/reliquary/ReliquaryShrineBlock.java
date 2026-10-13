package io.github.jimbozoomer.jugcraft.concordance.reliquary;

import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Reliquary Shrine's block (roadmap step 20): a blackstone plinth with an amethyst cradle. A relic used on it is
 * installed (if it has an installed mode and the shrine is empty) or recharged from the Ley Pylons beside it; an empty
 * hand takes the installed relic back. It glows while it holds a relic. GeckoLib draws it; the rest is
 * {@link ReliquaryShrineBlockEntity}.
 */
public class ReliquaryShrineBlock extends BaseEntityBlock {
	/** Whether it holds a relic (its light). */
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);

	public ReliquaryShrineBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(LIT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ReliquaryShrineBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel)) {
			return null;
		}
		return createTickerHelper(type, Reliquary.SHRINE_ENTITY, (tickLevel, pos, tickState, shrine) -> shrine.serverTick((ServerLevel) tickLevel));
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level instanceof ServerLevel && placer instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof ReliquaryShrineBlockEntity shrine) {
			shrine.setOwner(player.getUUID());
		}
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (hand != InteractionHand.MAIN_HAND || !(stack.getItem() instanceof RelicItem)) {
			return super.useItemOn(stack, state, level, pos, player, hand, hit);
		}
		if (player instanceof ServerPlayer server && level instanceof ServerLevel serverLevel
				&& level.getBlockEntity(pos) instanceof ReliquaryShrineBlockEntity shrine) {
			if (!RateGate.allow(server, "relic", 6)) {
				return InteractionResult.FAIL;
			}
			shrine.useWith(server, serverLevel, stack);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer server && level instanceof ServerLevel serverLevel
				&& level.getBlockEntity(pos) instanceof ReliquaryShrineBlockEntity shrine) {
			if (!RateGate.allow(server, "relic", 6)) {
				return InteractionResult.FAIL;
			}
			shrine.useEmpty(server, serverLevel);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}
}
