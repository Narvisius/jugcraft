package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The plant's swinging doors (doors round): the steel door, the glass door, the lead-lined door (LeadDoorBlock) and
 * the restroom's stall doors (RestroomBlocks.StallDoor). A vanilla door's states and:
 * <ul>
 *   <li>MOVING: the leaf is swinging (SWING_OPEN or SWING_SHUT ticks). The block model then shows only the frame and
 *   client/PlantDoorRenderer.java draws the leaf turning about its hinge, from the door's part models
 *   (plant_door_part, PlantDoors). A door ignores clicks while it swings;</li>
 *   <li>PAIRED: a double door's leaf, the other leaf beside its latch edge (no jamb or stop between them);</li>
 *   <li>EXIT: panic hardware on the push side;</li>
 *   <li>READER: a badge reader on the wall of the push side (the side you come from): from that side the door opens
 *   only for its owner (PlantDoorBlockEntity: who placed it, or the plant's owner when /nuclear plant built it), the
 *   owner's party and players in creative mode. Anyone may always leave through it.</li>
 * </ul>
 * A door with a closer shuts itself a few seconds after it has swung open, once nobody is in the doorway; held open
 * by redstone it stays open. The models are tools/plant_doors.py's.
 */
public class PlantDoorBlock extends DoorBlock implements EntityBlock {
	public static final BooleanProperty MOVING = BooleanProperty.create("moving");
	public static final BooleanProperty PAIRED = BooleanProperty.create("paired");
	public static final BooleanProperty EXIT = BooleanProperty.create("exit");
	public static final BooleanProperty READER = BooleanProperty.create("reader");
	/** How long a swing takes (ticks): open, and shut (the closer brings it in slower). tools/plant_doors.py SWING_*. */
	public static final int SWING_OPEN = 10, SWING_SHUT = 16;
	/** How long a closer leaves the door standing open (ticks) before it shuts it. */
	public static final int CLOSE_AFTER = 60;

	private final boolean closer;

	public PlantDoorBlock(BlockSetType type, Properties properties, boolean closer) {
		super(type, properties);
		this.closer = closer;
		registerDefaultState(defaultBlockState().setValue(MOVING, false).setValue(PAIRED, false).setValue(EXIT, false)
				.setValue(READER, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(MOVING, PAIRED, EXIT, READER);
	}

	/** Whether this door has a closer. */
	public boolean closes() {
		return closer;
	}

	/** Ticks the closer leaves the door open. */
	protected int closeAfter() {
		return CLOSE_AFTER;
	}

	public static BlockPos foot(BlockState state, BlockPos pos) {
		return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new PlantDoorBlockEntity(pos, state) : null;
	}

	// ------------------------------------------------------------------------------------------------ swinging

	/**
	 * Swings the door (both halves) open or shut: MOVING for the swing's length, the sound, the game event. Server side.
	 */
	public void swing(Level level, BlockPos pos, BlockState state, boolean open, @Nullable Entity entity, boolean sound) {
		BlockPos foot = foot(state, pos);
		BlockState lower = level.getBlockState(foot);
		if (!lower.is(this)) {
			return;
		}
		// The upper half follows (DoorBlock.updateShape copies the lower's state).
		level.setBlock(foot, lower.setValue(OPEN, open).setValue(MOVING, true), Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
		if (sound) {
			level.playSound(entity, foot, open ? type().doorOpen() : type().doorClose(), SoundSource.BLOCKS, 1.0F,
					level.getRandom().nextFloat() * 0.1F + 0.9F);
		}
		level.gameEvent(entity, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, foot);
		level.scheduleTick(foot, this, open ? SWING_OPEN : SWING_SHUT);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!type().canOpenByHand()) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide() || state.getValue(MOVING)) {
			return InteractionResult.SUCCESS;
		}
		BlockPos foot = foot(state, pos);
		boolean open = !state.getValue(OPEN);
		if (open && state.getValue(READER) && onReaderSide(state, foot, player)) {
			boolean ok = mayPass(level, foot, player);
			level.playSound(null, foot, (ok ? SoundEvents.NOTE_BLOCK_BIT : SoundEvents.NOTE_BLOCK_BASS).value(), SoundSource.BLOCKS, 0.6F,
					ok ? 2.0F : 0.6F);
			if (!ok) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.door_reader_denied"));
				return InteractionResult.SUCCESS;
			}
		}
		swing(level, pos, state, open, player, true);
		return InteractionResult.SUCCESS;
	}

	@Override
	public void setOpen(@Nullable Entity entity, Level level, BlockState state, BlockPos pos, boolean open) {
		if (!state.is(this) || state.getValue(OPEN) == open || state.getValue(MOVING)) {
			return;
		}
		swing(level, pos, state, open, entity, true);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean moving) {
		boolean was = state.getValue(OPEN);
		super.neighborChanged(state, level, pos, block, orientation, moving);
		BlockState now = level.getBlockState(pos);
		if (!level.isClientSide() && now.is(this) && now.getValue(OPEN) != was) {
			// Redstone swung it: let it swing, not jump.
			BlockPos foot = foot(now, pos);
			BlockState lower = level.getBlockState(foot);
			if (lower.is(this)) {
				level.setBlock(foot, lower.setValue(MOVING, true), Block.UPDATE_CLIENTS);
				level.scheduleTick(foot, this, now.getValue(OPEN) ? SWING_OPEN : SWING_SHUT);
			}
		}
	}

	/** The end of a swing; or the closer's time is up: it shuts the door unless someone is in the doorway. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!state.is(this) || state.getValue(HALF) != DoubleBlockHalf.LOWER) {
			return;
		}
		if (state.getValue(MOVING)) {
			level.setBlock(pos, state.setValue(MOVING, false), Block.UPDATE_CLIENTS);
			if (state.getValue(OPEN) && closes()) {
				level.scheduleTick(pos, this, closeAfter());
			}
			return;
		}
		if (!closes() || !state.getValue(OPEN) || state.getValue(POWERED)) {
			return;
		}
		if (!level.getEntitiesOfClass(Entity.class, new AABB(pos).expandTowards(0, 1, 0), e -> !e.isSpectator()).isEmpty()) {
			level.scheduleTick(pos, this, 20);
			return;
		}
		swing(level, pos, state, false, null, true);
	}

	// ------------------------------------------------------------------------------------------------ the reader

	/** Whether the player stands on the side the door is pushed from (the reader's side). */
	public static boolean onReaderSide(BlockState state, BlockPos foot, Player player) {
		Direction in = state.getValue(FACING);
		double d = (player.getX() - (foot.getX() + 0.5)) * in.getStepX() + (player.getZ() - (foot.getZ() + 0.5)) * in.getStepZ();
		return d < 0;
	}

	/** The door's owner, the owner's party, and players in creative mode (anyone, for a door with no owner). */
	public static boolean mayPass(Level level, BlockPos foot, Player player) {
		if (player.getAbilities().instabuild) {
			return true;
		}
		if (!(level.getBlockEntity(foot) instanceof PlantDoorBlockEntity door) || door.owner() == null) {
			return true;
		}
		UUID owner = door.owner();
		return owner.equals(player.getUUID()) || JugcraftParties.sameParty(owner, player.getUUID());
	}

	// ------------------------------------------------------------------------------------------------ placing

	/** A double door's leaf: the leaf beside its latch edge is this door's other half of the pair. */
	public static boolean paired(BlockGetter level, BlockPos pos, BlockState state) {
		Direction facing = state.getValue(FACING);
		Direction latch = state.getValue(HINGE) == DoorHingeSide.LEFT ? facing.getClockWise() : facing.getCounterClockWise();
		BlockState other = level.getBlockState(pos.relative(latch));
		return other.getBlock() == state.getBlock() && other.getValue(FACING) == facing && other.getValue(HINGE) != state.getValue(HINGE)
				&& other.getValue(HALF) == state.getValue(HALF);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		return state == null ? null : state.setValue(PAIRED, paired(context.getLevel(), context.getClickedPos(), state));
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		BlockState out = super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
		return out.is(this) ? out.setValue(PAIRED, paired(level, pos, out)) : out;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (!level.isClientSide() && placer instanceof Player player && level.getBlockEntity(pos) instanceof PlantDoorBlockEntity door) {
			door.setOwner(player.getUUID());
		}
	}
}
