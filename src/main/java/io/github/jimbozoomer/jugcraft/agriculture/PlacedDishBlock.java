package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
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
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A dish from the menu set down (the kitchen and cooking expansion's slice 3, {@link MenuDishes}; tools/menu.py): a
 * sneaking player uses the food on a block to set it down as a 3D model of the owner's icon, turned to face them, and
 * takes it back by using it with an empty hand. It is only ever the one food it was: it drops that food when broken,
 * gives it back when taken, and has no item of its own (the block shares its food's ID).
 */
public class PlacedDishBlock extends Block {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

	/** How a set-down dish stands (its model's template in tools/menu_data.py), for its outline and collision. */
	public enum DishShape {
		BOWL(Block.box(3.0, 0.0, 3.0, 13.0, 6.5, 13.0)),
		PLATE(Block.box(1.0, 0.0, 1.0, 15.0, 5.5, 15.0)),
		STACK(Block.box(3.0, 0.0, 3.0, 13.0, 6.0, 13.0)),
		FLAT(Block.box(1.0, 0.0, 1.0, 15.0, 1.0, 15.0)),
		STAND(Block.box(4.0, 0.0, 4.0, 12.0, 14.0, 12.0)),
		BOX(Block.box(3.0, 0.0, 3.0, 13.0, 13.5, 13.0)),
		/** The owner's milkshake glass, round its band and up to the top of its straw. */
		MILKSHAKE(Block.box(4.75, 0.0, 4.75, 11.25, 15.75, 11.25));

		final VoxelShape shape;

		DishShape(VoxelShape shape) {
			this.shape = shape;
		}
	}

	private final Item dish;
	private final DishShape shape;

	public PlacedDishBlock(Item dish, DishShape shape, Properties properties) {
		super(properties);
		this.dish = dish;
		this.shape = shape;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	public Item dish() {
		return dish;
	}

	public DishShape dishShape() {
		return shape;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape.shape;
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(dish);
	}

	/** Only an empty hand takes the dish back; anything held is used as usual. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		return stack.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
	}

	/** Takes the dish back into the empty hand (or the inventory, or drops it when that is full). */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			ItemStack food = new ItemStack(dish);
			level.removeBlock(pos, false);
			if (player.getMainHandItem().isEmpty()) {
				player.setItemInHand(InteractionHand.MAIN_HAND, food);
			} else if (!player.getInventory().add(food)) {
				Block.popResource(level, pos, food);
			}
			level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.4F, 1.2F);
			level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
		}
		return InteractionResult.SUCCESS;
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
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	/**
	 * Sets the dish a sneaking player holds down where they use it, facing them, as a block item would place it; PASS
	 * when they aren't sneaking with a dish in {@code dishes} or it can't go there.
	 */
	public static InteractionResult setDown(Map<Item, PlacedDishBlock> dishes, Player player, Level level, InteractionHand hand,
			BlockHitResult hit) {
		ItemStack stack = player.getItemInHand(hand);
		PlacedDishBlock dish = dishes.get(stack.getItem());
		if (dish == null || player.isSpectator() || !player.isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}
		BlockPlaceContext context = new BlockPlaceContext(new UseOnContext(player, hand, hit));
		BlockPos pos = context.getClickedPos();
		BlockState state = dish.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
		if (!context.canPlace() || !state.canSurvive(level, pos) || !player.mayUseItemAt(pos, hit.getDirection(), stack)
				|| !level.isUnobstructed(state, pos, CollisionContext.empty())) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			level.setBlock(pos, state, Block.UPDATE_ALL);
			level.playSound(null, pos, dish.defaultBlockState().getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.8F, 1.1F);
			level.gameEvent(player, GameEvent.BLOCK_PLACE, pos);
			stack.consume(1, player);
		}
		return InteractionResult.SUCCESS;
	}
}
