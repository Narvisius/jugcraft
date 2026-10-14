package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A station battery's charge (round 25; Justin: "make sure they actually hold charge in game"): a rack of 30 cells
 * holds {@link #CAPACITY} JE, kept when the world is saved. Its chargers charge it (LivePanelBlockEntity: a battery
 * charger linked to it); any cable touching the rack can charge it or draw on it. What went in and out last second is
 * kept for the DC switchboard's and the chargers' meters (synced with the charge).
 */
public class StationBatteryBlockEntity extends BlockEntity {
	public static final long CAPACITY = 1_500_000, RATE = 4_096;
	public final SimpleEnergyStorage energy = new SimpleEnergyStorage(CAPACITY, RATE, RATE, this::setChanged);
	/** JE a tick in (+) or out (-) over the last second, as the meters read it (synced). */
	public int flow;
	private long last = -1;
	private int timer;

	public StationBatteryBlockEntity(BlockPos pos, BlockState state) {
		super(PlantInterior.STATION_BATTERY_ENTITY, pos, state);
	}

	/** The charge as a share of full (0..1). */
	public double charge() {
		return energy.getAmount() / (double) CAPACITY;
	}

	public void serverTick(ServerLevel level) {
		if (timer-- > 0) {
			return;
		}
		timer = 19;
		long now = energy.getAmount();
		int f = last < 0 ? 0 : (int) ((now - last) / 20);
		last = now;
		if (f != flow || level.getGameTime() % 100 == 0) {
			flow = f;
			setChanged();
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	/** Full, as a plant's batteries are kept on float (a fuelled plant build). */
	public void fill() {
		energy.setAmount(CAPACITY);
		setChanged();
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		out.putLong("energy", energy.getAmount());
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		energy.setAmount(in.getLongOr("energy", 0L));
		flow = in.getIntOr("flow", 0);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
		out.putLong("energy", energy.getAmount());
		out.putInt("flow", flow);
		return out.buildResult();
	}
}
