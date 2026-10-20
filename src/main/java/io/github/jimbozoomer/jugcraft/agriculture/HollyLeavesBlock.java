package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/** Evergreen seasonal decoration: clipping sprigs keeps the tree alive and has a real regrowth interval. */
public final class HollyLeavesBlock extends SeasonalLeavesBlock {
	public static final BooleanProperty CLIPPED = BooleanProperty.create("clipped");
	public static final int REGROW_CHANCE = 16;
	public HollyLeavesBlock(Schedule schedule, Properties properties) {
		super(schedule, properties);
		registerDefaultState(defaultBlockState().setValue(CLIPPED, false));
	}
	private static boolean naturalSupported(BlockState state) {
		return !state.getValue(PERSISTENT) && state.getValue(DISTANCE) < DECAY_DISTANCE;
	}
	@Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hit) {
		if (!stack.is(Items.SHEARS) || state.getValue(CLIPPED) || !naturalSupported(state)) return InteractionResult.PASS;
		if (!level.isClientSide()) {
			// Re-read on the server: the first clip consumes availability before another interaction can yield a sprig.
			BlockState now = level.getBlockState(pos);
			if (!now.is(this) || now.getValue(CLIPPED) || !naturalSupported(now)) return InteractionResult.PASS;
			level.setBlock(pos, now.setValue(CLIPPED, true), Block.UPDATE_CLIENTS);
			Block.popResourceFromFace(level, pos, hit.getDirection(), new ItemStack(JugcraftAgriculture.item("holly_sprig")));
			stack.hurtAndBreak(1, player, hand);
			level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		return InteractionResult.SUCCESS;
	}
	@Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		super.randomTick(state, level, pos, random);
		BlockState now = level.getBlockState(pos);
		if (now.is(this) && now.getValue(CLIPPED) && naturalSupported(now) && random.nextInt(REGROW_CHANCE) == 0)
			level.setBlock(pos, now.setValue(CLIPPED, false), Block.UPDATE_CLIENTS);
	}
	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(CLIPPED);
	}
}
