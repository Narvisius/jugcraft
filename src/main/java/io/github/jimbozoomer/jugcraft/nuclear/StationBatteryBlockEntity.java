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
 * charger linked to it); any cable touching the rack can charge it or draw on it. What goes in and out is kept for the
 * meters (the DC switchboard's, the chargers', the battery rooms': synced with the charge).
 *
 * <p>It does not tick (Justin, 25 October 2026: the battery rooms only look live; "the plant's batteries still store
 * and give power when cabled, just without live calculating", the option he chose): only while energy moves through it
 * (each time a transfer commits) does it take the rate over the last second and send it with the charge, at most once a
 * second; once energy has stood still {@link #STALE} ticks a scheduled block tick (StationBatteryBlock) puts its rate
 * to zero and sends that. Idle, nothing runs.
 */
public class StationBatteryBlockEntity extends BlockEntity {
	public static final long CAPACITY = 1_500_000, RATE = 4_096;
	/** How long energy stands still before the rate reads zero, and the time a rate is taken over (ticks). */
	public static final int STALE = 40, SAMPLE = 20;
	public final SimpleEnergyStorage energy = new SimpleEnergyStorage(CAPACITY, RATE, RATE, this::moved);
	/** JE a tick in (+) or out (-) over the last second energy moved (synced). */
	private int rate;
	/** The charge when the second being taken began and when (-1: none begun), when energy last moved, the charge last sent. */
	private long sampleAmount = -1, sampleAt, movedAt, sentAmount = -1;

	public StationBatteryBlockEntity(BlockPos pos, BlockState state) {
		super(PlantInterior.STATION_BATTERY_ENTITY, pos, state);
	}

	/** The charge as a share of full (0..1). */
	public double charge() {
		return energy.getAmount() / (double) CAPACITY;
	}

	/** JE a tick in (+) or out (-) over the last second energy moved (zero standing still). */
	public int flow() {
		return rate;
	}

	/** Energy moved (a transfer committed): the rate over the second that just ended, sent with the charge. */
	private void moved() {
		setChanged();
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		long now = server.getGameTime(), amount = energy.getAmount();
		movedAt = now;
		server.scheduleTick(getBlockPos(), getBlockState().getBlock(), STALE + 1);
		if (sampleAmount < 0 || now - sampleAt > STALE) {
			// (Energy moving again after standing still: the second starts now.)
			sampleAmount = amount;
			sampleAt = now;
			return;
		}
		if (now - sampleAt < SAMPLE) {
			return;
		}
		int f = (int) ((amount - sampleAmount) / (now - sampleAt));
		boolean send = f != rate || Math.abs(amount - sentAmount) >= CAPACITY / 100;
		rate = f;
		sampleAmount = amount;
		sampleAt = now;
		if (send) {
			send(server);
		}
	}

	/** Its scheduled tick (StationBatteryBlock): energy has stood still long enough, its rate is zero (else again later). */
	void settle(ServerLevel level) {
		long still = level.getGameTime() - movedAt;
		if (still < STALE) {
			level.scheduleTick(getBlockPos(), getBlockState().getBlock(), (int) (STALE + 1 - still));
			return;
		}
		sampleAmount = -1;
		if (rate != 0 || energy.getAmount() != sentAmount) {
			rate = 0;
			send(level);
		}
	}

	private void send(ServerLevel level) {
		sentAmount = energy.getAmount();
		level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	/** The charge set by hand (a switchboard evening its strings, a plant build filling it): saved, and sent. */
	void changed() {
		setChanged();
		if (level instanceof ServerLevel server && energy.getAmount() != sentAmount) {
			send(server);
		}
	}

	/** Full, as a plant's batteries are kept on float (a fuelled plant build). */
	public void fill() {
		energy.setAmount(CAPACITY);
		changed();
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
		rate = in.getIntOr("flow", 0);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
		out.putLong("energy", energy.getAmount());
		out.putInt("flow", rate);
		return out.buildResult();
	}
}
