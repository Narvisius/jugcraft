package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.RecipePropertySet;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Kitchen Stove (the Farmhouse Kitchen, tools/kitchen.py STOVE): a brick range in the owner's textures. Flint and
 * steel or a fire charge lights it and a shovel puts it out, like a campfire; it burns no fuel. Lit, it gives light
 * {@value #LIGHT} and heats the block on top of it (block tag {@code jugcraft:heat_sources}), so a Cooking Pot, a
 * Skillet or any kettle cooks on it, and its top burns whatever stands on it as a magma block does. With nothing on
 * top, its hob cooks up to {@value KitchenStoveBlockEntity#SLOTS} foods ({@link KitchenStoveBlockEntity}): use anything
 * a campfire cooks on its top to put it on. Anything else (a pot, a skillet, a bucket) is placed or used as usual.
 */
public class KitchenStoveBlock extends BaseEntityBlock {
	public static final int LIGHT = 13;
	public static final float BURN = 1.0F;
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	public KitchenStoveBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
	}

	public static int light(BlockState state) {
		return state.getValue(LIT) ? LIGHT : 0;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new KitchenStoveBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || !state.getValue(LIT) || type != JugcraftAgriculture.KITCHEN_STOVE_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> ((KitchenStoveBlockEntity) entity).serverTick((ServerLevel) tickLevel, pos);
	}

	/**
	 * Whether a campfire recipe cooks {@code stack}, so it goes on the hob or in a skillet. Both sides know the campfire's
	 * inputs (as vanilla's campfire asks), so the client predicts the same as the server.
	 */
	public static boolean cookable(Level level, ItemStack stack) {
		return !stack.isEmpty() && level.recipeAccess().propertySet(RecipePropertySet.CAMPFIRE_INPUT).test(stack);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
		if(!level.isClientSide() && level.getBlockEntity(pos) instanceof KitchenStoveBlockEntity stove && stove.companionKitchen.handOver(player))return InteractionResult.SUCCESS;
		return InteractionResult.PASS;
	}
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (stack.is(ItemTags.SHOVELS) && state.getValue(LIT)) {
			if (!level.isClientSide()) {
				level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL);
				level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.8F);
				level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
			}
			return InteractionResult.SUCCESS;
		}
		InteractionResult lighting = CandleLighting.light(stack, state, level, pos, player, hand, LIT);
		if (lighting != InteractionResult.TRY_WITH_EMPTY_HAND) {
			return lighting;
		}
		if (hit.getDirection() != Direction.UP || !cookable(level, stack) || !(level.getBlockEntity(pos) instanceof KitchenStoveBlockEntity stove)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.getBlockState(pos.above()).isAir()) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (level instanceof ServerLevel server) {
			KitchenStoveBlockEntity.Placed placed = stove.place(server, stack, player);
			if (placed == KitchenStoveBlockEntity.Placed.PLACED) {
				level.playSound(null, pos, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.2F);
				level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
				if (!state.getValue(LIT)) {
					player.sendOverlayMessage(Component.translatable("message.jugcraft.kitchen_stove.unlit"));
				}
			} else if (placed == KitchenStoveBlockEntity.Placed.FULL) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.kitchen_stove.full"));
			} else {
				return InteractionResult.TRY_WITH_EMPTY_HAND;
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** Its hot top burns whatever stands on it while it is lit, unless it is sneaking (as a magma block does). */
	@Override
	public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
		if (state.getValue(LIT) && !entity.isSteppingCarefully() && level instanceof ServerLevel server && entity instanceof LivingEntity living
				&& !living.fireImmune()) {
			living.hurtServer(server, level.damageSources().hotFloor(), BURN);
		}
		super.stepOn(level, pos, state, entity);
	}

	/** Lit: flames in the firebox at the front, a little smoke from the grill, and the fire's crackle. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LIT)) {
			return;
		}
		Direction front = state.getValue(FACING);
		double x = pos.getX() + 0.5 + front.getStepX() * 0.52;
		double z = pos.getZ() + 0.5 + front.getStepZ() * 0.52;
		double across = random.nextDouble() * 0.4 - 0.2;
		x += front.getAxis() == Direction.Axis.Z ? across : 0.0;
		z += front.getAxis() == Direction.Axis.X ? across : 0.0;
		level.addParticle(ParticleTypes.FLAME, x, pos.getY() + 0.15 + random.nextDouble() * 0.25, z, 0.0, 0.0, 0.0);
		if (random.nextInt(4) == 0) {
			level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 1.05,
					pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.02, 0.0);
		}
		if (random.nextInt(10) == 0) {
			level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS,
					0.6F + random.nextFloat() * 0.4F, random.nextFloat() * 0.7F + 0.6F, false);
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
		builder.add(FACING, LIT);
	}
}
