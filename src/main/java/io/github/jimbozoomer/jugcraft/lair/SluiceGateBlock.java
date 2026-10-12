package io.github.jimbozoomer.jugcraft.lair;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A sluice gate in the Cinder Kiln's walls (docs/features/cinder-kiln.md): its {@link Part#FRAME frame} (posts and
 * lintel), its {@link Part#PANEL panels} and its {@link Part#WHEEL wheel}, each block facing into the kiln. A lair fixture:
 * any player in the kiln may turn a gate by using any block of it. A gate that is {@link Flow#READY ready} opens: its
 * panels rise, and the trough of {@link TroughStoneBlock trough stone} running from under it floods for
 * {@value #FLOOD_TICKS} ticks. Then it closes, the trough drains, and it {@link Flow#FILLING fills} again for
 * {@value #REFILL_TICKS} ticks before it can be turned again; turned meanwhile, it only says so. In the Cinder Tyrant's
 * Eruption one gate at a time is {@link Flow#CHOKED choked} with slag and will not turn at all ({@link #choke}). Everything
 * happens on the server, on the block ticks the gate schedules.
 *
 * <p>A gate's blocks are found by walking from the one used through the blocks of the gate touching it (at most
 * {@value #GATE_BLOCKS}), its trough by walking from under the gate along the trough stone touching it (at most
 * {@value #TROUGH_BLOCKS}), so the kiln's layout (tools/cinder_kiln.py) needs no table here.
 */
public class SluiceGateBlock extends Block {
	/** A gate's parts (tools/lairs.py SLUICE_PARTS). */
	public enum Part implements StringRepresentable {
		FRAME, PANEL, WHEEL;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	/** Where a gate is in its turn (tools/lairs.py SLUICE_FLOWS): ready to open, open, filling again, or choked with slag. */
	public enum Flow implements StringRepresentable {
		READY, OPEN, FILLING, CHOKED;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	/** What turning a gate did. */
	public enum Turn {
		OPENED, ALREADY_OPEN, FILLING, CHOKED, NOT_A_GATE
	}

	public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<Flow> FLOW = EnumProperty.create("flow", Flow.class);
	public static final int FLOOD_TICKS = 200;
	public static final int REFILL_TICKS = 400;
	public static final int GATE_BLOCKS = 40;
	public static final int TROUGH_BLOCKS = 96;
	// The wheel, on the post behind it: its shaft, hub and rim fill the half of its block toward the post.
	private static final VoxelShape WHEEL_NORTH = Block.box(1, 1, 8, 15, 15, 16);
	private static final VoxelShape WHEEL_SOUTH = Block.box(1, 1, 0, 15, 15, 8);
	private static final VoxelShape WHEEL_EAST = Block.box(0, 1, 1, 8, 15, 15);
	private static final VoxelShape WHEEL_WEST = Block.box(8, 1, 1, 16, 15, 15);

	public SluiceGateBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(PART, Part.FRAME).setValue(FACING, Direction.NORTH).setValue(FLOW, Flow.READY));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(PART, FACING, FLOW);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (state.getValue(PART) != Part.WHEEL) {
			return Shapes.block();
		}
		return switch (state.getValue(FACING)) {
			case SOUTH -> WHEEL_SOUTH;
			case EAST -> WHEEL_EAST;
			case WEST -> WHEEL_WEST;
			default -> WHEEL_NORTH;
		};
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level instanceof ServerLevel server) {
			turn(server, pos, player);
		}
		return InteractionResult.SUCCESS;
	}

	/** Turns the gate that {@code pos} is part of, for {@code player} (told why when it cannot turn; null for nobody). */
	public static Turn turn(ServerLevel level, BlockPos pos, @Nullable Player player) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof SluiceGateBlock)) {
			return Turn.NOT_A_GATE;
		}
		Flow flow = state.getValue(FLOW);
		if (flow != Flow.READY) {
			if (player != null) {
				player.sendOverlayMessage(Component.translatable(switch (flow) {
					case OPEN -> "message.jugcraft.lair.sluice.open";
					case CHOKED -> "message.jugcraft.lair.sluice.choked";
					default -> "message.jugcraft.lair.sluice.filling";
				}));
			}
			return switch (flow) {
				case OPEN -> Turn.ALREADY_OPEN;
				case CHOKED -> Turn.CHOKED;
				default -> Turn.FILLING;
			};
		}
		Set<BlockPos> gate = gate(level, pos);
		set(level, gate, Flow.OPEN);
		flood(level, trough(level, gate), true);
		level.scheduleTick(pos, state.getBlock(), FLOOD_TICKS);
		level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 0.6F);
		level.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.5F);
		level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.5F, 0.6F);
		return Turn.OPENED;
	}

	/** The gate's turn moving on: an open gate closes and its trough drains; a filling gate is ready again. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		switch (state.getValue(FLOW)) {
			case OPEN -> {
				Set<BlockPos> gate = gate(level, pos);
				set(level, gate, Flow.FILLING);
				flood(level, trough(level, gate), false);
				level.scheduleTick(pos, state.getBlock(), REFILL_TICKS);
				level.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.5F);
				level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 0.6F);
			}
			case FILLING -> {
				set(level, gate(level, pos), Flow.READY);
				level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.7F);
			}
			default -> {
				// A tick left over from an earlier instance of the kiln, or from before the gate was choked or reset, or a
				// gate already ready: nothing to do.
			}
		}
	}

	/**
	 * Chokes the gate {@code pos} is part of with slag, or clears it ready to turn (the Cinder Tyrant's Eruption). A gate
	 * choked while open closes at once and its trough drains. Returns whether the gate changed.
	 */
	public static boolean choke(ServerLevel level, BlockPos pos, boolean choked) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof SluiceGateBlock)) {
			return false;
		}
		Flow flow = state.getValue(FLOW);
		if (choked == (flow == Flow.CHOKED)) {
			return false;
		}
		Set<BlockPos> gate = gate(level, pos);
		if (choked && flow == Flow.OPEN) {
			flood(level, trough(level, gate), false);
		}
		set(level, gate, choked ? Flow.CHOKED : Flow.READY);
		level.playSound(null, pos, choked ? SoundEvents.FIRECHARGE_USE : SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.6F);
		return true;
	}

	/** Sets the gate {@code pos} is part of ready to turn and drains its trough (a fight that ends or resets). */
	public static void reset(ServerLevel level, BlockPos pos) {
		if (!(level.getBlockState(pos).getBlock() instanceof SluiceGateBlock)) {
			return;
		}
		Set<BlockPos> gate = gate(level, pos);
		flood(level, trough(level, gate), false);
		set(level, gate, Flow.READY);
	}

	/** The blocks of the gate {@code pos} is part of, touching one another (at most {@value #GATE_BLOCKS}). */
	public static Set<BlockPos> gate(Level level, BlockPos pos) {
		Set<BlockPos> found = new LinkedHashSet<>();
		Deque<BlockPos> queue = new ArrayDeque<>();
		queue.add(pos.immutable());
		while (!queue.isEmpty() && found.size() < GATE_BLOCKS) {
			BlockPos at = queue.poll();
			if (found.contains(at) || !(level.getBlockState(at).getBlock() instanceof SluiceGateBlock)) {
				continue;
			}
			found.add(at);
			for (Direction direction : Direction.values()) {
				queue.add(at.relative(direction));
			}
		}
		return found;
	}

	/**
	 * The trough of the gate made of {@code gate}: the trough stone under its blocks, and all the trough stone touching that
	 * on its level (at most {@value #TROUGH_BLOCKS}).
	 */
	public static Set<BlockPos> trough(Level level, Set<BlockPos> gate) {
		Set<BlockPos> found = new LinkedHashSet<>();
		Deque<BlockPos> queue = new ArrayDeque<>();
		for (BlockPos at : gate) {
			queue.add(at.below());
		}
		while (!queue.isEmpty() && found.size() < TROUGH_BLOCKS) {
			BlockPos at = queue.poll();
			if (found.contains(at) || !(level.getBlockState(at).getBlock() instanceof TroughStoneBlock)) {
				continue;
			}
			found.add(at);
			for (Direction direction : Direction.Plane.HORIZONTAL) {
				queue.add(at.relative(direction));
			}
		}
		return found;
	}

	private static void set(Level level, Set<BlockPos> gate, Flow flow) {
		for (BlockPos at : gate) {
			BlockState state = level.getBlockState(at);
			if (state.getBlock() instanceof SluiceGateBlock && state.getValue(FLOW) != flow) {
				level.setBlock(at, state.setValue(FLOW, flow), Block.UPDATE_CLIENTS);
			}
		}
	}

	private static void flood(Level level, Set<BlockPos> trough, boolean flooded) {
		for (BlockPos at : trough) {
			BlockState state = level.getBlockState(at);
			if (state.getBlock() instanceof TroughStoneBlock && state.getValue(TroughStoneBlock.FLOODED) != flooded) {
				level.setBlock(at, state.setValue(TroughStoneBlock.FLOODED, flooded), Block.UPDATE_CLIENTS);
			}
		}
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(PART) != Part.PANEL) {
			return;
		}
		Direction out = state.getValue(FACING);
		double x = pos.getX() + 0.5 + out.getStepX() * 0.55;
		double z = pos.getZ() + 0.5 + out.getStepZ() * 0.55;
		if (state.getValue(FLOW) == Flow.OPEN) {
			// Water gushing from under the raised panel.
			level.addParticle(ParticleTypes.SPLASH, x + (random.nextDouble() - 0.5) * 0.8, pos.getY() + random.nextDouble(),
					z + (random.nextDouble() - 0.5) * 0.8, out.getStepX() * 0.2, 0.0, out.getStepZ() * 0.2);
		} else if (state.getValue(FLOW) == Flow.READY && random.nextInt(10) == 0) {
			// A ready gate holds back its stream: water seeps from under it.
			level.addParticle(ParticleTypes.DRIPPING_WATER, x + (random.nextDouble() - 0.5) * 0.8, pos.getY() + 0.2,
					z + (random.nextDouble() - 0.5) * 0.8, 0.0, 0.0, 0.0);
		} else if (state.getValue(FLOW) == Flow.CHOKED && random.nextInt(4) == 0) {
			// Slag oozing from its seams, smoking.
			level.addParticle(random.nextInt(3) == 0 ? ParticleTypes.LAVA : ParticleTypes.FLAME,
					x + (random.nextDouble() - 0.5) * 0.8, pos.getY() + random.nextDouble(), z + (random.nextDouble() - 0.5) * 0.8,
					0.0, 0.0, 0.0);
		}
	}
}
