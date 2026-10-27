package io.github.jimbozoomer.jugcraft.concordance;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Crucible Stand (Concordance Chunk C, C-05): set an Alembic Crucible on it and run a cable to any face but its top;
 * it heats the crucible for its formula's next stir, or to its own setting. Empty-handed, a use sets its heat (no formula
 * above) or says how it stands (sneaking, or a formula decides); anything held is used as itself, so a crucible is set on
 * it and a cable beside it by a plain use. {@link #LIT} lights its coil on the ticks it pays (fed too little, also through
 * a few it cannot). It carries no worldgen connection marker: a cable meets it where it really takes power
 * (CableBlock.connectsTo asks the face), never its seat (C-P44). Everything else is {@link CrucibleStandBlockEntity}.
 */
public class CrucibleStandBlock extends BaseEntityBlock {
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	public CrucibleStandBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(LIT, false));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CrucibleStandBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel)) {
			return null;
		}
		return createTickerHelper(type, JugcraftConcordance.CRUCIBLE_STAND_ENTITY, (tickLevel, pos, tickState, stand) -> stand.heat((ServerLevel) tickLevel));
	}

	/** C-P46: only an empty hand sets or reads the stand; anything held is used as itself (a crucible set on it, a cable beside it). */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		return stack.isEmpty() ? super.useItemOn(stack, state, level, pos, player, hand, hit) : InteractionResult.PASS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof CrucibleStandBlockEntity stand) {
			stand.useEmpty(server, player.isShiftKeyDown());
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}
}
