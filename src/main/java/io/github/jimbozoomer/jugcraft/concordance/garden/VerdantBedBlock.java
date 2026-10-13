package io.github.jimbozoomer.jugcraft.concordance.garden;

import io.github.jimbozoomer.jugcraft.concordance.Illumination;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.farming.SprinklerBlock;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * A Verdant Bed (roadmap step 14): soil for the Greenwardens' crops. Its {@link #MOISTURE} works as farmland's (water
 * within {@value Garden#WATER_REACH} blocks and one above, a wet sprinkler as close, or rain keep it at 7; otherwise it
 * dries a step on each random tick), but it never turns back to dirt and cannot be trampled. Every step of growth
 * dries it by one. Its nutrients and whether it is awake are its block entity's ({@link VerdantBedBlockEntity}).
 * <p>
 * Bone meal on it gives {@value Garden#BONE_MEAL_NUTRIENTS} nutrients. An empty hand reports it; a Greenwarden's
 * empty hand wakes it and the dormant beds joined to it (up to {@value Garden#AWAKEN_LIMIT}).
 */
public class VerdantBedBlock extends BaseEntityBlock {
	public static final IntegerProperty MOISTURE = BlockStateProperties.MOISTURE;
	public static final int WET = 7;

	public VerdantBedBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(MOISTURE, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(MOISTURE);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new VerdantBedBlockEntity(pos, state);
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return true;
	}

	/** Wets the bed from water, rain or a wet sprinkler nearby, or lets it dry a step. */
	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		int moisture = state.getValue(MOISTURE);
		if (watered(level, pos)) {
			if (moisture < WET) {
				level.setBlock(pos, state.setValue(MOISTURE, WET), Block.UPDATE_CLIENTS);
			}
		} else if (moisture > 0) {
			level.setBlock(pos, state.setValue(MOISTURE, moisture - 1), Block.UPDATE_CLIENTS);
		}
	}

	/**
	 * Water within {@value Garden#WATER_REACH} blocks (at the bed's height or one above), a wet sprinkler within that
	 * reach and up to two above, or rain on the bed. Reads at most 243 positions, only on a random tick, never in a
	 * chunk that is not loaded.
	 */
	public static boolean watered(LevelReader level, BlockPos pos) {
		if (level instanceof Level world && world.isRainingAt(pos.above())) {
			return true;
		}
		int reach = Garden.WATER_REACH;
		for (BlockPos at : BlockPos.betweenClosed(pos.offset(-reach, 0, -reach), pos.offset(reach, 2, reach))) {
			if (!level.hasChunkAt(at)) {
				continue;
			}
			if (at.getY() <= pos.getY() + 1 && level.getFluidState(at).is(FluidTags.WATER)) {
				return true;
			}
			BlockState there = level.getBlockState(at);
			if (there.getBlock() instanceof SprinklerBlock && there.getValue(SprinklerBlock.WET)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level instanceof ServerLevel server && placer instanceof ServerPlayer player
				&& level.getBlockEntity(pos) instanceof VerdantBedBlockEntity bed) {
			bed.placedBy(player, Garden.knows(player));
			if (watered(server, pos)) {
				level.setBlock(pos, state.setValue(MOISTURE, WET), Block.UPDATE_CLIENTS);
			}
		}
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (!stack.is(Items.BONE_MEAL)) {
			return super.useItemOn(stack, state, level, pos, player, hand, hit);
		}
		if (player instanceof ServerPlayer server && level instanceof ServerLevel serverLevel
				&& level.getBlockEntity(pos) instanceof VerdantBedBlockEntity bed) {
			if (!RateGate.allow(server, "garden", 4) || !Illumination.mayChange(serverLevel, player, pos)) {
				return InteractionResult.FAIL;
			}
			if (bed.give(Garden.BONE_MEAL_NUTRIENTS) == 0) {
				Garden.tell(server, "garden.bed_full");
				return InteractionResult.FAIL;
			}
			stack.consume(1, player);
			level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, pos.above(), 5);
			Garden.tell(server, "garden.fed", bed.nutrients(), VerdantBedBlockEntity.CAPACITY);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer server && level instanceof ServerLevel serverLevel
				&& level.getBlockEntity(pos) instanceof VerdantBedBlockEntity bed) {
			if (!RateGate.allow(server, "garden", 4)) {
				return InteractionResult.FAIL;
			}
			if (!bed.awake() && Garden.knows(server) && Garden.enabled()) {
				Garden.tell(server, "garden.awakened", awaken(serverLevel, pos, server));
			} else {
				bed.report(serverLevel, server);
			}
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * Wakes the dormant bed at {@code start} and the dormant beds joined to it (side by side or one above or below), at
	 * most {@value Garden#AWAKEN_LIMIT}, in loaded chunks the player may change. Returns how many woke.
	 */
	public static int awaken(ServerLevel level, BlockPos start, ServerPlayer player) {
		Queue<BlockPos> open = new ArrayDeque<>();
		Set<BlockPos> seen = new HashSet<>();
		open.add(start);
		seen.add(start);
		int woken = 0;
		while (!open.isEmpty() && woken < Garden.AWAKEN_LIMIT) {
			BlockPos pos = open.remove();
			if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof VerdantBedBlockEntity bed) || bed.awake()
					|| !Illumination.mayChange(level, player, pos)) {
				continue;
			}
			bed.awaken(player.getUUID());
			woken++;
			for (Direction side : Direction.values()) {
				BlockPos next = pos.relative(side);
				if (seen.add(next.immutable())) {
					open.add(next.immutable());
				}
			}
		}
		return woken;
	}
}
