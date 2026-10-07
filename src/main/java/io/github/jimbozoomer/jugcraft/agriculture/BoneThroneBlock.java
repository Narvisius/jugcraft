package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Bone Throne (Halloween decorations batch 18): a seat two blocks tall framed in thigh bones, its armrests ending in
 * skulls, a red velvet cushion, and a fan of ribs behind topped by a horned skull. Use it to sit (sneak to get up).
 * While someone sits in it at night the crest's eye sockets glow red ({@link #LIT}): it looks once every
 * {@value #CHECK_TICKS} ticks while occupied and stops looking when nobody is.
 */
public class BoneThroneBlock extends TallDecorationBlock implements Seat.Sittable {
	public static final double SEAT = 0.6;
	public static final int CHECK_TICKS = 20;
	public static final int GLOW_LIGHT = 4;
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	public BoneThroneBlock(Properties properties) {
		super(properties, Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0), Block.box(1.0, 0.0, 9.0, 15.0, 15.0, 15.0));
		registerDefaultState(defaultBlockState().setValue(LIT, false));
	}

	public static int light(BlockState state) {
		return state.getValue(LIT) && state.getValue(HALF) == DoubleBlockHalf.UPPER ? GLOW_LIGHT : 0;
	}

	@Override
	public double seatHeight(BlockState state) {
		return SEAT;
	}

	private static BlockPos lower(BlockState state, BlockPos pos) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		BlockPos seat = lower(state, pos);
		if (player.isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}
		if (level instanceof ServerLevel server) {
			BlockState base = level.getBlockState(seat);
			if (!(base.getBlock() instanceof BoneThroneBlock) || !Seat.sit(server, seat, base, player)) {
				return InteractionResult.PASS;
			}
			server.playSound(null, seat, SoundEvents.BONE_BLOCK_STEP, SoundSource.BLOCKS, 0.8F, 0.7F);
			server.scheduleTick(seat, this, 1);
		}
		return InteractionResult.SUCCESS;
	}

	/** While someone sits here, glows at night and looks again in a second; with nobody, goes dark and stops looking. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(HALF) != DoubleBlockHalf.LOWER) {
			return;
		}
		boolean occupied = !Seat.at(level, pos).isEmpty();
		boolean glow = occupied && MourningAngelBlock.night(level);
		if (glow != state.getValue(LIT)) {
			setBoth(level, pos, state.setValue(LIT, glow));
		}
		if (occupied) {
			level.scheduleTick(pos, this, CHECK_TICKS);
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(LIT);
	}
}
