package io.github.jimbozoomer.jugcraft.control;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A control monitor panel's link (batch 37). Only the anchor (part 1) of a formed screen links: every
 * {@link #RELINK_INTERVAL} ticks it walks the data cables from each of its panels to the first logic controller, and
 * tells nearby clients which one. The client draws that controller's synced readings.
 */
public class ControlMonitorBlockEntity extends BlockEntity {
	public static final int RELINK_INTERVAL = 40;
	private @Nullable BlockPos controller;

	public ControlMonitorBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftControl.MONITOR_ENTITY, pos, state);
	}

	public @Nullable BlockPos controller() {
		return controller;
	}

	public boolean isAnchor() {
		BlockState state = level == null ? getBlockState() : level.getBlockState(worldPosition);
		return state.getBlock() instanceof ControlMonitorBlock && state.getValue(ControlMonitorBlock.PART) == 1;
	}

	void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
		if (state.getValue(ControlMonitorBlock.PART) == 1 && Math.floorMod(level.getGameTime() + pos.asLong(), RELINK_INTERVAL) == 0) {
			relink();
		}
	}

	/** Finds the controller cabled to this screen (or none); tells clients if that changed. */
	void relink() {
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		BlockPos found = null;
		if (isAnchor()) {
			for (BlockPos panel : ControlMonitorBlock.panels(worldPosition, getBlockState().getValue(ControlMonitorBlock.FACING))) {
				var controllers = ControlNetwork.find(server, panel).controllers();
				if (!controllers.isEmpty()) {
					found = controllers.getFirst().immutable();
					break;
				}
			}
		}
		if (found == null ? controller != null : !found.equals(controller)) {
			controller = found;
			setChanged();
			server.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		controller = input.getLong("controller").map(BlockPos::of).orElse(null);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (controller != null) {
			output.putLong("controller", controller.asLong());
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
