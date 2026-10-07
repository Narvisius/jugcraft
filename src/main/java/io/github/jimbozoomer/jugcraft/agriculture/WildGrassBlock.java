package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Short grass or a fern of the graveyard flora (tools/plants.py kind "grass"), withered grass and the ghost fern: it
 * grows where vanilla's short grass does, is replaced when built over (its properties are short grass's), drops only to
 * shears (its loot table), and bone meal grows it into its two-block form ("tall" in plants.json) where there is room,
 * as vanilla's grass becomes tall grass.
 */
public class WildGrassBlock extends VegetationBlock implements BonemealableBlock {
	private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);
	private final String tallId;

	public WildGrassBlock(Properties properties, String tallId) {
		super(properties);
		this.tallId = tallId;
	}

	/** The two-block plant bone meal grows this into. */
	public Block tall() {
		return JugcraftAgriculture.block(tallId);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
		return level.getBlockState(pos.above()).isAir();
	}

	@Override
	public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		return true;
	}

	@Override
	public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		BlockState tall = tall().defaultBlockState();
		if (tall.canSurvive(level, pos)) {
			DoublePlantBlock.placeAt(level, tall, pos, Block.UPDATE_CLIENTS);
		}
	}
}
