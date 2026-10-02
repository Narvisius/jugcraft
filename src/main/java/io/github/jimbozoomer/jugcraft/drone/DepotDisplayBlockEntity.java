package io.github.jimbozoomer.jugcraft.drone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A block that shows a drone depot: a control screen panel or a hologram table plate. Only the anchor
 * of a formed display (the screen's top-left panel, the table's middle plate) does anything: it
 * remembers which drone terminal it shows and tells nearby clients, who draw the depot's live readout
 * or hologram map. It re-checks the link every two seconds (a terminal placed or broken later is picked
 * up), which costs one look through the loaded terminals.
 */
public class DepotDisplayBlockEntity extends BlockEntity {
	public static final int RELINK_INTERVAL = 40;
	private @Nullable BlockPos terminal;

	/** Implemented by the blocks that show a depot: which state is the formed display's anchor. */
	public interface Display {
		boolean isAnchor(BlockState state);

		/** How far away the terminal may be. */
		int linkRange();
	}

	public DepotDisplayBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** The linked terminal, if this is a formed screen's anchor. */
	public @Nullable BlockPos terminal() {
		return terminal;
	}

	/** True for the anchor of a formed display (read from the world, which is always current). */
	public boolean isAnchor() {
		BlockState state = level == null ? getBlockState() : level.getBlockState(getBlockPos());
		return state.getBlock() instanceof Display display && display.isAnchor(state);
	}

	static void serverTick(Level level, BlockPos pos, BlockState state, DepotDisplayBlockEntity display) {
		if (state.getBlock() instanceof Display block && block.isAnchor(state) && (level.getGameTime() + pos.hashCode()) % RELINK_INTERVAL == 0) {
			display.relink();
		}
	}

	/** A terminal this display belongs to for good (a Drone Tower's own plotting table and video wall), or null. */
	private @Nullable BlockPos pinned;

	/** Ties this display to {@code terminal}: it shows that depot and no other, however close another one is. */
	public void pin(BlockPos terminal) {
		pinned = terminal.immutable();
		this.terminal = pinned;
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	/** Links to the nearest loaded terminal within range (or none); tells clients if that changed. */
	void relink() {
		if (level == null || level.isClientSide()) {
			return;
		}
		BlockPos best = null;
		if (pinned != null) {
			best = level.isLoaded(pinned) && !(level.getBlockEntity(pinned) instanceof DroneTerminalBlockEntity) ? null : pinned;
		} else if (isAnchor() && getBlockState().getBlock() instanceof Display display) {
			int range = display.linkRange();
			double bestDistance = Double.MAX_VALUE;
			for (BlockPos candidate : DroneDepots.terminals(level)) {
				double distance = candidate.distSqr(getBlockPos());
				if (distance <= range * range && distance < bestDistance
						&& level.getBlockEntity(candidate) instanceof DroneTerminalBlockEntity) {
					best = candidate;
					bestDistance = distance;
				}
			}
		}
		if (best == null ? terminal != null : !best.equals(terminal)) {
			terminal = best;
			setChanged();
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		terminal = input.getLong("terminal").map(BlockPos::of).orElse(null);
		pinned = input.getLong("pinned").map(BlockPos::of).orElse(null);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (terminal != null) {
			output.putLong("terminal", terminal.asLong());
		}
		if (pinned != null) {
			output.putLong("pinned", pinned.asLong());
		}
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}
}
