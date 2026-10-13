package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Rich Soil (the kitchen and cooking expansion's slice 5; tools/soil.py RICH_SOIL): what Organic Compost becomes. It
 * counts as dirt (the {@code minecraft:dirt} tag), and the plant standing on it gets {@value #BOOST} extra random tick
 * for each of the soil's own, so it grows about twice as fast. A hoe turns it into {@link RichFarmlandBlock}.
 */
public class RichSoilBlock extends Block {
	/** Extra random ticks the plant on rich soil gets for each of the soil's own (tools/soil.py BOOST). */
	public static final int BOOST = 1;

	public RichSoilBlock(Properties properties) {
		super(properties);
	}

	/**
	 * Gives the plant standing on the soil at {@code soil} its extra random ticks: only a plant (a crop, a sapling, a
	 * bush) that ticks by itself, so nothing that would not grow anyway does.
	 */
	static void boost(ServerLevel level, BlockPos soil, RandomSource random) {
		BlockPos above = soil.above();
		for (int i = 0; i < BOOST; i++) {
			BlockState plant = level.getBlockState(above);
			if (!(plant.getBlock() instanceof VegetationBlock) || !plant.isRandomlyTicking()) {
				return;
			}
			plant.randomTick(level, above, random);
		}
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return true;
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		boost(level, pos, random);
	}

	/**
	 * A hoe tills it into Rich Soil Farmland, as dirt into farmland: only with air above. A brown or red mushroom used on
	 * its top plants that mushroom's colony ({@link MushroomColonyBlock}) there; sneaking places the mushroom as usual.
	 */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		MushroomColonyBlock colony = JugcraftAgriculture.colony(stack.getItem());
		if (colony != null) {
			BlockPos above = pos.above();
			if (hit.getDirection() != Direction.UP || !level.getBlockState(above).isAir() || !player.mayBuild()
					|| !level.mayInteract(player, above)) {
				return super.useItemOn(stack, state, level, pos, player, hand, hit);
			}
			if (!level.isClientSide()) {
				level.setBlock(above, colony.defaultBlockState(), Block.UPDATE_ALL);
				level.playSound(null, above, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
				level.gameEvent(player, GameEvent.BLOCK_PLACE, above);
				stack.consume(1, player);
			}
			return InteractionResult.SUCCESS;
		}
		if (!stack.is(ItemTags.HOES) || !level.getBlockState(pos.above()).isAir()) {
			return super.useItemOn(stack, state, level, pos, player, hand, hit);
		}
		if (!level.isClientSide()) {
			level.setBlock(pos, JugcraftAgriculture.block("rich_soil_farmland").defaultBlockState(), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.HOE_TILL.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
			stack.hurtAndBreak(1, player, hand);
		}
		return InteractionResult.SUCCESS;
	}
}
