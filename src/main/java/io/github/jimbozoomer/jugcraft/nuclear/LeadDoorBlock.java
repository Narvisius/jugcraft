package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The lead-lined door (round 14): a heavy steel door with a lead core (closed, it stops 99.9% of radiation, Shielding;
 * open, nothing). Opened by hand, and:
 * <ul>
 *   <li>it closes itself (its door closer) a few seconds after it was opened, once nobody is in the doorway;</li>
 *   <li>lead-lined doors near each other form an airlock: one won't open while another of them is open (Justin's
 *   radwaste building: the vault is never open to the hall). A pair of doors side by side (double doors) counts as
 *   one door;</li>
 *   <li>a warning beacon showing green or red within two blocks of a door and up to three above its foot shows the
 *   interlock: green, the door may be opened; red, another door of the airlock is open (WarningBeaconBlock).</li>
 * </ul>
 */
public class LeadDoorBlock extends DoorBlock {
	/** How far apart (blocks, each way along the ground; up and down) doors are still one airlock. */
	public static final int AIRLOCK_REACH = 6, AIRLOCK_HEIGHT = 2;
	/** How long the door stays open before its closer shuts it (ticks). */
	public static final int CLOSE_AFTER = 100;

	public LeadDoorBlock(BlockSetType type, Properties properties) {
		super(type, properties);
	}

	private static BlockPos foot(BlockState state, BlockPos pos) {
		return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		BlockPos foot = foot(state, pos);
		if (!state.getValue(OPEN) && blockedBy(level, foot, state) != null) {
			if (!level.isClientSide()) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.airlock_interlock"));
				level.playSound(null, foot, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 0.8F, 0.8F);
			}
			return InteractionResult.SUCCESS;
		}
		InteractionResult result = super.useWithoutItem(state, level, pos, player, hit);
		if (!level.isClientSide()) {
			afterChange(level, foot);
		}
		return result;
	}

	@Override
	public void setOpen(Entity entity, Level level, BlockState state, BlockPos pos, boolean open) {
		super.setOpen(entity, level, state, pos, open);
		if (!level.isClientSide()) {
			afterChange(level, foot(state, pos));
		}
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, net.minecraft.world.level.redstone.Orientation orientation, boolean moving) {
		boolean was = state.getValue(OPEN);
		super.neighborChanged(state, level, pos, block, orientation, moving);
		BlockState now = level.getBlockState(pos);
		if (!level.isClientSide() && now.is(this) && now.getValue(OPEN) != was) {
			afterChange(level, foot(now, pos));
		}
	}

	/** After the door opened or shut: its closer is set going, and the airlock's beacons show the new state. */
	private void afterChange(Level level, BlockPos foot) {
		BlockState state = level.getBlockState(foot);
		if (state.is(this) && state.getValue(OPEN)) {
			level.scheduleTick(foot, this, CLOSE_AFTER);
		}
		showInterlock(level, foot);
	}

	/** The door closer: shut, unless someone is standing in the doorway (then try again in a second). */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!state.is(this) || state.getValue(HALF) != DoubleBlockHalf.LOWER || !state.getValue(OPEN) || state.getValue(POWERED)) {
			return;
		}
		if (!level.getEntitiesOfClass(Entity.class, new AABB(pos).expandTowards(0, 1, 0), e -> !e.isSpectator()).isEmpty()) {
			level.scheduleTick(pos, this, 20);
			return;
		}
		setOpen(null, level, state, pos, false);
	}

	/** Another door of this one's airlock that is open (so this one stays shut), or null. */
	public static BlockPos blockedBy(Level level, BlockPos foot, BlockState state) {
		for (BlockPos other : airlock(level, foot)) {
			if (!partner(foot, state, other, level.getBlockState(other)) && level.getBlockState(other).getValue(OPEN)) {
				return other;
			}
		}
		return null;
	}

	/** The feet of the other lead-lined doors within the airlock's reach. */
	static List<BlockPos> airlock(Level level, BlockPos foot) {
		List<BlockPos> out = new ArrayList<>();
		for (BlockPos p : BlockPos.betweenClosed(foot.offset(-AIRLOCK_REACH, -AIRLOCK_HEIGHT, -AIRLOCK_REACH),
				foot.offset(AIRLOCK_REACH, AIRLOCK_HEIGHT, AIRLOCK_REACH))) {
			if (!p.equals(foot)) {
				BlockState s = level.getBlockState(p);
				if (s.getBlock() instanceof LeadDoorBlock && s.getValue(HALF) == DoubleBlockHalf.LOWER) {
					out.add(p.immutable());
				}
			}
		}
		return out;
	}

	/** The other leaf of a double door: right beside this one in the same wall, facing the same way. */
	private static boolean partner(BlockPos foot, BlockState state, BlockPos other, BlockState otherState) {
		if (other.getY() != foot.getY() || otherState.getValue(FACING) != state.getValue(FACING)) {
			return false;
		}
		Direction side = state.getValue(FACING).getClockWise();
		return other.equals(foot.relative(side)) || other.equals(foot.relative(side.getOpposite()));
	}

	/** Sets every red or green beacon by the doors of this airlock: green where a door may open, red where it may not. */
	public static void showInterlock(Level level, BlockPos foot) {
		List<BlockPos> doors = airlock(level, foot);
		BlockState here = level.getBlockState(foot);
		if (here.getBlock() instanceof LeadDoorBlock) {
			doors.add(foot);
		}
		for (BlockPos door : doors) {
			BlockState state = level.getBlockState(door);
			boolean locked = blockedBy(level, door, state) != null;
			for (BlockPos p : BlockPos.betweenClosed(door.offset(-2, 1, -2), door.offset(2, 3, 2))) {
				BlockState lamp = level.getBlockState(p);
				if (lamp.getBlock() instanceof WarningBeaconBlock && lamp.getValue(WarningBeaconBlock.COLOUR) != WarningBeaconBlock.Colour.AMBER) {
					WarningBeaconBlock.Colour want = locked ? WarningBeaconBlock.Colour.RED : WarningBeaconBlock.Colour.GREEN;
					if (lamp.getValue(WarningBeaconBlock.COLOUR) != want) {
						level.setBlock(p, lamp.setValue(WarningBeaconBlock.COLOUR, want), Block.UPDATE_CLIENTS);
					}
				}
			}
		}
	}
}
