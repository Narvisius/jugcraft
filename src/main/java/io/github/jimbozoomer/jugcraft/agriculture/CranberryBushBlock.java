package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A cranberry bush, the Agriculture branch's bog crop. It grows in a still water source one block deep,
 * rooted in bog soil (block tag {@code jugcraft:bog_soil}: dirt, mud, grass, sand, clay or gravel), and
 * only grows while there is open air above the water. Like vanilla seagrass it always holds its water, so
 * breaking it leaves the water behind. Ripe bushes (age 3) are picked with a right-click or a sickle,
 * dropping 2-3 cranberries, and go back to flowering (age 1), like vanilla sweet berries.
 */
public class CranberryBushBlock extends VegetationBlock implements BonemealableBlock, LiquidBlockContainer {
	public static final int MAX_AGE = 3;
	public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
	public static final TagKey<Block> BOG_SOIL = TagKey.create(Registries.BLOCK, Jugcraft.id("bog_soil"));
	/** One growth stage in this many random ticks (vanilla sweet berries: 5). Keep in sync with tools/agriculture.py. */
	public static final int GROWTH_CHANCE = 5;
	public static final int PICK_MIN = 2;
	public static final int PICK_MAX = 3;
	public static final int PICK_RESET = 1;
	private static final VoxelShape SPROUT = Block.box(3.0, 0.0, 3.0, 13.0, 8.0, 13.0);
	private static final VoxelShape BUSH = Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);

	public CranberryBushBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(AGE, 0));
	}

	public static boolean isRipe(BlockState state) {
		return state.getValue(AGE) == MAX_AGE;
	}

	/** Water one block deep: a full water source here and none right above it. */
	public static boolean isShallowWater(LevelReader level, BlockPos pos) {
		FluidState fluid = level.getFluidState(pos);
		return fluid.is(FluidTags.WATER) && fluid.isSource() && !level.getFluidState(pos.above()).is(FluidTags.WATER);
	}

	@Override
	protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
		return state.is(BOG_SOIL);
	}

	/** Bog soil below and a water source here (the bush's own water once it stands). */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		FluidState fluid = level.getFluidState(pos);
		return fluid.is(FluidTags.WATER) && fluid.isSource() && super.canSurvive(state, level, pos);
	}

	/** Planted only into shallow water, as seagrass is only placed into water. */
	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return isShallowWater(context.getLevel(), context.getClickedPos()) ? super.getStateForPlacement(context) : null;
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		BlockState updated = super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
		if (!updated.isAir()) {
			ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		return updated;
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return Fluids.WATER.getSource(false);
	}

	@Override
	public boolean canPlaceLiquid(@Nullable LivingEntity entity, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
		return false;
	}

	@Override
	public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
		return false;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(AGE) == 0 ? SPROUT : BUSH;
	}

	// ---------------------------------------------------------------- growth

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return state.getValue(AGE) < MAX_AGE;
	}

	/** One stage in {@link #GROWTH_CHANCE} random ticks, with light 9 or more and open air above the water. */
	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		int age = state.getValue(AGE);
		if (age < MAX_AGE && canGrow(level, pos) && random.nextInt(GROWTH_CHANCE) == 0) {
			level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
		}
	}

	/** Whether the bush at {@code pos} can grow: open air above the water, and enough light. */
	public static boolean canGrow(LevelReader level, BlockPos pos) {
		return level.getBlockState(pos.above()).isAir() && level.getRawBrightness(pos.above(), 0) >= CropGrowth.MIN_GROWTH_LIGHT;
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
		return state.getValue(AGE) < MAX_AGE;
	}

	@Override
	public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		return true;
	}

	@Override
	public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		level.setBlock(pos, state.setValue(AGE, Math.min(MAX_AGE, state.getValue(AGE) + 1)), Block.UPDATE_CLIENTS);
	}

	// ---------------------------------------------------------------- harvest

	/** Picks a ripe bush at {@code pos}: 2-3 cranberries at {@code dropPos}, and it flowers again. False if not ripe. */
	public boolean pick(Level level, BlockPos pos, BlockPos dropPos) {
		BlockState state = level.getBlockState(pos);
		if (!state.is(this) || !isRipe(state)) {
			return false;
		}
		RandomSource random = level.getRandom();
		Block.popResource(level, dropPos, new ItemStack(JugcraftAgriculture.item("cranberries"), PICK_MIN + random.nextInt(PICK_MAX - PICK_MIN + 1)));
		level.setBlock(pos, state.setValue(AGE, PICK_RESET), Block.UPDATE_CLIENTS);
		level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.8F + random.nextFloat() * 0.4F);
		return true;
	}

	/** Bone meal on an unripe bush and sickles are item actions; anything else in hand may pick. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (stack.getItem() instanceof SickleItem || (stack.is(Items.BONE_MEAL) && !isRipe(state))) {
			return InteractionResult.PASS;
		}
		return super.useItemOn(stack, state, level, pos, player, hand, hit);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!isRipe(state)) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			pick(level, pos, pos.above());
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(JugcraftAgriculture.item("cranberries"));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AGE);
	}
}
