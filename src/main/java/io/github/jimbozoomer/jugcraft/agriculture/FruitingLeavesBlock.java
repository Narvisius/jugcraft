package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TintedParticleLeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Leaves of a fruit tree that bear fruit without the tree being cut down (the chestnut and apple trees). Leaves the tree
 * grew itself (not persistent ones a player placed) with air below them flower or set fruit ({@link #FRUIT} 1) that
 * ripens ({@link #FRUIT} 2), one step in {@link #fruitChance()} random ticks. A right-click picks the ripe fruit: it drops
 * below and the leaves start again. Otherwise these are vanilla leaves: they decay away from logs.
 */
public abstract class FruitingLeavesBlock extends TintedParticleLeavesBlock {
	public static final IntegerProperty FRUIT = IntegerProperty.create("fruit", 0, 2);
	public static final int RIPE = 2;

	protected FruitingLeavesBlock(Properties properties) {
		super(0.01F, properties);
		registerDefaultState(defaultBlockState().setValue(FRUIT, 0));
	}

	/** One fruit stage in this many random ticks. */
	protected abstract int fruitChance();

	/** What a ripe cluster gives when picked. */
	protected abstract ItemStack fruit(RandomSource random);

	/** Whether leaves in {@code state} at {@code pos} can bear fruit: grown by the tree, near a log, over air. */
	public static boolean canFruit(BlockState state, Level level, BlockPos pos) {
		return !state.getValue(PERSISTENT) && state.getValue(DISTANCE) < DECAY_DISTANCE && level.isEmptyBlock(pos.below());
	}

	/** Natural leaves always tick, to grow fruit (and, as in vanilla, to decay when cut off from logs). */
	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return !state.getValue(PERSISTENT);
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		super.randomTick(state, level, pos, random);
		BlockState now = level.getBlockState(pos);
		if (now.is(this) && now.getValue(FRUIT) < RIPE && canFruit(now, level, pos) && random.nextInt(fruitChance()) == 0) {
			level.setBlock(pos, now.setValue(FRUIT, now.getValue(FRUIT) + 1), Block.UPDATE_CLIENTS);
		}
	}

	/** Picks the ripe fruit at {@code pos}: it falls below the leaves. False if there is none. */
	public boolean pick(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!state.is(this) || state.getValue(FRUIT) != RIPE) {
			return false;
		}
		RandomSource random = level.getRandom();
		Block.popResource(level, pos.below(), fruit(random));
		level.setBlock(pos, state.setValue(FRUIT, 0), Block.UPDATE_CLIENTS);
		level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.7F + random.nextFloat() * 0.3F);
		return true;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (state.getValue(FRUIT) != RIPE) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			pick(level, pos);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FRUIT);
	}
}
