package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Floating Table Setting (Halloween decorations batch 19, the Poltergeist's Dinner Party): a place laid on top of any
 * block (a table) that hovers a hand's breadth above it, each piece bobbing out of step: a dinner of plate, goblet,
 * cutlery and a candlestick; tea for one, whose pot now and then pours itself a cup; or a feast platter. At night the
 * goblet now and then tips over and rights itself. Use it to change the {@link #SETTING}; flint and steel or a fire
 * charge lights the candle ({@link #LIT}, light {@value #LIGHT}), and an empty hand while sneaking puts it out. Nothing
 * collides with it. Everything floating is drawn by the client (client/TableSettingRenderer.java).
 */
public class FloatingTableSettingBlock extends HorizontalDirectionalBlock implements EntityBlock {
	public static final int LIGHT = 7;
	public static final EnumProperty<Setting> SETTING = EnumProperty.create("setting", Setting.class);
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 8.0, 15.0);

	/** What the table is laid for. */
	public enum Setting implements StringRepresentable {
		DINNER, TEA, FEAST;

		public Setting next() {
			return values()[(ordinal() + 1) % values().length];
		}

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public FloatingTableSettingBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SETTING, Setting.DINNER).setValue(LIT, false));
	}

	public static int light(BlockState state) {
		return state.getValue(LIT) ? LIGHT : 0;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (!stack.is(Items.FLINT_AND_STEEL) && !stack.is(Items.FIRE_CHARGE)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (state.getValue(LIT)) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
			level.playSound(null, pos, stack.is(Items.FIRE_CHARGE) ? SoundEvents.FIRECHARGE_USE : SoundEvents.FLINTANDSTEEL_USE,
					SoundSource.BLOCKS, 1.0F, 1.0F);
			if (stack.is(Items.FIRE_CHARGE)) {
				stack.consume(1, player);
			} else {
				stack.hurtAndBreak(1, player, hand);
			}
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (player.isSecondaryUseActive()) {
			if (!state.getValue(LIT)) {
				return InteractionResult.PASS;
			}
			level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
		} else {
			Setting next = state.getValue(SETTING).next();
			level.setBlock(pos, state.setValue(SETTING, next), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.LANTERN_PLACE, SoundSource.BLOCKS, 0.5F, 1.6F);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.table_setting",
					Component.translatable("setting.jugcraft." + next.getSerializedName())));
		}
		level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		return InteractionResult.SUCCESS;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DecorationBlockEntity(JugcraftAgriculture.TABLE_SETTING_ENTITY, pos, state);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, SETTING, LIT);
	}
}
