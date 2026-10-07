package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Halloween Bonfire: a tall cone of logs in a ring of stones, burning with flames two blocks high (light
 * {@value #LIGHT}; drawn by the client, client/HalloweenBonfireRenderer.java). It cooks whatever a campfire cooks, on
 * {@value HalloweenBonfireBlockEntity#SLOTS} skewers at once and {@value HalloweenBonfireBlockEntity#SPEED} times as
 * fast ({@link HalloweenBonfireBlockEntity}); use food on it to put it on a skewer. Hold a Marshmallow on a Stick over it
 * to toast it ({@link MarshmallowStickItem}). It burns whatever stands on it. A shovel puts it out; flint and steel or a
 * fire charge lights it again.
 */
public class HalloweenBonfireBlock extends BaseEntityBlock {
	public static final int LIGHT = 15;
	public static final BooleanProperty LIT = BooleanProperty.create("lit");
	private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);

	public HalloweenBonfireBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(LIT, true));
	}

	public static int light(BlockState state) {
		return state.getValue(LIT) ? LIGHT : 0;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new HalloweenBonfireBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || !state.getValue(LIT) || type != JugcraftAgriculture.BONFIRE_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> ((HalloweenBonfireBlockEntity) entity).serverTick((ServerLevel) tickLevel);
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
		if (state.getValue(LIT) && level.getBlockEntity(pos) instanceof HalloweenBonfireBlockEntity bonfire) {
			if (level instanceof ServerLevel server) {
				if (bonfire.cook(server, stack, player)) {
					level.playSound(null, pos, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.2F);
					level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
					return InteractionResult.SUCCESS;
				}
			} else if (bonfire.cookable(stack)) {
				return InteractionResult.SUCCESS;
			}
		}
		return CandleLighting.light(stack, state, level, pos, player, hand, LIT);
	}

	/** It burns whatever stands on it while it is lit. */
	@Override
	public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
		if (state.getValue(LIT) && level instanceof ServerLevel server && entity instanceof LivingEntity living) {
			living.hurtServer(server, level.damageSources().campfire(), 1.0F);
		}
		super.stepOn(level, pos, state, entity);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LIT)) {
			return;
		}
		if (random.nextInt(8) == 0) {
			level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS,
					1.0F + random.nextFloat(), random.nextFloat() * 0.7F + 0.6F, false);
		}
		level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 2.2,
				pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.07, 0.0);
		if (random.nextInt(3) == 0) {
			level.addParticle(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 0.0, 0.0, 0.0);
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}
}
