package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
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
 * The Judging Stand of the Halloween carving contest ({@link CarvingContest}): a draped pedestal for one
 * hand-carved pumpkin. Put the pumpkin on top and use the stand with an empty hand: its carver enters it (or
 * anyone, if the pumpkin was moved and forgot its carver); everyone else votes for it while the event runs.
 * Sneak-use shows the standings. Taking the pumpkin off withdraws the entry; votes already cast stay.
 */
public class JudgingStandBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	/** A foot, a post, the cloth's hem and a full top for the pumpkin to stand on (model: tools/festivity_data.py). */
	private static final VoxelShape SHAPE = Shapes.or(Block.box(1.0, 0.0, 1.0, 15.0, 2.0, 15.0), Block.box(4.0, 2.0, 4.0, 12.0, 10.0, 12.0),
			Block.box(1.0, 10.0, 1.0, 15.0, 13.0, 15.0), Block.box(0.0, 13.0, 0.0, 16.0, 16.0, 16.0));

	public JudgingStandBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new JudgingStandBlockEntity(pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	/** Blocks in hand are placed as usual (a pumpkin goes on top); anything else counts as an empty hand. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		return stack.getItem() instanceof BlockItem ? InteractionResult.PASS : InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(player instanceof ServerPlayer judge) || !(level.getBlockEntity(pos) instanceof JudgingStandBlockEntity entry)) {
			return InteractionResult.SUCCESS;
		}
		if (judge.isSecondaryUseActive()) {
			showStandings(judge);
			return InteractionResult.SUCCESS;
		}
		if (entry.entrant().isEmpty()) {
			CarvedPumpkinBlockEntity pumpkin = CarvingContest.pumpkinOn(judge.level(), pos);
			CarvingContest.Result result = CarvingContest.enter(judge, pos);
			judge.sendOverlayMessage(switch (result) {
				case ENTERED -> Component.translatable("message.jugcraft.judging_stand.entered");
				case NOT_YOURS -> Component.translatable("message.jugcraft.judging_stand.not_yours", pumpkin == null ? "?" : pumpkin.carverName());
				default -> Component.translatable("message.jugcraft.judging_stand.no_pumpkin");
			});
			if (result == CarvingContest.Result.ENTERED) {
				level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1.0F, 1.2F);
			}
			return InteractionResult.SUCCESS;
		}
		CarvingContest.Result result = CarvingContest.vote(judge, pos);
		String name = entry.entrantName();
		judge.sendOverlayMessage(switch (result) {
			case VOTED -> Component.translatable("message.jugcraft.judging_stand.voted", name);
			case MOVED -> Component.translatable("message.jugcraft.judging_stand.moved", name);
			case SAME -> Component.translatable("message.jugcraft.judging_stand.same", name);
			case OWN_ENTRY -> Component.translatable("message.jugcraft.judging_stand.own_entry");
			case CLOSED -> Component.translatable("message.jugcraft.judging_stand.closed", name);
			case FULL -> Component.translatable("message.jugcraft.judging_stand.full");
			case NO_ENTRY -> Component.translatable("message.jugcraft.judging_stand.no_entry");
			default -> Component.translatable("message.jugcraft.judging_stand.no_pumpkin");
		});
		if (result == CarvingContest.Result.VOTED || result == CarvingContest.Result.MOVED) {
			level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.8F, 1.0F);
		}
		return InteractionResult.SUCCESS;
	}

	/** This Halloween's top places, and the player's own votes if they are further down. */
	static void showStandings(ServerPlayer player) {
		int year = HalloweenSeason.year();
		List<CarvingContest.Standing> standings = CarvingContest.standings(player.level().getServer(), year);
		player.sendSystemMessage(Component.translatable("message.jugcraft.judging_stand.standings", year, standings.size()));
		for (int i = 0; i < standings.size(); i++) {
			CarvingContest.Standing standing = standings.get(i);
			if (i < CarvingContest.PLACES || standing.entrant().equals(player.getUUID())) {
				player.sendSystemMessage(Component.translatable("message.jugcraft.judging_stand.place", i + 1, standing.name(), standing.votes()));
			}
		}
	}

	/** The entry goes with the pumpkin on top. */
	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		if (direction == Direction.UP && !(neighborState.getBlock() instanceof CarvedPumpkinBlock)
				&& level.getBlockEntity(pos) instanceof JudgingStandBlockEntity entry) {
			entry.clear();
		}
		return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
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
