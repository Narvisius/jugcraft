package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Haunted Dining Chair (Halloween decorations batch 19, the Poltergeist's Dinner Party): a high-backed gothic chair
 * of dark wood with a red velvet seat, facing the table it stands at. Use it to sit. At night, when a player comes
 * within {@value #REACH} blocks of an empty chair, it scrapes out from the table toward them ({@link #OUT}), by
 * {@value #SLIDE} pixels, and slides back {@value #OUT_TICKS} ticks later. The chair is drawn by the client where it
 * has slid to (client/DiningChairRenderer.java).
 */
public class HauntedDiningChairBlock extends HorizontalDirectionalBlock implements EntityBlock, Seat.Sittable {
	public static final double SEAT = 0.5;
	public static final double REACH = 2.0;
	public static final int CHECK_TICKS = 20;
	public static final int OUT_TICKS = 100;
	public static final float SLIDE = 6.0F;
	public static final BooleanProperty OUT = BooleanProperty.create("out");
	private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

	public HauntedDiningChairBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OUT, false));
	}

	@Override
	public double seatHeight(BlockState state) {
		return SEAT;
	}

	/** It faces away from whoever placed it, toward the table they stood across from. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player.isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}
		if (level instanceof ServerLevel server && Seat.sit(server, pos, state, player)) {
			server.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.6F, 0.8F);
		}
		return InteractionResult.SUCCESS;
	}

	/** Slides it out (or back), with a scrape. */
	static void slide(Level level, BlockPos pos, BlockState state, boolean out) {
		level.setBlock(pos, state.setValue(OUT, out), Block.UPDATE_ALL);
		level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.6F, out ? 0.6F : 0.75F);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new HauntedDiningChairBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != JugcraftAgriculture.DINING_CHAIR_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> ((HauntedDiningChairBlockEntity) entity).serverTick((ServerLevel) tickLevel, pos, tickState);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, OUT);
	}
}
