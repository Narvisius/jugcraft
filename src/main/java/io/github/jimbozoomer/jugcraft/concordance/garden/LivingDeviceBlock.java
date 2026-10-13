package io.github.jimbozoomer.jugcraft.concordance.garden;

import io.github.jimbozoomer.jugcraft.concordance.Illumination;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A living device's block (roadmap step 14): placed, it remembers who placed it and wakes at once for a Greenwarden;
 * a Greenwarden's empty hand wakes a dormant one; otherwise an empty hand does what the device does
 * ({@link #useEmpty}). GeckoLib draws the animated ones on the client (their block renders nothing).
 */
public abstract class LivingDeviceBlock extends BaseEntityBlock {
	private final VoxelShape shape;
	private final boolean animated;

	protected LivingDeviceBlock(Properties properties, VoxelShape shape, boolean animated) {
		super(properties);
		this.shape = shape;
		this.animated = animated;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level instanceof ServerLevel && placer instanceof ServerPlayer player
				&& level.getBlockEntity(pos) instanceof LivingDeviceBlockEntity device) {
			device.placedBy(player);
		}
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer server && level instanceof ServerLevel serverLevel
				&& level.getBlockEntity(pos) instanceof LivingDeviceBlockEntity device) {
			if (!RateGate.allow(server, "garden", 4)) {
				return InteractionResult.FAIL;
			}
			if (!device.awake()) {
				if (Garden.knows(server) && Garden.enabled() && Illumination.mayChange(serverLevel, server, pos)) {
					device.awaken(server.getUUID());
					Garden.tell(server, "garden.awakened", 1);
				} else {
					Garden.tell(server, "garden.dormant");
				}
				return InteractionResult.SUCCESS;
			}
			useEmpty(device, server, serverLevel);
		}
		return InteractionResult.SUCCESS;
	}

	/** An awake device used with an empty hand: report, or change a setting. */
	protected abstract void useEmpty(LivingDeviceBlockEntity device, ServerPlayer player, ServerLevel level);

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return animated ? RenderShape.INVISIBLE : RenderShape.MODEL;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape;
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}
}
