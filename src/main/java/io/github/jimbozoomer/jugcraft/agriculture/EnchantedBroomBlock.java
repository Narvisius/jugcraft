package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Enchanted Broom (Halloween decorations batch 17): a besom on a crooked handle with an amethyst bound into its
 * twigs, standing on its bristles. Rub it with Flying Ointment (the hex brew) and it wakes for {@value #CHARGE_TICKS}
 * ticks ({@link #CHARGED}): it sweeps the floor round it ({@link EnchantedBroomBlockEntity}), leaning and swishing toward
 * the dropped items while it does ({@link #SWEEPING}; drawn by the client). The bottle comes back. Nothing collides with
 * it.
 */
public class EnchantedBroomBlock extends BaseEntityBlock {
	public static final int RANGE = 4;
	public static final int SWEEP_TICKS = 20;
	public static final int MAX_MOVES = 16;
	public static final int CHARGE_TICKS = 72000;
	public static final double PAN_REACH = 1.25;
	public static final double PUSH_SPEED = 0.22;
	public static final BooleanProperty CHARGED = BooleanProperty.create("charged");
	public static final BooleanProperty SWEEPING = BooleanProperty.create("sweeping");
	private static final VoxelShape SHAPE = Block.box(5.0, 0.0, 5.0, 11.0, 16.0, 11.0);

	public EnchantedBroomBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(CHARGED, false).setValue(SWEEPING, false));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new EnchantedBroomBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || !state.getValue(CHARGED) || type != JugcraftAgriculture.ENCHANTED_BROOM_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> ((EnchantedBroomBlockEntity) entity).serverTick((ServerLevel) tickLevel, pos, tickState);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	/** Flying Ointment wakes it (the bottle comes back); it refuses more while it is still nearly full. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (!stack.is(Hexes.draught(BubblingCauldronBlock.Brew.FLYING)) || !(level.getBlockEntity(pos) instanceof EnchantedBroomBlockEntity broom)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide()) {
			if (!broom.anoint()) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.enchanted_broom.full"));
				return InteractionResult.FAIL;
			}
			player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
			level.setBlock(pos, state.setValue(CHARGED, true), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.8F, 1.6F);
			((ServerLevel) level).sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 14, 0.25, 0.3, 0.25, 0.02);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.enchanted_broom.anointed"));
		}
		return InteractionResult.SUCCESS;
	}

	/** Charged, its bristles shed a sparkle now and then. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(CHARGED) && random.nextInt(5) == 0) {
			level.addParticle(ParticleTypes.WITCH, pos.getX() + 0.35 + random.nextDouble() * 0.3, pos.getY() + 0.1, pos.getZ() + 0.35 + random.nextDouble() * 0.3,
					0.0, 0.02, 0.0);
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(CHARGED, SWEEPING);
	}
}
