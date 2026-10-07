package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Witchlight Lamp-Post (Halloween decorations batch 19, the Witchlight Lantern Path): a crooked iron post two blocks
 * tall with a shepherd's-crook arm holding a purple-glass lantern with a gold cap. The lantern, its light and its watch
 * are on the upper half; both halves keep the same state ({@link Witchlights}). A signal into either half keeps it awake.
 */
public class WitchlightLampPostBlock extends TallDecorationBlock implements EntityBlock {
	public WitchlightLampPostBlock(Properties properties) {
		super(properties, Block.box(6.0, 0.0, 6.0, 10.0, 16.0, 10.0), Block.box(3.0, 0.0, 3.0, 13.0, 15.0, 13.0));
		registerDefaultState(defaultBlockState().setValue(Witchlights.LIT, false).setValue(Witchlights.COLOUR, Witchlights.Colour.PURPLE));
	}

	/** The lantern's light, on the upper half only. */
	public static int light(BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.UPPER ? Witchlights.light(state) : 0;
	}

	/** Sets {@code changed} on the half at {@code pos} and the other. */
	static void setPost(Level level, BlockPos pos, BlockState changed) {
		setBoth(level, pos, changed);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		return Witchlights.dye(level, pos, state, player, stack);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.UPPER ? new WitchlightBlockEntity(pos, state) : null;
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || state.getValue(HALF) != DoubleBlockHalf.UPPER || type != JugcraftAgriculture.WITCHLIGHT_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> ((WitchlightBlockEntity) entity).serverTick((ServerLevel) tickLevel, pos, tickState);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(Witchlights.LIT, Witchlights.COLOUR);
	}
}
