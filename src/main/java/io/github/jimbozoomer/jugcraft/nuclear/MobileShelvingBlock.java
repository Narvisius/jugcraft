package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A high-density mobile shelving carriage (document control; the administration building part 2): a ShelvingBlock on
 * wheels on the floor rails, side by side with the other carriages of its bank between two fixed units, with one aisle
 * open somewhere along the bank. Turn a carriage's handwheel (right-click its end) and it rolls toward the aisle, with
 * the carriages between, and the aisle opens beside it; the carriages never close on anyone in the aisle. SLIDE is how
 * far a carriage has rolled into its next block (quarter blocks, {@link #REST} at rest); VARIANT its label (sneak and
 * right-click its end to change it).
 */
public class MobileShelvingBlock extends ShelvingBlock {
	public static final IntegerProperty SLIDE = IntegerProperty.create("slide", 0, 6);
	public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 7);
	public static final int REST = 3, LABELS = 8, STEP_TICKS = 3, REACH = 12;
	/** The carriages' labels (tools/admin_rooms.py MS_LABELS). */
	public static final String[] NAMES = {"EOP - AOP - ARP", "OPERATING PROCEDURES", "SURVEILLANCE PROCEDURES", "MAINTENANCE PROCEDURES",
			"TECH SPECS - FSAR", "DESIGN DRAWINGS", "VENDOR MANUALS", "QA RECORDS"};
	private static final Map<Level, List<Move>> MOVES = new WeakHashMap<>();

	/** Carriages rolling: their bases (the nearest the aisle first), which way, how far, when it steps next. */
	static final class Move {
		final List<BlockPos> carriages;
		final Direction facing, dir;
		final int sign;
		int step;
		long next;

		Move(List<BlockPos> carriages, Direction facing, Direction dir, int sign, long next) {
			this.carriages = carriages;
			this.facing = facing;
			this.dir = dir;
			this.sign = sign;
			this.next = next;
		}
	}

	public MobileShelvingBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(SLIDE, REST).setValue(VARIANT, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(SLIDE, VARIANT);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape(state, (state.getValue(SLIDE) - REST) * 4.0);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		ServerLevel server = (ServerLevel) level;
		BlockPos base = base(state, pos);
		int label = state.getValue(VARIANT);
		if (state.getValue(SEG) != 0) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.ms_label", label + 1, NAMES[label]));
			return InteractionResult.SUCCESS_SERVER;
		}
		if (player.isShiftKeyDown()) {
			int next = (label + 1) % LABELS;
			Direction facing = state.getValue(FACING);
			for (int k = 0; k < SEGS; k++) {
				for (int h = 0; h < 2; h++) {
					BlockPos at = cell(base, facing, k, h);
					BlockState s = level.getBlockState(at);
					if (s.is(this)) {
						level.setBlock(at, s.setValue(VARIANT, next), Block.UPDATE_ALL);
					}
				}
			}
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.ms_label", next + 1, NAMES[next]));
			return InteractionResult.SUCCESS_SERVER;
		}
		turn(server, pos, player);
		return InteractionResult.SUCCESS_SERVER;
	}

	/** Whether a carriage at rest of this facing stands with its base at pos. */
	private boolean carriageAt(Level level, BlockPos pos, Direction facing) {
		BlockState s = level.getBlockState(pos);
		return s.is(this) && s.getValue(FACING) == facing && s.getValue(SEG) == 0 && s.getValue(HALF) == DoubleBlockHalf.LOWER;
	}

	/** Whether the slot with its base at pos is an open aisle: all the unit's eight cells empty. */
	private static boolean aisleAt(Level level, BlockPos pos, Direction facing) {
		for (int k = 0; k < SEGS; k++) {
			for (int h = 0; h < 2; h++) {
				if (!level.getBlockState(cell(pos, facing, k, h)).isAir()) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Turns the handwheel of the carriage at pos: finds its bank (the carriages either side of it, at rest, out to the
	 * fixed units or the walls) and the open aisle nearest it, and sets the carriage and those between it and the aisle
	 * rolling into the aisle; the aisle opens where the carriage stood. Refused with nobody moved when the bank is
	 * packed, already moving, or someone is in the aisle. Returns whether it started.
	 */
	public boolean turn(ServerLevel level, BlockPos pos, @Nullable Player player) {
		BlockState state = level.getBlockState(pos);
		if (!state.is(this)) {
			return false;
		}
		List<Move> moving = MOVES.computeIfAbsent(level, k -> new ArrayList<>());
		Direction facing = state.getValue(FACING);
		BlockPos base = base(state, pos);
		for (Move m : moving) {
			if (m.facing == facing && m.carriages.stream().anyMatch(c -> c.distManhattan(base) <= 2 * REACH)) {
				tell(player, "message.jugcraft.nuclear.ms_moving");
				return false;
			}
		}
		if (state.getValue(SLIDE) != REST) {
			tell(player, "message.jugcraft.nuclear.ms_moving");
			return false;
		}
		Direction right = facing.getClockWise();
		int gap = Integer.MAX_VALUE;
		for (int sign : new int[] {1, -1}) {
			for (int i = 1; i <= REACH; i++) {
				BlockPos at = base.relative(right, sign * i);
				if (aisleAt(level, at, facing)) {
					if (i < Math.abs(gap) || i == Math.abs(gap) && sign > 0) {
						gap = sign * i;
					}
					break;
				}
				if (!carriageAt(level, at, facing) || level.getBlockState(at).getValue(SLIDE) != REST) {
					break;
				}
			}
		}
		if (gap == Integer.MAX_VALUE) {
			tell(player, "message.jugcraft.nuclear.ms_no_aisle");
			return false;
		}
		int sign = Integer.signum(gap);
		BlockPos aisle = base.relative(right, gap);
		AABB box = new AABB(aisle.getX(), aisle.getY(), aisle.getZ(), aisle.getX() + 1, aisle.getY() + 2, aisle.getZ() + 1)
				.minmax(new AABB(cell(aisle, facing, SEGS - 1, 1)));
		if (!level.getEntitiesOfClass(LivingEntity.class, box.deflate(0.05), e -> e.isAlive() && !e.isSpectator()).isEmpty()) {
			tell(player, "message.jugcraft.nuclear.ms_busy");
			return false;
		}
		List<BlockPos> carriages = new ArrayList<>();
		for (int i = Math.abs(gap) - 1; i >= 0; i--) {
			carriages.add(base.relative(right, sign * i).immutable());
		}
		moving.add(new Move(carriages, facing, sign > 0 ? right : right.getOpposite(), sign, level.getGameTime() + 1));
		level.playSound(null, base, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.6F, 0.7F);
		return true;
	}

	private static void tell(@Nullable Player player, String key) {
		if (player != null) {
			player.sendOverlayMessage(Component.translatable(key).withStyle(ChatFormatting.YELLOW));
		}
	}

	/** Whether carriages are rolling in this level (the gametests wait on it). */
	public static boolean moving(Level level) {
		return !MOVES.getOrDefault(level, List.of()).isEmpty();
	}

	/** Each level tick: the rolling carriages step a quarter block, and at the next block are moved into it. */
	public static void tick(ServerLevel level) {
		List<Move> moving = MOVES.get(level);
		if (moving == null || moving.isEmpty()) {
			return;
		}
		long now = level.getGameTime();
		for (Iterator<Move> it = moving.iterator(); it.hasNext();) {
			Move m = it.next();
			if (now < m.next) {
				continue;
			}
			m.step++;
			m.next = now + STEP_TICKS;
			if (m.step < 4) {
				for (BlockPos c : m.carriages) {
					setSlide(level, c, m.facing, REST + m.sign * m.step);
				}
				if (m.step == 1) {
					level.playSound(null, m.carriages.get(0), SoundEvents.MINECART_RIDING, SoundSource.BLOCKS, 0.25F, 0.6F);
				}
				continue;
			}
			// At the next block: each carriage moved into it, the nearest the aisle first.
			for (BlockPos c : m.carriages) {
				for (int k = 0; k < SEGS; k++) {
					for (int h = 0; h < 2; h++) {
						BlockPos from = cell(c, m.facing, k, h);
						BlockState s = level.getBlockState(from);
						if (s.getBlock() instanceof MobileShelvingBlock) {
							level.setBlock(from.relative(m.dir), s.setValue(SLIDE, REST), Block.UPDATE_ALL);
							level.setBlock(from, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
						}
					}
				}
			}
			level.playSound(null, m.carriages.get(m.carriages.size() - 1), SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.4F, 0.6F);
			it.remove();
		}
	}

	private static void setSlide(ServerLevel level, BlockPos base, Direction facing, int slide) {
		for (int k = 0; k < SEGS; k++) {
			for (int h = 0; h < 2; h++) {
				BlockPos at = cell(base, facing, k, h);
				BlockState s = level.getBlockState(at);
				if (s.getBlock() instanceof MobileShelvingBlock) {
					level.setBlock(at, s.setValue(SLIDE, Math.max(0, Math.min(6, slide))), Block.UPDATE_CLIENTS);
				}
			}
		}
	}
}
