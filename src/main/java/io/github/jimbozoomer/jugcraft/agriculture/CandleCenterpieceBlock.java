package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Pumpkin Candle Centerpiece (Thanksgiving decorations, tools/thanksgiving_decor.py PROPS, kind "candles"): a table
 * centerpiece whose pillar candles flint and steel or a fire charge lights and an empty hand snuffs, as the Hurricane
 * Lamp's wick ({@link CandleLighting}). Lit, each candle shows its flame (the blockstate's lit model) and it gives
 * light {@value #LIGHT}. It burns no wax. Its model is tools/thanksgiving_decor_data.py pumpkin_candle_centerpiece().
 */
public class CandleCenterpieceBlock extends FacingDecorationBlock {
	public static final int LIGHT = 12;
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	/** {@code box}: its shape {x0, y0, z0, x1, y1, z1} in pixels, facing north. */
	public CandleCenterpieceBlock(Properties properties, double[] box) {
		super(properties, box);
		registerDefaultState(defaultBlockState().setValue(LIT, false));
	}

	/** The light it gives: {@value #LIGHT} while lit. */
	public static int light(BlockState state) {
		return state.getValue(LIT) ? LIGHT : 0;
	}

	/** Flint and steel or a fire charge lights it; anything else does what it would on any block. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		InteractionResult lighting = CandleLighting.light(stack, state, level, pos, player, hand, LIT);
		return lighting == InteractionResult.TRY_WITH_EMPTY_HAND && !stack.isEmpty() ? InteractionResult.PASS : lighting;
	}

	/** An empty hand snuffs its candles. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		return CandleLighting.snuff(state, level, pos, player, LIT);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(LIT);
	}
}
