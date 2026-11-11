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
 * The Drone Tower Core block. Set it on the ground where the middle of the tower's hall should be: placed on top of
 * level ground it sets itself into the ground, so the hall floor it builds is level with the land round it. Right-click
 * with a block the tower needs to put it in (pipes and hoppers can too), or sneak and right-click with an empty hand to
 * put in everything the tower needs from your inventory; right-click with an empty hand to open the tower status screen
 * with its UPGRADE DRONE TOWER button.
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
			if (sinks(level, pos)) {
				// Into the ground: the hall floor goes in at the core's level, level with the land.
				var store = new java.util.LinkedHashMap<>(core.store());
				level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), UPDATE_ALL);
				pos = pos.below();
				level.setBlock(pos, state, UPDATE_ALL);
				if (!(level.getBlockEntity(pos) instanceof TowerCoreBlockEntity sunk)) {
					return;
				}
				sunk.adoptStore(store);
				core = sunk;
			}
			core.setOwner(player.getUUID());
			TowerRegistry.get((ServerLevel) level).claim(player.getUUID(), pos);
		}
	}

	/**
	 * Does a core just placed at {@code pos} stand on top of the ground (solid, plain ground under it and nothing round
	 * it at its own level), rather than set into it? Then it moves down into the ground block.
	 */
	public static boolean sinks(Level level, BlockPos pos) {
		BlockPos down = pos.below();
		BlockState ground = level.getBlockState(down);
		if (ground.isAir() || !ground.isFaceSturdy(level, down, net.minecraft.core.Direction.UP) || level.getBlockEntity(down) != null
				|| ground.getDestroySpeed(level, down) < 0 || ground.getBlock() instanceof TowerCoreBlock) {
			return false;
		}
		for (net.minecraft.core.Direction side : net.minecraft.core.Direction.Plane.HORIZONTAL) {
			if (!level.getBlockState(pos.relative(side)).canBeReplaced()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Whether the whole finished tower fits in the world with its core placed at {@code pos} (set a block into plain
	 * ground, as {@link #sinks} has it): its mast's top under the build limit and its lowest block above the world's
	 * floor. Justin, 11 November 2026: "Make it so the core will not let you place it if it is too high" (a core much above
	 * y 60 left the mast's top unbuilt).
	 */
	public static boolean fits(Level level, BlockPos pos) {
		int y = core(level, pos);
		TowerData.Extent e = TowerData.get(TowerData.CURRENT_LAYOUT).extent();
		return y + e.maxY() <= level.getMaxY() && y + e.minY() >= level.getMinY();
	}

	/** Where a core placed at {@code pos} ends up standing (a block down, set into plain ground). */
	private static int core(Level level, BlockPos pos) {
		return sinks(level, pos) ? pos.getY() - 1 : pos.getY();
	}

	/** The highest a core can stand in {@code level} for its tower to fit (y). */
	public static int highest(Level level) {
		return level.getMaxY() - TowerData.get(TowerData.CURRENT_LAYOUT).extent().maxY();
	}

	/** The lowest a core can stand in {@code level} for its tower to fit (y). */
	public static int lowest(Level level) {
		return level.getMinY() - TowerData.get(TowerData.CURRENT_LAYOUT).extent().minY();
	}

	/**
	 * {@link #fits}, and if not, the player (if any) told why and how high (or low) the core may go: placing it is refused
	 * (the item stays in hand). Called from the block item for every placement, dispensers' too.
	 */
	public static boolean mayPlaceHere(Level level, @Nullable Player player, BlockPos pos) {
		if (fits(level, pos)) {
			return true;
		}
		if (player != null && !level.isClientSide()) {
			int y = core(level, pos), over = y + TowerData.get(TowerData.CURRENT_LAYOUT).extent().maxY() - level.getMaxY();
			player.sendOverlayMessage(over > 0
					? net.minecraft.network.chat.Component.translatable("message.jugcraft.tower.too_high", over, highest(level))
					: net.minecraft.network.chat.Component.translatable("message.jugcraft.tower.too_low", lowest(level)));
		}
		return false;
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
		if (level.getBlockEntity(pos) instanceof TowerCoreBlockEntity core) {
			if (core.wants(stack)) {
				if (!level.isClientSide()) {
					deposit(core, player, () -> core.deposit(stack));
				}
				return InteractionResult.SUCCESS;
			}
			if (!core.firstLayer() && !stack.isEmpty() && core.stillNeeded(stack.getItem()) > 0) {
				// after the first layer the tower's blocks go in through the Cargo Exchange only
				if (!level.isClientSide()) {
					player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.jugcraft.tower.use_cargo_exchange"));
				}
				return InteractionResult.SUCCESS;
			}
		}
		return super.useItemOn(stack, state, level, pos, player, hand, hit);
	}

	/** Whether right-clicking the core opens its first-layer loading screen (else the tower screen). */
	public static boolean opensLoading(TowerCoreBlockEntity core) {
		return core.firstLayer() && !core.firstLayerQueued();
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player.isSecondaryUseActive()) {
			if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TowerCoreBlockEntity core) {
				deposit(core, player, () -> core.depositFrom(player));
			}
			return InteractionResult.SUCCESS;
		}
		if (level.getBlockEntity(pos) instanceof TowerCoreBlockEntity core && opensLoading(core)) {
			// the first layer: the loading screen (a server-side screen, with the player's inventory)
			if (player instanceof net.minecraft.server.level.ServerPlayer server) {
				if (core.mayUse(server)) {
					TowerLoadMenu.openFirstLayer(server, core);
				} else {
					server.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.jugcraft.tower.not_owner"));
				}
			}
			return InteractionResult.SUCCESS;
		}
		if (level.isClientSide()) {
			openScreen.accept(pos);
		}
		return InteractionResult.SUCCESS;
	}

	/** Puts blocks in for a player who may run the tower, and tells them how many went in. */
	public static void deposit(TowerCoreBlockEntity core, Player player, java.util.function.IntSupplier put) {
		if (!core.mayUse(player)) {
			player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.jugcraft.tower.not_owner"));
			return;
		}
		if (!core.firstLayer() && !player.hasInfiniteMaterials()) {
			player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.jugcraft.tower.use_cargo_exchange"));
			return;
		}
		int taken = put.getAsInt();
		player.sendOverlayMessage(taken > 0 ? net.minecraft.network.chat.Component.translatable("message.jugcraft.tower.deposited_blocks", taken)
				: net.minecraft.network.chat.Component.translatable("message.jugcraft.tower.nothing_needed"));
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

}
