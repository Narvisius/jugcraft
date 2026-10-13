package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A mushroom colony (garden crops, tools/garden.py COLONY), brown or red: a farmed mushroom crop. A brown or red mushroom
 * used on Rich Soil plants one ({@link RichSoilBlock}). It stands on Rich Soil, or on a block mushrooms grow on in any
 * light ({@code #minecraft:mushroom_grow_block}: mycelium, podzol, nylium), and grows through its four stages ({@link #AGE})
 * with a 1 in {@value #GROWTH} chance on each random tick (the soil's boost included), only where the light from sky and
 * blocks is at most {@value #MAX_LIGHT}, as vanilla mushrooms spread: a cellar, a cave or a shaded shed, not an open
 * field. Grown, shears or a knife pick {@value #PICK_MIN}-{@value #PICK_MAX} of its mushroom and it goes back to stage
 * {@value #PICK_RESET}. Bone meal adds a stage in any light. Broken, it gives back its mushroom, and grown, as many more
 * as picking.
 */
public class MushroomColonyBlock extends VegetationBlock implements BonemealableBlock {
	public static final int MAX_AGE = 3;
	public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
	public static final int MAX_LIGHT = 12;
	public static final int GROWTH = 5;
	public static final int PICK_MIN = 2;
	public static final int PICK_MAX = 3;
	public static final int PICK_RESET = 1;
	/** Blocks mushrooms grow on in any light (vanilla's tag). */
	public static final TagKey<Block> GROWS_ON = TagKey.create(Registries.BLOCK, Identifier.withDefaultNamespace("mushroom_grow_block"));
	/** The owner's stages fill this much of the block, by age (pixels tall). */
	private static final VoxelShape[] SHAPES = {Block.box(3.0, 0.0, 3.0, 13.0, 9.0, 13.0), Block.box(2.0, 0.0, 2.0, 14.0, 11.0, 14.0),
			Block.box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0), Block.box(1.0, 0.0, 1.0, 15.0, 15.0, 15.0)};

	private final Item mushroom;

	public MushroomColonyBlock(Properties properties, Item mushroom) {
		super(properties);
		this.mushroom = mushroom;
		this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
	}

	/** The mushroom that plants it and that it gives. */
	public Item mushroom() {
		return mushroom;
	}

	public static boolean isGrown(BlockState state) {
		return state.getValue(AGE) == MAX_AGE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES[state.getValue(AGE)];
	}

	@Override
	protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
		return state.is(GROWS_ON) || state.getBlock() instanceof RichSoilBlock;
	}

	/** Whether {@code pos} is shaded enough for a colony to grow. */
	public static boolean shaded(LevelReader level, BlockPos pos) {
		return level.getRawBrightness(pos, 0) <= MAX_LIGHT;
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return !isGrown(state);
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!isGrown(state) && shaded(level, pos) && random.nextInt(GROWTH) == 0) {
			level.setBlock(pos, state.setValue(AGE, state.getValue(AGE) + 1), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
		return !isGrown(state);
	}

	@Override
	public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		return true;
	}

	@Override
	public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		level.setBlock(pos, state.setValue(AGE, Math.min(MAX_AGE, state.getValue(AGE) + 1)), Block.UPDATE_CLIENTS);
	}

	/** Shears or a knife pick a grown colony; anything else in hand does what it would. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (!isGrown(state) || !(stack.is(Items.SHEARS) || stack.is(JugcraftAgriculture.KNIVES))) {
			return super.useItemOn(stack, state, level, pos, player, hand, hit);
		}
		if (!level.isClientSide()) {
			pick(level, pos, state);
			stack.hurtAndBreak(1, player, hand);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		return InteractionResult.SUCCESS;
	}

	/** Picks a grown colony: drops its mushrooms and sets it back to stage {@value #PICK_RESET}. Returns false if not grown. */
	public boolean pick(Level level, BlockPos pos, BlockState state) {
		if (!state.is(this) || !isGrown(state)) {
			return false;
		}
		RandomSource random = level.getRandom();
		Block.popResource(level, pos, new ItemStack(mushroom, PICK_MIN + random.nextInt(PICK_MAX - PICK_MIN + 1)));
		level.setBlock(pos, state.setValue(AGE, PICK_RESET), Block.UPDATE_CLIENTS);
		level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.8F + random.nextFloat() * 0.2F);
		return true;
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(mushroom);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AGE);
	}
}
