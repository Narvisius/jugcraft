package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Spooky Sign: a weathered board on a stake, its warning slapped on in dripping red paint. Use it to paint the
 * next warning ({@link #WORDS}: BEWARE, KEEP OUT, TURN BACK, GO AWAY, NO TRESPASSING, ABANDON HOPE). Or write your
 * own, as on a gravestone: use a Name Tag named in an anvil on it (the tag is not used up), or rename the sign itself in
 * an anvil before placing it ({@link SpookySignBlockEntity}). Your own words cover the painted ones; sneak-use with an
 * empty hand to wipe them off. Painting changes the world, so it needs build permission. The words are drawn by the
 * client (client/SpookySignRenderer.java).
 */
public class SpookySignBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final EnumProperty<Words> WORDS = EnumProperty.create("words", Words.class);
	private static final VoxelShape[] SHAPES = {Block.box(0.5, 0.0, 7.0, 15.5, 15.5, 9.5), Block.box(6.5, 0.0, 0.5, 9.0, 15.5, 15.5),
			Block.box(0.5, 0.0, 6.5, 15.5, 15.5, 9.0), Block.box(7.0, 0.0, 0.5, 9.5, 15.5, 15.5)};

	public enum Words implements StringRepresentable {
		BEWARE, KEEP_OUT, TURN_BACK, GO_AWAY, NO_TRESPASSING, ABANDON_HOPE;

		public Words next() {
			return values()[(ordinal() + 1) % values().length];
		}

		/** The translation key of the painted words. */
		public String key() {
			return "message.jugcraft.spooky_sign." + getSerializedName();
		}

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public SpookySignBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WORDS, Words.BEWARE));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SpookySignBlockEntity(pos, state);
	}

	/** The board and its stake, turned with the sign. */
	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(FACING)) {
			case SOUTH -> SHAPES[2];
			case EAST -> SHAPES[1];
			case WEST -> SHAPES[3];
			default -> SHAPES[0];
		};
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	/** A named Name Tag paints its name; anything else does what it would do on any block. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (!stack.is(Items.NAME_TAG)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!player.mayBuild()) {
			return InteractionResult.PASS;
		}
		if (!(player instanceof ServerPlayer painter) || !(level.getBlockEntity(pos) instanceof SpookySignBlockEntity sign)) {
			return InteractionResult.SUCCESS;
		}
		Component name = stack.get(DataComponents.CUSTOM_NAME);
		if (name == null || name.getString().isBlank()) {
			painter.sendOverlayMessage(Component.translatable("message.jugcraft.spooky_sign.unnamed_tag"));
			return InteractionResult.SUCCESS;
		}
		sign.paint(name.getString());
		level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
		level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		return InteractionResult.SUCCESS;
	}

	/** Paints the next warning; sneaking, wipes your own words off. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.mayBuild()) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof SpookySignBlockEntity sign)) {
			return InteractionResult.SUCCESS;
		}
		if (player.isSecondaryUseActive()) {
			if (!sign.text().isEmpty()) {
				sign.paint("");
				level.playSound(null, pos, SoundEvents.AXE_STRIP.value(), SoundSource.BLOCKS, 0.6F, 1.4F);
				level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
			}
			return InteractionResult.SUCCESS;
		}
		if (!sign.text().isEmpty()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.spooky_sign.painted"));
			return InteractionResult.SUCCESS;
		}
		level.setBlock(pos, state.setValue(WORDS, state.getValue(WORDS).next()), Block.UPDATE_ALL);
		level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 0.9F);
		level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		return InteractionResult.SUCCESS;
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
		builder.add(FACING, WORDS);
	}
}
