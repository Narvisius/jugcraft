package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Fortune Teller's Table: a round table under a purple fringed cloth with a spirit board and a spread of tarot
 * cards on it. Use it and the server picks one of {@value #FORTUNES} silly fortunes for you, turns one of {@value #CARDS}
 * tarot cards and sends the planchette sliding to YES, NO or GOODBYE (a block event every client near sees, drawn by
 * client/FortuneTellerTableRenderer.java); the fortune is told to you alone. A table reads at most once every
 * {@value #COOLDOWN_TICKS} ticks.
 */
public class FortuneTellerTableBlock extends BaseEntityBlock {
	public static final int FORTUNES = 20;
	public static final int CARDS = 6;
	public static final int ANSWERS = 3;
	public static final int COOLDOWN_TICKS = 40;
	/** How long the card stays turned and the planchette moves, in ticks. */
	public static final int READING_TICKS = 100;
	public static final int READ = 1;
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 13.0, 15.0);

	public FortuneTellerTableBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	/** The card and the planchette's answer packed into a block event's parameter. */
	public static int reading(int card, int answer) {
		return card * ANSWERS + answer;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FortuneTellerTableBlockEntity(pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	/** Reads a fortune: a card turns, the planchette slides, and the fortune is told to the player alone. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof FortuneTellerTableBlockEntity table)) {
			return InteractionResult.SUCCESS;
		}
		long time = level.getGameTime();
		if (time - table.marked() < COOLDOWN_TICKS) {
			return InteractionResult.SUCCESS;
		}
		int fortune = level.getRandom().nextInt(FORTUNES);
		int card = level.getRandom().nextInt(CARDS);
		int answer = level.getRandom().nextInt(ANSWERS);
		table.read(time, reading(card, answer));
		level.blockEvent(pos, this, READ, reading(card, answer));
		level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 0.8F);
		level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6F, 0.6F);
		player.sendOverlayMessage(Component.translatable("message.jugcraft.fortune." + (fortune + 1)));
		return InteractionResult.SUCCESS;
	}

	/** A reading reaches clients as a block event, which the table's block entity records. */
	@Override
	protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
		super.triggerEvent(state, level, pos, id, param);
		BlockEntity entity = level.getBlockEntity(pos);
		return entity != null && entity.triggerEvent(id, param);
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
}
