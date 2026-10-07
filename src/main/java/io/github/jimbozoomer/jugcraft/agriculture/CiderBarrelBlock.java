package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
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
 * The Cider Barrel: an oak cask on its side in a cradle, with a brass tap at its head ({@link CiderBarrelBlockEntity}
 * holds the cider). Pour Sweet Cider in by the bottle and leave it: it ferments into Sparkling Cider in a day and matures
 * into Aged Cider in three. Use a glass bottle on it to draw off a serving of whatever it has become; use it with an empty
 * hand to see how far along it is. The chalk mark on its head ({@link #CIDER}: 0 empty, then sweet, sparkling, aged) shows
 * the stage, and a sparkling batch fizzes at the bung. Broken, it keeps its cider (and the cider keeps ageing).
 * Comparators read how full it is.
 */
public class CiderBarrelBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final IntegerProperty CIDER = IntegerProperty.create("cider", 0, 3);
	private static final String MESSAGES = "message.jugcraft.cider_barrel.";
	private static final VoxelShape ALONG_X = Block.box(0.0, 0.0, 1.0, 16.0, 15.0, 15.0);
	private static final VoxelShape ALONG_Z = Block.box(1.0, 0.0, 0.0, 15.0, 15.0, 16.0);

	public CiderBarrelBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CIDER, 0));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(FACING).getAxis() == Direction.Axis.Z ? ALONG_Z : ALONG_X;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CiderBarrelBlockEntity(pos, state);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != JugcraftAgriculture.CIDER_BARREL_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> ((CiderBarrelBlockEntity) entity).serverTick((ServerLevel) tickLevel, pos, tickState);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		boolean fresh = stack.is(JugcraftAgriculture.item("sweet_cider"));
		if (!fresh && !stack.is(Items.GLASS_BOTTLE)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof CiderBarrelBlockEntity barrel)) {
			return InteractionResult.SUCCESS;
		}
		long now = level.getGameTime();
		if (fresh) {
			if (!barrel.canFill(now)) {
				player.sendOverlayMessage(Component.translatable(MESSAGES + (barrel.servings() >= CiderBarrelBlockEntity.CAPACITY ? "full" : "fermenting")));
				return InteractionResult.SUCCESS;
			}
			barrel.fill(now);
			player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
			level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 0.9F);
			level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
			return InteractionResult.SUCCESS;
		}
		if (barrel.servings() == 0) {
			player.sendOverlayMessage(Component.translatable(MESSAGES + "empty"));
			return InteractionResult.SUCCESS;
		}
		String drink = barrel.draw(now);
		player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(JugcraftAgriculture.item(drink))));
		level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 0.8F);
		level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
		return InteractionResult.SUCCESS;
	}

	/** An empty hand shows how much is in it and how far along it is. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof CiderBarrelBlockEntity barrel)) {
			return InteractionResult.SUCCESS;
		}
		long now = level.getGameTime();
		if (barrel.servings() == 0) {
			player.sendOverlayMessage(Component.translatable(MESSAGES + "empty"));
			return InteractionResult.SUCCESS;
		}
		int stage = barrel.stage(now);
		Component drink = Component.translatable("item.jugcraft." + CiderBarrelBlockEntity.STAGES.get(stage));
		if (stage == 2) {
			player.sendOverlayMessage(Component.translatable(MESSAGES + "aged", barrel.servings(), CiderBarrelBlockEntity.CAPACITY, drink));
		} else {
			long minutes = (barrel.untilNext(now) + 1199) / 1200;
			Component next = Component.translatable("item.jugcraft." + CiderBarrelBlockEntity.STAGES.get(stage + 1));
			player.sendOverlayMessage(Component.translatable(MESSAGES + "ageing", barrel.servings(), CiderBarrelBlockEntity.CAPACITY, drink,
					next, minutes));
		}
		return InteractionResult.SUCCESS;
	}

	/** A sparkling batch fizzes at the bung (client only). */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(CIDER) == 2 && random.nextInt(3) == 0) {
			level.addParticle(ParticleTypes.BUBBLE_POP, pos.getX() + 0.45 + random.nextDouble() * 0.1, pos.getY() + 0.95,
					pos.getZ() + 0.45 + random.nextDouble() * 0.1, 0.0, 0.02, 0.0);
		}
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	/** How full it is: 0 when empty, 15 when full. */
	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		int servings = level.getBlockEntity(pos) instanceof CiderBarrelBlockEntity barrel ? barrel.servings() : 0;
		return servings == 0 ? 0 : 1 + (servings - 1) * 14 / (CiderBarrelBlockEntity.CAPACITY - 1);
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
		builder.add(FACING, CIDER);
	}
}
