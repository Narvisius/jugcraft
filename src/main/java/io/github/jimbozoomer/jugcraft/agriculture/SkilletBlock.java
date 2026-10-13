package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.EnumMap;
import java.util.Map;
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
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Skillet (the Farmhouse Kitchen, tools/kitchen.py SKILLET): a cast-iron pan with a wooden handle in the owner's
 * textures, set on any heat source (on a lit Kitchen Stove or campfire, over fire, lava or magma). Use anything a campfire
 * cooks on it to put up to {@value SkilletBlockEntity#CAPACITY} of one food in; it fries them one after another
 * ({@link SkilletBlockEntity}) and they gather in the pan. Use it with an empty hand to take everything back out.
 */
public class SkilletBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	private static final double[][] BOXES = {{3, 0, 4, 13, 3, 14}, {7, 1.5, 0, 9, 2.5, 4}};
	private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

	static {
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			SHAPES.put(facing, LongDecorationBlock.shape(BOXES, facing));
		}
	}

	public SkilletBlock(Properties properties) {
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
		return new SkilletBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != JugcraftAgriculture.SKILLET_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> ((SkilletBlockEntity) entity).serverTick((ServerLevel) tickLevel, pos);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (stack.isEmpty()) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		// Anything the pan can't fry is used as usual; it must not count as an empty hand, which takes the food out.
		if (!KitchenStoveBlock.cookable(level, stack) || !(level.getBlockEntity(pos) instanceof SkilletBlockEntity skillet)) {
			return InteractionResult.PASS;
		}
		if (level instanceof ServerLevel server) {
			SkilletBlockEntity.Added added = skillet.add(server, stack, player);
			switch (added) {
				case ADDED -> {
					level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.3F, 1.6F);
					level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
					if (!CookingPotBlockEntity.isHeated(level, pos)) {
						player.sendOverlayMessage(Component.translatable("message.jugcraft.skillet.cold"));
					}
				}
				case OTHER -> player.sendOverlayMessage(Component.translatable("message.jugcraft.skillet.other"));
				case FULL -> {
				}
				case UNCOOKABLE -> {
					return InteractionResult.PASS;
				}
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** An empty hand takes everything back out of the pan, fried and not. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof SkilletBlockEntity skillet) || skillet.isEmpty()) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			for (ItemStack stack : skillet.takeAll()) {
				if (!stack.isEmpty() && !player.getInventory().add(stack)) {
					Block.popResource(level, pos, stack);
				}
			}
			level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.4F, 1.2F);
		}
		return InteractionResult.SUCCESS;
	}

	/** While something fries over heat: steam, a little smoke and the sizzle (client only). */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!(level.getBlockEntity(pos) instanceof SkilletBlockEntity skillet) || skillet.raw().isEmpty()
				|| !CookingPotBlockEntity.isHeated(level, pos)) {
			return;
		}
		double x = pos.getX() + 0.3 + random.nextDouble() * 0.4;
		double z = pos.getZ() + 0.35 + random.nextDouble() * 0.4;
		level.addParticle(ParticleTypes.WHITE_SMOKE, x, pos.getY() + 0.2, z, 0.0, 0.03, 0.0);
		if (random.nextInt(3) == 0) {
			level.addParticle(ParticleTypes.SMOKE, x, pos.getY() + 0.2, z, 0.0, 0.02, 0.0);
		}
		if (random.nextInt(6) == 0) {
			level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS,
					0.15F, 1.8F + random.nextFloat() * 0.2F, false);
		}
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
