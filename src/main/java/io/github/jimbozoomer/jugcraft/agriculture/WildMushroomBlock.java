package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A wild autumn mushroom (chanterelle, porcini, puffball, fly agaric, or the glowing jack o'lantern mushroom), found in
 * patches on forest floors. It grows on soil ({@link #SOIL}) and spreads in the shade: one random
 * tick in {@value #SPREAD_CHANCE}, a mushroom with fewer than {@value #SPREAD_CAP} of its kind round it puts out another
 * nearby, where the light is below {@value #SPREAD_LIGHT}; bone meal makes it try at once. On a full-moon night, one
 * random tick in {@value #RING_CHANCE} it sprouts a fairy ring ({@link FairyRings}): its kind round a circle it stands on.
 * Picked with a Foraging Basket in either hand, it goes straight into the basket. A puffball bursts in a cloud of spores.
 */
public class WildMushroomBlock extends VegetationBlock implements BonemealableBlock {
	public static final int SPREAD_CHANCE = 25;
	public static final int SPREAD_CAP = 5;
	public static final int SPREAD_LIGHT = 13;
	public static final int RING_CHANCE = 40;
	/** What wild mushrooms grow on (block tag {@code jugcraft:mushroom_soil}: grass, dirt, podzol, mycelium, moss). */
	public static final TagKey<Block> SOIL = TagKey.create(Registries.BLOCK, Jugcraft.id("mushroom_soil"));
	private static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 8.0, 12.0);

	public WildMushroomBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
		return state.is(SOIL);
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return true;
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (MooncakeItem.fullMoonNight(level.getOverworldClockTime()) && random.nextInt(RING_CHANCE) == 0) {
			FairyRings.sprout(level, pos, state, random);
		} else if (random.nextInt(SPREAD_CHANCE) == 0) {
			spread(level, pos, state, random, true);
		}
	}

	/** Puts out another of {@code state}'s kind nearby, if fewer than {@value #SPREAD_CAP} are round it; returns whether it did. */
	public static boolean spread(ServerLevel level, BlockPos pos, BlockState state, RandomSource random, boolean shadeOnly) {
		int near = 0;
		for (BlockPos at : BlockPos.betweenClosed(pos.offset(-4, -1, -4), pos.offset(4, 1, 4))) {
			if (level.getBlockState(at).is(state.getBlock()) && ++near >= SPREAD_CAP) {
				return false;
			}
		}
		for (int attempt = 0; attempt < 4; attempt++) {
			BlockPos target = pos.offset(random.nextInt(5) - 2, random.nextInt(3) - 1, random.nextInt(5) - 2);
			if (level.isEmptyBlock(target) && state.canSurvive(level, target) && (!shadeOnly || level.getRawBrightness(target, 0) < SPREAD_LIGHT)) {
				level.setBlock(target, state, Block.UPDATE_ALL);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
		return true;
	}

	@Override
	public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		return true;
	}

	/** Bone meal: it tries to spread at once, in any light. */
	@Override
	public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		spread(level, pos, state, random, false);
	}

	/** Picked by hand: into a Foraging Basket in either hand if there is one (what doesn't fit drops); a puffball bursts. */
	@Override
	public void playerDestroy(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
		if (state.is(JugcraftAgriculture.block("puffball"))) {
			level.sendParticles(ParticleTypes.WHITE_ASH, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 24, 0.3, 0.2, 0.3, 0.02);
			level.playSound(null, pos, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.6F, 1.6F);
		}
		ItemStack basket = ForagingBasketItem.held(player);
		if (basket.isEmpty()) {
			super.playerDestroy(level, player, pos, state, blockEntity, tool);
			return;
		}
		player.awardStat(Stats.BLOCK_MINED.get(this));
		player.causeFoodExhaustion(0.005F);
		List<ItemStack> drops = Block.getDrops(state, level, pos, blockEntity, player, tool);
		for (ItemStack drop : drops) {
			ForagingBasketItem.fill(basket, drop, player);
			if (!drop.isEmpty()) {
				Block.popResource(level, pos, drop);
			}
		}
	}
}
