package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** When a roller door block's curtain started moving, for how long, and which way (client/RollerDoorRenderer.java). */
public class RollerDoorBlockEntity extends BlockEntity {
	private long start;
	private int ticks = 1;
	private boolean opening;

	public RollerDoorBlockEntity(BlockPos pos, BlockState state) {
		super(PlantInterior.ROLLER_ENTITY, pos, state);
	}

	public void start(long start, int ticks, boolean opening) {
		this.start = start;
		this.ticks = Math.max(1, ticks);
		this.opening = opening;
		setChanged();
		if (level != null) {
			BlockState state = getBlockState();
			level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
		}
	}

	/** How far open the door is at this moment, 0 (down) to 1 (rolled up). */
	public float openness(long gameTime, float partialTick) {
		BlockState state = getBlockState();
		boolean open = state.getValue(RollerDoorBlock.OPEN);
		if (!state.getValue(RollerDoorBlock.MOVING)) {
			return open ? 1 : 0;
		}
		float t = Math.min(1, Math.max(0, (gameTime - start + partialTick) / ticks));
		return opening ? t : 1 - t;
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		start = input.getLong("start").orElse(0L);
		ticks = input.getIntOr("ticks", 1);
		opening = input.getBooleanOr("opening", false);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putLong("start", start);
		output.putInt("ticks", ticks);
		output.putBoolean("opening", opening);
	}
}
