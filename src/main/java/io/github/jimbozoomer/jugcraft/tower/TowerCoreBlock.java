package io.github.jimbozoomer.jugcraft.tower;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Drone Tower Core block. Set it in the middle of a 15x15 plinth of chiseled stone bricks. Right-click
 * with tower modules to put them in (pipes and hoppers can too); right-click with an empty hand to open the
 * tower status screen with the UPGRADE DRONE TOWER button.
 */
public class TowerCoreBlock extends BaseEntityBlock {
	public TowerCoreBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TowerCoreBlockEntity(pos, state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel)) {
			return null;
		}
		return createTickerHelper(type, JugcraftTower.CORE_ENTITY, (tickLevel, pos, tickState, core) -> core.serverTick((ServerLevel) tickLevel));
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (!level.isClientSide() && placer instanceof Player player && level.getBlockEntity(pos) instanceof TowerCoreBlockEntity core) {
			core.setOwner(player.getUUID());
			TowerRegistry.get((ServerLevel) level).claim(player.getUUID(), pos);
		}
	}

	/**
	 * A player may own several Drone Towers in a dimension, but a new core must stand outside the build radius of
	 * their other towers ({@link TowerCoreBlockEntity#BUILD_RADIUS_CHUNKS} chunks each way): otherwise placing it is
	 * refused (the item stays in hand) and the player is told which tower is too close. Called from the block item.
	 */
	public static boolean mayPlace(Level level, Player player, BlockPos pos) {
		if (level.isClientSide()) {
			return true;
		}
		BlockPos existing = TowerRegistry.get((ServerLevel) level).coreCovering((ServerLevel) level, player.getUUID(), pos);
		if (existing != null) {
			player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.jugcraft.tower.too_close",
					existing.getX(), existing.getY(), existing.getZ(), TowerCoreBlockEntity.BUILD_RADIUS_CHUNKS));
			return false;
		}
		return true;
	}



	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (level.getBlockEntity(pos) instanceof TowerCoreBlockEntity core && JugcraftTower.MODULE_ITEMS.containsValue(stack.getItem())) {
			if (!level.isClientSide()) {
				if (!core.mayUse(player)) {
					player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.jugcraft.tower.not_owner"));
				} else if (core.deposit(stack)) {
					player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.jugcraft.tower.deposited"));
				}
			}
			return InteractionResult.SUCCESS;
		}
		return super.useItemOn(stack, state, level, pos, player, hand, hit);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			openScreen.accept(pos);
		}
		return InteractionResult.SUCCESS;
	}

	/** Opens the tower status screen on the client; set by the client entry point (no-op on a server). */
	public static java.util.function.Consumer<BlockPos> openScreen = pos -> {
	};

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		TowerRegistry.get(level).release(pos);
		TowerCoreBlockEntity.keepLoaded(level, pos, false);
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TowerCoreBlockEntity core) {
			core.dropContents(level, pos);
		}
		return super.playerWillDestroy(level, pos, state, player);
	}
}
