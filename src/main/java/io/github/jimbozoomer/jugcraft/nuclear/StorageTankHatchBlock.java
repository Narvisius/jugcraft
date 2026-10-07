package io.github.jimbozoomer.jugcraft.nuclear;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A storage tank's roof hatch: a cell of the roof (it counts as a plate, so the tank forms round it) with a square
 * opening, a raised coaming round it, a galvanised safety grate across it and a hinged lid on top. Right-click swings
 * the lid up and back over its hinges (client/StorageTankHatchRenderer draws it swinging); looking down through the
 * grate you see the inside of the tank and the liquid at its level (drawn by the outlet's renderer). The grate is
 * walkable and keeps the tank closed, so nothing gets in and nothing spawns inside. FACING is the side the hinges are
 * on: the lid opens away from whoever placed it.
 *
 * <p>With the lid up, a bucket of liquid (or any full fluid container) right-clicked on the hatch is poured in through
 * the grate (Justin, round 14): straight into the tank, valves or not. The tank holds one liquid, so another is
 * refused, and so is a bucket it has no room for. With the lid down, the click opens it first.
 */
public class StorageTankHatchBlock extends StorageTankPlateBlock implements EntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

	// The roof cell with its opening (x, z 3..13), the grate in it, the coaming above the roof, the lid on top.
	private static final VoxelShape FRAME = Shapes.or(Block.box(0, 0, 0, 16, 16, 3), Block.box(0, 0, 13, 16, 16, 16),
			Block.box(0, 0, 3, 3, 16, 13), Block.box(13, 0, 3, 16, 16, 13));
	private static final VoxelShape COAMING = Shapes.or(Block.box(2.5, 16, 2.5, 13.5, 19, 3), Block.box(2.5, 16, 13, 13.5, 19, 13.5),
			Block.box(2.5, 16, 3, 3, 19, 13), Block.box(13, 16, 3, 13.5, 19, 13));
	private static final VoxelShape OPEN_SHAPE = Shapes.or(FRAME, Block.box(3, 14.6, 3, 13, 15.6, 13), COAMING).optimize();
	private static final VoxelShape CLOSED_SHAPE = Shapes.or(Shapes.block(), Block.box(2, 16, 2, 14, 19.8, 14)).optimize();

	public StorageTankHatchBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(OPEN, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FACING, OPEN);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(OPEN) ? OPEN_SHAPE : CLOSED_SHAPE;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	/** Lid up, a fluid container in hand: pour it in. Otherwise the lid swings ({@link #useWithoutItem}). */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		Storage<FluidVariant> held = FluidStorage.ITEM.find(stack, ContainerItemContext.forPlayerInteraction(player, hand));
		if (!state.getValue(OPEN) || held == null) {
			return super.useItemOn(stack, state, level, pos, player, hand, hit);
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		StorageTankPortBlockEntity tank = tankOf(level, state, pos);
		if (tank == null) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.storage_tank.not_formed"));
			return InteractionResult.SUCCESS;
		}
		FluidVariant pouring = StorageUtil.findStoredResource(held);
		if (FluidStorageUtil.interactWithFluidStorage(FilteringStorage.insertOnlyOf(tank.fluid), player, hand)) {
			player.sendOverlayMessage(tank.contents());
		} else if (pouring == null || pouring.isBlank()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.storage_tank.hatch", tank.contents()));
		} else if (!tank.fluid.isResourceBlank() && tank.fluid.amount > 0 && !tank.fluid.variant.equals(pouring)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.storage_tank.hatch_other",
					FluidVariantAttributes.getName(tank.fluid.variant), FluidVariantAttributes.getName(pouring)));
		} else {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.storage_tank.hatch_full", tank.contents()));
		}
		return InteractionResult.SUCCESS;
	}

	/** The formed tank this hatch is in (found along the shell to its outlet), or null. */
	public static @Nullable StorageTankPortBlockEntity tankOf(Level level, BlockState state, BlockPos pos) {
		if (!state.getValue(FORMED)) {
			return null;
		}
		BlockPos outlet = StorageTanks.outletOf(level, pos);
		if (outlet != null && level.getBlockEntity(outlet) instanceof StorageTankPortBlockEntity port) {
			StorageTankPortBlockEntity tank = port.tank();
			return tank != null && tank.formed() ? tank : null;
		}
		return null;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			boolean open = !state.getValue(OPEN);
			level.setBlock(pos, state.setValue(OPEN, open), Block.UPDATE_ALL);
			level.playSound(null, pos, open ? SoundEvents.IRON_TRAPDOOR_OPEN : SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.8F,
					open ? 0.8F : 0.7F);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new StorageTankHatchBlockEntity(pos, state);
	}
}
