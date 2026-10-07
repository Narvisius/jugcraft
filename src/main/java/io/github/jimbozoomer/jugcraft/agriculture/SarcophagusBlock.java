package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * A Stone Sarcophagus (Halloween decorations batch 18), in stone brick, deepslate or blackstone: a tomb-chest two blocks
 * long with carved panels and a skull boss, under a heavy lid. Use it and the lid grinds aside and tilts ({@link #OPEN},
 * drawn by the client), showing a skeleton lying within and {@value SarcophagusBlockEntity#SLOTS} slots kept in its head
 * half. The Stonemason's Chisel recarves the lid ({@link #LID}: a plain lid, a knight, a lady or a skull and crossbones),
 * one use of the chisel each. At night, shut, it knocks ({@link SarcophagusBlockEntity}).
 */
public class SarcophagusBlock extends LongDecorationBlock implements EntityBlock {
	public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
	public static final EnumProperty<Effigy> LID = EnumProperty.create("lid", Effigy.class);
	private static final double[][] HEAD = {{0.5, 0.0, 0.5, 15.5, 13.0, 16.0}};
	private static final double[][] FOOT = {{0.5, 0.0, 0.0, 15.5, 13.0, 15.5}};

	/** The lid's carving, in the order the chisel turns them. */
	public enum Effigy implements StringRepresentable {
		PLAIN, KNIGHT, LADY, SKULL;

		public Effigy next() {
			return values()[(ordinal() + 1) % values().length];
		}

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public SarcophagusBlock(Properties properties) {
		super(properties, HEAD, FOOT);
		registerDefaultState(defaultBlockState().setValue(OPEN, false).setValue(LID, Effigy.PLAIN));
	}

	/** Sets {@code changed} on this half and the other one. */
	static void setBoth(Level level, BlockPos pos, BlockState changed) {
		level.setBlock(pos, changed, Block.UPDATE_ALL);
		BlockPos other = other(changed, pos);
		BlockState otherState = level.getBlockState(other);
		if (otherState.is(changed.getBlock())) {
			level.setBlock(other, otherState.setValue(OPEN, changed.getValue(OPEN)).setValue(LID, changed.getValue(LID)), Block.UPDATE_ALL);
		}
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (!stack.is(JugcraftAgriculture.item(Epitaphs.CHISEL))) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!player.mayBuild() || !level.mayInteract(player, pos)) {
			return InteractionResult.FAIL;
		}
		Effigy next = state.getValue(LID).next();
		setBoth(level, pos, state.setValue(LID, next));
		stack.hurtAndBreak(1, player, hand);
		level.playSound(null, pos, SoundEvents.DEEPSLATE_BRICKS_HIT, SoundSource.BLOCKS, 1.0F, 0.8F);
		level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.3F, 1.8F);
		level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		player.sendOverlayMessage(Component.translatable("message.jugcraft.sarcophagus.effigy",
				Component.translatable("effigy.jugcraft." + next.getSerializedName())));
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(head(state, pos)) instanceof SarcophagusBlockEntity tomb) {
			player.openMenu(tomb);
		}
		return InteractionResult.SUCCESS;
	}

	/** Re-checks who still has the lid off (scheduled by the openers' counter). */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof SarcophagusBlockEntity tomb) {
			tomb.recheckOpen();
		}
	}

	/** The lid shudders on clients when it knocks. */
	@Override
	protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
		if (level.getBlockEntity(pos) instanceof SarcophagusBlockEntity tomb) {
			tomb.mark(level.getGameTime());
			return true;
		}
		return false;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(PART) == BedPart.HEAD ? new SarcophagusBlockEntity(pos, state) : null;
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || state.getValue(PART) != BedPart.HEAD || type != JugcraftAgriculture.SARCOPHAGUS_TOMB_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> ((SarcophagusBlockEntity) entity).serverTick((ServerLevel) tickLevel, pos, tickState);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, net.minecraft.core.Direction direction) {
		return level.getBlockEntity(head(state, pos)) instanceof SarcophagusBlockEntity tomb
				? net.minecraft.world.inventory.AbstractContainerMenu.getRedstoneSignalFromContainer(tomb) : 0;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(OPEN, LID);
	}
}
