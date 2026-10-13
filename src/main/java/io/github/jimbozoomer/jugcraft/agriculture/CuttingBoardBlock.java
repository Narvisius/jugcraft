package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Cutting Board (the Farmhouse Kitchen, tools/kitchen.py BOARD): a wooden board in the owner's texture. Use
 * anything on the empty board to set one of it there (to cut, or just to show). Use a knife (item tag {@code jugcraft:knives})
 * on it to cut what lies there by a {@link CuttingRecipe}: the pieces pop up off the board and the knife loses one
 * durability. Use it with an empty hand to take back what lies there.
 */
public class CuttingBoardBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	private static final double[][] BOXES = {{1, 0, 0.5, 15, 1, 15.5}};
	private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

	static {
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			SHAPES.put(facing, LongDecorationBlock.shape(BOXES, facing));
		}
	}

	public CuttingBoardBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CuttingBoardBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (stack.isEmpty() || !(level.getBlockEntity(pos) instanceof CuttingBoardBlockEntity board)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (stack.is(JugcraftAgriculture.KNIVES)) {
			if (level instanceof ServerLevel server) {
				cut(server, pos, board, stack, player, hand);
			}
			return InteractionResult.SUCCESS;
		}
		if (!board.item().isEmpty()) {
			// The board is taken: the item is used as usual (eaten, placed beside), not swapped for what lies there.
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			board.put(stack);
			stack.consume(1, player);
			level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.6F, 1.4F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		return InteractionResult.SUCCESS;
	}

	private static void cut(ServerLevel level, BlockPos pos, CuttingBoardBlockEntity board, ItemStack knife, Player player,
			InteractionHand hand) {
		ItemStack item = board.item();
		if (item.isEmpty()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.cutting_board.nothing"));
			return;
		}
		Optional<CuttingRecipe> recipe = CuttingRecipe.find(level.getServer(), item, knife);
		if (recipe.isEmpty()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.cutting_board.uncuttable"));
			return;
		}
		board.take();
		for (ItemStack piece : recipe.get().cut()) {
			Block.popResource(level, pos.above(), piece);
		}
		knife.hurtAndBreak(1, player, hand);
		level.playSound(null, pos, SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.BLOCKS, 0.7F, 1.6F);
		level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
	}

	/** An empty hand takes back what lies on the board. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CuttingBoardBlockEntity ready && ready.companionKitchen.handOver(player))return InteractionResult.SUCCESS;
		if (!(level.getBlockEntity(pos) instanceof CuttingBoardBlockEntity board) || board.item().isEmpty()) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			ItemStack item = board.take();
			if (!player.getInventory().add(item)) {
				Block.popResource(level, pos, item);
			}
		}
		return InteractionResult.SUCCESS;
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
}
