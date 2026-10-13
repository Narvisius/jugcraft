package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.List;
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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Canning Kettle: a big speckled enamel pot with a jar rack, for sealing preserves in a boiling water bath
 * ({@link CanningKettleBlockEntity}). Set it over a heat source and use it:
 * <ul>
 * <li>with a water bucket, to fill it (an empty bucket takes the water back while no jars are in it);</li>
 * <li>with a full jar of preserves, fresh from the pot, to stand it in the water (up to four);</li>
 * <li>with an empty hand, to lift out the jars that have sealed (sneaking, every jar, sealed or not, or to see how it
 * is doing).</li>
 * </ul>
 * Once the water boils, a jar seals after twenty seconds in it. Comparators read how many jars have sealed.
 */
public class CanningKettleBlock extends BaseEntityBlock {
	private static final String MESSAGES = "message.jugcraft.canning_kettle.";
	private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 11.0, 15.0);

	public CanningKettleBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CanningKettleBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != JugcraftAgriculture.CANNING_KETTLE_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> ((CanningKettleBlockEntity) entity).serverTick((ServerLevel) tickLevel);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		boolean jar = stack.getItem() instanceof PreserveJarItem;
		if (!jar && !stack.is(Items.WATER_BUCKET) && !stack.is(Items.BUCKET)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof CanningKettleBlockEntity kettle)) {
			return InteractionResult.SUCCESS;
		}
		if (stack.is(Items.WATER_BUCKET)) {
			if (!kettle.water()) {
				kettle.fill();
				player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
				level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
				level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
			}
			return InteractionResult.SUCCESS;
		}
		if (stack.is(Items.BUCKET)) {
			if (kettle.drain()) {
				player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.WATER_BUCKET)));
				level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
				level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
			} else if (kettle.water()) {
				player.sendOverlayMessage(Component.translatable(MESSAGES + "jars_in"));
			}
			return InteractionResult.SUCCESS;
		}
		String refusal = !kettle.water() ? "no_water" : PreserveJarItem.sealed(stack) ? "already_sealed"
				: !PreserveJarItem.sealable(stack, level.getGameTime()) ? "opened" : kettle.jars().size() >= CanningKettleBlockEntity.JARS ? "full" : null;
		if (refusal != null) {
			player.sendOverlayMessage(Component.translatable(MESSAGES + refusal));
			return InteractionResult.SUCCESS;
		}
		kettle.add(stack);
		stack.consume(1, player);
		level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.8F, 0.6F);
		level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		return InteractionResult.SUCCESS;
	}

	/** An empty hand lifts out the sealed jars; sneaking, every jar (or, with none in, says how the kettle is doing). */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.getMainHandItem().isEmpty()) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof CanningKettleBlockEntity kettle)) {
			return InteractionResult.SUCCESS;
		}
		kettle.collectReturnedBucket(player);
		List<ItemStack> out = player.isSecondaryUseActive() ? kettle.takeAll() : kettle.takeSealed();
		if (out.isEmpty()) {
			if (!kettle.water()) {
				player.sendOverlayMessage(Component.translatable(MESSAGES + "no_water"));
			} else if (kettle.jars().isEmpty()) {
				player.sendOverlayMessage(Component.translatable(MESSAGES + (kettle.boiling() ? "boiling_empty" : "heating_empty")));
			} else {
				player.sendOverlayMessage(Component.translatable(MESSAGES + (kettle.boiling() ? "processing" : "heating"), kettle.jars().size(),
						(kettle.nextSeal() + 19) / 20));
			}
			return InteractionResult.SUCCESS;
		}
		int sealed = 0;
		for (ItemStack jar : out) {
			sealed += PreserveJarItem.sealed(jar) ? 1 : 0;
			if (!player.getInventory().add(jar)) {
				Block.popResource(level, pos.above(), jar);
			}
		}
		if (sealed > 0) {
			level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.7F, 2.0F);
		}
		player.sendOverlayMessage(Component.translatable(MESSAGES + "lifted", out.size(), sealed));
		level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		return InteractionResult.SUCCESS;
	}

	/** Boiling water bubbles and steams (client only). */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!(level.getBlockEntity(pos) instanceof CanningKettleBlockEntity kettle) || !kettle.boiling()) {
			return;
		}
		double x = pos.getX() + 0.25 + random.nextDouble() * 0.5;
		double z = pos.getZ() + 0.25 + random.nextDouble() * 0.5;
		level.addParticle(ParticleTypes.BUBBLE_POP, x, pos.getY() + 0.62, z, 0.0, 0.02, 0.0);
		if (random.nextInt(4) == 0) {
			level.addParticle(ParticleTypes.WHITE_SMOKE, x, pos.getY() + 0.75, z, 0.0, 0.02, 0.0);
		}
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	/** How many jars have sealed: 0 to 15 for none to four. */
	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		int sealed = level.getBlockEntity(pos) instanceof CanningKettleBlockEntity kettle ? kettle.sealedCount() : 0;
		return sealed * 15 / CanningKettleBlockEntity.JARS;
	}
}
