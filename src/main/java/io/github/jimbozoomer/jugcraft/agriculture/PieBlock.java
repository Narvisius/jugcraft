package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A baked pie in its tin, placed like a cake: {@value #SLICES} slices. A hungry player using it eats a slice; a knife (item
 * tag {@code jugcraft:knives}) cuts a slice off to take away ({@link PieFilling#slice}). The last slice takes the tin with
 * it. Only a whole pie can be picked up again (its loot drops it only uncut). A burnt pie (no slice) is eaten the same way
 * but gives {@value #BURNT_NUTRITION} hunger a slice and, one time in {@value #BURNT_SICK_CHANCE}, Hunger. A pie not
 * baked in the Hearth Oven (vanilla's pumpkin pie set down, {@link PlacedPieBlock}) names its own slice and food.
 */
public class PieBlock extends Block {
	public static final int SLICES = 4;
	public static final int BURNT_NUTRITION = 1;
	public static final float BURNT_SATURATION = 0.1F;
	public static final int BURNT_SICK_CHANCE = 3;
	public static final int BURNT_SICK_TICKS = 200;
	public static final IntegerProperty BITES = IntegerProperty.create("bites", 0, SLICES - 1);
	/** The pie in quarters, the first {@code bites} of them gone (the north-east first, then round). */
	private static final VoxelShape[] SHAPES = new VoxelShape[SLICES];
	private static final VoxelShape[] QUARTERS = {Block.box(8, 0, 2, 14, 4, 8), Block.box(8, 0, 8, 14, 4, 14), Block.box(2, 0, 8, 8, 4, 14),
			Block.box(2, 0, 2, 8, 4, 8)};

	static {
		for (int bites = 0; bites < SLICES; bites++) {
			VoxelShape shape = Shapes.empty();
			for (int quarter = bites; quarter < SLICES; quarter++) {
				shape = Shapes.or(shape, QUARTERS[quarter]);
			}
			SHAPES[bites] = shape;
		}
	}

	private final @Nullable PieFilling filling;
	/** The slice a knife cuts, and what a slice gives; null for a burnt pie. */
	private final @Nullable String slice;
	private final int nutrition;
	private final float saturation;

	public PieBlock(@Nullable PieFilling filling, Properties properties) {
		this(filling, filling == null ? null : filling.slice(), filling == null ? BURNT_NUTRITION : filling.nutrition,
				filling == null ? BURNT_SATURATION : filling.saturation, properties);
	}

	/** A pie not baked in the Hearth Oven: the slice a knife cuts and what a slice gives. */
	public PieBlock(String slice, int nutrition, float saturation, Properties properties) {
		this(null, slice, nutrition, saturation, properties);
	}

	private PieBlock(@Nullable PieFilling filling, @Nullable String slice, int nutrition, float saturation, Properties properties) {
		super(properties);
		this.filling = filling;
		this.slice = slice;
		this.nutrition = nutrition;
		this.saturation = saturation;
		registerDefaultState(stateDefinition.any().setValue(BITES, 0));
	}

	public @Nullable PieFilling filling() {
		return filling;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES[state.getValue(BITES)];
	}

	/** A knife (item tag jugcraft:knives: the Carving Knife and the kitchen knives) cuts a slice to take away (not from a burnt pie). */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (!stack.is(JugcraftAgriculture.KNIVES)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (slice == null) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			ItemStack cut = new ItemStack(JugcraftAgriculture.item(slice));
			if (!player.getInventory().add(cut)) {
				Block.popResource(level, pos, cut);
			}
			level.playSound(null, pos, SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.BLOCKS, 0.6F, 1.4F);
			takeSlice(level, pos, state, player);
		}
		return InteractionResult.SUCCESS;
	}

	/** A hungry player eats the next slice. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.canEat(false)) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		player.getFoodData().eat(nutrition, saturation);
		if (slice == null && level.getRandom().nextInt(BURNT_SICK_CHANCE) == 0) {
			player.addEffect(new MobEffectInstance(MobEffects.HUNGER, BURNT_SICK_TICKS));
			player.sendOverlayMessage(Component.translatable("message.jugcraft.pie.burnt"));
		}
		level.playSound(null, pos, SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
		level.gameEvent(player, GameEvent.EAT, pos);
		takeSlice(level, pos, state, player);
		return InteractionResult.SUCCESS;
	}

	private static void takeSlice(Level level, BlockPos pos, BlockState state, Player player) {
		int bites = state.getValue(BITES);
		if (bites + 1 >= SLICES) {
			level.removeBlock(pos, false);
			level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
		} else {
			level.setBlock(pos, state.setValue(BITES, bites + 1), Block.UPDATE_ALL);
		}
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return level.getBlockState(pos.below()).isSolid();
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighbor, RandomSource random) {
		return direction == Direction.DOWN && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	/** Slices left: 15 for a whole pie, down by a quarter a slice. */
	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return (SLICES - state.getValue(BITES)) * 15 / SLICES;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(BITES);
	}
}
