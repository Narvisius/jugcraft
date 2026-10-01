package io.github.jimbozoomer.jugcraft.farming;

import io.github.jimbozoomer.jugcraft.chemistry.PetroItems;
import io.github.jimbozoomer.jugcraft.fluid.FluidConnectable;
import io.github.jimbozoomer.jugcraft.fluid.FluidNetworks;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * A sprinkler on a post: pipes or buckets fill its water tank, and every {@link SprinklerBlockEntity#PULSE_TICKS} it
 * sprays the crops around it (see {@link SprinklerBlockEntity}). Use fertilizer on it to load its hopper. It shows
 * spray while it holds water.
 */
public class SprinklerBlock extends Block implements EntityBlock, FluidConnectable {
	public static final BooleanProperty WET = BooleanProperty.create("wet");

	public SprinklerBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(WET, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(WET);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SprinklerBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel) || type != JugcraftFarming.SPRINKLER_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> ((SprinklerBlockEntity) entity).serverTick((ServerLevel) tickLevel, pos, tickState);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof SprinklerBlockEntity sprinkler)) {
			return super.useItemOn(stack, state, level, pos, player, hand, hit);
		}
		if (stack.is(PetroItems.FERTILIZER)) {
			if (!level.isClientSide()) {
				int added = sprinkler.addFertilizer(stack.getCount());
				stack.shrink(added);
			}
			return InteractionResult.SUCCESS;
		}
		if (FluidStorageUtil.interactWithFluidStorage(sprinkler.water, player, hand)) {
			return InteractionResult.SUCCESS;
		}
		return super.useItemOn(stack, state, level, pos, player, hand, hit);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SprinklerBlockEntity sprinkler) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.sprinkler",
					sprinkler.water.amount / FluidNetworks.DROPLETS_PER_MB, sprinkler.fertilizer()));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(WET)) {
			return;
		}
		for (int i = 0; i < 2; i++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double speed = 0.15 + random.nextDouble() * 0.1;
			level.addParticle(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.95, pos.getZ() + 0.5,
					Math.cos(angle) * speed, 0.2, Math.sin(angle) * speed);
		}
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		FluidNetworks.invalidate(level);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		FluidNetworks.invalidate(level);
	}
}
