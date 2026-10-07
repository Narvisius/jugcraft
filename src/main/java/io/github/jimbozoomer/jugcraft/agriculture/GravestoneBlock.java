package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A gravestone, in one of three {@link Style}s, a decoration all year. Use a Name Tag that was named in an
 * anvil on it to engrave that name on its face (the tag is not used up), or rename the gravestone itself in an
 * anvil before placing it; see {@link GravestoneBlockEntity}. Engraving changes the world, so it needs build
 * rights, like editing a sign.
 */
public class GravestoneBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

	/**
	 * A gravestone's form, facing north: its boxes (pixels: x0, y0, z0, x1, y1, z1; models: tools/agriculture.py),
	 * and where the engraving goes: how far in front of the block's centre the engraved face is, the height of the
	 * text's middle, the width it may take, at most how many lines, and the size of a font pixel in blocks.
	 */
	public enum Style {
		ROUNDED("rounded_gravestone", new double[][] {{1, 0, 4, 15, 2, 12}, {2, 2, 6, 14, 12, 10}, {3, 12, 6, 13, 14, 10}, {5, 14, 6, 11, 15, 10}},
				2.0F, 7.5F, 10.0F, 6, 1.0F / 96),
		CROSS("cross_gravestone", new double[][] {{2, 0, 4, 14, 4, 12}, {6.5, 4, 6.5, 9.5, 16, 9.5}, {2.5, 10, 6.5, 13.5, 13, 9.5}},
				4.0F, 2.0F, 11.0F, 3, 1.0F / 112),
		OBELISK("obelisk_gravestone", new double[][] {{2, 0, 2, 14, 3, 14}, {4, 3, 4, 12, 13, 12}, {5, 13, 5, 11, 15, 11}, {6.5, 15, 6.5, 9.5, 16, 9.5}},
				4.0F, 8.0F, 7.0F, 6, 1.0F / 112);

		public final String id;
		final double[][] boxes;
		public final float front;
		public final float textY;
		public final float textWidth;
		public final int lines;
		public final float fontScale;
		private final VoxelShape[] shapes = new VoxelShape[4];

		Style(String id, double[][] boxes, float front, float textY, float textWidth, int lines, float fontScale) {
			this.id = id;
			this.boxes = boxes;
			this.front = front;
			this.textY = textY;
			this.textWidth = textWidth;
			this.lines = lines;
			this.fontScale = fontScale;
			for (Direction facing : Direction.Plane.HORIZONTAL) {
				VoxelShape shape = Shapes.empty();
				for (double[] b : boxes) {
					shape = Shapes.or(shape, turned(b, facing));
				}
				shapes[facing.get2DDataValue()] = shape;
			}
		}

		/** A box given facing north, turned to face {@code facing} about the block's centre. */
		private static VoxelShape turned(double[] b, Direction facing) {
			return switch (facing) {
				case SOUTH -> Block.box(16 - b[3], b[1], 16 - b[5], 16 - b[0], b[4], 16 - b[2]);
				case EAST -> Block.box(16 - b[5], b[1], b[0], 16 - b[2], b[4], b[3]);
				case WEST -> Block.box(b[2], b[1], 16 - b[3], b[5], b[4], 16 - b[0]);
				default -> Block.box(b[0], b[1], b[2], b[3], b[4], b[5]);
			};
		}
	}

	private final Style style;

	public GravestoneBlock(Properties properties, Style style) {
		super(properties);
		this.style = style;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	public Style style() {
		return style;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new GravestoneBlockEntity(pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return style.shapes[state.getValue(FACING).get2DDataValue()];
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	/** A named Name Tag engraves its name; anything else does what it would do on any block. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (!stack.is(Items.NAME_TAG)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!player.mayBuild()) {
			return InteractionResult.PASS;
		}
		if (!(player instanceof ServerPlayer engraver) || !(level.getBlockEntity(pos) instanceof GravestoneBlockEntity stone)) {
			return InteractionResult.SUCCESS;
		}
		Component name = stack.get(DataComponents.CUSTOM_NAME);
		if (name == null || name.getString().isBlank()) {
			engraver.sendOverlayMessage(Component.translatable("message.jugcraft.gravestone.unnamed_tag"));
			return InteractionResult.SUCCESS;
		}
		stone.engrave(name.getString());
		level.playSound(null, pos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F);
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
		builder.add(FACING);
	}

	/** At night a grave may stir: a restless spirit rises over it ({@link Spirits#stir}). */
	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		Spirits.stir(level, pos, random);
	}
}
