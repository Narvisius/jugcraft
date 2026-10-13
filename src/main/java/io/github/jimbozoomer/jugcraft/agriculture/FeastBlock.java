package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A feast (the kitchen and cooking expansion's slice 2, {@link FeastDish}): set down whole on a table, then served
 * {@value #SERVINGS} servings at a time. Use a bowl on it to take a serving away (the dish's serving, a bowl food that
 * gives the bowl back); use it with anything else while hungry to eat a serving there. The last serving leaves the
 * leftovers (the carcass, the bone, the crumbs, the hollow pumpkin), which a use clears, dropping what its loot table
 * gives (a bone, pumpkin seeds). Only a whole feast drops itself; a comparator reads the servings left.
 */
public class FeastBlock extends Block {
	public static final int SERVINGS = 4;
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final IntegerProperty SERVINGS_LEFT = IntegerProperty.create("servings", 0, SERVINGS);
	private final FeastDish dish;
	private final VoxelShape whole;
	private final VoxelShape leftovers;

	public FeastBlock(FeastDish dish, Properties properties) {
		super(properties);
		this.dish = dish;
		whole = Block.box(dish.inset, 0, dish.inset, 16 - dish.inset, dish.height, 16 - dish.inset);
		leftovers = Block.box(dish.inset, 0, dish.inset, 16 - dish.inset, dish.leftovers, 16 - dish.inset);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SERVINGS_LEFT, SERVINGS));
	}

	public FeastDish dish() {
		return dish;
	}

	/** The light of a feast of {@code dish}: its own while any is left (the gleaming salad's glow berries). */
	public static ToIntFunction<BlockState> light(FeastDish dish) {
		return state -> state.getValue(SERVINGS_LEFT) > 0 ? dish.light : 0;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(SERVINGS_LEFT) > 0 ? whole : leftovers;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	/** A bowl takes a serving away. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (!stack.is(Items.BOWL) || state.getValue(SERVINGS_LEFT) == 0) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide()) {
			ItemStack serving = new ItemStack(JugcraftAgriculture.item(dish.serving()));
			stack.consume(1, player);
			if (!player.getInventory().add(serving)) {
				Block.popResource(level, pos, serving);
			}
			level.playSound(null, pos, SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.BLOCKS, 0.6F, 1.2F);
			serve(level, pos, state, player);
		}
		return InteractionResult.SUCCESS;
	}

	/** A hungry player eats a serving; a full one is told to use a bowl. The leftovers are cleared. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (state.getValue(SERVINGS_LEFT) == 0) {
			if (!level.isClientSide()) {
				level.destroyBlock(pos, true, player);
			}
			return InteractionResult.SUCCESS;
		}
		if (!player.canEat(false)) {
			if (!level.isClientSide()) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.feast.full"));
			}
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		player.getFoodData().eat(dish.nutrition, dish.saturation);
		level.playSound(null, pos, SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
		level.gameEvent(player, GameEvent.EAT, pos);
		serve(level, pos, state, player);
		return InteractionResult.SUCCESS;
	}

	private static void serve(Level level, BlockPos pos, BlockState state, Player player) {
		level.setBlock(pos, state.setValue(SERVINGS_LEFT, state.getValue(SERVINGS_LEFT) - 1), Block.UPDATE_ALL);
		level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
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

	/** Servings left: 15 for a whole feast, down by a quarter a serving. */
	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return state.getValue(SERVINGS_LEFT) * 15 / SERVINGS;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, SERVINGS_LEFT);
	}
}
