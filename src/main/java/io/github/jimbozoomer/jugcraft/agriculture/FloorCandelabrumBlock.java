package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Floor Candelabrum (Halloween decorations batch 17): a wrought-iron candlestand two blocks tall on four scrolled
 * feet, its twisted stem carrying seven candles in two tiers and a crown. Both halves share its candles' state (wax,
 * flame, lit, drips), as they do its facing; the lower half's block entity is what the client draws the candles from.
 * Both halves give light when it burns. {@link Candelabra} has what the candles take and how they light.
 */
public class FloorCandelabrumBlock extends TallDecorationBlock implements EntityBlock {
	public static final String KIND = "floor_candelabrum";

	public FloorCandelabrumBlock(Properties properties) {
		super(properties, Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0), Block.box(1.0, 0.0, 1.0, 15.0, 15.0, 15.0));
		registerDefaultState(defaultBlockState().setValue(Candelabra.WAX, Candelabra.Wax.IVORY).setValue(Candelabra.FLAME, Candelabra.Flame.ORDINARY)
				.setValue(Candelabra.LIT, false).setValue(Candelabra.POWERED, false).setValue(Candelabra.DRIPS, 0));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new DecorationBlockEntity(JugcraftAgriculture.CANDELABRUM_ENTITY, pos, state) : null;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		return Candelabra.use(stack, state, level, pos, player, hand, changed -> setBoth(level, pos, changed));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		return Candelabra.snuff(state, level, pos, player, changed -> setBoth(level, pos, changed));
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation, boolean moved) {
		super.neighborChanged(state, level, pos, neighbor, orientation, moved);
		if (level instanceof ServerLevel server) {
			BlockPos other = pos.relative(state.getValue(HALF).getDirectionToOther());
			BlockState changed = Candelabra.powered(state, level.hasNeighborSignal(pos) || level.hasNeighborSignal(other));
			if (changed != null) {
				setBoth(level, pos, changed);
				server.playSound(null, pos, changed.getValue(Candelabra.LIT) ? SoundEvents.FIRECHARGE_USE : SoundEvents.CANDLE_EXTINGUISH,
						SoundSource.BLOCKS, 0.3F, 1.4F);
			}
		}
	}

	@Override
	protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
		if (!level.isClientSide() && projectile.isOnFire() && !state.getValue(Candelabra.LIT)) {
			setBoth(level, hit.getBlockPos(), state.setValue(Candelabra.LIT, true));
		}
	}

	/** Only the lower half grows drips, for both. */
	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER && Candelabra.drips(state);
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		BlockState changed = Candelabra.drip(state, random);
		if (changed != null) {
			setBoth(level, pos, changed);
		}
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
			Candelabra.smoke(KIND, state, level, pos, state.getValue(FACING), random);
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(Candelabra.WAX, Candelabra.FLAME, Candelabra.LIT, Candelabra.POWERED, Candelabra.DRIPS);
	}
}
