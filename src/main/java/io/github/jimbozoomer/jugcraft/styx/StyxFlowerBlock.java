package io.github.jimbozoomer.jugcraft.styx;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** A decorative cultivar: cuttings grow, mature plants propagate with bone meal; no potion or spell effects. */
public final class StyxFlowerBlock extends VegetationBlock implements BonemealableBlock {
	public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 2);
	public StyxFlowerBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(AGE, 0));
	}
	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(AGE); }
	@Override protected boolean isRandomlyTicking(BlockState state) { return state.getValue(AGE) < 2; }
	@Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(AGE) < 2 && level.getRawBrightness(pos, 0) >= 9 && random.nextInt(5) == 0) {
			level.setBlock(pos, state.setValue(AGE, state.getValue(AGE) + 1), Block.UPDATE_CLIENTS);
		}
	}
	@Override public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) { return true; }
	@Override public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) { return true; }
	@Override public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		if (state.getValue(AGE) < 2) level.setBlock(pos, state.setValue(AGE, state.getValue(AGE) + 1), Block.UPDATE_CLIENTS);
		else popResource(level, pos, new ItemStack(this));
	}
}
