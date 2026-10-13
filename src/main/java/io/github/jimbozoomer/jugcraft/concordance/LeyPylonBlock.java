package io.github.jimbozoomer.jugcraft.concordance;

import io.github.jimbozoomer.jugcraft.concordance.sign.Presentation;
import io.github.jimbozoomer.jugcraft.concordance.sign.Sign;
import io.github.jimbozoomer.jugcraft.concordance.sign.Signs;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.energy.EnergyConnectable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Ley Pylon: a ritual's channel (roadmap step 12). Use a Kindled Lantern on it to pour Radiance in as Ley Charge;
 * connect a Jugcraft power source to charge it with electricity; use it empty-handed to read its charge. The charge,
 * its owner and the conversions live in {@link LeyPylonBlockEntity}. {@link #CHARGED} lights its crystal.
 */
public class LeyPylonBlock extends BaseEntityBlock implements EnergyConnectable {
	public static final BooleanProperty CHARGED = BooleanProperty.create("charged");
	private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

	public LeyPylonBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(CHARGED, false));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new LeyPylonBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel)) {
			return null;
		}
		return createTickerHelper(type, JugcraftConcordance.PYLON_ENTITY, (tickLevel, pos, tickState, pylon) -> pylon.serverTick((ServerLevel) tickLevel));
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof LeyPylonBlockEntity pylon) {
			if (placer instanceof Player player) {
				pylon.setOwner(player.getUUID());
			}
			pylon.placed(server);
		}
	}

	/** A Kindled Lantern used on the pylon pours its Radiance in as Ley Charge. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (!stack.is(JugcraftConcordance.KINDLED_LANTERN)) {
			return super.useItemOn(stack, state, level, pos, player, hand, hit);
		}
		if (!(player instanceof ServerPlayer server) || !(level instanceof ServerLevel serverLevel)
				|| !(level.getBlockEntity(pos) instanceof LeyPylonBlockEntity pylon)) {
			return InteractionResult.SUCCESS;
		}
		if (!JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE)) {
			server.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.disabled"));
			return InteractionResult.FAIL;
		}
		if (!RateGate.allow(server, "pylon", 10)) {
			server.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.pylon.settling"));
			return InteractionResult.FAIL;
		}
		LeyPylonBlockEntity.Pour pour = pylon.pour(serverLevel, stack, ConcordanceData.rules().conversions().get(LeyPylonBlockEntity.CONVERSION));
		if (pour.refusal() == null) {
			server.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.pylon.poured", pour.radiance(), pour.ley(),
					pylon.ley(), LeyPylonBlockEntity.CAPACITY));
			level.playSound(null, pos, JugcraftConcordance.KINDLE_SOUND, SoundSource.BLOCKS, 0.6F, 0.7F);
			// Roadmap step 27: the Radiance poured, travelling from the lantern into the pylon.
			Signs.show(serverLevel, Vec3.atCenterOf(pos).add(0.0, 0.6, 0.0), Sign.FLOW, server.position().add(0.0, 1.0, 0.0));
		} else if (pour.refusal().equals("too_little")) {
			server.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.pylon.too_little",
					ConcordanceData.rules().conversions().get(LeyPylonBlockEntity.CONVERSION).fromAmount()));
			Signs.show(serverLevel, pos, Sign.WANT);
		} else {
			server.sendOverlayMessage(Component.translatable("message.jugcraft.concordance." + (pour.refusal().equals("disabled")
					? "disabled" : "pylon." + pour.refusal())));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof LeyPylonBlockEntity pylon) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.pylon.status", pylon.ley(),
					LeyPylonBlockEntity.CAPACITY));
		}
		return InteractionResult.SUCCESS;
	}

	/** A pylon placed or taken away changes the circles round it. */
	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean movedByPiston) {
		super.onPlace(state, level, pos, old, movedByPiston);
		if (level instanceof ServerLevel server && !old.is(state.getBlock())) {
			Rituals.changed(server, pos);
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		Rituals.changed(level, pos);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}

	/** A faint shimmer over a charged crystal (client only; fewer with reduced motion). */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(CHARGED) || !Presentation.ambient(random, 10, 30)) {
			return;
		}
		level.addParticle(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 0.0, 0.05, 0.0);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(CHARGED);
	}
}
