package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The vine of a giant pumpkin, grown from Giant Pumpkin Seeds on farmland like a pumpkin stem but
 * {@link #GROWTH_TIME} times slower ({@link CropGrowth}). Full grown, it sets one small fruit on a free side
 * on ground fruit can lie on, and becomes an {@link AttachedGiantPumpkinVineBlock}; the fruit then grows
 * into a {@link GiantPumpkinBlock} as long as the vine holds it. Unlike a pumpkin stem it bears one fruit
 * at a time and nothing more until that is harvested. Bone meal grows the vine and, full grown, sets the fruit
 * at once (a pumpkin stem's bone meal stops at full growth; here it would leave nothing to feed).
 */
public class GiantPumpkinVineBlock extends VegetationBlock implements BonemealableBlock {
	public static final int MAX_AGE = 7;
	public static final float GROWTH_TIME = 1.5F;
	public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
	private static final VoxelShape[] SHAPES = new VoxelShape[MAX_AGE + 1];

	static {
		for (int age = 0; age <= MAX_AGE; age++) {
			SHAPES[age] = Block.box(6.0, 0.0, 6.0, 10.0, 2.0 + age * 2.0, 10.0);
		}
	}

	public GiantPumpkinVineBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(AGE, 0));
	}

	@Override
	protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
		return state.is(BlockTags.SUPPORTS_STEM_CROPS);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES[state.getValue(AGE)];
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getRawBrightness(pos, 0) < CropGrowth.MIN_GROWTH_LIGHT
				|| random.nextInt(CropGrowth.chanceDivisor(CropGrowth.speed(level, pos, false), GROWTH_TIME)) != 0) {
			return;
		}
		int age = state.getValue(AGE);
		if (age < MAX_AGE) {
			level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
		} else {
			Direction side = freeSide(level, pos, random);
			if (side != null) {
				growFruit(level, pos, side);
			}
		}
	}

	/** Whether a fruit could be set on {@code side} of the vine at {@code pos}: free space, on ground fruit can lie on. */
	public static boolean roomForFruit(LevelReader level, BlockPos pos, Direction side) {
		BlockPos target = pos.relative(side);
		return level.getBlockState(target).isAir() && level.getBlockState(target.below()).is(BlockTags.SUPPORTS_STEM_FRUIT);
	}

	/** A random side of the vine at {@code pos} with room for a fruit, or null if there is none. */
	public static @Nullable Direction freeSide(LevelReader level, BlockPos pos, RandomSource random) {
		List<Direction> free = new ArrayList<>();
		for (Direction side : Direction.Plane.HORIZONTAL) {
			if (roomForFruit(level, pos, side)) {
				free.add(side);
			}
		}
		return free.isEmpty() ? null : free.get(random.nextInt(free.size()));
	}

	/** Sets a giant pumpkin's first fruit on {@code side} of a full-grown vine, if there is room. Returns whether it did. */
	public boolean growFruit(Level level, BlockPos pos, Direction side) {
		BlockState state = level.getBlockState(pos);
		BlockPos target = pos.relative(side);
		if (!state.is(this) || state.getValue(AGE) < MAX_AGE || !roomForFruit(level, pos, side)) {
			return false;
		}
		level.setBlockAndUpdate(target, JugcraftAgriculture.block("giant_pumpkin").defaultBlockState());
		if (level.getBlockEntity(target) instanceof GiantPumpkinBlockEntity fruit) {
			fruit.plant(target, side);
		}
		level.setBlockAndUpdate(pos, JugcraftAgriculture.block("attached_giant_pumpkin_vine").defaultBlockState()
				.setValue(HorizontalDirectionalBlock.FACING, side));
		return true;
	}

	/** Bone meal on a full-grown vine with nowhere to set its fruit says why nothing happens. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (stack.is(Items.BONE_MEAL) && !canBonemeal(level, pos, state)) {
			if (player instanceof ServerPlayer serverPlayer) {
				serverPlayer.sendOverlayMessage(Component.translatable("message.jugcraft.giant_pumpkin_vine.no_room"));
			}
			return InteractionResult.SUCCESS;
		}
		return super.useItemOn(stack, state, level, pos, player, hand, hit);
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(JugcraftAgriculture.item("giant_pumpkin_seeds"));
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
		return canBonemeal(level, pos, state);
	}

	/** Bone meal works while the vine grows, and full grown while it has room to set its fruit. */
	private static boolean canBonemeal(LevelReader level, BlockPos pos, BlockState state) {
		if (state.getValue(AGE) < MAX_AGE) {
			return true;
		}
		for (Direction side : Direction.Plane.HORIZONTAL) {
			if (roomForFruit(level, pos, side)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		return true;
	}

	@Override
	public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		if (state.getValue(AGE) < MAX_AGE) {
			int age = Math.min(MAX_AGE, state.getValue(AGE) + 1 + random.nextInt(3));
			level.setBlock(pos, state.setValue(AGE, age), Block.UPDATE_CLIENTS);
			return;
		}
		Direction side = freeSide(level, pos, random);
		if (side != null) {
			growFruit(level, pos, side);
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AGE);
	}
}
