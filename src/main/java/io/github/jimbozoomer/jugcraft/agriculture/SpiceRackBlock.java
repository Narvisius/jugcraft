package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Spice Rack (garden crops, herbs and spices, part b; tools/spices.py RACK): a shallow wooden rack of two shelves,
 * its back against the block behind it, for showing off spices ({@link SpiceRackBlockEntity}). Use it holding a spice to
 * put one up, four to a shelf; use it with an empty hand to take down the last (holding anything else, nothing happens).
 * Comparators read how full it is.
 */
public class SpiceRackBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	private static final String MESSAGES = "message.jugcraft.spice_rack.";
	/** Facing north, its back is the south half of the block (tools/spice_data.py draws it so). */
	private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Block.box(0.0, 0.0, 9.0, 16.0, 16.0, 16.0));

	public SpiceRackBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SpiceRackBlockEntity(pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (!SpiceRackBlockEntity.fits(stack)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SpiceRackBlockEntity rack) {
			if (rack.store(stack)) {
				stack.consume(1, player);
				level.playSound(null, pos, SoundEvents.CHISELED_BOOKSHELF_INSERT, SoundSource.BLOCKS, 0.6F, 1.6F);
				level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
			} else {
				player.sendOverlayMessage(Component.translatable(MESSAGES + "full"));
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.getMainHandItem().isEmpty()) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SpiceRackBlockEntity rack) {
			ItemStack spice = rack.takeLast();
			if (spice.isEmpty()) {
				player.sendOverlayMessage(Component.translatable(MESSAGES + "empty"));
			} else {
				if (!player.getInventory().add(spice)) {
					Block.popResource(level, pos, spice);
				}
				level.playSound(null, pos, SoundEvents.CHISELED_BOOKSHELF_PICKUP, SoundSource.BLOCKS, 0.6F, 1.6F);
				level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	/** How full it is: 0 when empty, 15 with every slot filled. */
	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		int count = level.getBlockEntity(pos) instanceof SpiceRackBlockEntity rack ? rack.count() : 0;
		return count == 0 ? 0 : 1 + (count - 1) * 14 / (SpiceRackBlockEntity.SLOTS - 1);
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
