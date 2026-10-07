package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Show Launcher: a painted crate of nine mortar tubes for a fireworks show ({@link ShowLauncherBlockEntity}).
 * <ul>
 * <li>Use it holding rockets (spooky fireworks or vanilla firework rockets) to load them, up to sixteen to a tube.</li>
 * <li>Use it with an empty hand to start the show, and again to stop it; a rising redstone signal does the same.</li>
 * <li>Sneak and use it with an empty hand to change how it fires: in sequence (one at a time), in volleys (a row of three
 * at once) or as a finale (one from every tube at once). The dial on its front shows which.</li>
 * </ul>
 * Comparators read how many tubes are loaded. Broken, it spills its rockets.
 */
public class ShowLauncherBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	private static final String MESSAGES = "message.jugcraft.show_launcher.";
	private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 14.0, 16.0);

	/** How a show fires. */
	public enum Mode implements StringRepresentable {
		SEQUENCE("sequence"), VOLLEY("volley"), FINALE("finale");

		private final String name;

		Mode(String name) {
			this.name = name;
		}

		public Mode next() {
			return values()[(ordinal() + 1) % values().length];
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public ShowLauncherBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(MODE, Mode.SEQUENCE).setValue(POWERED, false));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ShowLauncherBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != JugcraftAgriculture.SHOW_LAUNCHER_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> ((ShowLauncherBlockEntity) entity).serverTick((ServerLevel) tickLevel);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
				.setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (!SpookyFireworkItem.rocket(stack)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ShowLauncherBlockEntity launcher) {
			int loaded = launcher.load(stack);
			if (loaded > 0) {
				stack.consume(loaded, player);
				level.playSound(null, pos, SoundEvents.CHISELED_BOOKSHELF_INSERT, SoundSource.BLOCKS, 0.8F, 0.8F);
				level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
			} else {
				player.sendOverlayMessage(Component.translatable(MESSAGES + "full"));
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.getMainHandItem().isEmpty()) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof ShowLauncherBlockEntity launcher)) {
			return InteractionResult.SUCCESS;
		}
		if (player.isSecondaryUseActive()) {
			Mode mode = state.getValue(MODE).next();
			level.setBlock(pos, state.setValue(MODE, mode), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.COMPARATOR_CLICK, SoundSource.BLOCKS, 0.6F, 1.0F);
			player.sendOverlayMessage(Component.translatable(MESSAGES + "mode." + mode.getSerializedName()));
		} else {
			player.sendOverlayMessage(Component.translatable(MESSAGES + toggle(launcher)));
		}
		level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		return InteractionResult.SUCCESS;
	}

	/** Starts or stops the show; returns what happened ({@code started}, {@code stopped} or {@code empty}). */
	public static String toggle(ShowLauncherBlockEntity launcher) {
		if (launcher.running()) {
			launcher.stop();
			return "stopped";
		}
		return launcher.start() ? "started" : "empty";
	}

	/** A rising redstone signal starts the show, or stops one that is running. */
	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation, boolean moved) {
		super.neighborChanged(state, level, pos, neighbor, orientation, moved);
		if (level.isClientSide()) {
			return;
		}
		boolean powered = level.hasNeighborSignal(pos);
		if (powered == state.getValue(POWERED)) {
			return;
		}
		level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
		if (powered && level.getBlockEntity(pos) instanceof ShowLauncherBlockEntity launcher) {
			toggle(launcher);
		}
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	/** How many tubes are loaded: 0 when none, 15 with all nine. */
	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		int tubes = level.getBlockEntity(pos) instanceof ShowLauncherBlockEntity launcher ? launcher.loadedTubes() : 0;
		return tubes == 0 ? 0 : 1 + (tubes - 1) * 14 / (ShowLauncherBlockEntity.TUBES - 1);
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
		builder.add(FACING, MODE, POWERED);
	}
}
