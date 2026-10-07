package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
 * The Bat House: a slatted wooden roost hung on a wall or a post, its landing ledge below ({@link BatHouseBlockEntity}).
 * Bats roost in it by day and pour out at dusk; at dawn the bats nearby come back in, and each leaves a guano on the ledge
 * (shown in the block, {@link #GUANO}). With an empty hand, use it to scoop the guano up (and hear how many bats roost);
 * comparators read the roosting bats.
 */
public class BatHouseBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	/** How much guano shows on the ledge: none, a little, more, a pile. */
	public static final IntegerProperty GUANO = IntegerProperty.create("guano", 0, 3);
	private static final String MESSAGES = "message.jugcraft.bat_house.";
	private static final Map<Direction, VoxelShape> SHAPES = Map.of(
			Direction.NORTH, Block.box(2.0, 0.0, 9.0, 14.0, 15.0, 16.0),
			Direction.SOUTH, Block.box(2.0, 0.0, 0.0, 14.0, 15.0, 7.0),
			Direction.WEST, Block.box(9.0, 0.0, 2.0, 16.0, 15.0, 14.0),
			Direction.EAST, Block.box(0.0, 0.0, 2.0, 7.0, 15.0, 14.0));

	public BatHouseBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(GUANO, 0));
	}

	/** The ledge's look for {@code guano} guano: 0, then 1 to 3 as it piles up. */
	public static int guanoLevel(int guano) {
		return guano <= 0 ? 0 : guano < 6 ? 1 : guano < 12 ? 2 : 3;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	/** Hung on the side of a block, it faces out from it; set on top of one, it faces the player. */
	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction face = context.getClickedFace();
		Direction facing = face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite();
		return defaultBlockState().setValue(FACING, facing);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BatHouseBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != JugcraftAgriculture.BAT_HOUSE_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> ((BatHouseBlockEntity) entity).serverTick((ServerLevel) tickLevel);
	}

	/** An empty hand scoops up the guano, and says how many bats roost. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof BatHouseBlockEntity house)) {
			return InteractionResult.SUCCESS;
		}
		int guano = house.takeGuano();
		if (guano > 0) {
			ItemStack scooped = new ItemStack(JugcraftAgriculture.item("bat_guano"), guano);
			if (!player.getInventory().add(scooped)) {
				Block.popResource(level, pos, scooped);
			}
			level.playSound(null, pos, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.BLOCKS, 0.8F, 1.0F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		player.sendOverlayMessage(Component.translatable(MESSAGES + (guano > 0 ? "scooped" : "status"), house.residents(),
				BatHouseBlockEntity.CAPACITY, guano));
		return InteractionResult.SUCCESS;
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	/** The bats roosting: 0 to 15 for none to a full house. */
	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		int bats = level.getBlockEntity(pos) instanceof BatHouseBlockEntity house ? house.residents() : 0;
		return bats * 15 / BatHouseBlockEntity.CAPACITY;
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
		builder.add(FACING, GUANO);
	}
}
